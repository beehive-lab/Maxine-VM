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
package com.sun.mockvm;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.bcel.classfile.ExceptionTable;

import com.sun.cri.ri.RiExceptionHandler;
import com.sun.cri.ri.RiMethod;
import com.sun.cri.ri.RiMethodProfile;
import com.sun.cri.ri.RiSignature;
import com.sun.cri.ri.RiType;

/**
 * 
 * @author Thomas Wuerthinger
 *
 */
public class MockMethod implements RiMethod {
	
	private final InvokeTarget method;
	private final org.apache.bcel.classfile.Method fileMethod;
	private final RiSignature signature;
	private final MockType holder;
	
	public MockMethod(MockType holder, InvokeTarget rm, org.apache.bcel.classfile.Method fileMethod) {
		this.method = rm;
		this.fileMethod = fileMethod;
		this.holder = holder;
		signature = new MockSignature(rm.signature);
	}

	@Override
	public boolean canBeStaticallyBound() {
		return false;
	}

	@Override
	public byte[] code() {
		if (fileMethod == null) return null;
		return fileMethod.getCode().getCode();
	}

	@Override
	public List<RiExceptionHandler> exceptionHandlers() {
		

		if (fileMethod == null) {
			throw new UnsupportedOperationException();
		}
		
		final List<RiExceptionHandler> handlers = new ArrayList<RiExceptionHandler>();
		final ExceptionTable table = fileMethod.getExceptionTable();
		
		if (table != null) {
			for (int i=0; i<table.getNumberOfExceptions(); i++) {
				throw new UnsupportedOperationException();
			}
		}
		
		return handlers;
	}

	@Override
	public boolean hasBalancedMonitors() {
		// TODO: check
		return false;
	}

	@Override
	public boolean hasCode() {
		return code() != null;
	}

	@Override
	public boolean hasExceptionHandlers() {

		if (fileMethod == null) {
			throw new UnsupportedOperationException();
		}
		
		if (fileMethod.getExceptionTable() == null) {
			return false;
		}

		return fileMethod.getExceptionTable().getNumberOfExceptions() != 0;
	}

	@Override
	public RiType holder() {
		return MockUniverse.lookupType(method.declaringClass);
	}

	@Override
	public int indexInInterface() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int interfaceID() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isAbstract() {
		// TODO Auto-generated method stub
		return (method.modifiers & Modifier.ABSTRACT) != 0;
	}

	@Override
	public boolean isClassInitializer() {
		return method.name.equals("<cinit>");
	}

	@Override
	public boolean isConstructor() {
		return method.name.equals("<init>");
	}

	@Override
	public boolean isLeafMethod() {
		return false;
	}

	@Override
	public boolean isNative() {
		return Modifier.isNative(method.modifiers);
	}

	@Override
	public boolean isOverridden() {
		return true;
	}

	@Override
	public boolean isResolved() {
		return true;
	}

	@Override
	public boolean isStatic() {
		return Modifier.isStatic(method.modifiers);
	}

	@Override
	public boolean isStrictFP() {
		return Modifier.isStrict(method.modifiers);
	}

	@Override
	public boolean isSynchronized() {
		return Modifier.isSynchronized(method.modifiers);
	}

	@Override
	public int javaCodeAtBci(int bci) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String jniSymbol() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object liveness(int bci) {
		return null;
	}

	@Override
	public int maxLocals() {
		if (fileMethod == null) {
			throw new UnsupportedOperationException();
		}
		
		return fileMethod.getCode().getMaxLocals();
	}

	@Override
	public int maxStackSize() {
		if (fileMethod == null) {
			throw new UnsupportedOperationException();
		}
		
		return fileMethod.getCode().getMaxStack();
	}

	@Override
	public RiMethodProfile methodData() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String name() {
		return method.name;
	}

	@Override
	public RiSignature signatureType() {
		return signature;
	}

	public MockType getHolder() {
		return holder;
	}

	@Override
	public String toString() {
		return name();
	}
}
