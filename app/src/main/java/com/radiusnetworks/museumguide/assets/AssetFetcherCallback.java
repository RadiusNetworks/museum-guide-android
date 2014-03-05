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

/**
 * Created by dyoung on 1/28/14.
 */
public interface AssetFetcherCallback {
    public void requestComplete();
    public void requestFailed(Integer resonseCode, Exception e);
}
