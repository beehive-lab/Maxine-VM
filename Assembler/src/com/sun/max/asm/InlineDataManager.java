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
/*VCSID=df2c580f-cb34-4a29-923e-34743526b950*/
package com.sun.max.asm;

import java.io.*;
import java.util.*;

import com.sun.max.asm.dis.*;

/**
 *
 * @author Doug Simon
 */
public class InlineDataManager implements InlineDataRecorder, InlineDataDecoder {

    protected final Map<Integer, Integer> _dataSegments = new TreeMap<Integer, Integer>();

    public InlineDataManager() {
    }

    public InlineDataManager(int[] inlineDataPositions) {
        assert (inlineDataPositions.length % 2) == 0;
        for (int i = 0; i < inlineDataPositions.length; i += 2) {
            final int startPosition = inlineDataPositions[i];
            final int endPosition = inlineDataPositions[i + 1];
            assert startPosition <= endPosition;
            assert i == 0 || startPosition >= inlineDataPositions[i - 1];
            if (startPosition != endPosition) {
                record(startPosition, endPosition - startPosition);
            }
        }
    }

    public void record(int startPosition, int size) {
        _dataSegments.put(startPosition, size);
    }

    public int decodeData(int currentPosition, BufferedInputStream stream, OutputStream decodedDataBuffer) throws IOException {
        final Integer size = _dataSegments.get(currentPosition);
        if (size != null) {
            for (int i = 0; i != size; ++i) {
                final int dataByte = stream.read();
                if (decodedDataBuffer != null) {
                    decodedDataBuffer.write(dataByte);
                }
            }
            return size;
        }
        return 0;
    }

}
