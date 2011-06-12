/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.tele.debug.no;

import java.io.*;

import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.hosted.*;

/**
 * A null VM instance, with the boot image but no process.
 */
public final class ReadOnlyTeleVM extends TeleVM {

    /**
     * Creates a tele VM instance for inspecting a boot image without executing it.
     *
     * @param bootImageFile a file containing a boot image
     * @param bootImage the metadata describing the boot image in {@code bootImageFile}
     * @param sourcepath the source code path to search for class or interface definitions
     */
    public ReadOnlyTeleVM(File bootImageFile, BootImage bootImage, Classpath sourcepath) throws BootImageException {
        super(bootImageFile, bootImage, sourcepath, TeleProcess.EMPTY_COMMAND_LINE_ARGUMENTS);
    }

    @Override
    protected ReadOnlyTeleProcess createTeleProcess(String[] commandLineArguments) throws BootImageException {
        return new ReadOnlyTeleProcess(this, Platform.platform(), programFile());
    }

    @Override
    protected Pointer loadBootImage() throws BootImageException {
        final ReadOnlyTeleProcess teleProcess = (ReadOnlyTeleProcess) teleProcess();
        return teleProcess.heapPointer();
    }

    @Override
    public void advanceToJavaEntryPoint() {
    }
}
