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
package com.sun.max.ins.method;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.InspectorNameDisplay.*;
import com.sun.max.ins.gui.*;
import com.sun.max.lang.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;

/**
 * A dialog to let the user select a target method. The dialog has two flavors; one that shows all the
 * target methods currently in the VM and one that shows all the target methods pertaining to the
 * declared methods of a specified class actor.
 *
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public final class TargetMethodSearchDialog extends TeleObjectSearchDialog {

    @Override
    protected TeleObject convertSelectedItem(Object listItem) {
        return ((NamedTeleObject) listItem).teleObject();
    }

    @Override
    protected void rebuildList(String filterText) {
        final String filterLowerCase = filterText.toLowerCase();
        final List<NamedTeleObject> namedTeleTargetMethods = new ArrayList<NamedTeleObject>();

        if (teleClassActor != null) {
            for (TeleClassMethodActor teleClassMethodActor : teleClassActor.getTeleClassMethodActors()) {
                if (teleClassMethodActor.getCurrentJavaTargetMethod() != null) {
                    final MethodActor methodActor = teleClassMethodActor.methodActor();
                    final String methodNameLowerCase = methodActor.name.toString().toLowerCase();
                    if (filterLowerCase.isEmpty() ||
                                    (filterLowerCase.endsWith(" ") && methodNameLowerCase.equals(Strings.chopSuffix(filterLowerCase, 1))) ||
                                    methodNameLowerCase.contains(filterLowerCase)) {
                        for (TeleTargetMethod teleTargetMethod : teleClassMethodActor.targetMethods()) {
                            final String name = inspection().nameDisplay().shortName(teleTargetMethod, ReturnTypeSpecification.AS_SUFFIX);
                            namedTeleTargetMethods.add(new NamedTeleObject(name, teleTargetMethod));
                        }
                    }
                }
            }
        } else {
            for (TeleCodeRegion teleCodeRegion : inspection().vm().teleCodeRegions()) {
                for (TeleTargetMethod teleTargetMethod : teleCodeRegion.teleTargetMethods()) {
                    ClassMethodActor methodActor = teleTargetMethod.classMethodActor();
                    String targetMethodType = Classes.getSimpleName(teleTargetMethod.getTeleHub().getTeleClassActor().getName());
                    if (methodActor != null) {
                        final String textToMatch = methodActor.format("%h.%n " + targetMethodType).toLowerCase();
                        if (filterLowerCase.isEmpty() ||
                            (filterLowerCase.endsWith(" ") && textToMatch.equals(Strings.chopSuffix(filterLowerCase, 1))) ||
                             textToMatch.contains(filterLowerCase)) {
                            final String name = methodActor.format("%h.%n(%p) [" + targetMethodType + "]");
                            namedTeleTargetMethods.add(new NamedTeleObject(name, teleTargetMethod));
                        }
                    } else {
                        String description = teleTargetMethod.description();
                        if (filterLowerCase.isEmpty() || (description + " " + targetMethodType).toLowerCase().contains(filterLowerCase)) {
                            namedTeleTargetMethods.add(new NamedTeleObject(description + " [" + targetMethodType + "]", teleTargetMethod));
                        }
                    }
                }
            }
        }

        Collections.sort(namedTeleTargetMethods);
        for (NamedTeleObject namedTeleObject : namedTeleTargetMethods) {
            listModel.addElement(namedTeleObject);
        }
    }

    private final TeleClassActor teleClassActor;

    @Override
    protected String filterFieldLabelTooltip() {
        return "Enter target method name substring which can include a class name prefix. To filter only by method name, prefix filter text with '.'";
    }

    private TargetMethodSearchDialog(Inspection inspection, TeleClassActor teleClassActor, String title, String actionName, boolean multiSelection) {
        super(inspection, title == null ? "Select Target Method" : title, "Filter text", actionName, multiSelection);
        this.teleClassActor = teleClassActor;
        rebuildList();
    }

    /**
     * Displays a dialog to let the use select one or more compiled methods in the tele VM.
     *
     * @param teleClassActor a {@link ClassActor} in the tele VM. If null, then all target methods in the VM are presented for selection.
     * @param title for dialog window
     * @param actionName name to appear on button
     * @param multi allow multiple selections if true
     * @return references to the selected {@link TargetMethod}s in the tele VM, null if user canceled.
     */
    public static Sequence<TeleTargetMethod> show(Inspection inspection, TeleClassActor teleClassActor, String title, String actionName, boolean multi) {
        final TargetMethodSearchDialog dialog = new TargetMethodSearchDialog(inspection, teleClassActor, title, actionName, multi);
        dialog.setVisible(true);
        final Sequence<TeleObject> teleObjects = dialog.selectedObjects();
        if (teleObjects != null) {
            final AppendableSequence<TeleTargetMethod> teleTargetMethods = new LinkSequence<TeleTargetMethod>();
            for (TeleObject teleObject : teleObjects) {
                final TeleTargetMethod teleTargetMethod = (TeleTargetMethod) teleObject;
                teleTargetMethods.append(teleTargetMethod);
            }
            return teleTargetMethods;
        }
        return null;
    }

}
