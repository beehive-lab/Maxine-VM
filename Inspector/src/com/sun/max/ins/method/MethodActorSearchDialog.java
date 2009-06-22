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
import java.util.Arrays;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.lang.*;
import com.sun.max.tele.object.*;
import com.sun.max.util.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;

/**
 * A dialog to let the user select a method from a specified {@link ClassActor} in the tele VM.
 *
 * @author Doug Simon
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class MethodActorSearchDialog extends TeleObjectSearchDialog {

    @Override
    protected TeleObject convertSelectedItem(Object listItem) {
        return ((NamedTeleObject) listItem).teleObject();
    }

    @Override
    protected void rebuildList(String filterText) {
        final Iterator<TeleMethodActor> teleMethodActors = _localTeleMethodActors.iterator();

        int i = 0;
        final NamedTeleObject[] methods = new NamedTeleObject[_localTeleMethodActors.length()];

        final String filterLowerCase = filterText.toLowerCase();
        while (teleMethodActors.hasNext()) {
            final TeleMethodActor teleMethodActor = teleMethodActors.next();
            final MethodActor methodActor = teleMethodActor.methodActor();
            final String methodNameLowerCase = methodActor.name.toString().toLowerCase();
            if (filterLowerCase.isEmpty() ||
                (filterLowerCase.endsWith(" ") && methodNameLowerCase.equals(Strings.chopSuffix(filterLowerCase, 1))) ||
                methodNameLowerCase.contains(filterLowerCase)) {
                final String signature = methodActor.name + methodActor.descriptor().toJavaString(false, true);
                methods[i++] = new NamedTeleObject(signature, teleMethodActor);
            }
        }

        Arrays.sort(methods, 0, i);
        for (int j = 0; j < i; ++j) {
            _listModel.addElement(methods[j]);
        }
    }

    private final AppendableSequence<TeleMethodActor> _localTeleMethodActors = new LinkSequence<TeleMethodActor>();

    private MethodActorSearchDialog(Inspection inspection, TeleClassActor teleClassActor, Predicate<TeleMethodActor> filter, String title, String actionName) {
        super(inspection, title == null ? "Select Method" : title, "Method Name", actionName, false);
        for (TeleMethodActor teleMethodActor : teleClassActor.getTeleMethodActors()) {
            if (filter.evaluate(teleMethodActor)) {
                _localTeleMethodActors.append(teleMethodActor);
            }
        }
        rebuildList();
    }

    /**
     * Displays a dialog to let the user select a reference to a method defined by a given class actor reference.
     *
     * @param filter only methods for which this holds are listed
     * @param title Title string for the dialog frame, "Select Method" if null
     * @param actionName Name of the action, appears on on the button to activate
     * @return local surrogate for the selected method actor or null if the user canceled the dialog
     */
    public static TeleMethodActor show(Inspection inspection, TeleClassActor teleClassActor, Predicate<TeleMethodActor> filter, String title, String actionName) {
        final MethodActorSearchDialog dialog = new MethodActorSearchDialog(inspection, teleClassActor, filter, title, actionName);
        dialog.setVisible(true);
        return (TeleMethodActor) dialog.selectedObject();
    }

    /**
     * Displays a dialog to let the user select a method defined by a given class actor.
     *
     * @param inspection
     * @param classActorReference
     * @param title
     *                title string for the dialog frame, "Select Method" if null
     * @param actionName
     *                name of the action, appears on on the button to activate
     * @return local surrogate for the selected method actor or null if the user canceled the dialog
     */
    public static TeleMethodActor show(Inspection inspection, TeleClassActor teleClassActor, String title, String actionName) {
        return show(inspection, teleClassActor, Predicate.Static.alwaysTrue(TeleMethodActor.class), title, actionName);
    }
}
