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
package com.sun.max.ins;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.debug.TeleBytecodeBreakpoint.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;


/**
 * Standardized ways to display textual names of common entities during Inspection sessions.
 *
 * @author Michael Van De Vanter
 * @author Doug Simon
 */
public final class InspectorNameDisplay extends InspectionHolder {

    public InspectorNameDisplay(Inspection inspection) {
        super(inspection);

        _referenceRenderers.put(TeleArrayObject.class, new ArrayReferenceRenderer());
        _referenceRenderers.put(TeleHub.class, new HubReferenceRenderer());
        _referenceRenderers.put(TeleTupleObject.class, new TupleObjectReferenceRenderer());
        _referenceRenderers.put(TeleStaticTuple.class, new StaticTupleReferenceRenderer());
        _referenceRenderers.put(TeleMethodActor.class, new MethodActorReferenceRenderer());
        _referenceRenderers.put(TeleFieldActor.class, new FieldActorReferenceRenderer());
        _referenceRenderers.put(TeleClassActor.class, new ClassActorReferenceRenderer());
        _referenceRenderers.put(TeleString.class, new StringReferenceRenderer());
        _referenceRenderers.put(TeleUtf8Constant.class, new Utf8ConstantReferenceRenderer());
        _referenceRenderers.put(TeleStringConstant.class, new StringConstantReferenceRenderer());
        _referenceRenderers.put(TeleClass.class, new ClassReferenceRenderer());
        _referenceRenderers.put(TeleConstructor.class, new ConstructorReferenceRenderer());
        _referenceRenderers.put(TeleField.class, new FieldReferenceRenderer());
        _referenceRenderers.put(TeleMethod.class, new MethodReferenceRenderer());
        _referenceRenderers.put(TeleEnum.class, new EnumReferenceRenderer());
        _referenceRenderers.put(TeleConstantPool.class, new ConstantPoolReferenceRenderer());
        _referenceRenderers.put(TeleClassConstant.Resolved.class, new ClassConstantResolvedReferenceRenderer());
        _referenceRenderers.put(TeleFieldRefConstant.Resolved.class, new FieldRefConstantResolvedReferenceRenderer());
        _referenceRenderers.put(TeleClassMethodRefConstant.Resolved.class, new ClassMethodRefConstantResolvedReferenceRenderer());
        _referenceRenderers.put(TeleInterfaceMethodRefConstant.Resolved.class, new InterfaceMethodRefConstantResolvedReferenceRenderer());
        _referenceRenderers.put(TelePoolConstant.class, new PoolConstantReferenceRenderer());
        _referenceRenderers.put(TeleVmThread.class, new VmThreadReferenceRenderer());
    }

    /**
     * Constants specifying where the return type should appear in the value returned by the methods in this class that
     * produce a display name for a method.
     */
    public enum ReturnTypeSpecification {
        /**
         * Denotes that the return type is to be omitted from the display name of a method.
         */
        ABSENT,

        /**
         * Denotes that the return type is to be prefixed to the display name of a method.
         */
        AS_PREFIX,

        /**
         * Denotes that the return type is to be suffixed to the display name of a method.
         */
        AS_SUFFIX;
    }

    /**
     * Support for a standardized way to identify a heap object in the tele VM.
     *
     * @param prefix an optional string to precede everything else
     * @param teleObject an optional surrogate for the tele object being named, null if local
     * @param role an optional "role" name for low level Maxine objects whose implementation types aren't too interesting
     * @param type a name to describe the object, type name in simple cases
     * @return human readable string identifying an object in a standard format
     */
    private String objectReference(String prefix, TeleObject teleObject, String role, String type) {
        final StringBuilder name = new StringBuilder(32);
        if (prefix != null) {
            name.append(prefix);
        }

        if (teleObject != null) {
            name.append('<');
            name.append(teleObject.getOID());
            name.append('>');
        }
        if (role != null) {
            name.append(role);
            name.append('{');
            name.append(type);
            name.append('}');
        } else {
            name.append(type);
        }
        return name.toString();
    }

    /**
     * @return human readable string identifying a VM thread in a standard format.
     */
    public String shortName(TeleVmThread teleVmThread) {
        return teleVmThread.name();
    }

    /**
     * @return human readable string identifying a VM thread in a standard format.
     */
    public String longName(TeleVmThread teleVmThread) {
        final TeleNativeThread teleNativeThread = teleVmThread.teleNativeThread();
        if (teleNativeThread != null) {
            return shortName(teleVmThread) + " [" + teleNativeThread.id() + "]";
        }
        return shortName(teleVmThread);
    }

    /**
     * @return human readable string identifying a thread in a standard format.
     */
    public String shortName(TeleNativeThread teleNativeThread) {
        if (teleNativeThread == null) {
            return "null";
        }
        if (teleNativeThread == teleNativeThread.teleProcess().primordialThread()) {
            return "primordial";
        }
        if (teleNativeThread.teleVmThread() != null) {
            return shortName(teleNativeThread.teleVmThread());
        }
        return "native unnamed";
    }

    /**
     * @return human readable string identifying a thread in a standard format.
     */
    public String longName(TeleNativeThread teleNativeThread) {
        if (teleNativeThread == null) {
            return "null";
        }
        if (teleNativeThread.teleVmThread() != null) {
            return longName(teleNativeThread.teleVmThread());
        }
        return shortName(teleNativeThread) + " [" + teleNativeThread.id() + "]";
    }

    /**
     * E.g.: "[n]", where n is the index into the compilation history; first compilation n=0.
     */
    public String methodCompilationID(TeleTargetMethod teleTargetMethod) {
        if (teleTargetMethod != null) {
            final int compilationIndex = teleTargetMethod.getTeleClassMethodActor().indexOf(teleTargetMethod);
            if (compilationIndex >= 0) {
                return "[" + compilationIndex + "]";
            }
        }
        return "";
    }

    /**
     * E.g. an asterisk when a method has been substituted.
     */
    public String methodSubstitutionShortAnnotation(TeleMethodActor teleMethodActor) {
        return teleMethodActor.isSubstituted() ? " *" : "";
    }

    /**
     * E.g. an asterisk when a method has been substituted.
     */
    public String methodSubstitutionLongAnnotation(TeleMethodActor teleMethodActor) {
        return teleMethodActor.isSubstituted() ? " substituted from " + teleMethodActor.teleClassActorSubstitutedFrom().getName() : "";
    }

    /**
     * E.g. "Element.foo()[0]"
     */
    public String veryShortName(TeleTargetMethod teleTargetMethod) {
        return teleTargetMethod.classMethodActor().format("%h.%n()" + methodCompilationID(teleTargetMethod));
    }

    /**
     * E.g. "int foo(Pointer, Word, int[])[0]"
     *
     * @param returnTypeSpecification specifies where the return type should appear in the returned value
     */
    public String shortName(TeleTargetMethod teleTargetMethod, ReturnTypeSpecification returnTypeSpecification) {
        final ClassMethodActor classMethodActor = teleTargetMethod.classMethodActor();
        switch (returnTypeSpecification) {
            case ABSENT: {
                return classMethodActor.format("%n(%p)" + methodCompilationID(teleTargetMethod));
            }
            case AS_PREFIX: {
                return classMethodActor.format("%r %n(%p)" + methodCompilationID(teleTargetMethod));
            }
            case AS_SUFFIX: {
                return classMethodActor.format("%n(%p)" + methodCompilationID(teleTargetMethod) + " %r");
            }
            default: {
                throw ProgramError.unknownCase();
            }
        }
    }

    private String positionString(TeleTargetMethod teleTargetMethod, Address address) {
        final Pointer entry = teleTargetMethod.codeStart();
        final long position = address.minus(entry.asAddress()).toLong();
        return position == 0 ? "" : "+0x" + Long.toHexString(position);
    }

    /**
     * E.g. "int foo(Pointer, Word, int[])[0] in com.sun.max.ins.Bar"
     */
    public String longName(TeleTargetMethod teleTargetMethod) {
        return teleTargetMethod.classMethodActor().format("%r %n(%p)" + methodCompilationID(teleTargetMethod) + " in %H");
    }

    /**
     * E.g. "foo()[0]+0x7"
     */
    public String veryShortName(TeleTargetMethod teleTargetMethod, Address address) {
        return teleTargetMethod.classMethodActor().format("%n()" + methodCompilationID(teleTargetMethod) + positionString(teleTargetMethod, address));
    }

    /**
     * E.g. "int foo(Pointer, Word, int[])[0]+0x7"
     */
    public String shortName(TeleTargetMethod teleTargetMethod, Address address) {
        return teleTargetMethod.classMethodActor().format("%r %n(%p)" + methodCompilationID(teleTargetMethod) + positionString(teleTargetMethod, address));
    }

    /**
     * E.g. "int foo(Pointer, Word, int[])[0]+0x7 in com.sun.max.ins.Bar"
     */
    public String longName(TeleTargetMethod teleTargetMethod, Address address) {
        return teleTargetMethod.classMethodActor().format("%r %n(%p)" + methodCompilationID(teleTargetMethod) + positionString(teleTargetMethod, address) + " in %H");
    }

    /**
     * E.g. "int foo(Pointer, Word, int[])"
     */
    public String shortName(TeleClassMethodActor teleClassMethodActor, ReturnTypeSpecification returnTypeSpecification) {
        final ClassMethodActor classMethodActor = teleClassMethodActor.classMethodActor();
        switch (returnTypeSpecification) {
            case ABSENT: {
                return classMethodActor.format("%n(%p)");
            }
            case AS_PREFIX: {
                return classMethodActor.format("%r %n(%p)");
            }
            case AS_SUFFIX: {
                return classMethodActor.format("%n(%p) %r");
            }
            default: {
                throw ProgramError.unknownCase();
            }
        }
    }

    /**
     * E.g. "int foo(Pointer, Word, int[]) in com.sun.max.ins.Bar"
     */
    public String longName(TeleClassMethodActor teleClassMethodActor) {
        return teleClassMethodActor.classMethodActor().format("%r %n(%p)" + " in %H");
    }

    /**
     * E.g. "int foo(Pointer, Word, int[]) in com.sun.max.ins.Bar"
     * E.g. "int foo(Pointer, Word, int[])+14 in com.sun.max.ins.Bar"
     */
    public String longName(BytecodeLocation bytecodeLocation) {
        final int position = bytecodeLocation.position();
        return bytecodeLocation.classMethodActor().format("%r %n(%p)" + (position != 0 ? " +" + bytecodeLocation.position() : "") + " in %H");
    }

    /**
     * E.g. "int foo(Pointer, Word, int[])  in com.sun.max.ins.Bar"
     * E.g. "int foo(Pointer, Word, int[])  +14 in com.sun.max.ins.Bar"
     */
    public String longName(Key key) {
        final StringBuilder name = new StringBuilder();
        name.append(key.signature().getResultDescriptor().toJavaString(false)).append(" ").append(key.name()).append(key.signature().toJavaString(false, false));
        if (key.position() != 0) {
            name.append(" +").append(key.position());
        }
        name.append(" in ").append(key.holder().toJavaString());
        return name.toString();
    }

    public String longName(TeleCodeLocation teleCodeLocation) {
        if (teleCodeLocation == null) {
            return "null";
        }
        final StringBuilder name = new StringBuilder();
        if (teleCodeLocation.hasTargetCodeLocation()) {
            final Address address = teleCodeLocation.targetCodeInstructionAddresss();
            name.append("Target{0x").append(address.toHexString());
            if (TeleNativeTargetRoutine.get(teleVM(), address) != null) {
                name.append("}");
            } else {
                final TeleTargetMethod teleTargetMethod = TeleTargetMethod.make(teleVM(), address);
                if (teleTargetMethod != null) {
                    name.append(",  ").append(longName(teleTargetMethod, address)).append("} ");
                } else {
                    name.append("}");
                }
            }
        }
        if (teleCodeLocation.hasBytecodeLocation()) {
            name.append("Bytecode{").append(teleCodeLocation.bytecodeLocation()).append("} ");
        } else if (teleCodeLocation.hasKey()) {
            name.append("Key{").append(longName(teleCodeLocation.key())).append("} ");
        }
        return name.toString();
    }

    /**
     * Renderer for a textual label reference pointing at heap objects in the {@link TeleVM}.
     */
    private static interface ReferenceRenderer {

        /**
         * @return a short string suitable for a text label display of the object reference.
         */
        String referenceLabelText(TeleObject teleObject);

        /**
         * @return a longer string suitable for a tooltip display over the object reference.
         */
        String referenceToolTipText(TeleObject teleObject);
    }

    /**
     * Renderers for specific classes of objects in the heap in the {@teleVM}.
     * The most specific class that matches a particular {@link TeleObject} will
     * be used, in an emulation of virtual method dispatch.  All heap objects are
     * implemented as tuples, hubs. or arrays, so there should always be at least
     * a generic match for every type.
     */
    private final Map<Class, ReferenceRenderer> _referenceRenderers = new HashMap<Class, ReferenceRenderer>();

    /**
     * @return a short textual presentation of a reference to a heap object in the {@link TeleVM}, if possible, null if not.
     */
    public String referenceLabelText(TeleObject teleObject) {
        if (teleObject != null) {
            Class teleObjectClass = teleObject.getClass();
            while (teleObjectClass != null) {
                final ReferenceRenderer objectReferenceRenderer = _referenceRenderers.get(teleObjectClass);
                if (objectReferenceRenderer != null) {
                    try {
                        return objectReferenceRenderer.referenceLabelText(teleObject);
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        return "(Unexpected " + e.getClass().getName() + " when getting reference label)";
                    }
                }
                teleObjectClass = teleObjectClass.getSuperclass();
            }
            ProgramError.unexpected("InspectorNameDisplay failed to find render for teleObject = " + teleObject);
        }
        return null;
    }

    /**
     * @return a long textual presentation of a reference to a heap object in the {@link TeleVM}, if possible, null if not.
     */
    public String referenceToolTipText(TeleObject teleObject) {
        if (teleObject != null) {
            Class teleObjectClass = teleObject.getClass();
            while (teleObjectClass != null) {
                final ReferenceRenderer objectReferenceRenderer = _referenceRenderers.get(teleObjectClass);
                if (objectReferenceRenderer != null) {
                    try {
                        return objectReferenceRenderer.referenceToolTipText(teleObject);
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        return "(Unexpected " + e.getClass().getName() + " when getting tooltip)";
                    }
                }
                teleObjectClass = teleObjectClass.getSuperclass();
            }
            ProgramError.unexpected("InspectorNameDisplay failed to find render for teleObject = " + teleObject);
        }
        return null;
    }

    /**
     * Textual renderer for references to arrays.
     */
    private class ArrayReferenceRenderer implements ReferenceRenderer{
        public String referenceLabelText(TeleObject teleObject) {
            final TeleArrayObject teleArrayObject = (TeleArrayObject) teleObject;
            final ClassActor classActorForType = teleArrayObject.classActorForType();
            final String name = classActorForType.simpleName();
            final int length = teleArrayObject.getLength();
            return objectReference(null, teleArrayObject, null, name.substring(0, name.length() - 1) + length + "]");
        }

        public String referenceToolTipText(TeleObject teleObject) {
            final TeleArrayObject teleArrayObject = (TeleArrayObject) teleObject;
            final ClassActor classActorForType = teleArrayObject.classActorForType();
            final String name = classActorForType.name().toString();
            final int length = teleArrayObject.getLength();
            return objectReference(null, teleArrayObject, null, name.substring(0, name.length() - 1) + length + "]");
        }
    }

    /**
     * Textual renderer for references to static and dynamic hubs.
     */
    private class HubReferenceRenderer implements ReferenceRenderer{

        public String referenceLabelText(TeleObject teleObject) {
            final TeleHub teleHub = (TeleHub) teleObject;
            final Class javaType = teleHub.hub().classActor().toJava();
            return objectReference(null, teleHub, teleHub.maxineTerseRole(), javaType.getSimpleName());
        }

        public String referenceToolTipText(TeleObject teleObject) {
            final TeleHub teleHub = (TeleHub) teleObject;
            final Class javaType = teleHub.hub().classActor().toJava();
            if (!(javaType.isPrimitive() || Word.class.isAssignableFrom(javaType))) {
                return objectReference(null, teleHub, teleHub.maxineRole(), javaType.getName());
            }
            return null;
        }
    }

    /**
     * Textual renderer for references to ordinary objects, represented as tuples, for which there is no more specific renderer registered.
     */
    private class TupleObjectReferenceRenderer implements ReferenceRenderer{
        public String referenceLabelText(TeleObject teleObject) {
            final TeleTupleObject teleTupleObject = (TeleTupleObject) teleObject;
            final ClassActor classActorForType = teleTupleObject.classActorForType();
            if (classActorForType != null) {
                return objectReference(null, teleTupleObject, null, classActorForType.simpleName());
            }
            return null;
        }

        public String referenceToolTipText(TeleObject teleObject) {
            final TeleTupleObject teleTupleObject = (TeleTupleObject) teleObject;
            final ClassActor classActorForType = teleTupleObject.classActorForType();
            if (classActorForType != null) {
                return objectReference(null, teleTupleObject, null, classActorForType.name().toString());
            }
            return null;
        }
    }

    private class StaticTupleReferenceRenderer implements ReferenceRenderer{

        public String referenceLabelText(TeleObject teleObject) {
            final TeleStaticTuple teleStaticTuple = (TeleStaticTuple) teleObject;
            final ClassActor classActorForType = teleStaticTuple.classActorForType();
            return objectReference(null, teleStaticTuple, teleStaticTuple.maxineTerseRole(), classActorForType.simpleName());
        }

        public String referenceToolTipText(TeleObject teleObject) {
            final TeleStaticTuple teleStaticTuple = (TeleStaticTuple) teleObject;
            final ClassActor classActorForType = teleStaticTuple.classActorForType();
            return objectReference(null, teleStaticTuple, teleStaticTuple.maxineRole(), classActorForType.qualifiedName());
        }
    }

    private class MethodActorReferenceRenderer implements ReferenceRenderer{

        public String referenceLabelText(TeleObject teleObject) {
            final TeleMethodActor teleMethodActor = (TeleMethodActor) teleObject;
            final MethodActor methodActor = teleMethodActor.methodActor();
            return objectReference(null, teleObject, teleObject.maxineTerseRole(), methodActor.name().toString() + "()")  + methodSubstitutionShortAnnotation(teleMethodActor);
        }

        public String referenceToolTipText(TeleObject teleObject) {
            final TeleMethodActor teleMethodActor = (TeleMethodActor) teleObject;
            final MethodActor methodActor = teleMethodActor.methodActor();
            return objectReference(null, teleObject, teleObject.maxineRole(), methodActor.format("%r %n(%p)"))  + methodSubstitutionLongAnnotation(teleMethodActor);
        }
    }

    private class FieldActorReferenceRenderer implements ReferenceRenderer{

        public String referenceLabelText(TeleObject teleObject) {
            final TeleFieldActor teleFieldActor = (TeleFieldActor) teleObject;
            final FieldActor fieldActor = teleFieldActor.fieldActor();
            return objectReference(null, teleObject, teleObject.maxineTerseRole(), fieldActor.name().toString());
        }

        public String referenceToolTipText(TeleObject teleObject) {
            final TeleFieldActor teleFieldActor = (TeleFieldActor) teleObject;
            final FieldActor fieldActor = teleFieldActor.fieldActor();
            return objectReference(null, teleObject, teleObject.maxineRole(), fieldActor.format("%t %n"));
        }
    }

    private class ClassActorReferenceRenderer implements ReferenceRenderer{

        public String referenceLabelText(TeleObject teleObject) {
            final TeleClassActor teleClassActor = (TeleClassActor) teleObject;
            final ClassActor classActor = teleClassActor.classActor();
            return objectReference(null, teleObject,  teleObject.maxineTerseRole(), classActor.toJava().getSimpleName());
        }

        public String referenceToolTipText(TeleObject teleObject) {
            final TeleClassActor teleClassActor = (TeleClassActor) teleObject;
            final ClassActor classActor = teleClassActor.classActor();
            return objectReference(null, teleObject, teleObject.maxineRole(), classActor.name().toString());
        }
    }

    private static final int _maxStringLength = 40;

    private class StringReferenceRenderer implements ReferenceRenderer{

        public String referenceLabelText(TeleObject teleObject) {
            final TeleString teleString = (TeleString) teleObject;
            final String s = teleString.getString();
            if (s.length() > _maxStringLength) {
                return objectReference(null, teleObject, null, "\"" + s.substring(0, _maxStringLength) + "\"...");
            }
            return objectReference(null, teleObject, null, "\"" + s + "\"");
        }

        public String referenceToolTipText(TeleObject teleObject) {
            final TeleString teleString = (TeleString) teleObject;
            final ClassActor classActorForType = teleString.classActorForType();
            final String s = teleString.getString();
            return objectReference(null, teleObject, classActorForType.qualifiedName(), "\"" + s + "\"");
        }
    }

    private class Utf8ConstantReferenceRenderer implements ReferenceRenderer{
        public String referenceLabelText(TeleObject teleObject) {
            final TeleUtf8Constant teleUtf8Constant = (TeleUtf8Constant) teleObject;
            final String s = teleUtf8Constant.getString();
            if (s.length() > _maxStringLength) {
                return objectReference(null, teleObject, null, "\"" + s.substring(0, _maxStringLength) + "\"...");
            }
            return objectReference(null, teleObject, null, "\"" + s + "\"");
        }

        public String referenceToolTipText(TeleObject teleObject) {
            final TeleUtf8Constant teleUtf8Constant = (TeleUtf8Constant) teleObject;
            final ClassActor classActorForType = teleUtf8Constant.classActorForType();
            final String s = teleUtf8Constant.getString();
            return objectReference(null, teleObject, classActorForType.qualifiedName(), "\"" + s + "\"");
        }
    }

    private class StringConstantReferenceRenderer implements ReferenceRenderer{

        public String referenceLabelText(TeleObject teleObject) {
            final TeleStringConstant teleStringConstant = (TeleStringConstant) teleObject;
            final String s = teleStringConstant.getString();
            if (s.length() > _maxStringLength) {
                return objectReference(null, teleObject, null, "\"" + s.substring(0, _maxStringLength) + "\"...");
            }
            return objectReference(null, teleObject, null, "\"" + s + "\"");
        }

        public String referenceToolTipText(TeleObject teleObject) {
            final TeleStringConstant teleStringConstant = (TeleStringConstant) teleObject;
            final ClassActor classActorForType = teleStringConstant.classActorForType();
            final String s = teleStringConstant.getString();
            return objectReference(null, teleObject, classActorForType.qualifiedName(), "\"" + s + "\"");
        }
    }

    private class ClassReferenceRenderer implements ReferenceRenderer{

        public String referenceLabelText(TeleObject teleObject) {
            final TeleClass teleClass = (TeleClass) teleObject;
            final Class mirrorJavaClass = teleClass.toJava();
            return objectReference(null, teleObject,  teleObject.maxineTerseRole(), mirrorJavaClass.getSimpleName() + ".class");
        }

        public String referenceToolTipText(TeleObject teleObject) {
            final TeleClass teleClass = (TeleClass) teleObject;
            final Class mirrorJavaClass = teleClass.toJava();
            return objectReference(null, teleObject, teleObject.maxineRole(), mirrorJavaClass.getName() + ".class");
        }
    }

    private class ConstructorReferenceRenderer implements ReferenceRenderer{

        public String referenceLabelText(TeleObject teleObject) {
            final TeleConstructor teleConstructor = (TeleConstructor) teleObject;
            final Constructor mirrorJavaConstructor = teleConstructor.toJava();
            if (mirrorJavaConstructor != null) {
                return objectReference(null, teleObject,  teleObject.maxineTerseRole(), mirrorJavaConstructor.getName() + "()");
            }
            return null;
        }

        public String referenceToolTipText(TeleObject teleObject) {
            final TeleConstructor teleConstructor = (TeleConstructor) teleObject;
            final Constructor mirrorJavaConstructor = teleConstructor.toJava();
            if (mirrorJavaConstructor != null) {
                return objectReference(null, teleObject, teleObject.maxineRole(), mirrorJavaConstructor.toString());
            }
            return null;
        }
    }

    private class FieldReferenceRenderer implements ReferenceRenderer{

        public String referenceLabelText(TeleObject teleObject) {
            final TeleField teleField = (TeleField) teleObject;
            final Field mirrorJavaField = teleField.toJava();
            if (mirrorJavaField != null) {
                return objectReference(null, teleObject,  teleObject.maxineTerseRole(), mirrorJavaField.getName());
            }
            return null;
        }

        public String referenceToolTipText(TeleObject teleObject) {
            final TeleField teleField = (TeleField) teleObject;
            final Field mirrorJavaField = teleField.toJava();
            if (mirrorJavaField != null) {
                return objectReference(null, teleObject, teleObject.maxineRole(), mirrorJavaField.toString());
            }
            return null;
        }
    }

    private class MethodReferenceRenderer implements ReferenceRenderer{

        public String referenceLabelText(TeleObject teleObject) {
            final TeleMethod teleMethod = (TeleMethod) teleObject;
            final Method mirrorJavaMethod = teleMethod.toJava();
            if (mirrorJavaMethod != null) {
                return objectReference(null, teleObject, teleObject.maxineTerseRole(), mirrorJavaMethod.getName() + "()");
            }
            return null;
        }

        public String referenceToolTipText(TeleObject teleObject) {
            final TeleMethod teleMethod = (TeleMethod) teleObject;
            final Method mirrorJavaMethod = teleMethod.toJava();
            if (mirrorJavaMethod != null) {
                return objectReference(null, teleObject, teleObject.maxineRole(), mirrorJavaMethod.toString());
            }
            return null;
        }
    }

    private class EnumReferenceRenderer implements ReferenceRenderer{

        public String referenceLabelText(TeleObject teleObject) {
            final TeleEnum teleEnum = (TeleEnum) teleObject;
            final ClassActor classActorForType = teleEnum.classActorForType();
            final String name = teleEnum.toJava().name();
            return objectReference(null, teleObject, null, classActorForType.toJava().getSimpleName() + "." + name);
        }

        public String referenceToolTipText(TeleObject teleObject) {
            final TeleEnum teleEnum = (TeleEnum) teleObject;
            final ClassActor classActorForType = teleEnum.classActorForType();
            final String name = teleEnum.toJava().name();
            final int ordinal = teleEnum.toJava().ordinal();
            return objectReference(null, teleObject, null, classActorForType.qualifiedName() + "." + name + " ordinal=" + ordinal);
        }
    }

    private class ConstantPoolReferenceRenderer implements ReferenceRenderer{

        public String referenceLabelText(TeleObject teleObject) {
            final TeleConstantPool teleConstantPool = (TeleConstantPool) teleObject;
            final ClassActor classActor = teleConstantPool.getTeleHolder().classActor();
            return objectReference(null, teleObject, teleObject.maxineTerseRole(), classActor.toJava().getSimpleName());
        }

        public String referenceToolTipText(TeleObject teleObject) {
            final TeleConstantPool teleConstantPool = (TeleConstantPool) teleObject;
            final ClassActor classActor = teleConstantPool.getTeleHolder().classActor();
            return objectReference(null, teleObject, teleObject.maxineRole(), classActor.name().toString());
        }
    }

    private class ClassConstantResolvedReferenceRenderer implements ReferenceRenderer{

        public String referenceLabelText(TeleObject teleObject) {
            final TeleClassConstant.Resolved teleClassConstantResolved = (TeleClassConstant.Resolved) teleObject;
            final ClassActor classActor = teleClassConstantResolved.getTeleClassActor().classActor();
            return objectReference(null, teleObject,  teleObject.maxineTerseRole(), classActor.toJava().getSimpleName());
        }

        public String referenceToolTipText(TeleObject teleObject) {
            final TeleClassConstant.Resolved teleClassConstantResolved = (TeleClassConstant.Resolved) teleObject;
            final ClassActor classActor = teleClassConstantResolved.getTeleClassActor().classActor();
            return objectReference(null, teleObject, teleObject.maxineRole(), classActor.name().toString());
        }
    }

    private class FieldRefConstantResolvedReferenceRenderer implements ReferenceRenderer{

        public String referenceLabelText(TeleObject teleObject) {
            final TeleFieldRefConstant.Resolved teleFieldRefConstantResolved = (TeleFieldRefConstant.Resolved) teleObject;
            final FieldActor fieldActor = teleFieldRefConstantResolved.getTeleFieldActor().fieldActor();
            return objectReference(null, teleObject, teleObject.maxineTerseRole(), fieldActor.name().toString());
        }

        public String referenceToolTipText(TeleObject teleObject) {
            final TeleFieldRefConstant.Resolved teleFieldRefConstantResolved = (TeleFieldRefConstant.Resolved) teleObject;
            final FieldActor fieldActor = teleFieldRefConstantResolved.getTeleFieldActor().fieldActor();
            return objectReference(null, teleObject, teleObject.maxineRole(), fieldActor.format("%T %n"));
        }
    }

    private class ClassMethodRefConstantResolvedReferenceRenderer implements ReferenceRenderer{

        public String referenceLabelText(TeleObject teleObject) {
            final TeleClassMethodRefConstant.Resolved teleClassMethodRefConstantResolved = (TeleClassMethodRefConstant.Resolved) teleObject;
            final MethodActor methodActor = teleClassMethodRefConstantResolved.getTeleClassMethodActor().methodActor();
            return objectReference(null, teleObject, teleObject.maxineTerseRole(), methodActor.name().toString());
        }

        public String referenceToolTipText(TeleObject teleObject) {
            final TeleClassMethodRefConstant.Resolved teleClassMethodRefConstantResolved = (TeleClassMethodRefConstant.Resolved) teleObject;
            final MethodActor methodActor = teleClassMethodRefConstantResolved.getTeleClassMethodActor().methodActor();
            return objectReference(null, teleObject, teleObject.maxineRole(), methodActor.format("%r %n(%p)"));
        }
    }

    private class InterfaceMethodRefConstantResolvedReferenceRenderer implements ReferenceRenderer{
        public String referenceLabelText(TeleObject teleObject) {
            final TeleInterfaceMethodRefConstant.Resolved teleInterfaceMethodRefConstantResolved = (TeleInterfaceMethodRefConstant.Resolved) teleObject;
            final MethodActor methodActor = teleInterfaceMethodRefConstantResolved.getTeleInterfaceMethodActor().methodActor();
            return objectReference(null, teleObject, teleObject.maxineTerseRole(), methodActor.name().toString());
        }

        public String referenceToolTipText(TeleObject teleObject) {
            final TeleInterfaceMethodRefConstant.Resolved teleInterfaceMethodRefConstantResolved = (TeleInterfaceMethodRefConstant.Resolved) teleObject;
            final MethodActor methodActor = teleInterfaceMethodRefConstantResolved.getTeleInterfaceMethodActor().methodActor();
            return objectReference(null, teleObject, teleObject.maxineRole(), methodActor.format("%r %n(%p)"));
        }
    }

    private class PoolConstantReferenceRenderer implements ReferenceRenderer{
        public String referenceLabelText(TeleObject teleObject) {
            final TelePoolConstant telePoolConstant = (TelePoolConstant) teleObject;
            final ClassActor classActorForType = telePoolConstant.classActorForType();
            return objectReference(null, teleObject, null, classActorForType.simpleName());
        }

        public String referenceToolTipText(TeleObject teleObject) {
            final TelePoolConstant telePoolConstant = (TelePoolConstant) teleObject;
            final ClassActor classActorForType = telePoolConstant.classActorForType();
            return objectReference(null, teleObject, null, "PoolConstant: " + classActorForType.qualifiedName());
        }
    }

    private class VmThreadReferenceRenderer implements ReferenceRenderer{
        public String referenceLabelText(TeleObject teleObject) {
            final TeleVmThread teleVmThread = (TeleVmThread) teleObject;
            return objectReference(null, teleObject, teleVmThread.classActorForType().simpleName(), longName(teleVmThread));
        }

        public String referenceToolTipText(TeleObject teleObject) {
            final TeleVmThread teleVmThread = (TeleVmThread) teleObject;
            return objectReference(null, teleObject, teleVmThread.classActorForType().simpleName(), longName(teleVmThread));
        }
    }

}
