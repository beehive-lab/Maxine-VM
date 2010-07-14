/*
 * Copyright (c) 2010 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.compiler.c1x;

import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.snippet.NativeStubSnippet.*;

/**
 * Maxine implementation of {@link RiSnippets}.
 *
 * @author Doug Simon
 */
public class MaxRiSnippets implements RiSnippets {

    public final RiMethod link;
    public final RiMethod enterNative;
    public final RiMethod enterNativeForC;
    public final RiMethod enterVM;
    public final RiMethod enterVMForC;


    public MaxRiSnippets(MaxRiRuntime runtime) {
        link = LinkNativeMethod.SNIPPET.executable;
        enterNative = NativeCallPrologue.SNIPPET.executable;
        enterNativeForC = NativeCallPrologueForC.SNIPPET.executable;
        enterVM = NativeCallEpilogue.SNIPPET.executable;
        enterVMForC = NativeCallEpilogueForC.SNIPPET.executable;
    }

    @Override
    public RiSnippetCall enterNative(RiMethod nativeMethod) {
        ClassMethodActor methodActor = (ClassMethodActor) nativeMethod;
        return new RiSnippetCall(Bytecodes.INVOKESTATIC, methodActor.isCFunction() ? enterNativeForC : enterNative);
    }

    @Override
    public RiSnippetCall enterVM(RiMethod nativeMethod) {
        ClassMethodActor methodActor = (ClassMethodActor) nativeMethod;
        return new RiSnippetCall(Bytecodes.INVOKESTATIC, methodActor.isCFunction() ? enterVMForC : enterVM);
    }

    @Override
    public RiSnippetCall link(RiMethod nativeMethod) {
        ClassMethodActor methodActor = (ClassMethodActor) nativeMethod;
        RiSnippetCall call = new RiSnippetCall(Bytecodes.INVOKESTATIC, link, CiConstant.forObject(methodActor));
        if (!MaxineVM.isHosted() && methodActor.nativeFunction.isLinked()) {
            call.result = CiConstant.forWord(methodActor.nativeFunction.link().asAddress().toLong());
        }
        return call;
    }
}
