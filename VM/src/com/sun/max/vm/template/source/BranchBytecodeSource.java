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
package com.sun.max.vm.template.source;

import com.sun.max.annotate.*;
import com.sun.max.vm.compiler.builtin.*;


/**
 * Class holding Java code the templates for conditional branch bytecode instructions will be compiled from.
 * The templates only comprise the prefix of a conditional branch: popping operands of the comparison and the comparison itself.
 * These templates have no dependencies, i.e., they can be copied as is by the bytecode-to-target translator of the JIT. The actual branching
 * is emitted by the JIT. Templates for a same family of bytecodes are identical (what typically makes them different is the condition being tested).
 * The templates relies on two special builtins for comparing issuing an object comparison and a integer comparison. These are specific to template generation.
 *
 * This source of templates makes no assumptions.
 *
 * @author Laurent Daynes
 */
@TEMPLATE()
public final class BranchBytecodeSource {

    // The first four method below are the prefix

    @INLINE
    private static void icmp0_prefix() {
        SpecialBuiltin.compareInts(JitStackFrameOperation.popInt(), 0);
    }

    @INLINE
    private static void acmp0_prefix() {
        final Object value = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.removeSlots(1);
        SpecialBuiltin.compareReferences(value, null);
    }

    @INLINE
    private static void icmp_prefix() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(2);
        SpecialBuiltin.compareInts(value1, value2);
    }

    @INLINE
    private static void acmp_prefix() {
        final Object value2 = JitStackFrameOperation.peekReference(0);
        final Object value1 = JitStackFrameOperation.peekReference(1);
        JitStackFrameOperation.removeSlots(2);
        SpecialBuiltin.compareReferences(value1, value2);
    }

    @INLINE
    public static void if_acmpeq() {
        acmp_prefix();
    }

    @INLINE
    public static void if_acmpne() {
        acmp_prefix();
    }

    @INLINE
    public static void if_icmpeq() {
        icmp_prefix();
    }

    @INLINE
    public static void if_icmpne() {
        icmp_prefix();
    }

    @INLINE
    public static void if_icmplt() {
        icmp_prefix();
    }

    @INLINE
    public static void if_icmpge() {
        icmp_prefix();
    }

    @INLINE
    public static void if_icmpgt() {
        icmp_prefix();
    }

    @INLINE
    public static void if_icmple() {
        icmp_prefix();
    }

    @INLINE
    public static void ifeq() {
        icmp0_prefix();
    }

    @INLINE
    public static void ifne() {
        icmp0_prefix();
    }

    @INLINE
    public static void iflt() {
        icmp0_prefix();
    }

    @INLINE
    public static void ifge() {
        icmp0_prefix();
    }

    @INLINE
    public static void ifgt() {
        icmp0_prefix();
    }

    @INLINE
    public static void ifle() {
        icmp0_prefix();
    }

    @INLINE
    public static void ifnonnull() {
        acmp0_prefix();
    }

    @INLINE
    public static void ifnull() {
        acmp0_prefix();
    }
}
