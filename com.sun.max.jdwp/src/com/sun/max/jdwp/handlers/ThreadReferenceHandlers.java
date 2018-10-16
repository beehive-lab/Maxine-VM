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
import com.sun.max.jdwp.data.ID.*;
import com.sun.max.jdwp.protocol.*;
import com.sun.max.jdwp.protocol.ThreadReferenceCommands.*;
import com.sun.max.jdwp.protocol.ThreadReferenceCommands.ThreadGroup;
import com.sun.max.jdwp.vm.proxy.*;

/**
 *
 */
public class ThreadReferenceHandlers extends Handlers {

    public ThreadReferenceHandlers(JDWPSession session) {
        super(session);
    }

    @Override
    public void registerWith(CommandHandlerRegistry registry) {
        registry.addCommandHandler(new NameHandler());
        registry.addCommandHandler(new SuspendHandler());
        registry.addCommandHandler(new ResumeHandler());
        registry.addCommandHandler(new StatusHandler());
        registry.addCommandHandler(new ThreadGroupHandler());
        registry.addCommandHandler(new FramesHandler());
        registry.addCommandHandler(new FrameCountHandler());
        registry.addCommandHandler(new OwnedMonitorsHandler());
        registry.addCommandHandler(new CurrentContendedMonitorHandler());
        registry.addCommandHandler(new StopHandler());
        registry.addCommandHandler(new InterruptHandler());
        registry.addCommandHandler(new SuspendCountHandler());
        registry.addCommandHandler(new OwnedMonitorsStackDepthInfoHandler());
        registry.addCommandHandler(new ForceEarlyReturnHandler());
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadReference_Name">
     * JDWP specification for command ThreadReference-Name.</a>
     */
    private class NameHandler extends ThreadReferenceCommands.Name.Handler {

        @Override
        public Name.Reply handle(Name.IncomingRequest incomingRequest) throws JDWPException {
            final ID.ThreadID thread = incomingRequest.thread;
            final ThreadProvider threadProvider = session().getThread(thread);
            return new Name.Reply(threadProvider.getName());
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadReference_Suspend">
     * JDWP specification for command ThreadReference-Suspend.</a>
     */
    private class SuspendHandler extends ThreadReferenceCommands.Suspend.Handler {

        @Override
        public Suspend.Reply handle(Suspend.IncomingRequest incomingRequest) throws JDWPException {
            final ThreadProvider threadProvider = session().getThread(incomingRequest.thread);
            threadProvider.suspend();
            return new Suspend.Reply();
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadReference_Resume">
     * JDWP specification for command ThreadReference-Resume.</a>
     */
    private class ResumeHandler extends ThreadReferenceCommands.Resume.Handler {

        @Override
        public Resume.Reply handle(Resume.IncomingRequest incomingRequest) throws JDWPException {
            final ThreadProvider threadProvider = session().getThread(incomingRequest.thread);
            threadProvider.resume();
            return new Resume.Reply();
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadReference_Status">
     * JDWP specification for command ThreadReference-Status.</a>
     */
    private class StatusHandler extends ThreadReferenceCommands.Status.Handler {

        @Override
        public Status.Reply handle(Status.IncomingRequest incomingRequest) throws JDWPException {
            final ThreadProvider thread = session().getThread(incomingRequest.thread);
            final Status.Reply reply = new Status.Reply();
            reply.suspendStatus = session().getSuspendStatus(thread);
            reply.threadStatus = session().getThreadStatus(thread);
            return reply;
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadReference_ThreadGroup">
     * JDWP specification for command ThreadReference-ThreadGroup.</a>
     */
    private class ThreadGroupHandler extends ThreadReferenceCommands.ThreadGroup.Handler {

        @Override
        public ThreadGroup.Reply handle(ThreadGroup.IncomingRequest incomingRequest) throws JDWPException {
            final ThreadProvider threadProvider = session().getThread(incomingRequest.thread);
            final ThreadGroupProvider threadGroupProvider = threadProvider.getThreadGroup();
            return new ThreadGroup.Reply(session().toID(threadGroupProvider));
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadReference_Frames">
     * JDWP specification for command ThreadReference-Frames.</a>
     */
    private class FramesHandler extends ThreadReferenceCommands.Frames.Handler {

        @Override
        public Frames.Reply handle(Frames.IncomingRequest incomingRequest) throws JDWPException {
            final ThreadProvider threadProvider = session().getThread(incomingRequest.thread);
            final FrameProvider[] frames = threadProvider.getFrames();

            final Frames.Reply reply = new Frames.Reply();
            reply.frames = new Frames.Frame[frames.length];

            for (int i = 0; i < frames.length; i++) {
                final Frames.Frame frame = new Frames.Frame();
                frame.frameID = session().toID(threadProvider, frames[i]);
                final FrameProvider frameProvider = frames[i];
                frame.location = session().fromCodeLocation(frameProvider.getLocation());
                reply.frames[i] = frame;
            }
            return reply;
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadReference_FrameCount">
     * JDWP specification for command ThreadReference-FrameCount.</a>
     */
    private class FrameCountHandler extends ThreadReferenceCommands.FrameCount.Handler {

        @Override
        public FrameCount.Reply handle(FrameCount.IncomingRequest incomingRequest) throws JDWPException {
            final ThreadProvider thread = session().getThread(incomingRequest.thread);
            final FrameProvider[] frames = thread.getFrames();
            return new FrameCount.Reply(frames.length);
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadReference_OwnedMonitors">
     * JDWP specification for command ThreadReference-OwnedMonitors.</a>
     */
    private class OwnedMonitorsHandler extends ThreadReferenceCommands.OwnedMonitors.Handler {

        @Override
        public OwnedMonitors.Reply handle(OwnedMonitors.IncomingRequest incomingRequest) throws JDWPException {

            // TODO: Consider implementing this correctly!
            final OwnedMonitors.Reply r = new OwnedMonitors.Reply();
            r.owned = new JDWPValue[0];
            return r;
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadReference_CurrentContendedMonitor">
     * JDWP specification for command ThreadReference-CurrentContendedMonitor.</a>
     */
    private class CurrentContendedMonitorHandler extends ThreadReferenceCommands.CurrentContendedMonitor.Handler {

        @Override
        public CurrentContendedMonitor.Reply handle(CurrentContendedMonitor.IncomingRequest incomingRequest) throws JDWPException {

            // TODO: Consider implementing this correctly!
            final CurrentContendedMonitor.Reply r = new CurrentContendedMonitor.Reply();
            r.monitor = new JDWPValue(ObjectID.create(0, ObjectID.class));
            return r;
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadReference_Stop">
     * JDWP specification for command ThreadReference-Stop.</a>
     */
    private class StopHandler extends ThreadReferenceCommands.Stop.Handler {

        @Override
        public Stop.Reply handle(Stop.IncomingRequest incomingRequest) throws JDWPException {
            final ThreadProvider thread = session().getThread(incomingRequest.thread);
            final ObjectProvider exception = session().getObject(incomingRequest.throwable);
            thread.stop(exception);
            return new Stop.Reply();
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadReference_Interrupt">
     * JDWP specification for command ThreadReference-Interrupt.</a>
     */
    private class InterruptHandler extends ThreadReferenceCommands.Interrupt.Handler {

        @Override
        public Interrupt.Reply handle(Interrupt.IncomingRequest incomingRequest) throws JDWPException {
            final ThreadProvider thread = session().getThread(incomingRequest.thread);
            thread.interrupt();
            return new Interrupt.Reply();
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadReference_SuspendCount">
     * JDWP specification for command ThreadReference-SuspendCount.</a>
     */
    private class SuspendCountHandler extends ThreadReferenceCommands.SuspendCount.Handler {

        @Override
        public SuspendCount.Reply handle(SuspendCount.IncomingRequest incomingRequest) throws JDWPException {
            final ThreadProvider thread = session().getThread(incomingRequest.thread);
            return new SuspendCount.Reply(thread.suspendCount());
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadReference_OwnedMonitorsStackDepthInfo">
     * JDWP specification for command ThreadReference-OwnedMonitorsStackDepthInfo.</a>
     */
    private class OwnedMonitorsStackDepthInfoHandler extends ThreadReferenceCommands.OwnedMonitorsStackDepthInfo.Handler {

        @Override
        public OwnedMonitorsStackDepthInfo.Reply handle(OwnedMonitorsStackDepthInfo.IncomingRequest incomingRequest) throws JDWPException {
            // TODO: Consider implementing this.
            throw new JDWPNotImplementedException();
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ThreadReference_ForceEarlyReturn">
     * JDWP specification for command ThreadReference-ForceEarlyReturn.</a>
     */
    private class ForceEarlyReturnHandler extends ThreadReferenceCommands.ForceEarlyReturn.Handler {

        @Override
        public ForceEarlyReturn.Reply handle(ForceEarlyReturn.IncomingRequest incomingRequest) throws JDWPException {
            // TODO: Consider implementing this.
            throw new JDWPNotImplementedException();
        }
    }
}
