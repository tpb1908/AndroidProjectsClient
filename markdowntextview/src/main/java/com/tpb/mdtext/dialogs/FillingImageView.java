package com.tpb.mdtext.dialogs;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

/**
 * Created by theo on 31/01/17.
 */

public class FillingImageView extends AppCompatImageView {

    public FillingImageView(Context context) {
        super(context);
    }

    public FillingImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }


    public FillingImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        try {
            final Drawable drawable = getDrawable();

            if(drawable == null) {
                setMeasuredDimension(0, 0);
            } else {
                final float imageSideRatio = (float) drawable.getIntrinsicWidth() / (float) drawable
                        .getIntrinsicHeight(); //Image aspect ratio
                final float viewSideRatio = (float) MeasureSpec
                        .getSize(widthMeasureSpec) / (float) MeasureSpec
                        .getSize(heightMeasureSpec); //Aspect ratio of parent
                if(imageSideRatio >= viewSideRatio) {
                    // Image is wider than the display (ratio)
                    final int width = MeasureSpec.getSize(widthMeasureSpec);
                    final int height = (int) (width / imageSideRatio);
                    setMeasuredDimension(width, height);
                } else {
                    // Image is taller than the display (ratio)
                    final int height = MeasureSpec.getSize(heightMeasureSpec);
                    final int width = (int) (height * imageSideRatio);
                    setMeasuredDimension(width, height);
                }
            }
        } catch(Exception e) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

}
