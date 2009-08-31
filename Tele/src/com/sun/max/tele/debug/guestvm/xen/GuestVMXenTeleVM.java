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
package com.sun.max.tele.debug.guestvm.xen;

import java.io.*;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.prototype.*;

public class GuestVMXenTeleVM extends TeleVM {

    public GuestVMXenTeleVM(File bootImageFile, BootImage bootImage, Classpath sourcepath, String[] commandlineArguments, int processID) throws BootImageException {
        super(bootImageFile, bootImage, sourcepath, commandlineArguments, processID, null);
    }

    private GuestVMXenTeleDomain domain;

    @Override
    protected TeleProcess createTeleProcess(String[] commandLineArguments, TeleVMAgent agent) throws BootImageException {
        return attachToTeleProcess(-1);
    }

    @Override
    protected TeleProcess attachToTeleProcess(int processID) {
        if (domain != null) {
            throw new RuntimeException("Attempt to create multiple XenOSTeleDomains, not allowed.");
        }
        domain = new GuestVMXenTeleDomain(this, bootImage().vmConfiguration.platform(), processID);
        return domain;
    }

    @Override
    protected Pointer loadBootImage(TeleVMAgent agent) throws BootImageException {
        return domain.getBootHeap();
    }

}
