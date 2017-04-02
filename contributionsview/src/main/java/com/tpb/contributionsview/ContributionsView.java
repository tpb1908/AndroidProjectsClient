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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.VolleyError;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by theo on 12/01/17.
 */

public class ContributionsView extends View implements ContributionsLoader.ContributionsRequestListener {
    private static final String TAG = ContributionsView.class.getSimpleName();

    private boolean shouldDisplayMonths;
    private int textColor;
    private int textSize;
    private int backGroundColor;
    private ArrayList<ContributionsLoader.GitDay> contribs = new ArrayList<>();

    private Paint dayPainter;
    private Paint textPainter;

    private Rect rect;
    private final Rect textBounds = new Rect();
    private float gridY;

    private WeakReference<ContributionsLoadListener> listener;

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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private ContributionsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        rect = new Rect();

        final TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.ContributionsView, defStyleAttr, defStyleRes
        );
        useAttributes(attributes);

        textPainter = new Paint(Paint.ANTI_ALIAS_FLAG);
        dayPainter = new Paint(Paint.ANTI_ALIAS_FLAG);
        dayPainter.setStyle(Paint.Style.FILL);
    }

    private void useAttributes(TypedArray ta) {
        shouldDisplayMonths = ta.getBoolean(R.styleable.ContributionsView_showMonths, true);
        backGroundColor = ta.getColor(R.styleable.ContributionsView_backgroundColor,
                0xD6E685
        ); //GitHub default color
        textColor = ta.getColor(R.styleable.ContributionsView_textColor, Color.BLACK);
        textSize = ta.getDimensionPixelSize(R.styleable.ContributionsView_textSize, 7);
        if(ta.getString(R.styleable.ContributionsView_username) != null && !isInEditMode()) {
            loadUser(ta.getString(R.styleable.ContributionsView_username));
        }
    }

    public void loadUser(String user) {
        new ContributionsLoader(this).beginRequest(getContext(), user);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.getClipBounds(rect);

        final int w = rect.width();
        final int h = rect.height();

        final int hnum = contribs.size() == 0 ? 52 : (int) Math
                .ceil(contribs.size() / 7d); //The number of days to show horizontally

        final float bd = (w / (float) hnum) * 0.9f; //The dimension of a single block
        final float m = (w / (float) hnum) - bd; //The margin around a block

        final float tm = shouldDisplayMonths ? textSize : 0; //Top margin if we are displaying months
        final float mth = shouldDisplayMonths ? textSize : 0; //Height of month text

        //Draw the background
        dayPainter.setColor(backGroundColor);
        canvas.drawRect(0, (tm * mth), w, h + mth, dayPainter);

        textPainter.setColor(textColor);
        textPainter.setTextSize(mth);
        float x = 0;
        if(contribs.size() > 0) {
            int dow = getDayOfWeek(contribs.get(0).date) - 1;
            float y = (dow * (bd + m)) + tm + mth;
            gridY = y;
            for(ContributionsLoader.GitDay d : contribs) {
                dayPainter.setColor(d.color);
                canvas.drawRect(x, y, x + bd, y + bd, dayPainter);
                dow = getDayOfWeek(d.date) - 1;
                if(dow == 6) { //We just drew the last day of the week
                    x += bd + m;
                    y = tm + mth;
                } else {
                    y += bd + m;
                }

            }
        } else {
            int dow = 0;
            float y = tm + mth;
            gridY = y;
            dayPainter.setColor(Color.parseColor("#EEEEEE"));
            for(int i = 0; i < 364; i++) {
                canvas.drawRect(x, y, x + bd, y + bd, dayPainter);
                if(dow == 6) { //We just drew the last day of the week
                    x += bd + m;
                    y = tm + mth;
                    dow = 0;
                } else {
                    y += bd + m;
                    dow++;
                }
            }
        }
        if(shouldDisplayMonths) {
            cal.setTimeInMillis(System.currentTimeMillis());
            cal.add(Calendar.MONTH, -12);
            x = 0;
            for(int i = 0; i < 12; i++) {
                final String month = getMonthName(cal.getTimeInMillis());
                textPainter.getTextBounds(month, 0, month.length(), textBounds);
                if(w > x + textBounds.width()) {
                    canvas.drawText(
                            month,
                            x,
                            mth,
                            textPainter
                    );
                }
                cal.add(Calendar.MONTH, 1);
                x += w / 12;
            }
        }
        final ViewGroup.LayoutParams lp = getLayoutParams();
        lp.height = h;
        setLayoutParams(lp);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_UP) {
            final float cols = contribs.size() / 7f;
            final float pcr = event.getX() / rect.width();
            final int col = (int) (pcr * cols);
            if(event.getY() > textSize) {
                final float pcc = (event.getY() - gridY) / (rect.height() - gridY);
                final int row = (int) (pcc * 7);
                final int pos = (7 * col) + row;
                if(pos < contribs.size() && pos >= 0) {
                    final String date = new SimpleDateFormat("dd-MM-yyyy")
                            .format(new Date(contribs.get(pos).date));
                    Toast.makeText(getContext(), String.format("%1$d contributions on %2$s",
                            contribs.get(pos).contributions, date
                    ), Toast.LENGTH_SHORT).show();
                }
            }
        }
        return super.onTouchEvent(event);
    }

    private static final Calendar cal = Calendar.getInstance();

    private int getDayOfWeek(long stamp) {
        cal.setTimeInMillis(stamp);
        //Day of week is indexed 1 to 7
        return cal.get(Calendar.DAY_OF_WEEK);
    }

    private static final SimpleDateFormat month = new SimpleDateFormat("MMM");

    private String getMonthName(long stamp) {
        return month.format(stamp);
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    @Override
    public void onResponse(ArrayList<ContributionsLoader.GitDay> contributions) {
        contribs = contributions;
        invalidate();
        if(listener != null && listener.get() != null)
            listener.get().contributionsLoaded(contributions);
    }

    @Override
    public void onError(VolleyError error) {

    }

    public void setListener(ContributionsLoadListener listener) {
        this.listener = new WeakReference<>(listener);
    }

    public List<ContributionsLoader.GitDay> getContributions() {
        return contribs;
    }

    public interface ContributionsLoadListener {

        void contributionsLoaded(List<ContributionsLoader.GitDay> contributions);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable ss = super.onSaveInstanceState();
        final ContribState state = new ContribState(ss);
        state.contribs = contribs.toArray(new ContributionsLoader.GitDay[0]);
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);

        if(!(state instanceof ContribState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        final ContribState cs = (ContribState) state;
        super.onRestoreInstanceState(cs.getSuperState());
        this.contribs = new ArrayList<>(Arrays.asList(cs.contribs));
        invalidate();
    }

    private static class ContribState extends BaseSavedState {
        ContributionsLoader.GitDay[] contribs;

        ContribState(Parcel source) {
            super(source);
            this.contribs = source.createTypedArray(ContributionsLoader.GitDay.CREATOR);
        }

        ContribState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeTypedArray(contribs, flags);
        }

        public static final Parcelable.Creator<ContribState> CREATOR =
                new Parcelable.Creator<ContribState>() {
                    public ContribState createFromParcel(Parcel in) {
                        return new ContribState(in);
                    }

                    public ContribState[] newArray(int size) {
                        return new ContribState[size];
                    }
                };
    }

}
