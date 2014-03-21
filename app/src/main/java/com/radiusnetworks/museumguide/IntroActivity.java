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
import android.view.View;
import android.widget.Toast;

/**
 * Created by dyoung on 3/20/14.
 */
public class IntroActivity extends FragmentActivity {
    // When requested, this adapter returns a DemoObjectFragment,
    // representing an object in the collection.
    IntroCollectionPagerAdapter introCollectionPagerAdapter;

    ViewPager mViewPager;

    private static final String TAG = "IntroActivity";
    private MuseumGuideApplication application;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        application = (MuseumGuideApplication) this.getApplication();

        mViewPager = (ViewPager) findViewById(R.id.pager);
        introCollectionPagerAdapter =
                new IntroCollectionPagerAdapter(
                        getSupportFragmentManager());
        mViewPager.setAdapter(introCollectionPagerAdapter);
        Log.d(TAG, "intro activity started up");
    }

    @Override
    protected void onResume() {
        super.onResume();
        mViewPager.setCurrentItem(0); // go back to first page
        Context context = getApplicationContext();
        CharSequence text = "Swipe right to continue";
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
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
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            Log.d(TAG, "Got count (3) from  pager adapter that will create fragments");
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "POSITION " + (position + 1);
        }
    }

}

