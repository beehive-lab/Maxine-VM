/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * The Maxine Virtual Machine Advising/Analysis (VMA) package.
 *
 * The VMA package is intended to provide comprehensive framework for analysis for the Maxine VM.
 * The ultimate goal is that the analysis is applicable all levels of the system, i.e:
 * <ul>
 * <li>the virtual machine (VM) itself
 * <li>the platform (i.e. the JDK)
 * <li>the application classes
 *
 * The instrumentation that supports the analysis should be configurable at runtime
 * and replace ad hoc analysis, including in the VM itself, for example to detect hot methods for
 * recompilation. It should be possible to enable/disable the instrumentation dynamically.
 * Analysis should be possible both online and offline.
 * <p>
 * The implementation mechanism is based on adding <i>advice</i>, similar to aspect-oriented systems
 * such as <a href="http://www.eclipse.org/aspectj">AspectJ</a>. However, unlike Aspect/J which operates
 * at the language-level and achieves its effect by transforming the bytecodes of classes, VMA operates
 * at the virtual machine level and adds advice to the implementation of the bytecodes themselves.
 * Some VM actions, for example, garbage collection, can also generate advice. Currently this is
 * hard-wired into the VM code but, evidently, since Maxine is a meta-circular VM, this could
 * be done by adding specific advice at image build time, using some declarative mechanism
 * similar to <a href="http://www.eclipse.org/aspectj">AspectJ</a>.
 * <p>
 * The implementation has two components; data generation and data logging.
 * Eventually the instrumentation for data generation should be dynamically generated
 * and enabled/disabled using the standard mechanisms of the VM for recompilation.
 * Ideally the specification of the instrumentation would be declarative using a system
 * like Aspect/J or <a href="http://kenai.com/projects/btrace">BTrace</a>.
 * Currently, however, the instrumentation is hard-wired and must be statically defined at VM image build time.
 * <p>
 * The instrumentation is implemented using custom implementations of several Maxine schemes:
 * <ul>
 * <li>A minor {@link com.oracle.max.vm.ext.vma.heap.semi.heap.semi.VMASemiSpaceHeapScheme extension} of
 * {@link com.sun.max.vm.heap.sequential.semiSpace.SemiSpaceHeapScheme} for garbage collection
 * and object lifetime advice</li>
 * <li>An {@link com.oracle.max.vm.ext.t1x.vma.VMAT1X extension} of {@link com.oracle.max.vm.ext.t1x.T1X} that
 * uses modified templates to add advice to the bytecode implementations.</li>
 * <li>A {@link com.sun.max.vm.layout.xohm.XOhmLayoutScheme variant} of
 * {@link com.sun.max.vm.layout.ohm.OhmLayoutScheme} that adds an extra field to the object header
 * for storing unique object identifiers and other state.</li>
 * </ul>
 * <p>
 * Bootstrap issues currently preclude instrumenting the classes in the boot image, which is built using the
 * optimizing compiler. Dynamically loaded
 * classes are instrumented using the custom scheme}. Since the optimizing compiler does not support
 * advising, the normal threshold recompilation is disabled for instrumented classes.
 * <p>
 * The package evolved from a system based on bytecode transformation that was used to analyse
 * the immutability of objects over their lifetime, cf.{@link com.sun.max.annotate.CONSTANT_WHEN_NOT_ZERO},
 * and this is still reflected somewhat in the current capabilities of the analysis. The default advice
 * is currently limited to object creation and lifetime, field and array writes and reads. However, this enables
 * a large number of interesting analyses, including immutability, thread-object locality, etc.
 *
 * The default data logging mechanism is a compressed text file, see {@link com.oracle.max.vma.ot.log.txt.TextObjectTrackerLog}
 * and {@link com.oracle.max.vma.ot.log.txt.CompactTextObjectTrackerLog}.
 */
package com.oracle.max.vm.ext.vma;
