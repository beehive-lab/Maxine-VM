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
package com.sun.max.vm.compiler.eir;

import com.sun.max.collect.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;

/**
 * Eir ABI aggregate.
 *
 * @author Bernd Mathiske
 */
public abstract class EirABIsScheme<EirRegister_Type extends EirRegister> extends AbstractVMScheme implements VMScheme {

    private final EirABI<EirRegister_Type> _javaABI;

    public EirABI javaABI() {
        return _javaABI;
    }

    private final EirABI<EirRegister_Type> _trampolineABI;

    public EirABI trampolineABI() {
        return _trampolineABI;
    }

    private final EirABI<EirRegister_Type> _templateABI;

    public EirABI templateABI() {
        return _templateABI;
    }

    /**
     * ABI for method with native code called from Java (e.g., method annotated with C_FUNCTION).
     * If more than one compiler schemes are used, and they used different calling convention, frame adapter
     * needs to be generated for these.
     */
    private final EirABI<EirRegister_Type> _j2cFunctionABI;
    /**
     * ABI for java method than can only be called from native code. This includes "hook" (i.e., static private method annotated with C_FUNCTION)
     * and JNI function (i.e., method annotated with JNI_FUNCTION). These needs a single entry point and no frame adapter, regardless of how many
     * compiler scheme the VM uses, or their calling convention.
     */
    private final EirABI<EirRegister_Type> _c2jFunctionABI;

    public EirABI cFunctionABI(boolean isNative) {
        return isNative ? _j2cFunctionABI : _c2jFunctionABI;
    }


    public EirABI jniFunctionABI() {
        return _c2jFunctionABI;
    }

    private final EirABI<EirRegister_Type> _nativeABI;

    public EirABI nativeABI() {
        return _nativeABI;
    }

    private final EirABI<EirRegister_Type> _treeABI;

    public EirABI treeABI() {
        return _treeABI;
    }

    public abstract EirRegister_Type safepointLatchRegister();

    /**
     *
     * @param vmConfiguration
     * @param javaABI
     * @param nativeABI
     * @param j2cFunctionABI abi for method annotated as C_FUNCTION and called only from Java methods (e.g., native C_FUNCTION method).
     * @param c2jFunctionABI abi for method called only from native code (e.g., static non-native C_FUNCTION methods, method annotated as JNI_FUNCTION).
     * @param trampolineABI
     * @param templateABI
     * @param treeABI abi for tree calls.
     */
    protected EirABIsScheme(VMConfiguration vmConfiguration,
                            EirABI<EirRegister_Type> javaABI,
                            EirABI<EirRegister_Type> nativeABI,
                            EirABI<EirRegister_Type> j2cFunctionABI,
                            EirABI<EirRegister_Type> c2jFunctionABI,
                            EirABI<EirRegister_Type> trampolineABI,
                            EirABI<EirRegister_Type> templateABI,
                            EirABI<EirRegister_Type> treeABI
                            ) {
        super(vmConfiguration);
        _javaABI = javaABI;
        _trampolineABI = trampolineABI;
        _templateABI = templateABI;
        _j2cFunctionABI = j2cFunctionABI;
        _c2jFunctionABI = c2jFunctionABI;
        _nativeABI = nativeABI;
        _treeABI = treeABI;
        assert _nativeABI.calleeSavedRegisters().contains(safepointLatchRegister());
    }

    public EirABI getABIFor(ClassMethodActor classMethodActor) {
        final MethodActor compilee = classMethodActor.compilee();
        if (compilee.isCFunction()) {
            return cFunctionABI(compilee.isNative());
        }
        if (compilee.isJniFunction()) {
            return _c2jFunctionABI;
        }
        if (compilee.isTemplate()) {
            return _templateABI;
        }
        if (compilee instanceof TrampolineMethodActor) {
            return _trampolineABI;
        }
        return _javaABI;
    }

    public abstract Pool<EirRegister_Type> registerPool();
}
