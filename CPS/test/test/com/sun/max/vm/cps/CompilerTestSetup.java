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
package test.com.sun.max.vm.cps;

import static com.sun.max.asm.dis.Disassembler.*;
import static com.sun.max.vm.VMConfiguration.*;
import junit.framework.*;
import test.com.sun.max.vm.*;

import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.platform.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.cps.ir.interpreter.*;

public abstract class CompilerTestSetup<Method_Type> extends VmTestSetup {

    private static CompilerTestSetup compilerTestSetup = null;

    public static CompilerTestSetup compilerTestSetup() {
        return compilerTestSetup;
    }

    protected CompilerTestSetup(Test test) {
        super(test);
        compilerTestSetup = this;
    }

    @Override
    protected void chainedSetUp() {
        super.chainedSetUp();
        vmConfig().initializeSchemes(Phase.RUNNING);
        compilerScheme().compileSnippets();
    }

    /**
     * Gets a disassembler for a given target method.
     *
     * @param targetMethod a compiled method whose {@linkplain TargetMethod#code() code} is to be disassembled
     * @return a disassembler for the ISA specific code in {@code targetMethod} or null if no such disassembler is available
     */
    public final Disassembler disassemblerFor(TargetMethod targetMethod) {
        Platform platform = Platform.platform();
        InlineDataDecoder inlineDataDecoder = InlineDataDecoder.createFrom(targetMethod.encodedInlineDataDescriptors());
        return createDisassembler(platform.isa, platform.wordWidth(), targetMethod.codeStart().toLong(), inlineDataDecoder);
    }

    public static CPSCompiler compilerScheme() {
        return CPSCompiler.Static.compiler();
    }

    public abstract Method_Type translate(ClassMethodActor classMethodActor);

    protected IrInterpreter<? extends IrMethod> createInterpreter() {
        return null;
    }

}
