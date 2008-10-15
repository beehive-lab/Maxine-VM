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
package com.sun.max.vm.code;

import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;

/**
 * A code manager that reserves and allocates virtual memory at a fixed address.
 *
 * @author Bernd Mathiske
 */
public class FixedAddressCodeManager extends CodeManager {

    /**
     * Constructs a new code manager that allocates code at a particular fixed address in a
     * fixed number of regions, with the total size fixed to the total size of the code cache.
     */
    FixedAddressCodeManager() {
        super(NUMBER_OF_RUNTIME_CODE_REGIONS);
        setSize(Size.fromInt(CODE_CACHE_SIZE));
    }

    /**
     * Initialize this code manager.
     */
    @Override
    void initialize() {
        setStart(Code.bootCodeRegion().end().roundedUpBy(Platform.hostOrTarget().pageSize()));
    }

    /**
     * Creates a new code region. In this implementation, the next empty memory region is
     * selected and virtual memory space is allocated for it.
     * @return a reference to the next empty code region
     */
    @Override
    protected CodeRegion makeFreeCodeRegion() {
        for (int i = 0; i < NUMBER_OF_RUNTIME_CODE_REGIONS; i++) {
            final CodeRegion codeRegion = getRuntimeCodeRegion(i);
            if (codeRegion.size().isZero()) {
                final Address address = start().plus(i * RUNTIME_CODE_REGION_SIZE);
                if (!VirtualMemory.allocate(address, Size.fromInt(RUNTIME_CODE_REGION_SIZE))) {
                    ProgramError.unexpected("could not allocate runtime code region");
                }
                codeRegion.setStart(address);
                codeRegion.setMark(address);
                codeRegion.setSize(Size.fromInt(RUNTIME_CODE_REGION_SIZE));
                return codeRegion;
            }
        }
        Problem.unimplemented("cannot free code regions");
        return null;
    }

}
