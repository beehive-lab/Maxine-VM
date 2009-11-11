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
package com.sun.max.ins.gui;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;

import javax.activation.*;

import com.sun.max.ins.*;
import com.sun.max.memory.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;


/**
 * A collection of classes for transferring Inspector objects during drag and drop.
 * <br>
 * This implementation presumes that each subclass supports exactly one {@link DataFlavor}.
 *
 * @author Michael Van De Vanter
 * @param <Object_Type> The type of object that can be transferred
 */
public abstract class InspectorTransferable<Object_Type> extends AbstractInspectionHolder implements Transferable {

    private static final int TRACE_VALUE = 1;

    private static final String MAXINE_ADDRESS_MIME_TYPE = "Maxine VM address";
    public static final DataFlavor ADDRESS_FLAVOR =
        new ActivationDataFlavor(Address.class, DataFlavor.javaJVMLocalObjectMimeType, InspectorTransferable.MAXINE_ADDRESS_MIME_TYPE);

    private static final String MAXINE_MEMORY_REGION_MIME_TYPE = "Maxine VM memory region";
    public static final DataFlavor MEMORY_REGION_FLAVOR =
        new ActivationDataFlavor(MemoryRegion.class, DataFlavor.javaJVMLocalObjectMimeType, InspectorTransferable.MAXINE_MEMORY_REGION_MIME_TYPE);

    private static final String MAXINE_TELE_OBJECT_MIME_TYPE = "Maxine VM object";
    public static final DataFlavor TELE_OBJECT_FLAVOR =
        new ActivationDataFlavor(TeleObject.class, DataFlavor.javaJVMLocalObjectMimeType, InspectorTransferable.MAXINE_TELE_OBJECT_MIME_TYPE);

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
     * @author Michael Van De Vanter
     */
    public static final class AddressTransferable extends InspectorTransferable<Address> {

        public AddressTransferable(Inspection inspection, Address address) {
            super(inspection, address, InspectorTransferable.ADDRESS_FLAVOR, null);
        }
    }

    /**
     * Support for passing a VM memory address through drag and drop.
     *
     * @author Michael Van De Vanter
     */
    public static final class MemoryRegionTransferable extends InspectorTransferable<MemoryRegion> {

        public MemoryRegionTransferable(Inspection inspection, MemoryRegion memoryRegion) {
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
