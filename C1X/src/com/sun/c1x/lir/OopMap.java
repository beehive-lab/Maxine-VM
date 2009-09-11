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

import com.sun.c1x.ci.*;
import com.sun.c1x.util.*;

/**
 * The <code>OopMap</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class OopMap {


    private final int frameSize;
    private final CiTarget target;
    private final FrameMap frameMap;

    private BitMap stackMap;
    private BitMap registerMap;

    public OopMap(FrameMap frameMap, int frameSize, CiTarget target) {
        this.frameSize = frameSize;
        this.target = target;
        this.frameMap = frameMap;
        initMaps();
    }

    private void initMaps() {
        assert frameSize % target.arch.wordSize == 0 : "must be aligned";
        stackMap = new BitMap(frameSize / target.arch.wordSize);
        registerMap = new BitMap(target.registerReferenceMapTemplate.length);
    }

    public void setOop(CiLocation location) {
        if (location.isStackOffset()) {
            int offset = location.stackOffset;
            assert offset % target.arch.wordSize == 0 : "must be aligned";
            int stackMapIndex = offset / target.arch.wordSize;
            stackMap.set(stackMapIndex);
        } else {
            assert location.isSingleRegister() : "objects can only be in a single register";
            for (int i = 0; i < target.registerReferenceMapTemplate.length; i++) {
                if (target.registerReferenceMapTemplate[i] == location.first) {
                    assert !registerMap.get(i) : "bit already set";
                    registerMap.set(i);
                }
            }
        }
    }

    public OopMap deepCopy() {
        OopMap result = new OopMap(frameMap, frameSize, target);
        result.stackMap = stackMap.copy();
        result.registerMap = registerMap.copy();
        return result;
    }

    public boolean[] registerMap() {
        return registerMap.toArray();
    }

    public boolean[] stackMap() {
        return stackMap.toArray();
    }
}
