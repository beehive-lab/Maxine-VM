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
package com.sun.max.vm.compiler.b.c.d.e;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.*;
import com.sun.max.collect.*;
import com.sun.max.lang.Arrays;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.b.c.d.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.ir.*;

/**
 * @author Bernd Mathiske
 */
public abstract class BcdeCompiler<EirGenerator_Type extends EirGenerator> extends BcdCompiler implements EirGeneratorScheme<EirGenerator_Type> {

    public BcdeCompiler(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    @Override
    public IrGenerator irGenerator() {
        return eirGenerator();
    }

    @Override
    public Sequence<IrGenerator> irGenerators() {
        return Sequence.Static.appended(super.irGenerators(), eirGenerator());
    }

    private boolean[] _isBuiltinImplemented;

    @Override
    public boolean isBuiltinImplemented(Builtin builtin) {
        return _isBuiltinImplemented[builtin.serial()];
    }

    protected abstract Class<? extends BuiltinVisitor> builtinTranslationClass();

    @Override
    public void createBuiltins(PackageLoader packageLoader) {
        super.createBuiltins(packageLoader);

        final Set<String> methodNames = Sets.from(Arrays.map(builtinTranslationClass().getDeclaredMethods(), String.class,
            new MapFunction<Method, String>() {
                public String map(Method method) {
                    return method.getName();
                }
            }));

        _isBuiltinImplemented = new boolean[Builtin.builtins().length()];
        for (int i = 0; i < Builtin.builtins().length(); i++) {
            final Builtin builtin = Builtin.builtins().get(i);
            _isBuiltinImplemented[i] = methodNames.contains("visit" + Naming.toClassName(builtin.name()));
        }
    }

    @Override
    public void createSnippets(PackageLoader packageLoader) {
        super.createSnippets(packageLoader);
    }
}
