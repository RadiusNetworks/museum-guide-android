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

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Date;
import java.util.Stack;


/**
 * This class is the main activity for the application.  It controls a swipeable list of
 * MuseumItemFragments that can be in one of two modes.  Sequential mode is a static list of all
 * items in the museum that can be browsed.  Automatic mode is a list of all the items the user has
 * visited before, and new items are put on the stack when the user taps a notification bar at the
 * top of the screen when an item is nearby, which causes a new item to be displayed and put on the
 * stack of what the user has visited.
 *
 * Created by dyoung on 3/6/14.
 */
public class MuseumItemsActivity extends FragmentActivity {
    private static final String TAG = "MuseumItemsActivity";
    private ViewPager viewPager;
    private VisitedItemCollectionPagerAdapter visitedItemCollectionPagerAdapter;
    private SequentialItemCollectionPagerAdapter sequentialItemCollectionPagerAdapter;
    private MuseumGuideApplication application;
    private Stack<String> itemStack = new Stack<String>();
    private String currentItemId = null;
    private String nextItemId = null;
    private MuseumControls museumControls;
    private boolean mSuppressPageChangeEvents = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_museum_items);
        application = (MuseumGuideApplication) this.getApplication();

        application.setMuseumItemsActivity(this);

        if (this.getIntent().getExtras() != null) {
            currentItemId = this.getIntent().getExtras().getString("item_id");
        } else {
            // By default, show the very first item in the museum, unless told to do otherwise
            currentItemId = application.getMuseum().getItemList().get(0).getId();
        }
        viewPager = (ViewPager) findViewById(R.id.pager);
        museumControls = new MuseumControls(findViewById(R.id.activity_museum_items));
        findViewById(R.id.search_dialog).setVisibility(View.INVISIBLE);

        setAutomaticMode();
        hideNextItemNotification();
        Log.d(TAG, "Museum at startup:");
        for (MuseumItem item : application.getMuseum().getItemList() ) {
            Log.d(TAG, "Item id "+item.getId()+" has title: "+item.getTitle());
        }
    }

    /**
     * Sets this Activity to sequential mode.  This mode is a simple sequential list of all items
     * in the museum that the user swipes through.  iBeacons are not used.
     */
    private void setSequentialMode() {
        sequentialItemCollectionPagerAdapter =
                new SequentialItemCollectionPagerAdapter(
                        getSupportFragmentManager());
        viewPager.setAdapter(sequentialItemCollectionPagerAdapter);
        viewPager.setOnPageChangeListener(sequentialItemPageChangeListener);
        museumControls.setAutoMode(false);
        nextItemId = null;
        hideNextItemNotification();

    }

    /**
     * Sets this Activity to automatic mode.  This mode shows a swipeable history of where the user
     * has been, but new items are only shown by tapping on a notification when one becomoes nearby.
     */
    private void setAutomaticMode() {
        visitedItemCollectionPagerAdapter =
                new VisitedItemCollectionPagerAdapter(
                        getSupportFragmentManager());
        viewPager.setAdapter(visitedItemCollectionPagerAdapter);
        viewPager.setOnPageChangeListener(visitedItemPageChangeListener);
        museumControls.setAutoMode(true);
        nextItemId = null;
        itemStack.empty();
        hideNextItemNotification();
    }

    //------------------------------------------------------------------
    // The following section of code is only for Sequential Mode
    //------------------------------------------------------------------

    private String getItemForSequentialPage(int i) {
        MuseumItem item = application.getMuseum().getItemList().get(i);
        return item.getId();
    }

    private int getSequentialItemPageCount() {
        return application.getMuseum().getItemList().size();
    }

    /**
     * Executed when the user taps the search button on the search dialog.  It looks for an exact
     * match to the item id first, and if it doesn't find one, it tries to do a partial string match
     * to the item title.  If any match is found, the pager scrolls to that screen.
     *
     * @param text
     */
    private void search(String text) {
        // first, look for an exact match on id
        MuseumItem match = null;
        match = application.getMuseum().getItemById(text);
        if (match == null) {
            for (MuseumItem candidate : application.getMuseum().getItemList()) {
                if (candidate.getTitle().toLowerCase().indexOf(text.toLowerCase()) >= 0) {
                    match = candidate;
                    break;
                }
            }
        }
        if (match == null) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Item not found");
            builder.setMessage("No matching museum item exists.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.show();

        }
        else {
            final MuseumItem matchToShow = match;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    viewPager.setCurrentItem(application.getMuseum().getItemIndexById(matchToShow.getId()));
                }
            });
        }
    }

    ViewPager.OnPageChangeListener sequentialItemPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageScrollStateChanged(int arg0) {
            Log.d(TAG, "scrollStateChanged");
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
            Log.d(TAG, "pageScrolled");
        }

        @Override
        public void onPageSelected(int pos) {
            Log.d(TAG, "We just swiped to a new page");
            Log.d(TAG, "count is " + getVisitedItemPageCount());
            Log.d(TAG, "pos is " + pos);
        }

    };

    class SequentialItemCollectionPagerAdapter extends FragmentStatePagerAdapter {
        public SequentialItemCollectionPagerAdapter(FragmentManager fm) {
            super(fm);
            Log.d(TAG, "Constructed pager adapter that will create fragments");
        }

        @Override
        public Fragment getItem(int i) {
            MuseumItemFragment fragment = new MuseumItemFragment();
            String itemId = application.getMuseum().getItemList().get(i).getId();
            Log.d(TAG, "setting up fragment with " + itemId);
            fragment.showItem(itemId);

            return fragment;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_UNCHANGED;
        }

        @Override
        public int getCount() {
            Log.d(TAG, "Got count from  pager adapter that will create fragments");
            return application.getMuseum().getItemList().size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "POSITION " + (position + 1);
        }
    }


    //------------------------------------------------------------------
    // The following section of code is only for Automatic Mode
    //------------------------------------------------------------------

    private String getItemForVisitedPage(int i) {
        String itemId = null;
        if (i < itemStack.size()) {
            return itemStack.get(i);
        }
        if (i == itemStack.size()) {
            return currentItemId;
        }
        return null;
    }

    private int getVisitedItemPageCount() {
        int pageCount = itemStack.size() + 1;
        Log.d(TAG, "previously visited page count is " + pageCount);
        return pageCount;
    }

    ViewPager.OnPageChangeListener visitedItemPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageScrollStateChanged(int arg0) {
            Log.d(TAG, "scrollStateChanged visited");
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
            Log.d(TAG, "pageScrolled visited");

        }

        @Override
        public void onPageSelected(int pos) {
            // This is needed because on Android L, this event fires when we manually change the
            // page when a user taps on the button to go to the next item.  Without the suppression,
            // it was deciding that the user had swiped backwards, effectively leaving the user stuck
            // on a single page in Automatic mode.
            if (mSuppressPageChangeEvents) {
                Log.d(TAG, "*** onPageSelected event suppressed ***");
                return;
            }
            Log.d(TAG, "We just swiped to a new page");
            Log.d(TAG, "count is " + getVisitedItemPageCount());
            Log.d(TAG, "pos is " + pos);
            Log.d(TAG, "nextItemId=" + nextItemId);
            Log.d(TAG, "previously visited page count is " + itemStack.size());
            if (itemStack.size() > 0) {
                Log.d(TAG, "We seem to have gone backward");
                nextItemId = null;
                currentItemId = itemStack.pop();
                visitedItemCollectionPagerAdapter.notifyDataSetChanged(); // we just deleted the last page
            }
        }

    };


    public void recalculateNextItem(VisibleMuseumItems visibleMuseumItems) {
        String lastItemId = itemStack.empty() ? null : itemStack.peek();
        Log.d(TAG, "closest: ignoring " + lastItemId + " and " + currentItemId);
        MuseumItem nextItem = visibleMuseumItems.calculateClosestItemWithExceptions(currentItemId, lastItemId);
        showNextItem(nextItem == null ? null : nextItem.getId());
    }

    private void showNextItemNotification(final String itemId, final String title) {
        View button = findViewById(R.id.nextButtonLayout);
        Button titleButton = (Button) findViewById(R.id.nextButtonTitle);
        titleButton.setText("Next Item: " + title);
        button.setVisibility(View.VISIBLE);
        Log.d(TAG, "seting up tap listener");
        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "next item button tapped");
                if (museumControls.getAutoMode()) {
                    int currentPage = viewPager.getCurrentItem();
                    mSuppressPageChangeEvents = true;
                    itemStack.push(currentItemId);
                    currentItemId = itemId;
                    viewPager.getAdapter().notifyDataSetChanged();
                    viewPager.setCurrentItem(currentPage+1);
                    mSuppressPageChangeEvents = false;
                    Log.d(TAG, "We should have scrolled to the next item.  Everything should be done.");
                }
                else {
                    // scroll to next item
                    int index = MuseumItemsActivity.this.application.getMuseum().getItemIndexById(itemId);
                    viewPager.setCurrentItem(index);
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        View button = findViewById(R.id.nextButtonLayout);
                        button.setVisibility(View.INVISIBLE);
                    }
                });
            }
        };
        ((Button) findViewById(R.id.nextButton)).setOnClickListener(onClickListener);
        ((Button) findViewById(R.id.nextButtonTitle)).setOnClickListener(onClickListener);

    }

    /*
     Called when an ibeacon detection changes the next item
     */
    private void showNextItem(final String itemId) {
        if (!museumControls.getAutoMode()) {
            return;
        }
        else {
            if (itemId != null && itemId.equals(nextItemId)) {
                Log.d(TAG, "Next item is unchanged.  Doing nothing");
            }
            else {
                // only show the next item if it has been five seconds since entering auto
                // mode.  this gives the user time to read the instructions that appear
                // underneath
                if (museumControls.getSecondsSinceModeSwitch() > 5) {
                    nextItemId = itemId;
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (itemId == null) {
                                hideNextItemNotification();
                            }
                            else {
                                MuseumItem item  = application.getMuseum().getItemById(itemId);
                                Log.d(TAG, "showing next item "+item.getId()+" with title "+item.getTitle());
                                showNextItemNotification(item.getId(), item.getTitle());
                                View headerInstructions = findViewById(R.id.headerInstructions);
                                headerInstructions.setVisibility(View.INVISIBLE);
                            }
                        }
                    });
                }
                return;
            }
        }
        runOnUiThread(new Runnable() {
            public void run() {
                if (itemId == null) {
                    nextItemId = itemId;
                    visitedItemCollectionPagerAdapter.notifyDataSetChanged(); // we just added a page!
                    return;
                } else if (itemId.equals(nextItemId)) {
                    Log.d(TAG, "Next item is unchanged.  Doing nothing");
                } else {
                    nextItemId = itemId;
                    visitedItemCollectionPagerAdapter.notifyDataSetChanged(); // we just changed a page!
                }
            }
        });

    }

    class VisitedItemCollectionPagerAdapter extends FragmentStatePagerAdapter {
        public VisitedItemCollectionPagerAdapter(FragmentManager fm) {
            super(fm);
            Log.d(TAG, "Constructed pager adapter that will create fragments");
        }

        @Override
        public Fragment getItem(int i) {
            MuseumItemFragment fragment = new MuseumItemFragment();
            String itemId = getItemForVisitedPage(i);
            Log.d(TAG, "setting up fragment with " + itemId);
            fragment.showItem(itemId);

            return fragment;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            Log.d(TAG, "Got count from  pager adapter that will create fragments");
            return getVisitedItemPageCount();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "POSITION " + (position + 1);
        }
    }

    //--------------------------------------------------------------

    private void hideNextItemNotification() {
        View button = findViewById(R.id.nextButtonLayout);
        button.setVisibility(View.INVISIBLE);
    }


    private class MuseumControls {
        public MuseumControls(View controlLayoutView) {
            mainView = controlLayoutView;
            autoButton = (ImageButton) mainView.findViewById(R.id.autoButton);
            sequentialButton = (ImageButton) mainView.findViewById(R.id.sequentialButton);
            autoButton.setOnClickListener(modeTapListener);
            sequentialButton.setOnClickListener(modeTapListener);
            searchButton = (ImageView) mainView.findViewById(R.id.searchButton);
            searchButton.setOnClickListener(searchTapListener);
            headerInstructions = (TextView) mainView.findViewById(R.id.headerInstructions);
        }

        private View mainView;
        private ImageButton autoButton;
        private ImageButton sequentialButton;
        private ImageView searchButton;
        private TextView headerInstructions;
        private Date modeSwitchTime = new Date();

        private boolean autoMode;

        public void setAutoMode(boolean mode) {
            if (autoMode != mode) {
                modeSwitchTime = new Date();
                autoMode = mode;
                updateDisplay();
            }
        }
        public boolean getAutoMode() {
            return autoMode;
        }
        public long getSecondsSinceModeSwitch() {
            return (new Date().getTime() - modeSwitchTime.getTime()) / 1000;
        }

        public void updateDisplay() {
            runOnUiThread(new Runnable() {
                public void run() {
                    int museumGreen = Color.parseColor("#aecc79");
                    if (autoMode) {
                        autoButton.setBackgroundColor(museumGreen);
                        sequentialButton.setBackgroundColor(Color.WHITE);
                        sequentialButton.setImageResource(R.drawable.ordered_off);
                        searchButton.setImageResource(R.drawable.search_off);
                        autoButton.setImageResource(R.drawable.auto);
                        headerInstructions.setText("Automatic mode: Tap top bar when next item is nearby.  Swipe left to go back.");
                        headerInstructions.setVisibility(View.VISIBLE);
                    }
                    else {
                        sequentialButton.setBackgroundColor(museumGreen);
                        autoButton.setBackgroundColor(Color.WHITE);
                        sequentialButton.setImageResource(R.drawable.ordered);
                        autoButton.setImageResource(R.drawable.auto_off);
                        searchButton.setImageResource(R.drawable.search);
                        headerInstructions.setText("Browsing mode: swipe left and right to explore the museum.");
                        headerInstructions.setVisibility(View.VISIBLE);
                    }

                    // Now swtich the viewcontroller's functionality
                    if(autoMode) {
                        setAutomaticMode();
                    }
                    else {
                        setSequentialMode();
                    }
                }
            });
        }

        public View.OnClickListener modeTapListener = new View.OnClickListener() {
            public void onClick(View view) {
                Log.d(TAG, "button tapped");
                if (autoMode) {
                    if (view == sequentialButton)
                    setAutoMode(false);
                }
                else if (view == autoButton) {
                    setAutoMode(true);
                }
            }
        };

        public View.OnClickListener searchTapListener = new View.OnClickListener() {
            public void onClick(View view) {
                Log.d(TAG, "search button tapped");
                if (autoMode) {
                    Log.d(TAG, "ignored in auto mode");
                    return;
                }
                if (findViewById(R.id.search_dialog).getVisibility() == View.INVISIBLE) {
                    Log.d(TAG, "search dialog not shown");
                    ((EditText) findViewById(R.id.search_text)).setText("");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            findViewById(R.id.search_dialog).setVisibility(View.VISIBLE);
                            findViewById(R.id.search_go_button).setOnClickListener(searchGoTapListener);
                            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        }
                    });
                }

            }
        };

        public View.OnClickListener searchGoTapListener = new View.OnClickListener() {
            public void onClick(View view) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.search_dialog).setVisibility(View.INVISIBLE);
                        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(findViewById(R.id.search_text).getWindowToken(), 0);
                    }
                });

                search(((EditText) findViewById(R.id.search_text)).getText().toString());
            }
        };

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return new OptionsMenuCreator(this, application).onCreateOptionsMenu(menu);
    }

}