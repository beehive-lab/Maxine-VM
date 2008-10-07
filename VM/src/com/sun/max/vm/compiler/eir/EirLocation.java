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
package com.sun.max.vm.compiler.eir;

import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.value.*;

/**
 * Abstract location for Eir variables.
 * 
 * An Eir variable may be a constant, in which case it may have the immediate or literal location, depending on whether the constant value fits in an immediate operand
 * of the instruction that use the variable. An Eir variable may also be located on the stack, or in a register.
 * @author Bernd Mathiske
 */
public abstract class EirLocation {

    protected EirLocation() {
    }

    public abstract EirLocationCategory category();

    public EirStackSlot asStackSlot() {
        return (EirStackSlot) this;
    }

    public EirLiteral asLiteral() {
        return (EirLiteral) this;
    }

    public EirImmediate asImmediate() {
        return (EirImmediate) this;
    }

    public abstract TargetLocation toTargetLocation();

    public abstract static class Constant extends EirLocation {
        private Value _value;

        public Value value() {
            return _value;
        }

        protected Constant(Value value) {
            _value = value;
        }
    }

    public static final class Undefined extends EirLocation {
        private Undefined() {
        }

        @Override
        public EirLocationCategory category() {
            return EirLocationCategory.UNDEFINED;
        }

        @Override
        public TargetLocation toTargetLocation() {
            return TargetLocation._undefined;
        }
    }

    public static final Undefined UNDEFINED = new Undefined();

}
