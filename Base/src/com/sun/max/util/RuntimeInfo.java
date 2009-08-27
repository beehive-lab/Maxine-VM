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
package com.sun.max.util;

import java.io.*;
import java.util.regex.*;

/**
 * Class for run-time system information.
 *
 * @author Paul Caprioli
 */
public final class RuntimeInfo {

    /**
     * Gets the suggested maximum number of processes to fork.
     * @param requestedMemorySize the physical memory size (in bytes) that each process will consume.
     * @return the suggested number of processes that should be started in parallel.
     */
    public static int getSuggestedMaximumProcesses(long requestedMemorySize) {
        final Runtime runtime = Runtime.getRuntime();
        final String os = System.getProperty("os.name");
        long freeMemory = 0L;
        try {
            if (os.equals("Linux")) {
                final Process process = runtime.exec(new String[] {"/usr/bin/free", "-ob"});
                final BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                in.readLine();
                final String line = in.readLine();
                final String[] fields = line.split("\\s+");
                freeMemory = Long.parseLong(fields[3]);
            } else if (os.equals("SunOS")) {
                Process process = runtime.exec(new String[] {"/bin/kstat", "-p", "-nsystem_pages", "-sfreemem"});
                BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = in.readLine();
                String[] fields = line.split("\\s+");
                freeMemory = Long.parseLong(fields[1]);
                process = runtime.exec(new String[] {"/bin/getconf", "PAGESIZE"});
                in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                line = in.readLine();
                freeMemory *= Long.parseLong(line);
            } else if (os.equals("Mac OS X") || os.equals("Darwin")) {
                final Process process = runtime.exec("/usr/bin/vm_stat");
                final BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                final Matcher matcher = Pattern.compile("[^0-9]*([0-9]+)[^0-9]*").matcher(in.readLine());
                if (matcher.matches()) {
                    freeMemory = Long.parseLong(matcher.group(1));
                }
                matcher.reset(in.readLine());
                if (matcher.matches()) {
                    freeMemory *= Long.parseLong(matcher.group(1));
                }
            } else if (os.equals("GuestVM")) {
                freeMemory = 0L;
            }
        } catch (Exception e) {
            freeMemory = 0L;
        }
        final int processors = runtime.availableProcessors();
        if (freeMemory <= 0L || freeMemory >= requestedMemorySize * processors) {
            return processors;
        }
        return Math.max(1, (int) (freeMemory / requestedMemorySize));
    }

    private RuntimeInfo() {
    }
}
