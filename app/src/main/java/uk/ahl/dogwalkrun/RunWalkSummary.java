package uk.ahl.dogwalkrun;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Administrator on 21-Feb-16.
 */
public class RunWalkSummary implements Serializable {

    private static RunWalkSummary ourInstance = new RunWalkSummary();
    private Calendar rwDateTime;
    private double runDistance;
    private double totalDistance;
    private long runTime;
    private long totalTime;


    public static RunWalkSummary getInstance() {
        return ourInstance;
    }

    private RunWalkSummary() {
    }

    public void setInitial(Calendar rwDateTime) {
        this.rwDateTime = rwDateTime;
        this.runDistance = 0;
        this.totalDistance = 0;
        this.runTime = 0;
        this.totalTime = 3;
    }

    public Calendar getRwDateTime() {
        return rwDateTime;
    }

    public String getRwDateTimeStr() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date datetime = rwDateTime.getTime();
        return df.format(datetime);
    }

    public double getRunDistance() { return runDistance; }

    public String getRunDistanceStr() {
        return decString(runDistance);
    }

    public void setRunDistance(double runDistance) {
        this.runDistance = runDistance;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public String getTotalDistanceStr() {
        return decString(totalDistance);
    }

    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }

    public long getRunTime() {
        return runTime;
    }

    public String getRunTimeStr() {
        return formatTime(runTime);
    }

    public void setRunTime(long runTime) {
        this.runTime = runTime;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public String getTotalTimeStr() {
        return formatTime(totalTime);
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    public void incrementTime(long millisec){
        this.totalTime += millisec;
    }

    public void incrementTotals(double distance, long interval, boolean run){
        this.totalDistance += distance;
   //     this.totalTime += interval;  // do this separately
        if (run) {
            this.runDistance += distance;
            this.runTime += interval;
        }
    }

    public String toJSON(){

        JSONObject jsonObject= new JSONObject();
        try {
            jsonObject.put("rwdatetime", getRwDateTimeStr());
            jsonObject.put("totdist", getTotalDistance());
            jsonObject.put("rundist", getRunDistance());
            jsonObject.put("tottime", getTotalTime());
            jsonObject.put("runtime", getRunTime());
            jsonObject.put("notes", "" );

            return jsonObject.toString();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "";
        }

    }

    public String formatTime(long millis) {

        long second = (millis / 1000) % 60;
        long minute = (millis / (1000 * 60)) % 60;
        long hour = (millis / (1000 * 60 * 60)) % 24;
        String time = String.format("%02d:%02d:%02d", hour, minute, second);
        return time;
    }

    public String decString(double dist) {
        String decString = new DecimalFormat("##.##").format(dist/1000);
        return decString;
    }
}
