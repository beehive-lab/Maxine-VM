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
package com.sun.max.jdwp.vm.proxy;

/**
 * Listener interface for anybody who wants to be informed about VM events.
 *
 * @author Thomas Wuerthinger
 *
 */
public interface VMListener {

    /**
     * This method is called when the VM is started.
     */
    void vmStarted();

    /**
     * This method is called when the VM is shutdown.
     */
    void vmDied();

    /**
     * Informs about the start of a thread.
     *
     * @param thread the thread that has recently been started
     */
    void threadStarted(ThreadProvider thread);

    /**
     * Informs about the death of a thread.
     *
     * @param thread the thread that has recently died
     */
    void threadDied(ThreadProvider thread);

    /**
     * Informs about the preparation of a newly loaded class.
     *
     * @param thread the thread that loaded the class
     * @param klass the class that was loaded
     */
    void classPrepared(ThreadProvider thread, ClassProvider klass);

    /**
     * Informs about unloading a class.
     *
     * @param thread the thread that unloaded the class
     * @param klass the class that was unloaded
     */
    void classUnloaded(ThreadProvider thread, ClassProvider klass);

    /**
     * This method is called whenever a breakpoint is hit.
     *
     * @param thread the thread that hit the breakpoint
     * @param location the code location identifying the breakpoint
     */
    void breakpointHit(ThreadProvider thread, JdwpCodeLocation location);

    /**
     * This method is called from the VM whenever a single step is made.
     *
     * @param thread the thread that performed the single step
     * @param location the code location specifying the current instruction pointer of the thread, i.e. the code location after the single step was made
     */
    void singleStepMade(ThreadProvider thread, JdwpCodeLocation location);
}
