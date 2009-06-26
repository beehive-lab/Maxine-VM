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

import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.adaptive.*;
import com.sun.max.vm.compiler.target.*;

/**
 * A place holder class until the VM's adaptive compilation scheme understands about interpreted methods.
 * @see MethodState
 * @see AdaptiveMethodState
 *
 * @author Simon Wilkinson
 */
public class InterpretedMethodState extends MethodState {

    protected InterpretedMethodState(ClassMethodActor classMethodActor, int initialHistoryLength) {
        super(classMethodActor, initialHistoryLength);
    }

    public static InterpretedMethodState makeMethodState(ClassMethodActor classMethodActor) {
        synchronized (classMethodActor) {
            InterpretedMethodState methodState = (InterpretedMethodState) classMethodActor.methodState();
            if (methodState == null) {
                methodState = new InterpretedMethodState(classMethodActor, 1);
                classMethodActor.setMethodState(methodState);
            }
            return methodState;
        }
    }

    public void addInterprettedTargetMethod(InterpretedTargetMethod newTargetMethod) {
        addTargetMethod(newTargetMethod);
    }

    @Override
    public TargetMethod currentTargetMethod() {
        return targetMethodHistory()[0];
    }

    @Override
    public TargetMethod currentTargetMethod(CompilationDirective compilationDirective) {
        ProgramError.unexpected();
        return null;
    }
}
