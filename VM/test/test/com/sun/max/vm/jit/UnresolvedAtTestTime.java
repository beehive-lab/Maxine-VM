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
package test.com.sun.max.vm.jit;

import com.sun.max.annotate.*;
import com.sun.max.vm.template.generate.*;

/**
 * The purpose of this class is solely for testing cases JIT-compilation of bytecode with operands to class that aren't resolved
 * at compile-time. We need to use a class different than UnresolvedAtCompileTime to make sure the patching of literal references to
 * the class by the template-based JIT compiler are effective.
 *
 * @author Laurent Daynes
 */
@HOSTED_ONLY
public class UnresolvedAtTestTime extends UnresolvedAtCompileTime {
    public static byte staticByteField;
    public static boolean staticBooleanField;
    public static char staticCharField;
    public static short staticShortField;
    public static int staticIntField;
    public static float staticFloatField;
    public static long staticLongField;
    public static double staticDoubleField;
    public static Object staticObjectField;

    public int intField2;
    public byte byteField2;
    public char charField2;
    public short shortField2;
    public long longField2;
    public float floatField2;
    public boolean booleanField2;
    public double doubleField2;
    public Object objField2;

    public int getInt() {
        return intField2;
    }

    public void updateInt(int i1, int i2) {
        intField2 = i1 << i2;
    }

    public static int staticGetInt() {
        return staticIntField;
    }
}
