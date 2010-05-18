package com.sun.hotspot.c1x;

import com.sun.cri.ri.RiExceptionHandler;
import com.sun.cri.ri.RiMethod;
import com.sun.cri.ri.RiMethodProfile;
import com.sun.cri.ri.RiSignature;
import com.sun.cri.ri.RiType;

public class HotSpotMethod implements RiMethod {
	
	Object methodOop;
	private byte[] code;
	
	public HotSpotMethod(Object methodOop) {
		this.methodOop = methodOop;
	}

	@Override
	public int accessFlags() {
		return VMEntries.RiMethod_accessFlags(methodOop);
	}

	@Override
	public boolean canBeStaticallyBound() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public byte[] code() {
		if (code == null) {
			code = VMEntries.RiMethod_code(methodOop);
		}
		
		return code;
	}

	@Override
	public RiExceptionHandler[] exceptionHandlers() {
		// TODO: Add support for exception handlers
		return new RiExceptionHandler[0];
	}

	@Override
	public boolean hasBalancedMonitors() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public RiType holder() {
		return VMEntries.RiMethod_holder(methodOop);
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
		return VMEntries.RiMethod_maxLocals(methodOop);
	}

	@Override
	public int maxStackSize() {
		return VMEntries.RiMethod_maxStackSize(methodOop);
	}

	@Override
	public RiMethodProfile methodData() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String name() {
		return VMEntries.RiMethod_name(methodOop);
	}

	@Override
	public RiSignature signature() {
		return new HotSpotSignature(VMEntries.RiMethod_signature(methodOop));
	}

}
