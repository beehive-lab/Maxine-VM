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
/*VCSID=695b729f-0c34-4e13-bc0a-fb871309d6ab*/
package com.sun.max.asm.ppc;

import com.sun.max.asm.*;
import com.sun.max.util.*;

/**
 * Special Purpose Registers.
 * 
 * @author Bernd Mathiske
 */
public final class SPR extends AbstractSymbolicArgument {

    private SPR(int value) {
        super(value);
    }

    /**
     * Denotes the Fixed-Point Exception Register.
     */
    public static final SPR XER = new SPR(1);

    /**
     * Denotes the Link Register.
     */
    public static final SPR LR = new SPR(8);

    /**
     * Denotes the Count Register.
     */
    public static final SPR CTR = new SPR(9);

    public static final Symbolizer<SPR> SYMBOLIZER = Symbolizer.Static.initialize(SPR.class);

}
