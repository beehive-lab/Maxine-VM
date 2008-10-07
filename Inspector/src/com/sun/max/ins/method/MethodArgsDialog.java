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
/*VCSID=c5f43f09-874a-4e74-861b-b30f9a34dd0b*/
package com.sun.max.ins.method;

import java.math.*;

import com.sun.max.ins.*;
import com.sun.max.tele.interpreter.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;


/**
 * Interactive support for the interpreter, originally packaged with {@link InspectorInterpreter}.
 *
 * @author Athul Acharya
 */
public final class MethodArgsDialog {

    public static Value[] getArgs(Inspection inspection, MethodActor classMethodActor, ReferenceValue receiver) {
        final Kind[] argKinds = classMethodActor.getParameterKinds();
        final Value[] args = new Value[argKinds.length];
        int i = 0;

        if (receiver != null) {  //classMethodActor is a dynamic method
            args[0] = receiver;
            i++;
        }

        for (; i < argKinds.length; i++) {
            final Kind kind = argKinds[i];

            if (kind == Kind.BOOLEAN) {
                final String input = inspection.inputDialog("Argument " + i + " (boolean, enter 1 or 0):", "");

                if (input.equals("0")) {
                    args[i] = BooleanValue.from(false);
                } else {
                    args[i] = BooleanValue.from(true);
                }
            } else if (kind == Kind.BYTE) {
                final String input = inspection.inputDialog("Argument " + i + " (byte):", "");

                if (input == null) {
                    return null;
                }

                args[i] = IntValue.from(Byte.parseByte(input));
            } else if (kind == Kind.CHAR) {
                final String input = inspection.inputDialog("Argument " + i + " (char):", "");

                if (input == null) {
                    return null;
                }

                args[i] = IntValue.from(input.charAt(0));
            } else if (kind == Kind.DOUBLE) {
                final String input = inspection.inputDialog("Argument " + i + " (double):", "");

                if (input == null) {
                    return null;
                }

                args[i] = DoubleValue.from(Double.parseDouble(input));
            } else if (kind == Kind.FLOAT) {
                final String input = inspection.inputDialog("Argument " + i + " (float):", "");

                if (input == null) {
                    return null;
                }

                args[i] = FloatValue.from(Float.parseFloat(input));
            } else if (kind == Kind.INT) {
                final String input = inspection.inputDialog("Argument " + i + " (int):", "");

                if (input == null) {
                    return null;
                }

                args[i] = IntValue.from(Integer.parseInt(input));
            } else if (kind == Kind.LONG) {
                final String input = inspection.inputDialog("Argument " + i + " (long):", "");

                if (input == null) {
                    return null;
                }

                args[i] = LongValue.from(Long.parseLong(input));
            } else if (kind == Kind.REFERENCE) {
                final String input = inspection.inputDialog("Argument " + i + " (reference, origin address in hex):", "");

                if (input == null) {
                    return null;
                }

                args[i] = inspection.teleVM().createReferenceValue(inspection.teleVM().originToReference(Pointer.fromLong(new BigInteger(input, 16).longValue())));
            } else if (kind == Kind.SHORT) {
                final String input = inspection.inputDialog("Argument " + i + " (short):", "");

                if (input == null) {
                    return null;
                }

                args[i] = ShortValue.from(Short.parseShort(input));
            }
        }

        return args;
    }

}
