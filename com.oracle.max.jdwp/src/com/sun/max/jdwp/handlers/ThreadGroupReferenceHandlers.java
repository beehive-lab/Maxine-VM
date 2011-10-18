/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.jdwp.handlers;

import com.sun.max.jdwp.data.*;
import com.sun.max.jdwp.protocol.*;
import com.sun.max.jdwp.protocol.ThreadGroupReferenceCommands.*;
import com.sun.max.jdwp.vm.proxy.*;

/**
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
