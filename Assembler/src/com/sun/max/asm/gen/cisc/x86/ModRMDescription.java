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
/*VCSID=4850c5f4-3904-4dc1-bd37-ef486e453d9b*/
package com.sun.max.asm.gen.cisc.x86;

import com.sun.max.asm.gen.*;
import com.sun.max.collect.*;

/**
 * @author Bernd Mathiske
 */
public final class ModRMDescription extends InstructionDescription {

    private final ModRMGroup.Opcode _opcode;
    private final String _name;

    public ModRMDescription(ModRMGroup.Opcode opcode, String name, MutableSequence<Object> specifications) {
        super(specifications);
        _opcode = opcode;
        _name = name;
    }

    public ModRMGroup.Opcode opcode() {
        return _opcode;
    }

    public String name() {
        return _name;
    }

}
