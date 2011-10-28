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
import com.sun.max.jdwp.data.*;
import com.sun.max.jdwp.protocol.*;
import com.sun.max.jdwp.protocol.ClassTypeCommands.*;
import com.sun.max.jdwp.vm.proxy.*;

/**
 *
 */
public class ClassTypeHandlers extends Handlers {

    public ClassTypeHandlers(JDWPSession session) {
        super(session);
    }

    @Override
    public void registerWith(CommandHandlerRegistry registry) {
        registry.addCommandHandler(new SuperclassHandler());
        registry.addCommandHandler(new SetValuesHandler());
        registry.addCommandHandler(new InvokeMethodHandler());
        registry.addCommandHandler(new NewInstanceHandler());
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ClassType_Superclass">
     * JDWP specification for command ClassType-Superclass.</a>
     */
    private class SuperclassHandler extends ClassTypeCommands.Superclass.Handler {

        @Override
        public Superclass.Reply handle(Superclass.IncomingRequest incomingRequest) throws JDWPException {
            final ClassProvider klass = session().getClass(incomingRequest.clazz);
            return new Superclass.Reply(session().toID(klass.getSuperClass()));
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ClassType_SetValues"> JDWP
     * specification for command ClassType-SetValues.</a>
     */
    private class SetValuesHandler extends ClassTypeCommands.SetValues.Handler {

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
            assert data.clazz != null && data.values[index] != null : "Packet must be partially present!";

            final ID.FieldID fieldID = data.values[index].fieldID;
            final FieldProvider field = session().getField(data.clazz, fieldID);

            return JDWPSession.getValueTypeTag(field.getType());
        }

        @Override
        public SetValues.Reply handle(SetValues.IncomingRequest incomingRequest) throws JDWPException {

            for (SetValues.FieldValue fv : incomingRequest.values) {
                final FieldProvider field = session().getField(incomingRequest.clazz, fv.fieldID);
                field.setStaticValue(session().toValue(fv.value));
            }

            return new SetValues.Reply();
        }

    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ClassType_InvokeMethod">
     * JDWP specification for command ClassType-InvokeMethod.</a>
     */
    private class InvokeMethodHandler extends ClassTypeCommands.InvokeMethod.Handler {

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

            assert !nonVirtual : "Static methods do not allow nonVirtual to be set!";

            // TODO: Enable the possibility of excpeptions here!
            final VMValue result = methodProvider.invokeStatic(args, threadProvider, singleThreaded);

            final InvokeMethod.Reply reply = new InvokeMethod.Reply();
            reply.returnValue = session().toJDWPValue(result);
            reply.exception = new JDWPValue(ID.ObjectID.NULL);
            return reply;
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ClassType_NewInstance">
     * JDWP specification for command ClassType-NewInstance.</a>
     */
    private class NewInstanceHandler extends ClassTypeCommands.NewInstance.Handler {

        @Override
        public NewInstance.Reply handle(NewInstance.IncomingRequest incomingRequest) throws JDWPException {

            final ThreadProvider threadProvider = session().getThread(incomingRequest.thread);
            final MethodProvider methodProvider = session().getMethod(incomingRequest.clazz, incomingRequest.methodID);

            final int invokeOptions = incomingRequest.options;
            final boolean singleThreaded = (invokeOptions & InvokeOptions.INVOKE_SINGLE_THREADED) != 0;
            final boolean nonVirtual = (invokeOptions & InvokeOptions.INVOKE_NONVIRTUAL) != 0;

            final VMValue[] args = new VMValue[incomingRequest.arguments.length];
            for (int i = 0; i < args.length; i++) {
                args[i] = session().toValue(incomingRequest.arguments[i]);
            }

            assert !nonVirtual : "Static method invocations may not have nonVirtual set";

            // TODO: Enable the possibility of excpeptions here!
            final VMValue result = methodProvider.invokeStatic(args, threadProvider, singleThreaded);

            final NewInstance.Reply r = new NewInstance.Reply();
            r.newObject = session().toJDWPValue(result);
            r.exception = new JDWPValue(ID.ObjectID.NULL);
            return r;
        }
    }
}
