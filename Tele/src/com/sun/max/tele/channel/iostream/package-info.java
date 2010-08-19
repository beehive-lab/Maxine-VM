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
 * {@link com.sun.max.tele.TeleChannelDataIOProtocolImpl} is an implementation of the
 * method-based protocol defined by {@link TeleChannelProtocol} that uses
 * {@link java.io.DataInputStream} and {@link java.io.DataOutputStream}.
 *
 * {@link com.sun.max.tele.TeleChannelDataIOProtocolAdaptor} is a subclass
 * that handles the methods containing object types, translating them into simple
 * types that can be handled by {@link com.sun.max.tele.TeleChannelDataIOProtocolImpl}.
 *
 * @author Mick Jordan
 */
package com.sun.max.tele.channel.iostream;

