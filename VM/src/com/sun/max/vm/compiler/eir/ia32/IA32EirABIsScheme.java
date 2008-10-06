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
/*VCSID=0ced081c-bc29-4ff0-8460-57be093bf6f1*/
package com.sun.max.vm.compiler.eir.ia32;

import com.sun.max.collect.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.eir.*;

/**
 * @author Bernd Mathiske
 */
public abstract class IA32EirABIsScheme extends EirABIsScheme<IA32EirRegister> {

    protected IA32EirABIsScheme(VMConfiguration vmConfiguration,
                                 EirABI<IA32EirRegister> javaABI,
                                 EirABI<IA32EirRegister> nativeABI,
                                 EirABI<IA32EirRegister> j2cFunctionABI,
                                 EirABI<IA32EirRegister> c2iFunctionABI,
                                 EirABI<IA32EirRegister> trampolineABI,
                                 EirABI<IA32EirRegister> templateABI,
                                 EirABI<IA32EirRegister> treeABI) {
        super(vmConfiguration, javaABI, nativeABI, j2cFunctionABI, c2iFunctionABI, trampolineABI, templateABI, treeABI);
    }

    @Override
    public Pool<IA32EirRegister> registerPool() {
        return IA32EirRegister.pool();
    }

    @Override
    public IA32EirRegister safepointLatchRegister() {
        return (IA32EirRegister) javaABI().safepointLatchRegister();
    }
}
