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
@PROTOTYPE_ONLY
public class UnresolvedAtTestTime extends UnresolvedAtCompileTime {
    public static byte _staticByteField;
    public static boolean _staticBooleanField;
    public static char _staticCharField;
    public static short _staticShortField;
    public static int _staticIntField;
    public static float _staticFloatField;
    public static long _staticLongField;
    public static double _staticDoubleField;
    public static Object _staticObjectField;

    public int _intField2;
    public byte _byteField2;
    public char _charField2;
    public short _shortField2;
    public long _longField2;
    public float _floatField2;
    public boolean _booleanField2;
    public double _doubleField2;
    public Object _objField2;

    public int getInt() {
        return _intField2;
    }

    public void updateInt(int i1, int i2) {
        _intField2 = i1 << i2;
    }

    public static int staticGetInt() {
        return _staticIntField;
    }
}
