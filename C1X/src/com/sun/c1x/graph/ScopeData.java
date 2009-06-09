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

import com.sun.c1x.ir.*;
import com.sun.c1x.bytecode.BytecodeStream;
import com.sun.c1x.value.ValueStack;
import com.sun.c1x.C1XOptions;

import java.util.List;
import java.util.ArrayList;

/**
 * The <code>ScopeData</code> class represents inlining context when parsing the bytecodes
 * of an inlined method.
 * @author Ben L. Titzer
*/
public class ScopeData {
    // XXX: refactor and split this class into ScopeData, JsrScopeData, and InlineScopeData

    final ScopeData parent;
    // the IR scope
    final IRScope scope;
    // bci-to-block mapping
    final BlockMap blockMap;
    // whether this scope or any parent scope has exception handlers
    boolean _hasHandler;
    // the bytecode stream
    BytecodeStream _stream;
    // the worklist of blocks, managed like a sorted list
    BlockBegin[] _workList;
    // the current position in the worklist
    int _workListIndex;
    // maximum inline size for this scope
    int _maxInlineSize;
    // expression stack depth at point where inline occurred
    int _callerStackSize;

    // The continuation point for the inline. Currently only used in
    // multi-block inlines, but eventually would like to use this for
    // all inlines for uniformity and simplicity; in this case would
    // get the continuation point from the BlockList instead of
    // fabricating it anew because Invokes would be considered to be
    // BlockEnds.
    BlockBegin _continuation;

    // Without return value of inlined method on stack
    ValueStack _continuationState;

    // Number of returns seen in this scope
    int _numReturns;

    // In order to generate better code while inlining, we currently
    // have to perform an optimization for single-block inlined
    // methods where we continue parsing into the same block. This
    // allows us to perform LVN and folding across inlined scopes.
    BlockBegin _cleanupBlock;       // The block to which the return was added
    Instruction _cleanupReturnPrev; // Instruction before return instruction
    ValueStack _cleanupState;       // State of that block (not yet pinned)

    // We track the destination bci of the jsr only to determine
    // bailout conditions, since we only handle a subset of all of the
    // possible jsr-ret control structures. Recursive invocations of a
    // jsr are disallowed by the verifier. > 0 indicates parsing
    // of a jsr.
    int _jsrEntryBci;
    // We need to track the local variable in which the return address
    // was stored to ensure we can handle inlining the jsr, because we
    // don't handle arbitrary jsr/ret constructs.
    int _jsrRetAddrLocal;

    // If we are parsing a jsr, the continuation point for rets
    BlockBegin _jsrContinuation;

    // Cloned exception handlers for jsr-related ScopeDatas
    List<ExceptionHandler> _jsrHandlers;

    BlockBegin[] _jsrDuplicatedBlocks; // blocks that have been duplicated for JSR inlining

    /**
     * Constructs a new ScopeData instance with the specified parent ScopeData.
     * @param parent the parent scope data
     * @param scope the IR scope
     * @param blockMap the block map for this scope
     */
    public ScopeData(ScopeData parent, IRScope scope, BlockMap blockMap) {
        this.parent = parent;
        this.scope = scope;
        this.blockMap = blockMap;
        if (parent != null) {
            _maxInlineSize = (int) (C1XOptions.MaximumInlineRatio * parent.maxInlineSize());
            if (_maxInlineSize < C1XOptions.MaximumTrivialSize) {
                _maxInlineSize = C1XOptions.MaximumTrivialSize;
            }
            _hasHandler = parent._hasHandler;
        } else {
            _maxInlineSize = C1XOptions.MaximumInlineSize;
        }
        if (scope.exceptionHandlers() != null) {
            _hasHandler = true;
        }
    }

    /**
     * Gets the block beginning at the specified bytecode index. Note that this method
     * will clone the block if it the scope data is currently parsing a JSR.
     * @param bci the bytecode index of the start of the block
     * @return the block starting at the specified bytecode index
     */
    public BlockBegin blockAt(int bci) {
        if (parsingJsr()) {
            // all blocks in a JSR are duplicated on demand using an internal array,
            // including those for exception handlers in the scope of the method
            // containing the jsr (because those exception handlers may contain ret
            // instructions in some cases).
            BlockBegin block = _jsrDuplicatedBlocks[bci];
            if (block == null) {
                BlockBegin parent = this.parent.blockAt(bci);
                if (parent != null) {
                    BlockBegin newBlock = new BlockBegin(parent.bci());
                    newBlock.setDepthFirstNumber(parent.depthFirstNumber());
                    newBlock.copyBlockFlags(parent);
                    _jsrDuplicatedBlocks[bci] = newBlock;
                    block = newBlock;
                }
            }
            return block;
        }
        return blockMap.get(bci);
    }

    /**
     * Checks whether this ScopeData has any handlers.
     * @return <code>true</code> if there are any exception handlers
     */
    public boolean hasHandler() {
        return _hasHandler;
    }

    /**
     * Gets the bytecode stream for this ScopeData.
     * @return the bytecode stream
     */
    public BytecodeStream stream() {
        return _stream;
    }

    /**
     * Sets the bytecode stream for this ScopeData.
     * @param stream the bytecode stream
     */
    public void setStream(BytecodeStream stream) {
        _stream = stream;
    }

    /**
     * Gets the maximum inline size.
     * @return the maximum inline size
     */
    public int maxInlineSize() {
        return _maxInlineSize;
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
        return _continuation;
    }

    /**
     * Sets the continuation for this ScopeData.
     * @param continuation the continuation
     */
    public void setContinuation(BlockBegin continuation) {
        _continuation = continuation;
    }

    /**
     * Gets the state at the continuation point.
     * @return the state at the continuation point
     */
    public ValueStack continuationState() {
        return _continuationState;
    }

    /**
     * Sets the state at the continuation point.
     * @param state the state at the continuation
     */
    public void setContinuationState(ValueStack state) {
        _continuationState = state;
    }

    /**
     * Checks whether this ScopeData is parsing a JSR.
     * @return <code>true</code> if this scope data is parsing a JSR
     */
    public boolean parsingJsr() {
        return _jsrEntryBci > 0;
    }

    /**
     * Gets the bytecode index for the JSR entry.
     * @return the jsr entry bci
     */
    public int jsrEntryBCI() {
        return _jsrEntryBci;
    }

    /**
     * Sets the bytecode index for the JSR entry.
     * @param bci the bytecode index of the JSR entry
     */
    public void setJsrEntryBCI(int bci) {
        assert bci > 0 : "jsr cannot possibly jump to BCI 0";
        _jsrDuplicatedBlocks = new BlockBegin[scope.method().codeSize()];
        _jsrEntryBci = bci;
    }

    /**
     * Gets the index of the local variable containing the JSR return address.
     * @return the index of the local with the JSR return address
     */
    public int jsrEntryReturnAddressLocal() {
        return _jsrRetAddrLocal;
    }

    /**
     * Sets the index of the local variable containing the JSR return address.
     * @param local the local
     */
    public void setJsrEntryReturnAddressLocal(int local) {
        _jsrRetAddrLocal = local;
    }

    /**
     * Gets the continuation for parsing a JSR.
     * @return the jsr continuation
     */
    public BlockBegin jsrContinuation() {
        return _jsrContinuation;
    }

    /**
     * Sets the continuation for parsing a JSR.
     * @param block the block that is the continuation
     */
    public void setJsrContinuation(BlockBegin block) {
        _jsrContinuation = block;
    }

    /**
     * Gets the number of returns, in this scope or (if parsing a JSR) the parent scope.
     * @return the number of returns
     */
    public int numberOfReturns() {
        if (parsingJsr()) {
            return parent.numberOfReturns();
        }
        return _numReturns;
    }

    /**
     * Increments the number of returns, in this scope or (if parsing a JSR) the parent scope.
     */
    public void incrementNumberOfReturns() {
        if (parsingJsr()) {
            parent.incrementNumberOfReturns();
        } else {
            _numReturns++;
        }
    }

    /**
     * Sets the information for cleaning up after a failed inlining.
     * @param block the cleanup block
     * @param returnPrev the previous return
     * @param returnState the state at the previous return
     */
    public void setInlineCleanupInfo(BlockBegin block, Instruction returnPrev, ValueStack returnState) {
        _cleanupBlock = block;
        _cleanupReturnPrev = returnPrev;
        _cleanupState = returnState;
    }

    /**
     * Gets the cleanup block for when inlining fails.
     * @return the cleanup block
     */
    public BlockBegin inlineCleanupBlock() {
        return _cleanupBlock;
    }

    /**
     * Gets the previous return for when inlining fails.
     * @return the previous return
     */
    public Instruction inlineCleanupReturnPrev() {
        return _cleanupReturnPrev;
    }

    /**
     * Gets the state for when inlining fails.
     * @return the state
     */
    public ValueStack inlineCleanupState() {
        return _cleanupState;
    }

    /**
     * Sets up the exception handlers when parsing a JSR by copying the exception handlers
     * from the surrounding scope.
     */
    public void setupJsrExceptionHandlers() {
        assert parsingJsr();

        List<ExceptionHandler> shandlers = scope.exceptionHandlers();
        List<ExceptionHandler> handlers = new ArrayList<ExceptionHandler>(shandlers.size());
        for (ExceptionHandler h : shandlers) {
            ExceptionHandler n = new ExceptionHandler(h);
            if (n.handlerBCI() != Instruction.SYNCHRONIZATION_ENTRY_BCI) {
                n.setEntryBlock(blockAt(h.handlerBCI()));
            } else {
                assert n.entryBlock().checkBlockFlag(BlockBegin.BlockFlag.DefaultExceptionHandler);
            }
            handlers.add(n);
        }
        _jsrHandlers = handlers;
    }

    /**
     * Gets the list of exception handlers for this scope data.
     * @return the list of exception handlers
     */
    public List<ExceptionHandler> exceptionHandlers() {
        if (_jsrHandlers == null) {
            assert !parsingJsr();
            return scope.exceptionHandlers();
        }
        return _jsrHandlers;
    }

    /**
     * Adds an exception handler to this scope data.
     * @param handler the handler to add
     */
    public void addExceptionHandler(ExceptionHandler handler) {
        if (_jsrHandlers == null) {
            assert !parsingJsr();
            scope.addExceptionHandler(handler);
        } else {
            _jsrHandlers.add(handler);
        }
        _hasHandler = true;
    }

    /**
     * Adds a block to the worklist, if it is not already in the worklist.
     * This method will keep the worklist topologically stored (i.e. the lower
     * DFNs are earlier in the list).
     * @param block the block to add to the work list
     */
    public void addToWorkList(BlockBegin block) {
        if (!block.isOnWorkList()) {
            if (block == _continuation || block == _jsrContinuation) {
                return;
            }
            block.setOnWorkList(true);
            sortIntoWorkList(block);
        }
    }

    private void sortIntoWorkList(BlockBegin top) {
        // XXX: this is O(n), since the whole list is sorted; a heap could achieve O(nlogn), but
        //      would only pay off for large worklists
        if (_workList == null) {
            // need to allocate the worklist
            _workList = new BlockBegin[5];
        } else if (_workListIndex == _workList.length) {
            // need to grow the worklist
            BlockBegin[] nworkList = new BlockBegin[_workList.length * 3];
            System.arraycopy(_workList, 0, nworkList, 0, _workList.length);
            _workList = nworkList;
        }
        // put the block at the end of the array
        _workList[_workListIndex++] = top;
        int dfn = top.depthFirstNumber();
        assert dfn >= 0;
        int i = _workListIndex - 2;
        // push top towards the beginning of the array
        for (; i >= 0; i--) {
            BlockBegin b = _workList[i];
            if (b.depthFirstNumber() >= dfn) {
                break; // already in the right position
            }
            _workList[i + 1] = b; // bubble b down by one
            _workList[i] = top;   // and overwrite it with top
        }
    }

    /**
     * Removes the next block from the worklist. The list is sorted topologically, so the
     * block with the lowest depth first number in the list will be removed and returned.
     * @return the next block from the worklist; <code>null</code> if there are no blocks
     * in the worklist
     */
    public BlockBegin removeFromWorkList() {
        if (_workListIndex == 0) {
            return null;
        }
        // pop the last item off the end
        return _workList[--_workListIndex];
    }

    /**
     * Checks whether the work list is empty, which indicates parsing is complete.
     * @return <code>true</code> if there are no more blocks in the worklist
     */
    public boolean isWorkListEmpty() {
        return _workList == null || _workListIndex == 0;
    }

    /**
     * Converts this scope data to a string for debugging purposes.
     * @return a string representation of this scope data
     */
    public String toString() {
        if (parsingJsr()) {
            return "jsr@" + _jsrEntryBci + " data for " + scope.toString();
        } else {
            return "data for " + scope.toString();
        }

    }
}
