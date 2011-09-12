/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.config;


import static com.sun.max.lang.Classes.*;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Describes a package in the Maxine VM boot image, providing programmatic package information manipulation, which is
 * lacking in {@link java.lang.Package}.
 * <p>
 * Any {@link BootImagePackage} subclass describing one or more packages that
 * declare {@link Word} subclasses must override {@link #wordSubclasses()}.
 * <p>
 * Any {@link BootImagePackage} subclass describing one or more packages whose
 * classes instantiate one or more {@link VmThreadLocal}s must reference those thread
 * local objects in its constructor. This ensures all {@link VmThreadLocal}s
 * are registered before the {@linkplain VmThreadLocal#tlaSize() size} of the
 * thread locals area is computed. The only exception to this requirement are
 * the thread locals declared in the {@link VmThreadLocal} class itself.
 */
@HOSTED_ONLY
public class BootImagePackage implements Comparable<BootImagePackage>, Cloneable {

    /**
     * The name of the package.
     */
    private String name;

    private Map<String, BootImagePackage> others = Collections.emptyMap();

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
    protected BootImagePackage() {
        this.name = getPackageName(getClass());
    }

    /**
     * Creates an object denoting the package whose name is {@code this.getClass().getPackage().getName()}
     * and, if {@code recursive == true}, all its sub-packages.
     */
    protected BootImagePackage(String pkgName, boolean recursive) {
        this.name = pkgName == null ? getClass().getPackage().getName() : pkgName;
        this.recursive = recursive;
    }

    /**
     * Creates an object denoting the package whose name is {@code this.getClass().getPackage().getName()} as well as
     * the packages specified by {@code packageSpecs}.
     *
     * A package specification is one of the following:
     * <ol>
     * <li>A string ending with {@code ".*"} (e.g. {@code "com.sun.max.unsafe.*"}). This denotes a single package.</li>
     * <li>A string ending with {@code ".**"} (e.g. {@code "com.sun.max.asm.**"}). This denotes a package and all its
     * sub-packages.</li>
     * <li>A name of a class available on the class path via {@link Class#forName(String)}.</li>
     * </ol>
     *
     * @param packageSpecs a set of package specifications
     */
    protected BootImagePackage(String... packageSpecs) {
        this.name = getClass().getPackage().getName();
        this.recursive = false;

        Map<String, BootImagePackage> pkgs = new TreeMap<String, BootImagePackage>();
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

            // Honor existing Package.java classes, but watch for recursion when we are explicitly
            // including classes in this package
            BootImagePackage pkg = null;
            if (className != null && pkgName.equals(name)) {
                pkg = this;
            } else {
                pkg = fromName(pkgName);
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
                BootImagePackage oldPkg = pkgs.put(pkgName, pkg);
                assert oldPkg == null || oldPkg == pkg;
            }
            if (className != null) {
                pkg.initClasses();
                pkg.classes.add(className);
            }
        }

        this.others = pkgs;
    }

    /**
     * Determines if this package is part of the boot image under the given configuration.
     *
     * @param vmConfig
     */
    public boolean isPartOfMaxineVM(VMConfiguration vmConfig) {
        return true;
    }

    /**
     * Determines if this package contains any classes annotated with {@link METHOD_SUBSTITUTIONS}.
     */
    public boolean containsMethodSubstitutions() {
        return false;
    }

    private void initClasses() {
        if (classes == null) {
            classes = new HashSet<String>();
        }
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
                    classNames.add(className);
                }
                return true;
            }
        };
        classSearch.run(classpath, name.replace('.', '/'));
        return classNames.toArray(new String[classNames.size()]);
    }

    /**
     * Check whether the given class should be in the boot image.
     * @return
     */
    public boolean isBootImageClass(String name) {
        if (classes == null) {
            return true;
        }
        return classes.contains(name);
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
     * @param packageName denotes the name of a package which may contain a subclass of {@link BootImagePackage} named
     *            "Package"
     * @return an instance of the class name "Package" in {@code packageName}. If such a class does not exist or there
     *         was an error {@linkplain Class#newInstance() instantiating} it, then {@code null} is returned
     */
    public static BootImagePackage fromName(String packageName) {
        final String name = packageName + "." + Package.class.getSimpleName();
        if (name.equals(java.lang.Package.class.getName())) {
            // Special case!
            return null;
        }
        try {
            final Class packageClass = Class.forName(name);
            return (BootImagePackage) packageClass.newInstance();
        } catch (ClassNotFoundException e) {
        } catch (NoClassDefFoundError e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        }
        return null;
    }

    public static BootImagePackage fromClass(Class javaClass) {
        final java.lang.Package javaPackage = javaClass.getPackage();
        if (javaPackage == null) {
            return null;
        }
        return fromName(javaPackage.getName());
    }

    public String name() {
        return name;
    }

    public BootImagePackage superPackage() {
        final int end = name().lastIndexOf('.');
        if (end < 0) {
            return null;
        }
        return fromName(name().substring(0, end));
    }

    /**
     * Gets the subclasses of {@link Word} in this package.
     * The returned array must not include boxed (see com.sun.max.unsafe.Boxed)
     * word types as they are derived from the name of the unboxed types.
     */
    public Class[] wordSubclasses() {
        return NO_CLASSES;
    }

    protected static void registerThreadLocal(VmThreadLocal tl) {

    }

    protected static void registerThreadLocal(Class c, String fieldName) {
        registerThreadLocal(c.getName(), fieldName);
    }

    protected static void registerThreadLocal(String className, String fieldName) {
        try {
            Class c = Class.forName(className);
            Field f = c.getDeclaredField(fieldName);
            f.setAccessible(true);
            Object value = f.get(null);
            FatalError.check(value instanceof VmThreadLocal, f + " is not an instance of " + VmThreadLocal.class);
        } catch (Exception e) {
            throw FatalError.unexpected("Error looking up thread local " + className + "." + fieldName, e);
        }
    }

    public boolean isSubPackageOf(BootImagePackage superPackage) {
        return name().startsWith(superPackage.name());
    }

    static class RootPackageInfo {
        final BootImagePackage root;
        final Set<String> pkgNames = new TreeSet<String>();

        RootPackageInfo(Classpath classpath, final BootImagePackage root) {
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

    private BootImagePackage cloneAs(String pkgName) {
        assert fromName(pkgName) == null;
        try {
            BootImagePackage pkg = (BootImagePackage) clone();
            pkg.name = pkgName;
            return pkg;
        } catch (CloneNotSupportedException ex) {
            throw ProgramError.unexpected("BootImagePackage failed to clone " + this);
        }

    }

    /**
     * Add a new package or merge previous state in if it exists already.
     * @param pkg the (presumed) new package
     * @param pkgMap the global map of all packages discovered so far
     */
    private static void add(BootImagePackage pkg, Map<String, BootImagePackage> pkgMap) {
        BootImagePackage oldPkg = pkgMap.get(pkg.name());
        if (oldPkg == pkg) {
            // if this identical then we must have added it to the list previously
        } else {
            if (oldPkg == null) {
                // new
                pkgMap.put(pkg.name(), pkg);
            } else {
                // merge into oldPkg
                oldPkg.others.putAll(pkg.others);
                if (pkg.classes != null) {
                    oldPkg.initClasses();
                    oldPkg.classes.addAll(pkg.classes);
                }
            }
        }
    }

    /**
     * Finds all sub-packages in the class path for a given set of root packages.
     *
     * We maintain a global map of all the packages discovered. Duplicates may arise in the processing from different
     * subsystems referencing the same package, possibly including different subsets of the classes in the package. The
     * first occurrence of a package is the canonical representative and duplicates have their relevant state merged
     * into it.
     *
     * @param classpath the class path to search for packages
     * @param roots array of subclass of {@code BootImagePackage} that define search start and match
     * @return list of packages in the closure denoted by {@code roots}
     */
    public static List<BootImagePackage> getTransitiveSubPackages(Classpath classpath, final BootImagePackage... roots) {
        final Map<String, BootImagePackage> pkgMap = new TreeMap<String, BootImagePackage>();

        final ArrayList<RootPackageInfo> rootInfos = new ArrayList<RootPackageInfo>(roots.length);
        for (BootImagePackage root : roots) {
            rootInfos.add(new RootPackageInfo(classpath, root));
        }

        int rootIndex = 0;

        while (rootIndex < rootInfos.size()) {
            RootPackageInfo info = rootInfos.get(rootIndex++);
            for (String pkgName : info.pkgNames) {
                BootImagePackage pkg = BootImagePackage.fromName(pkgName);
                if (pkg == null) {
                    // No explicit Package class provided, so we will create one.
                    // Locate the closest parent in the tree that has a Package instance.
                    // N.B. One is guaranteed to exist, if only at the root.
                    // First check in case we are the root!
                    pkg = pkgMap.get(pkgName);
                    if (pkg == null) {
                        String parentPkgName = getPackageName(pkgName);
                        while (parentPkgName.length() != 0) {
                            BootImagePackage parent = pkgMap.get(parentPkgName);
                            if (parent != null) {
                                pkg = parent.cloneAs(pkgName);
                                break;
                            }
                            parentPkgName = getPackageName(parentPkgName);
                        }
                    }
                    assert pkg != null;
                }

                add(pkg, pkgMap);
                for (final BootImagePackage otherPkg : pkg.others.values()) {
                    // Add any redirects and, for recursive packages outside this tree, add them as a new root.
                    add(otherPkg, pkgMap);
                    if (!otherPkg.name().startsWith(info.root.name()) && otherPkg.recursive) {
                        rootInfos.add(new RootPackageInfo(classpath, otherPkg));
                    }
                }

            }
        }

        return Arrays.asList(pkgMap.values().toArray(new BootImagePackage[pkgMap.size()]));
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

    @Override
    public boolean equals(Object other) {
        if (other instanceof BootImagePackage) {
            return name.equals(((BootImagePackage) other).name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name().hashCode();
    }

    public int compareTo(BootImagePackage other) {
        return name.compareTo(other.name);
    }

    public synchronized <S extends VMScheme> Class<? extends S> loadSchemeImplementation(Class<S> schemeType) {
        final Class<? extends S> schemeImplementation = schemeTypeToImplementation(schemeType);
        if (schemeImplementation == null) {
            throw ProgramError.unexpected("could not find subclass of " + schemeType + " in " + this);
        } else {
            final Class<?> loadedImplementation = Classes.load(schemeType.getClassLoader(), schemeImplementation.getName());
            return loadedImplementation.asSubclass(schemeType);
        }
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
