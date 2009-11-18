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
package com.sun.max.vm.code;

import com.sun.max.lang.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * A constant modifier is a helper for modifying target code.
 * The modifier is specific to an annotated target code and can be used to
 * modify copies of that target code to adapt the code to a different constant value.
 * Currently used by the template-based JIT.
 *
 * @author Laurent Daynes
 */
public class ConstantModifier extends InstructionModifier {
    /**
     * Original value of the constant.
     */
    private final Value constantValue;

    public ConstantModifier(int position, int size, Value value) {
        super(position, size);
        constantValue = value;
    }

    public Kind kind() {
        return constantValue.kind();
    }

    public WordWidth signedEffectiveWidth() {
        return constantValue.signedEffectiveWidth();
    }

    public Value getConstantValue() {
        return constantValue;
    }
}
