/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.ir.interpreter;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.hosted.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * An interpreter for the IR of a method.
 *
 * Given that interpreter instances are instantiated from within the testing framework,
 * the behavior of a particular interpreter is configured via {@linkplain System#getProperty(String) properties}.
 * The name of all properties that pertain to an interpreter start with {@value #PROPERTY_PREFIX}.
 * A {@linkplain #setProperty(String, String) convenience method} is provided for accessing
 * these interpreter properties.
 *
 * Each concrete interpreter class documents the interpreter properties it understands and
 * what they mean.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class IrInterpreter<Method_Type extends IrMethod> {

    public static final String PROPERTY_PREFIX = "max.interpreter.";

    public static final OptionSet options = new OptionSet();
    protected static final Option<List<String>> traceFiltersOption = options.newStringListOption("filter", (String[]) null,
        "trace filters, separated by \",\"");
    protected static final Option<Boolean> jitOption = options.newBooleanOption("jit", false,
        "compile and interpret called methods (instead of using reflection)");
    protected static final Option<Boolean> traceCpuOption = options.newBooleanOption("tracecpu", false,
        "include CPU state in the trace");
    protected static final Option<Boolean> traceStackOption = options.newBooleanOption("tracestack", false,
        "include full stack frame state in the trace");
    protected static final Option<Boolean> traceOption = options.newBooleanOption("trace", false,
        "enable tracing of each IR instruction. This option can be omitted if -" + traceFiltersOption +
        ", -" + traceCpuOption + ", or -" + traceStackOption + " is given a non-default value.");

    static {
        options.loadSystemProperties(PROPERTY_PREFIX);
    }

    protected IrInterpreter() {
    }

    /**
     * Executes a given method with a set of arguments on the interpreter. If the method is a constructor, then value in
     * {@code arguments[0]} is updated to reflect the initialized value.
     */
    public abstract Value execute(IrMethod method, Value... arguments) throws InvocationTargetException;

    /**
     * Loads a value from memory. The first element of {@code arguments} is the base
     * for the memory access. There can only be exactly 1 or 2 extra arguments.
     * If there is 1 extra argument, then it is the offset from the base to the value to
     * be loaded. If there are 2 extra arguments, then the base is the address of an array,
     * {@code arguments[1]} is the array's {@link ArrayLayout#displacement displacement} and
     * {@code arguments[2]} is the index of the element to be retrieved.
     */
    public Value pointerLoad(Kind kind, Value[] arguments) {
        final Object object = arguments[0].asObject();
        if (object instanceof StaticTuple) {
            assert arguments.length == 2;
            final StaticTuple staticTuple = (StaticTuple) object;
            final int offset = arguments[1].toInt();
            final FieldActor fieldActor = staticTuple.findStaticFieldActor(offset);
            final Field field = fieldActor.toJava();
            field.setAccessible(true);
            try {
                return fieldActor.kind.asValue(field.get(null));
            } catch (Throwable throwable) {
                ProgramError.unexpected("could not read field: " + field, throwable);
            }
        }

        final HostedObjectMirror mirror = new HostedObjectMirror(object);
        final SpecificLayout specificLayout = mirror.classActor().dynamicHub().specificLayout;

        if (arguments.length == 2) {
            final int offset = arguments[1].toInt();
            return specificLayout.readValue(kind, mirror, offset);
        }
        ProgramError.check(arguments.length == 3, "wrong number of arguments for PointerLoadBuiltin");
        ProgramError.check(arguments[1].asInt() == ((ArrayLayout) specificLayout).getElementOffsetFromOrigin(0).toInt(), "invalid PointerLoadBuiltin array displacement");
        final int index = arguments[2].asInt();
        return mirror.readElement(kind, index);
    }

    /**
     * Stores a value to memory. The first element of {@code arguments} is the base
     * for the memory access. There can only be exactly 2 or 3 extra arguments.
     * If there are 2 extra arguments, then the first is the offset from the base at which to
     * store the value which is in the second extra argument. If there are 3 extra arguments,
     * then the base is the address of an array, {@code arguments[1]} is the array's
     * {@link ArrayLayout#displacement displacement}, {@code arguments[2]} is the index
     * at which to store {@code arguments[3]}, the value.
     */
    public void pointerStore(Kind kind, Value[] arguments) {
        final Object object = arguments[0].asObject();
        if (object instanceof StaticTuple) {
            assert arguments.length == 3;
            final StaticTuple staticTuple = (StaticTuple) object;
            final int offset = arguments[1].toInt();
            final FieldActor fieldActor = staticTuple.findStaticFieldActor(offset);
            final Object value = fieldActor.kind.convert(arguments[2]).asBoxedJavaValue();
            final Field field = fieldActor.toJava();
            field.setAccessible(true);
            try {
                field.set(null, value);
            } catch (Throwable throwable) {
                ProgramError.unexpected("could not set field: " + field, throwable);
            }
            return;
        }

        final HostedObjectMirror mirror = new HostedObjectMirror(object);
        final SpecificLayout specificLayout = mirror.classActor().dynamicHub().specificLayout;

        if (arguments.length == 3) {
            final int offset = arguments[1].toInt();
            specificLayout.writeValue(kind, mirror, offset, arguments[2]);
            return;
        }
        ProgramError.check(arguments.length == 4, "wrong number of arguments for PointerStoreBuiltin");
        ProgramError.check(arguments[1].asInt() == ((ArrayLayout) specificLayout).getElementOffsetFromOrigin(0).toInt(), "invalid PointerLoadBuiltin array displacement");
        final int index = arguments[2].asInt();
        mirror.writeElement(kind, index, arguments[3]);
    }
}
