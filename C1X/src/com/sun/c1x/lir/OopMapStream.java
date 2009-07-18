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
package com.sun.c1x.lir;

import com.sun.c1x.lir.OopMapValue.*;

/**
 * The <code>OopMapStream</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class OopMapStream {

    private CompressedReadStream stream;
    private int mask;
    private int size;
    private int position;
    private boolean validOmv;
    private OopMapValue omv;

    public OopMapStream(OopMap oopMap) {
        stream = new CompressedReadStream(oopMap.writeStream().buffer());
        if (oopMap.omvData() == null) {
            stream = new CompressedReadStream(oopMap.writeStream().buffer());
        } else {
            stream = new CompressedReadStream(oopMap.omvData());
        }
        mask = OopMapValueMask.TypeMaskInPlace.value;
        size = oopMap.omvCount();
        position = 0;
        validOmv = false;
    }

    public OopMapStream(OopMap oopMap, int oopTypesMask) {
        if (oopMap.omvData() == null) {
            stream = new CompressedReadStream(oopMap.writeStream().buffer());
        } else {
            stream = new CompressedReadStream(oopMap.omvData());
        }
        mask = oopTypesMask;
        size = oopMap.omvCount();
        position = 0;
        validOmv = false;
    }

    private void findNext() {
        while (position++ < size) {
            omv.readFrom(stream);
            if ((omv.type().mask() & mask) > 0) {
                validOmv = true;
                return;
            }
        }
        validOmv = false;
    }

    public boolean isDone() {
        if (!validOmv) {
            findNext();
        }
        return !validOmv;
    }

    public void next() {
        findNext();
    }

    public OopMapValue current() {
        return omv;
    }
}
