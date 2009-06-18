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
import com.sun.c1x.util.InstructionVisitor;
import com.sun.c1x.util.BitMap;
import com.sun.c1x.ci.CiField;
import com.sun.c1x.value.ValueType;
import com.sun.c1x.value.BasicType;
import com.sun.c1x.C1XMetrics;

/**
 * The <code>ValueMap</code> class implements a nested hashtable data structure
 * for use in local and global value numbering.
 *
 * @author Ben L. Titzer
 */
public class ValueMap {

    /**
     * The table of links, indexed by hashing using the {@link Instruction#valueNumber() method}.
     * The hash chains themselves may share parts of the parents' hash chains at the end.
     */
    private Link[] table;

    /**
     * Total number of entries in this map and the parent, used to compute unique ids.
     */
    private int count;

    /**
     * A visitor to kill necessary values.
     */
    private final ValueNumberingEffects effects = new ValueNumberingEffects();

    /**
     * A bitmap denoting which of the values have been killed (by their {@link Link#id}).
     */
    private final BitMap parentKill;

    /**
     * Creates a new value map.
     */
    public ValueMap() {
        table = new Link[19];
        parentKill = null;
    }

    /**
     * Creates a new value map with the specified parent value map.
     * @param parent the parent value map
     */
    public ValueMap(ValueMap parent) {
        table = parent.table.clone();
        parentKill = new BitMap(parent.count);
        count = parent.count;
    }

    /**
     * The class that forms hash chains.
     */
    private static class Link {
        final ValueMap map;
        final int valueNumber;
        final int id;
        final Instruction value;
        Link next;

        Link(ValueMap map, int valueNumber, int id, Instruction value, Link next) {
            this.map = map;
            this.valueNumber = valueNumber;
            this.id = id;
            this.value = value;
            this.next = next;
        }

        boolean isKilled(ValueMap where) {
            return where != map && where.parentKill.get(id);
        }
    }

    /**
     * Inserts a value into the value map and looks up any previously available value.
     * @param x the instruction to insert into the value map
     * @return the value with which to replace the specified instruction, or the specified
     * instruction if there is no replacement
     */
    public Instruction findInsert(Instruction x) {
        int valueNumber = x.valueNumber();
        if (valueNumber != 0) {
            // value number != 0 means the instruction can be value numbered
            int index = indexOf(valueNumber, table);
            Link l = table[index];
            // hash and linear search
            while (l != null) {
                if (!l.isKilled(this) && l.valueNumber == valueNumber && l.value.valueEqual(x)) {
                    return l.value;
                }
                l = l.next;
            }
            // not found; insert
            table[index] = new Link(this, valueNumber, count++, x, table[index]);
            if (count > table.length * 1.5) {
                resize();
            }
        }
        return x;
    }

    /**
     * Process the effects of an instruction on the value map. Effects include
     * killing all of memory, killing only a field, or killing an array.
     * @param x the instruction for which to process the effects
     */
    public void processEffects(Instruction x) {
        x.accept(effects);
    }

    /**
     * Removes all values from this value map. This should only be used for local value numbering.
     */
    public void killAll() {
        assert parentKill == null : "should only be used for local value numbering";
        for (int i = 0; i < table.length; i++) {
            table[i] = null;
        }
        count = 0;
    }

    private void resize() {
        C1XMetrics.ValueMapResizes++;
        Link[] ntable = new Link[table.length * 3 + 4];
        if (parentKill != null) {
            // first add all the (live) parent's entries by cloning them
            for (int i = 0; i < table.length; i++) {
                Link l = table[i];
                while (l != null && l.map == this) {
                    l = l.next; // skip entries in this map
                }
                while (l != null && !l.isKilled(this)) {
                    // add live entries from parent
                    int index = indexOf(l.valueNumber, ntable);
                    ntable[index] = new Link(l.map, l.valueNumber, l.id, l.value, ntable[index]);
                    l = l.next;
                }
            }
        }

        for (int i = 0; i < table.length; i++) {
            Link l = table[i];
            // now add all the new entries
            while (l != null && l.map == this) {
                int index = indexOf(l.valueNumber, ntable);
                ntable[index] = new Link(l.map, l.valueNumber, l.id, l.value, ntable[index]);
                l = l.next;
            }
        }
        table = ntable;
    }

    private int indexOf(int valueNumber, Link[] table) {
        return (valueNumber & 0x7fffffff) % table.length;
    }

    private void killMemory(boolean all, CiField field, BasicType basicType) {
        // loop through all the chains
        for (int i = 0; i < table.length; i++) {
            Link l = table[i];
            Link p = null;
            // kill all the values in this map first by removing them from the list
            while (l != null && l.map == this) {
                if (mustKill(l.value, all, field, basicType)) {
                    if (p != null) {
                        p.next = l.next;
                    } else {
                        table[i] = l.next;
                    }
                    count--;
                } else {
                    p = l;
                }
                l = l.next;
            }
            // kill all the values in the parent map by adding them to the parentKill bitmap
            while (l != null) {
                if (mustKill(l.value, all, field, basicType)) {
                    parentKill.set(l.id);
                }
                l = l.next;
            }
        }
    }

    private boolean mustKill(Instruction instr, boolean all, CiField field, BasicType basicType) {
        if (instr instanceof LoadField) {
            if (all || ((LoadField) instr).field() == field) {
                C1XMetrics.ValueMapKills++;
                return true;
            }
        }
        if (instr instanceof LoadIndexed) {
            if (all || instr.type().basicType() == basicType) {
                C1XMetrics.ValueMapKills++;
                return true;
            }
        }
        return false;
    }

    private void killMemory() {
        killMemory(true, null, null);
    }

    private void killField(CiField field) {
        killMemory(false, field, null);
    }

    private void killArray(ValueType elementType) {
        killMemory(false, null, elementType.basicType());
    }

    private CiField checkField(CiField field) {
        if (!field.isLoaded()) {
            killMemory();
        }
        return field;
    }

    private class ValueNumberingEffects extends InstructionVisitor {
        @Override
        public void visitLoadField(LoadField i) {
            checkField(i.field());
        }

        @Override
        public void visitStoreField(StoreField i) {
            killField(checkField(i.field()));
        }

        @Override
        public void visitStoreIndexed(StoreIndexed i) {
            killArray(i.type());
        }

        @Override
        public void visitMonitorEnter(MonitorEnter i) {
            killMemory();
        }

        @Override
        public void visitMonitorExit(MonitorExit i) {
            killMemory();
        }

        @Override
        public void visitIntrinsic(Intrinsic i) {
            if (!i.preservesState()) {
                killMemory();
            }
        }

        @Override
        public void visitUnsafePutRaw(UnsafePutRaw i) {
            killMemory();
        }

        @Override
        public void visitUnsafePutObject(UnsafePutObject i) {
            killMemory();
        }
    }
}
