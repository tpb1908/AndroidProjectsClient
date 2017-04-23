package com.tpb.mdtext.imagegetter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.widget.TextView;

/**
 * Copied from http://stackoverflow.com/a/22298833
 */
class ResImageGetter implements Html.ImageGetter {
    private final TextView container;

    public ResImageGetter(TextView textView) {
        this.container = textView;
    }

    public Drawable getDrawable(String source) {
        final Context context = container.getContext();
        int id = context.getResources().getIdentifier(source, "drawable", context.getPackageName());

        if(id == 0) {
            //Drawable not in this package, might be somewhere else
            id = context.getResources().getIdentifier(source, "drawable", "android");
        }

        if(id == 0) {
            return null;
        } else {
            final Drawable d = context.getResources().getDrawable(id);
            d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            return d;
        }
    }

}