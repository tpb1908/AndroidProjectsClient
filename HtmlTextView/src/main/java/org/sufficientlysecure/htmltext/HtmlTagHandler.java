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

package org.sufficientlysecure.htmltext;

import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.AlignmentSpan;
import android.text.style.BulletSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StrikethroughSpan;
import android.util.Log;

import org.sufficientlysecure.htmltext.htmltextview.HtmlTextView;
import org.xml.sax.XMLReader;

import java.util.Stack;

/**
 * Some parts of this code are based on android.text.Html
 */
public class HtmlTagHandler implements Html.TagHandler {

    private static final String UNORDERED_LIST = "HTML_TEXTVIEW_ESCAPED_UL_TAG";
    private static final String ORDERED_LIST = "HTML_TEXTVIEW_ESCAPED_OL_TAG";
    private static final String LIST_ITEM = "HTML_TEXTVIEW_ESCAPED_LI_TAG";

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

        if(html == null) return null;

        html = html.replace("<ul", "<" + UNORDERED_LIST);
        html = html.replace("</ul>", "</" + UNORDERED_LIST + ">");
        html = html.replace("<ol", "<" + ORDERED_LIST);
        html = html.replace("</ol>", "</" + ORDERED_LIST + ">");
        html = html.replace("<li", "<" + LIST_ITEM);
        html = html.replace("</li>", "</" + LIST_ITEM + ">");

        return html;
    }

    /**
     * Keeps track of lists (ol, ul). On bottom of Stack is the outermost list
     * and on top of Stack is the most nested list
     */
    private final Stack<String> lists = new Stack<>();
    /**
     * Tracks indexes of ordered lists so that after a nested list ends
     * we can continue with correct index of outer list
     */
    private final Stack<Integer> olNextIndex = new Stack<>();
    /**
     * List indentation in pixels. Nested lists use multiple of this.
     */
    /**
     * Running HTML table string based off of the root table tag. Root table tag being the tag which
     * isn't embedded within any other table tag. Example:
     * <!-- This is the root level opening table tag. This is where we keep track of tables. -->
     * <table>
     * ...
     * <table> <!-- Non-root table tags -->
     * ...
     * </table>
     * ...
     * </table>
     * <!-- This is the root level closing table tag and the end of the string we track. -->
     */
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

    public HtmlTagHandler(TextPaint paint) {
        mTextPaint = paint;
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

    private static class Bar {}

    @Override
    public void handleTag(final boolean opening, final String tag, Editable output, final XMLReader xmlReader) {
        if(opening) {
            // opening tag
            if(HtmlTextView.DEBUG) {
                Log.d(HtmlTextView.TAG, "opening, output: " + output.toString());
            }

            if(tag.equalsIgnoreCase(UNORDERED_LIST)) {
                lists.push(tag);
            } else if(tag.equalsIgnoreCase(ORDERED_LIST)) {
                lists.push(tag);
                olNextIndex.push(1);
            } else if(tag.equalsIgnoreCase(LIST_ITEM)) {
                if(output.length() > 0 && output.charAt(output.length() - 1) != '\n') {
                    output.append("\n");
                }
                if(!lists.isEmpty()) {
                    String parentList = lists.peek();
                    if(parentList.equalsIgnoreCase(ORDERED_LIST)) {
                        start(output, new Ol());
                        output.append(olNextIndex.peek().toString()).append("•");
                        olNextIndex.push(olNextIndex.pop() + 1);
                    } else if(parentList.equalsIgnoreCase(UNORDERED_LIST)) {
                        start(output, new Ul());
                    }
                } else {
                    start(output, new Ol());
                    output.append("•");
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
            } else if(tag.equalsIgnoreCase("bar")) {
                start(output, new Bar());
            }
        } else {
            // closing tag
            if(HtmlTextView.DEBUG) {
                Log.d(HtmlTextView.TAG, "closing, output: " + output.toString());
            }

            if(tag.equalsIgnoreCase(UNORDERED_LIST)) {
                lists.pop();
            } else if(tag.equalsIgnoreCase(ORDERED_LIST)) {
                lists.pop();
                olNextIndex.pop();
            } else if(tag.equalsIgnoreCase(LIST_ITEM)) {
                if(!lists.isEmpty()) {
                    if(lists.peek().equalsIgnoreCase(UNORDERED_LIST)) {
                        if(lists.peek().equalsIgnoreCase(UNORDERED_LIST)) {
                            if(output.length() > 0 && output.charAt(output.length() - 1) != '\n') {
                                output.append("\n");
                            }
                            // Nested BulletSpans increases distance between bullet and text, so we must prevent it.
                            int bulletMargin = indent;
                            if(lists.size() > 1) {
                                bulletMargin = indent - bullet.getLeadingMargin(true);
                                if(lists.size() > 2) {
                                    // This get's more complicated when we add a LeadingMarginSpan into the same line:
                                    // we have also counter it's effect to BulletSpan
                                    bulletMargin -= (lists.size() - 2) * listItemIndent;
                                }
                            }
                            BulletSpan newBullet = new BulletSpan(bulletMargin);
                            end(output, Ul.class, false,
                                    new LeadingMarginSpan.Standard(listItemIndent * (lists.size() - 1)),
                                    newBullet);
                        } else if(lists.peek().equalsIgnoreCase(ORDERED_LIST)) {
                            if(output.length() > 0 && output.charAt(output.length() - 1) != '\n') {
                                output.append("\n");
                            }
                            int numberMargin = listItemIndent * (lists.size() - 1);
                            if(lists.size() > 2) {
                                // Same as in ordered lists: counter the effect of nested Spans
                                numberMargin -= (lists.size() - 2) * listItemIndent;
                            }
                            NumberSpan numberSpan = new NumberSpan(mTextPaint, olNextIndex.lastElement() - 1);
                            end(output, Ol.class, false,
                                    new LeadingMarginSpan.Standard(numberMargin),
                                    numberSpan);
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

                output.removeSpan(obj);

                output.replace(where, len, "Click to view code");
                output.setSpan(new CodeSpan(), where, where + "Click to view code".length(), 0);

            }  else if(tag.equals("bar")) {
                final Object obj = getLast(output, Bar.class);
                final int where = output.getSpanStart(obj);
                output.removeSpan(obj); //Remove the old span
                output.replace(where, output.length(), " "); //We need a non-empty span
                output.setSpan(new BarSpan(), where, where + 1, 0); //Insert the bar span
            } else if(tag.equalsIgnoreCase("center")) {
                end(output, Center.class, true, new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER));
            } else if(tag.equalsIgnoreCase("s") || tag.equalsIgnoreCase("strike")) {
                Log.i(HtmlTagHandler.class.getSimpleName(), "handleTag: Ending strikethrough span");
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
            }
        }

        storeTableTags(opening, tag);
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
        if(HtmlTextView.DEBUG) {
            Log.d(HtmlTextView.TAG, "len: " + len);
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

            if(HtmlTextView.DEBUG) {
                Log.d(HtmlTextView.TAG, "where: " + where);
                Log.d(HtmlTextView.TAG, "thisLen: " + thisLen);
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
    private static Object getLast(Editable text, Class kind) {
        Object[] objs = text.getSpans(0, text.length(), kind);
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
} 
