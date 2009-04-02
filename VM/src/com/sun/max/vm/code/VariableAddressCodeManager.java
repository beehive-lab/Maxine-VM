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
import com.sun.max.program.*;
import com.sun.max.unsafe.*;

/**
 * A code manager that allocates virtual memory somewhere in the address space.
 * Since we cannot guarantee that an allocated address of virtual memory is at an address
 * that is a multiple of RUNTIME_CODE_REGION_SIZE (unlike @See FixedAddressCodeManager)
 * we keep the _runtimeCodeRegions array sorted by increasing address.
 *
 * In general we cannot easily guarantee the invariant that the regions managed by this manager
 * are within 32 bits of each other. We assume that the  @see com.sun.max.memory.VirtualMemory.allocate method
 * preserves the constraint when asked to allocate @See com.sun.max.memory.VirtualMemory.Type.CODE.
 *
 *
 * @author Bernd Mathiske
 * @author Mick Jordan
 */
public class VariableAddressCodeManager extends CodeManager {

    /**
     * Constructs a new code manager with the default number of virtual regions.
     * N.B. At image build time we have no idea what the actual address range is.
     */
    VariableAddressCodeManager() {
        super(NUMBER_OF_RUNTIME_CODE_REGIONS);
    }

    /**
     * The number of code regions currently being used.
     */
    private int _numberOfRuntimeCodeRegionsInUse = 0;

    /**
     * Get the next free code region.
     * In this implementation, virtual memory is allocated somewhere in the address space.
     * @return a reference to the code region
     */
    @Override
    protected CodeRegion makeFreeCodeRegion() {
        if (_numberOfRuntimeCodeRegionsInUse >= _runtimeCodeRegions.length) {
            Problem.unimplemented("cannot free code regions");
        }

        final Address address = allocateCodeRegionMemory(Size.fromInt(RUNTIME_CODE_REGION_SIZE));
        if (address.isZero() || address.isAllOnes()) {
            Problem.unimplemented("could not allocate runtime code region");
        }

        CodeRegion codeRegion = null;

        // TODO validate that address is within 32 bits of all existing code regions

        /* N.B. The mapping between the CodeRegion description "Code-N" and the index in the
        * _runTimeCodeRegions array is not maintained unless memory happens to be allocated
        * at increasing addresses
        */

        int index = 0;
        while (index < NUMBER_OF_RUNTIME_CODE_REGIONS) {
            codeRegion = getRuntimeCodeRegion(index);
            if (codeRegion.size().isZero() || codeRegion.start().greaterThan(address)) {
                break;
            }
            index++;
        }
        assert codeRegion != null;

        if (!codeRegion.size().isZero()) {
            // need to move down
            for (int i = _numberOfRuntimeCodeRegionsInUse - 1; i >= index;  i--) {
                _runtimeCodeRegions[i + 1] =  _runtimeCodeRegions[i];
            }
        }
        codeRegion.setStart(address);
        codeRegion.setMark(address);
        codeRegion.setSize(Size.fromInt(RUNTIME_CODE_REGION_SIZE));
        _runtimeCodeRegions[index] = codeRegion;

        if (_numberOfRuntimeCodeRegionsInUse == 0) {
            setStart(codeRegion.start());
            setSize(codeRegion.size());
        } else {
            final CodeRegion firstRegion = getRuntimeCodeRegion(0);
            final CodeRegion lastRegion = getRuntimeCodeRegion(_numberOfRuntimeCodeRegionsInUse);
            setStart(firstRegion.start());
            setSize(lastRegion.end().minus(firstRegion.start()).asSize());
        }
        _numberOfRuntimeCodeRegionsInUse++;

        return codeRegion;
    }

    protected Address allocateCodeRegionMemory(Size size) {
        return VirtualMemory.allocate(size, VirtualMemory.Type.CODE);
    }

    @Override
    protected CodeRegion codePointerToRuntimeCodeRegion(Address codePointer) {
        // simple linear search, could do binary search if performance concern
        for (int i = 0; i < _numberOfRuntimeCodeRegionsInUse; i++) {
            final CodeRegion codeRegion = getRuntimeCodeRegion(i);
            if (codeRegion.contains(codePointer)) {
                return codeRegion;
            }
        }
        return null;
    }


}
