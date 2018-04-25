/*
 * Copyright (c) 2017-2018, APT Group, School of Computer Science,
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

public class DebugMethodWriter {

    private final AtomicInteger methodCounter;
    private final Object fileLock;
    private final File file;
    private StringBuffer buffer;

    public DebugMethodWriter(String prefix) {
        methodCounter = new AtomicInteger(0x2000_0000);
        fileLock = new Object();
        file = initDebugMethods(prefix + "_method_id.txt");
        buffer = new StringBuffer();
    }

    public File initDebugMethods(String fileName) {
        File f;
        if ((f = new File(getDebugMethodsPath() + fileName)).exists()) {
            f.delete();
        }
        f = new File(getDebugMethodsPath() + fileName);
        return f;
    }

    public void appendDebugMethod(String name, int index) {
        buffer.append(index + " " + name + "\n");
    }

    public void flushDebugMethod() {
        synchronized (fileLock) {
            try {
                FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(buffer.toString());
                bw.close();
                buffer.setLength(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getDebugMethodsPath() {
        return System.getenv("MAXINE_HOME") + "/maxine-tester/junit-tests/";
    }

    public int getNextID() {
        return methodCounter.incrementAndGet();
    }
}
