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
import android.os.Bundle;
import android.webkit.WebView;

/**
 * Created by dyoung on 2/14/14.
 */
public class HelpActivity extends Activity {
    public static final String TAG = "HelpActivity";
    private MuseumGuideApplication application = null;
    private boolean validatingCode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sh_activity_help);

        application = (MuseumGuideApplication) this.getApplication();
        WebView webView = (WebView) this.findViewById(R.id.webView);
        webView.loadUrl("http://developer.radiusnetworks.com/museum_guide/help.html");
    }

}