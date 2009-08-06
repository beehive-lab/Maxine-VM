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

import com.sun.c1x.C1XMetrics;
import com.sun.c1x.ir.Instruction;

/**
 * The <code>ValueMap</code> class implements a nested hashtable data structure
 * for use in local and global value numbering.
 *
 * @author Ben L. Titzer
 */
public class ValueMap {
    /**
     * The class that forms hash chains.
     */
    private static class Link {
        final ValueMap map;
        final int valueNumber;
        final Instruction value;
        final Link next;

        Link(ValueMap map, int valueNumber, Instruction value, Link next) {
            this.map = map;
            this.valueNumber = valueNumber;
            this.value = value;
            this.next = next;
        }
    }

    private final ValueMap parent;

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
     * The maximum size allowed before triggering resizing.
     */
    private int max;

    /**
     * Creates a new value map.
     */
    public ValueMap() {
        parent = null;
        table = new Link[19];
    }

    /**
     * Creates a new value map with the specified parent value map.
     * @param parent the parent value map
     */
    public ValueMap(ValueMap parent) {
        this.parent = parent;
        this.table = parent.table.clone();
        this.count = parent.count;
        this.max = table.length + table.length / 2;
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
                if (l.valueNumber == valueNumber && l.value.valueEqual(x)) {
                    return l.value;
                }
                l = l.next;
            }
            // not found; insert
            table[index] = new Link(this, valueNumber, x, table[index]);
            if (count > max) {
                resize();
            }
        }
        return x;
    }

    /**
     * Kills all values in this local value map.
     */
    public void killAll() {
        assert parent == null : "should only be used for local value numbering";
        for (int i = 0; i < table.length; i++) {
            table[i] = null;
        }
        count = 0;
    }


    private void resize() {
        C1XMetrics.ValueMapResizes++;
        Link[] ntable = new Link[table.length * 3 + 4];
        if (parent != null) {
            // first add all the parent's entries by cloning them
            for (int i = 0; i < table.length; i++) {
                Link l = table[i];
                while (l != null && l.map == this) {
                    l = l.next; // skip entries in this map
                }
                while (l != null) {
                    // copy entries from parent
                    int index = indexOf(l.valueNumber, ntable);
                    ntable[index] = new Link(l.map, l.valueNumber, l.value, ntable[index]);
                    l = l.next;
                }
            }
        }

        for (int i = 0; i < table.length; i++) {
            Link l = table[i];
            // now add all the entries from this map
            while (l != null && l.map == this) {
                int index = indexOf(l.valueNumber, ntable);
                ntable[index] = new Link(l.map, l.valueNumber, l.value, ntable[index]);
                l = l.next;
            }
        }
        table = ntable;
        max = table.length + table.length / 2;
    }

    private int indexOf(int valueNumber, Link[] t) {
        return (valueNumber & 0x7fffffff) % t.length;
    }
}
