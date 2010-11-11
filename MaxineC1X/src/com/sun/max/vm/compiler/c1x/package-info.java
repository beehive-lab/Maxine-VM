/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
 * MaxineC1X defines {@link com.sun.max.vm.compiler.c1x.C1XCompilerScheme} which implements
 * {@link com.sun.max.vm.compiler.RuntimeCompilerScheme} and conforms to the standard API for schemes in Maxine. In
 * particular it implements the {@link com.sun.max.vm.VMScheme#initialize} method that is called during VM startup with
 * the startup phase as argument. Note that this includes the special
 * {@link com.sun.max.vm.MaxineVM.Phase#BOOTSTRAPPING bootstrapping} phase which is used when the VM image is being
 * built using another host JVM. In this phase several objects are created that are used in subsequent compilations:
 * <ul>
 * <li>{@link com.sun.max.vm.compiler.c1x.C1XCompilerScheme#c1xRuntime}: an instance of
 * {@link com.sun.max.vm.compiler.c1x.MaxRiRuntime} that implements {@link com.sun.cri.ri.RiRuntime}.</li>
 * <li>{@link com.sun.max.vm.compiler.c1x.C1XCompilerScheme#c1xTarget}: an instance of {@link com.sun.cri.ci.CiTarget}
 * that represents the target machine architecture.</li>
 * <li>{@link com.sun.max.vm.compiler.c1x.C1XCompilerScheme#c1xXirGenerator}: an instance of
 * {@link com.sun.max.vm.compiler.c1x.MaxXirGenerator} that extends {@link com.sun.cri.xir.RiXirGenerator} class.</li>
 * <li>{@link com.sun.max.vm.compiler.c1x.C1XCompilerScheme#c1xCompiler} : an instance of
 * {@link com.sun.c1x.C1XCompiler} , which is created with arguments
 * {@link com.sun.max.vm.compiler.c1x.C1XCompilerScheme#c1xRuntime} ,
 * {@link com.sun.max.vm.compiler.c1x.C1XCompilerScheme#c1xTarget} and
 * {@link com.sun.max.vm.compiler.c1x.C1XCompilerScheme#c1xXirGenerator} .
 * </ul>
 * The compilation entry method in {@link com.sun.max.vm.compiler.RuntimeCompilerScheme} is
 * {@link com.sun.max.vm.compiler.RuntimeCompilerScheme#compile} which returns a
 * {@link com.sun.max.vm.compiler.target.TargetMethod}. The {@link com.sun.max.vm.compiler.c1x.C1XCompilerScheme}
 * implementation of {@link com.sun.max.vm.compiler.RuntimeCompilerScheme#compile} first gets an
 * {@link com.sun.cri.ri.RiMethod} for the {@link com.sun.max.vm.actor.member.ClassMethodActor} using
 * {@link com.sun.max.vm.compiler.c1x.C1XCompilerScheme#c1xRuntime} and then calls
 * {@link com.sun.c1x.C1XCompiler#compileMethod}. The result is {@code null} if the compilation failed, otherwise an
 * instance of {@link com.sun.cri.ci.CiTargetMethod}, which encapsulates all the information about the resulting
 * compiled code, in particular, a byte array containing the actual bytes for the generated machine code, which is
 * available from {@link com.sun.cri.ci.CiTargetMethod#targetCode}. The {@link com.sun.cri.ci.CiTargetMethod} is wrapped
 * in a {@link com.sun.max.vm.compiler.c1x.C1XTargetMethod}, a subclass of Maxine's
 * {@link com.sun.max.vm.compiler.target.TargetMethod} class and returned as the result of
 * {@link com.sun.max.vm.compiler.c1x.C1XCompilerScheme#compile}.
 */
package com.sun.max.vm.compiler.c1x;
