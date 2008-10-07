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
/*VCSID=e054b762-59a4-479d-89da-4e17880a9387*/
package test.jdk;

import java.util.*;


/*
 * @Harness: java
 * @Runs: 0 = "A", 1 = "B", 2 = "C"
 */
public class EnumMap02 {

    public static String test(int i) {
        EnumMap<Enum, String> map = new EnumMap<Enum, String>(Enum.class);
        map.put(Enum.A, "A");
        map.put(Enum.B, "B");
        map.put(Enum.C, "C");
        return map.get(Enum.values()[i]);
    }

    private enum Enum {
        A, B, C
    }
}
