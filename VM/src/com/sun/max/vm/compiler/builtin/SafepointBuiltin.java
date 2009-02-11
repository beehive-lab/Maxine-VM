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

import com.sun.max.annotate.*;
import com.sun.max.vm.compiler.*;

/**
 * @author Bernd Mathiske
 */
public abstract class SafepointBuiltin extends SpecialBuiltin {

    protected SafepointBuiltin() {
        super(SafepointBuiltin.class);
    }

    @Override
    public int reasonsMayStop() {
        return Stoppable.SAFEPOINT;
    }

    @BUILTIN(builtinClass = SoftSafepoint.class)
    public static void softSafepoint() {
    }

    public static final class SoftSafepoint extends SafepointBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 0;
            visitor.visitSoftSafepoint(this, result, arguments);
        }
        public static final SoftSafepoint BUILTIN = new SoftSafepoint();
    }

    @BUILTIN(builtinClass = HardSafepoint.class)
    public static void hardSafepoint() {
    }

    public static final class HardSafepoint extends SafepointBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 0;
            visitor.visitHardSafepoint(this, result, arguments);
        }
        public static final HardSafepoint BUILTIN = new HardSafepoint();
    }
}
