package com.radiusnetworks.museumguide;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;

import com.radiusnetworks.museumguide.assets.RemoteAssetCache;

/**
 * Created by dyoung on 3/10/14.
 */
public class ScreenFragment extends Fragment {
    public static final String TAG = "ScreenFragment";
    public static final String ARG_OBJECT = "object";
    View rootView;
    String itemId = null;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "fragment onCreateView");
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        rootView = inflater.inflate(
                R.layout.museum_item, container, false);
        Bundle args = getArguments();
        showItem(itemId);
        return rootView;
    }

    public void showItem(String currentItemId) {
        itemId = currentItemId;
        if (rootView != null) {
            Log.d(TAG, "Setting up fragment with "+"item"+currentItemId);
            WebView webview = (WebView) rootView.findViewById(R.id.webView);
            RemoteAssetCache assetCache = new RemoteAssetCache(this.getActivity());
            String html = assetCache.getHtmlContentByName("item"+currentItemId+"_html");
            ImageView imageView2 = assetCache.getImageByName("item"+currentItemId);
            if (imageView2 != null) {
                ImageView imageView = (ImageView) rootView.findViewById(R.id.museumItemImage);
                imageView.setImageDrawable(imageView2.getDrawable());
            }
            if (html != null) {
                webview.loadData(html, "text/html", null);
            }
            rootView.findViewById(R.id.nextButton).setVisibility(View.INVISIBLE);
        }
    }


}
