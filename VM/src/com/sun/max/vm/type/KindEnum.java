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
package com.sun.max.vm.type;

import com.sun.max.collect.*;
import com.sun.max.vm.value.*;

/**
 * Since enums cannot have type parameters, we see ourselves forced to declare two parallel classes.
 * This one represents kinds as bare enums and 'Kind' represents kinds parametrized
 * with their corresponding 'Value' type.
 * Since operations on kinds usually have type parameters, we have not placed any of them here.
 * 
 * @see Kind
 * 
 * @author Bernd Mathiske
 */
public enum KindEnum {

    VOID {
        @Override
        public Kind<VoidValue> asKind() {
            return Kind.VOID;
        }
    },

    BYTE {
        @Override
        public Kind<ByteValue> asKind() {
            return Kind.BYTE;
        }
    },

    BOOLEAN {
        @Override
        public Kind<BooleanValue> asKind() {
            return Kind.BOOLEAN;
        }
    },

    SHORT {
        @Override
        public Kind<ShortValue> asKind() {
            return Kind.SHORT;
        }
    },

    CHAR {
        @Override
        public Kind<CharValue> asKind() {
            return Kind.CHAR;
        }
    },

    INT {
        @Override
        public Kind<IntValue> asKind() {
            return Kind.INT;
        }
    },

    FLOAT {
        @Override
        public Kind<FloatValue> asKind() {
            return Kind.FLOAT;
        }
    },

    LONG {
        @Override
        public Kind<LongValue> asKind() {
            return Kind.LONG;
        }
    },

    DOUBLE {
        @Override
        public Kind<DoubleValue> asKind() {
            return Kind.DOUBLE;
        }
    },

    WORD {
        @Override
        public Kind<WordValue> asKind() {
            return Kind.WORD;
        }
    },

    REFERENCE {
        @Override
        public Kind<ReferenceValue> asKind() {
            return Kind.REFERENCE;
        }
    };

    public abstract Kind asKind();

    /**
     * Convenient way to get an immutable view of all the values of this enumerated type without paying the cost of
     * an array clone operation.
     */
    public static final IndexedSequence<KindEnum> VALUES = new ArraySequence<KindEnum>(values());
}
