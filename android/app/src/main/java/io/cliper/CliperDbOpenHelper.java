package io.cliper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import io.cliper.ChatMessage;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by adv_zxy on 5/23/16.
 */
public class CliperDbOpenHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "cliper.db";
    private static final String CLIPER_TABLE_NAME = "history";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_MSG = "message";
    private static final String KEY_TIME = "time";
    private static final String CLIPER_TABLE_CREATE =
            "CREATE TABLE " + CLIPER_TABLE_NAME + " (" +
                    KEY_SOURCE + " INTEGER, " +
                    KEY_MSG + " TEXT, " +
                    KEY_TIME + " TEXT);";
    private static final String CLIPER_TABLE_DELETE =
            "DROP TABLE IF EXISTS " + CLIPER_TABLE_NAME;

    CliperDbOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CLIPER_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(CLIPER_TABLE_DELETE);
        onCreate(db);
    }

    public static void insertMsg (Boolean me, String msg, CliperDbOpenHelper dbHelper) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_SOURCE, me?1:0);
        values.put(KEY_MSG, msg);
        values.put(KEY_TIME, new Date().toString());
        db.insert(CLIPER_TABLE_NAME, "null", values);
    }

    public static ArrayList<ChatMessage> getAllMessages(CliperDbOpenHelper dbHelper) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {
                KEY_SOURCE,
                KEY_MSG,
                KEY_TIME
        };
        Cursor c = db.query(CLIPER_TABLE_NAME, projection, null, null, null, null, null);
        int cap = c.getCount();
        c.moveToFirst();
        ArrayList<ChatMessage> result = new ArrayList<ChatMessage>(cap);
        for (int i = 0; i < cap; i++) {
            boolean isMe = c.getInt(c.getColumnIndexOrThrow(KEY_SOURCE)) > 0;
            String msg = c.getString(c.getColumnIndexOrThrow(KEY_MSG));
            String time = c.getString(c.getColumnIndexOrThrow(KEY_TIME));
            result.add(new ChatMessage(isMe, msg, time));
            c.moveToNext();
        }
        return result;
    }
}
