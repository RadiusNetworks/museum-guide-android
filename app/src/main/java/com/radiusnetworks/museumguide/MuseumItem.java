package com.radiusnetworks.museumguide;

import java.util.Date;

/**
 * Created by dyoung on 2/28/14.
 */
public class MuseumItem {
    protected String itemId;
    protected String title;
    protected MuseumItem() {

    }
    public MuseumItem(String itemId, String title) {
        this.itemId = itemId;
        this.title = title;
    }
    public String getId() {
        return itemId;
    }
    public String getTitle() {
        return title;
    }
}
