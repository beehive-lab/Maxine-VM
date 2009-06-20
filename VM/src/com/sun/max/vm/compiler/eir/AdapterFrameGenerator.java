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

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.type.*;

/**
 * Generic Adapter Frame Generator.
 *
 * @author Laurent Daynes
 */
public abstract class AdapterFrameGenerator<Assembler_Type extends Assembler> {
    private final MethodActor classMethodActor;

    protected final Label _methodEntryPoint = new Label();
    protected final  Label adapterStart = new Label();

    private final Label adapterReturnPoint = new Label();

    public final Label adapterReturnPoint() {
        return adapterReturnPoint;
    }

    public MethodActor classMethodActor() {
        return classMethodActor;
    }

    private final EirABI optimizedABI;

    public EirABI optimizedABI() {
        return optimizedABI;
    }

    private Assembler_Type assembler;

    public Assembler_Type assembler() {
        return assembler;
    }

    private void setAssembler(Assembler_Type assembler) {
        this.assembler = assembler;
    }

    protected AdapterFrameGenerator(MethodActor classMethodActor, EirABI optimizedABI) {
        this.classMethodActor = classMethodActor;
        this.optimizedABI = optimizedABI;
    }


    protected abstract void emit(Kind[] parametersKinds, EirLocation[] parameterLocations, Label adapterReturnPt, Label methodEntryPoint);

    /**
     * Produces the code that resides at the entry point for the caller.
     */
    public abstract void emitPrologue(Assembler_Type asm);
    public abstract void emitEpilogue(Assembler_Type asm);

    /**
     * Specified the label corresponding to the JIT Entry point.
     * Relevant only for those cases where the JIT Entry point is not the end of the frame adapter and is
     * before the entry point of the adapter (so that the adapter doesn't execute JIT-JIT specific code).
     */
    public void setJitEntryPoint(Label jitEntryPoint) {
        throw new UnsupportedOperationException();
    }

    /**
     * Helper method that Indicates whether a method actor is a dynamic trampoline.
     * @param methodActor
     * @return
     */
    protected static boolean isDynamicTrampoline(MethodActor methodActor) {
        return methodActor instanceof TrampolineMethodActor && ((TrampolineMethodActor) methodActor).invocation() != TRAMPOLINE.Invocation.STATIC;
    }

    protected  EirStackSlot.Purpose adapterArgumentPurpose() {
        return EirStackSlot.Purpose.PARAMETER;
    }

    // CLEANUP: this is going to be replaced by emitEpilogue
    public void emitFrameAdapter(Assembler_Type asm) {
        setAssembler(asm);
        final Kind[] parametersKinds = classMethodActor.getParameterKinds();
        final EirLocation[] parameterLocations = optimizedABI.getParameterLocations(adapterArgumentPurpose(), parametersKinds);
        assembler().bindLabel(adapterStart);
        emit(parametersKinds, parameterLocations, adapterReturnPoint, _methodEntryPoint);
    }
}
