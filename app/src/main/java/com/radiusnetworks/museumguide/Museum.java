package com.radiusnetworks.museumguide;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by dyoung on 2/28/14.
 */
public class Museum {
    private List<MuseumItem> museumItems;
    private Context context;

    public Museum(Context context,List<MuseumItem> museumItems) {
        this.museumItems = museumItems;
        this.context = context;
    }

    public static Museum loadFromPreferences(Context c) {
        ArrayList<MuseumItem> list = new ArrayList<MuseumItem>();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        String museumIds = settings.getString("mg_item_ids", null);
        if (museumIds != null) {
            for (String museumId : museumIds.split(",")) {
                list.add(new MuseumItem(museumId));
            }
        }
        Museum museum = new Museum(c, list);
        return museum;
    }
    public void saveToPreferences(Context c) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("mg_museum_ids", getIds());
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
    public MuseumItem getItemById(String itemId) {
        for (MuseumItem item : museumItems) {
            if (item.getId().equals(itemId)) {
                return item;
            }
        }
        return null;
    }

    public List<MuseumItem> getItemList() {
        return museumItems;
    }
}
