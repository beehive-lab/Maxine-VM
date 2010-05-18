package com.sun.hotspot.c1x;

import com.sun.cri.ri.RiConstantPool;
import com.sun.cri.ri.RiField;
import com.sun.cri.ri.RiMethod;
import com.sun.cri.ri.RiSignature;
import com.sun.cri.ri.RiType;

public class VMEntries {
	public static native byte[] RiMethod_code(Object methodOop);
	public static native int RiMethod_maxStackSize(Object methodOop);
	public static native int RiMethod_maxLocals(Object methodOop);
	public static native RiType RiMethod_holder(Object methodOop);
	public static native String RiMethod_signature(Object methodOop);
	public static native String RiMethod_name(Object methodOop);
	public static native RiType RiSignature_lookupType(String returnType, Object accessingClass);
	public static native String RiSignature_symbolToString(Object symbolOop);
	public static native Class<?> RiType_javaClass(Object klassOop);
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
}
