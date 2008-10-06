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
/*VCSID=a4c201a1-0544-4d3b-a3c9-1ffb2da6cce6*/

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
