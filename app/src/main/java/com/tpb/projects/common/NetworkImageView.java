package com.tpb.projects.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.androidnetworking.error.ANError;
import com.androidnetworking.internal.ANImageLoader;
import com.tpb.projects.R;

/**
 * Created by theo on 01/04/17.
 */

public class NetworkImageView extends AppCompatImageView {

    private String mUrl;

    @IdRes private int mDefaultImageResId;
    @IdRes private int mErrorImageResId;

    private ANImageLoader.ImageContainer mImageContainer;

    public NetworkImageView(Context context) {
        super(context);
    }

    public NetworkImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        if(attrs != null) init(attrs, 0);
    }

    public NetworkImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if(attrs != null) init(attrs, defStyleAttr);
    }

    private void init(AttributeSet attrs, int defStyleAttr) {
        final TypedArray array = getContext()
                .obtainStyledAttributes(attrs, R.styleable.NetworkImageView, defStyleAttr, 0);
        mDefaultImageResId = array
                .getResourceId(R.styleable.NetworkImageView_default_image_resource,
                        R.drawable.ic_avatar_default
                );
        mErrorImageResId = array
                .getResourceId(R.styleable.NetworkImageView_error_image_resource, 0);
        array.recycle();
    }

    public void setImageUrl(@NonNull String url) {
        mUrl = url;
        loadImage(false);
    }

    public void setDefaultImageResId(@IdRes int defaultImage) {
        mDefaultImageResId = defaultImage;
    }

    public void setErrorImageResId(@IdRes int errorImage) {
        mErrorImageResId = errorImage;
    }

    private void loadImage(final boolean isInLayoutPass) {
        final int width = getWidth();
        final int height = getHeight();


        boolean wrapWidth = false, wrapHeight = false;
        if(getLayoutParams() != null) {
            wrapWidth = getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT;
            wrapHeight = getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT;
        }

        if(width == 0 && height == 0 && !(wrapWidth && wrapHeight)) {
            //Can't display the image as not size
            return;
        }

        if(TextUtils.isEmpty(mUrl)) {
            if(mImageContainer != null) {
                mImageContainer.cancelRequest();
                mImageContainer = null;
            }
            setDefaultImage();
            return;
        }

        if(mImageContainer != null && mImageContainer.getRequestUrl() != null) {
            if(mImageContainer.getRequestUrl().equals(mUrl)) {
                return;
            } else {
                mImageContainer.cancelRequest();
            }
        }

        final int maxWidth = wrapWidth ? 0 : width;
        final int maxHeight = wrapHeight ? 0 : height;

        final ScaleType scaleType = getScaleType();
        mImageContainer = ANImageLoader.getInstance().get(mUrl,
                new ANImageLoader.ImageListener() {
                    @Override
                    public void onResponse(final ANImageLoader.ImageContainer response,
                                           boolean isImmediate) {
                        if(isImmediate && isInLayoutPass) {
                            post(() -> onResponse(response, false));
                            return;
                        }

                        if(response.getBitmap() != null) {
                            setImageBitmap(response.getBitmap());
                        } else if(mDefaultImageResId != 0) {
                            setDefaultImage();
                        }
                    }

                    @Override
                    public void onError(ANError error) {
                        if(mErrorImageResId != 0) {
                            setImageResource(mErrorImageResId);
                        }
                    }
                }, maxWidth, maxHeight, scaleType
        );

    }

    private void setDefaultImage() {
        if(getDrawable() != null) return; //Drawable has been set manually
        if(mDefaultImageResId != 0) {
            setImageResource(mDefaultImageResId);
        } else {
            setImageBitmap(null);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        loadImage(true);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }
}
