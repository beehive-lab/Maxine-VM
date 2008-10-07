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
/*VCSID=f4829f2c-c62e-483c-9723-b57ab16069f7*/

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
