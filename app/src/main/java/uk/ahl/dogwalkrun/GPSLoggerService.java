package uk.ahl.dogwalkrun;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.Display;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class GPSLoggerService extends Service {

    public static final int VISIBILITY_PUBLIC = 1;
    IBinder mIBinder = new LocalBinder();
    Context context;
    private Handler handler = new Handler();
    Location lastLocation = null;
    long INTERVAL = 3000;
    LocationManager locManager;
    LocationListener mloclistener;
    GPSDatabase gpsDb;
    RunWalkSummary runWalkSum;
    NotificationManager mNotifyMgr;
    int mNotificationId;
    NotificationCompat.Builder mBuilder;
    boolean notifyActive = false;
    int updateCounter;
    SimpleDateFormat tf = new SimpleDateFormat("HH:mm:ss");

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("GLS", "onCreate()");
        context = getBaseContext();
        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mloclistener = new locListener();
        if (gpsDb == null) {
            gpsDb = new GPSDatabase(context);
            Log.d("GLS", "DB created?" + gpsDb);
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        Log.d("GLS", "OnBind");

        return mIBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("GLS", "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    public class LocalBinder extends Binder {
        public GPSLoggerService getGPSLoggerServiceInstance() {
            Log.d("GLS", "Onstart");
            return GPSLoggerService.this;
        }
    }

    public class locListener implements LocationListener {

        double lat;
        double lon;

        public void onLocationChanged(Location location) {
            Log.d("GLS", "onLocationChanged()");
            double pointDistance;
            lat = location.getLatitude();
            lon = location.getLongitude();
            updateCounter++;

            if (lastLocation != null) {
                pointDistance = lastLocation.distanceTo(location);
            } else {
                pointDistance = 0;
            }


            //update summary values
            double pace = pointDistance/INTERVAL;
            boolean running = false;
            if (pace > 1.75) { //assumes distance is in km
                running = true;
            }

            runWalkSum.incrementTotals(pointDistance, INTERVAL, running);
            //if screen is locked show updates on lock screen
            if(notifyActive && updateCounter >= 5) {
                updateLockNotify();
                updateCounter = 0;
            }
           updateDatabase(pointDistance);
            lastLocation = location;
        }

        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
        }

        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub
        }

        public void updateDatabase(double pdist) {

            Log.d("GLS", "updateDatabase()");
            String logdatetime = runWalkSum.getRwDateTimeStr();
         //   Date tnow = Calendar.getInstance().getTime();
            String pointTime = tf.format(Calendar.getInstance().getTime());
            gpsDb.open();
            gpsDb.insertRows(logdatetime, lat, lon, pointTime, pdist);
            gpsDb.close();

        }
    }

    public RunWalkSummary startLogging() {
        Log.d("GLS", "startLogging()");
        gpsDb.open();
        Log.d("GLS", "opened DB, id = " + gpsDb);

        //initialise summary data object
        Calendar dtstart = Calendar.getInstance();
        runWalkSum = RunWalkSummary.getInstance();
        runWalkSum.setInitial(dtstart);

        //write initial database record
        String logdatetime = runWalkSum.getRwDateTimeStr();
        Location startloc = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        String startTime = tf.format(Calendar.getInstance().getTime());
        gpsDb.open();
        gpsDb.insertRows(logdatetime, startloc.getLatitude(), startloc.getLongitude(), startTime, 0);
        gpsDb.close();

        // increment total time every INTERVAL millisecs whether location changed or not
        runWalkSum.incrementTime(INTERVAL);
        handler.postDelayed(runnable, INTERVAL);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null;
        }
        //start listening to GPS Service
        Log.d("GLS", "requesting location updates.......");
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, INTERVAL, 0, mloclistener);

        return runWalkSum;

    }

    public RunWalkSummary stopLogging() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null;

        handler.removeCallbacks(runnable);  //stop total time increment task
        locManager.removeUpdates(mloclistener);

        return runWalkSum;

    }

    public RunWalkSummary getUpdate() {

        return runWalkSum;
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {

            /// the thing you want to do every n seconds:
            runWalkSum.incrementTime(INTERVAL);
            //set update to run again in 2 secs
            handler.postDelayed(this, INTERVAL);
        }
    };

    @TargetApi(value=21)
    public void checkDisplayLock(){
        Log.d("GLS", "check Display Lock....");
        DisplayManager dm = (DisplayManager) this.getSystemService(Context.DISPLAY_SERVICE); //this = context
        boolean screenoff = false;
        for (Display display : dm.getDisplays()) {
            if (display.getState() != Display.STATE_ON) {
                Log.d("GLA", "found an off screen");
                screenoff = true;
            } else {
                Log.d("GLA", "found an on screen");
            }
        }
        if (screenoff) {
            Log.d("GLS", "issuing a notification");
            // Builds the notification and issues it.
            //set up Notification for when screen locks
//            mBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
//                    .setSmallIcon(R.drawable.notification)
//                    .setContentTitle("DogWalkRun")
//                    .setContentText("Initialising")
//                    .setVisibility(VISIBILITY_PUBLIC);
//            // Sets an ID for the notification
//            mNotificationId = 001;
//            // Gets an instance of the NotificationManager service
//            mNotifyMgr =
//                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//            mBuilder.setContentText("startnotify");
//            mNotifyMgr.notify(mNotificationId, mBuilder.build());
            notifyActive = true;
        }
    }

    @TargetApi(value=21)
    public void updateLockNotify() {
        Log.d("GLS", "updateLockNotify()");
//        String notifyText = "Run dist/time: " + (runWalkSum.getRunDistance()/1000) + "/" +
//                (formatTime(runWalkSum.getRunTime()));
//        mBuilder.setContentText(notifyText);
//        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }

    @TargetApi(value=21)
    public void cancelLockNotify() {
        Log.d("GLS", "cancelLockNotify()");
//        if (notifyActive = true) {
//            mNotifyMgr.cancel(mNotificationId);
            notifyActive = false;
//        }
    }

    public String formatTime(long millis) {

        long second = (millis / 1000) % 60;
        long minute = (millis / (1000 * 60)) % 60;
        long hour = (millis / (1000 * 60 * 60)) % 24;
        String time = String.format("%02d:%02d:%02d", hour, minute, second);
        return time;
    }

}

