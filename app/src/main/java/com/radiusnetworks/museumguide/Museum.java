package com.radiusnetworks.museumguide;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by dyoung on 2/28/14.
 */
public class Museum {
    private static String TAG = "Museum";
    private List<MuseumItem> museumItems;
    private Context context;

    public Museum(Context context,List<MuseumItem> museumItems) {
        this.museumItems = museumItems;
        this.context = context;

        Comparator<MuseumItem> comparator = new Comparator<MuseumItem>() {
            public int compare(MuseumItem c1, MuseumItem c2) {
                return c1.getId().compareTo(c2.getId());
            }
        };

        Collections.sort(museumItems, comparator);

    }

    public static Museum loadFromPreferences(Context c) {
        ArrayList<MuseumItem> list = new ArrayList<MuseumItem>();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        String museumIds = settings.getString("mg_museum_ids", null);
        String museumTitles = settings.getString("mg_museum_titles", null);
        Log.d(TAG, "loaded titles as "+museumTitles);
        if (museumIds != null) {
            String[] museumIdArray = museumIds.split(",");
            String[] museumTitleArray = {};
            if (museumTitles != null) {
                museumTitleArray = museumTitles.split(",");
            }
            for (int i = 0; i < museumIdArray.length; i++) {
                String title = null;
                if (museumTitleArray.length > i) {
                    title = museumTitleArray[i];
                }
                list.add(new MuseumItem(museumIdArray[i], title));
            }
        }
        Museum museum = new Museum(c, list);
        return museum;
    }
    public void saveToPreferences(Context c) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("mg_museum_ids", getIds());
        editor.putString("mg_museum_titles", getTitles());
        Log.d(TAG, "Saving titles as"+getTitles());
        editor.commit();
    }
    private String getIds() {
        StringBuilder sb = new StringBuilder();
        for (MuseumItem item : museumItems) {
           sb.append(item.getId());
            sb.append(",");
        }
        return sb.toString();
    }
    private String getTitles() {
        StringBuilder sb = new StringBuilder();
        for (MuseumItem item : museumItems) {
            sb.append(item.getTitle().replace(",",""));
            sb.append(",");
        }
        return sb.toString();
    }

    public MuseumItem getItemById(String itemId) {
        for (MuseumItem item : museumItems) {
            if (item.getId().equals(itemId)) {
                return item;
            }
        }
        return null;
    }
    public int getItemIndexById(String itemId) {
        int i = 0;
        for (MuseumItem item : museumItems) {
            if (item.getId().equals(itemId)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public List<MuseumItem> getItemList() {
        return museumItems;
    }
}
