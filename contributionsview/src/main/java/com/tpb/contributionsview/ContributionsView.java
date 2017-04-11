/*
 * Copyright  2016 Theo Pearson-Bray
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.tpb.contributionsview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.VolleyError;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Created by theo on 12/01/17.
 */

public class ContributionsView extends View implements ContributionsLoader.ContributionsRequestListener {

    private static final Calendar mCalendar = Calendar.getInstance();

    private boolean mShouldDisplayMonths;
    private int mTextColor;
    private int mTextSize;
    private int mBackGroundColor;
    private ArrayList<ContributionsLoader.ContributionsDay> mContributions = new ArrayList<>();

    private Paint mDayPainter;
    private Paint mTextPainter;

    private Rect mRect;
    private final Rect mTextBounds = new Rect();

    private WeakReference<ContributionsLoadListener> mListener;

    public ContributionsView(Context context) {
        super(context);
        initView(context, null, 0, 0);
    }

    public ContributionsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs, 0, 0);
    }

    public ContributionsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs, defStyleAttr, 0);
    }

    private void initView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mRect = new Rect();

        final TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.ContributionsView, defStyleAttr, defStyleRes
        );
        useAttributes(attributes);

        mTextPainter = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDayPainter = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDayPainter.setStyle(Paint.Style.FILL);
    }

    private void useAttributes(TypedArray ta) {
        mShouldDisplayMonths = ta.getBoolean(R.styleable.ContributionsView_showMonths, true);
        mBackGroundColor = ta.getColor(R.styleable.ContributionsView_backgroundColor,
                0xD6E685
        ); //GitHub default color
        mTextColor = ta.getColor(R.styleable.ContributionsView_textColor, Color.BLACK);
        mTextSize = ta.getDimensionPixelSize(R.styleable.ContributionsView_textSize, 7);
        if(ta.getString(R.styleable.ContributionsView_username) != null && !isInEditMode()) {
            loadContributions(ta.getString(R.styleable.ContributionsView_username));
        }
    }

    public void loadContributions(String user) {
        new ContributionsLoader(this).beginRequest(getContext(), user);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.getClipBounds(mRect);

        final int w = mRect.width();
        final int h = mRect.height();

        final int hnum = mContributions.size() == 0 ? 52 : (int) Math
                .ceil(mContributions.size() / 7d); //The number of columns to show horizontally

        final float bd = (w / (float) hnum) * 0.9f; //The dimension of a single block
        final float m = (w / (float) hnum) - bd; //The margin around a block

        final float mth = mShouldDisplayMonths ? mTextSize : 0; //Height of month text

        //Draw the background
        mDayPainter.setColor(mBackGroundColor);
        canvas.drawRect(0, (2 * mth), w, h + mth, mDayPainter);
        float x = 0;
        if(mContributions.size() > 0) {
            int dow = getDayOfWeek(mContributions.get(0).date) - 1;
            float y = (dow * (bd + m)) + 2 * mth;
            for(ContributionsLoader.ContributionsDay d : mContributions) {
                mDayPainter.setColor(d.color);
                canvas.drawRect(x, y, x + bd, y + bd, mDayPainter);
                dow = getDayOfWeek(d.date) - 1;
                if(dow == 6) { //We just drew the last day of the week
                    x += bd + m;
                    y = 2 * mth;
                } else {
                    y += bd + m;
                }

            }
        } else {
            int dow = 0;
            float y = 2 * mth;
            mDayPainter.setColor(0xffeeeeee);
            for(int i = 0; i < 364; i++) {
                canvas.drawRect(x, y, x + bd, y + bd, mDayPainter);
                if(dow == 6) { //We just drew the last day of the week
                    x += bd + m;
                    y = 2 * mth;
                    dow = 0;
                } else {
                    y += bd + m;
                    dow++;
                }
            }
        }
        if(mShouldDisplayMonths) {
            mTextPainter.setColor(mTextColor);
            mTextPainter.setTextSize(mth);
            mCalendar.setTimeInMillis(System.currentTimeMillis());
            mCalendar.add(Calendar.MONTH, -12);
            x = 0;
            for(int i = 0; i < 12; i++) {
                final String month = getMonthName(mCalendar.getTimeInMillis());
                mTextPainter.getTextBounds(month, 0, month.length(), mTextBounds);
                if(w > x + mTextBounds.width()) {
                    canvas.drawText(
                            month,
                            x,
                            mth,
                            mTextPainter
                    );
                } else {
                    break;
                }
                mCalendar.add(Calendar.MONTH, 1);
                x += w / 12;
            }
        }
        final ViewGroup.LayoutParams lp = getLayoutParams();
        lp.height = h;
        setLayoutParams(lp);
    }

    private int getDayOfWeek(long stamp) {
        mCalendar.setTimeInMillis(stamp);
        //Day of week is indexed 1 to 7
        return mCalendar.get(Calendar.DAY_OF_WEEK);
    }

    private static final SimpleDateFormat month = new SimpleDateFormat("MMM");

    private String getMonthName(long stamp) {
        return month.format(stamp);
    }

    @Override
    public void onResponse(ArrayList<ContributionsLoader.ContributionsDay> contributions) {
        mContributions = contributions;
        invalidate();
        if(mListener != null && mListener.get() != null) {
            mListener.get().contributionsLoaded(contributions);
        }
    }

    @Override
    public void onError(VolleyError error) {

    }

    public void setListener(ContributionsLoadListener listener) {
        mListener = new WeakReference<>(listener);
    }


    public interface ContributionsLoadListener {

        void contributionsLoaded(List<ContributionsLoader.ContributionsDay> contributions);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable ss = super.onSaveInstanceState();
        final ContributionsState state = new ContributionsState(ss);
        state.contributions = mContributions.toArray(new ContributionsLoader.ContributionsDay[0]);
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);

        if(!(state instanceof ContributionsState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        final ContributionsState cs = (ContributionsState) state;
        super.onRestoreInstanceState(cs.getSuperState());
        this.mContributions = new ArrayList<>(Arrays.asList(cs.contributions));
        invalidate();
    }

    private static class ContributionsState extends BaseSavedState {
        ContributionsLoader.ContributionsDay[] contributions;

        ContributionsState(Parcel source) {
            super(source);
            this.contributions = source.createTypedArray(ContributionsLoader.ContributionsDay.CREATOR);
        }

        ContributionsState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeTypedArray(contributions, flags);
        }

        public static final Parcelable.Creator<ContributionsState> CREATOR =
                new Parcelable.Creator<ContributionsState>() {
                    public ContributionsState createFromParcel(Parcel in) {
                        return new ContributionsState(in);
                    }

                    public ContributionsState[] newArray(int size) {
                        return new ContributionsState[size];
                    }
                };
    }

}


