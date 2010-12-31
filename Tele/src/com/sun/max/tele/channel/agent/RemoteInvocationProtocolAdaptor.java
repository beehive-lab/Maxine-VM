/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.tele.channel.agent;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.max.tele.channel.*;
import com.sun.max.tele.channel.iostream.TeleChannelDataIOProtocolImpl.ArrayMode;
import com.sun.max.tele.util.*;

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
        final List<Class< ? >> interfaces = new ArrayList<Class< ? >>();
        getTeleChannelDataIOProtocolInterfaceMethods(interfaces, this.getClass());
        methodMap = new HashMap<String, MethodInfo>();
        for (Class< ? > intf : interfaces) {
            final Method[] interfaceMethods = intf.getDeclaredMethods();
            final Method[] implMethods = getClass().getMethods();
            for (Method m : interfaceMethods) {
                final MethodInfo methodInfo = new MethodInfo(findMethod(implMethods, m.getName()));
                //Trace.line(2, "adding " + m.getName() + " from " + intf.getName() + " to method map");
                methodMap.put(m.getName(), methodInfo);
            }
        }
    }

    private static void getTeleChannelDataIOProtocolInterfaceMethods(List<Class< ? >> result, Class< ? > klass) {
        if (klass != null) {
            final Class< ? >[] interfaces = klass.getInterfaces();
            for (Class< ? > k : interfaces) {
                if (k.getName().endsWith("TeleChannelDataIOProtocol")) {
                    result.add(k);
                }
            }
            getTeleChannelDataIOProtocolInterfaceMethods(result, klass.getSuperclass());
        }
    }

    private static Method findMethod(Method[] methods, String name) {
        for (Method m : methods) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        TeleError.unexpected("RIProtocolAdaptor: cannot find method " + name);
        return null;
    }

    /**
     * Set the array parameter mode for given parameter of given method.
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
            } else if (klass == String.class) {
                result[index] = in.readUTF();
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
            } else if (klass == String[].class) {
                final int mv = in.readInt();
                final int length = in.readInt();
                //Trace.line(2, "    array mode: " + mv + ", length: " + length);
                ArrayMode am = ArrayMode.values()[mv];
                String[] data;
                if (am == ArrayMode.OUT) {
                    // allocate but don't read (output array)
                    data = new String[length];
                } else {
                    // allocate and read (input or input/output array)
                    data = new String[length];
                    for (int i = 0; i < length; i++) {
                        data[i] = in.readUTF();
                    }
                }
                result[index] = data;
            } else {
                TeleError.unexpected("unexpected argument type readArgs: " + klass.getName());
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
                TeleError.unexpected("unexpected result type writeResult: " + m.returnType.getName());
            }
        }
        out.flush();
    }

}
