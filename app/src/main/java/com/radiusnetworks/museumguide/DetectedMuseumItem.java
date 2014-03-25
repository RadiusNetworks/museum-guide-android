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

import java.util.Date;

/**
 * This object is a MuseumItem whose corresponding iBeacon has been detected by the app.  The time
 * of the last detection and the distance the item was when it was detected are stored.  This is used
 * by the VisibleMuseumItems class to determine which other museum items are nearby.
 *
 * Created by dyoung on 2/28/14.
 */
public class DetectedMuseumItem extends MuseumItem {
    private Date detectedTime;
    private double distance;
    public DetectedMuseumItem(MuseumItem item, double distance) {
        super();
        this.itemId = item.itemId;
        this.title = item.title;
        this.distance = distance;
        this.detectedTime = new Date();
    }
    public Date getDetectedTime() {
        return detectedTime;
    }
    public double getDistance() {
        return distance;
    }
}
