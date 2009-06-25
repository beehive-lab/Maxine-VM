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
package com.sun.max.jdwp.handlers;

import com.sun.max.jdwp.data.*;
import com.sun.max.jdwp.protocol.*;
import com.sun.max.jdwp.protocol.ThreadGroupReferenceCommands.*;
import com.sun.max.jdwp.vm.proxy.*;

/**
 * @author Thomas Wuerthinger
 * 
 */
public class ThreadGroupReferenceHandlers extends Handlers {

    public ThreadGroupReferenceHandlers(JDWPSession session) {
        super(session);
    }

    @Override
    public void registerWith(CommandHandlerRegistry registry) {
        registry.addCommandHandler(new NameHandler());
        registry.addCommandHandler(new ParentHandler());
        registry.addCommandHandler(new ChildrenHandler());
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadGroupReference_Name">
     * JDWP specification for command ThreadGroupReference-Name.</a>
     */
    private class NameHandler extends ThreadGroupReferenceCommands.Name.Handler {

        @Override
        public Name.Reply handle(Name.IncomingRequest incomingRequest) throws JDWPException {
            final String name = session().getThreadGroup(incomingRequest.group).getName();
            return new Name.Reply(name);
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadGroupReference_Parent">
     * JDWP specification for command ThreadGroupReference-Parent.</a>
     */
    private class ParentHandler extends ThreadGroupReferenceCommands.Parent.Handler {

        @Override
        public Parent.Reply handle(Parent.IncomingRequest incomingRequest) throws JDWPException {
            final ThreadGroupProvider p = session().getThreadGroup(incomingRequest.group).getParent();
            return new Parent.Reply(session().toID(p));
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadGroupReference_Children">
     * JDWP specification for command ThreadGroupReference-Children.</a>
     */
    private class ChildrenHandler extends ThreadGroupReferenceCommands.Children.Handler {

        @Override
        public Children.Reply handle(Children.IncomingRequest incomingRequest) throws JDWPException {
            final ThreadGroupProvider p = session().getThreadGroup(incomingRequest.group);
            final Children.Reply reply = new Children.Reply();
            final ThreadProvider[] threads = p.getThreadChildren();
            final ThreadGroupProvider[] threadGroups = p.getThreadGroupChildren();

            reply.childGroups = new ID.ThreadGroupID[threadGroups.length];
            for (int i = 0; i < threadGroups.length; i++) {
                reply.childGroups[i] = session().toID(threadGroups[i]);
            }

            reply.childThreads = new ID.ThreadID[threads.length];
            for (int i = 0; i < threads.length; i++) {
                reply.childThreads[i] = session().toID(threads[i]);
                assert threads[i].getThreadGroup() == p : "Thread group must match!";
            }
            return reply;
        }
    }
}
