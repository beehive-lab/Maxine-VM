/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.cir.snippet;

import com.sun.max.program.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.transform.*;

/**
 * Checks whether a method has been optimized such that it does not contain any calls except to builtins.
 *
 * This is for instance necessary for bootstrapping most snippets (which are essentially just methods).
 *
 * @author Bernd Mathiske
 */
public final class CirBuiltinCheck extends CirVisitor {

    private CirBuiltinCheck(Snippet snippet) {
        this.snippet = snippet;
    }

    private final Snippet snippet;

    public static void apply(CirClosure closure, Snippet snippet) {
        CirVisitingTraversal.apply(closure, new CirBuiltinCheck(snippet));
    }

    @Override
    public void visitMethod(CirMethod method) {
        ProgramError.unexpected(errorMessagePrefix() + "method found: " + method);
    }

    @Override
    public void visitBlock(CirBlock block) {
        ProgramError.unexpected(errorMessagePrefix() + "block found: " + block.traceToString(false) + "_" + block.id());
    }

    private String errorMessagePrefix() {
        return "checking for CIR reduction of " + snippet + " to mere builtins - ";
    }

}
