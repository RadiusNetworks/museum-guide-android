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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by dyoung on 3/5/14.
 */

/**
 * We need to filter which iBeacon associated with a MuseumItem is the closest.  If we do not filter
 * this, the one we think is closest will jump back and forth with radio noise.  There are various
 * algorithms that might help with this.  Several are below with PROs and CONs:
 *
 * 1. No filtering.  Simply consider whatever reading has lowest distance to be the closest in any
 *    one second cycle.
 *
 *    PRO: quickly shifts to new items
 *    --CON: flips back and forth, often to the item you were just at
 *
 * 2. Add momentum.  Once an item is established as the closest, you must get 2 subsequent
 *    determinations that a different item is closest before you change the closest one.
 *
 *    PRO: little flipping back and forth
 *    --CON: sluggish to change.  Can take several seconds after going to a new item before it is
 *           considered closest
 *
 * 3. Filter out the current displayed museum item and the previous displayed museum item.
 *
 *    PRO:  quickly shifts to new items when they are nearby.
 *    CON:  no way to go back to the previous item being the closest.
 *    CON:  can flip back and forth between two new items.
 *
 *
 *    Best test condition for this is to have four museum items.  Go to first, then the second, then
 *    have the third and fourth visible simultanously.  See how it behaves in offering 3 & 4.
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
