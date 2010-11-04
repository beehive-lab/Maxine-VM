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
/**
 * @author Mick Jordan
 *
 * Common code for the Unix-specific implementations of process/thread machinery of the Inspector.
 *
 * Information needed by the Inspector, critically the location of the boot heap, is garnered from
 * a {@link com.sun.max.tele.TeleVMAgent} that communicates back to the Inspector on a socket.
 *
 * {@link com.sun.max.tele.debug.unix.UnixAgentTeleChannelProtocol} reflects this by
 * extending {@link com.sun.max.tele.TeleChannelProtocol} with a new {@code create} method
 * that takes the {@code TeleVMAgent} as an extra parameter.
 *
 * Much of the implementation of {@link com.sun.max.tele.debug.TeleProcess} is common to
 * all the Unix implementations and is captured in {@link com.sun.max.tele.debug.unix.UnixTeleProcessAdaptor}.
 * Similarly, the access to the native methods that implement the process control is common and
 * captured in {@link com.sun.max.tele.debug.unix.UnixNativeTeleChannelProtocolAdaptor}.
 *
 */
package com.sun.max.tele.debug.unix;
