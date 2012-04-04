/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
/**
 * Converts the javadoc comment in the {@code package-info.java} file into Oracle Wiki format.
 */
package com.oracle.max.tools.javadoc.wiki;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.sun.max.ide.JavaProject;
import com.sun.max.program.Classpath;
import com.sun.max.program.ClasspathTraversal;
import com.sun.javadoc.*;

/**
 * {@link Doclet} to convert package-info javadoc to Oracle Wiki format.
 *
 * The conversion includes inline tags and embedded HTML.
 * There are many heuristics used to do a reasonable, given the ad hoc nature of HTML
 * and the Wiki notation rules. Writing proper HTML, e.g., always including end tags, helps.
 */
public class WikiDoclet extends Doclet {
    private static final String TEXT = "Text";
    private static final String KENAI_MAXINE_TIP = "http://kenai.com/hg/maxine~maxine/file/tip";
    private static final String AUTO_GEN_BEGIN = "{excerpt:hidden=true}[_Automatically generated from ";
    private static final String AUTO_GEN_END = "{excerpt}\n";
    private static String outputDir;
    private static String docletPath;
    private static String projectList;
    private static boolean includeToc = true;
    private static StringBuilder sb;
    private static Map<String, Integer> headerDepth = new HashMap<String, Integer>();
    private static Map<String, String> validHtmlTags = new HashMap<String, String>();
    private static Map<String, String> classToProject = new HashMap<String, String>();
    private static char[] commentTextArray;

    static class WikiException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        WikiException(String message) {
            super(message);
        }
    }

    private static class HtmlTag {
        String tag;
        String attributes;

        HtmlTag(String tag, String attributes) {
            this.tag = tag;
            this.attributes = attributes;
        }

        static HtmlTag createTag(StringRange range) {
            int spaceIndex = range.string.indexOf(' ', range.startIndex);
            if (spaceIndex >= range.endIndex) {
                spaceIndex = -1;
            }
            String tag = range.string.substring(range.startIndex, spaceIndex < 0 ? range.endIndex : spaceIndex).toUpperCase();
            String attributes = null;
            if (spaceIndex >= 0) {
                attributes = range.string.substring(spaceIndex + 1, range.endIndex);
            }
            return new HtmlTag(tag, attributes);
        }

        @Override
        public String toString() {
            return "<" + tag + (attributes != null ? " " + attributes : "") + ">";
        }
    }

    private static class StringRange {
        final String string;
        final int startIndex;
        final int endIndex;

        StringRange(String string, int startIndex, int endIndex) {
            this.string = string;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        StringRange(StringRange range, int startIndex, int endIndex) {
            this(range.string, startIndex, endIndex);
        }

        @Override
        public String toString() {
            return "\"" + string.substring(startIndex, endIndex) + "\"\nstart: " + startIndex + ", end: " + endIndex;
        }
    }

    private static class HtmlTagData {
        String content;
        StringRange contentRange;
        int tagEndIndex;
        HtmlTagData(String text, StringRange contentRange, int tagEndIndex) {
            this.contentRange = contentRange;
            this.content = text.substring(contentRange.startIndex, contentRange.endIndex);
            this.tagEndIndex = tagEndIndex;
        }

        @Override
        public String toString() {
            return contentRange.toString() + ", tagEnd: " + tagEndIndex;
        }
    }

    private static class InlineTagInfo {
        Tag inlineTag;
        StringRange range;
        InlineTagInfo(Tag inlineTag, StringRange range) {
            this.inlineTag = inlineTag;
            this.range = range;
        }

        @Override
        public String toString() {
            return Tag.class.getName() + ", index: " + range;
        }
    }

    private static abstract class EntityIndex {
        final int index;

        EntityIndex(int index) {
            this.index = index;
        }

    }

    private static class InlineTagEntityIndex extends EntityIndex {
        InlineTagInfo inlineTagInfo;

        InlineTagEntityIndex(int index, InlineTagInfo inlineTagInfo) {
            super(index);
            assert inlineTagInfo != null;
            this.inlineTagInfo = inlineTagInfo;
        }
    }

    private static class HtmlTagEntityIndex extends EntityIndex {
        HtmlTagEntityIndex(int index) {
            super(index);
        }
    }

    public static boolean start(RootDoc root) {
        readOptions(root.options());
        addHtmlTags();
        buildClassToProjectsMap();
        new File(outputDir).mkdir();

        PackageDoc[] packageDocs = root.specifiedPackages();
        for (PackageDoc packageDoc : packageDocs) {
            try {
                Tag[] inlineTags = packageDoc.inlineTags();
                String commentText = packageDoc.commentText();
                processPackageInfoDoc(packageDoc, commentText, inlineTags);
            } catch (Exception ex) {
                System.err.println("Exception processing package " + packageDoc.name() + ": " + ex);
                System.exit(1);
            }
        }
        return true;
    }

    private static void buildClassToProjectsMap() {
        // We can't use system classpath to find workspace root as it doesn't include any Maxine projects
        // javadoc -classpath is NOT the same as java -classpath!
        final File wsRoot = JavaProject.findWorkspace(new Classpath(docletPath));
        final int wsRootLength = wsRoot.getAbsolutePath().length();
        ArrayList<Classpath.Entry> projectEntries = new ArrayList<Classpath.Entry>();
        String[] projects = projectList.split(",");
        for (String project : projects) {
            projectEntries.add(new Classpath.Directory(new File(new File(wsRoot, project), "bin")));
        }
        Classpath projectClasspath = new Classpath(projectEntries);
        new ClasspathTraversal() {

            @Override
            protected boolean visitFile(File parent, String resource) {
                if (resource.endsWith(".class")) {
                    int x = resource.lastIndexOf(".class");
                    classToProject.put(resource.substring(0, x).replace('/', '.').replace('$', '.'), getProject(wsRootLength, parent.getAbsolutePath()));
                }
                return true;
            }
        }.run(projectClasspath);
    }

    private static String getProject(int wsRootEndIndex, String binPath) {
        int binIndex = binPath.lastIndexOf("/bin");
        return binPath.substring(wsRootEndIndex + 1, binIndex);
    }

    private static void addHtmlTags() {
        for (int i = 1; i <= 4; i++) {
            String hd = "H" + i;
            headerDepth.put(hd, i);
            addHtmlTag(hd);
        }
        addHtmlTag("P");
        addHtmlTag("I");
        addHtmlTag("UL");
        addHtmlTag("OL");
        addHtmlTag("I");
        addHtmlTag("B");
        addHtmlTag("LI");
        addHtmlTag("PRE");
        addHtmlTag("A");
        addHtmlTag("HR");
    }

    private static void addHtmlTag(String h) {
        validHtmlTags.put(h, h);
    }

    private static void processPackageInfoDoc(PackageDoc packageDoc, String commentText, Tag[] inlineTags) {
        commentTextArray = new char[commentText.length()];
        for (int i = 0; i < commentText.length(); i++) {
            commentTextArray[i] = commentText.charAt(i);
        }
        InlineTagInfo[] inlineTagInfo = computeTagIndices(commentText, inlineTags);
        sb = new StringBuilder();
        sb.append(AUTO_GEN_BEGIN);
        sb.append(createLink(packageDoc.name(), "package-info"));
        sb.append(AUTO_GEN_END);
        if (includeToc) {
            sb.append("{toc}\n");
        }
        processText(new StringRange(commentText, 0, commentText.length()), inlineTagInfo, null);
        File wikiFile = new File(outputDir, packageDoc.name() + ".wiki");
        BufferedWriter wr = null;
        try {
            wr = new BufferedWriter(new FileWriter(wikiFile));
            wr.write(sb.toString());
        } catch (IOException ex) {
            System.err.println(ex);
        } finally {
            if (wr != null) {
                try {
                    wr.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    /**
     * Compute the actual indices of the non-text tags in the comment text.
     * Experimentally, every tag in, say, a package-info comment, has the same "position",
     * that of the package statement - not very helpful.
     * @param inlineTags
     * @return
     */
    private static InlineTagInfo[] computeTagIndices(String commentText, Tag[] inlineTags) {
        ArrayList<InlineTagInfo> indicesList = new ArrayList<InlineTagInfo>();
        int index = 0;
        for (int i = 0; i < inlineTags.length; i++) {
            Tag tag = inlineTags[i];
            String tagText = tag.text();
            String tagName = tag.name();
            if (tagName.equals(TEXT)) {
                index += tagText.length();
            } else {
                int startIndex = index;
                assert commentText.charAt(index) == '{';
                int x = commentText.indexOf(tagText, index);
                assert x >= 0;
                x = commentText.indexOf('}', x + tagText.length());
                assert x >= 0;
                index = x + 1;
                indicesList.add(new InlineTagInfo(tag, new StringRange(commentText, startIndex, index)));
            }
        }
        InlineTagInfo[] indices = new InlineTagInfo[indicesList.size()];
        indicesList.toArray(indices);
        return indices;
    }

    /**
     * Convert any embedded HTML tags and inline javadoc tags in {@code s} into Wiki equivalents.
     * HTML tag content can contain inline tags, but not vice versa.
     * Transformed text is appended to {@link #sb}.
     * @param range {@code StringRange} to process
     * @param inlineTagInfo info on where the inline tags are
     * @param lastEntityIndex of the last entity processed
     *
     */
    private static void processText(StringRange range, InlineTagInfo[] inlineTagInfo, EntityIndex lastEntityIndex) {
        int lastIndex = range.startIndex;
        while (lastIndex < range.endIndex) {
            StringRange newRange = new StringRange(range, lastIndex, range.endIndex);
            EntityIndex entityIndex = nextEntity(newRange, inlineTagInfo);
            if (entityIndex == null) {
                sb.append(fixLineBreaks(newRange, lastEntityIndex));
                break;
            }
            sb.append(fixLineBreaks(new StringRange(range, lastIndex, entityIndex.index), lastEntityIndex));
            lastEntityIndex = entityIndex;
            if (entityIndex instanceof InlineTagEntityIndex) {
                InlineTagEntityIndex inlineTagEntityIndex = (InlineTagEntityIndex) entityIndex;
                Tag inlineTag = inlineTagEntityIndex.inlineTagInfo.inlineTag;
                if (inlineTag.name().equals("@code")) {
                    sb.append("{{");
                    sb.append(fixWikiEscapes(fixEntityReferences(inlineTag.text())));
                    sb.append("}}");
                } else if (inlineTag.name().startsWith("@link")) {
                    SeeTag seeTag = (SeeTag) inlineTag;
                    boolean plain = inlineTag.name().equals("@linkplain");
                    String label = seeTag.label();
                    if (label.isEmpty()) {
                        label = null;
                    }
                    String className = seeTag.referencedClassName();
                    String simpleClassName = stripPackage(className);
                    String projectName = classToProject.get(className);
                    String memberName = seeTag.referencedMemberName();
                    String classAndMemberName = memberName == null ? simpleClassName : simpleClassName + "." + memberName;
                    if (projectName != null) {
                        sb.append('[');
                    }
                    if (!plain) {
                        sb.append("{{");
                    }
                    sb.append(label == null ? classAndMemberName : label);
                    if (!plain) {
                        sb.append("}}");
                    }
                    if (projectName != null) {
                        // link to source on kenai
                        sb.append(createLink(projectName, className));
                    }

                } else {
                    assert false;
                }
                lastIndex = inlineTagEntityIndex.inlineTagInfo.range.endIndex;
            } else if (entityIndex instanceof HtmlTagEntityIndex) {
                int tagEndIndex = range.string.indexOf('>', lastIndex);
                if (tagEndIndex < 0) {
                    throw new WikiException("malformed HTML tag");
                }
                HtmlTag htmlTagInfo = HtmlTag.createTag(new StringRange(range.string, entityIndex.index + 1, tagEndIndex));
                String htmlTag = htmlTagInfo.tag;
                if (validHtmlTags.get(htmlTag) == null) {
                    throw new WikiException("unimplemented HTML tag: " + htmlTag);
                }
                HtmlTagData data = findmatchingTag(range.string, htmlTag, tagEndIndex + 1);
                int hd = isHeader(htmlTag);
                if (hd > 0) {
                    sb.append('\n');
                    sb.append('h');
                    sb.append(hd);
                    sb.append(". ");
                    processText(data.contentRange, inlineTagInfo, lastEntityIndex);
                    sb.append('\n');
                } else if (htmlTag.equals("I")) {
                    sb.append('_');
                    sb.append(data.content);
                    sb.append('_');
                } else if (htmlTag.equals("B")) {
                    sb.append('*');
                    sb.append(data.content);
                    sb.append('*');
                } else if (htmlTag.equals("P")) {
                    // matching tag usually omitted
                    sb.append('\n');
                    if (data != null) {
                        processText(data.contentRange, inlineTagInfo, lastEntityIndex);
                    } else {
                        lastIndex = tagEndIndex + 1;
                    }
                    sb.append('\n');
                } else if (htmlTag.equals("UL") | htmlTag.equals("OL")) {
                    processText(data.contentRange, inlineTagInfo, lastEntityIndex);
                    sb.append('\n');
                } else if (htmlTag.equals("PRE")) {
                    // no interpretation of body
                    sb.append("{code}\n");
                    sb.append(replacePreLeadingSpaces(fixEntityReferences(data.content)));
                    sb.append("{code}\n");
                } else if (htmlTag.equals("LI")) {
                    sb.append('\n');
                    sb.append("* ");
                    processText(data.contentRange, inlineTagInfo, lastEntityIndex);
                } else if (htmlTag.equals("A")) {
                    sb.append('[');
                    sb.append(data.content);
                    sb.append('|');
                    sb.append(getHRef(htmlTagInfo.attributes));
                    sb.append(']');
                } else if (htmlTag.equals("HR")) {
                    sb.append("\n----\n");
                }
                if (data != null) {
                    lastIndex = data.tagEndIndex;
                }
            }
        }
    }

    private static String getHRef(String s) {
        int index = s.indexOf('"');
        int lastIndex = s.lastIndexOf('"');
        return s.substring(index + 1, lastIndex);
    }

    private static String createLink(String projectName, String className) {
        StringBuilder ssb = new StringBuilder();
        ssb.append('|');
        ssb.append(KENAI_MAXINE_TIP);
        ssb.append('/');
        ssb.append(projectName);
        ssb.append("/src/");
        ssb.append(className.replace('.', '/'));
        ssb.append(".java");
        ssb.append(']');
        return ssb.toString();
    }

    /**
     * In package-info files class names must be fully qualified (pain), but javadoc
     * strips the package in the HTML, and we do the same.
     * @param qualName
     */
    private static String stripPackage(String qualName) {
        for (int i = 0; i < qualName.length(); i++) {
            if (Character.isUpperCase(qualName.charAt(i))) {
                return i == 0 ? qualName : qualName.substring(i);
            }
        }
        return qualName;
    }

    /**
     * Remove internal line breaks and leading space from the javadoc comment as Wiki will treat them literally.
     * @param s
     * @param range of {@code s} to be analysed
     * @return
     */
    private static String fixLineBreaks(StringRange range, EntityIndex lastEntityIndex) {
        String result = "";
        int index = range.startIndex;
        while (index < range.endIndex) {
            int breakIndex = range.string.indexOf("\n ", index);
            if (breakIndex < 0 || breakIndex >= range.endIndex - 2) {
                if (breakIndex == range.endIndex - 2) {
                    // if it ends in "\n " we drop the newline but keep the space
                    result += range.string.substring(index, range.endIndex - 2) + " ";
                } else {
                    result += range.string.substring(index, range.endIndex);
                }
                break;
            }
            result += range.string.substring(index, breakIndex);
            // if at start, after HTML, skip newline and space, else just the newline (space becomes separator)
            if (breakIndex == range.startIndex && (lastEntityIndex != null && lastEntityIndex instanceof HtmlTagEntityIndex)) {
                index = breakIndex + 2;
            } else {
                index = breakIndex + 1;
            }
        }
        return result;
    }

    private static String fixEntityReferences(String s) {
        StringBuilder ssb = new StringBuilder();
        int index = 0;
        while (index < s.length()) {
            int eIndex = s.indexOf('&', index);
            if (eIndex < 0) {
                break;
            }
               // append up to the '&'
            ssb.append(s.substring(index, eIndex));
            eIndex++;
            char c = s.charAt(eIndex);
            if (c == '#') {
                int nIndex = eIndex + 1;
                char dig = s.charAt(nIndex);
                int code = 0;
                while (dig >= '0' && dig <= '9') {
                    code = code * 10 + (dig - '0');
                    nIndex++;
                    dig = s.charAt(nIndex);
                }
                ssb.append((char) code);
                index = nIndex;
            } else if (entityMatch(s, eIndex, "amp")) {
                ssb.append('&');
                index = eIndex + 4;
            } else if (entityMatch(s, eIndex, "gt")) {
                ssb.append('>');
                index = eIndex + 3;
            } else if (entityMatch(s, eIndex, "ge")) {
                ssb.append(">=");
                index = eIndex + 3;
            } else if (entityMatch(s, eIndex, "lt")) {
                ssb.append('<');
                index = eIndex + 3;
            } else if (entityMatch(s, eIndex, "le")) {
                ssb.append("<=");
                index = eIndex + 3;
            } else {
                throw new WikiException("undecoded character entity reference");
            }
        }
        ssb.append(s, index, s.length());
        return ssb.toString();
    }

    private static boolean entityMatch(String s, int index, String m) {
        try {
            for (int i = 0; i < m.length(); i++) {
                if (s.charAt(index + i) != m.charAt(i)) {
                    return false;
                }
            }
            return s.charAt(index + m.length()) == ';';
        } catch (StringIndexOutOfBoundsException ex) {
            return false;
        }
    }

    private static String fixWikiEscapes(String s) {
        StringBuilder ssb = new StringBuilder();
        int index = 0;
        while (index < s.length()) {
            char ch = s.charAt(index);
            if (ch == '[' || ch == ']') {
                ssb.append('\\');
            }
            ssb.append(ch);
            index++;
        }
        return ssb.toString();
    }

    /**
     * Remove the first leading space after a newline (arises due to the way javadoc comments are written).
     * @param s
     * @return
     */
    private static String replacePreLeadingSpaces(String s) {
        StringBuilder ssb = new StringBuilder();
        int index = 0;
        while (index < s.length()) {
            int nlIndex = s.indexOf("\n ", index);
            if (nlIndex < 0) {
                break;
            }
            // There is always a "\n " at the start that we just want to ignore
            if (index > 0) {
                // append up to and including the newline
                nlIndex++;
                ssb.append(s.substring(index, nlIndex));
                // check for leading space
                try {
                    if (s.charAt(nlIndex) == ' ') {
                        nlIndex++;
                    }
                } catch (StringIndexOutOfBoundsException ex) {
                    break;
                }
            } else {
                nlIndex = 2;
            }
            index = nlIndex;
        }
        // append everything after the last position
        ssb.append(s.substring(index));
        return ssb.toString();
    }

    /**
     * Finds the index of the next inline tag or HTML tag in {@code s[range]}.
     * @param s
     * @param range
     * @param inlineTagInfo
     * @return an {@link EntityIndex} or null if not found
     */
    private static EntityIndex nextEntity(StringRange range, InlineTagInfo[] inlineTagInfo) {
        int htmlTagIndex = range.string.indexOf('<', range.startIndex);
        if (htmlTagIndex >= range.endIndex) {
            htmlTagIndex = -1;
        }
        int inlineTagIndex = -1;
        InlineTagInfo inlineTagInfoResult = null;
        for (int i = 0; i < inlineTagInfo.length; i++) {
            int thisIndex = inlineTagInfo[i].range.startIndex;
            if (thisIndex >= range.startIndex && thisIndex < range.endIndex) {
                // possibility
                inlineTagIndex = thisIndex;
                inlineTagInfoResult = inlineTagInfo[i];
                break;
            }
        }
        if (htmlTagIndex < 0 && inlineTagIndex < 0) {
            return null;
        } else {
            if (htmlTagIndex < 0) {
                return new InlineTagEntityIndex(inlineTagIndex, inlineTagInfoResult);
            } else if (inlineTagIndex < 0) {
                return new HtmlTagEntityIndex(htmlTagIndex);
            } else {
                // both possible
                if (htmlTagIndex < inlineTagIndex) {
                    return new HtmlTagEntityIndex(htmlTagIndex);
                } else {
                    return new InlineTagEntityIndex(inlineTagIndex, inlineTagInfoResult);
                }
            }
        }

    }

    /**
     * Find the tag that matches {@code tag} in {@code s} starting at {@code index}.
     * TODO Handle nested tags properly
     * @param s
     * @param tag
     * @param index index of first char after '>'
     * @return {@link HtmlTagData} or null if not found
     */
    private static HtmlTagData findmatchingTag(String s, String tag, final int index) {
        int curIndex = index;
        while (true) {
            int tagIndex = s.indexOf('<', curIndex);
            if (tagIndex < 0) {
                return null;
            }
            int tagEndIndex = s.indexOf('>', tagIndex);
            if (s.charAt(tagIndex + 1) == '/') {
                if (tagEndIndex < 0) {
                    throw new WikiException("malformed HTML tag");
                }
                String endTag = s.substring(tagIndex + 2, tagEndIndex).toUpperCase();
                if (endTag.equals(tag)) {
                    return new HtmlTagData(s, new StringRange(s, index, tagIndex), tagEndIndex + 1);
                } else {
                    // we found the end of a nested tag, just keep going
                    curIndex = tagEndIndex + 1;
                }
            } else {
                // start of nested tag, skip it unless it matches "tag" in which case treat it as an implicit end.
                // <P> is special, any nested tag matches (<P> as separator style)
                String endTag = s.substring(tagIndex + 1, tagEndIndex).toUpperCase();
                if (endTag.equals(tag) || (tag.equals("P") && endTag.equals("P"))) {
                    return new HtmlTagData(s, new StringRange(s, index, tagIndex), tagIndex);
                } else {
                    curIndex = tagIndex + 1;
                }
            }
        }
    }

    private static int isHeader(String tagName) {
        Integer depth = headerDepth.get(tagName.toUpperCase());
        if (depth == null) {
            return -1;
        } else {
            return depth;
        }
    }

    private static String readOptions(String[][] options) {
        String tagName = null;
        for (int i = 0; i < options.length; i++) {
            String[] opt = options[i];
            if (opt[0].equals("-d")) {
                outputDir = opt[1];
            } else if (opt[0].equals("-notoc")) {
                includeToc = false;
            } else if (opt[0].equals("-docletpath")) {
                docletPath = opt[1];
            } else if (opt[0].equals("-projects")) {
                projectList = opt[1];
            }
        }
        return tagName;
    }

    public static int optionLength(String option) {
        if (option.equals("-d") || option.equals("-link")) {
            return 2;
        } else if (option.equals("-notoc")) {
            return 1;
        } else if (option.equals("-projects")) {
            return 2;
        }
        return 0;
    }

    public static boolean validOptions(String[][] options, DocErrorReporter reporter) {
        boolean foundDirOption = false;
        for (int i = 0; i < options.length; i++) {
            String[] opt = options[i];
            if (opt[0].equals("-d")) {
                if (foundDirOption) {
                    reporter.printError("Only one -d option allowed.");
                    return false;
                } else {
                    foundDirOption = true;
                }
            } else if (opt[0].equals("-notoc")) {
                // ignore
            } else if (opt[0].equals("-link")) {
                // ignore
            } else if (opt[0].equals("-projects")) {
                // ignore
            }
        }
        if (!foundDirOption) {
            reporter.printError("Usage: javadoc -doclet WikiDoclet -d outputDir -link url ...");
        }
        return foundDirOption;
    }

}
