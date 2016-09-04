package uk.ahl.dogwalkrun;

/**
 * Created by Administrator on 12-Apr-16.
 */
public class LogEntry {

    private String logDateTime;
    private double latitude;
    private double longitude;
    private String pointTime;
    private double pointDist;

    public LogEntry(String datetime, double lat, double lon, String pointtime, double pointdist ) {
        this.logDateTime = datetime;
        this.latitude = lat;
        this.longitude = lon;
        this.pointTime = pointtime;
        this.pointDist = pointdist;
    }

    public String getLogDateTime() {
        return logDateTime;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getPointTime() {
        return pointTime;
    }

    public double getPointDist() {
        return pointDist;
    }
}
