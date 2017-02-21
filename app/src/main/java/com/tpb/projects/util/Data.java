package com.tpb.projects.util;

import android.util.Base64;
import android.util.Log;

import com.tpb.projects.data.models.Repository;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.HtmlWriter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

/**
 * Created by theo on 16/12/16.
 */

public class Data {
    private static final String TAG = Data.class.getSimpleName();

    private static final List<Extension> extensions = Arrays.asList(TablesExtension.create(), StrikethroughExtension.create());
    private static final HtmlRenderer renderer = HtmlRenderer.builder()
            .nodeRendererFactory(IndentedCodeBlockNodeRenderer::new)
            .extensions(extensions)
            .build();
    private static final Parser parser = Parser.builder().extensions(extensions).build();

    private static class IndentedCodeBlockNodeRenderer implements NodeRenderer {

        private final HtmlWriter html;

        IndentedCodeBlockNodeRenderer(HtmlNodeRendererContext context) {
            this.html = context.getWriter();
        }

        @Override
        public Set<Class<? extends Node>> getNodeTypes() {
            // Return the node types we want to use this renderer for.
            return Collections.singleton(FencedCodeBlock.class);
        }

        @Override
        public void render(Node node) {
            // We only handle one type as per getNodeTypes, so we can just cast it here.
            FencedCodeBlock codeBlock = (FencedCodeBlock) node;
            html.line();
            html.tag("code");
            html.text(codeBlock.getLiteral());
            html.tag("/code");
            html.line();
        }
    }

    public static final Comparator<Repository> repoAlphaSort = (r1, r2) -> r1.getName().compareToIgnoreCase(r2.getName());

    public static String intArrayForPrefs(List<Integer> values) {
        final StringBuilder builder = new StringBuilder();
        for(int i : values) {
            builder.append(i).append(",");
        }
        return builder.toString();


    }

    public static int[] intArrayFromPrefs(String value) {
        final String[] values = value.split(",");
        final int[] ints = new int[values.length];
        if(value.length() == 0) return ints;
        for(int i = 0; i < values.length; i++) ints[i] = Integer.parseInt(values[i]);
        return ints;
    }

    public static int indexOf(int[] values, int key) {
        for(int i = 0; i < values.length; i++) if(values[i] == key) return i;
        return -1;
    }

    public static int indexOf(String[] values, String key) {
        for(int i = 0; i < values.length; i++) if(values[i].equals(key)) return i;
        return -1;
    }

    public static String formatKB(int kb) {
        if(kb < 1024) return Integer.toString(kb) + " KB";
        if(kb < 1024 * 1024) return String.format("%.2f", kb / 1024f) + " MB";
        return String.format("%.2f", kb / (1024f * 1024f)) + " GB";
    }

    public static String formatBytes(int b) {
        if(b < 1024) return Integer.toString(b) + " B";
        if(b < 1024 * 1024) return String.format("%.2f", b / 1024f) + " KB";
        if(b < 1024 * 1024 * 1024) return String.format("%.2f", b / (1024f * 1024f)) + " MB";
        return String.format("%.2f", b / (1024f * 1024f * 1024f)) + " GB";
    }

    public static String base64Decode(String base64) {
        return new String(Base64.decode(base64, Base64.DEFAULT));
    }

    //http://stackoverflow.com/a/10621553/4191572
    private static final SimpleDateFormat ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    public static String toISO8061(long t) {
        return ISO8601.format(new Date(t * 1000));
    }

    /**
     * Transform ISO 8601 string to Calendar.
     */
    public static Calendar toCalendar(final String iso8601string)
            throws ParseException {
        final Calendar calendar = GregorianCalendar.getInstance();
        String s = iso8601string.replace("Z", "+00:00");
        try {
            s = s.substring(0, 22) + s.substring(23);  // to get rid of the ":"
        } catch(IndexOutOfBoundsException e) {
            throw new ParseException("Invalid length", 0);
        }
        final Date date = ISO8601.parse(s);
        calendar.setTime(date);
        return calendar;
    }


    public static String parseMD(String s, String fullRepoName) {
        return renderer.render(parser.parse(formatMD(s, fullRepoName)));
    }

    public static String parseMD(String s) {
        return renderer.render(parser.parse(s));
    }

    public static String formatMD(String s, String fullRepoPath) {

        final StringBuilder builder = new StringBuilder();
        char p = ' ';
        char pp = ' ';
        final char[] cs = s.toCharArray();
        for(int i = 0; i < s.length(); i++) {
            if(pp != '\n' && cs[i] == '\n' && i != cs.length - 1) {
                builder.append("\n");
            }
            if(cs[i] == '@' && (p == ' ' || p == '\n')) {
                //Max username length is 39 characters
                //Usernames can be alphanumeric with single hyphens
                i = parseUsername(builder, cs, i);
            } else if(cs[i] == '-' && p == '-' && pp == '-') {
                //TODO Find out if there is a way of computing characters per line and filling the string
                //I could try using the strike tag
                builder.setLength(builder.length() - 2);
                builder.append("──────────\n");

            } else if(cs[i] == '#' && (p == ' ' || p == '\n') && fullRepoPath != null) {
                i = parseIssue(builder, cs, i, fullRepoPath);
            } else if(pp == '[' && (p == 'x' || p == 'X') && cs[i] == ']') {
                if(builder.length() - 4 >= 0 && cs[i - 4] == '-') {
                    builder.setLength(builder.length() - 4);
                } else {
                    builder.setLength(builder.length() - 2);
                }
                builder.append("\u2611");
            } else if(p == '[' && cs[i] == ']') {
                if(builder.length() - 4 >= 0 && cs[i - 4] == '-') {
                    builder.setLength(builder.length() - 3);
                } else {
                    builder.setLength(builder.length() - 2);
                }
                builder.append("\u2610");
            } else if(pp == '[' && p == ' ' && cs[i] == ']') {
                if(builder.length() - 4 >= 0 && cs[i - 4] == '-') {
                    builder.setLength(builder.length() - 4);
                } else {
                    builder.setLength(builder.length() - 3);
                }
                builder.append("\u2610");
            } else if(cs[i] == '(') {
                builder.append("(");
                i = parseImageLink(builder, cs, i);
            } else if(pp == '`' && p == '`' && cs[i] == '`') {
                pp = ' ';
                p = ' ';
                for(int j = i; j < cs.length; j++) {
                    builder.append(cs[j]);
                    if(pp == '`' && p == '`' && cs[j] == '`') {
                        i = j;
                        break;
                    } else {
                        pp = p;
                        p = cs[j];
                    }
                }
            } else {
                builder.append(cs[i]);
            }
            pp = p;
            p = cs[i];

        }
        return builder.toString();
    }

    private static int parseUsername(StringBuilder builder, char[] cs, int pos) {
        final StringBuilder nameBuilder = new StringBuilder();
        char p = ' ';
        for(int i = ++pos; i < cs.length; i++) {
            if(((cs[i] >= 'A' && cs[i] <= 'Z') ||
                    (cs[i] >= '0' && cs[i] <= '9') ||
                    (cs[i] >= 'a' && cs[i] <= 'z') ||
                    (cs[i] == '-' && p != '-')) &&
                    i - pos < 38 &&
                    i != cs.length - 1) {
                nameBuilder.append(cs[i]);
                p = cs[i];
                //nameBuilder.length() > 0 stop us linking a single @
            } else if((cs[i] == ' ' || cs[i] == '\n' || i == cs.length - 1) && nameBuilder.length() > 0) {
                if(i == cs.length - 1) {
                    nameBuilder.append(cs[i]); //Otherwise we would miss the last char of the name
                }
                builder.append("[@");
                builder.append(nameBuilder.toString());
                builder.append(']');
                builder.append('(');
                builder.append("https://github.com/");
                builder.append(nameBuilder.toString());
                builder.append(')');
                if(i != cs.length - 1) {
                    builder.append(cs[i]); // We still need to append the space or newline
                }
                return i;
            } else {
                builder.append("@");
                return --pos;
            }

        }
        builder.append("@");
        return --pos;
    }

    private static int parseIssue(StringBuilder builder, char[] cs, int pos, String fullRepoPath) {
        final StringBuilder numBuilder = new StringBuilder();
        for(int i = ++pos; i < cs.length; i++) {
            if(cs[i] >= '0' && cs[i] <= '9' && i != cs.length - 1) {
                numBuilder.append(cs[i]);
            } else if(i > pos && (cs[i] == ' ' || cs[i] == '\n' || i == cs.length - 1)) {
                if(i == cs.length - 1) {
                    if(cs[i] >= '0' && cs[i] <= '9') {
                        numBuilder.append(cs[i]);
                    } else if(numBuilder.length() == 0) {
                        builder.append("#");
                        return --pos;
                    }
                }
                builder.append("[#");
                builder.append(numBuilder.toString());
                builder.append("]");
                builder.append("(");
                builder.append("https://github.com/");
                builder.append(fullRepoPath);
                builder.append("/issues/");
                builder.append(numBuilder.toString());
                builder.append(")");
                if(i != cs.length - 1) {
                    builder.append(cs[i]); // We still need to append the space or newline
                }
                return i;
            } else {
                builder.append("#");
                return --pos;
            }
        }
        builder.append("#");
        return --pos;
    }

    /*
    This function fixes positioning of text after images
    The TextView fucks up line spacing if there is text on the same line
    as an image, so if we find an image url we add a newline
     */
    private static int parseImageLink(StringBuilder builder, char[] cs, int pos) {
        for(int i = ++pos; i < cs.length; i++) {
            if(cs[i] == ')') {
                final String link = new String(Arrays.copyOfRange(cs, pos, i));
                final String extension = link.substring(link.lastIndexOf('.') + 1);
                if("png".equals(extension) ||
                        "jpg".equals(extension) ||
                        "gif".equals(extension) ||
                        "bmp".equals(extension) ||
                        "webp".equals(extension)) {
                    builder.append(link);
                    builder.append(") <br>");
                    Log.i(TAG, "parseImageLink: Builder after finding image " + builder.toString());
                } else {
                    builder.append(link);
                    builder.append(")");
                }
                return i;
            }
        }

        return --pos;
    }

    public static int instancesOf(String s, String i) {
        int last = 0;
        int count = 0;
        while(last != -1) {
            last = s.indexOf(i, last);
            if(last != -1) {
                count++;
                last += i.length();
            }
        }
        return count;
    }

}
