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
package com.sun.max.vm.cps.target;

import java.io.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;

/**
 * Java frame descriptors carrying target location information.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class TargetJavaFrameDescriptor extends JavaFrameDescriptor<TargetLocation> {

    public TargetJavaFrameDescriptor(TargetJavaFrameDescriptor parent, ClassMethodActor classMethodActor, int bytecodePosition, TargetLocation[] locals, TargetLocation[] stackSlots) {
        super(parent, classMethodActor, bytecodePosition, locals, stackSlots);
    }

    @Override
    public TargetJavaFrameDescriptor parent() {
        return (TargetJavaFrameDescriptor) super.parent();
    }

    /**
     * Compresses a given sequence of target Java frame descriptors as a byte array.
     *
     * @param javaFrameDescriptors the target Java frame descriptors to compress
     * @return a byte array encoding {@code javaFrameDescriptors} that can be {@linkplain #inflate(byte[]) inflated}
     */
    public static byte[] compress(Sequence<TargetJavaFrameDescriptor> javaFrameDescriptors) {
        final GrowableDeterministicSet<TargetJavaFrameDescriptor> parents = new LinkedIdentityHashSet<TargetJavaFrameDescriptor>();
        final GrowableDeterministicSet<ClassMethodActor> methods = new LinkedIdentityHashSet<ClassMethodActor>();
        int maxSlots = 0;
        for (TargetJavaFrameDescriptor descriptor : javaFrameDescriptors) {
            if (descriptor != null) {
                gatherParents(descriptor.parent(), parents);
                methods.add(descriptor.classMethodActor);
                maxSlots = Math.max(maxSlots, descriptor.maxSlots());
            }
        }

        final GrowableMapping<JavaFrameDescriptor, Integer> descriptorToSerial = HashMapping.createIdentityMapping();
        int parentSerial = FIRST_SERIAL;
        for (TargetJavaFrameDescriptor parent : parents) {
            methods.add(parent.classMethodActor);
            maxSlots = Math.max(maxSlots, parent.maxSlots());
            descriptorToSerial.put(parent, parentSerial);
            parentSerial++;
        }

        final GrowableMapping<ClassMethodActor, Integer> methodToSerial = HashMapping.createEqualityMapping();
        int methodSerial = 0;
        for (ClassMethodActor method : methods) {
            methodToSerial.put(method, methodSerial);
            methodSerial++;
        }

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            final DataOutput stream = new DataOutputStream(byteArrayOutputStream);
            final Writer writer;
            if (parentSerial <= CompactWriter.MAX_SERIAL && methodSerial <= CompactWriter.MAX_SERIAL && javaFrameDescriptors.length() <= CompactWriter.MAX_COUNT && maxSlots < CompactWriter.MAX_COUNT) {
                writer = new CompactWriter(stream, descriptorToSerial, methodToSerial);
                stream.writeBoolean(true);
            } else {
                writer = new Writer(stream, descriptorToSerial, methodToSerial);
                stream.writeBoolean(false);
            }
            writer.writeMethods(methods);
            writer.writeDescriptors(parents);
            writer.writeDescriptors(javaFrameDescriptors);
        } catch (IOException ioException) {
            ProgramError.unexpected(ioException);
        }

        final byte[] compressedJavaFrameDescriptors = byteArrayOutputStream.toByteArray();
        if (MaxineVM.isHosted()) {
            testInflation(javaFrameDescriptors, compressedJavaFrameDescriptors);
        }

        return compressedJavaFrameDescriptors;
    }

    /**
     * Adds a given descriptor and it's parent chain to a given set of descriptors.
     *
     * @param descriptor the descriptor to add
     * @param descriptors the set to which {@code descriptor} and its parent chain should be added
     */
    private static void gatherParents(TargetJavaFrameDescriptor descriptor, GrowableDeterministicSet<TargetJavaFrameDescriptor> descriptors) {
        if (descriptor == null || descriptors.contains(descriptor)) {
            return;
        }
        gatherParents(descriptor.parent(), descriptors);
        descriptors.add(descriptor);
    }

    @HOSTED_ONLY
    private static void testInflation(Sequence<TargetJavaFrameDescriptor> original, byte[] compressedJavaFrameDescriptors) {
        try {
            final Sequence<TargetJavaFrameDescriptor> inflated = inflate(compressedJavaFrameDescriptors);
            final Iterator<TargetJavaFrameDescriptor> originalIterator = original.iterator();
            final Iterator<TargetJavaFrameDescriptor> inflatedIterator = inflated.iterator();
            int index = 0;
            while (originalIterator.hasNext() && inflatedIterator.hasNext()) {
                final TargetJavaFrameDescriptor originalDescriptor = originalIterator.next();
                final TargetJavaFrameDescriptor inflatedDescriptor = inflatedIterator.next();
                final boolean match = originalDescriptor == null ? inflatedDescriptor == null : originalDescriptor.equals(inflatedDescriptor);

                if (!match) {
                    final StringBuilder buf = new StringBuilder(String.format("Inflated descriptor " + index + " does not match original descriptor:%n"));
                    buf.append(String.format("Original descriptor:%n"));
                    buf.append(String.format("%s%n", Strings.indent(originalDescriptor.toMultiLineString(), 4)));
                    buf.append(String.format("Inflated descriptor:%n"));
                    buf.append(String.format("%s%n", Strings.indent(inflatedDescriptor.toMultiLineString(), 4)));
                    final String message = buf.toString();
                    ProgramError.unexpected(message);
                }
                index++;
            }
            if (index != original.length()) {
                ProgramError.unexpected("Less inflated descriptors than original");
            }
            if (index != inflated.length()) {
                ProgramError.unexpected("More inflated descriptors than original");
            }
        } catch (Throwable throwable) {
            ProgramError.unexpected("Error while re-inflating compressed target Java frame descriptors", throwable);
        }
    }

    private static final byte FIRST_SERIAL = 2;
    private static final byte NO_DESCRIPTOR = 1;
    private static final byte NO_PARENT = 0;

    /**
     * Inflates a byte array encoding a set of target Java frame descriptors.
     *
     * @param compressedJavaFrameDescriptors a byte array conforming to the format produced by
     *            {@link #compress(Sequence)}
     * @return an object containing the set of target Java frame descriptors decoded from
     *         {@code compressedJavaFrameDescriptors} and an estimation of the heap space occupied by the inflated
     *         descriptors
     */
    private static Memoizer.Function.Result<IndexedSequence<TargetJavaFrameDescriptor>> inflateForMemoizer(byte[] compressedJavaFrameDescriptors) {
        Size size = Size.zero();
        try {
            final DataInput stream = new DataInputStream(new ByteArrayInputStream(compressedJavaFrameDescriptors));
            final boolean isCompact = stream.readBoolean();
            final Reader reader = isCompact ? new CompactReader(stream) : new Reader(stream);

            final int numberOfMethods = reader.readCount();
            final ClassMethodActor[] methods = new ClassMethodActor[numberOfMethods];
            for (int i = 0; i != numberOfMethods; ++i) {
                methods[i] = (ClassMethodActor) MethodActor.read(stream);
            }

            final int numberOfParents = reader.readCount();
            final TargetJavaFrameDescriptor[] serialToDescriptor = new TargetJavaFrameDescriptor[FIRST_SERIAL + numberOfParents];
            for (int serial = FIRST_SERIAL; serial < FIRST_SERIAL + numberOfParents; serial++) {
                final int parentSerial = reader.readSerial();
                final TargetJavaFrameDescriptor parent = parentSerial == NO_PARENT ? null : serialToDescriptor[parentSerial];

                final int methodSerial = reader.readSerial();
                final int bytecodePosition = stream.readUnsignedShort();
                final ClassMethodActor classMethodActor = methods[methodSerial];

                final TargetLocation[] locals = reader.readTargetLocations();
                size = size.plus(getArraySize(locals));

                final TargetLocation[] stackSlots = reader.readTargetLocations();
                size = size.plus(getArraySize(locals));

                final TargetJavaFrameDescriptor descriptor = new TargetJavaFrameDescriptor(parent, classMethodActor, bytecodePosition, locals, stackSlots);
                size = size.plus(getSize(descriptor));

                serialToDescriptor[serial] = descriptor;
            }

            final int length = reader.readCount();
            final MutableSequence<TargetJavaFrameDescriptor> descriptors = new ArraySequence<TargetJavaFrameDescriptor>(length);
            size = size.plus(getSize(descriptors));
            for (int i = 0; i < length; i++) {
                final int parentSerial = reader.readSerial();
                if (parentSerial != NO_DESCRIPTOR) {
                    final TargetJavaFrameDescriptor parent = parentSerial == NO_PARENT ? null : serialToDescriptor[parentSerial];

                    final int methodSerial = reader.readSerial();
                    final int bytecodePosition = stream.readUnsignedShort();
                    final ClassMethodActor classMethodActor = methods[methodSerial];

                    final TargetLocation[] locals = reader.readTargetLocations();
                    size = size.plus(getArraySize(locals));

                    final TargetLocation[] stackSlots = reader.readTargetLocations();
                    size = size.plus(getArraySize(locals));

                    final TargetJavaFrameDescriptor descriptor = new TargetJavaFrameDescriptor(parent, classMethodActor, bytecodePosition, locals, stackSlots);
                    size = size.plus(getSize(descriptor));

                    descriptors.set(i, descriptor);
                }
            }
            return new Memoizer.Function.Result<IndexedSequence<TargetJavaFrameDescriptor>>(descriptors, size);
        } catch (IOException ioException) {
            throw ProgramError.unexpected(ioException);
        }
    }

    /**
     * Inflates a byte array encoding a set of target Java frame descriptors.
     *
     * @param compressedJavaFrameDescriptors a byte array conforming to the format produced by
     *            {@link #compress(Sequence)}
     * @return set of target Java frame descriptors decoded from {@code compressedJavaFrameDescriptors}
     */
    public static IndexedSequence<TargetJavaFrameDescriptor> inflate(byte[] compressedJavaFrameDescriptors) {
        if (compressedJavaFrameDescriptors == null) {
            return IndexedSequence.Static.empty(TargetJavaFrameDescriptor.class);
        }
        return inflateForMemoizer(compressedJavaFrameDescriptors).value();
    }

    /**
     * Gets the cell size for a given object.
     */
    private static Size getSize(Object object) {
        if (MaxineVM.isHosted()) {
            return Size.zero();
        }
        return ObjectAccess.size(object);
    }

    /**
     * Gets the sum of the cell size for a {@code TargetLocation[]} and the cell size for each of its elements.
     */
    private static Size getArraySize(TargetLocation[] array) {
        Size size = getSize(array);
        for (TargetLocation element : array) {
            if (element != TargetLocation.undefined) {
                size = size.plus(getSize(element));
                // TODO: add Immediate._value, except if it's WordValue.zero()
            }
        }
        return size;
    }

    private static final Memoizer.Function<CPSTargetMethod, IndexedSequence<TargetJavaFrameDescriptor>> inflate = new Memoizer.Function<CPSTargetMethod, IndexedSequence<TargetJavaFrameDescriptor>>() {

        public Memoizer.Function.Result<IndexedSequence<TargetJavaFrameDescriptor>> create(CPSTargetMethod targetMethod) {
            return inflateForMemoizer(targetMethod.compressedJavaFrameDescriptors());
        }
    };

    private static final Mapping<CPSTargetMethod, IndexedSequence<TargetJavaFrameDescriptor>> methodToJavaFrameDescriptors = Memoizer.create(inflate);

    public static final TargetJavaFrameDescriptor get(CPSTargetMethod targetMethod, int index) {
        final IndexedSequence<TargetJavaFrameDescriptor> indexedSequence = methodToJavaFrameDescriptors.get(targetMethod);
        if (indexedSequence != null) {
            return indexedSequence.get(index);
        }
        return null;
    }

    /**
     * A helper class for writing a set of target Java frame descriptors to a stream.
     */
    static class Writer {

        final DataOutput stream;
        final Mapping<JavaFrameDescriptor, Integer> descriptorToSerial;
        final Mapping<ClassMethodActor, Integer> methodToSerial;

        Writer(DataOutput output, Mapping<JavaFrameDescriptor, Integer> descriptorToSerial, Mapping<ClassMethodActor, Integer> methodToSerial) {
            this.stream = output;
            this.descriptorToSerial = descriptorToSerial;
            this.methodToSerial = methodToSerial;
        }

        protected void writeSerial(int serial) throws IOException {
            stream.writeInt(serial);
        }

        protected void writeCount(int count) throws IOException {
            stream.writeInt(count);
        }

        protected final void writeDescriptor(TargetJavaFrameDescriptor descriptor) throws IOException {
            if (descriptor == null) {
                writeSerial(NO_DESCRIPTOR);
                return;
            }
            if (descriptor.parent() == null) {
                writeSerial(NO_PARENT);
            } else {
                writeSerial(descriptorToSerial.get(descriptor.parent()));
            }

            final int methodSerial = methodToSerial.get(descriptor.classMethodActor);
            writeSerial(methodSerial);
            stream.writeShort(descriptor.bytecodePosition);
            writeTargetLocations(stream, descriptor.locals);
            writeTargetLocations(stream, descriptor.stackSlots);
        }

        protected final void writeTargetLocations(DataOutput dos, TargetLocation[] targetLocations) throws IOException {
            writeCount(targetLocations.length);
            for (TargetLocation targetLocation : targetLocations) {
                targetLocation.write(dos);
            }
        }

        public final void writeDescriptors(Sequence<TargetJavaFrameDescriptor> descriptors) throws IOException {
            writeCount(descriptors.length());
            for (TargetJavaFrameDescriptor descriptor : descriptors) {
                writeDescriptor(descriptor);
            }
        }

        public final void writeMethods(Sequence<ClassMethodActor> methods) throws IOException {
            writeCount(methods.length());
            for (ClassMethodActor method : methods) {
                method.write(stream);
            }
        }
    }

    /**
     * Extends the standard writer to write a more compressed encoding. This writer can be used if the set of target
     * Java frame descriptors satisfies all the following conditions:
     * <ul>
     * <li>The number of descriptors is less than or equal to {@link #MAX_COUNT}.</li>
     * <li>The max parent serial number number is less than or equal to {@link #MAX_SERIAL}.</li>
     * <li>The number of unique methods associated with the descriptors is less than or equal to {@link #MAX_COUNT}.</li>
     * <li>The maximum number of Java expression {@linkplain JavaFrameDescriptor#stackSlots() stack slots} for any
     * descriptor is less than or equal to {@link #MAX_COUNT}.</li>
     * <li>The maximum number of Java {@linkplain JavaFrameDescriptor#locals() local variable slots} for any descriptor
     * is less than or equal to {@link #MAX_COUNT}.</li>
     * </ul>
     */
    static class CompactWriter extends Writer {

        static final int MAX_SERIAL = 0xFF;
        static final int MAX_COUNT = 0xFF;

        CompactWriter(DataOutput output, Mapping<JavaFrameDescriptor, Integer> descriptorToSerial, Mapping<ClassMethodActor, Integer> methodToSerial) {
            super(output, descriptorToSerial, methodToSerial);
        }

        @Override
        protected void writeCount(int count) throws IOException {
            assert count >= 0 && count <= 0xff;
            stream.writeByte(count);
        }

        @Override
        protected void writeSerial(int serial) throws IOException {
            assert serial >= 0 && serial <= 0xff;
            stream.writeByte(serial);
        }
    }

    static class Reader {

        final DataInput stream;

        Reader(DataInput stream) {
            this.stream = stream;
        }

        protected int readSerial() throws IOException {
            return stream.readInt();
        }

        protected int readCount() throws IOException {
            return stream.readInt();
        }

        public TargetLocation[] readTargetLocations() throws IOException {
            final int length = readCount();
            final TargetLocation[] result = new TargetLocation[length];
            for (int i = 0; i < length; i++) {
                result[i] = TargetLocation.read(stream);
            }
            return result;
        }
    }

    static class CompactReader extends Reader {

        CompactReader(DataInput stream) {
            super(stream);
        }

        @Override
        protected int readCount() throws IOException {
            return stream.readUnsignedByte();
        }

        @Override
        protected int readSerial() throws IOException {
            return stream.readUnsignedByte();
        }

    }
}
