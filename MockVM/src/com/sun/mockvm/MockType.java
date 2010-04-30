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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;

import com.sun.cri.ci.CiConstant;
import com.sun.cri.ci.CiKind;
import com.sun.cri.ri.RiConstantPool;
import com.sun.cri.ri.RiField;
import com.sun.cri.ri.RiMethod;
import com.sun.cri.ri.RiType;

/**
 * 
 * @author Thomas Wuerthinger
 *
 */
public class MockType implements RiType {

	private final Class<?> klass;
	private JavaClass fileKlass;
	private final Map<AccessibleObject, RiMethod> methodCache = new HashMap<AccessibleObject, RiMethod>();
	private final Map<Field, RiField> fieldCache = new HashMap<Field, RiField>();
	private MockConstantPool constantPool;
	
	public MockType(Class<?> klass) {
		this.klass = klass;
		
		// Search for class file
		String canonicalName = klass.getCanonicalName();
		
		String fileName = "bin" + File.separatorChar + canonicalName.replace('.', File.separatorChar) + ".class";
		File f = new File(fileName);
		if (f.exists()) {
			try {
				ClassParser parser = new ClassParser(fileName);
				fileKlass = parser.parse();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		if (fileKlass != null) {
			constantPool = new MockConstantPool(fileKlass.getConstantPool());
		}
	}
	
	@Override
	public RiType arrayOf() {
		Object array = Array.newInstance(klass, 0);
		return MockUniverse.lookupType(array.getClass());
	}

	@Override
	public RiType componentType() {
		assert klass.isArray();
		return MockUniverse.lookupType(klass.getComponentType());
	}

	@Override
	public RiType exactType() {
		// TODO: Sometimes return this if type is final etc
		return null;
	}

	@Override
	public CiConstant getEncoding(Representation r) {
		// TODO: return real encoding
		return CiConstant.forObject(this);
	}

	@Override
	public CiKind getRepresentationKind(Representation r) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasFinalizableSubclass() {
		return false;
	}

	@Override
	public boolean hasFinalizer() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasSubclass() {
		// TODO: Check
		return true;
	}

	@Override
	public boolean isArrayKlass() {
		return klass.isArray();
	}

	@Override
	public int accessFlags() {
		return klass.getModifiers();
	}

	@Override
	public boolean isInitialized() {
		return true;
	}

	@Override
	public boolean isInstance(Object obj) {
		return obj.getClass() == klass;
	}

	@Override
	public boolean isInstanceClass() {
		return !klass.isArray() && !klass.isInterface() && !klass.isPrimitive();
	}

	@Override
	public boolean isInterface() {
		return klass.isInterface();
	}

	@Override
	public boolean isResolved() {
		return true;
	}

	@Override
	public boolean isSubtypeOf(RiType other) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Class<?> javaClass() {
		return klass;
	}

	@Override
	public CiKind kind() {
		return CiKind.fromJavaClass(klass);
	}

	@Override
	public String name() {
		return MockSignature.toSignature(klass);
	}

	@Override
	public RiMethod resolveMethodImpl(RiMethod method) {
		// TODO: Implement correctly
		return method;
	}
	
	private RiMethod lookupMethod(AccessibleObject m) {
		RiMethod result = methodCache.get(m);
		
		if (result == null) {
			
			InvokeTarget rm = InvokeTarget.create(m);
			result = new MockMethod(this, rm, lookupFileMethod(rm));
			methodCache.put(m, result);
		}
		
		return result;
	}
	
	private RiField lookupField(Field f) {
		RiField result = fieldCache.get(f);
		
		if (result == null) {
			
			result = new MockField(this, f);
			fieldCache.put(f, result);
			
		}
		
		return result;
	}
	
	private org.apache.bcel.classfile.Method lookupFileMethod(InvokeTarget rm) {
		
		if (fileKlass == null) {
			return null;
		}
		
		for (org.apache.bcel.classfile.Method m : fileKlass.getMethods()) {
			if (m.getSignature().equals(rm.signature) && m.getName().equals(rm.name)) {
				return m;
			}
		}
		
		assert false;
		return null;
	}
	
	public RiMethod lookupMethod(String name, String signature) {

		if (name.equals("<init>") || name.equals("<cinit>")) {

			for (Constructor<?> c : klass.getDeclaredConstructors()) {
				if (signature.equals(MockSignature.toSignature(c))) {
					return lookupMethod(c);
				}
			}
		}
		
		Class<?> curKlass = klass;
		while (curKlass != null) {
			
			for (Method m : curKlass.getDeclaredMethods()) {
				if (name.equals(m.getName()) && signature.equals(MockSignature.toSignature(m))) {
					return lookupMethod(m);
				}
			}
			
			// TODO: Check if this is correct? Are we lookup up private method of super class?
			curKlass = curKlass.getSuperclass();
		}
		
		
		assert false : "method not found with name=" + name + " and signature = " + signature + " in class " +klass.getCanonicalName();
		return null;
		
	}

	public RiConstantPool getConstantPool() {
		return constantPool;
	}

	public RiField lookupField(String fieldName, String fieldSignature) {
		
		for (Field f : klass.getDeclaredFields()) {
			if (f.getName().equals(fieldName) && MockSignature.toSignature(f.getType()).equals(fieldSignature)) {
				return lookupField(f);
			}
		}
		
		assert false : "field not found with name=" + fieldName + " and signature " + fieldSignature;
		return null;
	}
	
	@Override
	public String toString() {
		return name();
	}

}
