/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.hosted;

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.VMConfiguration.*;

import java.io.*;
import java.lang.ref.Reference;
import java.util.*;
import java.util.concurrent.*;

import com.sun.max.atomic.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.TargetBundleLayout.ArrayField;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.debug.*;
import com.sun.max.vm.hosted.GraphPrototype.ClassInfo;
import com.sun.max.vm.hosted.GraphPrototype.ReferenceFieldInfo;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.SpecificLayout.ObjectCellVisitor;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Builds the data prototype from the graph prototype, determining the boot image addresses of objects and copying their
 * representation into a byte buffer.
 */
public final class DataPrototype extends Prototype {

    private final GraphPrototype graphPrototype;

    /**
     * Retrieves the graph prototype from which this data prototype was built.
     *
     * @return
     */
    public GraphPrototype graphPrototype() {
        return graphPrototype;
    }

    private Map<Object, Address> objectToCell = new IdentityHashMap<Object, Address>();

    /**
     * Gets the address of the cell allocated for the specified object.
     *
     * @param object the object for which to get the address
     * @return the address of the specified object
     */
    public Address objectToCell(Object object) {
        return objectToCell.get(JavaPrototype.hostToTarget(object));
    }

    /**
     * Gets the mapping from objects to their cells.
     *
     * @return a map from objects to cells
     */
    public Map<Object, Address> allocationMap() {
        return Collections.unmodifiableMap(objectToCell);
    }

    /**
     * Gets a pointer to the origin of the specified object.
     *
     * @param object the object for which to retrieve the origin
     * @return a pointer to the origin of the object in this data prototype
     */
    public Pointer objectToOrigin(Object object) {
        final Hub hub = ObjectAccess.readHub(object);
        final SpecificLayout specificLayout = hub.specificLayout;
        return specificLayout.cellToOrigin(objectToCell(object).asPointer());
    }

    /**
     * Assigns the specified cell to this specified object.
     *
     * @param object the object to associate with the specified cell
     * @param cell the cell
     * @return {@code true} if the object does not already have an address and the new address was successfully assigned
     */
    private boolean assignCell(Object object, Address cell) {
        if (object == null) {
            return false;
        }
        assert !objectToCell.containsKey(object);
        objectToCell.put(object, cell);
        return true;
    }

    private List<Object> codeObjects = new ArrayList<Object>();

    /**
     * Allocates a cell for a given object which is referenced from a given target bundle.
     *
     * @param object the object with which to associate the cell
     * @param start the start of the region described by {@code targetBundleLayout}
     * @param targetBundleLayout the layout of the object to assign to the cell
     * @param field the field of the target bundle that contains a reference to the array
     */
    private void assignCodeCell(Object object, Address start, TargetBundleLayout targetBundleLayout, ArrayField field) {
        if (object != null) {
            final Size cellSize = targetBundleLayout.cellSize(field);
            assert !cellSize.isZero();
            final Pointer cell = start.plus(targetBundleLayout.cellOffset(field)).asPointer();
            if (assignCell(object, cell)) {
                codeObjects.add(object);
                assert ObjectAccess.size(object).equals(cellSize);
            }
        }
    }

    /**
     * Assigns cells for all target methods, including the target bundle's constituent members.
     */
    private void assignCodeCells() {
        Trace.begin(1, "assignCodeCells");
        Size size = Size.zero();
        int n = 0;
        for (TargetMethod targetMethod : Code.bootCodeRegion().copyOfTargetMethods()) {
            final TargetBundleLayout targetBundleLayout = TargetBundleLayout.from(targetMethod);
            assignCodeCell(targetMethod.scalarLiterals(), targetMethod.start(), targetBundleLayout, ArrayField.scalarLiterals);
            assignCodeCell(targetMethod.referenceLiterals(), targetMethod.start(), targetBundleLayout, ArrayField.referenceLiterals);
            assignCodeCell(targetMethod.code(), targetMethod.start(), targetBundleLayout, ArrayField.code);
            size = size.plus(targetMethod.size());
            ++n;
        }
        ProgramError.check(size.equals(Code.bootCodeRegion().size()));
        Trace.end(1, "assignCodeCells: " + n + " target methods");
    }

    private List<Object> heapObjects = new ArrayList<Object>();

    /**
     * Assigns a heap cell to the specified object.
     *
     * @param object the object which to assign
     * @param cell the cell to assign to the object
     */
    private void assignHeapCell(Object object, Address cell) {
        if (assignCell(object, cell)) {
            heapObjects.add(object);
        } else {
            throw ProgramError.unexpected("null found in list of heap objects");
        }
    }

    private Address nonZeroBootHeapStart;

    private Hub objectHub;
    private Hub hubHub;

    /**
     * Allocate one object that is not referenced and sits at the bottom of the boot image heap. Thus we avoid having
     * reference pointers with offset zero relative to the heap. These would be confused with the value 'null' by the
     * relocator in the substrate.
     */
    private void preventNullConfusion() {
        final Object object = new Object();
        final Address cell = Heap.bootHeapRegion.allocate(ObjectAccess.size(object), true);
        assignHeapCell(object, cell);
        objectHub = ObjectAccess.readHub(object);
        hubHub = ObjectAccess.readHub(objectHub);
        assert hubHub == ObjectAccess.readHub(hubHub);

        nonZeroBootHeapStart = Heap.bootHeapRegion.getAllocationMark();
    }

    /**
     * Allocates a special object that fills the remaining space in the current page, so that the next allocation in the
     * region will be page-aligned.
     *
     * @param region the region in which to create the page alignment object
     * @return the object allocated
     */
    private Object createPageAlignmentObject(LinearAllocatorRegion region) {
        final int rest = region.getAllocationMark().minus(region.start()).remainder(pageSize);
        if (rest == 0) {
            return null;
        }
        assert 0 < rest && rest < pageSize;
        int size = pageSize - rest;

        final ArrayLayout byteArrayLayout = layoutScheme.byteArrayLayout;
        final int minSize = byteArrayLayout.getArraySize(0).toInt();
        if (size < minSize) {
            size += pageSize;
        }

        for (int i = 0; i <= 2 * pageSize; i++) {
            final int allocationSize = region.allocationSize(byteArrayLayout.getArraySize(i)).toInt();
            if (allocationSize == size) {
                return new byte[i];
            }
        }
        throw ProgramError.unexpected("no byte array length matches alignment size: " + size);
    }

    /**
     * Assigns cells to all heap objects.
     */
    private void assignHeapCells() {
        preventNullConfusion();
        final BootHeapRegion heapRegion = Heap.bootHeapRegion;

        assignHeapCells(heapRegion, true);
        assignHeapCells(heapRegion, false);

        final Object alignment = createPageAlignmentObject(heapRegion);
        if (alignment != null) {
            assignHeapCell(alignment, heapRegion.allocate(ObjectAccess.size(alignment), true));
        }

        heapRegion.trim();
        assert heapRegion.size().remainder(pageSize) == 0;
    }

    /**
     * Assigns cells to some heap objects.
     *
     * @param heapRegion the boot heap region in which the objects are to be allocated
     * @param objectsWithMutableReferences if {@code true} then only objects
     *            {@linkplain ClassInfo#containsMutableReferences() containing mutable references} are processed;
     *            otherwise only objects the do not contain mutable object references are processed
     */
    private void assignHeapCells(BootHeapRegion heapRegion, boolean objectsWithMutableReferences) {
        final String tracePrefix = "assign" + (objectsWithMutableReferences ? "Mutable" : "Immutable") + "HeapCells: ";
        Trace.begin(1, tracePrefix);
        int count = 0;
        final Address mark = heapRegion.getAllocationMark();
        final List<Object> mutableHeapObjects = new ArrayList<Object>(graphPrototype.objects().size());
        for (Object object : graphPrototype.objects()) {
            final ClassInfo classInfo = graphPrototype.classInfoFor(object);
            if (classInfo.containsMutableReferences(object) == objectsWithMutableReferences) {
                Address cell = objectToCell.get(object);
                if (cell != null) {
                    assert Code.bootCodeRegion().contains(cell);
                } else {
                    final Size size = ObjectAccess.size(object);
                    cell = heapRegion.allocate(size, true);
                    assignHeapCell(object, cell);

                    if (objectsWithMutableReferences) {
                        mutableHeapObjects.add(object);
                    }
                    if (++count % 200000 == 0) {
                        Trace.line(1, ": " + count);
                    }
                }
            }
        }
        Trace.end(1, tracePrefix + count + " heap objects, " + heapRegion.getAllocationMark().minus(mark).toInt() + " bytes");
        if (objectsWithMutableReferences) {
            createHeapReferenceMap(heapRegion, mutableHeapObjects);
        }
    }

    /**
     * Creates the reference map covering the objects in the boot heap that contain runtime-mutable references.
     * The assignment of addresses for the boot heap will have ensured that such mutable objects are
     * at the front of the heap, before all non-mutable objects.
     *
     * @param heapRegion the boot heap region
     * @param mutableHeapObjects the mutable objects in the boot heap
     */
    private void createHeapReferenceMap(BootHeapRegion heapRegion, List<Object> mutableHeapObjects) {
        Trace.begin(1, "createHeapReferenceMap:");
        final int mutableHeapObjectsSize = heapRegion.getAllocationMark().toInt();
        final int heapReferenceMapSize = Size.fromLong(ByteArrayBitMap.computeBitMapSize(mutableHeapObjectsSize / Word.size())).wordAligned().toInt();
        final ByteArrayBitMap referenceMap = new ByteArrayBitMap(new byte[heapReferenceMapSize]);
        final List<Reference> specialReferences = new ArrayList<Reference>();

        int count = 0;
        for (Object object : mutableHeapObjects) {
            final ClassInfo classInfo = graphPrototype.classInfoFor(object);
            assert classInfo.containsMutableReferences(object);
            final Address cell = objectToCell.get(object);
            final Hub hub = ObjectAccess.readHub(object);
            final SpecificLayout specificLayout = hub.specificLayout;
            if (specificLayout.isArrayLayout()) {
                if (specificLayout.isReferenceArrayLayout()) {
                    final ArrayLayout arrayLayout = (ArrayLayout) specificLayout;
                    final int n = ((Object[]) object).length;
                    if (n != 0) {
                        final Pointer origin = specificLayout.cellToOrigin(cell.asPointer());
                        final int element0Index = origin.plus(arrayLayout.getElementOffsetFromOrigin(0)).dividedBy(Word.size()).toInt();
                        for (int i = 0; i < n; i++) {
                            final int index = element0Index + i;
                            try {
                                assert !referenceMap.isSet(index);
                                referenceMap.set(index);
                            } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
                                throw ProgramError.unexpected("Error while preparing reference map for mutable array in boot heap of type " +
                                    classInfo.clazz.getName() + ": cell=" + cell.toHexString() + ", index=" + i +
                                    " [" + origin.toHexString() + "+" + (Word.size() * i) + "], refmap index=" + index, indexOutOfBoundsException);
                            }
                        }
                    }
                }
            } else {
                final Pointer origin = specificLayout.cellToOrigin(cell.asPointer());
                for (ReferenceFieldInfo fieldInfo : classInfo.fieldInfos(object)) {
                    final FieldActor fieldActor = fieldInfo.fieldActor();
                    if (fieldActor == ClassRegistry.JLRReference_referent) {
                        continue;
                    }
                    final Pointer address = origin.plus(fieldActor.offset());
                    final int index = address.toInt() / alignment;
                    try {
                        assert !referenceMap.isSet(index) : fieldActor;
                        referenceMap.set(index);
                    } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
                        throw ProgramError.unexpected("Error while preparing reference map for mutable object in boot heap of type " +
                            classInfo.clazz.getName() + ": cell=" + cell.toHexString() + ", field=" + fieldActor.name +
                            " [" + origin.toHexString() + "+" + fieldActor.offset() + "], refmap index=" + index, indexOutOfBoundsException);
                    }
                }

                if (object instanceof Reference) {
                    specialReferences.add((Reference) object);
                }
            }
            if (++count % 100000 == 0) {
                Trace.line(1, ": " + count);
            }
        }

        byte[] referenceMapBytes = referenceMap.bytes();
        if (dataModel.endianness != Endianness.LITTLE) {
            // Convert the bytes in the reference map to the target endianness so that it can be
            // correctly read as words. By default, the reference map can be read as an array
            // of little endian words and so there's no need for the conversion in this case.
            final int length = referenceMapBytes.length;
            final ByteArrayInputStream inputStream = new ByteArrayInputStream(referenceMapBytes);
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(length);
            try {
                final int referenceMapWordLength = length / Word.size();
                for (int i = 0; i < referenceMapWordLength; ++i) {
                    final Word referenceMapWord = Word.read(inputStream, Endianness.LITTLE);
                    referenceMapWord.write(outputStream, dataModel.endianness);
                }
            } catch (IOException ioException) {
                throw ProgramError.unexpected("Error converting boot heap reference map from little endian to " + dataModel.endianness, ioException);
            }
            referenceMapBytes = outputStream.toByteArray();
            assert length == referenceMapBytes.length;
        }

        final Reference[] specialReferenceArray = specialReferences.toArray(new Reference[specialReferences.size()]);
        assignHeapCell(referenceMapBytes, heapRegion.allocate(ObjectAccess.size(referenceMapBytes), true));
        assignHeapCell(specialReferenceArray, heapRegion.allocate(ObjectAccess.size(specialReferenceArray), true));
        heapRegion.init(referenceMapBytes, specialReferenceArray);
        Trace.end(1, "createHeapReferenceMap: width=" + referenceMap.width() + ", cardinality=" +
            referenceMap.cardinality() + ", size=" + referenceMap.size() + ", #special references=" + specialReferenceArray.length);
    }

    /**
     * Gets the raw data written for the code region.
     *
     * @return a byte array containing the raw data of the code region
     */
    public byte[] codeData() {
        return codeDataWriter.data();
    }

    /**
     * Gets the raw data written for the data (heap) region.
     *
     * @return a byte array containing the raw data of the code region
     */
    public byte[] heapData() {
        return heapDataWriter.data();
    }

    private ByteArrayMemoryRegionWriter heapDataWriter;

    private ByteArrayMemoryRegionWriter codeDataWriter;

    /**
     * A visitor that can visit an entire memory region, as well as individual object fields.
     */
    abstract class MemoryRegionVisitor implements ObjectCellVisitor, Cloneable {

        final MemoryRegion region;
        final String name;

        /**
         * Creates a new memory region visitor for the specified region with the specified name.
         *
         * @param region the region of memory to visit
         * @param name the name of this visitor
         */
        MemoryRegionVisitor(MemoryRegion region, String name) {
            this.region = region;
            this.name = name;
        }

        int offset;

        /**
         * Sets the current write offset to the specified offset.
         */
        void setOffset(int offset, Object object) {
            this.offset = offset;
        }

        /**
         * Makes a shallow copy of this memory region visitor.
         */
        @Override
        public MemoryRegionVisitor clone() {
            try {
                return (MemoryRegionVisitor) super.clone();
            } catch (CloneNotSupportedException cloneNotSupportedException) {
                throw ProgramError.unexpected();
            }
        }

        /**
         * Called when the last field/element in the current object has been visited.
         */
        void completeObject() {
        }

        /**
         * Gets the origin for the specified object.
         *
         * @param object the object for which to find the origin
         * @return the origin for {@code object}
         */
        Pointer originFor(Object object) {
            final Address cell = objectToCell(object);
            // This will occur if some code is executed after the GraphPrototype has been created where that
            // code mutates the object graph.
            // For example, calling "toString()" on a HashMap instance in the graph may cause the "entrySet"
            // field to change from null to a new instance.
            if (cell == null) {
                graphPrototype.printPath(object, System.err);
                throw new MissingCellException(object);
            }
            return objectToOrigin(object);
        }

        /**
         * Visit an array of bytes within an cell (e.g. the debug cell tag).
         *
         * @param name the name of the field or tag
         * @param value the value of the bytes
         */
        abstract void visitBytes(String name, byte[] value);
    }

    /**
     * This class implements a memory region visitor that writes all objects into a byte array.
     */
    class ByteArrayMemoryRegionWriter extends MemoryRegionVisitor {

        final byte[] data;
        final boolean[] used;
        final byte[] nullRefBytes;

        /**
         * Creates a new instance for the specified memory region with the specified name.
         *
         * @param region the memory region
         * @param name the name of this writer
         */
        ByteArrayMemoryRegionWriter(MemoryRegion region, String name) {
            super(region, name);
            data = new byte[region.size().roundedUpBy(pageSize).toInt()];
            used = new boolean[data.length];
            nullRefBytes = referenceScheme.nullAsBytes();
        }

        /**
         * Gets the data in this region.
         *
         * @return a byte array representing the bytes in this region
         */
        byte[] data() {
            return data;
        }

        /**
         * Visit a field in an object.
         *
         * @param offsetInCell the offset of the field within the cell
         * @param name the name of the field
         * @param type the type of the field
         * @param value the value of the field
         */
        public void visitField(int offsetInCell, Utf8Constant name, TypeDescriptor type, Value value) {
            write(value, offsetInCell);
        }

        /**
         * Visit a field in the header of an object.
         *
         * @param offsetInCell the offset of the field within the cell
         * @param name the name of the field
         * @param type the type of the field
         * @param value the value of the field
         */
        public void visitHeaderField(int offsetInCell, String name, TypeDescriptor type, Value value) {
            write(value, offsetInCell);
        }

        /**
         * Visit an element within an array.
         *
         * @param offsetInCell the offset of the element within the cell
         * @param arrayIndex the index into the array
         * @param value the value of the element
         */
        public void visitElement(int offsetInCell, int arrayIndex, Value value) {
            write(value, offsetInCell);
        }

        /**
         * Visit raw bytes within a cell (e.g. a debug cell tag).
         *
         * @param name the name of the field or tag
         * @param value the value of the bytes at the location
         */
        @Override
        void visitBytes(String name, byte[] value) {
            write(value, offset);
        }

        /**
         * Write a value into this region.
         *
         * @param value the value to write
         * @param offsetInCell the offset in the cell to which to write the value
         */
        private void write(Value value, int offsetInCell) {
            final byte[] valueBytes;
            if (value.kind().isReference) {
                valueBytes = (value.asObject() == null) ? nullRefBytes : referenceScheme.asBytes(originFor(value.asObject()));
            } else {
                valueBytes = value.toBytes(dataModel);
            }
            write(valueBytes, offsetInCell + offset);
        }

        /**
         * Write a sequence of bytes into this region at the specified offset.
         *
         * @param value the value of the bytes to write
         * @param offset the offset at which to write
         */
        private void write(byte[] value, int offset) {
            for (int i = 0; i != value.length; ++i) {
                assert !used[offset + i];
                used[offset + i] = true;
                data[offset + i] = value[i];
            }
        }

        /**
         * Clear a portion of this region.
         *
         * @param offset the start offset at which to clear
         * @param size the size of the region to clear
         */
        public void clear(int offset, int size) {
            for (int i = 0; i != size; ++i) {
                used[offset + i] = false;
                data[offset + i] = 0;
            }
        }
    }

    /**
     * A memory region visitor that constructs a map of a region for debugging purposes.
     */
    class MemoryRegionMapWriter extends MemoryRegionVisitor {

        private final PrintStream mapPrintStream;
        private Object object;
        private boolean objectIsArray;
        private String[] values;
        private final MemoryRegion otherRegion;

        /**
         * Creates a new map writer for the specified region.
         *
         * @param region the region for which to create a map
         * @param otherRegion a secondary region to check for references that do not lie in the first region
         * @param name the name of this map writer
         * @param mapPrintStream the print stream to which to output the result
         */
        MemoryRegionMapWriter(MemoryRegion region, MemoryRegion otherRegion, String name, PrintStream mapPrintStream) {
            super(region, name);
            this.mapPrintStream = mapPrintStream;
            this.otherRegion = otherRegion;
        }

        /**
         * Render an object as a string.
         *
         * @param javaClass the java class of the object representing its type
         * @param object the object to render as a string
         * @return a string representation of the object
         */
        private String asString(Class javaClass, Object object) {
            if (javaClass == String.class) {
                return '"' + object.toString() + '"';
            } else if (object instanceof Actor) {
                final Actor actor = (Actor) this.object;
                return actor.javaSignature(object instanceof ClassActor);
            }
            return null;
        }

        /**
         * Convert an offset into an appropriate string label.
         *
         * @param offset the offset to convert into a string
         * @return a string representation of the offset
         */
        private String addressLabel(int offset) {
            final long absoluteAddress = region.start().toLong() + offset;
            return absoluteAddress + "[+" + offset + "]";
        }

        /**
         * Prints the last object that was encountered.
         */
        private void printLastObject() {
            if (object != null) {
                final Class javaClass = object.getClass();
                final String asString = asString(javaClass, object);
                if (asString == null) {
                    mapPrintStream.println(addressLabel(offset) + ": class=" + javaClass.getName());
                } else {
                    mapPrintStream.println(addressLabel(offset) + ": class=" + javaClass.getName() + " asString=" + asString);
                }
                int offsetInCell = 0;
                for (String value : values) {
                    if (value != null) {
                        mapPrintStream.println("  +" + offsetInCell + ": " + value);
                    }
                    ++offsetInCell;
                }
                object = null;
                values = null;
            }
        }

        /**
         * Sets the current offset in the region to be the new offset.
         *
         * @param offset the new offset
         * @param object the new object
         */
        @Override
        public void setOffset(int offset, Object object) {
            printLastObject();

            if (object != null) {
                final Size size = ObjectAccess.size(object);
                this.object = object;
                values = new String[size.toInt()];
                objectIsArray = object.getClass().isArray();
            }

            super.setOffset(offset, object);
        }

        /**
         * Visit an element of an array.
         *
         * @param offsetInCell the offset within in the cell
         * @param arrayIndex the index into the array
         * @param value the value of the element of the array
         */
        public void visitElement(int offsetInCell, int arrayIndex, Value value) {
            if (objectIsArray) {
                print("[" + arrayIndex + "]", null, offsetInCell, value);
            } else {
                print(value.kind().name + "[" + arrayIndex + "]", null, offsetInCell, value);
            }
        }

        /**
         * Visit raw bytes within an object (e.g. a debug cell tag).
         *
         * @param name the name of the field or tag
         * @param value the value of the bytes
         */
        @Override
        void visitBytes(String name, byte[] value) {
            mapPrintStream.println(addressLabel(offset) + ": " + name + " = " + Arrays.toString(value));
        }

        /**
         * Visit a field within an object.
         *
         * @param offsetInCell the offset within the cell
         * @param name the name of the field
         * @param type the type of the field
         * @param value the value of the field
         */
        public void visitField(int offsetInCell, Utf8Constant name, TypeDescriptor type, Value value) {
            print(name.toString(), type.toJavaString(false), offsetInCell, value);
        }

        /**
         * Visit a field within the header of an object.
         *
         * @param offsetInCell the offset within the cell
         * @param name the name of the field in the header
         * @param type the type of the field
         * @param value the value of the field
         */
        public void visitHeaderField(int offsetInCell, String name, TypeDescriptor type, Value value) {
            print("header:" + name, type.toJavaString(false), offsetInCell, value);
        }

        /**
         * Print out an element, field, or other entity.
         *
         * @param name the name of the entity
         * @param type the type of the entityt
         * @param offsetInCell the offset within the cell
         * @param value the value
         */
        private void print(String name, String type, int offsetInCell, Value value) {
            final String valueString;
            if (value.kind().isReference) {
                if (value == ReferenceValue.NULL) {
                    valueString = "null";
                } else {
                    final Address address = objectToCell(value.asObject());
                    if (address == null) {
                        valueString = "*** no cell for instance of " + value.asObject().getClass().getName() + " ***";
                    } else {
                        if (region.contains(address)) {
                            final int offset = address.minus(region.start()).toInt();
                            valueString = address + "[+" + offset + "]";
                        } else if (otherRegion.contains(address)) {
                            final int offset = address.minus(otherRegion.start()).toInt();
                            valueString = address + "[" + otherRegion.start().toLong() + "+" + offset + "]";
                        } else {
                            valueString = address.toString() + "[" + address.toLong() + "]";
                        }
                    }
                }
            } else if (value.kind().isWord) {
                valueString = value.toString() + "[" + value.toLong() + "]";
            } else {
                valueString = value.toString();
            }
            values[offsetInCell] = (type == null ? "" : type) + " " + name + " = " + valueString;
        }
    }

    private final int threadCount;
    private static final int BATCH = 10000;

    /**
     * Create the data representation of the objects.
     *
     * @param objects the objects for which to create the data
     * @param memoryRegionVisitor the memory region visitor to apply after visiting the objects and assigning cells
     * @return the number of bytes of data (including padding bytes) initialized for region
     */
    private int createData(final List<Object> objects, MemoryRegionVisitor memoryRegionVisitor) {
        final String regionName = memoryRegionVisitor.name;
        Trace.begin(1, "createData: " + regionName);
        final byte[] tagBytes = DebugHeap.tagBytes(dataModel);

        final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadCount);
        final CompletionService<Integer> completionService = new ExecutorCompletionService<Integer>(executor);

        for (int n = 0; n < objects.size(); n += BATCH) {
            final MemoryRegionVisitor m = memoryRegionVisitor.clone(); // prevent 'setOffset()' below from causing a
                                                                        // sharing conflict
            final Address regionStart = m.region.start();
            final int start = n;
            completionService.submit(new Callable<Integer>() {
                public Integer call() throws Exception {
                    int numberOfBytes = 0;
                    final int end = Math.min(objects.size(), start + BATCH);
                    int previousSize = 0;
                    int previousOffset = 0;
                    Object previousObject = null;
                    for (int i = start; i < end; i++) {
                        final Object object = objects.get(i);
                        final Hub hub = ObjectAccess.readHub(object);
                        final int size = ObjectAccess.size(object).toInt();
                        numberOfBytes += size;

                        final int offset = objectToCell(object).minus(regionStart).toInt();
                        final int expectedOffset = previousOffset + previousSize;
                        assert previousObject == null || expectedOffset <= offset : "expected offset: 0x" + Integer.toHexString(expectedOffset) + ", actual offset: 0x" + Integer.toHexString(offset);

                        if (tagging) {
                            m.setOffset(offset - tagBytes.length, null);
                            m.visitBytes("debugTag", tagBytes);
                            numberOfBytes += tagBytes.length;
                        }

                        m.setOffset(offset, object);
                        try {
                            hub.specificLayout.visitObjectCell(object, m);
                        } catch (MissingCellException e) {
                            System.err.println("no cell for object: class=" + e.object.getClass().getName() + " toString=\"" + e.object + "\"");
                            graphPrototype.printPath(object, System.err);
                            throw ProgramError.unexpected(e);
                        }

                        previousOffset = offset;
                        previousSize = size;
                        previousObject = object;
                    }
                    return numberOfBytes;
                }
            });
        }
        int numberOfBytes = 0;
        for (int n = 0; n < objects.size(); n += BATCH) {
            try {
                numberOfBytes += completionService.take().get();
                Trace.line(1, "createData - objects: " + n + ", bytes: " + numberOfBytes);
            } catch (Throwable throwable) {
                throw ProgramError.unexpected(throwable);
            }
        }
        executor.shutdown();
        Trace.end(1, "createData: " + regionName);
        return numberOfBytes;
    }

    /**
     * Adjusts all pointers to objects in the code region by a given delta. Pointers from both heap and code objects are
     * scanned.
     *
     * @param delta the value to add to all pointers/references
     */
    private void adjustCodeAddresses(int delta) {
        for (Object object : codeObjects) {
            objectToCell.put(object, objectToCell.get(object).plus(delta));
        }

        for (ClassActor classActor : ClassRegistry.allBootImageClasses()) {
            if (classActor instanceof ReferenceClassActor) {
                DynamicHub dynamicHub = classActor.dynamicHub();
                StaticHub staticHub = classActor.staticHub();

                adjustVTableAddresses(delta, dynamicHub);
                adjustVTableAddresses(delta, staticHub);

                for (InterfaceActor interfaceActor : classActor.getAllInterfaceActors()) {
                    final int interfaceITableIndex = dynamicHub.getITableIndex(interfaceActor.id);
                    for (InterfaceMethodActor interfaceMethodActor : interfaceActor.localInterfaceMethodActors()) {
                        final int iTableIndex = interfaceITableIndex + interfaceMethodActor.iIndexInInterface();
                        dynamicHub.setWord(iTableIndex, dynamicHub.getWord(iTableIndex).asAddress().plus(delta));
                    }
                }
            }
        }

        for (TargetMethod targetMethod : Code.bootCodeRegion().copyOfTargetMethods()) {
            targetMethod.setStart(targetMethod.start().plus(delta));
            targetMethod.setCodeStart(targetMethod.codeStart().plus(delta).toPointer());
        }
    }

    private void adjustVTableAddresses(int delta, final Hub hub) {
        Word[] words = hub.expansion.words;
        for (int i = 0; i < hub.vTableLength(); i++) {
            final int vTableIndex = Hub.vTableStartIndex() + i;
            words[vTableIndex] = words[vTableIndex].asAddress().plus(delta);
        }
    }

    /**
     * Adjusts the code region to be immediately after the heap region. This includes adjusting all pointers from the
     * heap and code to objects within the code region.
     */
    private void adjustMemoryRegions() {
        Trace.begin(1, "adjustMemoryRegions");
        final LinearAllocatorRegion heap = Heap.bootHeapRegion;
        final LinearAllocatorRegion code = Code.bootCodeRegion();

        final Address codeStart = heap.end().roundedUpBy(pageSize);
        final int delta = codeStart.minus(code.start()).toInt();
        adjustCodeAddresses(delta);

        code.setStart(codeStart);
        code.setMark(code.getAllocationMark().plus(delta));
        Trace.end(1, "adjustMemoryRegions");
    }

    private final ByteArrayBitMap relocationFlags;

    /**
     * Gets a byte array that represents the relocation data for the entire data prototype.
     *
     * @return a byte array that represents the relocation data, with one bit per word
     */
    public byte[] relocationData() {
        return relocationFlags.bytes();
    }

    /**
     * Get the offset of an object field.
     *
     * @param javaClass the class which contains the field
     * @param fieldName the name of the field
     * @param fieldType the type of the field
     * @return the offset of the field from the beginning of a cell
     */
    private int getInstanceFieldOffsetInTupleCell(Class javaClass, Utf8Constant fieldName, TypeDescriptor fieldType) {
        final ClassActor classActor = ClassActor.fromJava(javaClass);
        final TupleLayout tupleLayout = (TupleLayout) classActor.dynamicHub().specificLayout;
        final FieldActor fieldActor = classActor.findInstanceFieldActor(fieldName, fieldType);
        ProgramError.check(fieldActor != null, "could not find field: " + fieldName);
        return tupleLayout.getFieldOffsetInCell(fieldActor);
    }

    /**
     * Sets the relocation flag for the specified address, indicating that the contents of the address need to be
     * relocated.
     *
     * @param address the address which contains a value to be relocated
     */
    private void setRelocationFlag(Address address) {
        assert address.remainder(alignment) == 0;
        final int index = address.toInt() / alignment;
        relocationFlags.set(index);
    }

    /**
     * A visitor that sets the relocation flag for references.
     */
    private final PointerIndexVisitor originVisitor = new PointerIndexVisitor() {
        @Override
        public void visit(Pointer origin, int index) {
            setRelocationFlag(origin.plus(index * Word.size()));
        }
    };

    /**
     * Sets the relocation flags for the specified object.
     *
     * @param object the object to scan
     * @param cell the cell which contains the object
     * @return the number of references within the object
     */
    private synchronized int setRelocationFlags(Object object, Address cell) {
        final Hub hub = ObjectAccess.readHub(object);
        final SpecificLayout specificLayout = hub.specificLayout;

        setRelocationFlag(cell.plus(specificLayout.getHubReferenceOffsetInCell()));
        if (specificLayout.isArrayLayout()) {
            if (specificLayout.isReferenceArrayLayout()) {
                final ArrayLayout arrayLayout = (ArrayLayout) specificLayout;
                assert arrayLayout.elementKind().isReference;
                final int n = ArrayAccess.readArrayLength(object);
                for (int i = 0; i < n; i++) {
                    final Address address = cell.plus(arrayLayout.getElementOffsetInCell(i));
                    setRelocationFlag(address);
                }
                return 1 + n;
            }
            return 1;
        }
        final Pointer origin = specificLayout.cellToOrigin(cell.asPointer());
        TupleReferenceMap.visitReferences(hub, origin, originVisitor);
        if (hub.isJLRReference) {
            originVisitor.visit(origin, ClassRegistry.JLRReference_referent.offset() / Word.size());
        }
        return 1 + hub.referenceMapLength;
    }

    /**
     * Assigns relocation flags for all objects.
     *
     * @param objects a list of all the objects for which to set the relocation flags
     * @param name the name of visitor
     */
    private void assignObjectRelocationFlags(final List<Object> objects, String name) {
        Trace.begin(1, "assignObjectRelocationFlags: " + name);
        final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadCount);
        final CompletionService<Integer> completionService = new ExecutorCompletionService<Integer>(executor);

        int numberOfRelocations = 0;
        for (int n = 0; n < objects.size(); n += BATCH) {
            final int start = n;
            completionService.submit(new Callable<Integer>() {
                public Integer call() throws Exception {
                    try {
                        int numberOfRelocationsInBatch = 0;
                        final int end = Math.min(objects.size(), start + BATCH);
                        for (int i = start; i < end; i++) {
                            final Object object = objects.get(i);
                            numberOfRelocationsInBatch += setRelocationFlags(object, objectToCell.get(object));
                        }
                        return numberOfRelocationsInBatch;
                    } catch (Exception e) {
                        executor.shutdown();
                        throw e;
                    }
                }
            });
        }

        for (int n = 0; n < objects.size(); n += BATCH) {
            try {
                numberOfRelocations += completionService.take().get();
            } catch (Throwable throwable) {
                ProgramError.unexpected(throwable);
            }
        }

        executor.shutdown();
        Trace.end(1, "assignObjectRelocationFlags - " + name + " relocations: " + numberOfRelocations);
    }

    /**
     * Assigns relocation flags for all method dispatch tables. TODO: generalize to any kind of hybrid?
     */
    private void assignMethodDispatchTableRelocationFlags() {
        Trace.begin(1, "assignMethodDispatchTableRelocationFlags");
        final ArrayLayout wordArrayLayout = layoutScheme.wordArrayLayout;
        for (ClassActor classActor : ClassRegistry.allBootImageClasses()) {
            if (classActor instanceof ReferenceClassActor) {
                final DynamicHub dynamicHub = classActor.dynamicHub();
                final StaticHub staticHub = classActor.staticHub();
                final Address dynamicHubCell = objectToCell.get(dynamicHub);
                final Address staticHubCell = objectToCell.get(staticHub);

                for (int i = 0; i < dynamicHub.vTableLength(); i++) {
                    final int vTableIndex = Hub.vTableStartIndex() + i;
                    setRelocationFlag(dynamicHubCell.plus(wordArrayLayout.getElementOffsetInCell(vTableIndex)));
                }

                for (int i = 0; i < staticHub.vTableLength(); i++) {
                    final int vTableIndex = Hub.vTableStartIndex() + i;
                    setRelocationFlag(staticHubCell.plus(wordArrayLayout.getElementOffsetInCell(vTableIndex)));
                }

                for (InterfaceActor interfaceActor : classActor.getAllInterfaceActors()) {
                    final int interfaceITableIndex = dynamicHub.getITableIndex(interfaceActor.id);
                    for (InterfaceMethodActor interfaceMethodActor : interfaceActor.localInterfaceMethodActors()) {
                        final int iTableIndex = interfaceITableIndex + interfaceMethodActor.iIndexInInterface();
                        setRelocationFlag(dynamicHubCell.plus(wordArrayLayout.getElementOffsetInCell(iTableIndex)));
                    }
                }
            }
        }
        Trace.end(1, "assignMethodDispatchTableRelocationFlags");
    }

    private static final Utf8Constant codeStart = SymbolTable.makeSymbol("codeStart");
    private static final Utf8Constant start = SymbolTable.makeSymbol("start");

    /**
     * Assign relocation flags for target methods.
     *
     * @param startFieldOffset the offset of the "start" field from the target method
     */
    private void assignTargetMethodRelocationFlags(int startFieldOffset) {
        Trace.begin(1, "assignTargetMethodRelocationFlags");

        final int codeStartFieldOffset = getInstanceFieldOffsetInTupleCell(TargetMethod.class, codeStart, JavaTypeDescriptor.forJavaClass(Pointer.class));

        for (TargetMethod targetMethod : Code.bootCodeRegion().copyOfTargetMethods()) {
            setRelocationFlag(objectToCell.get(targetMethod).plus(startFieldOffset));
            setRelocationFlag(objectToCell.get(targetMethod).plus(codeStartFieldOffset));
        }

        Trace.end(1, "assignTargetMethodRelocationFlags");
    }

    /**
     * Assign the relocation flags for all objects and code.
     */
    public void assignRelocationFlags() {
        Trace.begin(1, "assignRelocationFlags");

        assignObjectRelocationFlags(heapObjects, "heap");
        assignObjectRelocationFlags(codeObjects, "code");

        assignMethodDispatchTableRelocationFlags();

        final int startFieldOffset = getInstanceFieldOffsetInTupleCell(MemoryRegion.class, start, JavaTypeDescriptor.forJavaClass(Address.class));
        assignTargetMethodRelocationFlags(startFieldOffset);
        setRelocationFlag(objectToCell.get(Code.bootCodeRegion()).plus(startFieldOffset));

        setRelocationFlag(objectToCell.get(Heap.bootHeapRegion.mark).plus(AtomicWord.valueOffset()));
        setRelocationFlag(objectToCell.get(Code.bootCodeRegion().mark).plus(AtomicWord.valueOffset()));

        Trace.end(1, "assignRelocationFlags");
    }

    private final int pageSize;
    private final DataModel dataModel;
    private final int alignment;
    private final LayoutScheme layoutScheme;
    private final ReferenceScheme referenceScheme;
    private final boolean tagging;

    /**
     * Create and build a new data prototype from the specified graph prototype.
     *
     * @param graphPrototype the graph prototype for which to build the data prototype
     * @param mapFile a file to which to write map information; if {@code null}, no map information will be written
     */
    public DataPrototype(GraphPrototype graphPrototype, File mapFile, int threadCount) {
        this.graphPrototype = graphPrototype;
        final Platform platform = platform();
        this.threadCount = threadCount;
        pageSize = platform.pageSize;
        dataModel = platform.dataModel;
        alignment = Word.size();
        layoutScheme = vmConfig().layoutScheme();
        referenceScheme = vmConfig().referenceScheme();
        tagging = vmConfig().debugging() && vmConfig().heapScheme().supportsTagging();
        Trace.begin(1, DataPrototype.class.getSimpleName());

        assignCodeCells();
        assignHeapCells();

        adjustMemoryRegions();

        MaxineVM vm = vm();
        // From now on, all objects have been assigned their final cells location.
        // This is where scheme can do last minute adjustments to data in the boot image and compute relative offsets
        // for whatever startup purposes.
        vm.phase = MaxineVM.Phase.WRITING_IMAGE;
        // Makes the object to cell map visible to the prototyped VM. Schemes might need it for their WRITING_IMAGE phase.
        Heap.initObjectToCell(allocationMap());
        vmConfig().initializeSchemes(Phase.WRITING_IMAGE);
        // This is the phase the image will be in when loaded by the boot image loader. We set it now before writing the image.
        vm.phase = MaxineVM.Phase.PRIMORDIAL;
        heapDataWriter = new ByteArrayMemoryRegionWriter(Heap.bootHeapRegion, "heap");
        codeDataWriter = new ByteArrayMemoryRegionWriter(Code.bootCodeRegion(), "code");

        int numberOfBytes = createData(heapObjects, heapDataWriter);
        final int bootHeapRegionSize = Heap.bootHeapRegion.size().toInt();
        ProgramWarning.check(numberOfBytes == bootHeapRegionSize, "numberOfBytes != bootHeapRegionSize");

        numberOfBytes = createData(codeObjects, codeDataWriter);
        final int bootCodeRegionSize = Code.bootCodeRegion().size().toInt();
        ProgramWarning.check(numberOfBytes <= bootCodeRegionSize, "numberOfBytes > bootCodeRegionSize");

        // one bit per alignment unit
        relocationFlags = new ByteArrayBitMap((heapDataWriter.data().length + codeDataWriter.data().length) / alignment);

        assignRelocationFlags();

        if (mapFile != null) {
            try {
                final PrintStream mapPrintStream = new PrintStream(new FileOutputStream(mapFile));
                mapPrintStream.println("start heap");
                createData(heapObjects, new MemoryRegionMapWriter(Heap.bootHeapRegion, Code.bootCodeRegion(), "heap", mapPrintStream));
                mapPrintStream.println("end heap");
                mapPrintStream.println("start code");
                createData(codeObjects, new MemoryRegionMapWriter(Code.bootCodeRegion(), Heap.bootHeapRegion, "code", mapPrintStream));
                mapPrintStream.println("end code");
                mapPrintStream.close();
            } catch (IOException e) {
                ProgramWarning.message("Error while writing image map to " + mapFile);
                e.printStackTrace();
            }
        }

        Trace.end(1, DataPrototype.class.getSimpleName());
    }

    private static class MissingCellException extends RuntimeException {
        final Object object;
        MissingCellException(Object object) {
            this.object = object;
        }
    }
}
