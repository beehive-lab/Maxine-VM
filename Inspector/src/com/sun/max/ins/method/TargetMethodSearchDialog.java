/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.method;

import java.util.*;

import com.sun.max.ins.*;
import com.sun.max.ins.InspectorNameDisplay.*;
import com.sun.max.ins.gui.*;
import com.sun.max.lang.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;

/**
 * A dialog to let the user select a compiled method. The dialog has two flavors; one that shows all the
 * target methods currently in the VM and one that shows all the target methods pertaining to the
 * declared methods of a specified class actor.
 *
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public final class TargetMethodSearchDialog extends FilteredListDialog<MaxCompiledCode> {

    /**
     * A tuple (method compilation name, method compilation).
     */
    private final class NamedMethodCompilation implements Comparable<NamedMethodCompilation> {

        private final String name;

        private final MaxCompiledCode compiledCode;

        public NamedMethodCompilation(String name, MaxCompiledCode compiledCode) {
            this.name = name;
            this.compiledCode = compiledCode;
        }

        public String name() {
            return name;
        }

        public MaxCompiledCode compiledCode() {
            return compiledCode;
        }

        public int compareTo(NamedMethodCompilation o) {
            return name.compareTo(o.name);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Override
    protected MaxCompiledCode convertSelectedItem(Object listItem) {
        return ((NamedMethodCompilation) listItem).compiledCode();
    }

    @Override
    protected void rebuildList(String filterText) {
        final String filterLowerCase = filterText.toLowerCase();
        final List<NamedMethodCompilation> namedTeleTargetMethods = new ArrayList<NamedMethodCompilation>();

        if (teleClassActor != null) {
            for (TeleClassMethodActor teleClassMethodActor : teleClassActor.getTeleClassMethodActors()) {
                if (teleClassMethodActor.getCurrentCompilation() != null) {
                    final MethodActor methodActor = teleClassMethodActor.methodActor();
                    final String methodNameLowerCase = methodActor.name.toString().toLowerCase();
                    if (filterLowerCase.isEmpty() ||
                                    (filterLowerCase.endsWith(" ") && methodNameLowerCase.equals(Strings.chopSuffix(filterLowerCase, 1))) ||
                                    methodNameLowerCase.contains(filterLowerCase)) {
                        for (MaxCompiledCode compiledCode : vm().codeCache().compilations(teleClassMethodActor)) {
                            final String name = inspection().nameDisplay().shortName(compiledCode, ReturnTypeSpecification.AS_SUFFIX);
                            namedTeleTargetMethods.add(new NamedMethodCompilation(name, compiledCode));
                        }
                    }
                }
            }
        } else {
            for (MaxCompiledCodeRegion teleCompiledCodeRegion : inspection().vm().codeCache().compiledCodeRegions()) {
                for (MaxCompiledCode compiledCode : teleCompiledCodeRegion.compilations()) {
                    ClassMethodActor methodActor = compiledCode.classMethodActor();
                    String targetMethodType = Classes.getSimpleName(compiledCode.teleTargetMethod().getTeleHub().getTeleClassActor().getName());
                    if (methodActor != null) {
                        final String textToMatch = methodActor.format("%h.%n " + targetMethodType).toLowerCase();
                        if (filterLowerCase.isEmpty() ||
                            (filterLowerCase.endsWith(" ") && textToMatch.equals(Strings.chopSuffix(filterLowerCase, 1))) ||
                             textToMatch.contains(filterLowerCase)) {
                            final String name = methodActor.format("%h.%n(%p) [" + targetMethodType + "]");
                            namedTeleTargetMethods.add(new NamedMethodCompilation(name, compiledCode));
                        }
                    } else {
                        String regionName = compiledCode.entityName();
                        if (filterLowerCase.isEmpty() || (regionName + " " + targetMethodType).toLowerCase().contains(filterLowerCase)) {
                            namedTeleTargetMethods.add(new NamedMethodCompilation(regionName + " [" + targetMethodType + "]", compiledCode));
                        }
                    }
                }
            }
        }

        Collections.sort(namedTeleTargetMethods);
        for (NamedMethodCompilation namedMethodCompilation : namedTeleTargetMethods) {
            listModel.addElement(namedMethodCompilation);
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
    public static List<MaxCompiledCode> show(Inspection inspection, TeleClassActor teleClassActor, String title, String actionName, boolean multi) {
        final TargetMethodSearchDialog dialog = new TargetMethodSearchDialog(inspection, teleClassActor, title, actionName, multi);
        dialog.setVisible(true);
        return dialog.selectedObjects();
//        final Sequence<MaxMachineCode> teleObjects = dialog.selectedObjects();
//        if (teleObjects != null) {
//            final AppendableSequence<TeleTargetMethod> teleTargetMethods = new LinkedList<TeleTargetMethod>();
//            for (MaxMachineCode teleObject : teleObjects) {
//                final TeleTargetMethod teleTargetMethod = (TeleTargetMethod) teleObject;
//                teleTargetMethods.append(teleTargetMethod);
//            }
//            return teleTargetMethods;
//        }
//        return null;
    }

    @Override
    protected MaxCompiledCode noSelectedObject() {
        return null;
    }

}
