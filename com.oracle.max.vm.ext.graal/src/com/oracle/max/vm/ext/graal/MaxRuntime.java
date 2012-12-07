/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.graal;
import java.lang.reflect.*;
import java.util.*;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.code.RuntimeCall.Descriptor;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;
import com.sun.max.program.*;



public class MaxRuntime implements GraalCodeCacheProvider {

    @Override
    public InstalledCode addMethod(ResolvedJavaMethod method, CompilationResult compResult, CodeInfo[] info) {
        unimplemented();
        return null;
    }

    @Override
    public int getSizeOfLockData() {
        unimplemented();
        return 0;
    }

    @Override
    public String disassemble(CodeInfo code, CompilationResult tm) {
        unimplemented();
        return null;
    }

    @Override
    public RegisterConfig lookupRegisterConfig(JavaMethod method) {
        unimplemented();
        return null;
    }

    @Override
    public int getCustomStackAreaSize() {
        unimplemented();
        return 0;
    }

    @Override
    public int getMinimumOutgoingSize() {
        unimplemented();
        return 0;
    }

    @Override
    public Object lookupCallTarget(Object target) {
        unimplemented();
        return null;
    }

    @Override
    public RuntimeCall lookupRuntimeCall(Descriptor descriptor) {
        unimplemented();
        return null;
    }

    @Override
    public int encodeDeoptActionAndReason(DeoptimizationAction action, DeoptimizationReason reason) {
        unimplemented();
        return 0;
    }

    @Override
    public boolean needsDataPatch(Constant constant) {
        unimplemented();
        return false;
    }

    @Override
    public ResolvedJavaType lookupJavaType(Class< ? > clazz) {
        unimplemented();
        return null;
    }

    @Override
    public ResolvedJavaMethod lookupJavaMethod(Method reflectionMethod) {
        unimplemented();
        return null;
    }

    @Override
    public ResolvedJavaField lookupJavaField(Field reflectionField) {
        unimplemented();
        return null;
    }

    @Override
    public ResolvedJavaType lookupJavaType(Constant constant) {
        unimplemented();
        return null;
    }

    @Override
    public boolean constantEquals(Constant x, Constant y) {
        unimplemented();
        return false;
    }

    @Override
    public int lookupArrayLength(Constant array) {
        unimplemented();
        return 0;
    }

    @Override
    public void lower(Node n, LoweringTool tool) {
        unimplemented();

    }

    @Override
    public StructuredGraph intrinsicGraph(ResolvedJavaMethod caller, int bci, ResolvedJavaMethod method, List< ? extends Node> parameters) {
        unimplemented();
        return null;
    }

    private static void unimplemented() {
        ProgramError.unexpected("unimplemented");
    }

    @Override
    public ResolvedJavaMethod lookupJavaConstructor(Constructor reflectionConstructor) {
        unimplemented();
        return null;
    }

}
