package uk.ahl.dogwalkrun;

import uk.ahl.dogwalkrun.GPSLoggerService.LocalBinder;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.app.Activity;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Calendar;

public class MainActivity extends Activity {

    public final static String EXTRA_RWSUMMARY = "uk.ahl.dogwalkrun.RWSUMMARY";
    GPSLoggerService mGPSLoggerService;
    private Handler handler = new Handler();
    TextView runDistText;
    TextView runTimeText;
    TextView totDistText;
    TextView totTimeText;
    boolean isLogging = false;
    RunWalkSummary runWalkSum;
    long starttime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("GLA", "oncreate");
        setContentView(R.layout.activity_main);
        runDistText = (TextView) findViewById(R.id.runDist);
        runTimeText = (TextView) findViewById(R.id.runTime);
        totDistText = (TextView) findViewById(R.id.totalDist);
        totTimeText = (TextView) findViewById(R.id.totalTime);

     //   final Intent mIntent = new Intent(this,GPSLoggerService.class);

        final Button startButton = (Button)findViewById(R.id.buttonStart);
        final Button stopButton = (Button)findViewById(R.id.buttonStop);

        startButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Log.d("GLA", "" + "start logging" + mGPSLoggerService);
                runWalkSum = mGPSLoggerService.startLogging();
                isLogging = true;
                startButton.setVisibility(View.INVISIBLE);
                stopButton.setVisibility(View.VISIBLE);
                //TODO display start time using formattime
                updateValues();
                handler.postDelayed(runnable, 5000);

            }
        });

        stopButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {

                Log.d("GLA", "" + mGPSLoggerService);
                runWalkSum = mGPSLoggerService.stopLogging();
                mGPSLoggerService.cancelLockNotify();
                stopButton.setVisibility(View.INVISIBLE);
                startButton.setVisibility(View.VISIBLE);
                isLogging = false;
                runDistText.setText("0.00");
                runTimeText.setText("00:00");
                totDistText.setText("0.00");
                totTimeText.setText("00:00");
                handler.removeCallbacks(runnable);
                showSummary();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("GLA", "Onstart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("GLA", "onResume");
        if (!mBounded) {
            Log.d("GLA", "onResume - not bound so rebind");
            Intent mIntent = new Intent(getApplicationContext(),GPSLoggerService.class);
            bindService(mIntent, mServiceConnection, BIND_AUTO_CREATE);
        }
        if (isLogging) {
            updateValues();
            handler.postDelayed(runnable, 5000);
            mGPSLoggerService.cancelLockNotify();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("GLA", "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("GLA", "onstop");
        if(!isLogging && mBounded) {
            Log.d("GLA", "onstop - not logging so unbind");
            unbindService(mServiceConnection);
            mBounded = false;
        }
        handler.removeCallbacks(runnable);
        mGPSLoggerService.checkDisplayLock();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("GLA", "ondestroy");
        handler.removeCallbacks(runnable);
        mGPSLoggerService.cancelLockNotify();
        if(mBounded) {
            unbindService(mServiceConnection);
            mBounded = false;
        }
    }


    boolean mBounded;
    ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // TODO Auto-generated method stub
            Log.d("GLA","onServicedisConnected");
            Toast.makeText(MainActivity.this, "Service disconnected",Toast.LENGTH_SHORT).show();
            mBounded = false;
            mGPSLoggerService=null;
        }

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            // TODO Auto-generated method stub
            Log.d("GLA", "onServiceConnected");
            Toast.makeText(MainActivity.this, "Service connected",Toast.LENGTH_SHORT).show();
            mBounded = true;
            LocalBinder mLocalBinder = (LocalBinder)arg1;
            mGPSLoggerService = mLocalBinder.getGPSLoggerServiceInstance();
        }
    };

    public void showSummary() {

        Intent intent = new Intent(this, ReviewUploadActivity.class); //this = mainactivity context
        intent.putExtra(EXTRA_RWSUMMARY, runWalkSum);
        startActivity(intent);
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {

            updateValues();
            //set update to run again in 5 secs
            handler.postDelayed(this, 5000);
        }
    };

    public void updateValues(){

        runWalkSum = mGPSLoggerService.getUpdate();

            runDistText.setText(String.format("%.2f", runWalkSum.getRunDistance()));
            totDistText.setText(String.format("%.2f", runWalkSum.getTotalDistance()));
            runTimeText.setText(formatTime(runWalkSum.getRunTime()));
            totTimeText.setText(formatTime(runWalkSum.getTotalTime()));
    }

    public String formatTime(long millis) {

        long second = (millis / 1000) % 60;
        long minute = (millis / (1000 * 60)) % 60;
        long hour = (millis / (1000 * 60 * 60)) % 24;
        String time = String.format("%02d:%02d:%02d", hour, minute, second);
        return time;
    }

}

