/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.max.jdwp.data;

/**
 * Implementors of this interface are capable of managing a set of {@link CommandHandler} objects.
 *
 * @author Thomas Wuerthinger
 */
public interface CommandHandlerRegistry {

    /**
     * Searches the registry for a command handler based on a command id and a command set id.
     *
     * @param commandSetId the id of the command set containing the command
     * @param commandId the id of the command that the handler can process
     * @return the handler or null if no handler for the command is found
     */
    CommandHandler<? extends IncomingData, ? extends OutgoingData> findCommandHandler(byte commandSetId, byte commandId);

    /**
     * Adds a new command handler to the registry.
     *
     * @param handler the handler to be added
     */
    void addCommandHandler(CommandHandler<? extends IncomingData, ? extends OutgoingData> handler);
}
