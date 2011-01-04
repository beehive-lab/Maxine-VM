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
package com.sun.max.vm.cps.b.c.d.e;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.config.*;
import com.sun.max.lang.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.cps.b.c.d.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.ir.*;

/**
 * @author Bernd Mathiske
 */
public abstract class BcdeCompiler<EirGenerator_Type extends EirGenerator> extends BcdCompiler implements EirGeneratorScheme<EirGenerator_Type> {

    @HOSTED_ONLY
    public BcdeCompiler() {
    }

    @Override
    public IrGenerator irGenerator() {
        return eirGenerator();
    }

    @Override
    public List<IrGenerator> irGenerators() {
        final List<IrGenerator> result = new LinkedList<IrGenerator>(super.irGenerators());
        result.add(eirGenerator());
        return result;
    }

    private boolean[] isBuiltinImplemented;

    @Override
    public boolean isBuiltinImplemented(Builtin builtin) {
        return isBuiltinImplemented[builtin.serial()];
    }

    protected abstract Class<? extends BuiltinVisitor> builtinTranslationClass();

    @HOSTED_ONLY
    @Override
    public void createBuiltins(PackageLoader packageLoader) {
        super.createBuiltins(packageLoader);
        Method[] declaredMethods = builtinTranslationClass().getDeclaredMethods();
        final Set<String> methodNames = new HashSet<String>();
        for (Method method : declaredMethods) {
            methodNames.add(method.getName());
        }

        isBuiltinImplemented = new boolean[Builtin.builtins().size()];
        for (int i = 0; i < Builtin.builtins().size(); i++) {
            final Builtin builtin = Builtin.builtins().get(i);
            isBuiltinImplemented[i] = methodNames.contains("visit" + Strings.firstCharToUpperCase(builtin.name));
        }
    }
}
