package uk.ahl.dogwalkrun;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.msebera.android.httpclient.Header;


public class ReviewUploadActivity extends AppCompatActivity {
    String sumUrl = "http://192.168.1.108:8080/syncsum";
    String logUrl = "http://192.168.1.108:8080/synclogs";
    Context context;
    GPSDatabase gpsDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("RUA", "oncreate");
        context = getBaseContext();
        if (gpsDb == null) {
            gpsDb = new GPSDatabase(context);
        }
        Log.d("RUA", "oncreate get DB instance = " + gpsDb);
        setContentView(R.layout.activity_review_upload);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        final RunWalkSummary runWalkSum = (RunWalkSummary) intent.getSerializableExtra(MainActivity.EXTRA_RWSUMMARY);

        final String logdatetime = runWalkSum.getRwDateTimeStr();

        final Button saveButton = (Button) findViewById(R.id.buttonSave);
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Log.d("RUA", "" + "save pressed, opening db" + gpsDb);
                gpsDb.open();
                long result = gpsDb.addSummary(logdatetime, runWalkSum.getTotalDistanceStr(), runWalkSum.getRunDistanceStr(),
                        runWalkSum.getTotalTimeStr(), runWalkSum.getRunTimeStr(), "");
                long dbEntries = gpsDb.numberOfEntries(logdatetime);
                gpsDb.close();
                if (result > 0) {
                    Toast.makeText(ReviewUploadActivity.this, "Summary saved to local DB", Toast.LENGTH_SHORT).show();
                }
                Log.d("RUA", "Summary Stored = " + result + ", DB num entries = " + dbEntries);
                syncRemoteSum(logdatetime);
                // syncRemoteLog(logdatetime);
                 Toast.makeText(ReviewUploadActivity.this, "Upload request sent", Toast.LENGTH_SHORT).show();
            }
        });

        Date datetime = runWalkSum.getRwDateTime().getTime();
        long starttimemillis = datetime.getTime();
        long timenow = Calendar.getInstance().getTimeInMillis();
        long calcDuration = (timenow - starttimemillis);
        Log.d("RUA", "compare times (calc, log): " + formatTime(calcDuration) + ", " + formatTime(runWalkSum.getTotalTime()));
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yy");
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm:ss");
        TextView logDateText = (TextView) findViewById(R.id.logDate);
        TextView startTimeText = (TextView) findViewById(R.id.startTime);
        TextView runDistText = (TextView) findViewById(R.id.runDist);
        TextView runTimeText = (TextView) findViewById(R.id.runTime);
        TextView totDistText = (TextView) findViewById(R.id.totDist);
        TextView totTimeText = (TextView) findViewById(R.id.totTime);
        logDateText.setText(df.format(datetime));
        startTimeText.setText(tf.format(datetime));
        runDistText.setText(String.format("%.2f", (runWalkSum.getRunDistance() / 1000)));
        totDistText.setText(String.format("%.2f", (runWalkSum.getTotalDistance() / 1000)));
        runTimeText.setText(formatTime(runWalkSum.getRunTime()));
        totTimeText.setText(formatTime(runWalkSum.getTotalTime()));

        // TODO SAVE/DISCARD
    }

    public String formatTime(long millis) {

        long second = (millis / 1000) % 60;
        long minute = (millis / (1000 * 60)) % 60;
        long hour = (millis / (1000 * 60 * 60)) % 24;
        String time = String.format("%02d:%02d:%02d", hour, minute, second);
        return time;
    }

    public void syncRemoteSum(String datetimekey) {
        Log.d("RUA", "syncRemote...");
        // check for active network connection
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            HashMap<String, String> sumdata = gpsDb.getSummary(datetimekey);
            Log.d("RUA", "syncRemote sumdata = " + sumdata);
            if (sumdata != null) {
                AsyncHttpClient client = new AsyncHttpClient();
                RequestParams params = new RequestParams();
                for (Map.Entry<String, String> entry : sumdata.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    params.put(key, value);
                }

                Log.d("RUA", "params data = " + params.toString());
                client.post(sumUrl, params, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject responseBody) {
                        System.out.println("Response Code: " + statusCode);
                        System.out.println(responseBody.toString());
                        Log.d("RUA", "response body = " + statusCode + ": " + responseBody.toString());
                        int count = responseBody.optInt("count");
                        String msg = responseBody.optString("msg");
                        String success = responseBody.optString("success");
                        switch (success) {
                            case "yes":
                                Toast.makeText(getApplicationContext(), ("Success! " + count +
                                                " records inserted"),
                                        Toast.LENGTH_LONG).show();
                                break;
                            case "no":
                                Toast.makeText(getApplicationContext(), ("Error! Message is: " + msg),
                                        Toast.LENGTH_LONG).show();
                                break;
                            default:
                                Toast.makeText(getApplicationContext(), ("Unknown Response: " + responseBody.toString()),
                                        Toast.LENGTH_LONG).show();
                                break;
                        }
                    }

                    public void onFailure(int statusCode, @Nullable Header[] headers, @Nullable String responseBody, Throwable error) {
                        if (statusCode == 404) {
                            Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
                        } else if (statusCode == 500) {
                            Toast.makeText(getApplicationContext(), ("Server Error: " + responseBody), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(), ("Other Error: " + statusCode + responseBody), Toast.LENGTH_LONG).show();
                        }
                        error.printStackTrace(System.out);
                    }
                });
            }
        } else {
            // display error
            Log.d("RUA", "syncRemote - no connection");
            Toast.makeText(ReviewUploadActivity.this, "No network connection for syncing data", Toast.LENGTH_SHORT).show();
        }
    }

    public void syncRemoteLog(String datetimekey) {
        Log.d("RUA", "syncRemoteLog...");
        // check for active network connection
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            Log.d("RUA", "syncRemoteLog - have connections");
            new SyncLogTask().execute(datetimekey);
        } else {
            // display error
            Log.d("RUA", "syncRemoteLog - no connection");
            Toast.makeText(ReviewUploadActivity.this, "No network connection for syncing logs", Toast.LENGTH_SHORT).show();
        }
    }

    private class SyncLogTask extends AsyncTask<String, Void, Long> {

        protected Long doInBackground(String... urls) {

            List<Map<String, String>> logRecords = new ArrayList<>(gpsDb.getLogEntries(urls[0]));
            Log.d("RUA", "syncRemote log set returned:  = " + logRecords);
            AsyncHttpClient client = new AsyncHttpClient();
            RequestParams params = new RequestParams();
            int reccount = 0;
            if (logRecords != null) {
//                HashMap<String, String> logentry = null;
//                for (Map<String, String> entry : logRecords) {
//                    reccount++;
//                    Set<String> keys = entry.keySet();
//                    for (String key : keys) {
//                        String value = entry.get(key);
//                        System.out.println("key = " + key);
//                        System.out.println("value = " + value);
//                        logentry.put(key, value);
//                    }
//                    params.put(String.valueOf(reccount), logentry);
//                }
                params.put("logrecs",logRecords);

                Log.d("RUA", "params data = " + params.toString());
                client.post(logUrl, params, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject responseBody) {
                        System.out.println(responseBody.toString());
                        Log.d("RUA", "response body = " + responseBody.toString());
                        int count = responseBody.optInt("count");
                        String msg = responseBody.optString("msg");
                        String success = responseBody.optString("success");
                        switch (success) {
                            case "yes":
                                Toast.makeText(getApplicationContext(), ("Success! " + count +
                                                " records inserted"),
                                        Toast.LENGTH_LONG).show();
                                break;
                            case "no":
                                Toast.makeText(getApplicationContext(), ("Error! Message is: " + msg),
                                        Toast.LENGTH_LONG).show();
                                break;
                            default:
                                Toast.makeText(getApplicationContext(), ("Unknown Response: " + responseBody.toString()),
                                        Toast.LENGTH_LONG).show();
                                break;
                        }
                    }

                    public void onFailure(int statusCode, @Nullable Header[] headers, @Nullable String responseBody, Throwable error) {
                        if (statusCode == 404) {
                            Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
                        } else if (statusCode == 500) {
                            Toast.makeText(getApplicationContext(), ("Server Error: " + responseBody), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(), ("Other Error: " + responseBody), Toast.LENGTH_LONG).show();
                        }
                        error.printStackTrace(System.out);
                    }
                });
            }
            return null;
        }

        protected void onPostExecute(Long result) {
            Toast.makeText(ReviewUploadActivity.this, "Logs synced to remote DB: " + result, Toast.LENGTH_LONG).show();
        }
    }

    public String toJSON(HashMap datamap) {

        JSONObject jsonObject = new JSONObject(datamap);
        return jsonObject.toString();
    }

}
