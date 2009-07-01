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
package com.sun.max.vm.interpret.empty;

import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.interpret.*;

/**
 * Place holder scheme for specifying no interpreter should be included in the VM.
 *
 * (Currently we appear not allowed to have any null VMSchemes.)
 *
 * @author Simon Wilkinson
 */
public class EmptyInterpreterScheme extends AbstractVMScheme implements InterpreterScheme {

    public EmptyInterpreterScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    public TargetMethod makeInterpretedTargetMethod(ClassMethodActor callee) {
        ProgramError.unexpected();
        return null;
    }
}
