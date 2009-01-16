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

/**
 * A simple class that opens a file and dumps its contents.
 *
 * @author Ben L. Titzer
 */
public class FileReader {
    private static final int BYTES_PER_LINE = 32;
    private static final char[] _hex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static final String INPUT_FILE_NAME = FileReader.class.getSimpleName() + ".input";

    public static void main(String[] args) throws IOException, InterruptedException {
        int bufferSize = 100;
        if (args.length > 0) {
            bufferSize = Integer.parseInt(args[0]);
        }
        readFile(new File(INPUT_FILE_NAME), bufferSize);
    }

    private static void readFile(File file, int bufferSize) throws IOException {
        final InputStream inputStream = new FileInputStream(file);
        printStream(inputStream, bufferSize);
        System.out.println();
        System.out.flush();
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
        return _hex[val & 0xf];
    }
}
