package com.android.inventory.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.android.inventory.data.InventoryContract.InventoryEntry;


public class InventoryDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "inventory.db";

    public InventoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + InventoryEntry.TABLE_NAME;

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + InventoryEntry.TABLE_NAME + " (" +
                        InventoryEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        InventoryEntry.COLUMN_NAME + " TEXT NOT NULL, " +
                        InventoryEntry.COLUMN_QUANTITY + " INTEGER NOT NULL DEFAULT 0, " +
                        InventoryEntry.COLUMN_PRICE + " INTEGER NOT NULL, " +
                        InventoryEntry.COLUMN_IMAGE + " BLOB, " +
                        InventoryEntry.COLUMN_SOLD_QUANTITY + " INTEGER NOT NULL DEFAULT 0, " +
                        InventoryEntry.COLUMN_SUPPLIER_EMAIL + " TEXT, " +
                        InventoryEntry.COLUMN_SUPPLIER_PHONE + " TEXT);";


        sqLiteDatabase.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL(SQL_DELETE_ENTRIES);
        onCreate(sqLiteDatabase);
    }
}