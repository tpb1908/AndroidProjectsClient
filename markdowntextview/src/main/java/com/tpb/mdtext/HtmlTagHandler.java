/*
 * Copyright (C) 2013-2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2013-2015 Juha Kuitunen
 * Copyright (C) 2013 Mohammed Lakkadshaw
 * Copyright (C) 2007 The Android Open Source Project
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

package com.tpb.mdtext;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.util.Pair;
import android.widget.TextView;

import com.tpb.mdtext.handlers.CodeClickHandler;
import com.tpb.mdtext.handlers.LinkClickHandler;
import com.tpb.mdtext.mdtextview.MarkdownTextView;
import com.tpb.mdtext.spans.CleanURLSpan;
import com.tpb.mdtext.spans.ClickableTableSpan;
import com.tpb.mdtext.spans.CodeSpan;
import com.tpb.mdtext.spans.DrawTableLinkSpan;
import com.tpb.mdtext.spans.HorizontalRuleSpan;
import com.tpb.mdtext.spans.InlineCodeSpan;
import com.tpb.mdtext.spans.NumberSpan;
import com.tpb.mdtext.spans.QuoteSpan;
import com.tpb.mdtext.spans.RoundedBackgroundEndSpan;
import org.xml.sax.XMLReader;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Pattern;

/**
 * Some parts of this code are based on android.text.Html
 */
public class HtmlTagHandler implements Html.TagHandler {
    private static final String TAG = HtmlTagHandler.class.getSimpleName();

    private static final String UNORDERED_LIST_TAG = "ESCAPED_UL_TAG";
    private static final String ORDERED_LIST_TAG = "ESCAPED_OL_TAG";
    private static final String LIST_ITEM_TAG = "ESCAPED_LI_TAG";
    private static final String BLOCKQUOTE_TAG = "ESCAPED_BLOCKQUOTE_TAG";
    private static final String A_TAG = "ESCAPED_A_TAG";
    private static final String FONT_TAG = "ESCAPED_FONT_TAG";

    private static final Map<String, String> ESCAPE_MAP = new HashMap<>();

    static {
        ESCAPE_MAP.put("<ul", "<" + UNORDERED_LIST_TAG);
        ESCAPE_MAP.put("</ul>", "</" + UNORDERED_LIST_TAG + ">");
        ESCAPE_MAP.put("<ol", "<" + ORDERED_LIST_TAG);
        ESCAPE_MAP.put("</ol>", "</" + ORDERED_LIST_TAG + ">");
        ESCAPE_MAP.put("<li", "<" + LIST_ITEM_TAG);
        ESCAPE_MAP.put("</li>", "</" + LIST_ITEM_TAG + ">");
        ESCAPE_MAP.put("<blockquote>", "<" + BLOCKQUOTE_TAG + ">");
        ESCAPE_MAP.put("</blockquote>", "</" + BLOCKQUOTE_TAG + ">");
        ESCAPE_MAP.put("<a", "<" + A_TAG);
        ESCAPE_MAP.put("</a>", "</" + A_TAG + ">");
        ESCAPE_MAP.put("<font", "<" + FONT_TAG);
        ESCAPE_MAP.put("</font>", "</" + FONT_TAG + ">");
    }

    private static final Pattern ESCAPE_PATTERN = StringUtils.generatePattern(ESCAPE_MAP.keySet());

    /**
     * Newer versions of the Android SDK's {@link Html.TagHandler} handles &lt;ul&gt; and &lt;li&gt;
     * tags itself which means they never get delegated to this class. We want to handle the tags
     * ourselves so before passing the string html into Html.fromHtml(), we can use this method to
     * replace the &lt;ul&gt; and &lt;li&gt; tags with tags of our own.
     *
     * @param html String containing HTML, for example: "<b>Hello world!</b>"
     * @return html with replaced <ul> and <li> tags
     * @see <a href="https://github.com/android/platform_frameworks_base/commit/8b36c0bbd1503c61c111feac939193c47f812190">Specific Android SDK Commit</a>
     */
    public String overrideTags(@Nullable String html) {
        return StringUtils.replace(html, ESCAPE_MAP, ESCAPE_PATTERN);
    }

    /**
     * Keeps track of lists (ol, ul). On bottom of Stack is the outermost list
     * and on top of Stack is the most nested list
     */
    private final Stack<Pair<String, Boolean>> lists = new Stack<>();
    /**
     * Tracks indexes of ordered lists so that after a nested list ends
     * we can continue with correct index of outer list
     */
    private final Stack<Integer> olNextIndex = new Stack<>();

    private StringBuilder tableHtmlBuilder = new StringBuilder();
    /**
     * Tells us which level of table tag we're on; ultimately used to find the root table tag.
     */
    private int tableTagLevel = 0;

    private static final int indent = 10;
    private static final int listItemIndent = indent * 2;
    private static final BulletSpan bullet = new BulletSpan(indent);
    private ClickableTableSpan clickableTableSpan;
    private DrawTableLinkSpan drawTableLinkSpan;
    private final TextPaint mTextPaint;
    private LinkClickHandler mLinkHandler;
    private CodeClickHandler mCodeHandler;
    private Context mContext;

    public HtmlTagHandler(TextView tv, @Nullable LinkClickHandler linkHandler, @Nullable CodeClickHandler codeHandler) {
        mContext = tv.getContext();
        mTextPaint = tv.getPaint();
        mLinkHandler = linkHandler;
        mCodeHandler = codeHandler;
    }


    @Override
    public void handleTag(final boolean opening, final String tag, Editable output, final XMLReader xmlReader) {
        if(opening) {
            // opening tag
            if(MarkdownTextView.DEBUG) {
                Log.d(MarkdownTextView.TAG, "opening, output: " + output.toString());
            }

            if(tag.equalsIgnoreCase(UNORDERED_LIST_TAG)) {
                lists.push(
                        Pair.create(
                                tag,
                                safelyParseBoolean(getAttribute("bulleted", xmlReader, "true"),
                                        true
                                )
                        )
                );
            } else if(tag.equalsIgnoreCase(ORDERED_LIST_TAG)) {
                lists.push(
                        Pair.create(
                                tag,
                                safelyParseBoolean(getAttribute("numbered", xmlReader, "true"),
                                        true
                                )
                        )
                );
                olNextIndex.push(1);
            } else if(tag.equalsIgnoreCase(LIST_ITEM_TAG)) {
                if(output.length() > 0 && output.charAt(output.length() - 1) != '\n') {
                    output.append("\n");
                }
                if(!lists.isEmpty()) {
                    String parentList = lists.peek().first;
                    if(parentList.equalsIgnoreCase(ORDERED_LIST_TAG)) {
                        start(output, new Ol());
                        olNextIndex.push(olNextIndex.pop() + 1);
                    } else if(parentList.equalsIgnoreCase(UNORDERED_LIST_TAG)) {
                        start(output, new Ul());
                    }
                } else {
                    start(output, new Ol());
                    if(olNextIndex.isEmpty()) {
                        olNextIndex.push(0);
                    }
                    olNextIndex.push(olNextIndex.pop() + 1);
                }
            } else if(tag.equalsIgnoreCase("code")) {
                start(output, new Code());
            } else if(tag.equalsIgnoreCase("center")) {
                start(output, new Center());
            } else if(tag.equalsIgnoreCase("s") || tag.equalsIgnoreCase("strike")) {
                start(output, new Strike());
            } else if(tag.equalsIgnoreCase("table")) {
                start(output, new Table());
                if(tableTagLevel == 0) {
                    tableHtmlBuilder = new StringBuilder();
                    // We need some text for the table to be replaced by the span because
                    // the other tags will remove their text when their text is extracted
                    output.append("table placeholder");
                }

                tableTagLevel++;
            } else if(tag.equalsIgnoreCase("tr")) {
                start(output, new Tr());
            } else if(tag.equalsIgnoreCase("th")) {
                start(output, new Th());
            } else if(tag.equalsIgnoreCase("td")) {
                start(output, new Td());
            } else if(tag.equalsIgnoreCase("hr")) {
                start(output, new HorizontalRule());
            } else if(tag.equalsIgnoreCase(BLOCKQUOTE_TAG)) {
                start(output, new BlockQuote());
            } else if(tag.equalsIgnoreCase(A_TAG)) {
                start(output, new A(getAttribute("href", xmlReader, "invalid_url")));
            } else if(tag.equalsIgnoreCase("inlinecode")) {
                start(output, new InlineCode());
            } else if(tag.equalsIgnoreCase(FONT_TAG)) {
                final String font = getAttribute("face", xmlReader, "");
                final String fgColor = getAttribute("color", xmlReader, "");
                final String bgColor = getAttribute("bgcolor", xmlReader, "");
                final boolean rounded = safelyParseBoolean(getAttribute("rounded", xmlReader, ""),
                        false
                );
                if(font != null && !font.isEmpty()) {
                    start(output, new Font(font));
                }
                if(fgColor != null && !fgColor.isEmpty()) {
                    start(output, new ForegroundColor(fgColor));
                }
                if(bgColor != null && !bgColor.isEmpty()) {
                    start(output, new BackgroundColor(bgColor, rounded));
                }
            }
        } else {
            // closing tag
            if(MarkdownTextView.DEBUG) {
                Log.d(MarkdownTextView.TAG, "closing, output: " + output.toString());
            }

            if(tag.equalsIgnoreCase(UNORDERED_LIST_TAG)) {
                lists.pop();
            } else if(tag.equalsIgnoreCase(ORDERED_LIST_TAG)) {
                lists.pop();
                olNextIndex.pop();
            } else if(tag.equalsIgnoreCase(LIST_ITEM_TAG)) {
                if(!lists.isEmpty()) {
                    if(lists.peek().first.equalsIgnoreCase(UNORDERED_LIST_TAG)) {
                        if(output.length() > 0 && output.charAt(output.length() - 1) != '\n') {
                            output.append("\n");
                        }

                        if(lists.peek().second) {
                            // Nested BulletSpans increases distance between bullet and text, so we must prevent it.
                            int bulletMargin = indent;
                            if(lists.size() > 1) {
                                bulletMargin = indent - bullet.getLeadingMargin(true);
                                if(lists.size() > 2) {
                                    // This gets more complicated when we add a LeadingMarginSpan into the same line:
                                    // we have also counter it's effect to BulletSpan
                                    bulletMargin -= (lists.size() - 2) * listItemIndent;
                                }
                            }
                            end(output, Ul.class, false,
                                    new LeadingMarginSpan.Standard(
                                            listItemIndent * (lists.size() - 1)),
                                    new BulletSpan(bulletMargin)
                            );
                        } else {
                            end(output, Ul.class, false,
                                    new LeadingMarginSpan.Standard(
                                            listItemIndent * (lists.size() - 1)),
                                    null
                            );
                        }


                    } else if(lists.peek().first.equalsIgnoreCase(ORDERED_LIST_TAG)) {
                        if(output.length() > 0 && output.charAt(output.length() - 1) != '\n') {
                            output.append("\n");
                        }
                        int numberMargin = listItemIndent * (lists.size() - 1);
                        if(lists.size() > 2) {
                            // Same as in ordered lists: counter the effect of nested Spans
                            numberMargin -= (lists.size() - 2) * listItemIndent;
                        }
                        if(lists.peek().second) {
                            end(output, Ol.class, false,
                                    new LeadingMarginSpan.Standard(numberMargin),
                                    new NumberSpan(mTextPaint, olNextIndex.lastElement() - 1)
                            );
                        } else {
                            end(output, Ol.class, false,
                                    new LeadingMarginSpan.Standard(numberMargin),
                                    null
                            );
                        }


                    }
                } else {
                    end(output, Ol.class, true, new LeadingMarginSpan.Standard(1));
                }
            } else if(tag.equalsIgnoreCase("code")) {
                Object obj = getLast(output, Code.class);
                // start of the tag
                int where = output.getSpanStart(obj);
                // end of the tag
                int len = output.length();
                if(len > where + 1) {
                    output.removeSpan(obj);
                    final char[] chars = new char[len - where];
                    output.getChars(where, len, chars, 0);
                    output.insert(where, "\n"); // Another line for our CodeSpan to cover
                    output.replace(where + 1, len, " ");
                    final CodeSpan code = new CodeSpan(new String(chars), mCodeHandler);
                    output.setSpan(code, where, where + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    output.setSpan(new CodeSpan.ClickableCodeSpan(code), where, where + 3,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                }
            } else if(tag.equals("hr")) {
                final Object obj = getLast(output, HorizontalRule.class);
                final int where = output.getSpanStart(obj);
                output.removeSpan(obj); //Remove the old span
                output.replace(where, output.length(), " "); //We need a non-empty span
                output.setSpan(new HorizontalRuleSpan(), where, where + 1, 0); //Insert the bar span
            } else if(tag.equalsIgnoreCase("center")) {
                end(output, Center.class, true,
                        new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER)
                );
            } else if(tag.equalsIgnoreCase("s") || tag.equalsIgnoreCase("strike")) {
                end(output, Strike.class, false, new StrikethroughSpan());
            } else if(tag.equalsIgnoreCase("table")) {
                tableTagLevel--;

                // When we're back at the root-level table
                if(tableTagLevel == 0) {
                    final String tableHtml = tableHtmlBuilder.toString();

                    ClickableTableSpan myClickableTableSpan = null;
                    if(clickableTableSpan != null) {
                        myClickableTableSpan = clickableTableSpan.newInstance();
                        myClickableTableSpan.setTableHtml(tableHtml);
                    }

                    DrawTableLinkSpan myDrawTableLinkSpan = null;
                    if(drawTableLinkSpan != null) {
                        myDrawTableLinkSpan = drawTableLinkSpan.newInstance();
                    }

                    end(output, Table.class, false, myDrawTableLinkSpan, myClickableTableSpan);
                } else {
                    end(output, Table.class, false);
                }
            } else if(tag.equalsIgnoreCase("tr")) {
                end(output, Tr.class, false);
            } else if(tag.equalsIgnoreCase("th")) {
                end(output, Th.class, false);
            } else if(tag.equalsIgnoreCase("td")) {
                end(output, Td.class, false);
            } else if(tag.equalsIgnoreCase(BLOCKQUOTE_TAG)) {
                Object obj = getLast(output, BlockQuote.class);
                // start of the tag
                int where = output.getSpanStart(obj);
                // end of the tag
                int len = output.length();
                output.removeSpan(obj);
                output.setSpan(new QuoteSpan(), where, len, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            } else if(tag.equalsIgnoreCase(A_TAG)) {
                A obj = getLast(output, A.class);
                // start of the tag
                int where = output.getSpanStart(obj);
                // end of the tag
                int len = output.length();
                output.removeSpan(obj);
                if(isValidURL(obj.href)) {
                    output.setSpan(new CleanURLSpan(obj.href, mLinkHandler), where, len,
                            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                    );
                }
            } else if(tag.equalsIgnoreCase("inlinecode")) {
                final InlineCode obj = getLast(output, InlineCode.class);
                final int where = output.getSpanStart(obj);
                final int len = output.length();
                output.removeSpan(obj);
                output.setSpan(new InlineCodeSpan(mTextPaint.getTextSize()), where, len,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            } else if(tag.equalsIgnoreCase(FONT_TAG)) {
                final ForegroundColor fgc = getLast(output, ForegroundColor.class);
                final BackgroundColor bgc = getLast(output, BackgroundColor.class);
                final Font f = getLast(output, Font.class);
                if(fgc != null) {
                    final int where = output.getSpanStart(fgc);
                    final int len = output.length();
                    output.removeSpan(fgc);
                    output.setSpan(new ForegroundColorSpan(safelyParseColor(fgc.color)), where, len,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                }
                if(bgc != null) {
                    final int where = output.getSpanStart(bgc);
                    final int len = output.length();
                    output.removeSpan(bgc);

                    final int color = safelyParseColor(bgc.color);
                    if(bgc.rounded) {
                        output.insert(len, " ");
                        output.insert(where, " ");
                        output.setSpan(new RoundedBackgroundEndSpan(color, false), where, where + 1,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                        output.setSpan(new RoundedBackgroundEndSpan(color, true), len, len + 1,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                        output.setSpan(new BackgroundColorSpan(color), where + 1, len,
                                Spannable.SPAN_INCLUSIVE_INCLUSIVE
                        );
                    } else {
                        output.setSpan(new BackgroundColorSpan(color), where, len,
                                Spannable.SPAN_INCLUSIVE_INCLUSIVE
                        );
                    }

                }
                if(f != null) {
                    final int where = output.getSpanStart(f);
                    final int len = output.length();
                    output.removeSpan(f);
                    output.setSpan(new TypefaceSpan(f.face), where, len,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                }
            }
        }
        storeTableTags(opening, tag);
    }

    private static int safelyParseColor(String color) {
        try {
            return Color.parseColor(color);
        } catch(Exception e) {
            switch(color) {
                case "black":
                    return Color.BLACK;
                case "white":
                    return Color.WHITE;
                case "red":
                    return Color.RED;
                case "blue":
                    return Color.BLUE;
                case "green":
                    return Color.GREEN;
                case "grey":
                    return Color.GRAY;
                case "yellow":
                    return Color.YELLOW;
                case "aqua":
                    return Color.parseColor("#00FFFF");
                case "fuchsia":
                    return Color.parseColor("#FF00FF");
                case "lime":
                    return Color.parseColor("#00FF00");
                case "maroon":
                    return Color.parseColor("#800000");
                case "navy":
                    return Color.parseColor("#FF00FF");
                case "olive":
                    return Color.parseColor("#808000");
                case "purple":
                    return Color.parseColor("#800080");

                case "silver":
                    return Color.parseColor("#C0C0C0");
                case "teal":
                    return Color.parseColor("#008080");
                default:
                    return Color.WHITE;

            }
        }
    }

    private static Boolean safelyParseBoolean(String bool, boolean def) {
        try {
            return Boolean.valueOf(bool);
        } catch(Exception e) {
            return def;
        }
    }

    private static String getAttribute(@NonNull String attr, @NonNull XMLReader reader, String defaultAttr) {
        try {
            final Field elementField = reader.getClass().getDeclaredField("theNewElement");
            elementField.setAccessible(true);
            final Object element = elementField.get(reader);
            final Field attsField = element.getClass().getDeclaredField("theAtts");
            attsField.setAccessible(true);
            final Object atts = attsField.get(element);
            final Field dataField = atts.getClass().getDeclaredField("data");
            dataField.setAccessible(true);
            final String[] data = (String[]) dataField.get(atts);
            final Field lengthField = atts.getClass().getDeclaredField("length");
            lengthField.setAccessible(true);
            final int len = (Integer) lengthField.get(atts);
            for(int i = 0; i < len; i++) {
                if(attr.equals(data[i * 5 + 1])) {
                    return data[i * 5 + 4];
                }
            }

        } catch(Exception e) {
            Log.e(TAG, "handleTag: ", e);
        }
        return defaultAttr;
    }

    private static boolean isValidURL(String possible) {
        return URLPattern.AUTOLINK_WEB_URL.matcher(possible).matches();
    }

    /**
     * If we're arriving at a table tag or are already within a table tag, then we should store it
     * the raw HTML for our ClickableTableSpan
     */
    private void storeTableTags(boolean opening, String tag) {
        if(tableTagLevel > 0 || tag.equalsIgnoreCase("table")) {
            tableHtmlBuilder.append("<");
            if(!opening) {
                tableHtmlBuilder.append("/");
            }
            tableHtmlBuilder
                    .append(tag.toLowerCase())
                    .append(">");
        }
    }

    /**
     * Mark the opening tag by using private classes
     */
    private void start(Editable output, Object mark) {
        int len = output.length();
        output.setSpan(mark, len, len, Spannable.SPAN_MARK_MARK);
        if(MarkdownTextView.DEBUG) {
            Log.d(MarkdownTextView.TAG, "len: " + len);
        }
    }

    /**
     * Modified from {@link android.text.Html}
     */
    private void end(Editable output, Class kind, boolean paragraphStyle, Object... replaces) {
        Object obj = getLast(output, kind);
        // start of the tag
        int where = output.getSpanStart(obj);
        // end of the tag
        int len = output.length();

        // If we're in a table, then we need to store the raw HTML for later
        if(tableTagLevel > 0) {
            final CharSequence extractedSpanText = extractSpanText(output, kind);
            tableHtmlBuilder.append(extractedSpanText);
        }

        output.removeSpan(obj);

        if(where != len) {
            int thisLen = len;
            // paragraph styles like AlignmentSpan need to end with a new line!
            if(paragraphStyle) {
                output.append("\n");
                thisLen++;
            }
            for(Object replace : replaces) {
                output.setSpan(replace, where, thisLen, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            if(MarkdownTextView.DEBUG) {
                Log.d(MarkdownTextView.TAG, "where: " + where);
                Log.d(MarkdownTextView.TAG, "thisLen: " + thisLen);
            }
        }
    }

    /**
     * Returns the text contained within a span and deletes it from the output string
     */
    private CharSequence extractSpanText(Editable output, Class kind) {
        final Object obj = getLast(output, kind);
        // start of the tag
        final int where = output.getSpanStart(obj);
        // end of the tag
        final int len = output.length();

        final CharSequence extractedSpanText = output.subSequence(where, len);
        output.delete(where, len);
        return extractedSpanText;
    }

    /**
     * Get last marked position of a specific tag kind (private class)
     */
    private static <T> T getLast(Editable text, Class<T> kind) {
        final T[] objs = text.getSpans(0, text.length(), kind);
        if(objs.length == 0) {
            return null;
        } else {
            for(int i = objs.length; i > 0; i--) {
                if(text.getSpanFlags(objs[i - 1]) == Spannable.SPAN_MARK_MARK) {
                    return objs[i - 1];
                }
            }
            return null;
        }
    }


    public void setClickableTableSpan(ClickableTableSpan clickableTableSpan) {
        this.clickableTableSpan = clickableTableSpan;
    }

    public void setDrawTableLinkSpan(DrawTableLinkSpan drawTableLinkSpan) {
        this.drawTableLinkSpan = drawTableLinkSpan;
    }

    private static class Ul {

    }

    private static class Ol {
    }

    private static class Code {
    }

    private static class Center {
    }

    private static class Strike {
    }

    private static class Table {
    }

    private static class Tr {
    }

    private static class Th {
    }

    private static class Td {
    }

    private static class HorizontalRule {
    }

    private static class BlockQuote {
    }

    private static class InlineCode {
    }

    private static class A {

        String href;

        A(String href) {
            this.href = href;
        }

    }

    private static class ForegroundColor {
        String color;

        ForegroundColor(String color) {
            this.color = color;
        }

    }

    private static class BackgroundColor {
        String color;
        boolean rounded;

        BackgroundColor(String color, boolean rounded) {
            this.color = color;
            this.rounded = rounded;
        }
    }

    private static class Font {
        String face;

        Font(String face) {
            this.face = face;
        }
    }

}
