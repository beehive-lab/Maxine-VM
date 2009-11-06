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

import static com.sun.max.vm.verifier.types.VerificationType.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.verifier.types.*;

/**
 * Encapsulates the contexts an options for bytecode verification.
 *
 * @author Doug Simon
 */
public class Verifier implements VerificationRegistry {

    static final int TRACE_NONE = 0;
    static final int TRACE_CLASS = 1;
    static final int TRACE_METHOD = 2;

    /**
     * The level of bytecode verification tracing.
     */
    static int traceLevel;

    /**
     * If non-null, then the verification of any methods whose fully qualified name contains this field value as
     * a substring is traced in detail.
     */
    static String methodToTrace;

    /**
     * Determines if verification is performed for classes loaded locally.
     */
    @RESET
    private static boolean verifyLocal;

    /**
     * Determines if verification is performed for classes loaded over network.
     */
    private static boolean verifyRemote = true;

    static {
        // -XX:-BytecodeVerificationLocal option
        VMOptions.register(new VMBooleanXXOption("-XX:-BytecodeVerificationLocal",
            "Enable verification of local classes.") {
            @Override
            public boolean parseValue(Pointer optionValue) {
                if (super.parseValue(optionValue)) {
                    verifyLocal = getValue();
                    return true;
                }
                return false;
            }
        }, MaxineVM.Phase.STARTING);

        // -XX:+BytecodeVerificationRemote option
        VMOptions.register(new VMBooleanXXOption("-XX:+BytecodeVerificationRemote",
            "Enable verification of remote classes.") {
            @Override
            public boolean parseValue(Pointer optionValue) {
                if (super.parseValue(optionValue)) {
                    verifyRemote = getValue();
                    return true;
                }
                return false;
            }

        }, MaxineVM.Phase.STARTING);

        // -Xverify option
        VMOptions.register(new VMOption("-Xverify",
            "Enable verification process on classes loaded over network (default), all classes, or no classes respectively.") {
            @Override
            public boolean parseValue(Pointer optionValue) {
                if (CString.equals(optionValue, ":all") || CString.length(optionValue).isZero()) {
                    verifyLocal = true;
                    verifyRemote = true;
                } else if (CString.equals(optionValue, ":none")) {
                    verifyLocal = false;
                    verifyRemote = false;
                } else if (CString.equals(optionValue, ":remote")) {
                    verifyLocal = false;
                    verifyRemote = true;
                } else {
                    return false;
                }
                return true;
            }
            @Override
            public void printHelp() {
                VMOptions.printHelpForOption("-Xverify[:remote|all|none]", "", help);
            }

        }, MaxineVM.Phase.STARTING);

        // -XX:TraceVerification=<value> option
        VMOptions.register(new VMStringOption("-XX:TraceVerification=", false, null,
            "Trace bytecode verification in detail of method(s) whose qualified name contains <value>.") {
            @Override
            public boolean parseValue(Pointer optionValue) {
                if (super.parseValue(optionValue)) {
                    methodToTrace = getValue();
                    return true;
                }
                return false;
            }
        }, MaxineVM.Phase.STARTING);

        // -XX:TraceVerifierLevel=<value> option
        VMOptions.register(new VMIntOption("-XX:TraceVerifierLevel=", 0,
            "Trace bytecode verification level: 0 == none, 1 == class, 2 == methods.") {
            @Override
            public boolean parseValue(Pointer optionValue) {
                boolean result = super.parseValue(optionValue);
                if (result) {
                    traceLevel = getValue();
                }
                return result;
            }
        }, MaxineVM.Phase.STARTING);
    }

    /**
     * Determines if a class loaded by a given class loader needs bytecode verification.
     * The answer depends upon {@code classLoader} <i>and</i> the values of
     * {@link #verifyLocal} and {@link #verifyRemote}.
     *
     * @param classLoader the class loader to test
     * @param isRemote specifies if the stream is from a remote/untrusted (e.g. network) source. This is mainly used to
     *            determine the default bytecode verification policy for the class.
     * @return {@code true} if a class loaded by {@code classLoader} need bytecode verification
     */
    public static boolean shouldBeVerified(ClassLoader classLoader, boolean isRemote) {
        if (classLoader == BootClassLoader.BOOT_CLASS_LOADER || classLoader == null || !isRemote) {
            return verifyLocal;
        }
        return verifyRemote;
    }

    public static boolean relaxVerificationFor(ClassLoader classLoader) {
        boolean trusted = isTrustedLoader(classLoader);
        boolean needVerify =
            // -Xverify:all
            (verifyLocal && verifyRemote) ||
            // -Xverify:remote
            (!verifyLocal && verifyRemote && !trusted);
        return !needVerify;
    }

    private static boolean isTrustedLoader(ClassLoader classLoader) {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        while (cl != null) {
            if (cl == classLoader) {
                return true;
            }
            cl = cl.getParent();
        }
        return false;
    }

    private final ConstantPool constantPool;
    private final Map<TypeDescriptor, ObjectType> objectTypes;
    private final IntHashMap<UninitializedNewType> uninitializedNewTypes;
    private IntHashMap<Subroutine> subroutines;
    public boolean verbose;

    public Verifier(ConstantPool constantPool) {
        this.constantPool = constantPool;
        this.objectTypes = new HashMap<TypeDescriptor, ObjectType>();
        this.uninitializedNewTypes = new IntHashMap<UninitializedNewType>();

        for (ObjectType objectType : PREDEFINED_OBJECT_TYPES) {
            objectTypes.put(objectType.typeDescriptor(), objectType);
        }
    }

    public ObjectType getObjectType(TypeDescriptor typeDescriptor) {
        if (JavaTypeDescriptor.isPrimitive(typeDescriptor)) {
            return null;
        }
        ObjectType objectType = objectTypes.get(typeDescriptor);
        if (objectType == null) {
            objectType = JavaTypeDescriptor.isArray(typeDescriptor) ? new ArrayType(typeDescriptor, this) : new ObjectType(typeDescriptor, this);
            objectTypes.put(typeDescriptor, objectType);
        }
        return objectType;
    }

    public UninitializedNewType getUninitializedNewType(int position) {
        UninitializedNewType uninitializedNewType = uninitializedNewTypes.get(position);
        if (uninitializedNewType == null) {
            uninitializedNewType = new UninitializedNewType(position);
            uninitializedNewTypes.put(position, uninitializedNewType);
        }
        return uninitializedNewType;
    }

    public int clearSubroutines() {
        if (subroutines != null) {
            final int count = subroutines.count();
            subroutines = null;
            return count;
        }
        return 0;
    }

    public Subroutine getSubroutine(int entryPosition, int maxLocals) {
        if (subroutines == null) {
            subroutines = new IntHashMap<Subroutine>();
        }
        Subroutine subroutine = subroutines.get(entryPosition);
        if (subroutine == null) {
            subroutine = new Subroutine(entryPosition, maxLocals);
            subroutines.put(entryPosition, subroutine);
        }
        return subroutine;
    }

    public VerificationType getVerificationType(TypeDescriptor typeDescriptor) {
        return VerificationType.getVerificationType(typeDescriptor, this);
    }

    public ConstantPool constantPool() {
        return constantPool;
    }

    public static ClassVerifier verifierFor(ClassActor classActor) {
        final int majorVersion = classActor.majorVersion;
        if (majorVersion >= 50) {
            return new TypeCheckingVerifier(classActor);
        }
        return new TypeInferencingVerifier(classActor);
    }

    /**
     * Resolves a given TypeDescriptor to a class actor.
     */
    public ClassActor resolve(TypeDescriptor type) {
        return ClassActor.fromJava(type.resolveType(constantPool().classLoader()));
    }
}
