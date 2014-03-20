package com.radiusnetworks.museumguide;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.view.ViewParent;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;

import com.radiusnetworks.museumguide.assets.RemoteAssetCache;

import java.util.Stack;

public class MuseumItemActivity extends Activity {
    private static final String TAG = "MuseumItemActivity";

    private MuseumGuideApplication application;
    private Stack<String>itemStack = new Stack<String>();
    private String currentItemId = null;
    private String nextItemId = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        application = (MuseumGuideApplication) this.getApplication();
        //application.setMuseumItemsActivity(this);

        setContentView(R.layout.activity_museum_item);
        if (this.getIntent().getExtras() != null) {
            currentItemId = this.getIntent().getExtras().getString("item_id");
        }
        else {
            // TODO: load stack from preferences (should have saved off where you were)
            currentItemId = application.getMuseum().getItemList().get(0).getId();
        }

        //findViewById(R.id.nearestButton).setOnClickListener(nearestItemClickListener);
        findViewById(R.id.nextButton).setOnClickListener(nextItemClickListener);
        findViewById(R.id.nextButton).setVisibility(View.INVISIBLE);
        showItem();
    }

    public void recalculateNextItem(VisibleMuseumItems visibleMuseumItems) {
        String lastItemId = itemStack.empty()? null : itemStack.peek();
        Log.d(TAG, "closest: ignoring "+lastItemId+" and "+currentItemId);
        MuseumItem nextItem = visibleMuseumItems.calculateClosestItemWithExceptions(currentItemId, lastItemId);
        showNextItem(nextItem == null ? null : nextItem.getId());
    }

    private void showNextItem(final String itemId) {
        runOnUiThread(new Runnable() {
            public void  run() {
                if (itemId == null) {
                    nextItemId = itemId;
                    findViewById(R.id.nextButton).setVisibility(View.INVISIBLE);
                    return;
                }
                else if (itemId.equals(nextItemId)) {
                    Log.d(TAG, "Next item is unchanged.  Doing nothing");
                }
                findViewById(R.id.nextButton).setVisibility(View.VISIBLE);
            }
        });
        nextItemId = itemId;

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem settings = menu.add("Exit museum");
        settings.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        settings.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                application.startOver(MuseumItemActivity.this);
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

    View.OnClickListener nearestItemClickListener = new View.OnClickListener() {
        public void onClick(View view) {

            itemStack.push(currentItemId);
            MuseumItem closest = application.getVisibleMuseumItems().calculateClosestItem();
            Log.d(TAG, "Closest item is "+closest);
            if (closest != null) {
                itemStack.push(currentItemId);
                currentItemId = closest.getId();
                showItem();
                nextItemId = null;
                showNextItem(null);
            }
            else {
                // TODO: show a spinner here and try again
            }
        }
    };

    View.OnClickListener nextItemClickListener = new View.OnClickListener() {
        public void onClick(View view) {
            Log.d(TAG, "Tapping the next item");
            if (nextItemId == null) {
                Log.e(TAG, "nextItemId is null.  Button should not be visible");
            }
            else {
                itemStack.push(nextItemId);
                currentItemId = nextItemId;
                nextItemId = null;
                showItem();
                showNextItem(nextItemId);
            }

        }
    };


    @Override
    public void onBackPressed() {
        if (itemStack.isEmpty()) {
            this.finish();
        }
        else {
            nextItemId = currentItemId;
            currentItemId = itemStack.pop();
            showItem();
        }
    }

    private void showItem() {
        WebView webview = (WebView) this.findViewById(R.id.webView);
        RemoteAssetCache assetCache = new RemoteAssetCache(this);
        String html = assetCache.getHtmlContentByName("item"+currentItemId+"_html");
        ImageView imageView2 = assetCache.getImageByName("item"+currentItemId);
        if (imageView2 != null) {
            ImageView imageView = (ImageView) this.findViewById(R.id.museumItemImage);
            imageView.setImageDrawable(imageView2.getDrawable());
        }
        if (html != null) {
            webview.loadData(html, "text/html", null);
        }
    }

}
