package com.tpb.mdtext;

import android.nfc.Tag;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Pattern;

/**
 * Created by theo on 13/04/17.
 */

public class Parser {

    private static final String OPEN_ARROW = "<";
    private static final String CLOSE_ARROW = ">";
    private static final String CLOSE_SLASH = "/";
    private static final String H_TAG = "h";
    private static final String P_TAG = "p";
    private static final String A_TAG = "a";
    private static final String UL_TAG = "ul";
    private static final String OL_TAG = "ol";
    private static final String LI_TAG = "li";
    private static final String HR_TAG = "hr";
    private static final String TABLE_TAG = "table";
    private static final String TABLE_ROW = "tr";
    private static final String TABLE_DATA = "td";
    private static final String TABLE_HEADING_TAG = "th";
    private static final String BR_TAG = "br";
    private static final String BLOCKQUOTE_TAG = "blockquote";
    private static final String CODE_TAG = "code";
    private static final String INLINE_CODE  = "inlinecode";
    private static final String IMAGE_TAG = "img";
    private static final String BOLD_TAG = "b";
    private static final String ITALIC_TAG = "i";
    private static final String STRIKETHROUGH_TAG = "strikethrough";


    public static String parseMarkdown(final String md) {
        final StringBuilder builder = new StringBuilder();
        builder.append("<body>\n");
        final Stack<Element> elements = new Stack<>();
        int start = 0;
        int cinline = 0;
        for(int i = 0; i < md.length(); i++) {

            if(cinline == 0) { //Start of line
                if(isHeading(md, i)) {
                    elements.push(new Element(start, i, H_TAG));
                } else if(isBlockQuote(md, i)) {
                    elements.push(new Element(start, i, BLOCKQUOTE_TAG));
                }
            }

            if(isLineEnding(md, i)) {
                cinline = 0;
            } else {
                cinline++;
            }

            if(isMarkdownControl(md.charAt(i))) {

            }
        }
        builder.append("</body>");
        return builder.toString();
    }

    private static boolean isMarkdownControl(char c) {
        return c == '!' || c == '#' || c == '*' || c == '_' || c == '-' || c == '[' || c == '>' || c == '`';
    }

    private static boolean isWhiteSpace(char c) {
        //Space tab, newline, line tabulation, carriage return, form feed
        return c == ' ' || c == '\t' || c == '\n' || c == '\u000B' || c == '\r' || c == '\u000C';
    }

    private static boolean isPunctuation(char c) {
        return c == '!' ||
                c == '#' || c == '$' || c == '%' || c == '&' || c == '(' || c == ')' || c == '*' ||
                c == '+' || c == ',' || c == '-' || c == '.' || c == '/' || c == ':' || c == ';' ||
                c == '<' || c == '=' || c == '>' || c == '?' || c == '@' || c == '[' || c == ']' ||
                c == '^' || c == '`' || c == '{' || c == '|' || c == '}' || c == '~' ||
                c == '\'' || c == '\\';
    }

    private static boolean isLineEnding(String s, int i) {
        //Character is breaking, and (next character isn't or we are at end of string)
        return (s.charAt(i) == '\n' || s.charAt(i) == '\r') && (
                (i + 1 < s.length() && (s.charAt(i + 1) != '\n' && s.charAt(i + 1) != '\r')) ||
                        i == s.length() - 1
        );
    }

    private static int findNextLineEnding(String s, int i) {
        for(; i < s.length(); i++) {
            if(isLineEnding(s, i)) return i;
        }
        return i;
    }

    private static int skipCDATA(String s, int i) {
        char p = ' ';
        char pp = ' ';
        for(; i < s.length(); i++) {
            if(s.charAt(i) == '>' && p == ']' && pp == ']') {
               return i - 2;
            }
            pp = p;
            p = s.charAt(i);
        }
        return i;
    }

    private static Pattern HTML_TAG = Pattern.compile("address|article|aside|base|basefont|" + 
            "blockquote|body|caption|center|col|colgroup|dd|details|dialog|dir|div|dl|" + 
            "dt|fieldset|figcaption|figure|footer|form|frame|frameset|h1|h2|h3|h4|h5|" + 
            "h6|head|header|hr|html|iframe|legend|li|link|main|menu|menuitem|meta|nav|" +
            "noframes|ol|optgroup|option|p|param|section|source|summary|table|tbody|td|" +
            "tfoot|th|thead|title|tr|track|ul");

    private static boolean isHtmlTag(String s) {
        return HTML_TAG.matcher(s).matches();
    }

    private static boolean isBlockQuote(String s, int i) {
        int indent = 0;
        for(; i < s.length(); i++) {
            if(s.charAt(i) == ' ') {
                indent++;
                if(indent > 3) return false;
            } else return !(s.charAt(i) != '>' || isEscaped(s, i));
        }
        return false;
    }

    private static boolean isListItem(String s, int i) {
        for(; i < s.length(); i++) {

        }
        return false;
    }

    private static boolean isFencedCodeBlock(String s, int i) {
        int indent = 0;
        int backtickCount = 0;
        int tildeCount = 0;
        for(; i < s.length(); i++) {
            if(s.charAt(i) == ' ') {
                indent++;
                if(indent > 3) return false;
            } else if(!isEscaped(s, i)) {
                if(s.charAt(i) == '`') {
                    if(tildeCount > 0) return false;
                    backtickCount++;
                    if(backtickCount == 3) break;
                } else if(s.charAt(i) == '~') {
                    if(backtickCount > 0) return false;
                    tildeCount++;
                    if(tildeCount == 3) break;
                }
            }
        }
        if(backtickCount == 3 || tildeCount == 3) {
            final int end = findNextLineEnding(s, i);
            return !contains(s, '`', i, end) && !contains(s, '~', i, end);
        }
        return false;
    }

    private static boolean isCodeBlock(String s, int i) {
        int indent = 0;
        for(; i < s.length(); i++) {
            if(s.charAt(i) == ' ') {
                indent++;
            } else if(s.charAt(i) == '\t') {
                indent += 4;
            }
            if(indent >= 4) return true;
        }
        return false;
    }

    //TODO Setext headings
    private static boolean isHeading(String s, int i) {
        int indent = 0;
        int depth = 0;
        for(; i < s.length(); i++) {
            if(s.charAt(i) == ' ') {
                if(depth == 0) { //Starting whitespace
                    indent++;
                    if(indent > 3) return false;
                }
                if(depth > 0) {
                    //TODO Deal with ### fdsafsda ### style headings
                    return true;
                }
            } else if(s.charAt(i) == '#') {
                depth++;
            } else {
                return false;
            }

        }

        return false;
    }

    private static boolean isThematicBreak(String s, int i) {
        int indent = 0; //Max 3 spaces at start
        int starCount = 0;
        int hyphenCount = 0;
        int underscoreCount = 0;
        boolean found = false;
        for(; i < s.length(); i++) {
            if(s.charAt(i) == '*' || s.charAt(i) == '-' || s.charAt(i) == '_') {
                if(s.charAt(i) == '*') {
                    starCount++;
                    hyphenCount = 0;
                    underscoreCount = 0;
                } else if(s.charAt(i) == '-') {
                    hyphenCount++;
                    starCount = 0;
                    underscoreCount = 0;
                } else if(s.charAt(i) == '_') {
                    underscoreCount++;
                    hyphenCount = 0;
                    starCount = 0;
                }
                found |= (starCount == 3 || hyphenCount == 3 || underscoreCount == 3);
            } else if(!found && s.charAt(i) == ' ') { //Space at start
                indent++;
                if(indent > 3) return false;
            } else if(isLineEnding(s, i)) {
                return found;
            } else if(!isWhiteSpace(s.charAt(i))) {
                return false;
            }
        }
        return true; //If we hit the end of the string we must have found a hr
    }

    private static boolean isAlphaNumeric(char c) {
        return ('0' <= c && c <= '9') || ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z');
    }

    private static boolean isEscaped(String s, int i) {
        return i > 0 && s.length() > 0 && s.charAt(i - 1) == '\\';
    }

    private static boolean contains(String s, char c, int i, int j) {
        for(; i <= j && i < s.length(); i++) {
            if(s.charAt(i) == c && !isEscaped(s, i)) return true;
        }
        return false;
    }

    private static int instancesOf(@NonNull String s1, @NonNull String s2) {
        int last = 0;
        int count = 0;
        while(last != -1) {
            last = s1.indexOf(s2, last);
            if(last != -1) {
                count++;
                last += s2.length();
            }
        }
        return count;
    }

    private static String replaceUnsafe(String s) {
        return s.replace("\u0000", "\ufffd");
    }

    private static class Element {

        int start;
        int end;
        String tag;
        boolean paired;
        boolean multiline;

        List<Element> children = new ArrayList<>();

        Element(int start, int end, String tag, boolean paired, boolean multiline) {
            this.start = start;
            this.end = end;
            this.tag = tag;
            this.paired = paired;
            this.multiline = multiline;
        }

        Element(int start, int end, String tag) {
            this(start, end, tag, false, false);
        }

    }


    private static abstract class TAG {

        TAG parent;
        String content;
        List<Tag> children = new ArrayList<>();

        static String close(String tag) {
            return OPEN_ARROW + CLOSE_SLASH + tag + CLOSE_ARROW;
        }

        abstract void toHtmlString(StringBuilder builder);

    }

    private static class Literal extends TAG {

        String literal;

        Literal(String literal) {
            this.literal = literal;
        }


        @Override
        void toHtmlString(StringBuilder builder) {
            builder.append(literal);
        }
    }

    private static class Header extends TAG {

        int value;

        Header(int value) {
            this.value = value;
        }

        @Override
        void toHtmlString(StringBuilder builder) {
            builder.append(OPEN_ARROW);
            builder.append(H_TAG);
            builder.append(value);
            builder.append(CLOSE_ARROW);
            for(Tag t : children) builder.append(t.toString());
            builder.append(close(H_TAG + value));
        }
    }

    private static class Paragraph extends TAG {


        @Override
        void toHtmlString(StringBuilder builder) {
            builder.append(OPEN_ARROW);
            builder.append(P_TAG);
            builder.append(CLOSE_ARROW);
            for(Tag t : children) builder.append(t.toString());
            builder.append(close(P_TAG));
        }

    }

    private static class HREF extends TAG {

        String url;

        HREF(String url) {
            this.url = url;
        }


        @Override
        void toHtmlString(StringBuilder builder) {
            builder.append(OPEN_ARROW);
            builder.append(A_TAG);
            builder.append(" href=\"");
            builder.append(url);
            builder.append("\" ");
            builder.append(CLOSE_ARROW);
            for(Tag t : children) builder.append(t.toString());
            builder.append(close(A_TAG));
        }
    }

    private static class UnorderedList extends TAG {

        @Override
        void toHtmlString(StringBuilder builder) {
            builder.append(OPEN_ARROW);
            builder.append(UL_TAG);
            builder.append(CLOSE_ARROW);
            for(Tag t : children) builder.append(t.toString());
            builder.append(close(UL_TAG));
        }
    }

    private static class OrderedList extends TAG {

        String type;

        OrderedList(String type) {
            this.type = type;
        }

        @Override
        void toHtmlString(StringBuilder builder) {
            builder.append(OPEN_ARROW);
            builder.append(OL_TAG);
            if(type != null) {
                builder.append(" type=\"");
                builder.append(type);
                builder.append("\" ");
            }
            builder.append(CLOSE_ARROW);
            for(Tag t : children) builder.append(t.toString());
            builder.append(close(OL_TAG));
        }
    }

    private static class ListItem extends TAG {

        @Override
        void toHtmlString(StringBuilder builder) {
            builder.append(OPEN_ARROW);
            builder.append(LI_TAG);
            builder.append(CLOSE_ARROW);
            for(Tag t : children) builder.append(t.toString());
            builder.append(close(LI_TAG));
        }

    }

    private static class HorizontalRule extends TAG {
        @Override
        void toHtmlString(StringBuilder builder) {
            builder.append(OPEN_ARROW + HR_TAG + CLOSE_ARROW + " " + OPEN_ARROW + CLOSE_SLASH + CLOSE_ARROW);
        }
    }

    private static class Table extends TAG {

        @Override
        void toHtmlString(StringBuilder builder) {
            builder.append(OPEN_ARROW);
            builder.append(TABLE_TAG);
            builder.append(CLOSE_ARROW);
            for(Tag t : children) builder.append(t.toString());
            builder.append(close(TABLE_TAG));
        }

    }

    private static class TableRow extends TAG {

        @Override
        void toHtmlString(StringBuilder builder) {
            builder.append(OPEN_ARROW);
            builder.append(TABLE_ROW);
            builder.append(CLOSE_ARROW);
            for(Tag t : children) builder.append(t.toString());
            builder.append(close(TABLE_ROW));
        }

    }

    private static class TableHeading extends TAG {

        @Override
        void toHtmlString(StringBuilder builder) {
            builder.append(OPEN_ARROW);
            builder.append(TABLE_HEADING_TAG);
            builder.append(CLOSE_ARROW);
            for(Tag t : children) builder.append(t.toString());
            builder.append(close(TABLE_HEADING_TAG));
        }
    }

    private static class TableData extends TAG {

        @Override
        void toHtmlString(StringBuilder builder) {
            builder.append(OPEN_ARROW);
            builder.append(TABLE_DATA);
            builder.append(CLOSE_ARROW);
            for(Tag t : children) builder.append(t.toString());
            builder.append(close(TABLE_DATA));
        }

    }

    private static class Code extends TAG {

        String code;
        String language;

        Code(String code, String language) {
            this.code = code;
            this.language = language;
        }

        @Override
        void toHtmlString(StringBuilder builder) {
            builder.append(OPEN_ARROW);
            if(instancesOf(code, "\n") > 10) {
                builder.append(CODE_TAG);
                builder.append(CLOSE_ARROW);
                builder.append(String.format("[%1$s]%2$s<br>", language,
                        code.replace(" ", "&nbsp;").replace("\n", "<br>")
                ));
                builder.append(close(CODE_TAG));
            } else {
                builder.append(INLINE_CODE);
                builder.append(CLOSE_ARROW);
                builder.append(code.replace("\n", "<br>").replace(" ", "&nbsp;"));
                builder.append(close(INLINE_CODE));
            }
        }
    }

    private static class Image extends TAG {

        String url;
        String title;

        Image(String url, String title) {
            this.url = url;
            this.title = title;
        }

        @Override
        void toHtmlString(StringBuilder builder) {
            builder.append(OPEN_ARROW);
            builder.append(P_TAG);
            builder.append(CLOSE_ARROW);
            builder.append(OPEN_ARROW);
            builder.append(IMAGE_TAG);
            builder.append(" src=\"");
            builder.append(url);
            builder.append("\" title=\"");
            builder.append(title);
            builder.append("\" ");
            builder.append(CLOSE_SLASH);
            builder.append(CLOSE_ARROW);
            builder.append(close(P_TAG));
        }

    }

    private static class Bold extends TAG {

        @Override
        void toHtmlString(StringBuilder builder) {
            builder.append(OPEN_ARROW);
            builder.append(BOLD_TAG);
            builder.append(CLOSE_ARROW);
            for(Tag t : children) builder.append(t.toString());
            builder.append(close(BOLD_TAG));
        }
    }

    private static class Italic extends TAG {

        @Override
        void toHtmlString(StringBuilder builder) {
            builder.append(OPEN_ARROW);
            builder.append(ITALIC_TAG);
            builder.append(CLOSE_ARROW);
            for(Tag t : children) builder.append(t.toString());
            builder.append(close(ITALIC_TAG));
        }
    }

    private static class Strikethrough extends TAG {

        @Override
        void toHtmlString(StringBuilder builder) {
            builder.append(OPEN_ARROW);
            builder.append(STRIKETHROUGH_TAG);
            builder.append(CLOSE_ARROW);
            for(Tag t : children) builder.append(t.toString());
            builder.append(close(STRIKETHROUGH_TAG));
        }
    }


//    private static int isHtmlBlock(String s, int i) {
//        boolean inOpening = false;
//        boolean inClosing = false;
//        final StringBuilder tagBuilder = new StringBuilder();
//        final Stack<HTMLTag> tags = new Stack<>();
//        String tag;
//        char c;
//        int start = i;
//        for(; i < s.length(); i++) {
//            c = s.charAt(i);
//            if(c == '<') {
//                if(i < s.length() - 1 && s.charAt(i + 1) == '/') {
//                    inClosing = true;
//                } else {
//                    inOpening = true;
//                }
//                start = i;
//            } else if(c == '>') {
//                inOpening = false;
//                inClosing = false;
//            } else if(inOpening || inClosing) {
//                if(isAlphaNumeric(c) || c == '!' || c == '[' || c == ']' || c == '-' || c == '?') {
//                    tagBuilder.append(c);
//                } else if(!isWhiteSpace(c)) {
//                    inOpening = false;
//                    tag = tagBuilder.toString();
//                    if(tags.size() == 1) {
//                        if()
//                    }
//                    if(tags.size() == 0 || !tag.equalsIgnoreCase(tags.peek().tag)) {
//                        if("script".equalsIgnoreCase(tag) || "pre".equalsIgnoreCase(tag) ||
//                                "style".equalsIgnoreCase(tag)) {
//                            tags.push(new HTMLTag(start, i , tag, 1));
//                        } else if("!--".equals(tag)) {
//                            tags.push(new HTMLTag(start, i , tag, 2));
//                        } else if("?".equals(tag)) {
//                            tags.push(new HTMLTag(start, i , tag, 3));
//                        } else if(tag.startsWith("?")) {
//                            tags.push(new HTMLTag(start, i , tag, 4));
//                        } else if("![CDATA[".equals(tag)) {
//                            tags.push(new HTMLTag(start, i , tag, 5));
//                            i = skipCDATA(s, i);
//                        } else if(isHtmlTag(tag)) {
//                            tags.push(new HTMLTag(start, i , tag, 6));
//                        }
//                    }
//                    tagBuilder.setLength(0);
//                }
//            }
//        }
//
//        return 0;
//    }
}
