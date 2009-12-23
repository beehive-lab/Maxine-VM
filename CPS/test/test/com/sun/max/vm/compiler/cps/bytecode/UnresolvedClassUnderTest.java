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
package test.com.sun.max.vm.compiler.cps.bytecode;

import com.sun.max.unsafe.*;

/**
 * This is a class used to test the case where the compiled method
 * includes code that has symbolic references to members of a class
 * that is unresolved at compile time. The way to use it is to
 * include use of a member of the class in the method being compiled.
 * You should be careful to avoid loading the UnresolvedClassUnderTest
 * from within the prototype under test.
 * We leave the instance variables intentionally "package visible" so the
 * method being compiled can access without method invocation for
 * testing purposes.
 * 
 * @see BytecodeTest_getfield
 * @author Laurent Daynes
 */
public class UnresolvedClassUnderTest {
    byte byteField = 111;
    boolean booleanField = true;
    short shortField = 333;
    char charField = 444;
    int intField = 55;
    long longField = 77L;
    float floatField = 6.6F;
    double doubleField = 8.8;
    Word wordField;
    Object referenceField = this;

    public int getIntField() {
        return intField;
    }

    public static void unresolvedStaticMethod() {
        // Do enough stuff here to avoid being inlined.
        int j = 1;
        for (int i = 0; i < 1000; i++) {
            j = j * 2 + 1;
        }
    }
    public static void unresolvedStaticMethod(int k) {
        // Do enough stuff here to avoid being inlined.
        int j = 1;
        for (int i = 0; i < k; i++) {
            j = j * 2 + (k - i);
        }
    }
}
