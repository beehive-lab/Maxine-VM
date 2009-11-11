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
import com.sun.c1x.ci.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.opt.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;

/**
 * The <code>GraphBuilder</code> class parses the bytecode of a method and builds the IR graph.
 * A number of optimizations may be performed during parsing of the bytecode, including value
 * numbering, inlining, constant folding, strength reduction, etc.
 *
 * @author Ben L. Titzer
 */
public class GraphBuilder {

    final IR ir;
    final C1XCompilation compilation;
    final CiStatistics stats;

    final ValueMap localValueMap;          // map of values for local value numbering
    final MemoryMap memoryMap;             // map of field values for local load elimination
    final Canonicalizer canonicalizer;     // canonicalizer which does strength reduction + constant folding
    ScopeData scopeData;                   // Per-scope data; used for inlining
    BlockBegin curBlock;                   // the current block
    ValueStack curState;                   // the current execution state
    Instruction lastInstr;                 // the last instruction added

    boolean skipBlock;                     // skip processing of the rest of this block
    private Value rootMethodSynchronizedObject;

    /**
     * Creates a new instance and builds the graph for a the specified IRScope.
     *
     * @param compilation the compilation
     * @param scope the top IRScope
     * @param ir the IR to build the graph into
     */
    public GraphBuilder(C1XCompilation compilation, IRScope scope, IR ir) {
        this.compilation = compilation;
        this.ir = ir;
        this.stats = compilation.stats;
        this.memoryMap = C1XOptions.OptLocalLoadElimination ? new MemoryMap() : null;
        this.localValueMap = C1XOptions.OptLocalValueNumbering ? new ValueMap() : null;
        this.canonicalizer = C1XOptions.OptCanonicalize ? new Canonicalizer(compilation.runtime, compilation.method) : null;
        RiMethod rootMethod = compilation.method;

        // 1. create the start block
        ir.startBlock = new BlockBegin(0, ir.nextBlockNumber());
        BlockBegin startBlock = ir.startBlock;

        // 2. compute the block map and get the entrypoint(s)
        BlockMap blockMap = compilation.getBlockMap(scope.method, compilation.osrBCI);
        BlockBegin stdEntry = blockMap.get(0);
        BlockBegin osrEntry = compilation.osrBCI < 0 ? null : blockMap.get(compilation.osrBCI);
        pushRootScope(scope, blockMap, startBlock);
        ValueStack initialState = stateAtEntry(rootMethod);
        startBlock.merge(initialState);
        BlockBegin syncHandler = null;

        // 3. setup internal state for appending instructions
        curBlock = startBlock;
        lastInstr = startBlock;
        curState = initialState;

        if (rootMethod.isSynchronized()) {
            // 4A.1 add a monitor enter to the start block
            rootMethodSynchronizedObject = synchronizedObject(initialState, compilation.method);
            genMonitorEnter(rootMethodSynchronizedObject, Instruction.SYNCHRONIZATION_ENTRY_BCI);
            // 4A.2 finish the start block
            finishStartBlock(startBlock, stdEntry, osrEntry);

            // 4A.3 setup an exception handler to unlock the root method synchronized object
            syncHandler = new BlockBegin(Instruction.SYNCHRONIZATION_ENTRY_BCI, ir.nextBlockNumber());
            syncHandler.setExceptionEntry();
            syncHandler.setBlockFlag(BlockBegin.BlockFlag.IsOnWorkList);
            syncHandler.setBlockFlag(BlockBegin.BlockFlag.DefaultExceptionHandler);

            RiExceptionHandler desc = newDefaultExceptionHandler(rootMethod);
            ExceptionHandler h = new ExceptionHandler(desc);
            h.setEntryBlock(syncHandler);
            scopeData.addExceptionHandler(h);
        } else {
            // 4B.1 simply finish the start block
            finishStartBlock(startBlock, stdEntry, osrEntry);
        }

        scope().computeLockStackSize();
        C1XIntrinsic intrinsic = C1XIntrinsic.getIntrinsic(rootMethod);
        if (intrinsic != null) {
            // 6A.1 the root method is an intrinsic; load the parameters onto the stack and try to inline it
            curState = initialState.copy();
            lastInstr = curBlock;
            if (C1XOptions.OptIntrinsify) {
                // try to inline an Intrinsic node
                boolean isStatic = rootMethod.isStatic();
                int argsSize = rootMethod.signatureType().argumentSlots(!isStatic);
                Value[] args = new Value[argsSize];
                for (int i = 0; i < args.length; i++) {
                    args[i] = curState.localAt(i);
                }
                if (tryInlineIntrinsic(rootMethod, args, isStatic, intrinsic)) {
                    // intrinsic inlining succeeded, add the return node
                    CiKind rt = returnBasicType(rootMethod);
                    Value result = null;
                    if (rt != CiKind.Void) {
                        result = pop(rt);
                    }
                    genMethodReturn(result);
                    BlockEnd end = (BlockEnd) lastInstr;
                    curBlock.setEnd(end);
                    end.setStateAfter(curState.immutableCopy());
                }  else {
                    // try intrinsic failed; do the normal parsing
                    scopeData.addToWorkList(stdEntry);
                    iterateAllBlocks();
                }
            } else {
                // 6B.1 do the normal parsing
                scopeData.addToWorkList(stdEntry);
                iterateAllBlocks();
            }
        } else {
            // 6B.1 do the normal parsing
            scopeData.addToWorkList(stdEntry);
            iterateAllBlocks();
        }

        if (syncHandler != null && syncHandler.stateBefore() != null) {
            // generate unlocking code if the exception handler is reachable
            fillSyncHandler(rootMethodSynchronizedObject, syncHandler, false);
        }

        if (compilation.osrBCI >= 0) {
            BlockBegin osrBlock = blockMap.get(compilation.osrBCI);
            assert osrBlock.wasVisited();
            if (!osrBlock.stateBefore().stackEmpty()) {
                throw new CiBailout("cannot OSR with non-empty stack");
            }
        }
    }

    private void finishStartBlock(BlockBegin startBlock, BlockBegin stdEntry, BlockBegin osrEntry) {
        assert curBlock == startBlock;
        Base base = new Base(stdEntry, osrEntry);
        appendWithoutOptimization(base, 0);
        ValueStack stateAfter = curState.immutableCopy();
        base.setStateAfter(stateAfter);
        startBlock.setEnd(base);
        assert stdEntry.stateBefore() == null;
        stdEntry.merge(stateAfter);
    }

    private RiExceptionHandler newDefaultExceptionHandler(RiMethod method) {
        return constantPool().newExceptionHandler(0, method.codeSize(), -1, 0);
    }

    void pushRootScope(IRScope scope, BlockMap blockMap, BlockBegin start) {
        BytecodeStream stream = new BytecodeStream(scope.method.code());
        RiConstantPool constantPool = compilation.runtime.getConstantPool(scope.method);
        scopeData = new ScopeData(null, scope, blockMap, stream, constantPool);
        curBlock = start;
    }

    public boolean hasHandler() {
        return scopeData.hasHandler();
    }

    public IRScope scope() {
        return scopeData.scope;
    }

    public IRScope rootScope() {
        IRScope root = scope();
        while (root.caller != null) {
            root = root.caller;
        }
        return root;
    }

    public RiMethod method() {
        return scopeData.scope.method;
    }

    public BytecodeStream stream() {
        return scopeData.stream;
    }

    public int bci() {
        return scopeData.stream.currentBCI();
    }

    public int nextBCI() {
        return scopeData.stream.nextBCI();
    }

    void ipush(Value x) {
        curState.ipush(x);
    }

    void lpush(Value x) {
        curState.lpush(x);
    }

    void fpush(Value x) {
        curState.fpush(x);
    }

    void dpush(Value x) {
        curState.dpush(x);
    }

    void apush(Value x) {
        curState.apush(x);
    }

    void push(CiKind basicType, Value x) {
        curState.push(basicType, x);
    }

    void pushReturn(CiKind basicType, Value x) {
        if (basicType != CiKind.Void) {
            curState.push(basicType.stackType(), x);
        }
    }

    Value ipop() {
        return curState.ipop();
    }

    Value lpop() {
        return curState.lpop();
    }

    Value fpop() {
        return curState.fpop();
    }

    Value dpop() {
        return curState.dpop();
    }

    Value apop() {
        return curState.apop();
    }

    Value pop(CiKind basicType) {
        return curState.pop(basicType);
    }

    void loadLocal(int index, CiKind basicType) {
        push(basicType, curState.loadLocal(index));
    }

    void storeLocal(CiKind basicType, int index) {
        if (scopeData.parsingJsr()) {
            // We need to do additional tracking of the location of the return
            // address for jsrs since we don't handle arbitrary jsr/ret
            // constructs. Here we are figuring out in which circumstances we
            // need to bail out.
            if (basicType == CiKind.Object) {
                // might be storing the JSR return address
                Value x = curState.xpop();
                if (x.type().isJsr()) {
                    setJsrReturnAddressLocal(index);
                    curState.storeLocal(index, x);
                } else {
                    // nope, not storing the JSR return address
                    assert x.type().isObject();
                    curState.storeLocal(index, x);
                    overwriteJsrReturnAddressLocal(index);
                }
                return;
            } else {
                // not storing the JSR return address local, but might overwrite it
                overwriteJsrReturnAddressLocal(index);
            }
        }

        curState.storeLocal(index, roundFp(pop(basicType)));
    }

    private void overwriteJsrReturnAddressLocal(int index) {
        if (index == scopeData.jsrEntryReturnAddressLocal()) {
            scopeData.setJsrEntryReturnAddressLocal(-1);
        }
    }

    private void setJsrReturnAddressLocal(int index) {
        scopeData.setJsrEntryReturnAddressLocal(index);

        // Also check parent jsrs (if any) at this time to see whether
        // they are using this local. We don't handle skipping over a
        // ret.
        for (ScopeData cur = scopeData.parent; cur != null && cur.parsingJsr() && cur.scope == scope(); cur = cur.parent) {
            if (cur.jsrEntryReturnAddressLocal() == index) {
                throw new CiBailout("subroutine overwrites return address from previous subroutine");
            }
        }
    }

    Value roundFp(Value x) {
        if (C1XOptions.RoundFPResults && C1XOptions.SSEVersion < 2) {
            if (x.type().isDouble() && !(x instanceof Constant) && !(x instanceof Local) && !(x instanceof RoundFP)) {
                return append(new RoundFP(x));
            }
        }
        return x;
    }

    List<ExceptionHandler> handleException(Instruction x, int bci) {
        if (!hasHandler()) {
            return Util.uncheckedCast(Collections.EMPTY_LIST);
        }

        List<ExceptionHandler> exceptionHandlers = new ArrayList<ExceptionHandler>();
        ScopeData curScopeData = scopeData;
        ValueStack s = x.stateBefore();
        int scopeCount = 0;

        assert s != null : "exception handler state must be available for " + x;
        s = s.copy();
        do {
            assert curScopeData.scope == s.scope() : "scopes do not match";
            assert bci == Instruction.SYNCHRONIZATION_ENTRY_BCI || bci == curScopeData.stream.currentBCI() : "invalid bci";

            // join with all potential exception handlers
            List<ExceptionHandler> handlers = curScopeData.exceptionHandlers();
            if (handlers != null) {
                for (ExceptionHandler h : handlers) {
                    if (h.covers(bci)) {
                        // if the handler covers this bytecode index, add it to the list
                        if (addExceptionHandler(exceptionHandlers, h, curScopeData, s, scopeCount)) {
                            // if the handler was a default handler, we are done
                            return exceptionHandlers;
                        }
                    }
                }
            }
            // pop the scope to the next IRScope level
            // if parsing a JSR, skip scopes until the next IRScope level
            IRScope curScope = curScopeData.scope;
            while (curScopeData.parent != null && curScopeData.parent.scope == curScope) {
                curScopeData = curScopeData.parent;
            }
            if (curScopeData.parent == null) {
                // no more levels, done
                break;
            }
            // there is another level, pop
            s = s.popScope();
            bci = curScopeData.scope.callerBCI();
            curScopeData = curScopeData.parent;
            scopeCount++;

        } while (true);

        return exceptionHandlers;
    }

    private boolean addExceptionHandler(List<ExceptionHandler> exceptionHandlers, ExceptionHandler h, ScopeData curScopeData, ValueStack s, int scopeCount) {
        compilation.setHasExceptionHandlers();

        BlockBegin entry = h.entryBlock();
        if (entry == curBlock) {
            throw new CiBailout("Exception handler covers itself");
        }
        assert entry.bci() == h.handler.handlerBCI();
        assert entry.bci() == -1 || entry == curScopeData.blockAt(entry.bci()) : "blocks must correspond";
        assert entry.stateBefore() == null || s.locksSize() == entry.stateBefore().locksSize() : "locks do not match";

        // exception handler starts with an empty expression stack
        s.truncateStack(curScopeData.callerStackSize());

        entry.merge(s);

        // add current state for correct handling of phi functions
        int phiOperand = entry.addExceptionState(s);

        // add entry to the list of exception handlers of this block
        curBlock.addExceptionHandler(entry);

        // add back-edge from exception handler entry to this block
        if (!entry.predecessors().contains(curBlock)) {
            entry.addPredecessor(curBlock);
        }

        // clone exception handler
        ExceptionHandler newHandler = new ExceptionHandler(h);
        newHandler.setPhiOperand(phiOperand);
        newHandler.setScopeCount(scopeCount);
        exceptionHandlers.add(newHandler);

        // fill in exception handler subgraph lazily
        assert !entry.wasVisited() : "entry must not be visited yet";
        curScopeData.addToWorkList(entry);

        // stop when reaching catch all
        return h.isCatchAll();
    }

    void genLoadConstant(char cpi) {
        ValueStack stateBefore = curState.copy();
        Object con = constantPool().lookupConstant(cpi);

        if (con instanceof RiType) {
            // this is a load of class constant which might be unresolved
            RiType ritype = (RiType) con;
            if (!ritype.isLoaded() || C1XOptions.TestPatching) {
                push(CiKind.Object, append(new ResolveClass(ritype, RiType.Representation.JavaClass, stateBefore, cpi, constantPool())));
            } else {
                push(CiKind.Object, append(Constant.forObject(ritype.javaClass())));
            }
        } else if (con instanceof CiConstant) {
            CiConstant constant = (CiConstant) con;
            push(constant.basicType.stackType(), appendConstant(constant));
        } else {
            throw new Error("lookupConstant returned an object of incorrect type");
        }
    }

    void genLoadIndexed(CiKind type) {
        ValueStack stateBefore = curState.immutableCopy();
        Value index = ipop();
        Value array = apop();
        Value length = null;
        if (cseArrayLength(array)) {
            length = append(new ArrayLength(array, stateBefore));
        }
        push(type.stackType(), append(new LoadIndexed(array, index, length, type, stateBefore)));
    }

    void genStoreIndexed(CiKind type) {
        ValueStack stateBefore = curState.immutableCopy();
        Value value = pop(type.stackType());
        Value index = ipop();
        Value array = apop();
        Value length = null;
        if (cseArrayLength(array)) {
            length = append(new ArrayLength(array, stateBefore));
        }
        StoreIndexed result = new StoreIndexed(array, index, length, type, value, stateBefore);
        append(result);
        if (memoryMap != null) {
            memoryMap.storeValue(value);
        }
    }

    void stackOp(int opcode) {
        switch (opcode) {
            case Bytecodes.POP: {
                curState.xpop();
                break;
            }
            case Bytecodes.POP2: {
                curState.xpop();
                curState.xpop();
                break;
            }
            case Bytecodes.DUP: {
                Value w = curState.xpop();
                curState.xpush(w);
                curState.xpush(w);
                break;
            }
            case Bytecodes.DUP_X1: {
                Value w1 = curState.xpop();
                Value w2 = curState.xpop();
                curState.xpush(w1);
                curState.xpush(w2);
                curState.xpush(w1);
                break;
            }
            case Bytecodes.DUP_X2: {
                Value w1 = curState.xpop();
                Value w2 = curState.xpop();
                Value w3 = curState.xpop();
                curState.xpush(w1);
                curState.xpush(w3);
                curState.xpush(w2);
                curState.xpush(w1);
                break;
            }
            case Bytecodes.DUP2: {
                Value w1 = curState.xpop();
                Value w2 = curState.xpop();
                curState.xpush(w2);
                curState.xpush(w1);
                curState.xpush(w2);
                curState.xpush(w1);
                break;
            }
            case Bytecodes.DUP2_X1: {
                Value w1 = curState.xpop();
                Value w2 = curState.xpop();
                Value w3 = curState.xpop();
                curState.xpush(w2);
                curState.xpush(w1);
                curState.xpush(w3);
                curState.xpush(w2);
                curState.xpush(w1);
                break;
            }
            case Bytecodes.DUP2_X2: {
                Value w1 = curState.xpop();
                Value w2 = curState.xpop();
                Value w3 = curState.xpop();
                Value w4 = curState.xpop();
                curState.xpush(w2);
                curState.xpush(w1);
                curState.xpush(w4);
                curState.xpush(w3);
                curState.xpush(w2);
                curState.xpush(w1);
                break;
            }
            case Bytecodes.SWAP: {
                Value w1 = curState.xpop();
                Value w2 = curState.xpop();
                curState.xpush(w1);
                curState.xpush(w2);
                break;
            }
            default:
                throw Util.shouldNotReachHere();
        }

    }

    void genArithmeticOp(CiKind basicType, int opcode) {
        genArithmeticOp(basicType, opcode, null);
    }

    void genArithmeticOp(CiKind basicType, int opcode, ValueStack stack) {
        Value y = pop(basicType);
        Value x = pop(basicType);
        Value result = append(new ArithmeticOp(opcode, x, y, method().isStrictFP(), stack));
        if (C1XOptions.RoundFPResults && scopeData.scope.method.isStrictFP()) {
            result = roundFp(result);
        }
        push(basicType, result);
    }

    void genNegateOp(CiKind basicType) {
        push(basicType, append(new NegateOp(pop(basicType))));
    }

    void genShiftOp(CiKind basicType, int opcode) {
        Value s = ipop();
        Value x = pop(basicType);
        // note that strength reduction of e << K >>> K is correctly handled in canonicalizer now
        push(basicType, append(new ShiftOp(opcode, x, s)));
    }

    void genLogicOp(CiKind basicType, int opcode) {
        Value y = pop(basicType);
        Value x = pop(basicType);
        push(basicType, append(new LogicOp(opcode, x, y)));
    }

    void genCompareOp(CiKind basicType, int opcode) {
        ValueStack stateBefore = curState.immutableCopy();
        Value y = pop(basicType);
        Value x = pop(basicType);
        ipush(append(new CompareOp(opcode, x, y, stateBefore)));
    }

    void genConvert(int opcode, CiKind from, CiKind to) {
        CiKind tt = to.stackType();
        push(tt, append(new Convert(opcode, pop(from.stackType()), tt)));
    }

    void genIncrement() {
        int index = stream().readLocalIndex();
        int delta = stream().readIncrement();
        Value x = curState.localAt(index);
        Value y = append(Constant.forInt(delta));
        curState.storeLocal(index, append(new ArithmeticOp(Bytecodes.IADD, x, y, method().isStrictFP(), null)));
    }

    void genGoto(int fromBCI, int toBCI) {
        append(new Goto(blockAt(toBCI), null, toBCI <= fromBCI)); // backwards branch => safepoint
    }

    void ifNode(Value x, Condition cond, Value y, ValueStack stateBefore) {
        BlockBegin tsucc = blockAt(stream().readBranchDest());
        BlockBegin fsucc = blockAt(stream().nextBCI());
        int bci = stream().currentBCI();
        boolean isBackwards = tsucc.bci() <= bci || fsucc.bci() <= bci;
        append(new If(x, cond, false, y, tsucc, fsucc, isBackwards ? stateBefore : null, isBackwards));
    }

    void genIfZero(Condition cond) {
        ValueStack stateBefore = curState.immutableCopy();
        Value y = appendConstant(CiConstant.INT_0);
        Value x = ipop();
        ifNode(x, cond, y, stateBefore);
    }

    void genIfNull(Condition cond) {
        ValueStack stateBefore = curState.immutableCopy();
        Value y = appendConstant(CiConstant.NULL_OBJECT);
        Value x = apop();
        ifNode(x, cond, y, stateBefore);
    }

    void genIfSame(CiKind basicType, Condition cond) {
        ValueStack stateBefore = curState.immutableCopy();
        Value y = pop(basicType);
        Value x = pop(basicType);
        ifNode(x, cond, y, stateBefore);
    }

    void genThrow(int bci) {
        ValueStack stateBefore = curState.immutableCopy();
        Throw t = new Throw(apop(), stateBefore);
        appendWithoutOptimization(t, bci);
    }

    void genCheckCast() {
        ValueStack stateBefore = curState.immutableCopy();
        char cpi = stream().readCPI();
        RiType type = constantPool().lookupType(cpi);
        Value typeInstruction = genResolveClass(RiType.Representation.ObjectHub, type, !C1XOptions.TestPatching && type.isLoaded() && type.isInitialized(), cpi, stateBefore);
        CheckCast c = new CheckCast(type, typeInstruction, apop(), stateBefore);
        apush(append(c));
        if (assumeLeafClass(type) && !type.isArrayKlass()) {
            c.setDirectCompare();
        }
    }

    void genInstanceOf() {
        ValueStack stateBefore = curState.immutableCopy();
        char cpi = stream().readCPI();
        RiType type = constantPool().lookupType(cpi);
        Value typeInstruction = genResolveClass(RiType.Representation.ObjectHub, type, !C1XOptions.TestPatching && type.isLoaded() && type.isInitialized(), cpi, stateBefore);
        InstanceOf i = new InstanceOf(type, typeInstruction, apop(), stateBefore);
        ipush(append(i));
        if (assumeLeafClass(type) && !type.isArrayKlass()) {
            i.setDirectCompare();
        }
    }

    void genNewInstance(char cpi) {
        ValueStack stateBefore = curState.immutableCopy();
        RiType type = constantPool().lookupType(cpi);
        NewInstance n = new NewInstance(type, cpi, constantPool(), stateBefore);
        if (memoryMap != null) {
            memoryMap.newInstance(n);
        }
        apush(append(n));
    }

    void genNewTypeArray(int typeCode) {
        ValueStack stateBefore = curState.immutableCopy();
        apush(append(new NewTypeArray(ipop(), CiKind.fromArrayTypeCode(typeCode), stateBefore)));
    }

    void genNewObjectArray(char cpi) {
        RiType type = constantPool().lookupType(cpi);
        ValueStack stateBefore = curState.immutableCopy();
        NewArray n = new NewObjectArray(type, ipop(), stateBefore, cpi, constantPool());
        apush(append(n));
    }

    void genNewMultiArray(char cpi) {
        RiType type = constantPool().lookupType(cpi);
        ValueStack stateBefore = curState.immutableCopy();
        int rank = stream().readUByte(stream().currentBCI() + 3);
        Value[] dims = new Value[rank];
        for (int i = rank - 1; i >= 0; i--) {
            dims[i] = ipop();
        }
        NewArray n = new NewMultiArray(type, dims, stateBefore, cpi, constantPool());
        apush(append(n));
    }

    void genGetField(char cpi) {
        ValueStack stateBefore = curState.immutableCopy();
        RiField field = constantPool().lookupGetField(cpi);
        boolean isLoaded = !C1XOptions.TestPatching && field.isLoaded();
        LoadField load = new LoadField(apop(), field, false, stateBefore, isLoaded, cpi, constantPool());
        appendOptimizedLoadField(field.kind(), load);
    }

    void genPutField(char cpi) {
        ValueStack stateBefore = curState.immutableCopy();
        RiField field = constantPool().lookupPutField(cpi);
        boolean isLoaded = !C1XOptions.TestPatching && field.isLoaded();
        Value value = pop(field.kind().stackType());
        appendOptimizedStoreField(new StoreField(apop(), field, value, false, stateBefore, isLoaded, cpi, constantPool()));
    }

    void genGetStatic(char cpi) {
        ValueStack stateBefore = curState.immutableCopy();
        RiField field = constantPool().lookupGetStatic(cpi);
        RiType holder = field.holder();
        boolean isInitialized = !C1XOptions.TestPatching && field.isLoaded() && holder.isLoaded() && holder.isInitialized();
        Value container = genResolveClass(RiType.Representation.StaticFields, holder, isInitialized, cpi, stateBefore);
        LoadField load = new LoadField(container, field, true, stateBefore, isInitialized, cpi, constantPool());
        appendOptimizedLoadField(field.kind(), load);
    }

    void genPutStatic(char cpi) {
        ValueStack stateBefore = curState.immutableCopy();
        RiField field = constantPool().lookupPutStatic(cpi);
        RiType holder = field.holder();
        boolean isInitialized = !C1XOptions.TestPatching && field.isLoaded() && holder.isLoaded() && holder.isInitialized();
        Value container = genResolveClass(RiType.Representation.StaticFields, holder, isInitialized, cpi, stateBefore);
        Value value = pop(field.kind().stackType());
        StoreField store = new StoreField(container, field, value, true, stateBefore, isInitialized, cpi, constantPool());
        appendOptimizedStoreField(store);
    }

    private Value genResolveClass(RiType.Representation representation, RiType holder, boolean initialized, char cpi, ValueStack stateBefore) {
        Value holderInstr;
        if (initialized) {
            holderInstr = appendConstant(holder.getEncoding(representation));
        } else {
            holderInstr = append(new ResolveClass(holder, representation, stateBefore, cpi, constantPool()));
        }
        return holderInstr;
    }

    private void appendOptimizedStoreField(StoreField store) {
        if (memoryMap != null) {
            StoreField previous = memoryMap.store(store);
            if (previous == null) {
                // the store is redundant, do not append
                return;
            }
        }
        append(store);
    }

    private void appendOptimizedLoadField(CiKind basicType, LoadField load) {
        if (memoryMap != null) {
            Value replacement = memoryMap.load(load);
            if (replacement != load) {
                // the memory buffer found a replacement for this load (no need to append)
                push(basicType.stackType(), replacement);
                return;
            }
        }
        // append the load to the instruction
        Value optimized = append(load);
        if (memoryMap != null && optimized != load) {
            // local optimization happened, replace its value in the memory map
            memoryMap.setResult(load, optimized);
        }
        push(basicType.stackType(), optimized);
    }

    void genInvokeStatic(RiMethod target, char cpi, RiConstantPool constantPool) {
        ValueStack stateBefore = curState.immutableCopy();
        Value[] args = curState.popArguments(target.signatureType().argumentSlots(false));
        if (!tryOptimizeCall(target, args, true)) {
            if (!tryInline(target, args, null, stateBefore)) {
                appendInvoke(Bytecodes.INVOKESTATIC, target, args, true, cpi, constantPool, stateBefore);
            }
        }
    }

    void genInvokeInterface(RiMethod target, char cpi, RiConstantPool constantPool) {
        ValueStack stateBefore = curState.immutableCopy();
        Value[] args = curState.popArguments(target.signatureType().argumentSlots(true));
        if (!tryOptimizeCall(target, args, false)) {
            // XXX: attempt devirtualization / deinterfacification of INVOKEINTERFACE
            appendInvoke(Bytecodes.INVOKEINTERFACE, target, args, false, cpi, constantPool, stateBefore);
        }
    }

    void genInvokeVirtual(RiMethod target, char cpi, RiConstantPool constantPool) {
        ValueStack stateBefore = curState.immutableCopy();
        Value[] args = curState.popArguments(target.signatureType().argumentSlots(true));
        if (!tryOptimizeCall(target, args, false)) {
            Value receiver = args[0];
            // attempt to devirtualize the call
            if (target.isLoaded() && target.holder().isLoaded()) {
                RiType klass = target.holder();
                // 0. check for trivial cases
                if (target.canBeStaticallyBound() && !target.isAbstract()) {
                    // check for trivial cases (e.g. final methods, nonvirtual methods)
                    invokeDirect(target, args, target.holder(), cpi, constantPool, stateBefore);
                    return;
                }
                // 1. check if the exact type of the receiver can be determined
                RiType exact = getExactType(klass, receiver);
                if (exact != null && exact.isLoaded()) {
                    // either the holder class is exact, or the receiver object has an exact type
                    invokeDirect(exact.resolveMethodImpl(target), args, exact, cpi, constantPool, stateBefore);
                    return;
                }
                // 2. check if an assumed leaf method can be found
                RiMethod leaf = getAssumedLeafMethod(target, receiver);
                if (leaf != null && leaf.isLoaded() && !leaf.isAbstract() && leaf.holder().isLoaded()) {
                    invokeDirect(leaf, args, null, cpi, constantPool, stateBefore);
                    return;
                }
                // 3. check if the either of the holder or declared type of receiver can be assumed to be a leaf
                exact = getAssumedLeafType(klass, receiver);
                if (exact != null && exact.isLoaded()) {
                    // either the holder class is exact, or the receiver object has an exact type
                    invokeDirect(exact.resolveMethodImpl(target), args, exact, cpi, constantPool, stateBefore);
                    return;
                }
            }
            // devirtualization failed, produce an actual invokevirtual
            appendInvoke(Bytecodes.INVOKEVIRTUAL, target, args, false, cpi, constantPool, stateBefore);
        }
    }

    private CiKind returnBasicType(RiMethod target) {
        return target.signatureType().returnBasicType();
    }

    void genInvokeSpecial(RiMethod target, RiType knownHolder, char cpi, RiConstantPool constantPool) {
        ValueStack stateBefore = curState.immutableCopy();
        Value[] args = curState.popArguments(target.signatureType().argumentSlots(true));
        invokeDirect(target, args, knownHolder, cpi, constantPool, stateBefore);
    }

    private void invokeDirect(RiMethod target, Value[] args, RiType knownHolder, char cpi, RiConstantPool constantPool, ValueStack stateBefore) {
        if (!tryOptimizeCall(target, args, false)) {
            if (!tryInline(target, args, knownHolder, stateBefore)) {
                // could not optimize or inline the method call
                appendInvoke(Bytecodes.INVOKESPECIAL, target, args, false, cpi, constantPool, stateBefore);
            }
        }
    }

    private void appendInvoke(int opcode, RiMethod target, Value[] args, boolean isStatic, char cpi, RiConstantPool constantPool, ValueStack stateBefore) {
        CiKind resultType = returnBasicType(target);
        Value result = append(new Invoke(opcode, resultType.stackType(), args, isStatic, target.vtableIndex(), target, cpi, constantPool, stateBefore));
        if (C1XOptions.RoundFPResults && scopeData.scope.method.isStrictFP()) {
            pushReturn(resultType, roundFp(result));
        } else {
            pushReturn(resultType, result);
        }
    }

    private RiType getExactType(RiType staticType, Value receiver) {
        RiType exact = staticType.exactType();
        if (exact == null) {
            exact = receiver.exactType();
            if (exact == null) {
                RiType declared = receiver.declaredType();
                exact = declared == null ? null : declared.exactType();
            }
        }
        return exact;
    }

    private RiType getAssumedLeafType(RiType staticType, Value receiver) {
        if (assumeLeafClass(staticType)) {
            return staticType;
        }
        RiType declared = receiver.declaredType();
        if (declared != null && assumeLeafClass(declared)) {
            return declared;
        }
        return null;
    }

    private RiMethod getAssumedLeafMethod(RiMethod target, Value receiver) {
        if (assumeLeafMethod(target)) {
            return target;
        }
        RiType declared = receiver.declaredType();
        if (declared != null && declared.isLoaded() && !declared.isInterface()) {
            RiMethod impl = declared.resolveMethodImpl(target);
            if (impl != null && assumeLeafClass(declared)) {
                return impl;
            }
        }
        return null;
    }

    void callRegisterFinalizer() {
        Value receiver = curState.loadLocal(0);
        RiType declaredType = receiver.declaredType();
        RiType receiverType = declaredType;
        RiType exactType = receiver.exactType();
        if (exactType == null && declaredType != null) {
            exactType = declaredType.exactType();
        }
        if (exactType == null && receiver instanceof Local && ((Local) receiver).javaIndex() == 0) {
            // the exact type isn't known, but the receiver is parameter 0 => use holder
            receiverType = compilation.method().holder();
            exactType = receiverType.exactType();
        }
        boolean needsCheck = true;
        if (exactType != null) {
            // we have an exact type
            needsCheck = exactType.hasFinalizer();
        } else {
            // if either the declared type of receiver or the holder can be assumed to have no finalizers
            if (declaredType != null && declaredType.hasFinalizableSubclass()) {
                if (compilation.recordNoFinalizableSubclassAssumption(declaredType)) {
                    needsCheck = false;
                }
            }

            if (receiverType != null && receiverType.hasFinalizableSubclass()) {
                if (compilation.recordNoFinalizableSubclassAssumption(receiverType)) {
                    needsCheck = false;
                }
            }
        }

        if (needsCheck) {
            // append a call to the registration intrinsic
            loadLocal(0, CiKind.Object);
            append(new Intrinsic(CiKind.Void, C1XIntrinsic.java_lang_Object$init,
                                 null, curState.popArguments(1), false, curState.immutableCopy(), true, true));
            C1XMetrics.InlinedFinalizerChecks++;
        }

    }

    void genMethodReturn(Value x) {
        if (C1XOptions.GenFinalizerRegistration) {
            C1XIntrinsic intrinsic = C1XIntrinsic.getIntrinsic(method());
            if (intrinsic == C1XIntrinsic.java_lang_Object$init) {
                callRegisterFinalizer();
            }
        }

        // If inlining, then returns become gotos to the continuation point.
        if (scopeData.continuation() != null) {
            if (method().isSynchronized()) {
                // if the inlined method is synchronized, then the monitor
                // must be released before jumping to the continuation point
                assert C1XOptions.OptInlineSynchronized;
                int i = curState.scope().callerState().locksSize();
                assert curState.locksSize() == i + 1;
                Value object = curState.lockAt(i);
                if (object instanceof Instruction) {
                    Instruction obj = (Instruction) object;
                    if (!obj.isAppended()) {
                        appendWithoutOptimization(obj, Instruction.SYNCHRONIZATION_ENTRY_BCI);
                    }
                }
                genMonitorExit(object, Instruction.SYNCHRONIZATION_ENTRY_BCI);
            }

            // trim back stack to the caller's stack size
            curState.truncateStack(scopeData.callerStackSize());
            if (x != null) {
                curState.push(x.type(), x);
            }
            Goto gotoCallee = new Goto(scopeData.continuation(), null, false);

            // if this is the first return, store some of the state for a later return
            if (scopeData.numberOfReturns() == 0) {
                scopeData.setInlineCleanupInfo(curBlock, lastInstr, curState);
            }

            // State at end of inlined method is the state of the caller
            // without the method parameters on stack, including the
            // return value, if any, of the inlined method on operand stack.
            curState = scopeData.continuationState().copy();
            if (x != null) {
                curState.push(x.type(), x);
            }

            // The current bci() is in the wrong scope, so use the bci() of
            // the continuation point.
            appendWithoutOptimization(gotoCallee, scopeData.continuation().bci());
            scopeData.incrementNumberOfReturns();
            return;
        }

        curState.truncateStack(0);
        if (method().isSynchronized()) {
            ValueStack stateBefore = curState.immutableCopy();
            // unlock before exiting the method
            append(new MonitorExit(rootMethodSynchronizedObject, curState.unlock(), stateBefore));
        }
        append(new Return(x));
    }

    void genMonitorEnter(Value x, int bci) {
        ValueStack stateBefore = curState.immutableCopy();
        appendWithoutOptimization(new MonitorEnter(x, curState.lock(scope(), x), stateBefore), bci);
        killMemoryMap(); // prevent any optimizations across synchronization
    }

    void genMonitorExit(Value x, int bci) {
        if (curState.locksSize() < 1) {
            throw new CiBailout("monitor stack underflow");
        }
        ValueStack stateBefore = curState.immutableCopy();
        appendWithoutOptimization(new MonitorExit(x, curState.unlock(), stateBefore), bci);
        killMemoryMap(); // prevent any optimizations across synchronization
    }

    void genMonitorExit(int bci, ValueStack stateBefore) {
        if (curState.locksSize() < 1) {
            throw new CiBailout("monitor stack underflow");
        }
        appendWithoutOptimization(new MonitorExit(curState.apop(), curState.unlock(), stateBefore), bci);
        killMemoryMap(); // prevent any optimizations across synchronization
    }

    void genJsr(int dest) {
        for (ScopeData cur = scopeData; cur != null && cur.parsingJsr() && cur.scope == scope(); cur = cur.parent) {
            if (cur.jsrEntryBCI() == dest) {
                // the jsr/ret pattern includes a recursive invocation
                throw new CiBailout("recursive jsr/ret structure");
            }
        }
        push(CiKind.Jsr, append(Constant.forJsr(nextBCI())));
        tryInlineJsr(dest);
    }

    void genRet(int localIndex) {
        if (!scopeData.parsingJsr()) {
            throw new CiBailout("ret encountered when not parsing subroutine");
        }

        if (localIndex != scopeData.jsrEntryReturnAddressLocal()) {
            throw new CiBailout("jsr/ret structure is too complicated");
        }
        // rets become non-safepoint gotos
        append(new Goto(scopeData.jsrContinuation(), null, false));
    }

    void genTableswitch() {
        int bci = bci();
        BytecodeTableSwitch ts = new BytecodeTableSwitch(stream(), bci);
        int max = ts.numberOfCases();
        List<BlockBegin> list = new ArrayList<BlockBegin>(max + 1);
        boolean isBackwards = false;
        for (int i = 0; i < max; i++) {
            // add all successors to the successor list
            int offset = ts.offsetAt(i);
            list.add(blockAt(bci + offset));
            isBackwards |= offset < 0; // track if any of the successors are backwards
        }
        int offset = ts.defaultOffset();
        isBackwards |= offset < 0; // if the default successor is backwards
        list.add(blockAt(bci + offset));
        ValueStack stateBefore = isBackwards ? curState.copy() : null;
        append(new TableSwitch(ipop(), list, ts.lowKey(), stateBefore, isBackwards));
    }

    void genLookupswitch() {
        int bci = bci();
        BytecodeLookupSwitch ls = new BytecodeLookupSwitch(stream(), bci);
        int max = ls.numberOfCases();
        List<BlockBegin> list = new ArrayList<BlockBegin>(max + 1);
        int[] keys = new int[max];
        boolean isBackwards = false;
        for (int i = 0; i < max; i++) {
            // add all successors to the successor list
            int offset = ls.offsetAt(i);
            list.add(blockAt(bci + offset));
            keys[i] = ls.keyAt(i);
            isBackwards |= offset < 0; // track if any of the successors are backwards
        }
        int offset = ls.defaultOffset();
        isBackwards |= offset < 0; // if the default successor is backwards
        list.add(blockAt(bci + offset));
        ValueStack stateBefore = isBackwards ? curState.copy() : null;
        append(new LookupSwitch(ipop(), list, keys, stateBefore, isBackwards));
    }

    private boolean cseArrayLength(Value array) {
        // checks whether an array length access should be generated for CSE
        if (C1XOptions.OptCSEArrayLength) {
            // always access the length for CSE
            return true;
        } else if (array.isConstant()) {
            // the array itself is a constant
            return true;
        } else if (array instanceof AccessField && ((AccessField) array).field().isConstant()) {
            // the length is derived from a constant array
            return true;
        } else if (array instanceof NewArray) {
            // the array is derived from an allocation
            final Value length = ((NewArray) array).length();
            return length != null && length.isConstant();
        }
        return false;
    }

    private Value appendConstant(CiConstant type) {
        return appendWithBCI(new Constant(type), bci(), false);
    }

    private Value append(Instruction x) {
        return appendWithBCI(x, bci(), C1XOptions.OptCanonicalize);
    }

    private Value appendWithoutOptimization(Instruction x, int bci) {
        return appendWithBCI(x, bci, false);
    }

    private Value appendWithBCI(Instruction x, int bci, boolean canonicalize) {
        if (canonicalize) {
            // attempt simple constant folding and strength reduction
            // Canonicalizer canon = new Canonicalizer(x, bci);
            canonicalizer.canonicalize(x);
            List<Instruction> extra = canonicalizer.extra();
            if (extra != null) {
                // the canonicalization introduced instructions that should be added before this
                for (Instruction i : extra) {
                    appendWithBCI(i, bci, false); // don't try to canonicalize the new instructions
                }
            }
            Value r = canonicalizer.canonical();
            if (r instanceof Instruction) {
                // the result is an instruction that may need to be appended
                x = (Instruction) r;
            } else {
                // the result is not an instruction (and thus cannot be appended)
                return r;
            }
        }
        if (x.isAppended()) {
            // the instruction has already been added
            return x;
        }
        if (localValueMap != null) {
            // look in the local value map
            Value r = localValueMap.findInsert(x);
            if (r != x) {
                C1XMetrics.LocalValueNumberHits++;
                if (r instanceof Instruction) {
                    assert ((Instruction) r).isAppended() : "instruction " + r + "is not appended";
                }
                return r;
            }
        }

        assert x.next() == null : "instruction should not have been appended yet";
        assert lastInstr.next() == null : "cannot append instruction to instruction which isn't end";
        lastInstr = lastInstr.setNext(x, bci);
        if (++stats.nodeCount >= C1XOptions.MaximumInstructionCount) {
            // bailout if we've exceeded the maximum inlining size
            throw new CiBailout("Method and/or inlining is too large");
        }

        if (memoryMap != null && hasUncontrollableSideEffects(x)) {
            // conservatively kill all memory if there are unknown side effects
            memoryMap.kill();
        }

        if (x.canTrap()) {
            // connect the instruction to any exception handlers
            x.setExceptionHandlers(handleException(x, bci));
        }

        return x;
    }

    private boolean hasUncontrollableSideEffects(Value x) {
        return x instanceof Invoke || x instanceof Intrinsic && !((Intrinsic) x).preservesState() || x instanceof ResolveClass;
    }

    private BlockBegin blockAt(int bci) {
        return scopeData.blockAt(bci);
    }

    boolean tryInlineJsr(int jsrStart) {
        // start a new continuation point.
        // all ret instructions will be replaced with gotos to this point
        BlockBegin cont = blockAt(nextBCI());
        assert cont != null : "continuation must exist";

        // push callee scope
        pushScopeForJsr(cont, jsrStart);

        BlockBegin jsrStartBlock = blockAt(jsrStart);
        assert jsrStartBlock != null;
        assert !jsrStartBlock.wasVisited();
        Goto gotoSub = new Goto(jsrStartBlock, null, false);
        gotoSub.setStateAfter(curState.immutableCopy());
        assert jsrStartBlock.stateBefore() == null;
        jsrStartBlock.setStateBefore(curState.copy());
        append(gotoSub);
        curBlock.setEnd(gotoSub);
        lastInstr = curBlock = jsrStartBlock;

        scopeData.addToWorkList(jsrStartBlock);

        iterateAllBlocks();

        if (cont.stateBefore() != null) {
            if (!cont.wasVisited()) {
                scopeData.parent.addToWorkList(cont);
            }
        }

        BlockBegin jsrCont = scopeData.jsrContinuation();
        assert jsrCont == cont && (!jsrCont.wasVisited() || jsrCont.isParserLoopHeader());
        assert lastInstr != null && lastInstr instanceof BlockEnd;

        // continuation is in work list, so end iteration of current block
        skipBlock = true;
        popScopeForJsr();
        C1XMetrics.InlinedJsrs++;
        return true;
    }

    void pushScopeForJsr(BlockBegin jsrCont, int jsrStart) {
        BytecodeStream stream = new BytecodeStream(scope().method.code());
        RiConstantPool constantPool = scopeData.constantPool;
        ScopeData data = new ScopeData(scopeData, scope(), scopeData.blockMap, stream, constantPool, jsrStart);
        BlockBegin continuation = scopeData.continuation();
        data.setContinuation(continuation);
        if (continuation != null) {
            assert scopeData.continuationState() != null;
            data.setContinuationState(scopeData.continuationState().copy());
        }
        data.setJsrContinuation(jsrCont);
        scopeData = data;
    }

    void pushScope(RiMethod target, BlockBegin continuation) {
        IRScope calleeScope = new IRScope(compilation, scope(), bci(), target, -1);
        scope().addCallee(calleeScope);
        BlockMap blockMap = compilation.getBlockMap(calleeScope.method, -1);

        calleeScope.setCallerState(curState);
        calleeScope.setStoresInLoops(blockMap.getStoresInLoops());
        curState = curState.pushScope(calleeScope);
        BytecodeStream stream = new BytecodeStream(target.code());
        RiConstantPool constantPool = compilation.runtime.getConstantPool(target);
        ScopeData data = new ScopeData(scopeData, calleeScope, blockMap, stream, constantPool);
        data.setContinuation(continuation);
        scopeData = data;
    }

    ValueStack stateAtEntry(RiMethod method) {
        ValueStack state = new ValueStack(scope(), method.maxLocals(), method.maxStackSize());
        int index = 0;
        if (!method.isStatic()) {
            // add the receiver and assume it is non null
            Local local = new Local(CiKind.Object, index);
            local.setFlag(Value.Flag.NonNull, true);
            local.setDeclaredType(method.holder());
            state.storeLocal(index, local);
            index = 1;
        }
        RiSignature sig = method.signatureType();
        int max = sig.argumentCount(false);
        for (int i = 0; i < max; i++) {
            RiType type = sig.argumentTypeAt(i);
            CiKind vt = type.basicType().stackType();
            Local local = new Local(vt, index);
            if (type.isLoaded()) {
                local.setDeclaredType(type);
            }
            state.storeLocal(index, local);
            index += vt.sizeInSlots();
        }
        return state;
    }

    boolean tryOptimizeCall(RiMethod target, Value[] args, boolean isStatic) {
        if (target.isLoaded()) {
            if (C1XOptions.OptIntrinsify) {
                // try to create an intrinsic node
                C1XIntrinsic intrinsic = C1XIntrinsic.getIntrinsic(target);
                if (intrinsic != null && tryInlineIntrinsic(target, args, isStatic, intrinsic)) {
                    // this method is not an intrinsic
                    return true;
                }
            }
            if (C1XOptions.CanonicalizeFoldableMethods) {
                // next try to fold the method call
                if (tryFoldable(target, args)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tryInlineIntrinsic(RiMethod target, Value[] args, boolean isStatic, C1XIntrinsic intrinsic) {
        boolean preservesState = true;
        boolean canTrap = false;

        // handle intrinsics differently
        switch (intrinsic) {
            // java.lang.Object
            case java_lang_Object$init:   // fall through
            case java_lang_Object$clone:  return false;
            // TODO: preservesState and canTrap for complex intrinsics
        }

        // get the arguments for the intrinsic
        CiKind resultType = returnBasicType(target);

        // create the intrinsic node
        Intrinsic result = new Intrinsic(resultType.stackType(), intrinsic, target, args, isStatic, curState.copy(), preservesState, canTrap);
        pushReturn(resultType, append(result));
        stats.intrinsicCount++;
        return true;
    }

    private boolean tryFoldable(RiMethod target, Value[] args) {
        CiConstant result = Canonicalizer.foldInvocation(target, args);
        if (result != null) {
            pushReturn(returnBasicType(target), append(new Constant(result)));
            return true;
        }
        return false;
    }

    boolean tryInline(RiMethod target, Value[] args, RiType knownHolder, ValueStack stateBefore) {
        return checkInliningConditions(target) && tryInlineFull(target, args, knownHolder, stateBefore);
    }

    boolean checkInliningConditions(RiMethod target) {
        if (!C1XOptions.OptInline) {
            return false; // all inlining is turned off
        }
        if (!target.hasCode()) {
            return cannotInline(target, "method has no code");
        }
        if (!target.holder().isInitialized()) {
            return cannotInline(target, "holder is not initialized");
        }
        if (recursiveInlineLevel(target) > C1XOptions.MaximumRecursiveInlineLevel) {
            return cannotInline(target, "recursive inlining too deep");
        }
        if (compilation.runtime.mustInline(target)) {
            C1XMetrics.InlineForcedMethods++;
            return true;
        }
        if (scopeData.scope.level > C1XOptions.MaximumInlineLevel) {
            return cannotInline(target, "inlining too deep");
        }
        if (stats.nodeCount > C1XOptions.MaximumDesiredSize) {
            return cannotInline(target, "compilation already too big " + "(" + compilation.stats.nodeCount + " nodes)");
        }
        if (compilation.runtime.mustNotInline(target)) {
            C1XMetrics.InlineForbiddenMethods++;
            return cannotInline(target, "inlining excluded by runtime");
        }
        if (compilation.runtime.mustNotCompile(target)) {
            return cannotInline(target, "compile excluded by runtime");
        }
        if (target.isSynchronized() && !C1XOptions.OptInlineSynchronized) {
            return cannotInline(target, "is synchronized");
        }
        if (target.hasExceptionHandlers() && !C1XOptions.OptInlineExcept) {
            return cannotInline(target, "has exception handlers");
        }
        if (!target.hasBalancedMonitors()) {
            return cannotInline(target, "has unbalanced monitors");
        }
        if (target.codeSize() > scopeData.maxInlineSize()) {
            return cannotInline(target, "inlinee too large for this level");
        }
        if ("<init>".equals(target.name()) && target.holder().isSubtypeOf(compilation.throwableType())) {
            // don't inline constructors of throwable classes unless the inlining tree is
            // rooted in a throwable class
            if (!rootScope().method.holder().isSubtypeOf(compilation.throwableType())) {
                return cannotInline(target, "don't inline Throwable constructors");
            }
        }
        return true;
    }

    boolean cannotInline(RiMethod target, String reason) {
        compilation.recordInliningFailure(target, reason);
        return false;
    }

    boolean tryInlineFull(RiMethod target, Value[] args, RiType knownHolder, ValueStack stateBefore) {
        BlockBegin orig = curBlock;
        Value receiver = null;
        if (!target.isStatic()) {
            // the receiver object must be nullchecked for instance methods
            receiver = args[0];
            if (!receiver.isNonNull()) {
                NullCheck check = new NullCheck(receiver, stateBefore);
                receiver = check;
                args[0] = check;
                append(check);
            }
        }

        // Introduce a new callee continuation point. If the target has
        // more than one return instruction or the return does not allow
        // fall-through of control flow, all return instructions will be
        // transformed to Goto's to the continuation
        BlockBegin continuationBlock = blockAt(nextBCI());
        boolean continuationExisted = true;
        if (continuationBlock == null) {
            // there was not already a block starting at the next BCI
            continuationBlock = new BlockBegin(nextBCI(), ir.nextBlockNumber());
            continuationBlock.setDepthFirstNumber(0);
            continuationExisted = false;
        }
        // record the number of predecessors before inlining, to determine
        // whether the the inlined method has added edges to the continuation
        int continuationPredecessors = continuationBlock.predecessors().size();

        // push the target scope
        pushScope(target, continuationBlock);

        // pass parameters into the callee state
        ValueStack calleeState = curState;
        for (int i = 0; i < args.length; i++) {
            Value arg = args[i];
            if (arg != null) {
                calleeState.storeLocal(i, roundFp(arg));
            }
        }

        // setup state that is used at returns from the inlined method.
        // this is essentially the state of the continuation block,
        // but without the return value on the stack.
        scopeData.setContinuationState(scope().callerState().copy());

        // compute the lock stack size for callee scope
        scope().computeLockStackSize();

        Value lock = null;
        BlockBegin syncHandler = null;
        // inline the locking code if the target method is synchronized
        if (target.isSynchronized()) {
            // lock the receiver object if it is an instance method, the class object otherwise
            lock = synchronizedObject(curState, target);
            syncHandler = new BlockBegin(Instruction.SYNCHRONIZATION_ENTRY_BCI, ir.nextBlockNumber());
            inlineSyncEntry(lock, syncHandler);
            scope().computeLockStackSize();
        }

        BlockBegin calleeStartBlock = blockAt(0);
        if (calleeStartBlock.isParserLoopHeader()) {
            // the block is a loop header, so we have to insert a goto
            Goto gotoCallee = new Goto(calleeStartBlock, null, false);
            gotoCallee.setStateAfter(curState.immutableCopy());
            appendWithoutOptimization(gotoCallee, 0);
            curBlock.setEnd(gotoCallee);
            calleeStartBlock.merge(calleeState);
            lastInstr = curBlock = calleeStartBlock;
            scopeData.addToWorkList(calleeStartBlock);
            // now iterate over all the blocks
            iterateAllBlocks();
        } else {
            // ready to resume parsing inlined method into this block
            inlineIntoCurrentBlock();
            // now iterate over the rest of the blocks
            iterateAllBlocks();
        }

        assert continuationExisted || !continuationBlock.wasVisited() : "continuation should not have been parsed if we created it";

        // At this point we are almost ready to return and resume parsing of
        // the caller back in the GraphBuilder. The only thing we want to do
        // first is an optimization: during parsing of the callee we
        // generated at least one Goto to the continuation block. If we
        // generated exactly one, and if the inlined method spanned exactly
        // one block (and we didn't have to Goto its entry), then we snip
        // off the Goto to the continuation, allowing control to fall
        // through back into the caller block and effectively performing
        // block merging. This allows load elimination and CSE to take place
        // across multiple callee scopes if they are relatively simple, and
        // is currently essential to making inlining profitable.
        if (scopeData.numberOfReturns() == 1 && curBlock == orig && curBlock == scopeData.inlineCleanupBlock()) {
            lastInstr = scopeData.inlineCleanupReturnPrev();
            curState = scopeData.inlineCleanupState().popScope();
        } else if (continuationPredecessors == continuationBlock.predecessors().size()) {
            // Inlining caused that the instructions after the invoke in the
            // caller are not reachable any more. So skip filling this block
            // with instructions!
            assert continuationBlock == scopeData.continuation();
            assert lastInstr instanceof BlockEnd;
            skipBlock = true;
        } else {
            // Resume parsing in continuation block unless it was already parsed.
            // Note that if we don't change _last here, iteration in
            // iterateBytecodesForBlock will stop when we return.
            if (!scopeData.continuation().wasVisited()) {
                // add continuation to work list instead of parsing it immediately
                assert lastInstr instanceof BlockEnd;
                scopeData.parent.addToWorkList(scopeData.continuation());
                skipBlock = true;
            }
        }

        // fill the exception handler for synchronized methods with instructions
        if (target.isSynchronized()) {
            fillSyncHandler(lock, syncHandler, true);
        } else {
            popScope();
        }

        stats.inlineCount++;
        return true;
    }

    private Value synchronizedObject(ValueStack state, RiMethod target) {
        if (target.isStatic()) {
            Constant classConstant = Constant.forObject(target.holder().javaClass());
            return appendWithoutOptimization(classConstant, Instruction.SYNCHRONIZATION_ENTRY_BCI);
        } else {
            return state.localAt(0);
        }
    }

    void inlineSyncEntry(Value lock, BlockBegin syncHandler) {
        genMonitorEnter(lock, Instruction.SYNCHRONIZATION_ENTRY_BCI);
        syncHandler.setExceptionEntry();
        syncHandler.setBlockFlag(BlockBegin.BlockFlag.IsOnWorkList);
        RiExceptionHandler handler = newDefaultExceptionHandler(method());
        ExceptionHandler h = new ExceptionHandler(handler);
        h.setEntryBlock(syncHandler);
        scopeData.addExceptionHandler(h);
    }

    void fillSyncHandler(Value lock, BlockBegin syncHandler, boolean inlinedMethod) {
        BlockBegin origBlock = curBlock;
        ValueStack origState = curState;
        Instruction origLast = lastInstr;

        lastInstr = curBlock = syncHandler;
        while (lastInstr.next() != null) {
            // go forward to the end of the block
            lastInstr = lastInstr.next();
        }
        curState = syncHandler.stateBefore().copy();

        Value exception = appendWithoutOptimization(new ExceptionObject(), Instruction.SYNCHRONIZATION_ENTRY_BCI);

        int bci = Instruction.SYNCHRONIZATION_ENTRY_BCI;
        assert lock != null;
        assert curState.locksSize() > 0 && curState.lockAt(curState.locksSize() - 1) == lock;
        if (lock instanceof Instruction) {
            Instruction l = (Instruction) lock;
            if (!l.isAppended()) {
                lock = appendWithoutOptimization(l, Instruction.SYNCHRONIZATION_ENTRY_BCI);
            }
        }
        // exit the monitor
        genMonitorExit(lock, Instruction.SYNCHRONIZATION_ENTRY_BCI);

        // exit the context of the synchronized method
        if (inlinedMethod) {
            popScope();
            bci = curState.scope().callerBCI();
            curState = curState.popScope().copy();
        }

        apush(exception);
        genThrow(bci);
        BlockEnd end = (BlockEnd) lastInstr;
        curBlock.setEnd(end);
        end.setStateAfter(curState.immutableCopy());

        curBlock = origBlock;
        curState = origState;
        lastInstr = origLast;
    }

    void inlineIntoCurrentBlock() {
        iterateBytecodesForBlock(0);
    }

    void iterateAllBlocks() {
        BlockBegin b;
        while ((b = scopeData.removeFromWorkList()) != null) {
            if (!b.wasVisited()) {
                if (b.isOsrEntry()) {
                    // this is the OSR entry block, set up edges accordingly
                    setupOsrEntryBlock();
                    // this is no longer the OSR entry block
                    b.setOsrEntry(false);
                }
                b.setWasVisited(true);
                // now parse the block
                killMemoryMap();
                curBlock = b;
                curState = b.stateBefore().copy();
                lastInstr = b;
                iterateBytecodesForBlock(b.bci());
            }
        }
    }

    void popScope() {
        int numberOfLocks = scope().numberOfLocks();
        scopeData = scopeData.parent;
        scope().setMinimumNumberOfLocks(numberOfLocks);
    }

    void popScopeForJsr() {
        scopeData = scopeData.parent;
    }

    void setupOsrEntryBlock() {
        assert compilation.isOsrCompilation();

        int osrBCI = compilation.osrBCI;
        BytecodeStream s = scopeData.stream;
        RiOsrFrame frame = compilation.getOsrFrame();
        s.setBCI(osrBCI);
        s.next(); // XXX: why go to next bytecode?

        // create a new block to contain the OSR setup code
        ir.osrEntryBlock = new BlockBegin(osrBCI, ir.nextBlockNumber());
        ir.osrEntryBlock.setOsrEntry(true);
        ir.osrEntryBlock.setDepthFirstNumber(0);

        // get the target block of the OSR
        BlockBegin target = scopeData.blockAt(osrBCI);
        assert target != null && target.isOsrEntry();

        ValueStack state = target.stateBefore().copy();
        ir.osrEntryBlock.setStateBefore(state);

        killMemoryMap();
        curBlock = ir.osrEntryBlock;
        curState = state.copy();
        lastInstr = ir.osrEntryBlock;

        // create the entry instruction which represents the OSR state buffer
        // input from interpreter / JIT
        Instruction e = new OsrEntry();
        e.setFlag(Value.Flag.NonNull, true);

        for (int i = 0; i < state.localsSize(); i++) {
            Value local = state.localAt(i);
            Value get;
            int offset = frame.getLocalOffset(i);
            if (local != null) {
                // this is a live local according to compiler
                if (local.type().isObject() && !frame.isLiveObject(i)) {
                    // the compiler thinks this is live, but not the interpreter
                    // pretend that it passed null
                    get = appendConstant(CiConstant.NULL_OBJECT);
                } else {
                    Value oc = appendConstant(CiConstant.forInt(offset));
                    get = append(new UnsafeGetRaw(local.type(), e, oc, 0, true));
                }
                state.storeLocal(i, get);
            }
        }

        assert state.scope().callerState() == null;
        state.clearLocals();
        Goto g = new Goto(target, state.copy(), false);
        append(g);
        ir.osrEntryBlock.setEnd(g);
        target.merge(ir.osrEntryBlock.end().stateAfter());
    }

    BlockEnd iterateBytecodesForBlock(int bci) {

        // Temporary variable for constant pool index
        char cpi;

        skipBlock = false;
        assert curState != null;
        BytecodeStream s = scopeData.stream;
        s.setBCI(bci);

        BlockBegin block = curBlock;
        BlockEnd end = null;
        boolean pushException = block.isExceptionEntry() && block.next() == null;
        int prevBCI = bci;
        int endBCI = s.endBCI();
        while (bci < endBCI) {
            BlockBegin nextBlock = blockAt(bci);
            if (nextBlock != null && nextBlock != block) {
                // we fell through to the next block, add a goto and break
                end = new Goto(nextBlock, null, false);
                lastInstr = lastInstr.setNext(end, prevBCI);
                break;
            }
            // read the opcode
            int opcode = s.currentBC();

            // check for active JSR during OSR compilation
            if (compilation.isOsrCompilation() && scope().isTopScope() && scopeData.parsingJsr() && s.currentBCI() == compilation.osrBCI) {
                throw new CiBailout("OSR not supported while a JSR is active");
            }

            // push an exception object onto the stack if we are parsing an exception handler
            if (pushException) {
                apush(append(new ExceptionObject()));
                pushException = false;
            }

            // Checkstyle: stop
            switch (opcode) {
                case Bytecodes.NOP            : /* nothing to do */ break;
                case Bytecodes.ACONST_NULL    : apush(appendConstant(CiConstant.NULL_OBJECT)); break;
                case Bytecodes.ICONST_M1      : ipush(appendConstant(CiConstant.INT_MINUS_1)); break;
                case Bytecodes.ICONST_0       : ipush(appendConstant(CiConstant.INT_0)); break;
                case Bytecodes.ICONST_1       : ipush(appendConstant(CiConstant.INT_1)); break;
                case Bytecodes.ICONST_2       : ipush(appendConstant(CiConstant.INT_2)); break;
                case Bytecodes.ICONST_3       : ipush(appendConstant(CiConstant.INT_3)); break;
                case Bytecodes.ICONST_4       : ipush(appendConstant(CiConstant.INT_4)); break;
                case Bytecodes.ICONST_5       : ipush(appendConstant(CiConstant.INT_5)); break;
                case Bytecodes.LCONST_0       : lpush(appendConstant(CiConstant.LONG_0)); break;
                case Bytecodes.LCONST_1       : lpush(appendConstant(CiConstant.LONG_1)); break;
                case Bytecodes.FCONST_0       : fpush(appendConstant(CiConstant.FLOAT_0)); break;
                case Bytecodes.FCONST_1       : fpush(appendConstant(CiConstant.FLOAT_1)); break;
                case Bytecodes.FCONST_2       : fpush(appendConstant(CiConstant.FLOAT_2)); break;
                case Bytecodes.DCONST_0       : dpush(appendConstant(CiConstant.DOUBLE_0)); break;
                case Bytecodes.DCONST_1       : dpush(appendConstant(CiConstant.DOUBLE_1)); break;
                case Bytecodes.BIPUSH         : ipush(appendConstant(CiConstant.forInt(s.readByte()))); break;
                case Bytecodes.SIPUSH         : ipush(appendConstant(CiConstant.forInt(s.readShort()))); break;
                case Bytecodes.LDC            : // fall through
                case Bytecodes.LDC_W          : // fall through
                case Bytecodes.LDC2_W         : genLoadConstant(stream().readCPI()); break;
                case Bytecodes.ILOAD          : loadLocal(s.readLocalIndex(), CiKind.Int); break;
                case Bytecodes.LLOAD          : loadLocal(s.readLocalIndex(), CiKind.Long); break;
                case Bytecodes.FLOAD          : loadLocal(s.readLocalIndex(), CiKind.Float); break;
                case Bytecodes.DLOAD          : loadLocal(s.readLocalIndex(), CiKind.Double); break;
                case Bytecodes.ALOAD          : loadLocal(s.readLocalIndex(), CiKind.Object); break;
                case Bytecodes.ILOAD_0        : loadLocal(0, CiKind.Int); break;
                case Bytecodes.ILOAD_1        : loadLocal(1, CiKind.Int); break;
                case Bytecodes.ILOAD_2        : loadLocal(2, CiKind.Int); break;
                case Bytecodes.ILOAD_3        : loadLocal(3, CiKind.Int); break;
                case Bytecodes.LLOAD_0        : loadLocal(0, CiKind.Long); break;
                case Bytecodes.LLOAD_1        : loadLocal(1, CiKind.Long); break;
                case Bytecodes.LLOAD_2        : loadLocal(2, CiKind.Long); break;
                case Bytecodes.LLOAD_3        : loadLocal(3, CiKind.Long); break;
                case Bytecodes.FLOAD_0        : loadLocal(0, CiKind.Float); break;
                case Bytecodes.FLOAD_1        : loadLocal(1, CiKind.Float); break;
                case Bytecodes.FLOAD_2        : loadLocal(2, CiKind.Float); break;
                case Bytecodes.FLOAD_3        : loadLocal(3, CiKind.Float); break;
                case Bytecodes.DLOAD_0        : loadLocal(0, CiKind.Double); break;
                case Bytecodes.DLOAD_1        : loadLocal(1, CiKind.Double); break;
                case Bytecodes.DLOAD_2        : loadLocal(2, CiKind.Double); break;
                case Bytecodes.DLOAD_3        : loadLocal(3, CiKind.Double); break;
                case Bytecodes.ALOAD_0        : loadLocal(0, CiKind.Object); break;
                case Bytecodes.ALOAD_1        : loadLocal(1, CiKind.Object); break;
                case Bytecodes.ALOAD_2        : loadLocal(2, CiKind.Object); break;
                case Bytecodes.ALOAD_3        : loadLocal(3, CiKind.Object); break;
                case Bytecodes.IALOAD         : genLoadIndexed(CiKind.Int   ); break;
                case Bytecodes.LALOAD         : genLoadIndexed(CiKind.Long  ); break;
                case Bytecodes.FALOAD         : genLoadIndexed(CiKind.Float ); break;
                case Bytecodes.DALOAD         : genLoadIndexed(CiKind.Double); break;
                case Bytecodes.AALOAD         : genLoadIndexed(CiKind.Object); break;
                case Bytecodes.BALOAD         : genLoadIndexed(CiKind.Byte  ); break;
                case Bytecodes.CALOAD         : genLoadIndexed(CiKind.Char  ); break;
                case Bytecodes.SALOAD         : genLoadIndexed(CiKind.Short ); break;
                case Bytecodes.ISTORE         : storeLocal(CiKind.Int, s.readLocalIndex()); break;
                case Bytecodes.LSTORE         : storeLocal(CiKind.Long, s.readLocalIndex()); break;
                case Bytecodes.FSTORE         : storeLocal(CiKind.Float, s.readLocalIndex()); break;
                case Bytecodes.DSTORE         : storeLocal(CiKind.Double, s.readLocalIndex()); break;
                case Bytecodes.ASTORE         : storeLocal(CiKind.Object, s.readLocalIndex()); break;
                case Bytecodes.ISTORE_0       : storeLocal(CiKind.Int, 0); break;
                case Bytecodes.ISTORE_1       : storeLocal(CiKind.Int, 1); break;
                case Bytecodes.ISTORE_2       : storeLocal(CiKind.Int, 2); break;
                case Bytecodes.ISTORE_3       : storeLocal(CiKind.Int, 3); break;
                case Bytecodes.LSTORE_0       : storeLocal(CiKind.Long, 0); break;
                case Bytecodes.LSTORE_1       : storeLocal(CiKind.Long, 1); break;
                case Bytecodes.LSTORE_2       : storeLocal(CiKind.Long, 2); break;
                case Bytecodes.LSTORE_3       : storeLocal(CiKind.Long, 3); break;
                case Bytecodes.FSTORE_0       : storeLocal(CiKind.Float, 0); break;
                case Bytecodes.FSTORE_1       : storeLocal(CiKind.Float, 1); break;
                case Bytecodes.FSTORE_2       : storeLocal(CiKind.Float, 2); break;
                case Bytecodes.FSTORE_3       : storeLocal(CiKind.Float, 3); break;
                case Bytecodes.DSTORE_0       : storeLocal(CiKind.Double, 0); break;
                case Bytecodes.DSTORE_1       : storeLocal(CiKind.Double, 1); break;
                case Bytecodes.DSTORE_2       : storeLocal(CiKind.Double, 2); break;
                case Bytecodes.DSTORE_3       : storeLocal(CiKind.Double, 3); break;
                case Bytecodes.ASTORE_0       : storeLocal(CiKind.Object, 0); break;
                case Bytecodes.ASTORE_1       : storeLocal(CiKind.Object, 1); break;
                case Bytecodes.ASTORE_2       : storeLocal(CiKind.Object, 2); break;
                case Bytecodes.ASTORE_3       : storeLocal(CiKind.Object, 3); break;
                case Bytecodes.IASTORE        : genStoreIndexed(CiKind.Int   ); break;
                case Bytecodes.LASTORE        : genStoreIndexed(CiKind.Long  ); break;
                case Bytecodes.FASTORE        : genStoreIndexed(CiKind.Float ); break;
                case Bytecodes.DASTORE        : genStoreIndexed(CiKind.Double); break;
                case Bytecodes.AASTORE        : genStoreIndexed(CiKind.Object); break;
                case Bytecodes.BASTORE        : genStoreIndexed(CiKind.Byte  ); break;
                case Bytecodes.CASTORE        : genStoreIndexed(CiKind.Char  ); break;
                case Bytecodes.SASTORE        : genStoreIndexed(CiKind.Short ); break;
                case Bytecodes.POP            : // fall through
                case Bytecodes.POP2           : // fall through
                case Bytecodes.DUP            : // fall through
                case Bytecodes.DUP_X1         : // fall through
                case Bytecodes.DUP_X2         : // fall through
                case Bytecodes.DUP2           : // fall through
                case Bytecodes.DUP2_X1        : // fall through
                case Bytecodes.DUP2_X2        : // fall through
                case Bytecodes.SWAP           : stackOp(opcode); break;
                case Bytecodes.IADD           : // fall through
                case Bytecodes.ISUB           : // fall through
                case Bytecodes.IMUL           : genArithmeticOp(CiKind.Int, opcode); break;
                case Bytecodes.IDIV           : // fall through
                case Bytecodes.IREM           : genArithmeticOp(CiKind.Int, opcode, curState.copy()); break;
                case Bytecodes.LADD           : // fall through
                case Bytecodes.LSUB           : // fall through
                case Bytecodes.LMUL           : genArithmeticOp(CiKind.Long, opcode); break;
                case Bytecodes.LDIV           : // fall through
                case Bytecodes.LREM           : genArithmeticOp(CiKind.Long, opcode, curState.copy()); break;
                case Bytecodes.FADD           : // fall through
                case Bytecodes.FSUB           : // fall through
                case Bytecodes.FMUL           : // fall through
                case Bytecodes.FDIV           : // fall through
                case Bytecodes.FREM           : genArithmeticOp(CiKind.Float, opcode); break;
                case Bytecodes.DADD           : // fall through
                case Bytecodes.DSUB           : // fall through
                case Bytecodes.DMUL           : // fall through
                case Bytecodes.DDIV           : // fall through
                case Bytecodes.DREM           : genArithmeticOp(CiKind.Double, opcode); break;
                case Bytecodes.INEG           : genNegateOp(CiKind.Int); break;
                case Bytecodes.LNEG           : genNegateOp(CiKind.Long); break;
                case Bytecodes.FNEG           : genNegateOp(CiKind.Float); break;
                case Bytecodes.DNEG           : genNegateOp(CiKind.Double); break;
                case Bytecodes.ISHL           : // fall through
                case Bytecodes.ISHR           : // fall through
                case Bytecodes.IUSHR          : genShiftOp(CiKind.Int, opcode); break;
                case Bytecodes.IAND           : // fall through
                case Bytecodes.IOR            : // fall through
                case Bytecodes.IXOR           : genLogicOp(CiKind.Int, opcode); break;
                case Bytecodes.LSHL           : // fall through
                case Bytecodes.LSHR           : // fall through
                case Bytecodes.LUSHR          : genShiftOp(CiKind.Long, opcode); break;
                case Bytecodes.LAND           : // fall through
                case Bytecodes.LOR            : // fall through
                case Bytecodes.LXOR           : genLogicOp(CiKind.Long, opcode); break;
                case Bytecodes.IINC           : genIncrement(); break;
                case Bytecodes.I2L            : genConvert(opcode, CiKind.Int   , CiKind.Long  ); break;
                case Bytecodes.I2F            : genConvert(opcode, CiKind.Int   , CiKind.Float ); break;
                case Bytecodes.I2D            : genConvert(opcode, CiKind.Int   , CiKind.Double); break;
                case Bytecodes.L2I            : genConvert(opcode, CiKind.Long  , CiKind.Int   ); break;
                case Bytecodes.L2F            : genConvert(opcode, CiKind.Long  , CiKind.Float ); break;
                case Bytecodes.L2D            : genConvert(opcode, CiKind.Long  , CiKind.Double); break;
                case Bytecodes.F2I            : genConvert(opcode, CiKind.Float , CiKind.Int   ); break;
                case Bytecodes.F2L            : genConvert(opcode, CiKind.Float , CiKind.Long  ); break;
                case Bytecodes.F2D            : genConvert(opcode, CiKind.Float , CiKind.Double); break;
                case Bytecodes.D2I            : genConvert(opcode, CiKind.Double, CiKind.Int   ); break;
                case Bytecodes.D2L            : genConvert(opcode, CiKind.Double, CiKind.Long  ); break;
                case Bytecodes.D2F            : genConvert(opcode, CiKind.Double, CiKind.Float ); break;
                case Bytecodes.I2B            : genConvert(opcode, CiKind.Int   , CiKind.Byte  ); break;
                case Bytecodes.I2C            : genConvert(opcode, CiKind.Int   , CiKind.Char  ); break;
                case Bytecodes.I2S            : genConvert(opcode, CiKind.Int   , CiKind.Short ); break;
                case Bytecodes.LCMP           : genCompareOp(CiKind.Long, opcode); break;
                case Bytecodes.FCMPL          : genCompareOp(CiKind.Float, opcode); break;
                case Bytecodes.FCMPG          : genCompareOp(CiKind.Float, opcode); break;
                case Bytecodes.DCMPL          : genCompareOp(CiKind.Double, opcode); break;
                case Bytecodes.DCMPG          : genCompareOp(CiKind.Double, opcode); break;
                case Bytecodes.IFEQ           : genIfZero(Condition.eql); break;
                case Bytecodes.IFNE           : genIfZero(Condition.neq); break;
                case Bytecodes.IFLT           : genIfZero(Condition.lss); break;
                case Bytecodes.IFGE           : genIfZero(Condition.geq); break;
                case Bytecodes.IFGT           : genIfZero(Condition.gtr); break;
                case Bytecodes.IFLE           : genIfZero(Condition.leq); break;
                case Bytecodes.IF_ICMPEQ      : genIfSame(CiKind.Int, Condition.eql); break;
                case Bytecodes.IF_ICMPNE      : genIfSame(CiKind.Int, Condition.neq); break;
                case Bytecodes.IF_ICMPLT      : genIfSame(CiKind.Int, Condition.lss); break;
                case Bytecodes.IF_ICMPGE      : genIfSame(CiKind.Int, Condition.geq); break;
                case Bytecodes.IF_ICMPGT      : genIfSame(CiKind.Int, Condition.gtr); break;
                case Bytecodes.IF_ICMPLE      : genIfSame(CiKind.Int, Condition.leq); break;
                case Bytecodes.IF_ACMPEQ      : genIfSame(CiKind.Object, Condition.eql); break;
                case Bytecodes.IF_ACMPNE      : genIfSame(CiKind.Object, Condition.neq); break;
                case Bytecodes.GOTO           : genGoto(s.currentBCI(), s.readBranchDest()); break;
                case Bytecodes.JSR            : genJsr(s.readBranchDest()); break;
                case Bytecodes.RET            : genRet(s.readLocalIndex()); break;
                case Bytecodes.TABLESWITCH    : genTableswitch(); break;
                case Bytecodes.LOOKUPSWITCH   : genLookupswitch(); break;
                case Bytecodes.IRETURN        : genMethodReturn(ipop()); break;
                case Bytecodes.LRETURN        : genMethodReturn(lpop()); break;
                case Bytecodes.FRETURN        : genMethodReturn(fpop()); break;
                case Bytecodes.DRETURN        : genMethodReturn(dpop()); break;
                case Bytecodes.ARETURN        : genMethodReturn(apop()); break;
                case Bytecodes.RETURN         : genMethodReturn(null  ); break;
                case Bytecodes.GETSTATIC      : genGetStatic(stream().readCPI()); break;
                case Bytecodes.PUTSTATIC      : genPutStatic(stream().readCPI()); break;
                case Bytecodes.GETFIELD       : genGetField(stream().readCPI()); break;
                case Bytecodes.PUTFIELD       : genPutField(stream().readCPI()); break;
                case Bytecodes.INVOKEVIRTUAL  : cpi = s.readCPI(); genInvokeVirtual(constantPool().lookupInvokeVirtual(cpi), cpi, constantPool()); break;
                case Bytecodes.INVOKESPECIAL  : cpi = s.readCPI(); genInvokeSpecial(constantPool().lookupInvokeSpecial(cpi), null, cpi, constantPool()); break;
                case Bytecodes.INVOKESTATIC   : cpi = s.readCPI(); genInvokeStatic(constantPool().lookupInvokeStatic(cpi), cpi, constantPool()); break;
                case Bytecodes.INVOKEINTERFACE: cpi = s.readCPI(); genInvokeInterface(constantPool().lookupInvokeInterface(cpi), cpi, constantPool()); break;
                case Bytecodes.NEW            : genNewInstance(stream().readCPI()); break;
                case Bytecodes.NEWARRAY       : genNewTypeArray(stream().readLocalIndex()); break;
                case Bytecodes.ANEWARRAY      : genNewObjectArray(stream().readCPI()); break;
                case Bytecodes.ARRAYLENGTH    : genArrayLength(); break;
                case Bytecodes.ATHROW         : genThrow(s.currentBCI()); break;
                case Bytecodes.CHECKCAST      : genCheckCast(); break;
                case Bytecodes.INSTANCEOF     : genInstanceOf(); break;
                case Bytecodes.MONITORENTER   : genMonitorEnter(s.currentBCI(), curState.copy()); break;
                case Bytecodes.MONITOREXIT    : genMonitorExit(s.currentBCI(), curState.copy()); break;
                case Bytecodes.MULTIANEWARRAY : genNewMultiArray(stream().readCPI()); break;
                case Bytecodes.IFNULL         : genIfNull(Condition.eql); break;
                case Bytecodes.IFNONNULL      : genIfNull(Condition.neq); break;
                case Bytecodes.GOTO_W         : genGoto(s.currentBCI(), s.readFarBranchDest()); break;
                case Bytecodes.JSR_W          : genJsr(s.readFarBranchDest()); break;
                case Bytecodes.BREAKPOINT:
                    throw new CiBailout("concurrent setting of breakpoint");
                default:
                    throw new CiBailout("unknown bytecode " + opcode);
            }
            // Checkstyle: resume

            prevBCI = bci;
            s.next();

            if (lastInstr instanceof BlockEnd) {
                end = (BlockEnd) lastInstr;
                break;
            }
            bci = s.currentBCI();
        }

        // stop processing of this block
        if (skipBlock) {
            skipBlock = false;
            return (BlockEnd) lastInstr;
        }

        // if the method terminates, we don't need the stack anymore
        if (end instanceof Return) {
            curState.clearStack();
        } else if (end instanceof Throw) {
            // may have exception handlers in caller scopes
            curState.truncateStack(scope().lockStackSize());
        }

        // connect to begin and set state
        // NOTE that inlining may have changed the block we are parsing
        assert end != null : "end should exist after iterating over bytecodes";
        // assert curBlock.end() == null : "block already has an end";
        end.setStateAfter(curState.immutableCopy());
        curBlock.setEnd(end);
        // propagate the state
        for (BlockBegin succ : end.successors()) {
            assert succ.predecessors().contains(curBlock);
            succ.merge(end.stateAfter());
            scopeData.addToWorkList(succ);
        }
        return end;
    }

    private void genArrayLength() {
        ipush(append(new ArrayLength(apop(), curState.copy())));
    }

    private void genMonitorEnter(int bci, ValueStack stateBefore) {
        Value object = apop();
        appendWithoutOptimization(new MonitorEnter(object, curState.lock(scope(), object), stateBefore), bci);
        killMemoryMap(); // prevent any optimizations across synchronization
    }

    void killMemoryMap() {
        if (localValueMap != null) {
            localValueMap.killAll();
        }
        if (memoryMap != null) {
            memoryMap.kill();
        }
    }

    boolean assumeLeafClass(RiType type) {
        if (!C1XOptions.TestSlowPath && type.isLoaded()) {
            if (type.isFinal()) {
                return true;
            }
            if (C1XOptions.UseDeopt && C1XOptions.OptCHA) {
                if (!type.hasSubclass() && !type.isInterface()) {
                    return compilation.recordLeafTypeAssumption(type);
                }
            }
        }
        return false;
    }

    boolean assumeLeafMethod(RiMethod method) {
        if (!C1XOptions.TestSlowPath && method.isLoaded()) {
            if (method.isFinalMethod()) {
                return true;
            }
            if (C1XOptions.UseDeopt && C1XOptions.OptLeafMethods) {
                if (!method.isOverridden() && !method.holder().isInterface()) {
                    return compilation.recordLeafMethodAssumption(method);
                }
            }
        }
        return false;
    }

    int recursiveInlineLevel(RiMethod target) {
        int rec = 0;
        IRScope scope = scope();
        while (scope != null) {
            if (scope.method != target) {
                break;
            }
            scope = scope.caller;
            rec++;
        }
        return rec;
    }

    RiConstantPool constantPool() {
        return scopeData.constantPool;
    }
}
