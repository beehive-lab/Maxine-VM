/*
 * Copyright (c) 2010 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.hotspot.c1x;

import com.sun.cri.ri.RiConstantPool;
import com.sun.cri.ri.RiField;
import com.sun.cri.ri.RiMethod;
import com.sun.cri.ri.RiSignature;
import com.sun.cri.ri.RiType;

public class VMEntries {

    // Checkstyle: stop

    public static native byte[] RiMethod_code(Object methodOop);

    public static native int RiMethod_maxStackSize(Object methodOop);

    public static native int RiMethod_maxLocals(Object methodOop);

    public static native RiType RiMethod_holder(Object methodOop);

    public static native String RiMethod_signature(Object methodOop);

    public static native String RiMethod_name(Object methodOop);

    public static native RiType RiSignature_lookupType(String returnType, Object accessingClass);

    public static native String RiSignature_symbolToString(Object symbolOop);

    public static native Class< ? > RiType_javaClass(Object klassOop);

    public static native String RiType_name(Object klassOop);

    public static native Object RiConstantPool_lookupConstant(Object constantPoolOop, int cpi);

    public static native RiMethod RiConstantPool_lookupMethod(Object constantPoolOop, int cpi, byte byteCode);

    public static native RiSignature RiConstantPool_lookupSignature(Object constantPoolOop, int cpi);

    public static native RiType RiConstantPool_lookupType(Object constantPoolOop, int cpi);

    public static native RiField RiConstantPool_lookupField(Object constantPoolOop, int cpi);

    public static native RiType findRiType(Object holderKlassOop);

    public static native RiConstantPool RiRuntime_getConstantPool(Object klassOop);

    public static native boolean RiType_isArrayClass(Object klassOop);

    public static native boolean RiType_isInstanceClass(Object klassOop);

    public static native boolean RiType_isInterface(Object klassOop);

    public static native int RiMethod_accessFlags(Object methodOop);

    public static native void installCode(Object methodOop, byte[] code, int frameSize);

    // Checkstyle: resume

}
