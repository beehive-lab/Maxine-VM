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
