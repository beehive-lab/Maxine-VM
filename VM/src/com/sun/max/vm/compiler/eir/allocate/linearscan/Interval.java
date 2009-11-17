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

    private IntervalState state = IntervalState.NONE;

    public IntervalState state() {
        return state;
    }

    public void setState(IntervalState state) {
        this.state = state;
    }

    private static class Range {

        public int from;
        public int to;
        public Range next;

        public Range(int from, int to) {
            this.from = from;
            this.to = to;
            assert assertValid();
        }

        public boolean canMerge(Range r) {
            return r.to >= from && r.from <= from;
        }

        public void merge(Range r) {
            assert canMerge(r);
            from = r.from;
            assert assertValid();
        }

        public boolean biggerThan(Range r) {
            return r.to < from;
        }

        public boolean assertValid() {
            assert from < to : "Invalid range (" + from + " to " + to + ")";
            return true;
        }
    }

    public static enum UsePositionKind {
        NONE('N'), LOOP_END_MARKER('L'), SHOULD_HAVE_REGISTER('Y'), MUST_HAVE_REGISTER('X');

        private char shortCut;

        UsePositionKind(char shortCut) {
            this.shortCut = shortCut;
        }

        @Override
        public String toString() {
            return Character.toString(shortCut);
        }

        public static UsePositionKind stronger(UsePositionKind a, UsePositionKind b) {
            if (a.ordinal() > b.ordinal()) {
                return a;
            }
            return b;
        }
    }

    private static class UsePosition {

        public final int pos;
        public UsePosition next;
        public UsePositionKind kind;

        public UsePosition(int pos, UsePositionKind kind) {
            this.pos = pos;
            this.kind = kind;

            assert pos % 2 == 0 : "use positions are always at even positions";
        }

        public boolean biggerThan(UsePosition u) {
            return pos > u.pos;
        }

        @Override
        public String toString() {
            return pos + kind.toString();
        }
    }

    private boolean insertMoveWhenActivated;
    private Range firstRange;
    private ParentInterval parent;
    private UsePosition firstUsePosition;
    private Range lastRange;
    private EirVariable variable;
    private EirLocation preferredLocation;
    private EirRegister register;

    Interval(ParentInterval parent, EirVariable variable) {
        this.parent = parent;
        this.variable = variable;
        if (variable.location() != null) {
            this.register = variable.location().asRegister();
        }
    }

    public EirVariable variable() {
        return variable;
    }

    public ParentInterval parent() {
        return parent;
    }

    public EirLocation preferredLocation() {
        return preferredLocation;
    }

    public void setPreferredLocation(EirLocation location) {
        preferredLocation = location;
    }

    public void prependRange(int beginNumber, int endNumber) {
        final Range r = new Range(beginNumber, endNumber);
        if (firstRange == null) {
            firstRange = r;
            lastRange = r;
        } else if (firstRange.canMerge(r)) {
            firstRange.merge(r);
        } else {
            assert firstRange.biggerThan(r);
            r.next = firstRange;
            firstRange = r;
        }
    }

    public void addUsePosition(int pos, UsePositionKind kind) {
        final UsePosition u = new UsePosition(pos, kind);

        if (firstUsePosition != null && firstUsePosition.pos == pos) {
            firstUsePosition.kind = UsePositionKind.stronger(kind, firstUsePosition.kind);
        } else {
            assert firstUsePosition == null || firstUsePosition.biggerThan(u);
            u.next = firstUsePosition;
            firstUsePosition = u;
        }
    }

    public boolean isEmpty() {
        return firstRange == null;
    }

    public int getFirstRangeStart() {
        assert !isEmpty();
        return firstRange.from;
    }

    private int getFirstRangeEnd() {
        assert !isEmpty();
        return firstRange.to;
    }

    public int getLastRangeEnd() {
        assert !isEmpty();
        return lastRange.to;
    }

    public String detailedToString() {

        final StringBuilder result = new StringBuilder();
        result.append(toString() + ": ");
        Range curRange = firstRange;
        while (curRange != null) {
            result.append("[" + curRange.from + ", " + curRange.to + "[ ");
            curRange = curRange.next;
        }

        UsePosition curUsePosition = firstUsePosition;
        while (curUsePosition != null) {
            result.append(curUsePosition.toString() + " ");

            curUsePosition = curUsePosition.next;
        }

        return result.toString();
    }

    public void print(IndentWriter writer, int length) {

        UsePosition curUsePosition = firstUsePosition;
        Range curRange = firstRange;

        boolean inInterval = false;

        for (int i = 0; i < length; i++) {

            if (curRange != null && curRange.from == i) {
                inInterval = true;
            }

            if (curRange != null && curRange.to == i) {
                inInterval = false;
                curRange = curRange.next;
            }

            if (curUsePosition != null && curUsePosition.pos == i) {

                writer.print(curUsePosition.kind.toString());
                curUsePosition = curUsePosition.next;

            } else if (inInterval) {
                writer.print("-");
            } else {
                writer.print(" ");
            }
        }
    }

    public EirRegister register() {
        assert (variable().location() == null && register == null) || variable().location().asRegister() == register;
        return this.register;
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

        firstRange.from = pos;
        assert firstRange.assertValid();
    }

    public boolean coversEndInclusive(int position) {
        Range cur = firstRange;
        while (cur != null) {
            if (cur.from <= position && cur.to >= position) {
                return true;
            }
            cur = cur.next;
        }

        return false;

    }

    Range lastCoverRange;

    /**
     * Must only be called with increasing position!
     *
     * @param position
     * @return
     */
    public boolean coversIncremental(int position) {

        assert lastCoverRange == null || lastCoverRange.to <= lastRange.to;

        Range cur = lastCoverRange;
        if (cur == null) {
            cur = firstRange;
        }

        while (cur != null && cur.from <= position) {
            if (cur.to > position) {
                lastCoverRange = cur;
                assert covers(position);
                return true;
            }
            cur = cur.next;
        }

        assert !covers(position);
        lastCoverRange = cur;
        return false;
    }

    public boolean covers(int position) {
        Range cur = firstRange;
        while (cur != null && cur.from <= position) {
            if (cur.to > position) {
                return true;
            }
            cur = cur.next;
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

        return firstIntersection(firstRange, current.firstRange);
    }

    public int firstIntersectionIncremental(int position, Interval current) {

        if (isEmpty() || current.isEmpty()) {
            return -1;
        }

        final int result = firstIntersection((lastCoverRange == null) ? firstRange : lastCoverRange, (current.lastCoverRange == null) ? current.firstRange : current.lastCoverRange);
        assert result == firstIntersection(position, current);
        return result;
    }

    public int firstIntersection(Range startR1, Range startR2) {
        Range r1 = startR1;
        Range r2 = startR2;

        do {

            if (r1.from < r2.from) {
                if (r1.to <= r2.from) {
                    // Move r1 on
                    r1 = r1.next;
                } else {
                    return r2.from;
                }
            } else if (r2.from < r1.from) {
                if (r2.to <= r1.from) {
                    // Move r2 on
                    r2 = r2.next;
                } else {
                    return r1.from;
                }
            } else {
                assert r1.from == r2.from;
                return r1.from;
            }

        } while (r1 != null && r2 != null);

        return -1;
    }

    public void assignRegister(EirRegister eirRegister) {
        assert variable().location() == null;
        this.variable().setLocation(eirRegister);
        register = eirRegister;
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
        UsePosition cur = firstUsePosition;
        UsePosition last = null;
        while (cur != null && cur.pos < position) {

            if (cur.kind == UsePositionKind.SHOULD_HAVE_REGISTER || cur.kind == UsePositionKind.MUST_HAVE_REGISTER) {
                last = cur;
            }

            cur = cur.next;
        }

        if (last == null) {
            return -1;
        }

        assert last.kind == UsePositionKind.SHOULD_HAVE_REGISTER || last.kind == UsePositionKind.MUST_HAVE_REGISTER;
        return last.pos;
    }

    public int nextMustHaveRegister(int minPos) {
        UsePosition cur = firstUsePosition;

        while (cur != null) {

            if (cur.pos >= minPos && cur.kind == UsePositionKind.MUST_HAVE_REGISTER) {
                return cur.pos;
            }

            cur = cur.next;
        }

        return Integer.MAX_VALUE;
    }

    private boolean assertLastCoverRange() {

        if (lastCoverRange == null) {
            return true;
        }

        Range cur = firstRange;
        while (cur != null) {
            if (cur == lastCoverRange) {
                return true;
            }
            cur = cur.next;
        }

        return false;
    }

    public Interval split(int pos, EirVariable variable) {
        assert !this.isFixed() : "cannot split fixed intervals";
        assert pos > this.getFirstRangeStart() && pos < this.getLastRangeEnd() : "can only split inside interval";

        assert assertRangeValid();

        final Interval result = parent().createChild(variable);
        Range prev = null;
        Range cur = firstRange;
        while (cur != null && cur.to <= pos) {
            prev = cur;
            cur = cur.next;
        }

        assert cur != null : "split interval after end of last range";

        if (cur.from < pos) {
            result.firstRange = new Range(pos, cur.to);
            result.firstRange.next = cur.next;
            if (cur == lastRange) {
                result.lastRange = result.firstRange;
            } else {
                result.lastRange = lastRange;
            }

            cur.to = pos;
            cur.next = null;
            lastRange = cur;
        } else {
            assert prev != null : "split before start of first range";
            result.firstRange = cur;
            result.lastRange = lastRange;
            prev.next = null;
            lastRange = prev;
        }

        if (this.lastCoverRange != null && this.lastCoverRange.to > this.lastRange.to) {
            result.lastCoverRange = this.lastCoverRange;
            this.lastCoverRange = this.lastRange;
        }

        assert assertLastCoverRange();
        assert result.assertLastCoverRange();

        UsePosition curUsePosition = this.firstUsePosition;
        UsePosition prevUsePosition = null;
        while (curUsePosition != null && curUsePosition.pos < pos) {
            prevUsePosition = curUsePosition;
            curUsePosition = curUsePosition.next;
        }

        result.firstUsePosition = curUsePosition;
        if (prevUsePosition == null) {
            this.firstUsePosition = null;
        } else {
            prevUsePosition.next = null;
        }

        assert assertValid();
        assert result.assertValid();
        assert getLastRangeEnd() <= pos && result.getFirstRangeStart() >= pos;

        return result;
    }

    public boolean assertUsePositionsValid() {
        UsePosition cur = firstUsePosition;
        while (cur != null) {
            assert this.coversEndInclusive(cur.pos);
            cur = cur.next;
        }

        return true;
    }

    public boolean assertValid() {
        assert assertUsePositionsValid();
        assert assertRangeValid();
        return true;
    }

    public boolean assertRangeValid() {

        assert firstRange != null || lastRange == null;

        if (firstRange != null) {
            Range cur = firstRange;
            Range prev = null;
            while (cur != null) {

                if (prev != null) {
                    assert !cur.canMerge(prev);
                }

                prev = cur;
                cur = cur.next;
            }

            assert prev == lastRange;
        }

        return true;
    }

    public boolean insertMoveWhenActivated() {
        return insertMoveWhenActivated;
    }

    public void setInsertMoveWhenActivated(boolean b) {
        insertMoveWhenActivated = b;
    }

    public int nextUsageAfter(int minPos) {
        UsePosition cur = firstUsePosition;

        while (cur != null) {

            if (cur.pos >= minPos) {
                return cur.pos;
            }

            cur = cur.next;
        }

        return Integer.MAX_VALUE;
    }

    public int firstUsage() {

        // TODO: Check if this is a hack...
        if (firstUsePosition == null) {
            return this.getLastRangeEnd();
        }
        assert firstUsePosition != null;
        return firstUsePosition.pos;
    }

    public void assignStackSlot(EirStackSlot stackSlot) {
        variable().setLocation(stackSlot);
        register = null;
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
                return this.stackSlot().offset - other.stackSlot().offset;
            }
            return 1;
        }

        if (this.register() != null) {

            final int registerOrdial1 = this.register().ordinal + this.register().category().ordinal() * 100;
            if (other.register() != null) {
                final int registerOrdial2 = other.register().ordinal + (other.register().category().ordinal() * 100);
                return registerOrdial2 - registerOrdial1;
            }
            return 1;
        }

        return 0;
    }
}
