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
public class IntroFragment extends Fragment {
    public static final String TAG = "IntroFragment";
    public static final String ARG_OBJECT = "object";
    View rootView;
    int item;

    public IntroFragment(int page) {
        item = page+1;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "intro fragment onCreateView");
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        rootView = inflater.inflate(
                R.layout.intro_screen, container, false);
        Log.d(TAG, "I just tried to inflate "+R.layout.intro_screen+" and got "+rootView);

        Bundle args = getArguments();
        Log.d(TAG, "Setting up intro fragment with "+item);
        WebView webview = (WebView) rootView.findViewById(R.id.webView);
        //webview.loadData("intro page "+pageNumber, "text/html", null);
        // TODO: load an actual help page
        webview.loadUrl("file:///android_asset/intro" + item + ".html");
        Log.d(TAG, "Loaded intro page URL: "+webview.getUrl());

        return rootView;
    }

}
