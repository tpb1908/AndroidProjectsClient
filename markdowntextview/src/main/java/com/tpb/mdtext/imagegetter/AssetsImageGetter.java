/*
 * Copyright (C) 2016 Daniel Passos <daniel@passos.me>
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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;

/**
 * Assets Image Getter
 * <p>
 * Load image from assets folder
 *
 * @author <a href="mailto:daniel@passos.me">Daniel Passos</a>
 */
class AssetsImageGetter implements Html.ImageGetter {

    private final Context context;

    public AssetsImageGetter(Context context) {
        this.context = context;
    }

    public AssetsImageGetter(TextView textView) {
        this.context = textView.getContext();
    }

    @Override
    public Drawable getDrawable(String source) {

        try {
            final InputStream inputStream = context.getAssets().open(source);
            final Drawable d = Drawable.createFromStream(inputStream, null);
            d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            return d;
        } catch(IOException e) {
            return null;
        }

    }

}
