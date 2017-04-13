package com.tpb.mdtext;

import java.util.Stack;
import java.util.regex.Pattern;

/**
 * Created by theo on 13/04/17.
 */

public class Parser {

    private static final String P_TAG = "p";
    private static final String A_TAG = "a";
    private static final String UL_TAG = "ul";
    private static final String OL_TAG = "ol";
    private static final String HR_TAG = "hr";
    private static final String TABLE_TAG = "table";
    private static final String TABLE_ROW = "tr";
    private static final String TABLE_DATA = "td";
    private static final String TABLE_HEADING = "th";
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

        final Stack<Element> elements = new Stack<>();

        for(int i = 0; i < md.length(); i++) {
            if(isMarkdownControl(md.charAt(i))) {

            }
        }

        return "";
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

    private static boolean isCodeBlocks(String s, int i) {
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

    private static String replaceUnsafe(String s) {
        return s.replace("\u0000", "\ufffd");
    }

    private static class Element {

        int start;
        int end;
        String tag;

        Element(int start, int end, String tag) {
            this.start = start;
            this.end = end;
            this.tag = tag;
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
