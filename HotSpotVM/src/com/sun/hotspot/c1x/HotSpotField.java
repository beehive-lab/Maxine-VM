package com.sun.hotspot.c1x;

import com.sun.cri.ci.CiConstant;
import com.sun.cri.ci.CiKind;
import com.sun.cri.ri.RiField;
import com.sun.cri.ri.RiType;

public class HotSpotField implements RiField {
	
	private final RiType holder;
	private final Object nameSymbol;
	private final RiType type;
	private final int offset;
	
	public HotSpotField(RiType holder, Object nameSymbol, RiType type, int offset) {
		this.holder = holder;
		this.nameSymbol = nameSymbol;
		this.type = type;
		this.offset = offset;
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
		return holder;
	}

	@Override
	public boolean isConstant() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isResolved() {
		return offset != -1;
	}

	@Override
	public CiKind kind() {
		return type().kind();
	}

	@Override
	public String name() {
		return VMEntries.RiSignature_symbolToString(nameSymbol);
	}

	@Override
	public RiType type() {
		return type;
	}
	

}
