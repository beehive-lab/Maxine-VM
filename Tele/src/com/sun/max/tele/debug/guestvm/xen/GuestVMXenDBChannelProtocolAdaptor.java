/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.tele.debug.guestvm.xen;

import java.lang.reflect.*;
import java.util.*;
import com.sun.max.program.*;

/**
 * An adaptor that provides a mechanism for reflective invocation of the {@link GuestVMXenDBChannelProtocol} methods.
 *
 * @author Mick Jordan
 *
 */

public abstract class GuestVMXenDBChannelProtocolAdaptor implements GuestVMXenDBChannelProtocol {
    static class MethodInfo {
        Method method;
        Class<?>[] parameterTypes;
        Class<?> returnType;

        MethodInfo(Method m) {
            method = m;
            parameterTypes = m.getParameterTypes();
            returnType = m.getReturnType();
        }
    }

    Map<String, MethodInfo> methodMap;

    protected GuestVMXenDBChannelProtocolAdaptor() {
        final Method[] interfaceMethods = GuestVMXenDBChannelProtocol.class.getDeclaredMethods();
        final Method[] implMethods = getClass().getDeclaredMethods();
        methodMap = new HashMap<String, MethodInfo>(interfaceMethods.length);
        for (Method m : interfaceMethods) {
            final MethodInfo methodInfo = new MethodInfo(findMethod(implMethods, m.getName()));
            methodMap.put(m.getName(), methodInfo);
        }
    }

    private static Method findMethod(Method[] methods, String name) {
        for (Method m : methods) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        ProgramError.unexpected("GuestVMXenDBChannelProtocolAdaptor: cannot find method " + name);
        return null;
    }


}
