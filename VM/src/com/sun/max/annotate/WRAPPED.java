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
/*VCSID=5b4dcb5a-bad0-4953-ad27-f8f91c4ccbe1*/
package com.sun.max.annotate;

import java.lang.annotation.*;
import java.lang.reflect.*;

import com.sun.max.program.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.type.*;

/**
 * Marks a method is to be wrapped by a {@linkplain WRAPPER wrapper method} in a denoted class. When compiling a wrapped
 * method, the compiler searches for a wrapper method in the {@linkplain #value() denoted class}. A method {@code W} is
 * a wrapper for a method {@code M} iff:
 * <ul>
 * <li>{@code W} is annotated (explicitly or {@linkplain JNI_FUNCTION implicitly}) with {@link WRAPPER}.</li>
 * <li>The {@linkplain SignatureDescriptor#getResultKind() return kind} of {@code W} and {@code M} have the same
 * {@linkplain Kind#toStackKind() stack kind}.</li>
 * <li>The {@linkplain SignatureDescriptor#getParameterKinds() parameter kinds} (as stack kinds) of {@code W} are a
 * prefix of {@code M}'s parameter kinds.
 * <ul>
 * <p>
 * When compiling {@code M}, the code for {@code W} is compiled with any (recursive) calls to {@code W} replaced with
 * a call to {@code M} and that call is inlined. As such, this transformation can only be applied by a compiler that
 * supports inlining (e.g. {@link CirWrapping}).

 * <p>
 * <b>Design Note</b>: The current wrapping mechanism trades off source code repetition against extra complexity in the
 * compiler. In particular, because the return kind of a wrapper method must match the return kind of a wrapped method,
 * a number of wrappers with mostly duplicated code are required when wrapping a number of methods whose wrapping
 * semantics are the same but whose return kinds vary. An example is the {@linkplain JniFunctionWrapper wrappers} for
 * {@linkplain JniFunctions JNI functions}.
 * 
 * @author Doug Simon
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface WRAPPED {

    /**
     * Denotes the class holding a {@linkplain WRAPPER wrapper} for the annotated method.
     */
    Class value();

    public static final class Static {
        private Static() {
        }

        public static Method getWrapper(Method wrappedMethod, Class wrapperHolder) {
            final SignatureDescriptor wrappedDescriptor = SignatureDescriptor.fromJava(wrappedMethod);
            for (Method wrapperMethod : wrapperHolder.getDeclaredMethods()) {
                if (wrapperMethod.getAnnotation(WRAPPER.class) != null) {
                    final SignatureDescriptor wrapperDescriptor = SignatureDescriptor.fromJava(wrapperMethod);
                    if (wrapperDescriptor.getResultKind().toStackKind().equals(wrappedDescriptor.getResultKind().toStackKind())) {
                        final Kind[] wrappedParameters = wrappedDescriptor.getParameterKinds();
                        final Kind[] wrapperParameters = wrapperDescriptor.getParameterKinds();
                        if (wrapperParameters.length <= wrappedParameters.length) {
                            boolean match = true;
                            for (int i = 0; i != wrapperParameters.length; ++i) {
                                if (!wrappedParameters[i].toStackKind().equals(wrapperParameters[i].toStackKind())) {
                                    match = false;
                                    break;
                                }
                            }
                            if (match) {
                                return wrapperMethod;
                            }
                        }
                    }
                }
            }
            throw ProgramError.unexpected("could not find wrapper for " + wrappedMethod);
        }
    }
}
