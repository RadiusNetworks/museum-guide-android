package com.radiusnetworks.museumguide;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.Toast;

import java.util.Date;
import java.util.Stack;


/**
 * Created by dyoung on 3/6/14.
 */
public class MuseumItemsActivity extends FragmentActivity {
    // When requested, this adapter returns a DemoObjectFragment,
    // representing an object in the collection.
    VisitedItemCollectionPagerAdapter visitedItemCollectionPagerAdapter;
    SequentialItemCollectionPagerAdapter sequentialItemCollectionPagerAdapter;

    ViewPager mViewPager;

    private static final String TAG = "MuseumItemsActivity";
    private MuseumGuideApplication application;
    private Stack<String> itemStack = new Stack<String>();
    private String currentItemId = null;
    private String nextItemId = null;
    private MuseumControls museumControls;


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
            // TODO: load stack from preferences (should have saved off where you were)
            currentItemId = application.getMuseum().getItemList().get(0).getId();
        }
        mViewPager = (ViewPager) findViewById(R.id.pager);
        museumControls = new MuseumControls(findViewById(R.id.activity_museum_items));
        findViewById(R.id.search_dialog).setVisibility(View.INVISIBLE);

        setAutomaticMode();
        //setSequentialMode();
        hideNextItemNotification();
        Log.d(TAG, "Museum at startup:");
        for (MuseumItem item : application.getMuseum().getItemList() ) {
            Log.d(TAG, "Item id "+item.getId()+" has title: "+item.getTitle());
        }
    }
    
    private void setSequentialMode() {
        sequentialItemCollectionPagerAdapter =
                new SequentialItemCollectionPagerAdapter(
                        getSupportFragmentManager());
        mViewPager.setAdapter(sequentialItemCollectionPagerAdapter);
        mViewPager.setOnPageChangeListener(sequentialItemPageChangeListener);
        museumControls.setAutoMode(false);
        nextItemId = null;
        hideNextItemNotification();

    }
    private void setAutomaticMode() {
        visitedItemCollectionPagerAdapter =
                new VisitedItemCollectionPagerAdapter(
                        getSupportFragmentManager());
        mViewPager.setAdapter(visitedItemCollectionPagerAdapter);
        mViewPager.setOnPageChangeListener(visitedItemPageChangeListener);
        museumControls.setAutoMode(true);
        nextItemId = null;
        itemStack.empty();
        hideNextItemNotification();
    }
    
    private String getItemForSequentialPage(int i) {
        MuseumItem item = application.getMuseum().getItemList().get(i);
        return item.getId();
    }

    private int getSequentialItemPageCount() {
        return application.getMuseum().getItemList().size();
    }
    
    
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
        Log.d(TAG, "page count is " + pageCount);
        return pageCount;
    }

    // returns true if the user is currently on the "next" page
    // TODO delete this
    private boolean isNewPage(int currentPage) {
        return (nextItemId != null && currentPage == getVisitedItemPageCount() - 1);
    }

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
                    mViewPager.setCurrentItem(application.getMuseum().getItemIndexById(matchToShow.getId()));
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
            /*
            if (pos+1 < MuseumItemsActivity.this.application.getMuseum().getItemList().size()) {
                Log.d(TAG, "showing next item");
                MuseumItem item = MuseumItemsActivity.this.application.getMuseum().getItemList().get(pos+1);
                showNextItemNotification(item.getId(), item.getTitle());
            }
            else {
                Log.d(TAG, "not showing next item -- there is not one.  current pos="+pos+", total items="+MuseumItemsActivity.this.application.getMuseum().getItemList().size());
                hideNextItemNotification();
            }
            */
        }

    };

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
            Log.d(TAG, "We just swiped to a new page");
            Log.d(TAG, "count is " + getVisitedItemPageCount());
            Log.d(TAG, "pos is " + pos);
            Log.d(TAG, "nextItemId=" + nextItemId);
            Log.d(TAG, "stack size is " + itemStack.size());
            if (isNewPage(pos)) {
                Log.d(TAG, "THIS SHOULD NEVER HAPPEN: This is the new item page that just went into view.  We need to make it current.");
                /*
                itemStack.push(currentItemId);
                currentItemId = nextItemId;
                nextItemId = null;
                */
            } else {
                if (itemStack.size() > 0) {
                    Log.d(TAG, "We seem to have gone backward");
                    nextItemId = null;
                    currentItemId = itemStack.pop();
                    visitedItemCollectionPagerAdapter.notifyDataSetChanged(); // we just deleted the last page
                }
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
                    int currentPage = mViewPager.getCurrentItem();
                    itemStack.push(currentItemId);
                    currentItemId = itemId;
                    mViewPager.getAdapter().notifyDataSetChanged();
                    mViewPager.setCurrentItem(currentPage+1);
                }
                else {
                    // scroll to next item
                    int index = MuseumItemsActivity.this.application.getMuseum().getItemIndexById(itemId);
                    mViewPager.setCurrentItem(index);
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

    private void hideNextItemNotification() {
        View button = findViewById(R.id.nextButtonLayout);
        button.setVisibility(View.INVISIBLE);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem settings = menu.add("Exit museum");
        settings.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        settings.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                application.startOver(MuseumItemsActivity.this);
                return true;
            }
        });
        MenuItem help = menu.add("Help");
        help.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        help.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://developer.radiusnetworks.com/museum_guide/help.html"));
                startActivity(browserIntent);
                return true;
            }
        });
        return true;
    }


    //mPagerAdapter.notifyDataSetChanged()
    class VisitedItemCollectionPagerAdapter extends FragmentStatePagerAdapter {
        public VisitedItemCollectionPagerAdapter(FragmentManager fm) {
            super(fm);
            Log.d(TAG, "Constructed pager adapter that will create fragments");
        }

        @Override
        public Fragment getItem(int i) {
            ScreenFragment fragment = new ScreenFragment();
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

    //mPagerAdapter.notifyDataSetChanged()
    class SequentialItemCollectionPagerAdapter extends FragmentStatePagerAdapter {
        public SequentialItemCollectionPagerAdapter(FragmentManager fm) {
            super(fm);
            Log.d(TAG, "Constructed pager adapter that will create fragments");
        }

        @Override
        public Fragment getItem(int i) {
            ScreenFragment fragment = new ScreenFragment();
            String itemId = application.getMuseum().getItemList().get(i).getId();
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
            return application.getMuseum().getItemList().size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "POSITION " + (position + 1);
        }
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

}