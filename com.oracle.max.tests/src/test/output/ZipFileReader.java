/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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
package test.output;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * A simple class that opens a zip file and dumps its contents.
 */
public class ZipFileReader {
    private static final int BYTES_PER_LINE = 32;
    private static final char[] hex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static final String INPUT_FILE_NAME = ZipFileReader.class.getSimpleName() + ".input";

    public static void main(String[] args) throws IOException, InterruptedException {
        int bufferSize = 100;
        if (args.length > 0) {
            bufferSize = Integer.parseInt(args[0]);
        }
        readZipFile(new File(INPUT_FILE_NAME), bufferSize);
    }

    private static void readZipFile(File file, int bufferSize) throws IOException {
        final ZipFile zipFile = new ZipFile(file);
        final Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            final ZipEntry zipEntry = entries.nextElement();
            System.out.println(zipEntry.getName() + ": " + zipEntry.getCompressedSize() + " / " + zipEntry.getSize());
            final InputStream inputStream = zipFile.getInputStream(zipEntry);
            printStream(inputStream, bufferSize);
            System.out.println();
            System.out.flush();
        }
    }

    private static void printStream(final InputStream inputStream, int bufferSize) throws IOException {
        final byte[] buffer = new byte[bufferSize];
        int offset = 0;
        int len;
        while ((len = inputStream.read(buffer)) >= 0) {
            printBuffer(buffer, offset, len);
            offset += len;
        }
    }

    private static void printBuffer(final byte[] buffer, int offset, final int len) {
        final StringBuilder sbuf = new StringBuilder(len * 4);
        sbuf.append("+");
        sbuf.append(offset);
        sbuf.append(" (");
        sbuf.append(len);
        sbuf.append(" bytes)\n");
        int i = 0;
        while (i < len) {
            final int val = buffer[i];
            sbuf.append(hexChar(val >> 8));
            sbuf.append(hexChar(val));
            final int next = i + 1;
            if (next % BYTES_PER_LINE == 0 || next == len) {
                sbuf.append('\n');
            } else {
                sbuf.append(' ');
            }
            i = next;
        }
        System.out.print(sbuf.toString());
    }

    private static char hexChar(int val) {
        return hex[val & 0xf];
    }
}
