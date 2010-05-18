package com.sun.hotspot.c1x;

import com.sun.cri.ci.CiConstant;
import com.sun.cri.ci.CiKind;
import com.sun.cri.ri.RiMethod;
import com.sun.cri.ri.RiType;

public class HotSpotTypePrimitive implements RiType {

	private CiKind kind;
	
	public HotSpotTypePrimitive(CiKind kind) {
		this.kind = kind;
	}
	
	@Override
	public int accessFlags() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public RiType arrayOf() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RiType componentType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RiType exactType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CiConstant getEncoding(Representation r) {
		// TODO Auto-generated method stub
		
		return null;
	}

	@Override
	public CiKind getRepresentationKind(Representation r) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasFinalizableSubclass() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasFinalizer() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasSubclass() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isArrayClass() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isInitialized() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isInstance(Object obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isInstanceClass() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isInterface() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isResolved() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isSubtypeOf(RiType other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Class<?> javaClass() {
		return kind.toJavaClass();
	}

	@Override
	public CiKind kind() {
		return kind;
	}

	@Override
	public String name() {
		return kind.toString();
	}

	@Override
	public RiMethod resolveMethodImpl(RiMethod method) {
		return null;
	}

}
