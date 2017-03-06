package com.tpb.projects.markdown;

import android.support.annotation.Nullable;
import android.support.v4.util.ArraySet;
import android.util.Log;

import com.tpb.projects.util.Util;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.Strikethrough;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.HtmlWriter;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.tpb.projects.data.APIHandler.TAG;

/**
 * Created by theo on 24/02/17.
 */

public class Markdown {

    private Markdown() {}

    private static final List<Extension> extensions = Arrays.asList(TablesExtension.create(), StrikethroughExtension.create());
    private static final HtmlRenderer renderer = HtmlRenderer.builder()
            .nodeRendererFactory(CustomBlockRenderer::new)
            .extensions(extensions)
            .build();
    private static final Parser parser = Parser.builder().extensions(extensions).build();

    private static class CustomBlockRenderer implements NodeRenderer {

        private final HtmlWriter html;
        private HtmlNodeRendererContext context;
        private static ArraySet<Class<? extends Node>> nodeTypes = new ArraySet<>();
        static { //Nodes to capture
            nodeTypes.add(FencedCodeBlock.class);
            nodeTypes.add(Strikethrough.class);
        }

        CustomBlockRenderer(HtmlNodeRendererContext context) {
            this.context = context;
            this.html = context.getWriter();
        }

        @Override
        public Set<Class<? extends Node>> getNodeTypes() {
            return nodeTypes;
        }

        @Override
        public void render(Node node) {
            // We only handle one type as per getNodeTypes, so we can just cast it here.
            if(node instanceof FencedCodeBlock) {
                final FencedCodeBlock block = (FencedCodeBlock) node;

                if(Util.instancesOf(block.getLiteral(), "\n") > 7) {
                    html.line();
                    html.tag("code");
                    html.text(block.getLiteral());
                    html.tag("/code");
                    html.line();
                } else {
                    html.tag("small");
                    html.line();
                    Log.i(TAG, "render: Lang " + block.getInfo() + ", for " + block.getLiteral() );
                    if(block.getInfo() != null && !block.getInfo().isEmpty()) {
                        // TODO Highlight string
                    }
                    for(String s : block.getLiteral().replace("\n\n", "\n").split("\n")) {
                        html.raw(s.replace(" ", "&nbsp;"));
                        html.tag("br");
                    }
                    html.tag("/small");
                    html.line();

                }
            } else if(node instanceof Strikethrough) {
                html.line();
                html.tag("s"); //Proper tag
                context.render(node.getFirstChild()); //Fully render the children
                html.tag("/s");
                html.line();
            }
        }
    }


    public static String escape(String s) {
        s = s.replace("#", "&#35;"); //Hashes must be escaped first
        s = s.replace("@", "&#64;"); //Ignore tags and email addresses
        s = s.replace("<", "&#60;"); //Ignore html
        s = s.replace(">", "&#62;");
        return s;
    }

    public static String parseMD(String s, String fullRepoName) {
        return renderer.render(parser.parse(formatMD(s, fullRepoName)));
    }

    public static String parseMD(String s) {
        return renderer.render(parser.parse(s));
    }

    public static String formatMD(String s, @Nullable String fullRepoPath) {
        return formatMD(s, fullRepoPath, true);
    }

    public static String formatMD(String s, @Nullable String fullRepoPath, boolean linkUsernames) {
        final StringBuilder builder = new StringBuilder();
        char p = ' ';
        char pp = ' ';
        final char[] chars = s.toCharArray();
        for(int i = 0; i < s.length(); i++) {
            //Ensure that lines are properly spaced
            if(pp != '\n' && chars[i] == '\n' && i != chars.length - 1 && chars[i + 1] != '\n') {
                builder.append("\n");
            }
            if(linkUsernames && chars[i] == '@' && (p == ' ' || p == '\n')) {
                //Max username length is 39 characters
                //Usernames can be alphanumeric with single hyphens
                i = parseUsername(builder, chars, i);
            } else if(chars[i] == '-' && p == '-' && pp == '-') {
                //Full width bar
                builder.setLength(builder.length() - 2);
                builder.append("<bar></bar>");

            } else if(chars[i] == '#' && (p == ' ' || p == '\n') && fullRepoPath != null) {
                i = parseIssue(builder, chars, i, fullRepoPath);
            } else if(pp == '[' && (p == 'x' || p == 'X') && chars[i] == ']') {
                if(builder.length() - 4 >= 0 && chars[i - 4] == '-') {
                    builder.setLength(builder.length() - 4);
                } else {
                    builder.setLength(builder.length() - 2);
                }
                builder.append("\u2611"); //☑ ballot box with check
            } else if(p == '[' && chars[i] == ']') { //Closed box
                if(builder.length() - 4 >= 0 && chars[i - 4] == '-') {
                    builder.setLength(builder.length() - 3);
                } else {
                    builder.setLength(builder.length() - 2);
                }
                builder.append("\u2610"); //☐ ballot box
            } else if(pp == '[' && p == ' ' && chars[i] == ']') {//Open box

                if(builder.length() - 4 >= 0 && chars[i - 4] == '-') {
                    builder.setLength(builder.length() - 4);
                } else {
                    builder.setLength(builder.length() - 3);
                }
                builder.append("\u2610");
            } else if(chars[i] == '(') {
                builder.append("(");
                i = parseImageLink(builder, chars, i);
            } else if(pp == '`' && p == '`' && chars[i] == '`') {
                //We jump over the code block
                pp = ' ';
                p = ' ';
                for(int j = i; j < chars.length; j++) {
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
                } else {
                    builder.append(link);
                    builder.append(")");
                }
                return i;
            }
        }

        return --pos;
    }

}
