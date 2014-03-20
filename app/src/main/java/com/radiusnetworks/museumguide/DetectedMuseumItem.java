package com.radiusnetworks.museumguide;

import java.util.Date;

/**
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
