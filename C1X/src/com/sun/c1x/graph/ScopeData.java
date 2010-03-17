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
package com.sun.c1x.graph;

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.bytecode.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.value.*;

/**
 * The {@code ScopeData} class represents inlining context when parsing the bytecodes
 * of an inlined method.
 *
 * @author Ben L. Titzer
*/
public class ScopeData {
    // XXX: refactor and split this class into ScopeData, JsrScopeData, and InlineScopeData

    final ScopeData parent;
    // the IR scope
    final IRScope scope;
    // bci-to-block mapping
    final BlockMap blockMap;
    // the bytecode stream
    final BytecodeStream stream;
    // the constant pool
    final RiConstantPool constantPool;
    // whether this scope or any parent scope has exception handlers
    boolean hasHandler;
    // the worklist of blocks, managed like a sorted list
    BlockBegin[] workList;
    // the current position in the worklist
    int workListIndex;
    // maximum inline size for this scope
    int maxInlineSize;

    // Exception handler list
    List<ExceptionHandler> exceptionHandlers;

    // The continuation point for the inline. Currently only used in
    // multi-block inlines, but eventually would like to use this for
    // all inlines for uniformity and simplicity; in this case would
    // get the continuation point from the BlockList instead of
    // fabricating it anew because Invokes would be considered to be
    // BlockEnds.
    BlockBegin continuation;

    // Without return value of inlined method on stack
    ValueStack continuationState;

    // Number of returns seen in this scope
    int numReturns;

    // In order to generate better code while inlining, we currently
    // have to perform an optimization for single-block inlined
    // methods where we continue parsing into the same block. This
    // allows us to perform LVN and folding across inlined scopes.
    BlockBegin cleanupBlock;       // The block to which the return was added
    Instruction cleanupReturnPrev; // Instruction before return instruction
    ValueStack cleanupState;       // State of that block (not yet pinned)

    // We track the destination bci of the jsr only to determine
    // bailout conditions, since we only handle a subset of all of the
    // possible jsr-ret control structures. Recursive invocations of a
    // jsr are disallowed by the verifier. > 0 indicates parsing
    // of a jsr.
    final int jsrEntryBci;
    // We need to track the local variable in which the return address
    // was stored to ensure we can handle inlining the jsr, because we
    // don't handle arbitrary jsr/ret constructs.
    int jsrRetAddrLocal;

    // If we are parsing a jsr, the continuation point for rets
    BlockBegin jsrContinuation;

    final BlockBegin[] jsrDuplicatedBlocks; // blocks that have been duplicated for JSR inlining

    /**
     * Constructs a new ScopeData instance with the specified parent ScopeData.
     * @param parent the parent scope data
     * @param scope the IR scope
     * @param blockMap the block map for this scope
     * @param stream the bytecode stream
     * @param constantPool the constant pool
     */
    public ScopeData(ScopeData parent, IRScope scope, BlockMap blockMap, BytecodeStream stream, RiConstantPool constantPool) {
        this.parent = parent;
        this.scope = scope;
        this.blockMap = blockMap;
        this.stream = stream;
        this.constantPool = constantPool;
        this.jsrEntryBci = -1;
        this.jsrDuplicatedBlocks = null;
        if (parent != null) {
            maxInlineSize = (int) (C1XOptions.MaximumInlineRatio * parent.maxInlineSize());
            if (maxInlineSize < C1XOptions.MaximumTrivialSize) {
                maxInlineSize = C1XOptions.MaximumTrivialSize;
            }
            hasHandler = parent.hasHandler;
        } else {
            maxInlineSize = C1XOptions.MaximumInlineSize;
        }
        List<RiExceptionHandler> handlers = scope.method.exceptionHandlers();
        if (handlers != null && handlers.size() > 0) {
            exceptionHandlers = new ArrayList<ExceptionHandler>(handlers.size());
            for (RiExceptionHandler ch : handlers) {
                ExceptionHandler h = new ExceptionHandler(ch);
                h.setEntryBlock(blockAt(h.handler.handlerBCI()));
                exceptionHandlers.add(h);
            }
            hasHandler = true;
        }
    }

    /**
     * Constructs a new ScopeData instance with the specified parent ScopeData. This constructor variant creates
     * a scope data for parsing a JSR.
     * @param parent the parent scope data
     * @param scope the IR scope
     * @param blockMap the block map for this scope
     * @param stream the bytecode stream
     * @param constantPool the constant pool
     * @param jsrEntryBci the bytecode index of the entrypoint of the JSR
     */
    public ScopeData(ScopeData parent, IRScope scope, BlockMap blockMap, BytecodeStream stream, RiConstantPool constantPool, int jsrEntryBci) {
        this.parent = parent;
        this.scope = scope;
        this.blockMap = blockMap;
        this.stream = stream;
        this.constantPool = constantPool;
        assert jsrEntryBci > 0 : "jsr cannot jump to BCI 0";
        assert parent != null : "jsr must have parent scope";
        this.jsrEntryBci = jsrEntryBci;
        this.jsrDuplicatedBlocks = new BlockBegin[scope.method.codeSize()];
        this.jsrRetAddrLocal = -1;

        maxInlineSize = (int) (C1XOptions.MaximumInlineRatio * parent.maxInlineSize());
        if (maxInlineSize < C1XOptions.MaximumTrivialSize) {
            maxInlineSize = C1XOptions.MaximumTrivialSize;
        }
        hasHandler = parent.hasHandler;
        // duplicate the parent scope's exception handlers, if any
        List<ExceptionHandler> handlers = parent.exceptionHandlers();
        if (handlers != null && handlers.size() > 0) {
            exceptionHandlers = new ArrayList<ExceptionHandler>(handlers.size());
            for (ExceptionHandler ph : handlers) {
                ExceptionHandler h = new ExceptionHandler(ph);
                int handlerBci = h.handler.handlerBCI();
                if (handlerBci >= 0) {
                    // need to duplicate the handler block because it is a "normal" handler
                    h.setEntryBlock(blockAt(handlerBci));
                } else {
                    // don't duplicate the handler block because it is a synchronization handler
                    // that was added by parsing/inlining a synchronized method
                    assert ph.entryBlock().checkBlockFlag(BlockBegin.BlockFlag.DefaultExceptionHandler);
                    h.setEntryBlock(ph.entryBlock());
                }
                exceptionHandlers.add(h);
            }
            assert hasHandler;
        }
    }

    /**
     * Gets the block beginning at the specified bytecode index. Note that this method
     * will clone the block if it the scope data is currently parsing a JSR.
     * @param bci the bytecode index of the start of the block
     * @return the block starting at the specified bytecode index
     */
    public BlockBegin blockAt(int bci) {
        if (jsrDuplicatedBlocks != null) {
            // all blocks in a JSR are duplicated on demand using an internal array,
            // including those for exception handlers in the scope of the method
            // containing the jsr (because those exception handlers may contain ret
            // instructions in some cases).
            BlockBegin block = jsrDuplicatedBlocks[bci];
            if (block == null) {
                BlockBegin p = this.parent.blockAt(bci);
                if (p != null) {
                    BlockBegin newBlock = new BlockBegin(p.bci(), scope.compilation.hir().nextBlockNumber());
                    newBlock.setDepthFirstNumber(p.depthFirstNumber());
                    newBlock.copyBlockFlags(p);
                    jsrDuplicatedBlocks[bci] = newBlock;
                    block = newBlock;
                }
            }
            return block;
        }
        return blockMap.get(bci);
    }

    /**
     * Checks whether this ScopeData has any handlers.
     * @return {@code true} if there are any exception handlers
     */
    public boolean hasHandler() {
        return hasHandler;
    }

    /**
     * Gets the maximum inline size.
     * @return the maximum inline size
     */
    public int maxInlineSize() {
        return maxInlineSize;
    }

    /**
     * Gets the size of the stack at the caller.
     * @return the size of the stack
     */
    public int callerStackSize() {
        ValueStack state = scope.callerState();
        return state == null ? 0 : state.stackSize();
    }

    /**
     * Gets the block continuation for this ScopeData.
     * @return the continuation
     */
    public BlockBegin continuation() {
        return continuation;
    }

    /**
     * Sets the continuation for this ScopeData.
     * @param continuation the continuation
     */
    public void setContinuation(BlockBegin continuation) {
        this.continuation = continuation;
    }

    /**
     * Gets the state at the continuation point.
     * @return the state at the continuation point
     */
    public ValueStack continuationState() {
        return continuationState;
    }

    /**
     * Sets the state at the continuation point.
     * @param state the state at the continuation
     */
    public void setContinuationState(ValueStack state) {
        continuationState = state;
    }

    /**
     * Checks whether this ScopeData is parsing a JSR.
     * @return {@code true} if this scope data is parsing a JSR
     */
    public boolean parsingJsr() {
        return jsrEntryBci > 0;
    }

    /**
     * Gets the bytecode index for the JSR entry.
     * @return the jsr entry bci
     */
    public int jsrEntryBCI() {
        return jsrEntryBci;
    }

    /**
     * Gets the index of the local variable containing the JSR return address.
     * @return the index of the local with the JSR return address
     */
    public int jsrEntryReturnAddressLocal() {
        return jsrRetAddrLocal;
    }

    /**
     * Sets the index of the local variable containing the JSR return address.
     * @param local the local
     */
    public void setJsrEntryReturnAddressLocal(int local) {
        jsrRetAddrLocal = local;
    }

    /**
     * Gets the continuation for parsing a JSR.
     * @return the jsr continuation
     */
    public BlockBegin jsrContinuation() {
        return jsrContinuation;
    }

    /**
     * Sets the continuation for parsing a JSR.
     * @param block the block that is the continuation
     */
    public void setJsrContinuation(BlockBegin block) {
        jsrContinuation = block;
    }

    /**
     * Gets the number of returns, in this scope or (if parsing a JSR) the parent scope.
     * @return the number of returns
     */
    public int numberOfReturns() {
        if (parsingJsr()) {
            return parent.numberOfReturns();
        }
        return numReturns;
    }

    /**
     * Increments the number of returns, in this scope or (if parsing a JSR) the parent scope.
     */
    public void incrementNumberOfReturns() {
        if (parsingJsr()) {
            parent.incrementNumberOfReturns();
        } else {
            numReturns++;
        }
    }

    /**
     * Sets the information for cleaning up after a failed inlining.
     * @param block the cleanup block
     * @param returnPrev the previous return
     * @param returnState the state at the previous return
     */
    public void setInlineCleanupInfo(BlockBegin block, Instruction returnPrev, ValueStack returnState) {
        cleanupBlock = block;
        cleanupReturnPrev = returnPrev;
        cleanupState = returnState;
    }

    /**
     * Gets the cleanup block for when inlining fails.
     * @return the cleanup block
     */
    public BlockBegin inlineCleanupBlock() {
        return cleanupBlock;
    }

    /**
     * Gets the previous return for when inlining fails.
     * @return the previous return
     */
    public Instruction inlineCleanupReturnPrev() {
        return cleanupReturnPrev;
    }

    /**
     * Gets the state for when inlining fails.
     * @return the state
     */
    public ValueStack inlineCleanupState() {
        return cleanupState;
    }

    /**
     * Gets the list of exception handlers for this scope data.
     * @return the list of exception handlers
     */
    public List<ExceptionHandler> exceptionHandlers() {
        return exceptionHandlers;
    }

    /**
     * Adds an exception handler to this scope data.
     * @param handler the handler to add
     */
    public void addExceptionHandler(ExceptionHandler handler) {
        if (exceptionHandlers == null) {
            exceptionHandlers = new ArrayList<ExceptionHandler>();
        }
        assert !parsingJsr() : "jsr scope should already have all the handlers it needs";
        exceptionHandlers.add(handler);
        hasHandler = true;
    }

    /**
     * Adds a block to the worklist, if it is not already in the worklist.
     * This method will keep the worklist topologically stored (i.e. the lower
     * DFNs are earlier in the list).
     * @param block the block to add to the work list
     */
    public void addToWorkList(BlockBegin block) {
        if (!block.isOnWorkList()) {
            if (block == continuation || block == jsrContinuation) {
                return;
            }
            block.setOnWorkList(true);
            sortIntoWorkList(block);
        }
    }

    private void sortIntoWorkList(BlockBegin top) {
        // XXX: this is O(n), since the whole list is sorted; a heap could achieve O(nlogn), but
        //      would only pay off for large worklists
        if (workList == null) {
            // need to allocate the worklist
            workList = new BlockBegin[5];
        } else if (workListIndex == workList.length) {
            // need to grow the worklist
            BlockBegin[] nworkList = new BlockBegin[workList.length * 3];
            System.arraycopy(workList, 0, nworkList, 0, workList.length);
            workList = nworkList;
        }
        // put the block at the end of the array
        workList[workListIndex++] = top;
        int dfn = top.depthFirstNumber();
        assert dfn >= 0 : top + " does not have a depth first number";
        int i = workListIndex - 2;
        // push top towards the beginning of the array
        for (; i >= 0; i--) {
            BlockBegin b = workList[i];
            if (b.depthFirstNumber() >= dfn) {
                break; // already in the right position
            }
            workList[i + 1] = b; // bubble b down by one
            workList[i] = top;   // and overwrite it with top
        }
    }

    /**
     * Removes the next block from the worklist. The list is sorted topologically, so the
     * block with the lowest depth first number in the list will be removed and returned.
     * @return the next block from the worklist; {@code null} if there are no blocks
     * in the worklist
     */
    public BlockBegin removeFromWorkList() {
        if (workListIndex == 0) {
            return null;
        }
        // pop the last item off the end
        return workList[--workListIndex];
    }

    /**
     * Converts this scope data to a string for debugging purposes.
     * @return a string representation of this scope data
     */
    @Override
    public String toString() {
        if (parsingJsr()) {
            return "jsr@" + jsrEntryBci + " data for " + scope.toString();
        } else {
            return "data for " + scope.toString();
        }

    }
}
