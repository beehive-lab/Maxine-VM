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
package com.sun.max.tele.method;

import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;

/**
 * Represents a region of VM memory that holds compiled code for a method.
 * <br>
 * TEMPORARY:  this very awkward class gets rewritten when all the memory region work gets completed.
 *
 * @author Michael Van De Vanter
 */
@Deprecated
public final class MethodTargetCodeRegion extends CompiledMethodMemoryRegion {

    private final TeleTargetMethod teleTargetMethod;
    private Address regionStart = Address.zero();
    private Size regionSize = Size.zero();

    public MethodTargetCodeRegion(TeleVM teleVM, TeleTargetMethod teleTargetMethod) {
        super(teleVM, teleTargetMethod, Address.zero(), Size.zero(), "Method TeleTarget-" + teleTargetMethod.toString());
        this.teleTargetMethod = teleTargetMethod;
    }

    @Override
    public Address start() {
        if (regionStart.isZero()) {
            regionStart = teleTargetMethod.getRegionStart();
        }
        return regionStart;
    }

    @Override
    public Size size() {
        if (regionSize.isZero()) {
            regionSize = teleTargetMethod.getRegionSize();
        }
        return regionSize;
    }

}
