/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.xir;

import com.sun.c1x.ci.CiKind;
import com.sun.c1x.ci.CiTarget;

/**
 * This class is a sketch of types supported by Xir.
 *
 * @author Ben L. Titzer
 */
public enum XirType {
    I8,
    I16,
    I32,
    I64,
    R8,
    R16,
    R32,
    R64,
    X8,
    X16,
    X32,
    X64,
    VOID;

    public static XirType fromKind(CiKind kind, CiTarget target) {
        switch (kind) {
            case Boolean: return I8;
            case Byte: return I8;
            case Short: return I16;
            case Char: return I16;
            case Int: return I32;
            case Long: return I64;
            case Float: return I32;
            case Double: return I64;
            case Void: return VOID;
            case Object:
                switch (target.referenceSize) {
                    case 1: return R8;
                    case 2: return R16;
                    case 4: return R32;
                    case 8: return R64;
                }
                throw new Error("invalid target reference size");
            case Jsr:
            case Word:
                switch (target.arch.wordSize) {
                    case 1: return I8;
                    case 2: return I16;
                    case 4: return I32;
                    case 8: return I64;
                }
                throw new Error("invalid target word size");
            case Illegal: return VOID;
        }
        throw new Error("should not reach here");
    }
}
