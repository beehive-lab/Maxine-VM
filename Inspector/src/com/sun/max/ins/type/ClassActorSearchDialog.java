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

import java.util.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.lang.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.type.*;
import com.sun.max.vm.type.*;

/**
 * A dialog to let the user select a class in the {@linkplain TeleClassRegistry inspector class registry}.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public final class ClassActorSearchDialog extends TeleObjectSearchDialog {

    @Override
    protected TeleObject convertSelectedItem(Object listItem) {
        final String name = (String) listItem;
        return teleVM().teleClassRegistry().findTeleClassActor(JavaTypeDescriptor.getDescriptorForJavaString(name));
    }

    /**
     * Determines if a given filter matches a given type descriptor. The matching ignores case and is performed against
     * the {@linkplain TypeDescriptor#toJavaString() Java class name} derived from the type descriptor.
     *
     * @param filter a filter consisting only of {@linkplain String#toLowerCase() lower case} characters
     * @param typeDescriptor the type descriptor to match {@code filter} against
     * @return the {@linkplain TypeDescriptor#toJavaString() Java class name} derived from {@code typeDescriptor} if
     *         there is a match, null otherwise
     */
    public static String match(String filter, TypeDescriptor typeDescriptor) {
        final String className = typeDescriptor.toJavaString();
        if (filter.indexOf('.') == -1) {
            final int length = filter.length();
            final String simpleClassName = typeDescriptor.toJavaString(false);
            if (!filter.isEmpty() &&  filter.charAt(length - 1) == ' ') {
                if (simpleClassName.equalsIgnoreCase(Strings.chopSuffix(filter, 1))) {
                    return className;
                }
            }
            if (simpleClassName.toLowerCase().contains(filter)) {
                return className;
            }
        }
        if (className.toLowerCase().contains(filter)) {
            return className;
        }
        return null;
    }

    /**
     * Rebuilds the list from scratch.
     */
    @Override
    protected void rebuildList(String filterText) {
        if (!filterText.isEmpty()) {
            final String filter = filterText.toLowerCase();
            final Set<TypeDescriptor> typeDescriptors = teleVM().teleClassRegistry().typeDescriptors();
            final SortedSet<String> classNames = new TreeSet<String>();
            for (TypeDescriptor typeDescriptor : typeDescriptors) {
                final String className = match(filter, typeDescriptor);
                if (className != null) {
                    classNames.add(className);
                }
            }
            for (String className : classNames) {
                _listModel.addElement(className);
            }
        }
    }

    private ClassActorSearchDialog(Inspection inspection) {
        super(inspection, "Select Class", "Class Name", false);
    }

    private ClassActorSearchDialog(Inspection inspection, String title, String actionName) {
        super(inspection, title == null ? "Select Class" : title, "Class Name", actionName, false);
    }

    /**
     * Displays a dialog to let the user select a class from the {@linkplain TeleClassRegistry inspector class registry}.
     *
     * @return the reference to the selected class actor or {@link TeleReference#ZERO} if the user canceled the dialog
     */
    public static TeleClassActor show(Inspection inspection) {
        final ClassActorSearchDialog dialog = new ClassActorSearchDialog(inspection);
        dialog.setVisible(true);
        return (TeleClassActor) dialog.selectedObject();
    }

    /**
     * Displays a dialog to let the user select a class from the {@linkplain TeleClassRegistry inspector class registry}.
     * @param inspection
     * @param title Title string for the dialog frame.
     * @param actionName Name of the action, appears on on the button to activate
     * @return
     */
    public static TeleClassActor show(Inspection inspection, String title, String actionName) {
        final ClassActorSearchDialog dialog = new ClassActorSearchDialog(inspection, title, actionName);
        dialog.setVisible(true);
        return (TeleClassActor) dialog.selectedObject();
    }


}
