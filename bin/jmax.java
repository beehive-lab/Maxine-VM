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
import java.util.*;

/**
 * Database of Java project configurations used by the bin/max script.
 * This class includes definitions of the core Maxine projects. Extra
 * projects can be specified via the "extra_projects_dirs" environment
 * variable. The value of this variable is a comma separated list of
 * directories, each of which must contain a "projects.properties"
 * file. This file is loaded to augment the set of projects. For example,
 * projects.properties in the Maxine Virtual Edition project, would be:
 * <pre>
 * YANFS.sourceDirs=src,test
 * YANFS.dependencies=
 * 
 * VEBase.sourceDirs=src
 * VEBase.dependencies=VM
 * 
 * VEJDK.sourceDirs=src
 * VEJDK.dependencies=
 * 
 * ...
 * 
 * VEMain.sourceDirs=src,test
 * VEMain.dependencies=JNodeFS,NFSServer,VEBase,...
 * 
 * </pre> 
 * 
 * Note that only direct dependencies must be declared. Declaring transitive dependencies
 * is harmless but not necessary.
 */
public class jmax {

	static File maxine_dir = new File(getenv("maxine_dir", true)).getAbsoluteFile();
	
	static HashMap<String, Project> projects = new HashMap<String, Project>();
	static HashMap<String, Library> libs = new HashMap<String, Library>();
	
	static Library JUNIT4     = new Library("JUNIT4", "${JUNIT4_CP}");
	static Library JDK_TOOLS  = new Library("JDK_TOOLS", "${JAVA_HOME}/lib/tools.jar", false);
	
	static Project CRI        = new Project("CRI",       "src",      null);
	static Project C1X        = new Project("C1X",       "src",      "CRI");
	static Project Base       = new Project("Base",      "src,test", "JUNIT4");
	static Project Assembler  = new Project("Assembler", "src,test", "Base");
	static Project VM         = new Project("VM",        "src,test", "C1X,Assembler,JDK_TOOLS");
	static Project T1X        = new Project("T1X",       "src",      "VM");
	static Project MaxineELF  = new Project("MaxineELF", "src",      null);
	static Project VMDI       = new Project("VMDI",      "src",      null);
	static Project Tele       = new Project("Tele",      "src",      "VM,MaxineELF,VMDI");
	static Project JDWP       = new Project("JDWP",      "src",      "VMDI");
	static Project TeleJDWP   = new Project("TeleJDWP",  "src",      "Tele,JDWP");
	static Project Inspector  = new Project("Inspector", "src,test", "Tele");

	/**
	 * Processes the commands sent by the max script.
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
			if (request.size() == 1) {
				// No args -> return the class path for all projects
				response.println(asClasspath(sortedDependencies(true)));
			} else {
				// One or more args -> return the class path for the given projects
				List<Dependency> deps = new ArrayList<Dependency>();
				for (String p : request.subList(1, request.size())) {
					project(p).allDeps(deps, true);
				}
				response.println(asClasspath(deps));
			}
		} else if (cmd.equals("project_dir")) {
			// Return the absolute directory of a project
			Project p = project(request.get(1));
			response.println(p.baseDir.getPath() + File.separatorChar + p.name);
		} else if (cmd.equals("source_dirs")) {
			// Return the absolute source directories of a project
			Project p = project(request.get(1));
			String pfx = p.baseDir.getPath() + File.separatorChar + p.name + File.separatorChar;
			response.println(toString(p.sourceDirs, " ", pfx));
		} else if (cmd.equals("output_dir")) {
			// Return the absolute output directory of a project
			response.println(project(request.get(1)).outputDir());
		} else if (cmd.equals("projects_info")) {
			// Return a listing of each project and its configuration properties
			for (Dependency dep : sortedDependencies(false)) {
				Project p = (Project) dep;
				response.println(p);
				response.println("  baseDir: " + p.baseDir);
				response.println("  sourceDirs: " + p.sourceDirs);
				response.println("  dependencies: " + p.directDeps);
			}
		} else {
			throw new Error("Command '" + cmd + "' not known");
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
	 * Gets the value of a named property from a given property set, asserting it exists.
	 * 
	 * @param file the file from which {@code props} was loaded
	 */
	static String get(Properties props, String key, File file) {
		String value = props.getProperty(key);
		assert value != null : "missing property " + key + " in " + file.getAbsolutePath();
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
		
		HashMap<String, Project> loaded = new HashMap<String, Project>();
		for (Object key : props.keySet()) {
			String name = (String) key;
			int period = name.indexOf('.');
			assert period != -1 : "project property key must contain a period: " + name;
			name = name.substring(0, period);
			if (!loaded.containsKey(name)) {
				String sourceDirs = get(props, name + ".sourceDirs", f);
				String deps = get(props, name + ".dependencies", f);
				new Project(dir, loaded, name, sourceDirs, deps);
			}
		}
		
		for (Project p : loaded.values()) {
			Project o = projects.put(p.name, p);
			assert o == null : "cannot override project " + o + " in " + o.baseDir + " with project of the same name in " + dir;
		}
	}
	
	static {
		String value = getenv("extra_projects_dirs", false);
		if (value != null) {
			for (String path : value.split("\\s*,\\s*")) {
				File dir = new File(path);
				assert dir.isDirectory() : "extra projects path does not denote a directory: " + path;
				loadProjects(dir);
			}
		}
	}
	
	static String toString(Iterable<? extends Object> iterable, String sep, String pfx) {
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

	static String asClasspath(List<Dependency> deps) {
		StringBuilder cp = new StringBuilder(100);
		String prefix = getenv("cp_prefix", false);
		if (prefix != null) {
			cp.append(prefix);
		}
		
		for (Dependency p : deps) {
			p.appendToClasspath(cp);
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
	
	public static void main(String[] args) throws Exception {
		process(Arrays.asList(args), System.out);
	}
	
	static abstract class Dependency {
		final String name;
		
		public Dependency(String name) {
			this.name = name;
		}
		
		abstract void appendToClasspath(StringBuilder cp);
		
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
		final File lib;

		public Library(String name, String path) {
			this(name, path, true);
		}
		
		public Library(String name, String path, boolean mustExist) {
			super(name);
			lib = new File(expandVars(path));
			assert !mustExist || lib.exists() : "required library " + name + " not found";
			assert !projects.containsKey(name) : name + " cannot be both a library and a project";
			libs.put(name, this);
		}
		
		@Override
		void appendToClasspath(StringBuilder cp) {
			if (lib.exists()) {
				if (cp.length() != 0) {
					cp.append(File.pathSeparatorChar);
				}
				cp.append(lib.getPath());
			}
		}
	}
	
	static final class Project extends Dependency {
		final File baseDir;
		final List<String> sourceDirs;
		
		/**
		 * The direct dependencies of this project.
		 */
		final List<String> directDeps;
		
		public Project(File baseDir, HashMap<String, Project> projects, String name, String sourceDirs, String deps) {
			super(name);
			this.baseDir = baseDir;
			assert sourceDirs != null && !sourceDirs.isEmpty();
			assert deps == null || !deps.isEmpty();
			this.sourceDirs = Arrays.asList(sourceDirs.split("\\s*,\\s*"));
			if (deps == null) {
				this.directDeps = Collections.emptyList();
			} else {
				this.directDeps = Arrays.asList(deps.split("\\s*,\\s*"));
			}
			
			assert !libs.containsKey(name) : name + " cannot be both a library and a project";
			projects.put(name, this);
		}
		
		public Project(String name, String srcPaths, String deps) {
			this(maxine_dir, projects, name, srcPaths, deps);
		}

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

			for (String name : directDeps) {
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
			} else {
				return baseDir.getPath() + sep + name + sep + "bin";
			}
		}
		
		@Override
		void appendToClasspath(StringBuilder cp) {
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
		}
	}
}
