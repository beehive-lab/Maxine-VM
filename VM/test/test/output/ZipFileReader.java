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
package test.output;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * A simple class that opens a zip file and dumps its contents.
 *
 * @author Ben L. Titzer
 */
public class ZipFileReader {
    private static final char[] _hex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static void main(String[] args) throws IOException {
        readZipFile("VM/test/test/output/ZipFileReader-input.zip");
    }

    private static void readZipFile(String filename) throws IOException {
        final ZipFile zipFile = new ZipFile(new File(filename));
        final Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            final ZipEntry zipEntry = entries.nextElement();
            System.out.println(zipEntry.getName() + ": " + zipEntry.getCompressedSize() + " / " + zipEntry.getSize());
            final InputStream inputStream = zipFile.getInputStream(zipEntry);
            final byte[] buffer = new byte[100];
            int offset = 0;
            while (inputStream.available() > 0) {
                final int len = inputStream.read(buffer);
                System.out.println("offset: " + offset + " (" + len + " bytes)");
                final StringBuffer sbuf = new StringBuffer(len * 4);
                for (int i = 0; i < len; i++, offset++) {
                    final int val = buffer[i];
                    sbuf.append(hexChar(val >> 8));
                    sbuf.append(hexChar(val));
                    sbuf.append(' ');
                    if ((offset + 1) % 32 == 0) {
                        sbuf.append('\n');
                    }
                }
                System.out.print(sbuf.toString());
                System.out.println();
            }
            System.out.println();
        }
    }

    private static char hexChar(int val) {
        return _hex[val & 0xf];
    }
}
