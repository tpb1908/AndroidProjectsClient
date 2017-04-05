package com.tpb.mdtext.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.tpb.mdtext.handlers.ImageClickHandler;

import org.sufficientlysecure.htmltextview.R;

/**
 * Created by theo on 27/02/17.
 */

public class ImageDialog implements ImageClickHandler {

    private final Context mContext;

    public ImageDialog(Context context) {
        mContext = context;
    }

    @Override
    public void imageClicked(Drawable drawable) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        final LayoutInflater inflater = LayoutInflater.from(mContext);

        final View view = inflater.inflate(R.layout.dialog_image, null);

        builder.setView(view);

        final FillingImageView fiv = (FillingImageView) view.findViewById(R.id.dialog_imageview);
        fiv.setImageDrawable(drawable);

        final Dialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
        );

        fiv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();

            }
        });

        dialog.show();

    }

}
