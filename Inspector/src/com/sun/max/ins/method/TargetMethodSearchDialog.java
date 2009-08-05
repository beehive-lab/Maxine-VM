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
 * A dialog to let the user select a method actor that has target code in the tele VM defined by a given class actor.
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

        for (TeleClassMethodActor teleClassMethodActor : localTeleClassMethodActors) {
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
        Collections.sort(namedTeleTargetMethods);
        for (NamedTeleObject namedTeleObject : namedTeleTargetMethods) {
            listModel.addElement(namedTeleObject);
        }
    }

    private final AppendableSequence<TeleClassMethodActor> localTeleClassMethodActors = new LinkSequence<TeleClassMethodActor>();

    // TODO (mlvdv)  Include all compilations
    private TargetMethodSearchDialog(Inspection inspection, TeleClassActor teleClassActor, String title, String actionName, boolean multiSelection) {
        super(inspection, title == null ? "Select Target Method" : title, "Method Name", actionName, multiSelection);
        for (TeleClassMethodActor teleClassMethodActor : teleClassActor.getTeleClassMethodActors()) {
            if (teleClassMethodActor.getCurrentJavaTargetMethod() != null) {
                localTeleClassMethodActors.append(teleClassMethodActor);
            }
        }
        rebuildList();
    }

    /**
     * Displays a dialog to let the use select one or more compiled methods in the tele VM.
     *
     * @param teleClassActor a {@link ClassActor} in the tele VM.
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
