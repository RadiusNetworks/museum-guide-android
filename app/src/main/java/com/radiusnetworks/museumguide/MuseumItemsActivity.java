package com.radiusnetworks.museumguide;

import android.content.Intent;
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
import android.widget.Button;

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

    private static final String TAG = "MuseumItemActivity";
    private MuseumGuideApplication application;
    private Stack<String> itemStack = new Stack<String>();
    private String currentItemId = null;
    private String nextItemId = null;
    private boolean autoMode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        setAutomaticMode();
        //setSequentialMode();
        hideNextItemNotification();
    }
    
    private void setSequentialMode() {
        sequentialItemCollectionPagerAdapter =
                new SequentialItemCollectionPagerAdapter(
                        getSupportFragmentManager());
        mViewPager.setAdapter(sequentialItemCollectionPagerAdapter);
        mViewPager.setOnPageChangeListener(sequentialItemPageChangeListener);
        autoMode = false;

    }
    private void setAutomaticMode() {
        visitedItemCollectionPagerAdapter =
                new VisitedItemCollectionPagerAdapter(
                        getSupportFragmentManager());
        mViewPager.setAdapter(visitedItemCollectionPagerAdapter);
        mViewPager.setOnPageChangeListener(visitedItemPageChangeListener);
        autoMode = true;
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
        return nextItemId;
    }

    private int getVisitedItemPageCount() {
        int pageCount = itemStack.size() + 1 + (nextItemId == null ? 0 : 1);
        Log.d(TAG, "page count is " + pageCount);
        return pageCount;
    }

    // returns true if the user is currently on the "next" page
    private boolean isNewPage(int currentPage) {
        return (nextItemId != null && currentPage == getVisitedItemPageCount() - 1);
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
            if (pos+1 < MuseumItemsActivity.this.application.getMuseum().getItemList().size()) {
                Log.d(TAG, "showing next item");
                MuseumItem item = MuseumItemsActivity.this.application.getMuseum().getItemList().get(pos+1);
                showNextItemNotification(item.getId(), item.getTitle());
            }
            else {
                Log.d(TAG, "not showing next item -- there is not one.  current pos="+pos+", total items="+MuseumItemsActivity.this.application.getMuseum().getItemList().size());
                hideNextItemNotification();
            }
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
                Log.d(TAG, "This is the new item page that just went into view.  We need to make it current.");
                itemStack.push(currentItemId);
                currentItemId = nextItemId;
                nextItemId = null;
            } else {
                Log.d(TAG, "We seem to have gone backward");
                nextItemId = currentItemId;
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
        titleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "next item button tapped");
                if (autoMode) {
                    Log.e(TAG, "Need to program next item in auto mode");
                }
                else {
                    // scroll to next item
                    int index = MuseumItemsActivity.this.application.getMuseum().getItemIndexById(itemId);
                    mViewPager.setCurrentItem(index);
                }
            }
        });
    }

    private void hideNextItemNotification() {
        View button = findViewById(R.id.nextButtonLayout);
        button.setVisibility(View.INVISIBLE);
    }

    /*
     Called when an ibeacon detection changes the next item
     */
    private void showNextItem(final String itemId) {
        if (!autoMode) {
            return;
        }
        else {
            if (itemId != null && itemId.equals(nextItemId)) {
                Log.d(TAG, "Next item is unchanged.  Doing nothing");
            }
            else {
                nextItemId = itemId;
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (itemId == null) {
                            hideNextItemNotification();
                        }
                        else {
                            showNextItemNotification(itemId, application.getMuseum().getItemById(itemId).getTitle());
                        }
                    }
                });
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
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://developer.radiusnetworks.com/ibeacon/ibeacon_locate/help.html"));
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


}