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

import java.io.OutputStream;
import java.lang.reflect.Method;

import com.sun.cri.ci.CiTargetMethod;
import com.sun.cri.ri.RiConstantPool;
import com.sun.cri.ri.RiMethod;
import com.sun.cri.ri.RiOsrFrame;
import com.sun.cri.ri.RiRuntime;
import com.sun.cri.ri.RiSnippets;
import com.sun.cri.ri.RiType;

/**
 * 
 * @author Thomas Wuerthinger
 * 
 * CRI runtime implementation for the HotSpot VM.
 *
 */
public class HotSpotRuntime implements RiRuntime {

	@Override
	public int basicObjectLockOffsetInBytes() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int codeOffset() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void codePrologue(RiMethod method, OutputStream out) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String disassemble(byte[] code) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String disassemble(CiTargetMethod targetMethod) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String disassemble(RiMethod method) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RiConstantPool getConstantPool(RiMethod method) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Method getFoldingMethod(RiMethod method) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RiOsrFrame getOsrFrame(RiMethod method, int bci) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RiType getRiType(Class<?> javaClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RiSnippets getSnippets() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean mustInline(RiMethod method) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean mustNotCompile(RiMethod method) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean mustNotInline(RiMethod method) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object registerTargetMethod(CiTargetMethod targetMethod, String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int sizeofBasicObjectLock() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int threadExceptionOffset() {
		// TODO Auto-generated method stub
		return 0;
	}

}
