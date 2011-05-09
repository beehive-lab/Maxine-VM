package com.sun.max.vm.compiler.c1x;

import java.util.*;

import com.sun.c1x.debug.*;
import com.sun.max.config.*;
import com.sun.max.vm.*;
import com.sun.max.vm.hosted.*;

public class Package extends BootImagePackage{
	public Package() {
		JavaPrototype.addObjectIdentityMapContributor(new C1XObjectMapContributor());
	}

	public static class C1XObjectMapContributor implements JavaPrototype.ObjectIdentityMapContributor {
		@Override
		public void initializeObjectIdentityMap(Map<Object, Object> objectMap) {
	        objectMap.put(TTY.out(), new LogStream(Log.os));
	        objectMap.put(CFGPrinter.cfgFileStream(), JavaPrototype.NULL);
		}
	}
}
