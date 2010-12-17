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
package test.com.sun.max.vm.jit.amd64;

import static com.sun.max.platform.Platform.*;
import junit.framework.*;
import test.com.sun.max.vm.cps.amd64.*;
import test.com.sun.max.vm.jit.*;

import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.cps.jit.*;
import com.sun.max.vm.cps.jit.amd64.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.template.*;

/**
 * Test setup for JIT tests on AMD64.
 *
 * @author Laurent Daynes
 */
public class AMD64JITTestSetup extends AMD64TranslatorTestSetup implements JITTestSetup {
    public AMD64JITTestSetup(Test test) {
        super(test);
    }

    @Override
    protected void initializeVM() {
        Platform.set(platform().constrainedByInstructionSet(ISA.AMD64));
        VMConfigurator.installStandard(BuildLevel.DEBUG, CPSCompiler.Static.defaultCPSCompilerPackage());
    }

    public JitCompiler newJitCompiler(TemplateTable templateTable) {
        return new AMD64JitCompiler(templateTable);
    }

    public boolean disassembleCompiledMethods() {
        return true;
    }
}
