/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.java.*;
import com.sun.max.ins.memory.*;
import com.sun.max.ins.method.*;
import com.sun.max.ins.object.*;
import com.sun.max.ins.type.*;
import com.sun.max.ins.util.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.MaxMachineCode.InstructionMap;
import com.sun.max.tele.MaxWatchpointManager.MaxDuplicateWatchpointException;
import com.sun.max.tele.MaxWatchpointManager.MaxTooManyWatchpointsException;
import com.sun.max.tele.interpreter.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.layout.Layout.HeaderField;
import com.sun.max.vm.reference.*;
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
 * <li> {@code final class DoSometingAction extends InspectorAction}</li>
 * <li> The Action classes are in package scope so that they can be used by {@link InspectorKeyBindings}.</li>
 * <li> Add a title:  {@code private static final DEFAULT_NAME = "do something"}.</li>
 * <li> If the
 * action is interactive, for example if it produces a dialog, then the name should conclude with "...".
 * Capitalize the first word of the title but not the others, except for distinguished names such as
 * "Inspector" and acronyms.</li>
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
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 * @author Aritra Bandyopadhyay
 */
public class InspectionActions extends AbstractInspectionHolder implements Prober{

    private static final int TRACE_VALUE = 2;

    /**
     * Name of the Action for searching in an Inspector view.
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
     * Action: causes a specific Inspector to become visible,
     * even if hidden or iconic.
     */
    final class ShowViewAction extends InspectorAction {

        private final Inspector inspector;

        ShowViewAction(Inspector inspector) {
            super(inspection(), inspector.getTextForTitle());
            this.inspector = inspector;
        }

        @Override
        protected void procedure() {
            inspector.highlight();
        }

    }

    /**
     * @return an action that will make a specified inspector become
     * visible, even if hidden or iconic.
     */
    public final InspectorAction showView(Inspector inspector) {
        return new ShowViewAction(inspector);
    }

    /**
     * Action:  close all open inspectors that match a predicate.
     */
    final class CloseViewsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Close views";

        private final Predicate<Inspector> predicate;

        CloseViewsAction(Predicate<Inspector> predicate, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.predicate = predicate;
        }

        @Override
        protected void procedure() {
            gui().removeInspectors(predicate);
        }
    }

    /**
     * Closes views matching a specified criterion.
     *
     * @param predicate a predicate that returns true for all Inspectors to be closed.
     * @param actionTitle a string name for the Action, uses default name if null.
     * @return an Action that will close views.
     */
    public final InspectorAction closeViews(Predicate<Inspector> predicate, String actionTitle) {
        return new CloseViewsAction(predicate, actionTitle);
    }

    /**
     * Closes all views of a specified type, optionally with an exception.
     *
     * @param inspectorType the type of Inspectors to be closed
     * @param exceptInspector an inspector that should not be closed
     * @param actionTitle a string name for the Action
     * @return an action that will close views
     */
    public final InspectorAction closeViews(final Class<? extends Inspector> inspectorType, final Inspector exceptInspector, String actionTitle) {
        Predicate<Inspector> predicate = new Predicate<Inspector>() {
            public boolean evaluate(Inspector inspector) {
                return inspectorType.isAssignableFrom(inspector.getClass()) && inspector != exceptInspector;
            }
        };
        return new CloseViewsAction(predicate, actionTitle);
    }

    /**
     * Action:  closes all open inspectors.
     */
    final class CloseAllViewsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Close all views";

        CloseAllViewsAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            gui().removeInspectors(Predicate.Static.alwaysTrue(Inspector.class));
        }
    }

    private final InspectorAction closeAllViewsAction = new CloseAllViewsAction(null);

    /**
     * @return Singleton Action that closes all open inspectors.
     */
    public final InspectorAction closeAllViews() {
        return closeAllViewsAction;
    }

    /**
     * Action:  quits inspector.
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
     * Action:  relocates the boot image, assuming that the inspector was invoked
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
     * @return Singleton Action that relocates the boot image, assuming that the inspector was invoked
     * with the option {@link MaxineInspector#suspendingBeforeRelocating()} set.
     */
    public final InspectorAction relocateBootImage() {
        return relocateBootImageAction;
    }

    /**
     * Action:  sets level of trace output in inspector code.
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
     * @return Singleton interactive Action that permits setting the level of inspector {@link Trace} output.
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
            vm().classRegistry().updateLoadableTypeDescriptorsFromClasspath();
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
     * Action:  makes visible and highlights the {@link BootImageInspector}.
     */
    final class ViewBootImageAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Boot image info";

        ViewBootImageAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            BootImageInspector.make(inspection()).highlight();
        }
    }

    private InspectorAction viewBootImage = new ViewBootImageAction(null);

    /**
     * @return an Action that will make visible the {@link BootImageInspector}.
     */
    public final InspectorAction viewBootImage() {
        return viewBootImage;
    }

    /**
     * Action:  makes visible and highlights the {@link BreakpointsInspector}.
     */
    final class ViewBreakpointsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Breakpoints";

        ViewBreakpointsAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            BreakpointsInspector.make(inspection()).highlight();
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess());
        }
    }

    private InspectorAction viewBreakpoints = new ViewBreakpointsAction(null);

    /**
     * @return an Action that will make visible the {@link BreakpointsInspector}.
     */
    public final InspectorAction viewBreakpoints() {
        return viewBreakpoints;
    }

    /**
     * Action:  makes visible and highlights the {@link MemoryRegionsInspector}.
     */
    final class ViewMemoryRegionsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Memory regions";

        ViewMemoryRegionsAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            MemoryRegionsInspector.make(inspection()).highlight();
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess());
        }
    }

    private InspectorAction viewMemoryRegions = new ViewMemoryRegionsAction(null);

    /**
     * @return an Action that will make visible the {@link MemoryRegionsInspector}.
     */
    public final InspectorAction viewMemoryRegions() {
        return viewMemoryRegions;
    }

    /**
     * Action:  makes visible the {@link MethodInspector}.
     */
    final class ViewMethodCodeAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Method code";

        ViewMethodCodeAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            focus().setCodeLocation(focus().thread().ipLocation());
        }
    }

    private InspectorAction viewMethodCode = new ViewMethodCodeAction(null);

    /**
     * @return an Action that will make visible the {@link MethodInspector}, with
     * initial view set to the method containing the instruction pointer of the current thread.
     */
    public final InspectorAction viewMethodCode() {
        return viewMethodCode;
    }

    /**
     * Action:  makes visible and highlights the {@link RegistersInspector}.
     */
    final class ViewRegistersAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Registers";

        ViewRegistersAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            RegistersInspector.make(inspection()).highlight();
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess() && focus().hasThread());
        }
    }

    private InspectorAction viewRegisters = new ViewRegistersAction(null);

    /**
     * @return an Action that will make visible the {@link RegistersInspector}.
     */
    public final InspectorAction viewRegisters() {
        return viewRegisters;
    }

    /**
     * Action:  makes visible and highlights the {@link StackInspector}.
     */
    final class ViewStackAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Stack";

        ViewStackAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            StackInspector.make(inspection()).highlight();
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess() && focus().hasThread());
        }
    }

    private InspectorAction viewStack = new ViewStackAction(null);

    /**
     * @return an Action that will make visible the {@link StackInspector}.
     */
    public final InspectorAction viewStack() {
        return viewStack;
    }

    /**
     * Action:  makes visible and highlights the {@link ThreadsInspector}.
     */
    final class ViewThreadsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Threads";

        ViewThreadsAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            ThreadsInspector.make(inspection()).highlight();
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess());
        }
    }

    private InspectorAction viewThreads = new ViewThreadsAction(null);

    /**
     * @return an Action that will make visible the {@link ThreadsInspector}.
     */
    public final InspectorAction viewThreads() {
        return viewThreads;
    }

    /**
     * Action:  makes visible and highlights the {@link ThreadLocalsInspector}.
     */
    final class ViewTLAAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "VM thread locals";

        ViewTLAAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            ThreadLocalsInspector.make(inspection()).highlight();
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess() && focus().hasThread());
        }
    }

    private InspectorAction viewTLA = new ViewTLAAction(null);

    /**
     * @return an Action that will make visible the {@link ThreadsInspector}.
     */
    public final InspectorAction viewVmThreadLocals() {
        return viewTLA;
    }

    /**
     * Action:  makes visible and highlights the {@link WatchpointsInspector}.
     */
    final class ViewWatchpointsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Watchpoints";

        ViewWatchpointsAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            WatchpointsInspector.make(inspection()).highlight();
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(vm().watchpointManager() != null);
        }
    }

    private InspectorAction viewWatchpoints = new ViewWatchpointsAction(null);

    /**
     * @return an Action that will make visible the {@link WatchpointsInspector}.
     */
    public final InspectorAction viewWatchpoints() {
        return viewWatchpoints;
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
     * @param a {@link Word} wrapped as a {@link Value} from the VM.
     * @param actionTitle a string to use as the title of the action, uses default name if null.
     * @return an Action that copies the word's text value in hex to the system clipboard,
     * null if not a word.
     */
    public final InspectorAction copyValue(Value value, String actionTitle) {
        Word word = Word.zero();
        try {
            word = value.asWord();
        } catch (Throwable throwable) {
        }
        final InspectorAction action = new CopyWordAction(word, actionTitle);
        if (word.isZero()) {
            action.setEnabled(false);
        }
        return action;
    }

    /**
     * Action:  inspect a memory region, interactive if no location is specified.
     */
    final class InspectMemoryBytesAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect memory as bytes";
        private final Address address;
        private final TeleObject teleObject;

        InspectMemoryBytesAction() {
            super(inspection(), "Inspect memory bytes at address...");
            this.address = null;
            this.teleObject = null;
            refreshableActions.add(this);
        }

        InspectMemoryBytesAction(Address address, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.address = address;
            this.teleObject = null;
        }

        InspectMemoryBytesAction(TeleObject teleObject, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.address = null;
            this.teleObject = teleObject;
        }

        @Override
        protected void procedure() {
            if (teleObject != null) {
                MemoryBytesInspector.create(inspection(), teleObject).highlight();
            } else if (address != null) {
                MemoryBytesInspector.create(inspection(), address).highlight();
            } else {
                new AddressInputDialog(inspection(), vm().bootImageStart(), "Inspect memory bytes at address...", "Inspect") {
                    @Override
                    public void entered(Address address) {
                        MemoryBytesInspector.create(inspection(), address).highlight();
                    }
                };
            }
        }
    }

    private final InspectorAction inspectMemoryBytesAction = new InspectMemoryBytesAction();

    /**
     * @return Singleton interactive Action that will create a Memory Inspector
     */
    public final InspectorAction inspectMemoryBytes() {
        return inspectMemoryBytesAction;
    }

    /**
     * @param address a valid memory {@link Address} in the VM
     * @param actionTitle a string name for the action, uses default name if null
     * @return an interactive Action that will create a Memory Inspector at the address
     */
    public final InspectorAction inspectMemoryBytes(Address address, String actionTitle) {
        return new InspectMemoryBytesAction(address, actionTitle);
    }

    /**
     * Menu: display a sub-menu of commands to make visible
     * existing memory words inspectors.  It includes a command
     * that closes all of them.
     */
    final class MemoryWordsInspectorsMenu extends JMenu {
        public MemoryWordsInspectorsMenu() {
            super("Memory inspectors");
            addMenuListener(new MenuListener() {

                public void menuCanceled(MenuEvent e) {
                }

                public void menuDeselected(MenuEvent e) {
                }

                public void menuSelected(MenuEvent e) {
                    removeAll();
                    final Set<MemoryWordsInspector> inspectors = inspection().memoryWordsInspectors();
                    if (inspectors.size() > 0) {
                        final Set<MemoryWordsInspector> sortedSet  =  new TreeSet<MemoryWordsInspector>(new Comparator<MemoryWordsInspector>() {
                            public int compare(MemoryWordsInspector inspector1, MemoryWordsInspector inspector2) {
                                final Long startLocation1 = inspector1.getCurrentMemoryRegion().start().toLong();
                                final Long startLocation2 = inspector2.getCurrentMemoryRegion().start().toLong();
                                return startLocation1.compareTo(startLocation2);
                            }
                        });
                        for (MemoryWordsInspector inspector : inspectors) {
                            sortedSet.add(inspector);
                        }
                        for (MemoryWordsInspector inspector : sortedSet) {
                            add(actions().showView(inspector));
                        }
                        addSeparator();
                        add(actions().closeViews(MemoryWordsInspector.class, null, "Close all memory inspectors"));
                    }
                }
            });
        }
    }

    /**
     * Creates a menu of actions to make visible existing memory words inspectors.
     * <br>
     * <strong>Note:</strong> This menu does not depend on context, so it would be natural to use
     * a singleton to be shared among all uses.  Unfortunately, that does not seem to work.
     *
     * @return a dynamically populated menu that contains an action to make visible each
     * existing memory words inspector, even if hidden or iconic.
     */
    public final JMenu memoryWordsInspectorsMenu() {
        return new MemoryWordsInspectorsMenu();
    }

    /**
     * Action:   inspect a memory region, interactive if no location specified.
     */
    final class InspectMemoryWordsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect memory";
        private final Address address;
        private final MaxMemoryRegion memoryRegion;

        InspectMemoryWordsAction() {
            super(inspection(), "Inspect memory at address...");
            this.address = null;
            this.memoryRegion = null;
        }

        InspectMemoryWordsAction(MaxMemoryRegion memoryRegion, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.address = null;
            this.memoryRegion = memoryRegion;
        }

        InspectMemoryWordsAction(Address address, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.address = address;
            this.memoryRegion = null;
        }

        @Override
        protected void procedure() {
            if (memoryRegion != null) {
                final Inspector inspector = new MemoryWordsInspector(inspection(), memoryRegion, memoryRegion.regionName());
                inspector.highlight();
            } else  if (address != null) {
                final Inspector inspector = new MemoryWordsInspector(inspection(), new InspectorMemoryRegion(vm(), "", address, vm().platform().nBytesInWord() * 10));
                inspector.highlight();
            } else {
                new AddressInputDialog(inspection(), vm().bootImageStart(), "Inspect memory at address...", "Inspect") {

                    @Override
                    public void entered(Address address) {
                        final Inspector inspector = new MemoryWordsInspector(inspection(), new InspectorMemoryRegion(vm(), "", address, vm().platform().nBytesInWord() * 10));
                        inspector.highlight();
                    }
                };
            }
        }
    }

    private final InspectorAction inspectMemoryWordsAction = new InspectMemoryWordsAction();

    /**
     * @return Singleton interactive Action that will create a MemoryWords Inspector
     */
    public final InspectorAction inspectMemoryWords() {
        return inspectMemoryWordsAction;
    }

    public final InspectorAction inspectMemoryWords(MaxMemoryRegion memoryRegion) {
        return new InspectMemoryWordsAction(memoryRegion, null);
    }

    /**
     * @param address a valid memory {@link Address} in the VM
     * @param actionTitle a name for the action
     * @return an Action that will create a Memory Words Inspector at the address
     */
    public final InspectorAction inspectMemoryWords(Address address, String actionTitle) {
        return new InspectMemoryWordsAction(address, actionTitle);
    }

    /**
     * @param address a valid memory {@link Address} in the VM
     * @return an Action that will create a Memory Words Inspector at the address
     */
    public final InspectorAction inspectMemoryWords(Address address) {
        return new InspectMemoryWordsAction(address, null);
    }

    /**
     * Menu: display a sub-menu of commands to inspect the basic allocation
     * regions of the VM.
     */
    final class InspectMemoryRegionsMenu extends JMenu {
        public InspectMemoryRegionsMenu() {
            super("Inspect memory region");
            addMenuListener(new MenuListener() {

                public void menuCanceled(MenuEvent e) {
                }

                public void menuDeselected(MenuEvent e) {
                }

                public void menuSelected(MenuEvent e) {
                    removeAll();
                    final SortedSet<MaxMemoryRegion> regionSet = new TreeSet<MaxMemoryRegion>(MaxMemoryRegion.Util.nameComparator());
                    regionSet.addAll(vm().state().memoryRegions());
                    for (MaxMemoryRegion memoryRegion : regionSet) {
                        //System.out.println(memoryRegion.toString());
                        add(actions().inspectRegionMemoryWords(memoryRegion, memoryRegion.regionName(), memoryRegion.regionName()));
                    }
                }
            });
        }
    }

    /**
     * Creates a menu of actions to inspect memory regions.
     * <br>
     * <strong>Note:</strong> This menu does not depend on context, so it would be natural to use
     * a singleton to be shared among all uses.  Unfortunately, that does not seem to work.
     *
     * @return a dynamically populated menu that contains an action to inspect each currently allocated
     * region of memory in the VM.
     */
    public final JMenu inspectMemoryRegionsMenu() {
        return new InspectMemoryRegionsMenu();
    }

    /**
     * Action: inspects memory occupied by an object.
     */

    final class InspectObjectMemoryWordsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect memory";
        private final TeleObject teleObject;

        InspectObjectMemoryWordsAction(TeleObject teleObject, String actionTitle) {
            super(inspection(), (actionTitle == null) ? DEFAULT_TITLE : actionTitle);
            this.teleObject = teleObject;
        }

        @Override
        protected void procedure() {
            final Inspector inspector = new MemoryWordsInspector(inspection(), teleObject);
            inspector.highlight();
        }
    }

    /**
     * @param teleObject a surrogate for a valid object in the VM
     * @param actionTitle a name for the action
     * @return an Action that will create a Memory Words Inspector at the address
     */
    public final InspectorAction inspectObjectMemoryWords(TeleObject teleObject, String actionTitle) {
        return new InspectObjectMemoryWordsAction(teleObject, actionTitle);
    }

    /**
     * @param teleObject a surrogate for a valid object in the VM
     * @return an Action that will create a Memory Words Inspector at the address
     */
    public final InspectorAction inspectObjectMemoryWords(TeleObject teleObject) {
        return new InspectObjectMemoryWordsAction(teleObject, null);
    }

    /**
     * Action:  inspect the memory holding a block of machine code.
     */
    final class InspectTargetRegionMemoryWordsAction extends InspectorAction {

        private static final String  DEFAULT_TITLE = "Inspect Target Code memory region";
        private final MaxMachineCode machineCode;

        private InspectTargetRegionMemoryWordsAction(MaxMachineCode machineCode, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.machineCode = machineCode;
        }

        @Override
        protected void procedure() {
            final String description = machineCode.entityName();
            actions().inspectRegionMemoryWords(machineCode.memoryRegion(), description).perform();
        }
    }

    /**
     * Creates an action that will inspect memory containing a block of machine code.
     *
     * @param machineCode a block of machine code in the VM, either a Java method or external native
     * @param actionTitle a name for the action
     * @return an Action that will create a Memory Words Inspector for the code
     */
    public final InspectorAction inspectTargetRegionMemoryWords(MaxMachineCode machineCode, String actionTitle) {
        return new InspectTargetRegionMemoryWordsAction(machineCode, actionTitle);
    }

    /**
     * Creates an action that will inspect memory containing a block of target code.
     *
     * @param machineCode a block of target code in the VM, either a Java method or native
     * @return an Action that will create a Memory Words Inspector for the code
     */
    public final InspectorAction inspectTargetRegionMemoryWords(MaxMachineCode machineCode) {
        return new InspectTargetRegionMemoryWordsAction(machineCode, null);
    }

    /**
     *Action:  inspect the memory allocated to the currently selected thread.
     */
    final class InspectSelectedThreadMemoryWordsAction extends InspectorAction {

        public InspectSelectedThreadMemoryWordsAction(String actionTitle) {
            super(inspection(), actionTitle == null ? "Inspect memory for selected thread" : actionTitle);
        }

        @Override
        protected void procedure() {
            final MaxThread thread = focus().thread();
            if (thread != null) {
                final Inspector inspector = new MemoryWordsInspector(inspection(), thread.stack().memoryRegion(), "Thread " + thread.toShortString());
                inspector.highlight();
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
     * @return an action that will create a memory words inspector
     * for memory allocated by the currently selected thread
     */
    public final InspectorAction inspectSelectedThreadMemoryWords(String actionTitle) {
        return new InspectSelectedThreadMemoryWordsAction(actionTitle);
    }

    /**
     *Action:  inspect the memory allocated to the currently selected memory watchpoint.
     */
    final class InspectSelectedMemoryWatchpointWordsAction extends InspectorAction {

        public InspectSelectedMemoryWatchpointWordsAction(String actionTitle) {
            super(inspection(), actionTitle == null ? "Inspect memory at selected watchpoint" : actionTitle);
        }

        @Override
        protected void procedure() {
            final MaxWatchpoint watchpoint = focus().watchpoint();
            if (watchpoint != null) {
                final Inspector inspector =
                    new MemoryWordsInspector(inspection(), watchpoint.memoryRegion(), "Watchpoint " + watchpoint.description());
                inspector.highlight();
            } else {
                gui().errorMessage("no watchpoint selected");
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasWatchpoint());
        }
    }

    private final InspectorAction inspectSelectedMemoryWatchpointWordsAction = new InspectSelectedMemoryWatchpointWordsAction(null);

    /**
     * @return Singleton action that will create a memory words inspector
     * for memory allocated by the currently selected thread
     */
    public final InspectorAction inspectSelectedMemoryWatchpointWordsAction() {
        return inspectSelectedMemoryWatchpointWordsAction;
    }

    /**
     * Action: inspect a named region as memory words.
     */
    final class InspectRegionMemoryWordsAction extends InspectorAction {

        private final MaxMemoryRegion memoryRegion;
        private final String regionName;

        InspectRegionMemoryWordsAction(MaxMemoryRegion memoryRegion, String regionName, String actionTitle) {
            super(inspection(), actionTitle == null ? ("Inspect memory region \"" + regionName + "\"") : actionTitle);
            this.memoryRegion = memoryRegion;
            this.regionName = regionName;
            refreshableActions.add(this);
            refresh(true);
        }

        @Override
        protected void procedure() {
            final Inspector inspector = new MemoryWordsInspector(inspection(), memoryRegion, regionName);
            inspector.highlight();
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(!memoryRegion.start().isZero());
        }
    }

    /**
     * @return an Action that will create a Memory Words Inspector for the boot heap region.
     */
    public final InspectorAction inspectBootHeapMemoryWords() {
        return new InspectRegionMemoryWordsAction(vm().heap().bootHeapRegion().memoryRegion(), "Heap-Boot", null);
    }

    /**
     * @return an Action that will create a Memory Words Inspector for the immortal heap region.
     */
    public final InspectorAction inspectImmortalHeapMemoryWords() {
        return new InspectRegionMemoryWordsAction(vm().heap().immortalHeapRegion().memoryRegion(), "Heap-Immortal", null);
    }

    /**
     * @return an Action that will create a Memory Words Inspector for the boot code region.
     */
    public final InspectorAction inspectBootCodeMemoryWords() {
        return new InspectRegionMemoryWordsAction(vm().codeCache().bootCodeRegion().memoryRegion(), "Heap-Code", null);
    }

    /**
     * Creates a MemoryWords Inspector for a named region of memory.
     *
     * @param memoryRegion a region of memory in the VM
     * @param regionName the name of the region to display
     * @param actionTitle the name of the action that will create the display, default title if null
     * @return an action that will create a Memory Words Inspector for the region
     */
    public final InspectorAction inspectRegionMemoryWords(MaxMemoryRegion memoryRegion, String regionName, String actionTitle) {
        final String title = (actionTitle == null) ? ("Inspect memory region \"" + regionName + "\"") : actionTitle;
        return new InspectRegionMemoryWordsAction(memoryRegion, regionName, title);
    }

    /**
     * Creates a MemoryWords Inspector for a named region of memory.
     *
     * @param memoryRegion a region of memory in the VM
     * @param regionName the name of the region to display
     * @return an action that will create a Memory Words Inspector for the region
     */
    public final InspectorAction inspectRegionMemoryWords(MaxMemoryRegion memoryRegion, String regionName) {
        return new InspectRegionMemoryWordsAction(memoryRegion, regionName, null);
    }

    /**
     * Action:  creates a memory inspector for the currently selected memory region, if any.
     */

    final class InspectSelectedMemoryRegionWordsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect selected memory region";

        InspectSelectedMemoryRegionWordsAction() {
            super(inspection(), DEFAULT_TITLE);
            refreshableActions.add(this);
            refresh(true);
        }

        @Override
        protected void procedure() {
            final MaxMemoryRegion memoryRegion = focus().memoryRegion();
            if (memoryRegion != null) {
                final Inspector inspector = new MemoryWordsInspector(inspection(), memoryRegion, memoryRegion.regionName());
                inspector.highlight();
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasMemoryRegion());
        }
    }

    private final InspectorAction inspectSelectedMemoryRegionWordsAction = new InspectSelectedMemoryRegionWordsAction();

    /**
     * @return Singleton Action that will create a Memory inspector for the currently selected region of memory
     */
    public final InspectorAction inspectSelectedMemoryRegionWords() {
        return inspectSelectedMemoryRegionWordsAction;
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
     * Action: create an Object Inspector, interactively specified by address..
     */
    final class InspectObjectAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect object at address...";

        InspectObjectAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            new AddressInputDialog(inspection(), vm().heap().bootHeapRegion().memoryRegion().start(), "Inspect object at address...", "Inspect") {

                @Override
                public void entered(Address address) {
                    try {
                        final Pointer pointer = address.asPointer();
                        if (vm().isValidOrigin(pointer)) {
                            final Reference objectReference = vm().originToReference(pointer);
                            final TeleObject teleObject = vm().heap().findTeleObject(objectReference);
                            focus().setHeapObject(teleObject);
                        } else {
                            gui().errorMessage("heap object not found at "  + address.to0xHexString());
                        }
                    } catch (MaxVMBusyException maxVMBusyException) {
                        inspection().announceVMBusyFailure(name());
                    }
                }
            };
        }
    }

    /**
     * Menu: display a sub-menu of commands to make visible
     * existing object inspectors.  It includes a command
     * that closes all of them.
     */
    final class ObjectInspectorsMenu extends JMenu {
        public ObjectInspectorsMenu() {
            super("Object inspectors");
            addMenuListener(new MenuListener() {

                public void menuCanceled(MenuEvent e) {
                }

                public void menuDeselected(MenuEvent e) {
                }

                public void menuSelected(MenuEvent e) {
                    removeAll();
                    final Set<ObjectInspector> inspectors = inspection().objectInspectors();
                    if (inspectors.size() > 0) {
                        for (ObjectInspector objectInspector : inspection().objectInspectors()) {
                            add(actions().showView(objectInspector));
                        }
                        addSeparator();
                        add(actions().closeViews(ObjectInspector.class, null, "Close all object inspectors"));
                    }
                }
            });
        }
    }

    /**
     * Creates a menu of actions to make visible existing object inspectors.
     * <br>
     * <strong>Note:</strong> This menu does not depend on context, so it would be natural to use
     * a singleton to be shared among all uses.  Unfortunately, that does not seem to work.
     *
     * @return a dynamically populated menu that contains an action to make visible each
     * existing object inspector, even if hidden or iconic.
     */
    public final JMenu objectInspectorsMenu() {
        return new ObjectInspectorsMenu();
    }

    private final InspectorAction inspectObjectAction = new InspectObjectAction(null);

    /**
     * @return Singleton Action that will create an Object Inspector interactively, prompting the user for a numeric object ID
     */
    public final InspectorAction inspectObject() {
        return inspectObjectAction;
    }

    /**
     * Action:  creates an inspector for a specific heap object in the VM.
     */
    final class InspectSpecifiedObjectAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect object";
        final TeleObject teleObject;

        InspectSpecifiedObjectAction(TeleObject teleObject, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.teleObject = teleObject;
        }

        @Override
        protected void procedure() {
            focus().setHeapObject(teleObject);
        }
    }

    /**
     * @param surrogate for a heap object in the VM.
     * @param actionTitle a string name for the Action, uses default name if null
     * @return an Action that will create an Object Inspector
     */
    public final InspectorAction inspectObject(TeleObject teleObject, String actionTitle) {
        return new InspectSpecifiedObjectAction(teleObject, actionTitle);
    }

    /**
     * Action: create an Object Inspector, interactively specified by the inspector's OID.
     */
    final class InspectObjectByIDAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect object by ID...";

        InspectObjectByIDAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            final String input = gui().inputDialog("Inspect object by ID..", "");
            if (input == null) {
                // User clicked cancel.
                return;
            }
            try {
                final long oid = Long.parseLong(input);
                final TeleObject teleObject = vm().heap().findObjectByOID(oid);
                if (teleObject != null) {
                    focus().setHeapObject(teleObject);
                } else {
                    gui().errorMessage("failed to find heap object for ID: " + input);
                }
            } catch (NumberFormatException numberFormatException) {
                gui().errorMessage("Not a ID: " + input);
            }
        }
    }

    private final InspectorAction inspectObjectByIDAction = new InspectObjectByIDAction(null);

    /**
     * @return Singleton Action that will create an Object Inspector interactively, prompting the user for a numeric object ID
     */
    public final InspectorAction inspectObjectByID() {
        return inspectObjectByIDAction;
    }

    /**
     * Action: create an Object Inspector for the boot {@link ClassRegistry} in the VM.
     */
    final class InspectBootClassRegistryAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect boot class registry";

        InspectBootClassRegistryAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            try {
                final TeleObject teleBootClassRegistry = vm().heap().findTeleObject(vm().bootClassRegistryReference());
                focus().setHeapObject(teleBootClassRegistry);
            } catch (MaxVMBusyException maxVMBusyException) {
                inspection().announceVMBusyFailure(name());
            }
        }
    }

    private final InspectorAction inspectBootClassRegistryAction = new InspectBootClassRegistryAction(null);

    /**
     * @return Singleton action that will create an Object Inspector for the boot {@link ClassRegistry} in the VM.
     */
    public final InspectorAction inspectBootClassRegistry() {
        return inspectBootClassRegistryAction;
    }

    /**
     * Action:  inspect a {@link ClassActor} object for an interactively named class loaded in the VM,
     * specified by class name.
     */
    final class InspectClassActorByNameAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect ClassActor by name...";

        InspectClassActorByNameAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(inspection(), "Inspect ClassActor ...", "Inspect");
            if (teleClassActor != null) {
                focus().setHeapObject(teleClassActor);
            }
        }
    }

    private final InspectorAction inspectClassActorByNameAction = new InspectClassActorByNameAction(null);

    /**
     * @return Singleton interactive Action that inspects a {@link ClassActor} object for a class loaded in the VM,
     * specified by class name.
     */
    public final InspectorAction inspectClassActorByName() {
        return inspectClassActorByNameAction;
    }

    /**
     * Action:  inspect a {@link ClassActor} for an interactively named class loaded in the VM,
     * specified by the class ID in hex.
     */
    final class InspectClassActorByHexIdAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect ClassActor by ID (Hex) ...";

        InspectClassActorByHexIdAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            final String value = gui().questionMessage("ID (hex): ");
            if (value != null && !value.equals("")) {
                try {
                    final int serial = Integer.parseInt(value, 16);
                    final TeleClassActor teleClassActor = vm().classRegistry().findTeleClassActor(serial);
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

    private final InspectorAction inspectClassActorByHexIdAction = new InspectClassActorByHexIdAction(null);

    /**
     * @return Singleton interactive Action that inspects a {@link ClassActor} object for a class loaded in the VM,
     * specified by class ID in hex.
     */
    public final InspectorAction inspectClassActorByHexId() {
        return inspectClassActorByHexIdAction;
    }

    /**
     * Action:  inspect a {@link ClassActor} for an interactively named class loaded in the VM,
     * specified by the class ID in decimal.
     */
    final class InspectClassActorByDecimalIdAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect ClassActor by ID (decimal) ...";
        InspectClassActorByDecimalIdAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            final String value = gui().questionMessage("ID (decimal): ");
            if (value != null && !value.equals("")) {
                try {
                    final int serial = Integer.parseInt(value, 10);
                    final TeleClassActor teleClassActor = vm().classRegistry().findTeleClassActor(serial);
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

    private final InspectorAction inspectClassActorByDecimalIdAction = new InspectClassActorByDecimalIdAction(null);

    /**
     * @return Singleton interactive Action that inspects a {@link ClassActor} object for a class loaded in the VM,
     * specified by class ID in decimal.
     */
    public final InspectorAction inspectClassActorByDecimalId() {
        return inspectClassActorByDecimalIdAction;
    }

    /**
     * Action: visits a {@link MethodActor} object in the VM, specified by name.
     */
    final class InspectMethodActorByNameAction extends InspectorAction {

        private static final String DEFAULT_TITLE =  "Inspect MethodActor by name...";

        InspectMethodActorByNameAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(inspection(), "Inspect MethodActor in class...", "Select");
            if (teleClassActor != null) {
                final TeleMethodActor teleMethodActor = MethodActorSearchDialog.show(inspection(), teleClassActor, "Inspect MethodActor...", "Inspect");
                if (teleMethodActor != null) {
                    focus().setHeapObject(teleMethodActor);
                }
            }
        }

    }

    private InspectorAction inspectMethodActorByName = new InspectMethodActorByNameAction(null);

    /**
     * @return an interactive Action that will visit a {@link MethodActor} object in the VM, specified by name.
     */
    public final InspectorAction inspectMethodActorByName() {
        return inspectMethodActorByName;
    }

    /**
     * Action:  inspects the class actor from which a method was substituted.
     */
    final class InspectSubstitutionSourceClassActorAction extends InspectorAction {

        private static final String DEFAULT_TITLE =  "Method substitution source";

        private final TeleClassMethodActor teleClassMethodActor;

        private InspectSubstitutionSourceClassActorAction(TeleClassMethodActor teleClassMethodActor, String actionTitle) {
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
     * Creates an action to inspect the class actor from which a method was substituted.
     *
     * @param teleClassMethodActor representation of a class method in the VM
     * @param actionTitle name of the action
     * @return an action that will inspect the class actor, if any, from which the method was substituted
     */
    public final InspectorAction inspectSubstitutionSourceClassActorAction(TeleClassMethodActor teleClassMethodActor, String actionTitle) {
        return new InspectSubstitutionSourceClassActorAction(teleClassMethodActor, actionTitle);
    }

    /**
     * Creates an action to inspect the class actor from which a method was substituted.
     *
     * @param teleClassMethodActor representation of a class method in the VM
     * @return an action that will inspect the class actor, if any, from which the method was substituted
     */
    public final InspectorAction inspectSubstitutionSourceClassActorAction(TeleClassMethodActor teleClassMethodActor) {
        return new InspectSubstitutionSourceClassActorAction(teleClassMethodActor, null);
    }

    /**
     * Menu: contains actions to inspect each of the compilations of a method.
     */
    final class InspectTargetMethodCompilationsMenu extends InspectorMenu {

        private static final String DEFAULT_TITLE = "Compilations";
        private final TeleClassMethodActor teleClassMethodActor;

        public InspectTargetMethodCompilationsMenu(TeleClassMethodActor teleClassMethodActor, String actionTitle) {
            super(actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.teleClassMethodActor = teleClassMethodActor;
            refresh(true);
        }

        @Override
        public void refresh(boolean force) {
            final List<MaxCompiledCode> compilations = vm().codeCache().compilations(teleClassMethodActor);
            if (getMenuComponentCount() < compilations.size()) {
                for (int index = getMenuComponentCount(); index < compilations.size(); index++) {
                    final MaxCompiledCode compiledCode = compilations.get(index);
                    final String name = inspection().nameDisplay().shortName(compiledCode);
                    add(actions().inspectObject(compiledCode.teleTargetMethod(), name.toString()));
                }
            }
        }
    }

    /**
     * Creates a menu containing actions to inspect all compilations of a method, dynamically updated
     * as compilations are added.
     *
     * @param teleClassMethodActor representation of a Java method in the VM
     * @param actionTitle name of the action to appear on button or menu
     * @return a dynamically refreshed menu that contains actions to inspect each of the compilations of a method.
     */
    public InspectorMenu inspectTargetMethodCompilationsMenu(TeleClassMethodActor teleClassMethodActor, String actionTitle) {
        return new InspectTargetMethodCompilationsMenu(teleClassMethodActor, actionTitle);
    }

    /**
     * Creates a menu containing actions to inspect all compilations of a method, dynamically updated
     * as compilations are added.
     *
     * @param teleClassMethodActor representation of a Java method in the VM
     * @return a dynamically refreshed menu that contains actions to inspect each of the compilations of a method.
     */
    public InspectorMenu inspectTargetMethodCompilationsMenu(TeleClassMethodActor teleClassMethodActor) {
        return new InspectTargetMethodCompilationsMenu(teleClassMethodActor, null);
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
            inspection().viewSourceExternally(new BytecodeLocation(teleClassMethodActor.classMethodActor(), 0));
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
     * Action:  displays in the {@MethodInspector} the method whose target code contains
     * an interactively specified address.
     */
    final class ViewMethodCodeByAddressAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Target method by address...";

        public ViewMethodCodeByAddressAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            new AddressInputDialog(inspection(), vm().bootImageStart(), "View method code containing target code address...", "View Code") {

                @Override
                public String validateInput(Address address) {
                    if (vm().codeCache().findMachineCode(address) != null) {
                        return null;
                    }
                    return "There is no method containing the address " + address.toHexString();
                }

                @Override
                public void entered(Address address) {
                    focus().setCodeLocation(vm().codeManager().createMachineCodeLocation(address, "user specified address"));
                }
            };
        }
    }

    private final InspectorAction viewMethodCodeByAddressAction = new ViewMethodCodeByAddressAction(null);

    /**
     * @return Singleton interactive action that displays in the {@link MethodInspector} the method whose
     * target code contains the specified address in the VM.
     */
    public final InspectorAction viewMethodCodeByAddress() {
        return viewMethodCodeByAddressAction;
    }

    /**
     * @param actionTitle name of the action to appear in menu or button, uses default if null
     * @return an interactive action that displays in the {@link MethodInspector} the method whose
     * target code contains the specified address in the VM.
     */
    public final InspectorAction viewMethodCodeByAddress(String actionTitle) {
        return new ViewMethodCodeByAddressAction(actionTitle);
    }

    /**
     * Action:  displays in the {@MethodInspector} the method code containing an address.
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
     * @return an Action that displays in the {@link MethodInspector} a method at some code location.
     */
    public final InspectorAction viewMethodCodeAtLocation(MaxCodeLocation codeLocation, String actionTitle) {
        return new ViewMethodCodeAtLocationAction(codeLocation, actionTitle);
    }

    /**
     * Action:  displays in the {@MethodInspector} the method code containing the current code selection.
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
     * @return Singleton action that displays in the {@link MethodInspector} the method code
     * containing the current code selection.
     */
    public final InspectorAction viewMethodCodeAtSelection() {
        return viewMethodCodeAtSelection;
    }

    /**
     * Action:  displays in the {@MethodInspector} the method code containing the current instruction pointer.
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
     * @return Singleton Action that displays in the {@link MethodInspector} the method
     * containing the current instruction pointer.
     */
    public final InspectorAction viewMethodCodeAtIP() {
        return viewMethodCodeAtIP;
    }

    /**
     * Action:  displays in the {@MethodInspector} the bytecode for a specified method.
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
            final MaxCodeLocation teleCodeLocation = vm().codeManager().createBytecodeLocation(teleClassMethodActor, 0, "view method bytecode action");
            focus().setCodeLocation(teleCodeLocation);
        }
    }

    /**
     * @return an interactive Action that displays bytecode in the {@link MethodInspector}
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
     * @return an interactive Action that displays bytecode in the {@link MethodInspector}
     * for a selected method.
     */
    public final InspectorAction viewMethodBytecode(TeleClassMethodActor teleClassMethodActor, String actionTitle) {
        return new ViewMethodBytecodeAction(teleClassMethodActor, actionTitle);
    }

    /**
     * Action:  displays in the {@MethodInspector} the bytecode for an interactively specified method.
     */
    final class ViewMethodBytecodeByNameAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View bytecode...";

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
                final TeleMethodActor teleMethodActor = MethodActorSearchDialog.show(inspection(), teleClassActor, hasBytecodePredicate, "View Bytecodes for Method...", "Inspect");
                if (teleMethodActor != null && teleMethodActor instanceof TeleClassMethodActor) {
                    final TeleClassMethodActor teleClassMethodActor = (TeleClassMethodActor) teleMethodActor;
                    final MaxCodeLocation teleCodeLocation = vm().codeManager().createBytecodeLocation(teleClassMethodActor, 0, "view method by name bytecode action");
                    focus().setCodeLocation(teleCodeLocation);
                }
            }
        }
    }

    private final InspectorAction viewMethodBytecodeByNameAction = new  ViewMethodBytecodeByNameAction(null);

    /**
     * @return an interactive Action that displays bytecode in the {@link MethodInspector}
     * for a selected method.
     */
    public final InspectorAction viewMethodBytecodeByName() {
        return viewMethodBytecodeByNameAction;
    }

    /**
     * @param actionTitle name of the action to appear in menu or button
     * @return an interactive Action that displays bytecode in the {@link MethodInspector}
     * for a selected method.
     */
    public final InspectorAction viewMethodBytecodeByName(String actionTitle) {
        return new ViewMethodBytecodeByNameAction(actionTitle);
    }

    /**
     * Action:  displays in the {@MethodInspector} the target code for an interactively specified method.
     */
    final class ViewMethodTargetCodeByNameAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View compiled code...";

        public ViewMethodTargetCodeByNameAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(inspection(), "View compiled code for method in class...", "Select");
            if (teleClassActor != null) {
                final List<MaxCompiledCode> compilations =
                    TargetMethodSearchDialog.show(inspection(), teleClassActor, "View Compiled Code for Method...", "View Code", false);
                if (compilations != null) {
                    focus().setCodeLocation(Utils.first(compilations).getCallEntryLocation());
                }
            }
        }
    }

    private final InspectorAction viewMethodTargetCodeByNameAction = new ViewMethodTargetCodeByNameAction(null);

    /**
     * @return Singleton interactive Action that displays target code in the {@link MethodInspector}
     * for a selected method.
     */
    public final InspectorAction viewMethodTargetCodeByName() {
        return viewMethodTargetCodeByNameAction;
    }

    /**
     * Action:  displays in the {@MethodInspector} the compiled code for an interactively specified method.
     */
    final class ViewMethodTargetCodeAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View target code...";

        public ViewMethodTargetCodeAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            final List<MaxCompiledCode> compilations =
                TargetMethodSearchDialog.show(inspection(), null, "View Compiled Code for Method...", "View Code", false);
            if (compilations != null) {
                focus().setCodeLocation(Utils.first(compilations).getCallEntryLocation(), false);
            }
        }
    }

    private final InspectorAction viewMethodTargetCodeAction = new ViewMethodTargetCodeAction(null);

    /**
     * @return Singleton interactive Action that displays target code in the {@link MethodInspector}
     * for a selected method.
     */
    public final InspectorAction viewMethodTargetCode() {
        return viewMethodTargetCodeAction;
    }

    /**
     * Menu: contains actions to view code for each of the compilations of a method.
     */
    final class ViewTargetMethodCodeMenu extends InspectorMenu {

        private static final String DEFAULT_TITLE = "View compilations";
        private final TeleClassMethodActor teleClassMethodActor;

        public ViewTargetMethodCodeMenu(TeleClassMethodActor teleClassMethodactor, String actionTitle) {
            super(actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.teleClassMethodActor = teleClassMethodactor;
            refresh(true);
        }

        @Override
        public void refresh(boolean force) {
            final List<MaxCompiledCode> compilations = vm().codeCache().compilations(teleClassMethodActor);
            if (getMenuComponentCount() < compilations.size()) {
                for (int index = getMenuComponentCount(); index < compilations.size(); index++) {
                    final MaxCompiledCode compiledCode = compilations.get(index);
                    final StringBuilder name = new StringBuilder();
                    name.append(inspection().nameDisplay().methodCompilationID(compiledCode));
                    name.append("  ");
                    name.append(compiledCode.classActorForObjectType().simpleName());
                    add(actions().viewMethodCodeAtLocation(compiledCode.getCallEntryLocation(), name.toString()));
                }
            }
        }
    }


    /**
     * Creates a menu containing actions to inspect the target code for all compilations of a method, dynamically updated
     * as compilations are added.
     *
     * @param teleClassMethodActor representation of a Java method in the VM
     * @param actionTitle name of the action to appear on button or menu
     * @return a dynamically refreshed menu that contains actions to view code for each of the compilations of a method.
     */
    public InspectorMenu viewTargetMethodCodeMenu(TeleClassMethodActor teleClassMethodActor, String actionTitle) {
        return new ViewTargetMethodCodeMenu(teleClassMethodActor, actionTitle);
    }

    /**
     * Creates a menu containing actions to inspect the target code for all compilations of a method, dynamically updated
     * as compilations are added.
     *
     * @param teleClassMethodActor representation of a Java method in the VM
     * @return a dynamically refreshed menu that contains actions to view code for each of the compilations of a method.
     */
    public InspectorMenu viewTargetMethodCodeMenu(TeleClassMethodActor teleClassMethodActor) {
        return new ViewTargetMethodCodeMenu(teleClassMethodActor, null);
    }

    /**
     * Action:  displays in the {@link MethodInspector} the code of a specified
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
            focus().setCodeLocation(vm().codeManager().createMachineCodeLocation(vm().bootImageStart().plus(offset), "address from boot image"), true);
        }
    }

    private final InspectorAction viewRunMethodCodeInBootImageAction =
        new ViewMethodCodeInBootImageAction(vm().bootImage().header.vmRunMethodOffset, ClassRegistry.MaxineVM_run.toJava());

    /**
     * @return an Action that displays in the {@link MethodInspector} the code of
     * the {@link MaxineVM#run()} method in the boot image.
     */
    public final InspectorAction viewRunMethodCodeInBootImage() {
        return viewRunMethodCodeInBootImageAction;
    }

    private final InspectorAction viewThreadRunMethodCodeInBootImageAction =
        new ViewMethodCodeInBootImageAction(vm().bootImage().header.vmThreadRunMethodOffset, ClassRegistry.VmThread_run.toJava());

    /**
     * @return an Action that displays in the {@link MethodInspector} the code of
     * the {@link VmThread#run()} method in the boot image.
     */
    public final InspectorAction viewThreadRunMethodCodeInBootImage() {
        return viewThreadRunMethodCodeInBootImageAction;
    }

    /**
     * Action:  displays in the {@MethodInspector} a body of native code whose location contains
     * an interactively specified address.
     */
    final class ViewNativeCodeByAddressAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Native method by address...";

        public ViewNativeCodeByAddressAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            // Most likely situation is that we are just aboutMaxine to call a native method in which case RAX is the address
            final MaxThread thread = focus().thread();
            assert thread != null;
            final Address indirectCallAddress = thread.registers().getCallRegisterValue();
            final Address initialAddress = indirectCallAddress == null ? vm().bootImageStart() : indirectCallAddress;
            new AddressInputDialog(inspection(), initialAddress, "View native code containing code address...", "View Code") {
                @Override
                public void entered(Address address) {
                    focus().setCodeLocation(vm().codeManager().createMachineCodeLocation(address, "native code address specified by user"), true);
                }
            };
        }
    }

    private final InspectorAction viewNativeCodeByAddressAction = new ViewNativeCodeByAddressAction(null);

    /**
      * @return Singleton interactive action that displays in the {@link MethodInspector} a body of native code whose
     * location contains the specified address in the VM.
     */
    public final InspectorAction viewNativeCodeByAddress() {
        return viewNativeCodeByAddressAction;
    }

    /**
     * @param actionTitle name of the action, as it will appear on a menu or button, default name if null
     * @return an interactive action that displays in the {@link MethodInspector} a body of native code whose
     * location contains the specified address in the VM.
     */
    public final InspectorAction viewNativeCodeByAddress(String actionTitle) {
        return new ViewNativeCodeByAddressAction(actionTitle);
    }

    /**
     * Action:  copies to the system clipboard a textual representation of the
     * disassembled machine code for a method compilation.
     */
    final class CopyCompiledCodeToClipboardAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Copy disassembled machine code to clipboard";

        private final MaxCompiledCode compiledCode;

        private CopyCompiledCodeToClipboardAction(MaxCompiledCode compiledCode, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.compiledCode = compiledCode;
        }

        @Override
        public void procedure() {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            final PrintStream printStream = new PrintStream(outputStream);
            compiledCode.writeSummary(printStream);
            gui().postToClipboard(outputStream.toString());
        }
    }

    /**
     * @return an Action that copies to the system clipboard a textual disassembly of machine code.
     */
    public InspectorAction copyCompiledCodeToClipboard(MaxCompiledCode compiledCode, String actionTitle) {
        return new CopyCompiledCodeToClipboardAction(compiledCode, actionTitle);
    }

    /**
     * Menu: display a sub-menu of commands to make visible
     * existing object inspectors.  It includes a command
     * that closes all of them.
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
     * Action:  set a target code breakpoint at a particular address.
     */
    final class SetTargetCodeBreakpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Set target code breakpoint";

        private final MaxCodeLocation codeLocation;

        public SetTargetCodeBreakpointAction(MaxCodeLocation codeLocation, String actionTitle) {
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

    public final InspectorAction setTargetCodeBreakpoint(MaxCodeLocation codeLocation, String actionTitle) {
        return new SetTargetCodeBreakpointAction(codeLocation, actionTitle);
    }

    /**
     * Action:  remove a target code breakpoint at a particular address.
     */
    final class RemoveTargetCodeBreakpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Remove target code breakpoint";

        private final MaxCodeLocation codeLocation;

        public RemoveTargetCodeBreakpointAction(MaxCodeLocation codeLocation, String actionTitle) {
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

    public final InspectorAction removeTargetCodeBreakpoint(MaxCodeLocation codeLocation, String actionTitle) {
        return new RemoveTargetCodeBreakpointAction(codeLocation, actionTitle);
    }

     /**
     * Action:  toggle on/off a breakpoint at the target code location specified, or
     * if not initialized, then to the target code location of the current focus.
     */
    final class ToggleTargetCodeBreakpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Toggle target code breakpoint";

        private final MaxCodeLocation codeLocation;

        ToggleTargetCodeBreakpointAction(String actionTitle) {
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

        ToggleTargetCodeBreakpointAction(MaxCodeLocation codeLocation, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            assert codeLocation != null && codeLocation.hasAddress();
            this.codeLocation = codeLocation;
        }

        @Override
        protected void procedure() {
            final MaxCodeLocation toggleCodeLocation = (codeLocation != null) ? codeLocation : focus().codeLocation();
            if (toggleCodeLocation.hasAddress() && !toggleCodeLocation.address().isZero()) {
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

    private InspectorAction toggleTargetCodeBreakpoint = new ToggleTargetCodeBreakpointAction(null);

    /**
     * @return an Action that will toggle on/off a breakpoint at the target code location of the current focus.
     */
    public final InspectorAction toggleTargetCodeBreakpoint() {
        return toggleTargetCodeBreakpoint;
    }

    /**
     * @param actionTitle string that identifies the action
     * @return an Action that will toggle on/off a breakpoint at the target code location of the current focus.
     */
    public final InspectorAction toggleTargetCodeBreakpoint(String actionTitle) {
        return new ToggleTargetCodeBreakpointAction(actionTitle);
    }

    /**
     * @param codeLocation code location
     * @param actionTitle string that identifies the action
     * @return an Action that will toggle on/off a breakpoint at the specified target code location.
     */
    public final InspectorAction toggleTargetCodeBreakpoint(MaxCodeLocation codeLocation, String actionTitle) {
        return new ToggleTargetCodeBreakpointAction(codeLocation, actionTitle);
    }

    /**
     * Action:  sets a  breakpoint at the target code location specified interactively..
     */
    final class SetTargetCodeBreakpointAtAddressAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "At address...";

        SetTargetCodeBreakpointAtAddressAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            refreshableActions.add(this);
            refresh(true);
        }

        @Override
        protected void procedure() {
            new NativeLocationInputDialog(inspection(), "Break at target code address...", vm().bootImageStart(), "") {
                @Override
                public void entered(Address address, String description) {
                    if (!address.isZero()) {
                        try {
                            final MaxBreakpoint breakpoint = vm().breakpointManager().makeBreakpoint(vm().codeManager().createMachineCodeLocation(address, "set target breakpoint"));
                            if (breakpoint == null) {
                                gui().errorMessage("Unable to create breakpoint at: " + address.to0xHexString());
                            } else {
                                breakpoint.setDescription(description);
                            }
                        } catch (MaxVMBusyException maxVMBusyException) {
                            inspection().announceVMBusyFailure(name());
                        }
                    }
                }
            };
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess());
        }
    }

    private InspectorAction setTargetCodeBreakpointAtAddressAction = new SetTargetCodeBreakpointAtAddressAction(null);

    /**
     * @return an Action that will toggle on/off a breakpoint at the target code location of the current focus.
     */
    public final InspectorAction setTargetCodeBreakpointAtAddress() {
        return setTargetCodeBreakpointAtAddressAction;
    }

    /**
     * Action:  sets a breakpoint at every label in a compilation.
     */
    final class SetMachineCodeLabelBreakpointsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Set breakpoint at every machine code label";
        final MaxCompiledCode compiledCode;

        SetMachineCodeLabelBreakpointsAction(MaxCompiledCode compiledCode, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.compiledCode = compiledCode;
            setEnabled(inspection().hasProcess() && compiledCode.getInstructionMap().labelIndexes().size() > 0);
        }

        @Override
        protected void procedure() {
            try {
                final InstructionMap instructionMap = compiledCode.getInstructionMap();
                for (int index : instructionMap.labelIndexes()) {
                    vm().breakpointManager().makeBreakpoint(instructionMap.instructionLocation(index));
                }
            } catch (MaxVMBusyException maxVMBusyException) {
                inspection().announceVMBusyFailure(name());
            }
        }
    }

    /**
     * @return an Action that will set a breakpoint at every machine code label in a compilation
     */
    public final InspectorAction setMachineCodeLabelBreakpoints(MaxCompiledCode compiledCode, String actionTitle) {
        return new SetMachineCodeLabelBreakpointsAction(compiledCode, actionTitle);
    }

    /**
     * Action:  removes any breakpoints at machine code labels.
     */
    final class RemoveMachineCodeLabelBreakpointsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Remove breakpoint at every machine code label";
        private final MaxCompiledCode compiledCode;

        RemoveMachineCodeLabelBreakpointsAction(MaxCompiledCode compiledCode, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.compiledCode = compiledCode;
            setEnabled(inspection().hasProcess() && compiledCode.getInstructionMap().labelIndexes().size() > 0);
        }

        @Override
        protected void procedure() {
            try {
                final InstructionMap instructionMap = compiledCode.getInstructionMap();
                for (int index : instructionMap.labelIndexes()) {
                    final MaxBreakpoint breakpoint = vm().breakpointManager().findBreakpoint(instructionMap.instructionLocation(index));
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
    public final InspectorAction removeMachineCodeLabelBreakpoints(MaxCompiledCode compiledCode, String actionTitle) {
        return new RemoveMachineCodeLabelBreakpointsAction(compiledCode, actionTitle);
    }

     /**
     * Action:  sets machine code breakpoints at a specified compiled code entry.
     */
    final class SetMachineCodeBreakpointAtEntryAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Set machine code breakpoint at code entry";
        private final MaxCompiledCode compiledCode;
        SetMachineCodeBreakpointAtEntryAction(MaxCompiledCode compiledCode, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.compiledCode = compiledCode;
            refreshableActions.add(this);
        }

        @Override
        protected void procedure() {
            final MaxCodeLocation callEntryLocation = compiledCode.getCallEntryLocation();
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
    public final InspectorAction setMachineCodeBreakpointAtEntry(MaxCompiledCode compiledCode, String actionTitle) {
        return new  SetMachineCodeBreakpointAtEntryAction(compiledCode, actionTitle);
    }

    /**
     * @return an interactive Action that sets a compiled code breakpoint at  a method entry
     */
    public final InspectorAction setMachineCodeBreakpointAtEntry(MaxCompiledCode compiledCode) {
        return new  SetMachineCodeBreakpointAtEntryAction(compiledCode, null);
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
                final List<MaxCompiledCode> compilations =
                    TargetMethodSearchDialog.show(inspection(), teleClassActor, "Compiled Method Entry Breakpoints", "Set Breakpoints", true);
                if (compilations != null) {
                    try {
                        // There may be multiple compilations of a method in the result.
                        MaxBreakpoint targetBreakpoint = null;
                        for (MaxCompiledCode compiledCode : compilations) {
                            targetBreakpoint =
                                vm().breakpointManager().makeBreakpoint(compiledCode.getCallEntryLocation());
                        }
                        focus().setBreakpoint(targetBreakpoint);
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
     * @return Singleton interactive Action that sets a target code breakpoint at  a method entry to be selected by name.
     */
    public final InspectorAction setMachineCodeBreakpointAtEntriesByName() {
        return setMachineCodeBreakpointAtEntriesByNameAction;
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
                                    for (MaxCompiledCode compiledCode : vm().codeCache().compilations(teleClassMethodActor)) {
                                        final MaxCodeLocation callEntryLocation = compiledCode.getCallEntryLocation();
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
            final MaxCodeLocation location = vm().codeManager().createBytecodeLocation(teleClassMethodActor, -1, "teleClassMethodActor entry");
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
     * @return an Action  that will set a target code breakpoint at  a method entry.
     */
    public final InspectorAction setBytecodeBreakpointAtMethodEntry(TeleClassMethodActor teleClassMethodActor, String actionTitle) {
        return new SetBytecodeBreakpointAtMethodEntryAction(teleClassMethodActor, actionTitle);
    }

    /**
     * @return an Action  that will set a target code breakpoint at  a method entry.
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
                        vm().breakpointManager().makeBreakpoint(vm().codeManager().createBytecodeLocation(methodKey, "set bytecode breakpoint"));
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
     * @return Singleton interactive Action  that will set a target code breakpoint at  a method entry to be selected by name.
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
                    vm().breakpointManager().makeBreakpoint(vm().codeManager().createBytecodeLocation(methodKey, "set bytecode breakpoint"));
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
     * @return Singleton interactive Action  that will set a target code breakpoint at  a method entry to be selected by name.
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
                InspectorWarning.message("Watchpoint creation failed", maxVMBusyException);
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

    /**
     * @return an Action that will remove all watchpoints in the VM.
     */
    public final InspectorAction removeAllWatchpoints() {
        return removeAllWatchpoints;
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
            final MaxCodeLocation targetLocation = (codeLocation != null) ? codeLocation : focus().codeLocation();
            if (!targetLocation.address().isZero()) {
                try {
                    vm().runToInstruction(targetLocation, false, false);
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
            final MaxCodeLocation targetLocation = (codeLocation != null) ? codeLocation : focus().codeLocation();
            if (targetLocation != null && targetLocation.hasAddress()) {
                try {
                    vm().runToInstruction(targetLocation, false, true);
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
                final MaxCompiledCode compiledCode = vm().codeCache().findCompiledCode(maxCodeLocation.address());
                if (compiledCode != null) {
                    final InstructionMap instructionMap = compiledCode.getInstructionMap();
                    final int instructionIndex = instructionMap.findInstructionIndex(maxCodeLocation.address());
                    for (int index = instructionIndex + 1; index < instructionMap.length(); index++) {
                        if (instructionMap.isCall(index)) {
                            try {
                                vm().runToInstruction(instructionMap.instructionLocation(index), false, false);
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
                final MaxCompiledCode compiledCode = vm().codeCache().findCompiledCode(maxCodeLocation.address());
                if (compiledCode != null) {
                    final InstructionMap instructionMap = compiledCode.getInstructionMap();
                    final int instructionIndex = instructionMap.findInstructionIndex(maxCodeLocation.address());
                    for (int index = instructionIndex + 1; index < instructionMap.length(); index++) {
                        if (instructionMap.isCall(index)) {
                            try {
                                vm().runToInstruction(instructionMap.instructionLocation(index), false, true);
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
                receiver = vm().createReferenceValue(vm().originToReference(Pointer.fromLong(new BigInteger(input, 16).longValue())));
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
            } catch (TeleInterpreterException teleInterpreterException) {
                InspectorError.unexpected("Interpreter failure", teleInterpreterException);
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
     * Action:  displays and highlights an inspection of the current Java frame descriptor.
     */
    final class InspectJavaFrameDescriptorAction extends InspectorAction {
        private static final String DEFAULT_TITLE = "Inspect Java frame descriptor";
        private TargetJavaFrameDescriptor targetJavaFrameDescriptor;

        InspectJavaFrameDescriptorAction(String actionTitle) {
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
            assert targetJavaFrameDescriptor != null;
            if (focus().hasCodeLocation()) {
                final Address instructionAddress = focus().codeLocation().address();
                if (instructionAddress != null && !instructionAddress.isZero()) {
                    final MaxCompiledCode compiledCode = vm().codeCache().findCompiledCode(instructionAddress);
                    if (compiledCode != null) {
                        TargetJavaFrameDescriptorInspector.make(inspection(), targetJavaFrameDescriptor, compiledCode).highlight();
                    }
                }
            } else {
                gui().errorMessage("Could not locate Java frame descriptor");
            }
        }

        /**
         * @return whether there is a Java frame descriptor at the focus target code location
         */
        private boolean inspectable() {
            if (focus().hasCodeLocation()) {
                final Address instructionAddress = focus().codeLocation().address();
                if (instructionAddress != null && !instructionAddress.isZero()) {
                    final MaxCompiledCode compiledCode = vm().codeCache().findCompiledCode(instructionAddress);
                    if (compiledCode != null) {
                        final InstructionMap instructionMap = compiledCode.getInstructionMap();
                        final int instructionIndex = instructionMap.findInstructionIndex(instructionAddress);
                        targetJavaFrameDescriptor = instructionMap.targetFrameDescriptor(instructionIndex);
                        if (targetJavaFrameDescriptor == null) {
                            return false;
                        }
                        return true;
                    }
                }
            }
            targetJavaFrameDescriptor = null;
            return false;
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspectable());
        }
    }

    private InspectorAction inspectJavaFrameDescriptor = new InspectJavaFrameDescriptorAction(null);

    /**
     * @return an Action that will display an inspection of the current Java frame descriptor.
     */
    public final InspectorAction inspectJavaFrameDescriptor() {
        return inspectJavaFrameDescriptor;
    }

    /**
     * Action:  makes visible and highlight the {@link FocusInspector}.
     */
    final class ViewFocusAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View User Focus";

        ViewFocusAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            FocusInspector.make(inspection()).highlight();
        }
    }

    private InspectorAction viewFocus = new ViewFocusAction(null);

    /**
     * @return an Action that will make visible the {@link FocusInspector}.
     */
    public final InspectorAction viewFocus() {
        return viewFocus;
    }

    /**
     * Action:  makes visible and highlight the {@link NotepadInspector}.
     */
    final class ViewNotepadAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Notepad";

        ViewNotepadAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            NotepadInspector.make(inspection(), inspection().getNotepad()).highlight();
        }
    }

    private InspectorAction viewNotepad = new ViewNotepadAction(null);

    /**
     * @return an Action that will make visible the {@link NotepadInspector}.
     */
    public final InspectorAction viewNotepad() {
        return viewNotepad;
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
     * Action:  lists to the console all entries in the {@link MaxCodeCache}.
     */
    final class ListCodeRegistryAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "List code registry contents";

        ListCodeRegistryAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            vm().codeCache().writeSummary(System.out);
        }
    }

    private InspectorAction listCodeRegistry = new ListCodeRegistryAction(null);

    /**
     * @return an Action that will list to the console the entries in the {@link MaxCodeCache}.
     */
    public final InspectorAction listCodeRegistry() {
        return listCodeRegistry;
    }

    /**
     * Action:  lists to the console all entries in the {@link MaxCodeCache} to an interactively specified file.
     */
    final class ListCodeRegistryToFileAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "List code registry contents to a file...";

        ListCodeRegistryToFileAction(String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
        }

        @Override
        protected void procedure() {
            //Create a file chooser
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
            fileChooser.setDialogTitle("Save code registry summary to file:");
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
                vm().codeCache().writeSummary(printStream);
            } catch (FileNotFoundException fileNotFoundException) {
                gui().errorMessage("Unable to open " + file + " for writing:" + fileNotFoundException);
            }
        }
    }

    private InspectorAction listCodeRegistryToFile = new ListCodeRegistryToFileAction(null);

    /**
     * @return an interactive Action that will list to a specified file the entries in the {@link MaxCodeCache}.
     */
    public final InspectorAction listCodeRegistryToFile() {
        return listCodeRegistryToFile;
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
     * @return menu items for memory-related actions that are independent of context
     */
    public InspectorMenuItems genericMemoryMenuItems() {
        return new AbstractInspectorMenuItems(inspection()) {
            public void addTo(InspectorMenu menu) {
                menu.add(inspectMemoryRegionsMenu());
                menu.add(inspectMemoryWords());
                menu.add(inspectMemoryBytes());
                menu.add(memoryWordsInspectorsMenu());
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
                menu.add(actions().viewMethodTargetCode());
                final JMenu methodSub = new JMenu("View method code by name");
                methodSub.add(actions().viewMethodBytecodeByName());
                methodSub.add(actions().viewMethodTargetCodeByName());
                menu.add(methodSub);
                final JMenu bootMethodSub = new JMenu("View boot image method code");
                bootMethodSub.add(actions().viewRunMethodCodeInBootImage());
                bootMethodSub.add(actions().viewThreadRunMethodCodeInBootImage());
                menu.add(bootMethodSub);
                final JMenu byAddressSub = new JMenu("View target code by address");
                byAddressSub.add(actions().viewMethodCodeByAddress());
                byAddressSub.add(actions().viewNativeCodeByAddress());
                menu.add(byAddressSub);
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

                final InspectorMenu breakAt = new InspectorMenu("Break at target code");
                breakAt.add(actions().setTargetCodeBreakpointAtAddress());
                breakAt.add(actions().setMachineCodeBreakpointAtObjectInitializer());
                menu.add(breakAt);

                final InspectorMenu toggle = new InspectorMenu("Toggle breakpoint");
                toggle.add(actions().toggleTargetCodeBreakpoint());
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

                final JMenu methodActorMenu = new JMenu("Inspect method actor");
                methodActorMenu.add(inspectMethodActorByName());
                menu.add(methodActorMenu);

                final JMenu classActorMenu = new JMenu("Inspect class actor");
                classActorMenu.add(inspectClassActorByName());
                classActorMenu.add(inspectClassActorByHexId());
                classActorMenu.add(inspectClassActorByDecimalId());
                classActorMenu.add(inspectBootClassRegistry());
                menu.add(classActorMenu);

                final JMenu objectMenu = new JMenu("Inspect object");
                objectMenu.add(inspectObject());
                objectMenu.add(inspectObjectByID());
                menu.add(objectMenu);

                menu.add(objectInspectorsMenu());
            }
        };
    }

    /**
     * @return menu items for view-related actions that are independent of context
     */
    public InspectorMenuItems genericViewMenuItems() {
        return new AbstractInspectorMenuItems(inspection()) {
            public void addTo(InspectorMenu menu) {
                menu.add(actions().viewBootImage());
                menu.add(actions().viewBreakpoints());
                menu.add(actions().memoryWordsInspectorsMenu());
                menu.add(actions().viewMemoryRegions());
                menu.add(actions().viewMethodCode());
                menu.add(actions().viewNotepad());
                menu.add(actions().objectInspectorsMenu());
                menu.add(actions().viewRegisters());
                menu.add(actions().viewStack());
                menu.add(actions().viewThreads());
                menu.add(actions().viewVmThreadLocals());
                if (vm().watchpointManager() != null) {
                    menu.add(actions().viewWatchpoints());
                }
            }
        };
    }

}

