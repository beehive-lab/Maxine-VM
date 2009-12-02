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
package test.com.sun.max.vm.compiler;

import junit.framework.*;

import com.sun.max.platform.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.bir.*;

public class BirCompilerTestSetup extends CompilerTestSetup<BirMethod> {

    public BirCompilerTestSetup(Test test) {
        super(test);
    }

    public static BirGeneratorScheme birGeneratorScheme() {
        return (BirGeneratorScheme) javaPrototype().vmConfiguration().bootCompilerScheme();
    }

    public static BirGenerator birGenerator() {
        return birGeneratorScheme().birGenerator();
    }

    @Override
    public BirMethod translate(ClassMethodActor classMethodActor) {
        return birGenerator().makeIrMethod(classMethodActor);
    }

    @Override
    protected VMConfiguration createVMConfiguration() {
        return VMConfigurations.createStandard(BuildLevel.DEBUG, Platform.host(),
                        new com.sun.max.vm.compiler.b.Package());
    }
}
