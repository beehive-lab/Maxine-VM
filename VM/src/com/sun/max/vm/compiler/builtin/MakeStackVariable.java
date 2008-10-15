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
package com.sun.max.vm.compiler.builtin;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.reference.*;

/**
 * A mechanism for forcing a value onto the stack and obtaining the address of the stack slot. If the first parameter to
 * this built-in is a lvalue (i.e. something that can be assigned to), then the compiler must ensure the variable is
 * allocated on the stack. Otherwise, it must make a copy of the rvalue (i.e. a constant) and ensure the copy is
 * allocated on the stack.
 * 
 * @author Doug Simon
 */
public class MakeStackVariable extends SpecialBuiltin {

    protected MakeStackVariable() {
        super(null);
    }

    @Override
    public boolean isFoldable(IrValue[] arguments) {
        return false;
    }

    @Override
    public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
        visitor.visitMakeStackVariable(this, result, arguments);
    }

    public static final MakeStackVariable BUILTIN = new MakeStackVariable();

    /**
     * A key for identifying stack-located variables in {@linkplain TargetMethod compiled methods}. Any given key will
     * identify at most one stack located variable per compiled method but the same key can be used to identify
     * variables in more than one method.
     * <p>
     * A compiler {@linkplain StackVariable#record(TargetMethod, int) records} the offset of a variable created by a
     * call to one of the {@code createStackVariable} builtins that take a second parameter of type
     * {@link StackVariable}. The variable's offset is relative to the pointer used to access the stack
     * based local variables or parameters of the method.
     */
    public static final class StackVariable {

        private final String _name;

        private StackVariable(String name) {
            _name = name;
        }

        public static StackVariable create(String name) {
            return new StackVariable(name);
        }

        /*
         * Must not use identity hashing, because the inspector needs to have the same view as the target VM on this:
         */
        private final Map<ClassMethodActor, Integer> _stackOffsetPerTargetMethod = new HashMap<ClassMethodActor, Integer>();

        @PROTOTYPE_ONLY
        private static class ConflictDetectionMap extends HashMap<ClassMethodActor,  List<StackVariable>> {

            /**
             * Checks the offset of any previously recorded stack variable for a given method against the offset
             * of a new stack variable. A {@linkplain ProgramWarning warning} is issued if any two offsets match.
             */
            synchronized void check(ClassMethodActor key, StackVariable stackVariable, int offset) {
                List<StackVariable> existingStackVariables = get(key);
                if (existingStackVariables == null) {
                    existingStackVariables = new LinkedList<StackVariable>();
                    put(key, existingStackVariables);
                }
                for (StackVariable existingStackVariable : existingStackVariables) {
                    final Integer existingStackVariableOffset = existingStackVariable._stackOffsetPerTargetMethod.get(key);
                    if (existingStackVariableOffset != null) {
                        if (offset == existingStackVariableOffset) {
                            ProgramWarning.message(existingStackVariable + " and " + stackVariable + " both have the same offset (" + offset + ") in " + key);
                        }
                    }
                }
                existingStackVariables.add(stackVariable);
            }
        }

        @PROTOTYPE_ONLY
        private static final ConflictDetectionMap _conflictDetectionMap = new ConflictDetectionMap();

        /**
         * Records the frame-based offset of the variable identified by this key in the frame of a given compiled method.
         */
        public void record(TargetMethod targetMethod, int offset) {
            final ClassMethodActor key = targetMethod.classMethodActor();
            if (MaxineVM.isPrototyping()) {
                _conflictDetectionMap.check(key, this, offset);
            }
            final Integer oldOffset = _stackOffsetPerTargetMethod.put(key, offset);
            assert oldOffset == null || oldOffset.intValue() == offset;
        }

        /**
         * Gets the address of the variable identified by this key in the frame a given compiled method.
         * 
         * @param namedVariablesBasePointer the stack frame address that is the base for all stack variable's accessed via this mechanism
         */
        public Address address(TargetMethod targetMethod, Pointer namedVariablesBasePointer) {
            final Integer offset = _stackOffsetPerTargetMethod.get(targetMethod.classMethodActor());
            assert offset != null;
            return namedVariablesBasePointer.plus(offset);
        }

        @Override
        public String toString() {
            return _name;
        }
    }

    @BUILTIN(builtinClass = MakeStackVariable.class)
    public static native Address makeStackVariable(Reference value);

    /**
     * Forces the register allocator to put a given value on the stack (as opposed to in a register). The given key is
     * only live (i.e. a stack frame inspector can rely on it to read the value of the stack variable) at certain points
     * in the method. Specifically, it's valid only when either the address returned by the built-in is live or
     * {@code value} is live. For example:
     * 
     * <pre>
     *     1:    makeStackVariable(42, key1);
     * </pre>
     * 
     * In this code, {@code key1} is invalid immediately after the machine code sequence generated for line 1.
     * <p>
     * In this code sequence:
     * 
     * <pre>
     *     1:    long v = 42;
     *     2:    makeStackVariable(v, key1);
     *     3:    foo(v);
     * </pre>
     * 
     * {@code key1} is only guaranteed to be valid in line 3 (assuming there are no further uses of {@code v}).
     * <p>
     * In the following sequence, {@code key1} is valid in lines 3 and 4.
     * 
     * <pre>
     *     1:    long v = 42;
     *     2:    Address vAlias = makeStackVariable(v, key1);
     *     3:    foo(v);
     *     4:    bar(vAlias);
     * </pre>
     * 
     * @param value
     *                a value that is to be stack resident. If {@code value} is a lvalue, then the register allocator
     *                guarantees that it will be allocated on the stack. Otherwise, a stack slot is allocated and
     *                initialized with {@code value}.
     * @param key
     *                an object that can be used to access the value when inspecting the stack frame off the method
     *                containing this built-in call. This object must be a compile time constant. That is, it must be a
     *                {@code static} field that is either marked {@code final} or has {@link CONSTANT} or
     *                {@link CONSTANT_WHEN_NOT_ZERO} applied to its definition.
     * @return the address of the stack slot where {@code value} resides
     */
    @BUILTIN(builtinClass = MakeStackVariable.class)
    public static native Pointer makeStackVariable(byte value, StackVariable key);

    @BUILTIN(builtinClass = MakeStackVariable.class)
    public static native Pointer makeStackVariable(int value, StackVariable key);

    /**
     * @see #makeStackVariable(int, com.sun.max.vm.compiler.builtin.MakeStackVariable.StackVariable)
     */
    @BUILTIN(builtinClass = MakeStackVariable.class)
    public static native Pointer makeStackVariable(float value, StackVariable key);

    /**
     * @see #makeStackVariable(int, com.sun.max.vm.compiler.builtin.MakeStackVariable.StackVariable)
     */
    @BUILTIN(builtinClass = MakeStackVariable.class)
    public static native Pointer makeStackVariable(long value, StackVariable key);

    /**
     * @see #makeStackVariable(int, com.sun.max.vm.compiler.builtin.MakeStackVariable.StackVariable)
     */
    @BUILTIN(builtinClass = MakeStackVariable.class)
    public static native Pointer makeStackVariable(double value, StackVariable key);

    /**
     * @see #makeStackVariable(int, com.sun.max.vm.compiler.builtin.MakeStackVariable.StackVariable)
     */
    @BUILTIN(builtinClass = MakeStackVariable.class)
    public static native Pointer makeStackVariable(Reference value, StackVariable key);

    /**
     * @see #makeStackVariable(int, com.sun.max.vm.compiler.builtin.MakeStackVariable.StackVariable)
     */
    @BUILTIN(builtinClass = MakeStackVariable.class)
    public static native Pointer makeStackVariable(Word value, StackVariable key);
}
