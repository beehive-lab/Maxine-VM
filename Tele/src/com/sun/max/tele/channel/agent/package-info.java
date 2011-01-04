/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
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
