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

import com.sun.max.collect.*;
import com.sun.max.program.*;

/**
 * A facility for recording inline data descriptors associated with a sequence of assembled code.
 * The recorded descriptors described the structure of some contiguous inline data encoded
 * in the assembled code.
 *
 * @author Doug Simon
 */
public class InlineDataRecorder {

    private VariableSequence<InlineDataDescriptor> _descriptors;
    private boolean _normalized;

    /**
     * Adds an inline data descriptor to this object.
     */
    public void add(InlineDataDescriptor inlineData) {
        if (inlineData.size() != 0) {
            if (_descriptors == null) {
                _descriptors = new ArrayListSequence<InlineDataDescriptor>();
            }
            _descriptors.append(inlineData);
            _normalized = false;
        }
    }

    /**
     * Gets the sequence of inline data descriptors derived from the descriptors that have been
     * {@linkplain #add(InlineDataDescriptor) added} to this object. The returned sequence is comprised of
     * non-overlapping descriptors (i.e. the range implied by each descriptor's
     * {@linkplain InlineDataDescriptor#startPosition() start} and {@linkplain InlineDataDescriptor#size() size} is
     * disjoint from all other descriptors) that are sorted in ascending order of their
     * {@linkplain InlineDataDescriptor#startPosition() start} positions.
     *
     * @return null if no descriptors have been added to this object
     */
    public Sequence<InlineDataDescriptor> descriptors() {
        if (!_normalized) {
            normalize();
        }
        return _descriptors;
    }

    /**
     * Gets the result of {@link #descriptors()} encoded as a byte array in the format described
     * {@linkplain InlineDataDescriptor here}.
     */
    public byte[] encodedDescriptors() {
        if (_descriptors == null) {
            return null;
        }
        try {
            normalize();
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            final DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeInt(_descriptors.length());
            for (InlineDataDescriptor inlineDataDescriptor : _descriptors) {
                inlineDataDescriptor.writeTo(dataOutputStream);
            }
            final byte[] result = byteArrayOutputStream.toByteArray();
            return result;
        } catch (IOException ioException) {
            throw ProgramError.unexpected(ioException);
        }
    }

    private void normalize() {
        if (_descriptors != null && !_normalized) {
            final SortedSet<InlineDataDescriptor> sortedEntries = new TreeSet<InlineDataDescriptor>(Iterables.toCollection(_descriptors));
            final VariableSequence<InlineDataDescriptor> entries = new ArrayListSequence<InlineDataDescriptor>(_descriptors.length());
            int lastEnd = 0;
            for (InlineDataDescriptor inlineDataDescriptor : sortedEntries) {
                if (inlineDataDescriptor.startPosition() >= lastEnd) {
                    entries.append(inlineDataDescriptor);
                    lastEnd = inlineDataDescriptor.endPosition();
                }
            }
            _descriptors = entries;
            _normalized = true;
        }
    }
}
