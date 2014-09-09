        package com.radiusnetworks.museumguide;

        import android.app.Application;
        import android.util.Log;

        import com.radiusnetworks.ibeacon.IBeacon;
        import com.radiusnetworks.ibeacon.IBeaconData;
        import com.radiusnetworks.ibeacon.Region;
        import com.radiusnetworks.ibeacon.client.DataProviderException;
        import com.radiusnetworks.proximity.ProximityKitManager;
        import com.radiusnetworks.proximity.ProximityKitNotifier;
        import com.radiusnetworks.proximity.model.KitIBeacon;

        /**
         * Created by dyoung on 5/1/14.
         */
        public class MyApplication extends Application implements ProximityKitNotifier {

            private ProximityKitManager manager;
            private static String TAG="MyApplication";

            @Override
            public void onCreate() {
                super.onCreate();
                manager = ProximityKitManager.getInstanceForApplication(this);
            }

            @Override
            public void didSync() {
                // Called when ProximityKit data are updated from the server

                // The loop below will access every beacon configured in the kit, and print out the value
                // of an attribute named "myKey"
                for (KitIBeacon iBeacon : manager.getKit().getIBeacons()) {
                    Log.d(TAG, "For iBeacon: "+iBeacon.getProximityUuid()+" "+iBeacon.getMajor()+" "+iBeacon.getMinor()+
                               ", the value of myKey is "+iBeacon.getAttributes().get("myKey"));
                }
            }

            @Override
            public void didFailSync(Exception e) {

            }

            @Override
            public void iBeaconDataUpdate(IBeacon iBeacon, IBeaconData iBeaconData, DataProviderException e) {

            }

            @Override
            public void didEnterRegion(Region region) {

            }

            @Override
            public void didExitRegion(Region region) {

            }

            @Override
            public void didDetermineStateForRegion(int i, Region region) {

            }
        }