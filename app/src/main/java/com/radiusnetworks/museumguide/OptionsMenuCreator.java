/**
 * Created by dyoung on 3/5/14.
 */

package com.radiusnetworks.museumguide;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;

/**
 * A class to create a common options menu in various activities
 * Created by dyoung on 3/25/14.
 */
public class OptionsMenuCreator {
    private MuseumGuideApplication application;
    private Activity activity;
    public OptionsMenuCreator(Activity activity, MuseumGuideApplication application) {
        this.application = application;
        this.activity = activity;
    }
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem settings = menu.add("Exit museum");
        settings.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        settings.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                application.startOver(activity);
                return true;
            }
        });
        MenuItem help = menu.add("Help");
        help.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        help.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://developer.radiusnetworks.com/museum_guide/help.html"));
                application.startActivity(browserIntent);
                return true;
            }
        });
        return true;
    }

}
