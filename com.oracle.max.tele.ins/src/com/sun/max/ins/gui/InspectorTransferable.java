/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.gui;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;

import javax.activation.*;

import com.sun.max.ins.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;

/**
 * A collection of classes for transferring Inspector objects during drag and drop.
 * <br>
 * This implementation presumes that each subclass supports exactly one {@link DataFlavor}.
 * @param <Object_Type> The type of object that can be transferred
 */
public abstract class InspectorTransferable<Object_Type> extends AbstractInspectionHolder implements Transferable {

    private static final int TRACE_VALUE = 1;

    private static final String VM_ADDRESS_MIME_TYPE = "VM address";
    public static final DataFlavor ADDRESS_FLAVOR =
        new ActivationDataFlavor(Address.class, DataFlavor.javaJVMLocalObjectMimeType, InspectorTransferable.VM_ADDRESS_MIME_TYPE);

    private static final String VM_MEMORY_REGION_MIME_TYPE = "VM memory region";
    public static final DataFlavor MEMORY_REGION_FLAVOR =
        new ActivationDataFlavor(MaxMemoryRegion.class, DataFlavor.javaJVMLocalObjectMimeType, InspectorTransferable.VM_MEMORY_REGION_MIME_TYPE);

    private static final String VM_TELE_OBJECT_MIME_TYPE = "VM object";
    public static final DataFlavor TELE_OBJECT_FLAVOR =
        new ActivationDataFlavor(TeleObject.class, DataFlavor.javaJVMLocalObjectMimeType, InspectorTransferable.VM_TELE_OBJECT_MIME_TYPE);

    private final Object_Type object;
    private final DataFlavor[] supportedDataFlavors;
    private final Cursor dragCursor;

    /**
     * @param inspection
     * @param object the object to be transferred
     * @param supportedDataFlavor the flavor of data (only one supported)
     * @param dragCursor the visual cursor to display while drag is over areas
     * where the drop can be accepted; null for default.
     */
    protected InspectorTransferable(Inspection inspection, Object_Type object, DataFlavor supportedDataFlavor, Cursor dragCursor) {
        super(inspection);
        this.object = object;
        this.supportedDataFlavors = new DataFlavor[] {supportedDataFlavor};
        this.dragCursor = dragCursor;
    }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (flavor == supportedDataFlavors[0]) {
            return object;
        }
        throw new UnsupportedFlavorException(flavor);
    }

    public DataFlavor[] getTransferDataFlavors() {
        return supportedDataFlavors;
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor == supportedDataFlavors[0];
    }

    /**
     * @return an alternate appearance for the cursor to display while dragging this kind of object; null for default.
     */
    public Cursor getDragCursor() {
        return dragCursor;
    }

    /**
     * Support for passing a VM memory address through drag and drop.
     *
     */
    public static final class AddressTransferable extends InspectorTransferable<Address> {

        public AddressTransferable(Inspection inspection, Address address) {
            super(inspection, address, InspectorTransferable.ADDRESS_FLAVOR, null);
        }
    }

    /**
     * Support for passing a VM memory address through drag and drop.
     *
     */
    public static final class MemoryRegionTransferable extends InspectorTransferable<MaxMemoryRegion> {

        public MemoryRegionTransferable(Inspection inspection, MaxMemoryRegion memoryRegion) {
            super(inspection, memoryRegion, InspectorTransferable.MEMORY_REGION_FLAVOR, null);
        }
    }

    /**
     * Support for passing an object in the VM through drag and drop.
     */
    public static final class TeleObjectTransferable extends InspectorTransferable<TeleObject> {

        public TeleObjectTransferable(Inspection inspection, TeleObject teleObject) {
            super(inspection, teleObject, InspectorTransferable.TELE_OBJECT_FLAVOR, null);
        }
    }
}
