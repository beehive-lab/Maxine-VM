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
 * Implementors of this class handle a specific command of the <a
 * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html">JDWP protocol</a>. The incoming and
 * outgoing data is specified by template parameters.
 *
 * @see com.sun.max.jdwp.data.IncomingData
 * @see com.sun.max.jdwp.data.OutgoingData
 * @author Thomas Wuerthinger
 */
public interface CommandHandler<IncomingData_Type extends IncomingData, OutgoingData_Type extends OutgoingData> {

    /**
     * Handles an incoming JDWP request and returns the outgoing data to be sent.
     *
     * @param data the incoming data
     * @param replyChannel object that can be used to communicate directly with the sender
     * @return the outgoing data that should be sent as a reply
     * @throws JDWPException after throwing this exception, an error reply packet with the exception number is sent to
     *             the client
     */
    OutgoingData_Type handle(IncomingData_Type data, JDWPSender replyChannel) throws JDWPException;

    /**
     * While reading some JDWP commands, value types must be resolved by the context of already read data. This method must return a value
     * type based on the incoming data read until now. The values of the data not yet read is uninitialized (i.e. null or 0).
     * Based on the unfinished packet, the implementor of this function should return the tag {@link com.sun.max.jdwp.constants.Tag} of
     * the next value to be read.
     *
     * WARNING: This method should not produce any side effect! There is no guarantee of the sequence of calls to this method from
     * the JDWP input stream.
     *
     * @param incomingData the data read until now
     * @return the type of the value that should be read next
     * @throws JDWPException after throwing this exception, an error reply packet will be sent to the client immediately
     */
    int helpAtDecodingUntaggedValue(IncomingData_Type incomingData) throws JDWPException;

    /**
     * For reflective instantiation, a handler must provide the class of the incoming data.
     *
     * @return the class of the incoming data
     */
    IncomingData_Type createIncomingDataObject();

    /**
     * The command id identifies a command uniquely within its command set.
     *
     * @return the command identifier
     */
    byte getCommandId();

    /**
     * The command set id has to be unique within the protocol. Together with the command id, a command is uniquely
     * defined.
     *
     * @return the command set identifier
     */
    byte getCommandSetId();

    /**
     * Static utility functions for the {@link CommandHandler} class.
     */
    public static class Static {

        /**
         * This method extracts the command name based on a command handler class. The result is heuristic and should be
         * used for debugging printouts only.
         *
         * @param handler the handler of the command, whose name should be retrieved
         * @return the name of the command
         */
        public static String getCommandName(CommandHandler<? extends IncomingData, ? extends OutgoingData> handler) {
            assert handler != null : "Cannot retrieve command name when handler object is null!";
            final Class klass = handler.getClass();
            if (klass.getDeclaringClass() != null && klass.getDeclaringClass().getDeclaringClass() != null) {
                return klass.getDeclaringClass().getDeclaringClass().getSimpleName() + " - " + klass.getDeclaringClass().getSimpleName();
            }
            return klass.getName();
        }
    }
}
