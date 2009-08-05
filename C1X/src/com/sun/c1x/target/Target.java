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
package com.sun.c1x.target;

import com.sun.c1x.value.BasicType;
import com.sun.c1x.util.Util;

/**
 * The <code>Target</code> class represents the target of a compilation, including
 * the CPU architecture and other configuration information of the machine. Such
 * configuration information includes the size of pointers and references, alignment
 * of stacks, caches, etc.
 *
 * @author Ben L. Titzer
 */
public class Target {
    public final Architecture arch;
    public final Backend backend;

    public int referenceSize;
    public int stackAlignment;
    public int cacheAlignment;
    public int codeAlignment;
    public int heapAlignment;
    public final Register[] allocatableRegisters;
    public final Register[] callerSavedRegisters;
    public int firstAvailableSpInFrame;
    public int pageSize;
    public boolean isMP;

    public Target(Architecture arch, Register[] allocatableRegisters, Register[] callerSavedRegisters, int pageSize, boolean isMP) {
        this.arch = arch;
        referenceSize = arch.wordSize;
        stackAlignment = arch.wordSize;
        cacheAlignment = arch.wordSize;
        heapAlignment = arch.wordSize;
        codeAlignment = 16;
        backend = arch.getBackend(this);
        this.callerSavedRegisters = callerSavedRegisters;
        this.allocatableRegisters = allocatableRegisters;
        this.pageSize = pageSize;
        this.isMP = isMP;
    }

    /**
     * Checks whether this target requires special stack alignment, which may entail
     * padding stack frames and inserting alignment code.
     * @return <code>true</code> if this target requires special stack alignment
     * (i.e. {@link #stackAlignment} is greater than {@link #arch} the word size.
     */
    public boolean requiresStackAlignment() {
        return stackAlignment > arch.wordSize;
    }

    /**
     * Checks whether this target has compressed oops (i.e. 32-bit references
     * on a 64-bit machine).
     * @return <code>true</code> if this target has compressed oops
     */
    public boolean hasCompressedOops() {
        return referenceSize < arch.wordSize;
    }

    /**
     * Gets the size in bytes of the specified basic type for this target.
     * @param basicType the basic type for which to get the size
     * @return the size in bytes of the basic type
     */
    public int sizeInBytes(BasicType basicType) {
        return basicType.sizeInBytes(referenceSize, arch.wordSize);
    }

    public boolean supportsSSE() {
        return Util.nonFatalUnimplemented(true);
    }

    public boolean supports3DNOW() {
        return Util.nonFatalUnimplemented(true);
    }

    public boolean supportsSSE2() {
        return Util.nonFatalUnimplemented(true);
    }

    public boolean supportsLzcnt() {
        return Util.nonFatalUnimplemented(true);
    }

    public boolean supportsCmov() {
        return Util.nonFatalUnimplemented(true);
    }

    public boolean supportsMmx() {
        return Util.nonFatalUnimplemented(true);
    }

    public boolean supportsSse42() {
        return Util.nonFatalUnimplemented(false);
    }

    public boolean supportsMMX() {
        return Util.nonFatalUnimplemented(true);
    }

    public boolean isIntel() {
        return Util.nonFatalUnimplemented(false);
    }

    public boolean isAmd() {
        return Util.nonFatalUnimplemented(true);
    }

    public boolean supportsPopcnt() {
        return Util.nonFatalUnimplemented(true);
    }

    public boolean supportsSse41() {
        return Util.nonFatalUnimplemented(false);
    }

    public boolean isP6() {
        return Util.nonFatalUnimplemented(false);
    }

    public boolean supportsCx8() {
        return Util.nonFatalUnimplemented(true);
    }

    public boolean isWin64() {
        return Util.nonFatalUnimplemented(false);
    }

    public boolean isWindows() {
        return Util.nonFatalUnimplemented(false);
    }

    public boolean isSolaris() {
        return Util.nonFatalUnimplemented(true);
    }
}
