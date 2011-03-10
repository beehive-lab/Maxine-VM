/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * This package contains Maxine's implementation of the compiler interface required by the C1X compiler, including
 * adapting the Maxine VM's runtime data structures for use in C1X and also consuming the output from C1X compilations.
 * <p>
 * The adaptation for Maxine results in a set of classes, for example, {@link com.sun.max.vm.compiler.c1x.MaxRiRuntime}
 * that implement the corresponding interfaces defined in C1X, in this case {@link com.sun.cri.ri.RiRuntime}. The adaptor
 * classes essentially provide a mapping between Maxine classes and the functionally similar C1X classes.
 * Note that the majority of the interfaces, e.g., {@link com.sun.cri.ri.RiMethod} do not require adaptation as they are implemented
 * directly by Maxine, e.g., {@link com.sun.max.vm.actor.member.MethodActor}.
 * <p>
 * This package defines {@link com.sun.max.vm.compiler.c1x.C1X} which implements
 * {@link com.sun.max.vm.compiler.RuntimeCompiler} and conforms to the standard API for schemes in Maxine. In
 * particular it implements the {@link com.sun.max.vm.VMScheme#initialize} method that is called during VM startup with
 * the startup phase as argument. Note that this includes the special
 * {@link com.sun.max.vm.MaxineVM.Phase#BOOTSTRAPPING bootstrapping} phase which is used when the VM image is being
 * built using another host JVM. In this phase several objects are created that are used in subsequent compilations:
 * <ul>
 * <li>{@link com.sun.max.vm.compiler.c1x.C1X#c1xRuntime}: an instance of
 * {@link com.sun.max.vm.compiler.c1x.MaxRiRuntime} that implements {@link com.sun.cri.ri.RiRuntime}.</li>
 * <li>{@link com.sun.max.vm.compiler.c1x.C1X#c1xTarget}: an instance of {@link com.sun.cri.ci.CiTarget}
 * that represents the target machine architecture.</li>
 * <li>{@link com.sun.max.vm.compiler.c1x.C1X#c1xXirGenerator}: an instance of
 * {@link com.sun.max.vm.compiler.c1x.MaxXirGenerator} that extends {@link com.sun.cri.xir.RiXirGenerator} class.</li>
 * <li>{@link com.sun.max.vm.compiler.c1x.C1X#c1xCompiler} : an instance of
 * {@link com.sun.c1x.C1XCompiler} , which is created with arguments
 * {@link com.sun.max.vm.compiler.c1x.C1X#c1xRuntime} ,
 * {@link com.sun.max.vm.compiler.c1x.C1X#c1xTarget} and
 * {@link com.sun.max.vm.compiler.c1x.C1X#c1xXirGenerator} .
 * </ul>
 * The compilation entry method in {@link com.sun.max.vm.compiler.RuntimeCompiler} is
 * {@link com.sun.max.vm.compiler.RuntimeCompiler#compile} which returns a
 * {@link com.sun.max.vm.compiler.target.TargetMethod}. The {@link com.sun.max.vm.compiler.c1x.C1X}
 * implementation of {@link com.sun.max.vm.compiler.RuntimeCompiler#compile} first gets an
 * {@link com.sun.cri.ri.RiMethod} for the {@link com.sun.max.vm.actor.member.ClassMethodActor} using
 * {@link com.sun.max.vm.compiler.c1x.C1X#c1xRuntime} and then calls
 * {@link com.sun.c1x.C1XCompiler#compileMethod}. The result is {@code null} if the compilation failed, otherwise an
 * instance of {@link com.sun.cri.ci.CiTargetMethod}, which encapsulates all the information about the resulting
 * compiled code, in particular, a byte array containing the actual bytes for the generated machine code, which is
 * available from {@link com.sun.cri.ci.CiTargetMethod#targetCode}. The {@link com.sun.cri.ci.CiTargetMethod} is wrapped
 * in a {@link com.sun.max.vm.compiler.c1x.C1XTargetMethod}, a subclass of Maxine's
 * {@link com.sun.max.vm.compiler.target.TargetMethod} class and returned as the result of
 * {@link com.sun.max.vm.compiler.c1x.C1X#compile}.
 */
package com.sun.max.vm.compiler.c1x;
