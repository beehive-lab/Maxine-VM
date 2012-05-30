/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * This package contains the essential runtime components of the current advice handling
 * framework. The actual implementation of {@link com.oracle.max.vm.ext.vma.VMAdviceHandler}
 * is chosen by {@link com.oracle.max.vm.ext.vma.runtime.VMAdviceHandlerFactory.
 * <p>
 * To ensure that newly created threads generate advice a
 * {@link com.oracle.max.vm.ext.vma.runtime.VMAVmThread subclass} of
 * {@link com.sun.max.vm.thread.VmThread} is created by defining a
 * {@link com.oracle.max.vm.ext.vma.runtime.VMAVmThreadFactory custom}
 * {@link com.sun.max.vm.thread.VmThreadFactory thread factory}.
 * <p>
 * The connection between the compiled code, i.e., the template-based bytecode implementations,
 * is handled through a facade, {@link com.oracle.max.vm.ext.vma.runtime.VMAStaticBytecodeAdvice},
 * which defines static methods that correspond to the {@link com.oracle.max.vm.ext.vma.VMAdviceHandler}
 * methods, to avoid virtual dispatch in the templates. The facade gets the actual implementation
 * of {@link com.oracle.max.vm.ext.vma.VMAdviceHandler} from {@link com.oracle.max.vm.ext.vma.run.java.VMAJavaRunScheme}
 * and simply invokes the corresponding virtual method.
 * <p>
 * The default implementation of {@link com.oracle.max.vm.ext.vma.VMAdviceHandler}
 * is {@link com.oracle.max.vm.ext.vma.handlers.log.sync.h.SyncLogVMAdviceHandler}, which
 * performs synchronous logging of the advice events using an instance of
 * {@link com.oracle.max.vm.ext.vma.handlers.log.VMAdviceHandlerLog}. I.e, the same thread that
 * generated the advice is used to log the advice. An asynchronous implementation
 * is available in {@link ASyncVMAdviceHandler}, which stores a representation of the
 * advice data in an internal data structure, and uses a separate thread to process the
 * data. In this case, the decision to log or not is made by the processing thread.
 * <p>
 * Logging to an instance of {@link com.oracle.max.vm.ext.vma.handlers.log.VMAdviceHandlerLog}
 * is actually factored out into{@link com.oracle.max.vm.ext.vma.handlers.log.VMAdviceHandlerLogAdapter},
 * so that it can be used by both the synchronous and asynchronous implementations.
 * <p>
 * The other component of the runtime that is necessary for persistent recording of the advice
 * data is {@link ObjectStateHandler}. This manages the assignment of unique identifiers to
 * objects and the notification of object death. The default implementation of this abstract class
 * is {@link com.oracle.max.vm.ext.vma.handlers.objstate.BitSetObjectStateHandler}, which uses a
 * {@link java.util.BitSet} to manage both unique ids and the liveness of objects.
 *
 */
package com.oracle.max.vm.ext.vma.runtime;
