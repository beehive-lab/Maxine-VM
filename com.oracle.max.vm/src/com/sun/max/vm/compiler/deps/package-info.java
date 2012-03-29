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
 * Management of dependencies from compiled methods to classes, methods and
 * other entities where such dependencies may change and result in some
 * action (e.g. deoptimization) being applied to a compiled method.
 *
 * <h1>Overall Architecture</h1>
 * A <dependency> is a relationship between a {@link TargetMethod}, that is, the result of a compilation,
 * and an assumption that was made by the compiler during the compilation. The assumption may be any invariant
 * that can be checked for validity at any future time.
 * <p>
 * Assumptions are specified by subclasses of {@link CiAssumptions.Assumption}. Instances of such classes
 * typically contain references to VM objects that, for example, represent methods, i.e., {@link RiResolvedMethod}.
 * Note that assumptions at this level are generally specified using compiler and VM independent types,
 * and are defined in a compiler and VM project (package). However, there is nothing that prevents a VM specific assumption
 * being defined using VM specific types.
 * <p>
 * Since an assumption has to be validated any time the global state of the VM changes, for example, a new class
 * is loaded, they must persist as long as the associated {@linkplain TargetMethod}.
 * To minimize the amount of storage space occupied by assumptions, and to simplify analysis in a
 * concrete VM, validated assumptions are converted to {@link Dependencies dependencies}, which
 * use a densely encoded form of the concrete VM types using small integers, such as {@link ClassID}.
 * <p>
 * All assumptions have an associated <i>context</i> class which, loosely, identifies the class that
 * the assumption affects. For example, the {@link CiAssumptions.ConcreteSubtype concrete subtype}
 * assumption specifies that a class {@code T} has a single unique subtype {@code U}. In this case,
 * {@code T} is defined to be the context class.
 * <p>
 * The possible set of assumptions and associated dependencies is open-ended. In order to provide for easy extensibility
 * while keeping the core of the system independent, the concept of a {@link DependencyProcessor} is introduced.
 * A {@linkplain DependencyProcessor} is responsible for the following:
 * <ol>
 * <li> the validation of the associated assumption.</li>
 * <li> the encoding of the assumption into an efficient packed form</li>
 * <li> the processing of the packed form, converting back to an object form for ease of analysis</li>
 * <li> supporting the application of a {@link DependencyVisitor dependency visitor} for analysis</li>
 * <li> providing a string based representation of the dependency for tracing</li>
 * </ol>
 * <h2>Analysing Dependencies</h2>
 * A {@link DependencyVistor visitor pattern} is used to support the analysis of a {@linkplain Dependencies} instance.
 * Recall that each such instance relates to a single {@linkplain TargetMethod}, may contain dependencies related to
 * several context classes and each of these may contain dependencies corresponding to several
 * {@linkplain DependencyProcessor dependency processors}.
 * <p>
 * Since the set of {@linkplain DependencyProcessor dependency processors} is open ended, and a visitor may want to visit
 * the data corresponding to several dependency processors in one visit, implementation class inheritance cannot be used to
 * create a specific visitor. Instead, a two-level type structure is used, with interfaces defined in the
 * {@linkplain DependencyProcessor} class declaring the statically typed methods that result from decoding the packed
 * form of the dependency. Note that these typically correspond closely to the original {@linkplain CiAssumptions.Assumption}
 * but with compiler/VM independent types replaced with Maxine specific types. E.g., {@link RiResolvedType} replaced with
 * {@linkplain ClassActor}.
 * <p>
 * <h3>{@linkplain DependencyVisitor}</h3>
 * {@linkplain DependencyVisitor handles the aspects of the iteration that are independent of the
 * {@linkplain DependencyProcessor dependency processors}. See {@link DependencyVisitor} for more details.
 * <p>
 * The data for each {@linkplain DependencyProcessor dependency processor} is processed by
 * delegating to {@link DependencyProcessor#visitAll}, which will in turn invoke {@linkplain DependencyVisitor#visit}
 * for each individual dependency. This method is generic since it cannot know anything about the types
 * of the data associated with the dependency. The default implementation handles this by calling
 * {@link DependencyProcessor#match(DependencyVisitor dependencyVisitor} which returns {@code dependencyVisitor} null if
 * implements the {@link DependencyProcessorVisitor} interface defined by the processor that specifies the types of the data in the dependency,
 * or {@code null} if not. It then invokes {@link DependencyProcessor#visit} with this value, which invokes the
 * typed method if the value is non-null, and steps the index to the next dependency. Defining {@link DependencyProcessor#visit}
 * this way allows a different {@link DependencyProcessorVisitor} to be called by an overriding implementation of
 * {@linkplain DependencyVisitor#visit}. For example, a visitor that cannot know all the  {@linkplain DependencyProcessor dependency processors}
 * in the system, yet wants to invoke the {@linkplain com.sun.max.vm.compiler.deps.DependencyProcessor.ToStringDependencyProcessorVisitor}.
 */
package com.sun.max.vm.compiler.deps;
