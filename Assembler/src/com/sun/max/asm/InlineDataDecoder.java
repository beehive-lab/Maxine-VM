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
package com.sun.max.asm;

import java.io.*;
import java.util.*;

import com.sun.max.asm.InlineDataDescriptor.*;
import com.sun.max.io.*;
import com.sun.max.program.*;

/**
 * A decoder for a sequence of {@linkplain InlineDataDescriptor inline data descriptors} associated with an
 * instruction stream. The decoder is initialized either from an encoded or inflated sequence of descriptors.
 *
 * Once initialized, a inline data decoder can be {@linkplain #decode(int, BufferedInputStream) queried} for the
 * inline data descriptor describing the inline data at a given position in an instruction stream.
 *
 * @author Doug Simon
 */
public class InlineDataDecoder {

    protected final Map<Integer, InlineDataDescriptor> positionToDescriptorMap;

    /**
     * Creates a decoder from an encoded sequence of inline data descriptors.
     *
     * @param encodedDescriptors a sequence of descriptors encoded in a byte array whose format complies with that used
     *            by {@link InlineDataRecorder#encodedDescriptors()}. This value can be null.
     * @return null if {@code encodedDescriptors} is null
     */
    public static InlineDataDecoder createFrom(byte[] encodedDescriptors) {
        if (encodedDescriptors != null) {
            return new InlineDataDecoder(encodedDescriptors);
        }
        return null;
    }

    /**
     * Creates a decoder based on the descriptors in a given recorder.
     *
     * @param inlineDataRecorder
     * @return null if {@code inlineDataRecorder} does not contain any entries
     */
    public static InlineDataDecoder createFrom(InlineDataRecorder inlineDataRecorder) {
        final List<InlineDataDescriptor> descriptors = inlineDataRecorder.descriptors();
        if (descriptors != null) {
            return new InlineDataDecoder(descriptors);
        }
        return null;
    }

    public InlineDataDecoder(List<InlineDataDescriptor> descriptors) {
        positionToDescriptorMap = new TreeMap<Integer, InlineDataDescriptor>();
        for (InlineDataDescriptor descriptor : descriptors) {
            positionToDescriptorMap.put(descriptor.startPosition(), descriptor);
        }
    }

    /**
     * Creates a decoder from an encoded sequence of inline data descriptors.
     *
     * @param encodedDescriptors a sequence of descriptors encoded in a byte array whose format complies with that used
     *            by {@link InlineDataRecorder#encodedDescriptors()}
     */
    public InlineDataDecoder(byte[] encodedDescriptors) {
        try {
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(encodedDescriptors);
            final DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
            final int numberOfEntries = dataInputStream.readInt();
            positionToDescriptorMap = new TreeMap<Integer, InlineDataDescriptor>();
            for (int i = 0; i < numberOfEntries; ++i) {
                final Tag tag = InlineDataDescriptor.Tag.VALUES.get(dataInputStream.readByte());
                final InlineDataDescriptor inlineDataDescriptor = tag.decode(dataInputStream);
                positionToDescriptorMap.put(inlineDataDescriptor.startPosition(), inlineDataDescriptor);
            }
            assert byteArrayInputStream.available() == 0;
        } catch (IOException ioException) {
            throw ProgramError.unexpected(ioException);
        }
    }

    /**
     * Decodes the data (if any) from the current read position of a given stream.
     *
     * @param currentPosition the stream's current read position with respect to the start of the stream
     * @param stream the instruction stream being disassembled
     * @return the inline data decoded from the stream or null if there is no inline data at {@code currentPosition}
     */
    public InlineData decode(int currentPosition, BufferedInputStream stream) throws IOException {
        final InlineDataDescriptor inlineDataDescriptor = positionToDescriptorMap.get(currentPosition);
        if (inlineDataDescriptor != null) {
            final int size = inlineDataDescriptor.size();
            final byte[] data = new byte[size];
            Streams.readFully(stream, data);
            return new InlineData(inlineDataDescriptor, data);
        }
        return null;
    }
}
