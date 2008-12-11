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
package com.sun.max.vm.compiler.cir;

import com.sun.max.lang.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.cir.transform.*;

/**
 * Procedure application.
 * <p>
 * In a {@link CirPrinter trace}, procedure application is indicated
 * by an argument list in parentheses.
 *
 * @author Bernd Mathiske
 */
public final class CirCall extends CirNode {

    private CirValue _procedure;
    private CirValue[] _arguments;
    private BytecodeLocation _bytecodeLocation;
    private CirJavaFrameDescriptor _javaFrameDescriptor;

    public CirCall() {
    }

    public CirCall(CirValue procedure, CirValue... arguments) {
        setProcedure(procedure, null);
        setArguments(arguments);
    }

    public void setBytecodeLocation(BytecodeLocation bytecodeLocation) {
        _bytecodeLocation = bytecodeLocation;
    }

    /**
     * Sets the procedure that is the target of this call.
     *
     * @param procedure the target procedure
     * @param location the location of the VM bytecode instruction modeled by the call. This will always be a control
     *            transfer instruction (including method invocations).
     */
    public void setProcedure(CirValue procedure, BytecodeLocation location) {
        _procedure = procedure;
        _bytecodeLocation = location;
    }

    public void setProcedure(CirValue procedure) {
        _procedure = procedure;
    }

    public CirValue procedure() {
        return _procedure;
    }

    public void setArguments(CirValue... arguments) {
        _arguments = arguments;
        assert arguments.getClass() == CirValue[].class;
    }

    public boolean hasArguments() {
        return _arguments != null;
    }

    public void setArgument(int index, CirValue value) {
        assert value != null;
        _arguments[index] = value;
    }

    public CirValue[] arguments() {
        return _arguments;
    }

    public void removeArgument(int index) {
        _arguments = Arrays.remove(CirValue.class, _arguments, index);
    }

    /**
     * @return the location of the JVM bytecode instruction modeled by this call or null if this call does not model a
     *         JVM bytecode instruction
     */
    public BytecodeLocation bytecodeLocation() {
        return _bytecodeLocation;
    }

    public CirJavaFrameDescriptor javaFrameDescriptor() {
        return _javaFrameDescriptor;
    }

    public void setJavaFrameDescriptor(CirJavaFrameDescriptor javaFrameDescriptor) {
        _javaFrameDescriptor = javaFrameDescriptor;
    }

    public void clearJavaFrameDescriptorIfNotNeeded() {
        if (_procedure instanceof CirProcedure) {
            if (_procedure instanceof CirRoutine) {
                final CirRoutine routine = (CirRoutine) _procedure;
                if (routine.needsJavaFrameDescriptor()) {
                    return;
                }
            }
            _javaFrameDescriptor = null;
        }
    }

    public void assign(CirCall call) {
        _procedure = call._procedure;
        _arguments = call._arguments;
        _bytecodeLocation = call._bytecodeLocation;
        _javaFrameDescriptor = call._javaFrameDescriptor;
    }

    public boolean isFoldable() {
        return false;
    }

    public boolean isNative() {
        return _bytecodeLocation != null && _bytecodeLocation.isNativeCall();
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
                        !areValuesEqual(thisJavaFrameDescriptor.locals(), otherJavaFrameDescriptor.locals(), renaming) ||
                        !areValuesEqual(thisJavaFrameDescriptor.stackSlots(), otherJavaFrameDescriptor.stackSlots(), renaming)) {
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

    private transient int _hashcode = 0;
    private static int _hashcodeCounter = 0;

    @Override
    public int hashCode() {
        if (_hashcode == 0) {
            _hashcode = _hashcodeCounter++;
            if (_hashcode == 0) {  /* overflow */
                return hashCode(); /* try again */
            }
        }
        return _hashcode;
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
