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
package test.com.sun.max.vm.run;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.run.java.*;


/**
 * Shared support for testing run schemes.
 */
public abstract class AbstractTestRunScheme extends JavaRunScheme {

    private final Utf8Constant testMethod;
    protected static boolean noTests;

    protected AbstractTestRunScheme(String testMethod) {
        this.testMethod = SymbolTable.makeSymbol(testMethod);
    }

    private static final boolean COMPILE_ALL_TEST_METHODS = true;

    @HOSTED_ONLY
    public void addClassToImage(Class<?> javaClass) {
        final ClassActor actor = ClassActor.fromJava(javaClass);
        if (actor == null) {
            return;
        }

        if (COMPILE_ALL_TEST_METHODS) {
            // add all virtual and static methods to the image
            addMethods(actor.localStaticMethodActors());
            addMethods(actor.localVirtualMethodActors());
        } else {
            // add only the test method to the image
            final StaticMethodActor method = actor.findLocalStaticMethodActor(testMethod);
            if (method != null) {
                addMethodToImage(method);
            }
        }
        for (Class<?> declaredClass : javaClass.getDeclaredClasses()) {
            // load all inner and anonymous classes into the image as well
            addClassToImage(declaredClass);
        }
    }

    @HOSTED_ONLY
    private void addMethods(ClassMethodActor[] methodActors) {
        if (methodActors != null) {
            for (ClassMethodActor method : methodActors) {
                addMethodToImage(method);
            }
        }
    }

    @HOSTED_ONLY
    private void addMethodToImage(ClassMethodActor method) {
        CompiledPrototype.registerVMEntryPoint(method);
    }

    @Override
    protected boolean parseMain() {
        return noTests;
    }


}
