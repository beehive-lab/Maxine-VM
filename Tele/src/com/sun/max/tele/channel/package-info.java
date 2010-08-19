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
 * The Inspector communicates with the target VM through a {@link com.sun.max.tele.TeleChannel} using the
 * method-based protocol defined by {@link com.sun.max.tele.TeleChannelProtocol}. To simplify
 * an architecture where the Inspector runs on one (client) machine and the VM runs on a separate
 * (target) machine a minimal variant of the protocol, {@link com.sun.max.tele.TeleChannelDataIOProtocol},
 * that is capable of being implemented using {@link java.io.DataInputStream} and {@link java.io.DataOutputStream}
 * is also defined.
 *
 * @author Mick Jordan
 */
package com.sun.max.tele.channel;

