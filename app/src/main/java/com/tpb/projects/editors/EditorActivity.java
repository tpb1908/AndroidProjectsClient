package com.tpb.projects.editors;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.androidnetworking.error.ANError;
import com.tpb.github.data.Uploader;
import com.tpb.projects.BuildConfig;
import com.tpb.projects.R;
import com.tpb.projects.common.CircularRevealActivity;
import com.tpb.projects.util.Logger;
import com.tpb.projects.util.UI;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by theo on 16/02/17.
 */

public abstract class EditorActivity extends CircularRevealActivity {
    private static final String TAG = EditorActivity.class.getSimpleName();

    private static final int REQUEST_CAMERA = 9403; //Random request codes
    private static final int SELECT_FILE = 6113;
    private String mCurrentFilePath;
    protected ProgressDialog mUploadDialog;


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == AppCompatActivity.RESULT_OK) {
            if(requestCode == EmojiActivity.REQUEST_CODE_CHOOSE_EMOJI) {
                if(data.hasExtra(getString(R.string.intent_emoji))) {
                    emojiChosen(data.getStringExtra(getString(R.string.intent_emoji)));
                }
            } else if(requestCode == CharacterActivity.REQUEST_CODE_INSERT_CHARACTER) {
                if(data.hasExtra(getString(R.string.intent_character))) {
                    insertString(data.getStringExtra(getString(R.string.intent_character)));
                }
            } else {
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
                        uploadImage(Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT));
                    });

                } else if(requestCode == SELECT_FILE) {
                    final Uri selectedFile = data.getData();
                    pd.setTitle(R.string.title_image_conversion);
                    pd.show();
                    AsyncTask.execute(() -> {
                        try {
                            final String image = attemptLoadPicture(selectedFile);
                            pd.cancel();
                            uploadImage(image);
                        } catch(IOException ioe) {
                            Logger.e(TAG, "onActivityResult: ", ioe);
                            pd.cancel();
                            imageLoadException(ioe);
                        }
                    });
                }
            }
        }
    }

    abstract void imageLoadComplete(String url);

    abstract void imageLoadException(IOException ioe);

    void showImageUploadDialog() {
        final CharSequence[] items = {
                getString(R.string.text_take_a_picture),
                getString(R.string.text_choose_from_gallery),
                getString(R.string.text_insert_image_link),
                getString(R.string.action_cancel)
        };
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.text_upload_an_image));
        builder.setItems(items, (dialog, which) -> {
            if(mUploadDialog == null) {
                mUploadDialog = new ProgressDialog(EditorActivity.this);
                mUploadDialog.setTitle(R.string.title_image_upload);
                mUploadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            }
            if(which == 0) {
                attemptTakePicture();
            } else if(which == 1) {
                final Intent intent = new Intent(
                        Intent.ACTION_GET_CONTENT,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                );
                intent.setType("image/*");
                startActivityForResult(intent, SELECT_FILE);

            } else if(which == 2) {
                displayImageLinkDialog();
            } else if(which == 3) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void displayImageLinkDialog() {
        final LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPaddingRelative(UI.pxFromDp(16), 0, UI.pxFromDp(16), 0);

        final EditText desc = new EditText(this);
        desc.setHint(R.string.hint_url_description);
        wrapper.addView(desc);

        final EditText url = new EditText(this);
        url.setHint(R.string.hint_url_url);
        wrapper.addView(url);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_insert_image_link);
        builder.setView(wrapper);

        builder.setPositiveButton(R.string.action_insert, (v, di) -> {
            insertString(String.format(getString(R.string.text_image_link_with_desc),
                    desc.getText().toString(),
                    url.getText().toString()
            ));
        });
        builder.setNegativeButton(R.string.action_cancel, null);

        builder.create().show();
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
                final Uri photoURI = FileProvider
                        .getUriForFile(this, "com.tpb.projects.provider", photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(intent, REQUEST_CAMERA);
            } else {
                imageLoadException(new IOException(getString(R.string.error_image_file_not_created)));
            }
        } else {
            Toast.makeText(this, R.string.error_no_application_for_picture, Toast.LENGTH_SHORT)
                 .show();
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

    private void uploadImage(String image64) {
        new Handler(Looper.getMainLooper()).postAtFrontOfQueue(() -> mUploadDialog.show());
        new Uploader().uploadImage(new Uploader.ImgurUploadListener() {
                                       @Override
                                       public void imageUploaded(String link) {
                                           Logger.i(TAG, "imageUploaded: Image uploaded " + link);
                                           mUploadDialog.cancel();
                                           final String snippet = String.format(getString(R.string.text_image_link), link);
                                           imageLoadComplete(snippet);
                                       }

                                       @Override
                                       public void uploadError(ANError error) {
                                           //TODO Error message
                                       }
                                   }, image64, (bUP, bTotal) -> mUploadDialog.setProgress(Math.round((100 * bUP) / bTotal)),
                BuildConfig.IMGUR_CLIENT_ID
        );
    }

    protected abstract void emojiChosen(String emoji);

    protected abstract void insertString(String c);

}
