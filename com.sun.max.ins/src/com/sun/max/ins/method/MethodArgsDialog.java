/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.method;

import java.math.*;

import com.sun.max.ins.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Interactive support for remote method interpretation in the VM.
 */
public final class MethodArgsDialog {

    public static Value[] getArgs(Inspection inspection, MethodActor classMethodActor, ReferenceValue receiver) {
        return getArgs(inspection, classMethodActor.getParameterKinds(), receiver);
    }

    public static Value[] getArgs(Inspection inspection, Kind[] argKinds, ReferenceValue receiver) {
        final Value[] args = new Value[argKinds.length];
        int i = 0;

        if (receiver != null) {  //classMethodActor is a dynamic method
            args[0] = receiver;
            i++;
        }

        for (; i < argKinds.length; i++) {
            final Kind kind = argKinds[i];

            if (kind == Kind.BOOLEAN) {
                final String input = inspection.gui().inputDialog("Argument " + i + " (boolean, enter 1 or 0):", "");

                if (input.equals("0")) {
                    args[i] = BooleanValue.from(false);
                } else {
                    args[i] = BooleanValue.from(true);
                }
            } else if (kind == Kind.BYTE) {
                final String input = inspection.gui().inputDialog("Argument " + i + " (byte):", "");

                if (input == null) {
                    return null;
                }

                args[i] = IntValue.from(Byte.parseByte(input));
            } else if (kind == Kind.CHAR) {
                final String input = inspection.gui().inputDialog("Argument " + i + " (char):", "");

                if (input == null) {
                    return null;
                }

                args[i] = IntValue.from(input.charAt(0));
            } else if (kind == Kind.DOUBLE) {
                final String input = inspection.gui().inputDialog("Argument " + i + " (double):", "");

                if (input == null) {
                    return null;
                }

                args[i] = DoubleValue.from(Double.parseDouble(input));
            } else if (kind == Kind.FLOAT) {
                final String input = inspection.gui().inputDialog("Argument " + i + " (float):", "");

                if (input == null) {
                    return null;
                }

                args[i] = FloatValue.from(Float.parseFloat(input));
            } else if (kind == Kind.INT) {
                final String input = inspection.gui().inputDialog("Argument " + i + " (int):", "");

                if (input == null) {
                    return null;
                }

                args[i] = IntValue.from(Integer.parseInt(input));
            } else if (kind == Kind.LONG) {
                final String input = inspection.gui().inputDialog("Argument " + i + " (long):", "");

                if (input == null) {
                    return null;
                }

                args[i] = LongValue.from(Long.parseLong(input));
            } else if (kind.isReference) {
                final String input = inspection.gui().inputDialog("Argument " + i + " (reference, origin address in hex):", "");

                if (input == null) {
                    return null;
                }

                args[i] = inspection.vm().createReferenceValue(inspection.vm().makeReference(Pointer.fromLong(new BigInteger(input, 16).longValue())));
            } else if (kind == Kind.SHORT) {
                final String input = inspection.gui().inputDialog("Argument " + i + " (short):", "");

                if (input == null) {
                    return null;
                }

                args[i] = ShortValue.from(Short.parseShort(input));
            }
        }

        return args;
    }

}
