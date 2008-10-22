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
package com.sun.max.annotate;

import java.lang.annotation.*;

import com.sun.max.vm.jni.*;

/**
 * There are two uses of this annotation for Java methods. One applies to <i>VM entry points</i>, Java methods that
 * are directly called by native code (and only methods with this annotation can be called from native code), the other
 * to <i>VM exit points</i>, native methods to be called by Java.
 * <p>
 * <h3>VM entry points</h3>
 * These are <i>static non-native</i> methods. These methods are compiled so as to conform to the native ABI (e.g.
 * implement callee saved registers if necessary) so that it can be called as a C function pointer..
 * <p>
 * <h3>VM exit points</h3>
 * These are <i>private static native</i> methods. The {@link NativeStubGenerator native stub} generated for such methods will:
 * <ul>
 * <li>marshal only the parameters explicit in the Java signature for the native function call (i.e. the JniEnv and
 * jclass parameters are omitted)</li>
 * <li>omit the code to record the last Java frame info (e.g. stack, frame and instruction pointer) to thread local
 * storage</li>
 * </ul>
 * <p>
 * No parameter type or return type of VM entry or exit points may refer to object references - only primitive Java
 * values and 'Word' values are allowed.
 *
 * The  {@link #isInterruptHandler() element} prevents any use of safepoints or related machinery that depends on
 * a full Java thread state being available. It is used in the GuestVM port.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Mick Jordan
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface C_FUNCTION {
    boolean isInterruptHandler() default false;
    /**
     * Determines if this C function is directly called from a signal handler.
     * Used mostly as a marker by stack walking mechanisms to know where to obtain information about the next frame.
     */
    boolean isSignalHandler() default false;

    /**
     * Determines if this C function is the special trap stub that saves the CPU state and restores it.
     */
    boolean isTrapStub() default false;
}
