/*
 * Copyright (c) 2017-2019, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
package com.sun.max.vm.compiler;

import java.io.*;
import java.util.concurrent.atomic.*;

/**
 * Assigns a unique ID to each compiled method and dumps a map of IDs to method names in a file.
 * This class gets compiled in the bootimage thus we cannot use a {@link BufferedWriter} as a field of the class and use
 * it to append new mappings.
 * Doing so would result in using a host file descriptor in the image, which would result in crashes if the image was to
 * be run on a different host/path.
 * That said, a new {@link BufferedWriter} is created each time we want to flush the string buffer containing the
 * mappings up to now.
 */
public class DebugMethodWriter {

    private static final int INITIAL_METHOD_ID = 0x2000_0000;
    private static final String OUTPUT_FILE_SUFFIX = "_method_id.txt";
    private final AtomicInteger methodCounter;
    private final Object outputFileLock;
    private final String outputFileName;
    private StringBuffer buffer;

    public DebugMethodWriter(String prefix) {
        methodCounter = new AtomicInteger(INITIAL_METHOD_ID);
        outputFileLock = new Object();
        outputFileName = getDebugMethodsPath() + (prefix + OUTPUT_FILE_SUFFIX);
        buffer = new StringBuffer();

        // Delete the file if it exists since we will be appending to it
        File temp = new File(outputFileName);
        temp.delete();
    }

    public void append(String name, int index) {
        buffer.append(Integer.toHexString(index)).append(" ").append(name).append(System.lineSeparator());
    }

    public void flush() {
        synchronized (outputFileLock) { // Avoid concurrent writes to output file
            try {
                FileWriter fw = new FileWriter(outputFileName, true);
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(buffer.toString());
                bw.close();
                buffer.setLength(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String getDebugMethodsPath() {
        return System.getenv("MAXINE_HOME") + File.separator + "maxine-tester" + File.separator + "junit-tests" + File.separator;
    }

    public int getNextID() {
        return methodCounter.incrementAndGet();
    }
}
