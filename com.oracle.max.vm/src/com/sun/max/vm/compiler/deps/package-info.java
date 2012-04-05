/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * <h2>Management of Code Dependencies</h2>
 * The Management of dependencies from compiled methods to classes, methods and
 * other entities where such dependencies may change and result in some
 * action (e.g. deoptimization) being applied to a compiled method.
 * <h2>Overall Architecture</h2>
 * A <i>dependency</i> is a relationship between a {@link com.sun.max.vm.compiler.target.TargetMethod}, that is, the result of a compilation,
 * and an assumption that was made by the compiler during the compilation. The assumption may be any invariant
 * that can be checked for validity at a future time.
 * <p>
 * Assumptions are specified by subclasses of {@link com.sun.cri.ci.CiAssumptions.Assumption}. Instances of such classes
 * typically contain references to VM objects that, for example, represent methods, i.e., {@link com.sun.cri.ri.RiResolvedMethod}.
 * Note that assumptions at this level are generally specified using compiler and VM independent types,
 * and are defined in a compiler and VM independent project (package). However, there is nothing that prevents a VM specific assumption
 * being defined using VM specific types.
 * <p>
 * Since an assumption has to be validated any time the global state of the VM changes, for example, a new class
 * is loaded, it must persist as long as the associated {@link com.sun.max.vm.compiler.target.TargetMethod}.
 * To minimize the amount of storage space occupied by assumptions, and to simplify analysis in a
 * concrete VM, validated assumptions are converted to {@link com.sun.max.vm.compiler.deps.Dependencies dependencies}, which
 * use a densely encoded form of the concrete VM types using small integers, such as {@link com.sun.max.vm.actor.holder.ClassID}.
 * <p>
 * All assumptions have an associated <i>context</i> class which identifies the class that
 * the assumption affects. For example, the {@link com.sun.cri.ci.CiAssumptions.ConcreteSubtype concrete subtype}
 * assumption specifies that a class {@code T} has a single unique subtype {@code U}. In this case,
 * {@code T} is defined to be the context class.
 * <p>
 * The possible set of assumptions and associated dependencies is open-ended. In order to provide for easy extensibility
 * while keeping the core of the system independent, the concept of a {@link com.sun.max.vm.compiler.deps.DependencyProcessor} is introduced.
 * A {@link com.sun.max.vm.compiler.deps.DependencyProcessor} is responsible for the following:
 * <ol>
 * <li> the validation of the associated assumption.</li>
 * <li> the encoding of the assumption into an efficient packed form</li>
 * <li> the processing of the packed form, converting back to an object form for ease of analysis</li>
 * <li> supporting the application of a {@link com.sun.max.vm.compiler.deps.DependencyVisitor dependency visitor} for analysis</li>
 * <li> providing a string based representation of the dependency for tracing</li>
 * </ol>
 * <h2>Analysing Dependencies</h2>
 * A {@link com.sun.max.vm.compiler.deps.Dependencies.DependencyVisitor visitor pattern} is used to support the analysis of a
 * {@link com.sun.max.vm.compiler.deps.Dependencies} instance.
 * Recall that each such instance relates to a single {@link TargetMethod}, may contain dependencies related to
 * several context classes and each of these may contain dependencies corresponding to several
 * {@linkplain com.sun.max.vm.compiler.deps.DependencyProcessor dependency processors}.
 * <p>
 * Since the set of {@linkplain com.sun.max.vm.compiler.deps.DependencyProcessor dependency processors} is open ended, and a visitor may want to visit
 * the data corresponding to several dependency processors in one visit, implementation class inheritance cannot be used to
 * create a specific visitor. Instead, a two-level type structure is used, with interfaces defined in the specific
 * {@link com.sun.max.vm.compiler.deps.DependencyProcessor} class that declare the statically typed methods that result from decoding the packed
 * form of the dependency. Note that these typically correspond closely to the original {@link com.sun.cri.ci.CiAssumptions.Assumption}
 * but with compiler/VM independent types replaced with Maxine specific types. E.g., {@link com.sun.cri.ri.RiResolvedType} replaced with
 * {@link com.sun.max.vm.actor.holder.ClassActor}.
 * <h3>Dependencies Visitor</h3>
 * {@link com.sun.max.vm.compiler.deps.Dependencies.DependencyVisitor} handles the aspects of the iteration that are independent of the
 * {@link com.sun.max.vm.compiler.deps.DependencyProcessor dependency processors}.
 * See {@link com.sun.max.vm.compiler.deps.Dependencies.DependencyVisitor} for more details.
 * <p>
 * The data for each {@linkplain com.sun.max.vm.compiler.deps.DependencyProcessor dependency processor} is visited by
 * invoking {@link com.sun.max.vm.compiler.deps.Dependencies.DependencyVisitor#visit}
 * for each individual dependency. This method is generic since it cannot know anything about the types
 * of the data associated with the dependency. The default implementation handles this by calling
 * {@link com.sun.max.vm.compiler.deps.DependencyProcessor#match} which returns {@code dependencyVisitor} if the visitor
 * implements the {@link com.sun.max.vm.compiler.deps.DependencyProcessorVisitor} interface defined by the processor that specifies the types of the data in the dependency,
 * or {@code null} if not. It then invokes {@link com.sun.max.vm.compiler.deps.DependencyProcessor#visit} with this value, which invokes the
 * typed method in the interface if the value is non-null, and steps the index to the next dependency. Defining {@link com.sun.max.vm.compiler.deps.DependencyProcessor#visit}
 * this way allows a different {@link com.sun.max.vm.compiler.deps.DependencyProcessorVisitor} to be called by an overriding implementation of
 * {@link com.sun.max.vm.compiler.deps.Dependencies.DependencyVisitor#visit}. For example, a visitor that cannot know all the  {@linkplain com.sun.max.vm.compiler.deps.DependencyProcessor dependency processors}
 * in the system, yet wants to invoke the {@link com.sun.max.vm.compiler.deps.DependencyProcessor.ToStringDependencyProcessorVisitor}.
 * <h2>Defining a new Dependency Processor</h2>
 * The first step is to define a new subclass of {@link com.sun.cri.ci.CiAssumptions.Assumption}. If, as is typical, the dependency is
 * used within the optimizing compiler, then this subclass should be defined by adding it to {@link com.sun.cri.ci.CiAssumptions}.
 * <p>
 * Next define a subclass of {@link DependencyProcessor} that will handle this assumption in Maxine, and place it in the
 * {@code com.sun.max.vm.compiler.deps} package. Define a nested interface that extends
 * of {@link com.sun.max.vm.compiler.deps.DependencyProcessorVisitor} and defines a method with the same arguments as the method in the
 * {@link CiAssumptions.Assumption} subclass. To support generic tracing of dependencies you should also define
 * a subclass of {@link com.sun.max.vm.compiler.deps.DependencyProcessor.ToStringDependencyProcessorVisitor} that implements your interface method(s) and appends appropriate
 * tracing data to the {@link java.lang.StringBuilder} variable in {@link com.sun.max.vm.compiler.deps.DependencyProcessor.ToStringDependencyProcessorVisitor}.
 * <p>
 * Define a {@code static final} instance of the {@link com.sun.max.vm.compiler.deps.DependencyProcessor} subclass, which will cause it to be
 * registered with {@link com.sun.max.vm.compiler.deps.DependenciesManager} during boot image generation.
 * <p>
 * Finally, implement the remaining abstract methods:
 * <ul>
 * <li>{@link com.sun.max.vm.compiler.deps.DependencyProcessor#match}</li>
 * <li>{@link com.sun.max.vm.compiler.deps.DependencyProcessor.getToStringDependencyProcessorVisitor}</li>
 * <li>{@link com.sun.max.vm.compiler.deps.DependencyProcessor#visit}</li>
 * </ul>
 * The first two have trivial implementations. The {@code visit} method must step over the specific dependency
 * data and, if the {@code dependencyProcessorVisitor} is not {@code null}, invoke the associated method,
 * with the encoded data transformed into the appropriate argument types. Evidently, if the visitor is {@code null},
 * processing related to transforming the encoded data should be avoided.
 *
 */
package com.sun.max.vm.compiler.deps;

