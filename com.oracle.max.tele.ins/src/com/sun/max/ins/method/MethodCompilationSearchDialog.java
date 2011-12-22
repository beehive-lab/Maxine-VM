/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.ins.InspectorNameDisplay.ReturnTypeSpecification;
import com.sun.max.ins.gui.*;
import com.sun.max.lang.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;

/**
 * A dialog to let the user select a method compilation. The dialog has two flavors; one that shows all the
 * method compilations currently in the VM and one that shows all the method compilations pertaining to the
 * declared methods of a specified class actor.
 */
public final class MethodCompilationSearchDialog extends FilteredListDialog<MaxCompilation> {

    /**
     * A tuple (method compilation name, method compilation).
     */
    private final class NamedMethodCompilation implements Comparable<NamedMethodCompilation> {

        private final String name;

        private final MaxCompilation compilation;

        public NamedMethodCompilation(String name, MaxCompilation compilation) {
            this.name = name;
            this.compilation = compilation;
        }

        public String name() {
            return name;
        }

        public MaxCompilation compilation() {
            return compilation;
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
    protected MaxCompilation convertSelectedItem(Object listItem) {
        return ((NamedMethodCompilation) listItem).compilation();
    }

    @Override
    protected void rebuildList(String filterText) {
        final String filterLowerCase = filterText.toLowerCase();
        final List<NamedMethodCompilation> namedMethodCompilations = new ArrayList<NamedMethodCompilation>();

        if (teleClassActor != null) {
            for (TeleClassMethodActor teleClassMethodActor : teleClassActor.getTeleClassMethodActors()) {
                if (teleClassMethodActor.getCurrentCompilation() != null) {
                    final MethodActor methodActor = teleClassMethodActor.methodActor();
                    final String methodNameLowerCase = methodActor.name.toString().toLowerCase();
                    if (filterLowerCase.isEmpty() ||
                                    (filterLowerCase.endsWith(" ") && methodNameLowerCase.equals(Strings.chopSuffix(filterLowerCase, 1))) ||
                                    methodNameLowerCase.contains(filterLowerCase)) {
                        for (MaxCompilation compilation : vm().machineCode().compilations(teleClassMethodActor)) {
                            final String name = inspection().nameDisplay().shortName(compilation, ReturnTypeSpecification.AS_SUFFIX);
                            namedMethodCompilations.add(new NamedMethodCompilation(name, compilation));
                        }
                    }
                }
            }
        } else {
            for (MaxCodeCacheRegion teleCompiledCodeRegion : inspection().vm().codeCache().codeCacheRegions()) {
                for (MaxCompilation compilation : teleCompiledCodeRegion.compilations()) {
                    ClassMethodActor methodActor = compilation.classMethodActor();
                    String methodCompilationType = Classes.getSimpleName(compilation.representation().classActorForObjectType().javaClass().getSimpleName());
                    if (methodActor != null) {
                        final String textToMatch = methodActor.format("%h.%n " + methodCompilationType).toLowerCase();
                        if (filterLowerCase.isEmpty() ||
                            (filterLowerCase.endsWith(" ") && textToMatch.equals(Strings.chopSuffix(filterLowerCase, 1))) ||
                             textToMatch.contains(filterLowerCase)) {
                            final String name = methodActor.format("%h.%n(%p)" + inspection().nameDisplay().shortMethodCompilationID(compilation) + "  " + methodCompilationType);
                            namedMethodCompilations.add(new NamedMethodCompilation(name, compilation));
                        }
                    } else {
                        String regionName = compilation.entityName();
                        if (filterLowerCase.isEmpty() || (regionName + " " + methodCompilationType).toLowerCase().contains(filterLowerCase)) {
                            namedMethodCompilations.add(new NamedMethodCompilation(regionName + " [" + methodCompilationType + "]", compilation));
                        }
                    }
                }
            }
        }

        Collections.sort(namedMethodCompilations);
        for (NamedMethodCompilation namedMethodCompilation : namedMethodCompilations) {
            listModel.addElement(namedMethodCompilation);
        }
    }

    private final TeleClassActor teleClassActor;

    @Override
    protected String filterFieldLabelTooltip() {
        return "Enter method name substring which can include a class name prefix. To filter only by method name, prefix filter text with '.'";
    }

    private MethodCompilationSearchDialog(Inspection inspection, TeleClassActor teleClassActor, String title, String actionName, boolean multiSelection) {
        super(inspection, title == null ? "Select Method Compilation" : title, "Filter text", actionName, multiSelection);
        this.teleClassActor = teleClassActor;
        rebuildList();
    }

    /**
     * Displays a dialog to let the use select one or more compiled methods in the VM.
     *
     * @param teleClassActor a {@link ClassActor} in the VM. If null, then all method compilations in the VM are presented for selection.
     * @param title for dialog window
     * @param actionName name to appear on button
     * @param multi allow multiple selections if true
     * @return references to the selected instances of {@link MaxCompilation} in the VM, null if user canceled.
     */
    public static List<MaxCompilation> show(Inspection inspection, TeleClassActor teleClassActor, String title, String actionName, boolean multi) {
        final MethodCompilationSearchDialog dialog = new MethodCompilationSearchDialog(inspection, teleClassActor, title, actionName, multi);
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
    protected MaxCompilation noSelectedObject() {
        return null;
    }

}
