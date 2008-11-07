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
            reply._jdwpMajor = 1;
            reply._jdwpMinor = 5;
            reply._vmName = session().vm().getName();
            reply._vmVersion = session().vm().getVersion();
            reply._description = session().vm().getDescription();
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
            final ReferenceTypeProvider[] refs = vm().getReferenceTypesBySignature(incomingRequest._signature);
            final ClassesBySignature.Reply reply = new ClassesBySignature.Reply();
            reply._classes = new ClassesBySignature.ClassInfo[refs.length];
            for (int i = 0; i < refs.length; i++) {
                final ClassesBySignature.ClassInfo classInfo = new ClassesBySignature.ClassInfo();
                classInfo._typeID = session().toID(refs[i]);
                final ReferenceTypeProvider refType = refs[i];
                classInfo._status = refType.getStatus();
                classInfo._refTypeTag = session().getTypeTag(refType);
                reply._classes[i] = classInfo;
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
            reply._classes = new AllClasses.ClassInfo[refs.length];
            for (int i = 0; i < refs.length; i++) {
                final AllClasses.ClassInfo c = new AllClasses.ClassInfo();
                c._typeID = session().toID(refs[i]);
                final ReferenceTypeProvider refType = refs[i];
                c._status = refType.getStatus();
                c._refTypeTag = session().getTypeTag(refType);
                c._signature = refType.getSignature();
                reply._classes[i] = c;
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
            reply._threads = threads;
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
            reply._groups = new ID.ThreadGroupID[threadGroups.length];
            for (int i = 0; i < threadGroups.length; i++) {
                reply._groups[i] = session().toID(threadGroups[i]);
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
            reply._fieldIDSize = ID_SIZE;
            reply._frameIDSize = ID_SIZE;
            reply._methodIDSize = ID_SIZE;
            reply._objectIDSize = ID_SIZE;
            reply._referenceTypeIDSize = ID_SIZE;
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
            vm().exit(incomingRequest._exitCode);
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
            reply._stringObject = session().toID(vm().createString(incomingRequest._utf));
            if (reply._stringObject.value() == 0) {
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
            reply._canGetBytecodes = false;
            reply._canGetCurrentContendedMonitor = false;
            reply._canGetMonitorInfo = false;
            reply._canGetOwnedMonitorInfo = false;
            reply._canGetSyntheticAttribute = false;
            reply._canWatchFieldAccess = false;
            reply._canWatchFieldModification = false;
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
            reply._baseDir = "";
            reply._classpaths = vm().getClassPath();
            reply._bootclasspaths = vm().getBootClassPath();
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
            reply._canAddMethod = false;
            reply._canForceEarlyReturn = false;
            reply._canGetBytecodes = false;
            reply._canGetConstantPool = false;
            reply._canGetCurrentContendedMonitor = false;
            reply._canGetInstanceInfo = false;
            reply._canGetMonitorFrameInfo = false;
            reply._canGetMonitorInfo = false;
            reply._canGetOwnedMonitorInfo = false;
            reply._canGetSourceDebugExtension = false;
            reply._canGetSyntheticAttribute = false;
            reply._canPopFrames = false;
            reply._canRedefineClasses = false;
            reply._canRequestMonitorEvents = false;
            reply._canRequestVMDeathEvent = false;
            reply._canSetDefaultStratum = false;
            reply._canUnrestrictedlyRedefineClasses = false;
            reply._canUseInstanceFilters = false;
            reply._canUseSourceNameFilters = false;
            reply._canWatchFieldAccess = false;
            reply._canWatchFieldModification = false;
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
            reply._classes = new AllClassesWithGeneric.ClassInfo[refs.length];
            for (int i = 0; i < refs.length; i++) {
                final AllClassesWithGeneric.ClassInfo classInfo = new AllClassesWithGeneric.ClassInfo();
                classInfo._typeID = session().toID(refs[i]);
                final ReferenceTypeProvider refType = refs[i];
                classInfo._status = refType.getStatus();
                classInfo._refTypeTag = session().getTypeTag(refType);
                classInfo._signature = refType.getSignature();
                classInfo._genericSignature = refType.getSignatureWithGeneric();
                if (classInfo._genericSignature == null) {
                    classInfo._genericSignature = classInfo._signature;
                }
                reply._classes[i] = classInfo;
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
