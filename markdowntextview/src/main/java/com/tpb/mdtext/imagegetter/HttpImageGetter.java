/*
 * Copyright (C) 2014-2016 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2013 Antarix Tandon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tpb.mdtext.imagegetter;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.Html.ImageGetter;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HttpImageGetter implements ImageGetter {

    private static final HashMap<String, Pair<Drawable, Long>> cache = new HashMap<>();

    private final TextView mContainer;

    public HttpImageGetter(TextView container) {
        this.mContainer = container;
    }


    public Drawable getDrawable(String source) {
        final URLDrawable ud = new URLDrawable();
        new ImageGetterAsyncTask(ud, this, mContainer).execute(source);
        return ud;
    }

    private static class ImageGetterAsyncTask extends AsyncTask<String, Void, Drawable> {
        private final WeakReference<URLDrawable> mDrawableReference;
        private final WeakReference<HttpImageGetter> mGetterReference;
        private final WeakReference<View> mContainerReference;
        private final WeakReference<Resources> mResources;
        private String mSource;
        private float mScale;

        ImageGetterAsyncTask(URLDrawable d, HttpImageGetter imageGetter, View container) {
            this.mDrawableReference = new WeakReference<>(d);
            this.mGetterReference = new WeakReference<>(imageGetter);
            this.mContainerReference = new WeakReference<>(container);
            this.mResources = new WeakReference<>(container.getResources());
        }

        @Override
        protected Drawable doInBackground(String... params) {
            mSource = params[0];
            synchronized(cache) {
                Map.Entry<String, Pair<Drawable, Long>> entry;
                final Iterator<Map.Entry<String, Pair<Drawable, Long>>> it = cache.entrySet().iterator();
                for(; it.hasNext(); ) {
                    entry = it.next();
                    if(System.currentTimeMillis() > entry.getValue().second + 60000) {
                        it.remove();
                    }
                }

                if(cache.containsKey(mSource)) {
                    if(System.currentTimeMillis() > cache.get(mSource).second + 45000) {
                        // The drawable is still being accessed, so we update it
                        fetchDrawable(mResources.get(), mSource);
                    }
                    return cache.get(mSource).first.getConstantState().newDrawable();
                } else if(mResources.get() != null) {
                    return fetchDrawable(mResources.get(), mSource);
                }
            }


            return null;
        }

        @Override
        protected void onPostExecute(Drawable result) {
            final URLDrawable urlDrawable = mDrawableReference.get();
            final HttpImageGetter imageGetter = mGetterReference.get();
            // We exist outside of the lifespan of the view
            if(result == null || urlDrawable == null || imageGetter == null) return;


            // Scale is set here as drawable may be cached and view may have changed
            setDrawableScale(result);

            // Set the correct bound according to the result from HTTP call
            urlDrawable.setBounds(0, 0, (int) (result.getIntrinsicWidth() * mScale),
                    (int) (result.getIntrinsicHeight() * mScale)
            );

            // Change the reference of the current urlDrawable to the result from the HTTP call
            urlDrawable.mDrawable = result;

            // redraw the image by invalidating the container
            imageGetter.mContainer.invalidate();
            // re-set text to fix images overlapping text
            imageGetter.mContainer.setText(imageGetter.mContainer.getText());
        }

        @Nullable
        private Drawable fetchDrawable(Resources res, String urlString) {
            try {
                final InputStream is = fetch(urlString);
                final Drawable drawable = new BitmapDrawable(res, is);
                synchronized(cache) {
                    cache.put(mSource, Pair.create(drawable, System.currentTimeMillis()));
                }
                return drawable;
            } catch(Exception e) {
                return null;
            }
        }

        private void setDrawableScale(Drawable drawable) {
            mScale = getScale(drawable);
            drawable.setBounds(0, 0, (int) (drawable.getIntrinsicWidth() * mScale),
                    (int) (drawable.getIntrinsicHeight() * mScale)
            );
        }

        private float getScale(Drawable drawable) {
            final View container = mContainerReference.get();
            if(container == null) {
                return 1f;
            }
            float maxWidth = container.getWidth();
            float originalDrawableWidth = drawable.getIntrinsicWidth();
            return maxWidth / originalDrawableWidth;
        }

        @Nullable
        private InputStream fetch(String urlString) throws IOException {
            URL url;
            final HttpImageGetter imageGetter = mGetterReference.get();
            if(imageGetter == null) {
                return null;
            }
            url = URI.create(urlString).toURL();


            return (InputStream) url.getContent();
        }
    }

    @SuppressWarnings("deprecation")
    public class URLDrawable extends BitmapDrawable {
        Drawable mDrawable;

        @Override
        public void draw(Canvas canvas) {
            if(mDrawable != null) {
                mDrawable.draw(canvas);
            }
        }

        public Drawable getDrawable() {
            return mDrawable;
        }

    }
} 
