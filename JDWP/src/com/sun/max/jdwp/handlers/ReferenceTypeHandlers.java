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

import com.sun.max.jdwp.constants.Error;
import com.sun.max.jdwp.data.*;
import com.sun.max.jdwp.protocol.*;
import com.sun.max.jdwp.protocol.ReferenceTypeCommands.*;
import com.sun.max.jdwp.protocol.ReferenceTypeCommands.ClassLoader;
import com.sun.max.jdwp.protocol.ReferenceTypeCommands.Fields.*;
import com.sun.max.jdwp.protocol.ReferenceTypeCommands.Methods.*;
import com.sun.max.jdwp.protocol.ReferenceTypeCommands.Methods.Reply;
import com.sun.max.jdwp.vm.proxy.*;

/**
 * @author Thomas Wuerthinger
 *
 */
public class ReferenceTypeHandlers extends Handlers {

    public ReferenceTypeHandlers(JDWPSession session) {
        super(session);
    }

    @Override
    public void registerWith(CommandHandlerRegistry registry) {
        registry.addCommandHandler(new SignatureHandler());
        registry.addCommandHandler(new ClassLoaderHandler());
        registry.addCommandHandler(new ModifiersHandler());
        registry.addCommandHandler(new FieldsHandler());
        registry.addCommandHandler(new MethodsHandler());
        registry.addCommandHandler(new GetValuesHandler());
        registry.addCommandHandler(new SourceFileHandler());
        registry.addCommandHandler(new NestedTypesHandler());
        registry.addCommandHandler(new StatusHandler());
        registry.addCommandHandler(new InterfacesHandler());
        registry.addCommandHandler(new ClassObjectHandler());
        registry.addCommandHandler(new SourceDebugExtensionHandler());
        registry.addCommandHandler(new SignatureWithGenericHandler());
        registry.addCommandHandler(new FieldsWithGenericHandler());
        registry.addCommandHandler(new MethodsWithGenericHandler());
        registry.addCommandHandler(new InstancesHandler());
        registry.addCommandHandler(new ClassFileVersionHandler());
        registry.addCommandHandler(new ConstantPoolHandler());
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_Signature">
     * JDWP specification for command ReferenceType-Signature.</a>
     */
    private class SignatureHandler extends ReferenceTypeCommands.Signature.Handler {

        @Override
        public Signature.Reply handle(Signature.IncomingRequest incomingRequest) throws JDWPException {
            final ReferenceTypeProvider referenceType = session().getReferenceType(incomingRequest._refType);
            return new Signature.Reply(referenceType.getSignature());
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_ClassLoader">
     * JDWP specification for command ReferenceType-ClassLoader.</a>
     */
    private class ClassLoaderHandler extends ReferenceTypeCommands.ClassLoader.Handler {

        @Override
        public ClassLoader.Reply handle(ClassLoader.IncomingRequest incomingRequest) throws JDWPException {
            final ReferenceTypeProvider referenceType = session().getReferenceType(incomingRequest._refType);
            return new ClassLoader.Reply(session().toID(referenceType.classLoader()));
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_Modifiers">
     * JDWP specification for command ReferenceType-Modifiers.</a>
     */
    private class ModifiersHandler extends ReferenceTypeCommands.Modifiers.Handler {

        @Override
        public Modifiers.Reply handle(Modifiers.IncomingRequest incomingRequest) throws JDWPException {
            final ReferenceTypeProvider referenceType = session().getReferenceType(incomingRequest._refType);
            return new Modifiers.Reply(referenceType.getFlags());
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_Fields">
     * JDWP specification for command ReferenceType-Fields.</a>
     */
    private class FieldsHandler extends ReferenceTypeCommands.Fields.Handler {

        @Override
        public Fields.Reply handle(Fields.IncomingRequest incomingRequest) throws JDWPException {
            final ReferenceTypeProvider referenceType = session().getReferenceType(incomingRequest._refType);
            final FieldProvider[] fields = referenceType.getFields();
            final Fields.Reply reply = new Fields.Reply();
            reply._declared = new Fields.FieldInfo[fields.length];
            int z = 0;
            for (FieldProvider fieldProvider : fields) {
                final FieldInfo fieldInfo = new FieldInfo();
                fieldInfo._fieldID = session().toID(referenceType, fieldProvider);
                fieldInfo._modBits = fieldProvider.getFlags();
                fieldInfo._name = fieldProvider.getName();
                fieldInfo._signature = fieldProvider.getSignature();
                reply._declared[z] = fieldInfo;
                z++;
            }
            return reply;
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_Methods">
     * JDWP specification for command ReferenceType-Methods.</a>
     */
    private class MethodsHandler extends ReferenceTypeCommands.Methods.Handler {

        @Override
        public Methods.Reply handle(Methods.IncomingRequest incomingRequest) throws JDWPException {
            final ReferenceTypeProvider referenceType = session().getReferenceType(incomingRequest._refType);
            final MethodProvider[] methods = referenceType.getMethods();
            final Reply reply = new Reply();
            reply._declared = new MethodInfo[methods.length];
            int z = 0;
            for (MethodProvider methodProvider : methods) {
                final MethodInfo methodInfo = new MethodInfo();
                methodInfo._methodID = session().toID(referenceType, methodProvider);
                methodInfo._modBits = methodProvider.getFlags();
                methodInfo._name = methodProvider.getName();
                methodInfo._signature = methodProvider.getSignature();
                reply._declared[z] = methodInfo;
                z++;
            }
            return reply;
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_GetValues">
     * JDWP specification for command ReferenceType-GetValues.</a>
     */
    private class GetValuesHandler extends ReferenceTypeCommands.GetValues.Handler {

        @Override
        public GetValues.Reply handle(GetValues.IncomingRequest incomingRequest) throws JDWPException {
            final GetValues.Reply reply = new GetValues.Reply();
            reply._values = new JDWPValue[incomingRequest._fields.length];
            int z = 0;
            for (GetValues.Field f : incomingRequest._fields) {
                final ID.FieldID id = f._fieldID;
                final FieldProvider field = session().getField(incomingRequest._refType, id);
                reply._values[z] = session().toJDWPValue(field.getStaticValue());
                z++;
            }
            return reply;
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_SourceFile">
     * JDWP specification for command ReferenceType-SourceFile.</a>
     */
    private class SourceFileHandler extends ReferenceTypeCommands.SourceFile.Handler {

        @Override
        public SourceFile.Reply handle(SourceFile.IncomingRequest incomingRequest) throws JDWPException {
            final ReferenceTypeProvider referenceType = session().getReferenceType(incomingRequest._refType);
            return new SourceFile.Reply(referenceType.getSourceFileName());
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_NestedTypes">
     * JDWP specification for command ReferenceType-NestedTypes.</a>
     */
    private class NestedTypesHandler extends ReferenceTypeCommands.NestedTypes.Handler {

        @Override
        public NestedTypes.Reply handle(NestedTypes.IncomingRequest incomingRequest) throws JDWPException {
            final ReferenceTypeProvider referenceType = session().getReferenceType(incomingRequest._refType);
            final ReferenceTypeProvider[] refTypes = referenceType.getNestedTypes();
            final NestedTypes.Reply reply = new NestedTypes.Reply();
            reply._classes = new NestedTypes.TypeInfo[refTypes.length];
            for (int i = 0; i < refTypes.length; i++) {
                final ReferenceTypeProvider curRefType = refTypes[i];
                reply._classes[i] = new NestedTypes.TypeInfo();
                reply._classes[i]._refTypeTag = session().getTypeTag(curRefType);
                reply._classes[i]._typeID = session().toID(curRefType);
            }
            return reply;
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_Status">
     * JDWP specification for command ReferenceType-Status.</a>
     */
    private class StatusHandler extends ReferenceTypeCommands.Status.Handler {

        @Override
        public Status.Reply handle(Status.IncomingRequest incomingRequest) throws JDWPException {
            final ReferenceTypeProvider referenceType = session().getReferenceType(incomingRequest._refType);
            return new Status.Reply(referenceType.getStatus());
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_Interfaces">
     * JDWP specification for command ReferenceType-Interfaces.</a>
     */
    private class InterfacesHandler extends ReferenceTypeCommands.Interfaces.Handler {

        @Override
        public Interfaces.Reply handle(Interfaces.IncomingRequest incomingRequest) throws JDWPException {
            final ReferenceTypeProvider referenceType = session().getReferenceType(incomingRequest._refType);
            final Interfaces.Reply reply = new Interfaces.Reply();
            final InterfaceProvider[] interfaceProviders = referenceType.getImplementedInterfaces();
            final ID.InterfaceID[] result = new ID.InterfaceID[interfaceProviders.length];
            for (int i = 0; i < result.length; i++) {
                result[i] = session().toID(interfaceProviders[i]);
            }
            reply._interfaces = result;
            return reply;
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_ClassObject">
     * JDWP specification for command ReferenceType-ClassObject.</a>
     */
    private class ClassObjectHandler extends ReferenceTypeCommands.ClassObject.Handler {

        @Override
        public ClassObject.Reply handle(ClassObject.IncomingRequest incomingRequest) throws JDWPException {
            final ReferenceTypeProvider referenceType = session().getReferenceType(incomingRequest._refType);
            return new ClassObject.Reply(session().toID(referenceType.classObject()));
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_SourceDebugExtension">
     * JDWP specification for command ReferenceType-SourceDebugExtension.</a>
     */
    private class SourceDebugExtensionHandler extends ReferenceTypeCommands.SourceDebugExtension.Handler {

        @Override
        public SourceDebugExtension.Reply handle(SourceDebugExtension.IncomingRequest incomingRequest) throws JDWPException {
            // TODO: Consider to add implementation.
            throw new JDWPNotImplementedException();
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_SignatureWithGeneric">
     * JDWP specification for command ReferenceType-SignatureWithGeneric.</a>
     */
    private class SignatureWithGenericHandler extends ReferenceTypeCommands.SignatureWithGeneric.Handler {

        @Override
        public SignatureWithGeneric.Reply handle(SignatureWithGeneric.IncomingRequest incomingRequest) throws JDWPException {
            final ReferenceTypeProvider referenceType = session().getReferenceType(incomingRequest._refType);
            String signatureWithGeneric = referenceType.getSignatureWithGeneric();
            if (signatureWithGeneric == null) {
                signatureWithGeneric = referenceType.getSignature();
            }
            return new SignatureWithGeneric.Reply(referenceType.getSignature(), signatureWithGeneric);
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_FieldsWithGeneric">
     * JDWP specification for command ReferenceType-FieldsWithGeneric.</a>
     */
    private class FieldsWithGenericHandler extends ReferenceTypeCommands.FieldsWithGeneric.Handler {

        @Override
        public FieldsWithGeneric.Reply handle(FieldsWithGeneric.IncomingRequest incomingRequest) throws JDWPException {
            final ReferenceTypeProvider referenceType = session().getReferenceType(incomingRequest._refType);
            final FieldProvider[] fields = referenceType.getFields();
            final FieldsWithGeneric.Reply reply = new FieldsWithGeneric.Reply();
            reply._declared = new FieldsWithGeneric.FieldInfo[fields.length];
            int z = 0;
            for (FieldProvider fieldProvider : fields) {
                final FieldsWithGeneric.FieldInfo fieldInfo = new FieldsWithGeneric.FieldInfo();
                fieldInfo._fieldID = session().toID(referenceType, fieldProvider);
                fieldInfo._modBits = fieldProvider.getFlags();
                fieldInfo._name = fieldProvider.getName();
                fieldInfo._signature = fieldProvider.getSignature();
                fieldInfo._genericSignature = fieldProvider.getGenericSignature();
                if (fieldInfo._genericSignature == null) {
                    fieldInfo._genericSignature = fieldInfo._signature;
                }
                reply._declared[z] = fieldInfo;
                z++;
            }
            return reply;
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_MethodsWithGeneric">
     * JDWP specification for command ReferenceType-MethodsWithGeneric.</a>
     */
    private class MethodsWithGenericHandler extends ReferenceTypeCommands.MethodsWithGeneric.Handler {

        @Override
        public MethodsWithGeneric.Reply handle(MethodsWithGeneric.IncomingRequest incomingRequest) throws JDWPException {
            final ReferenceTypeProvider referenceType = session().getReferenceType(incomingRequest._refType);
            final MethodProvider[] methods = referenceType.getMethods();
            final MethodsWithGeneric.Reply reply = new MethodsWithGeneric.Reply();
            reply._declared = new MethodsWithGeneric.MethodInfo[methods.length];
            int z = 0;
            for (MethodProvider methodProvider : methods) {
                final MethodsWithGeneric.MethodInfo methodInfo = new MethodsWithGeneric.MethodInfo();
                methodInfo._methodID = session().toID(referenceType, methodProvider);
                methodInfo._modBits = methodProvider.getFlags();
                methodInfo._name = methodProvider.getName();
                methodInfo._signature = methodProvider.getSignature(); // ma.javaSignature(true, false, true);
                methodInfo._genericSignature = methodProvider.getSignatureWithGeneric();
                if (methodInfo._genericSignature == null) {
                    methodInfo._genericSignature = methodInfo._signature;
                }
                reply._declared[z] = methodInfo;
                z++;
            }
            return reply;
        }
    }

    /**
     * <a href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_Instances">
     * JDWP specification for command ReferenceType-Instances.</a>
     */
    private class InstancesHandler extends ReferenceTypeCommands.Instances.Handler {

        @Override
        public Instances.Reply handle(Instances.IncomingRequest incomingRequest) throws JDWPException {

            final int maxInstances = incomingRequest._maxInstances;
            final ReferenceTypeProvider referenceTypeProvider = session().getReferenceType(incomingRequest._refType);
            final Instances.Reply reply = new Instances.Reply();

            final ObjectProvider[] instances = referenceTypeProvider.getInstances();

            if (instances == null) {
                throw new JDWPException((short) Error.NOT_IMPLEMENTED);
            }

            final int count = Math.min(maxInstances, instances.length);
            reply._instances = new JDWPValue[count];
            for (int i = 0; i < count; i++) {
                reply._instances[i] = new JDWPValue(session().toID(instances[i]));
            }

            return reply;
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_ClassFileVersion">
     * JDWP specification for command ReferenceType-ClassFileVersion.</a>
     */
    private class ClassFileVersionHandler extends ReferenceTypeCommands.ClassFileVersion.Handler {

        @Override
        public ClassFileVersion.Reply handle(ClassFileVersion.IncomingRequest incomingRequest) throws JDWPException {
            final ReferenceTypeProvider referenceType = session().getReferenceType(incomingRequest._refType);
            final ClassFileVersion.Reply reply = new ClassFileVersion.Reply();
            reply._majorVersion = referenceType.majorVersion();
            reply._minorVersion = referenceType.minorVersion();
            return reply;
        }
    }

    /**
     * <a
     * href="http://download.java.net/jdk7/docs/platform/jpda/jdwp/jdwp-protocol.html#JDWP_ReferenceType_ConstantPool">
     * JDWP specification for command ReferenceType-ConstantPool.</a>
     */
    private class ConstantPoolHandler extends ReferenceTypeCommands.ConstantPool.Handler {

        @Override
        public ConstantPool.Reply handle(ConstantPool.IncomingRequest incomingRequest) throws JDWPException {
            // TODO: Consider to add implemtation.
            throw new JDWPNotImplementedException();
        }
    }

}
