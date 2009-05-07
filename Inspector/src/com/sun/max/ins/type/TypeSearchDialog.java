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
package com.sun.max.ins.type;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;
import com.sun.max.tele.type.*;
import com.sun.max.vm.type.*;

/**
 * A dialog for selecting a type available via the {@linkplain VM#loadableTypeDescriptors()} including those that may not have yet
 * been loaded into the {@linkplain TeleClassRegistry inspector class registry}.
 *
 * @author Doug Simon
 */
public final class TypeSearchDialog extends FilteredListDialog<TypeDescriptor> {

    @Override
    protected TypeDescriptor noSelectedObject() {
        return null;
    }

    @Override
    protected TypeDescriptor convertSelectedItem(Object listItem) {
        final String className = (String) listItem;
        return JavaTypeDescriptor.getDescriptorForWellFormedTupleName(className);
    }

    /**
     * Rebuilds the list from scratch.
     */
    @Override
    protected void rebuildList(String filterText) {
        if (!filterText.isEmpty()) {
            final String filter = filterText.toLowerCase();
            for (TypeDescriptor typeDescriptor : _types) {
                final String className = ClassActorSearchDialog.match(filter, typeDescriptor);
                if (className != null) {
                    _listModel.addElement(className);
                }
            }
        }
    }

    private TypeSearchDialog(Inspection inspection) {
        this(inspection, "Select Class", "Class Name");
    }

    private TypeSearchDialog(Inspection inspection, String title, String actionName) {
        super(inspection, title == null ? "Select Class" : title, "Class Name", actionName, false);
        _types = vm().loadableTypeDescriptors();
    }

    private final Iterable<TypeDescriptor> _types;

    /**
     * Displays a dialog for selecting a type available via the {@linkplain VM#loadableTypeDescriptors()} including those that may
     * not have yet been loaded into the {@linkplain TeleClassRegistry inspector class registry}.
     *
     * @return the type or null if the user canceled the dialog
     */
    public static TypeDescriptor show(Inspection inspection) {
        final TypeSearchDialog dialog = new TypeSearchDialog(inspection);
        dialog.setVisible(true);
        return dialog.selectedObject();
    }

    /**
     * Displays a dialog for selecting a type available via the {@linkplain VM#loadableTypeDescriptors()} including those that may
     * not have yet been loaded into the {@linkplain TeleClassRegistry inspector class registry}.
     *
     * @param inspection
     * @param title Title string for the dialog frame.
     * @param actionName Name of the action, appears on on the button to activate
     * @return the type or null if the user canceled the dialog
     */
    public static TypeDescriptor show(Inspection inspection, String title, String actionName) {
        final TypeSearchDialog dialog = new TypeSearchDialog(inspection, title, actionName);
        dialog.setVisible(true);
        return dialog.selectedObject();
    }
}
