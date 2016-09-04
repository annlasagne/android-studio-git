package uk.ahl.dogwalkrun;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 10-Feb-16.
 */
public class GPSDatabase {
    private Context context;
    private DbHelper dbHelper;
    public final String DBNAME="gps1";
    public final int DBVERSION=3;
    public SQLiteDatabase db;
    public final String POINTID="pointId";
    public final String LATITUDE="latitude";
    public final String LONGITUDE="longitude";
    public final String LOGDATETIME="logdatetime";
    public final String POINTTIME="pointtime";
    public final String POINTDIST="pointdist";
    public final String TABLENAMELOG="locpoint";
    public final String RWDATETIME="rwdatetime";
    public final String TOTALDIST="totaldist";
    public final String RUNDIST="rundist";
    public final String TOTALTIME="totaltime";
    public final String RUNTIME="runtime";
    public final String NOTES="notes";
    public final String TABLENAMESUM="rwsummary";
    public final String CREATERDBLOG="create table locpoint" +
            "(pointId integer primary key autoincrement,latitude real, " +
            "longitude real, logdatetime text not null, pointtime text, " +
            "pointdist real);";
    public final String CREATERDBSUM="create table rwsummary" +
            "(rwdatetime text not null primary key, totaldist text, " +
            "rundist text, totaltime text, runtime text, notes text);";
    //const
    public GPSDatabase(Context context){
        Log.d("GDB", "DB constructor()");
        this.context = context;
        dbHelper = new DbHelper(context);

    }
    public class DbHelper extends SQLiteOpenHelper {

        public DbHelper(Context context){
            super(context,DBNAME,null,DBVERSION);
            Log.d("GDB", "DBhelper constructor()");
        }
        @Override
        public void onCreate(SQLiteDatabase db) {
            // TODO Auto-generated method stub
            Log.d("GDB", "create db");
            db.execSQL(CREATERDBLOG);
            db.execSQL(CREATERDBSUM);
        }
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO Auto-generated method stub
        }
    }

    private class StartupTask extends AsyncTask
    {
        @Override
        protected Object doInBackground(final Object... objects)
        {
            Log.d("GDB", "async task to open db");
            try
            {
               dbHelper.getWritableDatabase();
            }
            catch (SQLException e)
            {
                Log.d("GDB", "SQL EXception on open DB");
                e.printStackTrace();
            }
            catch (Exception e)
            {
                Log.d("GDB", "General EXception on open");
                e.printStackTrace();
            }
            Log.d("GDB", "No Exception on open??");
            return null;
        }
    }

    public long insertRows(String logdt, double lat, double lon, String ptime, double pdist ){
        Log.d("GDB", "Insert rows...");
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues value = new ContentValues();

        value.put(LOGDATETIME, logdt);
        value.put(LATITUDE, lat);
        value.put(LONGITUDE, lon);
        value.put(POINTTIME, ptime);
        value.put(POINTDIST, pdist);

        return db.insert(TABLENAMELOG,null,value);
    }

    public long addSummary(String rwdt, String totdist, String rundist, String tottime,
                                                                String runtime, String notes){
        Log.d("GDB", "Add Summary...");
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues value = new ContentValues();

        value.put(RWDATETIME, rwdt);
        value.put(TOTALDIST, totdist);
        value.put(RUNDIST, rundist);
        value.put(TOTALTIME, tottime);
        value.put(RUNTIME, runtime);
        value.put(NOTES, notes);

        return db.insert(TABLENAMESUM,null,value);
    }

    public int numberOfEntries(String logdatetime){
        Log.d("GDB", "num entries...");
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // uname='" + loginname + "' and pwd='" + loginpass +"'",
        String datetimestr = "'" + logdatetime + "'";
        String numQuery = "LOGDATETIME = " + datetimestr;
        Log.d("GDB", "string Query = " + numQuery);
        int numRows = (int) DatabaseUtils.queryNumEntries(db, TABLENAMELOG, numQuery);
        return numRows;
    }

    public List<Map<String, String>> getLogEntries(String logdatetime){
        Log.d("GDB", "get all log entries...");
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String datetimestr = "'" + logdatetime + "'";
        String q = "SELECT * FROM " +  TABLENAMELOG + " WHERE LOGDATETIME = " + datetimestr + " ORDER BY pointId";
        Log.d("GDB", "string Query = " + q);
        Cursor cursor = db.rawQuery(q,null);
        List<Map<String, String>> loglist = new ArrayList<>();
        int entrycount = 0;
        if (cursor.moveToFirst()) {
            do {
                HashMap mMap = new HashMap();
                mMap.put("pointid",String.valueOf(cursor.getInt(cursor.getColumnIndex("pointId"))));
                mMap.put("logdatetime",cursor.getString(cursor.getColumnIndex("logdatetime")));
                String pointStr = String.format("%.4f", cursor.getDouble(cursor.getColumnIndex("latitude")));
                mMap.put("latitude",pointStr);
                pointStr = String.format("%.4f", cursor.getDouble(cursor.getColumnIndex("longitude")));
                mMap.put("longitude",pointStr);
                mMap.put("pointtime",cursor.getString(cursor.getColumnIndex("pointtime")));
                String distStr = String.format("%.2f", cursor.getDouble(cursor.getColumnIndex("pointdist")));
                mMap.put("pointdist", distStr);

                loglist.add(mMap);
                Log.d("GDB", "point Id processed: " + cursor.getInt(cursor.getColumnIndex("pointId")));
                entrycount++;
            } while (cursor.moveToNext());
        }
        cursor.close();
        return loglist;
    }

    public HashMap<String, String> getSummary(String datetime){
        Log.d("GDB", "getSummary..");
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        String datetimestr = "'" + datetime + "'";
        String sql = "SELECT rwdatetime, totaldist, rundist, totaltime, runtime, notes FROM rwsummary" +
            " WHERE rwdatetime = " + datetimestr;

        cursor = db.rawQuery(sql, null);
            if(cursor.moveToFirst()) {
                    HashMap<String, String> colvalue = new HashMap<>();
                    for (int i = 0; i < cursor.getColumnCount(); i++) {
                        colvalue.put(cursor.getColumnName(i), cursor.getString(i));
                    }
                    return colvalue;
            } else {
                return null;
            }
        }

//    public Cursor getAllRows(){
//        Cursor cursor = db.query(TABLENAMELOG, new String[]{LOGDATETIME, LATITUDE}, null,null, null, null, null);
//        return cursor;
//    }

    public void open() throws SQLException {

        dbHelper.getWritableDatabase();
     //   new StartupTask().execute();
    }

    public void close(){
        dbHelper.close();
        //return true;
    }
}
