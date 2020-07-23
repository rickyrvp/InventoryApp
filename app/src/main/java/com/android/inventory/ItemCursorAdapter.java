package com.android.inventory;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cursoradapter.widget.CursorAdapter;
import com.android.inventory.data.InventoryContract.InventoryEntry;

import java.text.DecimalFormat;
import java.util.Currency;
import java.util.Locale;


public class ItemCursorAdapter extends CursorAdapter {
    private Context activity;
    private int mQuantity;
    private int mSoldQuantity;
    private static int SELL_QUANTITY = 1;

    public ItemCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
        this.activity = context;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Find fields to populate in inflated template
        TextView itemName = (TextView) view.findViewById(R.id.list_item_name);
        TextView itemQuantity = (TextView) view.findViewById(R.id.list_item_quantity);
        TextView itemPrice = (TextView) view.findViewById(R.id.list_item_price);
        TextView itemSoldQuantity = (TextView) view.findViewById(R.id.list_item_sold_quantity);
        ImageView itemImage = (ImageView) view.findViewById(R.id.list_item_image);
        Button itemButton = (Button) view.findViewById((R.id.list_item_sell_button));
        // Extract properties from cursor
        final int rowId = cursor.getInt(cursor.getColumnIndexOrThrow(InventoryEntry._ID));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(InventoryEntry.COLUMN_NAME));
        final int quantity = cursor.getInt(cursor.getColumnIndexOrThrow(InventoryEntry.COLUMN_QUANTITY));
        int price = cursor.getInt(cursor.getColumnIndexOrThrow(InventoryEntry.COLUMN_PRICE));
        final int soldQuantity = cursor.getInt(cursor.getColumnIndexOrThrow(InventoryEntry.COLUMN_SOLD_QUANTITY));
        byte[] byteImage = cursor.getBlob(cursor.getColumnIndexOrThrow(InventoryEntry.COLUMN_IMAGE));
        // Populate fields with extracted properties
        itemName.setText(name);
        itemQuantity.setText("In stock " + quantity + " pieces");
        //get local currency
        Locale locale = Locale.UK;
        Currency currency = Currency.getInstance(locale);
        String symbol = currency.getSymbol();
        //set price to be displayed properly
        DecimalFormat df = new DecimalFormat("#.00"); // Set your desired format here.
        String stringPrice = df.format(price / 100.0);

        itemPrice.setText("Price: " + symbol + stringPrice);
        itemSoldQuantity.setText(context.getString(R.string.sold) + soldQuantity + context.getString(R.string.units));
        if (byteImage != null) {
            Bitmap bitmapItemImage = BitmapFactory.decodeByteArray(byteImage, 0, byteImage.length);
            itemImage.setImageBitmap(bitmapItemImage);
        } else {
            itemImage.setImageBitmap(null);
        }

        itemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mQuantity = quantity;
                mSoldQuantity = soldQuantity;
                if (mQuantity >= SELL_QUANTITY) {
                    mSoldQuantity += SELL_QUANTITY;
                    mQuantity -= SELL_QUANTITY;
                    ContentValues values = new ContentValues();
                    values.put(InventoryEntry.COLUMN_QUANTITY, mQuantity);
                    values.put(InventoryEntry.COLUMN_SOLD_QUANTITY, mSoldQuantity);
                    Uri currentItemUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, rowId);
                    int itemIdUpdated = activity.getContentResolver().update(currentItemUri, values, null, null);
                    showToastMassage(itemIdUpdated);
                } else {
                    Toast.makeText(activity, R.string.unavailable, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showToastMassage(int id) {
        if (id == 0) {
            Toast.makeText(activity, R.string.item_sold_error, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(activity, R.string.item_sold, Toast.LENGTH_SHORT).show();
        }
    }
}
