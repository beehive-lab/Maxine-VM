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
package com.sun.c1x.alloc;

import static com.sun.c1x.util.Util.*;

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.util.*;
import com.sun.cri.ci.*;

/**
 * Represents an interval in the linear scan register allocator.
 * @author Thomas Wuerthinger
 */
public final class Interval {

    /**
     * Constants denoting the register usage requirement for an interval.
     * The constants are declared in increasing order of priority.
     */
    enum UseKind {
        NoUse,
        LoopEndMarker,
        ShouldHaveRegister,
        MustHaveRegister;

        public static final UseKind[] VALUES = values();
    }

    /**
     * Constants denoting the constraints of an interval with respect to
     * whether its location is bound to a fixed register or not. This models
     * any platform dependencies on register usage for certain instructions.
     */
    enum IntervalKind {
        /**
         * Interval has a specific register assigned to it by the platform dependent backend.
         */
        Fixed,

        /**
         * Interval has no specific register or memory address assigned to it by the platform dependent backend.
         */
        Any;

        public static final IntervalKind[] VALUES = values();
    }

    /**
     * Constants denoting the linear-scan states an interval may be in with respect to the
     * {@linkplain Interval#from() start} {@code position} of the interval being processed.
     */
    enum State {
        /**
         * An interval that starts after {@code position}.
         */
        Unhandled,

        /**
         * An interval that {@linkplain Interval#covers covers} {@code position} and has an assigned register.
         */
        Active,

        /**
         * An interval that starts before and ends after {@code position} but does not
         * {@linkplain Interval#covers cover} it due to a lifetime hole.
         */
        Inactive,

        /**
         * An interval that ends before {@code position} or is spilled to memory.
         */
        Handled;
    }

    /**
     * Constants used in optimization of spilling of an interval.
     */
    enum SpillState {
        /**
         * Starting state of calculation: no definition found yet.
         */
        NoDefinitionFound,

        /**
         * One definition has already been found. Two consecutive definitions are treated as one
         * (e.g. a consecutive move and add because of two-operand LIR form).
         * The position of this definition is given by {@link Interval#spillDefinitionPos()}.
         */
        NoSpillStore,

        /**
         * One spill move has already been inserted.
         */
        OneSpillStore,

        /**
         * The interval should be stored immediately after its definition to prevent
         * multiple redundant stores.
         */
        StoreAtDefinition,

        /**
         * The interval starts in memory (e.g. method parameter), so a store is never necessary.
         */
        StartInMemory,

        /**
         * The interval has more than one definition (e.g. resulting from phi moves), so stores
         * to memory are not optimized.
         */
        NoOptimization
    }

    /**
     * The {@linkplain CiRegisterLocation register} or {@linkplain CiVariable variable} for this interval prior to register allocation.
     */
    public final CiLocation operand;

    /**
     * The {@linkplain OperandPool#operandNumber(CiLocation) operand number} for this interval's {@linkplain #operand operand}.
     */
    public final int operandNumber;

    /**
     * The {@linkplain CiRegisterLocation register} or {@linkplain CiStackSlot spill slot} assigned to this interval during register allocation.
     */
    private CiLocation location;

    /**
     * The stack slot to which all splits of this interval are spilled if necessary.
     */
    private CiStackSlot canonicalSpillSlot;

    /**
     * The kind of this interval.
     * Only valid if this is a {@linkplain #isVariable() variable}.
     */
    private CiKind kind;

    /**
     * The head of the list of ranges describing this interval. This list is sorted by {@linkplain LIRInstruction#id instruction ids}.
     */
    private Range first;

    /**
     * List of (use-positions, use-kinds) pairs, sorted by use-positions.
     */
    private List<Integer> usePosAndKinds;

    /**
     * Iterator used to traverse the ranges of an interval.
     */
    private Range current;

    /**
     * Link to next interval in a sorted list of intervals that ends with {@link #EndMarker}.
     */
    Interval next;

    /**
     * The linear-scan state of this interval.
     */
    State state;

    private int cachedTo; // cached value: to of last range (-1: not cached)

    /**
     * The interval from which this one is derived. If this is a {@linkplain #isSplitParent() split parent}, it points to itself.
     */
    private Interval splitParent;

    /**
     * List of all intervals that are split off from this interval. This is only used if this is a {@linkplain #isSplitParent() split parent}.
     */
    private List<Interval> splitChildren = Collections.emptyList();

    /**
     * Current split child that has been active or inactive last (always stored in split parents).
     */
    private Interval currentSplitChild;

    /**
     * Specifies if move is inserted between currentSplitChild and this interval when interval gets active the first time.
     */
    private boolean insertMoveWhenActivated;

    /**
     * For spill move optimization.
     */
    private SpillState spillState;

    /**
     * Position where this interval is defined (if defined only once).
     */
    private int spillDefinitionPos;

    /**
     * This interval should be assigned the same location as the hint interval.
     */
    private Interval locationHint;

    void assignLocation(CiLocation location) {
        if (location.isRegister()) {
            assert this.location == null : "cannot re-assign location for " + this;
            if (location.kind == CiKind.Illegal && kind != CiKind.Illegal) {
                location = location.asRegister().asLocation(kind);
            }
        } else {
            assert this.location == null || this.location.isRegister() : "cannot re-assign location for " + this;
            assert location.isStackSlot();
            assert location.kind != CiKind.Illegal;
            assert location.kind == this.kind;
        }
        this.location = location;
    }

    CiLocation location() {
        return location;
    }

    CiKind kind() {
        assert !operand.isRegister() : "cannot access type for fixed interval";
        return kind;
    }

    void setKind(CiKind kind) {
        assert operand.isRegister() || this.kind == CiKind.Illegal || this.kind == kind : "overwriting existing type";
        assert kind == kind.stackKind() || kind == CiKind.Short : "these kinds should have int type registers";
        this.kind = kind;
    }

    Range first() {
        return first;
    }

    int from() {
        return first.from;
    }

    int to() {
        if (cachedTo == -1) {
            cachedTo = calcTo();
        }
        assert cachedTo == calcTo() : "invalid cached value";
        return cachedTo;
    }

    int numUsePositions() {
        return usePosAndKinds.size() / 2;
    }

    void setLocationHint(Interval interval) {
        locationHint = interval;
    }

    boolean isSplitParent() {
        return splitParent == this;
    }

    boolean isSplitChild() {
        return splitParent != this;
    }

    /**
     * Gets the split parent for this interval.
     */
    Interval splitParent() {
        assert splitParent.isSplitParent() : "not a split parent: " + this;
        return splitParent;
    }

    /**
     * Gets the canonical spill slot for this interval.
     */
    CiStackSlot canonicalSpillSlot() {
        return splitParent().canonicalSpillSlot;
    }

    void setCanonicalSpillSlot(CiStackSlot slot) {
        assert splitParent().canonicalSpillSlot == null : "overwriting existing value";
        splitParent().canonicalSpillSlot = slot;
    }

    Interval currentSplitChild() {
        return splitParent().currentSplitChild;
    }

    void makeCurrentSplitChild() {
        splitParent().currentSplitChild = this;
    }

    boolean insertMoveWhenActivated() {
        return insertMoveWhenActivated;
    }

    void setInsertMoveWhenActivated(boolean b) {
        insertMoveWhenActivated = b;
    }

    // for spill optimization
    SpillState spillState() {
        return splitParent().spillState;
    }

    int spillDefinitionPos() {
        return splitParent().spillDefinitionPos;
    }

    void setSpillState(SpillState state) {
        assert state.ordinal() >= spillState().ordinal() : "state cannot decrease";
        splitParent().spillState = state;
    }

    void setSpillDefinitionPos(int pos) {
        assert spillDefinitionPos() == -1 : "cannot set the position twice";
        splitParent().spillDefinitionPos = pos;
    }

    // returns true if this interval has a shadow copy on the stack that is always correct
    boolean alwaysInMemory() {
        return splitParent().spillState == SpillState.StoreAtDefinition || splitParent().spillState == SpillState.StartInMemory;
    }

    void removeFirstUsePos() {
        Util.truncate(usePosAndKinds, usePosAndKinds.size() - 2);
    }

    // test intersection
    boolean intersects(Interval i) {
        return first.intersects(i.first);
    }

    int intersectsAt(Interval i) {
        return first.intersectsAt(i.first);
    }

    // range iteration
    void rewindRange() {
        current = first;
    }

    void nextRange() {
        assert this != EndMarker : "not allowed on sentinel";
        current = current.next;
    }

    int currentFrom() {
        return current.from;
    }

    int currentTo() {
        return current.to;
    }

    boolean currentAtEnd() {
        return current == Range.EndMarker;
    }

    boolean currentIntersects(Interval it) {
        return current.intersects(it.current);
    }

    int currentIntersectsAt(Interval it) {
        return current.intersectsAt(it.current);
    }

    /**
     * Sentinel interval to denote the end of an interval list.
     */
    static final Interval EndMarker = new Interval(CiValue.IllegalLocation, -1);

    Interval(CiLocation operand, int operandNumber) {
        C1XMetrics.LSRAIntervalsCreated++;
        assert operand != null;
        this.operand = operand;
        this.operandNumber = operandNumber;
        if (operand.isRegister()) {
            location = operand;
        }
        this.kind = CiKind.Illegal;
        this.first = Range.EndMarker;
        this.usePosAndKinds = new ArrayList<Integer>(12);
        this.current = Range.EndMarker;
        this.next = EndMarker;
        this.cachedTo = -1;
        this.spillState = SpillState.NoDefinitionFound;
        this.spillDefinitionPos = -1;
        splitParent = this;
        currentSplitChild = this;
    }

    int calcTo() {
        assert first != Range.EndMarker : "interval has no range";

        Range r = first;
        while (r.next != Range.EndMarker) {
            r = r.next;
        }
        return r.to;
    }

    // consistency check of split-children
    boolean checkSplitChildren() {
        if (!splitChildren.isEmpty()) {
            assert isSplitParent() : "only split parents can have children";

            for (int i = 0; i < splitChildren.size(); i++) {
                Interval i1 = splitChildren.get(i);

                assert i1.splitParent() == this : "not a split child of this interval";
                assert i1.kind() == kind() : "must be equal for all split children";
                assert i1.canonicalSpillSlot() == canonicalSpillSlot() : "must be equal for all split children";

                for (int j = i + 1; j < splitChildren.size(); j++) {
                    Interval i2 = splitChildren.get(j);

                    assert i1.operand != i2.operand : "same register number";

                    if (i1.from() < i2.from()) {
                        assert i1.to() <= i2.from() && i1.to() < i2.to() : "intervals overlapping";
                    } else {
                        assert i2.from() < i1.from() : "intervals start at same opId";
                        assert i2.to() <= i1.from() && i2.to() < i1.to() : "intervals overlapping";
                    }
                }
            }
        }

        return true;
    }

    Interval locationHint(boolean searchSplitChild, LinearScan allocator) {
        if (!searchSplitChild) {
            return locationHint;
        }

        if (locationHint != null) {
            assert locationHint.isSplitParent() : "ony split parents are valid hint registers";

            if (locationHint.location != null && locationHint.location.isRegister()) {
                return locationHint;
            } else if (!locationHint.splitChildren.isEmpty()) {
                // search the first split child that has a register assigned
                int len = locationHint.splitChildren.size();
                for (int i = 0; i < len; i++) {
                    Interval interval = locationHint.splitChildren.get(i);
                    if (interval.location != null && interval.location.isRegister()) {
                        return interval;
                    }
                }
            }
        }

        // no hint interval found that has a register assigned
        return null;
    }

    Interval getSplitChildAtOpId(int opId, LIRInstruction.OperandMode mode, LinearScan allocator) {
        assert isSplitParent() : "can only be called for split parents";
        assert opId >= 0 : "invalid opId (method cannot be called for spill moves)";

        if (splitChildren.isEmpty()) {
            assert this.covers(opId, mode) : this + " does not cover " + opId;
            return this;
        } else {
            Interval result = null;
            int len = splitChildren.size();

            // in outputMode, the end of the interval (opId == cur.to()) is not valid
            int toOffset = (mode == LIRInstruction.OperandMode.OutputMode ? 0 : 1);

            int i;
            for (i = 0; i < len; i++) {
                Interval cur = splitChildren.get(i);
                if (cur.from() <= opId && opId < cur.to() + toOffset) {
                    if (i > 0) {
                        // exchange current split child to start of list (faster access for next call)
                        Util.atPutGrow(splitChildren, i, splitChildren.get(0), null);
                        Util.atPutGrow(splitChildren, 0, cur, null);
                    }

                    // interval found
                    result = cur;
                    break;
                }
            }

            assert checkSplitChild(result, opId, allocator, toOffset, mode);
            return result;
        }
    }

    private boolean checkSplitChild(Interval result, int opId, LinearScan allocator, int toOffset, LIRInstruction.OperandMode mode) {
        if (result == null) {
            // this is an error
            StringBuilder msg = new StringBuilder(this.toString()).append(" has no child at ").append(opId);
            if (!splitChildren.isEmpty()) {
                Interval first = splitChildren.get(0);
                Interval last = splitChildren.get(splitChildren.size() - 1);
                msg.append(" (first = ").append(first).append(", last = ").append(last).append(")");
            }
            throw new CiBailout("Linear Scan Error: " + msg);
        }

        if (!splitChildren.isEmpty()) {
            for (Interval interval : splitChildren) {
                if (interval != result && interval.from() <= opId && opId < interval.to() + toOffset) {
                    TTY.println(String.format("two valid result intervals found for opId %d: %d and %d", opId, result.operandNumber, interval.operandNumber));
                    result.print(TTY.out(), allocator);
                    interval.print(TTY.out(), allocator);
                    throw new CiBailout("two valid result intervals found");
                }
            }
        }
        assert result.covers(opId, mode) : "opId not covered by interval";
        return true;
    }

    // returns the last split child that ends before the given opId
    Interval getSplitChildBeforeOpId(int opId) {
        assert opId >= 0 : "invalid opId";

        Interval parent = splitParent();
        Interval result = null;

        assert !parent.splitChildren.isEmpty() : "no split children available";
        int len = parent.splitChildren.size();

        for (int i = len - 1; i >= 0; i--) {
            Interval cur = parent.splitChildren.get(i);
            if (cur.to() <= opId && (result == null || result.to() < cur.to())) {
                result = cur;
            }
        }

        assert result != null : "no split child found";
        return result;
    }

    // checks if opId is covered by any split child
    boolean splitChildCovers(int opId, LIRInstruction.OperandMode mode) {
        assert isSplitParent() : "can only be called for split parents";
        assert opId >= 0 : "invalid opId (method can not be called for spill moves)";

        if (splitChildren.isEmpty()) {
            // simple case if interval was not split
            return covers(opId, mode);

        } else {
            // extended case: check all split children
            int len = splitChildren.size();
            for (int i = 0; i < len; i++) {
                Interval cur = splitChildren.get(i);
                if (cur.covers(opId, mode)) {
                    return true;
                }
            }
            return false;
        }
    }

    // Note: use positions are sorted descending . first use has highest index
    int firstUsage(UseKind minUseKind) {
        assert isVariable() : "cannot access use positions for fixed intervals";

        for (int i = usePosAndKinds.size() - 2; i >= 0; i -= 2) {
            if (usePosAndKinds.get(i + 1) >= minUseKind.ordinal()) {
                return usePosAndKinds.get(i);
            }
        }
        return Integer.MAX_VALUE;
    }

    int nextUsage(UseKind minUseKind, int from) {
        assert isVariable() : "cannot access use positions for fixed intervals";

        for (int i = usePosAndKinds.size() - 2; i >= 0; i -= 2) {
            if (usePosAndKinds.get(i) >= from && usePosAndKinds.get(i + 1) >= minUseKind.ordinal()) {
                return usePosAndKinds.get(i);
            }
        }
        return Integer.MAX_VALUE;
    }

    int nextUsageExact(UseKind exactUseKind, int from) {
        assert isVariable() : "cannot access use positions for fixed intervals";

        for (int i = usePosAndKinds.size() - 2; i >= 0; i -= 2) {
            if (usePosAndKinds.get(i) >= from && usePosAndKinds.get(i + 1) == exactUseKind.ordinal()) {
                return usePosAndKinds.get(i);
            }
        }
        return Integer.MAX_VALUE;
    }

    int previousUsage(UseKind minUseKind, int from) {
        assert isVariable() : "cannot access use positions for fixed intervals";

        int prev = 0;
        for (int i = usePosAndKinds.size() - 2; i >= 0; i -= 2) {
            if (usePosAndKinds.get(i) > from) {
                return prev;
            }
            if (usePosAndKinds.get(i + 1) >= minUseKind.ordinal()) {
                prev = usePosAndKinds.get(i);
            }
        }
        return prev;
    }

    void addUsePos(int pos, UseKind useKind) {
        assert covers(pos, LIRInstruction.OperandMode.InputMode) : "use position not covered by live range";

        // do not add use positions for precolored intervals because they are never used
        if (useKind != UseKind.NoUse && operand.isVariable()) {
            if (C1XOptions.DetailedAsserts) {
                assert Util.isEven(usePosAndKinds.size()) : "must be even";
                for (int i = 0; i < usePosAndKinds.size(); i += 2) {
                    assert pos <= usePosAndKinds.get(i) : "already added a use-position with lower position";
                    if (i > 0) {
                        assert usePosAndKinds.get(i) < usePosAndKinds.get(i - 2) : "not sorted descending";
                    }
                }
            }

            // Note: addUse is called in descending order, so list gets sorted
            // automatically by just appending new use positions
            int len = usePosAndKinds.size();
            if (len == 0 || usePosAndKinds.get(len - 2) > pos) {
                usePosAndKinds.add(pos);
                usePosAndKinds.add(useKind.ordinal());
            } else if (usePosAndKinds.get(len - 1) < useKind.ordinal()) {
                assert usePosAndKinds.get(len - 2) == pos : "list not sorted correctly";
                usePosAndKinds.set(len - 1, useKind.ordinal());
            }
        }
    }

    void addRange(int from, int to) {
        assert from < to : "invalid range";
        assert first() == Range.EndMarker || to < first().next.from : "not inserting at begin of interval";
        assert from <= first().to : "not inserting at begin of interval";

        if (first().from <= to) {
            // join intersecting ranges
            first().from = Math.min(from, first().from);
            first().to = Math.max(to, first().to);
        } else {
            // insert new range
            first = new Range(from, to, first());
        }
    }

    Interval newSplitChild(LinearScan allocator) {
        // allocate new interval
        Interval parent = splitParent();
        Interval result = allocator.createDerivedInterval(parent);
        result.setKind(kind());

        result.splitParent = parent;
        result.setLocationHint(parent);

        // insert new interval in children-list of parent
        if (parent.splitChildren.isEmpty()) {
            assert isSplitParent() : "list must be initialized at first split";

            // Create new non-shared list
            parent.splitChildren = new ArrayList<Interval>(4);
            parent.splitChildren.add(this);
        }
        parent.splitChildren.add(result);

        return result;
    }

    // split this interval at the specified position and return
    // the remainder as a new interval.
    //
    // when an interval is split, a bi-directional link is established between the original interval
    // (the split parent) and the intervals that are split off this interval (the split children)
    // When a split child is split again, the new created interval is also a direct child
    // of the original parent (there is no tree of split children stored, but a flat list)
    // All split children are spilled to the same stack slot (stored in canonicalSpillSlot)
    //
    // Note: The new interval has no valid regNum
    Interval split(int splitPos, LinearScan allocator) {
        assert isVariable() : "cannot split fixed intervals";

        // allocate new interval
        Interval result = newSplitChild(allocator);

        // split the ranges
        Range prev = null;
        Range cur = first;
        while (cur != Range.EndMarker && cur.to <= splitPos) {
            prev = cur;
            cur = cur.next;
        }
        assert cur != Range.EndMarker : "split interval after end of last range";

        if (cur.from < splitPos) {
            result.first = new Range(splitPos, cur.to, cur.next);
            cur.to = splitPos;
            cur.next = Range.EndMarker;

        } else {
            assert prev != null : "split before start of first range";
            result.first = cur;
            prev.next = Range.EndMarker;
        }
        result.current = result.first;
        cachedTo = -1; // clear cached value

        // split list of use positions
        int totalLen = usePosAndKinds.size();
        int startIdx = totalLen - 2;
        while (startIdx >= 0 && usePosAndKinds.get(startIdx) < splitPos) {
            startIdx -= 2;
        }

        List<Integer> newUsePosAndKinds = new ArrayList<Integer>(totalLen - startIdx);
        int i;
        for (i = startIdx + 2; i < totalLen; i++) {
            newUsePosAndKinds.add(usePosAndKinds.get(i));
        }

        Util.truncate(usePosAndKinds, startIdx + 2);
        result.usePosAndKinds = usePosAndKinds;
        usePosAndKinds = newUsePosAndKinds;

        assert usePosAndKinds.size() % 2 == 0 : "must have use kind for each use pos";
        assert result.usePosAndKinds.size() % 2 == 0 : "must have use kind for each use pos";
        assert usePosAndKinds.size() + result.usePosAndKinds.size() == totalLen : "missed some entries";

        for (i = 0; i < usePosAndKinds.size(); i += 2) {
            assert usePosAndKinds.get(i) < splitPos : "must be";
            assert usePosAndKinds.get(i + 1) >= 0 && usePosAndKinds.get(i + 1) < UseKind.VALUES.length : "invalid use kind";
        }
        for (i = 0; i < result.usePosAndKinds.size(); i += 2) {
            assert result.usePosAndKinds.get(i) >= splitPos : "must be";
            assert result.usePosAndKinds.get(i + 1) >= 0 && result.usePosAndKinds.get(i + 1) < UseKind.VALUES.length : "invalid use kind";
        }

        return result;
    }

    boolean isVariable() {
        return operand.isVariable();
    }

    // split this interval at the specified position and return
    // the head as a new interval (the original interval is the tail)
    //
    // Currently, only the first range can be split, and the new interval
    // must not have split positions
    Interval splitFromStart(int splitPos, LinearScan allocator) {
        assert isVariable() : "cannot split fixed intervals";
        assert splitPos > from() && splitPos < to() : "can only split inside interval";
        assert splitPos > first.from && splitPos <= first.to : "can only split inside first range";
        assert firstUsage(UseKind.NoUse) > splitPos : "can not split when use positions are present";

        // allocate new interval
        Interval result = newSplitChild(allocator);

        // the new created interval has only one range (checked by assert on above,
        // so the splitting of the ranges is very simple
        result.addRange(first.from, splitPos);

        if (splitPos == first.to) {
            assert first.next != Range.EndMarker : "must not be at end";
            first = first.next;
        } else {
            first.from = splitPos;
        }

        return result;
    }

    // returns true if the opId is inside the interval
    boolean covers(int opId, LIRInstruction.OperandMode mode) {
        Range cur = first;

        while (cur != Range.EndMarker && cur.to < opId) {
            cur = cur.next;
        }
        if (cur != Range.EndMarker) {
            assert cur.to != cur.next.from : "ranges not separated";

            if (mode == LIRInstruction.OperandMode.OutputMode) {
                return cur.from <= opId && opId < cur.to;
            } else {
                return cur.from <= opId && opId <= cur.to;
            }
        }
        return false;
    }

    // returns true if the interval has any hole between holeFrom and holeTo
    // (even if the hole has only the length 1)
    boolean hasHoleBetween(int holeFrom, int holeTo) {
        assert holeFrom < holeTo : "check";
        assert from() <= holeFrom && holeTo <= to() : "index out of interval";

        Range cur = first;
        while (cur != Range.EndMarker) {
            assert cur.to < cur.next.from : "no space between ranges";

            // hole-range starts before this range . hole
            if (holeFrom < cur.from) {
                return true;

                // hole-range completely inside this range . no hole
            } else {
                if (holeTo <= cur.to) {
                    return false;

                    // overlapping of hole-range with this range . hole
                } else {
                    if (holeFrom <= cur.to) {
                        return true;
                    }
                }
            }

            cur = cur.next;
        }

        return false;
    }

    @Override
    public String toString() {
        String to;
        if (cachedTo == -1) {
            to = "?";
        } else {
            to = String.valueOf(to());
        }
        return operand.name() + ":" + (operand.isRegister() ? "fixed" : kind().name()) + "[" + from() + "," + to + "]";
    }

    public void print(LogStream out, LinearScan allocator) {
        print(out, allocator, false);
    }

    public void print(LogStream out, LinearScan allocator, boolean c1VisualizerFormat) {

        out.printf("%d %s ", operandNumber, (operand.isRegister() ? "fixed" : kind().name()));
        if (operand.isRegister()) {
            out.printf("\"[%s|%c]\"", operand.name(), operand.kind.typeChar);
            if (!c1VisualizerFormat) {
                out.print(' ');
            }
        } else if (location != null) {
            out.printf("\"[%s|%c]\"", location.name(), location.kind.typeChar);
            if (!c1VisualizerFormat) {
                out.print(' ');
            }
        }

        Interval hint = locationHint(false, allocator);
        out.printf("%d %d ", splitParent().operandNumber, hint != null ? hint.operandNumber : -1);

        // print ranges
        Range cur = first;
        while (cur != Range.EndMarker) {
            if (c1VisualizerFormat) {
                out.printf("[%d, %d[", cur.from, cur.to);
            } else {
                out.printf("[%d, %d] ", cur.from, cur.to);
            }
            cur = cur.next;
            assert cur != null : "range list not closed with range sentinel";
        }

        // print use positions
        int prev = 0;
        assert isEven(usePosAndKinds.size()) : "must be even";
        for (int i = usePosAndKinds.size() - 2; i >= 0; i -= 2) {
            assert prev < usePosAndKinds.get(i) : "use positions not sorted";

            out.printf("%d %s ", usePosAndKinds.get(i), UseKind.VALUES[usePosAndKinds.get(i + 1)]);
            prev = usePosAndKinds.get(i);
        }

        out.printf(" \"%s\"", spillState());
        out.println();
    }
}
