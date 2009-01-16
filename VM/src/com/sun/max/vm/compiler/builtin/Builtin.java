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
public abstract class Builtin extends IrRoutine implements Comparable<Builtin>, ExceptionThrower {

    private int _thrownExceptions = ExceptionThrower.ANY;

    @CONSTANT
    private static IndexedSequence<Builtin> _builtins = new ArrayListSequence<Builtin>();

    public static IndexedSequence<Builtin> builtins() {
        return _builtins;
    }

    private static final GrowableMapping<ClassMethodActor, Builtin> _classMethodActorToBuiltin = HashMapping.createIdentityMapping();

    public static Builtin get(ClassMethodActor classMethodActor) {
        return _classMethodActorToBuiltin.get(classMethodActor);
    }


    /**
     * @return whether this builtin can be meta-evaluated by the compiler under any circumstances
     */
    public boolean hasSideEffects() {
        return true;
    }

    public int thrownExceptions() {
        return _thrownExceptions;
    }

    @PROTOTYPE_ONLY
    public static void registerMethod(ClassMethodActor classMethodActor) {
        if (classMethodActor.isBuiltin()) {
            final BUILTIN builtinAnnotation = classMethodActor.getAnnotation(BUILTIN.class);
            final Builtin builtin = BUILTIN.Static.get(builtinAnnotation.builtinClass());
            _classMethodActorToBuiltin.put(classMethodActor, builtin);
            builtin._thrownExceptions = builtinAnnotation.thrownExceptions();
        }
    }

    @INSPECTED
    @CONSTANT
    private int _serial;

    public final int serial() {
        return _serial;
    }

    private final MethodActor _hostFoldingMethodActor;

    /**
     * @return an alternate folding method to be used exclusively on the host VM
     */
    public MethodActor hostFoldingMethodActor() {
        return _hostFoldingMethodActor;
    }

    public boolean isHostFoldable(IrValue[] arguments) {
        return false;
    }

    public Builtin(Class foldingMethodHolder) {
        super(foldingMethodHolder);
        _serial = _builtins.length();
        final Class<AppendableIndexedSequence<Builtin>> type = null;
        final AppendableIndexedSequence<Builtin> builtins = StaticLoophole.cast(type, _builtins);
        builtins.append(this);
        _hostFoldingMethodActor = getFoldingMethodActor(getClass(), name(), false);
        assert _hostFoldingMethodActor == null || foldingMethodHolder == null || _hostFoldingMethodActor.holder() != ClassActor.fromJava(foldingMethodHolder);
    }

    /**
     * Assigning the serial numbers alphabetically and thus deterministically facilitates matching builtins between VM and Inspector.
     *
     * @see InspectorTransfer
     */
    @PROTOTYPE_ONLY
    public static void initialize() {
        _builtins = IndexedSequence.Static.sort(_builtins, Builtin.class);
        for (int i = 0; i < _builtins.length(); i++) {
            final Builtin builtin = _builtins.get(i);
            builtin._serial = i;
        }
    }

    @PROTOTYPE_ONLY
    public static void register(CompilerScheme compilerScheme) {
        for (ClassActor classActor : ClassRegistry.vmClassRegistry()) {
            for (ClassMethodActor classMethodActor : classActor.localStaticMethodActors()) {
                registerMethod(classMethodActor);
            }
            for (ClassMethodActor classMethodActor : classActor.localVirtualMethodActors()) {
                registerMethod(classMethodActor);
            }
        }
        for (Builtin builtin : _builtins) {
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
        return "<" + foldingMethodActor().name() + ">";
    }

    /**
     * Dispatch visitors that operate on intermediate representations.
     *
     * @param result the IR node that represents the result of a call to this builtin
     * @param arguments IR nodes that represent arguments of a call to this builtin
     */
    public abstract <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments);
}
