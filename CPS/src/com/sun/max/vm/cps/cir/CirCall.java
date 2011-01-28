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
package com.sun.max.vm.cps.cir;

import com.sun.cri.bytecode.*;
import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.cps.cir.variable.*;

/**
 * A CIR call is an application of procedure.
 * <p>
 * In a {@link CirPrinter trace}, procedure application is displayed as:
 *
 * <pre>
 * procedure ( arguments... )
 * </pre>
 *
 * @author Bernd Mathiske
 */
public final class CirCall extends CirNode {

    /**
     * The value that must be used when passing a zero-length array as the value of {@code parameters} to
     * {@link #CirCall(CirValue, CirValue...)} and {@link #setParameters(CirVariable...)}.
     */
    public static final CirValue[] NO_ARGUMENTS = {};

    public static CirValue[] newArguments(int count) {
        return count > 0 ? new CirValue[count] : NO_ARGUMENTS;
    }

    private CirValue procedure;
    private CirValue[] arguments;
    private CirJavaFrameDescriptor javaFrameDescriptor;
    private boolean isNative;

    public CirCall() {
    }

    /**
     * Creates a CIR call node to represent application of a procedure.
     *
     * @param procedure the procedure being applied
     * @param arguments the arguments of this procedure application. If {@code arguments.length == 0}, then the value of
     *            {@code arguments} must be {@link #NO_ARGUMENTS}.
     */
    public CirCall(CirValue procedure, CirValue... arguments) {
        setProcedure(procedure);
        setArguments(arguments);
    }

    /**
     * Sets the procedure that is the target of this call.
     *
     * @param procedure the target procedure
     */
    public void setProcedure(CirValue procedure) {
        this.procedure = procedure;
    }

    public CirValue procedure() {
        return procedure;
    }

    /**
     * Sets the arguments of this CIR call.
     *
     * @param arguments the arguments of this procedure application. If {@code arguments.length == 0}, then the value of
     *            {@code arguments} must be {@link #NO_ARGUMENTS}.
     */
    public void setArguments(CirValue... arguments) {
        assert (arguments.length > 0 && Utils.indexOfIdentical(arguments, null) == -1) || arguments == NO_ARGUMENTS;
        this.arguments = arguments;
        assert arguments.getClass() == CirValue[].class;
    }

    public boolean hasArguments() {
        return arguments != null;
    }

    public void setArgument(int index, CirValue value) {
        assert value != null;
        arguments[index] = value;
    }

    public CirValue[] arguments() {
        return arguments;
    }

    public void removeArgument(int index) {
        if (arguments.length == 1) {
            arguments = NO_ARGUMENTS;
        } else {
            assert arguments.length > 0;
            int newLength = arguments.length - 1;
            CirValue[] newArguments = new CirValue[newLength];
            System.arraycopy(arguments, 0, newArguments, 0, index);
            System.arraycopy(arguments, index + 1, newArguments, index, newLength - index);
            arguments = newArguments;
        }
    }

    public CirJavaFrameDescriptor javaFrameDescriptor() {
        return javaFrameDescriptor;
    }

    public void setJavaFrameDescriptor(CirJavaFrameDescriptor javaFrameDescriptor) {
        this.javaFrameDescriptor = javaFrameDescriptor;
    }

    public void clearJavaFrameDescriptorIfNotNeeded() {
        if (procedure instanceof CirProcedure) {
            if (procedure instanceof CirRoutine) {
                final CirRoutine routine = (CirRoutine) procedure;
                if (Stoppable.Static.canStop(routine)) {
                    return;
                }
            }
            javaFrameDescriptor = null;
        }
    }

    public void assign(CirCall call) {
        procedure = call.procedure;
        arguments = call.arguments;
        javaFrameDescriptor = call.javaFrameDescriptor;
        isNative = call.isNative;
    }

    public boolean isFoldable() {
        return false;
    }

    /**
     * @see #isNative()
     */
    public void setIsNative() {
        isNative = true;
    }

    /**
     * Determines if this is a call to a native function. Note, this does not mean a call to a native method, but the
     * call inside a native method's stub to the actual native code. This will be the translation of the
     * {@link Bytecodes#JNICALL} instruction.
     */
    public boolean isNative() {
        return isNative;
    }

    @Override
    public String toString() {
        return "<CirCall>";
    }

    private boolean areValuesEqual(CirValue[] values1, CirValue[] values2, CirVariableRenaming renaming) {
        if (values1.length != values2.length) {
            return false;
        }
        for (int i = 0; i < values1.length; i++) {
            if (values1[i] != null) {
                if (!values1[i].equals(values2[i], renaming)) {
                    return false;
                }
            } else if (values2[i] != null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object other, CirVariableRenaming renaming) {
        if (other instanceof CirCall) {
            final CirCall call = (CirCall) other;
            if (arguments().length != call.arguments().length ||
                    !procedure().equals(call.procedure(), renaming) ||
                    !areValuesEqual(arguments(), call.arguments(), renaming)) {
                return false;
            }
            CirJavaFrameDescriptor thisJavaFrameDescriptor = javaFrameDescriptor();
            CirJavaFrameDescriptor otherJavaFrameDescriptor = call.javaFrameDescriptor();
            while (thisJavaFrameDescriptor != null) {
                if (otherJavaFrameDescriptor == null ||
                        !areValuesEqual(thisJavaFrameDescriptor.locals, otherJavaFrameDescriptor.locals, renaming) ||
                        !areValuesEqual(thisJavaFrameDescriptor.stackSlots, otherJavaFrameDescriptor.stackSlots, renaming)) {
                    return false;
                }
                thisJavaFrameDescriptor = thisJavaFrameDescriptor.parent();
                otherJavaFrameDescriptor = otherJavaFrameDescriptor.parent();
            }
            return otherJavaFrameDescriptor == null;
        }
        return false;
    }

    @Override
    public boolean equals(Object other) {
        return equals(other, null);
    }

    @RESET
    private int hashcode = 0;
    private static int hashcodeCounter = 0;

    @Override
    public int hashCode() {
        if (hashcode == 0) {
            hashcode = hashcodeCounter++;
            if (hashcode == 0) {  /* overflow */
                return hashCode(); /* try again */
            }
        }
        return hashcode;
    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitCall(this);
    }

    @Override
    public void acceptBlockScopedVisitor(CirBlockScopedVisitor visitor, CirBlock scope) {
        visitor.visitCall(this, scope);
    }

    @Override
    public CirNode acceptTransformation(CirTransformation transformation) {
        return transformation.transformCall(this);
    }

    @Override
    public boolean acceptUpdate(CirUpdate update) {
        return update.updateCall(this);
    }

    @Override
    public boolean acceptPredicate(CirPredicate predicate) {
        return predicate.evaluateCall(this);
    }
}
