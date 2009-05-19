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
package com.sun.max.vm.compiler.eir.allocate.linearscan;

import com.sun.max.io.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.type.*;

/**
 * An live interval consisting of multiple ranges and multiple use positions of a variable.
 *
 * @author Thomas Wuerthinger
 */
public final class Interval {

    public enum IntervalState {
        NONE, UNHANDLED, ACTIVE, INACTIVE, HANDLED
    }

    private IntervalState _state = IntervalState.NONE;

    public IntervalState state() {
        return _state;
    }

    public void setState(IntervalState state) {
        _state = state;
    }

    private static class Range {

        public int _from;
        public int _to;
        public Range _next;

        public Range(int from, int to) {
            _from = from;
            _to = to;
            assert assertValid();
        }

        public boolean canMerge(Range r) {
            return r._to >= _from && r._from <= _from;
        }

        public void merge(Range r) {
            assert canMerge(r);
            _from = r._from;
            assert assertValid();
        }

        public boolean biggerThan(Range r) {
            return r._to < _from;
        }

        public boolean assertValid() {
            assert _from < _to : "Invalid range (" + _from + " to " + _to + ")";
            return true;
        }
    }

    public static enum UsePositionKind {
        NONE('N'), LOOP_END_MARKER('L'), SHOULD_HAVE_REGISTER('Y'), MUST_HAVE_REGISTER('X');

        private char _shortCut;

        UsePositionKind(char shortCut) {
            _shortCut = shortCut;
        }

        @Override
        public String toString() {
            return Character.toString(_shortCut);
        }

        public static UsePositionKind stronger(UsePositionKind a, UsePositionKind b) {
            if (a.ordinal() > b.ordinal()) {
                return a;
            }
            return b;
        }
    }

    private static class UsePosition {

        public int _pos;
        public UsePosition _next;
        public UsePositionKind _kind;

        public UsePosition(int pos, UsePositionKind kind) {
            _pos = pos;
            _kind = kind;

            assert pos % 2 == 0 : "use positions are always at even positions";
        }

        public boolean biggerThan(UsePosition u) {
            return _pos > u._pos;
        }

        @Override
        public String toString() {
            return _pos + _kind.toString();
        }
    }

    private boolean _insertMoveWhenActivated;
    private Range _firstRange;
    private ParentInterval _parent;
    private UsePosition _firstUsePosition;
    private Range _lastRange;
    private EirVariable _variable;
    private EirLocation _preferredLocation;
    private EirRegister _register;

    Interval(ParentInterval parent, EirVariable variable) {
        _parent = parent;
        _variable = variable;
        if (variable.location() != null) {
            _register = variable.location().asRegister();
        }
    }

    public EirVariable variable() {
        return _variable;
    }

    public ParentInterval parent() {
        return _parent;
    }

    public EirLocation preferredLocation() {
        return _preferredLocation;
    }

    public void setPreferredLocation(EirLocation location) {
        _preferredLocation = location;
    }

    public void prependRange(int beginNumber, int endNumber) {
        final Range r = new Range(beginNumber, endNumber);
        if (_firstRange == null) {
            _firstRange = r;
            _lastRange = r;
        } else if (_firstRange.canMerge(r)) {
            _firstRange.merge(r);
        } else {
            assert _firstRange.biggerThan(r);
            r._next = _firstRange;
            _firstRange = r;
        }
    }

    public void addUsePosition(int pos, UsePositionKind kind) {
        final UsePosition u = new UsePosition(pos, kind);

        if (_firstUsePosition != null && _firstUsePosition._pos == pos) {
            _firstUsePosition._kind = UsePositionKind.stronger(kind, _firstUsePosition._kind);
        } else {
            assert _firstUsePosition == null || _firstUsePosition.biggerThan(u);
            u._next = _firstUsePosition;
            _firstUsePosition = u;
        }
    }

    public boolean isEmpty() {
        return _firstRange == null;
    }

    public int getFirstRangeStart() {
        assert !isEmpty();
        return _firstRange._from;
    }

    private int getFirstRangeEnd() {
        assert !isEmpty();
        return _firstRange._to;
    }

    public int getLastRangeEnd() {
        assert !isEmpty();
        return _lastRange._to;
    }

    public String detailedToString() {

        final StringBuilder result = new StringBuilder();
        result.append(toString() + ": ");
        Range curRange = _firstRange;
        while (curRange != null) {
            result.append("[" + curRange._from + ", " + curRange._to + "[ ");
            curRange = curRange._next;
        }

        UsePosition curUsePosition = _firstUsePosition;
        while (curUsePosition != null) {
            result.append(curUsePosition.toString() + " ");

            curUsePosition = curUsePosition._next;
        }

        return result.toString();
    }

    public void print(IndentWriter writer, int length) {

        UsePosition curUsePosition = _firstUsePosition;
        Range curRange = _firstRange;

        boolean inInterval = false;

        for (int i = 0; i < length; i++) {

            if (curRange != null && curRange._from == i) {
                inInterval = true;
            }

            if (curRange != null && curRange._to == i) {
                inInterval = false;
                curRange = curRange._next;
            }

            if (curUsePosition != null && curUsePosition._pos == i) {

                writer.print(curUsePosition._kind.toString());
                curUsePosition = curUsePosition._next;

            } else if (inInterval) {
                writer.print("-");
            } else {
                writer.print(" ");
            }
        }
    }

    public EirRegister register() {
        assert (variable().location() == null && _register == null) || variable().location().asRegister() == _register;
        return this._register;
    }

    public EirStackSlot stackSlot() {
        final EirLocation location = variable().location();
        if (location == null || !(location instanceof EirStackSlot)) {
            return null;
        }
        assert location instanceof EirStackSlot;
        return (EirStackSlot) location;
    }

    public void cutTo(int pos) {
        assert !isEmpty();
        assert getFirstRangeStart() <= pos;
        assert getFirstRangeEnd() > pos;

        _firstRange._from = pos;
        assert _firstRange.assertValid();
    }

    public boolean coversEndInclusive(int position) {
        Range cur = _firstRange;
        while (cur != null) {
            if (cur._from <= position && cur._to >= position) {
                return true;
            }
            cur = cur._next;
        }

        return false;

    }

    Range _lastCoverRange;

    /**
     * Must only be called with increasing position!
     *
     * @param position
     * @return
     */
    public boolean coversIncremental(int position) {

        assert _lastCoverRange == null || _lastCoverRange._to <= _lastRange._to;

        Range cur = _lastCoverRange;
        if (cur == null) {
            cur = _firstRange;
        }

        while (cur != null && cur._from <= position) {
            if (cur._to > position) {
                _lastCoverRange = cur;
                assert covers(position);
                return true;
            }
            cur = cur._next;
        }

        assert !covers(position);
        _lastCoverRange = cur;
        return false;
    }

    public boolean covers(int position) {
        Range cur = _firstRange;
        while (cur != null && cur._from <= position) {
            if (cur._to > position) {
                return true;
            }
            cur = cur._next;
        }

        return false;
    }

    public boolean needsFloatingPointRegister() {
        return variable().locationCategories().contains(EirLocationCategory.FLOATING_POINT_REGISTER) && (variable().kind() == Kind.FLOAT || variable().kind() == Kind.DOUBLE);
    }

    public boolean needsIntegerRegister() {
        return variable().locationCategories().contains(EirLocationCategory.INTEGER_REGISTER);
    }

    public int firstIntersection(int position, Interval current) {

        if (isEmpty() || current.isEmpty()) {
            return -1;
        }

        return firstIntersection(_firstRange, current._firstRange);
    }

    public int firstIntersectionIncremental(int position, Interval current) {

        if (isEmpty() || current.isEmpty()) {
            return -1;
        }

        final int result = firstIntersection((_lastCoverRange == null) ? _firstRange : _lastCoverRange, (current._lastCoverRange == null) ? current._firstRange : current._lastCoverRange);
        assert result == firstIntersection(position, current);
        return result;
    }

    public int firstIntersection(Range startR1, Range startR2) {
        Range r1 = startR1;
        Range r2 = startR2;

        do {

            if (r1._from < r2._from) {
                if (r1._to <= r2._from) {
                    // Move r1 on
                    r1 = r1._next;
                } else {
                    return r2._from;
                }
            } else if (r2._from < r1._from) {
                if (r2._to <= r1._from) {
                    // Move r2 on
                    r2 = r2._next;
                } else {
                    return r1._from;
                }
            } else {
                assert r1._from == r2._from;
                return r1._from;
            }

        } while (r1 != null && r2 != null);

        return -1;
    }

    public void assignRegister(EirRegister eirRegister) {
        assert variable().location() == null;
        this.variable().setLocation(eirRegister);
        _register = eirRegister;
        assert this.variable().assertLocationCategory() : "incorrect register category";
    }

    public boolean isFixed() {
        final boolean result = variable().isLocationFixed();
        assert !result || variable().location() != null;
        return result;
    }

    @Override
    public String toString() {
        return variable().toString();
    }

    public int previousShouldHaveRegister(int position) {
        UsePosition cur = _firstUsePosition;
        UsePosition last = null;
        while (cur != null && cur._pos < position) {

            if (cur._kind == UsePositionKind.SHOULD_HAVE_REGISTER || cur._kind == UsePositionKind.MUST_HAVE_REGISTER) {
                last = cur;
            }

            cur = cur._next;
        }

        if (last == null) {
            return -1;
        }

        assert last._kind == UsePositionKind.SHOULD_HAVE_REGISTER || last._kind == UsePositionKind.MUST_HAVE_REGISTER;
        return last._pos;
    }

    public int nextMustHaveRegister(int minPos) {
        UsePosition cur = _firstUsePosition;

        while (cur != null) {

            if (cur._pos >= minPos && cur._kind == UsePositionKind.MUST_HAVE_REGISTER) {
                return cur._pos;
            }

            cur = cur._next;
        }

        return Integer.MAX_VALUE;
    }

    private boolean assertLastCoverRange() {

        if (_lastCoverRange == null) {
            return true;
        }

        Range cur = _firstRange;
        while (cur != null) {
            if (cur == _lastCoverRange) {
                return true;
            }
            cur = cur._next;
        }

        return false;
    }

    public Interval split(int pos, EirVariable variable) {
        assert !this.isFixed() : "cannot split fixed intervals";
        assert pos > this.getFirstRangeStart() && pos < this.getLastRangeEnd() : "can only split inside interval";

        assert assertRangeValid();

        final Interval result = parent().createChild(variable);
        Range prev = null;
        Range cur = _firstRange;
        while (cur != null && cur._to <= pos) {
            prev = cur;
            cur = cur._next;
        }

        assert cur != null : "split interval after end of last range";

        if (cur._from < pos) {
            result._firstRange = new Range(pos, cur._to);
            result._firstRange._next = cur._next;
            if (cur == _lastRange) {
                result._lastRange = result._firstRange;
            } else {
                result._lastRange = _lastRange;
            }

            cur._to = pos;
            cur._next = null;
            _lastRange = cur;
        } else {
            assert prev != null : "split before start of first range";
            result._firstRange = cur;
            result._lastRange = _lastRange;
            prev._next = null;
            _lastRange = prev;
        }

        if (this._lastCoverRange != null && this._lastCoverRange._to > this._lastRange._to) {
            result._lastCoverRange = this._lastCoverRange;
            this._lastCoverRange = this._lastRange;
        }

        assert assertLastCoverRange();
        assert result.assertLastCoverRange();

        UsePosition curUsePosition = this._firstUsePosition;
        UsePosition prevUsePosition = null;
        while (curUsePosition != null && curUsePosition._pos < pos) {
            prevUsePosition = curUsePosition;
            curUsePosition = curUsePosition._next;
        }

        result._firstUsePosition = curUsePosition;
        if (prevUsePosition == null) {
            this._firstUsePosition = null;
        } else {
            prevUsePosition._next = null;
        }

        assert assertValid();
        assert result.assertValid();
        assert getLastRangeEnd() <= pos && result.getFirstRangeStart() >= pos;

        return result;
    }

    public boolean assertUsePositionsValid() {
        UsePosition cur = _firstUsePosition;
        while (cur != null) {
            assert this.coversEndInclusive(cur._pos);
            cur = cur._next;
        }

        return true;
    }

    public boolean assertValid() {
        assert assertUsePositionsValid();
        assert assertRangeValid();
        return true;
    }

    public boolean assertRangeValid() {

        assert _firstRange != null || _lastRange == null;

        if (_firstRange != null) {
            Range cur = _firstRange;
            Range prev = null;
            while (cur != null) {

                if (prev != null) {
                    assert !cur.canMerge(prev);
                }

                prev = cur;
                cur = cur._next;
            }

            assert prev == _lastRange;
        }

        return true;
    }

    public boolean insertMoveWhenActivated() {
        return _insertMoveWhenActivated;
    }

    public void setInsertMoveWhenActivated(boolean b) {
        _insertMoveWhenActivated = b;
    }

    public int nextUsageAfter(int minPos) {
        UsePosition cur = _firstUsePosition;

        while (cur != null) {

            if (cur._pos >= minPos) {
                return cur._pos;
            }

            cur = cur._next;
        }

        return Integer.MAX_VALUE;
    }

    public int firstUsage() {

        // TODO: Check if this is a hack...
        if (_firstUsePosition == null) {
            return this.getLastRangeEnd();
        }
        assert _firstUsePosition != null;
        return _firstUsePosition._pos;
    }

    public void assignStackSlot(EirStackSlot stackSlot) {
        variable().setLocation(stackSlot);
        _register = null;
        assert this.variable().assertLocationCategory() : "cannot assign stack slot to this variable";
    }


    public boolean intersectsIncremental(Interval current) {
        return this.firstIntersectionIncremental(0, current) != -1;
    }

    public boolean intersects(Interval current) {
        return this.firstIntersection(0, current) != -1;
    }

    public boolean needsStackSlot() {
        return variable().locationCategories().contains(EirLocationCategory.STACK_SLOT);
    }

    public int compareLocation(Interval other) {

        if (this.register() == null && other.register() != null) {
            return 1;
        }

        if (other.register() == null && this.register() != null) {
            return -1;
        }

        if (this.stackSlot() != null) {
            if (other.stackSlot() != null) {
                return this.stackSlot().offset() - other.stackSlot().offset();
            }
            return 1;
        }

        if (this.register() != null) {

            final int registerOrdial1 = this.register().ordinal() + this.register().category().ordinal() * 100;
            if (other.register() != null) {
                final int registerOrdial2 = other.register().ordinal() + (other.register().category().ordinal() * 100);
                return registerOrdial2 - registerOrdial1;
            }
            return 1;
        }

        return 0;
    }
}
