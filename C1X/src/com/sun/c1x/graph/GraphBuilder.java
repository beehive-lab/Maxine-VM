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

import com.sun.c1x.*;
import com.sun.c1x.bytecode.*;
import com.sun.c1x.opt.Canonicalizer;
import com.sun.c1x.opt.ValueMap;
import com.sun.c1x.opt.PhiSimplifier;
import com.sun.c1x.ci.*;
import com.sun.c1x.util.Util;
import com.sun.c1x.value.*;
import com.sun.c1x.ir.*;

import java.util.*;

/**
 * The <code>GraphBuilder</code> class parses the bytecode of a method and builds the IR graph.
 * A number of optimizations may be performed during parsing of the bytecode, including value
 * numbering, inlining, constant folding, strength reduction, etc.
 *
 * @author Ben L. Titzer
 */
public class GraphBuilder {
    final C1XCompilation _compilation;

    // for each instance of GraphBuilder
    ScopeData _scopeData;                   // Per-scope data; used for inlining
    ValueMap _vmap;                         // the map of values encountered (for CSE)
    MemoryBuffer _memory;
    int _instructionCount;                  // for bailing out in pathological jsr/ret cases
    BlockBegin _start;                      // the start block
    BlockBegin _osrEntry;                   // the osr entry block
    ValueStack _initialState;               // The state for the start block

    // for each call to connectToEnd; can also be set by inliner
    BlockBegin _block;                      // the current block
    ValueStack _state;                      // the current execution state
    ValueStack _exceptionState;             // state that will be used by handle_exception
    Instruction _last;                      // the last instruction added
    boolean _skipBlock;                     // skip processing of the rest of this block

    /**
     * Creates a new instance and builds the graph for a the specified IRScope.
     * @param compilation the compilation
     * @param scope the top IRScope
     */
    public GraphBuilder(C1XCompilation compilation, IRScope scope) {
        _compilation = compilation;
        _memory = new MemoryBuffer();
        int osrBCI = compilation.osrBCI();
        BlockMap blockMap = compilation.getBlockMap(scope.method(), osrBCI);
        BlockBegin startBlock = blockMap.get(0);

        pushRootScope(scope, blockMap, startBlock);

        _initialState = stateAtEntry();
        startBlock.merge(_initialState);

        BlockBegin syncHandler = null;
        CiMethod method = method();
        if (method.isSynchronized()) {
            // setup and exception handler
            syncHandler = new BlockBegin(Instruction.SYNCHRONIZATION_ENTRY_BCI);
            syncHandler.setExceptionEntry();
            syncHandler.setBlockFlag(BlockBegin.BlockFlag.IsOnWorkList);
            syncHandler.setBlockFlag(BlockBegin.BlockFlag.DefaultExceptionHandler);

            CiExceptionHandler desc = newDefaultExceptionHandler(method);
            ExceptionHandler h = new ExceptionHandler(desc);
            h.setEntryBlock(syncHandler);
            _scopeData.addExceptionHandler(h);
            _scopeData.setHasHandler();
        }

        _vmap = new ValueMap();
        scope().computeLockStackSize();
        C1XIntrinsic intrinsic = C1XIntrinsic.getIntrinsic(method);
        if (intrinsic != null) {
            // the root method is an intrinsic; load the parameters onto the stack and try to inline it
            loadParameters(method);
            if (!tryInlineIntrinsic(method)) {
                throw new Bailout("could not inline intrinsic method " + method);
            }
            ValueType rt = returnValueType(method);
            Instruction result = null;
            if (rt.tag() != ValueTag.VOID_TAG) result = pop(rt);
            methodReturn(result);
            BlockEnd end = (BlockEnd) _last;
            _block.setEnd(end);
            end.setState(_state);
        } else {
            _scopeData.addToWorkList(startBlock);
            iterateAllBlocks(false);
        }

        if (syncHandler != null && syncHandler.state() != null) {
            Instruction lock = null;
            if (method.isSynchronized()) {
                lock = null;
                syncHandler.state().unlock();
                syncHandler.state().lock(scope, lock);
            }
            fillSyncHandler(lock, syncHandler, true);
        }

        _start = setupStartBlock(osrBCI, startBlock, _osrEntry, _initialState);
        // eliminate redundant phis
        new PhiSimplifier(_start);

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

    private void loadParameters(CiMethod method) {
        int index = 0;
        if (!method.isStatic()) {
            // add the receiver and assume it is non null
            loadLocal(ValueType.OBJECT_TYPE, 0);
            index++;
        }
        CiSignature sig = method.signatureType();
        int max = sig.arguments();
        for (int i = 0; i < max; i++) {
            BasicType type = sig.argumentBasicType(i);
            ValueType vt = ValueType.fromBasicType(type);
            loadLocal(vt, index);
            index += vt.size();
        }
    }

    public BlockBegin start() {
        return _start;
    }

    void pushRootScope(IRScope scope, BlockMap blockMap, BlockBegin start) {
        _scopeData = new ScopeData(null);
        _scopeData.setScope(scope);
        _scopeData.setBlockMap(blockMap);
        _block = start;
    }

    BlockBegin setupStartBlock(int osrBCI, BlockBegin stdEntry, BlockBegin osrEntry, ValueStack state) {
        BlockBegin start = new BlockBegin(0);

        BlockBegin newHeaderBlock;
        if (stdEntry.predecessors().size() == 0 && !profileBranches()) {
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
        BlockBegin h = new BlockBegin(entry.bci());
        h.setDepthFirstNumber(0);

        Instruction l = h;
        CiMethodData methodData = method().methodData();
        if (profileBranches() && methodData != null) {
            // increment the invocation counter;
            // note that the normal append() won't work, so we do this manually
            Instruction m = new Constant(ConstType.forObject(methodData.dataObject()));
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
        return _scopeData.hasHandler();
    }

    public IRScope scope() {
        return _scopeData.scope();
    }

    public IRScope rootScope() {
        IRScope root = scope();
        while (root.caller() != null) {
            root = root.caller();
        }
        return root;
    }

    public CiMethod method() {
        return scope().method();
    }

    public BytecodeStream stream() {
        return _scopeData.stream();
    }

    public int code() {
        return stream().currentBC();
    }

    public int bci() {
        return stream().currentBCI();
    }

    public int nextBCI() {
        return stream().nextBCI();
    }

    void ipush(Instruction x) {
        _state.ipush(x);
    }

    void lpush(Instruction x) {
        _state.lpush(x);
    }

    void fpush(Instruction x) {
        _state.fpush(x);
    }

    void dpush(Instruction x) {
        _state.dpush(x);
    }

    void apush(Instruction x) {
        _state.apush(x);
    }

    void push(ValueType type, Instruction x) {
        _state.push(type.tag(), x);
    }

    void pushReturn(ValueType type, Instruction x) {
        if (!type.isVoid()) {
            _state.push(type.tag(), x);
        }
    }

    Instruction ipop() {
        return _state.ipop();
    }

    Instruction lpop() {
        return _state.lpop();
    }

    Instruction fpop() {
        return _state.fpop();
    }

    Instruction dpop() {
        return _state.dpop();
    }

    Instruction apop() {
        return _state.apop();
    }

    Instruction pop(ValueType type) {
        return _state.pop(type.tag());
    }

    void loadLocal(ValueType type, int index) {
        push(type, _state.loadLocal(index));
    }

    void storeLocal(ValueType type, int index) {
        storeLocal(_state, pop(type), type, index);
    }

    void storeLocal(ValueStack state, Instruction x, ValueType type, int index) {
        if (_scopeData.parsingJsr()) {
            // We need to do additional tracking of the location of the return
            // address for jsrs since we don't handle arbitrary jsr/ret
            // constructs. Here we are figuring out in which circumstances we
            // need to bail out.
            if (x.type().tag() == ValueTag.JSR_TAG) {
                _scopeData.setJsrEntryReturnAddressLocal(index);

                // Also check parent jsrs (if any) at this time to see whether
                // they are using this local. We don't handle skipping over a
                // ret.
                for (ScopeData cur = _scopeData.parent();
                        cur != null && cur.parsingJsr() && cur.scope() == scope();
                        cur = cur.parent()) {
                    if (cur.jsrEntryReturnAddressLocal() == index) {
                        throw new Bailout("subroutine overwrites return address from previous subroutine");
                    }
                }
            } else {
                if (index == _scopeData.jsrEntryReturnAddressLocal()) {
                    _scopeData.setJsrEntryReturnAddressLocal(-1);
                }
            }
        }

        state.storeLocal(index, roundFp(x));
    }

    Instruction roundFp(Instruction x) {
        if (C1XOptions.RoundFPResults && C1XOptions.SSEVersion < 2) {
            if (x.type().tag() == ValueTag.DOUBLE_TAG
                    && !(x instanceof Constant)
                    && !(x instanceof Local)
                    && !(x instanceof RoundFP)) {
                return append(new RoundFP(x));
            }
        }
        return x;
    }

    void nullCheck(Instruction x) {
        if (x.isNonNull()) {
            // x is already proven to be non-null
            return;
        } else if (x.type().isConstant()) {
            ConstType con = x.type().asConstant();
            if (con.isObject() && con.asObject() != null) {
                // a constant object, and not null
                return;
            }
        }
        append(new NullCheck(x, lockStack()));
    }

    List<ExceptionHandler> handleException(int bci) {
        if (!hasHandler()) {
            return Util.uncheckedCast(Collections.EMPTY_LIST);
        }

        List<ExceptionHandler> exceptionHandlers = new ArrayList<ExceptionHandler>();
        ScopeData curScopeData = _scopeData;
        ValueStack s = _exceptionState;
        int scopeCount = 0;

        assert s != null : "exception handler state must be set";
        do {
            assert curScopeData.scope() == s.scope() : "scopes do not match";
            assert bci == Instruction.SYNCHRONIZATION_ENTRY_BCI || bci == curScopeData.stream().currentBCI() : "invalid bci";

            // join with all potential exception handlers
            for (ExceptionHandler h : curScopeData.exceptionHandlers()) {
                if (h.covers(bci)) {
                    // if the handler covers this bytecode index, add it to the list
                    if (addExceptionHandler(exceptionHandlers, h, curScopeData, s, scopeCount)) {
                        return exceptionHandlers;
                    }
                }

                // set up iteration for next time
                // if parsing a JSR, do not grab exception handlers from the parent
                // scopes for this method (already got them, and they need to be cloned)
                if (curScopeData.parsingJsr()) {
                    IRScope tmp = curScopeData.scope();
                    while (curScopeData.parent() != null && curScopeData.parent().scope() == tmp) {
                        curScopeData = curScopeData.parent();
                    }
                }
                if (curScopeData.parent() != null) {
                    s = s.popScope();
                }
                bci = curScopeData.scope().callerBCI();
                curScopeData = curScopeData.parent();
                scopeCount++;
            }

        } while (curScopeData != null);

        return exceptionHandlers;
    }

    private boolean addExceptionHandler(List<ExceptionHandler> exceptionHandlers, ExceptionHandler h, ScopeData curScopeData, ValueStack s, int scopeCount) {
        _compilation.setHasExceptionHandlers();

        BlockBegin entry = h.entryBlock();
        if (entry == _block) {
            throw new Bailout("Exception handler covers itself");
        }
        assert entry.bci() == h.handlerBCI();
        assert entry.bci() == -1 || entry == curScopeData.blockAt(entry.bci()) : "blocks must correspond";
        assert entry.state() == null || s.locksSize() == entry.state().locksSize() : "locks do not match";

        // exception handler starts with an empty expression stack
        s.truncateStack(curScopeData.callerStackSize());

        if (!entry.tryMerge(s)) {
            throw new Bailout("error while joining with exception handler, probably due to jsr/ret");
        }

        // add current state for correct handling of phi functions
        int phiOperand = entry.addExceptionState(s);

        // add entry to the list of exception handlers of this block
        _block.addExceptionHandler(entry);

        // add back-edge from exception handler entry to this block
        if (!entry.predecessors().contains(_block)) {
            entry.addPredecessor(_block);
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
        CiConstant con = constantPool().lookupConstant(stream().readCPI());
        ValueType type;
        if (con.isCiType()) {
            // this is a load of class constant which might be unresolved
            CiType citype = con.asCiType();
            type = new ClassType(citype);
            if (!citype.isLoaded() || C1XOptions.TestPatching) {
                push(type, append(new Constant(type, _state.copy())));
                return;
            }
        }
        switch (con.basicType()) {
            case Boolean: type = ConstType.forBoolean(con.asBoolean()); break;
            case Char: type = ConstType.forChar(con.asChar()); break;
            case Float: type = ConstType.forFloat(con.asFloat()); break;
            case Double: type = ConstType.forDouble(con.asDouble()); break;
            case Byte: type = ConstType.forByte(con.asByte()); break;
            case Short: type = ConstType.forShort(con.asShort()); break;
            case Int: type = ConstType.forInt(con.asInt()); break;
            case Long: type = ConstType.forLong(con.asLong()); break;
            case Object: type = ConstType.forObject(con.asObject()); break;
            case Array: type = ConstType.forObject(con.asObject()); break;
            default:
                throw new Bailout("could not resolve constant");
        }
        push(type, append(new Constant(type.asConstant())));
    }

    void loadIndexed(BasicType type) {
        Instruction index = ipop();
        Instruction array = apop();
        Instruction length = null;
        if (cseArrayLength(array)) {
            length = append(new ArrayLength(array, lockStack()));
        }
        push(ValueType.fromBasicType(type), append(new LoadIndexed(array, index, length, type, lockStack())));
    }

    void storeIndexed(BasicType type) {
        Instruction value = pop(ValueType.fromBasicType(type));
        Instruction index = ipop();
        Instruction array = apop();
        Instruction length = null;
        if (cseArrayLength(array)) {
            length = append(new ArrayLength(array, lockStack()));
        }
        StoreIndexed result = new StoreIndexed(array, index, length, type, value, lockStack());
        append(result);
        _memory.storeValue(value);
    }

    void stackOp(int opcode) {
        switch (opcode) {
            case Bytecodes.POP: {
                _state.xpop();
                break;
            }
            case Bytecodes.POP2: {
                _state.xpop();
                _state.xpop();
                break;
            }
            case Bytecodes.DUP: {
                Instruction w = _state.xpop();
                _state.xpush(w);
                _state.xpush(w);
                break;
            }
            case Bytecodes.DUP_X1: {
                Instruction w1 = _state.xpop();
                Instruction w2 = _state.xpop();
                _state.xpush(w1);
                _state.xpush(w2);
                _state.xpush(w1);
                break;
            }
            case Bytecodes.DUP_X2: {
                Instruction w1 = _state.xpop();
                Instruction w2 = _state.xpop();
                Instruction w3 = _state.xpop();
                _state.xpush(w1);
                _state.xpush(w3);
                _state.xpush(w2);
                _state.xpush(w1);
                break;
            }
            case Bytecodes.DUP2: {
                Instruction w1 = _state.xpop();
                Instruction w2 = _state.xpop();
                _state.xpush(w2);
                _state.xpush(w1);
                _state.xpush(w2);
                _state.xpush(w1);
                break;
            }
            case Bytecodes.DUP2_X1: {
                Instruction w1 = _state.xpop();
                Instruction w2 = _state.xpop();
                Instruction w3 = _state.xpop();
                _state.xpush(w2);
                _state.xpush(w1);
                _state.xpush(w3);
                _state.xpush(w2);
                _state.xpush(w1);
                break;
            }
            case Bytecodes.DUP2_X2: {
                Instruction w1 = _state.xpop();
                Instruction w2 = _state.xpop();
                Instruction w3 = _state.xpop();
                Instruction w4 = _state.xpop();
                _state.xpush(w2);
                _state.xpush(w1);
                _state.xpush(w4);
                _state.xpush(w3);
                _state.xpush(w2);
                _state.xpush(w1);
                break;
            }
            case Bytecodes.SWAP: {
                Instruction w1 = _state.xpop();
                Instruction w2 = _state.xpop();
                _state.xpush(w1);
                _state.xpush(w2);
                break;
            }
            default:
                throw Util.shouldNotReachHere();
        }

    }

    void arithmeticOp(ValueType type, int opcode) {
        arithmeticOp(type, opcode, null);
    }

    void arithmeticOp(ValueType type, int opcode, ValueStack stack) {
        Instruction y = pop(type);
        Instruction x = pop(type);
        Instruction result = append(new ArithmeticOp(opcode, x, y, method().isStrictFP(), stack));
        if (scope().method().isStrictFP()) {
            result = roundFp(result);
        }
        push(type, result);
    }

    void negateOp(ValueType type) {
        push(type, append(new NegateOp(pop(type))));
    }

    void shiftOp(ValueType type, int opcode) {
        Instruction s = ipop();
        Instruction x = pop(type);
        // note that strength reduction of e << K >>> K is correctly handled in canonicalizer now
        push(type, append(new ShiftOp(opcode, x, s)));
    }

    void logicOp(ValueType type, int opcode) {
        Instruction y = pop(type);
        Instruction x = pop(type);
        push(type, append(new LogicOp(opcode, x, y)));
    }

    void compareOp(ValueType type, int opcode) {
        ValueStack stateBefore = _state.copy();
        Instruction y = pop(type);
        Instruction x = pop(type);
        ipush(append(new CompareOp(opcode, x, y, stateBefore)));
    }

    void convert(int opcode, BasicType from, BasicType to) {
        ValueType ft = ValueType.fromBasicType(from);
        ValueType tt = ValueType.fromBasicType(to);
        push(tt, append(new Convert(opcode, pop(ft), tt)));
    }

    void increment() {
        int index = stream().readLocalIndex();
        int delta = stream().readIncrement();
        Instruction x = _state.localAt(index);
        Instruction y = append(new Constant(ConstType.forInt(delta)));
        _state.storeLocal(index, append(new ArithmeticOp(Bytecodes.IADD, x, y, method().isStrictFP(), null)));
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
        If ifnode = (If) append(new If(x, cond, false, y, tsucc, fsucc, isBackwards ? stateBefore : null, isBackwards));
        if (profileBranches() && ifnode != null) {
            ifnode.setProfile(method(), bci);
        }
    }

    void ifZero(ValueType type, Condition cond) {
        Instruction y = append(new Constant(ConstType.INT_0));
        ValueStack stateBefore = _state.copy();
        Instruction x = ipop();
        ifNode(x, cond, y, stateBefore);
    }

    void ifNull(ValueType type, Condition cond) {
        Instruction y = append(new Constant(ConstType.NULL_OBJECT));
        ValueStack stateBefore = _state.copy();
        Instruction x = apop();
        ifNode(x, cond, y, stateBefore);
    }

    void ifSame(ValueType type, Condition cond) {
        ValueStack stateBefore = _state.copy();
        Instruction y = pop(type);
        Instruction x = pop(type);
        ifNode(x, cond, y, stateBefore);
    }

    void throw_(int bci) {
        ValueStack stateBefore = _state.copy();
        Throw t = new Throw(apop(), stateBefore);
        appendWithBCI(t, bci, false); // don't bother trying to canonicalize throws
    }

    void checkcast_() {
        CiType type = constantPool().lookupType(stream().readCPI());
        ValueStack stateBefore = valueStackIfClassNotLoaded(type);
        CheckCast c = new CheckCast(type, apop(), stateBefore);
        apush(appendSplit(c));
        if (assumeLeafClass(type)) {
            c.setDirectCompare();
        }
        if (profileCheckcasts()) {
            c.setProfile(method(), bci());
        }
    }

    void instanceof_() {
        CiType type = constantPool().lookupType(stream().readCPI());
        ValueStack stateBefore = valueStackIfClassNotLoaded(type);
        InstanceOf i = new InstanceOf(type, apop(), stateBefore);
        ipush(appendSplit(i));
        if (assumeLeafClass(type)) {
            i.setDirectCompare();
        }
    }

    void newInstance() {
        CiType type = constantPool().lookupType(stream().readCPI());
        assert !type.isLoaded() || type.isInstanceClass();
        NewInstance n = new NewInstance(type);
        _memory.newInstance(n);
        apush(appendSplit(n));
    }

    void newTypeArray() {
        apush(appendSplit(new NewTypeArray(ipop(), BasicType.fromArrayTypeCode(stream().readLocalIndex()))));
    }

    void newObjectArray() {
        CiType type = constantPool().lookupType(stream().readCPI());
        ValueStack stateBefore = valueStackIfClassNotLoaded(type);
        NewArray n = new NewObjectArray(type, ipop(), stateBefore);
        apush(appendSplit(n));
    }

    void newMultiArray() {
        CiType type = constantPool().lookupType(stream().readCPI());
        ValueStack stateBefore = valueStackIfClassNotLoaded(type);
        int rank = stream().readUByte(stream().currentBCI() + 3);
        Instruction[] dims = new Instruction[rank];
        for (int i = rank - 1; i >= 0; i--) {
            dims[i] = ipop();
        }
        NewArray n = new NewMultiArray(type, dims, stateBefore);
        apush(appendSplit(n));
    }

    void accessField(int opcode) {
        CiField field = constantPool().lookupField(opcode, stream().readCPI());
        CiType holder = field.holder();
        boolean isLoaded = holder.isLoaded() && field.willLink(method().holder(), opcode);
        boolean isInitialized = isLoaded && holder.isInitialized();

        ValueStack stateCopy = null;
        if (!isInitialized || C1XOptions.TestPatching) {
            // save state before the instruction for debugging information when patching
            stateCopy = _state.copy();
        }

        ValueType type = ValueType.fromBasicType(field.basicType());
        int offset = isLoaded ? field.offset() : -1;

        switch (opcode) {
            case Bytecodes.GETSTATIC: {
                // note that folding of constant static fields happens in canonicalizer now
                Constant holderConstant = new Constant(new ClassType(holder), stateCopy);
                loadField(type, new LoadField(append(holderConstant), offset, field, true, lockStack(), stateCopy, isLoaded, isInitialized));
                break;
            }
            case Bytecodes.PUTSTATIC: {
                Instruction value = pop(type);
                Constant holderConstant = new Constant(new ClassType(holder), stateCopy);
                storeField(new StoreField(append(holderConstant), offset, field, value, true, lockStack(), stateCopy, isLoaded, isInitialized));
                break;
            }
            case Bytecodes.GETFIELD: {
                LoadField load = new LoadField(apop(), offset, field, false, lockStack(), stateCopy, isLoaded, true);
                loadField(type, load);
                break;
            }
            case Bytecodes.PUTFIELD: {
                Instruction value = pop(type);
                storeField(new StoreField(apop(), offset, field, value, false, lockStack(), stateCopy, isLoaded, true));
                break;
            }
            default:
                throw Util.shouldNotReachHere();
        }
    }

    private void storeField(StoreField store) {
        if (C1XOptions.EliminateFieldAccess) store = _memory.store(store);
        if (store != null) {
            // the memory buffer did not find the store to be redundant
            append(store);
        }
    }

    private void loadField(ValueType type, LoadField load) {
        Instruction replacement = C1XOptions.EliminateFieldAccess ? _memory.load(load) : load;
        if (replacement != load) {
            // the memory buffer found a replacement for this load
            assert replacement.isAppended() || replacement instanceof Phi || replacement instanceof Local;
            push(type, replacement);
        } else {
            push(type, append(load));
        }
    }

    void invokeStatic(CiMethod target) {
        checkForLinkError(target, Bytecodes.INVOKESTATIC);
        if (!tryInline(target, null)) {
            profileInvocation(target);
            ValueType resultType = returnValueType(target);
            Instruction[] args = popArguments(target);
            appendInvoke(Bytecodes.INVOKESTATIC, resultType, null, args, target);
        }
    }

    void invokeInterface(CiMethod target) {
        checkForLinkError(target, Bytecodes.INVOKEINTERFACE);
        ValueType resultType = returnValueType(target);
        Instruction[] args = popArguments(target);
        Instruction receiver = apop();
        profileCall(receiver, null);
        appendInvoke(Bytecodes.INVOKEINTERFACE, resultType, receiver, args, target);
    }

    void invokeVirtual(CiMethod target) {
        checkForLinkError(target, Bytecodes.INVOKEVIRTUAL);
        Instruction receiver = getReceiver(target);
        // attempt to devirtualize the call
        if (target.isLoaded() && target.holder().isLoaded()) {
            CiType klass = target.holder();
            // 0. check for trivial cases
            if (target.canBeStaticallyBound() && !target.isAbstract()) {
                // check for trivial cases (e.g. final methods, nonvirtual methods)
                invokeSpecial(target, target.holder());
                return;
            }
            // 1. check if the exact type of the receiver can be determined
            CiType exact = getExactType(klass, receiver);
            if (exact != null && exact.isLoaded()) {
                // either the holder class is exact, or the receiver object has an exact type
                invokeSpecial(exact.resolveMethodImpl(target), exact);
                return;
            }
            // 2. check if an assumed leaf method can be found
            CiMethod leaf = getAssumedLeafMethod(target, receiver);
            if (leaf != null && leaf.isLoaded() && !leaf.isAbstract() && leaf.holder().isLoaded()) {
                invokeSpecial(leaf, null);
                return;
            }
            // 3. check if the either of the holder or declared type of receiver can be assumed to be a leaf
            exact = getAssumedLeafType(klass, receiver);
            if (exact != null && exact.isLoaded()) {
                // either the holder class is exact, or the receiver object has an exact type
                invokeSpecial(exact.resolveMethodImpl(target), exact);
                return;
            }
        }
        // devirtualization failed, compile a bonafide invokevirtual
        ValueType resultType = returnValueType(target);
        Instruction[] args = popArguments(target);
        receiver = apop();
        profileCall(receiver, null);
        appendInvoke(Bytecodes.INVOKEVIRTUAL, resultType, receiver, args, target);
    }

    private ValueType returnValueType(CiMethod target) {
        return ValueType.fromBasicType(target.signatureType().returnBasicType());
    }

    void invokeSpecial(CiMethod target, CiType knownHolder) {
        checkForLinkError(target, Bytecodes.INVOKESPECIAL);
        if (!tryInline(target, knownHolder)) {
            profileInvocation(target);
            ValueType resultType = returnValueType(target);
            Instruction[] args = popArguments(target);
            Instruction receiver = apop();
            profileCall(receiver, target.holder());
            appendInvoke(Bytecodes.INVOKESPECIAL, resultType, receiver, args, target);
        }
    }


    private void appendInvoke(int opcode, ValueType resultType, Instruction receiver, Instruction[] args, CiMethod target) {
        int vtableIndex = target.vtableIndex();
        Invoke invoke = new Invoke(opcode, resultType, receiver, args, vtableIndex, target);
        appendSplit(invoke);
        if (method().isStrictFP()) {
            pushReturn(resultType, roundFp(invoke));
        } else {
            pushReturn(resultType, invoke);
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
        if (declared != null) {
            CiMethod impl = declared.resolveMethodImpl(target);
            if (impl != null && assumeLeafClass(declared)) {
                return impl;
            }
        }
        return null;
    }

    void checkForLinkError(CiMethod target, int opcode) {
        if (target.isLoaded() && target.isStatic() != (opcode == Bytecodes.INVOKESTATIC)) {
            throw new Bailout("will cause link error");
        }
    }

    Instruction getReceiver(CiMethod target) {
        return _state.stackAt(_state.stackSize() - target.signatureType().argumentSize(false) - 1);
    }

    Instruction[] popArguments(CiMethod target) {
        return _state.popArguments(target.signatureType().argumentSize(false));
    }

    void callRegisterFinalizer() {
        Instruction receiver = _state.loadLocal(0);
        CiType declaredType = receiver.declaredType();
        CiType receiverType = declaredType;
        CiType exactType = receiver.exactType();
        if (exactType == null && declaredType != null) {
            exactType = declaredType.exactType();
        }
        if (exactType == null && receiver instanceof Local && ((Local) receiver).javaIndex() == 0) {
            // the exact type isn't known, but the receiver is parameter 0 => use holder
            receiverType = _compilation.method().holder();
            exactType = receiverType.exactType();
        }
        boolean needsCheck = true;
        if (exactType != null) {
            // we have an exact type
            needsCheck = exactType.hasFinalizer();
        } else {
            // if either the declared type of receiver or the holder can be assumed to have no finalizers
            if (declaredType != null && declaredType.hasFinalizableSubclass()) {
                if (_compilation.recordNoFinalizableSubclassAssumption(declaredType)) {
                    needsCheck = false;
                }
            }

            if (receiverType != null && receiverType.hasFinalizableSubclass()) {
                if (_compilation.recordNoFinalizableSubclassAssumption(receiverType)) {
                    needsCheck = false;
                }
            }
        }

        if (needsCheck) {
            // append a call to the registration intrinsic
            loadLocal(ValueType.OBJECT_TYPE, 0);
            appendSplit(new Intrinsic(ValueType.VOID_TYPE, C1XIntrinsic.java_lang_Object$init,
                                      _state.popArguments(1), true, lockStack(), true, true));
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
        if (_scopeData.continuation() != null) {
            if (method().isSynchronized()) {
                // if the inlined method is synchronized, then the monitor
                // must be released before jumping to the continuation point
                assert C1XOptions.InlineSynchronizedMethods;
                int i = _state.scope().callerState().locksSize();
                assert _state.locksSize() == i + 1;
                monitorexit(_state.lockAt(i), Instruction.SYNCHRONIZATION_ENTRY_BCI);
            }

            // trim back stack to the caller's stack size
            _state.truncateStack(_scopeData.callerStackSize());
            if (x != null) {
                _state.push(x.type().tag(), x);
            }
            Goto gotoCallee = new Goto(_scopeData.continuation(), null, false);

            // if this is the first return, store some of the state for a later return
            if (_scopeData.numberOfReturns() == 0) {
                _scopeData.setInlineCleanupInfo(_block, _last, _state);
            }

            // State at end of inlined method is the state of the caller
            // without the method parameters on stack, including the
            // return value, if any, of the inlined method on operand stack.
            _state = _scopeData.continuationState().copy();
            if (x != null) {
                _state.push(x.type().tag(), x);
            }

            // The current bci() is in the wrong scope, so use the bci() of
            // the continuation point.
            appendWithBCI(gotoCallee, _scopeData.continuation().bci(), false);
            _scopeData.incrementNumberOfReturns();
            return;
        }

        _state.truncateStack(0);
        if (method().isSynchronized()) {
            // unlock before exiting the method
            Instruction receiver;
            if (!method().isStatic()) {
                receiver = _initialState.localAt(0);
            } else {
                receiver = append(new Constant(new ClassType(method().holder()), null));
            }
            appendSplit(new MonitorExit(receiver, _state.unlock()));
        }
        append(new Return(x));
    }

    private ValueStack valueStackIfClassNotLoaded(CiType type) {
        return !type.isLoaded() || C1XOptions.TestPatching ? _state.copy() : null;
    }

    void monitorenter(Instruction x, int bci) {
        ValueStack lockStack = lockStack();
        appendWithBCI(new MonitorEnter(x, _state.lock(scope(), x), lockStack), bci, false);
        killAll(); // prevent any optimizations across synchronization
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
        if (_state.locksSize() < 1) throw new Bailout("monitor stack underflow");
        appendWithBCI(new MonitorExit(x, _state.unlock()), bci, false);
        killAll(); // prevent any optimizations across synchronization
    }

    void jsr(int dest) {
        for (ScopeData cur = _scopeData; cur != null && cur.parsingJsr() && cur.scope() == scope(); cur = cur.parent()) {
            if (cur.jsrEntryBCI() == dest) {
                // the jsr/ret pattern includes a recursive invocation
                throw new Bailout("jsr/ret structure is too complicated");
            }
        }
        push(ValueType.ADDRESS_TYPE, append(new Constant(ConstType.forAddress(nextBCI()))));
        tryInlineJsr(dest);
    }

    void ret(int localIndex) {
        if (!_scopeData.parsingJsr()) {
            throw new Bailout("ret encountered when not parsing subroutine");
        }

        if (localIndex != _scopeData.jsrEntryReturnAddressLocal()) {
            throw new Bailout("jsr/ret structure is too complicated");
        }
        // rets become non-safepoint gotos
        append(new Goto(_scopeData.jsrContinuation(), null, false));
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
        isBackwards |= offset < 0; // track if any of the successors are backwards
        list.add(blockAt(bci + offset));
        ValueStack stateBefore = isBackwards ? _state.copy() : null;
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
        isBackwards |= offset < 0; // track if any of the successors are backwards
        list.add(blockAt(bci + offset));
        ValueStack stateBefore = isBackwards ? _state.copy() : null;
        append(new LookupSwitch(ipop(), list, keys, stateBefore, isBackwards));
    }

    private boolean cseArrayLength(Instruction array) {
        return C1XOptions.CSEArrayLength ||
            (array instanceof AccessField && ((AccessField) array).field().isConstant()) ||
            (array instanceof NewArray && ((NewArray) array).length().type().isConstant());
    }

    private void profileCall(Instruction receiver, CiType knownHolder) {
        if (profileCalls()) {
            append(new ProfileCall(method(), bci(), receiver, knownHolder));
        }
    }

    private void profileInvocation(CiMethod callee) {
        if (profileCalls()) {
            CiMethodData mdo = callee.methodData();
            if (mdo != null) {
                int offset = mdo.invocationCountOffset();
                if (offset >= 0) {
                    // if the method data object exists and it has an entry for the invocation count
                    Instruction m = appendConstant(ConstType.forObject(mdo.dataObject()));
                    append(new ProfileCounter(m, offset, 1));
                }
            }
        }
    }

    private void profileBCI(int bci) {
        if (profileBranches()) {
            CiMethodData mdo = method().methodData();
            if (mdo != null) {
                int offset = mdo.bciCountOffset(bci);
                if (offset >= 0) {
                    // if the method data object exists and it has an entry for the bytecode index
                    Instruction m = appendConstant(ConstType.forObject(mdo.dataObject()));
                    append(new ProfileCounter(m, offset, 1));
                }
            }
        }
    }

    private boolean profileCalls() {
        return C1XOptions.ProfileCalls;
    }

    private boolean profileInlinedCalls() {
        return C1XOptions.ProfileInlinedCalls;
    }

    private boolean profileCheckcasts() {
        return C1XOptions.ProfileCheckcasts;
    }

    private boolean profileBranches() {
        return C1XOptions.ProfileBranches;
    }

    private Instruction appendConstant(ConstType type) {
        return appendWithBCI(new Constant(type), bci(), false); // don't bother trying to canonicalize a constant
    }

    private Instruction append(Instruction x) {
        assert !(x instanceof StateSplit) || (x instanceof BlockEnd);
        return appendWithBCI(x, bci(), C1XOptions.CanonicalizeInstructions);
    }

    private Instruction appendSplit(StateSplit x) {
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
        } else if (C1XOptions.UseLocalValueNumbering) {
            // look in the local value map
            Instruction r = _vmap.findInsert(x);
            if (r != x) {
                assert r.isAppended() : "lvn result should already be linked";
                return r;
            }
            // process the effects of adding this instruction
            _vmap.processEffects(x);
        }

        if (!(x instanceof Phi) && !(x instanceof Local)) {
            // add instructions to the basic block (if not a phi or a local)
            assert x.next() == null : "instruction should not have been added yet";
            _last = _last.setNext(x, bci);
            if (++_instructionCount >= C1XOptions.MaximumInstructionCount) {
                // bailout if we've exceeded the maximum inlining size
                throw new Bailout("Method and/or inlining is too large");
            }
        }

        assert _last == x;
        if (x instanceof StateSplit) {
            if (x instanceof Invoke || (x instanceof Intrinsic && !((Intrinsic) x).preservesState())) {
                // conservatively kill all memory across calls
                _memory.kill();
            }
            // split the state for any state split operations
            ((StateSplit) x).setState(_state.copy());
        }

        if (x.canTrap()) {
            // connect the instruction to any exception handlers
            assert _exceptionState != null || !hasHandler() : "must have setup exception state";
            x.setExceptionHandlers(handleException(bci));
        }
        return x;
    }

    ValueStack lockStack() {
        return _state.copyLocks();
    }

    private BlockBegin blockAt(int bci) {
        return _scopeData.blockAt(bci);
    }

    boolean tryInlineJsr(int jsrStart) {
        // start a new continuation point.
        // all ret instructions will be replaced with gotos to this point
        BlockBegin cont = blockAt(nextBCI());
        assert cont != null : "continuation must exist";

        // push callee scope
        pushScopeForJsr(cont, jsrStart);

        // temporarily set up bytecode stream so we can append instructions
        // (only using the bci of this stream)
        _scopeData.setStream(_scopeData.parent().stream());

        BlockBegin jsrStartBlock = blockAt(jsrStart);
        assert jsrStartBlock != null;
        assert !jsrStartBlock.wasVisited();
        Goto gotoSub = new Goto(jsrStartBlock, null, false);
        gotoSub.setState(_state);
        assert jsrStartBlock.state() == null;
        jsrStartBlock.setState(_state.copy());
        append(gotoSub);
        _block.setEnd(gotoSub);
        _last = _block = jsrStartBlock;

        // clear the bytecode stream (?)
        _scopeData.setStream(null);

        _scopeData.addToWorkList(jsrStartBlock);

        iterateAllBlocks(false);

        if (cont.state() != null) {
            if (!cont.wasVisited()) {
                _scopeData.parent().addToWorkList(cont);
            }
        }

        BlockBegin jsrCont = _scopeData.jsrContinuation();
        assert jsrCont == cont && (!jsrCont.wasVisited() || jsrCont.isLoopHeader());
        assert _last != null && _last instanceof BlockEnd;

        // continuation is in work list, so end iteration of current block
        _skipBlock = true;
        popScopeForJsr();
        return true;
    }

    void pushScopeForJsr(BlockBegin jsrCont, int jsrStart) {
        ScopeData data = new ScopeData(_scopeData);
        data.setParsingJsr();
        data.setJsrEntryBCI(jsrStart);
        data.setJsrEntryReturnAddressLocal(-1);
        // the block map must be cloned because it will be mutated while inlining JSR blocks
        data.setBlockMap(_scopeData._blockMap);
        data.setScope(scope());
        data.setupJsrExceptionHandlers();
        data.setContinuation(jsrCont);
        if (_scopeData.continuation() != null) {
            assert _scopeData.continuationState() != null;
            data.setContinuationState(_scopeData.continuationState().copy());
        }
        data.setJsrContinuation(jsrCont);
        _scopeData = data;
    }

    void pushScope(CiMethod target, BlockBegin continuation) {
        IRScope calleeScope = new IRScope(_compilation, scope(), bci(), target, -1);
        scope().addCallee(calleeScope);
        BlockMap blockMap = _compilation.getBlockMap(calleeScope.method(), -1);

        calleeScope.setCallerState(_state);
        calleeScope.setStoresInLoops(blockMap.getStoresInLoops());
        _state = _state.pushScope(calleeScope);
        ScopeData data = new ScopeData(_scopeData);
        data.setScope(calleeScope);
        data.setBlockMap(blockMap);
        data.setContinuation(continuation);
        _scopeData = data;
    }

    ValueStack stateAtEntry() {
        CiMethod method = method();
        ValueStack state = new ValueStack(scope(), method.maxLocals(), method.maxStackSize());
        int index = 0;
        if (!method.isStatic()) {
            // add the receiver and assume it is non null
            Local local = new Local(ValueType.OBJECT_TYPE, index);
            local.setFlag(Instruction.Flag.NonNull, true);
            local.setDeclaredType(method.holder());
            state.storeLocal(index, local);
            index = 1;
        }
        CiSignature sig = method.signatureType();
        int max = sig.arguments();
        for (int i = 0; i < max; i++) {
            CiType type = sig.argumentType(i);
            ValueType vt = ValueType.fromBasicType(type.basicType());
            Local local = new Local(vt, index);
            if (type.isLoaded()) local.setDeclaredType(type);
            state.storeLocal(index, local);
            index += vt.size();
        }

        if (method.isSynchronized()) {
            state.lock(scope(), null); // XXX: why do we lock null?
        }
        return state;
    }

    boolean tryInline(CiMethod target, CiType knownHolder) {
        return tryInlineIntrinsic(target) || checkInliningConditions(target) && tryInlineFull(target, knownHolder);
    }

    boolean checkInliningConditions(CiMethod target) {
        if (scope().level() > C1XOptions.MaximumInlineLevel) {
            return cannotInline(target, "inlining too deep");
        }
        if (!target.isLoaded()) {
            return cannotInline(target, "method is not resolved");
        }
        if (recursiveInlineLevel(target) > C1XOptions.MaximumRecursiveInlineLevel) {
            return cannotInline(target, "recursive inlining too depth");
        }
        if (target.codeSize() > _scopeData.maxInlineSize()) {
            return cannotInline(target, "> " + _scopeData.maxInlineSize() + " bytecodes");
        }
        // TODO: check the total number of bytecodes is less than desired method limit
        if (!target.holder().isInitialized()) {
            return cannotInline(target, "holder is not initialized");
        }
        if (_compilation.runtime().mustNotInline(target)) {
            return cannotInline(target, "inlining excluded by runtime");
        }
        if (_compilation.runtime().mustNotCompile(target)) {
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
        if ("<init>".equals(target.name()) && target.holder().isSubtypeOf(_compilation.throwableType())) {
            // don't inline constructors of throwable classes unless the inlining tree is
            // rooted in a throwable class
            if (!rootScope().method().holder().isSubtypeOf(_compilation.throwableType())) {
                return cannotInline(target, "don't inline Throwable constructors");
            }
        }
        return true;
    }

    boolean cannotInline(CiMethod target, String reason) {
        _compilation.recordInliningFailure(target, reason);
        return false;
    }

    boolean tryInlineFull(CiMethod target, CiType knownHolder) {
        BlockBegin orig = _block;
        int argsBase = _state.stackSize() - target.signatureType().argumentSize(!target.isStatic());
        Instruction receiver = null;
        if (!target.isStatic()) {
            // the receiver object must be nullchecked for instance methods
            receiver = _state.stackAt(argsBase);
            nullCheck(receiver);
        }

        if (profileInlinedCalls()) {
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
            continuationBlock = new BlockBegin(nextBCI());
            continuationBlock.setDepthFirstNumber(0);
            continuationExisted = false;
        }
        // record the number of predecessors before inlining, to determine
        // whether the the inlined method has added edges to the continuation
        int continuationPredecessors = continuationBlock.predecessors().size();

        // push the target scope
        pushScope(target, continuationBlock);

        // temporarily set up the bytecode stream so we can append instructions
        // (using only the bci of the stream)
        _scopeData.setStream(_scopeData.parent().stream());

        // pass parameters into the callee state
        ValueStack calleeState = _state;
        ValueStack callerState = scope().callerState();
        for (int i = argsBase; i < callerState.stackSize(); i++) {
            int param = i - argsBase;
            Instruction arg = callerState.stackAt(i);
            storeLocal(calleeState, arg, arg.type().base(), param);
        }

        // remove arguments from the stack
        callerState.truncateStack(argsBase);

        // setup state that is used at returns from the inlined method.
        // this is essentially the state of the continuation block,
        // but without the return value on the stack.
        _scopeData.setContinuationState(callerState.copy());

        // compute the lock stack size for callee scope
        scope().computeLockStackSize();

        Instruction lock = null;
        BlockBegin syncHandler = null;
        // inline the locking code if the target method is synchronized
        if (target.isSynchronized()) {
            // lock the receiver object if it is an instance method, the class object otherwise
            lock = target.isStatic() ? append(new Constant(ConstType.forObject(target.holder().javaClass()))) : _state.localAt(0);
            syncHandler = new BlockBegin(Instruction.SYNCHRONIZATION_ENTRY_BCI);
            inlineSyncEntry(lock, syncHandler);
            scope().computeLockStackSize();
        }

        BlockBegin calleeStartBlock = blockAt(0);
        if (calleeStartBlock.isLoopHeader()) {
            // the block is a loop header, so we have to insert a goto
            Goto gotoCallee = new Goto(calleeStartBlock, null, false);
            gotoCallee.setState(_state);
            appendWithBCI(gotoCallee, 0, false);
            _block.setEnd(gotoCallee);
            calleeStartBlock.merge(calleeState);
            _last = _block = calleeStartBlock;
            _scopeData.addToWorkList(calleeStartBlock);
        }

        // clear out the bytecode stream
        _scopeData.setStream(null);

        // ready to resume parsing in inlined method
        // (either in the current block or the callee's start)
        iterateAllBlocks(!calleeStartBlock.isLoopHeader());

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
        if (_scopeData.numberOfReturns() == 1 && _block == orig && _block == _scopeData.inlineCleanupBlock()) {
            _last = _scopeData.inlineCleanupReturnPrev();
            _state = _scopeData.inlineCleanupState().popScope();
        } else if (continuationPredecessors == continuationBlock.predecessors().size()) {
            // Inlining caused that the instructions after the invoke in the
            // caller are not reachable any more. So skip filling this block
            // with instructions!
            assert continuationBlock == _scopeData.continuation();
            assert _last instanceof BlockEnd;
            _skipBlock = true;
        } else {
            // Resume parsing in continuation block unless it was already parsed.
            // Note that if we don't change _last here, iteration in
            // iterateBytecodesForBlock will stop when we return.
            if (_scopeData.continuation().wasVisited()) {
                // add continuation to work list instead of parsing it immediately
                assert _last instanceof BlockEnd;
                _scopeData.parent().addToWorkList(_scopeData.continuation());
                _skipBlock = true;
            }
        }

        // fill the exception handler for synchronized methods with instructions
        if (target.isSynchronized()) {
            fillSyncHandler(lock, syncHandler, true);
        } else {
            popScope();
        }

        _compilation.recordInlining(target);
        return true;
    }

    void inlineSyncEntry(Instruction lock, BlockBegin syncHandler) {
        _exceptionState = _state.copy();
        monitorenter(lock, Instruction.SYNCHRONIZATION_ENTRY_BCI);
        _last.setFlag(Instruction.Flag.NonNull, true);
        syncHandler.setExceptionEntry();
        syncHandler.setBlockFlag(BlockBegin.BlockFlag.IsOnWorkList);
        CiExceptionHandler handler = newDefaultExceptionHandler(method());
        ExceptionHandler h = new ExceptionHandler(handler);
        h.setEntryBlock(syncHandler);
        _scopeData.addExceptionHandler(h);
        _scopeData.setHasHandler();
    }

    void fillSyncHandler(Instruction lock, BlockBegin syncHandler, boolean defaultHandler) {
        BlockBegin origBlock = _block;
        ValueStack origState = _state;
        Instruction origLast = _last;

        _last = _block = syncHandler;
        _state = syncHandler.state().copy();

        assert !syncHandler.wasVisited() : "synch handler already visited";

        _block.setWasVisited(true);
        Instruction exception = appendWithBCI(new ExceptionObject(), Instruction.SYNCHRONIZATION_ENTRY_BCI, false);

        int bci = Instruction.SYNCHRONIZATION_ENTRY_BCI;
        if (lock != null) {
            assert _state.locksSize() > 0 && _state.lockAt(_state.locksSize() - 1) == lock;
            if (!lock.isAppended()) {
                lock = appendWithBCI(lock, Instruction.SYNCHRONIZATION_ENTRY_BCI, false);
            }
            // exit the monitor
            monitorexit(lock, Instruction.SYNCHRONIZATION_ENTRY_BCI);

            // exit the context of the synchronized method
            if (!defaultHandler) {
                popScope();
                _state = _state.copy();
                bci = _state.scope().callerBCI();
                _state = _state.popScope().copy();
            }
        }

        apush(exception);
        _exceptionState = _state.copy();
        throw_(bci);
        BlockEnd end = (BlockEnd) _last;
        _block.setEnd(end);
        end.setState(_state);

        _block = origBlock;
        _state = origState;
        _last = origLast;
    }

    boolean tryInlineIntrinsic(CiMethod target) {
        C1XIntrinsic intrinsic = C1XIntrinsic.getIntrinsic(target);
        if (intrinsic == null) {
            // this method is not an intrinsic
            return false;
        }
        boolean preservesState = true;
        boolean canTrap = false;

        // handle intrinsics differently
        switch (intrinsic) {
            // java.lang.Object
            case java_lang_Object$init:     // fall through
            case java_lang_Object$clone:    return false; // NOPE
        }

        // get the arguments for the intrinsic
        boolean hasReceiver = !target.isStatic();
        ValueType resultType = returnValueType(target);
        Instruction[] args = _state.popArguments(target.signatureType().argumentSize(hasReceiver));

        // create the intrinsic node
        Intrinsic result = new Intrinsic(resultType, intrinsic, args, hasReceiver, lockStack(), preservesState, canTrap);
        Instruction value = appendSplit(result);
        pushReturn(resultType, value);
        return true;
    }

    void iterateAllBlocks(boolean startInCurrentBlockForInlining) {
        do {
            if (startInCurrentBlockForInlining) {
                iterateBytecodesForBlock(0);
                startInCurrentBlockForInlining = false;
            } else {
                BlockBegin b;
                while ((b = _scopeData.removeFromWorkList()) != null) {
                    if (!b.wasVisited()) {
                        if (b.isOsrEntry()) {
                            // this is the OSR entry block, set up edges accordingly
                            setupOsrEntryBlock();
                            // this is no longer the OSR entry block
                            b.setOsrEntry(false);
                        }
                        b.setWasVisited(true);
                        connectToEnd(b);
                    }
                }
            }

        } while (!_scopeData.isWorkListEmpty());
    }

    void connectToEnd(BlockBegin b) {
        killAll();
        _block = b;
        _state = b.state().copy();
        _last = b;
        iterateBytecodesForBlock(b.bci());
    }

    void popScope() {
        int numberOfLocks = scope().numberOfLocks();
        _scopeData = _scopeData.parent();
        scope().setMinimumNumberOfLocks(numberOfLocks);
    }

    void popScopeForJsr() {
        _scopeData = _scopeData.parent();
    }

    void setupOsrEntryBlock() {
        assert _compilation.isOsrCompilation();

        int osrBCI = _compilation.osrBCI();
        BytecodeStream s = new BytecodeStream(method().code());
        CiOsrFrame frame = _compilation.getOsrFrame();
        s.setBCI(osrBCI);
        s.next(); // XXX: why go to next bytecode?
        _scopeData.setStream(s);

        // create a new block to contain the OSR setup code
        _osrEntry = new BlockBegin(osrBCI);
        _osrEntry.setOsrEntry(true);
        _osrEntry.setDepthFirstNumber(0);

        // get the target block of the OSR
        BlockBegin target = _scopeData.blockAt(osrBCI);
        assert target != null && target.isOsrEntry();

        ValueStack state = target.state().copy();
        _osrEntry.setState(state);

        // kill all of memory and value numbering
        killAll();
        _block = _osrEntry;
        _state = state.copy();
        _last = _osrEntry;

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
                    // the interpreter thinks this is live, but not the interpreter
                    // pretend that it passed null
                    get = appendConstant(ConstType.NULL_OBJECT);
                } else {
                    Instruction oc = appendConstant(ConstType.forInt(offset));
                    get = append(new UnsafeGetRaw(local.type().basicType(), e, oc, 0, true));
                }
                state.storeLocal(i, get);
            }
        }

        assert state.scope().callerState() == null;
        state.clearLocals();
        Goto g = new Goto(target, state.copy(), false);
        append(g);
        _osrEntry.setEnd(g);
        target.merge(_osrEntry.end().state());
        _scopeData.setStream(null);
    }

    BlockEnd iterateBytecodesForBlock(int bci) {
        _skipBlock = false;
        assert _state != null;
        BytecodeStream s = new BytecodeStream(method().code());
        s.setBCI(bci);
        _scopeData.setStream(s);

        BlockBegin block = _block;
        BlockEnd end = null;
        boolean pushException = block.isExceptionEntry() && block.next() == null;
        int prevBCI = bci;
        int endBCI = s.endBCI();
        while (bci < endBCI) {
            BlockBegin nextBlock = blockAt(bci);
            if (nextBlock != null && nextBlock != block) {
                // we fell through to the next block, add a goto and break
                end = new Goto(nextBlock, null, false);
                _last = _last.setNext(end, prevBCI);
                break;
            }
            int opcode = s.currentBC();
            // check whether the bytecode can cause an exception
            if (hasHandler() && Bytecodes.canTrap(opcode)) {
                _exceptionState = _state.copy();
            } else {
                _exceptionState = null;
            }
            // check for active JSR during OSR compilation
            if (_compilation.isOsrCompilation()
                    && scope().isTopScope()
                    && _scopeData.parsingJsr()
                    && s.currentBCI() == _compilation.osrBCI()) {
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
                case Bytecodes.ACONST_NULL    : apush(appendConstant(ConstType.NULL_OBJECT)); break;
                case Bytecodes.ICONST_M1      : ipush(appendConstant(ConstType.INT_MINUS_1)); break;
                case Bytecodes.ICONST_0       : ipush(appendConstant(ConstType.INT_0)); break;
                case Bytecodes.ICONST_1       : ipush(appendConstant(ConstType.INT_1)); break;
                case Bytecodes.ICONST_2       : ipush(appendConstant(ConstType.INT_2)); break;
                case Bytecodes.ICONST_3       : ipush(appendConstant(ConstType.INT_3)); break;
                case Bytecodes.ICONST_4       : ipush(appendConstant(ConstType.INT_4)); break;
                case Bytecodes.ICONST_5       : ipush(appendConstant(ConstType.INT_5)); break;
                case Bytecodes.LCONST_0       : lpush(appendConstant(ConstType.LONG_0)); break;
                case Bytecodes.LCONST_1       : lpush(appendConstant(ConstType.LONG_1)); break;
                case Bytecodes.FCONST_0       : fpush(appendConstant(ConstType.FLOAT_0)); break;
                case Bytecodes.FCONST_1       : fpush(appendConstant(ConstType.FLOAT_1)); break;
                case Bytecodes.FCONST_2       : fpush(appendConstant(ConstType.FLOAT_2)); break;
                case Bytecodes.DCONST_0       : dpush(appendConstant(ConstType.DOUBLE_0)); break;
                case Bytecodes.DCONST_1       : dpush(appendConstant(ConstType.DOUBLE_1)); break;
                case Bytecodes.BIPUSH         : ipush(appendConstant(ConstType.forInt(s.readByte()))); break;
                case Bytecodes.SIPUSH         : ipush(appendConstant(ConstType.forInt(s.readShort()))); break;
                case Bytecodes.LDC            : // fall through
                case Bytecodes.LDC_W          : // fall through
                case Bytecodes.LDC2_W         : loadConstant(); break;
                case Bytecodes.ILOAD          : loadLocal(ValueType.INT_TYPE   , s.readLocalIndex()); break;
                case Bytecodes.LLOAD          : loadLocal(ValueType.LONG_TYPE  , s.readLocalIndex()); break;
                case Bytecodes.FLOAD          : loadLocal(ValueType.FLOAT_TYPE , s.readLocalIndex()); break;
                case Bytecodes.DLOAD          : loadLocal(ValueType.DOUBLE_TYPE, s.readLocalIndex()); break;
                case Bytecodes.ALOAD          : loadLocal(ValueType.OBJECT_TYPE, s.readLocalIndex()); break;
                case Bytecodes.ILOAD_0        : loadLocal(ValueType.INT_TYPE   , 0); break;
                case Bytecodes.ILOAD_1        : loadLocal(ValueType.INT_TYPE   , 1); break;
                case Bytecodes.ILOAD_2        : loadLocal(ValueType.INT_TYPE   , 2); break;
                case Bytecodes.ILOAD_3        : loadLocal(ValueType.INT_TYPE   , 3); break;
                case Bytecodes.LLOAD_0        : loadLocal(ValueType.LONG_TYPE  , 0); break;
                case Bytecodes.LLOAD_1        : loadLocal(ValueType.LONG_TYPE  , 1); break;
                case Bytecodes.LLOAD_2        : loadLocal(ValueType.LONG_TYPE  , 2); break;
                case Bytecodes.LLOAD_3        : loadLocal(ValueType.LONG_TYPE  , 3); break;
                case Bytecodes.FLOAD_0        : loadLocal(ValueType.FLOAT_TYPE , 0); break;
                case Bytecodes.FLOAD_1        : loadLocal(ValueType.FLOAT_TYPE , 1); break;
                case Bytecodes.FLOAD_2        : loadLocal(ValueType.FLOAT_TYPE , 2); break;
                case Bytecodes.FLOAD_3        : loadLocal(ValueType.FLOAT_TYPE , 3); break;
                case Bytecodes.DLOAD_0        : loadLocal(ValueType.DOUBLE_TYPE, 0); break;
                case Bytecodes.DLOAD_1        : loadLocal(ValueType.DOUBLE_TYPE, 1); break;
                case Bytecodes.DLOAD_2        : loadLocal(ValueType.DOUBLE_TYPE, 2); break;
                case Bytecodes.DLOAD_3        : loadLocal(ValueType.DOUBLE_TYPE, 3); break;
                case Bytecodes.ALOAD_0        : loadLocal(ValueType.OBJECT_TYPE, 0); break;
                case Bytecodes.ALOAD_1        : loadLocal(ValueType.OBJECT_TYPE, 1); break;
                case Bytecodes.ALOAD_2        : loadLocal(ValueType.OBJECT_TYPE, 2); break;
                case Bytecodes.ALOAD_3        : loadLocal(ValueType.OBJECT_TYPE, 3); break;
                case Bytecodes.IALOAD         : loadIndexed(BasicType.Int   ); break;
                case Bytecodes.LALOAD         : loadIndexed(BasicType.Long  ); break;
                case Bytecodes.FALOAD         : loadIndexed(BasicType.Float ); break;
                case Bytecodes.DALOAD         : loadIndexed(BasicType.Double); break;
                case Bytecodes.AALOAD         : loadIndexed(BasicType.Object); break;
                case Bytecodes.BALOAD         : loadIndexed(BasicType.Byte  ); break;
                case Bytecodes.CALOAD         : loadIndexed(BasicType.Char  ); break;
                case Bytecodes.SALOAD         : loadIndexed(BasicType.Short ); break;
                case Bytecodes.ISTORE         : storeLocal(ValueType.INT_TYPE   , s.readLocalIndex()); break;
                case Bytecodes.LSTORE         : storeLocal(ValueType.LONG_TYPE  , s.readLocalIndex()); break;
                case Bytecodes.FSTORE         : storeLocal(ValueType.FLOAT_TYPE , s.readLocalIndex()); break;
                case Bytecodes.DSTORE         : storeLocal(ValueType.DOUBLE_TYPE, s.readLocalIndex()); break;
                case Bytecodes.ASTORE         : storeLocal(ValueType.OBJECT_TYPE, s.readLocalIndex()); break;
                case Bytecodes.ISTORE_0       : storeLocal(ValueType.INT_TYPE   , 0); break;
                case Bytecodes.ISTORE_1       : storeLocal(ValueType.INT_TYPE   , 1); break;
                case Bytecodes.ISTORE_2       : storeLocal(ValueType.INT_TYPE   , 2); break;
                case Bytecodes.ISTORE_3       : storeLocal(ValueType.INT_TYPE   , 3); break;
                case Bytecodes.LSTORE_0       : storeLocal(ValueType.LONG_TYPE  , 0); break;
                case Bytecodes.LSTORE_1       : storeLocal(ValueType.LONG_TYPE  , 1); break;
                case Bytecodes.LSTORE_2       : storeLocal(ValueType.LONG_TYPE  , 2); break;
                case Bytecodes.LSTORE_3       : storeLocal(ValueType.LONG_TYPE  , 3); break;
                case Bytecodes.FSTORE_0       : storeLocal(ValueType.FLOAT_TYPE , 0); break;
                case Bytecodes.FSTORE_1       : storeLocal(ValueType.FLOAT_TYPE , 1); break;
                case Bytecodes.FSTORE_2       : storeLocal(ValueType.FLOAT_TYPE , 2); break;
                case Bytecodes.FSTORE_3       : storeLocal(ValueType.FLOAT_TYPE , 3); break;
                case Bytecodes.DSTORE_0       : storeLocal(ValueType.DOUBLE_TYPE, 0); break;
                case Bytecodes.DSTORE_1       : storeLocal(ValueType.DOUBLE_TYPE, 1); break;
                case Bytecodes.DSTORE_2       : storeLocal(ValueType.DOUBLE_TYPE, 2); break;
                case Bytecodes.DSTORE_3       : storeLocal(ValueType.DOUBLE_TYPE, 3); break;
                case Bytecodes.ASTORE_0       : storeLocal(ValueType.OBJECT_TYPE, 0); break;
                case Bytecodes.ASTORE_1       : storeLocal(ValueType.OBJECT_TYPE, 1); break;
                case Bytecodes.ASTORE_2       : storeLocal(ValueType.OBJECT_TYPE, 2); break;
                case Bytecodes.ASTORE_3       : storeLocal(ValueType.OBJECT_TYPE, 3); break;
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
                case Bytecodes.IADD           : arithmeticOp(ValueType.INT_TYPE   , opcode); break;
                case Bytecodes.LADD           : arithmeticOp(ValueType.LONG_TYPE  , opcode); break;
                case Bytecodes.FADD           : arithmeticOp(ValueType.FLOAT_TYPE , opcode); break;
                case Bytecodes.DADD           : arithmeticOp(ValueType.DOUBLE_TYPE, opcode); break;
                case Bytecodes.ISUB           : arithmeticOp(ValueType.INT_TYPE   , opcode); break;
                case Bytecodes.LSUB           : arithmeticOp(ValueType.LONG_TYPE  , opcode); break;
                case Bytecodes.FSUB           : arithmeticOp(ValueType.FLOAT_TYPE , opcode); break;
                case Bytecodes.DSUB           : arithmeticOp(ValueType.DOUBLE_TYPE, opcode); break;
                case Bytecodes.IMUL           : arithmeticOp(ValueType.INT_TYPE   , opcode); break;
                case Bytecodes.LMUL           : arithmeticOp(ValueType.LONG_TYPE  , opcode); break;
                case Bytecodes.FMUL           : arithmeticOp(ValueType.FLOAT_TYPE , opcode); break;
                case Bytecodes.DMUL           : arithmeticOp(ValueType.DOUBLE_TYPE, opcode); break;
                case Bytecodes.IDIV           : arithmeticOp(ValueType.INT_TYPE   , opcode, lockStack()); break;
                case Bytecodes.LDIV           : arithmeticOp(ValueType.LONG_TYPE  , opcode, lockStack()); break;
                case Bytecodes.FDIV           : arithmeticOp(ValueType.FLOAT_TYPE , opcode); break;
                case Bytecodes.DDIV           : arithmeticOp(ValueType.DOUBLE_TYPE, opcode); break;
                case Bytecodes.IREM           : arithmeticOp(ValueType.INT_TYPE   , opcode, lockStack()); break;
                case Bytecodes.LREM           : arithmeticOp(ValueType.LONG_TYPE  , opcode, lockStack()); break;
                case Bytecodes.FREM           : arithmeticOp(ValueType.FLOAT_TYPE , opcode); break;
                case Bytecodes.DREM           : arithmeticOp(ValueType.DOUBLE_TYPE, opcode); break;
                case Bytecodes.INEG           : negateOp(ValueType.INT_TYPE   ); break;
                case Bytecodes.LNEG           : negateOp(ValueType.LONG_TYPE  ); break;
                case Bytecodes.FNEG           : negateOp(ValueType.FLOAT_TYPE ); break;
                case Bytecodes.DNEG           : negateOp(ValueType.DOUBLE_TYPE); break;
                case Bytecodes.ISHL           : shiftOp(ValueType.INT_TYPE , opcode); break;
                case Bytecodes.LSHL           : shiftOp(ValueType.LONG_TYPE, opcode); break;
                case Bytecodes.ISHR           : shiftOp(ValueType.INT_TYPE , opcode); break;
                case Bytecodes.LSHR           : shiftOp(ValueType.LONG_TYPE, opcode); break;
                case Bytecodes.IUSHR          : shiftOp(ValueType.INT_TYPE , opcode); break;
                case Bytecodes.LUSHR          : shiftOp(ValueType.LONG_TYPE, opcode); break;
                case Bytecodes.IAND           : logicOp(ValueType.INT_TYPE , opcode); break;
                case Bytecodes.LAND           : logicOp(ValueType.LONG_TYPE, opcode); break;
                case Bytecodes.IOR            : logicOp(ValueType.INT_TYPE , opcode); break;
                case Bytecodes.LOR            : logicOp(ValueType.LONG_TYPE, opcode); break;
                case Bytecodes.IXOR           : logicOp(ValueType.INT_TYPE , opcode); break;
                case Bytecodes.LXOR           : logicOp(ValueType.LONG_TYPE, opcode); break;
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
                case Bytecodes.LCMP           : compareOp(ValueType.LONG_TYPE  , opcode); break;
                case Bytecodes.FCMPL          : compareOp(ValueType.FLOAT_TYPE , opcode); break;
                case Bytecodes.FCMPG          : compareOp(ValueType.FLOAT_TYPE , opcode); break;
                case Bytecodes.DCMPL          : compareOp(ValueType.DOUBLE_TYPE, opcode); break;
                case Bytecodes.DCMPG          : compareOp(ValueType.DOUBLE_TYPE, opcode); break;
                case Bytecodes.IFEQ           : ifZero(ValueType.INT_TYPE   , Condition.eql); break;
                case Bytecodes.IFNE           : ifZero(ValueType.INT_TYPE   , Condition.neq); break;
                case Bytecodes.IFLT           : ifZero(ValueType.INT_TYPE   , Condition.lss); break;
                case Bytecodes.IFGE           : ifZero(ValueType.INT_TYPE   , Condition.geq); break;
                case Bytecodes.IFGT           : ifZero(ValueType.INT_TYPE   , Condition.gtr); break;
                case Bytecodes.IFLE           : ifZero(ValueType.INT_TYPE   , Condition.leq); break;
                case Bytecodes.IF_ICMPEQ      : ifSame(ValueType.INT_TYPE   , Condition.eql); break;
                case Bytecodes.IF_ICMPNE      : ifSame(ValueType.INT_TYPE   , Condition.neq); break;
                case Bytecodes.IF_ICMPLT      : ifSame(ValueType.INT_TYPE   , Condition.lss); break;
                case Bytecodes.IF_ICMPGE      : ifSame(ValueType.INT_TYPE   , Condition.geq); break;
                case Bytecodes.IF_ICMPGT      : ifSame(ValueType.INT_TYPE   , Condition.gtr); break;
                case Bytecodes.IF_ICMPLE      : ifSame(ValueType.INT_TYPE   , Condition.leq); break;
                case Bytecodes.IF_ACMPEQ      : ifSame(ValueType.OBJECT_TYPE, Condition.eql); break;
                case Bytecodes.IF_ACMPNE      : ifSame(ValueType.OBJECT_TYPE, Condition.neq); break;
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
                case Bytecodes.GETSTATIC      : // fall through
                case Bytecodes.PUTSTATIC      : // fall through
                case Bytecodes.GETFIELD       : // fall through
                case Bytecodes.PUTFIELD       : accessField(opcode); break;
                case Bytecodes.INVOKEVIRTUAL  : invokeVirtual(readMethod(opcode)); break;
                case Bytecodes.INVOKESPECIAL  : invokeSpecial(readMethod(opcode), null); break;
                case Bytecodes.INVOKESTATIC   : invokeStatic(readMethod(opcode)); break;
                case Bytecodes.INVOKEINTERFACE: invokeInterface(readMethod(opcode)); break;
                case Bytecodes.NEW            : newInstance(); break;
                case Bytecodes.NEWARRAY       : newTypeArray(); break;
                case Bytecodes.ANEWARRAY      : newObjectArray(); break;
                case Bytecodes.ARRAYLENGTH    : ipush(append(new ArrayLength(apop(), lockStack()))); break;
                case Bytecodes.ATHROW         : throw_(s.currentBCI()); break;
                case Bytecodes.CHECKCAST      : checkcast_(); break;
                case Bytecodes.INSTANCEOF     : instanceof_(); break;
                case Bytecodes.MONITORENTER   : monitorenter(apop(), s.currentBCI()); break;
                case Bytecodes.MONITOREXIT    : monitorexit (apop(), s.currentBCI()); break;
                case Bytecodes.MULTIANEWARRAY : newMultiArray(); break;
                case Bytecodes.IFNULL         : ifNull(ValueType.OBJECT_TYPE, Condition.eql); break;
                case Bytecodes.IFNONNULL      : ifNull(ValueType.OBJECT_TYPE, Condition.neq); break;
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

            if (_last instanceof BlockEnd) {
                end = (BlockEnd) _last;
                break;
            }
            bci = s.currentBCI();
        }

        // stop processing of this block
        if (_skipBlock) {
            _skipBlock = false;
            return (BlockEnd) _last;
        }

        // if the method terminates, we don't need the stack anymore
        if (end instanceof Return) {
            _state.clearStack();
        } else if (end instanceof Throw) {
            // may have exception handlers in caller scopes
            _state.truncateStack(scope().lockStackSize());
        }

        // connect to begin and set state
        // NOTE that inlining may have changed the block we are parsing
        _block.setEnd(end);
        end.setState(_state);
        // propagate the state
        for (BlockBegin succ : end.successors()) {
            assert succ.predecessors().contains(_block);
            if (!succ.tryMerge(_state)) {
                throw new Bailout("block join failed");
            }
            _scopeData.addToWorkList(succ);
        }
        _scopeData.setStream(null);
        return end;
    }

    private CiMethod readMethod(int opcode) {
        return constantPool().lookupMethod(opcode, stream().readCPI());
    }

    void killAll() {
        if (C1XOptions.UseLocalValueNumbering) {
            _vmap.killAll();
        }
        _memory.kill();
    }

    boolean assumeLeafClass(CiType type) {
        if (!C1XOptions.UseSlowPath && type.isLoaded()) {
            if (type.isFinal()) {
                return true;
            }
            if (C1XOptions.UseDeopt && C1XOptions.UseCHA) {
                if (!type.hasSubclass() && !type.isInterface()) {
                    return _compilation.recordLeafTypeAssumption(type);
                }
            }
        }
        return false;
    }

    boolean assumeLeafMethod(CiMethod method) {
        if (!C1XOptions.UseSlowPath && method.isLoaded()) {
            if (method.isFinalMethod()) {
                return true;
            }
            if (C1XOptions.UseDeopt && C1XOptions.UseCHALeafMethods) {
                if (!method.isOverridden() && !method.holder().isInterface()) {
                    return _compilation.recordLeafMethodAssumption(method);
                }
            }
        }
        return false;
    }

    int recursiveInlineLevel(CiMethod target) {
        int rec = 0;
        IRScope scope = scope();
        while (scope != null) {
            if (scope.method() != target) break;
            scope = scope.caller();
            rec++;
        }
        return rec;
    }

    CiConstantPool constantPool() {
        // XXX: speed up the access to the constant pool
        return _compilation.runtime().getConstantPool(method());
    }
}
