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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.radiusnetworks.museumguide.assets.RemoteAssetCache;

/**
 * This class represents a single page in the Intro as controlled by the IntroActivity
 *
 * Created by dyoung on 3/10/14.
 */
public class IntroFragment extends Fragment {
    private static final String TAG = "IntroFragment";
    private static final String ARG_OBJECT = "object";
    private boolean dontShowAgain = false;
    private View rootView;
    private int item;

    public IntroFragment(int page) {
        item = page+1;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(
                R.layout.intro_screen, container, false);

        View controlView = rootView.findViewById(R.id.introFinishControls);

        /*
         If we are on the last page of the intro, we show a special controlView that lets the user
         select if they ever want to see this again, and continue on to the main part of the app.
         */
        if (item==IntroActivity.NUM_INTRO_PAGES) {
            controlView.setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.checkBox).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dontShowAgainTapped();
                }
            });

            // This button lets the user move on to the main part of the app
            Button button  = (Button) rootView.findViewById(R.id.continueButton);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((IntroActivity)getActivity()).continueTapped(dontShowAgain);
                }
            });
        }
        else {
            controlView.setVisibility(View.INVISIBLE);
        }

        WebView webview = (WebView) rootView.findViewById(R.id.webView);
        webview.loadUrl("file:///android_asset/intro" + item + ".html");
        Log.d(TAG, "Loaded intro page URL: "+webview.getUrl());

        return rootView;
    }

    // Track the state of the checkbox here manually, because we are using a custom ImageButton so
    // we can make the checkbox have a special look
    public void dontShowAgainTapped() {
        dontShowAgain = !dontShowAgain;
        Log.d(TAG, "DONT SHOW AGAIN tapped.  it is now: " + dontShowAgain);
        if (dontShowAgain) {
            rootView.findViewById(R.id.checkBox).setBackground(getResources().getDrawable(R.drawable.checkbox_checked ));
        }
        else {
            rootView.findViewById(R.id.checkBox).setBackground(getResources().getDrawable(R.drawable.checkbox_unchecked ));
        }

    }
}
