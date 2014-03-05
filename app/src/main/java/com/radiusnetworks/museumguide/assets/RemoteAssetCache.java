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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dyoung on 1/28/14.
 *
 * Keeps a cache in the Android filesystem of the images needed for the application.
 * Provides a mechanism to download them and retrieve them later.
 */
public class RemoteAssetCache {
    private static final String TAG = "RemoteAssetCache";
    private int assetsToDownload = 0;
    private int failureCount = 0;
    private Context context;
    private static final Pattern DENSITY_PATTERN = Pattern.compile("^(.*_)([hxl]+)(dpi\\..*)");
    private AssetFetcherCallback callback;
    private Exception lastException;
    private Integer lastResponseCode;

    public RemoteAssetCache(Context context) {
        this.context = context;
    }

    /**
     * Downloads a set of images from a web server, based on a passed map keyed off of
     * the desired local filename that points to the remote URL as a string.
     *
     * If the URLString has a screen density modifier on it (e.g. _hdpi), and that image
     * cannot be downloaded, this class will retry downloading it from the _mdpi variant.
     * When complete, all files that were successfully downloaded are stored on the Android
     * file system in the home directory of the application under the filenames in the keys
     * to the assetUrls Map.  If all were downloaded successfully, the requestComplete callback
     * will be called, otherwise the requestFailed callback will be called.
     * @param assetUrls
     * @param callback
     */
    public void downloadAssets(Map<String,String> assetUrls, AssetFetcherCallback callback) {
        if (assetsToDownload > 0) {
            throw new RuntimeException("already downloading assets");
        }
        Log.d(TAG, "downloadAssets called with count of "+assetUrls.size());
        this.callback = callback;
        assetsToDownload = assetUrls.size();

        for (String standardizedFilename : assetUrls.keySet()) {
            final String filenameToSave = standardizedFilename;
            final String assetUrl = assetUrls.get(standardizedFilename);
            Log.d(TAG,"Trying to download asset at "+ assetUrl);
            AssetFetcher assetFetcher = new AssetFetcher(context, assetUrl, filenameToSave, new AssetFetcherCallback() {
                @Override
                public void requestComplete() {
                    Log.d(TAG, "Successfully downloaded "+assetUrl);
                    assetsToDownload--;
                    if (assetsToDownload == 0) {
                        if (failureCount == 0) {
                            RemoteAssetCache.this.callback.requestComplete();
                        }
                        else {
                            RemoteAssetCache.this.callback.requestFailed(lastResponseCode, lastException);
                        }

                    }
                }

                @Override
                public void requestFailed(Integer responseCode, Exception e) {
                    Log.w(TAG, "Failed to load "+ assetUrl);
                    RemoteAssetCache.this.lastException = e;
                    RemoteAssetCache.this.lastResponseCode = responseCode;

                    // If this was a specfic dpi url, fallback to the mdpi url
                    Matcher matcher = DENSITY_PATTERN.matcher(assetUrl);
                    // only try to get the mdpi version if this wasn't the mdpi version
                    if (matcher.matches() && matcher.group(2) != null && !matcher.group(2).equals("m")) {
                        final String mdpiUrl = matcher.group(1) + "m" + matcher.group(3);
                        AssetFetcher assetFetcher = new AssetFetcher(context, mdpiUrl, filenameToSave, new AssetFetcherCallback() {

                            @Override
                            public void requestComplete() {
                                Log.d(TAG, "Successfully downloaded "+mdpiUrl);
                                assetsToDownload--;
                                if (assetsToDownload == 0) {
                                    if (failureCount == 0) {
                                        RemoteAssetCache.this.callback.requestComplete();
                                    }
                                    else {
                                        RemoteAssetCache.this.callback.requestFailed(lastResponseCode, lastException);
                                    }

                                }
                            }

                            @Override
                            public void requestFailed(Integer responseCode, Exception e) {
                                Log.w(TAG, "Failed to load "+ mdpiUrl);
                                RemoteAssetCache.this.lastException = e;
                                RemoteAssetCache.this.lastResponseCode = responseCode;
                                assetsToDownload--;
                                failureCount++;

                            }
                        });
                        assetFetcher.execute();

                    }
                    else {
                        failureCount++;
                        assetsToDownload--;
                    }
                    if (assetsToDownload == 0) {
                        if (failureCount == 0) {
                            RemoteAssetCache.this.callback.requestComplete();
                        }
                        else {
                            RemoteAssetCache.this.callback.requestFailed(lastResponseCode, lastException);
                        }
                    }

                }
            });
            assetFetcher.execute();
        }

    }

    /**
     * Returns an ImageView of an image asset in the cache, keyed by the local filename
     * @param name
     * @return
     */
    public ImageView getImageByName(String name) {
        ImageView imageView = null;
        try {
            String fname = context.getFilesDir().getAbsolutePath()+"/"+name;
            Bitmap bitmap = BitmapFactory.decodeFile(fname);
            if (bitmap == null) {
                Log.d(TAG, "Can't load image named "+name+".  Bitmap is null.");
                return null;
            }
            imageView = new ImageView(context);
            imageView.setImageBitmap(bitmap);
        }
        catch (Exception e) {
            Log.d(TAG, "Can't load image named "+name, e);
        }
        return imageView;
    }

    /**
     * Returns an a string holding Html content from the local filename
     * @param name
     * @return
     */
    public String getHtmlContentByName(String name) {
        try {
            String fname = context.getFilesDir().getAbsolutePath()+"/"+name;
            File file = new File(fname);
            StringBuilder sb = new StringBuilder();

            Scanner scanner = new Scanner(file);

            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine());
            }
            scanner.close();
            return sb.toString();

        } catch (FileNotFoundException e) {
            Log.d(TAG, "Can't load html named "+name, e);
        }
        return null;
    }



    /**
     * Deletes all cached files
     */
    public void clear() {
        File file = new File(context.getFilesDir().getAbsolutePath());
        String[] files;

        files = file.list();
        for (int i=0; i < files.length; i++) {
            Log.d(TAG, "deleting "+files[i]);
            new File(file, files[i]).delete();
        }
    }



}
