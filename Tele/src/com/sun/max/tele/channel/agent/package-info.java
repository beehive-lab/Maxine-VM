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
 * It is possible to run the Inspector on a separate machine from the target Maxine VM using
 * the code in this package. A small agent implemented by {@link com.sun.max.tele.channel.agent.InspectorAgent}
 * runs on the target machine, listening for connections from the remote Inspector process on a TCP port
 * (default value {@value com.sun.max.tele.channel.tcp.TCPTeleChannelProtocol#DEFAULT_PORT}).
 *
 * Note this package defines stub variants of {@link com.sun.max.tele.debug.TeleProcess} and
 * {@link com.sun.max.tele.debug.TeleNativeThread}. This keeps the type system happy while
 * allowing the implementation of thread gathering to work remotely..
 *
 * The generic part of the remote method communication implementation is  in {@link com.sun.max.tele.channel.agent.RemoteInvocationProtocolAdaptor}.
 * {@link com.sun.max.tele.channel.agent.AgentProtocolAdaptor} subclasses this to provide specific support for the
 * actual {@link TeleChannelProtocol} adaptors, specifically to specify the handling of arrays, and implement the
 * interface by delegating to a specific target protocol implementation. Note that the delegated-to implementation is
 * exactly the one that is used in the non-separate configuration.
 */
package com.sun.max.tele.channel.agent;
