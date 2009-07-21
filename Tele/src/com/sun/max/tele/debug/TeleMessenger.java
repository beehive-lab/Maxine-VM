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
package com.sun.max.tele.debug;

import com.sun.max.tele.*;
import com.sun.max.vm.actor.member.*;

public interface TeleMessenger {

    /**
     * Writes information into the {@link TeleVM} causing it to set up for two way messaging;
     * must be done early in the startup sequence.
     */
    void enable();

    /**
     * Completes the set-up of two way messaging with the {@link TeleVM}, if not
     * yet done.
     * Requires that {@link #enable()} has been called early in the startup sequence.
     *
     * @return whether two-way messaging is active.
     */
    boolean activate();

    /**
     * @param methodKey
     * @param bytecodePosition < 0 selects homogeneous call entry point
     */
    void requestBytecodeBreakpoint(MethodKey methodKey, int bytecodePosition);

    /**
     * @param methodKey
     * @param bytecodePosition < 0 selects homogeneous call entry point
     */
    void cancelBytecodeBreakpoint(MethodKey methodKey, int bytecodePosition);

}
