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

package com.sun.hotspot.c1x;

import com.sun.cri.ci.CiCompiler;
import com.sun.cri.ci.CiConstant;
import com.sun.cri.ci.CiKind;
import com.sun.cri.ci.CiResult;
import com.sun.cri.ri.RiConstantPool;
import com.sun.cri.ri.RiField;
import com.sun.cri.ri.RiMethod;
import com.sun.cri.ri.RiSignature;
import com.sun.cri.ri.RiType;

/**
 * 
 * @author Thomas Wuerthinger
 * 
 * Exits from the HotSpot VM into Java code.
 *
 */
public class VMExits {
	
	public static void compileMethod(RiMethod method, int entry_bci) {
		
		assert method instanceof RiMethod : "And YES, this assert is necessary and a potential life saver as this method is called from the VM ;-)";
		
		System.out.println("compileMethod in Java code called!!");
		
		CiCompiler compiler = Compiler.getCompiler();
		CiResult result = compiler.compileMethod(method, null);
		
		System.out.println("Compilation result: ");
		if (result.bailout() != null) {
			System.out.println("Bailout:");
			result.bailout().printStackTrace();
		} else {
			System.out.println(result.targetMethod());
			VMEntries.installCode(((HotSpotMethod)method).methodOop, result.targetMethod().targetCode(), result.targetMethod().frameSize());
		}
	}
	
	public static RiMethod createRiMethod(Object methodOop) {
		System.out.println("creating RiMethod object");
		RiMethod m = new HotSpotMethod(methodOop);
		System.out.println("returning " + m);
		return m;
	}
	
	public static RiSignature createRiSignature(Object symbolOop) {
		System.out.println("Creating RiSignature object");
		String name = VMEntries.RiSignature_symbolToString(symbolOop);
		System.out.println("Signature name: " + name);
		return new HotSpotSignature(name);
	}
	
	public static RiField createRiField(RiType holder, Object nameSymbol, RiType type, int offset) {
		System.out.println("creating RiField object");
		return new HotSpotField(holder, nameSymbol, type, offset);
	}
	
	public static RiType createRiType(Object klassOop) {
		System.out.println("creating RiType object");
		return new HotSpotType(klassOop);
	}

	public static RiType createRiTypePrimitive(int basicType) {
		System.out.println("Creating primitive type with basicType " + basicType);
		CiKind kind = null;
		switch (basicType) {
		case 4:
			kind = CiKind.Boolean;
			break;
		case 5:
			kind = CiKind.Char;
			break;
		case 6:
			kind = CiKind.Float;
			break;
		case 7:
			kind = CiKind.Double;
			break;
		case 8:
			kind = CiKind.Byte;
			break;
		case 9:
			kind = CiKind.Short;
			break;
		case 10:
			kind = CiKind.Int;
			break;
		case 11:
			kind = CiKind.Long;
			break;
		case 14:
			kind = CiKind.Void;
			break;
		default:
			throw new IllegalArgumentException("Unknown basic type: " + basicType);
		}
		System.out.println("Chosen kind: " + kind);
		return new HotSpotTypePrimitive(kind);
	}
	
	public static RiType createRiTypeUnresolved(Object symbolOop, Object accessingKlassOop) {
		System.out.println("Creating unresolved RiType object");
		String name = VMEntries.RiSignature_symbolToString(symbolOop);
		System.out.println("Class name: " + name);
		return new HotSpotTypeUnresolved(name);
	}
	
	public static RiConstantPool createRiConstantPool(Object constantPoolOop) {
		System.out.println("creating RiConstantPool object");
		return new HotSpotConstantPool(constantPoolOop);
	}
	
	public static CiConstant createCiConstantInt(int value) {
		return CiConstant.forInt(value);
	}

	public static CiConstant createCiConstantLong(long value) {
		return CiConstant.forLong(value);
	}

	public static CiConstant createCiConstantFloat(float value) {
		return CiConstant.forFloat(value);
	}
	
	public static CiConstant createCiConstantDouble(double value) {
		return CiConstant.forDouble(value);
	}

	public static CiConstant createCiConstantObject(Object value) {
		return CiConstant.forObject(value);
	}
	
	public static void main(String[] args) throws InterruptedException {
		System.out.println(C1XHotSpotTests.add(1, 2));
		Thread.sleep(5000);
		System.out.println(C1XHotSpotTests.add(1, 2));
	}
}
