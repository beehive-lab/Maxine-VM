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
/*VCSID=722af4ec-be92-494c-aa58-756a375dfefa*/
package com.sun.max.jdwp.handlers;

import com.sun.max.jdwp.data.*;
import com.sun.max.jdwp.data.ID.*;
import com.sun.max.jdwp.protocol.*;
import com.sun.max.jdwp.protocol.ThreadReferenceCommands.*;
import com.sun.max.jdwp.protocol.ThreadReferenceCommands.ThreadGroup;
import com.sun.max.jdwp.vm.proxy.*;

/**
 * @author Thomas Wuerthinger
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
            final ID.ThreadID thread = incomingRequest._thread;
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
            final ThreadProvider threadProvider = session().getThread(incomingRequest._thread);
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
            final ThreadProvider threadProvider = session().getThread(incomingRequest._thread);
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
            final ThreadProvider thread = session().getThread(incomingRequest._thread);
            final Status.Reply reply = new Status.Reply();
            reply._suspendStatus = session().getSuspendStatus(thread);
            reply._threadStatus = session().getThreadStatus(thread);
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
            final ThreadProvider threadProvider = session().getThread(incomingRequest._thread);
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
            final ThreadProvider threadProvider = session().getThread(incomingRequest._thread);
            final FrameProvider[] frames = threadProvider.getFrames();

            final Frames.Reply reply = new Frames.Reply();
            reply._frames = new Frames.Frame[frames.length];

            for (int i = 0; i < frames.length; i++) {
                final Frames.Frame frame = new Frames.Frame();
                frame._frameID = session().toID(threadProvider, frames[i]);
                final FrameProvider frameProvider = frames[i];
                frame._location = session().fromCodeLocation(frameProvider.getLocation());
                reply._frames[i] = frame;
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
            final ThreadProvider thread = session().getThread(incomingRequest._thread);
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
            r._owned = new JDWPValue[0];
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
            r._monitor = new JDWPValue(ObjectID.create(0, ObjectID.class));
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
            final ThreadProvider thread = session().getThread(incomingRequest._thread);
            final ObjectProvider exception = session().getObject(incomingRequest._throwable);
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
            final ThreadProvider thread = session().getThread(incomingRequest._thread);
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
            final ThreadProvider thread = session().getThread(incomingRequest._thread);
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
