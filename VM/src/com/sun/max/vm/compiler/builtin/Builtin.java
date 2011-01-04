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
package com.sun.max.vm.compiler.builtin;

import java.util.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.reflection.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public abstract class Builtin extends Routine implements Comparable<Builtin>, Stoppable {

    @CONSTANT
    private static List<Builtin> builtins = new ArrayList<Builtin>();

    public static List<Builtin> builtins() {
        return builtins;
    }

    private static final HashMap<ClassMethodActor, Builtin> classMethodActorToBuiltin = new HashMap<ClassMethodActor, Builtin>();

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

    @HOSTED_ONLY
    private static void registerMethod(ClassMethodActor classMethodActor) {
        if (classMethodActor.isBuiltin()) {
            final BUILTIN builtinAnnotation = classMethodActor.getAnnotation(BUILTIN.class);
            final Builtin builtin = BUILTIN.Static.get(builtinAnnotation.value());
            classMethodActorToBuiltin.put(classMethodActor, builtin);
        }
    }

    @INSPECTED
    @CONSTANT
    private int serial;

    public final int serial() {
        return serial;
    }

    /**
     * The executable to be used when running hosted.
     */
    @HOSTED_ONLY
    public final MethodActor hostedExecutable;

    public Builtin(Class executableHolder) {
        super(executableHolder);
        this.serial = builtins.size();
        final Class<List<Builtin>> type = null;
        final List<Builtin> builtinList = Utils.cast(type, builtins);
        builtinList.add(this);
        this.hostedExecutable = getExecutable(getClass(), name, false);
        assert hostedExecutable == null || executableHolder == null || hostedExecutable.holder() != ClassActor.fromJava(executableHolder);
    }

    /**
     * Assigning the serial numbers alphabetically and thus deterministically facilitates matching builtins between VM and Inspector.
     */
    @HOSTED_ONLY
    public static void initialize() {
        Builtin[] result = builtins.toArray(new Builtin[builtins.size()]);
        Arrays.sort(result);
        builtins = Arrays.asList(result);
        for (int i = 0; i < builtins.size(); i++) {
            final Builtin builtin = builtins.get(i);
            builtin.serial = i;
        }
    }

    @HOSTED_ONLY
    public static final HashSet<ClassActor> builtinInvocationStubClasses = new HashSet<ClassActor>();

    @HOSTED_ONLY
    public static void register(CPSCompiler compilerScheme) {
        for (ClassActor classActor : ClassRegistry.BOOT_CLASS_REGISTRY.copyOfClasses()) {
            for (ClassMethodActor classMethodActor : classActor.localStaticMethodActors()) {
                registerMethod(classMethodActor);
            }
            for (ClassMethodActor classMethodActor : classActor.localVirtualMethodActors()) {
                registerMethod(classMethodActor);
            }
        }
        for (Builtin builtin : builtins) {
            // Only the CPS compiler needs to fold builtins while compiling
            if (!builtin.hasSideEffects() && compilerScheme.isBuiltinImplemented(builtin)) {
                if (CPSCompiler.Static.compiler() != null) {
                    InvocationStub stub = builtin.executable.makeInvocationStub();
                    builtinInvocationStubClasses.add(ClassActor.fromJava(stub.getClass()));
                    MaxineVM.registerImageInvocationStub(builtin.executable);
                }
            }
        }
    }

    public int compareTo(Builtin other) {
        return name.compareTo(other.name);
    }

    @Override
    public String toString() {
        return "<" + executable.name + ">";
    }

    /**
     * Dispatch visitors that operate on intermediate representations.
     *
     * @param result the IR node that represents the result of a call to this builtin
     * @param arguments IR nodes that represent arguments of a call to this builtin
     */
    public abstract <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments);
}
