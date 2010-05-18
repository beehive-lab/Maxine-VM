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
		// TODO: Check if this is correct.
		return CiConstant.forObject(constantPoolOop);
	}

	@Override
	public Object lookupConstant(int cpi) {
		return VMEntries.RiConstantPool_lookupConstant(constantPoolOop, cpi);
	}

	@Override
	public RiMethod lookupMethod(int cpi, byte byteCode) {
		return VMEntries.RiConstantPool_lookupMethod(constantPoolOop, cpi, byteCode);
	}

	@Override
	public RiSignature lookupSignature(int cpi) {
		return VMEntries.RiConstantPool_lookupSignature(constantPoolOop, cpi);
	}

	@Override
	public RiType lookupType(int cpi) {
		return VMEntries.RiConstantPool_lookupType(constantPoolOop, cpi);
	}

	@Override
	public RiField lookupField(int cpi) {
		return VMEntries.RiConstantPool_lookupField(constantPoolOop, cpi);
	}

}
