/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.compiler.c1x;

import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.lang.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.runtime.*;

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
        link = MethodActor.fromJava(Classes.getDeclaredMethod(Snippets.class, "linkNativeMethod", ClassMethodActor.class));
        enterNative = MethodActor.fromJava(Classes.getDeclaredMethod(Snippets.class, "nativeCallPrologue"));
        enterNativeForC = MethodActor.fromJava(Classes.getDeclaredMethod(Snippets.class, "nativeCallPrologueForC"));
        enterVM = MethodActor.fromJava(Classes.getDeclaredMethod(Snippets.class, "nativeCallEpilogue"));
        enterVMForC = MethodActor.fromJava(Classes.getDeclaredMethod(Snippets.class, "nativeCallEpilogueForC"));
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
