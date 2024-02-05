package com.itda.myapplication;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static android.content.ContentValues.TAG;


public class DatabaseHelper extends SQLiteOpenHelper
{

    static final String TABLE_NAME = "guide";
    public DatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        Log.d(TAG, "DataBaseHelper 생성자 호출");
    }



    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Log.d(TAG, "Table Create");
        String createQuery = "create table if not exists "+TABLE_NAME +
                "( ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "TITLE TEXT NOT NULL, " +
                "CONTENT TEXT NOT NULL);";
        sqLiteDatabase.execSQL(createQuery);


    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        Log.d(TAG, "Table onUpgrade");
        // 테이블 재정의하기 위해 현재의 테이블 삭제
        String createQuery = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
        sqLiteDatabase.execSQL(createQuery);

    }
}