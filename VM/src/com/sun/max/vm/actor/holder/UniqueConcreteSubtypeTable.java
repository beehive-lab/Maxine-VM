/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.actor.holder;

import java.util.*;

import com.sun.max.*;
import com.sun.max.vm.*;
import com.sun.max.vm.runtime.*;

/**
 * Global table recording unique concrete subtype relationship of class actors.
 * The table has one entry per class, indexed by the unique global
 * identifier of the class. For a given class C, the entry corresponding to C can be one of (i) null if
 * the class has no concrete subtype, (ii) the class actor of the Object class if the class has no single concrete
 * subtype (e.g., it has multiple concrete subtypes), or (iii) the class actor of the unique concrete subtype.
 * The table is updated at class definition time and class unloading time.
 *
 *
 * @author Laurent Daynes.
 */
public final class UniqueConcreteSubtypeTable {
    static class VariableLengthArray<E> {
        /*
         * Simple first implementation. Backing Storage for the array is made of a
         * initial prefix of a fixed size and a variable tail that is resized automatically
         * when trying to add an out of bound element.
         */

        private final E[] prefix;
        private E[] overflow;
        /**
         * Length denote the total length supported by the array, i.e.,
         * prefix.length + overflow.length * increment.
         */
        private int length;
        private final int increment;

        public VariableLengthArray(int initialCapacity, int increment) {
            final Class<E []> type = null;
            prefix =  Utils.newArray(type, initialCapacity);
            overflow = Utils.newArray(type, 0);
            length = initialCapacity;
            this.increment = increment;
        }

        private void ensureCapacity(int minCapacity) {
            // FIXME: need to make sure that capacity doesn't go beyond max int.
            int newCapacity = (length * 3) / 2 + 1;
            if (newCapacity < minCapacity) {
                newCapacity = minCapacity;
            }
            int newOverflowCapacity = newCapacity - prefix.length;
            E [] newOverflow = Arrays.copyOf(overflow, newOverflowCapacity);
            length = newCapacity;
            overflow = newOverflow;
        }

        public VariableLengthArray(int initialCapacity) {
            this(initialCapacity, 10);
        }

        public E set(int index, E element) {
            final int pl = prefix.length;
            if (index < pl) {
                E oldValue = prefix[index];
                prefix[index] = element;
                return oldValue;
            }
            if (index >= length) {
                ensureCapacity(index);
            }
            final int oindex = index - pl;
            E oldValue = overflow[oindex];
            overflow[oindex] = element;
            return oldValue;
        }

        public E get(int index) {
            final int pl = prefix.length;
            return (index < pl) ? prefix[index] : overflow[index - pl];
        }

        public int length() {
            return length;
        }
        // TODO:
        // Add trimming method
    }

    private static final VariableLengthArray<ClassActor> table = new VariableLengthArray<ClassActor>(500, 20);
    private static final ClassActor HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK = ClassActor.fromJava(Object.class);

    /**
     * Dump the table in the log.
     */
    public static void dump() {
        synchronized (table) {
            int classId = 0;
            int totalClasses = 0;
            int totalAbstractClasses = 0;
            int totalLeaves = 0;
            int totalUCP = 0;
            int totalClassesWithUCP = 0;
            int totalClassesWithMCP = 0;

            Log.println("class id, class name, concrete subtype, concrete subtype class id");
            for (int c = 0; c < table.length(); c++) {
                ClassActor classActor;
                do {
                    classActor = ClassID.toClassActor(classId++);
                } while(classActor == null);

                totalClasses++;
                if (classActor.isAbstract()) {
                    totalAbstractClasses++;
                }
                if (classActor.firstSubclassActor == null) {
                    totalLeaves++;
                }

                Log.print(classActor.id);
                Log.print(", ");
                Log.print(classActor.name());
                Log.print(", ");
                ClassActor uct = table.get(classActor.id);
                if (uct == null) {
                    Log.print("null, -");
                } else if (uct == HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK) {
                    totalClassesWithMCP++;
                    Log.print(" multiple, - ");
                } else {
                    totalClassesWithUCP++;
                    if (uct == classActor) {
                        totalUCP++;
                    }
                    Log.print(uct.id);
                    Log.print(", ");
                    Log.print(uct.name());
                }
                Log.println();
            }
        }
    }
    public static void recordClassActor(ClassActor classActor) {
        if (classActor.isTupleClass()) {
            recordClassActor((TupleClassActor) classActor);
        }
    }

    /**
     * Records a newly defined tuple class actor in the table and update the unique concrete sub-type information of its
     * super types.
     *
     * @param classActor
     */
    public static void recordClassActor(TupleClassActor classActor) {
        FatalError.check(classActor.firstSubclassActor == null, "Class must be leaf at class definition time");
        synchronized (table) {
            // If new class is abstract, the unique concrete sub-type table relationship doesn't change.
            if (!classActor.isAbstract()) {
                table.set(classActor.id, classActor);
                // First, update super-class relationship.
                ClassActor ancestor = classActor.superClassActor;
                if (ancestor == null) {
                    return;
                }
                ClassActor uct = table.get(ancestor.id);
                if (uct == null) {
                    // No single concrete sub-type has been recorded for the parent yet.
                    // Update all the ancestors without a concrete sub-type with this class actor.
                    do {
                        table.set(ancestor.id, classActor);
                        ancestor = ancestor.superClassActor;
                        uct = table.get(ancestor.id);
                    } while(uct == null);
                    while (uct != HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK) {
                        // Reached an ancestor that had a single-concrete sub-type.
                        // This isn't true anymore.
                        table.set(ancestor.id, HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK);
                        ancestor = ancestor.superClassActor;
                        uct = table.get(ancestor.id);
                    }
                } else {
                    while (uct != HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK) {
                        // Reached an ancestor that had a single-concrete sub-type.
                        // This isn't true anymore.
                        table.set(ancestor.id, HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK);
                        ancestor = ancestor.superClassActor;
                        uct = table.get(ancestor.id);
                    }
                }
                // When a class has multiple concrete sub types, all its ancestors are in a terminal state:
                // they cannot return to null or single concrete sub-type state until class unloading occurs.
                // So no need to update them.

                // Recording is made at class definition time, when the class hasn't yet any sub-type.
                // So the unique concrete sub-type is self.
            }
            // TODO: need to update the information for the interface implementors.
        }
    }

    public static void recordClassActor(ArrayClassActor arrayClassActor) {

    }

    public static void recordClassActor(HybridClassActor classActor) {

    }

    public static ClassActor getUniqueConcreteSubtype(ClassActor classActor) {
        return table.get(classActor.id);
    }
}
