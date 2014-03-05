package com.radiusnetworks.museumguide;

/**
 * Created by dyoung on 2/28/14.
 */
public class MuseumItem {
    private String itemId;
    public MuseumItem(String itemId) {
        this.itemId = itemId;
    }
    public String getId() {
        return itemId;
    }
}
