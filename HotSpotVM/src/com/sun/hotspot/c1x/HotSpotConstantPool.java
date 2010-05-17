package com.sun.hotspot.c1x;

import com.sun.cri.ci.CiConstant;
import com.sun.cri.ri.RiConstantPool;
import com.sun.cri.ri.RiField;
import com.sun.cri.ri.RiMethod;
import com.sun.cri.ri.RiSignature;
import com.sun.cri.ri.RiType;

public class HotSpotConstantPool implements RiConstantPool {
	
	private final Object constantPoolOop;
	
	public HotSpotConstantPool(Object o) {
		this.constantPoolOop = o;
	}

	@Override
	public CiConstant encoding() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object lookupConstant(int cpi) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RiMethod lookupMethod(int cpi) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RiSignature lookupSignature(int cpi) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RiType lookupType(int cpi) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RiType resolveType(int cpi) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RiField lookupField(int cpi) {
		// TODO Auto-generated method stub
		return null;
	}

}
