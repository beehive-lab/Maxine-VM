/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

/**
 * Database of Java project configurations used by the bin/max script.
 * The projects are loaded from the "projects.properties" file in the
 * core Maxine directory as well as from the directories specified by
 * the "extra_projects_dirs" environment variable.
 */
public class jmax {

    /**
     * Handles commands sent by the max script.
     *
     * @param request the command and its arguments
     * @param response where the response it to be written
     * @return the exit code
     */
    public static int process(List<String> request, PrintStream response) {
        String cmd = request.get(0);
        if (cmd.equals("projects")) {
            // Return the project names separated by spaces
            response.println(toString(sortedDependencies(false), " ", null));
        } else if (cmd.equals("classpath")) {
            // Return a class path for one or more projects
            classpathCommand(request, response, true);
        } else if (cmd.equals("classpath_noresolve")) {
            // Return a class path for one or more projects
            classpathCommand(request, response, false);
        } else if (cmd.equals("project_dir")) {
            // Return the absolute directory of a project
            Project p = project(request.get(1));
            response.println(p.baseDir.getPath() + File.separatorChar + p.name);
        } else if (cmd.equals("eclipse")) {
            int changedFiles = 0;
            if (request.size() == 1) {
                for (Project p : projects.values()) {
                    changedFiles += p.generateEclipseProject(response);
                }
            } else {
                for (String p : request.subList(1, request.size())) {
                    changedFiles += project(p).generateEclipseProject(response);
                }
            }
            return changedFiles;
        } else if (cmd.equals("filter")) {
            filter(new File(request.get(1)), new File(request.get(2)), response);
        } else if (cmd.equals("source_dirs")) {
            // Return the absolute source directories of a project
            Project p = project(request.get(1));
            String pfx = p.baseDir.getPath() + File.separatorChar + p.name + File.separatorChar;
            response.println(toString(p.sourceDirs, " ", pfx));
        } else if (cmd.equals("library")) {
            response.println(library(request.get(1)).appendToClasspath(new StringBuilder(), true).toString());
        } else if (cmd.equals("output_dir")) {
            // Return the absolute output directory of a project
            response.println(project(request.get(1)).outputDir());
        } else if (cmd.equals("properties")) {
            for (File dir : projectDirs) {
                response.println("# file:  " + new File(dir, "projects.properties"));
                for (Library l : libs.values()) {
                    if (l.baseDir.equals(dir)) {
                        l.printProperties(response);
                    }
                }
                for (Project p : projects.values()) {
                    if (p.baseDir.equals(dir)) {
                        p.printProperties(response);
                    }
                }
            }
        } else {
            throw new Error("Command ' " + cmd + "' not known");
        }
        return 0;
    }

    private static void filter(File inputFile, File excludeFile, PrintStream response) {
        ArrayList<String> patterns = new ArrayList<String>();
        for (String pattern : readLines(excludeFile)) {
            if (!pattern.startsWith("#")) {
                patterns.add(pattern);
            }
        }
        for (String path : readLines(inputFile)) {
            boolean excluded = false;
            for (String pattern : patterns) {
                if (path.contains(pattern)) {
                    excluded = true;
                    log.println("excluded: " + path);
                }
            }
            if (!excluded) {
                response.println(path);
            }
        }
    }

    static PrintStream log = System.err;

    private static void classpathCommand(List<String> request, PrintStream response, boolean resolve) {
        if (request.size() == 1) {
            // No args -> return the class path for all projects
            response.println(asClasspath(sortedDependencies(true), resolve));
        } else {
            // One or more args -> return the class path for the given projects
            List<Dependency> deps = new ArrayList<Dependency>();
            for (String p : request.subList(1, request.size())) {
                project(p).allDeps(deps, true);
            }
            response.println(asClasspath(deps, resolve));
        }
    }

    /**
     * Looks up an environment variable, asserting it is set if {@code required}.
     */
    static String getenv(String name, boolean required) {
        String value = System.getenv(name);
        assert !required || value != null : "the required environment variable ' " + name + "' is not set";
        return value;
    }

    /**
     * Gets the project for a given name, asserting that the project exists.
     */
    static Project project(String name) {
        Project p = projects.get(name);
        assert p != null : "project named ' " + name + "' not found";
        return p;
    }

    /**
     * Gets the library for a given name, asserting that the library exists.
     */
    static Library library(String name) {
        Library l = libs.get(name);
        assert l != null : "library named ' " + name + "' not found";
        return l;
    }

    /**
     * Gets and removes the value of a named property from a given property set.
     *
     * @param file the file from which {@code props} was loaded. If this is non-null, then the property must exist
     */
    static String get(Properties props, String key, File file) {
        String value = props.getProperty(key);
        if (value == null) {
            if (file != null) {
                throw new Error("missing property  " + key + " in  " + file);
            }
        } else {
            props.remove(key);
        }
        return value;
    }

    /**
     * Loads projects from a projects.properties file in a given directory.
     */
    static void loadProjects(File dir) {
        File f = new File(dir, "projects.properties");
        Properties props = new Properties();
        try {
            FileReader in = new FileReader(f);
            props.load(in);
            in.close();
        } catch (Exception e) {
            throw new Error("Error loading projects from  " + f.getAbsolutePath(), e);
        }

        HashMap<String, String> projectNames = new HashMap<String, String>();
        HashMap<String, String> libNames = new HashMap<String, String>();
        for (Enumeration<?> e = props.propertyNames(); e.hasMoreElements(); ) {
            String propName = (String) e.nextElement();
            HashMap<String, String> names;
            int nameStart;
            if (propName.startsWith("library@")) {
                names = libNames;
                nameStart = "library@".length();
            } else if (propName.startsWith("project@")) {
                names = projectNames;
                nameStart = "project@".length();
            } else {
                throw new Error("Property name does not start with \"project@\" or \"library@\":  " + propName);
            }
            int nameEnd = propName.indexOf('@', nameStart);
            assert nameEnd != -1 : "Property does not have name component:  " + propName;
            String name = propName.substring(nameStart, nameEnd);
            if (!names.containsKey(name)) {
                String pfx = propName.substring(0, nameEnd + 1);
                names.put(name, pfx);
            }
        }

        for (Map.Entry<String, String> e : projectNames.entrySet()) {
            String name = e.getKey();
            String pfx = e.getValue();
            assert !projects.containsKey(name) : "cannot override project  " + name + " in  " + project(name).baseDir + " with project of the same name in  " + dir;
            String sourceDirs = get(props, pfx + "sourceDirs", f);
            String deps = get(props, pfx + "dependencies", null);
            if (deps == null) {
                deps = "";
            }
            Project proj = new Project(dir, name, sourceDirs, deps);
            String eclipseOutput = get(props, pfx + "eclipse.output", null);
            proj.eclipseOutput = eclipseOutput == null ? "bin" : eclipseOutput;
        }

        for (Map.Entry<String, String> e : libNames.entrySet()) {
            String name = e.getKey();
            String pfx = e.getValue();
            assert !libs.containsKey(name) : "cannot redefine library  " + name;
            String path = get(props, pfx + "path", f);
            boolean optional = Boolean.valueOf(get(props, pfx + "optional", null));
            String urls = get(props, pfx + "urls", null);
            if (urls == null) {
                urls = "";
            }
            Library lib = new Library(dir, name, path, !optional, urls);
            lib.eclipseContainer = get(props, pfx + "eclipse.container", null);
        }
        assert props.isEmpty() : "unhandled properties in  " + f + ":\n " + toString(props.keySet(), "\n", null) + "\n";
    }

    static String toString(Iterable< ? extends Object> iterable, String sep, String pfx) {
        StringBuilder sb = new StringBuilder(100);
        for (Object o : iterable) {
            if (sb.length() != 0) {
                sb.append(sep);
            }
            if (pfx != null) {
                sb.append(pfx);
            }
            sb.append(o);
        }
        return sb.toString();
    }

    static String asClasspath(List<Dependency> deps, boolean resolve) {
        StringBuilder cp = new StringBuilder(100);
        String prefix = getenv("cp_prefix", false);
        if (prefix != null) {
            cp.append(prefix);
        }

        for (Dependency p : deps) {
            p.appendToClasspath(cp, resolve);
        }
        String suffix = getenv("cp_suffix", false);
        if (suffix != null) {
            cp.append(File.pathSeparatorChar).append(suffix);
        }
        return cp.toString();
    }

    static List<Dependency> sortedDependencies(boolean libs) {
        List<Dependency> sorted = new ArrayList<Dependency>();
        for (Project p : projects.values()) {
            p.allDeps(sorted, libs);
        }
        return sorted;
    }

    static Set<File> projectDirs = new HashSet<File>();
    static HashMap<String, Project> projects = new HashMap<String, Project>();
    static HashMap<String, Library> libs = new HashMap<String, Library>();

    public static void main(String[] args) throws Exception {
        File maxineDir = new File(getenv("maxine_dir", true));
        projectDirs.add(maxineDir);
        loadProjects(maxineDir);
        String value = getenv("extra_projects_dirs", false);
        if (value != null) {
            for (String path : split(value)) {
                File dir = new File(path);
                assert dir.isDirectory() : "extra projects path does not denote a directory:  " + path;
                projectDirs.add(dir);
                loadProjects(dir);
            }
        }
        System.exit(process(Arrays.asList(args), System.out));
    }

    static abstract class Dependency {

        final String name;

        public Dependency(String name) {
            this.name = name;
        }

        abstract StringBuilder appendToClasspath(StringBuilder cp, boolean resolve);

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Dependency) {
                return ((Dependency) obj).name.equals(name);
            }
            return false;
        }

        @Override
        public String toString() {
            return name;
        }

        abstract void printProperties(PrintStream out);
    }

    static String expandVars(String s) {
        while (true) {
            int i = s.indexOf('$');
            if (i == -1) {
                return s;
            }
            assert i + 1 < s.length() && s.charAt(i + 1) == '{' : "env vars names must be enclosed in '${' and '}'";
            int e = s.indexOf('}', i + 1);
            assert e != -1 : "env vars names must be enclosed in '${' and '}'";
            String varName = s.substring(i + 2, e);
            s = s.substring(0, i) + getenv(varName, true) + s.substring(e + 1);
        }
    }

    static final class Library extends Dependency {

        final File baseDir;
        final String path;
        final boolean mustExist;
        final String urls;

        String eclipseContainer;

        public Library(File baseDir, String name, String path, boolean mustExist, String urls) {
            super(name);
            this.baseDir = baseDir;
            this.path = path;
            this.mustExist = mustExist;
            this.urls = urls.trim();
            assert !projects.containsKey(name) : name + " cannot be both a library and a project";
            libs.put(name, this);
        }

        public File path() {
            return new File(expandVars(path));
        }

        @Override
        StringBuilder appendToClasspath(StringBuilder cp, boolean resolve) {
            File lib = path();
            if (!lib.isAbsolute()) {
                lib = new File(baseDir, lib.getPath());
            }
            if (resolve && mustExist && !lib.exists()) {
                assert !urls.isEmpty() : "cannot find required library  " + name;
                download(lib, split(urls));
            }

            if (lib.exists() || !resolve) {
                if (cp.length() != 0) {
                    cp.append(File.pathSeparatorChar);
                }
                cp.append(lib.getPath());
            }
            return cp;
        }

        @Override
        void printProperties(PrintStream out) {
            out.println("library@" + name + "@path=" + path);
            out.println("library@" + name + "@optional=" + !mustExist);
            out.println("library@" + name + "@urls=" + jmax.toString(Arrays.asList(urls), ", ", null));
            if (eclipseContainer != null) {
                out.println("library@ " + name + "@eclipse.container= " + eclipseContainer);
            }
        }
    }



    /**
     * Downloads content from a given URL to a given file.
     *
     * @param dst where to write the content
     * @param urls the URLs to try, stopping after the first successful one
     */
    static void download(File dst, String[] urls) {
        File parent = dst.getParentFile();
        makeDirectory(parent);

        // Enable use of system proxies
        System.setProperty("java.net.useSystemProxies", "true");

        String proxy = getenv("HTTP_PROXY", false);
        String proxyMsg = "";
        if (proxy != null) {
            Pattern p = Pattern.compile("(?:http://)?([^:]+)(:\\d+)?");
            Matcher m = p.matcher(proxy);
            if (m.matches()) {
                String host = m.group(1);
                String port = m.group(2);
                System.setProperty("http.proxyHost", host);
                if (port != null) {
                    port = port.substring(1); // strip ':'
                    System.setProperty("http.proxyPort", port);
                }
                proxyMsg = " via proxy  " + proxy;
            } else {
                log.println("Value of HTTP_PROXY is not valid:  " + proxy);
            }
        } else {
            log.println("** If behind a firewall without direct internet access, use the HTTP_PROXY environment variable (e.g. 'env HTTP_PROXY=proxy.company.com:80 max ...') or download manually with a web browser.");
        }

        for (String s : urls) {
            try {
                log.println("Downloading  " + s + " to  " + dst + proxyMsg);
                URL url = new URL(s);
                URLConnection conn = url.openConnection();
                // 10 second timeout to establish connection
                conn.setConnectTimeout(10000);
                InputStream in = conn.getInputStream();
                int size = conn.getContentLength();
                FileOutputStream out = new FileOutputStream(dst);
                int read = 0;
                byte[] buf = new byte[8192];
                int n = 0;
                while ((read = in.read(buf)) != -1) {
                    n += read;
                    log.print("\r " + n + " bytes " + (size == -1 ? "" : " ( " + (n * 100 / size) + "%)"));
                    out.write(buf, 0, read);
                }
                log.println();
                out.close();
                in.close();
                return;
            } catch (MalformedURLException e) {
                throw new Error("Error in URL " + s, e);
            } catch (IOException e) {
                log.println("Error reading from  " + s + ":  " + e);
                dst.delete();
            }
        }
        throw new Error("Could not download content to  " + dst + " from  " + Arrays.toString(urls));
    }

    private static void makeDirectory(File directory) {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new Error("Could not make directory " + directory);
        }
    }

    static String[] split(String list) {
        if (list.isEmpty()) {
            return new String[0];
        }
        return list.split("\\s*[ ,]\\s*");
    }

    static byte[] readFile(File file) {
        assert file.isFile();
        long size = file.length();
        assert size == (int) size;
        byte[] buf = new byte[(int) size];
        try {
            FileInputStream in = new FileInputStream(file);
            new DataInputStream(in).readFully(buf);
            in.close();
        } catch (IOException e) {
            throw new Error("Error reading file " + file, e);
        }
        return buf;
    }

    static ArrayList<String> readLines(File file) {
        ArrayList<String> lines = new ArrayList<String>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
            br.close();
            return lines;
        } catch (IOException e) {
            throw new Error("Error reading file " + file, e);
        }
    }

    /**
     * Utility for creating or modifying a file.
     */
    static abstract class FileUpdater {
        int changedFiles;
        public FileUpdater(File file, boolean canOverwrite, PrintStream status) {
            boolean exists = file.exists();
            if (exists && !canOverwrite) {
                return;
            }
            try {
                byte[] old = exists ? readFile(file) : null;
                ByteArrayOutputStream baos = new ByteArrayOutputStream(old == null ? 8192 : old.length);
                PrintStream out = new PrintStream(baos);
                generate(out);
                out.close();
                byte[] buf = baos.toByteArray();
                if (old != null && Arrays.equals(old, buf)) {
                    return;
                }
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(buf);
                fos.close();
                status.println((exists ? "modified " : "created ") + file);
                changedFiles = 1;
            } catch (IOException e) {
                throw new Error("Error while writing to " + file, e);
            }
        }

        abstract void generate(PrintStream out);
    }

    static final class Project extends Dependency {

        final File baseDir;
        final List<String> sourceDirs;

        /**
         * Default is "bin".
         */
        String eclipseOutput;

        /**
         * The direct dependencies of this project.
         */
        final List<String> dependencies;

        public Project(File baseDir, String name, String sourceDirs, String deps) {
            super(name);
            this.baseDir = baseDir;
            assert sourceDirs != null && !sourceDirs.isEmpty();
            this.sourceDirs = Arrays.asList(split(sourceDirs));
            if (deps == null || deps.isEmpty()) {
                this.dependencies = Collections.emptyList();
            } else {
                this.dependencies = Arrays.asList(split(deps));
            }

            assert !libs.containsKey(name) : name + " cannot be both a library and a project";
            projects.put(name, this);
        }

        @Override
        public String toString() {
            return name;
        }

        /**
         * Gets the transitive dependencies of this project (including this project itself).
         */
        List<Dependency> allDeps(List<Dependency> deps, boolean libs) {
            if (deps == null) {
                deps = new ArrayList<Dependency>();
            } else if (deps.contains(this)) {
                return deps;
            }

            for (String name : dependencies) {
                assert !name.equals(this.name);
                Library l = jmax.libs.get(name);
                if (l != null) {
                    if (libs && !deps.contains(l)) {
                        deps.add(l);
                    }
                } else {
                    Project p = project(name);
                    if (!deps.contains(p)) {
                        p.allDeps(deps, libs);
                    }
                }
            }
            if (!deps.contains(this)) {
                deps.add(this);
            }

            return deps;
        }

        String outputDir() {
            char sep = File.separatorChar;
            String ide = getenv("MAXINE_IDE", false);
            if (ide != null && ide.equals("INTELLIJ")) {
                return baseDir.getPath() + sep + "out " + sep + "production " + sep + name;
            }
            return baseDir.getPath() + sep + name + sep + "bin";
        }

        @Override
        StringBuilder appendToClasspath(StringBuilder cp, boolean resolve) {
            char sep = File.separatorChar;
            if (cp.length() != 0) {
                cp.append(File.pathSeparatorChar);
            }
            String ide = getenv("MAXINE_IDE", false);
            if (ide != null && ide.equals("INTELLIJ")) {
                cp.append(baseDir.getPath()).append(sep).append("out").append(sep).append("production").append(sep).append(name);
            } else {
                cp.append(baseDir.getPath()).append(sep).append(name).append(sep).append("bin");
            }
            File classes = new File(baseDir, name + sep + "classes");
            if (classes.isDirectory()) {
                cp.append(File.pathSeparatorChar).append(classes.getPath());
            }
            return cp;
        }

        @Override
        void printProperties(PrintStream out) {
            out.println("project@" + name + "@sourceDirs=" + jmax.toString(sourceDirs, ", ", null));
            out.println("project@" + name + "@dependencies=" + jmax.toString(dependencies, ", ", null));
            out.println("project@" + name + "@eclipse.output=" + eclipseOutput);
        }

        /**
         * Generates the Eclipse projects files for this project. Relative to the project directory, these files are:
         * <pre>
         *     .classpath
         *     .project
         * </pre>
         * In addition, these files are generated if they don't already exist:
         * <pre>
         *     .settings/org.eclipse.jdt.core.prefs
         *     .settings/org.eclipse.jdt.ui.prefs
         * </pre>
         *
         * @param response
         * @return number of files created/updated
         */
        int generateEclipseProject(PrintStream response) {
            final File projectDir = new File(baseDir, name);
            makeDirectory(projectDir);

            int changedFiles = 0;

            FileUpdater update = new FileUpdater(new File(projectDir, ".classpath"), true, response) {
                @Override
                void generate(PrintStream out) {
                    out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                    out.println("<classpath>");
                    for (String src : sourceDirs) {
                        File srcDir = new File(projectDir, src);
                        makeDirectory(srcDir);
                        out.println("\t<classpathentry kind=\"src\" path=\"" + src + "\"/>");
                    }

                    // Every Java program depends on the JRE
                    out.println("\t<classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\"/>");

                    for (String name : dependencies) {
                        Library l = jmax.libs.get(name);
                        if (l != null) {
                            if (l.eclipseContainer != null) {
                                out.println("\t<classpathentry exported=\"true\" kind=\"con\" path=\"" + l.eclipseContainer + "\"/>");
                            } else {
                                File path = l.path();
                                if (l.mustExist) {
                                    if (path.isAbsolute()) {
                                        out.println("\t<classpathentry exported=\"true\" kind=\"lib\" path=\"" + path + "\"/>");
                                    } else {
                                        out.println("\t<classpathentry exported=\"true\" kind=\"lib\" path=\"/" + path + "\"/>");
                                    }
                                }
                            }
                        } else {
                            Project p = project(name);
                            out.println("\t<classpathentry combineaccessrules=\"false\" exported=\"true\" kind=\"src\" path=\"/" + p.name + "\"/>");
                        }
                    }
                    out.println("\t<classpathentry kind=\"output\" path=\"" + eclipseOutput + "\"/>");
                    out.println("</classpath>");
                }
            };
            changedFiles += update.changedFiles;

            File dotCheckstyle = new File(projectDir, ".checkstyle");
            update = new FileUpdater(dotCheckstyle, true, response) {
                @Override
                void generate(PrintStream out) {
                    String checkstyleConfigPath;
                    File projectCheckstyleConfig = new File(projectDir, ".checkstyle_checks.xml");
                    if (projectCheckstyleConfig.exists()) {
                        checkstyleConfigPath = "/" + name + "/.checkstyle_checks.xml";
                    } else {
                        Project p = project("com.oracle.max.base");
                        File sharedCheckstyleConfig = new File(new File(p.baseDir, p.name), ".checkstyle_checks.xml");
                        if (!sharedCheckstyleConfig.exists()) {
                            throw new InternalError("Shared checkstyle config file not found: " + sharedCheckstyleConfig);
                        }
                        checkstyleConfigPath = "/" + p.name + "/.checkstyle_checks.xml";
                    }
                    out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                    out.println("<fileset-config file-format-version=\"1.2.0\" simple-config=\"true\">");
                    out.println("\t<local-check-config name=\"Maxine Checks\" location=\"" + checkstyleConfigPath + "\" type=\"project\" description=\"\">");
                    out.println("\t\t<additional-data name=\"protect-config-file\" value=\"false\"/>");
                    out.println("\t</local-check-config>");
                    out.println("\t<fileset name=\"all\" enabled=\"true\" check-config-name=\"Maxine Checks\" local=\"true\">");
                    out.println("\t\t<file-match-pattern match-pattern=\".\" include-pattern=\"true\"/>");
                    out.println("\t</fileset>");
                    out.println("\t<filter name=\"FileTypesFilter\" enabled=\"true\">");
                    out.println("\t\t<filter-data value=\"java\"/>");
                    out.println("\t</filter>");

                    File exclude = new File(projectDir, ".checkstyle.exclude");
                    if (exclude.exists()) {
                        out.println("\t<filter name=\"FilesFromPackage\" enabled=\"true\">");
                        for (String line : readLines(exclude)) {
                            if (!line.startsWith("#")) {
                                File exclDir = new File(projectDir, line);
                                assert exclDir.isDirectory() : "excluded source directory listed in " + exclude + " does not exist or is not a directory: " + exclDir;
                                out.println("\t\t<filter-data value=\"" + line + "\"/>");
                            }
                        }
                        out.println("\t</filter>");
                    }
                    out.println("</fileset-config>");
                }
            };
            changedFiles += update.changedFiles;

            update = new FileUpdater(new File(projectDir, ".project"), true, response) {
                @Override
                void generate(PrintStream out) {
                    out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                    out.println("<projectDescription>");
                    out.println("\t<name>" + name + "</name>");
                    out.println("\t<comment></comment>");
                    out.println("\t<projects>");
                    out.println("\t</projects>");
                    out.println("\t<buildSpec>");
                    out.println("\t\t<buildCommand>");
                    out.println("\t\t\t<name>org.eclipse.jdt.core.javabuilder</name>");
                    out.println("\t\t\t<arguments>");
                    out.println("\t\t\t</arguments>");
                    out.println("\t\t</buildCommand>");
                    out.println("\t\t<buildCommand>");
                    out.println("\t\t\t<name>net.sf.eclipsecs.core.CheckstyleBuilder</name>");
                    out.println("\t\t\t<arguments>");
                    out.println("\t\t\t</arguments>");
                    out.println("\t\t</buildCommand>");
                    out.println("\t</buildSpec>");
                    out.println("\t<natures>");
                    out.println("\t\t<nature>org.eclipse.jdt.core.javanature</nature>");
                    out.println("\t\t<nature>net.sf.eclipsecs.core.CheckstyleNature</nature>");
                    out.println("\t</natures>");
                    out.println("</projectDescription>");
                }
            };
            changedFiles += update.changedFiles;

            File settingsDir = new File(projectDir, ".settings");
            makeDirectory(settingsDir);

            update = new FileUpdater(new File(settingsDir, "org.eclipse.jdt.core.prefs"), true, response) {
                @Override
                void generate(PrintStream out) {
                    for (String line : org_eclipse_jdt_core_prefs.lines) {
                        out.println(line);
                    }
                }
            };
            changedFiles += update.changedFiles;

            update = new FileUpdater(new File(settingsDir, "org.eclipse.jdt.ui.prefs"), true, response) {
                @Override
                void generate(PrintStream out) {
                    for (String line : org_eclipse_jdt_ui_prefs.lines) {
                        out.println(line);
                    }
                }
            };
            changedFiles += update.changedFiles;

            return changedFiles;
        }
    }

    static class org_eclipse_jdt_ui_prefs {
        static final String[] lines = {
            "cleanup.add_default_serial_version_id=false",
            "cleanup.add_generated_serial_version_id=false",
            "cleanup.add_missing_annotations=false",
            "cleanup.add_missing_deprecated_annotations=false",
            "cleanup.add_missing_nls_tags=false",
            "cleanup.add_missing_override_annotations=false",
            "cleanup.add_serial_version_id=false",
            "cleanup.always_use_blocks=false",
            "cleanup.always_use_parentheses_in_expressions=false",
            "cleanup.always_use_this_for_non_static_field_access=false",
            "cleanup.always_use_this_for_non_static_method_access=false",
            "cleanup.convert_to_enhanced_for_loop=false",
            "cleanup.format_source_code=false",
            "cleanup.make_local_variable_final=false",
            "cleanup.make_parameters_final=false",
            "cleanup.make_private_fields_final=true",
            "cleanup.make_variable_declarations_final=false",
            "cleanup.never_use_blocks=false",
            "cleanup.never_use_parentheses_in_expressions=false",
            "cleanup.organize_imports=false",
            "cleanup.qualify_static_field_accesses_with_declaring_class=false",
            "cleanup.qualify_static_member_accesses_through_instances_with_declaring_class=false",
            "cleanup.qualify_static_member_accesses_through_subtypes_with_declaring_class=false",
            "cleanup.qualify_static_member_accesses_with_declaring_class=false",
            "cleanup.qualify_static_method_accesses_with_declaring_class=false",
            "cleanup.remove_private_constructors=false",
            "cleanup.remove_trailing_whitespaces=false",
            "cleanup.remove_trailing_whitespaces_all=false",
            "cleanup.remove_trailing_whitespaces_ignore_empty=false",
            "cleanup.remove_unnecessary_casts=false",
            "cleanup.remove_unnecessary_nls_tags=false",
            "cleanup.remove_unused_imports=false",
            "cleanup.remove_unused_local_variables=false",
            "cleanup.remove_unused_private_fields=false",
            "cleanup.remove_unused_private_members=false",
            "cleanup.remove_unused_private_methods=false",
            "cleanup.remove_unused_private_types=false",
            "cleanup.sort_members=false",
            "cleanup.sort_members_all=false",
            "cleanup.use_blocks=false",
            "cleanup.use_blocks_only_for_return_and_throw=false",
            "cleanup.use_parentheses_in_expressions=false",
            "cleanup.use_this_for_non_static_field_access=false",
            "cleanup.use_this_for_non_static_field_access_only_if_necessary=false",
            "cleanup.use_this_for_non_static_method_access=false",
            "cleanup.use_this_for_non_static_method_access_only_if_necessary=false",
            "cleanup_profile=_CleanUpAgitarTests",
            "cleanup_settings_version=2",
            "comment_clear_blank_lines=false",
            "comment_format_comments=true",
            "comment_format_header=true",
            "comment_format_html=true",
            "comment_format_source_code=true",
            "comment_indent_parameter_description=true",
            "comment_indent_root_tags=true",
            "comment_line_length=120",
            "comment_new_line_for_parameter=true",
            "comment_separate_root_tags=true",
            "eclipse.preferences.version=1",
            "editor_save_participant_org.eclipse.jdt.ui.postsavelistener.cleanup=true",
            "formatter_profile=_MaxineJavaCodeStyle",
            "formatter_settings_version=11",
            "org.eclipse.jdt.ui.exception.name=e",
            "org.eclipse.jdt.ui.gettersetter.use.is=true",
            "org.eclipse.jdt.ui.ignorelowercasenames=true",
            "org.eclipse.jdt.ui.importorder=java;javax;org;com;",
            "org.eclipse.jdt.ui.javadoc=false",
            "org.eclipse.jdt.ui.keywordthis=false",
            "org.eclipse.jdt.ui.ondemandthreshold=0",
            "org.eclipse.jdt.ui.overrideannotation=true",
            "org.eclipse.jdt.ui.staticondemandthreshold=0",
            "sp_cleanup.add_default_serial_version_id=false",
            "sp_cleanup.add_generated_serial_version_id=false",
            "sp_cleanup.add_missing_annotations=false",
            "sp_cleanup.add_missing_deprecated_annotations=false",
            "sp_cleanup.add_missing_methods=false",
            "sp_cleanup.add_missing_nls_tags=false",
            "sp_cleanup.add_missing_override_annotations=false",
            "sp_cleanup.add_serial_version_id=false",
            "sp_cleanup.always_use_blocks=false",
            "sp_cleanup.always_use_parentheses_in_expressions=false",
            "sp_cleanup.always_use_this_for_non_static_field_access=false",
            "sp_cleanup.always_use_this_for_non_static_method_access=false",
            "sp_cleanup.convert_to_enhanced_for_loop=false",
            "sp_cleanup.correct_indentation=false",
            "sp_cleanup.format_source_code=false",
            "sp_cleanup.format_source_code_changes_only=false",
            "sp_cleanup.make_local_variable_final=false",
            "sp_cleanup.make_parameters_final=false",
            "sp_cleanup.make_private_fields_final=false",
            "sp_cleanup.make_variable_declarations_final=false",
            "sp_cleanup.never_use_blocks=false",
            "sp_cleanup.never_use_parentheses_in_expressions=false",
            "sp_cleanup.on_save_use_additional_actions=true",
            "sp_cleanup.organize_imports=false",
            "sp_cleanup.qualify_static_field_accesses_with_declaring_class=false",
            "sp_cleanup.qualify_static_member_accesses_through_instances_with_declaring_class=false",
            "sp_cleanup.qualify_static_member_accesses_through_subtypes_with_declaring_class=false",
            "sp_cleanup.qualify_static_member_accesses_with_declaring_class=false",
            "sp_cleanup.qualify_static_method_accesses_with_declaring_class=false",
            "sp_cleanup.remove_private_constructors=false",
            "sp_cleanup.remove_trailing_whitespaces=true",
            "sp_cleanup.remove_trailing_whitespaces_all=true",
            "sp_cleanup.remove_trailing_whitespaces_ignore_empty=false",
            "sp_cleanup.remove_unnecessary_casts=false",
            "sp_cleanup.remove_unnecessary_nls_tags=false",
            "sp_cleanup.remove_unused_imports=false",
            "sp_cleanup.remove_unused_local_variables=false",
            "sp_cleanup.remove_unused_private_fields=false",
            "sp_cleanup.remove_unused_private_members=false",
            "sp_cleanup.remove_unused_private_methods=false",
            "sp_cleanup.remove_unused_private_types=false",
            "sp_cleanup.sort_members=false",
            "sp_cleanup.sort_members_all=false",
            "sp_cleanup.use_blocks=false",
            "sp_cleanup.use_blocks_only_for_return_and_throw=false",
            "sp_cleanup.use_parentheses_in_expressions=false",
            "sp_cleanup.use_this_for_non_static_field_access=false",
            "sp_cleanup.use_this_for_non_static_field_access_only_if_necessary=false",
            "sp_cleanup.use_this_for_non_static_method_access=false",
            "sp_cleanup.use_this_for_non_static_method_access_only_if_necessary=false"
        };
    }

    static class org_eclipse_jdt_core_prefs {
        static final String[] lines = {
            "eclipse.preferences.version=1",
            "org.eclipse.jdt.core.builder.cleanOutputFolder=clean",
            "org.eclipse.jdt.core.builder.duplicateResourceTask=warning",
            "org.eclipse.jdt.core.builder.invalidClasspath=abort",
            "org.eclipse.jdt.core.builder.resourceCopyExclusionFilter=*.launch",
            "org.eclipse.jdt.core.circularClasspath=error",
            "org.eclipse.jdt.core.classpath.exclusionPatterns=enabled",
            "org.eclipse.jdt.core.classpath.multipleOutputLocations=enabled",
            "org.eclipse.jdt.core.codeComplete.argumentPrefixes=",
            "org.eclipse.jdt.core.codeComplete.argumentSuffixes=",
            "org.eclipse.jdt.core.codeComplete.fieldPrefixes=",
            "org.eclipse.jdt.core.codeComplete.fieldSuffixes=",
            "org.eclipse.jdt.core.codeComplete.localPrefixes=",
            "org.eclipse.jdt.core.codeComplete.localSuffixes=",
            "org.eclipse.jdt.core.codeComplete.staticFieldPrefixes=",
            "org.eclipse.jdt.core.codeComplete.staticFieldSuffixes=",
            "org.eclipse.jdt.core.compiler.codegen.inlineJsrBytecode=enabled",
            "org.eclipse.jdt.core.compiler.codegen.targetPlatform=1.6",
            "org.eclipse.jdt.core.compiler.codegen.unusedLocal=preserve",
            "org.eclipse.jdt.core.compiler.compliance=1.6",
            "org.eclipse.jdt.core.compiler.debug.lineNumber=generate",
            "org.eclipse.jdt.core.compiler.debug.localVariable=generate",
            "org.eclipse.jdt.core.compiler.debug.sourceFile=generate",
            "org.eclipse.jdt.core.compiler.doc.comment.support=enabled",
            "org.eclipse.jdt.core.compiler.maxProblemPerUnit=100",
            "org.eclipse.jdt.core.compiler.problem.annotationSuperInterface=warning",
            "org.eclipse.jdt.core.compiler.problem.assertIdentifier=error",
            "org.eclipse.jdt.core.compiler.problem.autoboxing=ignore",
            "org.eclipse.jdt.core.compiler.problem.comparingIdentical=warning",
            "org.eclipse.jdt.core.compiler.problem.deadCode=ignore",
            "org.eclipse.jdt.core.compiler.problem.deprecation=error",
            "org.eclipse.jdt.core.compiler.problem.deprecationInDeprecatedCode=enabled",
            "org.eclipse.jdt.core.compiler.problem.deprecationWhenOverridingDeprecatedMethod=enabled",
            "org.eclipse.jdt.core.compiler.problem.discouragedReference=warning",
            "org.eclipse.jdt.core.compiler.problem.emptyStatement=warning",
            "org.eclipse.jdt.core.compiler.problem.enumIdentifier=error",
            "org.eclipse.jdt.core.compiler.problem.fallthroughCase=ignore",
            "org.eclipse.jdt.core.compiler.problem.fatalOptionalError=enabled",
            "org.eclipse.jdt.core.compiler.problem.fieldHiding=warning",
            "org.eclipse.jdt.core.compiler.problem.finalParameterBound=warning",
            "org.eclipse.jdt.core.compiler.problem.finallyBlockNotCompletingNormally=ignore",
            "org.eclipse.jdt.core.compiler.problem.forbiddenReference=warning",
            "org.eclipse.jdt.core.compiler.problem.hiddenCatchBlock=warning",
            "org.eclipse.jdt.core.compiler.problem.incompatibleNonInheritedInterfaceMethod=warning",
            "org.eclipse.jdt.core.compiler.problem.incompleteEnumSwitch=ignore",
            "org.eclipse.jdt.core.compiler.problem.indirectStaticAccess=ignore",
            "org.eclipse.jdt.core.compiler.problem.invalidJavadoc=ignore",
            "org.eclipse.jdt.core.compiler.problem.invalidJavadocTags=enabled",
            "org.eclipse.jdt.core.compiler.problem.invalidJavadocTagsDeprecatedRef=enabled",
            "org.eclipse.jdt.core.compiler.problem.invalidJavadocTagsNotVisibleRef=enabled",
            "org.eclipse.jdt.core.compiler.problem.invalidJavadocTagsVisibility=private",
            "org.eclipse.jdt.core.compiler.problem.localVariableHiding=ignore",
            "org.eclipse.jdt.core.compiler.problem.methodWithConstructorName=error",
            "org.eclipse.jdt.core.compiler.problem.missingDeprecatedAnnotation=error",
            "org.eclipse.jdt.core.compiler.problem.missingHashCodeMethod=ignore",
            "org.eclipse.jdt.core.compiler.problem.missingJavadocComments=ignore",
            "org.eclipse.jdt.core.compiler.problem.missingJavadocCommentsOverriding=enabled",
            "org.eclipse.jdt.core.compiler.problem.missingJavadocCommentsVisibility=public",
            "org.eclipse.jdt.core.compiler.problem.missingJavadocTags=ignore",
            "org.eclipse.jdt.core.compiler.problem.missingJavadocTagsOverriding=enabled",
            "org.eclipse.jdt.core.compiler.problem.missingJavadocTagsVisibility=private",
            "org.eclipse.jdt.core.compiler.problem.missingOverrideAnnotation=error",
            "org.eclipse.jdt.core.compiler.problem.missingOverrideAnnotationForInterfaceMethodImplementation=disabled",
            "org.eclipse.jdt.core.compiler.problem.missingSerialVersion=ignore",
            "org.eclipse.jdt.core.compiler.problem.missingSynchronizedOnInheritedMethod=ignore",
            "org.eclipse.jdt.core.compiler.problem.noEffectAssignment=warning",
            "org.eclipse.jdt.core.compiler.problem.noImplicitStringConversion=warning",
            "org.eclipse.jdt.core.compiler.problem.nonExternalizedStringLiteral=ignore",
            "org.eclipse.jdt.core.compiler.problem.nullReference=warning",
            "org.eclipse.jdt.core.compiler.problem.overridingPackageDefaultMethod=warning",
            "org.eclipse.jdt.core.compiler.problem.parameterAssignment=ignore",
            "org.eclipse.jdt.core.compiler.problem.possibleAccidentalBooleanAssignment=warning",
            "org.eclipse.jdt.core.compiler.problem.potentialNullReference=ignore",
            "org.eclipse.jdt.core.compiler.problem.rawTypeReference=ignore",
            "org.eclipse.jdt.core.compiler.problem.redundantNullCheck=ignore",
            "org.eclipse.jdt.core.compiler.problem.redundantSuperinterface=ignore",
            "org.eclipse.jdt.core.compiler.problem.specialParameterHidingField=disabled",
            "org.eclipse.jdt.core.compiler.problem.staticAccessReceiver=warning",
            "org.eclipse.jdt.core.compiler.problem.suppressOptionalErrors=disabled",
            "org.eclipse.jdt.core.compiler.problem.suppressWarnings=enabled",
            "org.eclipse.jdt.core.compiler.problem.syntheticAccessEmulation=ignore",
            "org.eclipse.jdt.core.compiler.problem.typeParameterHiding=warning",
            "org.eclipse.jdt.core.compiler.problem.uncheckedTypeOperation=warning",
            "org.eclipse.jdt.core.compiler.problem.undocumentedEmptyBlock=ignore",
            "org.eclipse.jdt.core.compiler.problem.unhandledWarningToken=error",
            "org.eclipse.jdt.core.compiler.problem.unnecessaryElse=ignore",
            "org.eclipse.jdt.core.compiler.problem.unnecessaryTypeCheck=warning",
            "org.eclipse.jdt.core.compiler.problem.unqualifiedFieldAccess=ignore",
            "org.eclipse.jdt.core.compiler.problem.unsafeTypeOperation=warning",
            "org.eclipse.jdt.core.compiler.problem.unusedDeclaredThrownException=ignore",
            "org.eclipse.jdt.core.compiler.problem.unusedDeclaredThrownExceptionExemptExceptionAndThrowable=enabled",
            "org.eclipse.jdt.core.compiler.problem.unusedDeclaredThrownExceptionIncludeDocCommentReference=enabled",
            "org.eclipse.jdt.core.compiler.problem.unusedDeclaredThrownExceptionWhenOverriding=disabled",
            "org.eclipse.jdt.core.compiler.problem.unusedImport=warning",
            "org.eclipse.jdt.core.compiler.problem.unusedLabel=warning",
            "org.eclipse.jdt.core.compiler.problem.unusedLocal=warning",
            "org.eclipse.jdt.core.compiler.problem.unusedObjectAllocation=ignore",
            "org.eclipse.jdt.core.compiler.problem.unusedParameter=ignore",
            "org.eclipse.jdt.core.compiler.problem.unusedParameterIncludeDocCommentReference=enabled",
            "org.eclipse.jdt.core.compiler.problem.unusedParameterWhenImplementingAbstract=disabled",
            "org.eclipse.jdt.core.compiler.problem.unusedParameterWhenOverridingConcrete=disabled",
            "org.eclipse.jdt.core.compiler.problem.unusedPrivateMember=ignore",
            "org.eclipse.jdt.core.compiler.problem.unusedWarningToken=ignore",
            "org.eclipse.jdt.core.compiler.problem.varargsArgumentNeedCast=warning",
            "org.eclipse.jdt.core.compiler.processAnnotations=disabled",
            "org.eclipse.jdt.core.compiler.source=1.6",
            "org.eclipse.jdt.core.compiler.taskCaseSensitive=enabled",
            "org.eclipse.jdt.core.compiler.taskPriorities=NORMAL,HIGH,NORMAL",
            "org.eclipse.jdt.core.compiler.taskTags=TODO,FIXME,XXX",
            "org.eclipse.jdt.core.formatter.align_type_members_on_columns=false",
            "org.eclipse.jdt.core.formatter.alignment_for_arguments_in_allocation_expression=16",
            "org.eclipse.jdt.core.formatter.alignment_for_arguments_in_enum_constant=16",
            "org.eclipse.jdt.core.formatter.alignment_for_arguments_in_explicit_constructor_call=16",
            "org.eclipse.jdt.core.formatter.alignment_for_arguments_in_method_invocation=16",
            "org.eclipse.jdt.core.formatter.alignment_for_arguments_in_qualified_allocation_expression=16",
            "org.eclipse.jdt.core.formatter.alignment_for_assignment=0",
            "org.eclipse.jdt.core.formatter.alignment_for_binary_expression=16",
            "org.eclipse.jdt.core.formatter.alignment_for_compact_if=16",
            "org.eclipse.jdt.core.formatter.alignment_for_conditional_expression=80",
            "org.eclipse.jdt.core.formatter.alignment_for_enum_constants=0",
            "org.eclipse.jdt.core.formatter.alignment_for_expressions_in_array_initializer=16",
            "org.eclipse.jdt.core.formatter.alignment_for_multiple_fields=16",
            "org.eclipse.jdt.core.formatter.alignment_for_parameters_in_constructor_declaration=16",
            "org.eclipse.jdt.core.formatter.alignment_for_parameters_in_method_declaration=16",
            "org.eclipse.jdt.core.formatter.alignment_for_selector_in_method_invocation=16",
            "org.eclipse.jdt.core.formatter.alignment_for_superclass_in_type_declaration=16",
            "org.eclipse.jdt.core.formatter.alignment_for_superinterfaces_in_enum_declaration=16",
            "org.eclipse.jdt.core.formatter.alignment_for_superinterfaces_in_type_declaration=16",
            "org.eclipse.jdt.core.formatter.alignment_for_throws_clause_in_constructor_declaration=16",
            "org.eclipse.jdt.core.formatter.alignment_for_throws_clause_in_method_declaration=16",
            "org.eclipse.jdt.core.formatter.blank_lines_after_imports=1",
            "org.eclipse.jdt.core.formatter.blank_lines_after_package=1",
            "org.eclipse.jdt.core.formatter.blank_lines_before_field=0",
            "org.eclipse.jdt.core.formatter.blank_lines_before_first_class_body_declaration=1",
            "org.eclipse.jdt.core.formatter.blank_lines_before_imports=1",
            "org.eclipse.jdt.core.formatter.blank_lines_before_member_type=1",
            "org.eclipse.jdt.core.formatter.blank_lines_before_method=1",
            "org.eclipse.jdt.core.formatter.blank_lines_before_new_chunk=1",
            "org.eclipse.jdt.core.formatter.blank_lines_before_package=0",
            "org.eclipse.jdt.core.formatter.blank_lines_between_import_groups=1",
            "org.eclipse.jdt.core.formatter.blank_lines_between_type_declarations=1",
            "org.eclipse.jdt.core.formatter.brace_position_for_annotation_type_declaration=end_of_line",
            "org.eclipse.jdt.core.formatter.brace_position_for_anonymous_type_declaration=end_of_line",
            "org.eclipse.jdt.core.formatter.brace_position_for_array_initializer=end_of_line",
            "org.eclipse.jdt.core.formatter.brace_position_for_block=end_of_line",
            "org.eclipse.jdt.core.formatter.brace_position_for_block_in_case=end_of_line",
            "org.eclipse.jdt.core.formatter.brace_position_for_constructor_declaration=end_of_line",
            "org.eclipse.jdt.core.formatter.brace_position_for_enum_constant=end_of_line",
            "org.eclipse.jdt.core.formatter.brace_position_for_enum_declaration=end_of_line",
            "org.eclipse.jdt.core.formatter.brace_position_for_method_declaration=end_of_line",
            "org.eclipse.jdt.core.formatter.brace_position_for_switch=end_of_line",
            "org.eclipse.jdt.core.formatter.brace_position_for_type_declaration=end_of_line",
            "org.eclipse.jdt.core.formatter.comment.clear_blank_lines=false",
            "org.eclipse.jdt.core.formatter.comment.clear_blank_lines_in_block_comment=false",
            "org.eclipse.jdt.core.formatter.comment.clear_blank_lines_in_javadoc_comment=false",
            "org.eclipse.jdt.core.formatter.comment.format_block_comments=true",
            "org.eclipse.jdt.core.formatter.comment.format_comments=true",
            "org.eclipse.jdt.core.formatter.comment.format_header=true",
            "org.eclipse.jdt.core.formatter.comment.format_html=true",
            "org.eclipse.jdt.core.formatter.comment.format_javadoc_comments=true",
            "org.eclipse.jdt.core.formatter.comment.format_line_comments=true",
            "org.eclipse.jdt.core.formatter.comment.format_source_code=true",
            "org.eclipse.jdt.core.formatter.comment.indent_parameter_description=true",
            "org.eclipse.jdt.core.formatter.comment.indent_root_tags=true",
            "org.eclipse.jdt.core.formatter.comment.insert_new_line_before_root_tags=insert",
            "org.eclipse.jdt.core.formatter.comment.insert_new_line_for_parameter=do not insert",
            "org.eclipse.jdt.core.formatter.comment.line_length=120",
            "org.eclipse.jdt.core.formatter.compact_else_if=true",
            "org.eclipse.jdt.core.formatter.continuation_indentation=4",
            "org.eclipse.jdt.core.formatter.continuation_indentation_for_array_initializer=4",
            "org.eclipse.jdt.core.formatter.format_guardian_clause_on_one_line=false",
            "org.eclipse.jdt.core.formatter.indent_body_declarations_compare_to_annotation_declaration_header=true",
            "org.eclipse.jdt.core.formatter.indent_body_declarations_compare_to_enum_constant_header=true",
            "org.eclipse.jdt.core.formatter.indent_body_declarations_compare_to_enum_declaration_header=true",
            "org.eclipse.jdt.core.formatter.indent_body_declarations_compare_to_type_header=true",
            "org.eclipse.jdt.core.formatter.indent_breaks_compare_to_cases=true",
            "org.eclipse.jdt.core.formatter.indent_empty_lines=false",
            "org.eclipse.jdt.core.formatter.indent_statements_compare_to_block=true",
            "org.eclipse.jdt.core.formatter.indent_statements_compare_to_body=true",
            "org.eclipse.jdt.core.formatter.indent_switchstatements_compare_to_cases=true",
            "org.eclipse.jdt.core.formatter.indent_switchstatements_compare_to_switch=true",
            "org.eclipse.jdt.core.formatter.indentation.size=8",
            "org.eclipse.jdt.core.formatter.insert_new_line_after_annotation=insert",
            "org.eclipse.jdt.core.formatter.insert_new_line_after_annotation_on_local_variable=insert",
            "org.eclipse.jdt.core.formatter.insert_new_line_after_annotation_on_member=insert",
            "org.eclipse.jdt.core.formatter.insert_new_line_after_annotation_on_parameter=do not insert",
            "org.eclipse.jdt.core.formatter.insert_new_line_after_opening_brace_in_array_initializer=do not insert",
            "org.eclipse.jdt.core.formatter.insert_new_line_at_end_of_file_if_missing=do not insert",
            "org.eclipse.jdt.core.formatter.insert_new_line_before_catch_in_try_statement=do not insert",
            "org.eclipse.jdt.core.formatter.insert_new_line_before_closing_brace_in_array_initializer=do not insert",
            "org.eclipse.jdt.core.formatter.insert_new_line_before_else_in_if_statement=do not insert",
            "org.eclipse.jdt.core.formatter.insert_new_line_before_finally_in_try_statement=do not insert",
            "org.eclipse.jdt.core.formatter.insert_new_line_before_while_in_do_statement=do not insert",
            "org.eclipse.jdt.core.formatter.insert_new_line_in_empty_annotation_declaration=insert",
            "org.eclipse.jdt.core.formatter.insert_new_line_in_empty_anonymous_type_declaration=insert",
            "org.eclipse.jdt.core.formatter.insert_new_line_in_empty_block=insert",
            "org.eclipse.jdt.core.formatter.insert_new_line_in_empty_enum_constant=insert",
            "org.eclipse.jdt.core.formatter.insert_new_line_in_empty_enum_declaration=insert",
            "org.eclipse.jdt.core.formatter.insert_new_line_in_empty_method_body=insert",
            "org.eclipse.jdt.core.formatter.insert_new_line_in_empty_type_declaration=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_and_in_type_parameter=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_assignment_operator=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_at_in_annotation=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_at_in_annotation_type_declaration=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_binary_operator=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_closing_angle_bracket_in_type_arguments=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_closing_angle_bracket_in_type_parameters=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_closing_brace_in_block=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_closing_paren_in_cast=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_colon_in_assert=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_colon_in_case=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_colon_in_conditional=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_colon_in_for=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_colon_in_labeled_statement=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_comma_in_allocation_expression=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_comma_in_annotation=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_comma_in_array_initializer=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_comma_in_constructor_declaration_parameters=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_comma_in_constructor_declaration_throws=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_comma_in_enum_constant_arguments=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_comma_in_enum_declarations=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_comma_in_explicitconstructorcall_arguments=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_comma_in_for_increments=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_comma_in_for_inits=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_comma_in_method_declaration_parameters=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_comma_in_method_declaration_throws=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_comma_in_method_invocation_arguments=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_comma_in_multiple_field_declarations=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_comma_in_multiple_local_declarations=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_comma_in_parameterized_type_reference=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_comma_in_superinterfaces=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_comma_in_type_arguments=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_comma_in_type_parameters=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_ellipsis=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_opening_angle_bracket_in_parameterized_type_reference=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_opening_angle_bracket_in_type_arguments=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_opening_angle_bracket_in_type_parameters=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_opening_brace_in_array_initializer=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_opening_bracket_in_array_allocation_expression=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_opening_bracket_in_array_reference=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_opening_paren_in_annotation=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_opening_paren_in_cast=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_opening_paren_in_catch=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_opening_paren_in_constructor_declaration=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_opening_paren_in_enum_constant=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_opening_paren_in_for=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_opening_paren_in_if=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_opening_paren_in_method_declaration=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_opening_paren_in_method_invocation=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_opening_paren_in_parenthesized_expression=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_opening_paren_in_switch=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_opening_paren_in_synchronized=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_opening_paren_in_while=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_postfix_operator=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_prefix_operator=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_question_in_conditional=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_question_in_wildcard=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_semicolon_in_for=insert",
            "org.eclipse.jdt.core.formatter.insert_space_after_unary_operator=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_and_in_type_parameter=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_assignment_operator=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_at_in_annotation_type_declaration=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_binary_operator=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_closing_angle_bracket_in_parameterized_type_reference=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_closing_angle_bracket_in_type_arguments=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_closing_angle_bracket_in_type_parameters=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_closing_brace_in_array_initializer=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_closing_bracket_in_array_allocation_expression=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_closing_bracket_in_array_reference=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_closing_paren_in_annotation=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_closing_paren_in_cast=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_closing_paren_in_catch=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_closing_paren_in_constructor_declaration=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_closing_paren_in_enum_constant=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_closing_paren_in_for=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_closing_paren_in_if=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_closing_paren_in_method_declaration=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_closing_paren_in_method_invocation=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_closing_paren_in_parenthesized_expression=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_closing_paren_in_switch=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_closing_paren_in_synchronized=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_closing_paren_in_while=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_colon_in_assert=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_colon_in_case=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_colon_in_conditional=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_colon_in_default=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_colon_in_for=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_colon_in_labeled_statement=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_comma_in_allocation_expression=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_comma_in_annotation=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_comma_in_array_initializer=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_comma_in_constructor_declaration_parameters=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_comma_in_constructor_declaration_throws=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_comma_in_enum_constant_arguments=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_comma_in_enum_declarations=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_comma_in_explicitconstructorcall_arguments=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_comma_in_for_increments=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_comma_in_for_inits=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_comma_in_method_declaration_parameters=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_comma_in_method_declaration_throws=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_comma_in_method_invocation_arguments=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_comma_in_multiple_field_declarations=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_comma_in_multiple_local_declarations=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_comma_in_parameterized_type_reference=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_comma_in_superinterfaces=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_comma_in_type_arguments=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_comma_in_type_parameters=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_ellipsis=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_angle_bracket_in_parameterized_type_reference=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_angle_bracket_in_type_arguments=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_angle_bracket_in_type_parameters=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_brace_in_annotation_type_declaration=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_brace_in_anonymous_type_declaration=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_brace_in_array_initializer=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_brace_in_block=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_brace_in_constructor_declaration=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_brace_in_enum_constant=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_brace_in_enum_declaration=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_brace_in_method_declaration=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_brace_in_switch=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_brace_in_type_declaration=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_bracket_in_array_allocation_expression=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_bracket_in_array_reference=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_bracket_in_array_type_reference=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_paren_in_annotation=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_paren_in_annotation_type_member_declaration=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_paren_in_catch=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_paren_in_constructor_declaration=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_paren_in_enum_constant=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_paren_in_for=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_paren_in_if=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_paren_in_method_declaration=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_paren_in_method_invocation=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_paren_in_parenthesized_expression=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_paren_in_switch=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_paren_in_synchronized=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_opening_paren_in_while=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_parenthesized_expression_in_return=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_parenthesized_expression_in_throw=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_postfix_operator=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_prefix_operator=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_question_in_conditional=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_question_in_wildcard=insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_semicolon=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_semicolon_in_for=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_before_unary_operator=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_between_brackets_in_array_type_reference=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_between_empty_braces_in_array_initializer=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_between_empty_brackets_in_array_allocation_expression=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_between_empty_parens_in_annotation_type_member_declaration=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_between_empty_parens_in_constructor_declaration=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_between_empty_parens_in_enum_constant=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_between_empty_parens_in_method_declaration=do not insert",
            "org.eclipse.jdt.core.formatter.insert_space_between_empty_parens_in_method_invocation=do not insert",
            "org.eclipse.jdt.core.formatter.join_lines_in_comments=true",
            "org.eclipse.jdt.core.formatter.join_wrapped_lines=true",
            "org.eclipse.jdt.core.formatter.keep_else_statement_on_same_line=false",
            "org.eclipse.jdt.core.formatter.keep_empty_array_initializer_on_one_line=true",
            "org.eclipse.jdt.core.formatter.keep_imple_if_on_one_line=false",
            "org.eclipse.jdt.core.formatter.keep_then_statement_on_same_line=false",
            "org.eclipse.jdt.core.formatter.lineSplit=200",
            "org.eclipse.jdt.core.formatter.never_indent_block_comments_on_first_column=true",
            "org.eclipse.jdt.core.formatter.never_indent_line_comments_on_first_column=true",
            "org.eclipse.jdt.core.formatter.number_of_blank_lines_at_beginning_of_method_body=0",
            "org.eclipse.jdt.core.formatter.number_of_empty_lines_to_preserve=1",
            "org.eclipse.jdt.core.formatter.put_empty_statement_on_new_line=true",
            "org.eclipse.jdt.core.formatter.tabulation.char=space",
            "org.eclipse.jdt.core.formatter.tabulation.size=4",
            "org.eclipse.jdt.core.formatter.use_tabs_only_for_leading_indentations=false",
            "org.eclipse.jdt.core.formatter.wrap_before_binary_operator=false",
            "org.eclipse.jdt.core.incompatibleJDKLevel=error",
            "org.eclipse.jdt.core.incompleteClasspath=error"
        };
    }
}

