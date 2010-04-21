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
package jtt.exbytecode;

import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.builtin.*;

/*
 * @Harness: java
 * @Runs: 1=true; 34=true
 */
public class EBC_lsa01 {
    public static boolean test(int i) {
        Pointer addr = MakeStackVariable.makeStackVariable(i);
        Pointer addr2 = MakeStackVariable.makeStackVariable(i + 1);
        if (addr.readInt(0) != i) {
            return false;
        }
        if (addr2.readInt(0) != i + 1) {
            return false;
        }
        return true;
    }
}
