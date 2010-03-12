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
package jtt.exbytecode;

import com.sun.max.vm.compiler.builtin.*;

// register -> memory

/*
 * @Harness: java
 * @Runs: 0x7ff8000000000000L = ()java.lang.Double.isNaN; 0x3ff0000000000000L = 1.0d; -4616189618054758400L = -1.0d; 4691882224927966680L = 473729.5945321d
*/
public class EBC_movl2d_03 {
    static class D {
        double d;
    }
    public static double test(long arg) {
        return doTest(new D(), arg);
    }

    private static double doTest(D d, long arg) {
        d.d = SpecialBuiltin.longToDouble(arg);
        return d.d;
    }

}
