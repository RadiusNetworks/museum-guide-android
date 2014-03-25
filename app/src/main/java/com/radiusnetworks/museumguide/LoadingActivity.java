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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.radiusnetworks.ibeacon.BleNotAvailableException;
import com.radiusnetworks.ibeacon.IBeaconManager;
import com.radiusnetworks.proximity.licensing.LicensingException;

import java.io.FileNotFoundException;

/**
 * This Activity manages the state of the app before the museum data is fully loaded.  It is the
 * entry point for the UI of the app and is launched on startup.  Its basic logic is:
 *
 * 1. Checks to see if the the museum has been entered already (e.g. phone rebooted and relaunched).
 *    If so, it switches to the MuseumItemsActivity
 * 2. Shows a simple title display to the user with a Start button.
 * 3. When the start button is tapped, it checks if the user has already gone through the intro.
 *    If not, it launches the IntroActivity.
 * 4. If the IntroActivity does not need to be shown, it shows the modal dialog for the user to
 *    to enter the museum code.
 * 5. After the user enters the code, it shows a modal dialog with a spinner while it waits to
 *    load data from ProximityKit.
 *
 * Created by dyoung on 1/28/14.
 */
public class LoadingActivity extends Activity {
    public static final String TAG = "LoadingActivity";
    private boolean validatingCode = false;
    private MuseumGuideApplication application;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        application = (MuseumGuideApplication) this.getApplication();
        application.setLoadingActivity(this);

        if (application.getMuseum() != null) {
            Log.d(TAG, "museum is ongoing");
            // user exited after starting a musuem.  resume where he or she left off
            Intent i = new Intent(this, MuseumItemsActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            finish();
            return;
        }


        setContentView(R.layout.sh_activity_code);
        this.findViewById(R.id.code_dialog).setVisibility(View.INVISIBLE);
        this.findViewById(R.id.validating_dialog).setVisibility(View.INVISIBLE);
        View startButton = (TextView) this.findViewById(R.id.sh_start_button);

        if (!application.hasDisplayedIntro() && !application.getDontShowIntroAgain()) {
            startButton.setOnClickListener(introStartOnClickListener);
        }
        else if (application.isCodeNeeded()) {
            startButton.setOnClickListener(codeStartOnClickListener);
        }
        else {
            setupLoadingView();
            startButton.setOnClickListener(loadingStartOnClickListener);
        }

        checkPrerequisites();  // complain to user if bluetooth is unavailable or turned off
    }


    View.OnClickListener introStartOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // need to display intro
            Intent i = new Intent(LoadingActivity.this, IntroActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            finish();
            return;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return new OptionsMenuCreator(this, application).onCreateOptionsMenu(menu);
    }


    View.OnClickListener loadingStartOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            setupLoadingView();
            application.startPk(null);
        }
    };

    View.OnClickListener codeStartOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            setContentView(R.layout.sh_activity_code);
            LoadingActivity.this.findViewById(R.id.code_dialog).setVisibility(View.VISIBLE);
            LoadingActivity.this.findViewById(R.id.validating_dialog).setVisibility(View.INVISIBLE);
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(LoadingActivity.this);
            ((EditText) LoadingActivity.this.findViewById(R.id.code)).setText(settings.getString("code", ""));
            TextView helpView = (TextView) LoadingActivity.this.findViewById(R.id.help);
            helpView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "help tapped");
                    Intent i = new Intent(getApplicationContext(), HelpActivity.class);
                    startActivity(i);
                }
            });

        }

    };

    /**
     * Force screen in portrait
     */
    @Override
    protected void onResume() {
        super.onResume();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    /**
     * User taps OK after entering a code.  Starts trying to sync with PK using this code
     * @param v
     */
    public void onCodeOkClicked(View v) {
        this.findViewById(R.id.code).setEnabled(false);
        this.findViewById(R.id.code_dialog).setVisibility(View.INVISIBLE);
        this.findViewById(R.id.validating_dialog).setVisibility(View.VISIBLE);

        EditText codeEditText = (EditText) this.findViewById(R.id.code);
        String code = codeEditText.getText().toString();

        this.validatingCode = true;
        // The method below will change the credentials for pk, which will then try to sync
        // sync pass or fail will cause a callback to ScavengerHuntApplication,
        // which will call the codeValidationPassed or codeValidationFailed methods below
        Log.d(TAG, "restarting proximity kit with code" + code);
        application.startPk(code);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Failed to load Museum data from ProximityKit
     * @param title
     * @param message
     */
    public void failAndTryAgain(final String title, final String message) {
        this.validatingCode = false;
        runOnUiThread(new Runnable() {
            public void run() {
                setContentView(R.layout.sh_activity_code);
                LoadingActivity.this.findViewById(R.id.code_dialog).setVisibility(View.INVISIBLE);
                LoadingActivity.this.findViewById(R.id.validating_dialog).setVisibility(View.INVISIBLE);
                final AlertDialog.Builder builder = new AlertDialog.Builder(LoadingActivity.this);
                builder.setTitle(title);
                builder.setMessage(message);
                builder.setPositiveButton(android.R.string.ok, null);
                builder.show();
            }
        });
    }

    public boolean isValidatingCode() {
        Log.d(TAG, "validatingCode is "+validatingCode);
        return validatingCode;
    }

    /**
     * Shows the loading modal dialog
     */
    public void setupLoadingView() {
        setContentView(R.layout.sh_activity_loading);
        Log.d(TAG, "setting loading activity");
    }

    /**
     * Callback when application determines the code is valid.
     * Will set up the loading dialog.
     */
    public void codeValidationPassed() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.putString("code", ((EditText) LoadingActivity.this.findViewById(R.id.code)).getText().toString());
        editor.commit();
        this.validatingCode = false;
        runOnUiThread(new Runnable() {
            public void run() {
                LoadingActivity.this.findViewById(R.id.code).setEnabled(true);
                setupLoadingView();
            }
        });
    }

    /**
     * Callback when application determines the code is wrong
     * Will set up the code dialog so the user can try again
     */
    public void codeValidationFailed(final Exception e) {
        this.validatingCode = false;
        runOnUiThread(new Runnable() {
            public void run() {
                LoadingActivity.this.findViewById(R.id.code_dialog).setVisibility(View.VISIBLE);
                LoadingActivity.this.findViewById(R.id.validating_dialog).setVisibility(View.INVISIBLE);
                LoadingActivity.this.findViewById(R.id.code).setEnabled(true);
                final AlertDialog.Builder builder = new AlertDialog.Builder(LoadingActivity.this);
                if (e.getClass() == LicensingException.class || e.getClass() == FileNotFoundException.class) {
                    builder.setTitle("Invalid code");
                    builder.setMessage("Please check that your museum code is valid and try again.");
                }
                else {
                    builder.setTitle("Network error");
                    builder.setMessage("Please check your internet connection and try again.");
                    Log.d(TAG, "code validation error: "+e);
                }
                builder.setPositiveButton(android.R.string.ok, null);
                builder.show();
            }
        });

    }

    /**
     * Checks if bluetooth is present and enabled and if wifi is on, which may conflict on some
     * devices
     * @return
     */
    private boolean checkPrerequisites() {
        IBeaconManager iBeaconManager = IBeaconManager.getInstanceForApplication(this);

        try {
            if (!iBeaconManager.checkAvailability()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Bluetooth not enabled");
                builder.setMessage("The museum guide requires that Bluetooth be turned on.  Please enable bluetooth in settings.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                    @Override
                    public void onDismiss(DialogInterface dialog) {
						/*finish();*/
                    }

                });
                builder.show();
                return false;
            }
        }
        catch (BleNotAvailableException e) {
            return false;
        }

        try {
            WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
            String message = null;

            if (wifi.isWifiEnabled()) {
                if (Build.MODEL.equals("Nexus 4") || Build.MODEL.equals("Nexus 7")) {
                    message = "There is a known issue with the Nexus 4 and Nexus 7 devices where WiFi and Bluetooth can disrupt each other.  We recommend disabling WiFi while using the Museum Guide.";
                }
                // Motorola Moto G (XT1028, XT1031, XT1032, XT1033, XT1034)
                // Motorola Moto X (XT1049, XT105x, XT1060)
                else if (Build.MODEL.startsWith("XT102") || Build.MODEL.startsWith("XT103") ||
                         Build.MODEL.startsWith("XT104") || Build.MODEL.startsWith("XT105") ||
                         Build.MODEL.startsWith("XT106")) {
                    message = "There is a known issue with the Moto G and Moto X devices where WiFi and Bluetooth can disrupt each other.  We recommend disabling WiFi while using the Museum Guide.";
                }
            }

            if (message != null) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Please Note");
                builder.setMessage(message);
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                    @Override
                    public void onDismiss(DialogInterface dialog) {
                    }

                });
                builder.show();
                return false;

            }
        }
        catch (Exception e) {
            Log.e(TAG, "Can't access wifi manager due to exception", e);
        }
        return true;
    }


}
