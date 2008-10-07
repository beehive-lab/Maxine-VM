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
package com.sun.max.vm.verifier;

import java.io.*;

import com.sun.max.profile.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;

/**
 * Adapted from JProcessor code.
 *
 * @author David Liu
 * @author Doug Simon
 */
public class TypeInferencingVerifier extends ClassVerifier {

    public TypeInferencingVerifier(ClassActor classActor) {
        super(classActor);
        //setVerbose(true);
    }

    @Override
    public CodeAttribute verify(ClassMethodActor classMethodActor, CodeAttribute originalCodeAttribute) {
        Metrics.increment("TypeInferencingVerifications");

        // The methods in class files whose version is greater than or equal to 50.0 are required to
        // have stack maps. This method is mostly like being verified with the type inferencing verifier
        // to update the stack maps after bytecode preprocessing.
        final boolean addStackMapAttribute = classMethodActor.holder().majorVersion() >= 50;
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
            final SubroutineInliner inliner = new SubroutineInliner(methodVerifier, verbose());
            codeAttribute = inliner.rewriteCode();
        }

        if (addStackMapAttribute) {
            addStackMapAttribute(methodVerifier);
        }

        // Trace only changes (if any)
        if (verbose() && (addStackMapAttribute || codeAttribute != originalCodeAttribute)) {
            final PrintStream out = Trace.stream();
            out.println();
            final String methodSignature = classMethodActor.format("%H.%n(%p)");
            out.println("Before rewriting " + methodSignature);
            CodeAttributePrinter.print(out, originalCodeAttribute);
            out.println();
            out.println("After rewriting " + methodSignature);
            CodeAttributePrinter.print(out, codeAttribute);
        }

        if (MaxineVM.isPrototyping() && addStackMapAttribute) {
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
