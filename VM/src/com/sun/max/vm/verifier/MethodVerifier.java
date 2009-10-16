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

import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.verifier.types.*;

/**
 * An instance of {@code MethodVerifier} is created to verify a single method.
 *
 * @author David Liu
 * @author Doug Simon
 */
public abstract class MethodVerifier {

    private static final VMStringOption traceVerifierOption = VMOptions.register(new VMStringOption("-XX:TraceVerifier=", false, "",
        "Trace bytecode verification of methods whose qualified name contains <value>."), MaxineVM.Phase.STARTING);

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
        this.constantPool = codeAttribute.constantPool;
        this.classLoader = constantPool.classLoader();
        if (classVerifier.verbose) {
            this.verbose = true;
        } else {
            this.verbose = traceVerifierOption.isPresent() && classMethodActor.format("%H.%n").contains(traceVerifierOption.getValue());
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
    public ObjectType getObjectType(TypeDescriptor typeDescriptor) {
        if (JavaTypeDescriptor.isPrimitive(typeDescriptor)) {
            throw verifyError("Expected a non-primitive type");
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
     * Gets the position of the instruction currently being verified.
     */
    public abstract int currentOpcodePosition();

    public final int currentOpcodePositionOrMinusOne() {
        try {
            return currentOpcodePosition();
        } catch (RuntimeException e) {
            return -1;
        }
    }

    public VerifyError verifyError(String message) {
        final int currentOpcodePosition = currentOpcodePositionOrMinusOne();
        try {
            ErrorContext.enterContext("verifying " + classMethodActor.format("%H.%n(%p)") + (currentOpcodePosition == -1 ? "" : " at bytecode position " + currentOpcodePosition));
            throw ErrorContext.verifyError(message, classMethodActor, codeAttribute, currentOpcodePosition);
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
            toType.isAssignableFrom(fromType);
            throw verifyError(errorMessage + notAssignableMessage(fromType.toString(), toType.toString()));
        }
    }

    public static String notAssignableMessage(String fromType, String toType) {
        return " (" + fromType + " is not assignable to " + toType + ")";
    }
}
