/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.cps.b.c.d.e;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.*;
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
