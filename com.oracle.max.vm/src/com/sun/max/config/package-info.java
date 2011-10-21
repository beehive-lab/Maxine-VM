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
/**
 * Support for building the Maxine VM boot image, possibly including extension by third parties.
 *
 * Then general strategy for boot image building is:
 * <ul>
 * <li>Generate a set of packages, and hence classes, from some designated root packages that are <i>candidates</> for
 * inclusion in the boot image.
 * <li>Apply configuration options to limit the set of packages (classes) that are actually included. Examples of
 * configuration options would be hardware and operating system platforms and specific choices of the
 * {@link com.sun.max.vm.VMScheme schemes} that provide Maxine with much of its configurability.
 * </ul>
 * Note that while the boot image really is composed from a set of classes it almost always the case that all classes
 * from a package are included, so we can use package as shorthand for all the classes in the package. This is true, in
 * part, because we do not mix configuration-specific classes in the same package, but instead use sub-packages, e.g.
 * {@code com.sun.max.vm.compiler.target} and {@code com.sun.max.vm.compiler.target.amd64}. In particular, there is no
 * support for configuration-specific inclusion classes only entire packages. However, it is possible to specifically
 * enumerate the set of classes to be included from a given package, which is used mostly for JDK classes.
 * <p>
 * {@link com.sun.max.config.BootImagePackage} provides the basic mechanisms for constructing the set of
 * candidate packages. The check on whether to actually include a package is made later by the
 * boot image builder. Key to a package being a candidate for inclusion is the existence of a class named
 * {@code Package} in the package to be included. {@code Package} must be a subclass of
 * {@link com.sun.max.config.BootImagePackage}. It is usually a direct subclass, but need not be.
 * An extension could define a subclass of {@link com.sun.max.config.BootImagePackage} with
 * extension-specific configuration methods and subclass that instead. The key is that the class must be
 * named {@code Package}, since that is what the search mechanism looks for. An example of the simplest
 * form of this class is {@link com.sun.max.config.Package here}.
 * <p>
 * The package discovery process is initiated by the
 * {@link com.sun.max.config.BootImagePackage#getTransitiveSubPackages(com.sun.max.program.Classpath, BootImagePackage[])
 * method. The first argument denotes the class path that the boot image builder is using, which must contain
 * all the packages needed for the build. The second argument is an array of root packages. These essentially define a forest
 * of trees that will be used as the starting point to search for packages. The {@link com.sun.max.vm.hosted.BootImageGenerator
 * boot image generator} passes exactly one root, {@code com.sun.max.config.Package}, so all packages must be reachable
 * from this package. At first sight this seems highly restrictive. However, {@link com.sun.max.config.BootImagePackage}
 * provides a <i>redirection</i> mechanism whereby additional roots can be specified using the
 * {@link com.sun.max.config.BootImagePackage#BootImagePackage(String...) constructor variant}.
 * <p>
 * The virtue of the single root is that it is easy to find the starting point in either the Maxine VM codebase or
 * an extension. Maxine use four sub-packages of {@code com.sun.max.config} to collect together the packages to include in the
 * boot image, {@code base}, {@code c1x}, {@code jdk} and {@code vm}. The {@code Package} classes
 * in each of the these sub-packages use the redirection constructor to include other packages in different
 * parts of the package namespace. In general, those other packages do not contain {@code Package}
 * classes as the system creates them automatically as needed. The only reason to have a {@code Package}
 * class in a redirected sub-package is to override the default behavior of the parent. For example, platform-specific
 * packages must have a definition for the method
 * {@link com.sun.max.config.BootImagePackage#isPartOfMaxineVM(com.sun.max.vm.VMConfiguration)}.
 * <p>
 * Package discovery proceeds by iterating over the roots in order and, for each root,
 * scanning every element of the given class path looking for packages that are sub-packages
 * of the root (or the root itself). The class path scan handles both file system directories and jar files.
 * A global map of discovered packages is maintained across the entire process
 * and is used to produce the result. At this stage we are gathering package names as {@link String strings}.
 * The result for each root is a set of names stored as a {@link java.util.TreeSet}. Note that only packages
 * that are non-empty are added to the set. The package names are then processed
 * in the order returned by {@link java.util.TreeSet}, that is, lexicographic, so parent packages are processed
 * before children. An attempt is made to instantiate a {@code Package} class in the given
 * package using {@link Class#forName(String)}. If this succeeds the {@code Package} instance is added
 * to the map. Depending on which superclass constructor the {@code Package} class invoked, the resulting instance
 * may contain redirection references to other packages. Any redirection references that are tagged
 * as <i>recursive</i> become new roots that are added to  the end of the root array for later processing.
 * A redirection package is defined as one that is not with the subtree of the root being processed.
 * <p>
 * If the attempt is made to instantiate a {@code Package} class fails then a search is made
 * in the parent hierarchy to find a {@code Package} instance to use as the clone with which to create a new substitute instance.
 * The search proceeds back up the package hierarchy until a {@code Package} instance is found
 * in the global map that matches the name associated with the node at that point in the hierarchy. Note that such a node must exist
 * as roots always contain a {@code Package} instance. The discovered instance is then cloned and
 * its name set appropriately. Other state is reset to the default values. Note that, if you subclass
 * {@link com.sun.max.config.BootImagePackage} and add additional state it is your responsibility to decide
 * whether the state should be reset or not after a clone (and you must override the {@link Object#clone} method).
 * The fact that the nearest parent is used for the clone ensures that any overridden methods
 * in a {@code Package} class in an interior node carries forward to all child nodes. So, for example,
 * platform-specific configuration in a sub-tree works as expected.
 * <p>
 * Once the package is discovered, it added to the global map. If the package contains references to other packages
 * then each of these is also added to the map. Furthermore, any of these that refer to packages outside of this tree
 * and are tagged as recursive are added as new roots to be processed later.
 * <p>
 * Once all the roots are processed the set of packages contained in the global map is returned as the result.
 * <h2>Constructors</h2>
 * Note that all constructors are {@code protected} to ensure that they are only invoked from {@code Package} classes.
 * <ul>
 * <li>{@code Package()}: this is typically only used in packages that need to override
 * {@link com.sun.max.config.BootImagePackage#isPartOfMaxineVM}, and causes all of the classes
 * in the package containing the {@Code Package} class to be candidates for inclusion. Nested packages are not included unless
 * some parent package was specified as recursive.
 * <li>{@code Package(String name, boolean recursive)}: this is the simplest form of redirection, designating a single
 * root, {@code name}, and specifying via {@code recursive}, whether nested packages should be included (recursively).
 * <li>{@code Package(String name...)}: this is the most general form of constructor. {@code name} may designate a single (non-recursive) package,
 * using {@code a.b.c.*}, a (recursive) package tree using {@code a.b.c.**} or a single class using {@code a.b.c.D}. In the latter case, several
 * specifications may denote different classes from the package and the end result is the union of them all. Note that it is not possible, by design,
 * to exclude a class. However,  it is possible to exclude the classes of a package within an tree that is otherwise destined to be included by recursive inclusion.
 * This is achieve by inheriting from {@link com.sun.max.config.ExcludedPackage}, which simply defines
 * {@link com.sun.max.config.BootImagePackage#isPartOfMaxineVM} to return {@code false}.
 * <p>
 *
 *<h2>Scheme packages</h2>
 * Packages that contain scheme definitions must always contain a {@code Package} class and
 * override {@link com.sun.max.config.BootImagePackage#isPartOfMaxineVM}
 * since schemes are configurable. The logic for deciding whether the package is to be included is limited to testing the
 * instance or name of the {@link com.sun.max.config.BootImagePackage} that has been configured, or testing whether the
 * scheme class itself is assignable to the scheme class defined by the package under test. In particular the scheme instance
 * cannot be used since the schemes are no instantiated until after the configuration analysis is complete.
 * <p>
 * The constructor for the {@code Package} class must also register the scheme class using the
 * {@link com.sun.max.config.BootImagePackage#registerScheme(Class, Class)} method.
 */
package com.sun.max.config;
