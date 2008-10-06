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
/*VCSID=5fa44f5c-ab2e-47da-957c-af5876b0075e*/
package com.sun.max.vm.compiler.eir.amd64;

import com.sun.max.collect.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.eir.*;

/**
 * @author Bernd Mathiske
 */
public abstract class AMD64EirABIsScheme extends EirABIsScheme<AMD64EirRegister> {

    protected AMD64EirABIsScheme(VMConfiguration vmConfiguration,
                    EirABI<AMD64EirRegister> javaABI,
                    EirABI<AMD64EirRegister> nativeABI,
                    EirABI<AMD64EirRegister> j2cFunctionABI,
                    EirABI<AMD64EirRegister> c2jFunctionABI,
                    EirABI<AMD64EirRegister> trampolineABI,
                    EirABI<AMD64EirRegister> templateABI,
                    EirABI<AMD64EirRegister> treeABI) {
        super(vmConfiguration, javaABI, nativeABI, j2cFunctionABI, c2jFunctionABI, trampolineABI, templateABI, treeABI);
    }

    @Override
    public Pool<AMD64EirRegister> registerPool() {
        return AMD64EirRegister.pool();
    }

    @Override
    public AMD64EirRegister safepointLatchRegister() {
        return (AMD64EirRegister) javaABI().safepointLatchRegister();
    }
}
