/**
 *
 * Copyright (c) 2013,2014 RadiusNetworks. All rights reserved.
 * http://www.radiusnetworks.com
 *
 * @author David G. Young
 *
 * Licensed to the Attribution Assurance License (AAL)
 * (adapted from the original BSD license) See the LICENSE file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 */
package com.radiusnetworks.museumguide.assets;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.radiusnetworks.ibeacon.IBeaconData;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by dyoung on 12/10/13.
 *
 * Asynchronously fetches a single static asset from a server
 * given a URL and stores it in the Android file system in the application
 * root directory under the filename.  Calls back to the requestComplete or RequestFailed
 * of the passed AssetFetcherCallback depending on how the request
 * goes.  If successful, the file will have been saved in the passed location.
 *
 */

public class AssetFetcher extends AsyncTask<Void, Void, Void> {

    private static final String TAG = "AsynchReader";
    private Exception exception = null;
    private String urlString = null;
    private String response = null;
    private int responseCode;
    private AssetFetcherCallback callback;
    private Context context;
    private String filename;

    public AssetFetcher(Context context, String urlString, String filename, AssetFetcherCallback callback) {
        this.context = context;
        this.callback = callback;
        this.filename = filename;
        this.urlString = urlString;
    }


    @Override
    protected Void doInBackground(Void... params) {
        try {
            this.request();
            callback.requestComplete();
            return null;
        }
        catch (SecurityException e) {
            Log.e(TAG, "Can't fetch asset at"+urlString+".  Have you added android.permission.INTERNET to your manifest?");
            callback.requestFailed(404, e);
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, "No asset exists at "+urlString);
            callback.requestFailed(404, e);
        }
        catch (Exception e) {
            callback.requestFailed(null, e);
            Log.e(TAG, "Can't fetch data due to "+e, e);
        }
        return null;
    }

    protected void onPostExecute(IBeaconData data) {
    }

    public void request() throws SecurityException, FileNotFoundException, java.io.IOException {
        response = null;
        exception = null;
        String currentUrlString = urlString;
        int requestCount = 0;
        StringBuilder responseBuilder = new StringBuilder();
        URL url = null;
        HttpURLConnection conn = null;
        do {
            if (requestCount != 0) {
                Log.d(TAG, "Following redirect from " + urlString + " to " + conn.getHeaderField("Location"));
                currentUrlString = conn.getHeaderField("Location");
            }
            requestCount++;
            responseCode = -1;
            try {
                url = new URL(currentUrlString);
            } catch (Exception e) {
                Log.e(TAG, "Can't construct URL from: " + urlString);
                exception = e;

            }
            if (url == null) {
                Log.d(TAG, "URL is null.  Cannot make request");
            } else {
                conn = (HttpURLConnection) url.openConnection();
                responseCode = conn.getResponseCode();
                Log.d(TAG, "response code is "+conn.getResponseCode());
            }
        }
        while (requestCount < 10 && responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                || responseCode == HttpURLConnection.HTTP_MOVED_PERM
                || responseCode == HttpURLConnection.HTTP_SEE_OTHER);

        FileOutputStream outputStream = null;
        InputStream inputStream;

        try {
            outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
            inputStream = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int bytesRead = 0;
            do {
                bytesRead = inputStream.read(buffer);
                if (bytesRead > 0) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            while (bytesRead > 0);
            outputStream.close();
        }
        finally {
            if (outputStream != null) {
                outputStream.close();
            }
        }

    }

}
