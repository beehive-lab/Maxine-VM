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
package com.sun.max.asm;

import com.sun.max.program.*;

/**
 * Assembler labels for symbolic addressing.
 *
 * This class provides combined functionality for both 32-bit and 64-bit address spaces.
 * The respective assembler narrows the usable interface to either.
 *
 * @see Assembler32
 * @see Assembler64
 *
 * @author Bernd Mathiske
 */
public final class Label implements Argument {

    public enum State {
        UNASSIGNED, BOUND, FIXED_32, FIXED_64;
    }

    protected State _state = State.UNASSIGNED;

    public static Label createBoundLabel(int position) {
        final Label label = new Label();
        label.bind(position);
        return label;
    }

    public Label() {
    }

    public State state() {
        return _state;
    }

    private int _position;

    /**
     * Must only be called when the label is bound!
     */
    public int position() throws AssemblyException {
        if (_state != State.BOUND) {
            throw new AssemblyException("unassigned or unbound label");
        }
        return _position;
    }

    /**
     * Binds this label to a position in the assembler's instruction stream that represents the start of an instruction.
     * The assembler may update the position if any emitted instructions change lengths, so that this label keeps
     * denoting the same logical instruction.
     *
     * Only to be called by {@link Assembler#bindLabel(Label)}.
     *
     * @param position
     *            an instruction's position in the assembler's instruction stream
     *
     * @see Assembler#bindLabel(Label)
     */
    void bind(int position) {
        _position = position;
        _state = State.BOUND;
    }

    void adjust(int delta) {
        assert _state == State.BOUND;
        _position += delta;
    }

    private int _address32;
    private long _address64;

    /**
     * Assigns a fixed, absolute 32-bit address to this label.
     * If used in a 64-bit assembler,
     * the effective address value would be unsigned-extended.
     *
     * @param address an absolute memory location
     *
     * @see Assembler#bindLabel(Label)
     */
    void fix32(int address32) {
        _address32 = address32;
        _state = State.FIXED_32;
    }

    /**
     * Assigns a fixed, absolute 64-bit address to this label.
     *
     * @param address an absolute memory location
     *
     * @see Assembler#bindLabel(Label)
     */
    void fix64(long address64) {
        _address64 = address64;
        _state = State.FIXED_64;
    }

    /**
     * Must only be called if this label has been {@link #fix32 fixed}.
     */
    public int address32() throws AssemblyException {
        switch (_state) {
            case FIXED_32: {
                return _address32;
            }
            case FIXED_64: {
                throw ProgramError.unexpected("64-bit address in 32-bit assembler");
            }
            default: {
                throw new AssemblyException("unassigned or unfixed label");
            }
        }
    }

    /**
     * Must only be called if this label has been {@link #fix64 fixed}.
     */
    public long address64() throws AssemblyException {
        switch (_state) {
            case FIXED_64: {
                return _address64;
            }
            case FIXED_32: {
                throw ProgramError.unexpected("32-bit address in 64-bit assembler");
            }
            default: {
                throw new AssemblyException("unassigned or unfixed label");
            }
        }
    }

    public String externalValue() {
        throw ProgramError.unexpected();
    }

    public String disassembledValue() {
        throw ProgramError.unexpected();
    }

    public long asLong() {
        Problem.unimplemented();
        return 0L;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Label) {
            final Label label = (Label) other;
            if (_state != label._state) {
                return false;
            }
            switch (_state) {
                case UNASSIGNED:
                    return this == label;
                case BOUND:
                    return _position == label._position;
                case FIXED_32:
                    return _address32 == label._address32;
                case FIXED_64:
                    return _address64 == label._address64;
                default:
                    ProgramError.unexpected();
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        switch (_state) {
            case BOUND:
                return _position;
            case FIXED_32:
                return _address32;
            case FIXED_64:
                return (int) (_address64 ^ (_address64 >> 32));
            default:
                return super.hashCode();
        }
    }

    @Override
    public String toString() {
        switch (_state) {
            case UNASSIGNED:
                return "<unassigned>";
            case BOUND:
                return _position >= 0 ? "+" + _position : String.valueOf(_position);
            case FIXED_32:
                return String.valueOf(_address32);
            case FIXED_64:
                return String.valueOf(_address64);
            default:
                throw ProgramError.unexpected();
        }
    }
}
