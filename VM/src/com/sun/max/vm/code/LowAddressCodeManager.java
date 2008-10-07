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
/*VCSID=c5a23947-f5eb-4660-bcbc-d2f159ffd81b*/
package com.sun.max.vm.code;

import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;

/**
 * A code manager that allocates virtual memory in the low 31 bits of the address range.
 *
 * @author Bernd Mathiske
 */
public class LowAddressCodeManager extends CodeManager {

    /**
     * Constructs a new code manager with the maximum number of virtual regions, with
     * the starting address at {@code 0x00000000}.
     */
    LowAddressCodeManager() {
        super(VIRTUAL_NUMBER_OF_RUNTIME_CODE_REGIONS);
        setStart(Address.zero());
        setSize(Size.fromInt(Integer.MAX_VALUE));
    }

    /**
     * The number of code regions currently being used.
     */
    private int _numberOfRuntimeCodeRegionsInUse = 0;

    /**
     * Get the next free code region. In this implementation, virtual memory is allocated
     * for the next empty code region in the low part of a 32-bit address space.
     * @return a reference to the next free code region
     */
    @Override
    protected CodeRegion makeFreeCodeRegion() {
        if (_numberOfRuntimeCodeRegionsInUse >= _runtimeCodeRegions.length) {
            Problem.unimplemented("cannot free code regions");
        }

        final Address address = VirtualMemory.allocateIn31BitSpace(Size.fromInt(RUNTIME_CODE_REGION_SIZE));
        if (address.isZero() || address.isAllOnes()) {
            if (_numberOfRuntimeCodeRegionsInUse == 0) {
                ProgramError.unexpected("could not allocate first runtime memory region");
            }
            Problem.unimplemented("cannot free code regions");
        }

        final int index = address.dividedBy(RUNTIME_CODE_REGION_SIZE).toInt();
        final CodeRegion codeRegion = getRuntimeCodeRegion(index);
        assert codeRegion.size().isZero();
        codeRegion.setStart(address);
        codeRegion.setMark(address);
        codeRegion.setSize(Size.fromInt(RUNTIME_CODE_REGION_SIZE));

        _numberOfRuntimeCodeRegionsInUse++;
        return codeRegion;
    }

}
