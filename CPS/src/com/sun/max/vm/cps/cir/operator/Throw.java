/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.max.vm.cps.cir.operator;

import com.sun.max.program.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.snippet.*;
import com.sun.max.vm.type.*;

public class Throw extends JavaOperator implements Lowerable{

    public Throw() {
        super(CALL_STOP);
    }

    public Kind resultKind() {
        ProgramError.unexpected();
        return null;
    }

    public void toLCir(Lowerable op, CirCall call, CPSCompiler compilerScheme) {
        final CirValue[] args = call.arguments();
        assert args[args.length - 2] == CirValue.UNDEFINED;
        assert call.procedure() == this;
        call.setProcedure(CirSnippet.get(Snippet.RaiseThrowable.SNIPPET));
    }

    private static final Kind[] parameterKinds = {Kind.REFERENCE};

    @Override
    public Kind[] parameterKinds() {
        return parameterKinds;
    }

    @Override
    public String toString() {
        return "Throw";
    }
}
