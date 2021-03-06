package com.tpb.projects.util;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.Dimension;
import android.support.annotation.NonNull;
import android.support.annotation.Px;
import android.support.v4.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Transformation;
import android.widget.ImageView;

import com.tpb.projects.R;
import com.tpb.projects.common.CircularRevealActivity;

/**
 * Created by theo on 16/12/16.
 */

public class UI {

    private UI() {}

    /**
     * Sets the expansion point for a {@link CircularRevealActivity} Intent
     * to the midpoint of a View
     *
     * @param i    The Intent to launch a {@link CircularRevealActivity} instance
     * @param view The View instance which was clicked
     */
    public static void setViewPositionForIntent(Intent i, View view) {
        final int[] pos = new int[2];
        view.getLocationOnScreen(pos);
        pos[0] += view.getWidth() / 2;
        pos[1] += view.getHeight() / 2;
        i.putExtra(view.getContext().getString(R.string.intent_position_x), pos[0]);
        i.putExtra(view.getContext().getString(R.string.intent_position_y), pos[1]);
    }

    /**
     * @param context Required to get string resource values
     * @param i       The Intent to launch a {@link CircularRevealActivity} instance
     * @param pos     The x and y coordinates of the click
     */
    public static void setClickPositionForIntent(Context context, Intent i, float[] pos) {
        i.putExtra(context.getString(R.string.intent_position_x), (int) pos[0]);
        i.putExtra(context.getString(R.string.intent_position_y), (int) pos[1]);
    }

    public static void expand(final View v) {
        v.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final int targetHeight = v.getMeasuredHeight();

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        v.getLayoutParams().height = 1;
        v.setVisibility(View.VISIBLE);
        final android.view.animation.Animation a = new android.view.animation.Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                        ? ViewGroup.LayoutParams.WRAP_CONTENT
                        : (int) (targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration(
                (int) (targetHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    public static void collapse(final View v) {
        final int initialHeight = v.getMeasuredHeight();

        android.view.animation.Animation a = new android.view.animation.Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if(interpolatedTime == 1) {
                    v.setVisibility(View.GONE);
                } else {
                    v.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration(
                (int) (initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    /**
     * Fades the background color of a View from original to flash and back
     *
     * @param view     The view to flash
     * @param original The current background color
     * @param flash    The color to fade to
     */
    public static void flashViewBackground(View view, @ColorInt int original, @ColorInt int flash) {
        final ObjectAnimator colorFade = ObjectAnimator.ofObject(
                view,
                "backgroundColor",
                new ArgbEvaluator(),
                original,
                flash
        );
        colorFade.setDuration(300);
        colorFade.setRepeatMode(ObjectAnimator.REVERSE);
        colorFade.setRepeatCount(1);
        colorFade.start();

    }

    @Dimension
    public static float dpFromPx(final float px) {
        return px / Resources.getSystem().getDisplayMetrics().density;
    }

    @Px
    public static int pxFromDp(final float dp) {
        return Math.round(dp * Resources.getSystem().getDisplayMetrics().density);
    }

    @Px
    public static int pxFromDp(final int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    @Px
    public static int pxFromSp(final float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp,
                Resources.getSystem().getDisplayMetrics()
        );
    }

    public static void setStatusBarColor(Window window, @ColorInt int color) {
        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        // finally change the color
        window.setStatusBarColor(color);
    }

    /**
     * Checks whether the device has a navigation bar, and if so returns a pair
     * for {@see ActivityOptionsCompat#makeSceneTransition}
     *
     * @param activity Any activity in which to find the navigation bar
     * @return The navigation bar view view pair, or an empty view pair
     */
    public static Pair<View, String> getSafeNavigationBarTransitionPair(@NonNull Activity activity) {
        final View nav = activity.findViewById(android.R.id.navigationBarBackground);
        return nav == null ?
                Pair.create(new View(activity), "not_for_transition") :
                Pair.create(nav, Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME);
    }


    public static void setDrawableForIntent(@NonNull ImageView iv, @NonNull Intent i) {
        if(iv.getDrawable() instanceof BitmapDrawable) {
            i.putExtra(iv.getResources().getString(R.string.intent_drawable),
                    ((BitmapDrawable) iv.getDrawable()).getBitmap()
            );
        }
    }


}
