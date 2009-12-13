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
package jtt.reflect;

import java.lang.reflect.*;

/*
 * @Harness: java
 * @Runs: 1 = 55
 */
public class Invoke_virtual01 {

    static final HelperTest helper = new HelperTest(55);

    public static int test(int input) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (input == 1) {
            final Method m =  HelperTest.class.getDeclaredMethod("getInt");
            Object o = m.invoke(helper);
            return ((Integer) o).intValue();
        }
        return 0;
    }

    public static class HelperTest {

        private int intField;

        public int getInt() {
            return intField;
        }

        public HelperTest(int i) {
            intField = i;
        }
    }

}
