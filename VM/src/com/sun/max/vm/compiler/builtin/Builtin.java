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
package com.sun.max.vm.compiler.builtin;


import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public abstract class Builtin extends IrRoutine implements Comparable<Builtin>, Stoppable {

    @CONSTANT
    private static IndexedSequence<Builtin> builtins = new ArrayListSequence<Builtin>();

    public static IndexedSequence<Builtin> builtins() {
        return builtins;
    }

    private static final GrowableMapping<ClassMethodActor, Builtin> classMethodActorToBuiltin = HashMapping.createIdentityMapping();

    public static Builtin get(ClassMethodActor classMethodActor) {
        return classMethodActorToBuiltin.get(classMethodActor);
    }


    /**
     * @return whether this builtin can be meta-evaluated by the compiler under any circumstances
     */
    public boolean hasSideEffects() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * This must be overridden by builtins that may cause a runtime exception such as {@link ArithmeticException} or
     * {@link NullPointerException}.
     */
    public int reasonsMayStop() {
        return Stoppable.NONE;
    }

    @PROTOTYPE_ONLY
    public static void registerMethod(ClassMethodActor classMethodActor) {
        if (classMethodActor.isBuiltin()) {
            final BUILTIN builtinAnnotation = classMethodActor.getAnnotation(BUILTIN.class);
            final Builtin builtin = BUILTIN.Static.get(builtinAnnotation.builtinClass());
            classMethodActorToBuiltin.put(classMethodActor, builtin);
        }
    }

    @INSPECTED
    @CONSTANT
    private int serial;

    public final int serial() {
        return serial;
    }

    private final MethodActor hostFoldingMethodActor;

    /**
     * @return an alternate folding method to be used exclusively on the host VM
     */
    public MethodActor hostFoldingMethodActor() {
        return hostFoldingMethodActor;
    }

    public boolean isHostFoldable(IrValue[] arguments) {
        return false;
    }

    public Builtin(Class foldingMethodHolder) {
        super(foldingMethodHolder);
        this.serial = builtins.length();
        final Class<AppendableIndexedSequence<Builtin>> type = null;
        final AppendableIndexedSequence<Builtin> builtinList = StaticLoophole.cast(type, builtins);
        builtinList.append(this);
        this.hostFoldingMethodActor = getFoldingMethodActor(getClass(), name(), false);
        assert hostFoldingMethodActor == null || foldingMethodHolder == null || hostFoldingMethodActor.holder() != ClassActor.fromJava(foldingMethodHolder);
    }

    /**
     * Assigning the serial numbers alphabetically and thus deterministically facilitates matching builtins between VM and Inspector.
     */
    @PROTOTYPE_ONLY
    public static void initialize() {
        builtins = IndexedSequence.Static.sort(builtins, Builtin.class);
        for (int i = 0; i < builtins.length(); i++) {
            final Builtin builtin = builtins.get(i);
            builtin.serial = i;
        }
    }

    @PROTOTYPE_ONLY
    public static void register(BootstrapCompilerScheme compilerScheme) {
        for (ClassActor classActor : ClassRegistry.vmClassRegistry()) {
            for (ClassMethodActor classMethodActor : classActor.localStaticMethodActors()) {
                registerMethod(classMethodActor);
            }
            for (ClassMethodActor classMethodActor : classActor.localVirtualMethodActors()) {
                registerMethod(classMethodActor);
            }
        }
        for (Builtin builtin : builtins) {
            if (!builtin.hasSideEffects() && compilerScheme.isBuiltinImplemented(builtin)) {
                MaxineVM.registerImageInvocationStub(builtin.foldingMethodActor());
            }
        }
    }

    public int compareTo(Builtin other) {
        return name().compareTo(other.name());
    }

    @Override
    public String toString() {
        return "<" + foldingMethodActor().name + ">";
    }

    /**
     * Dispatch visitors that operate on intermediate representations.
     *
     * @param result the IR node that represents the result of a call to this builtin
     * @param arguments IR nodes that represent arguments of a call to this builtin
     */
    public abstract <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments);
}
