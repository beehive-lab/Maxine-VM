/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins;

import java.io.*;
import java.lang.reflect.*;
import java.math.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import com.sun.cri.ci.*;
import com.sun.max.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.memory.*;
import com.sun.max.ins.method.*;
import com.sun.max.ins.object.*;
import com.sun.max.ins.type.*;
import com.sun.max.ins.util.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.MaxWatchpointManager.MaxDuplicateWatchpointException;
import com.sun.max.tele.MaxWatchpointManager.MaxTooManyWatchpointsException;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.layout.Layout.HeaderField;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Provider of {@link InspectorAction}s that are of general use.
 * <p>
 * <b>How to create an {@link InspectorAction} to perform "doSomething":</b>
 *
 * <ol>
 *
 * <li><b>Create an action class:</b>
 * <ul>
 * <li> {@code final class DoSomethingAction extends InspectorAction}</li>
 * <li> The Action classes are in package scope so that they can be used by {@link InspectorKeyBindings}.</li>
 * <li> Add a title:  {@code private static final DEFAULT_NAME = "do something"}.</li>
 * <li> If the
 * action is interactive, for example if it produces a dialog, then the name should conclude with "...".
 * Capitalize the first word of the title but not the others, except for distinguished names such as
 * "View" and acronyms.</li>
 * <li> For singletons, add a package scope constructor with one argument:  {@code String title}</li>
 * <li> For non-singletons, package scope constructor contains additional arguments that
 * customize the action, for example that specify to what "something" is to be done.</li>
 * <li> In the constructor: {@code super(inspection(), title == null ? DEFAULT_TITLE : title);}
 * (being able to override isn't used in many cases, but it adds flexibility).</li>
 * <li> If a singleton and if it contains state, for example enabled/disabled, that might change
 * depending on external circumstances, then register for general notification:
 * {@code _refreshableActions.append(this);} in the constructor.</li>
 * <li> Alternately, if state updates depend on a more specific kind of event, register
 * in the constructor explicitly for that event with a listener, for example
 * {@code focus().addListener(new InspectionFocusAdapter() { .... });}
 * The body of the listener should call {@code refresh}.</li>
 * <li>Override {@code protected void procedure()} with a method that does what
 * needs to be done.</li>
 * <li>If a singleton and if it contains state that might be changed depending on
 * external circumstances, override {@code public void refresh(boolean force)}
 * with a method that updates the state.</li>
 * </ul></li>
 *
 *<li><b>Create a singleton variable if needed</b>:
 *<ul>
 * <li>If the command is a singleton, create an initialized variable, static if possible.</li>
 * <li>{@code private static InspectorAction _doSomething = new DoSomethingAction(null);}</li>
 * </ul></li>
 *
 * <li><b>Create an accessor:</b>
 * <ul>
 * <li>Singleton: {@code public InspectorAction doSomething()}.</li>
 * <li> Singleton accessor returns the singleton variable.</li>
 * <li>Non-singletons have additional arguments that customize the action, e.g. specifying to what "something"
 * is to be done; they also take a {@code String title} argument that permits customization of the
 * action's name, for example when it appears in menus.</li>
 * <li> Non-singletons return {@code new DoSomethignAction(args, String title)}.</li>
 * <li>Add a descriptive Javadoc comment:  "@return an Action that does something".</li>
 * </ul></li>
 *
 * </ol>
 * <p>
 */
public class InspectionActions extends AbstractInspectionHolder implements Prober{

    private static final int TRACE_VALUE = 2;

    /**
     * Name of the Action for searching in a view.
     */
    public static final String SEARCH_ACTION = "Search";

    /**
     * Actions that are held and shared; they have state that will be refreshed.
     * This is particularly important for actions that enable/disable, depending on the inspection state.
     */
    private final List<InspectorAction> refreshableActions = new ArrayList<InspectorAction>();

    InspectionActions(Inspection inspection) {
        super(inspection);
        Trace.line(TRACE_VALUE, "InspectionActions initialized.");
    }

    public final void refresh(boolean force) {
        for (Prober prober : refreshableActions) {
            prober.refresh(force);
        }
    }

    public final void redisplay() {
        // non-op
    }

    /**
     * Action:  displays the {@link AboutSessionDialog}.
     */
    final class AboutSessionAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "About this session";

        AboutSessionAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            new AboutSessionDialog(inspection());
        }
    }

    /**
     * @return an Action that will display the {@link AboutSessionDialog}.
     */
    public final InspectorAction aboutSession(String title) {
        return new AboutSessionAction(title);
    }

    /**
     * @return an Action that doesn't do anything and is disabled.
     */
    public final InspectorAction inertAction(String actionTitle) {
        final InspectorAction action = new InspectorAction(inspection(), actionTitle) {

            @Override
            protected void procedure() {

            }
        };
        action.setEnabled(false);
        return action;
    }

    /**
     * Action:  displays the {@link AboutMaxineDialog}.
     */
    final class AboutMaxineAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "About Maxine";

        AboutMaxineAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            AboutMaxineDialog.create(inspection());
        }
    }

    /**
     * @return an Action that will display the {@link AboutMaxineDialog}.
     */
    public final InspectorAction aboutMaxine(String title) {
        return new AboutMaxineAction(title);
    }

    /**
     * Action:  displays the {@link PreferenceDialog}.
     */
    final class PreferencesAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Preferences";

        PreferencesAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            PreferenceDialog.create(inspection());
        }
    }

    private InspectorAction preferences = new PreferencesAction(null);

    /**
     * @return an Action that will display the {@link PreferenceDialog}.
     */
    public final InspectorAction preferences() {
        return preferences;
    }

    /**
     * Action:  refreshes all data from the VM.
     */
    final class RefreshAllAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Refresh all views";

        RefreshAllAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            inspection().refreshAll(true);
        }
    }

    private final InspectorAction refreshAll = new RefreshAllAction(null);

    /**
     * @return singleton Action that updates all displayed information read from the VM.
     */
    public final InspectorAction refreshAll() {
        return refreshAll;
    }

    /**
     * Action:  quits inspection session.
     */
    final class QuitAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Quit Inspector";

        QuitAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            inspection().quit();
        }
    }

    private final InspectorAction quitAction = new QuitAction(null);

    /**
     * @return Singleton Action that quits the VM inspection session.
     */
    public final InspectorAction quit() {
        return quitAction;
    }

    /**
     * Action:  relocates the boot image, assuming that the Inspector was invoked
     * with the option {@link MaxineInspector#suspendingBeforeRelocating()} set.
     */
    final class RelocateBootImageAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Relocate Boot Image";

        RelocateBootImageAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            try {
                vm().advanceToJavaEntryPoint();
            } catch (IOException ioException) {
                gui().errorMessage("error during relocation of boot image");
            }
            setEnabled(false);
        }
    }

    private final InspectorAction relocateBootImageAction = new RelocateBootImageAction(null);

    /**
     * @return Singleton Action that relocates the boot image, assuming that the Inspector was invoked
     * with the option {@link MaxineInspector#suspendingBeforeRelocating()} set.
     */
    public final InspectorAction relocateBootImage() {
        return relocateBootImageAction;
    }

    /**
     * Action:  sets level of trace output in Inspector code.
     */
    final class SetInspectorTraceLevelAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Set Inspector trace level...";

        SetInspectorTraceLevelAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            final int oldLevel = Trace.level();
            int newLevel = oldLevel;
            final String input = gui().inputDialog(DEFAULT_TITLE, Integer.toString(oldLevel));
            if (input == null) {
                // User clicked cancel.
                return;
            }
            try {
                newLevel = Integer.parseInt(input);
            } catch (NumberFormatException numberFormatException) {
                gui().errorMessage(numberFormatException.toString());
            }
            if (newLevel != oldLevel) {
                Trace.on(newLevel);
            }
        }
    }

    private final InspectorAction setInspectorTraceLevelAction = new SetInspectorTraceLevelAction(null);

    /**
     * @return Singleton interactive Action that permits setting the level of Inspector {@link Trace} output.
     */
    public final InspectorAction setInspectorTraceLevel() {
        return setInspectorTraceLevelAction;
    }

    /**
     * Action:  changes the threshold determining when the Inspectors uses its
     * {@linkplain InspectorInterpeter interpreter} for access to VM state.
     */
    final class ChangeInterpreterUseLevelAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Change Interpreter use level...";

        ChangeInterpreterUseLevelAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            final int oldLevel = vm().getInterpreterUseLevel();
            int newLevel = oldLevel;
            final String input = gui().inputDialog("Change interpreter use level (0=none, 1=some, etc)", Integer.toString(oldLevel));
            if (input == null) {
                // User clicked cancel.
                return;
            }
            try {
                newLevel = Integer.parseInt(input);
            } catch (NumberFormatException numberFormatException) {
                gui().errorMessage(numberFormatException.toString());
            }
            if (newLevel != oldLevel) {
                vm().setInterpreterUseLevel(newLevel);
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess());
        }
    }

    private final InspectorAction changeInterpreterUseLevelAction = new ChangeInterpreterUseLevelAction(null);

    /**
     * @return Singleton interactive action that permits changing the level at which the interpreter
     * will be used when communicating with the VM.
     */
    public final InspectorAction changeInterpreterUseLevel() {
        return changeInterpreterUseLevelAction;
    }

    /**
     * Action:  sets debugging level for transport.
     * Appears unused October '08 (mlvdv)
     */
    final class SetTransportDebugLevelAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Set transport debug level...";

        SetTransportDebugLevelAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            final int oldLevel = vm().transportDebugLevel();
            int newLevel = oldLevel;
            final String input = gui().inputDialog(" (Set transport debug level, 0=none, 1=some, etc)", Integer.toString(oldLevel));
            if (input == null) {
                // User clicked cancel.
                return;
            }
            try {
                newLevel = Integer.parseInt(input);
            } catch (NumberFormatException numberFormatException) {
                gui().errorMessage(numberFormatException.toString());
            }
            if (newLevel != oldLevel) {
                vm().setTransportDebugLevel(newLevel);
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess());
        }
    }

    private final InspectorAction setTransportDebugLevelAction = new SetTransportDebugLevelAction(null);

    /**
     * @return Singleton interactive action that permits setting the debugging level for transport.
     */
    public final InspectorAction setTransportDebugLevel() {
        return setTransportDebugLevelAction;
    }

    /**
     * Action: runs Inspector commands from a specified file.
     */
    final class RunFileCommandsAction extends InspectorAction {

        RunFileCommandsAction() {
            super(inspection(), "Execute commands from file...");
        }

        @Override
        protected void procedure() {
            final String fileName = gui().inputDialog("File name: ", FileCommands.defaultCommandFile());
            if (fileName != null && !fileName.equals("")) {
                vm().executeCommandsFromFile(fileName);
            }
        }
    }

    private final InspectorAction runFileCommandsAction = new RunFileCommandsAction();

    /**
     * @return Singleton interactive Action that will run Inspector commands from a specified file.
     */
    public final InspectorAction runFileCommands() {
        return runFileCommandsAction;
    }


    /**
     * Action:  updates the {@linkplain MaxVM#updateLoadableTypeDescriptorsFromClasspath() types available} on
     * the VM's class path by rescanning the complete class path for types.
     */
    final class UpdateClasspathTypesAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Rescan class path for types";

        UpdateClasspathTypesAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            vm().classes().updateLoadableTypeDescriptorsFromClasspath();
        }
    }

    private final InspectorAction updateClasspathTypesAction = new UpdateClasspathTypesAction(null);

    /**
     * @return Singleton Action that updates the {@linkplain MaxVM#updateLoadableTypeDescriptorsFromClasspath() types available} on
     * the VM's class path by rescanning the complete class path for types.
     */
    public final InspectorAction updateClasspathTypes() {
        return updateClasspathTypesAction;
    }

    /**
     * Action: sets the level of tracing in the VM interactively.
     */
    final class SetVMTraceLevelAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Set VM trace level";

        SetVMTraceLevelAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            final int oldLevel = vm().getVMTraceLevel();
            int newLevel = oldLevel;
            final String input = gui().inputDialog("Set VM Trace Level", Integer.toString(oldLevel));
            if (input == null) {
                // User clicked cancel.
                return;
            }
            try {
                newLevel = Integer.parseInt(input);
            } catch (NumberFormatException numberFormatException) {
                gui().errorMessage(numberFormatException.toString());
            }
            if (newLevel != oldLevel) {
                vm().setVMTraceLevel(newLevel);
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess());
        }
    }

    private InspectorAction setVMTraceLevel = new SetVMTraceLevelAction(null);

    /**
     * @return an interactive Action that will set the level of tracing in the VM.
     */
    public final InspectorAction setVMTraceLevel() {
        return setVMTraceLevel;
    }

    /**
     * Action: sets the threshold of tracing in the VM interactively.
     */
    final class SetVMTraceThresholdAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Set VM trace threshold";

        SetVMTraceThresholdAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            final long oldThreshold = vm().getVMTraceThreshold();
            long newThreshold = oldThreshold;
            final String input = gui().inputDialog("Set VM trace threshold", Long.toString(oldThreshold));
            if (input == null) {
                // User clicked cancel.
                return;
            }
            try {
                newThreshold = Long.parseLong(input);
            } catch (NumberFormatException numberFormatException) {
                gui().errorMessage(numberFormatException.toString());
            }
            if (newThreshold != oldThreshold) {
                vm().setVMTraceThreshold(newThreshold);
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess());
        }
    }

    private InspectorAction setVMTraceThreshold = new SetVMTraceThresholdAction(null);

    /**
     * @return an interactive Action that will set the threshold of tracing in the VM.
     */
    public final InspectorAction setVMTraceThreshold() {
        return setVMTraceThreshold;
    }

    /**
     * Action:  copies a hex string version of a {@link Word} to the system clipboard.
     */
    final class CopyWordAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Copy word to clipboard";
        private final Word word;

        private CopyWordAction(Word word, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.word = word;
        }

        @Override
        public void procedure() {
            gui().postToClipboard(word.toHexString());
        }
    }

    /**
     * @param a {@link Word} from the VM.
     * @param actionTitle a string to use as the title of the action, uses default name if null.
     * @return an Action that copies the word's text value in hex to the system clipboard
     */
    public final InspectorAction copyWord(Word word, String actionTitle) {
        return new CopyWordAction(word, actionTitle);
    }

    /**
     * Action:  copies a string version of a {@link Value} to the system clipboard.
     */
    final class CopyValueAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Copy value to clipboard";
        private final Value value;

        private CopyValueAction(Value value, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.value = value;
        }

        @Override
        public void procedure() {
            final Kind kind = value.kind();
            if (kind == Kind.REFERENCE || kind == Kind.WORD) {
                gui().postToClipboard(value.asWord().toHexString());
            } else {
                gui().postToClipboard(value.toString());
            }
        }
    }

    /**
     * @param a {@link Word} wrapped as a {@link Value} from the VM.
     * @param actionTitle a string to use as the title of the action, uses default name if null.
     * @return an Action that copies the word's text value in hex to the system clipboard,
     * null if not a word.
     */
    public final InspectorAction copyValue(Value value, String actionTitle) {
        return new CopyValueAction(value, actionTitle);
    }

    /**
     * Action:  copies a hex string version of a VM object's origin to the system clipboard.
     */
    final class CopyObjectOriginAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Copy object origin to clipboard";
        private final TeleObject teleObject;

        private CopyObjectOriginAction(TeleObject teleObject, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.teleObject = teleObject;
        }

        @Override
        public void procedure() {
            gui().postToClipboard(teleObject.origin().toHexString());
        }
    }

    /**
     * Creates an action that will copy a hex string version of a VM object's origin
     * to the system clipboard.
     *
     * @param teleObject a VM object
     * @param actionTitle optional title of the action
     * @return an action to copy the object's origin addreess
     */
    public final InspectorAction copyObjectOrigin(TeleObject teleObject, String actionTitle) {
        return new CopyObjectOriginAction(teleObject, actionTitle);
    }

    /**
     * Action:  copies a hex string version of a VM object's origin,
     * followed by a textual description of the object to the system clipboard.
     */
    final class CopyObjectDescriptionAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Copy object origin to clipboard";
        private final TeleObject teleObject;

        private CopyObjectDescriptionAction(TeleObject teleObject, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.teleObject = teleObject;
        }

        @Override
        public void procedure() {
            final StringBuilder sb = new StringBuilder(teleObject.origin().toHexString());
            sb.append(": ");
            sb.append(inspection().nameDisplay().referenceLabelText(teleObject));
            gui().postToClipboard(sb.toString());
        }
    }

    /**
     * Creates an action that will copy a hex string version of a VM object's origin,
     * followed by a textual description of the object to the system clipboard.
     *
     * @param teleObject the VM object to be described
     * @param actionTitle optional title of the action
     * @return an action to copy the description
     */
    public final InspectorAction copyObjectDescription(TeleObject teleObject, String actionTitle) {
        return new CopyObjectDescriptionAction(teleObject, actionTitle);
    }

    /**
     * Menu: display a sub-menu of commands to inspect the basic allocation
     * regions of the VM.
     */
    final class ViewMemoryAllocationsMenu extends JMenu {
        public ViewMemoryAllocationsMenu() {
            super("View Memory allocated for:");
            addMenuListener(new MenuListener() {

                public void menuCanceled(MenuEvent e) {
                }

                public void menuDeselected(MenuEvent e) {
                }

                public void menuSelected(MenuEvent e) {
                    removeAll();
                    final SortedSet<MaxMemoryRegion> regionSet = new TreeSet<MaxMemoryRegion>(MaxMemoryRegion.Util.nameComparator());
                    regionSet.addAll(vm().state().memoryAllocations());
                    for (MaxMemoryRegion memoryRegion : regionSet) {
                        //System.out.println(memoryRegion.toString());
                        add(views().memory().makeViewAction(memoryRegion, memoryRegion.regionName(), memoryRegion.regionName()));
                    }
                }
            });
        }
    }

    /**
     * Creates a menu of actions to view allocated memory regions.
     * <br>
     * <strong>Note:</strong> This menu does not depend on context, so it would be natural to use
     * a singleton to be shared among all uses.  Unfortunately, that does not seem to work.
     *
     * @return a dynamically populated menu that contains an action to view each currently allocated
     * region of memory in the VM.
     */
    public final JMenu viewMemoryAllocationsMenu() {
        return new ViewMemoryAllocationsMenu();
    }

    public final JMenu viewHeapRegionInfoMenu() {
        return HeapRegionInfoView.viewManager(inspection()).viewMenu();
    }
    /**
     * Action: view the HeapRegionInfo for the currently selected WordValueLabel when this one is in a mode denoting a heap address.
     */
    final class ViewSelectedAddressHeapRegionInfo extends InspectorAction {
        public ViewSelectedAddressHeapRegionInfo(String actionTitle) {
            super(inspection(), actionTitle == null ? "View heap region info for selected address" : actionTitle);
        }

        @Override
        protected void procedure() {
            MaxMemoryManagementInfo info = inspection().vm().getMemoryManagementInfo(focus().address());
            // TODO: revisit this.
            if (info.status().equals(MaxMemoryStatus.LIVE)) {
                final TeleObject teleObject = info.tele();
                focus().setHeapObject(teleObject);
            }
        }
    }
    /**
     * @param actionTitle title for the action, uses a default if null
     * @return an action that will create a heap region info view
     * for the memory location at the selected address
     */
    public final InspectorAction viewSelectedAddressHeapRegionInfo(String actionTitle) {
        return new ViewSelectedAddressHeapRegionInfo(actionTitle);
    }

    /**
     *Action:  view the memory allocated to the currently selected thread's stack.
     */
    final class ViewSelectedThreadStackMemoryAction extends InspectorAction {

        public ViewSelectedThreadStackMemoryAction(String actionTitle) {
            super(inspection(), actionTitle == null ? "View memory for selected thread's stack" : actionTitle);
        }

        @Override
        protected void procedure() {
            final MaxThread thread = focus().thread();
            if (thread != null) {
                views().memory().makeView(thread.stack().memoryRegion(), "Thread " + thread.toShortString()).highlight();
            } else {
                gui().errorMessage("no thread selected");
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasThread());
        }
    }

    /**
     * @param actionTitle title for the action, uses a default if null
     * @return an action that will create a memory view
     * for memory allocated by the currently selected stack frame
     */
    public final InspectorAction viewSelectedThreadStackMemory(String actionTitle) {
        return new ViewSelectedThreadStackMemoryAction(actionTitle);
    }

    /**
     *Action:  view the memory allocated to the currently selected thread's stack.
     */
    final class ViewSelectedThreadVMLogMemoryAction extends InspectorAction {

        public ViewSelectedThreadVMLogMemoryAction(String actionTitle) {
            super(inspection(), actionTitle == null ? "View memory for selected thread's VM log" : actionTitle);
        }

        @Override
        protected void procedure() {
            final MaxThread thread = focus().thread();
            if (thread != null) {
                MaxEntityMemoryRegion<MaxThreadVMLog> memoryRegion = thread.vmLog().memoryRegion();
                if (memoryRegion != null) {
                    views().memory().makeView(thread.vmLog().memoryRegion(), "Thread " + thread.toShortString()).highlight();
                } else {
                    gui().errorMessage("thread has no VM log buffer");
                }
            } else {
                gui().errorMessage("no thread selected");
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasThread());
        }
    }

    /**
     * @param actionTitle title for the action, uses a default if null
     * @return an action that will create a memory view
     * for memory allocated by the currently selected stack frame
     */
    public final InspectorAction viewSelectedThreadVMLogMemory(String actionTitle) {
        return new ViewSelectedThreadVMLogMemoryAction(actionTitle);
    }

    /**
     *Action:  view the memory allocated to the currently selected thread's locals block.
     */
    final class ViewSelectedThreadLocalsBlockMemoryAction extends InspectorAction {

        public ViewSelectedThreadLocalsBlockMemoryAction(String actionTitle) {
            super(inspection(), actionTitle == null ? "View memory for selected thread's locals block" : actionTitle);
        }

        @Override
        protected void procedure() {
            final MaxThread thread = focus().thread();
            if (thread != null) {
                views().memory().makeView(thread.localsBlock().memoryRegion(), "Thread locals block " + thread.toShortString()).highlight();
            } else {
                gui().errorMessage("no thread selected");
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasThread() && focus().thread().localsBlock().memoryRegion() != null);
        }
    }

    /**
     * @param actionTitle title for the action, uses a default if null
     * @return an action that will create a memory view
     * for the thread locals block allocated by the currently selected thread
     */
    public final InspectorAction viewSelectedThreadLocalsBlockMemory(String actionTitle) {
        return new ViewSelectedThreadLocalsBlockMemoryAction(actionTitle);
    }

    /**
     *Action:  view the memory allocated to one of the currently selected thread's locals areas.
     */
    final class ViewSelectedThreadLocalsAreaMemoryAction extends InspectorAction {

        private final SafepointPoll.State state;

        public ViewSelectedThreadLocalsAreaMemoryAction(SafepointPoll.State state, String actionTitle) {
            super(inspection(), actionTitle == null ? "View memory for selected thread's locals area=" + state.name() : actionTitle);
            this.state = state;
        }

        @Override
        protected void procedure() {
            final MaxMemoryRegion memoryRegion = getMemoryRegion();
            if (memoryRegion != null) {
                final String regionName = "Thread locals area " + state.name() + " for " + focus().thread().toShortString();
                views().memory().makeView(memoryRegion, regionName).highlight();
            } else {
                gui().errorMessage("no region found");
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(getMemoryRegion() != null);
        }

        private MaxMemoryRegion getMemoryRegion() {
            final MaxThread thread = focus().thread();
            if (thread != null) {
                final MaxThreadLocalsBlock localsBlock = thread.localsBlock();
                if (localsBlock != null) {
                    final MaxThreadLocalsArea tlaFor = localsBlock.tlaFor(state);
                    if (tlaFor != null) {
                        final MaxEntityMemoryRegion<MaxThreadLocalsArea> memoryRegion = tlaFor.memoryRegion();
                        return memoryRegion;
                    }
                }
            }
            return null;
        }
    }

    /**
     * @param actionTitle title for the action, uses a default if null
     * @return an action that will create a memory view
     * for one of the thread locals areas allocated by the currently selected thread
     */
    public final InspectorAction viewSelectedThreadLocalsAreaMemory(SafepointPoll.State state, String actionTitle) {
        return new ViewSelectedThreadLocalsAreaMemoryAction(state, actionTitle);
    }

    /**
     *Action:  view the memory allocated to the currently selected stack frame.
     */
    final class ViewSelectedStackFrameMemoryAction extends InspectorAction {

        public ViewSelectedStackFrameMemoryAction(String actionTitle) {
            super(inspection(), actionTitle == null ? "View memory for selected stack frame" : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            final MaxStackFrame stackFrame = focus().stackFrame();
            if (stackFrame != null) {
                views().memory().makeView(stackFrame.memoryRegion(), "Stack Frame " + stackFrame.entityName()).highlight();
            } else {
                gui().errorMessage("no stack frame selected");
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasStackFrame() && focus().stackFrame().memoryRegion() != null);
        }
    }

    /**
     * @param actionTitle title for the action, uses a default if null
     * @return an action that will create a memory view
     * for memory allocated by the currently selected stack frame
     */
    public final InspectorAction viewSelectedStackFrameMemory(String actionTitle) {
        return new ViewSelectedStackFrameMemoryAction(actionTitle);
    }

    /**
     *Action:  view the memory allocated to a stack frame.
     */
    final class ViewStackFrameMemoryAction extends InspectorAction {

        private final MaxStackFrame stackFrame;

        public ViewStackFrameMemoryAction(MaxStackFrame stackFrame, String actionTitle) {
            super(inspection(), actionTitle == null ? "View memory for stack frame" : actionTitle);
            assert stackFrame != null;
            this.stackFrame = stackFrame;
        }

        @Override
        protected void procedure() {
            if (stackFrame.memoryRegion() != null) {
                views().memory().makeView(stackFrame.memoryRegion(), "Stack Frame " + stackFrame.entityName()).highlight();
            } else {
                gui().errorMessage("stack frame " + stackFrame.entityName() + " null memory region");
            }
        }
    }

    /**
     * @param actionTitle title for the action, uses a default if null
     * @return an action that will create a memory view
     * for memory allocated by the currently selected stack frame
     */
    public final InspectorAction viewtackFrameMemory(MaxStackFrame stackFrame, String actionTitle) {
        return new ViewStackFrameMemoryAction(stackFrame, actionTitle);
    }

    /**
     *Action:  view the memory allocated to the currently selected memory watchpoint.
     */
    final class ViewSelectedMemoryWatchpointAction extends InspectorAction {

        public ViewSelectedMemoryWatchpointAction(String actionTitle) {
            super(inspection(), actionTitle == null ? "View memory at selected watchpoint" : actionTitle);
        }

        @Override
        protected void procedure() {
            final MaxWatchpoint watchpoint = focus().watchpoint();
            if (watchpoint != null) {
                views().memory().makeView(watchpoint.memoryRegion(), "Watchpoint " + watchpoint.description()).highlight();
            } else {
                gui().errorMessage("no watchpoint selected");
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasWatchpoint());
        }
    }

    private final InspectorAction viewSelectedMemoryWatchpointAction = new ViewSelectedMemoryWatchpointAction(null);

    /**
     * @return Singleton action that will create a memory view
     * for memory allocated by the currently selected thread
     */
    public final InspectorAction viewSelectedMemoryWatchpointAction() {
        return viewSelectedMemoryWatchpointAction;
    }

    /**
     * Action:  creates a memory view for the currently selected memory region, if any.
     */

    final class ViewSelectedMemoryRegionAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View selected memory region";

        ViewSelectedMemoryRegionAction() {
            super(inspection(), DEFAULT_TITLE);
            refreshableActions.add(this);
            refresh(true);
        }

        @Override
        protected void procedure() {
            final MaxMemoryRegion memoryRegion = focus().memoryRegion();
            if (memoryRegion != null) {
                views().memory().makeView(memoryRegion, memoryRegion.regionName()).highlight();
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasMemoryRegion());
        }
    }

    private final InspectorAction viewSelectedMemoryRegionAction = new ViewSelectedMemoryRegionAction();

    /**
     * @return Singleton Action that will create a Memory view for the currently selected region of memory
     */
    public final InspectorAction viewSelectedMemoryRegion() {
        return viewSelectedMemoryRegionAction;
    }

    /**
     * Action: sets inspection focus to specified {@link MaxMemoryRegion}.
     */
    final class SelectMemoryRegionAction extends InspectorAction {

        private final MaxMemoryRegion memoryRegion;
        private static final String DEFAULT_TITLE = "Select memory region";

        SelectMemoryRegionAction(MaxMemoryRegion memoryRegion, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.memoryRegion = memoryRegion;
        }

        @Override
        protected void procedure() {
            focus().setMemoryRegion(memoryRegion);
        }
    }

    /**
     * @return an Action that will create a Memory Inspector at the start of the boot code
     */
    public final InspectorAction selectMemoryRegion(MaxMemoryRegion memoryRegion) {
        final String actionTitle = "Select memory region \"" + memoryRegion.regionName() + "\"";
        return new SelectMemoryRegionAction(memoryRegion, actionTitle);
    }

    /**
     * Action: create an Object view for the boot {@link ClassRegistry} in the VM.
     */
    final class ViewBootClassRegistryAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View boot class registry";

        ViewBootClassRegistryAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            try {
                final TeleObject teleBootClassRegistry = vm().objects().findTeleObject(vm().bootClassRegistryReference());
                focus().setHeapObject(teleBootClassRegistry);
            } catch (MaxVMBusyException maxVMBusyException) {
                inspection().announceVMBusyFailure(name());
            }
        }
    }

    private final InspectorAction viewBootClassRegistryAction = new ViewBootClassRegistryAction(null);

    /**
     * @return Singleton action that will create an Object view for the boot {@link ClassRegistry} in the VM.
     */
    public final InspectorAction viewBootClassRegistry() {
        return viewBootClassRegistryAction;
    }

    /**
     * Action:  view a {@link ClassActor} object for an interactively named class loaded in the VM,
     * specified by class name.
     */
    final class ViewClassActorByNameAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View ClassActor by name...";

        ViewClassActorByNameAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(inspection(), "View ClassActor ...", "View");
            if (teleClassActor != null) {
                focus().setHeapObject(teleClassActor);
            }
        }
    }

    private final InspectorAction viewClassActorByNameAction = new ViewClassActorByNameAction(null);

    /**
     * @return Singleton interactive Action that views a {@link ClassActor} object for a class loaded in the VM,
     * specified by class name.
     */
    public final InspectorAction viewClassActorByName() {
        return viewClassActorByNameAction;
    }

    /**
     * Action:  view a {@link ClassActor} for an interactively named class loaded in the VM,
     * specified by the class ID in hex.
     */
    final class ViewClassActorByHexIdAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View ClassActor by ID (Hex) ...";

        ViewClassActorByHexIdAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            final String value = gui().questionMessage("ID (hex): ");
            if (value != null && !value.equals("")) {
                try {
                    final int serial = Integer.parseInt(value, 16);
                    final TeleClassActor teleClassActor = vm().classes().findTeleClassActor(serial);
                    if (teleClassActor == null) {
                        gui().errorMessage("failed to find classActor for ID:  " + InspectorLabel.intTo0xHex(serial));
                    } else {
                        focus().setHeapObject(teleClassActor);
                    }
                } catch (NumberFormatException ex) {
                    gui().errorMessage("Hex integer required");
                }
            }
        }
    }

    private final InspectorAction viewClassActorByHexIdAction = new ViewClassActorByHexIdAction(null);

    /**
     * @return Singleton interactive Action that views a {@link ClassActor} object for a class loaded in the VM,
     * specified by class ID in hex.
     */
    public final InspectorAction viewClassActorByHexId() {
        return viewClassActorByHexIdAction;
    }

    /**
     * Action:  view a {@link ClassActor} for an interactively named class loaded in the VM,
     * specified by the class ID in decimal.
     */
    final class ViewClassActorByDecimalIdAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View ClassActor by ID (decimal) ...";
        ViewClassActorByDecimalIdAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            final String value = gui().questionMessage("ID (decimal): ");
            if (value != null && !value.equals("")) {
                try {
                    final int serial = Integer.parseInt(value, 10);
                    final TeleClassActor teleClassActor = vm().classes().findTeleClassActor(serial);
                    if (teleClassActor == null) {
                        gui().errorMessage("failed to find ClassActor for ID: " + serial);
                    } else {
                        focus().setHeapObject(teleClassActor);
                    }
                } catch (NumberFormatException ex) {
                    gui().errorMessage("Hex integer required");
                }
            }
        }
    }

    private final InspectorAction viewClassActorByDecimalIdAction = new ViewClassActorByDecimalIdAction(null);

    /**
     * @return Singleton interactive Action that views a {@link ClassActor} object for a class loaded in the VM,
     * specified by class ID in decimal.
     */
    public final InspectorAction viewClassActorByDecimalId() {
        return viewClassActorByDecimalIdAction;
    }

    /**
     * Action:  view the {@link StaticTuple} object for an interactively named class loaded in the VM,
     * specified by class name.
     */
    final class ViewStaticTupleByNameAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View StaticTuple by class name...";

        ViewStaticTupleByNameAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(inspection(), "View StaticTuple for class ...", "View");
            if (teleClassActor != null) {
                TeleStaticTuple staticTuple = teleClassActor.getTeleStaticTuple();
                if (staticTuple == null) {
                    gui().errorMessage("StaticTuple for class not available");
                } else {
                    focus().setHeapObject(staticTuple);
                }
            }
        }
    }

    private final InspectorAction viewStaticTupleByNameAction = new ViewStaticTupleByNameAction(null);

    /**
     * @return Singleton interactive Action that views the {@link StaticTuple} object for a class loaded in the VM,
     * specified by class name.
     */
    public final InspectorAction viewStaticTupleByName() {
        return viewStaticTupleByNameAction;
    }

    /**
     * Action:  view the {@link DynamicHub} object for an interactively named class loaded in the VM,
     * specified by class name.
     */
    final class ViewDynamicHubByNameAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View DynamicHub by class name...";

        ViewDynamicHubByNameAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(inspection(), "View DynamicHub for class ...", "View");
            if (teleClassActor != null) {
                final TeleDynamicHub dynamicHub = teleClassActor.getTeleDynamicHub();
                if (dynamicHub == null) {
                    gui().errorMessage("DynamicHub for class not available");
                } else {
                    focus().setHeapObject(dynamicHub);
                }
            }
        }
    }

    private final InspectorAction viewDynamicHubByNameAction = new ViewDynamicHubByNameAction(null);

    /**
     * @return Singleton interactive Action that views a {@link DynamicHub} object for a class loaded in the VM,
     * specified by class name.
     */
    public final InspectorAction viewDynamicHubByName() {
        return viewDynamicHubByNameAction;
    }

    /**
     * Action:  view the {@link StaticHub} object for an interactively named class loaded in the VM,
     * specified by class name.
     */
    final class ViewStaticHubByNameAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View StaticHub by class name...";

        ViewStaticHubByNameAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(inspection(), "View StaticHub for class ...", "View");
            if (teleClassActor != null) {
                final TeleStaticHub staticicHub = teleClassActor.getTeleStaticHub();
                if (staticicHub == null) {
                    gui().errorMessage("StaticHub for class not available");
                } else {
                    focus().setHeapObject(staticicHub);
                }
            }
        }
    }

    private final InspectorAction viewStaticHubByNameAction = new ViewStaticHubByNameAction(null);

    /**
     * @return Singleton interactive Action that views the {@link StaticHub} object for a class loaded in the VM,
     * specified by class name.
     */
    public final InspectorAction viewStaticHubByName() {
        return viewStaticHubByNameAction;
    }

    /**
     * Action: view a {@link MethodActor} object in the VM, specified by name.
     */
    final class ViewMethodActorByNameAction extends InspectorAction {

        private static final String DEFAULT_TITLE =  "View MethodActor by name...";

        ViewMethodActorByNameAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(inspection(), "View MethodActor in class...", "Select");
            if (teleClassActor != null) {
                final TeleMethodActor teleMethodActor = MethodActorSearchDialog.show(inspection(), teleClassActor, "View MethodActor...", "View");
                if (teleMethodActor != null) {
                    focus().setHeapObject(teleMethodActor);
                }
            }
        }
    }

    private InspectorAction viewMethodActorByName = new ViewMethodActorByNameAction(null);

    /**
     * @return an interactive Action that will view a {@link MethodActor} object in the VM, specified by name.
     */
    public final InspectorAction viewMethodActorByName() {
        return viewMethodActorByName;
    }

    /**
     * Action:  view the class actor from which a method was substituted.
     */
    final class ViewSubstitutionSourceClassActorAction extends InspectorAction {

        private static final String DEFAULT_TITLE =  "View method substitution source";

        private final TeleClassMethodActor teleClassMethodActor;

        private ViewSubstitutionSourceClassActorAction(TeleClassMethodActor teleClassMethodActor, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.teleClassMethodActor = teleClassMethodActor;
            setEnabled(teleClassMethodActor.isSubstituted());
        }

        @Override
        public void procedure() {
            if (teleClassMethodActor != null) {
                focus().setHeapObject(teleClassMethodActor.teleClassActorSubstitutedFrom());
            }
        }
    }

    /**
     * Creates an action to view the class actor from which a method was substituted.
     *
     * @param teleClassMethodActor representation of a class method in the VM
     * @param actionTitle name of the action
     * @return an action that will view the class actor, if any, from which the method was substituted
     */
    public final InspectorAction viewSubstitutionSourceClassActorAction(TeleClassMethodActor teleClassMethodActor, String actionTitle) {
        return new ViewSubstitutionSourceClassActorAction(teleClassMethodActor, actionTitle);
    }

    /**
     * Creates an action to view the class actor from which a method was substituted.
     *
     * @param teleClassMethodActor representation of a class method in the VM
     * @return an action that will view the class actor, if any, from which the method was substituted
     */
    public final InspectorAction viewSubstitutionSourceClassActorAction(TeleClassMethodActor teleClassMethodActor) {
        return new ViewSubstitutionSourceClassActorAction(teleClassMethodActor, null);
    }

    /**
     * Menu: contains actions to view each of the compilations of a method.
     */
    final class ViewMethodCompilationsMenu extends InspectorMenu {

        private static final String DEFAULT_TITLE = "Compilations";
        private final TeleClassMethodActor teleClassMethodActor;

        public ViewMethodCompilationsMenu(TeleClassMethodActor teleClassMethodActor, String actionTitle) {
            super(actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.teleClassMethodActor = teleClassMethodActor;
            refresh(true);
        }

        @Override
        public void refresh(boolean force) {
            final List<MaxCompilation> compilations = vm().machineCode().compilations(teleClassMethodActor);
            if (getMenuComponentCount() < compilations.size()) {
                for (int index = getMenuComponentCount(); index < compilations.size(); index++) {
                    final MaxCompilation compilation = compilations.get(index);
                    final String name = inspection().nameDisplay().shortName(compilation);
                    add(views().objects().makeViewAction(compilation.representation(), name.toString()));
                }
            }
        }
    }

    /**
     * Creates a menu containing actions to view all compilations of a method, dynamically updated
     * as compilations are added.
     *
     * @param teleClassMethodActor representation of a Java method in the VM
     * @param actionTitle name of the action to appear on button or menu
     * @return a dynamically refreshed menu that contains actions to view each of the compilations of a method.
     */
    public InspectorMenu inspectMethodCompilationsMenu(TeleClassMethodActor teleClassMethodActor, String actionTitle) {
        return new ViewMethodCompilationsMenu(teleClassMethodActor, actionTitle);
    }

    /**
     * Creates a menu containing actions to view all compilations of a method, dynamically updated
     * as compilations are added.
     *
     * @param teleClassMethodActor representation of a Java method in the VM
     * @return a dynamically refreshed menu that contains actions to view each of the compilations of a method.
     */
    public InspectorMenu viewMethodCompilationsMenu(TeleClassMethodActor teleClassMethodActor) {
        return new ViewMethodCompilationsMenu(teleClassMethodActor, null);
    }

    /**
     * Action:  displays Java source for a specified method.
     */
    final class ViewJavaSourceAction extends InspectorAction {

        private final TeleClassMethodActor teleClassMethodActor;

        public ViewJavaSourceAction(TeleClassMethodActor teleClassMethodActor) {
            super(inspection(), "View Java Source (external)");
            this.teleClassMethodActor = teleClassMethodActor;
        }

        @Override
        public void procedure() {
            inspection().viewSourceExternally(new CiCodePos(null, teleClassMethodActor.classMethodActor(), 0));
        }
    }

    /**
     * Creates an action that will produce an external view of method source code.
     *
     * @param teleClassMethodActor surrogate of a Java method in the VM.
     * @return an action that creates an external of the Java source for the method.
     */
    public InspectorAction viewJavaSource(TeleClassMethodActor teleClassMethodActor) {
        return new ViewJavaSourceAction(teleClassMethodActor);
    }

    /**
     * Action:  displays in the {@MethodInspector} the method or native function whose machine code contains
     * an interactively specified address.
     */
    final class ViewMachineCodeByAddressAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View code by address...";

        public ViewMachineCodeByAddressAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            // Most likely situation is that we are just about to call a function method in which case RAX is the address
            Address defaultAddress = Address.zero();
            final MaxThread thread = focus().thread();
            if (thread != null) {
                final Address registerValue = thread.registers().getCallRegisterValue();
                if (vm().machineCode().findMachineCode(registerValue) != null) {
                    defaultAddress = registerValue;
                }
            }
            new AddressInputDialog(inspection(), defaultAddress, "View machine code containing address...", "View Code") {

                @Override
                protected String validateInput(Address address) {
                    if (address.isZero()) {
                        return "Zero is not a valid code address";
                    }
                    final MaxEntityMemoryRegion<? extends MaxEntity> memoryRegion = vm().state().findMemoryRegion(address);
                    if (!(memoryRegion.owner() instanceof MaxCodeHoldingRegion<?>)) {
                        return address.to0xHexString() + " points into region \"" + memoryRegion.regionName() + "\"";
                    }
                    return null;
                }

                @Override
                public void entered(Address address) {
                    if (vm().machineCode().findMachineCode(address) == null) {
                        final String message = "Address " + address.to0xHexString() + " not in any known compilation or native function, try anyway?";
                        if (!gui().yesNoDialog(message)) {
                            return;
                        }
                    }
                    try {
                        focus().setCodeLocation(vm().codeLocations().createMachineCodeLocation(address, "user specified address"));
                    } catch (InvalidCodeAddressException e) {
                        gui().errorMessage("Unable to view, no code @ " + e.getAddressString() + ":  " + e.getMessage());
                    }
                }
            };
        }
    }

    private final InspectorAction viewMachineCodeByAddressAction = new ViewMachineCodeByAddressAction(null);

    /**
     * @return Singleton interactive action that displays in the {@link MethodView} the method whose
     * machine code contains the specified address in the VM.
     */
    public final InspectorAction viewMachineCodeByAddress() {
        return viewMachineCodeByAddressAction;
    }

    /**
     * Action:  displays the method code containing an address.
     */
    final class ViewMethodCodeAtLocationAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View code at a location";
        private final MaxCodeLocation codeLocation;

        public ViewMethodCodeAtLocationAction(MaxCodeLocation codeLocation, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            assert codeLocation != null;
            this.codeLocation = codeLocation;
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            focus().setCodeLocation(codeLocation, true);
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasThread());
        }
    }

    /**
     * @return an Action that displays in the {@link MethodView} a method at some code location.
     */
    public final InspectorAction viewMethodCodeAtLocation(MaxCodeLocation codeLocation, String actionTitle) {
        return new ViewMethodCodeAtLocationAction(codeLocation, actionTitle);
    }

    /**
     * Action:  displays the method code containing the current code selection.
     */
    final class ViewMethodCodeAtSelectionAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View code at current selection";
        public ViewMethodCodeAtSelectionAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            focus().setCodeLocation(focus().codeLocation(), true);
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasCodeLocation());
        }
    }

    private final ViewMethodCodeAtSelectionAction viewMethodCodeAtSelection = new ViewMethodCodeAtSelectionAction(null);

    /**
     * @return Singleton action that displays in the {@link MethodView} the method code
     * containing the current code selection.
     */
    public final InspectorAction viewMethodCodeAtSelection() {
        return viewMethodCodeAtSelection;
    }

    /**
     * Action:  displays the method code containing the current instruction pointer.
     */
    final class ViewMethodCodeAtIPAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View code at current IP";
        public ViewMethodCodeAtIPAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            focus().setCodeLocation(focus().thread().ipLocation());
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasThread());
        }
    }

    private final ViewMethodCodeAtIPAction viewMethodCodeAtIP = new ViewMethodCodeAtIPAction(null);

    /**
     * @return Singleton Action that displays in the {@link MethodView} the method
     * containing the current instruction pointer.
     */
    public final InspectorAction viewMethodCodeAtIP() {
        return viewMethodCodeAtIP;
    }

    /**
     * Action:  displays the bytecode for a specified method.
     */
    final class ViewMethodBytecodeAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View bytecode...";
        private final TeleClassMethodActor teleClassMethodActor;

        public ViewMethodBytecodeAction(TeleClassMethodActor teleClassMethodActor, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.teleClassMethodActor = teleClassMethodActor;
            setEnabled(teleClassMethodActor.hasCodeAttribute());
        }

        @Override
        protected void procedure() {
            final MaxCodeLocation teleCodeLocation = vm().codeLocations().createBytecodeLocation(teleClassMethodActor, 0, "view method bytecode action");
            focus().setCodeLocation(teleCodeLocation);
        }
    }

    /**
     * @return an interactive Action that displays bytecode in the {@link MethodView}
     * for a selected method.
     */
    public final InspectorAction viewMethodBytecode(TeleClassMethodActor teleClassMethodActor) {
        return new ViewMethodBytecodeAction(teleClassMethodActor, null);
    }

    /**
     * Creates an action to view the bytecodes (if they exist) for a Java method.
     *
     * @param teleClassMethodActor
     * @param actionTitle name of the action to appear in menu or button
     * @return an interactive Action that displays bytecode in the {@link MethodView}
     * for a selected method.
     */
    public final InspectorAction viewMethodBytecode(TeleClassMethodActor teleClassMethodActor, String actionTitle) {
        return new ViewMethodBytecodeAction(teleClassMethodActor, actionTitle);
    }

    /**
     * Action:  displays the bytecode for an interactively specified method.
     */
    final class ViewMethodBytecodeByNameAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Bytecode...";

        public ViewMethodBytecodeByNameAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(inspection(), "View bytecode for method in class...", "Select");
            if (teleClassActor != null) {
                final Predicate<TeleMethodActor> hasBytecodePredicate = new Predicate<TeleMethodActor>() {
                    public boolean evaluate(TeleMethodActor teleMethodActor) {
                        return teleMethodActor.hasCodeAttribute();
                    }
                };
                final TeleMethodActor teleMethodActor = MethodActorSearchDialog.show(inspection(), teleClassActor, hasBytecodePredicate, "View Bytecodes for Method...", "View");
                if (teleMethodActor != null && teleMethodActor instanceof TeleClassMethodActor) {
                    final TeleClassMethodActor teleClassMethodActor = (TeleClassMethodActor) teleMethodActor;
                    final MaxCodeLocation teleCodeLocation = vm().codeLocations().createBytecodeLocation(teleClassMethodActor, 0, "view method by name bytecode action");
                    focus().setCodeLocation(teleCodeLocation);
                }
            }
        }
    }

    private final InspectorAction viewMethodBytecodeByNameAction = new  ViewMethodBytecodeByNameAction(null);

    /**
     * @return an interactive Action that displays bytecode in the {@link MethodView}
     * for a selected method.
     */
    public final InspectorAction viewMethodBytecodeByName() {
        return viewMethodBytecodeByNameAction;
    }

    /**
     * @param actionTitle name of the action to appear in menu or button
     * @return an interactive Action that displays bytecode in the {@link MethodView}
     * for a selected method.
     */
    public final InspectorAction viewMethodBytecodeByName(String actionTitle) {
        return new ViewMethodBytecodeByNameAction(actionTitle);
    }

    /**
     * Action:  displays the machine code for an interactively specified method.
     */
    final class ViewMethodCompilationByNameAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Method compilation...";

        public ViewMethodCompilationByNameAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(inspection(), "View compiled code for method in class...", "Select");
            if (teleClassActor != null) {
                final List<MaxCompilation> compilations =
                    MethodCompilationSearchDialog.show(inspection(), teleClassActor, "View Compiled Code for Method...", "View Code", false);
                if (compilations != null) {
                    focus().setCodeLocation(Utils.first(compilations).getCallEntryLocation());
                }
            }
        }
    }

    private final InspectorAction viewMethodCompilationByNameAction = new ViewMethodCompilationByNameAction(null);

    /**
     * @return Singleton interactive Action that displays machine code in the {@link MethodView}
     * for a selected method.
     */
    public final InspectorAction viewMethodCompilationByName() {
        return viewMethodCompilationByNameAction;
    }

    /**
     * Action:  displays the machine code for an interactively specified method.
     */
    final class ViewNativeFunctionByNameAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Native function...";

        public ViewNativeFunctionByNameAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            final MaxNativeLibrary nativeLibrary = NativeLibrarySearchDialog.show(inspection(), "View code for native function in library...", "Select");
            if (nativeLibrary != null) {
                final List<MaxNativeFunction> functions = NativeFunctionSearchDialog.show(inspection(), nativeLibrary, "View Native Function...", "View Code", false);
                if (functions != null) {
                    try {
                        focus().setCodeLocation(vm().codeLocations().createMachineCodeLocation(Utils.first(functions).getCodeStart(), "native function address from library"), true);
                    } catch (InvalidCodeAddressException e) {
                        gui().errorMessage("Unable to view code, no code @ " + e.getAddressString() + ":  " + e.getMessage());
                    }
                }
            }
        }
    }

    private final InspectorAction viewNativeFunctionByNameAction = new ViewNativeFunctionByNameAction(null);

    /**
     * @return Singleton interactive Action that displays machine code in the {@link MethodView}
     * for a selected method.
     */
    public final InspectorAction viewNativeFunctionByName() {
        return viewNativeFunctionByNameAction;
    }

   /**
     * Action:  displays the compiled code for an interactively specified method.
     */
    final class ViewMethodMachineCodeAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View compiled code...";

        public ViewMethodMachineCodeAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            final List<MaxCompilation> compilations =
                MethodCompilationSearchDialog.show(inspection(), null, "View compiled code for method...", "View Code", false);
            if (compilations != null) {
                focus().setCodeLocation(Utils.first(compilations).getCallEntryLocation(), false);
            }
        }
    }

    private final InspectorAction viewMethodMachineCodeAction = new ViewMethodMachineCodeAction(null);

    /**
     * @return Singleton interactive Action that displays machine code in the {@link MethodView}
     * for a selected method.
     */
    public final InspectorAction viewMethodMachineCode() {
        return viewMethodMachineCodeAction;
    }

    /**
     * Menu: contains actions to view code for each of the compilations of a method.
     */
    final class ViewMethodCompilationsCodeMenu extends InspectorMenu {

        private static final String DEFAULT_TITLE = "View compilations";
        private final TeleClassMethodActor teleClassMethodActor;

        public ViewMethodCompilationsCodeMenu(TeleClassMethodActor teleClassMethodactor, String actionTitle) {
            super(actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.teleClassMethodActor = teleClassMethodactor;
            refresh(true);
        }

        @Override
        public void refresh(boolean force) {
            final List<MaxCompilation> compilations = vm().machineCode().compilations(teleClassMethodActor);
            if (getMenuComponentCount() < compilations.size()) {
                for (int index = getMenuComponentCount(); index < compilations.size(); index++) {
                    final MaxCompilation compilation = compilations.get(index);
                    final StringBuilder name = new StringBuilder();
                    name.append(inspection().nameDisplay().shortMethodCompilationID(compilation));
                    name.append("  ");
                    name.append(compilation.classActorForObjectType().simpleName());
                    add(actions().viewMethodCodeAtLocation(compilation.getCallEntryLocation(), name.toString()));
                }
            }
        }
    }


    /**
     * Creates a menu containing actions to view the machine code for all compilations of a method, dynamically updated
     * as compilations are added.
     *
     * @param teleClassMethodActor representation of a Java method in the VM
     * @param actionTitle name of the action to appear on button or menu
     * @return a dynamically refreshed menu that contains actions to view code for each of the compilations of a method.
     */
    public InspectorMenu viewMethodCompilationsMenu(TeleClassMethodActor teleClassMethodActor, String actionTitle) {
        return new ViewMethodCompilationsCodeMenu(teleClassMethodActor, actionTitle);
    }

    /**
     * Creates a menu containing actions to inspect the machine code for all compilations of a method, dynamically updated
     * as compilations are added.
     *
     * @param teleClassMethodActor representation of a Java method in the VM
     * @return a dynamically refreshed menu that contains actions to view code for each of the compilations of a method.
     */
    public InspectorMenu viewMethodCompilationsCodeMenu(TeleClassMethodActor teleClassMethodActor) {
        return new ViewMethodCompilationsCodeMenu(teleClassMethodActor, null);
    }

    /**
     * Action:  displays in the {@link MethodView} the code of a specified
     * method in the boot image.
     */
    final class ViewMethodCodeInBootImageAction extends InspectorAction {

        private final int offset;

        public ViewMethodCodeInBootImageAction(int offset, Class clazz, String name, Class... parameterTypes) {
            super(inspection(), clazz.getName() + "." + name + SignatureDescriptor.fromJava(Void.TYPE, parameterTypes).toJavaString(false, false));
            this.offset = offset;
        }

        public ViewMethodCodeInBootImageAction(int offset, Method method) {
            this(offset, method.getDeclaringClass(), method.getName(), method.getParameterTypes());
        }

        @Override
        protected void procedure() {
            try {
                focus().setCodeLocation(vm().codeLocations().createMachineCodeLocation(vm().bootImageStart().plus(offset), "address from boot image"), true);
            } catch (InvalidCodeAddressException e) {
                gui().errorMessage("Unable to view, no machine code @ " + e.getAddressString() + ":  " + e.getMessage());
            }
        }
    }

    private final InspectorAction viewRunMethodCodeInBootImageAction =
        new ViewMethodCodeInBootImageAction(vm().bootImage().header.vmRunMethodOffset, ClassRegistry.MaxineVM_run.toJava());

    /**
     * @return an Action that displays in the {@link MethodView} the code of
     * the {@link MaxineVM#run()} method in the boot image.
     */
    public final InspectorAction viewRunMethodCodeInBootImage() {
        return viewRunMethodCodeInBootImageAction;
    }

    private final InspectorAction viewThreadRunMethodCodeInBootImageAction =
        new ViewMethodCodeInBootImageAction(vm().bootImage().header.vmThreadRunMethodOffset, ClassRegistry.VmThread_run.toJava());

    /**
     * @return an Action that displays in the {@link MethodView} the code of
     * the {@link VmThread#run()} method in the boot image.
     */
    public final InspectorAction viewThreadRunMethodCodeInBootImage() {
        return viewThreadRunMethodCodeInBootImageAction;
    }


    /**
     * Action:  copies to the system clipboard a textual representation of the
     * disassembled machine code for a method compilation.
     */
    final class CopyCompiledCodeToClipboardAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Copy disassembled machine code to clipboard";

        private final MaxCompilation compilation;

        private CopyCompiledCodeToClipboardAction(MaxCompilation compilation, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.compilation = compilation;
        }

        @Override
        public void procedure() {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            final PrintStream printStream = new PrintStream(outputStream);
            compilation.writeSummary(printStream);
            gui().postToClipboard(outputStream.toString());
        }
    }

    /**
     * @return an Action that copies to the system clipboard a textual disassembly of machine code.
     */
    public InspectorAction copyCompiledCodeToClipboard(MaxCompilation compilation, String actionTitle) {
        return new CopyCompiledCodeToClipboardAction(compilation, actionTitle);
    }

    /**
     * Action:  copies to the system clipboard a textual representation of the
     * disassembled machine code for a method compilation.
     */
    final class CopyBytecodeToClipboardAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Copy disassembled bytecode to clipboard";

        private final TeleClassMethodActor teleClassMethodActor;

        private CopyBytecodeToClipboardAction(TeleClassMethodActor teleClassMethodActor, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.teleClassMethodActor = teleClassMethodActor;
        }

        @Override
        public void procedure() {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            final PrintStream printStream = new PrintStream(outputStream);
            teleClassMethodActor.writeSummary(printStream);
            gui().postToClipboard(outputStream.toString());
        }
    }

    /**
     * @return an Action that copies to the system clipboard a textual disassembly of machine code.
     */
    public InspectorAction copyBytecodeToClipboard(TeleClassMethodActor teleClassMethodActor, String actionTitle) {
        return new CopyBytecodeToClipboardAction(teleClassMethodActor, actionTitle);
    }

    /**
     * Menu: display a sub-menu of commands for setting breakpoints at standard locations.
     */
    final class BuiltinBreakpointsMenu extends InspectorMenu {
        public BuiltinBreakpointsMenu(String title) {
            super(title == null ? "Break at" : title);
            addMenuListener(new MenuListener() {

                public void menuCanceled(MenuEvent e) {
                }

                public void menuDeselected(MenuEvent e) {
                }

                public void menuSelected(MenuEvent e) {
                    removeAll();
                    for (MaxCodeLocation codeLocation : vm().inspectableMethods()) {
                        add(actions().setBreakpoint(codeLocation));
                    }
                }
            });
        }
    }

   /**
     * Action:  removes the currently selected breakpoint from the VM.
     */
    final class RemoveSelectedBreakpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Remove selected breakpoint";

        RemoveSelectedBreakpointAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            focus().addListener(new InspectionFocusAdapter() {
                @Override
                public void breakpointFocusSet(MaxBreakpoint oldBreakpoint, MaxBreakpoint breakpoint) {
                    refresh(false);
                }
            });
            refresh(false);
        }

        @Override
        protected void procedure() {
            final MaxBreakpoint selectedBreakpoint = focus().breakpoint();
            if (selectedBreakpoint != null) {
                try {
                    selectedBreakpoint.remove();
                    focus().setBreakpoint(null);
                }  catch (MaxVMBusyException maxVMBusyException) {
                    inspection().announceVMBusyFailure(name());
                }
            } else {
                gui().errorMessage("No breakpoint selected");
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasBreakpoint());
        }
    }

    private InspectorAction removeBreakpoint = new RemoveSelectedBreakpointAction(null);

    /**
     * @return an Action that will remove the currently selected breakpoint, if any.
     */
    public final InspectorAction removeSelectedBreakpoint() {
        return removeBreakpoint;
    }

    /**
     * Action: removes a specific  breakpoint in the VM.
     */
    final class RemoveBreakpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Remove breakpoint";

        final MaxBreakpoint breakpoint;

        RemoveBreakpointAction(MaxBreakpoint breakpoint, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.breakpoint = breakpoint;
        }

        @Override
        protected void procedure() {
            if (focus().breakpoint() == breakpoint) {
                focus().setBreakpoint(null);
            }
            try {
                breakpoint.remove();
            } catch (MaxVMBusyException maxVMBusyException) {
                inspection().announceVMBusyFailure(name());
            }
        }
    }

    /**
     * @param surrogate for a breakpoint in the VM.
     * @param actionTitle a string name for the Action, uses default name if null.
     * @return an Action that will remove the breakpoint
     */
    public final InspectorAction removeBreakpoint(MaxBreakpoint breakpoint, String actionTitle) {
        return new RemoveBreakpointAction(breakpoint, actionTitle);
    }

    /**
     * Action: removes all existing breakpoints in the VM.
     */
    final class RemoveAllBreakpointsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Remove all breakpoints";

        RemoveAllBreakpointsAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
            inspection().addInspectionListener(new InspectionListenerAdapter() {
                @Override
                public void breakpointStateChanged() {
                    refresh(true);
                }
            });
        }

        @Override
        protected void procedure() {
            focus().setBreakpoint(null);
            try {
                for (MaxBreakpoint breakpoint : vm().breakpointManager().breakpoints()) {
                    breakpoint.remove();
                }
            } catch (MaxVMBusyException maxVMBusyException) {
                inspection().announceVMBusyFailure(name());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess() && (vm().breakpointManager().breakpoints().size() > 0));
        }
    }

    private InspectorAction removeAllBreakpoints = new RemoveAllBreakpointsAction(null);

    /**
     * @return an Action that will remove all breakpoints in the VM.
     */
    public final InspectorAction removeAllBreakpoints() {
        return removeAllBreakpoints;
    }

    /**
     * Action: enables a specific  breakpoint in the VM.
     */
    final class EnableBreakpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Enable breakpoint";
        final MaxBreakpoint breakpoint;

        EnableBreakpointAction(MaxBreakpoint breakpoint, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.breakpoint = breakpoint;
        }

        @Override
        protected void procedure() {
            if (focus().breakpoint() == breakpoint) {
                focus().setBreakpoint(null);
            }
            try {
                breakpoint.setEnabled(true);
            } catch (MaxVMBusyException maxVMBusyException) {
                inspection().announceVMBusyFailure(name());
            }
            inspection().refreshAll(false);
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess() && !breakpoint.isEnabled());
        }
    }

    /**
     * @param breakpoint surrogate for a breakpoint in the VM.
     * @param actionTitle a string name for the Action, uses default name if null.
     * @return an Action that will enable the breakpoint
     */
    public final InspectorAction enableBreakpoint(MaxBreakpoint breakpoint, String actionTitle) {
        return new EnableBreakpointAction(breakpoint, actionTitle);
    }

    /**
     * Action: disables a specific  breakpoint in the VM.
     */
    final class DisableBreakpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Disable breakpoint";

        final MaxBreakpoint breakpoint;

        DisableBreakpointAction(MaxBreakpoint breakpoint, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.breakpoint = breakpoint;
        }

        @Override
        protected void procedure() {
            if (focus().breakpoint() == breakpoint) {
                focus().setBreakpoint(null);
            }
            try {
                breakpoint.setEnabled(false);
            } catch (MaxVMBusyException maxVMBusyException) {
                inspection().announceVMBusyFailure(name());
            }
            inspection().refreshAll(false);
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess() && breakpoint.isEnabled());
        }
    }

    /**
     * @param breakpoint surrogate for a breakpoint in the VM.
     * @param actionTitle a string name for the Action, uses default name if null.
     * @return an Action that will disable the breakpoint
     */
    public final InspectorAction disableBreakpoint(MaxBreakpoint breakpoint, String actionTitle) {
        return new DisableBreakpointAction(breakpoint, actionTitle);
    }

    /**
     * Action:  set a breakpoint.
     */
    final class SetBreakpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Set breakpoint";

        private final MaxCodeLocation codeLocation;

        public SetBreakpointAction(MaxCodeLocation codeLocation, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            assert codeLocation != null;
            this.codeLocation = codeLocation;
            refresh(true);
        }

        @Override
        protected void procedure() {
            try {
                final MaxBreakpoint breakpoint = vm().breakpointManager().makeBreakpoint(codeLocation);
                focus().setBreakpoint(breakpoint);
            } catch (MaxVMBusyException maxVMBusyException) {
                inspection().announceVMBusyFailure(name());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(vm().breakpointManager().findBreakpoint(codeLocation) == null);
        }
    }

    public final InspectorAction setBreakpoint(MaxCodeLocation codeLocation, String actionTitle) {
        return new SetBreakpointAction(codeLocation, actionTitle);
    }


    public final InspectorAction setBreakpoint(MaxCodeLocation codeLocation) {
        return new SetBreakpointAction(codeLocation, codeLocation.description());
    }

    /**
     * Action:  set a machine code breakpoint at a particular address.
     */
    final class SetMachineCodeBreakpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Set machine code breakpoint";

        private final MaxCodeLocation codeLocation;

        public SetMachineCodeBreakpointAction(MaxCodeLocation codeLocation, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            assert codeLocation != null && codeLocation.hasAddress();
            this.codeLocation = codeLocation;
            refresh(true);
        }

        @Override
        protected void procedure() {
            try {
                final MaxBreakpoint breakpoint = vm().breakpointManager().makeBreakpoint(codeLocation);
                focus().setBreakpoint(breakpoint);
            } catch (MaxVMBusyException maxVMBusyException) {
                inspection().announceVMBusyFailure(name());
            }

        }

        @Override
        public void refresh(boolean force) {
            setEnabled(vm().breakpointManager().findBreakpoint(codeLocation) == null);
        }
    }

    public final InspectorAction setMachineCodeBreakpoint(MaxCodeLocation codeLocation, String actionTitle) {
        return new SetMachineCodeBreakpointAction(codeLocation, actionTitle);
    }

    /**
     * Action:  remove a machine code breakpoint at a particular address.
     */
    final class RemoveMachineCodeBreakpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Remove machine code breakpoint";

        private final MaxCodeLocation codeLocation;

        public RemoveMachineCodeBreakpointAction(MaxCodeLocation codeLocation, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            assert codeLocation != null && codeLocation.hasAddress();
            this.codeLocation = codeLocation;
            refresh(true);
        }

        @Override
        protected void procedure() {
            final MaxBreakpoint breakpoint = vm().breakpointManager().findBreakpoint(codeLocation);
            if (breakpoint != null) {
                try {
                    breakpoint.remove();
                } catch (MaxVMBusyException maxVMBusyException) {
                    inspection().announceVMBusyFailure(name());
                }
                focus().setBreakpoint(null);
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(vm().breakpointManager().findBreakpoint(codeLocation) != null);
        }
    }

    public final InspectorAction removeMachineCodeBreakpoint(MaxCodeLocation codeLocation, String actionTitle) {
        return new RemoveMachineCodeBreakpointAction(codeLocation, actionTitle);
    }

     /**
     * Action:  toggle on/off a breakpoint at the machine code location specified, or
     * if not initialized, then to the machine code location of the current focus.
     */
    final class ToggleMachineCodeBreakpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Toggle machine code breakpoint";

        private final MaxCodeLocation codeLocation;

        ToggleMachineCodeBreakpointAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.codeLocation = null;
            refreshableActions.add(this);
            focus().addListener(new InspectionFocusAdapter() {
                @Override
                public void codeLocationFocusSet(MaxCodeLocation codeLocation, boolean interactiveForNative) {
                    refresh(false);
                }
            });
            refresh(true);
        }

        ToggleMachineCodeBreakpointAction(MaxCodeLocation codeLocation, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            assert codeLocation != null && codeLocation.hasAddress();
            this.codeLocation = codeLocation;
        }

        @Override
        protected void procedure() {
            final MaxCodeLocation toggleCodeLocation = (codeLocation != null) ? codeLocation : focus().codeLocation();
            if (toggleCodeLocation.hasAddress() && toggleCodeLocation.address().isNotZero()) {
                MaxBreakpoint breakpoint = vm().breakpointManager().findBreakpoint(toggleCodeLocation);
                try {
                    if (breakpoint == null) {
                        try {
                            breakpoint = vm().breakpointManager().makeBreakpoint(toggleCodeLocation);
                            focus().setBreakpoint(breakpoint);
                        } catch (MaxVMBusyException maxVMBusyException) {
                            inspection().announceVMBusyFailure(name());
                        }
                    } else {
                        breakpoint.remove();
                        focus().setBreakpoint(null);
                    }
                } catch (MaxVMBusyException maxVMBusyException) {
                    inspection().announceVMBusyFailure(name());
                }
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess()  && focus().hasCodeLocation() && focus().codeLocation().hasAddress());
        }
    }

    private InspectorAction toggleMachineCodeBreakpoint = new ToggleMachineCodeBreakpointAction(null);

    /**
     * @return an Action that will toggle on/off a breakpoint at the machine code location of the current focus.
     */
    public final InspectorAction toggleMachineCodeBreakpoint() {
        return toggleMachineCodeBreakpoint;
    }

    /**
     * @param actionTitle string that identifies the action
     * @return an Action that will toggle on/off a breakpoint at the machine code location of the current focus.
     */
    public final InspectorAction toggleMachineCodeBreakpoint(String actionTitle) {
        return new ToggleMachineCodeBreakpointAction(actionTitle);
    }

    /**
     * @param codeLocation code location
     * @param actionTitle string that identifies the action
     * @return an Action that will toggle on/off a breakpoint at the specified machine code location.
     */
    public final InspectorAction toggleMachineCodeBreakpoint(MaxCodeLocation codeLocation, String actionTitle) {
        return new ToggleMachineCodeBreakpointAction(codeLocation, actionTitle);
    }

    /**
     * Action:  sets a  breakpoint at the machine code location specified interactively..
     */
    final class SetMachineCodeBreakpointAtAddressAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "At address...";

        SetMachineCodeBreakpointAtAddressAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
            refresh(true);
        }

        @Override
        protected void procedure() {
            new NativeLocationInputDialog(inspection(), "Break at machine code address...", vm().bootImageStart(), "") {

                @Override
                public void entered(Address address, String description) {
                    MaxBreakpoint breakpoint = null;
                    try {
                        breakpoint = vm().breakpointManager().makeBreakpoint(vm().codeLocations().createMachineCodeLocation(address, "set machine breakpoint"));
                    } catch (MaxVMBusyException maxVMBusyException) {
                        inspection().announceVMBusyFailure(name());
                        return;
                    } catch (InvalidCodeAddressException e) {
                        gui().errorMessage("Unable to create breakpoint, no code @ " + e.getAddressString() + ":  " + e.getMessage());
                        return;
                    }
                    if (breakpoint == null) {
                        gui().errorMessage("Unable to create breakpoint at: " + address.to0xHexString());
                        return;
                    }
                    breakpoint.setDescription(description);
                }
            };
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess());
        }
    }

    private InspectorAction setMachineCodeBreakpointAtAddressAction = new SetMachineCodeBreakpointAtAddressAction(null);

    /**
     * @return an Action that will toggle on/off a breakpoint at the machine code location of the current focus.
     */
    public final InspectorAction setMachineCodeBreakpointAtAddress() {
        return setMachineCodeBreakpointAtAddressAction;
    }

    /**
     * Action:  sets a breakpoint at every label in a compilation.
     */
    final class SetMachineCodeLabelBreakpointsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Set breakpoint at every machine code label";
        final MaxCompilation compilation;

        SetMachineCodeLabelBreakpointsAction(MaxCompilation compilation, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.compilation = compilation;
            setEnabled(inspection().hasProcess() && compilation.getMachineCodeInfo().labelIndexes().size() > 0);
        }

        @Override
        protected void procedure() {
            try {
                final MaxMachineCodeInfo machineCodeInfo = compilation.getMachineCodeInfo();
                for (int index : machineCodeInfo.labelIndexes()) {
                    vm().breakpointManager().makeBreakpoint(machineCodeInfo.instructionLocation(index));
                }
            } catch (MaxVMBusyException maxVMBusyException) {
                inspection().announceVMBusyFailure(name());
            }
        }
    }

    /**
     * @return an Action that will set a breakpoint at every machine code label in a compilation
     */
    public final InspectorAction setMachineCodeLabelBreakpoints(MaxCompilation compilation, String actionTitle) {
        return new SetMachineCodeLabelBreakpointsAction(compilation, actionTitle);
    }

    /**
     * Action:  removes any breakpoints at machine code labels.
     */
    final class RemoveMachineCodeLabelBreakpointsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Remove breakpoint at every machine code label";
        private final MaxCompilation compilation;

        RemoveMachineCodeLabelBreakpointsAction(MaxCompilation compilation, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.compilation = compilation;
            setEnabled(inspection().hasProcess() && compilation.getMachineCodeInfo().labelIndexes().size() > 0);
        }

        @Override
        protected void procedure() {
            try {
                final MaxMachineCodeInfo machineCodeInfo = compilation.getMachineCodeInfo();
                for (int index : machineCodeInfo.labelIndexes()) {
                    final MaxBreakpoint breakpoint = vm().breakpointManager().findBreakpoint(machineCodeInfo.instructionLocation(index));
                    if (breakpoint != null) {
                        breakpoint.remove();
                    }
                }
            } catch (MaxVMBusyException maxVMBusyException) {
                inspection().announceVMBusyFailure(name());
            }
        }
    }

    /**
     * @return an Action that will remove any breakpoints machine code labels.
     */
    public final InspectorAction removeMachineCodeLabelBreakpoints(MaxCompilation compilation, String actionTitle) {
        return new RemoveMachineCodeLabelBreakpointsAction(compilation, actionTitle);
    }

     /**
     * Action:  sets machine code breakpoints at a specified compiled code entry.
     */
    final class SetMachineCodeBreakpointAtEntryAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Set machine code breakpoint at code entry";
        private final MaxCompilation compilation;
        SetMachineCodeBreakpointAtEntryAction(MaxCompilation compilation, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.compilation = compilation;
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            final MaxCodeLocation callEntryLocation = compilation.getCallEntryLocation();
            try {
                MaxBreakpoint breakpoint = vm().breakpointManager().makeBreakpoint(callEntryLocation);
                focus().setBreakpoint(breakpoint);
            } catch (MaxVMBusyException maxVMBusyException) {
                inspection().announceVMBusyFailure(name());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess());
        }
    }

    /**
     * @return an interactive Action that sets a machine code breakpoint at code entry.
     */
    public final InspectorAction setMachineCodeBreakpointAtEntry(MaxCompilation compilation, String actionTitle) {
        return new  SetMachineCodeBreakpointAtEntryAction(compilation, actionTitle);
    }

    /**
     * @return an interactive Action that sets a compiled code breakpoint at  a method entry
     */
    public final InspectorAction setMachineCodeBreakpointAtEntry(MaxCompilation compilation) {
        return new  SetMachineCodeBreakpointAtEntryAction(compilation, null);
    }

    /**
     * Action:  sets machine code breakpoints at code entries to be selected interactively by name.
     */
    final class SetMachineCodeBreakpointAtEntriesByNameAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Compiled code...";

        SetMachineCodeBreakpointAtEntriesByNameAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor =
                ClassActorSearchDialog.show(inspection(), "Class for machine code entry breakpoints...", "Select");
            if (teleClassActor != null) {
                final List<MaxCompilation> compilations =
                    MethodCompilationSearchDialog.show(inspection(), teleClassActor, "Compiled Method Entry Breakpoints", "Set Breakpoints", true);
                if (compilations != null) {
                    try {
                        // There may be multiple compilations of a method in the result.
                        MaxBreakpoint machineCodeBreakpoint = null;
                        for (MaxCompilation compilation : compilations) {
                            machineCodeBreakpoint =
                                vm().breakpointManager().makeBreakpoint(compilation.getCallEntryLocation());
                        }
                        focus().setBreakpoint(machineCodeBreakpoint);
                    } catch (MaxVMBusyException maxVMBusyException) {
                        inspection().announceVMBusyFailure(name());
                    }
                }
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess());
        }
    }

    private final SetMachineCodeBreakpointAtEntriesByNameAction setMachineCodeBreakpointAtEntriesByNameAction =
        new SetMachineCodeBreakpointAtEntriesByNameAction(null);

    /**
     * @return Singleton interactive Action that sets a machine code breakpoint at  a method entry to be selected by name.
     */
    public final InspectorAction setMachineCodeBreakpointAtEntriesByName() {
        return setMachineCodeBreakpointAtEntriesByNameAction;
    }

    /**
     * Action:  sets machine code breakpoints at native function entries to be selected interactively by name.
     */
    final class SetNativeFunctionBreakpointByNameAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Native function...";

        SetNativeFunctionBreakpointByNameAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            final MaxNativeLibrary maxNativeLibrary = NativeLibrarySearchDialog.show(inspection(), "Native function library for breakpoints...", "Select");
            if (maxNativeLibrary != null) {
                final List<MaxNativeFunction> functions = NativeFunctionSearchDialog.show(inspection(), maxNativeLibrary, "Native Function Breakpoints...", "Set Breakpoint", true);
                if (functions != null) {
                    try {
                        MaxBreakpoint machineCodeBreakpoint = null;
                        for (MaxNativeFunction nativeFunction : functions) {
                            try {
                                machineCodeBreakpoint =
                                    vm().breakpointManager().makeBreakpoint(vm().codeLocations().createMachineCodeLocation(nativeFunction.getCodeStart(), "set machine breakpoint"));
                            } catch (InvalidCodeAddressException e) {
                                gui().errorMessage("Unable to set breakpoint, no code @ " + e.getAddressString() + ":  " + e.getMessage());
                            }
                        }
                        focus().setBreakpoint(machineCodeBreakpoint);
                    } catch (MaxVMBusyException maxVMBusyException) {
                        inspection().announceVMBusyFailure(name());
                    }
                }
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess());
        }
    }

    private final SetNativeFunctionBreakpointByNameAction setNativeFunctionBreakpointByNameAction =
        new SetNativeFunctionBreakpointByNameAction(null);

    /**
     * @return Singleton interactive Action that sets a machine code breakpoint at  a method entry to be selected by name.
     */
    public final InspectorAction setNativeFunctionBreakpointByName() {
        return setNativeFunctionBreakpointByNameAction;
    }

    /**
     * Action: sets machine code breakpoint at object initializers of a class specified interactively by name.
     */
    final class SetMachineCodeBreakpointAtObjectInitializerAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Object initializers of class...";

        SetMachineCodeBreakpointAtObjectInitializerAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(inspection(), "Add breakpoint in Object Initializers of Class...", "Set Breakpoint");
            if (teleClassActor != null) {
                final ClassActor classActor = teleClassActor.classActor();
                if (classActor.localVirtualMethodActors() != null) {
                    try {
                        MaxBreakpoint breakpoint = null;
                        for (VirtualMethodActor virtualMethodActor : classActor.localVirtualMethodActors()) {
                            if (virtualMethodActor.name == SymbolTable.INIT) {
                                final TeleClassMethodActor teleClassMethodActor = vm().findTeleMethodActor(TeleClassMethodActor.class, virtualMethodActor);
                                if (teleClassMethodActor != null) {
                                    for (MaxCompilation compilation : vm().machineCode().compilations(teleClassMethodActor)) {
                                        final MaxCodeLocation callEntryLocation = compilation.getCallEntryLocation();
                                        breakpoint = vm().breakpointManager().makeBreakpoint(callEntryLocation);
                                    }
                                }
                            }
                        }
                        if (breakpoint != null) {
                            focus().setBreakpoint(breakpoint);
                        }
                    } catch (MaxVMBusyException maxVMBusyException) {
                        inspection().announceVMBusyFailure(name());
                    }
                }
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess());
        }
    }

    private InspectorAction setMachineCodeBreakpointAtObjectInitializer =
        new SetMachineCodeBreakpointAtObjectInitializerAction(null);

    /**
     * @return an interactive Action that will set a machine code breakpoint at the
     * object initializer for a class specified by name.
     */
    public final InspectorAction setMachineCodeBreakpointAtObjectInitializer() {
        return setMachineCodeBreakpointAtObjectInitializer;
    }

    /**
     * Action:  toggle on/off a breakpoint at the  bytecode location of the current focus.
     */
    class ToggleBytecodeBreakpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Toggle bytecode breakpoint";

        ToggleBytecodeBreakpointAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
            focus().addListener(new InspectionFocusAdapter() {
                @Override
                public void codeLocationFocusSet(MaxCodeLocation codeLocation, boolean interactiveForNative) {
                    refresh(false);
                }
            });
        }

        @Override
        protected void procedure() {
            final MaxCodeLocation codeLocation = focus().codeLocation();
            if (codeLocation.hasMethodKey()) {
                final MaxBreakpoint breakpoint = vm().breakpointManager().findBreakpoint(codeLocation);
                try {
                    if (breakpoint == null) {
                        vm().breakpointManager().makeBreakpoint(codeLocation);
                    } else {
                        breakpoint.remove();
                    }
                } catch (MaxVMBusyException maxVMBusyException) {
                    inspection().announceVMBusyFailure(name());
                }
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess()  && focus().hasCodeLocation() && focus().codeLocation().hasTeleClassMethodActor());
        }
    }

    private InspectorAction toggleBytecodeBreakpoint = new ToggleBytecodeBreakpointAction(null);

    /**
     * @return an Action that will toggle on/off a breakpoint at the bytecode location of the current focus.
     */
    public final InspectorAction toggleBytecodeBreakpoint() {
        return toggleBytecodeBreakpoint;
    }

    /**
     * Action: sets a bytecode breakpoint at a specified method entry.
     */
    final class SetBytecodeBreakpointAtMethodEntryAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Method on classpath";
        private final TeleClassMethodActor teleClassMethodActor;

        SetBytecodeBreakpointAtMethodEntryAction(TeleClassMethodActor teleClassMethodActor, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.teleClassMethodActor = teleClassMethodActor;
            refreshableActions.add(this);
            refresh(true);
        }

        @Override
        protected void procedure() {
            final MaxCodeLocation location = vm().codeLocations().createBytecodeLocation(teleClassMethodActor, -1, "teleClassMethodActor entry");
            try {
                vm().breakpointManager().makeBreakpoint(location);
            } catch (MaxVMBusyException maxVMBusyException) {
                inspection().announceVMBusyFailure(name());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess() && teleClassMethodActor.hasCodeAttribute());
        }
    }

    /**
     * @return an Action  that will set a bytecode breakpoint at a method entry.
     */
    public final InspectorAction setBytecodeBreakpointAtMethodEntry(TeleClassMethodActor teleClassMethodActor, String actionTitle) {
        return new SetBytecodeBreakpointAtMethodEntryAction(teleClassMethodActor, actionTitle);
    }

    /**
     * @return an Action that will set a bytecode breakpoint at a method entry.
     */
    public final InspectorAction setBytecodeBreakpointAtMethodEntry(TeleClassMethodActor teleClassMethodActor) {
        return new SetBytecodeBreakpointAtMethodEntryAction(teleClassMethodActor, null);
    }

     /**
     * Action: sets a bytecode breakpoint at a method entry specified interactively by name.
     */
    final class SetBytecodeBreakpointAtMethodEntryByNameAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Method on classpath, by name...";

        SetBytecodeBreakpointAtMethodEntryByNameAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            final TypeDescriptor typeDescriptor = TypeSearchDialog.show(inspection(), "Class for bytecode method entry breakpoint...", "Select");
            if (typeDescriptor != null) {
                final MethodKey methodKey = MethodSearchDialog.show(inspection(), typeDescriptor, "Bytecodes method entry breakpoint", "Set Breakpoint");
                if (methodKey != null) {
                    try {
                        vm().breakpointManager().makeBreakpoint(vm().codeLocations().createBytecodeLocation(methodKey, "set bytecode breakpoint"));
                    } catch (MaxVMBusyException maxVMBusyException) {
                        inspection().announceVMBusyFailure(name());
                    }
                }
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess());
        }
    }

    private final SetBytecodeBreakpointAtMethodEntryByNameAction setBytecodeBreakpointAtMethodEntryByNameAction =
        new SetBytecodeBreakpointAtMethodEntryByNameAction(null);

    /**
     * @return Singleton interactive Action that will set a bytecode breakpoint at a method entry to be selected by name.
     */
    public final InspectorAction setBytecodeBreakpointAtMethodEntryByName() {
        return setBytecodeBreakpointAtMethodEntryByNameAction;
    }

    /**
     * Action: sets a bytecode breakpoint at a method entry specified interactively by name.
     */
    final class SetBytecodeBreakpointAtMethodEntryByKeyAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Method matched by key...";

        SetBytecodeBreakpointAtMethodEntryByKeyAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            final MethodKey methodKey = MethodKeyInputDialog.show(inspection(), "Specify method");
            if (methodKey != null) {
                try {
                    vm().breakpointManager().makeBreakpoint(vm().codeLocations().createBytecodeLocation(methodKey, "set bytecode breakpoint"));
                } catch (MaxVMBusyException maxVMBusyException) {
                    inspection().announceVMBusyFailure(name());
                }
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess());
        }
    }

    private final SetBytecodeBreakpointAtMethodEntryByKeyAction setBytecodeBreakpointAtMethodEntryByKeyAction =
        new SetBytecodeBreakpointAtMethodEntryByKeyAction(null);

    /**
     * @return Singleton interactive Action that will set a bytecode breakpoint at  a method entry to be selected by name.
     */
    public final InspectorAction setBytecodeBreakpointAtMethodEntryByKey() {
        return setBytecodeBreakpointAtMethodEntryByKeyAction;
    }

   /**
     * Action: create a memory word watchpoint, interactive if no location specified.
     */
    final class SetWordWatchpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Watch memory word";
        private final MaxMemoryRegion memoryRegion;

        SetWordWatchpointAction() {
            super(inspection(), "Watch memory word at address...");
            this.memoryRegion = null;
            setEnabled(true);
        }

        SetWordWatchpointAction(Address address, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.memoryRegion = new MemoryWordRegion(vm(), address, 1);
            setEnabled(vm().watchpointManager().findWatchpoints(memoryRegion).isEmpty());
        }

        @Override
        protected void procedure() {
            if (memoryRegion != null) {
                setWatchpoint(memoryRegion, "");
            } else {
                final int nBytesInWord = vm().platform().nBytesInWord();
                new MemoryRegionInputDialog(inspection(), vm().bootImageStart(), "Watch memory starting at address...", "Watch") {
                    @Override
                    public void entered(Address address, long nBytes) {
                        setWatchpoint(new MemoryWordRegion(vm(), address, nBytes / nBytesInWord), "User specified region");
                    }
                };
            }
        }

        private void setWatchpoint(MaxMemoryRegion memoryRegion, String description) {
            final WatchpointsViewPreferences prefs = WatchpointsViewPreferences.globalPreferences(inspection());
            try {
                final MaxWatchpoint watchpoint = vm().watchpointManager().createRegionWatchpoint(description, memoryRegion, prefs.settings());
                if (watchpoint == null) {
                    gui().errorMessage("Watchpoint creation failed");
                } else {
                    focus().setWatchpoint(watchpoint);
                }
            } catch (MaxTooManyWatchpointsException tooManyWatchpointsException) {
                gui().errorMessage(tooManyWatchpointsException.getMessage());
            } catch (MaxDuplicateWatchpointException duplicateWatchpointException) {
                gui().errorMessage(duplicateWatchpointException.getMessage());
            } catch (MaxVMBusyException maxVMBusyException) {
                inspection().announceVMBusyFailure(name());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess()  && vm().watchpointManager() != null);
        }
    }

    private final SetWordWatchpointAction setWordWatchpointAction = new SetWordWatchpointAction();

    /**
     * @return Singleton interactive Action that will create a memory word watchpoint in the VM.
     */
    public final InspectorAction setWordWatchpoint() {
        return setWordWatchpointAction;
    }

    /**
     * Creates an action that will create a memory word watchpoint.
     *
     * @param address a memory location in the VM
     * @return an Action that will set a memory watchpoint at the address.
     */
    public final InspectorAction setWordWatchpoint(Address address, String string) {
        return new SetWordWatchpointAction(address, string);
    }

    /**
     * Action: create a memory watchpoint, interactive if no location specified.
     */
    final class SetRegionWatchpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Watch memory region";
        private static final String DEFAULT_REGION_DESCRIPTION = "";
        private final MaxMemoryRegion memoryRegion;
        private final String regionDescription;

        SetRegionWatchpointAction() {
            super(inspection(), "Watch memory region...");
            this.memoryRegion = null;
            this.regionDescription = null;
            setEnabled(true);
        }

        SetRegionWatchpointAction(MaxMemoryRegion memoryRegion, String actionTitle, String regionDescription) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.memoryRegion = memoryRegion;
            this.regionDescription = regionDescription == null ? DEFAULT_REGION_DESCRIPTION : regionDescription;
            setEnabled(vm().watchpointManager().findWatchpoints(memoryRegion).isEmpty());
        }

        @Override
        protected void procedure() {
            if (memoryRegion != null) {
                setWatchpoint(memoryRegion, regionDescription);
            } else {
                // TODO (mlvdv) Generalize AddressInputDialog for a Region
                new AddressInputDialog(inspection(), vm().bootImageStart(), "Watch memory...", "Watch") {
                    @Override
                    public void entered(Address address) {
                        setWatchpoint(new InspectorMemoryRegion(vm(), "", address, vm().platform().nBytesInWord()), "User specified region");
                    }
                };
            }
        }

        private void setWatchpoint(MaxMemoryRegion memoryRegion, String description) {
            final WatchpointsViewPreferences prefs = WatchpointsViewPreferences.globalPreferences(inspection());
            try {
                final MaxWatchpoint watchpoint = vm().watchpointManager().createRegionWatchpoint(description, memoryRegion, prefs.settings());
                if (watchpoint == null) {
                    gui().errorMessage("Watchpoint creation failed");
                } else {
                    focus().setWatchpoint(watchpoint);
                }
            } catch (MaxTooManyWatchpointsException tooManyWatchpointsException) {
                gui().errorMessage(tooManyWatchpointsException.getMessage());
            } catch (MaxDuplicateWatchpointException duplicateWatchpointException) {
                gui().errorMessage(duplicateWatchpointException.getMessage());
            } catch (MaxVMBusyException maxVMBusyException) {
                inspection().announceVMBusyFailure(name());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess()  && vm().watchpointManager() != null);
        }
    }

    private final InspectorAction setRegionWatchpointAction = new SetRegionWatchpointAction();

    /**
     * @return Singleton interactive Action that will create a memory  watchpoint in the VM.
     */
    public final InspectorAction setRegionWatchpoint() {
        return setRegionWatchpointAction;
    }

    /**
     * Creates an action that will create a memory watchpoint.
     *
     * @param memoryRegion an area of memory in the VM
     * @param actionTitle a name for the action, use default name if null
     * @param regionName a description that will be attached to the watchpoint for viewing purposes, default if null.
     * @return an Action that will set a memory watchpoint at the address.
     */
    public final InspectorAction setRegionWatchpoint(MaxMemoryRegion memoryRegion, String actionTitle, String regionName) {
        return new SetRegionWatchpointAction(memoryRegion, actionTitle, regionName);
    }

     /**
     * Action: create an object memory watchpoint.
     */
    final class SetObjectWatchpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Watch object memory";
        private final TeleObject teleObject;
        private final MaxMemoryRegion memoryRegion;

        SetObjectWatchpointAction(TeleObject teleObject, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.teleObject = teleObject;
            this.memoryRegion = teleObject.objectMemoryRegion();
            refresh(true);
        }

        @Override
        protected void procedure() {
            final WatchpointsViewPreferences prefs = WatchpointsViewPreferences.globalPreferences(inspection());
            try {
                final String description = "Whole object";
                final MaxWatchpoint watchpoint = vm().watchpointManager().createObjectWatchpoint(description, teleObject, prefs.settings());
                if (watchpoint == null) {
                    gui().errorMessage("Watchpoint creation failed");
                } else {
                    focus().setWatchpoint(watchpoint);
                }
            } catch (MaxTooManyWatchpointsException tooManyWatchpointsException) {
                gui().errorMessage(tooManyWatchpointsException.getMessage());
            } catch (MaxDuplicateWatchpointException duplicateWatchpointException) {
                gui().errorMessage(duplicateWatchpointException.getMessage());
            } catch (MaxVMBusyException maxVMBusyException) {
                InspectorWarning.message(inspection(), "Watchpoint creation failed", maxVMBusyException);
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess()
                && vm().watchpointManager() != null
                && vm().watchpointManager().findWatchpoints(memoryRegion).isEmpty());
        }
    }

    /**
     * Creates an action that will create an object memory watchpoint.
     *
     * @param teleObject a heap object in the VM
     * @param actionTitle a name for the action, use default name if null
     * @return an Action that will set an object field watchpoint.
     */
    public final InspectorAction setObjectWatchpoint(TeleObject teleObject, String actionTitle) {
        return new SetObjectWatchpointAction(teleObject, actionTitle);
    }

    /**
     * Action: create an object field watchpoint.
     */
    final class SetFieldWatchpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Watch object field";
        private final TeleObject teleObject;
        private final FieldActor fieldActor;
        private final MaxMemoryRegion memoryRegion;

        SetFieldWatchpointAction(TeleObject teleObject, FieldActor fieldActor, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.teleObject = teleObject;
            this.fieldActor = fieldActor;
            this.memoryRegion = teleObject.fieldMemoryRegion(fieldActor);
            refresh(true);
        }

        @Override
        protected void procedure() {
            final WatchpointsViewPreferences prefs = WatchpointsViewPreferences.globalPreferences(inspection());
            try {
                final String description = "Field \"" + fieldActor.name.toString() + "\"";
                final MaxWatchpoint watchpoint = vm().watchpointManager().createFieldWatchpoint(description, teleObject, fieldActor, prefs.settings());
                if (watchpoint == null) {
                    gui().errorMessage("Watchpoint creation failed");
                } else {
                    focus().setWatchpoint(watchpoint);
                }
            } catch (MaxTooManyWatchpointsException tooManyWatchpointsException) {
                gui().errorMessage(tooManyWatchpointsException.getMessage());
            } catch (MaxDuplicateWatchpointException duplicateWatchpointException) {
                gui().errorMessage(duplicateWatchpointException.getMessage());
            } catch (MaxVMBusyException maxVMBusyException) {
                inspection().announceVMBusyFailure(name());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess()
                && vm().watchpointManager() != null
                && vm().watchpointManager().findWatchpoints(memoryRegion).isEmpty());
        }
    }

    /**
     * Creates an action that will create an object field watchpoint.
     *
     * @param teleObject a heap object in the VM
     * @param fieldActor description of a field in the class type of the heap object
     * @param actionTitle a name for the action, use default name if null
     * @return an Action that will set an object field watchpoint.
     */
    public final InspectorAction setFieldWatchpoint(TeleObject teleObject, FieldActor fieldActor, String actionTitle) {
        return new SetFieldWatchpointAction(teleObject, fieldActor, actionTitle);
    }

    /**
     * Action: create an object field watchpoint.
     */
    final class SetArrayElementWatchpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Watch array element";
        private final TeleObject teleObject;
        private final Kind elementKind;
        private final int arrayOffsetFromOrigin;
        private final int index;
        private final String indexPrefix;
        private final MaxMemoryRegion memoryRegion;

        SetArrayElementWatchpointAction(TeleObject teleObject, Kind elementKind, int arrayOffsetFromOrigin, int index, String indexPrefix, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.teleObject = teleObject;
            this.elementKind = elementKind;
            this.arrayOffsetFromOrigin = arrayOffsetFromOrigin;
            this.index = index;
            this.indexPrefix = indexPrefix;
            final Pointer address = teleObject.origin().plus(arrayOffsetFromOrigin + (index * elementKind.width.numberOfBytes));
            this.memoryRegion = new InspectorMemoryRegion(vm(), "", address, elementKind.width.numberOfBytes);
            refresh(true);
        }

        @Override
        protected void procedure() {
            final WatchpointsViewPreferences prefs = WatchpointsViewPreferences.globalPreferences(inspection());
            try {
                final String description = "Element " + indexPrefix + "[" + Integer.toString(index) + "]";
                final MaxWatchpoint watchpoint
                    = vm().watchpointManager().createArrayElementWatchpoint(description, teleObject, elementKind, arrayOffsetFromOrigin, index, prefs.settings());
                if (watchpoint == null) {
                    gui().errorMessage("Watchpoint creation failed");
                } else {
                    focus().setWatchpoint(watchpoint);
                }
            } catch (MaxTooManyWatchpointsException tooManyWatchpointsException) {
                gui().errorMessage(tooManyWatchpointsException.getMessage());
            } catch (MaxDuplicateWatchpointException duplicateWatchpointException) {
                gui().errorMessage(duplicateWatchpointException.getMessage());
            } catch (MaxVMBusyException maxVMBusyException) {
                inspection().announceVMBusyFailure(name());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess()
                && vm().watchpointManager() != null
                && vm().watchpointManager().findWatchpoints(memoryRegion).isEmpty());
        }
    }

    /**
     * Creates an action that will create an array element watchpoint.
     *
     * @param teleObject a heap object in the VM
     * @param elementKind type category of the array elements
     * @param arrayOffsetFromOrigin offset in bytes from the object origin of element 0
     * @param index index into the array
     * @param indexPrefix  text to prepend to the displayed name(index) of each element.
     * @param actionTitle a name for the action, use default name if null
     * @return an Action that will set an array element watchpoint.
     */
    public final InspectorAction setArrayElementWatchpoint(TeleObject teleObject, Kind elementKind, int arrayOffsetFromOrigin, int index, String indexPrefix, String actionTitle) {
        return new SetArrayElementWatchpointAction(teleObject, elementKind, arrayOffsetFromOrigin, index, indexPrefix, actionTitle);
    }

     /**
     * Action: create an object header field watchpoint.
     */
    final class SetHeaderWatchpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Watch object header field";
        private final TeleObject teleObject;
        private final HeaderField headerField;
        private final MaxMemoryRegion memoryRegion;

        SetHeaderWatchpointAction(TeleObject teleObject, HeaderField headerField, String actionTitle)  {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.teleObject = teleObject;
            this.headerField = headerField;
            this.memoryRegion = teleObject.headerMemoryRegion(headerField);
            refresh(true);
        }

        @Override
        protected void procedure() {
            final WatchpointsViewPreferences prefs = WatchpointsViewPreferences.globalPreferences(inspection());
            try {
                final String description = "Field \"" + headerField.name + "\"";
                final MaxWatchpoint watchpoint = vm().watchpointManager().createHeaderWatchpoint(description, teleObject, headerField, prefs.settings());
                if (watchpoint == null) {
                    gui().errorMessage("Watchpoint creation failed");
                } else {
                    focus().setWatchpoint(watchpoint);
                }
            } catch (MaxTooManyWatchpointsException tooManyWatchpointsException) {
                gui().errorMessage(tooManyWatchpointsException.getMessage());
            } catch (MaxDuplicateWatchpointException duplicateWatchpointException) {
                gui().errorMessage(duplicateWatchpointException.getMessage());
            } catch (MaxVMBusyException maxVMBusyException) {
                inspection().announceVMBusyFailure(name());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess()
                && vm().watchpointManager() != null
                && vm().watchpointManager().findWatchpoints(memoryRegion).isEmpty());
        }
    }

    /**
     * Creates an action that will create an object header field watchpoint.
     *
     * @param teleObject a heap object in the VM
     * @param headerField identification of an object header field
     * @param actionTitle a name for the action, use default name if null
     * @return an Action that will set an object header watchpoint
     */
    public final InspectorAction setHeaderWatchpoint(TeleObject teleObject, HeaderField headerField, String actionTitle) {
        return new SetHeaderWatchpointAction(teleObject, headerField, actionTitle);
    }

    /**
     * Action: create an object field watchpoint.
     */
    final class SetThreadLocalWatchpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Watch thread local variable";
        private final MaxThreadLocalVariable threadLocalVariable;

        SetThreadLocalWatchpointAction(MaxThreadLocalVariable threadLocalVariable, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.threadLocalVariable = threadLocalVariable;
            refresh(true);
        }

        @Override
        protected void procedure() {
            final WatchpointsViewPreferences prefs = WatchpointsViewPreferences.globalPreferences(inspection());
            try {
                final String description = "Thread local variable\"" + threadLocalVariable.variableName()
                    + "\" (" + inspection().nameDisplay().shortName(threadLocalVariable.thread()) + ","
                    + threadLocalVariable.safepointState().toString() + ")";
                final MaxWatchpoint watchpoint = vm().watchpointManager().createVmThreadLocalWatchpoint(description, threadLocalVariable, prefs.settings());
                if (watchpoint == null) {
                    gui().errorMessage("Watchpoint creation failed");
                } else {
                    focus().setWatchpoint(watchpoint);
                }
            } catch (MaxTooManyWatchpointsException tooManyWatchpointsException) {
                gui().errorMessage(tooManyWatchpointsException.getMessage());
            } catch (MaxDuplicateWatchpointException duplicateWatchpointException) {
                gui().errorMessage(duplicateWatchpointException.getMessage());
            } catch (MaxVMBusyException maxVMBusyException) {
                inspection().announceVMBusyFailure(name());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess()
                && vm().watchpointManager() != null
                && vm().watchpointManager().findWatchpoints(threadLocalVariable.memoryRegion()).isEmpty());
        }
    }

    /**
     * Creates an action that will create a thread local variable watchpoint.
     *
     * @param threadLocalVariable a thread local variable
     * @param actionTitle a name for the action, use default name if null
     * @return an action that will create a thread local variable watchpoint
     */
    public final InspectorAction setThreadLocalWatchpoint(MaxThreadLocalVariable threadLocalVariable, String actionTitle) {
        return new SetThreadLocalWatchpointAction(threadLocalVariable, actionTitle);
    }

    /**
     * Action: remove a specified memory watchpoint.
     */
    final class RemoveWatchpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Remove memory watchpoint";
        private final MaxWatchpoint watchpoint;

        RemoveWatchpointAction(MaxWatchpoint watchpoint, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.watchpoint = watchpoint;
        }

        @Override
        protected void procedure() {
            try {
                if (watchpoint.remove()) {
                    focus().setWatchpoint(null);
                }  else {
                    gui().errorMessage("Watchpoint removal failed");
                }
            } catch (MaxVMBusyException maxVMBusyException) {
                inspection().announceVMBusyFailure(name());
            }
        }

    }

    /**
     * Creates an action that will remove a watchpoint.
     *
     * @param watchpoint an existing VM memory watchpoint
     * @param actionTitle a title for the action, use default name if null
     * @return an Action that will remove a watchpoint, if present at memory location.
     */
    public final InspectorAction removeWatchpoint(MaxWatchpoint watchpoint, String actionTitle) {
        return new RemoveWatchpointAction(watchpoint, actionTitle);
    }

    /**
     * Action:  removes the watchpoint from the VM that is currently selected.
     */
    final class RemoveSelectedWatchpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Remove selected watchpoint";

        RemoveSelectedWatchpointAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            focus().addListener(new InspectionFocusAdapter() {
                @Override
                public void watchpointFocusSet(MaxWatchpoint oldWatchpoint, MaxWatchpoint watchpoint) {
                    refresh(false);
                }
            });
            refresh(false);
        }

        @Override
        protected void procedure() {
            final MaxWatchpoint watchpoint = focus().watchpoint();
            try {
                if (watchpoint != null) {
                    if (watchpoint.remove()) {
                        focus().setWatchpoint(null);
                    } else {
                        gui().errorMessage("Watchpoint removal failed");
                    }
                } else {
                    gui().errorMessage("No watchpoint selected");
                }
            } catch (MaxVMBusyException maxVMBusyException) {
                inspection().announceVMBusyFailure(name());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasWatchpoint());
        }
    }

    private InspectorAction removeSelectedWatchpoint = new RemoveSelectedWatchpointAction(null);

    /**
     * @return an Action that will remove the currently selected breakpoint, if any.
     */
    public final InspectorAction removeSelectedWatchpoint() {
        return removeSelectedWatchpoint;
    }

    /**
     * Action: removes a set of existing watchpoints in the VM.
     */
    final class RemoveWatchpointsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Remove watchpoints";
        private final List<MaxWatchpoint> watchpoints;

        RemoveWatchpointsAction(List<MaxWatchpoint> watchpoints, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.watchpoints = watchpoints;
        }

        @Override
        protected void procedure() {
            final MaxWatchpoint focusWatchpoint = focus().watchpoint();
            for (MaxWatchpoint watchpoint : watchpoints) {
                if (focusWatchpoint == watchpoint) {
                    focus().setWatchpoint(null);
                }
                try {
                    if (!watchpoint.remove()) {
                        gui().errorMessage("Failed to remove watchpoint" + watchpoint);
                    }
                } catch (MaxVMBusyException maxVMBusyException) {
                    inspection().announceVMBusyFailure(name());
                }
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(watchpoints.size() > 0);
        }
    }

    /**
     * @return an Action that will remove all watchpoints in the VM.
     */
    public final InspectorAction removeWatchpoints(List<MaxWatchpoint> watchpoints, String actionTitle) {
        return new RemoveWatchpointsAction(watchpoints, actionTitle);
    }

     /**
     * Action: removes all existing watchpoints in the VM.
     */
    final class RemoveAllWatchpointsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Remove all watchpoints";

        RemoveAllWatchpointsAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
            inspection().addInspectionListener(new InspectionListenerAdapter() {
                @Override
                public void watchpointSetChanged() {
                    refresh(true);
                }
            });
        }

        @Override
        protected void procedure() {
            focus().setWatchpoint(null);
            for (MaxWatchpoint watchpoint : vm().watchpointManager().watchpoints()) {
                try {
                    if (!watchpoint.remove()) {
                        gui().errorMessage("Failed to remove watchpoint" + watchpoint);
                    }
                } catch (MaxVMBusyException maxVMBusyException) {
                    inspection().announceVMBusyFailure(name());
                }
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(vm().watchpointManager() != null && vm().watchpointManager().watchpoints().size() > 0);
        }
    }

    private InspectorAction removeAllWatchpoints = new RemoveAllWatchpointsAction(null);


    final class SetMarkBitAction extends InspectorAction {
        private static final String DEFAULT_TITLE = "Mark bit";
        SetMarkBitAction() {
            super(inspection(), "Display mark bit for word at address...");
            setEnabled(true);
        }

        @Override
        protected void procedure() {
            new AddressInputDialog(inspection(), Address.zero(), "View Mark Bit for word at address...", "View") {
                @Override
                public void entered(Address address) {
                    MaxMarkBitsInfo m = vm().heap().markBitInfo();
                    assert m != null;
                    if (!m.isCovered(address)) {
                        gui().errorMessage("Address " + address + " is not covered with a mark bit");
                        return;
                    }
                    // TODO (ld): Need to add a mark bit  if one isn't created already for the address, and update  the MarkBitView.
                }
            };
        }
    }


    /**
     * @return an Action that will remove all watchpoints in the VM.
     */
    public final InspectorAction removeAllWatchpoints() {
        return removeAllWatchpoints;
    }

    private InspectorAction setMarkBitAtIndex;

    /**
     * @return an Action that will add a mark bit from a specified bit index to the mark bits view.
     */
    public final InspectorAction setMarkBitAtIndex() {
        return setMarkBitAtIndex;
    }

    private InspectorAction setMarkBitAtAddress;

    /**
     * @return an Action that will add a mark bit from a specified heap address to the mark bits view.
     */
    public final InspectorAction setMarkBitAtAddress() {
        return setMarkBitAtAddress;
    }

    private InspectorAction removeSelectedMarkBit;

    /**
     * @return an Action that will remove the currently selected mark bit from the mark bits view.
     */
    public final InspectorAction removeSelectedMarkBit() {
        return removeSelectedMarkBit;
    }

    private InspectorAction removeAllMarkBits;
    /**
     * @return an Action that will remove all mark bits from the mark bits view.
     */
    public final InspectorAction removeAllMarkBits() {
        return removeAllMarkBits;
    }

     /**
     * Action:  pause the running VM.
     */
    final class DebugPauseAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Pause process";

        DebugPauseAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            try {
                vm().pauseVM();
            } catch (Exception exception) {
                gui().errorMessage("Pause could not be initiated", exception.toString());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasThread() && inspection().isVMRunning());
        }
    }

    private final InspectorAction debugPauseAction = new DebugPauseAction(null);

    /**
     * @return Singleton Action that will pause execution of theVM.
     */
    public final InspectorAction debugPause() {
        return debugPauseAction;
    }

    /**
     * Action: resumes the running VM.
     */
    final class DebugResumeAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Resume";

        DebugResumeAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            try {
                vm().resume(false, true);
            } catch (Exception exception) {
                gui().errorMessage("Run to instruction could not be performed.", exception.toString());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasThread() && inspection().isVMReady());
        }
    }

    private final InspectorAction debugResumeAction = new DebugResumeAction(null);

     /**
     * @return Singleton Action that will resume full execution of theVM.
     */
    public final InspectorAction debugResume() {
        return debugResumeAction;
    }

    /**
     * Action:  advance the currently selected thread until it returns from its current frame in the VM,
     * ignoring breakpoints.
     */
    final class DebugReturnFromFrameAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Return from frame (ignoring breakpoints)";

        DebugReturnFromFrameAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            try {
                vm().returnFromFrame(focus().thread(), false, false);
                // TODO (mlvdv) too broad a catch; narrow this
            } catch (Exception exception) {
                gui().errorMessage("Return from frame (ignoring breakpoints) could not be performed.", exception.toString());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasThread() && inspection().isVMReady());
        }
    }

    private final InspectorAction debugReturnFromFrameAction = new DebugReturnFromFrameAction(null);

    /**
     * @return Singleton Action that will resume execution in the VM, stopping at the first instruction after returning
     *         from the current frame of the currently selected thread
     */
    public final InspectorAction debugReturnFromFrame() {
        return debugReturnFromFrameAction;
    }

    /**
     * Action:  advance the currently selected thread until it returns from its current frame
     * or hits a breakpoint in the VM.
     */
    final class DebugReturnFromFrameWithBreakpointsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Return from frame";

        DebugReturnFromFrameWithBreakpointsAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        public void procedure() {
            try {
                vm().returnFromFrame(focus().thread(), false, true);
            } catch (Exception exception) {
                gui().errorMessage("Return from frame could not be performed.", exception.toString());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasThread() && inspection().isVMReady());
        }
    }

    private final InspectorAction debugReturnFromFrameWithBreakpointsAction = new DebugReturnFromFrameWithBreakpointsAction(null);

    /**
     * @return Singleton Action that will resume execution in the VM, stopping at the first instruction after returning
     *         from the current frame of the currently selected thread, or at a breakpoint, whichever comes first.
     */
    public final InspectorAction debugReturnFromFrameWithBreakpoints() {
        return debugReturnFromFrameWithBreakpointsAction;
    }

    /**
     * Action:  advance the currently selected thread in the VM until it reaches the specified instruction, or
     * if none specified, then the currently selected instruction, ignoring breakpoints.
     */
    final class DebugRunToInstructionAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Run to selected instruction (ignoring breakpoints)";

        private final MaxCodeLocation codeLocation;

        DebugRunToInstructionAction(MaxCodeLocation codeLocation, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.codeLocation = codeLocation;
            refreshableActions.add(this);
            refresh(true);
        }

        @Override
        protected void procedure() {
            final MaxCodeLocation machineCodeLocation = (codeLocation != null) ? codeLocation : focus().codeLocation();
            if (machineCodeLocation.address().isNotZero()) {
                try {
                    vm().runToInstruction(machineCodeLocation, false, false);
                } catch (Exception exception) {
                    InspectorError.unexpected("Run to instruction (ignoring breakpoints) could not be performed.", exception);
                }
            } else {
                gui().errorMessage("No instruction selected");
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasThread() && focus().hasCodeLocation() && inspection().isVMReady());
        }
    }

    private final InspectorAction debugRunToSelectedInstructionAction = new DebugRunToInstructionAction(null, null);

    /**
     * @return Singleton Action that will resume execution in the VM, stopping at the the currently
     * selected instruction, ignoring breakpoints.
     */
    public final InspectorAction debugRunToSelectedInstruction() {
        return debugRunToSelectedInstructionAction;
    }

    /**
     * @param codeLocation a code location in the VM
     * @param actionTitle string that describes the action
     * @return an Action that will resume execution in the VM, stopping at the specified
     * code location, ignoring breakpoints.
     */
    public final InspectorAction debugRunToInstruction(MaxCodeLocation codeLocation, String actionTitle) {
        return new DebugRunToInstructionAction(codeLocation, actionTitle);
    }

    /**
     * Action:  advance the currently selected thread in the VM until it reaches the specified code location
     * (or the currently selected instruction if none specified) or a breakpoint, whichever comes first.
     */
    final class DebugRunToInstructionWithBreakpointsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Run to selected instruction";

        final MaxCodeLocation codeLocation;

        DebugRunToInstructionWithBreakpointsAction(MaxCodeLocation codeLocation, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.codeLocation = codeLocation;
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            final MaxCodeLocation machineCodeLocation = (codeLocation != null) ? codeLocation : focus().codeLocation();
            if (machineCodeLocation != null && machineCodeLocation.hasAddress()) {
                try {
                    vm().runToInstruction(machineCodeLocation, false, true);
                    // TODO (mlvdv)  narrow the catch
                } catch (Exception exception) {
                    InspectorError.unexpected("Run to selection instruction could not be performed.", exception);
                }
            } else {
                gui().errorMessage("No instruction selected");
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasThread() && focus().hasCodeLocation() && inspection().isVMReady());
        }
    }

    private final InspectorAction debugRunToSelectedInstructionWithBreakpointsAction = new DebugRunToInstructionWithBreakpointsAction(null, null);

    /**
     * @return Singleton Action that will resume execution in the VM, stopping at the selected instruction
     * or a breakpoint, whichever comes first..
     */
    public final InspectorAction debugRunToSelectedInstructionWithBreakpoints() {
        return debugRunToSelectedInstructionWithBreakpointsAction;
    }

    /**
     * @param codeLocation a code location in the VM
     * @param actionTitle string that identifies the action
     * @return an Action that will resume execution in the VM, stopping at the specified instruction
     * or a breakpoint, whichever comes first..
     */
    public final InspectorAction debugRunToInstructionWithBreakpoints(MaxCodeLocation codeLocation, String actionTitle) {
        return new DebugRunToInstructionWithBreakpointsAction(codeLocation, actionTitle);
    }

    /**
     * Action:  advance the currently selected thread in the VM until it reaches the next call instruction,
     * ignoring breakpoints; fails if there is no known call in the method containing the IP.
     */
    final class DebugRunToNextCallAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Run to next call instruction (ignoring breakpoints)";

        DebugRunToNextCallAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            final MaxCodeLocation maxCodeLocation = focus().codeLocation();
            if (maxCodeLocation != null && maxCodeLocation.hasAddress()) {
                final MaxCompilation compilation = vm().machineCode().findCompilation(maxCodeLocation.address());
                if (compilation != null) {
                    final MaxMachineCodeInfo machineCodeInfo = compilation.getMachineCodeInfo();
                    final int instructionIndex = machineCodeInfo.findInstructionIndex(maxCodeLocation.address());
                    for (int index = instructionIndex + 1; index < machineCodeInfo.length(); index++) {
                        if (machineCodeInfo.isCall(index)) {
                            try {
                                vm().runToInstruction(machineCodeInfo.instructionLocation(index), false, false);
                            } catch (Exception exception) {
                                InspectorError.unexpected("Run to next call instruction (ignoring breakpoints) could not be performed.", exception);
                            }
                            break;
                        }
                    }
                }
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasThread() && focus().hasCodeLocation() && inspection().isVMReady());
        }
    }

    private final InspectorAction debugRunToNextCallAction = new DebugRunToNextCallAction(null);

    /**
     * @return Singleton Action that will resume execution in the VM, stopping at the next call instruction,
     * ignoring breakpoints; fails if there is no known call in the method containing the IP.
     */
    public final InspectorAction debugRunToNextCall() {
        return debugRunToNextCallAction;
    }

    /**
     * Action:  advance the currently selected thread in the VM until it reaches the selected instruction
     * or a breakpoint.
     */
    final class DebugRunToNextCallWithBreakpointsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Run to next call instruction";

        DebugRunToNextCallWithBreakpointsAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {

            final MaxCodeLocation maxCodeLocation = focus().codeLocation();
            if (maxCodeLocation != null && maxCodeLocation.hasAddress()) {
                final MaxCompilation compilation = vm().machineCode().findCompilation(maxCodeLocation.address());
                if (compilation != null) {
                    final MaxMachineCodeInfo machineCodeInfo = compilation.getMachineCodeInfo();
                    final int instructionIndex = machineCodeInfo.findInstructionIndex(maxCodeLocation.address());
                    for (int index = instructionIndex + 1; index < machineCodeInfo.length(); index++) {
                        if (machineCodeInfo.isCall(index)) {
                            try {
                                vm().runToInstruction(machineCodeInfo.instructionLocation(index), false, true);
                            } catch (Exception exception) {
                                InspectorError.unexpected("Run to next call instruction could not be performed.", exception);
                            }
                            break;
                        }
                    }
                }
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasThread() && focus().hasCodeLocation() && inspection().isVMReady());
        }
    }

    private final InspectorAction debugNextRunToCallWithBreakpointsAction = new DebugRunToNextCallWithBreakpointsAction(null);

    /**
     * @return Singleton Action that will resume execution in the VM, stopping at the first instruction after returning
     *         from the current frame of the currently selected thread, or at a breakpoint, whichever comes first.
     */
    public final InspectorAction debugRunToNextCallWithBreakpoints() {
        return debugNextRunToCallWithBreakpointsAction;
    }

    /**
     * Action:  advances the currently selected thread one step in the VM.
     */
    class DebugSingleStepAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Single instruction step";

        DebugSingleStepAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        public  void procedure() {
            final MaxThread thread = focus().thread();
            try {
                vm().singleStepThread(thread, false);
            } catch (Exception exception) {
                gui().errorMessage("Couldn't single step", exception.toString());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasThread() && inspection().isVMReady());
        }
    }

    private final InspectorAction debugSingleStepAction = new DebugSingleStepAction(null);

    /**
     * @return Singleton action that will single step the currently selected thread in the VM
     */
    public final InspectorAction debugSingleStep() {
        return debugSingleStepAction;
    }

    /**
     * Action:   resumes execution of the VM, stopping at the one immediately after the current
     *         instruction of the currently selected thread.
     */
    final class DebugStepOverAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Step over (ignoring breakpoints)";

        DebugStepOverAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            final MaxThread thread = focus().thread();
            try {
                vm().stepOver(thread, false, false);
            } catch (Exception exception) {
                gui().errorMessage("Step over (ignoring breakpoints) could not be performed.", exception.toString());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasThread() && inspection().isVMReady());
        }
    }

    private final InspectorAction debugStepOverAction = new DebugStepOverAction(null);

    /**
     * @return Singleton Action that will resume execution of the VM, stopping at the one immediately after the current
     *         instruction of the currently selected thread
     */
    public final InspectorAction debugStepOver() {
        return debugStepOverAction;
    }

    /**
     * Action:   resumes execution of the VM, stopping at the one immediately after the current
     *         instruction of the currently selected thread or at a breakpoint.
     */
    final class DebugStepOverWithBreakpointsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Step over";

        DebugStepOverWithBreakpointsAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            final MaxThread thread = focus().thread();
            try {
                vm().stepOver(thread, false, true);
            } catch (Exception exception) {
                gui().errorMessage("Step over could not be performed.", exception.toString());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasThread() && inspection().isVMReady());
        }
    }

    private final InspectorAction debugStepOverWithBreakpointsAction = new DebugStepOverWithBreakpointsAction(null);

    /**
     * @return Singleton Action that will resume execution of the VM, stopping at the one immediately after the current
     *         instruction of the currently selected thread
     */
    public final InspectorAction debugStepOverWithBreakpoints() {
        return debugStepOverWithBreakpointsAction;
    }

    /**
     * Action:  interactively invoke a method.
     */
    private class DebugInvokeMethodAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Invoke method";
        private final TeleClassMethodActor teleClassMethodActor;

        public DebugInvokeMethodAction(TeleClassMethodActor teleClassMethodActor, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.teleClassMethodActor =  teleClassMethodActor;
        }

        @Override
        public void procedure() {
            ClassMethodActor classMethodActor = teleClassMethodActor.classMethodActor();
            ReferenceValue receiver = null;

            if (classMethodActor instanceof VirtualMethodActor) {
                final String input = gui().inputDialog("Argument 0 (receiver, must be a reference to a " + classMethodActor.holder() + " or subclass, origin address in hex):", "");
                if (input == null) {
                    // User clicked cancel.
                    return;
                }
                receiver = vm().createReferenceValue(vm().makeReference(Pointer.fromLong(new BigInteger(input, 16).longValue())));
                final ClassActor dynamicClass = receiver.getClassActor();
                classMethodActor = dynamicClass.findClassMethodActor(classMethodActor.name, classMethodActor.descriptor());
            }
            final Value[] arguments = MethodArgsDialog.getArgs(inspection(), classMethodActor, receiver);
            if (arguments == null) {
                // User clicked cancel.
                return;
            }
            try {
                final Value returnValue = vm().interpretMethod(classMethodActor, arguments);
                gui().informationMessage("Method " + classMethodActor.name + " returned " + returnValue.toString());
            } catch (InvocationTargetException e) {
                InspectorError.unexpected("Interpreter failure", e);
            }
        }
    }

    /**
     * Creates an action that lets the user invoke a method interactively.
     *
     * @param teleClassMethodActor representation of a method in the VM
     * @param actionTitle name of the action for display on menu or button
     * @return an interactive action for method invocation
     */
    public InspectorAction debugInvokeMethod(TeleClassMethodActor teleClassMethodActor, String actionTitle) {
        return new DebugInvokeMethodAction(teleClassMethodActor, actionTitle);
    }

    /**
     * Creates an action that lets the user invoke a method interactively.
     *
     * @param teleClassMethodActor representation of a method in the VM
     * @return an interactive action for method invocation
     */
    public InspectorAction debugInvokeMethod(TeleClassMethodActor teleClassMethodActor) {
        return new DebugInvokeMethodAction(teleClassMethodActor, null);
    }

    /**
     * Action:  lists to the console this history of the VM state.
     */
    final class ListVMStateHistoryAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "List VM state history";

        ListVMStateHistoryAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            vm().state().writeSummary(System.out);
        }
    }

    private InspectorAction listVMStateHistory = new ListVMStateHistoryAction(null);

    /**
     * @return an Action that will list to the console the history of the VM state
     */
    public final InspectorAction listVMStateHistory() {
        return listVMStateHistory;
    }

    /**
     * Action:  lists to the console all existing threads.
     */
    final class ListThreadsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "List all threads";

        ListThreadsAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            vm().threadManager().writeSummary(System.out);
        }
    }

    private InspectorAction listThreads = new ListThreadsAction(null);

    /**
     * @return an Action that will list to the console a description of every thread.
     */
    public final InspectorAction listThreads() {
        return listThreads;
    }

    /**
     * Action:  lists to the console the stack frames in the currently focused thread.
     */
    final class ListStackFrames extends InspectorAction {

        private static final String DEFAULT_TITLE = "List current thread's stack";

        ListStackFrames(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            final MaxThread thread = focus().thread();
            if (thread != null) {
                thread.stack().writeSummary(System.out);
            }
        }
    }

    private InspectorAction listStackFrames = new ListStackFrames(null);

    /**
     * @return an Action that will list to the console the history of the VM state
     */
    public final InspectorAction listStackFrames() {
        return listStackFrames;
    }

    /**
     * Action:  lists to the console all method compilations and native functions.
     */
    final class ListMachineCodeAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "List compilations & native functions";

        ListMachineCodeAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            vm().machineCode().writeSummary(System.out);
        }
    }

    private InspectorAction listMachineCode = new ListMachineCodeAction(null);

    /**
     * @return an Action that will list to the console the entries in the {@link MaxCodeCache}.
     */
    public final InspectorAction listMachineCode() {
        return listMachineCode;
    }

    /**
     * Action:  lists all method compilations and native functions to an interactively specified file.
     */
    final class ListMachineCodeToFileAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "List compilations & native functions to a file...";

        ListMachineCodeToFileAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            //Create a file chooser
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
            fileChooser.setDialogTitle("Save machine code summary to file:");
            final int returnVal = fileChooser.showSaveDialog(gui().frame());
            if (returnVal != JFileChooser.APPROVE_OPTION) {
                return;
            }
            final File file = fileChooser.getSelectedFile();
            if (file.exists() && !gui().yesNoDialog("File " + file + "exists.  Overwrite?\n")) {
                return;
            }
            try {
                final PrintStream printStream = new PrintStream(new FileOutputStream(file, false));
                vm().machineCode().writeSummary(printStream);
            } catch (FileNotFoundException fileNotFoundException) {
                gui().errorMessage("Unable to open " + file + " for writing:" + fileNotFoundException);
            }
        }
    }

    private InspectorAction listMachineCodeToFile = new ListMachineCodeToFileAction(null);

    /**
     * @return an interactive Action that will list to a specified file the entries in the {@link MaxCodeCache}.
     */
    public final InspectorAction listMachineCodeToFile() {
        return listMachineCodeToFile;
    }

    /**
     * Action:  lists to the console all existing breakpoints.
     */
    final class ListBreakpointsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "List all breakpoints";

        ListBreakpointsAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            vm().breakpointManager().writeSummary(System.out);
        }
    }

    private InspectorAction listBreakpoints = new ListBreakpointsAction(null);

    /**
     * @return an Action that will list to the console a summary of breakpoints in the VM.
     */
    public final InspectorAction listBreakpoints() {
        return listBreakpoints;
    }

    /**
     * Action:  lists to the console all existing breakpoints.
     */
    final class ListInspectableMethodsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "List inspectable methods for menu";

        ListInspectableMethodsAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            System.out.println("Inspectable Methods for menu:");
            for (MaxCodeLocation codeLocation : vm().inspectableMethods()) {
                codeLocation.hasAddress();
                System.out.println("\t" + codeLocation.description() + ", " + codeLocation.toString());
            }
        }
    }

    private InspectorAction listInspectableMethods = new ListInspectableMethodsAction(null);

    /**
     * @return an Action that will list to the console a summary of breakpoints in the VM.
     */
    public final InspectorAction listInspectableMethods() {
        return listInspectableMethods;
    }

    /**
     * Action:  lists to the console all existing watchpoints.
     */
    final class ListWatchpointsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "List all watchpoints";

        ListWatchpointsAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            vm().watchpointManager().writeSummary(System.out);
        }
    }

    private InspectorAction listWatchpoints = new ListWatchpointsAction(null);

    /**
     * @return an Action that will list to the console a summary of watchpoints in the VM.
     */
    public final InspectorAction listWatchpoints() {
        return listWatchpoints;
    }

    /**
     * Action:  lists to the console current settings.
     */
    final class ListSettingsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "List all settings";

        ListSettingsAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            inspection().settings().writeSummary(System.out);
        }
    }

    private InspectorAction listSettings = new ListSettingsAction(null);

    /**
     * @return an Action that will list to the console a summary of current settings
     * in the inspection session.
     */
    public final InspectorAction listSettings() {
        return listSettings;
    }

    /**
     * @return menu items for memory-related actions that are independent of context
     */
    public InspectorMenuItems genericMemoryMenuItems() {
        return new AbstractInspectorMenuItems(inspection()) {
            public void addTo(InspectorMenu menu) {
                menu.add(viewMemoryAllocationsMenu());
                menu.add(views().memory().viewMenu());
                if (vm().heap().providesHeapRegionInfo()) {
                    menu.add(viewHeapRegionInfoMenu());
                }
                menu.add(views().memoryBytes().viewMenu());
            }
        };
    }

    /**
     * @return menu items for code-related actions that are independent of context
     */
    public InspectorMenuItems genericCodeMenuItems() {
        return new AbstractInspectorMenuItems(inspection()) {
            public void addTo(InspectorMenu menu) {
                menu.add(actions().viewMethodCodeAtSelection());
                menu.add(actions().viewMethodCodeAtIP());
                menu.add(actions().viewMethodMachineCode());
                final JMenu byNameSub = new JMenu("View code by name");
                byNameSub.add(actions().viewMethodBytecodeByName());
                byNameSub.add(actions().viewMethodCompilationByName());
                byNameSub.add(actions().viewNativeFunctionByName());
                menu.add(byNameSub);
                menu.add(actions().viewMachineCodeByAddress());
                final JMenu bootMethodSub = new JMenu("View boot image method code");
                bootMethodSub.add(actions().viewRunMethodCodeInBootImage());
                bootMethodSub.add(actions().viewThreadRunMethodCodeInBootImage());
                menu.add(bootMethodSub);
            }
        };
    }

    /**
     * @return menu items for breakpoint-related actions that are independent of context
     */
    public InspectorMenuItems genericBreakpointMenuItems() {
        return new AbstractInspectorMenuItems(inspection()) {
            public void addTo(InspectorMenu menu) {

                final InspectorMenu builtinBreakpointsMenu = new BuiltinBreakpointsMenu("Break at builtin");
                menu.add(builtinBreakpointsMenu);

                final InspectorMenu methodEntryBreakpoints = new InspectorMenu("Break at method entry");
                methodEntryBreakpoints.add(actions().setMachineCodeBreakpointAtEntriesByName());
                methodEntryBreakpoints.add(actions().setBytecodeBreakpointAtMethodEntryByName());
                methodEntryBreakpoints.add(actions().setBytecodeBreakpointAtMethodEntryByKey());
                menu.add(methodEntryBreakpoints);

                final InspectorMenu breakAt = new InspectorMenu("Break at machine code");
                breakAt.add(actions().setMachineCodeBreakpointAtAddress());
                breakAt.add(actions().setNativeFunctionBreakpointByName());
                breakAt.add(actions().setMachineCodeBreakpointAtObjectInitializer());
                menu.add(breakAt);

                final InspectorMenu toggle = new InspectorMenu("Toggle breakpoint");
                toggle.add(actions().toggleMachineCodeBreakpoint());
                menu.add(toggle);

                menu.add(actions().removeAllBreakpoints());
            }
        };
    }

    /**
     * @return menu items for watchpoint-related actions that are independent of context
     */
    public InspectorMenuItems genericWatchpointMenuItems() {
        return new AbstractInspectorMenuItems(inspection()) {
            public void addTo(InspectorMenu menu) {
                menu.add(actions().setWordWatchpoint());
                menu.add(actions().removeAllWatchpoints());
            }
        };
    }

    /**
     * @return menu items for object-related actions that are independent of context
     */
    public InspectorMenuItems genericObjectMenuItems() {
        return new AbstractInspectorMenuItems(inspection()) {
            public void addTo(InspectorMenu menu) {

                final JMenu methodActorMenu = new JMenu("View method actor");
                methodActorMenu.add(viewMethodActorByName());
                menu.add(methodActorMenu);

                final JMenu classActorMenu = new JMenu("View class actor");
                classActorMenu.add(viewClassActorByName());
                classActorMenu.add(viewClassActorByHexId());
                classActorMenu.add(viewClassActorByDecimalId());
                classActorMenu.add(viewBootClassRegistry());
                menu.add(classActorMenu);

                final JMenu classStaticsMenu = new JMenu("View class statics");
                classStaticsMenu.add(viewStaticTupleByName());
                menu.add(classStaticsMenu);

                final JMenu classHubsMenu = new JMenu("View class hubs");
                classHubsMenu.add(viewDynamicHubByName());
                classHubsMenu.add(viewStaticHubByName());
                menu.add(classHubsMenu);

                menu.add(views().objects().viewMenu());
            }
        };
    }

    /**
     * @return menu items for view-related actions that are independent of context
     */
    public InspectorMenuItems genericViewMenuItems() {
        return new AbstractInspectorMenuItems(inspection()) {
            public void addTo(InspectorMenu menu) {
                menu.add(views().activateSingletonViewAction(ViewKind.ALLOCATIONS));
                menu.add(views().activateSingletonViewAction(ViewKind.BOOT_IMAGE));
                menu.add(views().activateSingletonViewAction(ViewKind.BREAKPOINTS));
                menu.add(views().activateSingletonViewAction(ViewKind.DEBUG_INFO));
                menu.add(views().memory().viewMenu());
                menu.add(views().memoryBytes().viewMenu());
                menu.add(views().activateSingletonViewAction(ViewKind.METHODS));
                menu.add(views().activateSingletonViewAction(ViewKind.NOTEPAD));
                menu.add(views().objects().viewMenu());
                menu.add(views().activateSingletonViewAction(ViewKind.REGISTERS));
                menu.add(views().activateSingletonViewAction(ViewKind.STACK));
                menu.add(views().activateSingletonViewAction(ViewKind.STACK_FRAME));
                menu.add(views().activateSingletonViewAction(ViewKind.THREADS));
                menu.add(views().activateSingletonViewAction(ViewKind.THREAD_LOCALS));
                menu.add(views().activateSingletonViewAction(ViewKind.WATCHPOINTS));
                menu.add(views().activateSingletonViewAction(ViewKind.VMLOG));
            }
        };
    }

}

