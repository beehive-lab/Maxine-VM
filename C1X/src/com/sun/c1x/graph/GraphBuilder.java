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

    final ValueMap localValueMap;          // map of values for local value numbering
    final MemoryMap memoryMap;             // map of field values for local load elimination
    ScopeData scopeData;                   // Per-scope data; used for inlining
    BlockBegin curBlock;                   // the current block
    ValueStack curState;                   // the current execution state
    Instruction lastInstr;                 // the last instruction added
    int totalInstructions;                        // for bailing out in pathological jsr/ret cases

    ValueStack initialState;               // The state for the start block
    ValueStack exceptionState;             // state that will be used by handleException
    boolean skipBlock;                     // skip processing of the rest of this block

    /**
     * Creates a new instance and builds the graph for a the specified IRScope.
     * @param compilation the compilation
     * @param scope the top IRScope
     * @param ir the IR to build the graph into
     */
    public GraphBuilder(C1XCompilation compilation, IRScope scope, IR ir) {
        this.compilation = compilation;
        this.ir = ir;
        this.memoryMap = C1XOptions.EliminateFieldAccess ? new MemoryMap() : null;
        this.localValueMap = C1XOptions.UseLocalValueNumbering ? new ValueMap() : null;
        int osrBCI = compilation.osrBCI;
        BlockMap blockMap = compilation.getBlockMap(scope.method, osrBCI);
        BlockBegin start = blockMap.get(0);

        pushRootScope(scope, blockMap, start);

        this.initialState = stateAtEntry();
        start.merge(initialState);

        BlockBegin syncHandler = null;
        CiMethod method = method();
        if (method.isSynchronized()) {
            // setup and exception handler
            syncHandler = new BlockBegin(Instruction.SYNCHRONIZATION_ENTRY_BCI, ir.nextBlockNumber());
            syncHandler.setExceptionEntry();
            syncHandler.setBlockFlag(BlockBegin.BlockFlag.IsOnWorkList);
            syncHandler.setBlockFlag(BlockBegin.BlockFlag.DefaultExceptionHandler);

            CiExceptionHandler desc = newDefaultExceptionHandler(method);
            ExceptionHandler h = new ExceptionHandler(desc);
            h.setEntryBlock(syncHandler);
            scopeData.addExceptionHandler(h);
        }

        scope().computeLockStackSize();
        C1XIntrinsic intrinsic = C1XIntrinsic.getIntrinsic(method);
        if (intrinsic != null) {
            // the root method is an intrinsic; load the parameters onto the stack and try to inline it
            curState = initialState.copy();
            lastInstr = curBlock;
            if (C1XOptions.InlineIntrinsics) {
                // try to inline an Intrinsic node
                boolean isStatic = method.isStatic();
                int argsSize = method.signatureType().argumentSlots(!isStatic);
                Instruction[] args = new Instruction[argsSize];
                for (int i = 0; i < args.length; i++) {
                    args[i] = curState.localAt(i);
                }
                if (tryInlineIntrinsic(method, args, isStatic, intrinsic)) {
                    // intrinsic inlining succeeded, add the return node
                    BasicType rt = returnBasicType(method);
                    Instruction result = null;
                    if (rt != BasicType.Void) {
                        result = pop(rt);
                    }
                    methodReturn(result);
                    BlockEnd end = (BlockEnd) lastInstr;
                    curBlock.setEnd(end);
                    end.setState(curState);
                }  else {
                    // try intrinsic failed; do the normal parsing
                    scopeData.addToWorkList(start);
                    iterateAllBlocks();
                }
            } else {
                // do the normal parsing
                scopeData.addToWorkList(start);
                iterateAllBlocks();
            }
        } else {
            // do the normal parsing
            scopeData.addToWorkList(start);
            iterateAllBlocks();
        }

        if (syncHandler != null && syncHandler.state() != null) {
            Instruction lock = null;
            if (method.isSynchronized()) {
                curBlock = syncHandler;
                lastInstr = syncHandler;
                curState = syncHandler.state().copy();
                lock = synchronizedObject(initialState, method);
                syncHandler.state().unlock(); // pop the null off the stack
                syncHandler.state().lock(scope, lock);
            }
            fillSyncHandler(lock, syncHandler, true);
        }

        ir.startBlock = setupStartBlock(osrBCI, start, ir.osrEntryBlock, initialState);
        // eliminate redundant phis
        if (C1XOptions.SimplifyPhis) {
            new PhiSimplifier(ir.startBlock);
        }

        if (osrBCI >= 0) {
            BlockBegin osrBlock = blockMap.get(osrBCI);
            assert osrBlock.wasVisited();
            if (!osrBlock.state().stackEmpty()) {
                throw new Bailout("cannot OSR with non-empty stack");
            }
        }
    }

    private CiExceptionHandler newDefaultExceptionHandler(CiMethod method) {
        return constantPool().newExceptionHandler(0, method.codeSize(), -1, 0);
    }

    void pushRootScope(IRScope scope, BlockMap blockMap, BlockBegin start) {
        BytecodeStream stream = new BytecodeStream(scope.method.code());
        CiConstantPool constantPool = compilation.runtime.getConstantPool(scope.method);
        scopeData = new ScopeData(null, scope, blockMap, stream, constantPool);
        curBlock = start;
    }

    BlockBegin setupStartBlock(int osrBCI, BlockBegin stdEntry, BlockBegin osrEntry, ValueStack state) {
        BlockBegin start = new BlockBegin(0, ir.nextBlockNumber());

        BlockBegin newHeaderBlock;
        if (stdEntry.predecessors().size() == 0 && !C1XOptions.ProfileBranches) {
            newHeaderBlock = stdEntry;
        } else {
            newHeaderBlock = headerBlock(stdEntry, BlockBegin.BlockFlag.StandardEntry, state);
        }

        Base base = new Base(newHeaderBlock, osrEntry);
        start.setNext(base, 0);
        start.setEnd(base);
        // create and setup state for start block
        start.setState(state.copy());
        base.setState(state.copy());

        if (base.standardEntry().state() == null) {
            base.standardEntry().merge(state);
        }
        assert base.standardEntry().state() != null;
        return start;
    }

    BlockBegin headerBlock(BlockBegin entry, BlockBegin.BlockFlag f, ValueStack state) {
        assert entry.checkBlockFlag(f);
        // create header block
        BlockBegin h = new BlockBegin(entry.bci(), ir.nextBlockNumber());
        h.setDepthFirstNumber(0);

        Instruction l = h;
        CiMethodData methodData = method().methodData();
        if (C1XOptions.ProfileBranches && methodData != null) {
            // increment the invocation counter;
            // note that the normal append() won't work, so we do this manually
            Instruction m = Constant.forObject(methodData.dataObject());
            h.setNext(m, 0);
            Instruction p = new ProfileCounter(m, methodData.invocationCountOffset(), 1);
            m.setNext(p, 0);
            l = p;
        }

        BlockEnd g = new Goto(entry, null, false);
        l.setNext(g, entry.bci());
        h.setEnd(g);
        h.setBlockFlag(f);
        ValueStack s = state.copy();
        assert s.stackEmpty();
        g.setState(s);
        return h;
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

    public CiMethod method() {
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

    void ipush(Instruction x) {
        curState.ipush(x);
    }

    void lpush(Instruction x) {
        curState.lpush(x);
    }

    void fpush(Instruction x) {
        curState.fpush(x);
    }

    void dpush(Instruction x) {
        curState.dpush(x);
    }

    void apush(Instruction x) {
        curState.apush(x);
    }

    void push(BasicType basicType, Instruction x) {
        curState.push(basicType, x);
    }

    void pushReturn(BasicType basicType, Instruction x) {
        if (basicType != BasicType.Void) {
            curState.push(basicType.stackType(), x);
        }
    }

    Instruction ipop() {
        return curState.ipop();
    }

    Instruction lpop() {
        return curState.lpop();
    }

    Instruction fpop() {
        return curState.fpop();
    }

    Instruction dpop() {
        return curState.dpop();
    }

    Instruction apop() {
        return curState.apop();
    }

    Instruction pop(BasicType basicType) {
        return curState.pop(basicType);
    }

    void loadLocal(int index, BasicType basicType) {
        push(basicType, curState.loadLocal(index));
    }

    void storeLocal(BasicType basicType, int index) {
        if (scopeData.parsingJsr()) {
            // We need to do additional tracking of the location of the return
            // address for jsrs since we don't handle arbitrary jsr/ret
            // constructs. Here we are figuring out in which circumstances we
            // need to bail out.
            if (basicType == BasicType.Object) {
                // might be storing the JSR return address
                Instruction x = curState.xpop();
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
        for (ScopeData cur = scopeData.parent;
                cur != null && cur.parsingJsr() && cur.scope == scope();
                cur = cur.parent) {
            if (cur.jsrEntryReturnAddressLocal() == index) {
                throw new Bailout("subroutine overwrites return address from previous subroutine");
            }
        }
    }

    Instruction roundFp(Instruction x) {
        if (C1XOptions.RoundFPResults && C1XOptions.SSEVersion < 2) {
            if (x.type().isDouble() && !(x instanceof Constant) && !(x instanceof Local) && !(x instanceof RoundFP)) {
                return append(new RoundFP(x));
            }
        }
        return x;
    }

    void appendNullCheck(Instruction x) {
        if (!x.isNonNull()) {
            append(new NullCheck(x, lockStack()));
        }
    }

    List<ExceptionHandler> handleException(int bci) {
        if (!hasHandler()) {
            return Util.uncheckedCast(Collections.EMPTY_LIST);
        }

        List<ExceptionHandler> exceptionHandlers = new ArrayList<ExceptionHandler>();
        ScopeData curScopeData = scopeData;
        ValueStack s = exceptionState;
        int scopeCount = 0;

        assert s != null : "exception handler state must be set";
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
            throw new Bailout("Exception handler covers itself");
        }
        assert entry.bci() == h.handler.handlerBCI();
        assert entry.bci() == -1 || entry == curScopeData.blockAt(entry.bci()) : "blocks must correspond";
        assert entry.state() == null || s.locksSize() == entry.state().locksSize() : "locks do not match";

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

    void loadConstant() {
        Object con = constantPool().lookupConstant(stream().readCPI());

        if (con instanceof CiType) {
            // this is a load of class constant which might be unresolved
            CiType citype = (CiType) con;
            if (!citype.isLoaded() || C1XOptions.TestPatching) {
                push(BasicType.Object, append(new ResolveClass(citype, curState.copy())));
            } else {
                push(BasicType.Object, append(Constant.forObject(citype.javaClass())));
            }
        } else if (con instanceof CiConstant) {
            CiConstant constant = (CiConstant) con;
            push(constant.basicType.stackType(), appendConstant(constant));
        } else {
            throw new Error("lookupConstant returned an object of incorrect type");
        }
    }

    void loadIndexed(BasicType type) {
        Instruction index = ipop();
        Instruction array = apop();
        Instruction length = null;
        if (cseArrayLength(array)) {
            length = append(new ArrayLength(array, lockStack()));
        }
        push(type.stackType(), append(new LoadIndexed(array, index, length, type, lockStack())));
    }

    void storeIndexed(BasicType type) {
        Instruction value = pop(type.stackType());
        Instruction index = ipop();
        Instruction array = apop();
        Instruction length = null;
        if (cseArrayLength(array)) {
            length = append(new ArrayLength(array, lockStack()));
        }
        StoreIndexed result = new StoreIndexed(array, index, length, type, value, lockStack());
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
                Instruction w = curState.xpop();
                curState.xpush(w);
                curState.xpush(w);
                break;
            }
            case Bytecodes.DUP_X1: {
                Instruction w1 = curState.xpop();
                Instruction w2 = curState.xpop();
                curState.xpush(w1);
                curState.xpush(w2);
                curState.xpush(w1);
                break;
            }
            case Bytecodes.DUP_X2: {
                Instruction w1 = curState.xpop();
                Instruction w2 = curState.xpop();
                Instruction w3 = curState.xpop();
                curState.xpush(w1);
                curState.xpush(w3);
                curState.xpush(w2);
                curState.xpush(w1);
                break;
            }
            case Bytecodes.DUP2: {
                Instruction w1 = curState.xpop();
                Instruction w2 = curState.xpop();
                curState.xpush(w2);
                curState.xpush(w1);
                curState.xpush(w2);
                curState.xpush(w1);
                break;
            }
            case Bytecodes.DUP2_X1: {
                Instruction w1 = curState.xpop();
                Instruction w2 = curState.xpop();
                Instruction w3 = curState.xpop();
                curState.xpush(w2);
                curState.xpush(w1);
                curState.xpush(w3);
                curState.xpush(w2);
                curState.xpush(w1);
                break;
            }
            case Bytecodes.DUP2_X2: {
                Instruction w1 = curState.xpop();
                Instruction w2 = curState.xpop();
                Instruction w3 = curState.xpop();
                Instruction w4 = curState.xpop();
                curState.xpush(w2);
                curState.xpush(w1);
                curState.xpush(w4);
                curState.xpush(w3);
                curState.xpush(w2);
                curState.xpush(w1);
                break;
            }
            case Bytecodes.SWAP: {
                Instruction w1 = curState.xpop();
                Instruction w2 = curState.xpop();
                curState.xpush(w1);
                curState.xpush(w2);
                break;
            }
            default:
                throw Util.shouldNotReachHere();
        }

    }

    void arithmeticOp(BasicType basicType, int opcode) {
        arithmeticOp(basicType, opcode, null);
    }

    void arithmeticOp(BasicType basicType, int opcode, ValueStack stack) {
        Instruction y = pop(basicType);
        Instruction x = pop(basicType);
        Instruction result = append(new ArithmeticOp(opcode, x, y, method().isStrictFP(), stack));
        if (C1XOptions.RoundFPResults && scopeData.scope.method.isStrictFP()) {
            result = roundFp(result);
        }
        push(basicType, result);
    }

    void negateOp(BasicType basicType) {
        push(basicType, append(new NegateOp(pop(basicType))));
    }

    void shiftOp(BasicType basicType, int opcode) {
        Instruction s = ipop();
        Instruction x = pop(basicType);
        // note that strength reduction of e << K >>> K is correctly handled in canonicalizer now
        push(basicType, append(new ShiftOp(opcode, x, s)));
    }

    void logicOp(BasicType basicType, int opcode) {
        Instruction y = pop(basicType);
        Instruction x = pop(basicType);
        push(basicType, append(new LogicOp(opcode, x, y)));
    }

    void compareOp(BasicType basicType, int opcode) {
        ValueStack stateBefore = curState.copy();
        Instruction y = pop(basicType);
        Instruction x = pop(basicType);
        ipush(append(new CompareOp(opcode, x, y, stateBefore)));
    }

    void convert(int opcode, BasicType from, BasicType to) {
        BasicType tt = to.stackType();
        push(tt.basicType, append(new Convert(opcode, pop(from.stackType()), tt)));
    }

    void increment() {
        int index = stream().readLocalIndex();
        int delta = stream().readIncrement();
        Instruction x = curState.localAt(index);
        Instruction y = append(Constant.forInt(delta));
        curState.storeLocal(index, append(new ArithmeticOp(Bytecodes.IADD, x, y, method().isStrictFP(), null)));
    }

    void goto_(int fromBCI, int toBCI) {
        profileBCI(fromBCI);
        append(new Goto(blockAt(toBCI), null, toBCI <= fromBCI)); // backwards branch => safepoint
    }

    void ifNode(Instruction x, Condition cond, Instruction y, ValueStack stateBefore) {
        BlockBegin tsucc = blockAt(stream().readBranchDest());
        BlockBegin fsucc = blockAt(stream().nextBCI());
        int bci = stream().currentBCI();
        boolean isBackwards = tsucc.bci() <= bci || fsucc.bci() <= bci;
        final Instruction instr = append(new If(x, cond, false, y, tsucc, fsucc, isBackwards ? stateBefore : null, isBackwards));
        if (instr instanceof If && C1XOptions.ProfileBranches) {
            ((If) instr).setProfile(method(), bci);
        }
    }

    void ifZero(Condition cond) {
        Instruction y = appendConstant(CiConstant.INT_0);
        ValueStack stateBefore = curState.copy();
        Instruction x = ipop();
        ifNode(x, cond, y, stateBefore);
    }

    void ifNull(Condition cond) {
        Instruction y = appendConstant(CiConstant.NULL_OBJECT);
        ValueStack stateBefore = curState.copy();
        Instruction x = apop();
        ifNode(x, cond, y, stateBefore);
    }

    void ifSame(BasicType basicType, Condition cond) {
        ValueStack stateBefore = curState.copy();
        Instruction y = pop(basicType);
        Instruction x = pop(basicType);
        ifNode(x, cond, y, stateBefore);
    }

    void throw_(int bci) {
        ValueStack stateBefore = curState.copy();
        Throw t = new Throw(apop(), stateBefore);
        appendWithBCI(t, bci, false); // don't bother trying to canonicalize throws
    }

    void checkcast_() {
        CiType type = constantPool().lookupType(stream().readCPI());
        ValueStack stateBefore = valueStackIfClassNotLoaded(type);
        CheckCast c = new CheckCast(type, apop(), stateBefore);
        apush(append(c));
        if (assumeLeafClass(type)) {
            c.setDirectCompare();
        }
        if (C1XOptions.ProfileCheckcasts) {
            c.setProfile(method(), bci());
        }
    }

    void instanceof_() {
        CiType type = constantPool().lookupType(stream().readCPI());
        ValueStack stateBefore = valueStackIfClassNotLoaded(type);
        InstanceOf i = new InstanceOf(type, apop(), stateBefore);
        ipush(append(i));
        if (assumeLeafClass(type)) {
            i.setDirectCompare();
        }
    }

    void newInstance() {
        char cpi = stream().readCPI();
        CiType type = constantPool().lookupType(cpi);
        assert !type.isLoaded() || type.isInstanceClass();
        NewInstance n = new NewInstance(type, cpi, constantPool());
        if (memoryMap != null) {
            memoryMap.newInstance(n);
        }
        apush(append(n));
    }

    void newTypeArray() {
        apush(append(new NewTypeArray(ipop(), BasicType.fromArrayTypeCode(stream().readLocalIndex()))));
    }

    void newObjectArray() {
        char cpi = stream().readCPI();
        CiType type = constantPool().lookupType(cpi);
        ValueStack stateBefore = valueStackIfClassNotLoaded(type);
        NewArray n = new NewObjectArray(type, ipop(), stateBefore, cpi, constantPool());
        apush(append(n));
    }

    void newMultiArray() {
        char cpi = stream().readCPI();
        CiType type = constantPool().lookupType(cpi);
        ValueStack stateBefore = valueStackIfClassNotLoaded(type);
        int rank = stream().readUByte(stream().currentBCI() + 3);
        Instruction[] dims = new Instruction[rank];
        for (int i = rank - 1; i >= 0; i--) {
            dims[i] = ipop();
        }
        NewArray n = new NewMultiArray(type, dims, stateBefore, cpi, constantPool());
        apush(append(n));
    }

    void getField() {
        CiField field = constantPool().lookupGetField(stream().readCPI());
        boolean isLoaded = field.isLoaded() && !C1XOptions.TestPatching;
        ValueStack stateCopy = !isLoaded ? curState.copy() : null;
        LoadField load = new LoadField(apop(), field, false, lockStack(), stateCopy, isLoaded);
        loadField(field.basicType(), load);
    }

    void putField() {
        CiField field = constantPool().lookupPutField(stream().readCPI());
        boolean isLoaded = field.isLoaded() && !C1XOptions.TestPatching;
        ValueStack stateCopy = !isLoaded ? curState.copy() : null;
        Instruction value = pop(field.basicType().stackType());
        storeField(new StoreField(apop(), field, value, false, lockStack(), stateCopy, isLoaded));
    }

    void getStatic() {
        CiField field = constantPool().lookupGetStatic(stream().readCPI());
        CiType holder = field.holder();
        boolean isLoaded = field.isLoaded() && holder.isLoaded() && !C1XOptions.TestPatching;
        boolean isInitialized = isLoaded && holder.isInitialized();
        ValueStack stateCopy = isInitialized ? null : curState.copy();
        Instruction holderConstant = getStaticContainer(holder, isInitialized);
        LoadField load = new LoadField(holderConstant, field, true, lockStack(), stateCopy, isLoaded);
        loadField(field.basicType(), load);
    }

    void putStatic() {
        CiField field = constantPool().lookupPutStatic(stream().readCPI());
        CiType holder = field.holder();
        boolean isLoaded = field.isLoaded() && holder.isLoaded() && !C1XOptions.TestPatching;
        boolean isInitialized = isLoaded && holder.isInitialized();
        ValueStack stateCopy = isInitialized ? null : curState.copy();
        Instruction holderConstant = getStaticContainer(holder, isInitialized);
        Instruction value = pop(field.basicType().stackType());
        StoreField store = new StoreField(holderConstant, field, value, true, lockStack(), stateCopy, isLoaded);
        storeField(store);
    }

    private Instruction getStaticContainer(CiType holder, boolean isInitialized) {
        Instruction holderConstant = null;
        if (isInitialized) {
            holderConstant = appendConstant(holder.getStaticContainer());
        }
        return holderConstant;
    }

    private void storeField(StoreField store) {
        if (memoryMap != null) {
            StoreField previous = memoryMap.store(store);
            if (previous == null) {
                // the store is redundant, do not append
                return;
            }
        }
        append(store);
    }

    private void loadField(BasicType basicType, LoadField load) {
        if (memoryMap != null) {
            Instruction replacement = memoryMap.load(load);
            if (replacement != load) {
                // the memory buffer found a replacement for this load (no need to append)
                push(basicType.stackType(), replacement);
                return;
            }
        }
        // append the load to the instruction
        Instruction optimized = append(load);
        if (memoryMap != null && optimized != load) {
            // local optimization happened, replace its value in the memory map
            memoryMap.setResult(load, optimized);
        }
        push(basicType.stackType(), optimized);
    }

    void invokeStatic(CiMethod target, char cpi, CiConstantPool constantPool) {
        Instruction[] args = curState.popArguments(target.signatureType().argumentSlots(false));
        if (!tryOptimizeCall(target, args, true)) {
            if (!tryInline(target, args, null)) {
                profileInvocation(target);
                appendInvoke(Bytecodes.INVOKESTATIC, target, args, true, cpi, constantPool);
            }
        }
    }

    void invokeInterface(CiMethod target, char cpi, CiConstantPool constantPool) {
        Instruction[] args = curState.popArguments(target.signatureType().argumentSlots(true));
        if (!tryOptimizeCall(target, args, false)) {
            // XXX: attempt devirtualization / deinterfacification of INVOKEINTERFACE
            profileCall(args[0], null);
            appendInvoke(Bytecodes.INVOKEINTERFACE, target, args, false, cpi, constantPool);
        }
    }

    void invokeVirtual(CiMethod target, char cpi, CiConstantPool constantPool) {
        Instruction[] args = curState.popArguments(target.signatureType().argumentSlots(true));
        if (!tryOptimizeCall(target, args, false)) {
            Instruction receiver = args[0];
            // attempt to devirtualize the call
            if (target.isLoaded() && target.holder().isLoaded()) {
                CiType klass = target.holder();
                // 0. check for trivial cases
                if (target.canBeStaticallyBound() && !target.isAbstract()) {
                    // check for trivial cases (e.g. final methods, nonvirtual methods)
                    invokeDirect(target, args, target.holder(), cpi, constantPool);
                    return;
                }
                // 1. check if the exact type of the receiver can be determined
                CiType exact = getExactType(klass, receiver);
                if (exact != null && exact.isLoaded()) {
                    // either the holder class is exact, or the receiver object has an exact type
                    invokeDirect(exact.resolveMethodImpl(target), args, exact, cpi, constantPool);
                    return;
                }
                // 2. check if an assumed leaf method can be found
                CiMethod leaf = getAssumedLeafMethod(target, receiver);
                if (leaf != null && leaf.isLoaded() && !leaf.isAbstract() && leaf.holder().isLoaded()) {
                    invokeDirect(leaf, args, null, cpi, constantPool);
                    return;
                }
                // 3. check if the either of the holder or declared type of receiver can be assumed to be a leaf
                exact = getAssumedLeafType(klass, receiver);
                if (exact != null && exact.isLoaded()) {
                    // either the holder class is exact, or the receiver object has an exact type
                    invokeDirect(exact.resolveMethodImpl(target), args, exact, cpi, constantPool);
                    return;
                }
            }
            // devirtualization failed, produce an actual invokevirtual
            profileCall(args[0], null);
            appendInvoke(Bytecodes.INVOKEVIRTUAL, target, args, false, cpi, constantPool);
        }
    }

    private BasicType returnBasicType(CiMethod target) {
        return target.signatureType().returnBasicType();
    }

    void invokeSpecial(CiMethod target, CiType knownHolder, char cpi, CiConstantPool constantPool) {
        invokeDirect(target, curState.popArguments(target.signatureType().argumentSlots(true)), knownHolder, cpi, constantPool);
    }

    private void invokeDirect(CiMethod target, Instruction[] args, CiType knownHolder, char cpi, CiConstantPool constantPool) {
        if (!tryOptimizeCall(target, args, false)) {
            if (!tryInline(target, args, knownHolder)) {
                // could not optimize or inline the method call
                profileInvocation(target);
                profileCall(args[0], target.holder());
                appendInvoke(Bytecodes.INVOKESPECIAL, target, args, false, cpi, constantPool);
            }
        }
    }

    private void appendInvoke(int opcode, CiMethod target, Instruction[] args, boolean isStatic, char cpi, CiConstantPool constantPool) {
        BasicType resultType = returnBasicType(target);
        Instruction result = append(new Invoke(opcode, resultType.stackType(), args, isStatic, target.vtableIndex(), target, cpi, constantPool));
        if (C1XOptions.RoundFPResults && scopeData.scope.method.isStrictFP()) {
            pushReturn(resultType, roundFp(result));
        } else {
            pushReturn(resultType, result);
        }
    }

    private CiType getExactType(CiType staticType, Instruction receiver) {
        CiType exact = staticType.exactType();
        if (exact == null) {
            exact = receiver.exactType();
            if (exact == null) {
                CiType declared = receiver.declaredType();
                exact = declared == null ? null : declared.exactType();
            }
        }
        return exact;
    }

    private CiType getAssumedLeafType(CiType staticType, Instruction receiver) {
        if (assumeLeafClass(staticType)) {
            return staticType;
        }
        CiType declared = receiver.declaredType();
        if (declared != null && assumeLeafClass(declared)) {
            return declared;
        }
        return null;
    }

    private CiMethod getAssumedLeafMethod(CiMethod target, Instruction receiver) {
        if (assumeLeafMethod(target)) {
            return target;
        }
        CiType declared = receiver.declaredType();
        if (declared != null && declared.isLoaded() && !declared.isInterface()) {
            CiMethod impl = declared.resolveMethodImpl(target);
            if (impl != null && assumeLeafClass(declared)) {
                return impl;
            }
        }
        return null;
    }

    Instruction getReceiver(CiMethod target) {
        return curState.stackAt(curState.stackSize() - target.signatureType().argumentSlots(false) - 1);
    }

    Instruction[] popArguments(CiMethod target) {
        return curState.popArguments(target.signatureType().argumentSlots(false));
    }

    void callRegisterFinalizer() {
        Instruction receiver = curState.loadLocal(0);
        CiType declaredType = receiver.declaredType();
        CiType receiverType = declaredType;
        CiType exactType = receiver.exactType();
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
            loadLocal(0, BasicType.Object);
            append(new Intrinsic(BasicType.Void, C1XIntrinsic.java_lang_Object$init,
                                          curState.popArguments(1), false, lockStack(), true, true));
            C1XMetrics.InlinedFinalizerChecks++;
        }

    }

    void methodReturn(Instruction x) {
        if (C1XOptions.RegisterFinalizersAtInit) {
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
                assert C1XOptions.InlineSynchronizedMethods;
                int i = curState.scope().callerState().locksSize();
                assert curState.locksSize() == i + 1;
                monitorexit(curState.lockAt(i), Instruction.SYNCHRONIZATION_ENTRY_BCI);
            }

            // trim back stack to the caller's stack size
            curState.truncateStack(scopeData.callerStackSize());
            if (x != null) {
                curState.push(x.type().basicType, x);
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
                curState.push(x.type().basicType, x);
            }

            // The current bci() is in the wrong scope, so use the bci() of
            // the continuation point.
            appendWithBCI(gotoCallee, scopeData.continuation().bci(), false);
            scopeData.incrementNumberOfReturns();
            return;
        }

        curState.truncateStack(0);
        if (method().isSynchronized()) {
            // unlock before exiting the method
            append(new MonitorExit(synchronizedObject(initialState, method()), curState.unlock()));
        }
        append(new Return(x));
    }

    private ValueStack valueStackIfClassNotLoaded(CiType type) {
        return !type.isLoaded() || C1XOptions.TestPatching ? curState.copy() : null;
    }

    void monitorenter(Instruction x, int bci) {
        ValueStack lockStack = lockStack();
        appendWithBCI(new MonitorEnter(x, curState.lock(scope(), x), lockStack), bci, false);
        killMemoryMap(); // prevent any optimizations across synchronization
    }

    void monitorexit(Instruction x, int bci) {
        // Mysterious C1 comment:
        // Note: the comment below is only relevant for the case where we do
        // not deoptimize due to asynchronous exceptions (!(DeoptC1 &&
        // DeoptOnAsyncException), which is not used anymore)

        // Note: Potentially, the monitor state in an exception handler
        //       can be wrong due to wrong 'initialization' of the handler
        //       via a wrong asynchronous exception path. This can happen,
        //       if the exception handler range for asynchronous exceptions
        //       is too long (see also java bug 4327029, and comment in
        //       GraphBuilder::handle_exception()). This may cause 'under-
        //       flow' of the monitor stack => bailout instead.
        if (curState.locksSize() < 1) {
            throw new Bailout("monitor stack underflow");
        }
        appendWithBCI(new MonitorExit(x, curState.unlock()), bci, false);
        killMemoryMap(); // prevent any optimizations across synchronization
    }

    void jsr(int dest) {
        for (ScopeData cur = scopeData; cur != null && cur.parsingJsr() && cur.scope == scope(); cur = cur.parent) {
            if (cur.jsrEntryBCI() == dest) {
                // the jsr/ret pattern includes a recursive invocation
                throw new Bailout("recursive jsr/ret structure");
            }
        }
        push(BasicType.Jsr, append(Constant.forJsr(nextBCI())));
        tryInlineJsr(dest);
    }

    void ret(int localIndex) {
        if (!scopeData.parsingJsr()) {
            throw new Bailout("ret encountered when not parsing subroutine");
        }

        if (localIndex != scopeData.jsrEntryReturnAddressLocal()) {
            throw new Bailout("jsr/ret structure is too complicated");
        }
        // rets become non-safepoint gotos
        append(new Goto(scopeData.jsrContinuation(), null, false));
    }

    void tableswitch() {
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

    void lookupswitch() {
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

    private boolean cseArrayLength(Instruction array) {
        // checks whether an array length access should be generated for CSE
        if (C1XOptions.AlwaysCSEArrayLength) {
            // always access the length for CSE
            return true;
        } else if (array.isConstant()) {
            // the array itself is a constant
            return true;
        } else if (array instanceof AccessField && ((AccessField) array).field().isConstant()) {
            // the length is derived from a constant array
            return true;
        } else  if (array instanceof NewArray) {
            // the array is derived from an allocation
            final Instruction length = ((NewArray) array).length();
            return length != null && length.isConstant();
        }
        return false;
    }

    private void profileCall(Instruction receiver, CiType knownHolder) {
        if (C1XOptions.ProfileCalls) {
            append(new ProfileCall(method(), bci(), receiver, knownHolder));
        }
    }

    private void profileInvocation(CiMethod callee) {
        if (C1XOptions.ProfileCalls) {
            CiMethodData mdo = callee.methodData();
            if (mdo != null) {
                int offset = mdo.invocationCountOffset();
                if (offset >= 0) {
                    // if the method data object exists and it has an entry for the invocation count
                    Instruction m = append(Constant.forObject(mdo.dataObject()));
                    append(new ProfileCounter(m, offset, 1));
                }
            }
        }
    }

    private void profileBCI(int bci) {
        if (C1XOptions.ProfileBranches) {
            CiMethodData mdo = method().methodData();
            if (mdo != null) {
                int offset = mdo.bciCountOffset(bci);
                if (offset >= 0) {
                    // if the method data object exists and it has an entry for the bytecode index
                    Instruction m = append(Constant.forObject(mdo.dataObject()));
                    append(new ProfileCounter(m, offset, 1));
                }
            }
        }
    }

    private Instruction appendConstant(CiConstant type) {
        return appendWithBCI(new Constant(type), bci(), false); // don't bother trying to canonicalize/lvn a constant
    }

    private Instruction append(Instruction x) {
        return appendWithBCI(x, bci(), C1XOptions.CanonicalizeInstructions);
    }

    private Instruction appendWithBCI(Instruction x, int bci, boolean canonicalize) {
        if (canonicalize) {
            // attempt simple constant folding and strength reduction
            Canonicalizer canon = new Canonicalizer(x, bci);
            List<Instruction> extra = canon.extra();
            if (extra != null) {
                // the canonicalization introduced instructions that should be added before this
                for (Instruction i : extra) {
                    appendWithBCI(i, bci, false); // don't try to canonicalize the new instructions
                }
            }
            x = canon.canonical();
        }
        if (x.isAppended()) {
            // the instruction has already been added
            return x;
        }
        if (C1XOptions.UseLocalValueNumbering) {
            // look in the local value map
            Instruction r = localValueMap.findInsert(x);
            if (r != x) {
                C1XMetrics.LocalValueNumberHits++;
                assert r.isAppended() : "lvn result should already be appended";
                return r;
            }
        }

        if (!(x instanceof Phi || x instanceof Local)) {
            // add instructions to the basic block (if not a phi or a local)
            assert x.next() == null : "instruction should not have been appended yet";
            lastInstr = lastInstr.setNext(x, bci);
            if (++totalInstructions >= C1XOptions.MaximumInstructionCount) {
                // bailout if we've exceeded the maximum inlining size
                throw new Bailout("Method and/or inlining is too large");
            }

            if (x instanceof StateSplit) {
                if (x instanceof Invoke || x instanceof Intrinsic && !((Intrinsic) x).preservesState()) {
                    // conservatively kill all memory across calls
                    if (memoryMap != null) {
                        memoryMap.kill();
                    }
                }
                // split the state for any state split operations
                ((StateSplit) x).setState(curState.copy());
            }

            if (x.canTrap()) {
                // connect the instruction to any exception handlers
                assert exceptionState != null || !hasHandler() : "must have setup exception state";
                x.setExceptionHandlers(handleException(bci));
            }
        }

        return x;
    }

    ValueStack lockStack() {
        return curState.copyLocks();
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
        gotoSub.setState(curState);
        assert jsrStartBlock.state() == null;
        jsrStartBlock.setState(curState.copy());
        append(gotoSub);
        curBlock.setEnd(gotoSub);
        lastInstr = curBlock = jsrStartBlock;

        scopeData.addToWorkList(jsrStartBlock);

        iterateAllBlocks();

        if (cont.state() != null) {
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
        return true;
    }

    void pushScopeForJsr(BlockBegin jsrCont, int jsrStart) {
        BytecodeStream stream = new BytecodeStream(scope().method.code());
        CiConstantPool constantPool = scopeData.constantPool;
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

    void pushScope(CiMethod target, BlockBegin continuation) {
        IRScope calleeScope = new IRScope(compilation, scope(), bci(), target, -1);
        scope().addCallee(calleeScope);
        BlockMap blockMap = compilation.getBlockMap(calleeScope.method, -1);

        calleeScope.setCallerState(curState);
        calleeScope.setStoresInLoops(blockMap.getStoresInLoops());
        curState = curState.pushScope(calleeScope);
        BytecodeStream stream = new BytecodeStream(target.code());
        CiConstantPool constantPool = compilation.runtime.getConstantPool(target);
        ScopeData data = new ScopeData(scopeData, calleeScope, blockMap, stream, constantPool);
        data.setContinuation(continuation);
        scopeData = data;
    }

    ValueStack stateAtEntry() {
        CiMethod method = method();
        ValueStack state = new ValueStack(scope(), method.maxLocals(), method.maxStackSize());
        int index = 0;
        if (!method.isStatic()) {
            // add the receiver and assume it is non null
            Local local = new Local(BasicType.Object, index);
            local.setFlag(Instruction.Flag.NonNull, true);
            local.setDeclaredType(method.holder());
            state.storeLocal(index, local);
            index = 1;
        }
        CiSignature sig = method.signatureType();
        int max = sig.argumentCount(false);
        for (int i = 0; i < max; i++) {
            CiType type = sig.argumentTypeAt(i);
            BasicType vt = type.basicType().stackType();
            Local local = new Local(vt, index);
            if (type.isLoaded()) {
                local.setDeclaredType(type);
            }
            state.storeLocal(index, local);
            index += vt.sizeInSlots();
        }

        if (method.isSynchronized()) {
            state.lock(scope(), null); // XXX: why do we lock null?
        }
        return state;
    }

    boolean tryOptimizeCall(CiMethod target, Instruction[] args, boolean isStatic) {
        if (target.isLoaded()) {
            if (C1XOptions.InlineIntrinsics) {
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

    private boolean tryInlineIntrinsic(CiMethod target, Instruction[] args, boolean isStatic, C1XIntrinsic intrinsic) {
        boolean preservesState = true;
        boolean canTrap = false;

        // handle intrinsics differently
        switch (intrinsic) {
            // java.lang.Object
            case java_lang_Object$init:     // fall through
            case java_lang_Object$clone:    return false;
            // TODO: preservesState and canTrap for complex intrinsics
        }

        // get the arguments for the intrinsic
        BasicType resultType = returnBasicType(target);

        // create the intrinsic node
        Intrinsic result = new Intrinsic(resultType.stackType(), intrinsic, args, isStatic, lockStack(), preservesState, canTrap);
        pushReturn(resultType, append(result));
        C1XMetrics.InlinedIntrinsics++;
        return true;
    }

    private boolean tryFoldable(CiMethod target, Instruction[] args) {
        CiConstant result = Canonicalizer.foldInvocation(target, args);
        if (result != null) {
            pushReturn(returnBasicType(target), new Constant(result));
            return true;
        }
        return false;
    }

    boolean tryInline(CiMethod target, Instruction[] args, CiType knownHolder) {
        return checkInliningConditions(target) && tryInlineFull(target, args, knownHolder);
    }

    boolean checkInliningConditions(CiMethod target) {
        if (!C1XOptions.InlineMethods) {
            return false; // all inlining is turned off
        }
        if (!target.isLoaded()) {
            return cannotInline(target, "method is not resolved");
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
        if (totalInstructions > C1XOptions.MaximumDesiredSize) {
            return cannotInline(target, "compilation already too big " + "(" + compilation.totalInstructions() + " insts)");
        }
        if (compilation.runtime.mustNotInline(target)) {
            return cannotInline(target, "inlining excluded by runtime");
        }
        if (compilation.runtime.mustNotCompile(target)) {
            return cannotInline(target, "compile excluded by runtime");
        }
        if (target.isAbstract()) {
            return cannotInline(target, "is abstract");
        }
        if (target.isNative()) {
            return cannotInline(target, "is native");
        }
        if (target.isSynchronized() && !C1XOptions.InlineSynchronizedMethods) {
            return cannotInline(target, "is synchronized");
        }
        if (target.hasExceptionHandlers() && !C1XOptions.InlineMethodsWithExceptionHandlers) {
            return cannotInline(target, "has exception handlers");
        }
        if (!target.hasBalancedMonitors()) {
            return cannotInline(target, "has unbalanced monitors");
        }
        if (C1XOptions.SSEVersion < 2 && target.isStrictFP() != method().isStrictFP()) {
            return cannotInline(target, "strictfp mismatch on x87");
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

    boolean cannotInline(CiMethod target, String reason) {
        compilation.recordInliningFailure(target, reason);
        return false;
    }

    boolean tryInlineFull(CiMethod target, Instruction[] args, CiType knownHolder) {
        BlockBegin orig = curBlock;
        Instruction receiver = null;
        if (!target.isStatic()) {
            // the receiver object must be nullchecked for instance methods
            receiver = args[0];
            appendNullCheck(receiver);
        }

        if (C1XOptions.ProfileInlinedCalls) {
            profileCall(receiver, knownHolder);
        }

        profileInvocation(target);

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
            Instruction arg = args[i];
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

        Instruction lock = null;
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
            gotoCallee.setState(curState);
            appendWithBCI(gotoCallee, 0, false);
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

        C1XMetrics.InlinedMethods++;
        return true;
    }

    private Instruction synchronizedObject(ValueStack state, CiMethod target) {
        return target.isStatic() ? append(Constant.forObject(target.holder().javaClass())) : state.localAt(0);
    }

    void inlineSyncEntry(Instruction lock, BlockBegin syncHandler) {
        exceptionState = curState.copy();
        monitorenter(lock, Instruction.SYNCHRONIZATION_ENTRY_BCI);
        lastInstr.setFlag(Instruction.Flag.NonNull, true);
        syncHandler.setExceptionEntry();
        syncHandler.setBlockFlag(BlockBegin.BlockFlag.IsOnWorkList);
        CiExceptionHandler handler = newDefaultExceptionHandler(method());
        ExceptionHandler h = new ExceptionHandler(handler);
        h.setEntryBlock(syncHandler);
        scopeData.addExceptionHandler(h);
    }

    void fillSyncHandler(Instruction lock, BlockBegin syncHandler, boolean defaultHandler) {
        BlockBegin origBlock = curBlock;
        ValueStack origState = curState;
        Instruction origLast = lastInstr;

        lastInstr = curBlock = syncHandler;
        curState = syncHandler.state().copy();

        assert !syncHandler.wasVisited() : "synch handler already visited";

        curBlock.setWasVisited(true);
        Instruction exception = appendWithBCI(new ExceptionObject(), Instruction.SYNCHRONIZATION_ENTRY_BCI, false);

        int bci = Instruction.SYNCHRONIZATION_ENTRY_BCI;
        if (lock != null) {
            assert curState.locksSize() > 0 && curState.lockAt(curState.locksSize() - 1) == lock;
            if (!lock.isAppended()) {
                lock = appendWithBCI(lock, Instruction.SYNCHRONIZATION_ENTRY_BCI, false);
            }
            // exit the monitor
            monitorexit(lock, Instruction.SYNCHRONIZATION_ENTRY_BCI);

            // exit the context of the synchronized method
            if (!defaultHandler) {
                popScope();
                curState = curState.copy();
                bci = curState.scope().callerBCI();
                curState = curState.popScope().copy();
            }
        }

        apush(exception);
        exceptionState = curState.copy();
        throw_(bci);
        BlockEnd end = (BlockEnd) lastInstr;
        curBlock.setEnd(end);
        end.setState(curState);

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
                curState = b.state().copy();
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
        CiOsrFrame frame = compilation.getOsrFrame();
        s.setBCI(osrBCI);
        s.next(); // XXX: why go to next bytecode?

        // create a new block to contain the OSR setup code
        ir.osrEntryBlock = new BlockBegin(osrBCI, ir.nextBlockNumber());
        ir.osrEntryBlock.setOsrEntry(true);
        ir.osrEntryBlock.setDepthFirstNumber(0);

        // get the target block of the OSR
        BlockBegin target = scopeData.blockAt(osrBCI);
        assert target != null && target.isOsrEntry();

        ValueStack state = target.state().copy();
        ir.osrEntryBlock.setState(state);

        killMemoryMap();
        curBlock = ir.osrEntryBlock;
        curState = state.copy();
        lastInstr = ir.osrEntryBlock;

        // create the entry instruction which represents the OSR state buffer
        // input from interpreter / JIT
        Instruction e = new OsrEntry();
        e.setFlag(Instruction.Flag.NonNull, true);

        for (int i = 0; i < state.localsSize(); i++) {
            Instruction local = state.localAt(i);
            Instruction get;
            int offset = frame.getLocalOffset(i);
            if (local != null) {
                // this is a live local according to compiler
                if (local.type().isObject() && !frame.isLiveObject(i)) {
                    // the compiler thinks this is live, but not the interpreter
                    // pretend that it passed null
                    get = appendConstant(CiConstant.NULL_OBJECT);
                } else {
                    Instruction oc = appendConstant(CiConstant.forInt(offset));
                    get = append(new UnsafeGetRaw(local.type().basicType, e, oc, 0, true));
                }
                state.storeLocal(i, get);
            }
        }

        assert state.scope().callerState() == null;
        state.clearLocals();
        Goto g = new Goto(target, state.copy(), false);
        append(g);
        ir.osrEntryBlock.setEnd(g);
        target.merge(ir.osrEntryBlock.end().state());
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

            // check whether the bytecode can cause an exception
            exceptionState = hasHandler() && Bytecodes.canTrap(opcode) ? curState.copy() : null;

            // check for active JSR during OSR compilation
            if (compilation.isOsrCompilation()
                    && scope().isTopScope()
                    && scopeData.parsingJsr()
                    && s.currentBCI() == compilation.osrBCI) {
                throw new Bailout("OSR not supported while a JSR is active");
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
                case Bytecodes.LDC2_W         : loadConstant(); break;
                case Bytecodes.ILOAD          : loadLocal(s.readLocalIndex(), BasicType.Int); break;
                case Bytecodes.LLOAD          : loadLocal(s.readLocalIndex(), BasicType.Long); break;
                case Bytecodes.FLOAD          : loadLocal(s.readLocalIndex(), BasicType.Float); break;
                case Bytecodes.DLOAD          : loadLocal(s.readLocalIndex(), BasicType.Double); break;
                case Bytecodes.ALOAD          : loadLocal(s.readLocalIndex(), BasicType.Object); break;
                case Bytecodes.ILOAD_0        : loadLocal(0, BasicType.Int); break;
                case Bytecodes.ILOAD_1        : loadLocal(1, BasicType.Int); break;
                case Bytecodes.ILOAD_2        : loadLocal(2, BasicType.Int); break;
                case Bytecodes.ILOAD_3        : loadLocal(3, BasicType.Int); break;
                case Bytecodes.LLOAD_0        : loadLocal(0, BasicType.Long); break;
                case Bytecodes.LLOAD_1        : loadLocal(1, BasicType.Long); break;
                case Bytecodes.LLOAD_2        : loadLocal(2, BasicType.Long); break;
                case Bytecodes.LLOAD_3        : loadLocal(3, BasicType.Long); break;
                case Bytecodes.FLOAD_0        : loadLocal(0, BasicType.Float); break;
                case Bytecodes.FLOAD_1        : loadLocal(1, BasicType.Float); break;
                case Bytecodes.FLOAD_2        : loadLocal(2, BasicType.Float); break;
                case Bytecodes.FLOAD_3        : loadLocal(3, BasicType.Float); break;
                case Bytecodes.DLOAD_0        : loadLocal(0, BasicType.Double); break;
                case Bytecodes.DLOAD_1        : loadLocal(1, BasicType.Double); break;
                case Bytecodes.DLOAD_2        : loadLocal(2, BasicType.Double); break;
                case Bytecodes.DLOAD_3        : loadLocal(3, BasicType.Double); break;
                case Bytecodes.ALOAD_0        : loadLocal(0, BasicType.Object); break;
                case Bytecodes.ALOAD_1        : loadLocal(1, BasicType.Object); break;
                case Bytecodes.ALOAD_2        : loadLocal(2, BasicType.Object); break;
                case Bytecodes.ALOAD_3        : loadLocal(3, BasicType.Object); break;
                case Bytecodes.IALOAD         : loadIndexed(BasicType.Int   ); break;
                case Bytecodes.LALOAD         : loadIndexed(BasicType.Long  ); break;
                case Bytecodes.FALOAD         : loadIndexed(BasicType.Float ); break;
                case Bytecodes.DALOAD         : loadIndexed(BasicType.Double); break;
                case Bytecodes.AALOAD         : loadIndexed(BasicType.Object); break;
                case Bytecodes.BALOAD         : loadIndexed(BasicType.Byte  ); break;
                case Bytecodes.CALOAD         : loadIndexed(BasicType.Char  ); break;
                case Bytecodes.SALOAD         : loadIndexed(BasicType.Short ); break;
                case Bytecodes.ISTORE         : storeLocal(BasicType.Int, s.readLocalIndex()); break;
                case Bytecodes.LSTORE         : storeLocal(BasicType.Long, s.readLocalIndex()); break;
                case Bytecodes.FSTORE         : storeLocal(BasicType.Float, s.readLocalIndex()); break;
                case Bytecodes.DSTORE         : storeLocal(BasicType.Double, s.readLocalIndex()); break;
                case Bytecodes.ASTORE         : storeLocal(BasicType.Object, s.readLocalIndex()); break;
                case Bytecodes.ISTORE_0       : storeLocal(BasicType.Int, 0); break;
                case Bytecodes.ISTORE_1       : storeLocal(BasicType.Int, 1); break;
                case Bytecodes.ISTORE_2       : storeLocal(BasicType.Int, 2); break;
                case Bytecodes.ISTORE_3       : storeLocal(BasicType.Int, 3); break;
                case Bytecodes.LSTORE_0       : storeLocal(BasicType.Long, 0); break;
                case Bytecodes.LSTORE_1       : storeLocal(BasicType.Long, 1); break;
                case Bytecodes.LSTORE_2       : storeLocal(BasicType.Long, 2); break;
                case Bytecodes.LSTORE_3       : storeLocal(BasicType.Long, 3); break;
                case Bytecodes.FSTORE_0       : storeLocal(BasicType.Float, 0); break;
                case Bytecodes.FSTORE_1       : storeLocal(BasicType.Float, 1); break;
                case Bytecodes.FSTORE_2       : storeLocal(BasicType.Float, 2); break;
                case Bytecodes.FSTORE_3       : storeLocal(BasicType.Float, 3); break;
                case Bytecodes.DSTORE_0       : storeLocal(BasicType.Double, 0); break;
                case Bytecodes.DSTORE_1       : storeLocal(BasicType.Double, 1); break;
                case Bytecodes.DSTORE_2       : storeLocal(BasicType.Double, 2); break;
                case Bytecodes.DSTORE_3       : storeLocal(BasicType.Double, 3); break;
                case Bytecodes.ASTORE_0       : storeLocal(BasicType.Object, 0); break;
                case Bytecodes.ASTORE_1       : storeLocal(BasicType.Object, 1); break;
                case Bytecodes.ASTORE_2       : storeLocal(BasicType.Object, 2); break;
                case Bytecodes.ASTORE_3       : storeLocal(BasicType.Object, 3); break;
                case Bytecodes.IASTORE        : storeIndexed(BasicType.Int   ); break;
                case Bytecodes.LASTORE        : storeIndexed(BasicType.Long  ); break;
                case Bytecodes.FASTORE        : storeIndexed(BasicType.Float ); break;
                case Bytecodes.DASTORE        : storeIndexed(BasicType.Double); break;
                case Bytecodes.AASTORE        : storeIndexed(BasicType.Object); break;
                case Bytecodes.BASTORE        : storeIndexed(BasicType.Byte  ); break;
                case Bytecodes.CASTORE        : storeIndexed(BasicType.Char  ); break;
                case Bytecodes.SASTORE        : storeIndexed(BasicType.Short ); break;
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
                case Bytecodes.IMUL           : arithmeticOp(BasicType.Int, opcode); break;
                case Bytecodes.IDIV           : // fall through
                case Bytecodes.IREM           : arithmeticOp(BasicType.Int, opcode, lockStack()); break;
                case Bytecodes.LADD           : // fall through
                case Bytecodes.LSUB           : // fall through
                case Bytecodes.LMUL           : arithmeticOp(BasicType.Long, opcode); break;
                case Bytecodes.LDIV           : // fall through
                case Bytecodes.LREM           : arithmeticOp(BasicType.Long, opcode, lockStack()); break;
                case Bytecodes.FADD           : // fall through
                case Bytecodes.FSUB           : // fall through
                case Bytecodes.FMUL           : // fall through
                case Bytecodes.FDIV           : // fall through
                case Bytecodes.FREM           : arithmeticOp(BasicType.Float, opcode); break;
                case Bytecodes.DADD           : // fall through
                case Bytecodes.DSUB           : // fall through
                case Bytecodes.DMUL           : // fall through
                case Bytecodes.DDIV           : // fall through
                case Bytecodes.DREM           : arithmeticOp(BasicType.Double, opcode); break;
                case Bytecodes.INEG           : negateOp(BasicType.Int); break;
                case Bytecodes.LNEG           : negateOp(BasicType.Long); break;
                case Bytecodes.FNEG           : negateOp(BasicType.Float); break;
                case Bytecodes.DNEG           : negateOp(BasicType.Double); break;
                case Bytecodes.ISHL           : // fall through
                case Bytecodes.ISHR           : // fall through
                case Bytecodes.IUSHR          : shiftOp(BasicType.Int, opcode); break;
                case Bytecodes.IAND           : // fall through
                case Bytecodes.IOR            : // fall through
                case Bytecodes.IXOR           : logicOp(BasicType.Int, opcode); break;
                case Bytecodes.LSHL           : // fall through
                case Bytecodes.LSHR           : // fall through
                case Bytecodes.LUSHR          : shiftOp(BasicType.Long, opcode); break;
                case Bytecodes.LAND           : // fall through
                case Bytecodes.LOR            : // fall through
                case Bytecodes.LXOR           : logicOp(BasicType.Long, opcode); break;
                case Bytecodes.IINC           : increment(); break;
                case Bytecodes.I2L            : convert(opcode, BasicType.Int   , BasicType.Long  ); break;
                case Bytecodes.I2F            : convert(opcode, BasicType.Int   , BasicType.Float ); break;
                case Bytecodes.I2D            : convert(opcode, BasicType.Int   , BasicType.Double); break;
                case Bytecodes.L2I            : convert(opcode, BasicType.Long  , BasicType.Int   ); break;
                case Bytecodes.L2F            : convert(opcode, BasicType.Long  , BasicType.Float ); break;
                case Bytecodes.L2D            : convert(opcode, BasicType.Long  , BasicType.Double); break;
                case Bytecodes.F2I            : convert(opcode, BasicType.Float , BasicType.Int   ); break;
                case Bytecodes.F2L            : convert(opcode, BasicType.Float , BasicType.Long  ); break;
                case Bytecodes.F2D            : convert(opcode, BasicType.Float , BasicType.Double); break;
                case Bytecodes.D2I            : convert(opcode, BasicType.Double, BasicType.Int   ); break;
                case Bytecodes.D2L            : convert(opcode, BasicType.Double, BasicType.Long  ); break;
                case Bytecodes.D2F            : convert(opcode, BasicType.Double, BasicType.Float ); break;
                case Bytecodes.I2B            : convert(opcode, BasicType.Int   , BasicType.Byte  ); break;
                case Bytecodes.I2C            : convert(opcode, BasicType.Int   , BasicType.Char  ); break;
                case Bytecodes.I2S            : convert(opcode, BasicType.Int   , BasicType.Short ); break;
                case Bytecodes.LCMP           : compareOp(BasicType.Long, opcode); break;
                case Bytecodes.FCMPL          : compareOp(BasicType.Float, opcode); break;
                case Bytecodes.FCMPG          : compareOp(BasicType.Float, opcode); break;
                case Bytecodes.DCMPL          : compareOp(BasicType.Double, opcode); break;
                case Bytecodes.DCMPG          : compareOp(BasicType.Double, opcode); break;
                case Bytecodes.IFEQ           : ifZero(Condition.eql); break;
                case Bytecodes.IFNE           : ifZero(Condition.neq); break;
                case Bytecodes.IFLT           : ifZero(Condition.lss); break;
                case Bytecodes.IFGE           : ifZero(Condition.geq); break;
                case Bytecodes.IFGT           : ifZero(Condition.gtr); break;
                case Bytecodes.IFLE           : ifZero(Condition.leq); break;
                case Bytecodes.IF_ICMPEQ      : ifSame(BasicType.Int, Condition.eql); break;
                case Bytecodes.IF_ICMPNE      : ifSame(BasicType.Int, Condition.neq); break;
                case Bytecodes.IF_ICMPLT      : ifSame(BasicType.Int, Condition.lss); break;
                case Bytecodes.IF_ICMPGE      : ifSame(BasicType.Int, Condition.geq); break;
                case Bytecodes.IF_ICMPGT      : ifSame(BasicType.Int, Condition.gtr); break;
                case Bytecodes.IF_ICMPLE      : ifSame(BasicType.Int, Condition.leq); break;
                case Bytecodes.IF_ACMPEQ      : ifSame(BasicType.Object, Condition.eql); break;
                case Bytecodes.IF_ACMPNE      : ifSame(BasicType.Object, Condition.neq); break;
                case Bytecodes.GOTO           : goto_(s.currentBCI(), s.readBranchDest()); break;
                case Bytecodes.JSR            : jsr(s.readBranchDest()); break;
                case Bytecodes.RET            : ret(s.readLocalIndex()); break;
                case Bytecodes.TABLESWITCH    : tableswitch(); break;
                case Bytecodes.LOOKUPSWITCH   : lookupswitch(); break;
                case Bytecodes.IRETURN        : methodReturn(ipop()); break;
                case Bytecodes.LRETURN        : methodReturn(lpop()); break;
                case Bytecodes.FRETURN        : methodReturn(fpop()); break;
                case Bytecodes.DRETURN        : methodReturn(dpop()); break;
                case Bytecodes.ARETURN        : methodReturn(apop()); break;
                case Bytecodes.RETURN         : methodReturn(null  ); break;
                case Bytecodes.GETSTATIC      : getStatic(); break;
                case Bytecodes.PUTSTATIC      : putStatic(); break;
                case Bytecodes.GETFIELD       : getField(); break;
                case Bytecodes.PUTFIELD       : putField(); break;
                case Bytecodes.INVOKEVIRTUAL  : cpi = s.readCPI(); invokeVirtual(constantPool().lookupInvokeVirtual(cpi), cpi, constantPool()); break;
                case Bytecodes.INVOKESPECIAL  : cpi = s.readCPI(); invokeSpecial(constantPool().lookupInvokeSpecial(cpi), null, cpi, constantPool()); break;
                case Bytecodes.INVOKESTATIC   : cpi = s.readCPI(); invokeStatic(constantPool().lookupInvokeStatic(cpi), cpi, constantPool()); break;
                case Bytecodes.INVOKEINTERFACE: cpi = s.readCPI(); invokeInterface(constantPool().lookupInvokeInterface(cpi), cpi, constantPool()); break;
                case Bytecodes.NEW            : newInstance(); break;
                case Bytecodes.NEWARRAY       : newTypeArray(); break;
                case Bytecodes.ANEWARRAY      : newObjectArray(); break;
                case Bytecodes.ARRAYLENGTH    : ipush(append(new ArrayLength(apop(), lockStack()))); break;
                case Bytecodes.ATHROW         : throw_(s.currentBCI()); break;
                case Bytecodes.CHECKCAST      : checkcast_(); break;
                case Bytecodes.INSTANCEOF     : instanceof_(); break;
                case Bytecodes.MONITORENTER   : monitorenter(apop(), s.currentBCI()); break;
                case Bytecodes.MONITOREXIT    : monitorexit(apop(), s.currentBCI()); break;
                case Bytecodes.MULTIANEWARRAY : newMultiArray(); break;
                case Bytecodes.IFNULL         : ifNull(Condition.eql); break;
                case Bytecodes.IFNONNULL      : ifNull(Condition.neq); break;
                case Bytecodes.GOTO_W         : goto_(s.currentBCI(), s.readFarBranchDest()); break;
                case Bytecodes.JSR_W          : jsr(s.readFarBranchDest()); break;
                case Bytecodes.BREAKPOINT:
                    throw new Bailout("concurrent setting of breakpoint");
                default:
                    throw new Bailout("unknown bytecode " + opcode);
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
        assert end != null;
        curBlock.setEnd(end);
        end.setState(curState);
        // propagate the state
        for (BlockBegin succ : end.successors()) {
            assert succ.predecessors().contains(curBlock);
            succ.merge(curState);
            scopeData.addToWorkList(succ);
        }
        return end;
    }

    void killMemoryMap() {
        if (localValueMap != null) {
            localValueMap.killAll();
        }
        if (memoryMap != null) {
            memoryMap.kill();
        }
    }

    boolean assumeLeafClass(CiType type) {
        if (!C1XOptions.TestSlowPath && type.isLoaded()) {
            if (type.isFinal()) {
                return true;
            }
            if (C1XOptions.UseDeopt && C1XOptions.UseCHA) {
                if (!type.hasSubclass() && !type.isInterface()) {
                    return compilation.recordLeafTypeAssumption(type);
                }
            }
        }
        return false;
    }

    boolean assumeLeafMethod(CiMethod method) {
        if (!C1XOptions.TestSlowPath && method.isLoaded()) {
            if (method.isFinalMethod()) {
                return true;
            }
            if (C1XOptions.UseDeopt && C1XOptions.UseCHALeafMethods) {
                if (!method.isOverridden() && !method.holder().isInterface()) {
                    return compilation.recordLeafMethodAssumption(method);
                }
            }
        }
        return false;
    }

    int recursiveInlineLevel(CiMethod target) {
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

    CiConstantPool constantPool() {
        return scopeData.constantPool;
    }

    /**
     * Returns the number of instructions parsed into this graph.
     * @return the number of instructions parsed into the graph
     */
    public int totalInstructions() {
        return totalInstructions;
    }
}
