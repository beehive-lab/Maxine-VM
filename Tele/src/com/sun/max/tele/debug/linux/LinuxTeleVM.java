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
package com.sun.max.tele.debug.linux;

import java.io.*;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.prototype.*;

/**
 * @author Bernd Mathiske
 */
public final class LinuxTeleVM extends TeleVM {

    @Override
    protected LinuxTeleProcess createTeleProcess(String[] commandLineArguments, int id) {
        return new LinuxTeleProcess(this, bootImage().vmConfiguration().platform(), programFile(), commandLineArguments);
    }

    private Pointer findHeap(String bootImageFileName) throws IOException {
        FileReader fileReader = null;
        final LinuxTeleProcess linuxInferiorProcess = (LinuxTeleProcess) teleProcess();
        try {
            fileReader = new FileReader("/proc/" + linuxInferiorProcess.processID() + "/maps");
            final BufferedReader bufferedReader = new BufferedReader(fileReader);
            while (true) {
                final String line = bufferedReader.readLine();
                if (line == null) {
                    return Pointer.zero();
                }
                if (line.contains(bootImageFileName)) {
                    final String s = line.substring(0, line.indexOf('-'));
                    return Pointer.fromLong(Long.parseLong(s, 16));
                }
            }
        } catch (IOException ioException) {
            return Pointer.zero();
        } finally {
            if (fileReader != null) {
                fileReader.close();
            }
        }
    }

    @Override
    protected Pointer loadBootImage() throws IOException {
        Pointer heap = Pointer.zero();
        final LinuxTeleProcess linuxInferiorProcess = (LinuxTeleProcess) teleProcess();
        try {
            final int maxSyscallsBeforeGivingUp = 10000;
            for (int i = 0; i < maxSyscallsBeforeGivingUp; i++) {
                try {
                    linuxInferiorProcess.waitForNextSyscall();
                } catch (IOException ioException) {
                    // the call in the try block may fail sporadically, but we do not care here and just keep trying
                }
                heap = findHeap(bootImageFile().getName());
                if (!heap.isZero()) {
                    linuxInferiorProcess.initializeDebugging();
                    return heap;
                }
            }
        } finally {
            if (heap.isZero()) {
                try {
                    teleProcess().controller().terminate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return heap;
    }

    public LinuxTeleVM(File bootImageFile, BootImage bootImage, Classpath sourcepath, String[] commandLineArguments, int id) throws BootImageException, IOException {
        super(bootImageFile, bootImage, sourcepath, commandLineArguments, id);
    }

}
