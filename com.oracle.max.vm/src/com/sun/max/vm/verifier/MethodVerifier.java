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

import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.verifier.types.*;

/**
 * An instance of {@code MethodVerifier} is created to verify a single method.
 */
public abstract class MethodVerifier {

    private final ClassVerifier classVerifier;
    private final ClassLoader classLoader;
    private final ConstantPool constantPool;
    private final ClassMethodActor classMethodActor;
    private final CodeAttribute codeAttribute;
    public final boolean verbose;

    protected MethodVerifier(ClassVerifier classVerifier, ClassMethodActor classMethodActor, CodeAttribute codeAttribute) {
        this.classVerifier = classVerifier;
        this.codeAttribute = codeAttribute;
        this.classMethodActor = classMethodActor;
        this.constantPool = codeAttribute.cp;
        this.classLoader = constantPool.classLoader();
        if (classVerifier.verbose) {
            this.verbose = true;
        } else {
            this.verbose = Verifier.TraceVerification != null && classMethodActor.format("%H.%n").contains(Verifier.TraceVerification);
        }
    }

    public ClassVerifier classVerifier() {
        return classVerifier;
    }

    public ClassLoader classLoader() {
        return classLoader;
    }

    /**
     * @see ClassVerifier#getObjectType(TypeDescriptor)
     */
    public VerificationType getObjectType(TypeDescriptor typeDescriptor) {
        if (JavaTypeDescriptor.isPrimitive(typeDescriptor)) {
            verifyError("Expected a non-primitive type");
        }
        return classVerifier().getObjectType(typeDescriptor);
    }

    /**
     * @see ClassVerifier#getVerificationType(TypeDescriptor)
     */
    public VerificationType getVerificationType(TypeDescriptor typeDescriptor) {
        return classVerifier().getVerificationType(typeDescriptor);
    }

    public ConstantPool constantPool() {
        return constantPool;
    }

    public ClassActor classActor() {
        return classVerifier.classActor;
    }

    public MethodActor classMethodActor() {
        return classMethodActor;
    }

    public CodeAttribute codeAttribute() {
        return codeAttribute;
    }

    /**
     * Performs bytecode verification on a given method.
     */
    public abstract void verify();

    /**
     * Gets the BCI of the instruction currently being verified.
     */
    public abstract int currentOpcodeBCI();

    public final int currentOpcodeBCIOrMinusOne() {
        try {
            return currentOpcodeBCI();
        } catch (RuntimeException e) {
            return -1;
        }
    }

    public void verifyError(String message) {
        if (Verifier.noVerify.isPresent()) {
            return;
        }
        fatalVerifyError(message);
    }

    public VerifyError fatalVerifyError(String message) {
        final int currentOpcodeBCI = currentOpcodeBCIOrMinusOne();
        try {
            int sourceLine = currentOpcodeBCI == -1 ? -1 : classMethodActor.sourceLineNumber(currentOpcodeBCI);
            String sourceFile = classMethodActor.holder().sourceFileName;
            Object source = sourceLine == -1 || sourceFile == null ? "" : " (" + sourceFile + ":" + sourceLine + ")";
            ErrorContext.enterContext("verifying " + classMethodActor.format("%H.%n(%p)") + (currentOpcodeBCI == -1 ? "" : " at BCI " + currentOpcodeBCI) + source);
            throw ErrorContext.verifyError(message, classMethodActor, codeAttribute, currentOpcodeBCI);
        } finally {
            ErrorContext.exitContext();
        }
    }

    /**
     * Verifies that {@code fromType} is assignable to {@code toType}.
     *
     * @param errorMessage
     *            the message that will be included in the verification error thrown if the assignability test fails.
     *            Only String constants should be used for this parameter so that the cost of a string concatenation
     *            expression is not incurred when the assignability test succeeds.
     */
    public void verifyIsAssignable(VerificationType fromType, VerificationType toType, String errorMessage) {
        if (!toType.isAssignableFrom(fromType)) {
            if (!VerificationType.isTypeIncompatibilityBetweenPointerAndAccessor(fromType, toType)) {
                verifyError(errorMessage + notAssignableMessage(fromType.toString(), toType.toString()));
            }
        }
    }

    public static String notAssignableMessage(String fromType, String toType) {
        return " (" + fromType + " is not assignable to " + toType + ")";
    }
}
