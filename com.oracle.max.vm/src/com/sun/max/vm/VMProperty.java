/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm;

import static com.sun.max.vm.MaxineVM.*;

import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * The set of "VM" properties, defined and used by the VM. This is a subset of the standard set of properties defined by
 * {@link java.lang.System#getProperties(), and is used by {@code JVMTI}. The set below is the same as reported by
 * HotSpot.
 *
 * Several properties are defined to be immutable, and all of those except {@code java.vm.info}, have values that are
 * computed once, at image build time. Values set as {@code null} here are filled in by the VM at startup, (but not early
 * enough to be strictly compliant with JVMTI). Access to the property names and values must be possible in primordial
 * mode. In addition a JVMTI agent can set a property in primordial mode and that value has to take effect in the VM
 * startup.
 *
 * JVMTI specifically does <b>not</b> require that the values initially set here track changes subsequently made by
 * {@link System#setProperty(String, String)}.
 *
 * We consistently use "Oracle" for vendor even though Hotspot still reports "Sun" for JDK 6.
 */
public enum VMProperty {
    // These are the "strongly recommended" properties
    JAVA_VM_VENDOR("java.vm.vendor", "Oracle Corporation", false),
    JAVA_VM_VERSION("java.vm.version", MaxineVM.VERSION_STRING, false),
    JAVA_VM_NAME("java.vm.name", MaxineVM.name(), false),
    JAVA_VM_INFO("java.vm.info", vm().compilationBroker.mode(), false), // updated by VM at runtime, immutable otherwise
    JAVA_LIBRARY_PATH("java.library.path", null, true),
    JAVA_CLASS_PATH("java.class.path", null, true),

    // These are the remainder supported by Hotspot
    JAVA_VM_SPECIFICATION_NAME("java.vm.specification.name", "Java Virtual Machine Specification", false),
    JAVA_VM_SPECIFICATION_VENDOR("java.vm.specification.vendor", "Oracle Corporation", false),
    JAVA_VM_SPECIFICATION_VERSION("java.vm.specification.version", "1.0", false),
    JAVA_EXT_DIRS("java.ext.dirs", null, true),
    JAVA_ENDORSED_DIRS("java.endorsed.dirs", null, true),
    JAVA_HOME("java.home", null, true),
    SUN_BOOT_LIBRARY_PATH("sun.boot.library.path", null, true),
    SUN_BOOT_CLASS_PATH("sun.boot.class.path", null, true),
    SUN_JAVA_COMMAND("sun.java.command", null, false),
    SUN_JAVA_LAUNCHER("sun.java.launcher", "SUN_STANDARD", false);

    private static final Offset BYTE_DATA_OFFSET = VMConfiguration.vmConfig().layoutScheme().byteArrayLayout.getElementOffsetFromOrigin(0);

    public final String property;
    private String value;
    public final boolean mutable;

    /**
     * Computed at boot image time to avoid the allocation that occurs in {@link String#getBytes}.
     * The cached values evidently depend on the boot heap not being relocated.
     */
    private byte[] propertyBytes;
    private byte[] valueBytes;

    /**
     * This value, if set, indicates that a JVMTI agent has set the value in primordial mode.
     * Evidently the property must be mutable.
     */
    private Pointer valueCString;

    private VMProperty(String property, String value, boolean mutable) {
        this.property = property;
        this.propertyBytes = property.getBytes();
        this.value = value;
        if (value != null) {
            valueBytes = value.getBytes();
        }
        this.mutable = mutable;
    }

    public static final VMProperty[] VALUES = values();

    /**
     * Set the value of this property, provided it is writeable.
     * @param value
     */
    public void setValue(String value) {
        if (mutable) {
            this.value = value;
            if (value != null) {
                valueBytes = value.getBytes();
            }
        }
    }

    /**
     * Update this property's value, even if not writable.
     * Used by the VM to update values that are not writeable by VMTI agents.
     * Only applicable to  {@java.vm.info} currently.
     * @param value
     */
    public void updateImmutableValue(String value) {
        assert value != null;
        this.value = value;
        valueBytes = value.getBytes();
    }

    /**
     * A way for a JVMTI agent to set the value before the VM is initialized.
     * This will override any calls to {@link setValue}.
     * @param value
     */
    public void setValue(Pointer value) {
        valueCString = value;
    }

    /**
     * Check if {@code} name is a {@linkplain VMProperty}.
     * @param name
     * @return the associated {@linkplain VMProperty} or {@code null} if no match.
     */
    public static VMProperty isVMProperty(String name) {
        for (int i = 0; i < VALUES.length; i++) {
            VMProperty vmProperty = VALUES[i];
            if (name.equals(vmProperty.property)) {
                return vmProperty;
            }
        }
        return null;
    }

    /**
     * Check if {@code} cstring is a {@linkplain VMProperty}.
     * @param name
     * @return the associated {@linkplain VMProperty} or {@code null} if no match.
     */
    public static VMProperty isVMProperty(Pointer cstring) {
        for (int i = 0; i < VALUES.length; i++) {
            VMProperty vmProperty = VALUES[i];
            if (CString.equals(cstring, vmProperty.property)) {
                return vmProperty;
            }
        }
        return null;
    }

    private static Pointer getByteArrayStart(byte[] data) {
        return Reference.fromJava(data).toOrigin().plus(BYTE_DATA_OFFSET);
    }

    /**
     * Return the property name as a C string.
     * @return
     */
    public Pointer propertyAsCString() {
        return getByteArrayStart(propertyBytes);
    }

    /**
     * Get the value of the property as a {@link String}.
     * @return
     */
    public String value() {
        if (valueCString.isNotZero()) {
            try {
                return CString.utf8ToJava(valueCString);
            } catch (Utf8Exception ex) {
                FatalError.unexpected("VMProperty.value", ex);
                return null;
            }
        } else {
            return value;
        }
    }

    /**
     * Get the value of the property as a C string.
     * @return
     */
    public Pointer valueAsCString() {
        if (valueCString.isNotZero()) {
            return valueCString;
        }
        if (value == null) {
            return Pointer.zero();
        } else {
            return getByteArrayStart(valueBytes);
        }
    }


}

