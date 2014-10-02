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
package com.radiusnetworks.museumguide;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.EditText;

import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.IBeaconData;
import com.radiusnetworks.ibeacon.Region;
import com.radiusnetworks.ibeacon.client.DataProviderException;
import com.radiusnetworks.proximity.ProximityKitManager;
import com.radiusnetworks.proximity.ProximityKitNotifier;
import com.radiusnetworks.proximity.ibeacon.powersave.BackgroundPowerSaver;
import com.radiusnetworks.proximity.licensing.PropertiesFile;
import com.radiusnetworks.proximity.model.KitIBeacon;
import com.radiusnetworks.museumguide.assets.AssetFetcherCallback;
import com.radiusnetworks.museumguide.assets.RemoteAssetCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dyoung on 2/28/14.
 *
 * This is the central application class for the Museum Guide.  It is responsible for:
 * 1. Initializing ProxomityKit, which downloads all the iBeacons associated with the museum
 *    along with their configured item_id, image_url, html_url, and title values.  It then starts
 *    ranging and monitoring for these iBeacons, and will continue to do so even across
 *    a reboot.
 * 2. Downloads all the museum item images and html pages needed.
 * 3. Updates the LoadingActivity with the status of the above download state.
 * 4. Once loading completes, launches the MuseumItemsActivity which is the main screen for the
 *    museum, which shows a swipe view of all the museum items.
 * 5. Handles all ranging and monitoring callbacks for iBeacons.  When an iBeacon is ranged,
 *    this class checks to see if it matches a museum item and displays a notification on the
 *    MuseumItemsActivity that the next museum item is nearby.
 */

public class MuseumGuideApplication extends Application implements ProximityKitNotifier {

    private static final String TAG = "MuseumGuideApplication";
    private ProximityKitManager manager;
    @SuppressWarnings("unused")
    private BackgroundPowerSaver backgroundPowerSaver;
    private LoadingActivity loadingActivity = null;
    private MuseumItemsActivity museumItemsActivity;
    private RemoteAssetCache remoteAssetCache;
    private String loadingFailedTitle;
    private String loadingFailedMessage;
    private boolean codeNeeded;
    private VisibleMuseumItems visibleMuseumItems = new VisibleMuseumItems();
    private Museum museum = null;
    private Boolean displayedIntro = false;
    int startCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application onCreate.  Museum is "+museum);
        backgroundPowerSaver = new BackgroundPowerSaver(this);
        manager = ProximityKitManager.getInstanceForApplication(this);
        manager.setNotifier(this);
        manager.getIBeaconManager().setDebug(false);

        if (!new PropertiesFile().exists()) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
            String code = settings.getString("code", null);
            Log.d(TAG, "Code is needed");
            this.codeNeeded = true;
            return;
        }
        else {
            Log.d(TAG, "Code is not needed because we have a properties file");
            startPk(null);
        }
    }

    public void startPk(String code) {
        Log.d(TAG, "startPk called with code "+code );
        if (code != null) {
            manager.restart(code);
        }
        else {
            manager.start(); // This starts ranging and monitoring for iBeacons defined in ProximityKit\
        }
    }

    /**
     * switches to a different museum
     * @param activity
     */
    public void startOver(Activity activity) {
        if (!new PropertiesFile().exists()) {
            Log.d(TAG, "clearing shared preferences");
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
            String code = settings.getString("code", null);
            Boolean dontShowAgain = settings.getBoolean("dont_show_intro_again", false);
            SharedPreferences.Editor editor = settings.edit();
            setDisplayedIntro(false);
            editor.clear();
            editor.putString("code", code);
            editor.putBoolean("dont_show_intro_again", dontShowAgain);
            editor.commit();
            museum = null;
            remoteAssetCache = new RemoteAssetCache(this);
            remoteAssetCache.clear();

            this.codeNeeded = true;
        }

        Intent i = new Intent(activity, LoadingActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        if (this.museumItemsActivity != null) {
            this.museumItemsActivity.finish();  // do this so it won't show up again on back press
        }
    }

    public void setLoadingActivity(LoadingActivity activity) {
        this.loadingActivity = activity;
        if (this.loadingFailedTitle != null) {
            showFailedErrorMessage();
        }
    }

    // if loading dependencies fails, we want to display a message to the user
    // we can only do so if the loading activity has already been created
    // otherwise, we store the messages for display a second or so later
    // when that activity finally launches
    private void dependencyLoadingFailed(String title, String message) {
        Log.d(TAG, "dependencyLoadingFailed");
        this.loadingFailedTitle = title;
        this.loadingFailedMessage = message;
        if (this.loadingActivity != null) {
            showFailedErrorMessage();
        }
    }

    // actually get the loading activity to display an error message to the user
    private void showFailedErrorMessage() {
        Log.d(TAG, "showFailedErrorMesage");
        loadingActivity.failAndTryAgain(loadingFailedTitle, loadingFailedMessage);
        loadingFailedTitle = null;
    }


    private long lastNextMuseumItemRecalculationTime = 0l;
    @Override
    public void iBeaconDataUpdate(IBeacon iBeacon, IBeaconData iBeaconData, DataProviderException e) {
        // Called every second with data from ProximityKit when an iBeacon defined in ProximityKit is nearby
        Log.d(TAG, "iBeaconDataUpdate: " + iBeacon.getProximityUuid() + " " + iBeacon.getMajor() + " " + iBeacon.getMinor());
        String itemId = null;
        if (iBeaconData != null) {
            itemId = iBeaconData.get("item_id");
        }
        if (itemId == null) {
            Log.d(TAG, "The iBeacon I just saw is not part of the museum, according to ProximityKit");
            return;
        }
        if (museum == null) {
            // Museum has not been initialized from PK yet.  Ignoring all iBeacons
            return;
        }
        MuseumItem item = museum.getItemById(itemId);
        if (item == null) {
            Log.w(TAG, "The iBeacon I just saw has a item_id of " + itemId + ", but it was not part of the museum when this app was started.");
            return;
        }
        else {
            visibleMuseumItems.detect(item, iBeacon.getAccuracy());
            long now = new java.util.Date().getTime();
            if (this.museumItemsActivity != null && now - lastNextMuseumItemRecalculationTime> 1000 /* 1 sec */) {
                this.museumItemsActivity.recalculateNextItem(visibleMuseumItems);
                lastNextMuseumItemRecalculationTime = now;
            }
        }


    }

    @Override
    public void didEnterRegion(Region region) {
        // Called when one of the iBeacons defined in ProximityKit first appears
        Log.d(TAG, "didEnterRegion");
        Log.d(TAG, "Sending notification.");
            sendNotification();
    }

    @Override
    public void didExitRegion(Region region) {
        // Called when one of the iBeacons defined in ProximityKit first disappears
        Log.d(TAG, "didExitRegion");
    }

    @Override
    public void didDetermineStateForRegion(int i, Region region) {
        // Called when one of the iBeacons defined in ProximityKit first appears or disappears
        Log.d(TAG, "didExitRegion");
    }

    @Override
    public void didSync() {
        // Called when ProximityKit data are updated from the server
        Log.d(TAG, "proximityKit didSync.  kit is " + manager.getKit());

        ArrayList<MuseumItem> museumItems = new ArrayList<MuseumItem>();
        Map<String, String> urlMap = new HashMap<String, String>();

        for (KitIBeacon iBeacon : manager.getKit().getIBeacons()) {
            String itemId = iBeacon.getAttributes().get("item_id");
            if (itemId != null) {
                String title = iBeacon.getAttributes().get("title");
                if (title == null) {
                    Log.e(TAG, "ERROR: No title specified in ProximityKit for item with item_id=" + itemId);
                }
                MuseumItem item = new MuseumItem(itemId, title);
                museumItems.add(item);
                String imageUrl = iBeacon.getAttributes().get("image_url");
                if (imageUrl == null) {
                    Log.e(TAG, "ERROR: No image_url specified in ProximityKit for item with item_id=" + itemId);
                }
                String htmlUrl = iBeacon.getAttributes().get("html_url");
                if (imageUrl == null) {
                    Log.e(TAG, "ERROR: No html_url specified in ProximityKit for item with item_id=" + itemId);
                }
                urlMap.put("item" + itemId, imageUrl);
                urlMap.put("item" + itemId+"_html", htmlUrl);
            }
        }

        if (museumItems.size() != 0) {
            if (loadingActivity != null && loadingActivity.isValidatingCode()) {
                loadingActivity.codeValidationPassed();
            }
        }
        else {
            if (loadingActivity != null && loadingActivity.isValidatingCode()) {
                loadingActivity.codeValidationFailed(new RuntimeException("No targets configured for the entered code"));
            }
        }

        // load the saved state of the hunt from the phone's persistent
        // storage
        museum = Museum.loadFromPreferences(this);
        boolean museumListChanged = museum.getItemList().size() != museumItems.size();
        for (MuseumItem item : museum.getItemList() ) {
            boolean itemFound = false;
            for (MuseumItem itemFromPk : museumItems) {
                if (itemFromPk.getId().equals(item.getId())) itemFound = true;
            }
            if (itemFound == false) {
                museumListChanged = true;
                Log.d(TAG, "Item with item_id="+item.getId()+" is no longer in PK.  Museum item list has changed.");
            }
        }
        if (museumListChanged) {
            Log.w(TAG, "the items in the museum have changed from what we have in the settings.  starting over");
            this.museum = new Museum(this,museumItems);
            this.museum.saveToPreferences(this);
        }

        // After we have all our data from ProximityKit, we need to download the images and html files
        // and cache them for display in the app.  We do this every time, so that the app can update the i
        // files, and have users get the update if they restart the app.
        remoteAssetCache = new RemoteAssetCache(this);
        remoteAssetCache.downloadAssets(urlMap, new AssetFetcherCallback() {
            @Override
            public void requestComplete() {
                dependencyLoadFinished();
            }

            @Override
            public void requestFailed(Integer responseCode, Exception e) {
                dependencyLoadFinished();
            }
        });
    }

    @Override
    public void didFailSync(Exception e) {
        // called when ProximityKit data are requested from the server, but the request fails
        if (loadingActivity != null && loadingActivity.isValidatingCode()) {
            Log.w(TAG, "proximityKit didFailSync due to " + e + "  bad code entered?");
            loadingActivity.codeValidationFailed(e);
            return;
        }
        Log.w(TAG, "proximityKit didFailSync due to " + e + "  We may be offline.");
        museum = Museum.loadFromPreferences(this);
        this.dependencyLoadFinished();
    }

    // This method is called when we have tried to download all dependencies (ProximityKit data and
    // all target images.)  This may or may not have failed, so we check that everything loaded
    // properly.
    public void dependencyLoadFinished() {
        Log.d(TAG, "all dependencies loaded");
        if (ProximityKitManager.getInstanceForApplication(this).getKit() == null || museum == null || museum.getItemList().size() == 0) {
            dependencyLoadingFailed("Network error", "Can't access museum data.  Please verify your network connection and try again.");
            return;
        }

        List<String> missingAssets = getMissingAssetList();
        if (missingAssets.size() == 0) {
            // Yes, we have everything we need to start up.  Let's start the image activity with the first exhibit item
            Intent i = new Intent(loadingActivity, MuseumItemsActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            i.putExtra("item_id", museum.getItemList().get(0).getId());
            startActivity(i);
            this.loadingActivity.finish(); // do this so that if we hit back, the loading activity won't show up again
            return;
        } else {
            dependencyLoadingFailed("Error loading museum", "Can't download images and/or html."+
                    "  Please verify your network connection and try again.  Missing items: "+
                    missingAssets.toString().replaceAll("(^\\[|\\]$)", ""));
            return;
        }
    }

    public VisibleMuseumItems getVisibleMuseumItems() {
        return visibleMuseumItems;
    }

    public Museum getMuseum() { return museum; }

    public MuseumItemsActivity getMuseumItemsActivity() {
        return museumItemsActivity;
    }

    public void setMuseumItemsActivity(MuseumItemsActivity museumItemsActivity) {
        this.museumItemsActivity = museumItemsActivity;
    }


    public boolean isCodeNeeded() { return this.codeNeeded; }

    public boolean hasDisplayedIntro() { return this.displayedIntro; }

    public void setDisplayedIntro(boolean value) { this.displayedIntro = value; }

    public RemoteAssetCache getRemoteAssetCache() {
        return remoteAssetCache;
    }

    public void setDontShowIntroAgain() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("dont_show_intro_again", true);
        editor.commit();
    }
    public boolean getDontShowIntroAgain() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        return settings.getBoolean("dont_show_intro_again", false);
    }

    // Checks to see that one found and one not found image has been downloaded for each target
    private List<String> getMissingAssetList() {
        Log.d(TAG, "Validating required assets are present");
        ArrayList<String> missingAssets = new ArrayList<String>();
        boolean missing = false;
        for (MuseumItem museumItem : museum.getItemList()) {
            if (remoteAssetCache.getImageByName("item" + museumItem.getId()) == null) {
                missingAssets.add("image for item "+museumItem.getId());
            }
            if (remoteAssetCache.getHtmlContentByName("item" + museumItem.getId() + "_html") == null) {
                missingAssets.add("html for item "+museumItem.getId());
            }
        }
        return missingAssets;
    }

    /*
     Sends a notification to the user when a beacon is nearby.
     */
    private void sendNotification() {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("Museum Guide")
                        .setContentText("An exhibit item is nearby.")
                        .setSmallIcon(R.drawable.launcher);


        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(new Intent(this, MuseumItemsActivity.class));
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }




}
