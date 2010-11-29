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
package com.sun.max;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * Describes a package in the Maxine VM, providing programmatic package information manipulation, which is
 * lacking in {@link java.lang.Package}.
 * <p>
 * Various concrete subclasses of this package exist for parts of Maxine, namely:
 * <ul>
 * <li>{@link BasePackage} for base (utility) packages in {@code com.sun.max}.
 * <li>{@link AsmPackage} for packages in the Maxine assembler.
 * <li>{@link VMPackage} for packages in the VM proper.
 * <li>{@link ExtPackage} for the extension packages.
 * </ul>
 * Generally, for the classes in a package to be included in Maxine, each subpackage must contain
 * a class named {@code Package} that is a subclass of one of the above.
 * However, it is possible to indicate recursive inclusion at the root by using the appropriate constructor,
 * in which case the {@code Package} class must also declare a public constructor {@code Package(String, boolean}}
 * to enable the appropriate {@code Package} instances to be created for the recursive packages.
 * <p>
 * It is also possible to redirect the search to another package for extensions that must lie outside the
 * {@code com.sun.max} package name space. These always support recursion.
 * <p>
 * It is recommended to include a file called 'package-info.java' in every package, which is where you can put
 * package-related JavaDoc comments.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class MaxPackage implements Comparable<MaxPackage> {

    private final String packageName;
    private final boolean reDIrect;
    private final boolean recursive;
    private static final Class[] NO_CLASSES = new Class[0];

    /**
     * Used to represent the missing {@code Package} classes in recursively included packages.
     * Using the simple name {@code Package} meets the naming assertion check in {@link MaxPackage#MaxPackage(String, boolean, boolean)}.
     */
    static class Package extends MaxPackage {
        private MaxPackage root;
        Package(MaxPackage root, String packageName, boolean recursive) {
            super(packageName, root.reDIrect, recursive);
            this.root = root;
        }

        @Override
        public boolean isPartOfMaxine() {
            return root.isPartOfMaxine();
        }
    }

    /**
     * No recursion, no redirection.
     */
    protected MaxPackage() {
        this(null, false, false);
    }

    /**
     * No redirection, optional recursion.
     * @param recursive
     */
    protected MaxPackage(boolean recursive) {
        this(null, false, recursive);
    }

    /**
     * Used only for recursion support.
     * @param packageName name of recursively included sub-package
     * @param recursive exists solely to distinguish constructor, value ignored
     */
    protected MaxPackage(String packageName, boolean recursive) {
        this(packageName, false, true);
    }

    /**
     * Redirection and recursion.
     * @param packageName
     */
    protected MaxPackage(String packageName) {
        this(packageName, true, true);
    }

    private MaxPackage(String packageName, boolean reDirect, boolean recursive) {
        this.packageName = packageName == null ? getClass().getPackage().getName() : packageName;
        this.reDIrect = reDirect;
        this.recursive = recursive;
        assert getClass().getSimpleName().equals(Package.class.getSimpleName());
    }

    /**
     * Is the package denoted by this instance part of the VM?
     * Default assumption is that all packages are included in the VM.
     * Particular subclasses may override and/or specialise (i.e. ignore) this method.
     * @return
     */
    public boolean isPartOfMaxine() {
        return false;
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

    // @Deprecated - only used in a CPS test
    public static MaxPackage fromJava(String name) {
        return new MaxPackage(name, false) {

        };
    }

    public static MaxPackage fromClass(Class javaClass) {
        final java.lang.Package javaPackage = javaClass.getPackage();
        if (javaPackage == null) {
            return null;
        }
        return fromName(javaPackage.getName());
    }

    public java.lang.Package toJava() {
        if (reDIrect) {
            java.lang.Package.getPackage(name());
        }
        return getClass().getPackage();
    }

    public String name() {
        return packageName;
    }

    public String lastIdentifier() {
        return packageName.substring(packageName.lastIndexOf('.') + 1);
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

    public MaxPackage subPackage(String... suffices) {
        String name = name();
        for (String suffix : suffices) {
            name += "." + suffix;
        }
        final MaxPackage subPackage = fromName(name);
        ProgramError.check(subPackage != null, "Could not find sub-package of " + this + " named " + name);
        return subPackage;
    }

    public boolean isSubPackageOf(MaxPackage superPackage) {
        return name().startsWith(superPackage.name());
    }

    /**
     * Finds all sub-packages in the classpath that are an instance of the superclass of the class of this instance.
     * Neither this class nor its immediate subclasses are typically instantiable, so a concrete subclass
     * must be passed in, but the match check should be against the superclass.
     * @param classpath
     * @return list of subclasses of {@link MaxPackage}
     */
    public List<MaxPackage> getTransitiveSubPackages(Classpath classpath) {
        final Set<String> packageNames = new TreeSet<String>();
        new ClassSearch() {

            @Override
            protected boolean visitClass(String className) {
                final String pkgName = Classes.getPackageName(className);
                if (pkgName.startsWith(name())) {
                    packageNames.add(pkgName);
                }
                return true;
            }
        }.run(classpath, name().replace('.', '/'));

        final Class<?> matcherClass = this.getClass().getSuperclass();
        final List<MaxPackage> packages = new ArrayList<MaxPackage>(packageNames.size());
        for (String pkgName : packageNames) {
            MaxPackage maxPackage = MaxPackage.fromName(pkgName);
            MaxPackage recursiveParent = null;
            if (maxPackage == null) {
                // check for a parent with recursive == true
                // there is an assumption that parent packages are processed before sub-packages
                for (MaxPackage parent : packages) {
                    if (pkgName.startsWith(parent.name()) && parent.recursive) {
                        // need to create a clone of parent class with sub-package name
                        maxPackage = createRecursivePackageInstance(parent, pkgName);
                        recursiveParent = parent;
                        break;
                    }
                }
                if (maxPackage == null) {
                    System.err.println("WARNING: missing Package class in package: " + pkgName);
                    continue;
                }
            }
            if ((recursiveParent == null && matcherClass.isInstance(maxPackage)) ||
                (recursiveParent != null && matcherClass.isInstance(recursiveParent))) {
                packages.add(maxPackage);
                if (maxPackage.reDIrect) {
                    // the above recursive traversal only operates in sub-packages of "this"
                    // so we have to process redirected packages explicitly
                    packages.addAll(checkReDirectedSubpackages(classpath, maxPackage));
                }
            }
        }
        return packages;
    }

    /**
     * Create a {@code MaxPackage} instance that denotes a sub-package of a recursively included package.
     * Note that we don't try to create the correct subclass corresponding to the parent (which would require the deprecated clone
     * technology), we just create an instance directly.
     * @param parent
     * @param pkgName
     * @return
     */
    private static MaxPackage createRecursivePackageInstance(MaxPackage parent, String pkgName) {
//        try {
        return new Package(parent, pkgName, true);
            //return parent.getClass().getConstructor(String.class, boolean.class).newInstance(pkgName, true);
//        } catch (Exception ex) {
//            ProgramError.unexpected("failed to create recursive Package object in " + pkgName + ": " + ex);
//            return null;
//        }
    }

    private static List<MaxPackage> checkReDirectedSubpackages(Classpath classpath, final MaxPackage redirectedPackage) {
        final ArrayList<MaxPackage> result = new ArrayList<MaxPackage>();
        final Set<String> packageNames = new TreeSet<String>();
        // find all the packages that start with the redirected package name
        new ClassSearch() {
            @Override
            protected boolean visitClass(String className) {
                final String pkgName = Classes.getPackageName(className);
                if (pkgName.startsWith(redirectedPackage.name())) {
                    packageNames.add(pkgName);
                }
                return true;
            }
        }.run(classpath, redirectedPackage.name().replace('.', '/'));
        for (String pkgName : packageNames) {
            // A redirected package is not expected to contain any Package classes, and the
            // expectation is that we load the entire tree.
            result.add(createRecursivePackageInstance(redirectedPackage, pkgName));
        }
        return result;

    }

    private Map<Class<? extends Scheme>, Class<? extends Scheme>> schemeTypeToImplementation;

    /**
     * Registers a class in this package that implements a given scheme type.
     *
     * @param schemeType a scheme type
     * @param schemeImplementation a class that implements {@code schemType}
     */
    public synchronized <S extends Scheme> void registerScheme(Class<S> schemeType, Class<? extends S> schemeImplementation) {
        assert schemeType.isInterface() || Modifier.isAbstract(schemeType.getModifiers());
        assert schemeImplementation.getPackage().getName().equals(name()) : "cannot register implmentation class from another package: " + schemeImplementation;
        if (schemeTypeToImplementation == null) {
            schemeTypeToImplementation = new IdentityHashMap<Class<? extends Scheme>, Class<? extends Scheme>>();
        }
        Class<? extends Scheme> oldValue = schemeTypeToImplementation.put(schemeType, schemeImplementation);
        assert oldValue == null;
    }

    /**
     * Gets the class within this package implementing a given scheme type (represented as an abstract class or interface).
     *
     * @return the class directly within this package that implements {@code scheme} or null if no such class
     *         exists
     */
    public synchronized <S extends Scheme> Class<? extends S> schemeTypeToImplementation(Class<S> schemeType) {
        if (schemeTypeToImplementation == null) {
            return null;
        }
        final Class< ? extends Scheme> implementation = schemeTypeToImplementation.get(schemeType);
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
            return packageName.equals(((MaxPackage) other).packageName);
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

    /**
     * Gets the set of packages excluded by this package. Excluded packages will not be loaded into a system that is
     * configured by package loading (such as the Maxine VM) if the package represented by this object is loaded. Such a
     * system should ensure that configuration fails if any excluded packages encountered on the class path before the
     * package that excludes them.
     */
    public Set<MaxPackage> excludes() {
        return Collections.emptySet();
    }

    public int compareTo(MaxPackage other) {
        final Set<MaxPackage> myPrerequisites = prerequisites();
        final Set<MaxPackage> otherPrerequisites = other.prerequisites();
        if (myPrerequisites.isEmpty()) {
            if (otherPrerequisites.isEmpty()) {
                return packageName.compareTo(other.packageName);
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
        return packageName.compareTo(other.packageName);
    }

    public synchronized <S extends Scheme> Class<? extends S> loadSchemeImplementation(Class<S> schemeType) {
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
    public synchronized <S extends Scheme> S loadAndInstantiateScheme(Class<S> schemeType) {
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
