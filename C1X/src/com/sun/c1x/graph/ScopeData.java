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
*
* @author Ben L. Titzer
*/
public class ScopeData {
    final ScopeData _parent;
    // bci-to-block mapping
    BlockMap _blockMap;
    // the IR scope
    IRScope _scope;
    // whether this scope or any parent scope has exception handlers
    boolean _hasHandler;
    // the bytecode stream
    BytecodeStream _stream;
    // the worklist of blocks, managed like a stack, with the top of stack at the end
    List<BlockBegin> _workList;
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

    // Was this ScopeData created only for the parsing and inlining of
    // a jsr?
    boolean _parsingJsr;
    // We track the destination bci of the jsr only to determine
    // bailout conditions, since we only handle a subset of all of the
    // possible jsr-ret control structures. Recursive invocations of a
    // jsr are disallowed by the verifier.
    int _jsrEntryBci;
    // We need to track the local variable in which the return address
    // was stored to ensure we can handle inlining the jsr, because we
    // don't handle arbitrary jsr/ret constructs.
    int _jsrRetAddrLocal; // XXX: could probably be a character

    // If we are parsing a jsr, the continuation point for rets
    BlockBegin _jsrContinuation;

    // Cloned exception handlers for jsr-related ScopeDatas
    List<ExceptionHandler> _jsrHandlers;

    // Number of returns seen in this scope
    int _numReturns; // XXX: could probably be a character

    // In order to generate profitable code for inlining, we currently
    // have to perform an optimization for single-block inlined
    // methods where we continue parsing into the same block. This
    // allows us to perform CSE across inlined scopes and to avoid
    // storing parameters to the stack. Having a global register
    // allocator and being able to perform global CSE would allow this
    // code to be removed and thereby simplify the inliner.
    BlockBegin _cleanupBlock;       // The block to which the return was added
    Instruction _cleanupReturnPrev; // Instruction before return instruction
    ValueStack _cleanupState;       // State of that block (not yet pinned)

    BlockBegin[] _jsrDuplicatedBlocks; // blocks that have been duplicated for JSR inlining

    /**
     * Constructs a new ScopeData instance with the specified parent ScopeData.
     * @param parent the parent scope data
     */
    public ScopeData(ScopeData parent) {
        _parent = parent;
        if (parent != null) {
            _maxInlineSize = (int) (C1XOptions.MaximumInlineRatio * parent.maxInlineSize());
            if (_maxInlineSize < C1XOptions.MaximumTrivialSize) {
                _maxInlineSize = C1XOptions.MaximumTrivialSize;
            }
        } else {
            _maxInlineSize = C1XOptions.MaximumInlineSize;
        }
    }

    /**
     * Gets the parent scope data for this scope data.
     * @return the parent scope data
     */
    public ScopeData parent() {
        return _parent;
    }

    /**
     * Sets the mapping between bytecode indices and the basic blocks.
     * @param blockMap the mapping between bci and block begin
     */
    public void setBlockMap(BlockMap blockMap) {
        _blockMap = blockMap;
    }

    /**
     * Gets the IRScope for this ScopeData.
     * @return the IRScope
     */
    public IRScope scope() {
        return _scope;
    }

    /**
     * Sets the IRScope for this ScopeData.
     * @param scope the IRScope
     */
    public void setScope(IRScope scope) {
        _scope = scope;
    }

    /**
     * Gets the block beginning at the specified bytecode index. Note that this method
     * will clone the block if it the scope data is currently parsing a JSR.
     * @param bci the bytecode index of the start of the block
     * @return the block starting at the specified bytecode index
     */
    public BlockBegin blockAt(int bci) {
        if (_parsingJsr) {
            // all blocks in a JSR are duplicated on demand using an internal array,
            // including those for exception handlers in the scope of the method
            // containing the jsr (because those exception handlers may contain ret
            // instructions in some cases).
            BlockBegin block = _jsrDuplicatedBlocks[bci];
            if (block == null) {
                BlockBegin parent = parent().blockAt(bci);
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
        return _blockMap.get(bci);
    }

    /**
     * Checks whether this ScopeData has any handlers.
     * @return <code>true</code> if there are any exception handlers
     */
    public boolean hasHandler() {
        return _hasHandler;
    }

    /**
     * Records that this ScopeData has exception handlers.
     */
    public void setHasHandler() {
        _hasHandler = true;
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
        ValueStack state = scope().callerState();
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
        return _parsingJsr;
    }

    /**
     * Sets this ScopeData to indicate it is parsing a JSR.
     */
    public void setParsingJsr() {
        _parsingJsr = true;
        _jsrDuplicatedBlocks = new BlockBegin[_scope.method().codeSize()];
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
        // XXX: combine _parsingJsr and _jsrEntryBci into one field (i.e. >= 0 indicates parsing_jsr)
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
            return _parent.numberOfReturns();
        }
        return _numReturns;
    }

    /**
     * Increments the number of returns, in this scope or (if parsing a JSR) the parent scope.
     */
    public void incrementNumberOfReturns() {
        if (parsingJsr()) {
            _parent.incrementNumberOfReturns();
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

        List<ExceptionHandler> shandlers = scope().exceptionHandlers();
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
            assert !_parsingJsr;
            return _scope.exceptionHandlers();
        }
        return _jsrHandlers;
    }

    /**
     * Adds a block to the worklist, if it is not already in the worklist.
     * This method will keep the worklist topologically stored (i.e. the lower
     * DFNs are earlier in the list).
     * @param block the block to add to the work list
     */
    public void addToWorkList(BlockBegin block) {
        if (_workList == null) {
            _workList = new ArrayList<BlockBegin>();
        }
        if (!block.isOnWorkList()) {
            if (_parsingJsr) {
                if (block == _jsrContinuation) {
                    return;
                }
            } else if (block == _continuation) {
                return;
            }
            block.setOnWorkList(true);
            sortTopologicallyIntoWorkList(block);
        }
    }

    private void sortTopologicallyIntoWorkList(BlockBegin top) {
        // the worklist is managed like a stack, with the top of the stack at the end of the list
        _workList.add(top);
        int dfn = top.depthFirstNumber();
        assert dfn >= 0;
        int i = _workList.size() - 2;
        // push top down in the stack
        for (; i >= 0; i--) {
            BlockBegin b = _workList.get(i);
            if (b.depthFirstNumber() >= dfn) {
                break; // already in the right position
            }
            _workList.set(i + 1, b);
            _workList.set(i, top);
        }
    }

    /**
     * Removes the next block from the worklist. The list is sorted topologically, so the
     * block with the lowest depth first number in the list will be removed and returned.
     * @return the next block from the worklist; <code>null</code> if there are no blocks
     * in the worklist
     */
    public BlockBegin removeFromWorkList() {
        // the worklist is managed like a stack, with the top of the stack at the end of the list
        final int max = _workList.size();
        if (max == 0) {
            return null;
        }
        // pop the last item off the end
        return _workList.remove(max - 1);
    }

    /**
     * Checks whether the work list is empty, which indicates parsing is complete.
     * @return <code>true</code> if there are no more blocks in the worklist
     */
    public boolean isWorkListEmpty() {
        return _workList == null || _workList.size() == 0;
    }
}
