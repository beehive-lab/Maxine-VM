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
package com.oracle.max.vm.ext.maxri;

import com.oracle.max.cri.intrinsics.*;
import com.oracle.max.graal.nodes.*;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;

/**
 * Interface for intrinsic implementations targeting C1X. The intrinsic has access to the {@link RiRuntime} so that
 * it can append new instructions to the instruction stream.
 */
public interface GraalIntrinsicImpl extends IntrinsicImpl {
    /**
     * Creates the HIR instructions necessary for the implementation of the intrinsic and appends them using the
     * supplied {@link GraphBuilder} object.
     * @param runtime The RiRuntime, used to get information about types, etc.
     * @param caller The method that calls the intrinsified method.
     * @param target The intrinsic method, i.e., the method that has the {@link INTRINSIC} annotation.
     * @param graph the graph that the intrinsic will be created into
     * @param args The arguments of the intrinsic methods, to be used as the parameters of the intrinsic instruction.
     * @return The instruction that should be returned by the intrinsic, or null if no result should be returned.
     */
    ValueNode createHIR(RiRuntime runtime, StructuredGraph graph, RiResolvedMethod caller, RiResolvedMethod target, ValueNode[] args);
}
