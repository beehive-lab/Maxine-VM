/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.jdwp.protocol.VirtualMachineCommands.*;
import com.sun.max.jdwp.vm.proxy.*;

/**
 * @author Thomas Wuerthinger
 *
 */
public class VirtualMachineHandlers extends Handlers {

    public VirtualMachineHandlers(JDWPSession session) {
        super(session);
    }

    @Override
    public void registerWith(CommandHandlerRegistry registry) {
        registry.addCommandHandler(new VersionHandler());
        registry.addCommandHandler(new ClassesBySignatureHandler());
        registry.addCommandHandler(new AllClassesHandler());
        registry.addCommandHandler(new AllThreadsHandler());
        registry.addCommandHandler(new TopLevelThreadGroupsHandler());
        registry.addCommandHandler(new DisposeHandler());
        registry.addCommandHandler(new IDSizesHandler());
        registry.addCommandHandler(new SuspendHandler());
        registry.addCommandHandler(new ResumeHandler());
        registry.addCommandHandler(new ExitHandler());
        registry.addCommandHandler(new CreateStringHandler());
        registry.addCommandHandler(new CapabilitiesHandler());
        registry.addCommandHandler(new ClassPathsHandler());
        registry.addCommandHandler(new DisposeObjectsHandler());
        registry.addCommandHandler(new HoldEventsHandler());
        registry.addCommandHandler(new ReleaseEventsHandler());
        registry.addCommandHandler(new CapabilitiesNewHandler());
        registry.addCommandHandler(new RedefineClassesHandler());
        registry.addCommandHandler(new SetDefaultStratumHandler());
        registry.addCommandHandler(new AllClassesWithGenericHandler());
        registry.addCommandHandler(new InstanceCountsHandler());
    }

    private VMAccess vm() {
        return session().vm();
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_Version">
     * JDWP specification for command VirtualMachine-Version.</a>
     */
    private class VersionHandler extends VirtualMachineCommands.Version.Handler {

        @Override
        public Version.Reply handle(Version.IncomingRequest incomingRequest) throws JDWPException {
            final Version.Reply reply = new Version.Reply();
            reply.jdwpMajor = 1;
            reply.jdwpMinor = 5;
            reply.vmName = session().vm().getName();
            reply.vmVersion = session().vm().getVersion();
            reply.description = session().vm().getDescription();
            return reply;
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_ClassesBySignature">
     * JDWP specification for command VirtualMachine-ClassesBySignature.</a>
     */
    private class ClassesBySignatureHandler extends VirtualMachineCommands.ClassesBySignature.Handler {

        @Override
        public ClassesBySignature.Reply handle(ClassesBySignature.IncomingRequest incomingRequest) throws JDWPException {
            final ReferenceTypeProvider[] refs = vm().getReferenceTypesBySignature(incomingRequest.signature);
            final ClassesBySignature.Reply reply = new ClassesBySignature.Reply();
            reply.classes = new ClassesBySignature.ClassInfo[refs.length];
            for (int i = 0; i < refs.length; i++) {
                final ClassesBySignature.ClassInfo classInfo = new ClassesBySignature.ClassInfo();
                classInfo.typeID = session().toID(refs[i]);
                final ReferenceTypeProvider refType = refs[i];
                classInfo.status = refType.getStatus();
                classInfo.refTypeTag = session().getTypeTag(refType);
                reply.classes[i] = classInfo;
            }
            return reply;
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_AllClasses">
     * JDWP specification for command VirtualMachine-AllClasses.</a>
     */
    private class AllClassesHandler extends VirtualMachineCommands.AllClasses.Handler {

        @Override
        public AllClasses.Reply handle(AllClasses.IncomingRequest incomingRequest) throws JDWPException {
            final ReferenceTypeProvider[] refs = vm().getAllReferenceTypes();
            final AllClasses.Reply reply = new AllClasses.Reply();
            reply.classes = new AllClasses.ClassInfo[refs.length];
            for (int i = 0; i < refs.length; i++) {
                final AllClasses.ClassInfo c = new AllClasses.ClassInfo();
                c.typeID = session().toID(refs[i]);
                final ReferenceTypeProvider refType = refs[i];
                c.status = refType.getStatus();
                c.refTypeTag = session().getTypeTag(refType);
                c.signature = refType.getSignature();
                reply.classes[i] = c;
            }
            return reply;
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_AllThreads">
     * JDWP specification for command VirtualMachine-AllThreads.</a>
     */
    private class AllThreadsHandler extends VirtualMachineCommands.AllThreads.Handler {

        @Override
        public AllThreads.Reply handle(AllThreads.IncomingRequest incomingRequest) throws JDWPException {
            final AllThreads.Reply reply = new AllThreads.Reply();
            final ThreadProvider[] threadProvider = vm().getAllThreads();
            final ID.ThreadID[] threads = new ID.ThreadID[threadProvider.length];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = session().toID(threadProvider[i]);
            }
            reply.threads = threads;
            return reply;
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_TopLevelThreadGroups">
     * JDWP specification for command VirtualMachine-TopLevelThreadGroups.</a>
     */
    private class TopLevelThreadGroupsHandler extends VirtualMachineCommands.TopLevelThreadGroups.Handler {

        @Override
        public TopLevelThreadGroups.Reply handle(TopLevelThreadGroups.IncomingRequest incomingRequest) throws JDWPException {
            final TopLevelThreadGroups.Reply reply = new TopLevelThreadGroups.Reply();
            final ThreadGroupProvider[] threadGroups = session().vm().getThreadGroups();
            reply.groups = new ID.ThreadGroupID[threadGroups.length];
            for (int i = 0; i < threadGroups.length; i++) {
                reply.groups[i] = session().toID(threadGroups[i]);
            }
            return reply;
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_Dispose">
     * JDWP specification for command VirtualMachine-Dispose.</a>
     */
    private class DisposeHandler extends VirtualMachineCommands.Dispose.Handler {

        @Override
        public Dispose.Reply handle(Dispose.IncomingRequest incomingRequest) throws JDWPException {
            vm().dispose();
            return new Dispose.Reply();
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_IDSizes">
     * JDWP specification for command VirtualMachine-IDSizes.</a>
     */
    private class IDSizesHandler extends VirtualMachineCommands.IDSizes.Handler {

        public static final int ID_SIZE = 8;

        @Override
        public IDSizes.Reply handle(IDSizes.IncomingRequest incomingRequest) throws JDWPException {
            final IDSizes.Reply reply = new IDSizes.Reply();
            reply.fieldIDSize = ID_SIZE;
            reply.frameIDSize = ID_SIZE;
            reply.methodIDSize = ID_SIZE;
            reply.objectIDSize = ID_SIZE;
            reply.referenceTypeIDSize = ID_SIZE;
            return reply;
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_Suspend">
     * JDWP specification for command VirtualMachine-Suspend.</a>
     */
    private class SuspendHandler extends VirtualMachineCommands.Suspend.Handler {

        @Override
        public Suspend.Reply handle(Suspend.IncomingRequest incomingRequest) throws JDWPException {
            vm().suspend();
            return new Suspend.Reply();
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_Resume">
     * JDWP specification for command VirtualMachine-Resume.</a>
     */
    private class ResumeHandler extends VirtualMachineCommands.Resume.Handler {

        @Override
        public Resume.Reply handle(Resume.IncomingRequest incomingRequest) throws JDWPException {
            vm().resume();
            return new Resume.Reply();
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_Exit"> JDWP
     * specification for command VirtualMachine-Exit.</a>
     */
    private class ExitHandler extends VirtualMachineCommands.Exit.Handler {

        @Override
        public Exit.Reply handle(Exit.IncomingRequest incomingRequest) throws JDWPException {
            vm().exit(incomingRequest.exitCode);
            return new Exit.Reply();
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_CreateString">
     * JDWP specification for command VirtualMachine-CreateString.</a>
     */
    private class CreateStringHandler extends VirtualMachineCommands.CreateString.Handler {

        @Override
        public CreateString.Reply handle(CreateString.IncomingRequest incomingRequest) throws JDWPException {
            final CreateString.Reply reply = new CreateString.Reply();
            reply.stringObject = session().toID(vm().createString(incomingRequest.utf));
            if (reply.stringObject.value() == 0) {
                throw new JDWPNotImplementedException();
            }
            return reply;
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_Capabilities">
     * JDWP specification for command VirtualMachine-Capabilities.</a>
     */
    private class CapabilitiesHandler extends VirtualMachineCommands.Capabilities.Handler {

        @Override
        public Capabilities.Reply handle(Capabilities.IncomingRequest incomingRequest) throws JDWPException {
            final Capabilities.Reply reply = new Capabilities.Reply();
            reply.canGetBytecodes = false;
            reply.canGetCurrentContendedMonitor = false;
            reply.canGetMonitorInfo = false;
            reply.canGetOwnedMonitorInfo = false;
            reply.canGetSyntheticAttribute = false;
            reply.canWatchFieldAccess = false;
            reply.canWatchFieldModification = false;
            return reply;
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_ClassPaths">
     * JDWP specification for command VirtualMachine-ClassPaths.</a>
     */
    private class ClassPathsHandler extends VirtualMachineCommands.ClassPaths.Handler {

        @Override
        public ClassPaths.Reply handle(ClassPaths.IncomingRequest incomingRequest) throws JDWPException {
            final ClassPaths.Reply reply = new ClassPaths.Reply();
            reply.baseDir = "";
            reply.classpaths = vm().getClassPath();
            reply.bootclasspaths = vm().getBootClassPath();
            return reply;
        }

    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_DisposeObjects">
     * JDWP specification for command VirtualMachine-DisposeObjects.</a>
     */
    private class DisposeObjectsHandler extends VirtualMachineCommands.DisposeObjects.Handler {

        @Override
        public DisposeObjects.Reply handle(DisposeObjects.IncomingRequest incomingRequest) throws JDWPException {
            // TODO: Consider implementation of this for performance reasons.
            return new DisposeObjects.Reply();
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_HoldEvents">
     * JDWP specification for command VirtualMachine-HoldEvents.</a>
     */
    private class HoldEventsHandler extends VirtualMachineCommands.HoldEvents.Handler {

        @Override
        public HoldEvents.Reply handle(HoldEvents.IncomingRequest incomingRequest) throws JDWPException {
            session().holdEvents();
            return new HoldEvents.Reply();
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_ReleaseEvents">
     * JDWP specification for command VirtualMachine-ReleaseEvents.</a>
     */
    private class ReleaseEventsHandler extends VirtualMachineCommands.ReleaseEvents.Handler {

        @Override
        public ReleaseEvents.Reply handle(ReleaseEvents.IncomingRequest incomingRequest) throws JDWPException {
            session().releaseEvents();
            return new ReleaseEvents.Reply();
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_CapabilitiesNew">
     * JDWP specification for command VirtualMachine-CapabilitiesNew.</a>
     */
    private class CapabilitiesNewHandler extends VirtualMachineCommands.CapabilitiesNew.Handler {

        @Override
        public CapabilitiesNew.Reply handle(CapabilitiesNew.IncomingRequest incomingRequest) throws JDWPException {
            final CapabilitiesNew.Reply reply = new CapabilitiesNew.Reply();
            reply.canAddMethod = false;
            reply.canForceEarlyReturn = false;
            reply.canGetBytecodes = false;
            reply.canGetConstantPool = false;
            reply.canGetCurrentContendedMonitor = false;
            reply.canGetInstanceInfo = false;
            reply.canGetMonitorFrameInfo = false;
            reply.canGetMonitorInfo = false;
            reply.canGetOwnedMonitorInfo = false;
            reply.canGetSourceDebugExtension = false;
            reply.canGetSyntheticAttribute = false;
            reply.canPopFrames = false;
            reply.canRedefineClasses = false;
            reply.canRequestMonitorEvents = false;
            reply.canRequestVMDeathEvent = false;
            reply.canSetDefaultStratum = false;
            reply.canUnrestrictedlyRedefineClasses = false;
            reply.canUseInstanceFilters = false;
            reply.canUseSourceNameFilters = false;
            reply.canWatchFieldAccess = false;
            reply.canWatchFieldModification = false;
            return reply;
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_RedefineClasses">
     * JDWP specification for command VirtualMachine-RedefineClasses.</a>
     */
    private class RedefineClassesHandler extends VirtualMachineCommands.RedefineClasses.Handler {

        @Override
        public RedefineClasses.Reply handle(RedefineClasses.IncomingRequest incomingRequest) throws JDWPException {
            // TODO: Consider implementing this!
            throw new JDWPNotImplementedException();
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_SetDefaultStratum">
     * JDWP specification for command VirtualMachine-SetDefaultStratum.</a>
     */
    private class SetDefaultStratumHandler extends VirtualMachineCommands.SetDefaultStratum.Handler {

        @Override
        public SetDefaultStratum.Reply handle(SetDefaultStratum.IncomingRequest incomingRequest) throws JDWPException {
            // TODO: Consider implementing this!
            throw new JDWPNotImplementedException();
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_AllClassesWithGeneric">
     * JDWP specification for command VirtualMachine-AllClassesWithGeneric.</a>
     */
    private class AllClassesWithGenericHandler extends VirtualMachineCommands.AllClassesWithGeneric.Handler {

        @Override
        public AllClassesWithGeneric.Reply handle(AllClassesWithGeneric.IncomingRequest incomingRequest) throws JDWPException {
            final ReferenceTypeProvider[] refs = vm().getAllReferenceTypes();
            final AllClassesWithGeneric.Reply reply = new AllClassesWithGeneric.Reply();
            reply.classes = new AllClassesWithGeneric.ClassInfo[refs.length];
            for (int i = 0; i < refs.length; i++) {
                final AllClassesWithGeneric.ClassInfo classInfo = new AllClassesWithGeneric.ClassInfo();
                classInfo.typeID = session().toID(refs[i]);
                final ReferenceTypeProvider refType = refs[i];
                classInfo.status = refType.getStatus();
                classInfo.refTypeTag = session().getTypeTag(refType);
                classInfo.signature = refType.getSignature();
                classInfo.genericSignature = refType.getSignatureWithGeneric();
                if (classInfo.genericSignature == null) {
                    classInfo.genericSignature = classInfo.signature;
                }
                reply.classes[i] = classInfo;
            }
            return reply;
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_VirtualMachine_InstanceCounts">
     * JDWP specification for command VirtualMachine-InstanceCounts.</a>
     */
    private class InstanceCountsHandler extends VirtualMachineCommands.InstanceCounts.Handler {

        @Override
        public InstanceCounts.Reply handle(InstanceCounts.IncomingRequest incomingRequest) throws JDWPException {
            // TODO: Consider implementing this!
            throw new JDWPNotImplementedException();
        }
    }
}
