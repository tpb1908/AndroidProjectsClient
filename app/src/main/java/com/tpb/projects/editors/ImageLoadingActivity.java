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

import com.tpb.projects.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by theo on 16/02/17.
 */

public abstract class ImageLoadingActivity extends CircularRevealActivity {
    private static final String TAG = ImageLoadingActivity.class.getSimpleName();

    private static final int REQUEST_CAMERA = 9403; //Random request codes
    private static final int SELECT_FILE = 6113;
    private String mCurrentFilePath;
    protected ProgressDialog mUploadDialog;

    abstract void imageLoadComplete(String image64);

    abstract void imageLoadException(IOException ioe);

    void showImageUploadDialog() {
        final CharSequence[] items = {
                getString(R.string.text_take_a_picture),
                getString(R.string.text_choose_from_gallery),
                getString(R.string.action_cancel)
        };
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.text_upload_an_image));
        builder.setItems(items, (dialog, which) -> {
            if(mUploadDialog == null) {
                mUploadDialog = new ProgressDialog(ImageLoadingActivity.this);
                mUploadDialog.setTitle(R.string.title_image_upload);
                mUploadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            }
            if(items[which].equals(items[0])) {
                attemptTakePicture();
            } else if(items[which].equals(items[1])) {
                final Intent intent = new Intent(
                        Intent.ACTION_GET_CONTENT,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                startActivityForResult(intent, SELECT_FILE);

            } else if(items[which].equals(items[2])) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void attemptTakePicture() {
        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Check if there is an activity which can take a picture
        if(intent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                //Create the file for the image to be stored in
                photoFile = createImageFile();
            } catch(IOException ioe) {
                Log.e(TAG, "attemptTakePicture: ", ioe);
                imageLoadException(ioe);
            }

            if(photoFile != null) {
                final Uri photoURI = FileProvider.getUriForFile(this, "com.tpb.projects.provider", photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(intent, REQUEST_CAMERA);
            } else {
                imageLoadException(new IOException("File not created"));
            }
        } else {
            Toast.makeText(this, R.string.error_no_application_for_picture, Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        //Create an image file with a formatted name
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        final String imageFileName = "JPEG_" + timeStamp + "_";
        final File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        final File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        mCurrentFilePath = image.getAbsolutePath();
        return image;
    }

    private String attemptLoadPicture(Uri uri) throws IOException {
        // Open FileDescriptor in read mode
        final ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        final FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();

        //Decode to a bitmap, and convert to a byte array
        final Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] array = stream.toByteArray();
        //Return base64 string for Imgur
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
                pd.setTitle(R.string.title_image_conversion);
                pd.show();
                AsyncTask.execute(() -> { // Execute asynchronously
                    final Bitmap image = BitmapFactory.decodeFile(mCurrentFilePath);
                    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    image.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    pd.cancel();
                    imageLoadComplete(Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT));
                });

            } else if(requestCode == SELECT_FILE) {
                final Uri selectedFile = data.getData();
                Log.i(TAG, "onActivityResult: Uri is " + selectedFile.toString());
                pd.setTitle(R.string.title_image_conversion);
                pd.show();
                AsyncTask.execute(() -> {
                    try {
                        final String image = attemptLoadPicture(selectedFile);
                        pd.cancel();
                        imageLoadComplete(image);
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
