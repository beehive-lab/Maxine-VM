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
package com.sun.max.vm.runtime;

import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.*;

/**
 * A memory region containing hand-crafted machine code.
 *
 * @author Laurent Daynes
 */
public abstract class RuntimeStub extends RuntimeMemoryRegion {

    /**
     * Creates a memory region containing some given hand-crafted machine code.
     * The given machine code is copied into the memory managed by the {@linkplain Code code manager}.
     * It's a {@link FatalError} if space for {@code code} cannot be allocated by the code manager.
     *
     * @param code the machine that is to be copied into the code region
     */
    public RuntimeStub(byte[] code) {
        super(Size.fromInt(code.length));
        setDescription("Stub-" + name());
        if (!Code.allocateRuntimeStub(this)) {
            FatalError.unexpected("Could not allocate the stub code for " + this.getClass().getName());
        }
        _mark = end();
        final Pointer stubStart = this.start().asPointer();
        Memory.writeBytes(code, stubStart);
    }

    public abstract boolean walkFrame(StackFrameWalker stackFrameWalker, boolean isTopFrame, Purpose purpose, Object context);

    /**
     * Two runtime stubs are equal if their {@linkplain Object#getClass() types} are identical and they denote the same
     * address range.
     */
    @Override
    public boolean equals(Object object) {
        if (object != null && getClass().equals(object.getClass())) {
            final RuntimeStub runtimeStub = (RuntimeStub) object;
            return start().equals(runtimeStub.start()) && end().equals(runtimeStub.end());
        }
        return false;
    }

    /**
     * Gets a descriptive name for this stub.
     */
    public abstract String name();
}
