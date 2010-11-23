/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.ir;

import java.util.*;

import com.sun.c1x.value.*;
import com.sun.cri.ci.*;

/**
 * This class collects a number of debugging and exception-related information about
 * an HIR node. Instances of this class can be attached to HIR nodes and contain
 * the {@link com.sun.cri.ci.CiCodePos code position}, the {@link com.sun.c1x.value.FrameState frame state}
 * potential exceptions, and exception handlers.
 *
 * @author Ben L. Titzer
 */
public class Info {

    /**
     * An enumeration of the possible exceptions or stops that an instruction may generate.
     */
    public enum StopType {
        /**
         * This instruction may throw {@link ArrayIndexOutOfBoundsException}.
         */
        java_lang_ArrayOutOfBoundsException,

        /**
         * This instruction may throw {@link NullPointerException}.
         */
        java_lang_NullPointerException,

        /**
         * This instruction may throw {@link ClassCastException}.
         */
        java_lang_ClassCastException,

        /**
         * This instruction may throw {@link ArrayStoreException}.
         */
        java_lang_ArrayStoreException,

        /**
         * This instruction may throw {@link ArithmeticException}.
         */
        java_lang_ArithmeticException,

        /**
         * This instruction may throw {@link NegativeArraySizeException}.
         */
        java_lang_NegativeArraySizeException,

        /**
         * This instruction may throw {@link OutOfMemoryError}.
         */
        java_lang_OutOfMemoryError,

        /**
         * This instruction may throw {@link IncompatibleClassChangeError}.
         */
        java_lang_IncompatibleClassChangeError,

        /**
         * This instruction may cause a safepoint.
         */
        Safepoint,

        /**
         * This instruction may throw any exception or cause a safepoint.
         */
        Unknown;

        public final int mask = 1 << ordinal();

    }

    public final CiCodePos pos;
    public final int id;
    private int stopFlags;
    private FrameState state;
    private List<ExceptionHandler> exceptionHandlers;

    public Info(CiCodePos pos, int id, FrameState javaFrameState) {
        this.pos = pos;
        this.id = id;
        this.state = javaFrameState;
    }

    public FrameState frameState() {
        return state;
    }

    public boolean mayStop() {
        return stopFlags != 0;
    }

    public boolean mayCauseException() {
        return (stopFlags & ~StopType.Safepoint.mask) != 0;
    }

    public void clearStop(StopType stopType) {
        stopFlags &= ~stopType.mask;
    }

    public void setStop(StopType stopType) {
        stopFlags |= stopType.mask;
    }

}
