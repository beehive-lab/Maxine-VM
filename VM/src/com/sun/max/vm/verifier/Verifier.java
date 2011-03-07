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
    public static int TraceVerifierLevel;
    static {
        VMOptions.addFieldOption("-XX:", "TraceVerifierLevel", "Trace bytecode verification level: 0 = none, 1 = class, 2 = methods.");
    }

    /**
     * If non-null, then the verification of any methods whose fully qualified name contains this field value as
     * a substring is traced in detail.
     */
    public static String TraceVerification;
    static {
        VMOptions.addFieldOption("-XX:", "TraceVerification",
            "Trace bytecode verification in detail of method(s) whose qualified name contains <value>.");
    }

    /**
     * Determines if verification is performed for classes loaded locally.
     */
    @RESET
    private static boolean BytecodeVerificationLocal;
    static {
        VMOptions.addFieldOption("-XX:", "BytecodeVerificationLocal", "Enable verification of local classes.");
    }

    /**
     * Determines if verification is performed for classes loaded over network.
     */
    private static boolean BytecodeVerificationRemote = true;
    static {
        VMOptions.addFieldOption("-XX:", "BytecodeVerificationRemote", "Enable verification of remote classes.");
    }

    static {
        // -Xverify option
        VMOptions.register(new VMOption("-Xverify",
            "Enable verification process on classes loaded over network (default), all classes, or no classes respectively.") {
            @Override
            public boolean parseValue(Pointer optionValue) {
                if (CString.equals(optionValue, ":all") || CString.length(optionValue).isZero()) {
                    BytecodeVerificationLocal = true;
                    BytecodeVerificationRemote = true;
                } else if (CString.equals(optionValue, ":none")) {
                    BytecodeVerificationLocal = false;
                    BytecodeVerificationRemote = false;
                } else if (CString.equals(optionValue, ":remote")) {
                    BytecodeVerificationLocal = false;
                    BytecodeVerificationRemote = true;
                } else {
                    return false;
                }
                return true;
            }
            @Override
            public void printHelp() {
                VMOptions.printHelpForOption(category(), "-Xverify[:remote|all|none]", "", help);
            }

        }, MaxineVM.Phase.STARTING);

    }

    /**
     * Hotspot supports this undocumented option. Maxine must do some verification because it must process class files
     * whose version is older than 50.0 to remove jsr/ret constriucts, which it does by using the verifier. So this
     * option simply turns of the error reporting and lets the verifier continue.
     */
    static VMOption noVerify = new VMOption("-noverify ", "suppress all verification");
    static {
        VMOptions.register(noVerify, MaxineVM.Phase.PRISTINE);
    }

    /**
     * Determines if a class loaded by a given class loader needs bytecode verification.
     * The answer depends upon {@code classLoader} <i>and</i> the values of
     * {@link #BytecodeVerificationLocal} and {@link #BytecodeVerificationRemote}.
     *
     * @param classLoader the class loader to test
     * @param isRemote specifies if the stream is from a remote/untrusted (e.g. network) source. This is mainly used to
     *            determine the default bytecode verification policy for the class.
     * @return {@code true} if a class loaded by {@code classLoader} need bytecode verification
     */
    public static boolean shouldBeVerified(ClassLoader classLoader, boolean isRemote) {
        if (classLoader == BootClassLoader.BOOT_CLASS_LOADER || classLoader == null || !isRemote) {
            return BytecodeVerificationLocal;
        }
        return BytecodeVerificationRemote;
    }

    public static boolean relaxVerificationFor(ClassLoader classLoader) {
        boolean trusted = isTrustedLoader(classLoader);
        boolean needVerify =
            // -Xverify:all
            (BytecodeVerificationLocal && BytecodeVerificationRemote) ||
            // -Xverify:remote
            (!BytecodeVerificationLocal && BytecodeVerificationRemote && !trusted);
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

    public VerificationType getObjectType(TypeDescriptor typeDescriptor) {
        if (JavaTypeDescriptor.isPrimitive(typeDescriptor)) {
            return null;
        }
        if (typeDescriptor.toKind().isWord) {
            return VerificationType.WORD;
        }
        ObjectType objectType = objectTypes.get(typeDescriptor);
        if (objectType == null) {
            objectType = JavaTypeDescriptor.isArray(typeDescriptor) ? new ArrayType(typeDescriptor, this) : new ObjectType(typeDescriptor, this);
            objectTypes.put(typeDescriptor, objectType);
        }
        return objectType;
    }

    public UninitializedNewType getUninitializedNewType(int bci) {
        UninitializedNewType uninitializedNewType = uninitializedNewTypes.get(bci);
        if (uninitializedNewType == null) {
            uninitializedNewType = new UninitializedNewType(bci);
            uninitializedNewTypes.put(bci, uninitializedNewType);
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

    public Subroutine getSubroutine(int entryBCI, int maxLocals) {
        if (subroutines == null) {
            subroutines = new IntHashMap<Subroutine>();
        }
        Subroutine subroutine = subroutines.get(entryBCI);
        if (subroutine == null) {
            subroutine = new Subroutine(entryBCI, maxLocals);
            subroutines.put(entryBCI, subroutine);
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
