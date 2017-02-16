package com.tpb.projects.editors;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by theo on 16/02/17.
 */

public abstract class ImageLoadingActivity extends AppCompatActivity {
    private static final String TAG = ImageLoadingActivity.class.getSimpleName();


    private static final int REQUEST_CAMERA = 9403;
    private static final int SELECT_FILE = 6113;
    private String mCurrentFilePath;

    abstract void imageLoadComplete(String image64, ProgressDialog dialog);

    abstract void imageLoadException(IOException ioe);

    void showDialog() {
        final CharSequence[] items = {"Take a picture", "Choose from gallery", "Cancel"};
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Upload an image");
        builder.setItems(items, (dialog, which) -> {
            if (items[which].equals(items[0])) {
                attemptTakePicture();
            } else if (items[which].equals(items[1])) {
                final Intent intent = new Intent(
                        Intent.ACTION_GET_CONTENT,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                startActivityForResult(intent, SELECT_FILE);

            } else if (items[which].equals(items[2])) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void attemptTakePicture() {
        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(intent.resolveActivity(getPackageManager()) != null) {

            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch(IOException ioe) {
                Log.e(TAG, "attemptTakePicture: ", ioe);
                imageLoadException(ioe);
            }

            if(photoFile != null) {
                final Uri photoURI = FileProvider.getUriForFile(this, "com.tpb.projects.provider", photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                Log.i(TAG, "attemptTakePicture: File created");
                startActivityForResult(intent, REQUEST_CAMERA);
            } else {
                Log.i(TAG, "attemptTakePicture: File is null");
                imageLoadException(new IOException("File not created"));
            }
        } else {
            Log.i(TAG, "onClick: No camera to use");
        }
    }

    private File createImageFile() throws IOException {
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        final String imageFileName = "JPEG_" + timeStamp + "_";
        final File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        final File image =  File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        mCurrentFilePath = image.getAbsolutePath();
        return image;
    }

    private String attemptLoadPicture(Uri uri) throws IOException {
        final ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        final FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();

        final Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        Log.i(TAG, "onActivityResult: Image is: " + image.toString());
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] array = stream.toByteArray();
        Log.i(TAG, "onActivityResult: Array length " + array.length);
        return Base64.encodeToString(array, Base64.DEFAULT);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == AppCompatActivity.RESULT_OK) {
            final ProgressDialog pd = new ProgressDialog(this);
            pd.setCanceledOnTouchOutside(false);
            pd.setCancelable(false);
            if(requestCode == REQUEST_CAMERA) {
                Log.i(TAG, "onActivityResult: Camera request returned");
                Toast.makeText(this, mCurrentFilePath, Toast.LENGTH_LONG).show();
                pd.setTitle("Converting image");
                pd.show();
                AsyncTask.execute(() -> {
                    final Bitmap image = BitmapFactory.decodeFile(mCurrentFilePath);
                    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    image.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    imageLoadComplete(Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT), pd);
                });

            } else if(requestCode == SELECT_FILE) {
                Log.i(TAG, "onActivityResult: File request returned");
                final Uri selectedFile = data.getData();
                Log.i(TAG, "onActivityResult: Uri is "+ selectedFile.toString());
                pd.setTitle("Converting image");
                pd.show();
                AsyncTask.execute(() -> {
                    try {
                        imageLoadComplete(attemptLoadPicture(selectedFile), pd);
                    } catch(IOException ioe) {
                        Log.e(TAG, "onActivityResult: ", ioe);
                        pd.cancel();
                        imageLoadException(ioe);
                    }
                });
            }
        }
    }

}
