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

package com.sun.max.vm.cps.tir;

import java.util.*;

import com.sun.max.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.cps.b.c.d.*;
import com.sun.max.vm.cps.cir.dir.*;
import com.sun.max.vm.cps.collect.*;
import com.sun.max.vm.cps.dir.*;
import com.sun.max.vm.cps.dir.transform.*;
import com.sun.max.vm.cps.hotpath.compiler.*;
import com.sun.max.vm.type.*;

public class DirTracer {
    private static final Mapping<ClassMethodActor, DirMethod> translationCache = new OpenAddressingHashMapping<ClassMethodActor, DirMethod>();

    /**
     * Translates a {@link Snippet} into Trace IR.
     */
    public static TirInstruction trace(Snippet snippet, TirTrace trace, TirInstruction[] arguments, TirRecorder recorder) {
        return trace((ClassMethodActor) snippet.executable, trace, arguments, recorder);
    }

    public static DirMethod makeDirMethod(ClassMethodActor method) {
        final BcdCompiler compiler = new BcdCompiler();
        final CirToDirTranslator translator = new CirToDirTranslator(compiler);
        return translator.makeIrMethod(method);
    }

    /**
     * Translates a {@link ClassMethodActor} into Trace IR.
     */
    public static TirInstruction trace(ClassMethodActor method, TirTrace trace, TirInstruction[] arguments, TirRecorder recorder) {
        synchronized (DirTracer.class) {
            DirMethod dirMethod = translationCache.get(method);
            if (dirMethod == null) {
                dirMethod = makeDirMethod(method);
                translationCache.put(method, dirMethod);
            }
            assert dirMethod.parameters().length == arguments.length;
            return new DirTracer(dirMethod).trace(trace, arguments, recorder);
        }
    }

    private final DirMethod method;
    private final LinkedIdentityHashSet<DirBlock> tracedBlocks = new LinkedIdentityHashSet<DirBlock>();
    private final Mapping<DirValue, TirInstruction> values = new OpenAddressingHashMapping<DirValue, TirInstruction>();
    private TirInstruction returnInstruction;

    protected DirTracer(DirMethod method) {
        this.method = method;
    }

    private TirInstruction trace(final TirTrace trace, TirInstruction[] arguments, TirRecorder recorder) {
        // Map arguments onto parameters.
        for (int i = 0; i < method.parameters().length; i++) {
            values.put(method.parameters()[i], arguments[i]);
        }

        final List<TirInstruction> path = new ArrayList<TirInstruction>();
        final boolean isTraceable = trace(Utils.first(method.blocks()), path, trace, recorder);

        if (isTraceable) {
            trace.append(path);
        } else {
            final TirDirCall call = new TirDirCall(method, arguments);
            trace.append(call);
            if (call.resultKind() != Kind.VOID) {
                returnInstruction = call;
            }
        }

        return returnInstruction;
    }

    private TirInstruction map(DirValue value) {
        if (value instanceof DirConstant) {
            final DirConstant dirConstant = (DirConstant) value;
            return new TirConstant(dirConstant.value());
        }
        final TirInstruction instruction = values.get(value);
        assert instruction != null;
        return instruction;
    }

    private TirInstruction[] mapMany(DirValue... values) {
        final TirInstruction[] instructions = new TirInstruction[values.length];
        for (int i = 0; i < instructions.length; i++) {
            instructions[i] = map(values[i]);
        }
        return instructions;
    }

    private boolean trace(DirBlock block, final List<TirInstruction> path, final TirTrace trace, final TirRecorder recorder) {
        final MutableInnerClassGlobal<Boolean> isTraceable = new MutableInnerClassGlobal<Boolean>(true);
        tracedBlocks.add(block);

        for (DirInstruction instruction : block.instructions()) {
            if (isTraceable.value() == false) {
                // The block is no longer traceable, give up.
                break;
            }
            instruction.acceptVisitor(new DirAdapter() {
                @Override
                public void visitInstruction(DirInstruction dirInstruction) {
                    isTraceable.setValue(false);
                }

                @Override
                public void visitAssign(DirAssign dirMove) {
                    values.put(dirMove.destination(), map(dirMove.source()));
                }

                @Override
                public void visitBuiltinCall(DirBuiltinCall dirBuiltinCall) {
                    final TirInstruction[] instructions = mapMany(dirBuiltinCall.arguments());
                    final TirBuiltinCall call = new TirBuiltinCall(dirBuiltinCall.builtin(), instructions);
                    if (dirBuiltinCall.builtin().resultKind != Kind.VOID) {
                        assert dirBuiltinCall.result() != null;
                        values.put(dirBuiltinCall.result(), call);
                    }
                    path.add(call);
                }

                @Override
                public void visitReturn(DirReturn dirReturn) {
                    if (dirReturn.returnValue() != DirConstant.VOID) {
                        returnInstruction = map(dirReturn.returnValue());
                    } else {
                        ProgramError.check(method.classMethodActor().resultKind() == Kind.VOID);
                    }
                }

                @Override
                public void visitSwitch(DirSwitch dirSwitch) {
                    // We can only trace control flow that is non-cyclic ...
                    if (tracedBlocks.contains(dirSwitch.targetBlocks()[0])) {
                        isTraceable.setValue(false);
                        return;
                    }

                    // ... and that branches to exit blocks.
                    final Class thrownException = throwsException(dirSwitch.defaultTargetBlock());
                    if (dirSwitch.targetBlocks().length == 1 && thrownException != null) {
                        final TirInstruction operand0 = map(dirSwitch.tag());
                        final TirInstruction operand1 = map(dirSwitch.matches()[0]);
                        final TirGuard guard = new TirGuard(operand0, operand1, dirSwitch.valueComparator(), recorder.takeSnapshot(), trace, thrownException);
                        path.add(guard);
                        isTraceable.setValue(trace(dirSwitch.targetBlocks()[0], path, trace, recorder));
                    } else {
                        // We can't trace control flow.
                        isTraceable.setValue(false);
                    }
                }
            });
        }

        return isTraceable.value();
    }

    /**
     * Checks if the specified block terminates control flow by throwing an exception.
     */
    private static Class throwsException(DirBlock block) {
        Class thrownException = null;
        for (DirInstruction instruction : block.instructions()) {
            if (instruction instanceof DirMethodCall) {
                final DirMethodCall call = (DirMethodCall) instruction;
                final DirMethodValue method = (DirMethodValue) call.method();
                if (method.classMethodActor().isInitializer()) {
                    ProgramError.check(thrownException == null);
                    thrownException = method.classMethodActor().holder().toJava();
                }
                continue;
            }

            if (instruction instanceof DirThrow) {
                ProgramError.check(thrownException != null, "Expected an initializer call.");
                return thrownException;
            }
        }
        return null;
    }

/*
    private TirInstruction translate2(final DirMethod dirMethod, final TirTrace trace, TirInstruction[] arguments) {
        final IndexedSequence<DirBlock> blocks = dirMethod.blocks();
        final MutableInnerClassGlobal<TirInstruction> tirReturn = new MutableInnerClassGlobal<TirInstruction>();

        // Only translate linear sequences of DirInstructions, complex control flow is wrapped around
        // a TirDirCall instruction.
        if (blocks.length() == 1) {
            final DirBlock block = blocks.get(0);
            final GrowableMapping<DirValue, TirInstruction> valueMap = new OpenAddressingHashMapping<DirValue, TirInstruction>();

            // Map arguments onto parameters.
            for (int i = 0; i < dirMethod.parameters().length; i++) {
                valueMap.put(dirMethod.parameters()[i], arguments[i]);
            }

            // Visit block and translate DirInstructions into TirInstructions.
            for (DirInstruction instruction : block.instructions()) {
                instruction.acceptVisitor(new DirAdapter() {
                    private TirInstruction mapValue(DirValue value) {
                        if (value instanceof DirConstant) {
                            DirConstant dirConstant = (DirConstant) value;
                            return new TirConstant(dirConstant.value());
                        } else {
                            TirInstruction tirValue = valueMap.get(value);
                            assert tirValue != null;
                            return tirValue;
                        }
                    }

                    @Override
                    public void visitInstruction(DirInstruction dirInstruction) {
                        ProgramError.unexpected();
                    }
                });
            }
        } else {
            final TirDirCall call = new TirDirCall(dirMethod, arguments);
            if (call.method().classMethodActor().resultKind() != Kind.VOID) {
                tirReturn.setValue(call);
            }
            trace.append(call);
        }
        return tirReturn.value();
    }
    */

}
