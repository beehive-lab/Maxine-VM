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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.sun.cri.ci.CiConstant;
import com.sun.cri.ci.CiKind;
import com.sun.cri.ri.RiField;
import com.sun.cri.ri.RiType;

/**
 * @author Thomas Wuerthinger
 *
 */
public class MockField implements RiField {

	private final Field field;
	private final MockType holder;

	public MockField(MockType holder, Field field) {
		this.field = field;
		this.holder = holder;
	}

	@Override
	public CiConstant constantValue() {
		throw new UnsupportedOperationException();
	}

	@Override
	public RiType holder() {
		return holder;
	}

	@Override
	public boolean isConstant() {
		return false;
	}

	@Override
	public boolean isResolved() {
		return true;
	}

	@Override
	public boolean isStatic() {
		return Modifier.isStatic(field.getModifiers());
	}

	@Override
	public boolean isVolatile() {
		return Modifier.isVolatile(field.getModifiers());
	}

	@Override
	public CiKind kind() {
		return CiKind.fromJavaClass(field.getType());
	}

	@Override
	public String name() {
		return field.getName();
	}

	@Override
	public RiType type() {
		return MockUniverse.lookupType(field.getType());
	}

}
