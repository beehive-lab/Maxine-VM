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
package com.sun.max.vm.actor.member;

import static com.sun.cri.bytecode.Bytecodes.*;
import static com.sun.max.vm.actor.member.LivenessAdapter.*;

import java.lang.reflect.*;

import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.bytecode.graft.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.verifier.*;
import com.sun.org.apache.bcel.internal.generic.*;

/**
 * Non-interface methods.
 *
 * @author Bernd Mathiske
 * @author Hiroshi Yamauchi
 * @author Doug Simon
 */
public abstract class ClassMethodActor extends MethodActor {

    @RESET
    public static boolean TraceJNI;
    static {
        VMOptions.addFieldOption("-XX:", "TraceJNI", "Trace JNI calls.");
    }

    @INSPECTED
    private CodeAttribute codeAttribute;

    @INSPECTED
    public volatile Object targetState;

    private CodeAttribute originalCodeAttribute;

    /**
     * This is the method whose code is actually compiled/executed. In most cases, it will be
     * equal to this object, unless this method has a {@linkplain SUBSTITUTE substitute}.
     *
     * This field is declared volatile so that the double-checking locking idiom used in
     * {@link #compilee()} works as expected. This correctness is guaranteed as long as the
     * compiler follows all the rules of the Java Memory Model as of JDK5 (JSR-133).
     */
    private volatile ClassMethodActor compilee;

    /**
     * The object representing the linkage of this native method actor to a native machine code address.
     * This value is {@code null} if this method is not {@linkplain #isNative() native}
     */
    public final NativeFunction nativeFunction;

    private RiExceptionHandler[] exceptionHandlers;

    public ClassMethodActor(Utf8Constant name, SignatureDescriptor descriptor, int flags, CodeAttribute codeAttribute, int intrinsic) {
        super(name, descriptor, flags, intrinsic);
        this.originalCodeAttribute = codeAttribute;
        this.nativeFunction = isNative() ? new NativeFunction(this) : null;

        if (MaxineVM.isHosted() && codeAttribute != null) {
            ClassfileWriter.classfileCodeAttributeMap.put(this, codeAttribute);
            ClassfileWriter.classfileCodeMap.put(this, codeAttribute.code().clone());
        }
    }

    /**
     * @return number of locals used by the method parameters (including the receiver if this method isn't static).
     */
    public int numberOfParameterSlots() {
        return descriptor().computeNumberOfSlots() + ((isStatic()) ? 0 : 1);
    }

    public boolean isDeclaredNeverInline() {
        return compilee().isNeverInline();
    }

    @INLINE
    public final boolean noSafepoints() {
        return noSafepoints(flags());
    }

    @INLINE
    public final boolean isDeclaredFoldable() {
        return isDeclaredFoldable(flags());
    }

    /**
     * Gets the {@code CodeAttribute} with which this method actor was originally constructed.
     *
     * This is basically a hack to ensure that C1X can obtain code that hasn't been subject to
     * the {@linkplain Preprocessor preprocessing} required by the CPS and template JIT compilers.
     * @param intrinsify specifies if intrinsifaction should be performed
     */
    public final CodeAttribute originalCodeAttribute(boolean intrinsify) {
        if (isNative()) {
            // C1X must compile the generated JNI stub
            return codeAttribute();
        }

        if (intrinsify) {
            // This call ensures that intrinsification is performed on the bytecode array in
            // 'originalCodeAttribute' before it is returned.
            compilee();
        }

        return originalCodeAttribute;
    }

    @Override
    public final byte[] code() {
        CodeAttribute codeAttribute = originalCodeAttribute(true);
        if (codeAttribute != null) {
            return codeAttribute.code();
        }
        return null;
    }

    private CiBitMap[] livenessMap;

    @Override
    public final CiBitMap[] livenessMap() {
        if (livenessMap != null) {
            return livenessMap == NO_LIVENESS_MAP ? null : livenessMap;
        }
        livenessMap = new LivenessAdapter(this).livenessMap;
        if (livenessMap.length == 0) {
            assert livenessMap == NO_LIVENESS_MAP;
            return null;
        }
        return livenessMap;
    }

    @Override
    public RiExceptionHandler[] exceptionHandlers() {
        if (exceptionHandlers != null) {
            // return the cached exception handlers
            return exceptionHandlers;
        }

        ExceptionHandlerEntry[] exceptionHandlerTable = ExceptionHandlerEntry.NONE;
        CodeAttribute codeAttribute = originalCodeAttribute(true);
        if (codeAttribute != null) {
            exceptionHandlerTable = codeAttribute.exceptionHandlerTable();
        }
        if (exceptionHandlerTable.length == 0) {
            exceptionHandlers = RiExceptionHandler.NONE;
        } else {
            exceptionHandlers = new RiExceptionHandler[exceptionHandlerTable.length];
            int i = 0;
            for (ExceptionHandlerEntry entry : exceptionHandlerTable) {
                RiType catchType;
                int catchTypeIndex = entry.catchTypeIndex();
                if (catchTypeIndex == 0) {
                    catchType = null;
                } else {
                    ConstantPool pool = codeAttribute.constantPool;
                    catchType = pool.classAt(catchTypeIndex).resolve(pool, catchTypeIndex);
                }
                exceptionHandlers[i++] = new CiExceptionHandler(
                                (char) entry.startPosition(),
                                (char) entry.endPosition(),
                                (char) entry.handlerPosition(),
                                (char) catchTypeIndex, catchType);
            }
        }
        return exceptionHandlers;
    }

    @Override
    public String jniSymbol() {
        if (nativeFunction != null) {
            return nativeFunction.makeSymbol();
        }
        return null;
    }

    @Override
    public int maxLocals() {
        CodeAttribute codeAttribute = originalCodeAttribute(true);
        if (codeAttribute != null) {
            return codeAttribute.maxLocals;
        }
        return 0;
    }

    @Override
    public int maxStackSize() {
        CodeAttribute codeAttribute = originalCodeAttribute(true);
        if (codeAttribute != null) {
            return codeAttribute.maxStack;
        }
        return 0;
    }

    /**
     * Gets the bytecode that is to be compiled and/or executed for this actor.
     * @return the code attribute
     */
    public final CodeAttribute codeAttribute() {
        // Ensure that any prerequisite substitution and/or preprocessing of the code to be
        // compiled/executed is performed first
        compilee();

        return codeAttribute;
    }

    /**
     * Allows bytecode verification and {@linkplain #validateInlineAnnotation(ClassMethodActor) validation} of
     * inlining annotations to be disabled by a hosted process.
     */
    @HOSTED_ONLY
    public static boolean hostedVerificationDisabled;

    /**
     * @return the actor for the method that will be compiled and/or executed in lieu of this method
     */
    public final ClassMethodActor compilee() {
        if (this.compilee == null) {
            synchronized (this) {
                if (compilee != null) {
                    return compilee;
                }

                if (!isMiranda()) {
                    final boolean isConstructor = isInitializer();
                    final ClassMethodActor substitute = METHOD_SUBSTITUTIONS.Static.findSubstituteFor(this);
                    if (substitute != null) {
                        compilee = substitute.compilee();
                        codeAttribute = compilee.codeAttribute;
                        originalCodeAttribute = compilee.originalCodeAttribute;
                        return compilee;
                    }
                    if (MaxineVM.isHosted() && !hostedVerificationDisabled && !isConstructor) {
                        validateInlineAnnotation(this);
                    }
                }

                ClassMethodActor compilee = this;
                CodeAttribute codeAttribute = originalCodeAttribute;
                ClassVerifier verifier = null;

                final CodeAttribute processedCodeAttribute = Preprocessor.apply(compilee, codeAttribute);
                final boolean modified = processedCodeAttribute != codeAttribute;
                codeAttribute = processedCodeAttribute;

                final ClassActor holder = compilee.holder();
                if (MaxineVM.isHosted()) {
                    if (!hostedVerificationDisabled) {
                        // We simply verify all methods during boot image build time as the overhead should be acceptable.
                        verifier = modified ? new TypeInferencingVerifier(holder) : Verifier.verifierFor(holder);
                    }
                } else {
                    if (holder().majorVersion < 50) {
                        // The compiler/JIT/interpreter cannot handle JSR or RET instructions. However, these instructions
                        // can legally appear in class files whose version number is less than 50.0. So, we inline them
                        // with the type inferencing verifier if they appear in the bytecode of a pre-version-50.0 class file.
                        if (codeAttribute != null && containsSubroutines(codeAttribute.code())) {
                            verifier = new TypeInferencingVerifier(holder);
                        }
                    } else {
                        if (modified) {
                            // The methods in class files whose version is greater than or equal to 50.0 are required to
                            // have stack maps. If the bytecode of such a method has been preprocessed, then its
                            // pre-existing stack maps will have been invalidated and must be regenerated with the
                            // type inferencing verifier
                            verifier = new TypeInferencingVerifier(holder);
                        }
                    }
                }

                if (verifier != null && codeAttribute != null && !compilee.holder().isReflectionStub()) {
                    if (MaxineVM.isHosted()) {
                        try {
                            codeAttribute = verifier.verify(compilee, codeAttribute);
                        } catch (HostOnlyClassError e) {
                        } catch (HostOnlyMethodError e) {
                        } catch (OmittedClassError e) {
                            // Ignore: assume all classes being loaded during boot imaging are verifiable.
                        }
                    } else {
                        codeAttribute = verifier.verify(compilee, codeAttribute);
                    }
                }

                intrinsify(compilee, codeAttribute);
                if (codeAttribute != originalCodeAttribute && originalCodeAttribute != null) {
                    // C1X must also see the intrinsified code
                    intrinsify(compilee, originalCodeAttribute);
                }

                this.codeAttribute = codeAttribute;
                this.compilee = compilee;
            }
        }
        return compilee;
    }

    private boolean intrinsify(ClassMethodActor compilee, CodeAttribute codeAttribute) {
        if (codeAttribute != null) {
            Intrinsics intrinsics = new Intrinsics(compilee, codeAttribute);
            intrinsics.run();
            if (intrinsics.unsafe) {
                compilee.beUnsafe();
                beUnsafe();
            }
            if (intrinsics.extended) {
                compilee.beExtended();
                beExtended();
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a {@link StackTraceElement} object describing the source code location corresponding to a given bytecode
     * position in this method.
     *
     * @param bytecodePosition a bytecode position in this method's {@linkplain #codeAttribute() code}
     * @return the stack trace element
     */
    public StackTraceElement toStackTraceElement(int bytecodePosition) {
        final ClassActor holder = holder();
        return new StackTraceElement(holder.name.string, name.string, holder.sourceFileName, sourceLineNumber(bytecodePosition));
    }

    /**
     * Gets the source line number corresponding to a given bytecode position in this method.
     * @param bytecodePosition the byte code position
     * @return -1 if a source line number is not available
     */
    public int sourceLineNumber(int bytecodePosition) {
        CodeAttribute codeAttribute = this.codeAttribute;
        if (codeAttribute == null) {
            codeAttribute = this.originalCodeAttribute;
        }
        if (codeAttribute == null) {
            return -1;
        }
        return codeAttribute.lineNumberTable().findLineNumber(bytecodePosition);
    }

    /**
     * Determines if a given bytecode sequence contains either of the instructions ({@link JSR} or {@link RET}) used
     * to implement bytecode subroutines.
     * @param code the byte array of code
     * @return {@link true} if the code contains subroutines
     */
    private static boolean containsSubroutines(byte[] code) {
        final BytecodeVisitor visitor = new BytecodeAdapter() {
            @Override
            protected void opcodeDecoded() {
                final int currentOpcode = currentOpcode();
                if (currentOpcode == JSR || currentOpcode == RET || currentOpcode == JSR_W) {
                    bytecodeScanner().stop();
                }
            }
        };
        final BytecodeScanner scanner = new BytecodeScanner(visitor);
        scanner.scan(new BytecodeBlock(code));
        return scanner.wasStopped();
    }

    /**
     * @see InliningAnnotationsValidator#apply(ClassMethodActor)
     */
    @HOSTED_ONLY
    private void validateInlineAnnotation(ClassMethodActor compilee) {
        if (!compilee.holder().isReflectionStub()) {
            try {
                InliningAnnotationsValidator.apply(compilee);
            } catch (LinkageError linkageError) {
                ProgramWarning.message("Error while validating INLINE annotation for " + compilee + ": " + linkageError);
            }
        }
    }

    /**
     * Gets the original if this is a {@linkplain SUBSTITUTE substitute} method actor otherwise
     * just return this method actor.
     */
    public ClassMethodActor original() {
        final ClassMethodActor original = METHOD_SUBSTITUTIONS.Static.findOriginal(this);
        if (original != null) {
            return original;
        }
        return this;
    }

    public synchronized void verify(ClassVerifier classVerifier) {
        if (codeAttribute() != null) {
            codeAttribute = classVerifier.verify(this, codeAttribute);
        }
    }

    public static ClassMethodActor fromJava(Method javaMethod) {
        return (ClassMethodActor) MethodActor.fromJava(javaMethod);
    }

    public TargetMethod currentTargetMethod() {
        return TargetState.currentTargetMethod(targetState);
    }

    public TargetMethod[] targetMethodHistory() {
        return TargetState.targetMethodHistory(targetState);
    }

    public int targetMethodCount() {
        return TargetState.targetMethodCount(targetState);
    }

    @Override
    public boolean hasCompiledCode() {
        return targetMethodCount() > 0;
    }
}
