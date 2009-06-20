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
package com.sun.c1x.opt;

import com.sun.c1x.ir.*;
import com.sun.c1x.value.*;
import com.sun.c1x.bytecode.Bytecodes;
import com.sun.c1x.util.InstructionVisitor;
import com.sun.c1x.util.Util;
import com.sun.c1x.C1XOptions;
import com.sun.c1x.ci.CiType;
import com.sun.c1x.ci.CiField;
import com.sun.c1x.ci.CiConstant;

import java.util.List;
import java.util.LinkedList;

/**
 * The <code>Canonicalizer</code> reduces instructions to a canonical form by folding constants,
 * putting constants on the right side of commutative operators, simplifying conditionals,
 * and several other transformations.
 *
 * @author Ben L. Titzer
 */
public class Canonicalizer extends InstructionVisitor {

    Instruction _canonical;
    List<Instruction> _extra;
    int _bci;

    /**
     * Creates a new Canonicalizer for the specified instruction.
     * @param original the original instruction to be canonicalized
     * @param bci the bytecode index of the original instruction
     */
    public Canonicalizer(Instruction original, int bci) {
        // XXX: reusing a canonicalizer instance for each operation would reduce allocation
        _canonical = original;
        _bci = bci;
        original.accept(this);
    }

    public static Instruction canonicalize(Instruction i, int bci) {
        if (C1XOptions.CanonicalizeInstructions) {
            return new Canonicalizer(i, bci).canonical();
        }
        return i;
    }

    /**
     * Gets the canonicalized version of the instruction.
     * @return the canonicalized version of the instruction
     */
    public Instruction canonical() {
        return _canonical;
    }

    public List<Instruction> extra() {
        return _extra;
    }

    private <T extends Instruction> T addInstr(T x) {
        if (_extra == null) {
            _extra = new LinkedList<Instruction>();
        }
        _extra.add(x);
        return x;
    }

    private Constant intInstr(int v) {
        return addInstr(Constant.forInt(v));
    }

    private Constant longInstr(long v) {
        return addInstr(Constant.forLong(v));
    }

    private Instruction setCanonical(Instruction x) {
        return _canonical = x;
    }

    private Instruction setIntConstant(int val) {
        return _canonical = Constant.forInt(val);
    }

    private Instruction setBooleanConstant(boolean val) {
        return _canonical = Constant.forBoolean(val);
    }

    private Instruction setObjectConstant(Object val) {
        if (C1XOptions.SupportObjectConstants) {
            return _canonical = Constant.forObject(val);
        }
        return _canonical;
    }

    private Instruction setLongConstant(long val) {
        return _canonical = Constant.forLong(val);
    }

    private Instruction setFloatConstant(float val) {
        return _canonical = Constant.forFloat(val);
    }

    private Instruction setDoubleConstant(double val) {
        return _canonical = Constant.forDouble(val);
    }

    private Instruction setByteConstant(byte val) {
        return _canonical = new Constant(ConstType.forByte(val));
    }

    private Instruction setCharConstant(char val) {
        return _canonical = new Constant(ConstType.forChar(val));
    }

    private Instruction setShortConstant(short val) {
        return _canonical = new Constant(ConstType.forShort(val));
    }

    private void moveConstantToRight(Op2 x) {
        if (x.x().type().isConstant() && Bytecodes.isCommutative(x.opcode())) {
            x.swapOperands();
        }
    }

    private void visitOp2(Op2 i) {
        Instruction x = i.x();
        Instruction y = i.y();

        if (x == y) {
            // the left and right operands are the same value, try reducing some operations
            switch (i.opcode()) {
                case Bytecodes.ISUB: setIntConstant(0); return;
                case Bytecodes.LSUB: setLongConstant(0); return;
                case Bytecodes.IAND: // fall through
                case Bytecodes.LAND: // fall through
                case Bytecodes.IOR:  // fall through
                case Bytecodes.LOR: setCanonical(x); return;
                case Bytecodes.IXOR: setIntConstant(0); return;
                case Bytecodes.LXOR: setLongConstant(0); return;
            }
        }

        ValueType xt = x.type();
        ValueType yt = y.type();
        if (xt.isConstant() && yt.isConstant()) {
            // both operands are constants, try constant folding
            switch (xt.basicType()) {
                case Int: {
                    Integer val = Bytecodes.foldIntOp2(i.opcode(), xt.asConstant().asInt(), yt.asConstant().asInt());
                    if (val != null) {
                        setIntConstant(val); // the operation was successfully folded to an int
                        return;
                    }
                    break;
                }
                case Long: {
                    Long val = Bytecodes.foldLongOp2(i.opcode(), xt.asConstant().asLong(), yt.asConstant().asLong());
                    if (val != null) {
                        setLongConstant(val); // the operation was successfully folded to a long
                        return;
                    }
                    break;
                }
                case Float: {
                    if (C1XOptions.CanonicalizeFloatingPoint) {
                        // try to fold a floating point operation
                        Float val = Bytecodes.foldFloatOp2(i.opcode(), xt.asConstant().asFloat(), yt.asConstant().asFloat());
                        if (val != null) {
                            setFloatConstant(val); // the operation was successfully folded to a float
                            return;
                        }
                    }
                    break;
                }
                case Double: {
                    if (C1XOptions.CanonicalizeFloatingPoint) {
                        // try to fold a floating point operation
                        Double val = Bytecodes.foldDoubleOp2(i.opcode(), xt.asConstant().asDouble(), yt.asConstant().asDouble());
                        if (val != null) {
                            setDoubleConstant(val); // the operation was successfully folded to a double
                            return;
                        }
                    }
                    break;
                }
            }
        }

        // if there is a constant on the left and the operation is commutative, move it to the right
        moveConstantToRight(i);

        yt = i.y().type();
        if (yt.isConstant()) {
            // the right side is a constant, try strength reduction
            switch (xt.basicType()) {
                case Int: {
                    if (reduceIntOp2(i, x, yt.asConstant().asInt()) != null) {
                        return;
                    }
                    break;
                }
                case Long: {
                    if (reduceLongOp2(i, x, yt.asConstant().asLong()) != null) {
                        return;
                    }
                    break;
                }
                // XXX: note that other cases are possible, but harder
                // floating point operations need to be extra careful
            }
        }
        assert Instruction.sameBasicType(i, _canonical);
    }

    private Instruction reduceIntOp2(Op2 original, Instruction x, int y) {
        // attempt to reduce a binary operation with a constant on the right
        int opcode = original.opcode();
        switch (opcode) {
            case Bytecodes.IADD: return y == 0 ? setCanonical(x) : null;
            case Bytecodes.ISUB: return y == 0 ? setCanonical(x) : null;
            case Bytecodes.IMUL: {
                if (y == 1) {
                    return setCanonical(x);
                }
                if (y > 0 && (y & y - 1) == 0 && C1XOptions.CanonicalizeMultipliesToShifts) {
                    // strength reduce multiply by power of 2 to shift operation
                    return setCanonical(new ShiftOp(Bytecodes.ISHL, x, intInstr(Util.log2(y))));
                }
                return y == 0 ? setIntConstant(0) : null;
            }
            case Bytecodes.IDIV: return y == 1 ? setCanonical(x) : null;
            case Bytecodes.IREM: return y == 1 ? setCanonical(x) : null;
            case Bytecodes.IAND: {
                if (y == -1) {
                    return setCanonical(x);
                }
                return y == 0 ? setIntConstant(0) : null;
            }
            case Bytecodes.IOR: {
                if (y == -1) {
                    return setIntConstant(-1);
                }
                return y == 0 ? setCanonical(x) : null;
            }
            case Bytecodes.IXOR: return y == 0 ? setCanonical(x) : null;
            case Bytecodes.ISHL: return reduceShift(false, opcode, Bytecodes.IUSHR, x, y);
            case Bytecodes.ISHR: return reduceShift(false, opcode, 0, x, y);
            case Bytecodes.IUSHR: return reduceShift(false, opcode, Bytecodes.ISHL, x, y);
        }
        return null;
    }

    private Instruction reduceShift(boolean islong, int opcode, int reverse, Instruction x, long y) {
        int mod = islong ? 0x3f : 0x1f;
        long shift = y & mod;
        if (shift == 0) {
            return setCanonical(x);
        }
        if (x instanceof ShiftOp) {
            // this is a chained shift operation ((e shift e) shift K)
            ShiftOp s = (ShiftOp) x;
            if (s.y().type().isConstant()) {
                long z = s.y().type().asConstant().asLong();
                if (s.opcode() == opcode) {
                    // this is a chained shift operation (e >> C >> K)
                    y = y + z;
                    shift = y & mod;
                    if (shift == 0) {
                        return setCanonical(s.x());
                    }
                    // reduce to (e >> (C + K))
                    Instruction c = islong ? longInstr(y) : intInstr((int) y);
                    return setCanonical(new ShiftOp(opcode, s.x(), c));
                }
                if (s.opcode() == reverse && y == z) {
                    // this is a chained shift of the form (e >> K << K)
                    long mask = -1;
                    if (opcode == Bytecodes.IUSHR || opcode == Bytecodes.LUSHR) {
                        mask = mask >>> y;
                    } else {
                        mask = mask << y;
                    }
                    // reduce to (e & mask)
                    Instruction c = islong ? longInstr(mask) : intInstr((int) mask);
                    return setCanonical(new ArithmeticOp(Bytecodes.IAND, s.x(), c, false, null));
                }
            }
        }
        if (y != shift) {
            // (y & mod) != y
            return setCanonical(new ShiftOp(opcode, x, intInstr((int) shift)));
        }
        return null;
    }

    private Instruction reduceLongOp2(Op2 original, Instruction x, long y) {
        // attempt to reduce a binary operation with a constant on the right
        int opcode = original.opcode();
        switch (opcode) {
            case Bytecodes.LADD: return y == 0 ? setCanonical(x) : null;
            case Bytecodes.LSUB: return y == 0 ? setCanonical(x) : null;
            case Bytecodes.LMUL: {
                if (y == 1) {
                    return setCanonical(x);
                }
                if (y > 0 && (y & y - 1) == 0 && C1XOptions.CanonicalizeMultipliesToShifts) {
                    // strength reduce multiply by power of 2 to shift operation
                    return setCanonical(new ShiftOp(Bytecodes.LSHL, x, longInstr(Util.log2(y))));
                }
                return y == 0 ? setLongConstant(0) : null;
            }
            case Bytecodes.LDIV: return y == 1 ? setCanonical(x) : null;
            case Bytecodes.LREM: return y == 1 ? setCanonical(x) : null;
            case Bytecodes.LAND: {
                if (y == -1) {
                    return setCanonical(x);
                }
                return y == 0 ? setLongConstant(0) : null;
            }
            case Bytecodes.LOR: {
                if (y == -1) {
                    return setLongConstant(-1);
                }
                return y == 0 ? setCanonical(x) : null;
            }
            case Bytecodes.LXOR: return y == 0 ? setCanonical(x) : null;
            case Bytecodes.LSHL: return reduceShift(true, opcode, Bytecodes.LUSHR, x, y);
            case Bytecodes.LSHR: return reduceShift(true, opcode, 0, x, y);
            case Bytecodes.LUSHR: return reduceShift(true, opcode, Bytecodes.LSHL, x, y);
        }
        return null;
    }

    private boolean inCurrentBlock(Instruction x) {
        int max = 4; // XXX: anything special about 4? seems like a tunable heuristic
        while (max > 0 && x != null && !(x instanceof BlockEnd)) {
            x = x.next();
            max--;
        }
        return x == null;
    }

    private Instruction eliminateNarrowing(BasicType type, Convert c) {
        Instruction nv = null;
        switch (c.opcode()) {
            case Bytecodes.I2B:
                if (type == BasicType.Byte) {
                    nv = c.value();
                }
                break;
            case Bytecodes.I2S:
                if (type == BasicType.Short || type == BasicType.Byte) {
                    nv = c.value();
                }
                break;
            case Bytecodes.I2C:
                if (type == BasicType.Char || type == BasicType.Byte) {
                    nv = c.value();
                }
                break;
        }
        return nv;
    }

    @Override
    public void visitLoadField(LoadField i) {
        if (i.isStatic() && i.isLoaded() && C1XOptions.CanonicalizeConstantFields) {
            // only try to canonicalize static field loads
            CiField field = i.field();
            if (field.isConstant()) {
                CiConstant val = field.constantValue();
                // XXX: this is clunky, perhaps constantValue() should return ConstType?
                switch (val.basicType()) {
                    case Boolean: setBooleanConstant(val.asBoolean()); break;
                    case Char:    setCharConstant(val.asChar()); break;
                    case Float:   setFloatConstant(val.asFloat()); break;
                    case Double:  setDoubleConstant(val.asDouble()); break;
                    case Byte:    setByteConstant(val.asByte()); break;
                    case Short:   setShortConstant(val.asShort()); break;
                    case Int:     setIntConstant(val.asInt()); break;
                    case Long:    setLongConstant(val.asLong()); break;
                    case Object:
                        if (C1XOptions.SupportObjectConstants) {
                            setObjectConstant(val.asObject()); break;
                        }
                }
            }
        }
    }

    @Override
    public void visitStoreField(StoreField i) {
        if (C1XOptions.CanonicalizeNarrowingInStores) {
            // Eliminate narrowing conversions emitted by javac which are unnecessary when
            // writing the value to a field that is packed
            Instruction v = i.value();
            if (v instanceof Convert) {
                Instruction nv = eliminateNarrowing(i.field().basicType(), (Convert) v);
                // limit this optimization to the current basic block
                if (nv != null && inCurrentBlock(v)) {
                    setCanonical(new StoreField(i.object(), i.field(), nv, i.isStatic(),
                                                i.lockStack(), i.stateBefore(), i.isLoaded(), i.isInitialized()));
                }
            }
        }
    }

    @Override
    public void visitArrayLength(ArrayLength i) {
        // we can compute the length of the array statically if the object
        // is a NewArray of a constant, or if the object is a constant reference
        // (either by itself or loaded from a constant value field)
        Instruction array = i.array();
        if (array instanceof NewArray) {
            // the array is a NewArray; check if it has a constant length
            NewArray newArray = (NewArray) array;
            Instruction length = newArray.length();
            if (length instanceof Constant) {
                // note that we don't use the Constant instruction itself
                // as that would cause problems with liveness later
                int actualLength = length.type().asConstant().asInt();
                setIntConstant(actualLength);
            }
        } else if (array instanceof LoadField) {
            // the array is a load of a field; check if it is a constant
            CiField field = ((LoadField) array).field();
            if (field.isConstant()) {
                Object obj = field.constantValue().asObject();
                if (obj != null) {
                    setIntConstant(java.lang.reflect.Array.getLength(obj));
                }
            }
        } else if (C1XOptions.SupportObjectConstants && array instanceof Constant) {
            // the array itself is a constant object reference
            Object obj = array.type().asConstant().asObject();
            if (obj != null) {
                setIntConstant(java.lang.reflect.Array.getLength(obj));
            }
        }
    }

    @Override
    public void visitStoreIndexed(StoreIndexed i) {
        if (C1XOptions.CanonicalizeNarrowingInStores) {
            // Eliminate narrowing conversions emitted by javac which are unnecessary when
            // writing the value to an array (which is packed)
            Instruction v = i.value();
            if (v instanceof Convert) {
                Instruction nv = eliminateNarrowing(i.elementType(), (Convert) v);
                if (nv != null && inCurrentBlock(v)) {
                    setCanonical(new StoreIndexed(i.array(), i.index(), i.length(), i.elementType(), nv, i.lockStack()));
                }
            }
        }
    }

    @Override
    public void visitNegateOp(NegateOp i) {
        ValueType vt = i.x().type();
        if (vt.isConstant()) {
            switch (vt.basicType()) {
                case Int: setIntConstant(-vt.asConstant().asInt()); break;
                case Long: setLongConstant(-vt.asConstant().asLong()); break;
                case Float: setFloatConstant(-vt.asConstant().asFloat()); break;
                case Double: setDoubleConstant(-vt.asConstant().asDouble()); break;
            }
        }
        assert vt.basicType() == _canonical.type().basicType();
    }

    @Override
    public void visitArithmeticOp(ArithmeticOp i) {
        visitOp2(i);
    }

    @Override
    public void visitShiftOp(ShiftOp i) {
        visitOp2(i);
    }

    @Override
    public void visitLogicOp(LogicOp i) {
        visitOp2(i);
    }

    @Override
    public void visitCompareOp(CompareOp i) {
        // we can reduce a compare op if the two inputs are the same,
        // or if both are constants
        Instruction x = i.x();
        Instruction y = i.y();
        ValueType xt = x.type();
        ValueType yt = y.type();
        if (x == y) {
            // x and y are generated by the same instruction
            switch (xt.basicType()) {
                case Long: setIntConstant(0); return;
                case Float:
                    if (xt.isConstant()) {
                        float xval = xt.asConstant().asFloat(); // get the actual value of x (and y since x == y)
                        Integer val = Bytecodes.foldFloatCompare(i.opcode(), xval, xval);
                        assert val != null : "invalid opcode in float compare op";
                        setIntConstant(val);
                        return;
                    }
                    break;
                case Double:
                    if (xt.isConstant()) {
                        double xval = xt.asConstant().asDouble(); // get the actual value of x (and y since x == y)
                        Integer val = Bytecodes.foldDoubleCompare(i.opcode(), xval, xval);
                        assert val != null : "invalid opcode in double compare op";
                        setIntConstant(val);
                        return;
                    }
                    break;
                // note that there are no integer CompareOps
            }
        }
        if (xt.isConstant() && yt.isConstant()) {
            // both x and y are constants
            switch (xt.basicType()) {
                case Long:
                    setIntConstant(Bytecodes.foldLongCompare(xt.asConstant().asLong(), yt.asConstant().asLong()));
                    break;
                case Float: {
                    Integer val = Bytecodes.foldFloatCompare(i.opcode(), xt.asConstant().asFloat(), yt.asConstant().asFloat());
                    assert val != null : "invalid opcode in float compare op";
                    setIntConstant(val);
                    break;
                }
                case Double: {
                    Integer val = Bytecodes.foldDoubleCompare(i.opcode(), xt.asConstant().asDouble(), yt.asConstant().asDouble());
                    assert val != null : "invalid opcode in float compare op";
                    setIntConstant(val);
                    break;
                }
            }
        }
        assert Instruction.sameBasicType(i, _canonical);
    }

    @Override
    public void visitIfOp(IfOp i) {
        moveConstantToRight(i);
    }

    @Override
    public void visitConvert(Convert i) {
        Instruction v = i.value();
        ValueType xt = v.type();
        if (xt.isConstant()) {
            // fold conversions between primitive types
            // Checkstyle: stop
            switch (i.opcode()) {
                case Bytecodes.I2B: setIntConstant   ((byte)   xt.asConstant().asInt()); return;
                case Bytecodes.I2S: setIntConstant   ((short)  xt.asConstant().asInt()); return;
                case Bytecodes.I2C: setIntConstant   ((char)   xt.asConstant().asInt()); return;
                case Bytecodes.I2L: setLongConstant  (         xt.asConstant().asInt()); return;
                case Bytecodes.I2F: setFloatConstant (         xt.asConstant().asInt()); return;
                case Bytecodes.L2I: setIntConstant   ((int)    xt.asConstant().asLong()); return;
                case Bytecodes.L2F: setFloatConstant (         xt.asConstant().asLong()); return;
                case Bytecodes.L2D: setDoubleConstant(         xt.asConstant().asLong()); return;
                case Bytecodes.F2D: setDoubleConstant(         xt.asConstant().asFloat()); return;
                case Bytecodes.F2I: setIntConstant   ((int)    xt.asConstant().asFloat()); return;
                case Bytecodes.F2L: setLongConstant  ((long)   xt.asConstant().asFloat()); return;
                case Bytecodes.D2F: setFloatConstant ((float)  xt.asConstant().asDouble()); return;
                case Bytecodes.D2I: setIntConstant   ((int)    xt.asConstant().asDouble()); return;
                case Bytecodes.D2L: setLongConstant  ((long)   xt.asConstant().asDouble()); return;
            }
            // Checkstyle: resume
        }

        BasicType type = BasicType.Illegal;
        if (v instanceof LoadField) {
            // remove redundant conversions from field loads of the correct type
            type = ((LoadField) v).field().basicType();
        } else if (v instanceof LoadIndexed) {
            // remove redundant conversions from array loads of the correct type
            type = ((LoadIndexed) v).elementType();
        } else if (v instanceof Convert) {
            // remove chained redundant conversions
            Convert c = (Convert) v;
            switch (c.opcode()) {
                case Bytecodes.I2B: type = BasicType.Byte; break;
                case Bytecodes.I2S: type = BasicType.Short; break;
                case Bytecodes.I2C: type = BasicType.Char; break;
            }
        }

        if (type != BasicType.Illegal) {
            // if any of the above matched
            switch (i.opcode()) {
                case Bytecodes.I2B:
                    if (type == BasicType.Byte) {
                        setCanonical(v);
                    }
                    break;
                case Bytecodes.I2S:
                    if (type == BasicType.Byte || type == BasicType.Short) {
                        setCanonical(v);
                    }
                    break;
                case Bytecodes.I2C:
                    if (type == BasicType.Char) {
                        setCanonical(v);
                    }
                    break;
            }
        }

        if (v instanceof Op2) {
            // check if the operation was IAND with a constant; it may have narrowed the value already
            Op2 op = (Op2) v;
            ValueType mt = op.y().type(); // constant should be on right hand side if there is one
            if (op.opcode() == Bytecodes.IAND && mt.isConstant()) {
                int safebits = 0;
                int mask = mt.asConstant().asInt();
                switch (i.opcode()) {
                    case Bytecodes.I2B: safebits = 0x7f; break;
                    case Bytecodes.I2S: safebits = 0x7fff; break;
                    case Bytecodes.I2C: safebits = 0xffff; break;
                }
                if (safebits != 0 && (mask & ~safebits) == 0) {
                    // the mask already cleared all the upper bits necessary.
                    setCanonical(v);
                }
            }
        }
    }

    @Override
    public void visitNullCheck(NullCheck i) {
        Instruction o = i.object();
        if (o.isNonNull()) {
            // if the instruction producing the object was a new, no check is necessary
            setCanonical(o);
        } else if (o.type().isConstant()) {
            // if the object is a constant, check if it is nonnull
            ConstType c = o.type().asConstant();
            if (c.isObject() && c.asObject() != null) {
                setCanonical(o);
            }
        }
    }

    @Override
    public void visitCheckCast(CheckCast i) {
        // we can remove a redundant check cast if it is an object constant or the exact type is known
        if (i.targetClass().isLoaded()) {
            Instruction o = i.object();
            CiType type = o.exactType();
            if (type == null) {
                type = o.declaredType();
            }
            if (type != null && type.isLoaded() && type.isSubtypeOf(i.targetClass())) {
                // cast is redundant if exact type or declared type is already a subtype of the target type
                setCanonical(o);
            }
            if (o.type().isConstant()) {
                final Object obj = o.type().asConstant().asObject();
                if (obj == null) {
                    // checkcast of null is null
                    setCanonical(o);
                } else if (C1XOptions.SupportObjectConstants && C1XOptions.CanonicalizeObjectCheckCast) {
                    if (i.targetClass().isInstance(obj)) {
                        // fold the cast if it will succeed
                        setCanonical(o);
                    }
                }
            }
        }
    }

    @Override
    public void visitInstanceOf(InstanceOf i) {
        // we can fold an instanceof if it is an object constant or the exact type is known
        if (i.targetClass().isLoaded()) {
            Instruction o = i.object();
            CiType exact = o.exactType();
            if (exact != null && exact.isLoaded() && (o instanceof NewArray || o instanceof NewInstance)) {
                // compute instanceof statically for NewArray and NewInstance
                // XXX: why is it necessary to check (o instanceof New)? isn't exact type sufficient?
                setIntConstant(exact.isSubtypeOf(i.targetClass()) ? 1 : 0);
            }
            if (o.type().isConstant()) {
                final Object obj = o.type().asConstant().asObject();
                if (obj == null) {
                    // instanceof of null is false
                    setIntConstant(0);
                } else if (C1XOptions.SupportObjectConstants && C1XOptions.CanonicalizeObjectInstanceOf) {
                    // fold the instanceof test
                    final boolean result = i.targetClass().isInstance(obj);
                    setIntConstant(result ? 1 : 0);
                }
            }
        }
    }

    @Override
    public void visitIntrinsic(Intrinsic i) {
        if (!C1XOptions.CanonicalizeIntrinsics) {
            return;
        }
        Instruction[] args = i.arguments();
        for (Instruction arg : args) {
            if (!arg.type().isConstant()) {
                // one input is not constant, give up
                return;
            }
        }
        switch (i.intrinsic()) {
            // XXX: using Java reflection is a tempting option to simplify this code a LOT,
            // but is relatively heavyweight and causes bootstrap problems
            case java_lang_Object$hashCode: {
                Object object = argAsObject(args, 0);
                if (object != null) {
                    setIntConstant(System.identityHashCode(object));
                }
                return;
            }
            case java_lang_Object$getClass: {
                Object object = argAsObject(args, 0);
                if (object != null) {
                    setObjectConstant(object.getClass());
                }
                return;
            }

            // java.lang.Class
            case java_lang_Class$isAssignableFrom: {
                Class<?> javaClass = argAsClass(args, 0);
                Class<?> otherClass = argAsClass(args, 1);
                if (javaClass != null && otherClass != null) {
                    setBooleanConstant(javaClass.isAssignableFrom(otherClass));
                }
                return;
            }
            case java_lang_Class$isInstance: {
                Class<?> javaClass = argAsClass(args, 0);
                Object object = argAsObject(args, 1);
                if (javaClass != null && object != null) {
                    setBooleanConstant(javaClass.isInstance(object));
                }
                return;
            }
            case java_lang_Class$getModifiers: {
                Class<?> javaClass = argAsClass(args, 0);
                if (javaClass != null) {
                    setIntConstant(javaClass.getModifiers());
                }
                return;
            }
            case java_lang_Class$isInterface: {
                Class<?> javaClass = argAsClass(args, 0);
                if (javaClass != null) {
                    setBooleanConstant(javaClass.isInterface());
                }
                return;
            }
            case java_lang_Class$isArray: {
                Class<?> javaClass = argAsClass(args, 0);
                if (javaClass != null) {
                    setBooleanConstant(javaClass.isArray());
                }
                return;
            }
            case java_lang_Class$isPrimitive: {
                Class<?> javaClass = argAsClass(args, 0);
                if (javaClass != null) {
                    setBooleanConstant(javaClass.isPrimitive());
                }
                return;
            }
            case java_lang_Class$getSuperclass: {
                Class<?> javaClass = argAsClass(args, 0);
                if (javaClass != null) {
                    setObjectConstant(javaClass.getSuperclass());
                }
                return;
            }
            case java_lang_Class$getComponentType: {
                Class<?> javaClass = argAsClass(args, 0);
                if (javaClass != null) {
                    setObjectConstant(javaClass.getComponentType());
                }
                return;
            }

            // java.lang.String
            case java_lang_String$compareTo: {
                String s1 = argAsString(args, 0);
                String s2 = argAsString(args, 1);
                if (s1 != null && s2 != null) {
                    setIntConstant(s1.compareTo(s2));
                }
                return;
            }
            case java_lang_String$indexOf: {
                String s1 = argAsString(args, 0);
                String s2 = argAsString(args, 1);
                if (s1 != null && s2 != null) {
                    setIntConstant(s1.indexOf(s2));
                }
                return;
            }
            case java_lang_String$equals: {
                String s1 = argAsString(args, 0);
                String s2 = argAsString(args, 1);
                if (s1 != null && s2 != null) {
                    setBooleanConstant(s1.equals(s2));
                }
                return;
            }

            // java.lang.Math
            case java_lang_Math$abs:   setDoubleConstant(Math.abs(argAsDouble(args, 0))); return;
            case java_lang_Math$sin:   setDoubleConstant(Math.sin(argAsDouble(args, 0))); return;
            case java_lang_Math$cos:   setDoubleConstant(Math.cos(argAsDouble(args, 0))); return;
            case java_lang_Math$tan:   setDoubleConstant(Math.tan(argAsDouble(args, 0))); return;
            case java_lang_Math$atan2: setDoubleConstant(Math.atan2(argAsDouble(args, 0), argAsDouble(args, 1))); return;
            case java_lang_Math$sqrt:  setDoubleConstant(Math.sqrt(argAsDouble(args, 0))); return;
            case java_lang_Math$log:   setDoubleConstant(Math.log(argAsDouble(args, 0))); return;
            case java_lang_Math$log10: setDoubleConstant(Math.log10(argAsDouble(args, 0))); return;
            case java_lang_Math$pow:   setDoubleConstant(Math.pow(argAsDouble(args, 0), argAsDouble(args, 1))); return;
            case java_lang_Math$exp:   setDoubleConstant(Math.exp(argAsDouble(args, 0))); return;
            case java_lang_Math$min:   setIntConstant(Math.min(argAsInt(args, 0), argAsInt(args, 1))); return;
            case java_lang_Math$max:   setIntConstant(Math.max(argAsInt(args, 0), argAsInt(args, 1))); return;

            // java.lang.Float
            case java_lang_Float$floatToRawIntBits: setIntConstant(Float.floatToRawIntBits(argAsFloat(args, 0))); return;
            case java_lang_Float$floatToIntBits: setIntConstant(Float.floatToIntBits(argAsFloat(args, 0))); return;
            case java_lang_Float$intBitsToFloat: setFloatConstant(Float.intBitsToFloat(argAsInt(args, 0))); return;

            // java.lang.Double
            case java_lang_Double$doubleToRawLongBits: setLongConstant(Double.doubleToRawLongBits(argAsDouble(args, 0))); return;
            case java_lang_Double$doubleToLongBits: setLongConstant(Double.doubleToLongBits(argAsDouble(args, 0))); return;
            case java_lang_Double$longBitsToDouble: setDoubleConstant(Double.longBitsToDouble(argAsLong(args, 0))); return;

            // java.lang.Integer
            case java_lang_Integer$bitCount: setIntConstant(Integer.bitCount(argAsInt(args, 0))); return;
            case java_lang_Integer$reverseBytes: setIntConstant(Integer.reverseBytes(argAsInt(args, 0))); return;

            // java.lang.Long
            case java_lang_Long$bitCount: setIntConstant(Long.bitCount(argAsLong(args, 0))); return;
            case java_lang_Long$reverseBytes: setLongConstant(Long.reverseBytes(argAsLong(args, 0))); return;

            // java.lang.System
            case java_lang_System$identityHashCode: {
                Object object = argAsObject(args, 0);
                if (object != null) {
                    setIntConstant(System.identityHashCode(object));
                }
                return;
            }

            // java.lang.reflect.Array
            case java_lang_reflect_Array$getLength: {
                Object object = argAsObject(args, 0);
                if (object != null && object.getClass().isArray()) {
                    setIntConstant(java.lang.reflect.Array.getLength(object));
                }
                return;
            }
        }
        assert Instruction.sameBasicType(i, _canonical);
    }

    @Override
    public void visitIf(If i) {
        if (i.x().type().isConstant()) {
            // move constant to the right
            i.swapOperands();
        }
        Instruction l = i.x();
        Instruction r = i.y();

        if (l == r) {
            // this is a comparison of x op x
            reduceReflexiveIf(i);
            return;
        }

        ValueType lt = l.type();
        ValueType rt = r.type();

        Condition ifcond = i.condition();
        if (lt.isConstant() && rt.isConstant()) {
            // fold comparisons between constants and convert to Goto
            Boolean result = ifcond.foldCondition(lt.asConstant(), rt.asConstant());
            if (result != null) {
                setCanonical(new Goto(i.successor(result), i.stateBefore(), i.isSafepoint()));
                return;
            }
        }

        if (rt.isConstant() && rt.isInt()) {
            // attempt to reduce comparisons with constant on right side
            if (l instanceof CompareOp) {
                // attempt to reduce If ((a cmp b) op const)
                reduceIfCompareOpConstant(i, rt.asConstant());
            }
        }

        if (isNullConstant(rt) && l.isNonNull()) {
            // this is a comparison of null against something that is not null
            if (ifcond == Condition.eql) {
                // new() == null is always false
                setCanonical(new Goto(i.falseSuccessor(), i.stateBefore(), i.isSafepoint()));
            } else if (ifcond == Condition.neq) {
                // new() != null is always true
                setCanonical(new Goto(i.trueSuccessor(), i.stateBefore(), i.isSafepoint()));
            }
        }
    }

    private boolean isNullConstant(ValueType rt) {
        return rt.isConstant() && rt.isObject() && rt.asConstant().asObject() == null;
    }

    private void reduceIfCompareOpConstant(If i, ConstType rtc) {
        Condition ifcond = i.condition();
        Instruction l = i.x();
        CompareOp cmp = (CompareOp) l;
        boolean unorderedIsLess = cmp.opcode() == Bytecodes.FCMPL || cmp.opcode() == Bytecodes.DCMPL;
        BlockBegin lssSucc = i.successor(ifcond.foldCondition(ConstType.forInt(-1), rtc));
        BlockBegin eqlSucc = i.successor(ifcond.foldCondition(ConstType.forInt(0), rtc));
        BlockBegin gtrSucc = i.successor(ifcond.foldCondition(ConstType.forInt(1), rtc));
        BlockBegin nanSucc = unorderedIsLess ? lssSucc : gtrSucc;
        // Note: At this point all successors (lssSucc, eqlSucc, gtrSucc, nanSucc) are
        //       equal to x->tsux() or x->fsux(). Furthermore, nanSucc equals either
        //       lssSucc or gtrSucc.
        if (lssSucc == eqlSucc && eqlSucc == gtrSucc) {
            // all successors identical => simplify to: Goto
            setCanonical(new Goto(lssSucc, i.stateBefore(), i.isSafepoint()));
        } else {
            // two successors differ and two successors are the same => simplify to: If (x cmp y)
            // determine new condition & successors
            Condition cond;
            BlockBegin tsux;
            BlockBegin fsux;
            if (lssSucc == eqlSucc) {
                cond = Condition.leq;
                tsux = lssSucc;
                fsux = gtrSucc;
            } else if (lssSucc == gtrSucc) {
                cond = Condition.neq;
                tsux = lssSucc;
                fsux = eqlSucc;
            } else if (eqlSucc == gtrSucc) {
                cond = Condition.geq;
                tsux = eqlSucc;
                fsux = lssSucc;
            } else {
                throw Util.shouldNotReachHere();
            }
            If canon = new If(cmp.x(), cond, nanSucc == tsux, cmp.y(), tsux, fsux, cmp.stateBefore(), i.isSafepoint());
            if (cmp.x() == cmp.y()) {
                // re-canonicalize the new if
                visitIf(canon);
            } else {
                setCanonical(canon);
                _canonical.setBCI(cmp.bci());
            }
        }
    }

    private void reduceReflexiveIf(If i) {
        // simplify reflexive comparisons If (x op x) to Goto
        BlockBegin succ;
        switch (i.condition()) {
            case eql: succ = i.successor(true); break;
            case neq: succ = i.successor(false); break;
            case lss: succ = i.successor(false); break;
            case leq: succ = i.successor(true); break;
            case gtr: succ = i.successor(false); break;
            case geq: succ = i.successor(true); break;
            default:
                throw Util.shouldNotReachHere();
        }
        setCanonical(new Goto(succ, i.stateBefore(), i.isSafepoint()));
    }

    @Override
    public void visitTableSwitch(TableSwitch i) {
        Instruction v = i.value();
        if (v.type().isConstant()) {
            // fold a table switch over a constant by replacing it with a goto
            int val = v.type().asConstant().asInt();
            BlockBegin succ = i.defaultSuccessor();
            if (val >= i.lowKey() && val <= i.highKey()) {
                succ = i.successors().get(val - i.lowKey());
            }
            setCanonical(new Goto(succ, i.stateBefore(), i.isSafepoint()));
            return;
        }
        int max = i.numberOfCases();
        if (max == 0) {
            // replace switch with Goto
            addInstr(v); // the value expression may produce side effects
            setCanonical(new Goto(i.defaultSuccessor(), i.stateBefore(), i.isSafepoint()));
            return;
        }
        if (max == 1) {
            // replace switch with If
            Constant key = intInstr(i.lowKey());
            setCanonical(new If(v, Condition.eql, false, key, i.successors().get(0), i.defaultSuccessor(), i.stateBefore(), i.isSafepoint()));
        }
    }

    @Override
    public void visitLookupSwitch(LookupSwitch i) {
        Instruction v = i.value();
        if (v.type().isConstant()) {
            // fold a lookup switch over a constant by replacing it with a goto
            int val = v.type().asConstant().asInt();
            BlockBegin succ = i.defaultSuccessor();
            for (int j = 0; j < i.numberOfCases(); j++) {
                if (val == i.keyAt(j)) {
                    succ = i.successors().get(j);
                    break;
                }
            }
            setCanonical(new Goto(succ, i.stateBefore(), i.isSafepoint()));
            return;
        }
        int max = i.numberOfCases();
        if (max == 1) {
            // replace switch with Goto
            addInstr(v); // the value expression may produce side effects
            setCanonical(new Goto(i.defaultSuccessor(), i.stateBefore(), i.isSafepoint()));
            return;
        }
        if (max == 2) {
            // replace switch with If
            Constant key = intInstr(i.keyAt(0));
            setCanonical(new If(v, Condition.eql, false, key, i.successors().get(0), i.defaultSuccessor(), i.stateBefore(), i.isSafepoint()));
        }
    }

    private void visitUnsafeRawOp(UnsafeRawOp i) {
        if (i.base() instanceof ArithmeticOp) {
            // if the base is an arithmetic op, try reducing
            ArithmeticOp root = (ArithmeticOp) i.base();
            if (!root.isPinned() && root.opcode() == Bytecodes.LADD) {
                // match unsafe(x + y) if the x + y is not pinned
                // try reducing (x + y) and (y + x)
                Instruction y = root.y();
                Instruction x = root.x();
                if (reduceRawOp(i, x, y) || reduceRawOp(i, y, x)) {
                    // the operation was reduced
                    return;
                }
                if (y instanceof Convert) {
                    // match unsafe(x + (long) y)
                    Convert convert = (Convert) y;
                    if (convert.opcode() == Bytecodes.I2L && convert.value().type().isInt()) {
                        // the conversion is redundant
                        setUnsafeRawOp(i, x, convert.value(), 0);
                    }
                }
            }
        }
    }

    private boolean reduceRawOp(UnsafeRawOp i, Instruction base, Instruction index) {
        if (index instanceof Convert) {
            // skip any conversion operations
            index = ((Convert) index).value();
        }
        if (index instanceof ShiftOp) {
            // try to match the index as a shift by a constant
            ShiftOp shift = (ShiftOp) index;
            ValueType st = shift.y().type();
            if (st.isConstant() && st.isInt()) {
                int val = st.asConstant().asInt();
                switch (val) {
                    case 0: // fall through
                    case 1: // fall through
                    case 2: // fall through
                    case 3: return setUnsafeRawOp(i, base, shift.x(), val);
                }
            }
        }
        if (index instanceof ArithmeticOp) {
            // try to match the index as a multiply by a constant
            // note that this case will not happen if C1XOptions.CanonicalizeMultipliesToShifts is true
            ArithmeticOp arith = (ArithmeticOp) index;
            ValueType st = arith.y().type();
            if (arith.opcode() == Bytecodes.IMUL && st.isConstant() && st.isInt()) {
                int val = st.asConstant().asInt();
                switch (val) {
                    case 1: return setUnsafeRawOp(i, base, arith.x(), 0);
                    case 2: return setUnsafeRawOp(i, base, arith.x(), 1);
                    case 4: return setUnsafeRawOp(i, base, arith.x(), 2);
                    case 8: return setUnsafeRawOp(i, base, arith.x(), 3);
                }
            }
        }

        return false;
    }

    private boolean setUnsafeRawOp(UnsafeRawOp i, Instruction base, Instruction index, int log2scale) {
        i.setBase(base);
        i.setIndex(index);
        i.setLog2Scale(log2scale);
        return true;
    }

    @Override
    public void visitUnsafeGetRaw(UnsafeGetRaw i) {
        if (C1XOptions.CanonicalizeUnsafes) {
            visitUnsafeRawOp(i);
        }
    }

    @Override
    public void visitUnsafePutRaw(UnsafePutRaw i) {
        if (C1XOptions.CanonicalizeUnsafes) {
            visitUnsafeRawOp(i);
        }
    }

    private Object argAsObject(Instruction[] args, int index) {
        return args[index].type().asConstant().asObject();
    }

    private Class<?> argAsClass(Instruction[] args, int index) {
        return (Class<?>) args[index].type().asConstant().asObject();
    }

    private String argAsString(Instruction[] args, int index) {
        return (String) args[index].type().asConstant().asObject();
    }

    private double argAsDouble(Instruction[] args, int index) {
        return args[index].type().asConstant().asDouble();
    }

    private float argAsFloat(Instruction[] args, int index) {
        return args[index].type().asConstant().asFloat();
    }

    private int argAsInt(Instruction[] args, int index) {
        return args[index].type().asConstant().asInt();
    }

    private long argAsLong(Instruction[] args, int index) {
        return args[index].type().asConstant().asLong();
    }

}
