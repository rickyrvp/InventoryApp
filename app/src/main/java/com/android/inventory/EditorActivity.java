package com.android.inventory;

import android.Manifest;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.core.content.FileProvider;
import com.android.inventory.data.InventoryContract.InventoryEntry;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;


public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = EditorActivity.class.getSimpleName();

    private static final int GALLERY_PERMISSIONS_REQUEST = 0;
    private static final int GALLERY_IMAGE_REQUEST = 1;
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;
    public static final int CAMERA_IMAGE_REQUEST = 3;
    public static final int IMAGE_SIZE = 800;
    public static final String FILE_NAME = "temp.jpg";

    private static final int EXISTING_ITEM_LOADER = 0;

    private ExifInterface exif = null;

    private Uri mCurrentItemUri;
    private boolean mItemHasChanged = false;
    private EditText mNameEditText;
    private EditText mQuantityEditText;
    private EditText mPriceEditText;
    private EditText mEmailEditText;
    private EditText mPhoneEditText;
    private TextView mSoldQuantityText;
    private ImageView mItemImageView;
    private Bitmap mBitmapImage;
    private String mCurrentPhotoPath;
    private int priceLength = 0;//for stopping infinity loop

    private int soldQuantity = 0;
    private int quantity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        final Intent intent = getIntent();
        mCurrentItemUri = intent.getData();
        // find all views by id
        mNameEditText = (EditText) findViewById(R.id.edit_item_name);
        mQuantityEditText = (EditText) findViewById(R.id.edit_item_quantity);
        mPriceEditText = (EditText) findViewById(R.id.edit_item_price);
        mSoldQuantityText = (TextView) findViewById(R.id.edit_item_sold_quantity);
        mItemImageView = (ImageView) findViewById(R.id.edit_item_image);
        mEmailEditText = (EditText) findViewById(R.id.edit_item_email);
        mPhoneEditText = (EditText) findViewById(R.id.edit_item_phone);
        Button imageButton = (Button) findViewById(R.id.editor_add_image_button);
        Button orderEmailButton = (Button) findViewById(R.id.edit_order_email_button);
        Button orderPhoneButton = (Button) findViewById(R.id.edit_order_phone_button);
        TextView currencyTextView = (TextView) findViewById(R.id.edit_item_currency);
        RelativeLayout sellOrderLayout = (RelativeLayout) findViewById(R.id.edit_sell_order_layout);

        //difference between new and edit activity
        if (mCurrentItemUri == null) {
            setTitle(R.string.editor_activity_title_new_item);
            sellOrderLayout.setVisibility(View.GONE);
            orderEmailButton.setVisibility(View.GONE);
            orderPhoneButton.setVisibility(View.GONE);
        } else {
            setTitle(R.string.editor_activity_title_edit_item);
            invalidateOptionsMenu();
            getLoaderManager().initLoader(EXISTING_ITEM_LOADER, null, this);
            Button sellButton = (Button) findViewById(R.id.edit_sell_button);
            Button shipmentButton = (Button) findViewById(R.id.edit_shipment_button);
            final EditText quantitySizeForOrderOrSell = (EditText) findViewById(R.id.edit_quantity_size);
            sellButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int quantitySize = Integer.parseInt(quantitySizeForOrderOrSell.getText().toString());
                    sellQuantity(quantitySize);
                }
            });
            shipmentButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int quantitySize = Integer.parseInt(quantitySizeForOrderOrSell.getText().toString());
                    orderQuantity(quantitySize);
                }
            });
            orderEmailButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:")); // only email apps should handle this
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{mEmailEditText.getText().toString().trim()});
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Order");
                    intent.putExtra(Intent.EXTRA_TEXT, "Order 100 pieces of item " + mNameEditText.getText().toString().trim());
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }
            });
            orderPhoneButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Uri number = Uri.parse("tel:" + mPhoneEditText.getText().toString().trim());
                    Intent callIntent = new Intent(Intent.ACTION_DIAL, number);
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(callIntent);
                    } else {
                        Toast.makeText(EditorActivity.this, R.string.phone_error, Toast.LENGTH_SHORT).show();
                    }

                }
            });

        }
        //set currency symbol

        Locale locale = Locale.UK;
        Currency currency = Currency.getInstance(locale);
        String symbol = currency.getSymbol();
        currencyTextView.setText(symbol);
        // set Touch Listeners
        mNameEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);


        // Image button listener

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addImageAlertDialog(); // adding image starts with opening dialog and finish with rotating image
            }
        });
        mPriceEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (priceLength < charSequence.length()) { // check this state or this will be infinity loop
                    priceLength = charSequence.length();
                    String currentString = charSequence.toString();
                    currentString = currentString.replaceAll("[^\\d]", ""); // remove all non numeric characters
                    int price = Integer.parseInt(currentString);
                    DecimalFormat df = new DecimalFormat("#0.00"); // Set your desired format here.
                    currentString = df.format(price / 100.0);
                    mPriceEditText.setText(currentString);
                    mPriceEditText.setSelection(currentString.length());//Set cursor at the end
                } else {
                    priceLength = charSequence.length();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

    }

    private void displayCorrectPrice() {
        String currentString = mPriceEditText.getText().toString();
        currentString = currentString.replaceAll("[^\\d]", ""); // remove all non numeric characters
        int price = Integer.parseInt(currentString);
        DecimalFormat df = new DecimalFormat("#0.00"); // Set your desired format here.
        currentString = df.format(price / 100.0);
        mPriceEditText.setText(currentString);
    }

    private void sellQuantity(int quantitySize) {
        if (quantity >= quantitySize) {
            quantity -= quantitySize;
            soldQuantity += quantitySize;
            mQuantityEditText.setText(String.valueOf(quantity));
            if (!checkEditInput()) {
                saveItem();
            } else {
                Toast.makeText(this, R.string.fields_required, Toast.LENGTH_SHORT).show();
            }

        } else {
            showToastMassage(0);
        }
    }

    private void orderQuantity(int quantitySize) {
        quantity += quantitySize;
        mQuantityEditText.setText(String.valueOf(quantity));
        if (!checkEditInput()) {
            saveItem();
        } else {
            Toast.makeText(this, R.string.fields_required, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * dialog form to choose gallery or camera
     */
    private void addImageAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(EditorActivity.this);
        builder
                .setMessage(R.string.dialog_select_prompt)
                .setPositiveButton(R.string.dialog_select_gallery, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startGalleryChooser();
                    }
                })
                .setNegativeButton(R.string.dialog_select_camera, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startCamera();
                    }
                });
        builder.create().show();
    }

    /**
     * if user chose gallery run this code
     */
    public void startGalleryChooser() {
        if (PermissionUtils.requestPermission(this, GALLERY_PERMISSIONS_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select a photo"),
                    GALLERY_IMAGE_REQUEST);
        }
    }

    /**
     * if user chose camera run this code
     */
    public void startCamera() {
        if (PermissionUtils.requestPermission(
                this,
                CAMERA_PERMISSIONS_REQUEST,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    return;
                }
                if (photoFile != null) {
                    Uri photoUri = FileProvider.getUriForFile(EditorActivity.this,
                            BuildConfig.APPLICATION_ID + ".provider",
                            photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    startActivityForResult(takePictureIntent, CAMERA_IMAGE_REQUEST);
                }
            }
        }
    }
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "Camera");
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }
    /**
     * @return picture file taken from camera
     */
    public File getCameraFile() {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
    }

    /**
     * This code is run after startActivityForResult method
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            try {
                exif = new ExifInterface(getRealPathFromURI(data.getData()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            showImage(data.getData());
        } else if (requestCode == CAMERA_IMAGE_REQUEST && resultCode == RESULT_OK) {
            Uri imageUri = Uri.parse(mCurrentPhotoPath);
            File file = new File(imageUri.getPath());
            try {
                exif = new ExifInterface(Uri.fromFile(file).getPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            showImage(Uri.fromFile(file));
        }
    }

    /**
     * Not sure what exactly is this for but it do something if user did not give permission for camera or to read internal storage
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, CAMERA_PERMISSIONS_REQUEST, grantResults)) {
                    startCamera();
                }
                break;
            case GALLERY_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, GALLERY_PERMISSIONS_REQUEST, grantResults)) {
                    startGalleryChooser();
                }
                break;
        }
    }

    /**
     * We want to show image and rotate it to be in portrait mode
     *
     * @param uri path of picture file
     */
    public void showImage(Uri uri) {
        if (uri != null) {
            try {
                Bitmap bitmap = scaleBitmapDown(MediaStore.Images.Media.getBitmap(getContentResolver(), uri), IMAGE_SIZE);
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                bitmap = rotateBitmap(bitmap, orientation);
                mItemImageView.setImageBitmap(bitmap);
                mBitmapImage = bitmap;
            } catch (IOException e) {
                Log.d(TAG, "Image picking failed because " + e.getMessage());
                Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(TAG, "Image picker gave us a null image.");
            Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Get uri for different android versions
     */
    public String getRealPathFromURI(Uri uri) {
        if (Build.VERSION.SDK_INT >= 19) {
            String id = uri.getLastPathSegment().split(":")[1];
            final String[] imageColumns = {MediaStore.Images.Media.DATA};
            final String imageOrderBy = null;
            Uri tempUri = getUri();
            Cursor imageCursor = getContentResolver().query(tempUri, imageColumns,
                    MediaStore.Images.Media._ID + "=" + id, null, imageOrderBy);
            if (imageCursor.moveToFirst()) {
                return imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.Media.DATA));
            } else {
                return null;
            }
        } else {
            String[] projection = {MediaStore.MediaColumns.DATA};
            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                cursor.moveToFirst();
                return cursor.getString(column_index);
            } else
                return null;
        }
    }

    private Uri getUri() {
        String state = Environment.getExternalStorageState();
        if (!state.equalsIgnoreCase(Environment.MEDIA_MOUNTED))
            return MediaStore.Images.Media.INTERNAL_CONTENT_URI;

        return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    }

    /**
     * scale image because we don't need big image to be shown in 90*90dp box
     *
     * @param bitmap       is picture file
     * @param maxDimension set this in IMAGE_SIZE variable
     * @return Bitmap picture that is smaller in size
     */
    public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    /**
     * Rotate image that is shown wrong like upside down
     */
    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mItemHasChanged = true;
            return false;
        }
    };

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new item, hide the "Delete" menu item.
        if (mCurrentItemUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                if (!checkEditInput()) {
                    // Save item to database
                    saveItem();
                    finish();
                    return true;
                } else {
                    Toast.makeText(this, R.string.fields_required, Toast.LENGTH_SHORT).show();
                }
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // Navigate back to parent activity (CatalogActivity)
                if (!mItemHasChanged) {
                    NavUtils.navigateUpFromSameTask(this);
                } else {
                    // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                    // Create a click listener to handle the user confirming that
                    // changes should be discarded.
                    DialogInterface.OnClickListener discardButtonClickListener =
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    // User clicked "Discard" button, navigate to parent activity.
                                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                                }
                            };

                    // Show a dialog that notifies the user they have unsaved changes
                    showUnsavedChangesDialog(discardButtonClickListener);
                    return true;
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * check for right user input when creating or editing item
     *
     * @return true if any of required fields are empty
     */
    private boolean checkEditInput() {
        boolean checkState = false;
        String name = mNameEditText.getText().toString().trim();
        String quantity = mQuantityEditText.getText().toString().trim();
        String price = mPriceEditText.getText().toString().trim();
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(quantity) || TextUtils.isEmpty(price)) {
            checkState = true;
        }
        return checkState;
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the item.
                deleteItem();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void saveItem() {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String nameString = mNameEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        int quantity = Integer.parseInt(quantityString);
        String priceString = mPriceEditText.getText().toString().trim();
        priceString = priceString.replaceAll("[^\\d]", ""); // remove all non numeric characters
        int price = Integer.parseInt(priceString);
        String email = mEmailEditText.getText().toString().trim();
        String phone = mPhoneEditText.getText().toString().trim();


        // Create a ContentValues object where column names are the keys,
        // and item attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(InventoryEntry.COLUMN_NAME, nameString);
        values.put(InventoryEntry.COLUMN_QUANTITY, quantity);
        values.put(InventoryEntry.COLUMN_PRICE, price);
        // if user choose image than put it in
        if (mBitmapImage != null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            mBitmapImage.compress(Bitmap.CompressFormat.PNG, 0, bos);
            byte[] image = bos.toByteArray();
            values.put(InventoryEntry.COLUMN_IMAGE, image);
        }
        values.put(InventoryEntry.COLUMN_SOLD_QUANTITY, soldQuantity);
        values.put(InventoryEntry.COLUMN_SUPPLIER_EMAIL, email);
        values.put(InventoryEntry.COLUMN_SUPPLIER_PHONE, phone);

        // Inserting or updating data in database
        if (mCurrentItemUri == null) {
            Uri newUri = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);
            showToastMassage(newUri);
        } else {
            int itemIdUpdated = getContentResolver().update(mCurrentItemUri, values, null, null);
            showToastMassage(itemIdUpdated);
        }
    }

    /**
     * Show a toast message depending on whether or not the insertion was successful
     *
     * @param newUri Uri returned from insert method
     */
    private void showToastMassage(Uri newUri) {
        if (newUri == null) {
            Toast.makeText(this, R.string.item_saving_error, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.item_saved, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show a toast message depending on whether or not the update was successful
     *
     * @param id integer returned from update method
     */
    private void showToastMassage(int id) {
        if (id == 0) {
            Toast.makeText(this, R.string.item_saving_error, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.item_saved, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Perform the deletion of the item in the database.
     */
    private void deleteItem() {
        // Only perform the delete if this is an existing item
        if (mCurrentItemUri != null) {
            int deletedItem = getContentResolver().delete(mCurrentItemUri, null, null);
            if (deletedItem != 0) {
                Toast.makeText(this, R.string.editor_delete_item_successful, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.editor_delete_item_failed, Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        // If there is no change, continue with handling back button press
        if (!mItemHasChanged) {
            super.onBackPressed();
        } else {
            // Otherwise if there are unsaved changes, setup a dialog to warn the user.
            // Create a click listener to handle the user confirming that changes should be discarded.
            DialogInterface.OnClickListener discardButtonClickListener =
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // User clicked "Discard" button, close the current activity.
                            finish();
                        }
                    };

            // Show dialog that there are unsaved changes
            showUnsavedChangesDialog(discardButtonClickListener);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = {
                InventoryEntry._ID,
                InventoryEntry.COLUMN_NAME,
                InventoryEntry.COLUMN_QUANTITY,
                InventoryEntry.COLUMN_PRICE,
                InventoryEntry.COLUMN_IMAGE,
                InventoryEntry.COLUMN_SOLD_QUANTITY,
                InventoryEntry.COLUMN_SUPPLIER_EMAIL,
                InventoryEntry.COLUMN_SUPPLIER_PHONE
        };

        return new CursorLoader(this, mCurrentItemUri,
                projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor.moveToFirst()) {
            // Find the columns of items attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_NAME);
            int quantityColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_QUANTITY);
            int priceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRICE);
            int imageColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_IMAGE);
            int soldQuantityColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_SOLD_QUANTITY);
            int emailColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_SUPPLIER_EMAIL);
            int phoneColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_SUPPLIER_PHONE);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            quantity = cursor.getInt(quantityColumnIndex);
            int price = cursor.getInt(priceColumnIndex);
            soldQuantity = cursor.getInt(soldQuantityColumnIndex);
            byte[] imgByte = null;
            if (cursor.getBlob(imageColumnIndex) != null) {
                imgByte = cursor.getBlob(imageColumnIndex);
            }
            if (imgByte != null) {
                mBitmapImage = BitmapFactory.decodeByteArray(imgByte, 0, imgByte.length);
                mItemImageView.setImageBitmap(mBitmapImage);
            } else {
                mItemImageView.setImageBitmap(null);
            }
            String email = cursor.getString(emailColumnIndex);
            String phone = cursor.getString(phoneColumnIndex);
            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mQuantityEditText.setText(Integer.toString(quantity));
            mPriceEditText.setText(Integer.toString(price));
            displayCorrectPrice();
            mEmailEditText.setText(email);
            mPhoneEditText.setText(phone);
            mSoldQuantityText.setText(getString(R.string.sold) + soldQuantity + getString(R.string.units));

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

}
