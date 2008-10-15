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
package com.sun.max.vm.compiler.ir;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * IR representations of operands implement this interface to facilitate meta-evaluation.
 *
 * @author Bernd Mathiske
 */
public interface IrValue {

    /**
     * @return whether this is a compile time constant
     */
    boolean isConstant();

    Kind kind();

    /**
     * @return the encapsulated Value or 'null' if none, i.e. this is not a compile time constant,
     */
    Value value();

    public static final class Static {
        private Static() {
        }

        public static Kind[] toKinds(IrValue[] irValues) {
            return Arrays.map(irValues, Kind.class, new MapFunction<IrValue, Kind>() {
                public Kind map(IrValue irValue) {
                    return irValue.kind();
                }
            });
        }

        public static boolean areConstant(IrValue[] irValues) {
            for (IrValue irValue : irValues) {
                if (!irValue.isConstant()) {
                    return false;
                }
            }
            return true;
        }

    }

}
