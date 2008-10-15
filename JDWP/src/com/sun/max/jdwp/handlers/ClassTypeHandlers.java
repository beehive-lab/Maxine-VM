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

import com.sun.max.jdwp.constants.*;
import com.sun.max.jdwp.data.*;
import com.sun.max.jdwp.protocol.*;
import com.sun.max.jdwp.protocol.ClassTypeCommands.*;
import com.sun.max.jdwp.vm.proxy.*;

/**
 * @author Thomas Wuerthinger
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
            final ClassProvider klass = session().getClass(incomingRequest._clazz);
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
            for (SetValues.FieldValue fieldValue : data._values) {
                if (fieldValue == null) {
                    break;
                }
                index++;
            }

            assert index >= 0 && index < data._values.length : "Index must be valid!";
            assert data._clazz != null && data._values[index] != null : "Packet must be partially present!";

            final ID.FieldID fieldID = data._values[index]._fieldID;
            final FieldProvider field = session().getField(data._clazz, fieldID);

            return JDWPSession.getValueTypeTag(field.getType());
        }

        @Override
        public SetValues.Reply handle(SetValues.IncomingRequest incomingRequest) throws JDWPException {

            for (SetValues.FieldValue fv : incomingRequest._values) {
                final FieldProvider field = session().getField(incomingRequest._clazz, fv._fieldID);
                field.setStaticValue(session().toValue(fv._value));
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

            final ThreadProvider threadProvider = session().getThread(incomingRequest._thread);
            final MethodProvider methodProvider = session().getMethod(incomingRequest._clazz, incomingRequest._methodID);

            final int invokeOptions = incomingRequest._options;
            final boolean singleThreaded = (invokeOptions & InvokeOptions.INVOKE_SINGLE_THREADED) != 0;
            final boolean nonVirtual = (invokeOptions & InvokeOptions.INVOKE_NONVIRTUAL) != 0;

            final VMValue[] args = new VMValue[incomingRequest._arguments.length];
            for (int i = 0; i < args.length; i++) {
                args[i] = session().toValue(incomingRequest._arguments[i]);
            }

            assert !nonVirtual : "Static methods do not allow nonVirtual to be set!";

            // TODO: Enable the possibility of excpeptions here!
            final VMValue result = methodProvider.invokeStatic(args, threadProvider, singleThreaded);

            final InvokeMethod.Reply reply = new InvokeMethod.Reply();
            reply._returnValue = session().toJDWPValue(result);
            reply._exception = new JDWPValue(ID.ObjectID.NULL);
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

            final ThreadProvider threadProvider = session().getThread(incomingRequest._thread);
            final MethodProvider methodProvider = session().getMethod(incomingRequest._clazz, incomingRequest._methodID);

            final int invokeOptions = incomingRequest._options;
            final boolean singleThreaded = (invokeOptions & InvokeOptions.INVOKE_SINGLE_THREADED) != 0;
            final boolean nonVirtual = (invokeOptions & InvokeOptions.INVOKE_NONVIRTUAL) != 0;

            final VMValue[] args = new VMValue[incomingRequest._arguments.length];
            for (int i = 0; i < args.length; i++) {
                args[i] = session().toValue(incomingRequest._arguments[i]);
            }

            assert !nonVirtual : "Static method invocations may not have nonVirtual set";

            // TODO: Enable the possibility of excpeptions here!
            final VMValue result = methodProvider.invokeStatic(args, threadProvider, singleThreaded);

            final NewInstance.Reply r = new NewInstance.Reply();
            r._newObject = session().toJDWPValue(result);
            r._exception = new JDWPValue(ID.ObjectID.NULL);
            return r;
        }
    }
}
