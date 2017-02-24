package com.tpb.projects.util;

import android.support.annotation.Nullable;

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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by theo on 24/02/17.
 */

public class MDParser {

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
        final char[] cs = s.toCharArray();
        for(int i = 0; i < s.length(); i++) {
            if(pp != '\n' && cs[i] == '\n' && i != cs.length - 1) {
                builder.append("\n");
            }
            if(linkUsernames && cs[i] == '@' && (p == ' ' || p == '\n')) {
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
