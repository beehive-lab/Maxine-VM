/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
