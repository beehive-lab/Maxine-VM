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
package com.sun.max.vm.compiler.cps.b.c;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.cps.bir.*;
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

    private void scanBlock(BlockState blockState) {
        final BytecodeTranslation visitor = new BytecodeTranslation(blockState, translation);
        final BytecodeScanner bytecodeScanner = new BytecodeScanner(visitor);
        try {
            bytecodeScanner.scan(blockState.birBlock().bytecodeBlock());
        } catch (RuntimeException runtimeException) {
            throw (InternalError) new InternalError("Error while translating " + bytecodeScanner.getCurrentLocationAsString(translation.classMethodActor())).initCause(runtimeException);
        } catch (Error error) {
            throw (InternalError) new InternalError("Error while translating " + bytecodeScanner.getCurrentLocationAsString(translation.classMethodActor())).initCause(error);
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

    private Sequence<BirBlock> scanExceptionDispatchersWithKnownFrame(Sequence<BirBlock> exceptionDispatchers) {
        final AppendableSequence<BirBlock> remainingExceptionDispatchers = new LinkSequence<BirBlock>();
        for (BirBlock exceptionDispatcher : exceptionDispatchers) {
            final BlockState blockState = translation.getBlockStateAt(exceptionDispatcher.bytecodeBlock().start);
            if (blockState.frame() == null) {
                remainingExceptionDispatchers.append(exceptionDispatcher);
            } else {
                assert blockState.stack() == null;
                final JavaStack stack = translation.createStack();
                // there must be exactly one object on the stack here: the throwable
                stack.push(Kind.REFERENCE);
                blockState.setStack(stack);

                scanReachableBlocks(blockState);
            }
        }
        return remainingExceptionDispatchers;
    }

    private void scanExceptionDispatchers() {
        Sequence<BirBlock> exceptionDispatchers = translation.birExceptionDispatchers();
        while (true) {
            final Sequence<BirBlock> remainingExceptionDispatchers = scanExceptionDispatchersWithKnownFrame(exceptionDispatchers);
            if (remainingExceptionDispatchers.length() == exceptionDispatchers.length()) {
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
