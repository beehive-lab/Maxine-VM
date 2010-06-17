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

import static com.sun.cri.bytecode.Bytecodes.*;
import static java.lang.reflect.Modifier.*;

import java.lang.reflect.*;
import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.graph.ScopeData.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.opt.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * The {@code GraphBuilder} class parses the bytecode of a method and builds the IR graph.
 * A number of optimizations may be performed during parsing of the bytecode, including value
 * numbering, inlining, constant folding, strength reduction, etc.
 *
 * @author Ben L. Titzer
 */
public final class GraphBuilder {

    /**
     * The minimum value to which {@link C1XOptions#TraceBytecodeParserLevel} must be set to trace
     * the bytecode instructions as they are parsed.
     */
    public static final int TRACELEVEL_INSTRUCTIONS = 1;

    /**
     * The minimum value to which {@link C1XOptions#TraceBytecodeParserLevel} must be set to trace
     * the frame state before each bytecode instruction as it is parsed.
     */
    public static final int TRACELEVEL_STATE = 2;

    final IR ir;
    final C1XCompilation compilation;
    final CiStatistics stats;

    /**
     * Map used to implement local value numbering for the current block.
     */
    final ValueMap localValueMap;

    /**
     * Map used for local load elimination (i.e. within the current block).
     */
    final MemoryMap memoryMap;

    final Canonicalizer canonicalizer;     // canonicalizer which does strength reduction + constant folding
    ScopeData scopeData;                   // Per-scope data; used for inlining
    BlockBegin curBlock;                   // the current block
    FrameState curState;                   // the current execution state
    Instruction lastInstr;                 // the last instruction added
    final LogStream log;
    final int traceLevel;

    boolean skipBlock;                     // skip processing of the rest of this block
    private Value rootMethodSynchronizedObject;

    /**
     * Creates a new, initialized, {@code GraphBuilder} instance for a given compilation.
     *
     * @param compilation the compilation
     * @param ir the IR to build the graph into
     */
    public GraphBuilder(C1XCompilation compilation, IR ir) {
        this.compilation = compilation;
        this.ir = ir;
        this.stats = compilation.stats;
        this.memoryMap = C1XOptions.OptLocalLoadElimination ? new MemoryMap() : null;
        this.localValueMap = C1XOptions.OptLocalValueNumbering ? new ValueMap() : null;
        this.canonicalizer = C1XOptions.OptCanonicalize ? new Canonicalizer(compilation.runtime, compilation.method, compilation.target) : null;
        traceLevel = C1XOptions.TraceBytecodeParserLevel;
        log = traceLevel > 0 ? new LogStream(TTY.out()) : null;
    }

    /**
     * Builds the graph for a the specified {@code IRScope}.
     * @param scope the top IRScope
     */
    public void build(IRScope scope) {
        RiMethod rootMethod = compilation.method;

        if (log != null) {
            log.println();
            log.println("Compiling " + compilation.method);
        }

        // 1. create the start block
        ir.startBlock = new BlockBegin(0, ir.nextBlockNumber());
        BlockBegin startBlock = ir.startBlock;

        // 2. compute the block map and get the entrypoint(s)
        BlockMap blockMap = compilation.getBlockMap(scope.method, compilation.osrBCI);
        BlockBegin stdEntry = blockMap.get(0);
        BlockBegin osrEntry = compilation.osrBCI < 0 ? null : blockMap.get(compilation.osrBCI);
        pushRootScope(scope, blockMap, startBlock);
        FrameState initialState = stateAtEntry(rootMethod);
        startBlock.merge(initialState);
        BlockBegin syncHandler = null;

        // 3. setup internal state for appending instructions
        curBlock = startBlock;
        lastInstr = startBlock;
        curState = initialState;

        if (isSynchronized(rootMethod.accessFlags())) {
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

            ExceptionHandler h = new ExceptionHandler(new CiExceptionHandler(0, rootMethod.code().length, -1, 0, null));
            h.setEntryBlock(syncHandler);
            scopeData.addExceptionHandler(h);
        } else {
            // 4B.1 simply finish the start block
            finishStartBlock(startBlock, stdEntry, osrEntry);
        }

        // 5.
        scope().computeLockStackSize();
        C1XIntrinsic intrinsic = C1XIntrinsic.getIntrinsic(rootMethod);
        if (intrinsic != null) {
            // 6A.1 the root method is an intrinsic; load the parameters onto the stack and try to inline it
            curState = initialState.copy();
            lastInstr = curBlock;
            if (C1XOptions.OptIntrinsify) {
                // try to inline an Intrinsic node
                boolean isStatic = Modifier.isStatic(rootMethod.accessFlags());
                int argsSize = rootMethod.signature().argumentSlots(!isStatic);
                Value[] args = new Value[argsSize];
                for (int i = 0; i < args.length; i++) {
                    args[i] = curState.localAt(i);
                }
                if (tryInlineIntrinsic(rootMethod, args, isStatic, intrinsic)) {
                    // intrinsic inlining succeeded, add the return node
                    CiKind rt = returnKind(rootMethod);
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
        FrameState stateAfter = curState.immutableCopy();
        base.setStateAfter(stateAfter);
        startBlock.setEnd(base);
        assert stdEntry.stateBefore() == null;
        stdEntry.merge(stateAfter);
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

    void wpush(Value x) {
        curState.wpush(x);
    }

    void push(CiKind kind, Value x) {
        curState.push(kind, x);
    }

    void pushReturn(CiKind kind, Value x) {
        if (kind != CiKind.Void) {
            curState.push(kind.stackKind(), x);
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

    Value wpop() {
        return curState.wpop();
    }

    Value pop(CiKind kind) {
        return curState.pop(kind);
    }

    void loadLocal(int index, CiKind kind) {
        push(kind, curState.loadLocal(index));
    }

    void storeLocal(CiKind kind, int index) {
        if (scopeData.parsingJsr()) {
            // We need to do additional tracking of the location of the return
            // address for jsrs since we don't handle arbitrary jsr/ret
            // constructs. Here we are figuring out in which circumstances we
            // need to bail out.
            if (kind == CiKind.Object) {
                // might be storing the JSR return address
                Value x = curState.xpop();
                if (x.kind.isJsr()) {
                    setJsrReturnAddressLocal(index);
                    curState.storeLocal(index, x);
                } else {
                    // nope, not storing the JSR return address
                    assert x.kind.isObject();
                    curState.storeLocal(index, x);
                    overwriteJsrReturnAddressLocal(index);
                }
                return;
            } else {
                // not storing the JSR return address local, but might overwrite it
                overwriteJsrReturnAddressLocal(index);
            }
        }

        curState.storeLocal(index, roundFp(pop(kind)));
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

    /**
     * Adds extra node to round a floating point value if necessary to comply with the requirements of the {@code strictfp} keyword.
     */
    Value roundFp(Value x) {
        // C1X assumes that all X86 backends support SSE2
        return x;
    }

    List<ExceptionHandler> handleException(Instruction x, int bci) {
        if (!hasHandler()) {
            return Util.uncheckedCast(Collections.EMPTY_LIST);
        }

        ArrayList<ExceptionHandler> exceptionHandlers = new ArrayList<ExceptionHandler>();
        ScopeData curScopeData = scopeData;
        FrameState state = x.stateBefore();
        int scopeCount = 0;

        assert state != null : "exception handler state must be available for " + x;
        state = state.immutableCopy();
        do {
            assert curScopeData.scope == state.scope() : "scopes do not match";
            assert bci == Instruction.SYNCHRONIZATION_ENTRY_BCI || bci == curScopeData.stream.currentBCI() : "invalid bci";

            // join with all potential exception handlers
            List<ExceptionHandler> handlers = curScopeData.exceptionHandlers();
            if (handlers != null) {
                for (ExceptionHandler handler : handlers) {
                    if (handler.covers(bci)) {
                        // if the handler covers this bytecode index, add it to the list
                        if (addExceptionHandler(exceptionHandlers, handler, curScopeData, state, scopeCount)) {
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
            state = state.popScope();
            bci = curScopeData.scope.callerBCI();
            curScopeData = curScopeData.parent;
            scopeCount++;

        } while (true);

        return exceptionHandlers;
    }

    /**
     * Adds an exception handler to the {@linkplain BlockBegin#exceptionHandlerBlocks() list}
     * of exception handlers for the {@link #curBlock current block}.
     *
     * @param exceptionHandlers
     * @param handler
     * @param curScopeData
     * @param curState
     * @param scopeCount
     * @return {@code true} if handler catches all exceptions (i.e. {@code handler.isCatchAll() == true})
     */
    private boolean addExceptionHandler(ArrayList<ExceptionHandler> exceptionHandlers, ExceptionHandler handler, ScopeData curScopeData, FrameState curState, int scopeCount) {
        compilation.setHasExceptionHandlers();

        BlockBegin entry = handler.entryBlock();
        FrameState entryState = entry.stateBefore();

        assert entry.bci() == handler.handler.handlerBCI();
        assert entry.bci() == -1 || entry == curScopeData.blockAt(entry.bci()) : "blocks must correspond";
        assert entryState == null || curState.locksSize() == entryState.locksSize() : "locks do not match";

        // exception handler starts with an empty expression stack
        curState.truncateStack(curScopeData.callerStackSize());

        entry.merge(curState);

        // add current state for correct handling of phi functions
        int phiOperand = entry.addExceptionState(curState);

        // add entry to the list of exception handlers of this block
        curBlock.addExceptionHandler(entry);

        // add back-edge from exception handler entry to this block
        if (!entry.predecessors().contains(curBlock)) {
            entry.addPredecessor(curBlock);
        }

        // clone exception handler
        ExceptionHandler newHandler = new ExceptionHandler(handler);
        newHandler.setPhiOperand(phiOperand);
        newHandler.setScopeCount(scopeCount);
        exceptionHandlers.add(newHandler);

        // fill in exception handler subgraph lazily
        if (!entry.wasVisited()) {
            curScopeData.addToWorkList(entry);
        } else {
            // This will occur for exception handlers that cover themselves. This code
            // pattern is generated by javac for synchronized blocks. See the following
            // for why this change to javac was made:
            //
            //   http://www.cs.arizona.edu/projects/sumatra/hallofshame/java-async-race.html
        }

        // stop when reaching catch all
        return handler.isCatchAll();
    }

    void genLoadConstant(int cpi) {
        FrameState stateBefore = curState.immutableCopy();
        Object con = constantPool().lookupConstant(cpi);

        if (con instanceof RiType) {
            // this is a load of class constant which might be unresolved
            RiType riType = (RiType) con;
            if (!riType.isResolved() || C1XOptions.TestPatching) {
                push(CiKind.Object, append(new ResolveClass(riType, RiType.Representation.JavaClass, stateBefore)));
            } else {
                push(CiKind.Object, append(Constant.forObject(riType.javaClass())));
            }
        } else if (con instanceof CiConstant) {
            CiConstant constant = (CiConstant) con;
            push(constant.kind.stackKind(), appendConstant(constant));
        } else {
            throw new Error("lookupConstant returned an object of incorrect type");
        }
    }

    void genLoadIndexed(CiKind kind) {
        FrameState stateBefore = curState.immutableCopy();
        Value index = ipop();
        Value array = apop();
        Value length = null;
        if (cseArrayLength(array)) {
            length = append(new ArrayLength(array, stateBefore));
        }
        push(kind.stackKind(), append(new LoadIndexed(array, index, length, kind, stateBefore)));
    }

    void genStoreIndexed(CiKind kind) {
        FrameState stateBefore = curState.immutableCopy();
        Value value = pop(kind.stackKind());
        Value index = ipop();
        Value array = apop();
        Value length = null;
        if (cseArrayLength(array)) {
            length = append(new ArrayLength(array, stateBefore));
        }
        StoreIndexed result = new StoreIndexed(array, index, length, kind, value, stateBefore);
        append(result);
        if (memoryMap != null) {
            memoryMap.storeValue(value);
        }
    }

    void stackOp(int opcode) {
        switch (opcode) {
            case POP: {
                curState.xpop();
                break;
            }
            case POP2: {
                curState.xpop();
                curState.xpop();
                break;
            }
            case DUP: {
                Value w = curState.xpop();
                curState.xpush(w);
                curState.xpush(w);
                break;
            }
            case DUP_X1: {
                Value w1 = curState.xpop();
                Value w2 = curState.xpop();
                curState.xpush(w1);
                curState.xpush(w2);
                curState.xpush(w1);
                break;
            }
            case DUP_X2: {
                Value w1 = curState.xpop();
                Value w2 = curState.xpop();
                Value w3 = curState.xpop();
                curState.xpush(w1);
                curState.xpush(w3);
                curState.xpush(w2);
                curState.xpush(w1);
                break;
            }
            case DUP2: {
                Value w1 = curState.xpop();
                Value w2 = curState.xpop();
                curState.xpush(w2);
                curState.xpush(w1);
                curState.xpush(w2);
                curState.xpush(w1);
                break;
            }
            case DUP2_X1: {
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
            case DUP2_X2: {
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
            case SWAP: {
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

    void genArithmeticOp(CiKind kind, int opcode) {
        genArithmeticOp(kind, opcode, null);
    }

    void genArithmeticOp(CiKind kind, int opcode, FrameState state) {
        genArithmeticOp(kind, opcode, kind, kind, state);
    }

    void genArithmeticOp(CiKind result, int opcode, CiKind x, CiKind y, FrameState state) {
        Value yValue = pop(y);
        Value xValue = pop(x);
        Value result1 = append(new ArithmeticOp(opcode, result, xValue, yValue, isStrict(method().accessFlags()), state));
        push(result, result1);
    }

    void genNegateOp(CiKind kind) {
        push(kind, append(new NegateOp(pop(kind))));
    }

    void genShiftOp(CiKind kind, int opcode) {
        Value s = ipop();
        Value x = pop(kind);
        // note that strength reduction of e << K >>> K is correctly handled in canonicalizer now
        push(kind, append(new ShiftOp(opcode, x, s)));
    }

    void genLogicOp(CiKind kind, int opcode) {
        Value y = pop(kind);
        Value x = pop(kind);
        push(kind, append(new LogicOp(opcode, x, y)));
    }

    void genCompareOp(CiKind kind, int opcode) {
        FrameState stateBefore = curState.immutableCopy();
        Value y = pop(kind);
        Value x = pop(kind);
        ipush(append(new CompareOp(opcode, x, y, stateBefore)));
    }

    void genUnsignedCompareOp(CiKind kind, int opcode, int op) {
        FrameState stateBefore = curState.immutableCopy();
        Value y = pop(kind);
        Value x = pop(kind);
        ipush(append(new UnsignedCompareOp(opcode, op, x, y, stateBefore)));
    }

    void genConvert(int opcode, CiKind from, CiKind to) {
        CiKind tt = to.stackKind();
        push(tt, append(new Convert(opcode, pop(from.stackKind()), tt)));
    }

    void genIncrement() {
        int index = stream().readLocalIndex();
        int delta = stream().readIncrement();
        Value x = curState.localAt(index);
        Value y = append(Constant.forInt(delta));
        curState.storeLocal(index, append(new ArithmeticOp(IADD, CiKind.Int, x, y, isStrict(method().accessFlags()), null)));
    }

    void genGoto(int fromBCI, int toBCI) {
        append(new Goto(blockAt(toBCI), null, toBCI <= fromBCI)); // backwards branch => safepoint
    }

    void ifNode(Value x, Condition cond, Value y, FrameState stateBefore) {
        BlockBegin tsucc = blockAt(stream().readBranchDest());
        BlockBegin fsucc = blockAt(stream().nextBCI());
        int bci = stream().currentBCI();
        boolean isBackwards = tsucc.bci() <= bci || fsucc.bci() <= bci;
        append(new If(x, cond, false, y, tsucc, fsucc, isBackwards ? stateBefore : null, isBackwards));
    }

    void genIfZero(Condition cond) {
        FrameState stateBefore = curState.immutableCopy();
        Value y = appendConstant(CiConstant.INT_0);
        Value x = ipop();
        ifNode(x, cond, y, stateBefore);
    }

    void genIfNull(Condition cond) {
        FrameState stateBefore = curState.immutableCopy();
        Value y = appendConstant(CiConstant.NULL_OBJECT);
        Value x = apop();
        ifNode(x, cond, y, stateBefore);
    }

    void genIfSame(CiKind kind, Condition cond) {
        FrameState stateBefore = curState.immutableCopy();
        Value y = pop(kind);
        Value x = pop(kind);
        ifNode(x, cond, y, stateBefore);
    }

    void genThrow(int bci) {
        FrameState stateBefore = curState.immutableCopy();
        Throw t = new Throw(apop(), stateBefore);
        appendWithoutOptimization(t, bci);
    }

    void genUnsafeCast(RiMethod method) {
        RiSignature signature = method.signature();
        int argCount = signature.argumentCount(false);
        RiType accessingClass = scope().method.holder();
        RiType fromType;
        RiType toType = signature.returnType(accessingClass);
        if (argCount == 1) {
            fromType = signature.argumentTypeAt(0, accessingClass);
        } else {
            assert argCount == 0 : "method with @UNSAFE_CAST must have exactly 1 argument";
            fromType = method.holder();
        }
        curState.unsafe = true;
        CiKind from = fromType.kind();
        CiKind to = toType.kind();
        curState.push(to, append(new UnsafeCast(toType, curState.pop(from))));
    }

    void genCheckCast() {
        FrameState stateBefore = curState.immutableCopy();
        int cpi = stream().readCPI();
        RiType type = constantPool().lookupType(cpi, CHECKCAST);
        Value typeInstruction = genResolveClass(RiType.Representation.ObjectHub, type, !C1XOptions.TestPatching && type.isResolved() && type.isInitialized(), cpi, stateBefore);
        CheckCast c = new CheckCast(type, typeInstruction, apop(), stateBefore);
        apush(append(c));
        if (assumeLeafClass(type) && !type.isArrayClass()) {
            c.setDirectCompare();
        }
    }

    void genInstanceOf() {
        FrameState stateBefore = curState.immutableCopy();
        int cpi = stream().readCPI();
        RiType type = constantPool().lookupType(cpi, INSTANCEOF);
        Value typeInstruction = genResolveClass(RiType.Representation.ObjectHub, type, !C1XOptions.TestPatching && type.isResolved() && type.isInitialized(), cpi, stateBefore);
        InstanceOf i = new InstanceOf(type, typeInstruction, apop(), stateBefore);
        ipush(append(i));
        if (assumeLeafClass(type) && !type.isArrayClass()) {
            i.setDirectCompare();
        }
    }

    void genNewInstance(int cpi) {
        FrameState stateBefore = curState.immutableCopy();
        RiType type = constantPool().lookupType(cpi, NEW);
        NewInstance n = new NewInstance(type, cpi, constantPool(), stateBefore);
        if (memoryMap != null) {
            memoryMap.newInstance(n);
        }
        apush(append(n));
    }

    void genNewTypeArray(int typeCode) {
        FrameState stateBefore = curState.immutableCopy();
        apush(append(new NewTypeArray(ipop(), CiKind.fromArrayTypeCode(typeCode), stateBefore)));
    }

    void genNewObjectArray(int cpi) {
        RiType type = constantPool().lookupType(cpi, ANEWARRAY);
        FrameState stateBefore = curState.immutableCopy();
        NewArray n = new NewObjectArray(type, ipop(), stateBefore, cpi, constantPool());
        apush(append(n));
    }

    void genNewMultiArray(int cpi) {
        RiType type = constantPool().lookupType(cpi, MULTIANEWARRAY);
        FrameState stateBefore = curState.immutableCopy();
        int rank = stream().readUByte(stream().currentBCI() + 3);
        Value[] dims = new Value[rank];
        for (int i = rank - 1; i >= 0; i--) {
            dims[i] = ipop();
        }
        NewArray n = new NewMultiArray(type, dims, stateBefore, cpi, constantPool());
        apush(append(n));
    }

    void genGetField(int cpi, RiField field) {
        FrameState stateBefore = curState.immutableCopy();
        boolean isLoaded = !C1XOptions.TestPatching && field.isResolved();
        LoadField load = new LoadField(apop(), field, false, stateBefore, isLoaded);
        appendOptimizedLoadField(field.kind(), load);
    }

    void genPutField(int cpi, RiField field) {
        FrameState stateBefore = curState.immutableCopy();
        boolean isLoaded = !C1XOptions.TestPatching && field.isResolved();
        Value value = pop(field.kind().stackKind());
        appendOptimizedStoreField(new StoreField(apop(), field, value, false, stateBefore, isLoaded));
    }

    void genGetStatic(int cpi, RiField field) {
        FrameState stateBefore = curState.immutableCopy();
        RiType holder = field.holder();
        boolean isInitialized = !C1XOptions.TestPatching && holder.isResolved() && holder.isInitialized();
        Value container = genResolveClass(RiType.Representation.StaticFields, holder, isInitialized, cpi, stateBefore);
        LoadField load = new LoadField(container, field, true, stateBefore, isInitialized);
        appendOptimizedLoadField(field.kind(), load);
    }

    void genPutStatic(int cpi, RiField field) {
        FrameState stateBefore = curState.immutableCopy();
        RiType holder = field.holder();
        boolean isInitialized = !C1XOptions.TestPatching && field.isResolved() && holder.isResolved() && holder.isInitialized();
        Value container = genResolveClass(RiType.Representation.StaticFields, holder, isInitialized, cpi, stateBefore);
        Value value = pop(field.kind().stackKind());
        StoreField store = new StoreField(container, field, value, true, stateBefore, isInitialized);
        appendOptimizedStoreField(store);
    }

    private Value genResolveClass(RiType.Representation representation, RiType holder, boolean initialized, int cpi, FrameState stateBefore) {
        Value holderInstr;
        if (initialized) {
            holderInstr = appendConstant(holder.getEncoding(representation));
        } else {
            holderInstr = append(new ResolveClass(holder, representation, stateBefore));
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

    private void appendOptimizedLoadField(CiKind kind, LoadField load) {
        if (memoryMap != null) {
            Value replacement = memoryMap.load(load);
            if (replacement != load) {
                // the memory buffer found a replacement for this load (no need to append)
                push(kind.stackKind(), replacement);
                return;
            }
        }
        // append the load to the instruction
        Value optimized = append(load);
        if (memoryMap != null && optimized != load) {
            // local optimization happened, replace its value in the memory map
            memoryMap.setResult(load, optimized);
        }
        push(kind.stackKind(), optimized);
    }

    void genInvokeStatic(RiMethod target, int cpi, RiConstantPool constantPool) {
        FrameState stateBefore = curState.immutableCopy();
        Value[] args = curState.popArguments(target.signature().argumentSlots(false));
        if (!tryRemoveCall(target, args, true)) {
            if (!tryInline(target, args, stateBefore)) {
                appendInvoke(INVOKESTATIC, target, args, true, cpi, constantPool, stateBefore);
            }
        }
    }

    void genInvokeInterface(RiMethod target, int cpi, RiConstantPool constantPool) {
        FrameState stateBefore = curState.immutableCopy();
        Value[] args = curState.popArguments(target.signature().argumentSlots(true));
        if (!tryRemoveCall(target, args, false)) {
            genInvokeIndirect(INVOKEINTERFACE, target, args, stateBefore, cpi, constantPool);
        }
    }

    void genInvokeVirtual(RiMethod target, int cpi, RiConstantPool constantPool) {
        FrameState stateBefore = curState.immutableCopy();
        Value[] args = curState.popArguments(target.signature().argumentSlots(true));
        if (!tryRemoveCall(target, args, false)) {
            genInvokeIndirect(INVOKEVIRTUAL, target, args, stateBefore, cpi, constantPool);
        }
    }

    void genInvokeSpecial(RiMethod target, RiType knownHolder, int cpi, RiConstantPool constantPool) {
        FrameState stateBefore = curState.immutableCopy();
        Value[] args = curState.popArguments(target.signature().argumentSlots(true));
        if (!tryRemoveCall(target, args, false)) {
            invokeDirect(target, args, knownHolder, cpi, constantPool, stateBefore);
        }
    }

    private void genInvokeIndirect(int opcode, RiMethod target, Value[] args, FrameState stateBefore, int cpi, RiConstantPool constantPool) {
        Value receiver = args[0];
        // attempt to devirtualize the call
        if (target.isResolved() && target.holder().isResolved()) {
            RiType klass = target.holder();
            // 0. check for trivial cases
            if (target.canBeStaticallyBound() && !isAbstract(target.accessFlags())) {
                // check for trivial cases (e.g. final methods, nonvirtual methods)
                invokeDirect(target, args, target.holder(), cpi, constantPool, stateBefore);
                return;
            }
            // 1. check if the exact type of the receiver can be determined
            RiType exact = getExactType(klass, receiver);
            if (exact != null && exact.isResolved()) {
                // either the holder class is exact, or the receiver object has an exact type
                invokeDirect(exact.resolveMethodImpl(target), args, exact, cpi, constantPool, stateBefore);
                return;
            }
            // 2. check if an assumed leaf method can be found
            RiMethod leaf = getAssumedLeafMethod(target, receiver);
            if (leaf != null && leaf.isResolved() && !isAbstract(leaf.accessFlags()) && leaf.holder().isResolved()) {
                invokeDirect(leaf, args, null, cpi, constantPool, stateBefore);
                return;
            }
            // 3. check if the either of the holder or declared type of receiver can be assumed to be a leaf
            exact = getAssumedLeafType(klass, receiver);
            if (exact != null && exact.isResolved()) {
                // either the holder class is exact, or the receiver object has an exact type
                invokeDirect(exact.resolveMethodImpl(target), args, exact, cpi, constantPool, stateBefore);
                return;
            }
        }
        // devirtualization failed, produce an actual invokevirtual
        appendInvoke(opcode, target, args, false, cpi, constantPool, stateBefore);
    }

    private CiKind returnKind(RiMethod target) {
        return target.signature().returnKind();
    }

    private void invokeDirect(RiMethod target, Value[] args, RiType knownHolder, int cpi, RiConstantPool constantPool, FrameState stateBefore) {
        if (!tryInline(target, args, stateBefore)) {
            // could not optimize or inline the method call
            appendInvoke(INVOKESPECIAL, target, args, false, cpi, constantPool, stateBefore);
        }
    }

    private void appendInvoke(int opcode, RiMethod target, Value[] args, boolean isStatic, int cpi, RiConstantPool constantPool, FrameState stateBefore) {
        CiKind resultType = returnKind(target);
        Value result = append(new Invoke(opcode, resultType.stackKind(), args, isStatic, target, stateBefore));
        pushReturn(resultType, result);
    }

    private RiType getExactType(RiType staticType, Value receiver) {
        RiType exact = staticType.exactType();
        if (exact == null) {
            exact = receiver.exactType();
            if (exact == null) {
                if (receiver.isConstant()) {
                    CiConstant constant = receiver.asConstant();
                    assert constant.kind.isObject();
                    Object object = constant.asObject();
                    if (object != null) {
                        exact = compilation.runtime.getRiType(object.getClass());
                    }
                }
                if (exact == null) {
                    RiType declared = receiver.declaredType();
                    exact = declared == null || !declared.isResolved() ? null : declared.exactType();
                }
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
        if (declared != null && declared.isResolved() && !declared.isInterface()) {
            RiMethod impl = declared.resolveMethodImpl(target);
            if (impl != null && (assumeLeafMethod(impl) || assumeLeafClass(declared))) {
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
            receiverType = compilation.method.holder();
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
            if (isSynchronized(method().accessFlags())) {
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
                curState.push(x.kind, x);
            }
            Goto gotoCallee = new Goto(scopeData.continuation(), null, false);

            scopeData.updateSimpleInlineInfo(curBlock, lastInstr, curState);

            // State at end of inlined method is the state of the caller
            // without the method parameters on stack, including the
            // return value, if any, of the inlined method on operand stack.
            boolean unsafe = curState.unsafe;
            curState = scopeData.continuationState().copy();
            curState.unsafe = unsafe;
            if (x != null) {
                curState.push(x.kind, x);
            }

            // The current bci is in the wrong scope, so use the bci of the continuation point.
            appendWithoutOptimization(gotoCallee, scopeData.continuation().bci());
            return;
        }

        curState.truncateStack(0);
        if (Modifier.isSynchronized(method().accessFlags())) {
            FrameState stateBefore = curState.immutableCopy();
            // unlock before exiting the method
            int lockNumber = curState.locksSize() - 1;
            append(new MonitorExit(rootMethodSynchronizedObject, lockNumber, stateBefore));
            curState.unlock();
        }
        append(new Return(x));
    }

    void genMonitorEnter(Value x, int bci) {
        FrameState stateBefore = curState.immutableCopy();
        int lockNumber = curState.locksSize();
        appendWithoutOptimization(new MonitorEnter(x, lockNumber, stateBefore), bci);
        curState.lock(scope(), x);
        killMemoryMap(); // prevent any optimizations across synchronization
    }

    void genMonitorExit(Value x, int bci) {
        int lockNumber = curState.locksSize() - 1;
        if (lockNumber < 0) {
            throw new CiBailout("monitor stack underflow");
        }
        FrameState stateBefore = curState.immutableCopy();
        appendWithoutOptimization(new MonitorExit(x, lockNumber, stateBefore), bci);
        curState.unlock();
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
        FrameState stateBefore = isBackwards ? curState.immutableCopy() : null;
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
        FrameState stateBefore = isBackwards ? curState.immutableCopy() : null;
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
            Value r = canonicalizer.canonicalize(x);
            List<Instruction> extra = canonicalizer.extra();
            if (extra != null) {
                // the canonicalization introduced instructions that should be added before this
                for (Instruction i : extra) {
                    appendWithBCI(i, bci, false); // don't try to canonicalize the new instructions
                }
            }
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
        jsrStartBlock.setStateBefore(curState.immutableCopy());
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

    FrameState stateAtEntry(RiMethod method) {
        FrameState state = new FrameState(scope(), method.maxLocals(), method.maxStackSize());
        int index = 0;
        if (!isStatic(method.accessFlags())) {
            // add the receiver and assume it is non null
            Local local = new Local(method.holder().kind(), index);
            local.setFlag(Value.Flag.NonNull, true);
            local.setDeclaredType(method.holder());
            state.storeLocal(index, local);
            index = 1;
        }
        RiSignature sig = method.signature();
        int max = sig.argumentCount(false);
        RiType accessingClass = method.holder();
        for (int i = 0; i < max; i++) {
            RiType type = sig.argumentTypeAt(i, accessingClass);
            CiKind kind = type.kind().stackKind();
            Local local = new Local(kind, index);
            if (type.isResolved()) {
                local.setDeclaredType(type);
            }
            state.storeLocal(index, local);
            index += kind.sizeInSlots();
        }
        return state;
    }

    boolean tryRemoveCall(RiMethod target, Value[] args, boolean isStatic) {
        if (target.isResolved()) {
            if (C1XOptions.OptIntrinsify) {
                // try to create an intrinsic node instead of a call
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
        CiKind resultType = returnKind(target);

        // create the intrinsic node
        Intrinsic result = new Intrinsic(resultType.stackKind(), intrinsic, target, args, isStatic, curState.immutableCopy(), preservesState, canTrap);
        pushReturn(resultType, append(result));
        stats.intrinsicCount++;
        return true;
    }

    private boolean tryFoldable(RiMethod target, Value[] args) {
        CiConstant result = Canonicalizer.foldInvocation(compilation.runtime, target, args);
        if (result != null) {
            if (traceLevel > 0) {
                log.println("|");
                log.println("|   [folded " + target + " --> " + result + "]");
                log.println("|");
            }

            pushReturn(returnKind(target), append(new Constant(result)));
            return true;
        }
        return false;
    }

    boolean tryInline(RiMethod target, Value[] args, FrameState stateBefore) {
        boolean forcedInline = compilation.runtime.mustInline(target);
        if (forcedInline) {
            for (IRScope scope = scope().caller; scope != null; scope = scope.caller) {
                if (scope.method.equals(target)) {
                    throw new CiBailout("Cannot recursively inline method that is force-inlined");
                }
            }
            C1XMetrics.InlineForcedMethods++;
        }
        if (forcedInline || checkInliningConditions(target)) {
            if (traceLevel > 0) {
                log.adjustIndentation(1);
                log.println("\\");
                log.adjustIndentation(1);
                if (traceLevel < TRACELEVEL_STATE) {
                    log.println("|   [inlining " + target + "]");
                    log.println("|");
                }
            }
            inline(target, args, forcedInline, stateBefore);
            if (traceLevel > 0) {
                if (traceLevel < TRACELEVEL_STATE) {
                    log.println("|");
                    log.println("|   [return to " + curState.scope().method + "]");
                }
                log.adjustIndentation(-1);
                log.println("/");
                log.adjustIndentation(-1);
            }
            return true;
        }
        return false;
    }

    boolean checkInliningConditions(RiMethod target) {
        if (!C1XOptions.OptInline) {
            return false; // all inlining is turned off
        }
        if (!target.isResolved()) {
            return cannotInline(target, "unresolved method");
        }
        if (target.code() == null) {
            return cannotInline(target, "method has no code");
        }
        if (!target.holder().isInitialized()) {
            return cannotInline(target, "holder is not initialized");
        }
        if (recursiveInlineLevel(target) > C1XOptions.MaximumRecursiveInlineLevel) {
            return cannotInline(target, "recursive inlining too deep");
        }
        if (target.code().length > scopeData.maxInlineSize()) {
            return cannotInline(target, "inlinee too large for this level");
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
        if (isSynchronized(target.accessFlags()) && !C1XOptions.OptInlineSynchronized) {
            return cannotInline(target, "is synchronized");
        }
        if (target.exceptionHandlers().length != 0 && !C1XOptions.OptInlineExcept) {
            return cannotInline(target, "has exception handlers");
        }
        if (!target.hasBalancedMonitors()) {
            return cannotInline(target, "has unbalanced monitors");
        }
        if (target.isConstructor() && target.holder().isSubtypeOf(compilation.throwableType())) {
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

    void inline(RiMethod target, Value[] args, boolean forcedInline, FrameState stateBefore) {
        BlockBegin orig = curBlock;
        if (!forcedInline && !isStatic(target.accessFlags())) {
            // the receiver object must be null-checked for instance methods
            Value receiver = args[0];
            if (!receiver.isNonNull() && !receiver.kind.isWord()) {
                NullCheck check = new NullCheck(receiver, stateBefore);
                args[0] = append(check);
            }
        }

        // Introduce a new callee continuation point. All return instructions
        // in the callee will be transformed to Goto's to the continuation
        BlockBegin continuationBlock = blockAt(nextBCI());
        boolean continuationExisted = true;
        if (continuationBlock == null) {
            // there was not already a block starting at the next BCI
            continuationBlock = new BlockBegin(nextBCI(), ir.nextBlockNumber());
            continuationBlock.setDepthFirstNumber(0);
            continuationExisted = false;
        }
        // record the number of predecessors before inlining, to determine
        // whether the inlined method has added edges to the continuation
        int continuationPredecessors = continuationBlock.predecessors().size();

        // push the target scope
        pushScope(target, continuationBlock);

        // pass parameters into the callee state
        FrameState calleeState = curState;
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
        if (Modifier.isSynchronized(target.accessFlags())) {
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
            iterateBytecodesForBlock(0, true);
            // now iterate over the rest of the blocks
            iterateAllBlocks();
        }

        assert continuationExisted || !continuationBlock.wasVisited() : "continuation should not have been parsed if we created it";

        ReturnBlock simpleInlineInfo = scopeData.simpleInlineInfo();
        if (simpleInlineInfo != null && curBlock == orig) {
            // Optimization: during parsing of the callee we
            // generated at least one Goto to the continuation block. If we
            // generated exactly one, and if the inlined method spanned exactly
            // one block (and we didn't have to Goto its entry), then we snip
            // off the Goto to the continuation, allowing control to fall
            // through back into the caller block and effectively performing
            // block merging. This allows local load elimination and local value numbering
            // to take place across multiple callee scopes if they are relatively simple, and
            // is currently essential to making inlining profitable. It also reduces the
            // number of blocks in the CFG
            lastInstr = simpleInlineInfo.returnPredecessor;
            curState = simpleInlineInfo.returnState.popScope();
            lastInstr.setNext(null, -1);
        } else if (continuationPredecessors == continuationBlock.predecessors().size()) {
            // Inlining caused the instructions after the invoke in the
            // caller to not reachable any more (i.e. no control flow path
            // in the callee was terminated by a return instruction).
            // So skip filling this block with instructions!
            assert continuationBlock == scopeData.continuation();
            assert lastInstr instanceof BlockEnd;
            skipBlock = true;
        } else {
            // Resume parsing in continuation block unless it was already parsed.
            // Note that if we don't change lastInstr here, iteration in
            // iterateBytecodesForBlock will stop when we return.
            if (!scopeData.continuation().wasVisited()) {
                // add continuation to work list instead of parsing it immediately
                assert lastInstr instanceof BlockEnd;
                scopeData.parent.addToWorkList(scopeData.continuation());
                skipBlock = true;
            }
        }

        // fill the exception handler for synchronized methods with instructions
        if (Modifier.isSynchronized(target.accessFlags())) {
            fillSyncHandler(lock, syncHandler, true);
        } else {
            popScope();
        }

        stats.inlineCount++;
    }

    private Value synchronizedObject(FrameState state, RiMethod target) {
        if (isStatic(target.accessFlags())) {
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
        ExceptionHandler handler = new ExceptionHandler(new CiExceptionHandler(0, method().code().length, -1, 0, null));
        handler.setEntryBlock(syncHandler);
        scopeData.addExceptionHandler(handler);
    }

    void fillSyncHandler(Value lock, BlockBegin syncHandler, boolean inlinedMethod) {
        BlockBegin origBlock = curBlock;
        FrameState origState = curState;
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
                iterateBytecodesForBlock(b.bci(), false);
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

        FrameState state = target.stateBefore().copy();
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
                if (local.kind.isObject() && !frame.isLiveObject(i)) {
                    // the compiler thinks this is live, but not the interpreter
                    // pretend that it passed null
                    get = appendConstant(CiConstant.NULL_OBJECT);
                } else {
                    Value oc = appendConstant(CiConstant.forInt(offset));
                    get = append(new UnsafeGetRaw(local.kind, e, oc, 0, true));
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

    BlockEnd iterateBytecodesForBlock(int bci, boolean inliningIntoCurrentBlock) {
        int cpi;

        skipBlock = false;
        assert curState != null;
        BytecodeStream s = scopeData.stream;
        s.setBCI(bci);

        BlockBegin block = curBlock;
        BlockEnd end = null;
        boolean pushException = block.isExceptionEntry() && block.next() == null;
        int prevBCI = bci;
        int endBCI = s.endBCI();
        boolean blockStart = true;

        while (bci < endBCI) {
            BlockBegin nextBlock = blockAt(bci);
            if (bci == 0 && inliningIntoCurrentBlock) {
                if (!nextBlock.isParserLoopHeader()) {
                    // Ignore the block boundary of the entry block of a method
                    // being inlined unless the block is a loop header.
                    nextBlock = null;
                    blockStart = false;
                }
            }
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

            traceState();
            traceInstruction(bci, s, opcode, blockStart);

            // Checkstyle: stop
            switch (opcode) {
                case NOP            : /* nothing to do */ break;
                case ACONST_NULL    : apush(appendConstant(CiConstant.NULL_OBJECT)); break;
                case ICONST_M1      : ipush(appendConstant(CiConstant.INT_MINUS_1)); break;
                case ICONST_0       : ipush(appendConstant(CiConstant.INT_0)); break;
                case ICONST_1       : ipush(appendConstant(CiConstant.INT_1)); break;
                case ICONST_2       : ipush(appendConstant(CiConstant.INT_2)); break;
                case ICONST_3       : ipush(appendConstant(CiConstant.INT_3)); break;
                case ICONST_4       : ipush(appendConstant(CiConstant.INT_4)); break;
                case ICONST_5       : ipush(appendConstant(CiConstant.INT_5)); break;
                case LCONST_0       : lpush(appendConstant(CiConstant.LONG_0)); break;
                case LCONST_1       : lpush(appendConstant(CiConstant.LONG_1)); break;
                case FCONST_0       : fpush(appendConstant(CiConstant.FLOAT_0)); break;
                case FCONST_1       : fpush(appendConstant(CiConstant.FLOAT_1)); break;
                case FCONST_2       : fpush(appendConstant(CiConstant.FLOAT_2)); break;
                case DCONST_0       : dpush(appendConstant(CiConstant.DOUBLE_0)); break;
                case DCONST_1       : dpush(appendConstant(CiConstant.DOUBLE_1)); break;
                case BIPUSH         : ipush(appendConstant(CiConstant.forInt(s.readByte()))); break;
                case SIPUSH         : ipush(appendConstant(CiConstant.forInt(s.readShort()))); break;
                case LDC            : // fall through
                case LDC_W          : // fall through
                case LDC2_W         : genLoadConstant(s.readCPI()); break;
                case ILOAD          : loadLocal(s.readLocalIndex(), CiKind.Int); break;
                case LLOAD          : loadLocal(s.readLocalIndex(), CiKind.Long); break;
                case FLOAD          : loadLocal(s.readLocalIndex(), CiKind.Float); break;
                case DLOAD          : loadLocal(s.readLocalIndex(), CiKind.Double); break;
                case ALOAD          : loadLocal(s.readLocalIndex(), CiKind.Object); break;
                case ILOAD_0        : loadLocal(0, CiKind.Int); break;
                case ILOAD_1        : loadLocal(1, CiKind.Int); break;
                case ILOAD_2        : loadLocal(2, CiKind.Int); break;
                case ILOAD_3        : loadLocal(3, CiKind.Int); break;
                case LLOAD_0        : loadLocal(0, CiKind.Long); break;
                case LLOAD_1        : loadLocal(1, CiKind.Long); break;
                case LLOAD_2        : loadLocal(2, CiKind.Long); break;
                case LLOAD_3        : loadLocal(3, CiKind.Long); break;
                case FLOAD_0        : loadLocal(0, CiKind.Float); break;
                case FLOAD_1        : loadLocal(1, CiKind.Float); break;
                case FLOAD_2        : loadLocal(2, CiKind.Float); break;
                case FLOAD_3        : loadLocal(3, CiKind.Float); break;
                case DLOAD_0        : loadLocal(0, CiKind.Double); break;
                case DLOAD_1        : loadLocal(1, CiKind.Double); break;
                case DLOAD_2        : loadLocal(2, CiKind.Double); break;
                case DLOAD_3        : loadLocal(3, CiKind.Double); break;
                case ALOAD_0        : loadLocal(0, CiKind.Object); break;
                case ALOAD_1        : loadLocal(1, CiKind.Object); break;
                case ALOAD_2        : loadLocal(2, CiKind.Object); break;
                case ALOAD_3        : loadLocal(3, CiKind.Object); break;
                case IALOAD         : genLoadIndexed(CiKind.Int   ); break;
                case LALOAD         : genLoadIndexed(CiKind.Long  ); break;
                case FALOAD         : genLoadIndexed(CiKind.Float ); break;
                case DALOAD         : genLoadIndexed(CiKind.Double); break;
                case AALOAD         : genLoadIndexed(CiKind.Object); break;
                case BALOAD         : genLoadIndexed(CiKind.Byte  ); break;
                case CALOAD         : genLoadIndexed(CiKind.Char  ); break;
                case SALOAD         : genLoadIndexed(CiKind.Short ); break;
                case ISTORE         : storeLocal(CiKind.Int, s.readLocalIndex()); break;
                case LSTORE         : storeLocal(CiKind.Long, s.readLocalIndex()); break;
                case FSTORE         : storeLocal(CiKind.Float, s.readLocalIndex()); break;
                case DSTORE         : storeLocal(CiKind.Double, s.readLocalIndex()); break;
                case ASTORE         : storeLocal(CiKind.Object, s.readLocalIndex()); break;
                case ISTORE_0       : storeLocal(CiKind.Int, 0); break;
                case ISTORE_1       : storeLocal(CiKind.Int, 1); break;
                case ISTORE_2       : storeLocal(CiKind.Int, 2); break;
                case ISTORE_3       : storeLocal(CiKind.Int, 3); break;
                case LSTORE_0       : storeLocal(CiKind.Long, 0); break;
                case LSTORE_1       : storeLocal(CiKind.Long, 1); break;
                case LSTORE_2       : storeLocal(CiKind.Long, 2); break;
                case LSTORE_3       : storeLocal(CiKind.Long, 3); break;
                case FSTORE_0       : storeLocal(CiKind.Float, 0); break;
                case FSTORE_1       : storeLocal(CiKind.Float, 1); break;
                case FSTORE_2       : storeLocal(CiKind.Float, 2); break;
                case FSTORE_3       : storeLocal(CiKind.Float, 3); break;
                case DSTORE_0       : storeLocal(CiKind.Double, 0); break;
                case DSTORE_1       : storeLocal(CiKind.Double, 1); break;
                case DSTORE_2       : storeLocal(CiKind.Double, 2); break;
                case DSTORE_3       : storeLocal(CiKind.Double, 3); break;
                case ASTORE_0       : storeLocal(CiKind.Object, 0); break;
                case ASTORE_1       : storeLocal(CiKind.Object, 1); break;
                case ASTORE_2       : storeLocal(CiKind.Object, 2); break;
                case ASTORE_3       : storeLocal(CiKind.Object, 3); break;
                case IASTORE        : genStoreIndexed(CiKind.Int   ); break;
                case LASTORE        : genStoreIndexed(CiKind.Long  ); break;
                case FASTORE        : genStoreIndexed(CiKind.Float ); break;
                case DASTORE        : genStoreIndexed(CiKind.Double); break;
                case AASTORE        : genStoreIndexed(CiKind.Object); break;
                case BASTORE        : genStoreIndexed(CiKind.Byte  ); break;
                case CASTORE        : genStoreIndexed(CiKind.Char  ); break;
                case SASTORE        : genStoreIndexed(CiKind.Short ); break;
                case POP            : // fall through
                case POP2           : // fall through
                case DUP            : // fall through
                case DUP_X1         : // fall through
                case DUP_X2         : // fall through
                case DUP2           : // fall through
                case DUP2_X1        : // fall through
                case DUP2_X2        : // fall through
                case SWAP           : stackOp(opcode); break;
                case IADD           : // fall through
                case ISUB           : // fall through
                case IMUL           : genArithmeticOp(CiKind.Int, opcode); break;
                case IDIV           : // fall through
                case IREM           : genArithmeticOp(CiKind.Int, opcode, curState.immutableCopy()); break;
                case LADD           : // fall through
                case LSUB           : // fall through
                case LMUL           : genArithmeticOp(CiKind.Long, opcode); break;
                case LDIV           : // fall through
                case LREM           : genArithmeticOp(CiKind.Long, opcode, curState.immutableCopy()); break;
                case FADD           : // fall through
                case FSUB           : // fall through
                case FMUL           : // fall through
                case FDIV           : // fall through
                case FREM           : genArithmeticOp(CiKind.Float, opcode); break;
                case DADD           : // fall through
                case DSUB           : // fall through
                case DMUL           : // fall through
                case DDIV           : // fall through
                case DREM           : genArithmeticOp(CiKind.Double, opcode); break;
                case INEG           : genNegateOp(CiKind.Int); break;
                case LNEG           : genNegateOp(CiKind.Long); break;
                case FNEG           : genNegateOp(CiKind.Float); break;
                case DNEG           : genNegateOp(CiKind.Double); break;
                case ISHL           : // fall through
                case ISHR           : // fall through
                case IUSHR          : genShiftOp(CiKind.Int, opcode); break;
                case IAND           : // fall through
                case IOR            : // fall through
                case IXOR           : genLogicOp(CiKind.Int, opcode); break;
                case LSHL           : // fall through
                case LSHR           : // fall through
                case LUSHR          : genShiftOp(CiKind.Long, opcode); break;
                case LAND           : // fall through
                case LOR            : // fall through
                case LXOR           : genLogicOp(CiKind.Long, opcode); break;
                case IINC           : genIncrement(); break;
                case I2L            : genConvert(opcode, CiKind.Int   , CiKind.Long  ); break;
                case I2F            : genConvert(opcode, CiKind.Int   , CiKind.Float ); break;
                case I2D            : genConvert(opcode, CiKind.Int   , CiKind.Double); break;
                case L2I            : genConvert(opcode, CiKind.Long  , CiKind.Int   ); break;
                case L2F            : genConvert(opcode, CiKind.Long  , CiKind.Float ); break;
                case L2D            : genConvert(opcode, CiKind.Long  , CiKind.Double); break;
                case F2I            : genConvert(opcode, CiKind.Float , CiKind.Int   ); break;
                case F2L            : genConvert(opcode, CiKind.Float , CiKind.Long  ); break;
                case F2D            : genConvert(opcode, CiKind.Float , CiKind.Double); break;
                case D2I            : genConvert(opcode, CiKind.Double, CiKind.Int   ); break;
                case D2L            : genConvert(opcode, CiKind.Double, CiKind.Long  ); break;
                case D2F            : genConvert(opcode, CiKind.Double, CiKind.Float ); break;
                case I2B            : genConvert(opcode, CiKind.Int   , CiKind.Byte  ); break;
                case I2C            : genConvert(opcode, CiKind.Int   , CiKind.Char  ); break;
                case I2S            : genConvert(opcode, CiKind.Int   , CiKind.Short ); break;
                case LCMP           : genCompareOp(CiKind.Long, opcode); break;
                case FCMPL          : genCompareOp(CiKind.Float, opcode); break;
                case FCMPG          : genCompareOp(CiKind.Float, opcode); break;
                case DCMPL          : genCompareOp(CiKind.Double, opcode); break;
                case DCMPG          : genCompareOp(CiKind.Double, opcode); break;
                case IFEQ           : genIfZero(Condition.EQ); break;
                case IFNE           : genIfZero(Condition.NE); break;
                case IFLT           : genIfZero(Condition.LT); break;
                case IFGE           : genIfZero(Condition.GE); break;
                case IFGT           : genIfZero(Condition.GT); break;
                case IFLE           : genIfZero(Condition.LE); break;
                case IF_ICMPEQ      : genIfSame(CiKind.Int, Condition.EQ); break;
                case IF_ICMPNE      : genIfSame(CiKind.Int, Condition.NE); break;
                case IF_ICMPLT      : genIfSame(CiKind.Int, Condition.LT); break;
                case IF_ICMPGE      : genIfSame(CiKind.Int, Condition.GE); break;
                case IF_ICMPGT      : genIfSame(CiKind.Int, Condition.GT); break;
                case IF_ICMPLE      : genIfSame(CiKind.Int, Condition.LE); break;
                case IF_ACMPEQ      : genIfSame(CiKind.Object, Condition.EQ); break;
                case IF_ACMPNE      : genIfSame(CiKind.Object, Condition.NE); break;
                case GOTO           : genGoto(s.currentBCI(), s.readBranchDest()); break;
                case JSR            : genJsr(s.readBranchDest()); break;
                case RET            : genRet(s.readLocalIndex()); break;
                case TABLESWITCH    : genTableswitch(); break;
                case LOOKUPSWITCH   : genLookupswitch(); break;
                case IRETURN        : genMethodReturn(ipop()); break;
                case LRETURN        : genMethodReturn(lpop()); break;
                case FRETURN        : genMethodReturn(fpop()); break;
                case DRETURN        : genMethodReturn(dpop()); break;
                case ARETURN        : genMethodReturn(apop()); break;
                case RETURN         : genMethodReturn(null  ); break;
                case GETSTATIC      : cpi = s.readCPI(); genGetStatic(cpi, constantPool().lookupField(cpi, opcode)); break;
                case PUTSTATIC      : cpi = s.readCPI(); genPutStatic(cpi, constantPool().lookupField(cpi, opcode)); break;
                case GETFIELD       : cpi = s.readCPI(); genGetField(cpi, constantPool().lookupField(cpi, opcode)); break;
                case PUTFIELD       : cpi = s.readCPI(); genPutField(cpi, constantPool().lookupField(cpi, opcode)); break;
                case INVOKEVIRTUAL  : cpi = s.readCPI(); genInvokeVirtual(constantPool().lookupMethod(cpi, opcode), cpi, constantPool()); break;
                case INVOKESPECIAL  : cpi = s.readCPI(); genInvokeSpecial(constantPool().lookupMethod(cpi, opcode), null, cpi, constantPool()); break;
                case INVOKESTATIC   : cpi = s.readCPI(); genInvokeStatic(constantPool().lookupMethod(cpi, opcode), cpi, constantPool()); break;
                case INVOKEINTERFACE: cpi = s.readCPI(); genInvokeInterface(constantPool().lookupMethod(cpi, opcode), cpi, constantPool()); break;
                case NEW            : genNewInstance(s.readCPI()); break;
                case NEWARRAY       : genNewTypeArray(s.readLocalIndex()); break;
                case ANEWARRAY      : genNewObjectArray(s.readCPI()); break;
                case ARRAYLENGTH    : genArrayLength(); break;
                case ATHROW         : genThrow(s.currentBCI()); break;
                case CHECKCAST      : genCheckCast(); break;
                case INSTANCEOF     : genInstanceOf(); break;
                case MONITORENTER   : genMonitorEnter(apop(), s.currentBCI()); break;
                case MONITOREXIT    : genMonitorExit(apop(), s.currentBCI()); break;
                case MULTIANEWARRAY : genNewMultiArray(s.readCPI()); break;
                case IFNULL         : genIfNull(Condition.EQ); break;
                case IFNONNULL      : genIfNull(Condition.NE); break;
                case GOTO_W         : genGoto(s.currentBCI(), s.readFarBranchDest()); break;
                case JSR_W          : genJsr(s.readFarBranchDest()); break;


                case UNSAFE_CAST    : genUnsafeCast(constantPool().lookupMethod(s.readCPI(), (byte)Bytecodes.UNSAFE_CAST)); break;
                case WLOAD          : loadLocal(s.readLocalIndex(), CiKind.Word); break;
                case WLOAD_0        : loadLocal(0, CiKind.Word); break;
                case WLOAD_1        : loadLocal(1, CiKind.Word); break;
                case WLOAD_2        : loadLocal(2, CiKind.Word); break;
                case WLOAD_3        : loadLocal(3, CiKind.Word); break;

                case WSTORE         : storeLocal(CiKind.Word, s.readLocalIndex()); break;
                case WSTORE_0       : storeLocal(CiKind.Word, 0); break;
                case WSTORE_1       : storeLocal(CiKind.Word, 1); break;
                case WSTORE_2       : storeLocal(CiKind.Word, 2); break;
                case WSTORE_3       : storeLocal(CiKind.Word, 3); break;

                case WCONST_0       : wpush(appendConstant(CiConstant.ZERO)); break;
                case WDIV           : // fall through
                case WREM           : genArithmeticOp(CiKind.Word, opcode, curState.immutableCopy()); break;
                case WDIVI          : genArithmeticOp(CiKind.Word, opcode, CiKind.Word, CiKind.Int, curState.immutableCopy()); break;
                case WREMI          : genArithmeticOp(CiKind.Int, opcode, CiKind.Word, CiKind.Int, curState.immutableCopy()); break;

                case READREG        : genLoadRegister(s.readCPI()); break;
                case WRITEREG       : genStoreRegister(s.readCPI()); break;

                case PREAD          : genLoadPointer(PREAD      | (s.readCPI() << 8)); break;
                case PGET           : genLoadPointer(PGET       | (s.readCPI() << 8)); break;
                case PWRITE         : genStorePointer(PWRITE    | (s.readCPI() << 8)); break;
                case PSET           : genStorePointer(PSET      | (s.readCPI() << 8)); break;
                case PCMPSWP        : getCompareAndSwap(PCMPSWP | (s.readCPI() << 8)); break;

                case WRETURN        : genMethodReturn(wpop()); break;
                case READ_PC        : genLoadPC(); break;
                case JNICALL        : genNativeCall(s.readCPI()); break;
                case JNIOP          : genJniOp(s.readCPI()); break;
                case ALLOCA         : genStackAllocate(); break;

                case MOV_I2F        : genConvert(opcode, CiKind.Int, CiKind.Float ); break;
                case MOV_F2I        : genConvert(opcode, CiKind.Float, CiKind.Int ); break;
                case MOV_L2D        : genConvert(opcode, CiKind.Long, CiKind.Double ); break;
                case MOV_D2L        : genConvert(opcode, CiKind.Double, CiKind.Long ); break;

                case UCMP           : genUnsignedCompareOp(CiKind.Int, opcode, s.readCPI()); break;
                case UWCMP          : genUnsignedCompareOp(CiKind.Word, opcode, s.readCPI()); break;

                case ALLOCSTKVAR    : genLoadStackAddress(); break;
                case PAUSE          : genPause(); break;
                case LSB            : // fall through
                case MSB            : genSignificantBit(opcode);break;

//                case PCMPSWP: {
//                    opcode |= readUnsigned2() << 8;
//                    switch (opcode) {
//                        case PCMPSWP_INT:
//                        case PCMPSWP_INT_I:
//                        case PCMPSWP_WORD:
//                        case PCMPSWP_WORD_I: {
//                            popCategory1();
//                            popCategory1();
//                            popCategory1();
//                            break;
//                        }
//                        case PCMPSWP_REFERENCE:
//                        case PCMPSWP_REFERENCE_I: {
//                            popCategory1();
//                            popCategory1();
//                            popCategory1();
//                            popCategory1();
//                            pushRef();
//                            break;
//                        }
//                        default: {
//                            FatalError.unexpected("Unknown bytcode");
//                        }
//                    }
//                    break;
//                }

//                case MEMBAR: {
//                    skip2();
//                    break;
//                }



                case BREAKPOINT:
                    throw new CiBailout("concurrent setting of breakpoint");
                default:
                    throw new CiBailout("unknown bytecode " + opcode + " (" + nameOf(opcode) + ") [bci=" + bci + "]");
            }
            // Checkstyle: resume

            prevBCI = bci;
            s.next();

            if (lastInstr instanceof BlockEnd) {
                end = (BlockEnd) lastInstr;
                break;
            }
            bci = s.currentBCI();
            blockStart = false;
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

    private void traceState() {
        if (traceLevel >= TRACELEVEL_STATE) {
            log.println(String.format("|   state [nr locals = %d, stack depth = %d, method = %s]", curState.localsSize(), curState.stackSize(), curState.scope().method));
            for (int i = 0; i < curState.localsSize(); ++i) {
                Value value = curState.localAt(i);
                log.println(String.format("|   local[%d] = %-8s : %s", i, value == null ? "bogus" : value.kind.javaName, value));
            }
            for (int i = 0; i < curState.stackSize(); ++i) {
                Value value = curState.stackAt(i);
                log.println(String.format("|   stack[%d] = %-8s : %s", i, value == null ? "bogus" : value.kind.javaName, value));
            }
            for (int i = 0; i < curState.locksSize(); ++i) {
                Value value = curState.lockAt(i);
                log.println(String.format("|   lock[%d] = %-8s : %s", i, value == null ? "bogus" : value.kind.javaName, value));
            }
        }
    }

    private void traceInstruction(int bci, BytecodeStream s, int opcode, boolean blockStart) {
        if (traceLevel >= TRACELEVEL_INSTRUCTIONS) {
            StringBuilder sb = new StringBuilder(40);
            sb.append(blockStart ? '+' : '|');
            if (bci < 10) {
                sb.append("  ");
            } else if (bci < 100) {
                sb.append(' ');
            }
            sb.append(bci).append(": ").append(Bytecodes.nameOf(opcode));
            for (int i = bci + 1; i < s.nextBCI(); ++i) {
                sb.append(' ').append(s.readUByte(i));
            }
            log.println(sb.toString());
        }
    }

    private void genPause() {
        append(new Pause());
    }

    private void genLoadStackAddress() {
        Value value = curState.xpop();
        wpush(append(new AllocateStackVariable(value)));
    }

    private void genStackAllocate() {
        Value size = pop(CiKind.Word);
        wpush(append(new StackAllocate(size)));
    }

    private void genSignificantBit(int opcode) {
        Value value = pop(CiKind.Word);
        push(CiKind.Int, append(new SignificantBitOp(value, opcode)));
    }

    private void appendSnippetCall(RiSnippetCall snippetCall, FrameState stateBefore) {
        Value[] args = new Value[snippetCall.arguments.length];
        RiMethod snippet = snippetCall.snippet;
        RiSignature signature = snippet.signature();
        assert signature.argumentCount(!isStatic(snippet.accessFlags())) == args.length;
        for (int i = args.length - 1; i >= 0; --i) {
            CiKind argKind = signature.argumentKindAt(i);
            if (snippetCall.arguments[i] == null) {
                args[i] = pop(argKind);
            } else {
                args[i] = append(new Constant(snippetCall.arguments[i]));
            }
        }

        if (!tryRemoveCall(snippet, args, true)) {
            if (!tryInline(snippet, args, stateBefore)) {
                appendInvoke(snippetCall.opcode, snippet, args, true, (char) 0, constantPool(), stateBefore);
            }
        }
    }

    void genJniOp(int operand) {
        RiSnippets snippets = compilation.runtime.getSnippets();
        switch (operand) {
            case JniOp.LINK: {
                FrameState stateBefore = curState.immutableCopy();
                RiMethod nativeMethod = scope().method;
                RiSnippetCall linkSnippet = snippets.link(nativeMethod);
                if (linkSnippet.result != null) {
                    wpush(appendConstant(linkSnippet.result));
                } else {
                    appendSnippetCall(linkSnippet, stateBefore);
                }
                break;
            }
            case JniOp.J2N: {
                FrameState stateBefore = curState.immutableCopy();
                RiMethod nativeMethod = scope().method;
                appendSnippetCall(snippets.enterNative(nativeMethod), stateBefore);
                break;
            }
            case JniOp.N2J: {
                FrameState stateBefore = curState.immutableCopy();
                RiMethod nativeMethod = scope().method;
                appendSnippetCall(snippets.enterVM(nativeMethod), stateBefore);
                break;
            }
        }
     }

    void genNativeCall(int cpi) {
        FrameState stateBefore = curState.immutableCopy();
        RiSignature sig = constantPool().lookupSignature(cpi);
        Value nativeFunctionAddress = wpop();
        Value[] args = curState.popArguments(sig.argumentSlots(false));

        RiMethod nativeMethod = scope().method;
        CiKind returnKind = sig.returnKind();
        pushReturn(returnKind, append(new NativeCall(nativeMethod, sig, nativeFunctionAddress, args, stateBefore)));
    }

    private void genLoadPC() {
        wpush(append(new LoadPC()));
    }

    private void genLoadRegister(int registerId) {
        RiRegisterConfig registerConfig = compilation.target.registerConfig;
        CiRegister register = registerConfig.getIntegerRegister(registerId);
        if (register == null) {
            throw new CiBailout("Unsupported READREG operand " + registerId);
        }
        wpush(append(new LoadRegister(CiKind.Word, register)));
    }

    private void genStoreRegister(int registerId) {
        RiRegisterConfig registerConfig = compilation.target.registerConfig;
        CiRegister register = registerConfig.getIntegerRegister(registerId);
        if (register == null) {
            throw new CiBailout("Unsupported WRITEREG operand " + registerId);
        }
        Value value = pop(CiKind.Word);
        append(new StoreRegister(CiKind.Word, register, value));
    }

    private static CiKind kindForPointerOp(int opcode) {
        switch (opcode) {
            case PGET_BYTE          :
            case PSET_BYTE          :
            case PREAD_BYTE         :
            case PREAD_BYTE_I       :
            case PWRITE_BYTE        :
            case PWRITE_BYTE_I      : return CiKind.Byte;
            case PGET_CHAR          :
            case PREAD_CHAR         :
            case PREAD_CHAR_I       : return CiKind.Char;
            case PGET_SHORT         :
            case PSET_SHORT         :
            case PREAD_SHORT        :
            case PREAD_SHORT_I      :
            case PWRITE_SHORT       :
            case PWRITE_SHORT_I     : return CiKind.Short;
            case PGET_INT           :
            case PSET_INT           :
            case PREAD_INT          :
            case PREAD_INT_I        :
            case PWRITE_INT         :
            case PWRITE_INT_I       : return CiKind.Int;
            case PGET_FLOAT         :
            case PSET_FLOAT         :
            case PREAD_FLOAT        :
            case PREAD_FLOAT_I      :
            case PWRITE_FLOAT       :
            case PWRITE_FLOAT_I     : return CiKind.Float;
            case PGET_LONG          :
            case PSET_LONG          :
            case PREAD_LONG         :
            case PREAD_LONG_I       :
            case PWRITE_LONG        :
            case PWRITE_LONG_I      : return CiKind.Long;
            case PGET_DOUBLE        :
            case PSET_DOUBLE        :
            case PREAD_DOUBLE       :
            case PREAD_DOUBLE_I     :
            case PWRITE_DOUBLE      :
            case PWRITE_DOUBLE_I    : return CiKind.Double;
            case PGET_WORD          :
            case PSET_WORD          :
            case PREAD_WORD         :
            case PREAD_WORD_I       :
            case PWRITE_WORD        :
            case PWRITE_WORD_I      : return CiKind.Word;
            case PGET_REFERENCE     :
            case PSET_REFERENCE     :
            case PREAD_REFERENCE    :
            case PREAD_REFERENCE_I  :
            case PWRITE_REFERENCE   :
            case PWRITE_REFERENCE_I : return CiKind.Object;
            default:
                throw new CiBailout("Unsupported pointer operation opcode " + opcode + "(" + nameOf(opcode) + ")");
        }
    }

    private void genLoadPointer(int opcode) {
        FrameState stateBefore = curState.immutableCopy();
        CiKind kind = kindForPointerOp(opcode);
        Value offsetOrIndex;
        Value displacement;
        if ((opcode & 0xff) == PREAD) {
            offsetOrIndex = (opcode >= PREAD_BYTE_I && opcode <= PREAD_REFERENCE_I) ? ipop() : wpop();
            displacement = null;
        } else {
            offsetOrIndex = ipop();
            displacement = ipop();
        }
        Value pointer = wpop();
        push(kind.stackKind(), append(new LoadPointer(kind.stackKind(), opcode, pointer, displacement, offsetOrIndex, stateBefore, false)));
    }

    private void genStorePointer(int opcode) {
        FrameState stateBefore = curState.immutableCopy();
        CiKind kind = kindForPointerOp(opcode);
        Value value = pop(kind);
        Value offsetOrIndex;
        Value displacement;
        if ((opcode & 0xff) == PWRITE) {
            offsetOrIndex = (opcode >= PWRITE_BYTE_I && opcode <= PWRITE_REFERENCE_I) ? ipop() : wpop();
            displacement = null;
        } else {
            offsetOrIndex = ipop();
            displacement = ipop();
        }
        Value pointer = wpop();
        append(new StorePointer(opcode, pointer, displacement, offsetOrIndex, value, stateBefore, false));
    }

    private static CiKind kindForCompareAndSwap(int opcode) {
        switch (opcode) {
            case PCMPSWP_INT        :
            case PCMPSWP_INT_I      : return CiKind.Int;
            case PCMPSWP_WORD       :
            case PCMPSWP_WORD_I     : return CiKind.Word;
            case PCMPSWP_REFERENCE  :
            case PCMPSWP_REFERENCE_I: return CiKind.Object;
            default:
                throw new CiBailout("Unsupported compare-and-swap opcode " + opcode + "(" + nameOf(opcode) + ")");
        }
    }

    private void getCompareAndSwap(int opcode) {
        FrameState stateBefore = curState.immutableCopy();
        CiKind kind = kindForCompareAndSwap(opcode);
        Value newValue = pop(kind);
        Value expectedValue = pop(kind);
        Value offset = (opcode >= PCMPSWP_INT_I && opcode <= PCMPSWP_REFERENCE_I) ? ipop() : wpop();
        Value pointer = wpop();
        push(kind, append(new CompareAndSwap(opcode, pointer, offset, expectedValue, newValue, stateBefore, false)));
    }


    private void genArrayLength() {
        ipush(append(new ArrayLength(apop(), curState.immutableCopy())));
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
        if (!C1XOptions.TestSlowPath && type.isResolved()) {
            if (isFinal(type.accessFlags())) {
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
        if (!C1XOptions.TestSlowPath && method.isResolved()) {
            if (method.isLeafMethod()) {
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
