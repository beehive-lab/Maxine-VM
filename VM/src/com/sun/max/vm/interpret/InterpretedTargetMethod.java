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
package com.sun.max.vm.interpret;

import com.sun.max.asm.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.stack.*;

/**
 * A collection of objects that represent stub code and auxiliary
 * data structures necessary for interpreting a Java method.
 *
 * @author Simon Wilkinson
 */
public abstract class InterpretedTargetMethod extends TargetMethod {

    public InterpretedTargetMethod(ClassMethodActor classMethodActor) {
        super(classMethodActor);
    }

    @Override
    public abstract JavaStackFrameLayout stackFrameLayout();

    @Override
    public abstract InstructionSet instructionSet();

    @Override
    public boolean areReferenceMapsFinalized() {
        // TODO
        return false;
    }

    @Override
    public void forwardTo(TargetMethod newTargetMethod) {
        // TODO
    }

    @Override
    public void patchCallSite(int callOffset, Word callEntryPoint) {
        // TODO
    }

    @Override
    public int registerReferenceMapSize() {
        // TODO
        return 0;
    }



}
