/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins;

import java.lang.reflect.*;
import java.util.*;

import com.sun.cri.ci.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.util.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;

/**
 * Standardized ways to display textual names of common entities during Inspection sessions.
 */
public final class InspectorNameDisplay extends AbstractInspectionHolder {

    public InspectorNameDisplay(Inspection inspection) {
        super(inspection);
        referenceRenderers.put(TeleArrayObject.class, new ArrayReferenceRenderer());
        referenceRenderers.put(TeleHub.class, new HubReferenceRenderer());
        referenceRenderers.put(TeleTupleObject.class, new TupleObjectReferenceRenderer());
        referenceRenderers.put(TeleStaticTuple.class, new StaticTupleReferenceRenderer());
        referenceRenderers.put(TeleMethodActor.class, new MethodActorReferenceRenderer());
        referenceRenderers.put(TeleFieldActor.class, new FieldActorReferenceRenderer());
        referenceRenderers.put(TeleClassActor.class, new ClassActorReferenceRenderer());
        referenceRenderers.put(TeleString.class, new StringReferenceRenderer());
        referenceRenderers.put(TeleUtf8Constant.class, new Utf8ConstantReferenceRenderer());
        referenceRenderers.put(TeleStringConstant.class, new StringConstantReferenceRenderer());
        referenceRenderers.put(TeleClass.class, new ClassReferenceRenderer());
        referenceRenderers.put(TeleConstructor.class, new ConstructorReferenceRenderer());
        referenceRenderers.put(TeleField.class, new FieldReferenceRenderer());
        referenceRenderers.put(TeleMethod.class, new MethodReferenceRenderer());
        referenceRenderers.put(TeleEnum.class, new EnumReferenceRenderer());
        referenceRenderers.put(TeleConstantPool.class, new ConstantPoolReferenceRenderer());
        referenceRenderers.put(TeleClassConstant.Resolved.class, new ClassConstantResolvedReferenceRenderer());
        referenceRenderers.put(TeleFieldRefConstant.Resolved.class, new FieldRefConstantResolvedReferenceRenderer());
        referenceRenderers.put(TeleClassMethodRefConstant.Resolved.class, new ClassMethodRefConstantResolvedReferenceRenderer());
        referenceRenderers.put(TeleInterfaceMethodRefConstant.Resolved.class, new InterfaceMethodRefConstantResolvedReferenceRenderer());
        referenceRenderers.put(TelePoolConstant.class, new PoolConstantReferenceRenderer());
        referenceRenderers.put(TeleVmThread.class, new VmThreadReferenceRenderer());
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
     * Support for a standardized way to identify a heap object in the VM.
     *
     * @param prefix an optional string to precede everything else
     * @param object an optional surrogate for the tele object being named, null if local
     * @param role an optional "role" name for low level VM objects whose implementation types aren't too interesting
     * @param type a name to describe the object, type name in simple cases
     * @return human readable string identifying an object in a standard format
     */
    private String objectReference(String prefix, MaxObject object, String role, String type) {
        final StringBuilder name = new StringBuilder(32);
        if (prefix != null) {
            name.append(prefix);
        }

        if (object != null) {
            name.append('<');
            name.append(object.getOID());
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
     * @return a short string, suitable for label text, to use in place of data that should be read from the VM,
     * but which cannot be for some reason (no process, process terminated, other i/o error).
     */
    public String unavailableDataShortText() {
        return "<?>";
    }

    /**
     * @return a long string, suitable for tool tip text, to use in place of data that should be read from the VM,
     * but which cannot be for some reason (no process, process terminated, other i/o error).
     */
    public String unavailableDataLongText() {
        return "<?> Data unavailable";
    }


    /**
     * @return a short string, suitable for label text, to use in place of data that should be read from the VM,
     * but which cannot be for some reason (no process, process terminated, other i/o error).
     */
    public String zappedDataShortText() {
        return "<ZAPPED>";
    }

    /**
     * @return a long string, suitable for tool tip text, to use in place of data that should be read from the VM,
     * but which cannot be for some reason (no process, process terminated, other i/o error).
     */
    public String zappedDataLongText() {
        return "Memory contents ZAPPED";
    }


    /**
     * @return a short string, suitable for label text, to use in place of data that should be read from the VM,
     * but which cannot be for some reason (no process, process terminated, other i/o error).
     */
    public String noProcessShortText() {
        return "<no process>";
    }

    /**
     * @return human readable string identifying a thread in a terse standard format.
     */
    public String shortName(MaxThread thread) {
        if (thread == null) {
            return "null";
        }
        if (thread.isPrimordial()) {
            return "primordial";
        }
        if (thread.isJava()) {
            final String vmThreadName = thread.vmThreadName();
            return vmThreadName == null ? unavailableDataShortText() : vmThreadName;
        }
        return "native unnamed";
    }

    /**
     * @return human readable string identifying a thread in a standard format.
     */
    public String longName(MaxThread thread) {
        if (thread == null) {
            return "null";
        }
        return shortName(thread) + " [" + thread.id() + "]";
    }

    /**
     * @return human readable string identifying a thread in a standard format.
     */
    public String longNameWithState(MaxThread thread) {
        if (thread == null) {
            return "null";
        }
        return longName(thread) + " (" + thread.state() + ")";
    }

    /**
     * An identifier for a particular compilation that distinguishes it from other
     * compilations of the same method.
     *
     * E.g.: "[B]", meaning a baseline compilation
     */
    public String shortMethodCompilationID(MaxCompilation compilation) {
        // Only have an index if a compiled method.
        if (compilation != null && compilation.getTeleClassMethodActor() != null) {
            return "[" + compilation.shortDesignator() + "]";
        }
        return "";
    }

    /**
     * An identifier for a particular compilation that distinguishes it from other
     * compilations of the same method.
     *
     * E.g.: "[OPTIMIZED], meaning an optimized compilation
     */
    public String longMethodCompilationID(MaxCompilation compilation) {
        // Only have an index if a compiled method.
        if (compilation != null && compilation.getTeleClassMethodActor() != null) {
            return "[" + compilation.longDesignator() + "]";
        }
        return "";
    }

   /**
     * E.g. an asterisk when a method has been substituted.
     */
    public String methodSubstitutionShortAnnotation(TeleMethodActor teleMethodActor) {
        if (teleMethodActor == null) {
            return "";
        }
        try {
            vm().acquireLegacyVMAccess();
            try {
                return teleMethodActor.isSubstituted() ? " *" : "";
            } finally {
                vm().releaseLegacyVMAccess();
            }
        } catch (MaxVMBusyException e) {
            return unavailableDataShortText();
        }
    }

    /**
     * E.g. an asterisk when a method has been substituted.
     */
    public String methodSubstitutionLongAnnotation(TeleMethodActor teleMethodActor) {
        if (teleMethodActor == null) {
            return "";
        }
        try {
            vm().acquireLegacyVMAccess();
            try {
                return teleMethodActor.isSubstituted() ? " *=substituted from " + teleMethodActor.teleClassActorSubstitutedFrom().getName() : "";
            } finally {
                vm().releaseLegacyVMAccess();
            }
        } catch (MaxVMBusyException e) {
            return unavailableDataLongText();
        }
    }

    /**
     * E.g. "foo()[0]"
     */
    public String extremelyShortName(MaxCompilation compilation) {
        if (compilation == null) {
            return unavailableDataShortText();
        }
        return compilation.classMethodActor() == null ?
                        compilation.entityName() :
                            compilation.classMethodActor().format("%n()" + shortMethodCompilationID(compilation));
    }

    /**
     * E.g. "Element.foo()[0]"
     */
    public String veryShortName(MaxCompilation compilation) {
        if (compilation == null) {
            return unavailableDataShortText();
        }
        return compilation.classMethodActor() == null ?
                        compilation.entityName() :
                            compilation.classMethodActor().format("%h.%n()" + shortMethodCompilationID(compilation));
    }


    /**
     * E.g. "foo(Pointer, Word, int[])[0]"
     */
    public String shortName(MaxCompilation compilation) {
        try {
            vm().acquireLegacyVMAccess();
            try {
                return compilation.classMethodActor() == null ?
                                compilation.entityName() :
                                    compilation.classMethodActor().format("%n(%p)" + shortMethodCompilationID(compilation));
            } finally {
                vm().releaseLegacyVMAccess();
            }
        } catch (MaxVMBusyException e) {
            return unavailableDataShortText();
        }
    }

    /**
     * E.g. "int foo(Pointer, Word, int[])[0]"
     *
     * @param returnTypeSpecification specifies where the return type should appear in the returned value
     */
    public String shortName(MaxCompilation compilation, ReturnTypeSpecification returnTypeSpecification) {
        final ClassMethodActor classMethodActor = compilation.classMethodActor();

        if (classMethodActor == null) {
            return compilation.entityName();
        }

        switch (returnTypeSpecification) {
            case ABSENT: {
                return classMethodActor.format("%n(%p)" + shortMethodCompilationID(compilation));
            }
            case AS_PREFIX: {
                return classMethodActor.format("%r %n(%p)" + shortMethodCompilationID(compilation));
            }
            case AS_SUFFIX: {
                return classMethodActor.format("%n(%p)" + shortMethodCompilationID(compilation) + " %r");
            }
            default: {
                throw InspectorError.unknownCase();
            }
        }
    }

    private String positionString(MaxCompilation compilation, Address address) {
        final Address entry = compilation.getCodeStart();
        final long position = address.minus(entry).toLong();
        return position == 0 ? "" : "+" + InspectorLabel.longTo0xHex(position);
    }

    /**
     * E.g. "int foo(Pointer, Word, int[])[0] in com.sun.max.ins.Bar"
     */
    public String longName(MaxCompilation compilation) {
        return compilation.classMethodActor() ==
            null ? compilation.entityDescription() :
                compilation.classMethodActor().format("%r %n(%p)" + shortMethodCompilationID(compilation) + " in %H");
    }

    /**
     * E.g. "foo()[0]+0x7"
     */
    public String veryShortName(MaxCompilation compilation, Address address) {
        return compilation.classMethodActor() ==
            null ? compilation.entityName() :
                compilation.classMethodActor().format("%n()" + shortMethodCompilationID(compilation) + positionString(compilation, address));
    }

    /**
     * E.g. "int foo(Pointer, Word, int[])[0]+0x7 in com.sun.max.ins.Bar"
     */
    public String longName(MaxCompilation compilation, Address address) {
        if (compilation == null) {
            return unavailableDataLongText();
        }
        if (compilation.classMethodActor() != null) {
            return compilation.classMethodActor().format("%r %n(%p)" + shortMethodCompilationID(compilation) + positionString(compilation, address) + " in %H");
        }
        return compilation.entityName();
    }


    /**
     * E.g. "foo()"
     */
    public String veryShortName(TeleClassMethodActor teleClassMethodActor) {
        final ClassMethodActor classMethodActor = teleClassMethodActor.classMethodActor();
        return classMethodActor.format("%n()");
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
                throw InspectorError.unknownCase();
            }
        }
    }

    /**
     * E.g. "int foo(Pointer, Word, int[]) in com.sun.max.ins.Bar"
     */
    public String longName(TeleClassMethodActor teleClassMethodActor) {
        if (teleClassMethodActor == null) {
            return "<no method actor>";
        }
        return teleClassMethodActor.classMethodActor().format("%r %n(%p)" + " in %H");
    }

    /**
     * E.g. "virtualMemory_pageAlign" if known, default "@0xffffffffffffffff"
     */
    public String shortName(MaxNativeFunction nativeFunction) {
        final String title = nativeFunction.name();
        return title == null ? "@0x" + nativeFunction.getCodeStart().toHexString() : title;
    }

    /**
     * E.g. "libjvmlinkage:virtualMemory_pageAlign" if known, default "@0xffffffffffffffff"
     */
    public String longName(MaxNativeFunction nativeFunction) {
        final String title = nativeFunction.qualName();
        return title == null ? "Native function @" + nativeFunction.getCodeStart().to0xHexString() : title;
    }

    /**
     * E.g. "int foo(Pointer, Word, int[]) in com.sun.max.ins.Bar"
     * E.g. "int foo(Pointer, Word, int[])+14 in com.sun.max.ins.Bar"
     */
    public String longName(CiCodePos codePos) {
        final int position = codePos.bci;
        ClassMethodActor classMethodActor = (ClassMethodActor) codePos.method;
        return classMethodActor.format("%r %n(%p)" + (position != 0 ? " +" + codePos.bci : "") + " in %H");
    }

    /**
     * E.g. "int foo(Pointer, Word, int[])  in com.sun.max.ins.Bar"
     */
    public String longName(MethodKey key) {
        final StringBuilder name = new StringBuilder();
        name.append(key.signature().resultDescriptor().toJavaString(false)).append(" ").append(key.name()).append(key.signature().toJavaString(false, false));
        name.append(" in ").append(key.holder().toJavaString());
        return name.toString();
    }

    /**
     * E.g. "foo() in com.sun.max.ins.Bar"
     */
    public String shortName(MethodKey key) {
        final StringBuilder name = new StringBuilder();
        name.append(key.name()).append(key.signature().toJavaString(false, false));
        name.append(" in ").append(key.holder().toJavaString());
        return name.toString();
    }

    /**
     * E.g. "0x11111111, int foo(int, int)[0]+0x235 in com.sun.Example, bci=-1.
     */
    public String longName(MaxCodeLocation codeLocation) {
        if (codeLocation == null) {
            return unavailableDataShortText();
        }
        final StringBuilder name = new StringBuilder();
        if (codeLocation.hasAddress()) {
            final Address address = codeLocation.address();
            name.append(address.to0xHexString());
            TeleNativeFunction nativeFunction = vm().machineCode().findNativeFunction(address);
            if (nativeFunction != null) {
                name.append(" in " + longName(nativeFunction));
            } else {
                final MaxCompilation compilation = vm().machineCode().findCompilation(address);
                if (compilation != null) {
                    name.append(",  ").append(longName(compilation, address));
                }
                if (codeLocation.hasTeleClassMethodActor()) {
                    name.append(", bci=").append(codeLocation.bci());
                } else if (codeLocation.methodKey() != null) {
                    name.append(", MethodKey=").append(longName(codeLocation.methodKey()));
                }
            }
        } else if (codeLocation.hasTeleClassMethodActor()) {
            name.append(longName(codeLocation.teleClassMethodActor())).append(", bci=").append(codeLocation.bci());
        } else if (codeLocation.methodKey() != null) {
            name.append("MethodKey=").append(longName(codeLocation.methodKey()));
        }
        return name.toString();
    }

    /**
     * E.g. "0x11111111 foo()[0] com.sun.Example bci=-1.
     */
    public String shortName(MaxCodeLocation codeLocation) {
        if (codeLocation == null) {
            return unavailableDataShortText();
        }
        final StringBuilder name = new StringBuilder();
        if (codeLocation.hasAddress()) {
            final Address address = codeLocation.address();
            name.append(address.to0xHexString()).append(" ");
            TeleNativeFunction nativeFunction = vm().machineCode().findNativeFunction(address);
            if (nativeFunction != null) {
                name.append(" in " + shortName(nativeFunction));
            } else {
                final MaxCompilation compilation = vm().machineCode().findCompilation(address);
                if (compilation != null) {
                    name.append(extremelyShortName(compilation)).append(" ");
                }
                if (codeLocation.hasTeleClassMethodActor()) {
                    name.append(" bci=").append(codeLocation.bci());
                } else if (codeLocation.methodKey() != null) {
                    name.append(shortName(codeLocation.methodKey()));
                }
            }
        } else if (codeLocation.hasTeleClassMethodActor()) {
            name.append(veryShortName(codeLocation.teleClassMethodActor())).append(" bci=").append(codeLocation.bci());
        } else if (codeLocation.methodKey() != null) {
            name.append(shortName(codeLocation.methodKey()));
        }
        return name.toString();
    }

    public String shortName(MaxMemoryRegion memoryRegion) {
        return memoryRegion.regionName();
    }

    public String longName(MaxMemoryRegion memoryRegion) {
        assert memoryRegion != null;
        final String regionName = memoryRegion.regionName();

        // Is it a heap region?
        if (memoryRegion.sameAs(vm().heap().bootHeapRegion().memoryRegion())) {
            return "boot heap region";
        }
        for (MaxHeapRegion heapRegion : vm().heap().heapRegions()) {
            if (memoryRegion.sameAs(heapRegion.memoryRegion())) {
                return "dynamic heap region \"" + regionName + "\"";
            }
        }
        final MaxHeapRegion immortalHeapRegion = vm().heap().immortalHeapRegion();
        if (immortalHeapRegion != null && memoryRegion.sameAs(immortalHeapRegion.memoryRegion())) {
            return "immortal heap \"" + regionName + "\"";
        }

        // Is it a compiled code region?
        if (memoryRegion.sameAs(vm().codeCache().bootCodeRegion().memoryRegion())) {
            return "boot code region \"" + regionName + "\"";
        }
        for (MaxCodeCacheRegion codeRegion : vm().codeCache().codeCacheRegions()) {
            if (memoryRegion.sameAs(codeRegion.memoryRegion())) {
                return "dynamic code region \"" + regionName + "\"";
            }
        }

        // Is it a thread-related region?
        for (MaxThread thread : vm().threadManager().threads()) {
            if (memoryRegion.sameAs(thread.stack().memoryRegion())) {
                return "stack region for thread: " + longName(thread);
            }
            if (memoryRegion.sameAs(thread.localsBlock().memoryRegion())) {
                return "locals region for thread: " + longName(thread);
            }
        }
        return regionName;
    }

    /**
     * Creates a comma-separated list of register names.
     *
     * @param registers a possibly empty list of registers
     * @return a string containing the register names, separated by comma-space
     */
    public String registerNameList(List<MaxRegister> registers) {
        String nameList = "";
        for (MaxRegister register : registers) {
            if (nameList.length() > 0) {
                nameList += ",";
            }
            nameList += register.name();
        }
        return nameList;

    }

    /**
     * Renderer for a textual label reference pointing at heap objects in the VM.
     */
    private interface ReferenceRenderer {


        /**
         * @return a short string suitable for a text label display of the object reference.
         */
        String referenceLabelText(MaxObject object);

        /**
         * @return a short string suitable for a text label display of the object reference,
         * with a limited number of characters.
         */
        String referenceLabelText(MaxObject object, int maxLength);

        /**
         * @return a longer string suitable for a tooltip display over the object reference.
         */
        String referenceToolTipText(MaxObject object);

        /**
         * @return a longer string suitable for a tooltip display over the object reference,
         * with a limited number of characters.
         */
        String referenceToolTipText(MaxObject object, int maxLength);
    }

    private static abstract class AbstractReferenceRenderer implements ReferenceRenderer {

        /**
         * {@inheritDoc}
         * <p>
         * Default implementation of character limits for renderers.
         */
        public String referenceLabelText(MaxObject object, int maxLength) {
            final String text = referenceLabelText(object);
            if (text.length() < maxLength) {
                return text;
            }
            return text.substring(0, maxLength - 4) + "...";
        }

        /**
         * {@inheritDoc}
         * <p>
         * Default implementation of character limits for renderers.
         */
        public String referenceToolTipText(MaxObject object, int maxLength) {
            final String text = referenceToolTipText(object);
            if (text.length() < maxLength) {
                return text;
            }
            return text.substring(0, maxLength - 1);
        }

    }

    /**
     * Renderers for specific classes of objects in the heap in the VM.
     * The most specific class that matches a particular {@link MaxObject} will
     * be used, in an emulation of virtual method dispatch.  All heap objects are
     * implemented as tuples, hubs. or arrays, so there should always be at least
     * a generic match for every type.
     */
    private final Map<Class, ReferenceRenderer> referenceRenderers = new HashMap<Class, ReferenceRenderer>();

    /**
     * Creates a short textual presentation of a reference to a heap object in the VM, if possible, null if not.
     *
     * @param object surrogate for a heap object in the VM
     * @return a textual presentation of a reference to a heap object
     */
    public String referenceLabelText(MaxObject object) {
        return referenceLabelText(object, Integer.MAX_VALUE);
    }

    /**
     * Creates a short textual presentation of a reference to a heap object in the VM, if possible, null if not.
     *
     * @param object surrogate for a heap object in the VM
     * @param maxLength maximum number of characters that should be produced, thought not guaranteed
     * @return a textual presentation of a reference to a heap object
     */
    public String referenceLabelText(MaxObject object, int maxLength) {
        if (object != null) {
            Class objectClass = object.getClass();
            while (objectClass != null) {
                final ReferenceRenderer objectReferenceRenderer = referenceRenderers.get(objectClass);
                if (objectReferenceRenderer != null) {
                    try {
                        vm().acquireLegacyVMAccess();
                        try {
                            return objectReferenceRenderer.referenceLabelText(object, maxLength);
                        } finally {
                            vm().releaseLegacyVMAccess();
                        }
                    } catch (MaxVMBusyException maxVMBusyException) {
                        return unavailableDataShortText();
                    } catch (Throwable throwable) {
                        throwable.printStackTrace(Trace.stream());
                        return "(Unexpected error when getting reference label: " + throwable + ")";
                    }
                }
                objectClass = objectClass.getSuperclass();
            }
            InspectorError.unexpected("InspectorNameDisplay failed to find renderer for maxObject = " + object);
        }
        return null;
    }

    /**
     * @return a long textual presentation of a reference to a heap object in the VM, if possible, null if not.
     */
    public String referenceToolTipText(MaxObject object) {
        if (object != null) {
            try {
                final Class leafClass = object.getClass();
                Class objectClass = leafClass;
                do {
                    final ReferenceRenderer objectReferenceRenderer = referenceRenderers.get(objectClass);
                    if (objectReferenceRenderer != null) {
                        if (leafClass != objectClass) {
                            // cache it so we will not have to search for it next time.
                            referenceRenderers.put(leafClass, objectReferenceRenderer);
                        }
                        try {
                            vm().acquireLegacyVMAccess();
                            try {
                                return objectReferenceRenderer.referenceToolTipText(object);
                            } finally {
                                vm().releaseLegacyVMAccess();
                            }
                        } catch (MaxVMBusyException maxVMBusyException) {
                            return unavailableDataLongText();
                        } catch (Throwable throwable) {
                            throwable.printStackTrace(Trace.stream());
                            return "(Unexpected error when getting tool tip label: " + throwable + ")";
                        }
                    }
                    objectClass = objectClass.getSuperclass();
                } while (objectClass != null);
                InspectorError.unexpected("InspectorNameDisplay failed to find renderer for maxObject = " + object);
            } catch (Throwable e) {
                e.printStackTrace(Trace.stream());
                return "<html><b><font color=\"red\">" + e + "</font></b><br>See log for complete stack trace.";
            }
        }
        return null;
    }

    /**
     * E.g.  "Object 0x01234567890 <99>ClassActor in BootHeap Region"
     */
    public String longName(MaxObject object) {
        final Pointer origin = object.origin();
        final MaxMemoryRegion memoryRegion = vm().state().findMemoryRegion(origin);
        final String name = "Object " + origin.toHexString() + inspection().nameDisplay().referenceLabelText(object);
        final String suffix = " in "
            + (memoryRegion == null ? "unknown region" : memoryRegion.regionName());
        String prefix = "";
        final ObjectStatus memoryStatus = object.status();
        if (!memoryStatus.isLive()) {
            prefix = memoryStatus.label() + " ";
        }
        return prefix + name + suffix;
    }

    /**
     * Textual renderer for references to arrays.
     */
    private class ArrayReferenceRenderer extends AbstractReferenceRenderer {
        public String referenceLabelText(MaxObject object) {
            final TeleArrayObject teleArrayObject = (TeleArrayObject) object;
            final ClassActor classActorForType = teleArrayObject.classActorForObjectType();
            final String name = classActorForType.simpleName();
            final int length = teleArrayObject.length();
            return objectReference(null, teleArrayObject, null, name.substring(0, name.length() - 1) + length + "]");
        }

        public String referenceToolTipText(MaxObject object) {
            final TeleArrayObject teleArrayObject = (TeleArrayObject) object;
            final ClassActor classActorForType = teleArrayObject.classActorForObjectType();
            final String name = classActorForType.name.toString();
            final int length = teleArrayObject.length();
            return objectReference(null, teleArrayObject, null, name.substring(0, name.length() - 1) + length + "]");
        }
    }

    /**
     * Textual renderer for references to static and dynamic hubs.
     */
    private class HubReferenceRenderer extends AbstractReferenceRenderer {

        public String referenceLabelText(MaxObject object) {
            final TeleHub teleHub = (TeleHub) object;
            //final Class javaType = teleHub.classActorForType().toJava();
            final Class javaType = teleHub.hub().classActor.toJava();
            return objectReference(null, teleHub, teleHub.maxineTerseRole(), javaType.getSimpleName());
        }

        public String referenceToolTipText(MaxObject object) {
            final TeleHub teleHub = (TeleHub) object;
            //final Class javaType = teleHub.classActorForType().toJava();
            final Class javaType = teleHub.hub().classActor.toJava();
            if (!(javaType.isPrimitive() || Word.class.isAssignableFrom(javaType))) {
                return objectReference(null, teleHub, teleHub.maxineRole(), javaType.getName());
            }
            return null;
        }
    }

    /**
     * Textual renderer for references to ordinary objects, represented as tuples, for which there is no more specific renderer registered.
     */
    private class TupleObjectReferenceRenderer extends AbstractReferenceRenderer {
        public String referenceLabelText(MaxObject object) {
            final TeleObject teleTupleObject = (TeleObject) object;
            final ClassActor classActorForType = teleTupleObject.classActorForObjectType();
            if (classActorForType != null) {
                return objectReference(null, teleTupleObject, null, classActorForType.simpleName());
            }
            return null;
        }

        public String referenceToolTipText(MaxObject object) {
            final TeleObject teleTupleObject = (TeleObject) object;
            final ClassActor classActorForType = teleTupleObject.classActorForObjectType();
            if (classActorForType != null) {
                return objectReference(null, teleTupleObject, null, classActorForType.name.toString());
            }
            return null;
        }
    }

    private class StaticTupleReferenceRenderer extends AbstractReferenceRenderer {

        public String referenceLabelText(MaxObject object) {
            final TeleStaticTuple teleStaticTuple = (TeleStaticTuple) object;
            final ClassActor classActorForType = teleStaticTuple.classActorForObjectType();
            return objectReference(null, teleStaticTuple, teleStaticTuple.maxineTerseRole(), classActorForType.simpleName());
        }

        public String referenceToolTipText(MaxObject object) {
            final TeleStaticTuple teleStaticTuple = (TeleStaticTuple) object;
            final ClassActor classActorForType = teleStaticTuple.classActorForObjectType();
            return objectReference(null, teleStaticTuple, teleStaticTuple.maxineRole(), classActorForType.qualifiedName());
        }
    }

    private class MethodActorReferenceRenderer extends AbstractReferenceRenderer {

        public String referenceLabelText(MaxObject object) {
            final TeleMethodActor teleMethodActor = (TeleMethodActor) object;
            final MethodActor methodActor = teleMethodActor.methodActor();
            return objectReference(null, object, object.maxineTerseRole(), methodActor.name.toString() + "()")  + methodSubstitutionShortAnnotation(teleMethodActor);
        }

        public String referenceToolTipText(MaxObject object) {
            final TeleMethodActor teleMethodActor = (TeleMethodActor) object;
            final MethodActor methodActor = teleMethodActor.methodActor();
            return objectReference(null, object, object.maxineRole(), methodActor.format("%r %n(%p)"))  + methodSubstitutionLongAnnotation(teleMethodActor);
        }
    }

    private class FieldActorReferenceRenderer extends AbstractReferenceRenderer {

        public String referenceLabelText(MaxObject object) {
            final TeleFieldActor teleFieldActor = (TeleFieldActor) object;
            final FieldActor fieldActor = teleFieldActor.fieldActor();
            return objectReference(null, object, object.maxineTerseRole(), fieldActor.name.toString());
        }

        public String referenceToolTipText(MaxObject object) {
            final TeleFieldActor teleFieldActor = (TeleFieldActor) object;
            final FieldActor fieldActor = teleFieldActor.fieldActor();
            return objectReference(null, object, object.maxineRole(), fieldActor.format("%t %n"));
        }
    }

    private class ClassActorReferenceRenderer extends AbstractReferenceRenderer {

        public String referenceLabelText(MaxObject object) {
            final TeleClassActor teleClassActor = (TeleClassActor) object;
            final ClassActor classActor = teleClassActor.classActor();
            return objectReference(null, object,  object.maxineTerseRole(), classActor.toJava().getSimpleName());
        }

        public String referenceToolTipText(MaxObject object) {
            final TeleClassActor teleClassActor = (TeleClassActor) object;
            final ClassActor classActor = teleClassActor.classActor();
            return objectReference(null, object, object.maxineRole(), classActor.name.toString());
        }
    }

    private class StringReferenceRenderer extends AbstractReferenceRenderer {


        public String referenceLabelText(MaxObject object) {
            final TeleString teleString = (TeleString) object;
            final String string = teleString.getString();
            final String stringText = string == null ? unavailableDataShortText() : "\"" + string + "\"";
            return objectReference(null, object, null, stringText);
        }

        @Override
        public String referenceLabelText(MaxObject object, int maxLength) {
            final String defaultLabelText = referenceLabelText(object);
            final int trimLength = defaultLabelText.length() - maxLength;
            if (trimLength <= 0) {
                return defaultLabelText;
            }
            // The default's too long, try again with some trimming
            final TeleString teleString = (TeleString) object;
            final String stringText = teleString.getString();
            if (stringText != null) {
                if (trimLength <= stringText.length() - 3) {
                    // We can trim the string's text sufficiently and still leave room for the elipsis
                    return objectReference(null, object, null, "\"" + stringText.substring(0, stringText.length() - trimLength) + "...\"");
                }
            }
            // Give up showing string contents; just show the standard object reference text
            return objectReference(null, object, null, "");
        }

        public String referenceToolTipText(MaxObject object) {
            final TeleString teleString = (TeleString) object;
            final ClassActor classActorForType = teleString.classActorForObjectType();
            final String s = teleString.getString();
            return objectReference(null, object, classActorForType.qualifiedName(), "\"" + s + "\"");
        }
    }

    private class Utf8ConstantReferenceRenderer extends AbstractReferenceRenderer {

        public String referenceLabelText(MaxObject object) {
            final TeleUtf8Constant teleUtf8Constant = (TeleUtf8Constant) object;
            final String string = teleUtf8Constant.utf8Constant().string;
            final String stringText = string == null ? unavailableDataShortText() : "\"" + string + "\"";
            return objectReference(null, object, null, stringText);
        }

        @Override
        public String referenceLabelText(MaxObject object, int maxLength) {
            final String defaultLabelText = referenceLabelText(object);
            final int trimLength = defaultLabelText.length() - maxLength;
            if (trimLength <= 0) {
                return defaultLabelText;
            }
            // The default's too long, try again with some trimming
            final TeleUtf8Constant teleUtf8Constant = (TeleUtf8Constant) object;
            final String stringText = teleUtf8Constant.utf8Constant().string;
            if (stringText != null) {
                if (trimLength <= stringText.length() - 3) {
                    // We can trim the string's text sufficiently and still leave room for the elipsis
                    return objectReference(null, object, null, "\"" + stringText.substring(0, stringText.length() - trimLength) + "...\"");
                }
            }
            // Give up showing string contents; just show the standard object reference text
            return objectReference(null, object, null, "");
        }

        public String referenceToolTipText(MaxObject object) {
            final TeleUtf8Constant teleUtf8Constant = (TeleUtf8Constant) object;
            final ClassActor classActorForType = teleUtf8Constant.classActorForObjectType();
            final String s = teleUtf8Constant.utf8Constant().string;
            return objectReference(null, object, classActorForType.qualifiedName(), "\"" + s + "\"");
        }
    }

    private class StringConstantReferenceRenderer extends AbstractReferenceRenderer {

        public String referenceLabelText(MaxObject object) {
            final TeleStringConstant teleStringConstant = (TeleStringConstant) object;
            final String string = teleStringConstant.getString();
            final String stringText = string == null ? unavailableDataShortText() : "\"" + string + "\"";
            return objectReference(null, object, null, stringText);
        }

        @Override
        public String referenceLabelText(MaxObject object, int maxLength) {
            final String defaultLabelText = referenceLabelText(object);
            final int trimLength = defaultLabelText.length() - maxLength;
            if (trimLength <= 0) {
                return defaultLabelText;
            }
            // The default's too long, try again with some trimming
            final TeleStringConstant teleStringConstant = (TeleStringConstant) object;
            final String stringText = teleStringConstant.getString();
            if (stringText != null) {
                if (trimLength <= stringText.length() - 3) {
                    // We can trim the string's text sufficiently and still leave room for the elipsis
                    return objectReference(null, object, null, "\"" + stringText.substring(0, stringText.length() - trimLength) + "...\"");
                }
            }
            // Give up showing string contents; just show the standard object reference text
            return objectReference(null, object, null, "");
        }

        public String referenceToolTipText(MaxObject object) {
            final TeleStringConstant teleStringConstant = (TeleStringConstant) object;
            final ClassActor classActorForType = teleStringConstant.classActorForObjectType();
            final String s = teleStringConstant.getString();
            return objectReference(null, object, classActorForType.qualifiedName(), "\"" + s + "\"");
        }
    }

    private class ClassReferenceRenderer extends AbstractReferenceRenderer {

        public String referenceLabelText(MaxObject object) {
            final TeleClass teleClass = (TeleClass) object;
            final Class mirrorJavaClass = teleClass.toJava();
            return objectReference(null, object,  object.maxineTerseRole(), mirrorJavaClass.getSimpleName() + ".class");
        }

        public String referenceToolTipText(MaxObject object) {
            final TeleClass teleClass = (TeleClass) object;
            final Class mirrorJavaClass = teleClass.toJava();
            return objectReference(null, object, object.maxineRole(), mirrorJavaClass.getName() + ".class");
        }
    }

    private class ConstructorReferenceRenderer extends AbstractReferenceRenderer {

        public String referenceLabelText(MaxObject object) {
            final TeleConstructor teleConstructor = (TeleConstructor) object;
            final Constructor mirrorJavaConstructor = teleConstructor.toJava();
            if (mirrorJavaConstructor != null) {
                return objectReference(null, object,  object.maxineTerseRole(), mirrorJavaConstructor.getName() + "()");
            }
            return null;
        }

        public String referenceToolTipText(MaxObject object) {
            final TeleConstructor teleConstructor = (TeleConstructor) object;
            final Constructor mirrorJavaConstructor = teleConstructor.toJava();
            if (mirrorJavaConstructor != null) {
                return objectReference(null, object, object.maxineRole(), mirrorJavaConstructor.toString());
            }
            return null;
        }
    }

    private class FieldReferenceRenderer extends AbstractReferenceRenderer {

        public String referenceLabelText(MaxObject object) {
            final TeleField teleField = (TeleField) object;
            final Field mirrorJavaField = teleField.toJava();
            if (mirrorJavaField != null) {
                return objectReference(null, object,  object.maxineTerseRole(), mirrorJavaField.getName());
            }
            return null;
        }

        public String referenceToolTipText(MaxObject object) {
            final TeleField teleField = (TeleField) object;
            final Field mirrorJavaField = teleField.toJava();
            if (mirrorJavaField != null) {
                return objectReference(null, object, object.maxineRole(), mirrorJavaField.toString());
            }
            return null;
        }
    }

    private class MethodReferenceRenderer extends AbstractReferenceRenderer {

        public String referenceLabelText(MaxObject object) {
            final TeleMethod teleMethod = (TeleMethod) object;
            final Method mirrorJavaMethod = teleMethod.toJava();
            if (mirrorJavaMethod != null) {
                return objectReference(null, object, object.maxineTerseRole(), mirrorJavaMethod.getName() + "()");
            }
            return null;
        }

        public String referenceToolTipText(MaxObject object) {
            final TeleMethod teleMethod = (TeleMethod) object;
            final Method mirrorJavaMethod = teleMethod.toJava();
            if (mirrorJavaMethod != null) {
                return objectReference(null, object, object.maxineRole(), mirrorJavaMethod.toString());
            }
            return null;
        }
    }

    private class EnumReferenceRenderer extends AbstractReferenceRenderer {

        public String referenceLabelText(MaxObject object) {
            final TeleEnum teleEnum = (TeleEnum) object;
            final ClassActor classActorForType = teleEnum.classActorForObjectType();
            final String name = teleEnum.toJava().name();
            return objectReference(null, object, null, classActorForType.toJava().getSimpleName() + "." + name);
        }

        public String referenceToolTipText(MaxObject object) {
            final TeleEnum teleEnum = (TeleEnum) object;
            final ClassActor classActorForType = teleEnum.classActorForObjectType();
            final String name = teleEnum.toJava().name();
            final int ordinal = teleEnum.toJava().ordinal();
            return objectReference(null, object, null, classActorForType.qualifiedName() + "." + name + " ordinal=" + ordinal);
        }
    }

    private class ConstantPoolReferenceRenderer extends AbstractReferenceRenderer {

        public String referenceLabelText(MaxObject object) {
            final TeleConstantPool teleConstantPool = (TeleConstantPool) object;
            final ClassActor classActor = teleConstantPool.getTeleHolder().classActor();
            return objectReference(null, object, object.maxineTerseRole(), classActor.toJava().getSimpleName());
        }

        public String referenceToolTipText(MaxObject object) {
            final TeleConstantPool teleConstantPool = (TeleConstantPool) object;
            final ClassActor classActor = teleConstantPool.getTeleHolder().classActor();
            return objectReference(null, object, object.maxineRole(), classActor.name.toString());
        }
    }

    private class ClassConstantResolvedReferenceRenderer extends AbstractReferenceRenderer {

        public String referenceLabelText(MaxObject object) {
            final TeleClassConstant.Resolved teleClassConstantResolved = (TeleClassConstant.Resolved) object;
            final ClassActor classActor = teleClassConstantResolved.getTeleClassActor().classActor();
            return objectReference(null, object,  object.maxineTerseRole(), classActor.toJava().getSimpleName());
        }

        public String referenceToolTipText(MaxObject object) {
            final TeleClassConstant.Resolved teleClassConstantResolved = (TeleClassConstant.Resolved) object;
            final ClassActor classActor = teleClassConstantResolved.getTeleClassActor().classActor();
            return objectReference(null, object, object.maxineRole(), classActor.name.toString());
        }
    }

    private class FieldRefConstantResolvedReferenceRenderer extends AbstractReferenceRenderer {

        public String referenceLabelText(MaxObject object) {
            final TeleFieldRefConstant.Resolved teleFieldRefConstantResolved = (TeleFieldRefConstant.Resolved) object;
            final FieldActor fieldActor = teleFieldRefConstantResolved.getTeleFieldActor().fieldActor();
            return objectReference(null, object, object.maxineTerseRole(), fieldActor.name.toString());
        }

        public String referenceToolTipText(MaxObject object) {
            final TeleFieldRefConstant.Resolved teleFieldRefConstantResolved = (TeleFieldRefConstant.Resolved) object;
            final FieldActor fieldActor = teleFieldRefConstantResolved.getTeleFieldActor().fieldActor();
            return objectReference(null, object, object.maxineRole(), fieldActor.format("%T %n"));
        }
    }

    private class ClassMethodRefConstantResolvedReferenceRenderer extends AbstractReferenceRenderer {

        public String referenceLabelText(MaxObject object) {
            final TeleClassMethodRefConstant.Resolved teleClassMethodRefConstantResolved = (TeleClassMethodRefConstant.Resolved) object;
            final MethodActor methodActor = teleClassMethodRefConstantResolved.getTeleClassMethodActor().methodActor();
            return objectReference(null, object, object.maxineTerseRole(), methodActor.name.toString());
        }

        public String referenceToolTipText(MaxObject object) {
            final TeleClassMethodRefConstant.Resolved teleClassMethodRefConstantResolved = (TeleClassMethodRefConstant.Resolved) object;
            final MethodActor methodActor = teleClassMethodRefConstantResolved.getTeleClassMethodActor().methodActor();
            return objectReference(null, object, object.maxineRole(), methodActor.format("%r %n(%p)"));
        }
    }

    private class InterfaceMethodRefConstantResolvedReferenceRenderer extends AbstractReferenceRenderer {
        public String referenceLabelText(MaxObject object) {
            final TeleInterfaceMethodRefConstant.Resolved teleInterfaceMethodRefConstantResolved = (TeleInterfaceMethodRefConstant.Resolved) object;
            final MethodActor methodActor = teleInterfaceMethodRefConstantResolved.getTeleInterfaceMethodActor().methodActor();
            return objectReference(null, object, object.maxineTerseRole(), methodActor.name.toString());
        }

        public String referenceToolTipText(MaxObject object) {
            final TeleInterfaceMethodRefConstant.Resolved teleInterfaceMethodRefConstantResolved = (TeleInterfaceMethodRefConstant.Resolved) object;
            final MethodActor methodActor = teleInterfaceMethodRefConstantResolved.getTeleInterfaceMethodActor().methodActor();
            return objectReference(null, object, object.maxineRole(), methodActor.format("%r %n(%p)"));
        }
    }

    private class PoolConstantReferenceRenderer extends AbstractReferenceRenderer {
        public String referenceLabelText(MaxObject object) {
            final TelePoolConstant telePoolConstant = (TelePoolConstant) object;
            final ClassActor classActorForType = telePoolConstant.classActorForObjectType();
            return objectReference(null, object, null, classActorForType.simpleName());
        }

        public String referenceToolTipText(MaxObject object) {
            final TelePoolConstant telePoolConstant = (TelePoolConstant) object;
            final ClassActor classActorForType = telePoolConstant.classActorForObjectType();
            return objectReference(null, object, null, "PoolConstant: " + classActorForType.qualifiedName());
        }
    }

    private class VmThreadReferenceRenderer extends AbstractReferenceRenderer {
        public String referenceLabelText(MaxObject object) {
            final TeleVmThread teleVmThread = (TeleVmThread) object;
            return objectReference(null, object, "VmThread", longName(teleVmThread.maxThread()));
        }

        public String referenceToolTipText(MaxObject object) {
            final TeleVmThread teleVmThread = (TeleVmThread) object;
            return objectReference(null, object, "VmThread", longName(teleVmThread.maxThread()));
        }
    }

}
