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
package test.com.sun.max.vm.compiler.eir.amd64;

import junit.framework.*;
import test.com.sun.max.vm.compiler.*;

import com.sun.max.asm.*;
import com.sun.max.platform.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.amd64.*;
import com.sun.max.vm.interpreter.eir.*;
import com.sun.max.vm.interpreter.eir.amd64.*;

/**
 * @author Bernd Mathiske
 */
public class AMD64EirTranslatorTestSetup extends CompilerTestSetup<EirMethod> {

    public AMD64EirTranslatorTestSetup(Test test) {
        super(test);
    }

    public static AMD64EirGeneratorScheme eirGeneratorScheme() {
        return (AMD64EirGeneratorScheme) javaPrototype().vmConfiguration().compilerScheme();
    }

    public static AMD64EirGenerator eirGenerator() {
        return eirGeneratorScheme().eirGenerator();
    }

    @Override
    public EirMethod translate(ClassMethodActor classMethodActor) {
        return eirGenerator().makeIrMethod(classMethodActor);
    }

    @Override
    protected EirInterpreter createInterpreter() {
        return new AMD64EirInterpreter(eirGenerator());
    }

    @Override
    protected VMConfiguration createVMConfiguration() {
        return VMConfigurations.createStandard(BuildLevel.DEBUG, Platform.host().constrainedByInstructionSet(InstructionSet.AMD64), new com.sun.max.vm.compiler.b.c.d.e.amd64.Package());
    }
}
