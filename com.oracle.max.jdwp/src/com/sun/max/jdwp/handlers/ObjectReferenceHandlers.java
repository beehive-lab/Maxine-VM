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

import com.sun.max.jdwp.constants.*;
import com.sun.max.jdwp.constants.Error;
import com.sun.max.jdwp.data.*;
import com.sun.max.jdwp.protocol.*;
import com.sun.max.jdwp.protocol.ObjectReferenceCommands.*;
import com.sun.max.jdwp.vm.proxy.*;

/**
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
            final ObjectProvider object = session().getObject(incomingRequest.object);
            final ReferenceTypeProvider refType = object.getReferenceType();
            final ReferenceType.Reply reply = new ReferenceType.Reply();
            reply.refTypeTag = session().getTypeTag(refType);
            reply.typeID = session().toID(refType);
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

            final ObjectProvider object = session().getObject(incomingRequest.object);
            final ReferenceTypeProvider referenceType = object.getReferenceType();

            final GetValues.Reply reply = new GetValues.Reply();
            reply.values = new JDWPValue[incomingRequest.fields.length];

            for (int i = 0; i < reply.values.length; i++) {
                final ID.FieldID fieldID = incomingRequest.fields[i].fieldID;
                final FieldProvider field = session().getField(session().toID(referenceType), fieldID);
                final JDWPValue value = session().toJDWPValue(field.getValue(object));
                reply.values[i] = value;
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
            for (SetValues.FieldValue fieldValue : data.values) {
                if (fieldValue == null) {
                    break;
                }
                index++;
            }

            assert index >= 0 && index < data.values.length : "Index must be valid!";
            assert data.object != null && data.values[index] != null : "Packet must be partially present!";
            final ObjectProvider object = session().getObject(data.object);
            final ID.FieldID fieldID = data.values[index].fieldID;
            final FieldProvider field = session().getField(session().toID(object.getReferenceType()), fieldID);
            return JDWPSession.getValueTypeTag(field.getType());
        }

        @Override
        public SetValues.Reply handle(SetValues.IncomingRequest incomingRequest) throws JDWPException {
            final ObjectProvider object = session().getObject(incomingRequest.object);
            for (SetValues.FieldValue fv : incomingRequest.values) {
                final FieldProvider field = session().getField(session().toID(object.getReferenceType()), fv.fieldID);
                field.setValue(object, session().toValue(fv.value));
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

            final ThreadProvider threadProvider = session().getThread(incomingRequest.thread);
            final MethodProvider methodProvider = session().getMethod(incomingRequest.clazz, incomingRequest.methodID);

            final int invokeOptions = incomingRequest.options;
            final boolean singleThreaded = (invokeOptions & InvokeOptions.INVOKE_SINGLE_THREADED) != 0;
            final boolean nonVirtual = (invokeOptions & InvokeOptions.INVOKE_NONVIRTUAL) != 0;

            final VMValue[] args = new VMValue[incomingRequest.arguments.length];
            for (int i = 0; i < args.length; i++) {
                args[i] = session().toValue(incomingRequest.arguments[i]);
            }

            final ObjectProvider object = session().getObject(incomingRequest.object);

            // TODO: Enable the possibility of exceptions here!
            final VMValue result = methodProvider.invoke(object, args, threadProvider, singleThreaded, nonVirtual);

            final InvokeMethod.Reply r = new InvokeMethod.Reply();
            r.returnValue = session().toJDWPValue(result);
            r.exception = new JDWPValue(ID.ObjectID.NULL);
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
