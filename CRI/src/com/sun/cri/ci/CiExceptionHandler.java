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
package com.sun.cri.ci;

import com.sun.cri.ri.*;

/**
 * An implementation of the {@link RiExceptionHandler} interface.
 *
 * @author Ben L. Titzer
 */
public class CiExceptionHandler implements RiExceptionHandler {

    public final int startBCI;
    public final int endBCI;
    public final int handlerBCI;
    public final int classCPI;
    public final RiType classType;

    /**
     * Creates a new exception handler with the specified ranges.
     * @param startBCI the start index of the protected range
     * @param endBCI the end index of the protected range
     * @param catchBCI the index of the handler
     * @param classCPI the index of the throwable class in the constant pool
     * @param classType the type caught by this exception handler
     */
    public CiExceptionHandler(int startBCI, int endBCI, int catchBCI, int classCPI, RiType classType) {
        this.startBCI = startBCI;
        this.endBCI = endBCI;
        this.handlerBCI = catchBCI;
        this.classCPI = classCPI;
        this.classType = classType;
    }

    public int startBCI() {
        return startBCI;
    }

    public int endBCI() {
        return endBCI;
    }

    public int handlerBCI() {
        return handlerBCI;
    }

    public int catchClassIndex() {
        return classCPI;
    }

    public boolean isCatchAll() {
        return classCPI == 0;
    }

    public RiType catchKlass() {
        return classType;
    }
}
