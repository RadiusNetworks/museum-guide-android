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
    Stack<String>itemStack = new Stack<String>();
    String nextItemId = null;
    String currentItemId = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        application = (MuseumGuideApplication) this.getApplication();
        application.setMuseumItemActivity(this);

        setContentView(R.layout.activity_museum_item);
        if (this.getIntent().getExtras() != null) {
            currentItemId = this.getIntent().getExtras().getString("item_id");
        }
        else {
            // TODO: load stack from preferences (should have saved off where you were)
            currentItemId = application.getMuseum().getItemList().get(0).getId();
        }
        showItem();
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

    View.OnClickListener nextItemClickListener = new View.OnClickListener() {
        public void onClick(View view) {
            itemStack.push(currentItemId);
            currentItemId = nextItemId;
            nextItemId = null;
            showItem();
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


    public void setNearestItem(MuseumItem item) {
        final Button button = (Button) findViewById(R.id.nextbutton);
        if (currentItemId.equals(item.getId())) {
            Log.d(TAG, "Not switching to another item.  We are already displaying item " + item.getId());
            return;
        }
        nextItemId = item.getId();
        runOnUiThread(new Runnable() {
            public void run() {
                ViewParent parent = button.getParent();
                parent.bringChildToFront(button);
                button.setText("Next item #" + nextItemId);
                button.setVisibility(View.VISIBLE);
                button.setAlpha(200);
                button.setBackgroundColor(Color.BLACK);
                button.setOnClickListener(nextItemClickListener);
            }
        });
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
        Button button = (Button) findViewById(R.id.nextbutton);
        button.setVisibility(View.INVISIBLE);

    }

}
