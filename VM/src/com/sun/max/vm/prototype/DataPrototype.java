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
package com.sun.max.vm.prototype;

import java.io.*;
import java.lang.management.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.Arrays;
import java.util.concurrent.*;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.TargetBundleLayout.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.SpecificLayout.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.prototype.GraphPrototype.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Builds the data prototype from the graph prototype, determining the boot image addresses of objects and copying their
 * representation into a byte buffer.
 *
 * @author Bernd Mathiske
 */
public final class DataPrototype extends Prototype {

    private final GraphPrototype _graphPrototype;

    /**
     * Retrieves the graph prototype from which this data prototype was built.
     *
     * @return
     */
    public GraphPrototype graphPrototype() {
        return _graphPrototype;
    }

    private Map<Object, Address> _objectToCell = new IdentityHashMap<Object, Address>();

    /**
     * Gets the address of the cell allocated for the specified object.
     *
     * @param object the object for which to get the address
     * @return the address of the specified object
     */
    public Address objectToCell(Object object) {
        return _objectToCell.get(HostObjectAccess.hostToTarget(object));
    }

    /**
     * Gets the mapping from objects to their cells.
     *
     * @return a map from objects to cells
     */
    public Map<Object, Address> allocationMap() {
        return Collections.unmodifiableMap(_objectToCell);
    }

    /**
     * Gets a pointer to the origin of the specified object.
     *
     * @param object the object for which to retrieve the origin
     * @return a pointer to the origin of the object in this data prototype
     */
    public Pointer objectToOrigin(Object object) {
        return _layoutScheme.generalLayout().cellToOrigin(objectToCell(object).asPointer());
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
        assert !_objectToCell.containsKey(object);
        _objectToCell.put(object, cell);
        return true;
    }

    private AppendableIndexedSequence<Object> _codeObjects = new ArrayListSequence<Object>();

    /**
     * Allocates a cell for a given object which is referenced from a given target bundle.
     *
     * @param object the object with which to associate the cell
     * @param targetBundle the bundle of objects to assign to the cell
     * @param field the field of the target bundle that contains a reference to the array
     */
    private void assignCodeCell(Object object, TargetBundle targetBundle, ArrayField field) {
        if (object != null) {
            final Size cellSize = targetBundle.layout().cellSize(field);
            assert !cellSize.isZero();
            final Pointer cell = targetBundle.cell(field);
            if (assignCell(object, cell)) {
                _codeObjects.append(object);
                assert HostObjectAccess.getSize(object).equals(cellSize);
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
        for (TargetMethod targetMethod : Code.bootCodeRegion().targetMethods()) {
            final TargetBundle targetBundle = TargetBundle.from(targetMethod);
            assignCodeCell(targetMethod.catchRangePositions(), targetBundle, ArrayField.catchRangePositions);
            assignCodeCell(targetMethod.catchBlockPositions(), targetBundle, ArrayField.catchBlockPositions);
            assignCodeCell(targetMethod.stopPositions(), targetBundle, ArrayField.stopPositions);
            assignCodeCell(targetMethod.directCallees(), targetBundle, ArrayField.directCallees);
            assignCodeCell(targetMethod.referenceMaps(), targetBundle, ArrayField.referenceMaps);
            assignCodeCell(targetMethod.scalarLiteralBytes(), targetBundle, ArrayField.scalarLiteralBytes);
            assignCodeCell(targetMethod.referenceLiterals(), targetBundle, ArrayField.referenceLiterals);
            assignCodeCell(targetMethod.code(), targetBundle, ArrayField.code);
            size = size.plus(targetMethod.size());
            ++n;
        }
        ProgramError.check(size.equals(Code.bootCodeRegion().size()));
        Trace.end(1, "assignCodeCells: " + n + " target methods");
    }

    private AppendableIndexedSequence<Object> _heapObjects = new ArrayListSequence<Object>();

    /**
     * Assigns a heap cell to the specified object.
     *
     * @param object the object which to assign
     * @param cell the cell to assign to the object
     */
    private void assignHeapCell(Object object, Address cell) {
        if (assignCell(object, cell)) {
            _heapObjects.append(object);
        } else {
            ProgramError.unexpected("null found in list of heap objects");
        }
    }

    private Address _nonZeroBootHeapStart;

    /**
     * Allocate one object that is not referenced and sits at the bottom of the boot image heap. Thus we avoid having
     * reference pointers with offset zero relative to the heap. These would be confused with the value 'null' by the
     * relocator in the substrate.
     */
    private void preventNullConfusion() {
        final Object object = new Object();
        final Address cell = Heap.bootHeapRegion().allocateCell(HostObjectAccess.getSize(object));
        assignHeapCell(object, cell);
        _nonZeroBootHeapStart = Heap.bootHeapRegion().getAllocationMark();
    }

    /**
     * Allocates a special object that fills the remaining space in the current page, so that the next allocation in the
     * region will be page-aligned.
     *
     * @param region the region in which to create the page alignment object
     * @return the object allocated
     */
    private Object createPageAlignmentObject(LinearAllocatorHeapRegion region) {
        final int rest = region.getAllocationMark().minus(region.start()).remainder(_pageSize);
        if (rest == 0) {
            return null;
        }
        assert 0 < rest && rest < _pageSize;
        int size = _pageSize - rest;

        final ByteArrayLayout byteArrayLayout = _layoutScheme.byteArrayLayout();
        final int minSize = byteArrayLayout.getArraySize(0).toInt();
        if (size < minSize) {
            size += _pageSize;
        }

        for (int i = 0; i <= 2 * _pageSize; i++) {
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
        final BootHeapRegion heapRegion = Heap.bootHeapRegion();

        final int size = assignHeapCells(heapRegion, true);
        createHeapReferenceMap(heapRegion, size);
        assignHeapCells(heapRegion, false);

        final Object alignment = createPageAlignmentObject(heapRegion);
        if (alignment != null) {
            assignHeapCell(alignment, heapRegion.allocateCell(HostObjectAccess.getSize(alignment)));
        }

        heapRegion.trim();
        assert heapRegion.size().remainder(_pageSize) == 0;
    }

    /**
     * Assigns cells to some heap objects.
     *
     * @param heapRegion the boot heap region in which the objects are to be allocated
     * @param objectsWithMutableReferences if {@code true} then only objects
     *            {@linkplain ClassInfo#containsMutableReferences() containing mutable references} are processed;
     *            otherwise only objects the do not contain mutable object references are processed
     */
    private int assignHeapCells(final LinearAllocatorHeapRegion heapRegion, boolean objectsWithMutableReferences) {
        final String tracePrefix = "assign" + (objectsWithMutableReferences ? "Mutable" : "Immutable") + "HeapCells: ";
        Trace.begin(1, tracePrefix);
        int count = 0;
        final Address mark = heapRegion.getAllocationMark();
        for (Object object : _graphPrototype.objects()) {
            if (_graphPrototype.classInfoFor(object).containsMutableReferences() == objectsWithMutableReferences) {
                Address cell = _objectToCell.get(object);
                if (cell != null) {
                    assert Code.bootCodeRegion().contains(cell);
                } else {
                    final Hub hub = HostObjectAccess.readHub(object);
                    final Size size = HostObjectAccess.getSize(hub, object);
                    cell = heapRegion.allocateCell(size);
                    assignHeapCell(object, cell);

                    if (++count % 100000 == 0) {
                        Trace.line(1, ": " + count);
                    }
                }
            }
        }
        Trace.end(1, tracePrefix + count + " heap objects");
        return heapRegion.getAllocationMark().minus(mark).toInt();
    }

    private void createHeapReferenceMap(final BootHeapRegion heapRegion, final int mutableHeapObjectsSize) {
        Trace.begin(1, "createHeapReferenceMap:");
        final int heapReferenceMapSize = Size.fromLong(ByteArrayBitMap.computeBitMapSize(mutableHeapObjectsSize / _alignment)).aligned().toInt();
        final ByteArrayBitMap referenceMap = new ByteArrayBitMap(new byte[heapReferenceMapSize]);

        int count = 0;
        for (Object object : _graphPrototype.objects()) {
            final ClassInfo classInfo = _graphPrototype.classInfoFor(object);
            if (classInfo.containsMutableReferences()) {
                final Address cell1 = _objectToCell.get(object);
                if (!Code.bootCodeRegion().contains(cell1)) {
                    final Hub hub = HostObjectAccess.readHub(object);
                    final SpecificLayout specificLayout = hub.specificLayout();
                    if (specificLayout.isArrayLayout()) {
                        if (specificLayout.isReferenceArrayLayout()) {
                            final ArrayLayout arrayLayout = (ArrayLayout) specificLayout;
                            final int n = ((Object[]) object).length;
                            if (n != 0) {
                                final int element0Index = cell1.plus(arrayLayout.getElementOffsetInCell(0)).dividedBy(_alignment).toInt();
                                for (int i = 0; i < n; i++) {
                                    final int index = element0Index + i;
                                    assert !referenceMap.isSet(index);
                                    referenceMap.set(index);
                                }
                            }
                        }
                    } else {
                        final Pointer origin = specificLayout.cellToOrigin(cell1.asPointer());
                        for (Field field : classInfo._mutableReferenceFields) {
                            final FieldActor fieldActor = FieldActor.fromJava(field);
                            final Pointer address = origin.plus(fieldActor.offset());
                            final int index = address.toInt() / _alignment;
                            assert !referenceMap.isSet(index);
                            referenceMap.set(index);
                        }
                    }
                    if (++count % 100000 == 0) {
                        Trace.line(1, ": " + count);
                    }
                }
            }
        }
        final byte[] referenceMapBytes = referenceMap.bytes();
        final Address cell = heapRegion.allocateCell(HostObjectAccess.getSize(referenceMapBytes));
        assignHeapCell(referenceMapBytes, cell);
        heapRegion.setReferenceMap(referenceMapBytes);
        Trace.end(1, "createHeapReferenceMap:");
    }

    /**
     * Gets the raw data written for the code region.
     *
     * @return a byte array containing the raw data of the code region
     */
    public byte[] codeData() {
        return _codeDataWriter.data();
    }

    /**
     * Gets the raw data written for the data (heap) region.
     *
     * @return a byte array containing the raw data of the code region
     */
    public byte[] heapData() {
        return _heapDataWriter.data();
    }

    private ByteArrayMemoryRegionWriter _heapDataWriter;

    private ByteArrayMemoryRegionWriter _codeDataWriter;

    /**
     * A visitor that can visit an entire memory region, as well as individual object fields.
     */
    abstract class MemoryRegionVisitor implements ObjectCellVisitor, Cloneable {

        final MemoryRegion _region;
        final String _name;

        /**
         * Creates a new memory region visitor for the specified region with the specified name.
         *
         * @param region the region of memory to visit
         * @param name the name of this visitor
         */
        MemoryRegionVisitor(MemoryRegion region, String name) {
            _region = region;
            _name = name;
        }

        /**
         * Gets the name of this visitor.
         *
         * @return the name of this visitor
         */
        String name() {
            return _name;
        }

        /**
         * Gets the memory region for this visitor.
         *
         * @return the memory region for this visitor
         */
        MemoryRegion region() {
            return _region;
        }

        int _offset;

        /**
         * Sets the current write offset to the specified offset.
         */
        void setOffset(int offset, Object object) {
            _offset = offset;
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
         * Attempts to find a cell for the specified object.
         *
         * @param object the object for which to find the cell
         * @return a cell for the specified object, if one exists; an unexpected program error if one does not
         */
        Address cellFor(Object object) {
            final Address cell = objectToCell(object);
            // This will occur if some code is executed after the GraphPrototype has been created where that
            // code mutates the object graph.
            // For example, calling "toString()" on a HashMap instance in the graph may cause the "entrySet"
            // field to change from null to a new instance.
            if (cell == null) {
                throw new MissingCellException(object);
            }

            return cell;
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

        final byte[] _data;
        final boolean[] _used;
        final byte[] _nullGripBytes;

        /**
         * Creates a new instance for the specified memory region with the specified name.
         *
         * @param region the memory region
         * @param name the name of this writer
         */
        ByteArrayMemoryRegionWriter(MemoryRegion region, String name) {
            super(region, name);
            _data = new byte[region.size().roundedUpBy(_pageSize).toInt()];
            _used = new boolean[_data.length];
            _nullGripBytes = _gripScheme.createPrototypeNullGrip();
        }

        /**
         * Gets the data in this region.
         *
         * @return a byte array representing the bytes in this region
         */
        byte[] data() {
            return _data;
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
            write(value, _offset);
        }

        /**
         * Write a value into this region.
         *
         * @param value the value to write
         * @param offsetInCell the offset in the cell to which to write the value
         */
        private void write(Value value, int offsetInCell) {
            final byte[] valueBytes;
            if (value.kind() == Kind.REFERENCE) {
                valueBytes = (value.asObject() == null) ? _nullGripBytes : _gripScheme.createPrototypeGrip(cellFor(value.asObject()));
            } else {
                valueBytes = value.toBytes(_dataModel);
            }
            write(valueBytes, offsetInCell + _offset);
        }

        /**
         * Write a sequence of bytes into this region at the specified offset.
         *
         * @param value the value of the bytes to write
         * @param offset the offset at which to write
         */
        private void write(byte[] value, int offset) {
            for (int i = 0; i != value.length; ++i) {
                assert !_used[offset + i];
                _used[offset + i] = true;
                _data[offset + i] = value[i];
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
                _used[offset + i] = false;
                _data[offset + i] = 0;
            }
        }
    }

    /**
     * A memory region visitor that constructs a map of a region for debugging purposes.
     */
    class MemoryRegionMapWriter extends MemoryRegionVisitor {

        private final PrintStream _mapPrintStream;
        private Object _object;
        private boolean _objectIsArray;
        private String[] _values;
        private final MemoryRegion _otherRegion;

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
            _mapPrintStream = mapPrintStream;
            _otherRegion = otherRegion;
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
                final Actor actor = (Actor) _object;
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
            final long absoluteAddress = _region.start().toLong() + offset;
            return absoluteAddress + "[+" + offset + "]";
        }

        /**
         * Prints the last object that was encountered.
         */
        private void printLastObject() {
            if (_object != null) {
                final Class javaClass = _object.getClass();
                final String asString = asString(javaClass, _object);
                if (asString == null) {
                    _mapPrintStream.println(addressLabel(_offset) + ": class=" + javaClass.getName());
                } else {
                    _mapPrintStream.println(addressLabel(_offset) + ": class=" + javaClass.getName() + " asString=" + asString);
                }
                int offsetInCell = 0;
                for (String value : _values) {
                    if (value != null) {
                        _mapPrintStream.println("  +" + offsetInCell + ": " + value);
                    }
                    ++offsetInCell;
                }
                _object = null;
                _values = null;
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
                final Size size = HostObjectAccess.getSize(object);
                _object = object;
                _values = new String[size.toInt()];
                _objectIsArray = object.getClass().isArray();
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
            if (_objectIsArray) {
                print("[" + arrayIndex + "]", null, offsetInCell, value);
            } else {
                print(value.kind().name() + "[" + arrayIndex + "]", null, offsetInCell, value);
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
            _mapPrintStream.println(addressLabel(_offset) + ": " + name + " = " + Arrays.toString(value));
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
            if (value.kind() == Kind.REFERENCE) {
                if (value == ReferenceValue.NULL) {
                    valueString = "null";
                } else {
                    final Address address = objectToCell(value.asObject());
                    if (address == null) {
                        valueString = "*** no cell for instance of " + value.asObject().getClass().getName() + " ***";
                    } else {
                        if (region().contains(address)) {
                            final int offset = address.minus(_region.start()).toInt();
                            valueString = address + "[+" + offset + "]";
                        } else if (_otherRegion.contains(address)) {
                            final int offset = address.minus(_otherRegion.start()).toInt();
                            valueString = address + "[" + _otherRegion.start().toLong() + "+" + offset + "]";
                        } else {
                            valueString = address.toString() + "[" + address.toLong() + "]";
                        }
                    }
                }
            } else if (value.kind() == Kind.WORD) {
                valueString = value.toString() + "[" + value.toLong() + "]";
            } else {
                valueString = value.toString();
            }
            _values[offsetInCell] = (type == null ? "" : type) + " " + name + " = " + valueString;
        }
    }

    private final int _numberOfProcessors = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
    private static final int BATCH = 10000;

    /**
     * Create the data representation of the objects.
     *
     * @param objects the objects for which to create the data
     * @param memoryRegionVisitor the memory region visitor to apply after visiting the objects and assigning cells
     * @return the number of bytes of data (including padding bytes) initialized for region
     */
    private int createData(final IndexedSequence<Object> objects, MemoryRegionVisitor memoryRegionVisitor) {
        final String regionName = memoryRegionVisitor.name();
        Trace.begin(1, "createData: " + regionName);
        final byte[] tagBytes = _dataModel.wordWidth() == WordWidth.BITS_64 ? _dataModel.toBytes(DebugHeap.LONG_OBJECT_TAG) : _dataModel.toBytes(DebugHeap.INT_OBJECT_TAG);

        final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(_numberOfProcessors);
        final CompletionService<Integer> completionService = new ExecutorCompletionService<Integer>(executor);

        for (int n = 0; n < objects.length(); n += BATCH) {
            final MemoryRegionVisitor m = memoryRegionVisitor.clone(); // prevent 'setOffset()' below from causing a
                                                                        // sharing conflict
            final Address regionStart = m.region().start();
            final int start = n;
            completionService.submit(new Callable<Integer>() {

                @Override
                public Integer call() throws Exception {
                    int numberOfBytes = 0;
                    final int end = Math.min(objects.length(), start + BATCH);
                    int previousSize = 0;
                    int previousOffset = 0;
                    Object previousObject = null;
                    for (int i = start; i < end; i++) {
                        final Object object = objects.get(i);
                        final Hub hub = HostObjectAccess.readHub(object);
                        final int size = HostObjectAccess.getSize(hub, object).toInt();
                        numberOfBytes += size;

                        final int offset = objectToCell(object).minus(regionStart).toInt();
                        final int expectedOffset = previousOffset + previousSize;
                        assert previousObject == null || expectedOffset <= offset : "expected offset: 0x" + Integer.toHexString(expectedOffset) + ", actual offset: 0x" + Integer.toHexString(offset);

                        if (_debugging) {
                            m.setOffset(offset - tagBytes.length, null);
                            m.visitBytes("debugTag", tagBytes);
                            numberOfBytes += tagBytes.length;
                        }

                        m.setOffset(offset, object);
                        try {
                            hub.specificLayout().visitObjectCell(object, m);
                        } catch (MissingCellException e) {
                            System.err.println("no cell for object: class=" + e._object.getClass().getName() + " toString=\"" + e._object + "\"");
                            _graphPrototype.printPath(object, System.err);
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
        for (int n = 0; n < objects.length(); n += BATCH) {
            try {
                numberOfBytes += completionService.take().get();
                Trace.line(1, "createData - objects: " + n + ", bytes: " + numberOfBytes);
            } catch (Throwable throwable) {
                ProgramError.unexpected(throwable);
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
        for (Object object : _codeObjects) {
            _objectToCell.put(object, _objectToCell.get(object).plus(delta));
        }

        for (ClassActor classActor : ClassRegistry.vmClassRegistry()) {
            if (classActor instanceof ReferenceClassActor) {
                final DynamicHub dynamicHub = classActor.dynamicHub();

                for (int i = 0; i < dynamicHub.vTableLength(); i++) {
                    final int vTableIndex = Hub.vTableStartIndex() + i;
                    dynamicHub.setWord(vTableIndex, dynamicHub.getWord(vTableIndex).asAddress().plus(delta));
                }

                for (InterfaceActor interfaceActor : classActor.getAllInterfaceActors()) {
                    final int interfaceITableIndex = dynamicHub.getITableIndex(interfaceActor.id());
                    for (InterfaceMethodActor interfaceMethodActor : interfaceActor.localInterfaceMethodActors()) {
                        final int iTableIndex = interfaceITableIndex + interfaceMethodActor.iIndexInInterface();
                        dynamicHub.setWord(iTableIndex, dynamicHub.getWord(iTableIndex).asAddress().plus(delta));
                    }
                }
            }
        }

        for (TargetMethod targetMethod : Code.bootCodeRegion().targetMethods()) {
            targetMethod.setStart(targetMethod.start().plus(delta));
            targetMethod.setCodeStart(targetMethod.codeStart().plus(delta));
        }
    }

    /**
     * Adjusts the code region to be immediately after the heap region. This includes adjusting all pointers from the
     * heap and code to objects within the code region.
     */
    private void adjustMemoryRegions() {
        Trace.begin(1, "adjustMemoryRegions");
        final LinearAllocatorHeapRegion heap = Heap.bootHeapRegion();
        final LinearAllocatorHeapRegion code = Code.bootCodeRegion();

        final Address codeStart = heap.end().roundedUpBy(_pageSize);
        final int delta = codeStart.minus(code.start()).toInt();
        adjustCodeAddresses(delta);

        code.setStart(codeStart);
        code.setMark(code.getAllocationMark().plus(delta));
        Trace.end(1, "adjustMemoryRegions");
    }

    private final ByteArrayBitMap _relocationFlags;

    /**
     * Gets a byte array that represents the relocation data for the entire data prototype.
     *
     * @return a byte array that represents the relocation data, with one bit per word
     */
    public byte[] relocationData() {
        return _relocationFlags.bytes();
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
        final TupleLayout tupleLayout = (TupleLayout) classActor.dynamicHub().specificLayout();
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
        assert address.remainder(_alignment) == 0;
        final int index = address.toInt() / _alignment;
        _relocationFlags.set(index);
    }

    /**
     * A visitor that sets the relocation flag for the origin of all objects.
     */
    private final PointerOffsetVisitor _originOffsetVisitor = new PointerOffsetVisitor() {
        @Override
        public void visitPointerOffset(Pointer origin, int offset) {
            setRelocationFlag(origin.plus(offset));
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
        final Hub hub = HostObjectAccess.readHub(object);
        final SpecificLayout specificLayout = hub.specificLayout();
        setRelocationFlag(cell.plus(specificLayout.getHubReferenceOffsetInCell()));
        if (specificLayout.isArrayLayout()) {
            if (specificLayout.isReferenceArrayLayout()) {
                final ArrayLayout arrayLayout = (ArrayLayout) specificLayout;
                assert arrayLayout.elementKind() == Kind.REFERENCE;
                final int n = HostObjectAccess.getArrayLength(object);
                for (int i = 0; i < n; i++) {
                    final Address address = cell.plus(arrayLayout.getElementOffsetInCell(i));
                    setRelocationFlag(address);
                }
                return 1 + n;
            }
            return 1;
        }
        final Pointer origin = specificLayout.cellToOrigin(cell.asPointer());
        TupleReferenceMap.visitOriginOffsets(hub, origin, _originOffsetVisitor);
        return 1 + hub.referenceMapLength();
    }

    /**
     * Assigns relocation flags for all objects.
     *
     * @param objects a list of all the objects for which to set the relocation flags
     * @param name the name of visitor
     */
    private void assignObjectRelocationFlags(final IndexedSequence<Object> objects, String name) {
        Trace.begin(1, "assignObjectRelocationFlags: " + name);
        final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(_numberOfProcessors);
        final CompletionService<Integer> completionService = new ExecutorCompletionService<Integer>(executor);

        int numberOfRelocations = 0;
        for (int n = 0; n < objects.length(); n += BATCH) {
            final int start = n;
            completionService.submit(new Callable<Integer>() {

                @Override
                public Integer call() throws Exception {
                    try {
                        int numberOfRelocationsInBatch = 0;
                        final int end = Math.min(objects.length(), start + BATCH);
                        for (int i = start; i < end; i++) {
                            final Object object = objects.get(i);
                            numberOfRelocationsInBatch += setRelocationFlags(object, _objectToCell.get(object));
                        }
                        return numberOfRelocationsInBatch;
                    } catch (Exception e) {
                        executor.shutdown();
                        throw e;
                    }
                }
            });
        }

        for (int n = 0; n < objects.length(); n += BATCH) {
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
        final WordArrayLayout wordArrayLayout = _layoutScheme.wordArrayLayout();
        for (ClassActor classActor : ClassRegistry.vmClassRegistry()) {
            if (classActor instanceof ReferenceClassActor) {
                final DynamicHub dynamicHub = classActor.dynamicHub();
                final Address hubCell = _objectToCell.get(dynamicHub);

                for (int i = 0; i < dynamicHub.vTableLength(); i++) {
                    final int vTableIndex = Hub.vTableStartIndex() + i;
                    setRelocationFlag(hubCell.plus(wordArrayLayout.getElementOffsetInCell(vTableIndex)));
                }

                for (InterfaceActor interfaceActor : classActor.getAllInterfaceActors()) {
                    final int interfaceITableIndex = dynamicHub.getITableIndex(interfaceActor.id());
                    for (InterfaceMethodActor interfaceMethodActor : interfaceActor.localInterfaceMethodActors()) {
                        final int iTableIndex = interfaceITableIndex + interfaceMethodActor.iIndexInInterface();
                        setRelocationFlag(hubCell.plus(wordArrayLayout.getElementOffsetInCell(iTableIndex)));
                    }
                }
            }
        }
        Trace.end(1, "assignMethodDispatchTableRelocationFlags");
    }

    private static final Utf8Constant _codeStart = SymbolTable.makeSymbol("_codeStart");
    private static final Utf8Constant _start = SymbolTable.makeSymbol("_start");
    private static final Utf8Constant _mark = SymbolTable.makeSymbol("_mark");

    /**
     * Assign relocation flags for target methods.
     *
     * @param startFieldOffset the offset of the "_start" field from the target method
     */
    private void assignTargetMethodRelocationFlags(int startFieldOffset) {
        Trace.begin(1, "assignTargetMethodRelocationFlags");

        final int codeStartFieldOffset = getInstanceFieldOffsetInTupleCell(TargetMethod.class, _codeStart, JavaTypeDescriptor.forJavaClass(Pointer.class));

        for (TargetMethod targetMethod : Code.bootCodeRegion().targetMethods()) {
            setRelocationFlag(_objectToCell.get(targetMethod).plus(startFieldOffset));
            setRelocationFlag(_objectToCell.get(targetMethod).plus(codeStartFieldOffset));
        }

        Trace.end(1, "assignTargetMethodRelocationFlags");
    }

    /**
     * Assign the relocation flags for all objects and code.
     */
    public void assignRelocationFlags() {
        Trace.begin(1, "assignRelocationFlags");

        assignObjectRelocationFlags(_heapObjects, "heap");
        assignObjectRelocationFlags(_codeObjects, "code");

        assignMethodDispatchTableRelocationFlags();

        final int startFieldOffset = getInstanceFieldOffsetInTupleCell(RuntimeMemoryRegion.class, _start, JavaTypeDescriptor.forJavaClass(Address.class));
        assignTargetMethodRelocationFlags(startFieldOffset);
        setRelocationFlag(_objectToCell.get(Code.bootCodeRegion()).plus(startFieldOffset));

        final int markFieldOffset = getInstanceFieldOffsetInTupleCell(LinearAllocatorHeapRegion.class, _mark, JavaTypeDescriptor.forJavaClass(Address.class));
        setRelocationFlag(_objectToCell.get(Heap.bootHeapRegion()).plus(markFieldOffset));
        setRelocationFlag(_objectToCell.get(Code.bootCodeRegion()).plus(markFieldOffset));

        Trace.end(1, "assignRelocationFlags");
    }

    private final int _pageSize;
    private final DataModel _dataModel;
    private final int _alignment;
    private final LayoutScheme _layoutScheme;
    private final GripScheme _gripScheme;
    private final boolean _debugging;

    /**
     * Create and build a new data prototype from the specifeid graph prototype.
     *
     * @param graphPrototype the graph prototype for which to build the data prototype
     * @param mapFile a file to which to write map information; if {@code null}, no map information will be written
     */
    public DataPrototype(GraphPrototype graphPrototype, File mapFile) {
        super(graphPrototype.vmConfiguration());
        _graphPrototype = graphPrototype;
        final Platform platform = graphPrototype.vmConfiguration().platform();
        _pageSize = platform.pageSize();
        _dataModel = platform.processorKind().dataModel();
        _alignment = _dataModel.alignment().numberOfBytes();
        _layoutScheme = graphPrototype.vmConfiguration().layoutScheme();
        _gripScheme = graphPrototype.vmConfiguration().gripScheme();
        _debugging = graphPrototype.vmConfiguration().debugging();

        Trace.begin(1, DataPrototype.class.getSimpleName());

        assignCodeCells();
        assignHeapCells();

        adjustMemoryRegions();

        MaxineVM.target().setPhase(MaxineVM.Phase.PRIMORDIAL);
        _heapDataWriter = new ByteArrayMemoryRegionWriter(Heap.bootHeapRegion(), "heap");
        _codeDataWriter = new ByteArrayMemoryRegionWriter(Code.bootCodeRegion(), "code");

        int numberOfBytes = createData(_heapObjects, _heapDataWriter);
        final int bootHeapRegionSize = Heap.bootHeapRegion().size().toInt();
        ProgramWarning.check(numberOfBytes == bootHeapRegionSize, "numberOfBytes != bootHeapRegionSize");

        numberOfBytes = createData(_codeObjects, _codeDataWriter);
        final int bootCodeRegionSize = Code.bootCodeRegion().size().toInt();
        ProgramWarning.check(numberOfBytes <= bootCodeRegionSize, "numberOfBytes > bootCodeRegionSize");

        // one bit per alignment unit
        _relocationFlags = new ByteArrayBitMap((_heapDataWriter.data().length + _codeDataWriter.data().length) / _alignment);

        assignRelocationFlags();

        if (mapFile != null) {
            try {
                final PrintStream mapPrintStream = new PrintStream(new FileOutputStream(mapFile));
                mapPrintStream.println("start heap");
                createData(_heapObjects, new MemoryRegionMapWriter(Heap.bootHeapRegion(), Code.bootCodeRegion(), "heap", mapPrintStream));
                mapPrintStream.println("end heap");
                mapPrintStream.println("start code");
                createData(_codeObjects, new MemoryRegionMapWriter(Code.bootCodeRegion(), Heap.bootHeapRegion(), "code", mapPrintStream));
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
        final Object _object;
        MissingCellException(Object object) {
            this._object = object;
        }
    }
}
