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
/*VCSID=ff1f96ab-a1d6-4459-b231-7292641c1dbd*/
package util;

import java.io.*;
import java.util.*;

import com.sun.max.program.*;


public final class PropertyInfo {

    private PropertyInfo() {
        super();
    }

    private static void list(PrintStream out) {
        final Dictionary h = System.getProperties();
        for (final Enumeration e = h.keys(); e.hasMoreElements();) {
            final Object key = e.nextElement();
            out.println(key + " = " + h.get(key));
        }
    }

    public static void main(String[] args) {
        list(System.out);

        final Classpath classpath = Classpath.fromSystem();
        final File file = classpath.entries().first().file();
        try {
            System.out.println("vanilla: " + file.getCanonicalFile());
            System.out.println("absolute: " + file.getAbsoluteFile());
            System.out.println("canonical: " + file.getCanonicalFile());
        } catch (Throwable throwable) {
            ProgramError.unexpected();
        }
    }

}
