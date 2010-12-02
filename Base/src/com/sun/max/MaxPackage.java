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
 * Various subclasses of this package exist for parts of Maxine, namely:
 * <ul>
 * <li>{@link BasePackage} for base (utility) packages in {@code com.sun.max}.
 * <li>{@link AsmPackage} for packages in the Maxine assembler.
 * <li>{@link VMPackage} for packages in the VM proper.
 * <li>{@link ExtPackage} for the extension packages.
 * <li>{@link JDKPackage} for JDK packages
 * </ul>
 * Generally, for the classes in a package to be included in Maxine, each sub-package must contain
 * a class named {@code Package} that is a subclass of one of the above.
 * However, it is possible to indicate recursive inclusion at the root by using the appropriate constructor.
 * In the recursive case, anonymous instances are cloned from the root  instance, with
 * {@link #packageName} appropriately modified.
 * <p>
 * It is also possible to redirect the search to another package for extensions that must lie outside the
 * {@code com.sun.max} package name space. These always support recursion.
 * <p>
 * The exact set of classes that are loaded from a package can be further constrained by the
 * {@link #setExclusions(String...)} and {@link #setInclusions(String...)} methods.
 * <p>
 * To deal with initialization cycles with code that would naturally execute in the constructor,
 * this can be delayed until just before loading by overriding the {@link #loading} method.
 * N.B. be aware that this will be called for all cloned instances in the recursive context.
 * Before any class is actually loaded it is checked for inclusion by calling {@link #isIncluded(String)}.
 * The default implementation checks the {@link #classExclusions} and {@link #classInclusions} lists.
 * <p>
 * It is recommended to include a file called 'package-info.java' in every package, which is where you can put
 * package-related JavaDoc comments.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Mick Jordan
 */
public class MaxPackage implements Comparable<MaxPackage>, Cloneable {

    private String packageName;
    private final String[] redirects;
    private final boolean recursive;
    private List<String> classInclusions;
    private List<String> classExclusions;
    private static final Class[] NO_CLASSES = new Class[0];

    /**
     * No redirection, no recursion.
     */
    protected MaxPackage() {
        this(null, false);
    }

    /**
     * No redirection, optional recursion.
     * @param recursive
     */
    protected MaxPackage(boolean recursive) {
        this(null, recursive);
    }

    /**
     * Redirection and recursion.
     * @param packageName
     */
    protected MaxPackage(String... redirects) {
        this(redirects, true);
    }

    /**
     * Redirection optional recursion.
     *
     * @param redirects
     * @param recursive
     */
    protected MaxPackage(String[] redirects, boolean recursive) {
        this.packageName = getClass().getPackage().getName();
        this.redirects = redirects;
        this.recursive = recursive;
    }

    /**
     * Specifies a list of just those classes that should be loaded from this package.
     * @param classNames
     */
    protected void setInclusions(String... classNames) {
        if (classInclusions == null) {
            classInclusions = new ArrayList<String>(classNames.length);
        }
        for (String classname : classNames) {
            classInclusions.add(classname);
        }
    }

    /**
     * As {@link #setInclusions(String...), except specified as {@Class classes} for better error checking.
     * @param classes
     */
    protected void setInclusions(Class<?>... classes) {
        for (Class<?> klass : classes) {
            checkInExclusion(klass);
            setInclusions(klass.getSimpleName());
        }
    }

    /**
     * Specifies a list of classes to exclude from this package.
     * @param classNames
     */
    protected void setExclusions(String... classNames) {
        if (classExclusions == null) {
            classExclusions = new ArrayList<String>(classNames.length);
        }
        for (String classname : classNames) {
            classExclusions.add(classname);
        }
    }

    /**
     * As {@link #setExclusions(String...), except specified as {@Class classes} for better error checking.
     * @param classes
     */
    protected void setExclusions(Class<?>... classes) {
        for (Class<?> klass : classes) {
            checkInExclusion(klass);
            setExclusions(klass.getSimpleName());
        }
    }

    private void checkInExclusion(Class<?> klass) {
        final String classPackageName = klass.getPackage().getName();
        if (!classPackageName.equals(packageName)) {
            throw new IllegalArgumentException("class " + klass.getName() + " is not in package " + packageName);
        }
    }

    /**
     * Determines whether a given class should be included based on {{@link #setExclusions(String...)} and {@link #setInclusions(String...)}.
     * @param qualClassName fully qualified name of class to check for inclusion
     * @return true if and only if the class should be included (loaded)
     */
    public boolean isIncluded(String qualClassName) {
        final int index = qualClassName.lastIndexOf('.');
        if (!packageName.equals(qualClassName.substring(0, index))) {
            return false;
        }

        if (classInclusions == null && classExclusions == null) {
            return true;
        }
        final String className = qualClassName.substring(index + 1);
        // If no inclusions then default is to include unless excluded below
        boolean included = classInclusions == null;
        // inclusions take precedence
        if (classInclusions != null) {
            for (String classInclusion : classInclusions) {
                if (classInclusion.equals(className)) {
                    Trace.line(1, "  class " + className + " explicitly included");
                    included = true;
                    break;
                }
            }
        }
        if (included) {
            if (classExclusions != null) {
                for (String classExclusion : classExclusions) {
                    if (classExclusion.equals(className)) {
                        Trace.line(1, "  class " + className + " explicitly excluded");
                        included = false;
                        break;
                    }
                }

            }
        }
        return included;
    }

    /**
     * The list of classes to be included from this package.
     * Only these classes should be loaded.
     * If the list is not empty then {@link #getClassExclusions()} is implicitly defined.
     * @return list of classes to include or {@code null}
     */
    public List<String> getClassInclusions() {
        return classInclusions;
    }

    /**
     * The list of class to be excluded from this package.
     * All classes except these should be loaded from the package.
     * If the list is not empty then {@link #getInclusions()} is implicitly defined.
     * @return list of classes to exclude or {@code null}
     */
    public List<String> getClassExclusions() {
        return classExclusions;
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

    // @Deprecated - only used in a CPS test
    public static MaxPackage fromJava(String name) {
        final MaxPackage result =  new MaxPackage();
        result.packageName = name;
        return result;
    }

    public static MaxPackage fromClass(Class javaClass) {
        final java.lang.Package javaPackage = javaClass.getPackage();
        if (javaPackage == null) {
            return null;
        }
        return fromName(javaPackage.getName());
    }

    public java.lang.Package toJava() {
        return java.lang.Package.getPackage(name());
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

    private static class RootPackageInfo {
        MaxPackage root;
        Class<?> matcherClass;
        Set<String> packageNames = new TreeSet<String>();
        List<MaxPackage> packages;
        RootPackageInfo(MaxPackage root, Class<?> matcherClass) {
            this.root = root;
            this.matcherClass = matcherClass;
        }
    }

    /**
     * Finds all sub-packages in the classpath that are an instance of the superclass of the classes specified in
     * {@code rootPackages}. The actual classes should be {@link Package} instances from the package defining the search
     * start point. One pass is made over the file system and the resulting list is all matching packages. However, the
     * packages for each root are kept separate until the end when they are then merged in the order
     * in {@code rootPackages}. The ordering is important for packages that use {@link MaxPackage#excludes}.
     * HACK alert: To support CPS we sort any root that has packages with non-empty prerequisites.
     * TODO eliminate prerequisites when CPS dies
     *
     * @param classpath
     * @param rootPackages array of subclass of {@code MaxPackage} that define search start and match
     * @return list of subclasses of {@link MaxPackage}
     */
    public static List<MaxPackage> getTransitiveSubPackages(Classpath classpath, final MaxPackage[] rootPackages) {
        final RootPackageInfo[] rootPackagesInfo = new RootPackageInfo[rootPackages.length];
        // Standard use has one root with a common prefix. Arbitrary paths would be more complicated.
        MaxPackage baseRoot = null;
        int index = 0;
        for (MaxPackage root : rootPackages) {
            if (isPrefixOf(root, rootPackages)) {
                baseRoot = root;
            }
            rootPackagesInfo[index++] = new RootPackageInfo(root, root.getClass().getSuperclass());

        }
        if (baseRoot == null) {
            ProgramError.unexpected("MaxPackage.getTransitiveSubPackages: roots have no common prefix");
        }

        // search from baseRoot
        new ClassSearch() {

            @Override
            protected boolean visitClass(String className) {
                final String pkgName = Classes.getPackageName(className);
                for (int i = 0; i < rootPackages.length; i++) {
                    MaxPackage root = rootPackages[i];
                    if (pkgName.startsWith(root.name())) {
                        rootPackagesInfo[i].packageNames.add(pkgName);
                    }
                }
                return true;
            }
        }.run(classpath, baseRoot.name().replace('.', '/'));

        // Now find Package classes that match the roots
        int totalSize = 0;
        for (RootPackageInfo rootPackageInfo : rootPackagesInfo) {
            rootPackageInfo.packages = new ArrayList<MaxPackage>(rootPackageInfo.packageNames.size());
            for (String pkgName : rootPackageInfo.packageNames) {
                MaxPackage maxPackage = MaxPackage.fromName(pkgName);
                MaxPackage recursiveParent = null;
                if (maxPackage == null) {
                    // check for a parent with recursive == true
                    // there is an assumption that parent packages are processed before sub-packages
                    for (MaxPackage parent : rootPackageInfo.packages) {
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
                if ((recursiveParent == null && rootPackageInfo.matcherClass.isInstance(maxPackage)) || (recursiveParent != null && rootPackageInfo.matcherClass.isInstance(recursiveParent))) {
                    rootPackageInfo.packages.add(maxPackage);
                    if (maxPackage.redirects != null && maxPackage.redirects.length > 0) {
                        /* the above recursive traversal only operates in sub-packages of "this"
                         * so we have to process redirected packages explicitly.
                         */
                        for (String redirect : maxPackage.redirects) {
                            rootPackageInfo.packages.addAll(checkReDirectedSubpackages(classpath, maxPackage, redirect));
                        }
                    }
                }
            }
            totalSize += rootPackageInfo.packages.size();
        }
        final MaxPackage[] result = new MaxPackage[totalSize];
        index = 0;
        for (RootPackageInfo rootPackageInfo : rootPackagesInfo) {
            final MaxPackage[] temp = rootPackageInfo.packages.toArray(new MaxPackage[rootPackageInfo.packages.size()]);
            if (hasPrerequisites(temp)) {
                Arrays.sort(temp);
            }
            System.arraycopy(temp, 0, result, index, temp.length);
            index += temp.length;
        }
        return Arrays.asList(result);
    }

    public List<MaxPackage> getTransitiveSubPackages(Classpath classpath) {
        return getTransitiveSubPackages(classpath, new MaxPackage[] {this});
    }

    private static boolean hasPrerequisites(MaxPackage[] packages) {
        for (MaxPackage maxPackage : packages) {
            if (!maxPackage.prerequisites().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPrefixOf(MaxPackage p, MaxPackage[] set) {
        for (MaxPackage s : set) {
            if (!s.name().startsWith(p.name())) {
                return false;
            }
        }
        return true;
    }


    /**
     * Called after this instance is created for recursively sub-packages.
     * Allows {@link #setExclusions(String...)} or {@link #setInclusions(String...)} to
     * be called for such instances.
     */
    protected void recursiveOverride() {
    }

    /**
     * Create a {@code MaxPackage} instance that denotes a sub-package of a recursively included package
     * by cloning the parent and resetting the {@link #packageName}.
     * @param parent
     * @param pkgName
     * @return
     */
    private static MaxPackage createRecursivePackageInstance(MaxPackage parent, String pkgName) {
        try {
            MaxPackage pkg = (MaxPackage) parent.clone();
            pkg.packageName = pkgName;
            pkg.recursiveOverride();
            return pkg;
        } catch (CloneNotSupportedException ex) {
            throw ProgramError.unexpected("MaxPackage failed to clone " + parent);
        }
    }

    private static List<MaxPackage> checkReDirectedSubpackages(Classpath classpath, MaxPackage redirector, final String redirectedPackage) {
        final ArrayList<MaxPackage> result = new ArrayList<MaxPackage>();
        final Set<String> packageNames = new TreeSet<String>();
        final boolean exactMatch = redirector.recursive == false;
        // Find all the packages that start with or equal  the redirected package name.
        // If redirector.recursive == false, we only process the actual redirected package (exact match)
        new ClassSearch() {
            @Override
            protected boolean visitClass(String className) {
                final String pkgName = Classes.getPackageName(className);
                if (exactMatch && pkgName.equals(redirectedPackage) || !exactMatch && pkgName.startsWith(redirectedPackage)) {
                    packageNames.add(pkgName);
                }
                return true;
            }
        }.run(classpath, redirectedPackage.replace('.', '/'));
        for (String pkgName : packageNames) {
            /* A redirected package is not expected to contain any Package classes, and the
            * expectation is that we load the entire tree. However, any Package classes
            * that do exist are honored.
            */
            MaxPackage maxPackage = MaxPackage.fromName(pkgName);
            if (maxPackage == null) {
                maxPackage = createRecursivePackageInstance(redirector, pkgName);
            }
            result.add(maxPackage);
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
