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

package org.sufficientlysecure.htmltext.imagegetter;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.Html.ImageGetter;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.sufficientlysecure.htmltext.htmltextview.HtmlTextView;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HtmlHttpImageGetter implements ImageGetter {

    private static final HashMap<String, Pair<Drawable, Long>> cache = new HashMap<>();

    private final TextView container;
    private DrawableCacheHandler cacheHandler;
    private URI baseUri;
    private boolean matchParentWidth = true;

    public HtmlHttpImageGetter(TextView container, @Nullable DrawableCacheHandler cacheHandler) {
        this.container = container;
        this.cacheHandler = cacheHandler;
    }

    public HtmlHttpImageGetter(TextView container, String baseUrl, @Nullable DrawableCacheHandler cacheHandler) {
        this.container = container;
        this.cacheHandler = cacheHandler;
        if(baseUrl != null) {
            this.baseUri = URI.create(baseUrl);
        }
    }

    public HtmlHttpImageGetter(HtmlTextView textView, String baseUrl, boolean matchParentWidth) {
        this.container = textView;
        this.matchParentWidth = matchParentWidth;
        if(baseUrl != null) {
            this.baseUri = URI.create(baseUrl);
        }
    }

    public Drawable getDrawable(String source) {
        final UrlDrawable urlDrawable = new UrlDrawable();

        // get the actual source
        new ImageGetterAsyncTask(urlDrawable, this, container, matchParentWidth).execute(source);

        // return reference to URLDrawable which will asynchronously load the image specified in the src tag
        return urlDrawable;
    }

    /**
     * Static inner {@link AsyncTask} that keeps a {@link WeakReference} to the {@link UrlDrawable}
     * and {@link HtmlHttpImageGetter}.
     * <p/>
     * This way, if the AsyncTask has a longer life span than the UrlDrawable,
     * we won't leak the UrlDrawable or the HtmlRemoteImageGetter.
     */
    private static class ImageGetterAsyncTask extends AsyncTask<String, Void, Drawable> {
        private final WeakReference<UrlDrawable> drawableReference;
        private final WeakReference<HtmlHttpImageGetter> imageGetterReference;
        private final WeakReference<View> containerReference;
        private final WeakReference<Resources> resources;
        private String source;
        private final boolean matchParentWidth;
        private float scale;

        ImageGetterAsyncTask(UrlDrawable d, HtmlHttpImageGetter imageGetter, View container, boolean matchParentWidth) {
            this.drawableReference = new WeakReference<>(d);
            this.imageGetterReference = new WeakReference<>(imageGetter);
            this.containerReference = new WeakReference<>(container);
            this.resources = new WeakReference<>(container.getResources());
            this.matchParentWidth = matchParentWidth;
        }

        @Override
        protected Drawable doInBackground(String... params) {
            source = params[0];
            synchronized(cache) {
                Map.Entry<String, Pair<Drawable, Long>> entry;
                for(Iterator<Map.Entry<String, Pair<Drawable, Long>>> it = cache.entrySet()
                                                                                .iterator(); it
                            .hasNext(); ) {
                    entry = it.next();
                    if(System.currentTimeMillis() > entry.getValue().second + 60000) {
                        it.remove();
                    }
                }

                if(cache.containsKey(source)) {
                    if(System.currentTimeMillis() > cache.get(source).second + 30000) {
                        // The drawable is still being accesses, so we update it
                        fetchDrawable(resources.get(), source);
                    }
                    return cache.get(source).first.getConstantState().newDrawable();
                }
            }

            if(resources.get() != null) {
                return fetchDrawable(resources.get(), source);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Drawable result) {
            if(result == null) {
                Log.w(HtmlTextView.TAG, "Drawable result is null! (source: " + source + ")");
                return;
            }
            final UrlDrawable urlDrawable = drawableReference.get();
            if(urlDrawable == null) { // We exist outside of the lifespan of the view
                return;
            }
            // Scale is set here as drawable may be cached and view may have changed
            setDrawableScale(result);

            // set the correct bound according to the result from HTTP call
            urlDrawable.setBounds(0, 0, (int) (result.getIntrinsicWidth() * scale),
                    (int) (result.getIntrinsicHeight() * scale)
            );

            // change the reference of the current drawable to the result from the HTTP call
            urlDrawable.drawable = result;
            final HtmlHttpImageGetter imageGetter = imageGetterReference.get();
            if(imageGetter == null) {
                return;
            }

            //We add the drawable to the image view so that it can get it on click
            if(imageGetter.cacheHandler != null) {
                imageGetter.cacheHandler.drawableLoaded(
                        urlDrawable.drawable.getConstantState().newDrawable(),
                        source
                );
            }

            // redraw the image by invalidating the container
            imageGetter.container.invalidate();
            // re-set text to fix images overlapping text
            imageGetter.container.setText(imageGetter.container.getText());
        }

        /**
         * Get the Drawable from URL
         */
        Drawable fetchDrawable(Resources res, String urlString) {
            try {
                InputStream is = fetch(urlString);
                final Drawable drawable = new BitmapDrawable(res, is);
                synchronized(cache) {
                    cache.put(source, new Pair<>(drawable, System.currentTimeMillis()));
                }
                return drawable;
            } catch(Exception e) {
                return null;
            }
        }

        private void setDrawableScale(Drawable drawable) {
            scale = getScale(drawable);
            drawable.setBounds(0, 0, (int) (drawable.getIntrinsicWidth() * scale),
                    (int) (drawable.getIntrinsicHeight() * scale)
            );
        }

        private float getScale(Drawable drawable) {
            final View container = containerReference.get();
            if(!matchParentWidth || container == null) {
                return 1f;
            }
            float maxWidth = container.getWidth();
            float originalDrawableWidth = drawable.getIntrinsicWidth();
            return maxWidth / originalDrawableWidth;
        }

        private InputStream fetch(String urlString) throws IOException {
            URL url;
            final HtmlHttpImageGetter imageGetter = imageGetterReference.get();
            if(imageGetter == null) {
                return null;
            }
            if(imageGetter.baseUri != null) {
                url = imageGetter.baseUri.resolve(urlString).toURL();
            } else {
                url = URI.create(urlString).toURL();
            }

            return (InputStream) url.getContent();
        }
    }

    public interface DrawableCacheHandler {

        void drawableLoaded(Drawable d, String source);

    }

    @SuppressWarnings("deprecation")
    private class UrlDrawable extends BitmapDrawable {
        Drawable drawable;

        @Override
        public void draw(Canvas canvas) {
            // override the draw to facilitate refresh function later
            if(drawable != null) {
                drawable.draw(canvas);
            }
        }


    }
} 
