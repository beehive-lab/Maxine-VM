// Checkstyle: stop
package com.sun.max.ide.eclipse;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * A simple utility for generating an Eclipse refactoring script based on the result of Java Search for field
 * definitions:
 * <ol>
 * <li>Perform a Java search for field definitions matching some regular expression.</li>
 * <li>In the "Search" view, change the presentation to "Show as List" (little triangle icon at top right of Search
 * view)</li>
 * <li>Select all the matches (CTRL-A) and then use the "Copy Qualified Name" option from the context menu brought up by
 * right clicking on the selected results.</li>
 * <li>Paste the results into a new file (e.g. scratch).</li>
 * <li>Run this program over the file. For example:
 *
 * <pre>
 * MakeFieldNameRefactorScript &lt;source directories separated by ':'&gt; &lt;field list file&gt; &lt;regex&gt; &lt;replacement&gt;
 * </pre>
 *
 * The source directories are those in which the relevant source files can be found.
 * The format accepted for {@code regex} and {@code replacement} is specified by {@link String#replaceAll(String, String)}.
 * </li>
 * <li>Run the script via the <b>Refactor -> Apply Script...</b> in Eclipse.</li>
 * </ol>
 *
 * @author Doug Simon
 */
public class MakeFieldNameRefactorScript {

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println("Usage: MakeFieldNameRefactorScript <source directories separated by ':'> <field list file> <regex> <replacement>");
            System.exit(1);
        }

        String[] srcDirs = args[0].split(":");
        String fieldListFile = args[1];
        Pattern regex = Pattern.compile(args[2]);
        String replacement = args[3];

        List<String> lines = readLines(new File(fieldListFile));

        File outFile = new File(fieldListFile + ".xml");
        PrintStream out = new PrintStream(new FileOutputStream(outFile));
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<session version=\"1.0\">");

        int count = 0;
        for (String line : lines) {
            if (line.contains("{...}") || line.contains("()")) {
                System.err.println("Cannot handle field in anonymous class: " + line);
                continue;
            }

            int lastDot = line.lastIndexOf('.');
            final String field = line.substring(lastDot + 1);

            Matcher matcher = regex.matcher(field);
            if (!matcher.matches()) {
                System.err.printf("Field name '%s' is not matched by regular expression '%s'%n", field, args[2]);
                continue;
            }

            final String className = line.substring(0, lastDot);

            try {
                String[] inputElements = inputElements(className, srcDirs);
                String srcDir = inputElements[0];

                if (srcDir == null) {
                    System.err.println("Could not file source file for " + line);
                    continue;
                }
                String pkg = inputElements[1];
                String topLevelClass = inputElements[2];

                String newName = matcher.replaceAll(replacement);
                File project = new File(srcDir).getAbsoluteFile();
                while (!new File(project, ".project").exists()) {
                    project = project.getParentFile();
                }

                String projectRelSrcDir = new File(srcDir).getAbsolutePath().substring(project.getAbsolutePath().length());
                String desc = "Rename field " + line;
                StringBuilder input = new StringBuilder(projectRelSrcDir + "&lt;" + pkg + '{' + topLevelClass + ".java");
                for (int i = 2; i < inputElements.length; i++) {
                    input.append('[' + inputElements[i]);
                }
                input.append("^" + field);

                out.println("<refactoring");
                out.println("  comment=\"" + desc + "\"");
                out.println("  delegate=\"false\"");
                out.println("  deprecate=\"false\"");
                out.println("  description=\"" + desc + "\"");
                out.println("  flags=\"589830\"");
                out.println("  getter=\"false\"");
                out.println("  id=\"org.eclipse.jdt.ui.rename.field\"");
                out.println("  input=\"" + input.toString() +'"');
                out.println("  name=\"" + newName + '"');
                out.println("  project=\"" + project.getName() + '"');
                out.println("  references=\"true\"");
                out.println("  setter=\"false\"");
                out.println("  textual=\"false\"");
                out.println("  version=\"1.0\"");
                out.println("/>");

                ++count;
            } catch (StringIndexOutOfBoundsException e) {
                System.err.println("Could not make refactoring for " + line + ": " + e);
                e.printStackTrace();
            }
        }

        out.println("</session>");
        out.close();

        System.out.printf("%d refactorings generated to %s [%d fields skipped]%n", count, outFile.getAbsolutePath(), lines.size() - count);
    }

    public static List<String> readLines(File file) throws IOException {
        List<String> lines = new ArrayList<String>();
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        reader.close();
        return lines;
    }

    private static File findFile(String[] srcDirs, String suffix) {
        for (String srcDir : srcDirs) {
            File file = new File(srcDir, suffix);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    private static String[] inputElements(String className, String[] srcDirs) {
        String sourceFilePath = className.replace('.', '/') + ".java";
        File sourceFile = findFile(srcDirs, sourceFilePath);
        if (sourceFile == null) {
            int lastDot = className.lastIndexOf('.');
            if (lastDot == -1) {
                return new String[] {null, null, className};
            }
            String[] outerClasses = inputElements(className.substring(0, lastDot), srcDirs);
            String[] result = java.util.Arrays.copyOf(outerClasses, outerClasses.length + 1);
            result[result.length - 1] = className.substring(lastDot + 1);
            return result;
        }

        String path = sourceFile.getPath();
        String srcDir = path.substring(0, path.length() - (sourceFilePath.length() + 1));

        int index = className.lastIndexOf('.');
        String pkg = "";
        String simpleClassName = className;
        if (index > 0) {
            pkg = className.substring(0, index);
            simpleClassName = className.substring(index + 1);
        }
        return new String[] {srcDir, pkg, simpleClassName};
    }
}
