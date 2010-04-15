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

/**
 * This class contains statistics gathered during the compilation of a method and reported back
 * from the compiler as the result of compilation.
 *
 * @author Ben L. Titzer
 */
public class CiStatistics {

    /**
     * The total number of bytes of bytecode parsed during this compilation, including any inlined methods.
     */
    public int byteCount;

    /**
     * The number of internal graph nodes created during this compilation.
     */
    public int nodeCount;

    /**
     * The number of basic blocks created during this compilation.
     */
    public int blockCount;

    /**
     * The number of loops in the compiled method.
     */
    public int loopCount;

    /**
     * The number of methods inlined.
     */
    public int inlineCount;

    /**
     * The number of methods folded (i.e. evaluated).
     */
    public int foldCount;

    /**
     * The number of intrinsics inlined in this compilation.
     */
    public int intrinsicCount;

}
