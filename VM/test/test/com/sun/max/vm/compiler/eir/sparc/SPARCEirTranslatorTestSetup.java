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
package test.com.sun.max.vm.compiler.eir.sparc;

import junit.framework.*;
import test.com.sun.max.vm.compiler.*;

import com.sun.max.asm.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.sparc.*;
import com.sun.max.vm.interpreter.eir.*;
import com.sun.max.vm.interpreter.eir.sparc.*;
import com.sun.max.vm.prototype.*;

/**
 * @author Laurent Daynes
 */
public class SPARCEirTranslatorTestSetup extends CompilerTestSetup<EirMethod> {

    public SPARCEirTranslatorTestSetup(Test test) {
        super(test);
        System.setProperty(Prototype.ENDIANNESS_PROPERTY, Endianness.BIG.name());
        System.setProperty(Prototype.OPERATING_SYSTEM_PROPERTY, OperatingSystem.SOLARIS.name());
        System.setProperty(Prototype.WORD_WIDTH_PROPERTY, String.valueOf(WordWidth.BITS_64.numberOfBits));
        System.setProperty(Prototype.INSTRUCTION_SET_PROPERTY, InstructionSet.SPARC.name());
    }

    public static SPARCEirGeneratorScheme eirGeneratorScheme() {
        return (SPARCEirGeneratorScheme) javaPrototype().vmConfiguration().compilerScheme();
    }

    public static SPARCEirGenerator eirGenerator() {
        return eirGeneratorScheme().eirGenerator();
    }
    @Override
    protected EirInterpreter createInterpreter() {
        return new SPARCEirInterpreter(eirGenerator());
    }

    @Override
    protected VMConfiguration createVMConfiguration() {
        return VMConfigurations.createStandard(BuildLevel.DEBUG, Platform.host().constrainedByInstructionSet(InstructionSet.SPARC), new com.sun.max.vm.compiler.b.c.d.e.sparc.Package());
    }

    @Override
    public EirMethod translate(ClassMethodActor classMethodActor) {
        return eirGenerator().makeIrMethod(classMethodActor);
    }
}
