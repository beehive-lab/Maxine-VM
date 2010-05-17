package com.sun.hotspot.c1x;

import com.sun.cri.ci.CiConstant;
import com.sun.cri.ci.CiKind;
import com.sun.cri.ri.RiField;
import com.sun.cri.ri.RiType;

public class HotSpotField implements RiField {
	
	private final Object klassOop;
	private final int index;
	
	public HotSpotField(Object o, int index) {
		this.klassOop = o;
		this.index = index;
	}

	@Override
	public int accessFlags() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public CiConstant constantValue(Object object) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RiType holder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isConstant() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isResolved() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public CiKind kind() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String name() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RiType type() {
		// TODO Auto-generated method stub
		return null;
	}
	

}
