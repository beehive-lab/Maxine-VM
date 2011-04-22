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
     */
    public static void process(List<String> request, PrintStream response) {
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
                response.println("# file: " + new File(dir, "projects.properties"));
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
            throw new Error("Command '" + cmd + "' not known");
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
        assert !required || value != null : "the required environment variable '" + name + "' is not set";
        return value;
    }

    /**
     * Gets the project for a given name, asserting that the project exists.
     */
    static Project project(String name) {
        Project p = projects.get(name);
        assert p != null : "project named '" + name + "' not found";
        return p;
    }

    /**
     * Gets the library for a given name, asserting that the library exists.
     */
    static Library library(String name) {
        Library l = libs.get(name);
        assert l != null : "library named '" + name + "' not found";
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
                throw new Error("missing property " + key + " in " + file);
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
            throw new Error("Error loading projects from " + f.getAbsolutePath(), e);
        }

        String[] names = split(get(props, "projects", dir));
        for (String name : names) {
            assert !projects.containsKey(name) : "cannot override project " + name + " in " + project(name).baseDir + " with project of the same name in " + dir;
            String pfx = "project." + name;
            String sourceDirs = get(props, pfx + ".sourceDirs", f);
            String deps = get(props, pfx + ".dependencies", f);
            new Project(dir, name, sourceDirs, deps);
        }

        String value = get(props, "libraries", null);
        if (value != null) {
            names = split(value);
            for (String name : names) {
                assert !libs.containsKey(name) : "cannot redefine library " + name;
                String pfx = "library." + name;
                String path = get(props, pfx + ".path", f);
                boolean optional = Boolean.valueOf(get(props, pfx + ".optional", f));
                String urls = get(props, pfx + ".urls", f);
                new Library(dir, name, path, !optional, urls);
            }
        }
        assert props.isEmpty() : "unhandled properties in " + f + ":\n" + toString(props.keySet(), "\n", null) + "\n";
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
                assert dir.isDirectory() : "extra projects path does not denote a directory: " + path;
                projectDirs.add(dir);
                loadProjects(dir);
            }
        }
        process(Arrays.asList(args), System.out);
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

        public Library(File baseDir, String name, String path, boolean mustExist, String urls) {
            super(name);
            this.baseDir = baseDir;
            this.path = path;
            this.mustExist = mustExist;
            this.urls = urls.trim();
            assert !projects.containsKey(name) : name + " cannot be both a library and a project";
            libs.put(name, this);
        }

        @Override
        StringBuilder appendToClasspath(StringBuilder cp, boolean resolve) {
            File lib = new File(expandVars(path));
            if (resolve && mustExist && !lib.exists()) {
                assert !urls.isEmpty() : "cannot find required library " + name;
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
            out.println("library." + name + ".path=" + path);
            out.println("library." + name + ".optional=" + !mustExist);
            out.println("library." + name + ".urls=" + jmax.toString(Arrays.asList(urls), ", ", null));
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
        if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new Error("Could not make directory: " + parent);
        }

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
                proxyMsg = " via proxy " + proxy;
            } else {
                log.println("Value of HTTP_PROXY is not valid: " + proxy);
            }
        } else {
            log.println("** If behind a firewall without direct internet access, use the HTTP_PROXY environment variable (e.g. 'env HTTP_PROXY=proxy.company.com:80 max ...') or download manually with a web browser.");
        }

        for (String s : urls) {
            try {
                log.println("Downloading " + s + " to " + dst + proxyMsg);
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
                    log.print("\r" + n + " bytes" + (size == -1 ? "" : " (" + (n * 100 / size) + "%)"));
                    out.write(buf, 0, read);
                }
                log.println();
                out.close();
                in.close();
                return;
            } catch (MalformedURLException e) {
                throw new Error("Error in URL" + s, e);
            } catch (IOException e) {
                log.println("Error reading from " + s + ": " + e);
                dst.delete();
            }
        }
        throw new Error("Could not download content to " + dst + " from " + Arrays.toString(urls));
    }

    static String[] split(String list) {
        if (list.isEmpty()) {
            return new String[0];
        }
        return list.split("\\s*[ ,]\\s*");
    }

    static final class Project extends Dependency {

        final File baseDir;
        final List<String> sourceDirs;

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
                return baseDir.getPath() + sep + "out" + sep + "production" + sep + name;
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
            out.println("project." + name + ".sourceDirs=" + jmax.toString(sourceDirs, ", ", null));
            out.println("project." + name + ".dependencies=" + jmax.toString(dependencies, ", ", null));
        }

    }
}
