/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.jdwp.vm.proxy;

/**
 * Listener interface for anybody who wants to be informed about VM events.
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
