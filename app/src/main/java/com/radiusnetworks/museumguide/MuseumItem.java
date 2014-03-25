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
 * Represents a single item in the museum.  The values are set in ProximityKit and the objects are
 * instantiated with PK syncs.
 *
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
