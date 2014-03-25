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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

/**
 * This Activity shows a walkthrough of how the app works using a swipe paging control
 * The pages are displayed as webviews within a FragmentStatePagerAdapter.  Each page view is
 * represented by the IntroFragment class.
 *
 * Created by dyoung on 3/20/14.
 */
public class IntroActivity extends FragmentActivity {
    private static final String TAG = "IntroActivity";
    public static final int NUM_INTRO_PAGES = 3;
    private IntroCollectionPagerAdapter introCollectionPagerAdapter;
    private ViewPager viewPager;
    private MuseumGuideApplication application;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        application = (MuseumGuideApplication) this.getApplication();

        viewPager = (ViewPager) findViewById(R.id.pager);
        introCollectionPagerAdapter =
                new IntroCollectionPagerAdapter(
                        getSupportFragmentManager());
        viewPager.setAdapter(introCollectionPagerAdapter);
        Log.d(TAG, "intro activity started up");
    }


    @Override
    protected void onResume() {
        super.onResume();
        // go back to first page whenever we restart
        viewPager.setCurrentItem(0);
        Context context = getApplicationContext();

        // Give the user a hint that they swipe to navigate
        CharSequence text = "Swipe right to continue";
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return new OptionsMenuCreator(this, application).onCreateOptionsMenu(menu);
    }

    public void continueTapped(boolean dontShowAgain) {
        Log.d(TAG, "DONT SHOW AGAIN IS "+dontShowAgain);
        if (dontShowAgain) {
            application.setDontShowIntroAgain();
        }
        application.setDisplayedIntro(true);
        Intent intent = new Intent(this, LoadingActivity.class);
        startActivity(intent);
        finish();
    }

    class IntroCollectionPagerAdapter extends FragmentStatePagerAdapter {
        public IntroCollectionPagerAdapter(FragmentManager fm) {
            super(fm);
            Log.d(TAG, "Constructed pager adapter that will create fragments");
        }

        @Override
        public Fragment getItem(int i) {
            IntroFragment fragment = new IntroFragment(i);
            Log.d(TAG, "constructed fragment with " + i);
            return fragment;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_UNCHANGED;
        }

        @Override
        public int getCount() {
            return NUM_INTRO_PAGES;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "POSITION " + (position + 1);
        }
    }

}

