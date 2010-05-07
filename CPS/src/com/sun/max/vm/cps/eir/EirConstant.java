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
package com.sun.max.vm.cps.eir;

import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public class EirConstant extends EirValue {

    @Override
    public final boolean isConstant() {
        return true;
    }

    private Value value;

    @Override
    public final Value value() {
        return value;
    }

    public EirConstant(Value value) {
        this.value = value;
    }

    @Override
    public final Kind kind() {
        return value.kind();
    }

    @Override
    public String toString() {
        String s = "<" + value.toString() + ">";
        if (location() != null) {
            s += "@" + location();
        }
        return s;
    }

    public int compareTo(EirConstant other) {
        if (this == other) {
            return 0;
        }
        return value.compareTo(other.value());
    }

    public static final class Reference extends EirConstant {
        private int serial;

        public Reference(Value value, int serial) {
            super(value);
            this.serial = serial;
        }

        @Override
        public int compareTo(EirConstant other) {
            if (this == other) {
                return 0;
            }
            if (other instanceof Reference) {
                final Reference referenceConstant = (Reference) other;
                assert serial != referenceConstant.serial;
                return serial > referenceConstant.serial ? 1 : -1;
            }
            return 1;
        }
    }

}
