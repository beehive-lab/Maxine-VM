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
     * Initialize this code manager.
     */
    @Override
    void initialize() {
        final Address address = Code.bootCodeRegion.end().roundedUpBy(Platform.hostOrTarget().pageSize);
        final Size size = runtimeCodeRegionSize.getValue();
        if (!VirtualMemory.allocateAtFixedAddress(address, size, VirtualMemory.Type.CODE)) {
            ProgramError.unexpected("could not allocate runtime code region");
        }
        runtimeCodeRegion.bind(address, size);
    }
}
