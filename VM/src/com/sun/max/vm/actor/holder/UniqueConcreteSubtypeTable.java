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
         * fixed size initial prefix and a variable tail that is resized automatically
         * when trying to add an out of bound element.
         */

        private final E[] prefix;
        private E[] overflow;

        public VariableLengthArray(int initialCapacity) {
            final Class<E []> type = null;
            prefix =  Utils.newArray(type, initialCapacity);
            overflow = Utils.newArray(type, 0);
        }

        private void ensureCapacity(int minOverflowCapacity) {
            // FIXME: need to make sure that capacity doesn't go beyond max int.
            int newCapacity = (overflow.length * 3) / 2 + 1;
            if (newCapacity < minOverflowCapacity) {
                newCapacity = minOverflowCapacity;
            }
            E [] newOverflow = Arrays.copyOf(overflow, newCapacity);
            overflow = newOverflow;
        }

        public E set(int index, E element) {
            final int pl = prefix.length;
            if (index < pl) {
                E oldValue = prefix[index];
                prefix[index] = element;
                return oldValue;
            }
            final int oindex = index - pl;

            if (oindex >= overflow.length) {
                ensureCapacity(oindex + 1);
            }
            E oldValue = overflow[oindex];
            overflow[oindex] = element;
            return oldValue;
        }

        public E get(int index) {
            final int pl = prefix.length;
            if (index < pl) {
                return prefix[index];
            }
            final int oindex = index - pl;
            if (oindex < overflow.length) {
                return overflow[oindex];
            }
            return null;
        }

        public int length() {
            return prefix.length + overflow.length;
        }
        // TODO:
        // Add trimming method
    }

    private static final VariableLengthArray<ClassActor> table = new VariableLengthArray<ClassActor>(4000);
    private static final ClassActor HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK = ClassActor.fromJava(Object.class);

    private static void dump(ClassActor classActor, ClassActor uct) {
        Log.print(classActor.id);
        Log.print(", ");
        Log.print(classActor.name());
        Log.print(", ");
        if (uct == null) {
            Log.print("null, -, ");
        } else if (uct == HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK) {
            Log.print(" multiple, -, ");
        } else {
            Log.print(uct.id);
            Log.print(", ");
            Log.print(uct.name());
        }
        Log.println();
    }

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

            boolean printDetails = false;
            if (printDetails) {
                Log.println("class id, class name, concrete subtype, concrete subtype class id");
            }
            final int length = table.length();
            while (classId < length) {
                ClassActor classActor;
                // Skip unused ids
                do {
                    classActor = ClassID.toClassActor(classId++);
                } while(classActor == null && classId < length);
                if (classId >= length) {
                    break;
                }
                totalClasses++;
                if (classActor.isAbstract()) {
                    totalAbstractClasses++;
                }
                if (classActor.firstSubclassActor == null) {
                    totalLeaves++;
                }
                ClassActor uct = table.get(classActor.id);
                if (uct == HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK) {
                    totalClassesWithMCP++;
                } else {
                    totalClassesWithUCP++;
                    if (uct == classActor) {
                        totalUCP++;
                    }
                }
                if (printDetails) {
                    dump(classActor, uct);
                }
            }

            Log.print("# classes            :");
            Log.println(totalClasses);
            Log.print("# abstract classes   :");
            Log.println(totalAbstractClasses);
            Log.print("# leaves             :");
            Log.println(totalLeaves);
            Log.print("# UCP                :");
            Log.println(totalUCP);
            Log.print("# classes with UCP   :");
            Log.println(totalClassesWithUCP);
            Log.print("# classes with MCP   :");
            Log.println(totalClassesWithMCP);
        }
    }

    public static void recordClassActor(PrimitiveClassActor classActor) {
        synchronized (table) {
            table.set(classActor.id, classActor);
        }
    }

    /**
     * Walk up the ancestry, and update the concrete type information.
     * @param ancestor
     * @param uniqueConcreteSubtype
     */
    private static void propagateConcreteSubType(ClassActor ancestor, ClassActor uniqueConcreteSubtype) {
        ClassActor uct = table.get(ancestor.id);
        // Update all the ancestors without a concrete sub-type with the unique concrete subtype.
        while (uct == null) {
            // No single concrete sub-type has been recorded for this ancestor yet.
            table.set(ancestor.id, uniqueConcreteSubtype);
            ancestor = ancestor.superClassActor;
            uct = table.get(ancestor.id);
        }
        // We reached an ancestor with at least one concrete sub-type (either it is one itself,
        // or one or more of its other children has a concrete sub-type). From here on, we can only
        // have ancestors with some concrete sub-types.
        while (uct != HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK) {
            // Reached an ancestor that had a unique-concrete sub-type.
            // This isn't true anymore, so update the mark.
            table.set(ancestor.id, HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK);
            ancestor = ancestor.superClassActor;
            uct = table.get(ancestor.id);
            if (MaxineVM.isDebug()) {
                FatalError.check(uct != null, "must have at least one concrete sub-type");
            }
        }
        // We reached an ancestor with multiple concrete sub types. From here on, all ancestors can only have
        // more than one concrete sub-type. This is a terminal state that will not change until class
        // unloading occurs.
    }

    private static void recordNonArrayClassActor(ClassActor classActor) {
        FatalError.check(classActor.firstSubclassActor == null, "must be leaf at class definition time");
        // If new class is abstract, the unique concrete sub-type table relationship doesn't change.
        if (!classActor.isAbstract()) {
            table.set(classActor.id, classActor);
            // First, update super-class relationship.
            // Recording is made at class definition time, when the class hasn't yet any sub-type.
            // So the unique concrete sub-type is self.
            ClassActor ancestor = classActor.superClassActor;
            if (ancestor == null) {
                // Can only be the class actor for java.lang.Object
                return;
            }
            propagateConcreteSubType(ancestor, classActor);

            // Now, update the unique concrete sub-type of the interfaces the class implements.
            for (ClassActor implemented : classActor.localInterfaceActors()) {
                propagateConcreteSubType(implemented, classActor);
            }
        }
    }

    /**
     * Records a newly defined tuple class actor in the table and update the unique concrete sub-type information of its
     * super types.
     *
     * @param classActor
     */
    public static void recordClassActor(TupleClassActor classActor) {
        synchronized (table) {
            recordNonArrayClassActor(classActor);
        }
    }

    public static void recordClassActor(InterfaceActor interfaceActor) {
        // Do nothing. Interfaces are recorded when defined, at which points they have no implementors yet
        // and therefore cannot have any concrete type yet.
    }

    public static void recordClassActor(ArrayClassActor arrayClassActor) {
        // The uniqueConcreteSubtype method is only used for non-array types at the moment.
        // So we don't bother and do nothing for now (except for the simple case of primitive type arrays).
        // This leaves the entry in the table to null,
        // which is the default conservative behavior.
        if (arrayClassActor.componentClassActor().isPrimitiveClassActor()) {
            synchronized (table) {
                // Primitive type arrays are leaves in the type hierarchy.
                table.set(arrayClassActor.id, arrayClassActor);
            }
        }
    }

    public static void recordClassActor(HybridClassActor classActor) {
        // Same as a TupleClassActor.
        synchronized (table) {
            recordNonArrayClassActor(classActor);
        }
    }

    public static ClassActor getUniqueConcreteSubtype(ClassActor classActor) {
        synchronized (table) {
            final ClassActor uct = table.get(classActor.id);
            if (uct == HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK) {
                return null;
            }
            return uct;
        }
    }
}
