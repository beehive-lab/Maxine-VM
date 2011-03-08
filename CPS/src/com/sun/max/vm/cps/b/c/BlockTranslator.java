/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.b.c;

import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.cps.*;
import com.sun.max.vm.cps.bir.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.operator.*;
import com.sun.max.vm.cps.ir.IrBlock.Role;
import com.sun.max.vm.type.*;

/**
 * Translates all BIR blocks in a method to CIR blocks.
 *
 * @author Bernd Mathiske
 */
final class BlockTranslator {

    private final BirToCirMethodTranslation translation;

    private BlockTranslator(BirToCirMethodTranslation translation) {
        this.translation = translation;
    }

    private static final StaticMethodActor safepointAndLoadExceptionObject = (StaticMethodActor) ClassMethodActor.fromJava(Classes.getDeclaredMethod(CPSAbstractCompiler.class, "safepointAndLoadExceptionObject"));

    private void scanBlock(BlockState blockState) {
        final BytecodeTranslation visitor = new BytecodeTranslation(blockState, translation);
        final BytecodeScanner bytecodeScanner = new BytecodeScanner(visitor);
        try {
            if (blockState.birBlock().role() == Role.EXCEPTION_DISPATCHER) {
                visitor.completeInvocation(new InvokeStatic(safepointAndLoadExceptionObject), Kind.REFERENCE, CirCall.newArguments(2));
            }
            bytecodeScanner.scan(blockState.birBlock().bytecodeBlock());
        } catch (Throwable e) {
            String dis = "\n\n" + BytecodePrinter.toString(translation.classMethodActor().codeAttribute().cp, blockState.birBlock().bytecodeBlock());
            throw (InternalError) new InternalError("Error while translating " + bytecodeScanner.getCurrentLocationAsString(translation.classMethodActor()) + dis).initCause(e);
        }
        visitor.terminateBlock();
    }

    private void scanReachableBlocks(BlockState blockState) {
        final LinkedList<BlockState> toDo = new LinkedList<BlockState>();
        BlockState state = blockState;
        while (true) {
            scanBlock(state);
            for (BirBlock successorBirBlock : state.birBlock().successors()) {
                final BlockState successor = translation.getBlockStateAt(successorBirBlock.bytecodeBlock().start);
                assert (successor.frame() == null) == (successor.stack() == null);
                if (successor.frame() == null) {
                    successor.setFrame(state.frame().copy());
                    successor.setStack(state.stack().copy());
                    toDo.add(successor);
                }
            }
            if (toDo.isEmpty()) {
                return;
            }
            state = toDo.removeFirst();
        }
    }

    private void scanNormalBlocks() {
        final BlockState root = translation.getBlockStateAt(0);
        root.setFrame(translation.createFrame());
        root.setStack(translation.createStack());
        scanReachableBlocks(root);
    }

    private List<BirBlock> scanExceptionDispatchersWithKnownFrame(List<BirBlock> exceptionDispatchers) {
        final List<BirBlock> remainingExceptionDispatchers = new LinkedList<BirBlock>();
        for (BirBlock exceptionDispatcher : exceptionDispatchers) {
            final BlockState blockState = translation.getBlockStateAt(exceptionDispatcher.bytecodeBlock().start);
            if (blockState.frame() == null) {
                remainingExceptionDispatchers.add(exceptionDispatcher);
            } else {
                assert blockState.stack() == null;
                final JavaStack stack = translation.createStack();
                blockState.setStack(stack);
                scanReachableBlocks(blockState);
            }
        }
        return remainingExceptionDispatchers;
    }

    private void scanExceptionDispatchers() {
        List<BirBlock> exceptionDispatchers = translation.birExceptionDispatchers();
        while (true) {
            final List<BirBlock> remainingExceptionDispatchers = scanExceptionDispatchersWithKnownFrame(exceptionDispatchers);
            if (remainingExceptionDispatchers.size() == exceptionDispatchers.size()) {
                for (BirBlock exceptionDispatcher : remainingExceptionDispatchers) {
                    assert !translation.getBlockStateAt(exceptionDispatcher.bytecodeBlock().start).hasCirBlock();
                    //ProgramWarning.message("unreachable exception dispatcher: " + exceptionDispatcher);
                }
                return;
            }
            exceptionDispatchers = remainingExceptionDispatchers;
        }
    }

    static void run(BirToCirMethodTranslation translation) {
        final BlockTranslator blockScanner = new BlockTranslator(translation);
        blockScanner.scanNormalBlocks();
        blockScanner.scanExceptionDispatchers();

    }
}
