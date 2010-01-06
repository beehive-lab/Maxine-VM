/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.compiler.target;

import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.program.ProgramError;

/**
 * An adapter is a code stub interposing a call between two methods that have different calling conventions.
 *
 * @author Doug Simon
 */
public class Adapter extends TargetMethod {

    public enum Type {
        JIT2OPT,
        OPT2JIT;
    }

    public final Type type;

    public Adapter(String description, Type type, RuntimeCompilerScheme compilerScheme, TargetABI abi) {
        super(description, compilerScheme, abi);
        this.type = type;
    }


    @Override
    public void forwardTo(TargetMethod newTargetMethod) {
        FatalError.unimplemented();
    }

    @Override
    public void gatherCalls(AppendableSequence<MethodActor> directCalls, AppendableSequence<MethodActor> virtualCalls, AppendableSequence<MethodActor> interfaceCalls) {
    }

    @Override
    public void prepareFrameReferenceMap(int stopIndex, Pointer refmapFramePointer, StackReferenceMapPreparer preparer) {
        // TODO Auto-generated method stub
    }

    @Override
    public void patchCallSite(int callOffset, Word callEntryPoint) {
        FatalError.unimplemented();
    }

    @Override
    public String referenceMapsToString() {
        return null;
    }

    @Override
    public Address throwAddressToCatchAddress(boolean isTopFrame, Address throwAddress, Class<? extends Throwable> throwableClass) {
        throw FatalError.unexpected("Exception occurred in adapter frame stub");
    }

    @Override
    public void traceDebugInfo(IndentWriter writer) {
        // TODO Auto-generated method stub
    }

    @Override
    public void traceExceptionHandlers(IndentWriter writer) {
        // TODO Auto-generated method stub
    }

    @Override
    public byte[] referenceMaps() {
        // TODO Auto-generated method stub
        return null;
    }

    public void prepareReferenceMap(StackFrameWalker.Cursor current, StackFrameWalker.Cursor callee, StackReferenceMapPreparer preparer) {
        throw ProgramError.unexpected();
    }

    public void catchException(StackFrameWalker.Cursor current, StackFrameWalker.Cursor callee, Throwable throwable) {
        throw ProgramError.unexpected();
    }

    public boolean acceptJavaFrameVisitor(StackFrameWalker.Cursor current, StackFrameWalker.Cursor callee, StackFrameVisitor visitor) {
        throw ProgramError.unexpected();
    }

    public void advance(StackFrameWalker.Cursor current) {
        throw ProgramError.unexpected();
    }
}
