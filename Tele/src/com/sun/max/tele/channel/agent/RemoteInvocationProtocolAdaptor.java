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
package com.sun.max.tele.channel.agent;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import com.sun.max.program.*;
import com.sun.max.tele.channel.TeleChannelDataIOProtocol;
import com.sun.max.tele.channel.iostream.TeleChannelDataIOProtocolImpl.*;

/**
 * An adaptor that provides a mechanism for remote reflective invocation of the {@link TeleChannelDataIOProtocol} methods.
 * It must be the superclass of any implementation of {@link TeleChannelDataIOProtocol} that wishes to participate in
 * remote reflective invocation.
 *
 * @author Mick Jordan
 *
 */

public abstract class RemoteInvocationProtocolAdaptor {
    public static class MethodInfo {
        public final Method method;
        public final Class<?>[] parameterTypes;
        public final ArrayMode[] arrayModes;
        public final Class<?> returnType;

        MethodInfo(Method m) {
            method = m;
            parameterTypes = m.getParameterTypes();
            arrayModes = new ArrayMode[parameterTypes.length];
            for (int i = 0; i < arrayModes.length; i++) {
                // default mode
                arrayModes[i] = ArrayMode.IN;
            }
            returnType = m.getReturnType();
        }
    }

    public final Map<String, MethodInfo> methodMap;

    protected RemoteInvocationProtocolAdaptor() {
        final Method[] interfaceMethods = TeleChannelDataIOProtocol.class.getDeclaredMethods();
        final Method[] implMethods = getClass().getMethods();
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
        ProgramError.unexpected("RIProtocolAdaptor: cannot find method " + name);
        return null;
    }

    /**
     * Set the array paramater mode for given parameter of given method.
     * @param methodName name of method
     * @param param parameter index, based at zero
     * @param mode the array mode
     */
    protected void setArrayMode(String methodName, int param, ArrayMode mode) {
        final MethodInfo methodInfo = methodMap.get(methodName);
        if (methodInfo == null) {
            throw new IllegalArgumentException(getClass().getName() + ".setArrayMode: no such method: " + methodName);
        }
        methodInfo.arrayModes[param] = mode;
    }

    public Object[] readArgs(DataInputStream in, MethodInfo m) throws IOException {
        //Trace.line(2, "reading args for " + m.method.getName());
        final Object[] result = new Object[m.parameterTypes.length];
        int index = 0;
        for (Class<?> klass : m.parameterTypes) {
            //Trace.line(2, "  class " + klass.getSimpleName());
            if (klass == long.class) {
                result[index] = in.readLong();
            } else if (klass == int.class) {
                result[index] = in.readInt();
            } else if (klass == boolean.class) {
                result[index] = in.readBoolean();
            } else if (klass == byte.class) {
                result[index] = in.readByte();
            } else if (klass == byte[].class) {
                final int mv = in.readInt();
                final int length = in.readInt();
                //Trace.line(2, "    array mode: " + mv + ", length: " + length);
                ArrayMode am = ArrayMode.values()[mv];
                byte[] data;
                if (am == ArrayMode.OUT) {
                    // allocate but don't read (output array)
                    data = new byte[length];
                } else {
                    // allocate and read (input or input/output array)
                    data = new byte[length];
                    in.read(data);
                }
                result[index] = data;
            } else {
                ProgramError.unexpected("unexpected argument type readArgs: " + klass.getName());
            }
            //Trace.line(2, "    value " + result[index] + ((klass == long.class) ? (" 0x" + Long.toHexString((Long) result[index])) : ""));
            index++;
        }
        return result;
    }

    public void writeResult(DataOutputStream out, MethodInfo m, Object result, Object[] args) throws IOException {
        // deal with byte arrays as output
        int index = 0;
        for (Class< ? > klass : m.parameterTypes) {
            if (klass == byte[].class && m.arrayModes[index] != ArrayMode.IN) {
                out.write((byte[]) args[index]);
            }
            index++;
        }
        if (m.returnType != void.class) {
            // //Trace.line(2, "writing result " + result);
            if (m.returnType == boolean.class) {
                out.writeBoolean((Boolean) result);
            } else if (m.returnType == int.class) {
                out.writeInt((Integer) result);
            } else if (m.returnType == long.class) {
                // //Trace.line(2, "  0x" + Long.toHexString((Long) result));
                out.writeLong((Long) result);
            } else {
                ProgramError.unexpected("unexpected result type writeResult: " + m.returnType.getName());
            }
        }
        out.flush();
    }

}
