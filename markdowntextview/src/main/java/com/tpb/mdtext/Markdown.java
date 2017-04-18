package com.tpb.mdtext;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArraySet;

import com.tpb.mdtext.emoji.Emoji;
import com.tpb.mdtext.emoji.EmojiLoader;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.Strikethrough;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Code;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Image;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlNodeRendererFactory;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.HtmlWriter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by theo on 24/02/17.
 */

public class Markdown {

    private Markdown() {
    }

    private static final List<Extension> extensions = Arrays
            .asList(TablesExtension.create(), StrikethroughExtension.create());
    private static final HtmlRenderer renderer =
            HtmlRenderer.builder()
                        .nodeRendererFactory(
                                new HtmlNodeRendererFactory() {
                                    @Override
                                    public NodeRenderer create(HtmlNodeRendererContext context) {
                                        return new BlockRenderer(
                                                context);
                                    }
                                })
                        .extensions(extensions)
                        .build();
    private static final Parser parser = Parser.builder().extensions(extensions).build();

    private static class BlockRenderer implements NodeRenderer {

        private final HtmlWriter html;
        private final HtmlNodeRendererContext context;
        private static final ArraySet<Class<? extends Node>> nodeTypes = new ArraySet<>();

        static { //Nodes to capture
            nodeTypes.add(FencedCodeBlock.class);
            nodeTypes.add(IndentedCodeBlock.class);
            nodeTypes.add(Code.class);
            nodeTypes.add(Strikethrough.class);
            nodeTypes.add(Image.class);
        }

        private static Map<String, String> codeBackgroundAttrs = new HashMap<>();

        static {
            codeBackgroundAttrs.put("background-color", "#808080");
            codeBackgroundAttrs.put("face", "monospace");
        }

        BlockRenderer(HtmlNodeRendererContext context) {
            this.context = context;
            this.html = context.getWriter();
        }

        @Override
        public Set<Class<? extends Node>> getNodeTypes() {
            return nodeTypes;
        }

        @Override
        public void render(Node node) {
            if(node instanceof FencedCodeBlock) {
                final FencedCodeBlock block = (FencedCodeBlock) node;
                // Greater than 8 lines of code
                if(TextUtils.instancesOf(block.getLiteral(), "\n") > 10) {
                    html.line();
                    html.tag("code");
                    html.raw(String.format("[%1$s]%2$s<br>", block.getInfo(),
                            block.getLiteral().replace(" ", "&nbsp;").replace("\n", "<br>")
                    ));
                    html.tag("/code");
                    html.tag("br");
                    html.line();
                } else {
                    html.tag("inlinecode");
                    if(block.getInfo() != null && !block.getInfo().isEmpty()) {
                        // TODO Highlight string
                    }
                    html.raw(block.getLiteral().replace("\n", "<br>").replace(" ", "&nbsp;"));
                    html.tag("/inlinecode");
                    html.tag("br");
                }
            } else if(node instanceof IndentedCodeBlock) {
                final IndentedCodeBlock block = (IndentedCodeBlock) node;
                html.tag("code");
                html.text(String.format("[%1$s]\u0002%2$s", "",
                        block.getLiteral().replace("\n", "<br>").replace(" ", "&nbsp;")
                ));
                html.tag("/code");
            } else if(node instanceof Code) {
                final String literal = ((Code) node).getLiteral();
                if(TextUtils.instancesOf(literal, "\n") == 0) {
                    html.tag("font", codeBackgroundAttrs);
                    html.text(literal);
                    html.tag("/font");
                } else {
                    html.tag("inlinecode");
                    html.raw(literal.replace(" ", "&nbsp;"));
                    html.tag("/inlinecode");
                }

            } else if(node instanceof Strikethrough) {
                html.line();
                html.tag("s"); //Proper tag
                context.render(node.getFirstChild()); //Fully render the children
                html.tag("/s");
                html.line();
            } else if(node instanceof Image) {
                html.line();
                html.tag("br");
                html.raw("<img src=\"" + ((Image) node).getDestination() + "\">");
                html.line();
            }
        }
    }

    private static final Map<String, String> ESCAPE_MAP = new HashMap<>();

    static {
        ESCAPE_MAP.put("#", "&#35;"); //Hashes must be escaped first
        ESCAPE_MAP.put("@", "&#64;"); //Ignore tags and email addresses
        ESCAPE_MAP.put("<", "&#60;"); //Ignore html
        ESCAPE_MAP.put(">", "&#62;");
        ESCAPE_MAP.put("`", "&#96;"); //Code tags in titles
    }

    private static final Pattern ESCAPE_PATTERN = TextUtils.generatePattern(ESCAPE_MAP.keySet());

    /**
     * Escapes characters to stop parser mishandling them
     *
     * @param s The string to escape
     * @return String with #, @, <, and > replaced with their HTML codes
     */
    public static String escape(@Nullable String s) {
        return TextUtils.replace(s, ESCAPE_MAP, ESCAPE_PATTERN);
    }

    public static String fixRelativeImageSrcs(@NonNull String s, @NonNull String fullRepoName) {
        int next = s.indexOf("<img");
        while(next != -1) {
            int srcStart = s.indexOf("src=\"", next) + "src=\"".length();
            if(srcStart > "src=".length()) {
                int srcEnd = s.indexOf("\"", srcStart);
                if(srcEnd != -1) {
                    s = s.substring(0, srcStart) + concatenateRawContentUrl(
                            s.substring(srcStart, srcEnd), fullRepoName) + s.substring(srcEnd);

                }
            }
            next = s.indexOf("<img", next + 1);
        }
        return s;
    }

    public static String parseMD(@NonNull String s) {
        return renderer.render(parser.parse(s));
    }

    public static String formatMD(@NonNull String s) {
        return formatMD(s, null);
    }

    public static String formatMD(@NonNull String s, @Nullable String fullRepoPath) {
        return formatMD(s, fullRepoPath, true);
    }

    public static String formatMD(@NonNull String s, @Nullable String fullRepoPath, boolean linkUsernames) {
        final StringBuilder builder = new StringBuilder();
        char p = ' ';
        char pp = ' ';
        final char[] chars = ("\n" + s).toCharArray();
        for(int i = 0; i < chars.length; i++) {
            if(linkUsernames && chars[i] == '@' && isWhiteSpace(p)) {
                //Max username length is 39 characters
                //Usernames can be alphanumeric with single hyphens
                i = parseUsername(builder, chars, i);
            } else if(chars[i] == '#' && isWhiteSpace(p) && fullRepoPath != null) {
                i = parseIssue(builder, chars, i, fullRepoPath);
            } else if(pp == '[' && (p == 'x' || p == 'X') && chars[i] == ']') {
                builder.setLength(builder.length() - 2);
                builder.append("\u2611");  //☑ ballot box with check
            } else if(p == '[' && chars[i] == ']') { //Closed box
                builder.setLength(builder.length() - 1);
                builder.append("\u2610"); //☐ ballot box
            } else if(pp == '[' && p == ' ' && chars[i] == ']') {//Open box
                builder.setLength(builder.length() - 2);
                builder.append("\u2610");
            } else if(chars[i] == '(' && fullRepoPath != null) {
                builder.append("(");
                i = parseImageLink(builder, chars, i, fullRepoPath);
            } else if(chars[i] == ':') {
                i = parseEmoji(builder, chars, i);
            } else if(pp == '`' && p == '`' && chars[i] == '`') {
                //We jump over the code block
                pp = ' ';
                p = ' ';
                int j = i;
                for(; j < chars.length; j++) {
                    builder.append(chars[j]);
                    if(pp == '`' && p == '`' && chars[j] == '`') {
                        i = j;
                        p = ' ';
                        break;
                    } else {
                        pp = p;
                        p = chars[j];
                    }
                }
            } else {
                builder.append(chars[i]);
            }
            pp = p;
            p = chars[i];
        }
        return builder.toString();
    }

    /**
     * Formats a username as a link, appends it to a StringBuilder, and returns the position
     * after the username in cs
     *
     * @return The position after the username
     */
    private static int parseUsername(StringBuilder builder, char[] cs, int pos) {
        final StringBuilder nameBuilder = new StringBuilder();
        char p = ' ';
        for(int i = pos + 1; i < cs.length; i++) {
            if(((cs[i] >= 'A' && cs[i] <= 'Z') ||
                    (cs[i] >= 'a' && cs[i] <= 'z') ||
                    (cs[i] >= '0' && cs[i] <= '9') ||
                    (cs[i] == '-' && p != '-')) &&
                    i - pos <= 39 &&
                    i != cs.length - 1) {
                nameBuilder.append(cs[i]);
                p = cs[i];
                //nameBuilder.length() > 0 stop us linking a single @
            } else if((isWhiteSpace(cs[i]) || i == cs.length - 1) && i - pos > 1) {
                if(i == cs.length - 1) {
                    nameBuilder.append(cs[i]); //Otherwise we would miss the last char of the name
                }
                builder.append("[@");
                builder.append(nameBuilder.toString());
                builder.append("](https://github.com/");
                builder.append(nameBuilder.toString());
                builder.append(')');
                if(i != cs.length - 1) {
                    builder.append(cs[i]); // We still need to append the space or newline
                }
                return i;
            } else {
                break;
            }

        }
        builder.append("@");
        return pos;
    }

    /**
     * Formats an Issue reference as a link, appends it to a StringBuilder, and returns the
     * position after the Issue reference in cs
     *
     * @return The position after the Issue number
     */
    private static int parseIssue(StringBuilder builder, char[] cs, int pos, String fullRepoPath) {
        final StringBuilder numBuilder = new StringBuilder();
        for(int i = pos + 1; i < cs.length; i++) {
            if(cs[i] >= '0' && cs[i] <= '9' && i != cs.length - 1) {
                numBuilder.append(cs[i]);
            } else if((isWhiteSpace(cs[i]) || isLineEnding(cs, i)) && i > pos + 1) {
                if(i == cs.length - 1) {
                    if(cs[i] >= '0' && cs[i] <= '9') {
                        numBuilder.append(cs[i]);
                    } else {
                        break;
                    }
                }
                builder.append("[#");
                builder.append(numBuilder.toString());
                builder.append("](https://github.com/");
                builder.append(fullRepoPath);
                builder.append("/issues/");
                builder.append(numBuilder.toString());
                builder.append(")");
                if(i != cs.length - 1) {
                    builder.append(cs[i]); // We still need to append the whitespace
                }
                return i;
            } else {
                break;
            }
        }
        builder.append("#");
        return pos;
    }

    private static String concatenateRawContentUrl(String url, String fullRepoName) {
        if(url.startsWith("http://") ||url.startsWith("https://")) return url;
        int offset = 0;
        if(url.startsWith("./")) offset = 2;
        else if(url.startsWith("/")) offset = 1;
        return "https://raw.githubusercontent.com/" + fullRepoName + "/master/" + url
                    .substring(offset);
    }

    private static int parseImageLink(StringBuilder builder, char[] cs, int pos, String fullRepoPath) {
        for(int i = pos + 1; i < cs.length; i++) {
            if(cs[i] == ')') {
                final String link = new String(Arrays.copyOfRange(cs, pos + 1, i));
                final String extension = link.substring(link.lastIndexOf('.') + 1);
                if("png".equalsIgnoreCase(extension) ||
                        "jpg".equalsIgnoreCase(extension) ||
                        "gif".equalsIgnoreCase(extension) ||
                        "bmp".equalsIgnoreCase(extension) ||
                        "webp".equalsIgnoreCase(extension)) {
                    if(TextUtils.isValidURL(link)) {
                        builder.append(link);
                    } else {
                        builder.append(concatenateRawContentUrl(link, fullRepoPath));
                    }
                    builder.append(") <br><br>");
                } else {
                    builder.append(link);
                    builder.append(")");
                }
                return i;
            } else if(isWhiteSpace(cs[i])) {
                break;
            }
        }

        return pos;
    }

    private static int parseEmoji(StringBuilder builder, char[] cs, int pos) {
        final StringBuilder emojiBuilder = new StringBuilder();

        for(int i = pos + 1; i < cs.length; i++) {
            if((cs[i] >= 'A' && cs[i] <= 'Z') ||
                    (cs[i] >= '0' && cs[i] <= '9') ||
                    (cs[i] >= 'a' && cs[i] <= 'z') ||
                    cs[i] == '_') {
                emojiBuilder.append(cs[i]);
            } else if(cs[i] == ':') {
                final Emoji eww = EmojiLoader.getEmojiForAlias(emojiBuilder.toString());
                if(eww == null) break;
                builder.append(eww.getUnicode());
                return i;
            } else {
                break;
            }
        }
        builder.append(":");
        return pos;
    }

    private static boolean isLineEnding(char[] cs, int i) {
        return i == cs.length - 1 || cs[i] == '\n' || cs[i] == '\r';
    }

    private static boolean isWhiteSpace(char c) {
        //Space tab, newline, line tabulation, carriage return, form feed
        return c == ' ' || c == '\t' || c == '\n' || c == '\u000B' || c == '\r' || c == '\u000C';
    }


}
