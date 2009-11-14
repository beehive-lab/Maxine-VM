/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;

/**
 * This class finds double-blank lines in files and removes them. We don't know <i>who</i>
 * would write code like that, but if they did, this program would fix that.
 *
 * @author Ben L. Titzer
 */
public class DoubleBlank {
    public static void main(String[] args) throws IOException {
        for (String f : args) {
            process(f);
        }
    }

    public static void process(String name) throws IOException {
        FileInputStream f = new FileInputStream(name);
        ByteArrayOutputStream out = new ByteArrayOutputStream(f.available());

        int line = 1;
        byte[] buffer = new byte[1024];
        byte a = 0;
        byte b = 0;
        byte c = 0;
        boolean different = false;
        while (true) {
            int len = f.read(buffer);
            if (len == -1) {
                break;
            }
            for (int i = 0 ; i < len; i++) {
                byte d = buffer[i];
                boolean gen = true;
                if (d == '\n') {
                    if (c == '\n' && (b == '\n')) {
                        System.out.println("Removed " + name + ":" + line);
                        gen = false;
                        different = true;
                    }
                    line++;
                } else if (d == '}' && a == '}') {
                    if (c == '\n' && (b == '\n')) {
                        System.out.println("Warning } \\n\\n } @ " + name + ":" + line);
                    }

                } else if (d == '{' && a == '{') {
                    if (c == '\n' && (b == '\n')) {
                        System.out.println("Warning { \\n\\n { @ " + name + ":" + line);
                    }

                }
                a = b;
                b = c;
                c = d;
                if (gen) {
                    out.write(d);
                }
            }
        }

        f.close();
        if (different) {
            FileOutputStream g = new FileOutputStream(name);
            g.write(out.toByteArray());
            g.close();
        }
    }

}
