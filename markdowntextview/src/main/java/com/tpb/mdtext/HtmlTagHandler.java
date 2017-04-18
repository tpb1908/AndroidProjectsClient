package com.tpb.mdtext;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
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
import android.widget.TextView;

import com.tpb.mdtext.handlers.CodeClickHandler;
import com.tpb.mdtext.handlers.LinkClickHandler;
import com.tpb.mdtext.handlers.TableClickHandler;
import com.tpb.mdtext.views.spans.CleanURLSpan;
import com.tpb.mdtext.views.spans.CodeSpan;
import com.tpb.mdtext.views.spans.HorizontalRuleSpan;
import com.tpb.mdtext.views.spans.InlineCodeSpan;
import com.tpb.mdtext.views.spans.ListNumberSpan;
import com.tpb.mdtext.views.spans.QuoteSpan;
import com.tpb.mdtext.views.spans.RoundedBackgroundEndSpan;
import com.tpb.mdtext.views.spans.TableSpan;
import com.tpb.mdtext.views.spans.WrappingClickableSpan;

import org.xml.sax.XMLReader;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Pattern;

import static com.tpb.mdtext.TextUtils.isValidURL;

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

    private static final Pattern ESCAPE_PATTERN = TextUtils.generatePattern(ESCAPE_MAP.keySet());

    /**
     * Android captures some tags before they get here, so we escape them
     */
    public String overrideTags(@Nullable String html) {
        return TextUtils.replace(html, ESCAPE_MAP, ESCAPE_PATTERN);
    }

    /**
     * Stack of nested list tags, bulleted flag and list type (For OL)
     */
    private final Stack<Triple<String, Boolean, ListNumberSpan.ListType>> mLists = new Stack<>();
    /**
     * Tracks indexes of ordered lists so that after a nested list ends
     * we can continue with correct index of outer list
     */
    private final Stack<Pair<Integer, ListNumberSpan.ListType>> mOlIndices = new Stack<>();

    private StringBuilder mTableHtmlBuilder = new StringBuilder();

    private int mTableLevel = 0;

    private static int mSingleIndent = 10;
    private static final int mListIndent = mSingleIndent * 2;
    private final TextPaint mTextPaint;
    private LinkClickHandler mLinkHandler;
    private CodeClickHandler mCodeHandler;
    private TableClickHandler mTableHandler;

    public HtmlTagHandler(TextView tv, @Nullable LinkClickHandler linkHandler, @Nullable CodeClickHandler codeHandler, @Nullable TableClickHandler tableHandler) {
        mTextPaint = tv.getPaint();
        mSingleIndent = (int) mTextPaint.measureText("t");
        mLinkHandler = linkHandler;
        mCodeHandler = codeHandler;
        mTableHandler = tableHandler;
    }

    @Override
    public void handleTag(final boolean opening, final String tag, Editable output, final XMLReader xmlReader) {
        if(opening) {
            handleOpeningTag(tag, output, xmlReader);
        } else {
            handleClosingTag(tag, output);
        }
        storeTableTags(opening, tag);
    }

    private void handleOpeningTag(final String tag, Editable output, final XMLReader xmlReader) {
        switch(tag.toUpperCase()) {
            case UNORDERED_LIST_TAG:
                mLists.push(
                        new Triple<>(
                                tag,
                                safelyParseBoolean(getAttribute("bulleted", xmlReader, "true"),
                                        true
                                ),
                                ListNumberSpan.ListType.NUMBER
                        )
                );
                break;
            case ORDERED_LIST_TAG:
                final ListNumberSpan.ListType type = ListNumberSpan.ListType
                        .fromString(getAttribute("type", xmlReader, ""));
                mLists.push(
                        new Triple<>(
                                tag,
                                safelyParseBoolean(getAttribute("numbered", xmlReader, "true"),
                                        true
                                ),
                                type
                        )
                );
                mOlIndices.push(Pair.create(1, type));
                break;
            case LIST_ITEM_TAG:
                if(output.length() > 0 && output.charAt(output.length() - 1) != '\n') {
                    output.append("\n");
                }
                if(!mLists.isEmpty()) {
                    String parentList = mLists.peek().first;
                    if(parentList.equalsIgnoreCase(ORDERED_LIST_TAG)) {
                        start(output, new Ol());
                        mOlIndices.push(Pair.create(mOlIndices.pop().first + 1, mLists.peek().third));
                    } else if(parentList.equalsIgnoreCase(UNORDERED_LIST_TAG)) {
                        start(output, new Ul());
                    }
                } else {
                    start(output, new Ol());
                    mOlIndices.push(Pair.create(1, ListNumberSpan.ListType.NUMBER));
                }
                break;
            case "TABLE":
                start(output, new Table());
                if(mTableLevel == 0) {
                    mTableHtmlBuilder = new StringBuilder();
                }
                mTableLevel++;
                break;
            case FONT_TAG:
                final String font = getAttribute("face", xmlReader, "");
                final String fgColor = getAttribute("color", xmlReader, "");
                final String bgColor = getAttribute("background-color", xmlReader, "");
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
                break;
            case "CODE":
                start(output, new Code());
                break;
            case "CENTER":
                start(output, new Center());
                break;
            case "S":
            case "STRIKE":
                start(output, new StrikeThrough());
                break;
            case "TR":
                start(output, new Tr());
                break;
            case "TH":
                start(output, new Th());
                break;
            case "TD":
                start(output, new Td());
                break;
            case "HR":
                start(output, new HorizontalRule());
                break;
            case BLOCKQUOTE_TAG:
                start(output, new BlockQuote());
                break;
            case A_TAG:
                start(output, new A(getAttribute("href", xmlReader, "invalid_url")));
                break;
            case "INLINECODE":
                start(output, new InlineCode());
                break;

        }
    }

    private void handleClosingTag(final String tag, Editable output) {
        switch(tag.toUpperCase()) {
            case UNORDERED_LIST_TAG:
                mLists.pop();
                break;
            case ORDERED_LIST_TAG:
                mLists.pop();
                mOlIndices.pop();
                break;
            case LIST_ITEM_TAG:
                if(!mLists.isEmpty()) {
                    if(mLists.peek().first.equalsIgnoreCase(UNORDERED_LIST_TAG)) {
                        handleULTag(output);
                    } else if(mLists.peek().first.equalsIgnoreCase(ORDERED_LIST_TAG)) {
                        handleOLTag(output);
                    }
                } else {
                    end(output, Ol.class, true);
                }
                break;
            case "CODE":
                handleCodeTag(output);
                break;
            case "HR":
                handleHorizontalRule(output);
                break;
            case "CENTER":
                end(output, Center.class, true,
                        new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER)
                );
                break;
            case "S":
            case "STRIKE":
                end(output, StrikeThrough.class, false, new StrikethroughSpan());
                break;
            case "TABLE":
                handleTableTag(output);
                break;
            case BLOCKQUOTE_TAG:
                handleBlockQuoteTag(output);
                break;
            case A_TAG:
                handleATag(output);
                break;
            case "INLINECODE":
                handleInlineCodeTag(output);
                break;
            case FONT_TAG:
                handleFontTag(output);
                break;
            case "TR":
                end(output, Tr.class, false);
                break;
            case "TH":
                end(output, Th.class, false);
                break;
            case "TD":
                end(output, Td.class, false);
                break;
        }
    }

    private void handleBlockQuoteTag(Editable output) {
        final Object obj = getLast(output, BlockQuote.class);
        final int start = output.getSpanStart(obj);
        final int end = output.length();
        output.removeSpan(obj);
        output.setSpan(new QuoteSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void handleInlineCodeTag(Editable output) {
        final Object obj = getLast(output, InlineCode.class);
        final int start = output.getSpanStart(obj);
        final int end = output.length();
        output.removeSpan(obj);
        output.setSpan(new InlineCodeSpan(mTextPaint.getTextSize()), start, end,
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        );
    }

    private void handleTableTag(Editable output) {
        mTableLevel--;
        // When we're back at the root-level table
        if(mTableLevel == 0) {
            final Table obj = getLast(output, Table.class);
            final int start = output.getSpanStart(obj);
            output.removeSpan(obj); //Remove the old span
            output.insert(start, "\n \n");
            final TableSpan table = new TableSpan(mTableHtmlBuilder.toString(), mTableHandler);
            output.setSpan(table, start, start + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            output.setSpan(new WrappingClickableSpan(table), start, start + 3,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );

        } else {
            end(output, Table.class, false);
        }
    }

    private void handleHorizontalRule(Editable output) {
        final Object obj = getLast(output, HorizontalRule.class);
        final int start = output.getSpanStart(obj);
        output.removeSpan(obj); //Remove the old span
        output.replace(start, output.length(), " "); //We need a non-empty span
        output.setSpan(new HorizontalRuleSpan(), start, start + 1, 0); //Insert the bar span
    }

    private void handleCodeTag(Editable output) {
        final Object obj = getLast(output, Code.class);
        final int start = output.getSpanStart(obj);
        final int end = output.length();
        if(end > start + 1) {
            final CharSequence chars = extractSpanText(output, Code.class);
            output.removeSpan(obj);
            output.insert(start, "\n \n"); // Another line for our CodeSpan to cover
            final CodeSpan code = new CodeSpan(chars.toString(), mCodeHandler);
            output.setSpan(code, start, start + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            output.setSpan(new WrappingClickableSpan(code), start, start + 3,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }

    private void handleULTag(Editable output) {
        if(output.length() > 0 && output.charAt(output.length() - 1) != '\n') {
            output.append("\n");
        }

        if(mLists.peek().second) {
            //Check for checkboxes
            if(output.length() > 2 &&
                    ((output.charAt(0) >= '\u2610' && output.charAt(0) <= '\u2612')
                            || (output.charAt(1) >= '\u2610' && output
                            .charAt(1) <= '\u2612')
                    )) {
                end(output, Ul.class, false,
                        new LeadingMarginSpan.Standard(
                                mListIndent * (mLists.size() - 1))
                );
            } else {
                end(output, Ul.class, false,
                        new LeadingMarginSpan.Standard(
                                mListIndent * (mLists.size() - 1)),
                        new BulletSpan(mSingleIndent)
                );
            }
        } else {
            end(output, Ul.class, false,
                    new LeadingMarginSpan.Standard(mListIndent * (mLists.size() - 1))
            );
        }
    }

    private void handleOLTag(Editable output) {
        if(output.length() > 0 && output.charAt(output.length() - 1) != '\n') {
            output.append("\n");
        }
        int numberMargin = mListIndent * (mLists.size() - 1);
        if(mLists.size() > 2) {
            // Same as in ordered lists: counter the effect of nested Spans
            numberMargin -= (mLists.size() - 2) * mListIndent;
        }
        if(mLists.peek().second) {
            end(output, Ol.class, false,
                    new LeadingMarginSpan.Standard(numberMargin),
                    new ListNumberSpan(mTextPaint, mOlIndices.lastElement().first - 1,
                            mLists.peek().third
                    )
            );
        } else {
            end(output, Ol.class, false,
                    new LeadingMarginSpan.Standard(numberMargin)
            );
        }
    }

    private void handleFontTag(Editable output) {
        final ForegroundColor fgc = getLast(output, ForegroundColor.class);
        final BackgroundColor bgc = getLast(output, BackgroundColor.class);
        final Font f = getLast(output, Font.class);
        if(fgc != null) {
            final int start = output.getSpanStart(fgc);
            final int end = output.length();
            output.removeSpan(fgc);
            output.setSpan(new ForegroundColorSpan(safelyParseColor(fgc.color)), start, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        if(bgc != null) {
            final int start = output.getSpanStart(bgc);
            final int end = output.length();
            output.removeSpan(bgc);

            final int color = safelyParseColor(bgc.color);
            if(bgc.rounded) {
                output.insert(end, " ");
                output.insert(start, " ");
                output.setSpan(new RoundedBackgroundEndSpan(color, false), start, start + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                output.setSpan(new RoundedBackgroundEndSpan(color, true), end, end + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                output.setSpan(new BackgroundColorSpan(color), start + 1, end,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                );
            } else {
                output.setSpan(new BackgroundColorSpan(color), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }

        }
        if(f != null) {
            final int start = output.getSpanStart(f);
            final int end = output.length();
            output.removeSpan(f);
            output.setSpan(new TypefaceSpan(f.face), start, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }

    private void handleATag(Editable output) {
        final A obj = getLast(output, A.class);
        final int start = output.getSpanStart(obj);
        final int end = output.length();
        output.removeSpan(obj);
        if(isValidURL(obj.href)) {
            output.setSpan(new CleanURLSpan(obj.href, mLinkHandler), start, end,
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE
            );
        }
    }

    /**
     * Mark the opening tag by using private classes
     */
    private void start(Editable output, Object mark) {
        final int point = output.length();
        output.setSpan(mark, point, point, Spannable.SPAN_MARK_MARK);
    }

    /**
     * Modified from {@link android.text.Html}
     */
    private void end(Editable output, Class kind, boolean paragraphStyle, Object... replaces) {
        final Object obj = getLast(output, kind);
        final int start = output.getSpanStart(obj);
        final int end = output.length();

        // If we're in a table, then we need to store the raw HTML for later
        if(mTableLevel > 0) {
            final CharSequence extractedSpanText = extractSpanText(output, kind);
            mTableHtmlBuilder.append(extractedSpanText);
        }

        output.removeSpan(obj);
        if(start != end) {
            int len = end;
            // paragraph styles like AlignmentSpan need to end with a new line!
            if(paragraphStyle) {
                output.append("\n");
                len++;
            }
            for(Object replace : replaces) {
                if(output.length() > 0) {
                    output.setSpan(replace, start, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
    }

    /**
     * Returns the text contained within a span and deletes it from the output string
     */
    private CharSequence extractSpanText(Editable output, Class kind) {
        final Object obj = getLast(output, kind);
        final int start = output.getSpanStart(obj);
        final int end = output.length();

        final CharSequence extractedSpanText = output.subSequence(start, end);
        output.delete(start, end);
        return extractedSpanText;
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
                    return 0xff00ffff;
                case "fuchsia":
                    return 0xffff00ff;
                case "lime":
                    return 0xff00ff00;
                case "maroon":
                    return 0xff800000;
                case "navy":
                    return 0xffff00ff;
                case "olive":
                    return 0xff808000;
                case "purple":
                    return 0xff800080;
                case "silver":
                    return 0xffc0c0c0;
                case "teal":
                    return 0xff008080;
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
            final Field fElement = reader.getClass().getDeclaredField("theNewElement");
            fElement.setAccessible(true);
            final Object element = fElement.get(reader);
            final Field fAtts = element.getClass().getDeclaredField("theAtts");
            fAtts.setAccessible(true);
            final Object attrs = fAtts.get(element);
            final Field fData = attrs.getClass().getDeclaredField("data");
            fData.setAccessible(true);
            final String[] data = (String[]) fData.get(attrs);
            final Field fLength = attrs.getClass().getDeclaredField("length");
            fLength.setAccessible(true);
            final int len = (Integer) fLength.get(attrs);
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

    /**
     * If we're arriving at a table tag or are already within a table tag, then we should store
     * the raw HTML for our span
     */
    private void storeTableTags(boolean opening, String tag) {
        if(mTableLevel > 0 || tag.equalsIgnoreCase("table")) {
            mTableHtmlBuilder.append("<");
            if(!opening) {
                mTableHtmlBuilder.append("/");
            }
            mTableHtmlBuilder
                    .append(tag.toLowerCase())
                    .append(">");
        }
    }


    /**
     * Get last marked position of a specific tag kind (private class)
     */
    private static <T> T getLast(Editable text, Class<T> kind) {
        final T[] objs = text.getSpans(0, text.length(), kind);
        if(objs.length == 0) {
            return null;
        } else {
            //In reverse as items are returned in order they were inserted
            for(int i = objs.length; i > 0; i--) {
                if(text.getSpanFlags(objs[i - 1]) == Spannable.SPAN_MARK_MARK) {
                    return objs[i - 1];
                }
            }
            return null;
        }
    }

    private static class Ul {

    }

    private static class Ol {
    }

    private static class Code {
    }

    private static class Center {
    }

    private static class StrikeThrough {
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

    private static class Triple<T, U, V> {

        T first;
        U second;
        V third;

        Triple(T t, U u, V v) {
            first = t;
            second = u;
            third = v;
        }

    }

}
