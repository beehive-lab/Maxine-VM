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
package com.sun.c1x.asm;

/**
 *
 * @author Thomas Wuerthinger
 *
 */
public class RelocInfo {

    public enum Type {
        none, // Used when no relocation should be generated
        oopType, // embedded oop
        virtualCallType, // a standard inline cache call for a virtual send
        optVirtualCallType, // a virtual call that has been statically bound (i.e., no IC cache)
        staticCallType, // a static send
        staticStubType, // stub-entry for static send (takes care of interpreter case)
        runtimeCallType, // call to fixed external routine
        externalWordType, // reference to fixed external address
        internalWordType, // reference within the current code blob
        sectionWordType, // internal, but a cross-section reference
        pollType, // polling instruction for safepoints
        pollReturnType, // polling instruction for safepoints at return
        breakpointType, // an initialization barrier or safepoint
        yetUnusedType, // Still unused
        yetUnusedType_2, // Still unused
        dataPrefixTag
        // tag for a prefix (carries data arguments)
        // typeMask = 15 // A mask which selects only the above values
    };
}
