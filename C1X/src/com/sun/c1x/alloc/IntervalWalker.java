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

import com.sun.c1x.*;
import com.sun.c1x.ci.CiBailout;
import com.sun.c1x.alloc.Interval.*;
import com.sun.c1x.debug.*;

/**
 *
 * @author Thomas Wuerthinger
 */
public class IntervalWalker {

    private static final IntervalKind[] INTERVAL_KINDS = IntervalKind.values();

    protected final C1XCompilation compilation;
    protected final LinearScan allocator;

    Interval[] unhandledFirst = new Interval[INTERVAL_KINDS.length]; // sorted list of intervals, not live before
                                                                     // the current position
    Interval[] activeFirst = new Interval[INTERVAL_KINDS.length];    // sorted list of intervals, live
                                                                     // at the current position
    Interval[] inactiveFirst = new Interval[INTERVAL_KINDS.length];  // sorted list of intervals, intervals in a life
                                                                     // time hole at the current position

    protected Interval current; // the current interval coming from unhandled list
    protected int currentPosition; // the current position (intercept point through the intervals)
    protected IntervalKind currentKind; // and whether it is IntervalKind.fixedKind or IntervalKind.anyKind.

    boolean activateCurrent() {
        // activateCurrent() is called when an unhandled interval becomes active (in current(), currentKind()).
        // Return false if current() should not be moved the the active interval list.
        // It is safe to append current to any interval list but the unhandled list.
        return true;
    }

    Interval unhandledFirst(IntervalKind kind) {
        return unhandledFirst[kind.ordinal()];
    }

    Interval activeFirst(IntervalKind kind) {
        return activeFirst[kind.ordinal()];
    }

    Interval inactiveFirst(IntervalKind kind) {
        return inactiveFirst[kind.ordinal()];
    }

    void walkBefore(int lirOpId) {
        walkTo(lirOpId - 1);
    }

    void walk() {
        walkTo(Integer.MAX_VALUE);
    }

    IntervalWalker(LinearScan allocator, Interval unhandledFixedFirst, Interval unhandledAnyFirst) {
        this.compilation = allocator.compilation;
        this.allocator = allocator;

        unhandledFirst[IntervalKind.FixedKind.ordinal()] = unhandledFixedFirst;
        unhandledFirst[IntervalKind.AnyKind.ordinal()] = unhandledAnyFirst;
        activeFirst[IntervalKind.FixedKind.ordinal()] = Interval.EndMarker;
        inactiveFirst[IntervalKind.FixedKind.ordinal()] = Interval.EndMarker;
        activeFirst[IntervalKind.AnyKind.ordinal()] = Interval.EndMarker;
        inactiveFirst[IntervalKind.AnyKind.ordinal()] = Interval.EndMarker;
        currentPosition = -1;
        current = null;
        nextInterval();
    }

    // append interval in order of current range from()
    Interval appendSorted(Interval list, Interval interval) {
        Interval prev = null;
        Interval cur = list;
        while (cur.currentFrom() < interval.currentFrom()) {
            prev = cur;
            cur = cur.next;
        }
        Interval result = list;
        if (prev == null) {
            result = interval;
        } else {
            prev.next = interval;
        }
        interval.next = cur;
        return result;
    }

    Interval appendToUnhandled(Interval list, Interval interval) {
        assert interval.from() >= current.currentFrom() : "cannot append new interval before current walk position";

        Interval prev = null;
        Interval cur = list;
        while (cur.from() < interval.from() || (cur.from() == interval.from() && cur.firstUsage(IntervalUseKind.NoUse) < interval.firstUsage(IntervalUseKind.NoUse))) {
            prev = cur;
            cur = cur.next;
        }
        if (prev == null) {
            list = interval;
        } else {
            prev.next = interval;
        }
        interval.next = cur;

        return list;
    }

    Interval removeFromList(Interval list, Interval i) {
        Interval prev = null;
        Interval cur = list;
        while (cur != Interval.EndMarker && cur != i) {
            prev = cur;
            cur = cur.next;
        }
        if (cur != Interval.EndMarker) {
            assert cur == i : "check";
            if (prev == null) {
                return cur.next;
            } else {
                prev.next = cur.next;
                return list;
            }
        }

        throw new CiBailout("interval has not been found in list");
    }

    void removeFromList(Interval i) {
        if (i.state == IntervalState.Active) {
            activeFirst[IntervalKind.AnyKind.ordinal()] = removeFromList(activeFirst(IntervalKind.AnyKind), i);
        } else {
            assert i.state == IntervalState.Inactive : "invalid state";
            inactiveFirst[IntervalKind.AnyKind.ordinal()] = removeFromList(inactiveFirst(IntervalKind.AnyKind), i);
        }
    }

    void walkTo(IntervalState state, int from) {
        assert state == IntervalState.Active || state == IntervalState.Inactive : "wrong state";
        for (IntervalKind kind : INTERVAL_KINDS) {
            Interval prevprev = null;
            Interval prev = (state == IntervalState.Active) ? activeFirst(kind) : inactiveFirst(kind);
            Interval next = prev;
            while (next.currentFrom() <= from) {
                Interval cur = next;
                next = cur.next;

                boolean rangeHasChanged = false;
                while (cur.currentTo() <= from) {
                    cur.nextRange();
                    rangeHasChanged = true;
                }

                // also handle move from inactive list to active list
                rangeHasChanged = rangeHasChanged || (state == IntervalState.Inactive && cur.currentFrom() <= from);

                if (rangeHasChanged) {
                    // remove cur from list
                    if (prevprev == null) {
                        if (state == IntervalState.Active) {
                            activeFirst[kind.ordinal()] = next;
                        } else {
                            inactiveFirst[kind.ordinal()] = next;
                        }
                    } else {
                        prevprev.next = next;
                    }
                    prev = next;
                    if (cur.currentAtEnd()) {
                        // move to handled state (not maintained as a list)
                        cur.state = IntervalState.Handled;
                        intervalMoved(cur, kind, state, IntervalState.Handled);
                    } else if (cur.currentFrom() <= from) {
                        // sort into active list
                        activeFirst[kind.ordinal()] = appendSorted(activeFirst(kind), cur);
                        cur.state = IntervalState.Active;
                        if (prev == cur) {
                            assert state == IntervalState.Active : "check";
                            prevprev = prev;
                            prev = cur.next;
                        }
                        intervalMoved(cur, kind, state, IntervalState.Active);
                    } else {
                        // sort into inactive list
                        inactiveFirst[kind.ordinal()] = appendSorted(inactiveFirst(kind), cur);
                        cur.state = IntervalState.Inactive;
                        if (prev == cur) {
                            assert state == IntervalState.Inactive : "check";
                            prevprev = prev;
                            prev = cur.next;
                        }
                        intervalMoved(cur, kind, state, IntervalState.Inactive);
                    }
                } else {
                    prevprev = prev;
                    prev = cur.next;
                }
            }
        }
    }

    void nextInterval() {
        IntervalKind kind;
        Interval any = unhandledFirst[IntervalKind.AnyKind.ordinal()];
        Interval fixed = unhandledFirst[IntervalKind.FixedKind.ordinal()];

        if (any != Interval.EndMarker) {
            // intervals may start at same position . prefer fixed interval
            kind = fixed != Interval.EndMarker && fixed.from() <= any.from() ? IntervalKind.FixedKind : IntervalKind.AnyKind;

            assert kind == IntervalKind.FixedKind && fixed.from() <= any.from() || kind == IntervalKind.AnyKind && any.from() <= fixed.from() : "wrong interval!!!";
            assert any == Interval.EndMarker || fixed == Interval.EndMarker || any.from() != fixed.from() || kind == IntervalKind.FixedKind : "if fixed and any-Interval start at same position, fixed must be processed first";

        } else if (fixed != Interval.EndMarker) {
            kind = IntervalKind.FixedKind;
        } else {
            current = null;
            return;
        }
        currentKind = kind;
        current = unhandledFirst[kind.ordinal()];
        unhandledFirst[kind.ordinal()] = current.next;
        current.next = Interval.EndMarker;
        current.rewindRange();
    }

    void walkTo(int lirOpId) {
        assert currentPosition <= lirOpId : "can not walk backwards";
        while (current != null) {
            boolean isActive = current.from() <= lirOpId;
            int id = isActive ? current.from() : lirOpId;

            if (C1XOptions.TraceLinearScanLevel >= 2) {
                if (currentPosition < id) {
                    TTY.println();
                    TTY.println("walkTo(%d) *", id);
                }
            }

            // set currentPosition prior to call of walkTo
            currentPosition = id;

            // call walkTo even if currentPosition == id
            walkTo(IntervalState.Active, id);
            walkTo(IntervalState.Inactive, id);

            if (isActive) {
                current.state = IntervalState.Active;
                if (activateCurrent()) {
                    activeFirst[currentKind.ordinal()] = appendSorted(activeFirst(currentKind), current);
                    intervalMoved(current, currentKind, IntervalState.Unhandled, IntervalState.Active);
                }

                nextInterval();
            } else {
                return;
            }
        }
    }

    void intervalMoved(Interval interval, IntervalKind kind, IntervalState from, IntervalState to) {
        // intervalMoved() is called whenever an interval moves from one interval list to another.
        // In the implementation of this method it is prohibited to move the interval to any list.
        if (C1XOptions.TraceLinearScanLevel >= 4) {
            TTY.print(from.toString() + " to " + to.toString());
            TTY.fillTo(23);
            interval.print(TTY.out(), allocator);
        }
    }
}
