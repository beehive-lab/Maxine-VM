/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.verifier;

import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;

/**
 * Adapted from JProcessor code.
 */
public class TypeInferencingVerifier extends ClassVerifier {

    public TypeInferencingVerifier(ClassActor classActor) {
        super(classActor);
        //setVerbose(true);
    }

    @Override
    public CodeAttribute verify(ClassMethodActor classMethodActor, CodeAttribute originalCodeAttribute) {

        // The methods in class files whose version is greater than or equal to 50.0 are required to
        // have stack maps. This method is mostly like being verified with the type inferencing verifier
        // to update the stack maps after bytecode preprocessing.
        final boolean addStackMapAttribute = classMethodActor.holder().majorVersion >= 50;
        return verify(classMethodActor, originalCodeAttribute, addStackMapAttribute);
    }

    public CodeAttribute verify(ClassMethodActor classMethodActor, CodeAttribute originalCodeAttribute, boolean addStackMapAttribute) {
        assert originalCodeAttribute != null;
        TypeInferencingMethodVerifier methodVerifier;
        CodeAttribute codeAttribute = originalCodeAttribute;
        while (true) {
            methodVerifier = new TypeInferencingMethodVerifier(this, classMethodActor, codeAttribute);
            methodVerifier.verify();
            if (clearSubroutines() == 0 && !methodVerifier.hasUnvisitedCode()) {
                break;
            }
            final SubroutineInliner inliner = new SubroutineInliner(methodVerifier, methodVerifier.verbose);
            codeAttribute = inliner.rewriteCode();
        }

        if (addStackMapAttribute) {
            addStackMapAttribute(methodVerifier);
        }

        // Trace only changes (if any)
        if (methodVerifier.verbose && (addStackMapAttribute || codeAttribute != originalCodeAttribute)) {
            Log.println();
            final String methodSignature = classMethodActor.format("%H.%n(%p)");
            Log.println("Before rewriting " + methodSignature);
            CodeAttributePrinter.print(Log.out, originalCodeAttribute);
            Log.println();
            Log.println("After rewriting " + methodSignature);
            CodeAttributePrinter.print(Log.out, codeAttribute);
        }

        if (MaxineVM.isHosted() && addStackMapAttribute) {
            final TypeCheckingMethodVerifier typeCheckingMethodVerifier = new TypeCheckingMethodVerifier(this, classMethodActor, codeAttribute);
            typeCheckingMethodVerifier.verify();
        }
        return codeAttribute;
    }

    private void addStackMapAttribute(final TypeInferencingMethodVerifier methodVerifier) {
        constantPool().edit(new ConstantPoolEditorClient() {
            public void edit(ConstantPoolEditor constantPoolEditor) {
                final StackMapTable stackMapTable = methodVerifier.generateStackMapTable(constantPoolEditor);
                methodVerifier.codeAttribute().setStackMapTableAttribute(stackMapTable);
            }
        });
    }
}
