package com.sun.hotspot.c1x;

import com.sun.cri.ri.RiExceptionHandler;
import com.sun.cri.ri.RiMethod;
import com.sun.cri.ri.RiMethodProfile;
import com.sun.cri.ri.RiSignature;
import com.sun.cri.ri.RiType;

public class HotSpotMethod implements RiMethod {
	
	private Object methodOop;
	
	public HotSpotMethod(Object methodOop) {
		this.methodOop = methodOop;
	}

	@Override
	public int accessFlags() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean canBeStaticallyBound() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public byte[] code() {
		return VMEntries.RiMethod_code(methodOop);
	}

	@Override
	public RiExceptionHandler[] exceptionHandlers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasBalancedMonitors() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public RiType holder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isClassInitializer() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isConstructor() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isLeafMethod() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isOverridden() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isResolved() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String jniSymbol() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object liveness(int bci) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int maxLocals() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int maxStackSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public RiMethodProfile methodData() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String name() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RiSignature signature() {
		// TODO Auto-generated method stub
		return null;
	}

}
