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

import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by dyoung on 3/5/14.
 */

/**
 * We need to filter which iBeacon associated with a MuseumItem is the closest.  If we do not filter
 * this, the one we think is closest will jump back and forth with radio noise.
 *
 * Filter out the current displayed museum item and the previous displayed museum item, so these
 * can never get displayed.  Any other items will flip back and forth with noise, unless one is
 * consistently closer than the other.
 *
 */
public class VisibleMuseumItems {
    // determines how many subsequent closest detections must take place for
    private static final int MILLISECONDS_TO_TRACK_ITEMS = 10*1000;
    private static final String TAG = "VisibleMuseumItems";

    private long oldestTrackedItemTime = -1;
    private HashMap<String,DetectedMuseumItem> detectedMuseumItems = new HashMap<String,DetectedMuseumItem>();

    public void detect(MuseumItem item, double distance) {
        DetectedMuseumItem detectedItem = new DetectedMuseumItem(item, distance);
        if (oldestTrackedItemTime == -1) {
            oldestTrackedItemTime = detectedItem.getDetectedTime().getTime();
        }
        detectedMuseumItems.put(item.getId(), detectedItem);
    }

    public MuseumItem calculateClosestItem() {
        return calculateClosestItemWithExceptions(null, null);
    }
    public MuseumItem calculateClosestItemWithExceptions(final String blacklistedItemId1, final String blacklistedItemId2) {
        purgeOldItems();
        DetectedMuseumItem currentClosestItem = null;
        synchronized (detectedMuseumItems) {
            for (DetectedMuseumItem item : detectedMuseumItems.values()) {
                if (item.getId().equals(blacklistedItemId1) || item.getId().equals(blacklistedItemId2)) {
                    Log.d(TAG, "closest? ignoring blacklisted item: "+item.getId());
                }
                else {
                    Log.d(TAG, "closest? itemId: "+item.getId()+", distance: "+item.getDistance());
                    if (currentClosestItem == null || item.getDistance() < currentClosestItem.getDistance()) {
                        currentClosestItem = item;
                    }
                }
            }
            Log.d(TAG, "closest? this cycle winner: "+(currentClosestItem == null ? null : currentClosestItem.getId()));
        }
        return currentClosestItem;
    }


    private void purgeOldItems() {
        long now = new Date().getTime();
        if (oldestTrackedItemTime == -1 || now - oldestTrackedItemTime < MILLISECONDS_TO_TRACK_ITEMS) {
            // nothing is out of date, so nothing to do here.
            return;
        }

        ArrayList<String> idsToDelete = new ArrayList<String>();
        synchronized (detectedMuseumItems) {
            for (String id : detectedMuseumItems.keySet()) {
                if (now - detectedMuseumItems.get(id).getDetectedTime().getTime() > MILLISECONDS_TO_TRACK_ITEMS) {
                    idsToDelete.add(id);
                }
            }
            for (String id: idsToDelete) {
                Log.d(TAG, "purging museum item id: " + id + " from tracked list");
                detectedMuseumItems.remove(id);
            }
        }
    }
}
