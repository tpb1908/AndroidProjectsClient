package com.tpb.mdtext.views.spans;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.style.ReplacementSpan;

import com.tpb.mdtext.handlers.TableClickHandler;

import org.sufficientlysecure.htmltextview.R;

import java.lang.ref.WeakReference;

/**
 * Created by theo on 08/04/17.
 */

public class TableSpan extends ReplacementSpan implements WrappingClickableSpan.WrappedClickableSpan {

    private WeakReference<TableClickHandler> mHandler;
    private static String mTableString = "Table";
    private static Bitmap mTableBM;
    private PorterDuffColorFilter mBMFilter;
    private String mHtml;
    private int mBaseOffset = 7;

    public TableSpan(String html, TableClickHandler handler) {
        mHtml = html;
        mHandler = new WeakReference<>(handler);
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, @IntRange(from = 0) int start, @IntRange(from = 0) int end, @Nullable Paint.FontMetricsInt fm) {
        mBaseOffset = (int) paint.measureText("c");
        return 0;
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, @IntRange(from = 0) int start, @IntRange(from = 0) int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
        paint.setTextSize(paint.getTextSize() - 1);
        final int textHeight = paint.getFontMetricsInt().descent - paint.getFontMetricsInt().ascent;

        int offset = mBaseOffset;
        if(mTableBM != null) offset += mTableBM.getWidth();

        final int textStart = top + textHeight / 4;

        canvas.drawText(mTableString, x + mBaseOffset + offset, textStart, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(mBaseOffset / 4);
        canvas.drawRoundRect(new RectF(x, top + top - bottom, x + canvas.getWidth(), bottom), 7, 7,
                paint
        );

        if(mTableBM != null) {
            if(mBMFilter == null)
                mBMFilter = new PorterDuffColorFilter(paint.getColor(), PorterDuff.Mode.SRC_IN);
            paint.setColorFilter(mBMFilter);
            canvas.drawBitmap(mTableBM, x + mBaseOffset, textStart - textHeight, paint);
        }
    }


    public void onClick() {
        if(mHandler.get() != null) mHandler.get().onClick(mHtml);
    }

    public static void initialise(Context context) {
        final Drawable drawable = context.getResources().getDrawable(R.drawable.ic_table);
        final Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888
        );
        final Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        mTableBM = bitmap;
        mTableString = context.getString(R.string.table_span);
    }

    public static boolean isInitialised() {
        return mTableBM != null;
    }

}
