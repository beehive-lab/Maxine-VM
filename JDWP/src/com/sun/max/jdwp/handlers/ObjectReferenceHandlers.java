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
/*VCSID=90e1bbef-4c63-4940-84cd-e8b5f68ffa40*/
package com.sun.max.jdwp.handlers;

import com.sun.max.jdwp.constants.*;
import com.sun.max.jdwp.constants.Error;
import com.sun.max.jdwp.data.*;
import com.sun.max.jdwp.protocol.*;
import com.sun.max.jdwp.protocol.ObjectReferenceCommands.*;
import com.sun.max.jdwp.vm.proxy.*;

/**
 * @author Thomas Wuerthinger
 *
 */
public class ObjectReferenceHandlers extends Handlers {

    public ObjectReferenceHandlers(JDWPSession session) {
        super(session);
    }

    @Override
    public void registerWith(CommandHandlerRegistry registry) {
        registry.addCommandHandler(new ReferenceTypeHandler());
        registry.addCommandHandler(new GetValuesHandler());
        registry.addCommandHandler(new SetValuesHandler());
        registry.addCommandHandler(new MonitorInfoHandler());
        registry.addCommandHandler(new InvokeMethodHandler());
        registry.addCommandHandler(new DisableCollectionHandler());
        registry.addCommandHandler(new EnableCollectionHandler());
        registry.addCommandHandler(new IsCollectedHandler());
        registry.addCommandHandler(new ReferringObjectsHandler());
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ObjectReference_ReferenceType">
     * JDWP specification for command ObjectReference-ReferenceType.</a>
     */
    private class ReferenceTypeHandler extends ObjectReferenceCommands.ReferenceType.Handler {

        @Override
        public ReferenceType.Reply handle(ReferenceType.IncomingRequest incomingRequest) throws JDWPException {
            final ObjectProvider object = session().getObject(incomingRequest._object);
            final ReferenceTypeProvider refType = object.getReferenceType();
            final ReferenceType.Reply reply = new ReferenceType.Reply();
            reply._refTypeTag = session().getTypeTag(refType);
            reply._typeID = session().toID(refType);
            return reply;
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ObjectReference_GetValues">
     * JDWP specification for command ObjectReference-GetValues.</a>
     */
    private class GetValuesHandler extends ObjectReferenceCommands.GetValues.Handler {

        @Override
        public GetValues.Reply handle(GetValues.IncomingRequest incomingRequest) throws JDWPException {

            final ObjectProvider object = session().getObject(incomingRequest._object);
            final ReferenceTypeProvider referenceType = object.getReferenceType();

            final GetValues.Reply reply = new GetValues.Reply();
            reply._values = new JDWPValue[incomingRequest._fields.length];

            for (int i = 0; i < reply._values.length; i++) {
                final ID.FieldID fieldID = incomingRequest._fields[i]._fieldID;
                final FieldProvider field = session().getField(session().toID(referenceType), fieldID);
                final JDWPValue value = session().toJDWPValue(field.getValue(object));
                reply._values[i] = value;
            }

            return reply;
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ObjectReference_SetValues">
     * JDWP specification for command ObjectReference-SetValues.</a>
     */
    private class SetValuesHandler extends ObjectReferenceCommands.SetValues.Handler {

        /**
         * WARNING: See the interface method comment for this method.
         */
        @Override
        public int helpAtDecodingUntaggedValue(SetValues.IncomingRequest data) throws JDWPException {

            int index = -1;
            for (SetValues.FieldValue fieldValue : data._values) {
                if (fieldValue == null) {
                    break;
                }
                index++;
            }

            assert index >= 0 && index < data._values.length : "Index must be valid!";
            assert data._object != null && data._values[index] != null : "Packet must be partially present!";
            final ObjectProvider object = session().getObject(data._object);
            final ID.FieldID fieldID = data._values[index]._fieldID;
            final FieldProvider field = session().getField(session().toID(object.getReferenceType()), fieldID);
            return JDWPSession.getValueTypeTag(field.getType());
        }

        @Override
        public SetValues.Reply handle(SetValues.IncomingRequest incomingRequest) throws JDWPException {
            final ObjectProvider object = session().getObject(incomingRequest._object);
            for (SetValues.FieldValue fv : incomingRequest._values) {
                final FieldProvider field = session().getField(session().toID(object.getReferenceType()), fv._fieldID);
                field.setValue(object, session().toValue(fv._value));
            }

            return new SetValues.Reply();
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ObjectReference_MonitorInfo">
     * JDWP specification for command ObjectReference-MonitorInfo.</a>
     */
    private class MonitorInfoHandler extends ObjectReferenceCommands.MonitorInfo.Handler {

        @Override
        public MonitorInfo.Reply handle(MonitorInfo.IncomingRequest incomingRequest) throws JDWPException {
            // TODO: Consider implementing this.
            throw new JDWPException((short) Error.NOT_IMPLEMENTED);
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ObjectReference_InvokeMethod">
     * JDWP specification for command ObjectReference-InvokeMethod.</a>
     */
    private class InvokeMethodHandler extends ObjectReferenceCommands.InvokeMethod.Handler {

        @Override
        public InvokeMethod.Reply handle(InvokeMethod.IncomingRequest incomingRequest) throws JDWPException {

            final ThreadProvider threadProvider = session().getThread(incomingRequest._thread);
            final MethodProvider methodProvider = session().getMethod(incomingRequest._clazz, incomingRequest._methodID);

            final int invokeOptions = incomingRequest._options;
            final boolean singleThreaded = (invokeOptions & InvokeOptions.INVOKE_SINGLE_THREADED) != 0;
            final boolean nonVirtual = (invokeOptions & InvokeOptions.INVOKE_NONVIRTUAL) != 0;

            final VMValue[] args = new VMValue[incomingRequest._arguments.length];
            for (int i = 0; i < args.length; i++) {
                args[i] = session().toValue(incomingRequest._arguments[i]);
            }

            final ObjectProvider object = session().getObject(incomingRequest._object);

            // TODO: Enable the possibility of exceptions here!
            final VMValue result = methodProvider.invoke(object, args, threadProvider, singleThreaded, nonVirtual);

            final InvokeMethod.Reply r = new InvokeMethod.Reply();
            r._returnValue = session().toJDWPValue(result);
            r._exception = new JDWPValue(ID.ObjectID.NULL);
            return r;
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ObjectReference_DisableCollection">
     * JDWP specification for command ObjectReference-DisableCollection.</a>
     */
    private class DisableCollectionHandler extends ObjectReferenceCommands.DisableCollection.Handler {

        @Override
        public DisableCollection.Reply handle(DisableCollection.IncomingRequest incomingRequest) throws JDWPException {
            // TODO: Consider implementing this otherwise. Currently an object with an ID is never garbage collected.
            return new DisableCollection.Reply();
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ObjectReference_EnableCollection">
     * JDWP specification for command ObjectReference-EnableCollection.</a>
     */
    private class EnableCollectionHandler extends ObjectReferenceCommands.EnableCollection.Handler {

        @Override
        public EnableCollection.Reply handle(EnableCollection.IncomingRequest incomingRequest) throws JDWPException {
            // TODO: Consider implementing this otherwise. Currently a object with an ID is never garbage collected.
            return new EnableCollection.Reply();
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ObjectReference_IsCollected">
     * JDWP specification for command ObjectReference-IsCollected.</a>
     */
    private class IsCollectedHandler extends ObjectReferenceCommands.IsCollected.Handler {

        @Override
        public IsCollected.Reply handle(IsCollected.IncomingRequest incomingRequest) throws JDWPException {
            // TODO: Consider implementing this otherwise. Currently a object with an ID is never garbage collected.
            return new IsCollected.Reply(false);
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ObjectReference_ReferringObjects">
     * JDWP specification for command ObjectReference-ReferringObjects.</a>
     */
    private class ReferringObjectsHandler extends ObjectReferenceCommands.ReferringObjects.Handler {

        @Override
        public ReferringObjects.Reply handle(ReferringObjects.IncomingRequest incomingRequest) throws JDWPException {
            // TODO: Consider implementing this.
            throw new JDWPNotImplementedException();
        }
    }
}
