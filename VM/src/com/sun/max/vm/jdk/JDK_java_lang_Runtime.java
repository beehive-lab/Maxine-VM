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
/*VCSID=7ce59958-7bce-4615-8dba-f283988ce702*/
package com.sun.max.vm.jdk;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;

/**
 * Implements method substitutions for {@link java.lang.Runtime java.lang.Runtime}.
 * 
 * @author Bernd Mathiske
 */
@METHOD_SUBSTITUTIONS(Runtime.class)
public final class JDK_java_lang_Runtime {

    private JDK_java_lang_Runtime() {
    }

    /**
     * Returns the amount of free memory.
     * @see java.lang.Runtime#freeMemory()
     * @return the amount of free memory in bytes
     */
    @SUBSTITUTE
    private long freeMemory() {
        return Heap.reportFreeSpace().toLong();
    }

    /**
     * Returns the total amount of memory available to the virtual machine.
     * @see java.lang.Runtime#totalMemory()
     * @return the total amount of memory available to the virtual machine in bytes
     */
    @SUBSTITUTE
    private long totalMemory() {
        // TODO: ask the OS what is available beyond this
        return Heap.maxSize().toLong();
    }

    /**
     * Returns the maximum heap size.
     * @see java.lang.Runtime#maxMemory()
     * @return the maximum heap size in bytes
     */
    @SUBSTITUTE
    private long maxMemory() {
        return Heap.maxSize().toLong();
    }

    /**
     * Request a garbage collection.
     * @see java.lang.Runtime#gc()
     */
    @SUBSTITUTE
    private void gc() {
        Heap.collectGarbage(Size.zero());
    }

    /**
     * Invoke finalizers of garbage collected objects.
     * @see java.lang.Runtime#runFinalization()
     */
    @SUBSTITUTE
    private static void runFinalization0() {
        Heap.runFinalization();
    }

    /**
     * Turn tracing of instructions on or off. Ignored.
     * @param on {@code true} if instructions should be traced; {@code false} otherwise
     */
    @SUBSTITUTE
    private void traceInstructions(boolean on) {
        // do nothing.
    }

    /**
     * Turn tracing of method calls on or off. Ignored.
     * @param on {@code true} if the instructions should be traced; {@code false} otherwise
     */
    @SUBSTITUTE
    private void traceMethodCalls(boolean on) {
        // do nothing.
    }
}
