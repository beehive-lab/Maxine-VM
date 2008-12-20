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
package com.sun.max.tele.debug.no;

import java.io.*;

import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.prototype.*;

/**
 * A null VM instance, with the boot image but no process.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class NoTeleMaxineVM extends TeleVM {

    private boolean _relocating;

    private TeleMessenger _messenger = new NoVMInspectorMessenger();

    public NoTeleMaxineVM(File bootImageFile, BootImage bootImage, Classpath sourcepath, boolean relocating) throws BootImageException, IOException {
        super(bootImageFile, bootImage, sourcepath, TeleProcess.NO_COMMAND_LINE_ARGUMENTS, -1);
        _relocating = relocating;
    }

    @Override
    public TeleMessenger messenger() {
        return _messenger;
    }

    @Override
    protected NoTeleProcess createTeleProcess(String[] commandLineArguments, int id) throws IOException {
        return new NoTeleProcess(this, bootImage().vmConfiguration().platform(), programFile());
    }

    @Override
    protected Pointer loadBootImage() throws IOException {
        if (bootImage().vmConfiguration().platform().operatingSystem() != OperatingSystem.GUESTVM) {
            return bootImage().map(bootImageFile(), _relocating);
        }
        return null;
    }

    @Override
    public boolean isBootImageRelocated() {
        return _relocating;
    }

    @Override
    public void advanceToJavaEntryPoint() {
    }

}
