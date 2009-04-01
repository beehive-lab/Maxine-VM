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
package com.sun.max.vm.interpret.dt.amd64;

import com.sun.max.vm.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.interpret.*;
import com.sun.max.vm.interpret.dt.*;
import com.sun.max.vm.jit.amd64.*;
import com.sun.max.vm.template.*;

/**
 * Interpreter bytecode templates for AMD64.
 *
 * (We currently have this architecture specific class as a workaround. See templateTable())
 *
 * @author Simon Wilkinson
 */
public class AMD64DtInterpreterTemplateSet extends DtInterpreterTemplateSet {

    @Override
    protected boolean exclude(Bytecode bytecode) {
        // We can't handle bytecode operands at the moment.
        return BytecodeSizes.lookup(bytecode) != 1;
    }

    @Override
    protected TemplateTable templateTable() {
        // We have to cheat at the moment and grab the JIT compiler's templates.
        // At the moment some dependency stops the templates being built correctly
        // unless the the JIT is in the prototype.
        return ((AMD64JitCompiler) VMConfiguration.target().jitScheme()).peekTemplateTable();
    }
}
