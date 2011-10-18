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
package com.sun.c1x.intrinsics;

import com.oracle.max.cri.intrinsics.*;
import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.value.*;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;

/**
 * Interface for intrinsic implementations targeting C1X. The intrinisc has access to the {@link GraphBuilder} so that
 * it can append new instructions to the instruction stream.
 */
public interface C1XIntrinsicImpl extends IntrinsicImpl {
    /**
     * Creates the HIR instructions necessary for the implementation of the intrinsic and appends them using the
     * supplied {@link GraphBuilder} object.
     * @param b The {@link GraphBuilder} for appending instructions and for accessing general information about the compilation.
     * @param target The intrinsic method, i.e., the method that has the {@link INTRINSIC} annotation.
     * @param args The arguments of the intrinsic methods, to be used as the parameters of the intrinsic instruction.
     * @param isStatic True if it is a static method call for the intrinsic.
     * @param stateBefore Frame state of the intrinisc call site.
     * @return The instruction that should be pushed on the operand stack, or null if no result should be pushed.
     */
    Value createHIR(GraphBuilder b, RiMethod target, Value[] args, boolean isStatic, FrameState stateBefore);
}
