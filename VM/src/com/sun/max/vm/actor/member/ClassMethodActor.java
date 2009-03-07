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
package com.sun.max.vm.actor.member;

import static com.sun.max.vm.bytecode.Bytecode.Flags.*;

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.bytecode.graft.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.prototype.*;
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

    @INSPECTED
    private CodeAttribute _codeAttribute;

    public ClassMethodActor(Utf8Constant name, SignatureDescriptor descriptor, int flags, CodeAttribute codeAttribute) {
        super(name, descriptor, flags);
        _codeAttribute = codeAttribute;
        _nativeFunction = isNative() ? new NativeFunction(this) : null;
    }

    /**
     * @return number of locals used by the method parameters (including the receiver if this method isn't static).
     */
    public int numberOfParameterLocals() {
        return descriptor().getNumberOfLocals() + ((isStatic()) ? 0 : 1);
    }

    @INSPECTED
    private MethodState _methodState;

    /**
     * Gets the {@link MethodState} object that encapsulates the current runtime compilation state of the method.
     * The object, once set, does not change, although its contents do.
     */
    @INLINE
    public final MethodState methodState() {
        return _methodState;
    }

    @INLINE
    public final void setMethodState(MethodState methodState) {
        assert _methodState == null;
        _methodState = methodState;
    }

    public final void resetMethodState() {
        _methodState = null;
    }

    private ClassMethodActor _compilee;

    private final NativeFunction _nativeFunction;

    /**
     * Gets the object representing the linkage of this native method actor to a native machine code address.
     *
     * @return {@code null} if this method is not {@linkplain #isNative() native}
     */
    public final NativeFunction nativeFunction() {
        return _nativeFunction;
    }

    /**
     * Determines if JNI activity should be traced at a level useful for debugging.
     */
    @INLINE
    public static boolean traceJNI() {
        return _traceJNI;
    }

    private static boolean _traceJNI;

    static {
        new VMOption("-XX:TraceJNI", "Trace JNI activity for debugging purposes.", MaxineVM.Phase.STARTING) {
            @Override
            public boolean parse(Pointer optionStart) {
                _traceJNI = true;
                return super.parse(optionStart);
            }
        };
    }

    public boolean isDeclaredNeverInline() {
        return compilee().isNeverInline();
    }

    public boolean isDeclaredInline(CompilerScheme compilerScheme) {
        if (compilee().isInline()) {
            if (MaxineVM.isPrototyping()) {
                if (compilee().isInlineAfterSnippetsAreCompiled()) {
                    return compilerScheme.areSnippetsCompiled();
                }
            }
            return true;
        }
        return false;
    }

    @INLINE
    public final boolean noSafepoints() {
        return noSafepoints(compilee().flags());
    }

    @INLINE
    public final boolean isDeclaredFoldable() {
        return isDeclaredFoldable(compilee().flags());
    }

    /**
     * Gets the CodeAttribute currently associated with this actor, irrespective of whether or not it will be replaced
     * later as a result of substitution or preprocessing.
     *
     * Note: This method must not be synchronized as it is called by a GC thread during stack walking.
     */
    public final CodeAttribute rawCodeAttribute() {
        return _codeAttribute;
    }

    /**
     * Gets the bytecode that is to be compiled and/or executed for this actor.
     */
    public final synchronized CodeAttribute codeAttribute() {
        // Ensure that any prerequisite substitution of the code to be compiled/executed is performed first
        compilee();

        return _codeAttribute;
    }

    /**
     * @return the actor for the method that will be compiled and/or executed in lieu of this method
     */
    public synchronized ClassMethodActor compilee() {
        if (_compilee == null) {
            _compilee = this;
            if (!isHiddenToReflection()) {
                final ClassMethodActor substitute = METHOD_SUBSTITUTIONS.Static.findSubstituteFor(this);
                if (substitute != null) {
                    _compilee = substitute;
                    _codeAttribute = substitute._codeAttribute;
                }
                if (MaxineVM.isPrototyping()) {
                    validateInlineAnnotation();
                }
            }

            ClassVerifier verifier = null;

            final CodeAttribute codeAttribute = Preprocessor.apply(_compilee, _codeAttribute);
            final boolean modified = _codeAttribute != codeAttribute;
            _codeAttribute = codeAttribute;

            final ClassActor holder = _compilee.holder();
            if (MaxineVM.isPrototyping()) {
                if (holder.kind() != Kind.WORD) {
                    // We simply verify all methods during boot image build time as the overhead should be acceptable.
                    verifier = modified ? new TypeInferencingVerifier(holder) : Verifier.verifierFor(holder);
                }
            } else {
                if (holder().majorVersion() < 50) {
                    // The compiler/JIT/interpreter cannot handle JSR or RET instructions. However, these instructions
                    // can legally appear in class files whose version number is less than 50.0. So, we inline them
                    // with the type inferencing verifier if they appear in the bytecode of a pre-version-50.0 class file.
                    if (containsSubroutines(_codeAttribute.code())) {
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

            if (verifier != null && _codeAttribute != null) {
                _codeAttribute = verifier.verify(_compilee, codeAttribute);
            }
        }
        return _compilee;
    }

    /**
     * Gets a {@link StackTraceElement} object describing the source code location corresponding to a given bytecode
     * position in this method.
     *
     * @param bytecodePosition a bytecode position in this method's {@linkplain #codeAttribute() code}
     */
    public StackTraceElement toStackTraceElement(int bytecodePosition) {
        final ClassActor holder = holder();
        return new StackTraceElement(holder.name().string(), name().string(), holder.sourceFileName(), sourceLineNumber(bytecodePosition));
    }

    /**
     * Gets the source line number corresponding to a given bytecode position in this method.
     *
     * @return -1 if a source line number is not available
     */
    public int sourceLineNumber(int bytecodePosition) {
        return codeAttribute().lineNumberTable().findLineNumber(bytecodePosition);
    }

    /**
     * Gets the source file name of this method's holder.
     *
     * @return null if a source file name is not available
     */
    public String sourceFileName() {
        return holder().sourceFileName();
    }

    /**
     * Determines if a given bytecode sequence contains either of the instructions ({@link JSR} or {@link RET}) used
     * to implement bytecode subroutines.
     */
    private static boolean containsSubroutines(byte[] code) {
        final BytecodeVisitor visitor = new BytecodeAdapter() {
            @Override
            protected void opcodeDecoded() {
                final Bytecode currentOpcode = currentOpcode();
                if (currentOpcode.is(JSR_OR_RET)) {
                    bytecodeScanner().stop();
                }
            }
        };
        final BytecodeScanner scanner = new BytecodeScanner(visitor);
        scanner.scan(new BytecodeBlock(code));
        if (scanner.wasStopped()) {
            return true;
        }
        return false;
    }

    /**
     * @see InliningAnnotationsValidator#apply(Method)
     */
    @PROTOTYPE_ONLY
    private void validateInlineAnnotation() {
        if (!_compilee.holder().isGenerated()) {
            try {
                InliningAnnotationsValidator.apply(_compilee.toJava());
            } catch (LinkageError linkageError) {
                ProgramWarning.message("Error while validating INLINE annotation for " + _compilee + ": " + linkageError);
            }
        }
    }

    public MethodActor original() {
        final MethodActor original = METHOD_SUBSTITUTIONS.Static.findOriginal(this);
        if (original != null) {
            return original;
        }
        return this;
    }

    public synchronized void verify(ClassVerifier classVerifier) {
        if (_codeAttribute != null) {
            _codeAttribute = classVerifier.verify(this, _codeAttribute);
        }
    }

    public static ClassMethodActor fromJava(Method javaMethod) {
        return (ClassMethodActor) MethodActor.fromJava(javaMethod);
    }
}
