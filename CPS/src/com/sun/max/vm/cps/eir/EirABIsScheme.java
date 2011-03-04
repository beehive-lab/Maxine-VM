/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.cps.eir;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;

/**
 * Eir ABI aggregate.
 *
 * @author Bernd Mathiske
 */
public abstract class EirABIsScheme<EirRegister_Type extends EirRegister> extends AbstractVMScheme implements VMScheme {

    public final EirABI<EirRegister_Type> javaABI;
    public final EirABI<EirRegister_Type> templateABI;

    /**
     * The ABI for a {@code native} method annotated with {@link C_FUNCTION}. If more than one compiler scheme is in use
     * with differing calling conventions, frame adapters need to be generated for such methods.
     */
    public final EirABI<EirRegister_Type> j2cFunctionABI;

    /**
     * The ABI for a Java method than is only called from native code. These are all the methods annotated
     * with {@link VM_ENTRY_POINT}. These methods only need a single entry point and have no frame adapter.
     */
    public final EirABI<EirRegister_Type> c2jFunctionABI;

    public final EirABI<EirRegister_Type> nativeABI;
    public final EirABI<EirRegister_Type> treeABI;

    public abstract EirRegister_Type safepointLatchRegister();

    /**
     *
     * @param javaABI
     * @param nativeABI the ABI
     * @param j2cFunctionABI ABI for {@linkplain C_FUNCTION VM exit} methods
     * @param c2jFunctionABI ABI for {@linkplain VM_ENTRY_POINT VM entry} methods
     * @param trampolineABI
     * @param templateABI
     * @param treeABI abi for tree calls.
     */
    @HOSTED_ONLY
    protected EirABIsScheme(EirABI<EirRegister_Type> javaABI,
                            EirABI<EirRegister_Type> nativeABI,
                            EirABI<EirRegister_Type> j2cFunctionABI,
                            EirABI<EirRegister_Type> c2jFunctionABI,
                            EirABI<EirRegister_Type> templateABI,
                            EirABI<EirRegister_Type> treeABI
                            ) {
        this.javaABI = javaABI;
        this.templateABI = templateABI;
        this.j2cFunctionABI = j2cFunctionABI;
        this.c2jFunctionABI = c2jFunctionABI;
        this.nativeABI = nativeABI;
        this.treeABI = treeABI;
        assert Utils.indexOfIdentical(c2jFunctionABI.calleeSavedRegisters(), safepointLatchRegister()) != -1;
    }

    /**
     * Gets the ABI for a given method.
     *
     * @param classMethodActor the method for which the calling conventions are being requested
     * @return
     */
    public EirABI getABIFor(ClassMethodActor classMethodActor) {
        final MethodActor compilee = classMethodActor.compilee();
        if (compilee.isVmEntryPoint()) {
            return c2jFunctionABI;
        }
        if (compilee.isCFunction()) {
            return j2cFunctionABI;
        }
        if (compilee.isTemplate()) {
            return templateABI;
        }
        return javaABI;
    }

    public abstract Pool<EirRegister_Type> registerPool();
}
