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
/*VCSID=0af0abb6-3046-4429-b2fa-4c255a08e4f3*/
package com.sun.max.vm.compiler.dir;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.ir.*;

/**
 * @author Bernd Mathiske
 */
public abstract class DirGenerator extends IrGenerator<DirGeneratorScheme, DirMethod> {

    public DirGenerator(DirGeneratorScheme dirGeneratorScheme) {
        super(dirGeneratorScheme, "DIR");
    }

    @Override
    public DirMethod createIrMethod(ClassMethodActor classMethodActor) {
        // DirMethods are not cached here.
        final DirMethod dirMethod = new DirMethod(classMethodActor);
        notifyAllocation(dirMethod);
        return dirMethod;
    }
}
