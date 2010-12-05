/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.config;

import static com.sun.max.lang.Classes.*;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;

/**
 * Describes a package in the Maxine VM, providing programmatic package information manipulation, which is
 * lacking in {@link java.lang.Package}.
 * <p>
 * To deal with initialization cycles with code that would naturally execute in the constructor,
 * this can be delayed until just before loading by overriding the {@link #loading} method.
 * N.B. be aware that this will be called for all cloned instances in the recursive context.
 * Before any class is actually loaded it is checked for inclusion by calling {@link #isIncluded(String)}.
 * The default implementation checks the {@link #classExclusions} and {@link #classes} lists.
 * <p>
 * It is recommended to include a file called 'package-info.java' in every package, which is where you can put
 * package-related JavaDoc comments.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Mick Jordan
 */
public class MaxPackage implements Comparable<MaxPackage>, Cloneable {

    /**
     * The name of the package.
     */
    private String name;

    private Map<String, MaxPackage> others = Collections.emptyMap();

    /**
     * Specifies if this package also denotes all its sub-packages.
     */
    public boolean recursive;

    /**
     * Exact set of classes to be returned by {@link #listClasses(Classpath)}.
     * If this value is {@code null}, then a class path {@linkplain ClassSearch search}
     * is performed to find the classes in this package.
     */
    private Set<String> classes;

    private static final Class[] NO_CLASSES = {};

    /**
     * Creates an object denoting the package whose name is {@code this.getClass().getPackage().getName()}.
     */
    protected MaxPackage() {
        this.name = getPackageName(getClass());
    }

    /**
     * Creates an object denoting the package whose name is {@code this.getClass().getPackage().getName()}
     * and, if {@code recursive == true}, all its sub-packages.
     */
    public MaxPackage(String pkgName, boolean recursive) {
        this.name = pkgName == null ? getClass().getPackage().getName() : pkgName;
        this.recursive = recursive;
    }

    /**
     * Creates an object denoting the package whose name is {@code this.getClass().getPackage().getName()}
     * as well as the packages specified by {@code packageSpecs}.
     *
     * A package specification is one of the following:
     * <ol>
     * <li>A string ending with {@code ".*"} (e.g. {@code "com.sun.max.unsafe.*"}). This denotes a single package.</li>
     * <li>A string ending with {@code ".**"} (e.g. {@code "com.sun.max.asm.**"}). This denotes a package and all its sub-packages.</li>
     * <li>A name of a class available in the current runtime via {@link Class#forName(String)}.</li>
     * </ol>
     *
     * @param packageSpecs a set of package specifications
     */
    protected MaxPackage(String... packageSpecs) {
        this.name = getClass().getPackage().getName();
        this.recursive = false;

        Map<String, MaxPackage> pkgs = new TreeMap<String, MaxPackage>();
        for (String spec : packageSpecs) {
            String pkgName;
            String className = null;
            boolean recursive = false;
            if (spec.endsWith(".**")) {
                recursive = true;
                pkgName = spec.substring(0, spec.length() - 3);
            } else if (spec.endsWith(".*")) {
                pkgName = spec.substring(0, spec.length() - 2);
            } else {
                className = spec;
                try {
                    Class.forName(className);
                } catch (ClassNotFoundException e) {
                    throw ProgramError.unexpected("Package specification does not end with \".*\" or \".**\" or name a class on the class path: " + spec);
                }
                pkgName = getPackageName(className);
            }

            // Honor existing Package.java classes.
            MaxPackage pkg = fromName(pkgName);
            if (pkg == null) {
                pkg = pkgs.get(pkgName);
                if (pkg == null) {
                    pkg = cloneAs(pkgName);
                }
                pkg.recursive = recursive;
            } else {
                assert !pkg.recursive : "Package created via reflection should not be recursive";
                pkg.recursive = recursive;
            }
            MaxPackage oldPkg = pkgs.put(pkgName, pkg);
            assert oldPkg == null || oldPkg == pkg;
            if (className != null) {
                if (pkg.classes == null) {
                    pkg.classes = new HashSet<String>();
                }
                pkg.classes.add(className);
            }
        }

        this.others = pkgs;
    }

    /**
     * Lists the classes included from this package.
     * If this package object was created based on at least one explicit
     * class name, then only the set of explicit class names is returned.
     * Otherwise the names of all the class files on the given class path
     * whose package name matches this package is returned.
     *
     * Note that in the former case, there is no check as to whether the
     * class files really exist.
     *
     * @param name the name of the package to search
     * @return the class names
     */
    public String[] listClasses(Classpath classpath) {
        final HashSet<String> classNames = new HashSet<String>();
        if (classes != null) {
            assert !classes.isEmpty();
            return classes.toArray(new String[classes.size()]);
        }
        final ClassSearch classSearch = new ClassSearch() {
            @Override
            protected boolean visitClass(boolean isArchiveEntry, String className) {
                if (!className.endsWith("package-info") && !classNames.contains(className) && name.equals(getPackageName(className))) {
                    if (className.equals("com.sun.c1x")) {
                        System.console();
                    }
                    classNames.add(className);
                }
                return true;
            }
        };
        classSearch.run(classpath, name.replace('.', '/'));
        return classNames.toArray(new String[classNames.size()]);
    }

    /**
     * Last chance for special setup for this instance.
     * Call just before loading any classes.
     */
    public void loading() {
    }

    /**
     * Gets an instance of the class named "Package" in a named package.
     *
     * @param packageName denotes the name of a package which may contain a subclass of {@link MaxPackage} named
     *            "Package"
     * @return an instance of the class name "Package" in {@code packageName}. If such a class does not exist or there
     *         was an error {@linkplain Class#newInstance() instantiating} it, then {@code null} is returned
     */
    public static MaxPackage fromName(String packageName) {
        final String name = packageName + "." + Package.class.getSimpleName();
        if (name.equals(java.lang.Package.class.getName())) {
            // Special case!
            return null;
        }
        try {
            final Class packageClass = Class.forName(name);
            return (MaxPackage) packageClass.newInstance();
        } catch (ClassNotFoundException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        }
        return null;
    }

    public static MaxPackage fromClass(Class javaClass) {
        final java.lang.Package javaPackage = javaClass.getPackage();
        if (javaPackage == null) {
            return null;
        }
        return fromName(javaPackage.getName());
    }

    public String name() {
        return name;
    }

    public MaxPackage superPackage() {
        final int end = name().lastIndexOf('.');
        if (end < 0) {
            return null;
        }
        return fromName(name().substring(0, end));
    }

    /**
     * Gets the subclasses of {@code com.sun.max.unsafe.Word} in this package.
     * The returned array must not include boxed (see com.sun.max.unsafe.Boxed)
     * word types as they are derived from the name of the unboxed types.
     */
    public Class[] wordSubclasses() {
        return NO_CLASSES;
    }

    public boolean isSubPackageOf(MaxPackage superPackage) {
        return name().startsWith(superPackage.name());
    }

    static class RootPackageInfo {
        final MaxPackage root;
        final Set<String> pkgNames = new TreeSet<String>();

        RootPackageInfo(Classpath classpath, final MaxPackage root) {
            this.root = root;
            //long start = System.currentTimeMillis();
            new ClassSearch() {
                @Override
                protected boolean visitClass(String className) {
                    if (className.startsWith(root.name())) {
                        pkgNames.add(Classes.getPackageName(className));
                    }
                    return true;
                }
            }.run(classpath, root.name().replace('.', '/'));
            //System.out.println("scan: " + root + " [pkgs=" + pkgNames.size() + ", time=" + (System.currentTimeMillis() - start) + "ms]");
        }

        @Override
        public String toString() {
            return root.toString();
        }
    }


    protected MaxPackage cloneAs(String pkgName) {
        assert fromName(pkgName) == null;
        try {
            MaxPackage pkg = (MaxPackage) clone();
            pkg.name = pkgName;
            return pkg;
        } catch (CloneNotSupportedException ex) {
            throw ProgramError.unexpected("MaxPackage failed to clone " + this);
        }

    }

    private static boolean add(MaxPackage pkg, Map<String, MaxPackage> pkgMap, ArrayList<MaxPackage> pkgs) {
        pkgs.add(pkg);
        MaxPackage oldPkg = pkgMap.put(pkg.name(), pkg);
        assert oldPkg == null || oldPkg == pkg;
        return !pkg.prerequisites().isEmpty();
    }

    /**
     * Finds all sub-packages in the class path for a given set of root packages.
     *
     * HACK alert: To support CPS we sort any root that has packages with non-empty prerequisites.
     * TODO eliminate prerequisites when CPS dies
     *
     * @param classpath the class path to search for packages
     * @param roots array of subclass of {@code MaxPackage} that define search start and match
     * @return list of packages in the closure denoted by {@code roots}
     */
    public static List<MaxPackage> getTransitiveSubPackages(Classpath classpath, final MaxPackage[] roots) {
        final Map<String, MaxPackage> pkgMap = new TreeMap<String, MaxPackage>();
        final ArrayList<MaxPackage> pkgs = new ArrayList<MaxPackage>();

        final ArrayList<RootPackageInfo> rootInfos = new ArrayList<RootPackageInfo>(roots.length);
        for (MaxPackage root : roots) {
            rootInfos.add(new RootPackageInfo(classpath, root));
        }

        int rootIndex = 0;
        int listIndex = 0;

        while (rootIndex < rootInfos.size()) {
            boolean hasPrerequisites = false;
            RootPackageInfo info = rootInfos.get(rootIndex++);
            for (String pkgName : info.pkgNames) {
                MaxPackage pkg = MaxPackage.fromName(pkgName);
                if (pkg == null) {
                    String parentPkgName = getPackageName(pkgName);
                    while (parentPkgName.length() != 0) {
                        MaxPackage parent = pkgMap.get(parentPkgName);
                        if (parent != null) {
                            pkg = parent.others.get(pkgName);
                            if (pkg == null && parent.recursive) {
                                pkg = parent.cloneAs(pkgName);
                            }
                        }
                        parentPkgName = getPackageName(parentPkgName);
                    }
                    if (pkg == null) {
                        // ProgramWarning.message("WARNING: missing Package class in package: " + pkgName);
                        continue;
                    }
                }

                hasPrerequisites = add(pkg, pkgMap, pkgs) || hasPrerequisites;

                for (final MaxPackage otherPkg : pkg.others.values()) {
                    hasPrerequisites = add(otherPkg, pkgMap, pkgs) || hasPrerequisites;
                    if (!otherPkg.name().startsWith(info.root.name()) && otherPkg.recursive) {
                        rootInfos.add(new RootPackageInfo(classpath, otherPkg));
                    }
                }
            }
            if (hasPrerequisites) {
                Collections.sort(pkgs.subList(listIndex, pkgs.size()));
            }
            listIndex = pkgs.size();
        }

        return pkgs;
    }

    public List<MaxPackage> getTransitiveSubPackages(Classpath classpath) {
        return getTransitiveSubPackages(classpath, new MaxPackage[] {this});
    }

    private static boolean isPrefixOf(MaxPackage p, MaxPackage[] set) {
        for (MaxPackage s : set) {
            if (!s.name().startsWith(p.name())) {
                return false;
            }
        }
        return true;
    }

    private Map<Class<? extends VMScheme>, Class<? extends VMScheme>> schemeTypeToImplementation;

    /**
     * Registers a class in this package that implements a given scheme type.
     *
     * @param schemeType a scheme type
     * @param schemeImplementation a class that implements {@code schemType}
     */
    public synchronized <S extends VMScheme> void registerScheme(Class<S> schemeType, Class<? extends S> schemeImplementation) {
        assert schemeType.isInterface() || Modifier.isAbstract(schemeType.getModifiers());
        assert schemeImplementation.getPackage().getName().equals(name()) : "cannot register implmentation class from another package: " + schemeImplementation;
        if (schemeTypeToImplementation == null) {
            schemeTypeToImplementation = new IdentityHashMap<Class<? extends VMScheme>, Class<? extends VMScheme>>();
        }
        Class<? extends VMScheme> oldValue = schemeTypeToImplementation.put(schemeType, schemeImplementation);
        assert oldValue == null;
    }

    /**
     * Gets the class within this package implementing a given scheme type (represented as an abstract class or interface).
     *
     * @return the class directly within this package that implements {@code scheme} or null if no such class
     *         exists
     */
    public synchronized <S extends VMScheme> Class<? extends S> schemeTypeToImplementation(Class<S> schemeType) {
        if (schemeTypeToImplementation == null) {
            return null;
        }
        final Class< ? extends VMScheme> implementation = schemeTypeToImplementation.get(schemeType);
        if (implementation == null) {
            return null;
        }
        return implementation.asSubclass(schemeType);
    }

    public static boolean equal(MaxPackage p1, MaxPackage p2) {
        if (p1 == null) {
            return p2 == null;
        }
        return p1.equals(p2);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof MaxPackage) {
            return name.equals(((MaxPackage) other).name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name().hashCode();
    }

    public Set<MaxPackage> prerequisites() {
        return Collections.emptySet();
    }

    public int compareTo(MaxPackage other) {
        final Set<MaxPackage> myPrerequisites = prerequisites();
        final Set<MaxPackage> otherPrerequisites = other.prerequisites();
        if (myPrerequisites.isEmpty()) {
            if (otherPrerequisites.isEmpty()) {
                return name.compareTo(other.name);
            }
            return -1;
        }
        for (MaxPackage myPrerequisite : myPrerequisites) {
            if (other.equals(myPrerequisite)) {
                return 1;
            }
        }
        if (otherPrerequisites.isEmpty()) {
            return 1;
        }
        for (MaxPackage otherPrerequisite : otherPrerequisites) {
            if (equals(otherPrerequisite)) {
                return -1;
            }
        }
        return name.compareTo(other.name);
    }

    public synchronized <S extends VMScheme> Class<? extends S> loadSchemeImplementation(Class<S> schemeType) {
        final Class<? extends S> schemeImplementation = schemeTypeToImplementation(schemeType);
        if (schemeImplementation == null) {
            ProgramError.unexpected("could not find subclass of " + schemeType + " in " + this);
        } else {
            final Class<?> loadedImplementation = Classes.load(schemeType.getClassLoader(), schemeImplementation.getName());
            return loadedImplementation.asSubclass(schemeType);
        }
        return null;
    }

    /**
     * Instantiates the scheme implementation class in this package implementing a given scheme type.
     *
     * @param schemeType the interface or abstract class defining a scheme type
     * @return a new instance of the scheme implementation class
     */
    public synchronized <S extends VMScheme> S loadAndInstantiateScheme(Class<S> schemeType) {
        final Class<? extends S> schemeImplementation = loadSchemeImplementation(schemeType);
        try {
            return schemeImplementation.newInstance();
        } catch (Throwable throwable) {
            throw ProgramError.unexpected("could not instantiate class: " + schemeImplementation.getName(), throwable);
        }
    }

    @Override
    public String toString() {
        return name();
    }
}
