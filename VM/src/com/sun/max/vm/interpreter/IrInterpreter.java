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
package com.sun.max.vm.interpreter;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.layout.*;
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

    public static final OptionSet _options = new OptionSet();
    protected static final Option<List<String>> _traceFiltersOption = _options.newStringListOption("filter", null,
                    "trace filters, separated by \",\"");
    protected static final Option<Integer> _traceOption = _options.newIntegerOption("trace", 3,
                    "trace level");
    protected static final Option<Boolean> _jitOption = _options.newBooleanOption("jit", false,
                    "compile and interpret called methods (instead of using reflection)");
    protected static final Option<Boolean> _traceCpuOption = _options.newBooleanOption("tracecpu", false,
                    "include CPU state in the trace");
    protected static final Option<Boolean> _traceStackOption = _options.newBooleanOption("tracestack", false,
                    "include full stack frame state in the trace");

    static {
        _options.loadSystemProperties(PROPERTY_PREFIX);
    }

    protected IrInterpreter() {
    }

    /**
     * Executes a given method with a set of arguments on the interpreter. If the method is a constructor, then value in
     * {@code arguments[0]} is updated to reflect the initialized value.
     */
    public abstract Value execute(Method_Type method, Value... arguments) throws InvocationTargetException;

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
                return fieldActor.kind().asValue(field.get(null));
            } catch (Throwable throwable) {
                ProgramError.unexpected("could not read field: " + field, throwable);
            }
        }

        final InterpreterObjectMirror mirror = new InterpreterObjectMirror(object);
        final SpecificLayout specificLayout = mirror.classActor().dynamicHub().specificLayout();

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
            final Object value = fieldActor.kind().convert(arguments[2]).asBoxedJavaValue();
            final Field field = fieldActor.toJava();
            field.setAccessible(true);
            try {
                field.set(null, value);
            } catch (Throwable throwable) {
                ProgramError.unexpected("could not set field: " + field, throwable);
            }
            return;
        }

        final InterpreterObjectMirror mirror = new InterpreterObjectMirror(object);
        final SpecificLayout specificLayout = mirror.classActor().dynamicHub().specificLayout();

        if (arguments.length == 3) {
            final int offset = arguments[1].toInt();
            specificLayout.writeValue(kind, mirror, offset, arguments[2]);
            return;
        }
        ProgramError.check(arguments.length == 4, "wrong number of arguments for PointerLoadBuiltin");
        ProgramError.check(arguments[1].asInt() == ((ArrayLayout) specificLayout).getElementOffsetFromOrigin(0).toInt(), "invalid PointerLoadBuiltin array displacement");
        final int index = arguments[2].asInt();
        mirror.writeElement(kind, index, arguments[3]);
    }
}
