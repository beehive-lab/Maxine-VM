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
package com.sun.max.ins;

import java.io.*;

import javax.swing.*;

import com.sun.max.collect.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.java.*;
import com.sun.max.ins.memory.*;
import com.sun.max.ins.method.*;
import com.sun.max.ins.type.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.debug.TeleWatchpoint.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.layout.Layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.run.*;
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
 * <li> <code>final class DoSometingAction extends {@link InspectorAction}</code></li>
 * <li> The Action classes are in package scope so that they can be used by {@link InspectorKeyBindings}.</li>
 * <li> Add a title:  <code>private static final DEFAULT_NAME = "do something"</code>.</li>
 * <li> If the
 * action is interactive, for example if it produces a dialog, then the name should conclude with "...".
 * Capitalize the first word of the title but not the others, except for distinguished names such as
 * "Inspector" and acronyms.</li>
 * <li> For singletons, add a package scope constructor with one argument:  <code>String title</code></li>
 * <li> For non-singletons, package scope constructor contains additional arguments that
 * customize the action, for example that specify to what "something" is to be done.</li>
 * <li> In the constructor: <code>super(inspection(), title == null ? DEFAULT_TITLE : title);</code>
 * (being able to override isn't used in many cases, but it adds flexibility).</li>
 * <li> If a singleton and if it contains state, for example enabled/disabled, that might change
 * depending on external circumstances, then register for general notification:
 * <code>_refreshableActions.append(this);</code> in the constructor.</li>
 * <li> Alternately, if state updates depend on a more specific kind of event, register
 * in the constructor explicitly for that event with a listener, for example
 * <code>focus().addListener(new InspectionFocusAdapter() { .... });</code>
 * The body of the listener should call <code>refresh</code>.</li>
 * <li>Override <code>protected void procedure()</code> with a method that does what
 * needs to be done.</li>
 * <li>If a singleton and if it contains state that might be changed depending on
 * external circumstances, override <code>public void refresh(boolean force)</code>
 * with a method that updates the state.</li>
 * </ul></li>
 *
 *<li><b>Create a singleton variable if needed</b>:
 *<ul>
 * <li>If the command is a singleton, create an initialized variable, static if possible.</li>
 * <li><code>private static InspectorAction _doSomething = new DoSomethingAction(null);</code></li>
 * </ul></li>
 *
 * <li><b>Create an accessor:</b>
 * <ul>
 * <li>Singleton: <code>public InspectorAction doSomething()</code>.</li>
 * <li> Singleton accessor returns the singleton variable.</li>
 * <li>Non-singletons have additional arguments that customize the action, e.g. specifying to what "something"
 * is to be done; they also take a <code>String title</code> argument that permits customization of the
 * action's name, for example when it appears in menus.</li>
 * <li> Non-singletons return <code>new DoSomethignAction(args, String title)</code>.</li>
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
    private final AppendableSequence<InspectorAction> refreshableActions = new ArrayListSequence<InspectorAction>();

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
     * Action:  displays the {@link AboutDialog}.
     */
    final class AboutAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "About";

        AboutAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            AboutDialog.create(inspection());
        }
    }

    private InspectorAction about = new AboutAction(null);

    /**
     * @return an Action that will display the {@link AboutDialog}.
     */
    public final InspectorAction about() {
        return about;
    }


    /**
     * Action:  displays the {@link PreferenceDialog}.
     */
    final class PreferencesAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Preferences";

        PreferencesAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
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

        RefreshAllAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            inspection().refreshAll(true);
        }
    }

    private final InspectorAction refreshAll = new RefreshAllAction(null);

    /**
     * @return an Action that updates all displayed information read from the VM.
     */
    public final InspectorAction refreshAll() {
        return refreshAll;
    }

    /**
     * Action:  close all open inspectors that match a predicate.
     */
    final class CloseViewsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Close views";

        private final Predicate<Inspector> predicate;

        CloseViewsAction(Predicate<Inspector> predicate, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            this.predicate = predicate;
        }

        @Override
        protected void procedure() {
            gui().removeInspectors(predicate);
        }
    }

    /**
     * @param predicate a predicate that returns true for all Inspectors to be closed.
     * @param title a string name for the Action, uses default name if null.
     * @return an Action that will close views.
     */
    public final InspectorAction closeViews(Predicate<Inspector> predicate, String title) {
        return new CloseViewsAction(predicate, title);
    }


    /**
     * Action:  closes all open inspectors.
     */
    final class CloseAllViewsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Close all views";

        CloseAllViewsAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            gui().removeInspectors(Predicate.Static.alwaysTrue(Inspector.class));
        }
    }

    private final InspectorAction closeAll = new CloseAllViewsAction(null);

    /**
     * @return an Action that closes all open inspectors.
     */
    public final InspectorAction closeAllViews() {
        return closeAll;
    }


    /**
     * Action:  quits inspector.
     */
    final class QuitAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Quit Inspector";

        QuitAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            inspection().quit();
        }
    }

    private final InspectorAction quit = new QuitAction(null);

    /**
     * @return an Action that quits the VM inspection session.
     */
    public final InspectorAction quit() {
        return quit;
    }


    /**
     * Action:  relocates the boot image, assuming that the inspector was invoked
     * with the option {@link MaxineInspector#suspendingBeforeRelocating()} set.
     */
    final class RelocateBootImageAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Relocate Boot Image";

        RelocateBootImageAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            try {
                maxVM().advanceToJavaEntryPoint();
            } catch (IOException ioException) {
                gui().errorMessage("error during relocation of boot image");
            }
            setEnabled(false);
        }
    }

    private final InspectorAction relocateBootImage = new RelocateBootImageAction(null);

    /**
     * @return an Action that relocates the boot image, assuming that the inspector was invoked
     * with the option {@link MaxineInspector#suspendingBeforeRelocating()} set.
     */
    public final InspectorAction relocateBootImage() {
        return relocateBootImage;
    }


    /**
     * Action:  sets level of trace output in inspector code.
     */
    final class SetInspectorTraceLevelAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Set Inspector trace level...";

        SetInspectorTraceLevelAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            final int oldLevel = Trace.level();
            int newLevel = oldLevel;
            final String input = gui().inputDialog(DEFAULT_TITLE, Integer.toString(oldLevel));
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

    private final InspectorAction setInspectorTraceLevel = new SetInspectorTraceLevelAction(null);

    /**
     * @return an interactive Action that permits setting the level of inspector {@link Trace} output.
     */
    public final InspectorAction setInspectorTraceLevel() {
        return setInspectorTraceLevel;
    }


    /**
     * Action:  changes the threshold determining when the Inspectors uses its
     * {@linkplain InspectorInterpeter interpreter} for access to VM state.
     */
    final class ChangeInterpreterUseLevelAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Change Interpreter use level...";

        ChangeInterpreterUseLevelAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
        }

        @Override
        protected void procedure() {
            final int oldLevel = maxVM().getInterpreterUseLevel();
            int newLevel = oldLevel;
            final String input = gui().inputDialog("Change interpreter use level (0=none, 1=some, etc)", Integer.toString(oldLevel));
            try {
                newLevel = Integer.parseInt(input);
            } catch (NumberFormatException numberFormatException) {
                gui().errorMessage(numberFormatException.toString());
            }
            if (newLevel != oldLevel) {
                maxVM().setInterpreterUseLevel(newLevel);
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess());
        }
    }

    private final InspectorAction changeInterpreterUseLevel = new ChangeInterpreterUseLevelAction(null);

    /**
     * @return an interactive action that permits changing the level at which the interpreter
     * will be used when communicating with the VM.
     */
    public final InspectorAction changeInterpreterUseLevel() {
        return changeInterpreterUseLevel;
    }


    /**
     * Action:  sets debugging level for transport.
     * Appears unused October '08 (mlvdv)
     */
    final class SetTransportDebugLevelAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Set transport debug level...";

        SetTransportDebugLevelAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
        }

        @Override
        protected void procedure() {
            final int oldLevel = maxVM().transportDebugLevel();
            int newLevel = oldLevel;
            final String input = gui().inputDialog(" (Set transport debug level, 0=none, 1=some, etc)", Integer.toString(oldLevel));
            try {
                newLevel = Integer.parseInt(input);
            } catch (NumberFormatException numberFormatException) {
                gui().errorMessage(numberFormatException.toString());
            }
            if (newLevel != oldLevel) {
                maxVM().setTransportDebugLevel(newLevel);
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess());
        }
    }

    private final InspectorAction setTransportDebugLevel = new SetTransportDebugLevelAction(null);

    /**
     * @return an interactive action that permits setting the debugging level for transport.
     */
    public final InspectorAction setTransportDebugLevel() {
        return setTransportDebugLevel;
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
                maxVM().executeCommandsFromFile(fileName);
            }
        }
    }

    private final InspectorAction runFileCommands = new RunFileCommandsAction();

    /**
     * @return an interactive Action that will run Inspector commands from a specified file.
     */
    public final InspectorAction runFileCommands() {
        return runFileCommands;
    }


    /**
     * Action:  updates the {@linkplain MaxVM#updateLoadableTypeDescriptorsFromClasspath() types available} on
     * the VM's class path by rescanning the complete class path for types.
     */
    final class UpdateClasspathTypesAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Rescan class path for types";

        UpdateClasspathTypesAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            maxVM().updateLoadableTypeDescriptorsFromClasspath();
        }
    }

    private final InspectorAction updateClasspathTypes = new UpdateClasspathTypesAction(null);

    /**
     * @return an Action that updates the {@linkplain MaxVM#updateLoadableTypeDescriptorsFromClasspath() types available} on
     * the VM's class path by rescanning the complete class path for types.
     */
    public final InspectorAction updateClasspathTypes() {
        return updateClasspathTypes;
    }


    /**
     * Action: sets the level of tracing in the VM interactively.
     */
    final class SetVMTraceLevelAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Set VM trace level";

        SetVMTraceLevelAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
        }

        @Override
        protected void procedure() {
            final int oldLevel = maxVM().getVMTraceLevel();
            int newLevel = oldLevel;
            final String input = gui().inputDialog("Set VM Trace Level", Integer.toString(oldLevel));
            try {
                newLevel = Integer.parseInt(input);
            } catch (NumberFormatException numberFormatException) {
                gui().errorMessage(numberFormatException.toString());
            }
            if (newLevel != oldLevel) {
                maxVM().setVMTraceLevel(newLevel);
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

        SetVMTraceThresholdAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
        }

        @Override
        protected void procedure() {
            final long oldThreshold = maxVM().getVMTraceThreshold();
            long newThreshold = oldThreshold;
            final String input = gui().inputDialog("Set VM trace threshold", Long.toString(oldThreshold));
            try {
                newThreshold = Long.parseLong(input);
            } catch (NumberFormatException numberFormatException) {
                gui().errorMessage(numberFormatException.toString());
            }
            if (newThreshold != oldThreshold) {
                maxVM().setVMTraceThreshold(newThreshold);
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

        ViewBootImageAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
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

        ViewBreakpointsAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
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
     * Action:  makes visible and highlights the {@link WatchpointsInspector}.
     */
    final class ViewWatchpointsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Watchpoints";

        ViewWatchpointsAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
        }

        @Override
        protected void procedure() {
            WatchpointsInspector.make(inspection()).highlight();
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(maxVM().watchpointsEnabled());
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
     * Action:  makes visible and highlights the {@link MemoryRegionsInspector}.
     */
    final class ViewMemoryRegionsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Memory regions";

        ViewMemoryRegionsAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
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

        ViewMethodCodeAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            final TeleCodeLocation teleCodeLocation = maxVM().createCodeLocation(focus().thread().instructionPointer());
            focus().setCodeLocation(teleCodeLocation, true);
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

        ViewRegistersAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
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

        ViewStackAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
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

        ViewThreadsAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
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
    final class ViewVmThreadLocalsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "VM thread locals";

        ViewVmThreadLocalsAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
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

    private InspectorAction viewVmThreadLocals = new ViewVmThreadLocalsAction(null);

    /**
     * @return an Action that will make visible the {@link ThreadsInspector}.
     */
    public final InspectorAction viewVmThreadLocals() {
        return viewVmThreadLocals;
    }


    /**
     * Action:  copies a hex string version of a {@link Word} to the system clipboard.
     */
    final class CopyWordAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Copy word to clipboard";
        private final Word word;

        private CopyWordAction(Word word, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            this.word = word;
        }

        @Override
        public void procedure() {
            gui().postToClipboard(word.toHexString());
        }
    }

    /**
     * @param a {@link Word} from the VM.
     * @param title a string to use as the title of the action, uses default name if null.
     * @return an Action that copies the word's text value in hex to the system clipboard
     */
    public final InspectorAction copyWord(Word word, String title) {
        return new CopyWordAction(word, title);
    }

    /**
     * @param a {@link Word} wrapped as a {@link Value} from the VM.
     * @param title a string to use as the title of the action, uses default name if null.
     * @return an Action that copies the word's text value in hex to the system clipboard,
     * null if not a word.
     */
    public final InspectorAction copyValue(Value value, String title) {
        Word word = Word.zero();
        try {
            word = value.asWord();
        } catch (Throwable throwable) {
        }
        final InspectorAction action = new CopyWordAction(word, title);
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
            refreshableActions.append(this);
        }

        InspectMemoryBytesAction(Address address, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            this.address = address;
            this.teleObject = null;
        }

        InspectMemoryBytesAction(TeleObject teleObject, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
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
                new AddressInputDialog(inspection(), maxVM().bootImageStart(), "Inspect memory bytes at address...", "Inspect") {
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
     * @return an interactive Action that will create a Memory Inspector
     */
    @Deprecated
    public final InspectorAction inspectMemoryBytes() {
        return inspectMemoryBytesAction;
    }

    /**
     * @param teleObject surrogate for an object in the VM
     * @param title a string name for the Action, uses default name if null
     * @return an Action that will create a Memory Inspector at the address
     */
    @Deprecated
    public final InspectorAction inspectMemoryBytes(TeleObject teleObject, String title) {
        return new InspectMemoryBytesAction(teleObject, title);
    }

    /**
     * @param address a valid memory {@link Address} in the VM
     * @param title a string name for the action, uses default name if null
     * @return an interactive Action that will create a Memory Inspector at the address
     */
    public final InspectorAction inspectMemoryBytes(Address address, String title) {
        return new InspectMemoryBytesAction(address, title);
    }


    /**
     * Action:   inspect a memory region, interactive if no location specified.
     */
    final class InspectMemoryWordsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect memory";
        private final Address address;;

        InspectMemoryWordsAction() {
            super(inspection(), "Inspect memory at address...");
            refreshableActions.append(this);
            this.address = null;
        }

        InspectMemoryWordsAction(Address address, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            this.address = address;
        }

        @Override
        protected void procedure() {
            if (address != null) {
                final Inspector inspector = new MemoryWordsInspector(inspection(), new FixedMemoryRegion(address, maxVM().wordSize().times(10), ""));
                inspector.highlight();
            } else {
                new AddressInputDialog(inspection(), maxVM().bootImageStart(), "Inspect memory at address...", "Inspect") {

                    @Override
                    public void entered(Address address) {
                        final Inspector inspector = new MemoryWordsInspector(inspection(), new FixedMemoryRegion(address, maxVM().wordSize().times(10), ""));
                        inspector.highlight();
                    }
                };
            }
        }
    }

    private final InspectorAction inspectMemoryWordsAction = new InspectMemoryWordsAction();

    /**
     * @return an interactive Action that will create a MemoryWords Inspector
     */
    public final InspectorAction inspectMemoryWords() {
        return inspectMemoryWordsAction;
    }


    /**
     * @param address a valid memory {@link Address} in the VM
     * @param title a name for the action
     * @return an Action that will create a Memory Words Inspector at the address
     */
    public final InspectorAction inspectMemoryWords(Address address, String title) {
        return new InspectMemoryWordsAction(address, title);
    }


    /**
     * Action: inspects memory occupied by an object.
     */

    final class InspectObjectMemoryWordsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect memory";
        private final TeleObject teleObject;

        InspectObjectMemoryWordsAction(TeleObject teleObject, String title) {
            super(inspection(), (title == null) ? DEFAULT_TITLE : title);
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
     * @param title a name for the action
     * @return an Action that will create a Memory Words Inspector at the address
     */
    public final InspectorAction inspectObjectMemoryWords(TeleObject teleObject, String title) {
        return new InspectObjectMemoryWordsAction(teleObject, title);
    }


    /**
     * Action: inspect a named region as memory words.
     */
    final class InspectRegionMemoryWordsAction extends InspectorAction {

        private final MemoryRegion memoryRegion;
        private final String regionName;

        InspectRegionMemoryWordsAction(MemoryRegion memoryRegion, String regionName, String actionTitle) {
            super(inspection(), actionTitle == null ? ("Inspect memory region \"" + regionName + "\"") : actionTitle);
            this.memoryRegion = memoryRegion;
            this.regionName = regionName;
            refreshableActions.append(this);
        }

        @Override
        protected void procedure() {
            final Inspector inspector = new MemoryWordsInspector(inspection(), memoryRegion, regionName);
            inspector.highlight();
        }
    }

    /**
     * @return an Action that will create a Memory Words Inspector for the boot heap region.
     */
    public final InspectorAction inspectBootHeapMemoryWords() {
        return new InspectRegionMemoryWordsAction(maxVM().teleBootHeapRegion(), "Heap-Boot", null);
    }

    /**
     * @return an Action that will create a Memory Words Inspector for the boot code region.
     */
    public final InspectorAction inspectBootCodeMemoryWords() {
        return new InspectRegionMemoryWordsAction(maxVM().teleBootCodeRegion(), "Heap-Code", null);
    }

    /**
     * @return an Action that will create a MemoryWords Inspector for a named region of memory.
     */
    public final InspectorAction inspectRegionMemoryWords(MemoryRegion memoryRegion, String regionName, String actionTitle) {
        final String title = (actionTitle == null) ? ("Inspect memory region \"" + regionName + "\"") : actionTitle;
        return new InspectRegionMemoryWordsAction(memoryRegion, regionName, title);
    }


    /**
     * Action:  creates a memory inspector for the currently selected memory region, if any.
     */

    private final class InspectSelectedMemoryRegionWordsAction extends InspectorAction {
        private static final String DEFAULT_TITLE = "Inspect selected memory region";
        InspectSelectedMemoryRegionWordsAction() {
            super(inspection(), DEFAULT_TITLE);
            refreshableActions.append(this);
            refresh(true);
        }

        @Override
        protected void procedure() {
            final MemoryRegion memoryRegion = focus().memoryRegion();
            if (memoryRegion != null) {
                final Inspector inspector = new MemoryWordsInspector(inspection(), memoryRegion, memoryRegion.description());
                inspector.highlight();
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().focus().hasMemoryRegion());
        }
    }

    private final InspectorAction inspectSelectedMemoryRegionWordsAction = new InspectSelectedMemoryRegionWordsAction();

    /**
     * @return an Action that will create a Memory inspector for the currently selected region of memory
     */
    public final InspectorAction inspectSelectedMemoryRegionWords() {
        return inspectSelectedMemoryRegionWordsAction;
    }

    /**
     * Action: sets inspection focus to specified {@link MemoryRegion}.
     */
    final class SelectMemoryRegionAction extends InspectorAction {

        private final MemoryRegion memoryRegion;
        private static final String DEFAULT_TITLE = "Select memory region";

        SelectMemoryRegionAction(String title, MemoryRegion memoryRegion) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
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
    public final InspectorAction selectMemoryRegion(MemoryRegion memoryRegion) {
        final String title = "Select memory region \"" + memoryRegion.description() + "\"";
        return new SelectMemoryRegionAction(title, memoryRegion);
    }


    /**
     * Action: create an Object Inspector, interactively specified by address..
     */
    final class InspectObjectAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect heap object at address...";

        InspectObjectAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            new AddressInputDialog(inspection(), maxVM().teleBootHeapRegion().start(), "Inspect heap object at address...", "Inspect") {

                @Override
                public void entered(Address address) {
                    final Pointer pointer = address.asPointer();
                    if (maxVM().isValidOrigin(pointer)) {
                        final Reference objectReference = maxVM().originToReference(pointer);
                        final TeleObject teleObject = maxVM().makeTeleObject(objectReference);
                        focus().setHeapObject(teleObject);
                    } else {
                        gui().errorMessage("heap object not found at 0x"  + address.toHexString());
                    }
                }
            };
        }
    }

    private final InspectorAction inspectObject = new InspectObjectAction(null);

    /**
     * @return an Action that will create an Object Inspector interactively, prompting the user for a numeric object ID
     */
    public final InspectorAction inspectObject() {
        return inspectObject;
    }


    /**
     * Action:  creates an inspector for a specific heap object in the VM.
     */
    final class InspectSpecifiedObjectAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect object";
        final TeleObject teleObject;

        InspectSpecifiedObjectAction(TeleObject teleObject, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            this.teleObject = teleObject;
        }

        @Override
        protected void procedure() {
            focus().setHeapObject(teleObject);
        }
    }

    /**
     * @param surrogate for a heap object in the VM.
     * @param title a string name for the Action, uses default name if null
     * @return an Action that will create an Object Inspector
     */
    public final InspectorAction inspectObject(TeleObject teleObject, String title) {
        return new InspectSpecifiedObjectAction(teleObject, title);
    }


    /**
     * Action: create an Object Inspector, interactively specified by the inspector's OID.
     */
    final class InspectObjectByIDAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect heap object by ID...";

        InspectObjectByIDAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            final String input = gui().inputDialog("Inspect heap object by ID..", "");
            try {
                final long oid = Long.parseLong(input);
                final TeleObject teleObject = maxVM().findObjectByOID(oid);
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

    private final InspectorAction inspectObjectByID = new InspectObjectByIDAction(null);

    /**
     * @return an Action that will create an Object Inspector interactively, prompting the user for a numeric object ID
     */
    public final InspectorAction inspectObjectByID() {
        return inspectObjectByID;
    }

    /**
     * Action: create an Object Inspector for the boot {@link ClassRegistry} in the VM.
     */
    final class InspectBootClassRegistryAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect boot class registry";

        InspectBootClassRegistryAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            final TeleObject teleBootClassRegistry = maxVM().makeTeleObject(maxVM().bootClassRegistryReference());
            focus().setHeapObject(teleBootClassRegistry);
        }
    }

    private final InspectorAction inspectBootClassRegistry = new InspectBootClassRegistryAction(null);

    /**
     * @return an action that will create an Object Inspector for the boot {@link ClassRegistry} in the VM.
     */
    public final InspectorAction inspectBootClassRegistry() {
        return inspectBootClassRegistry;
    }


    /**
     * Action:  inspect a {@link ClassActor} object for an interactively named class loaded in the VM,
     * specified by class name.
     */
    final class InspectClassActorByNameAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect ClassActor by name...";

        InspectClassActorByNameAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(inspection(), "Inspect ClassActor ...", "Inspect");
            if (teleClassActor != null) {
                focus().setHeapObject(teleClassActor);
            }
        }
    }

    private final InspectorAction inspectClassActorByName = new InspectClassActorByNameAction(null);

    /**
     * @return an interactive Action that inspects a {@link ClassActor} object for a class loaded in the VM,
     * specified by class name.
     */
    public final InspectorAction inspectClassActorByName() {
        return inspectClassActorByName;
    }

    /**
     * Action:  inspect a {@link ClassActor} for an interactively named class loaded in the VM,
     * specified by the class ID in hex.
     */
    final class InspectClassActorByHexIdAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect ClassActor by ID (Hex) ...";

        InspectClassActorByHexIdAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            final String value = gui().questionMessage("ID (hex): ");
            if (value != null && !value.equals("")) {
                try {
                    final int serial = Integer.parseInt(value, 16);
                    final TeleClassActor teleClassActor = maxVM().findTeleClassActor(serial);
                    if (teleClassActor == null) {
                        gui().errorMessage("failed to find classActor for ID:  0x" + Integer.toHexString(serial));
                    } else {
                        focus().setHeapObject(teleClassActor);
                    }
                } catch (NumberFormatException ex) {
                    gui().errorMessage("Hex integer required");
                }
            }
        }
    }

    private final InspectorAction inspectClassActorByHexId = new InspectClassActorByHexIdAction(null);

    /**
     * @return an interactive Action that inspects a {@link ClassActor} object for a class loaded in the VM,
     * specified by class ID in hex.
     */
    public final InspectorAction inspectClassActorByHexId() {
        return inspectClassActorByHexId;
    }


    /**
     * Action:  inspect a {@link ClassActor} for an interactively named class loaded in the VM,
     * specified by the class ID in decimal.
     */
    final class InspectClassActorByDecimalIdAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect ClassActor by ID (decimal) ...";
        InspectClassActorByDecimalIdAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            final String value = gui().questionMessage("ID (decimal): ");
            if (value != null && !value.equals("")) {
                try {
                    final int serial = Integer.parseInt(value, 10);
                    final TeleClassActor teleClassActor = maxVM().findTeleClassActor(serial);
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
    private final InspectorAction inspectClassActorByDecimalId = new InspectClassActorByDecimalIdAction(null);

    /**
     * @return an interactive Action that inspects a {@link ClassActor} object for a class loaded in the VM,
     * specified by class ID in decimal.
     */
    public final InspectorAction inspectClassActorByDecimalId() {
        return inspectClassActorByDecimalId;
    }


    /**
     * Action: visits a {@link MethodActor} object in the VM, specified by name.
     */
    final class InspectMethodActorByNameAction extends InspectorAction {

        private static final String DEFAULT_TITLE =  "Inspect MethodActor...";

        InspectMethodActorByNameAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
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
     * Action:  displays in the {@MethodInspector} the method whose target code contains
     * an interactively specified address.
     */
    final class ViewMethodCodeByAddressAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View method code containing target code address...";

        public ViewMethodCodeByAddressAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            new AddressInputDialog(inspection(), maxVM().bootImageStart(), "View method code containing target code address...", "View Code") {

                @Override
                public String validateInput(Address address) {
                    if (maxVM().makeTeleTargetMethod(address) != null) {
                        return null;
                    }
                    return "There is no method containing the address " + address.toHexString();
                }

                @Override
                public void entered(Address address) {
                    focus().setCodeLocation(maxVM().createCodeLocation(address), false);
                }
            };
        }
    }

    private final InspectorAction viewMethodCodeByAddress = new ViewMethodCodeByAddressAction(null);

    /**
     * @return an interactive action that displays in the {@link MethodInspector} the method whose
     * target code contains the specified address in the VM.
     */
    public final InspectorAction viewMethodCodeByAddress() {
        return viewMethodCodeByAddress;
    }

    /**
     * Action:  displays in the {@MethodInspector} the method whose target code contains
     * an interactively specified address.
     */
    final class ViewRuntimeStubByAddressAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View runtime stub code containing target code address...";

        public ViewRuntimeStubByAddressAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            new AddressInputDialog(inspection(), maxVM().bootImageStart(), "View runtime stub code containing target code address...", "View Code") {

                @Override
                public String validateInput(Address address) {
                    if (maxVM().makeTeleRuntimeStub(address) != null) {
                        return null;
                    }
                    return "There is no runtime stub containing the address " + address.toHexString();
                }

                @Override
                public void entered(Address address) {
                    focus().setCodeLocation(maxVM().createCodeLocation(address), false);
                }
            };
        }

    }

    private final InspectorAction viewRuntimeStubByAddress = new ViewRuntimeStubByAddressAction(null);

    /**
     * @return an interactive action that displays in the {@link MethodInspector} the method whose
     * target code contains the specified address in the VM.
     */
    public final InspectorAction viewRuntimeStubByAddress() {
        return viewRuntimeStubByAddress;
    }



    /**
     * Action:  displays in the {@MethodInspector} the method code containing the current code selection.
     */
    final class ViewMethodCodeAtSelectionAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View method code at current selection";
        public ViewMethodCodeAtSelectionAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
        }

        @Override
        protected void procedure() {
            final TeleCodeLocation teleCodeLocation = focus().codeLocation();
            focus().setCodeLocation(teleCodeLocation, true);
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasCodeLocation());
        }
    }

    private final ViewMethodCodeAtSelectionAction viewMethodCodeAtSelection = new ViewMethodCodeAtSelectionAction(null);

    /**
     * @return an action that displays in the {@link MethodInspector} the method code
     * containing the current code selection.
     */
    public final InspectorAction viewMethodCodeAtSelection() {
        return viewMethodCodeAtSelection;
    }


    /**
     * Action:  displays in the {@MethodInspector} the method code containing the current instruction pointer.
     */
    final class ViewMethodCodeAtIPAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View method code at IP";
        public ViewMethodCodeAtIPAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
        }

        @Override
        protected void procedure() {
            final Pointer instructionPointer = focus().thread().instructionPointer();
            focus().setCodeLocation(maxVM().createCodeLocation(instructionPointer), true);
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasThread());
        }
    }

    private final ViewMethodCodeAtIPAction viewMethodCodeAtIP = new ViewMethodCodeAtIPAction(null);

    /**
     * @return an Action that displays in the {@link MethodInspector} the method
     * containing the current instruction pointer.
     */
    public final InspectorAction viewMethodCodeAtIP() {
        return viewMethodCodeAtIP;
    }


    /**
     * Action:  displays in the {@MethodInspector} the bytecode for an interactively specified method.
     */
    final class ViewMethodBytecodeByNameAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View method bytecode...";

        public ViewMethodBytecodeByNameAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
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
                final TeleMethodActor teleMethodActor = MethodActorSearchDialog.show(inspection(), teleClassActor, hasBytecodePredicate, "View Bytecode for Method...", "Inspect");
                if (teleMethodActor != null && teleMethodActor instanceof TeleClassMethodActor) {
                    final TeleClassMethodActor teleClassMethodActor = (TeleClassMethodActor) teleMethodActor;
                    final TeleCodeLocation teleCodeLocation = maxVM().createCodeLocation(teleClassMethodActor, 0);
                    focus().setCodeLocation(teleCodeLocation, false);
                }
            }
        }
    }

    private final InspectorAction viewMethodBytecodeByName = new ViewMethodBytecodeByNameAction(null);

    /**
     * @return an interactive Action that displays bytecode in the {@link MethodInspector}
     * for a selected method.
     */
    public final InspectorAction viewMethodBytecodeByName() {
        return viewMethodBytecodeByName;
    }


    /**
     * Action:  displays in the {@MethodInspector} the target code for an interactively specified method.
     */
    final class ViewMethodTargetCodeByNameAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View method target code...";

        public ViewMethodTargetCodeByNameAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(inspection(), "View target code for method in class...", "Select");
            if (teleClassActor != null) {
                final Sequence<TeleTargetMethod> teleTargetMethods = TargetMethodSearchDialog.show(inspection(), teleClassActor, "View Target Code for Method...", "View Code", false);
                if (teleTargetMethods != null) {
                    focus().setCodeLocation(maxVM().createCodeLocation(teleTargetMethods.first().callEntryPoint()), false);
                }
            }
        }
    }

    private final InspectorAction viewMethodTargetCodeByName = new ViewMethodTargetCodeByNameAction(null);

    /**
     * @return an interactive Action that displays target code in the {@link MethodInspector}
     * for a selected method.
     */
    public final InspectorAction viewMethodTargetCodeByName() {
        return viewMethodTargetCodeByName;
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

        @Override
        protected void procedure() {
            focus().setCodeLocation(maxVM().createCodeLocation(maxVM().bootImageStart().plus(offset)), true);
        }
    }

    private final InspectorAction viewRunMethodCodeInBootImage =
        new ViewMethodCodeInBootImageAction(maxVM().bootImage().header().vmRunMethodOffset, MaxineVM.class, "run", MaxineVM.runMethodParameterTypes());

    /**
     * @return an Action that displays in the {@link MethodInspector} the code of
     * the {@link MaxineVM#run()} method in the boot image.
     */
    public final InspectorAction viewRunMethodCodeInBootImage() {
        return viewRunMethodCodeInBootImage;
    }

    private final InspectorAction viewThreadRunMethodCodeInBootImage =
        new ViewMethodCodeInBootImageAction(maxVM().bootImage().header().vmThreadRunMethodOffset, VmThread.class, "run", int.class, Address.class, Pointer.class,
                    Pointer.class, Pointer.class, Pointer.class, Pointer.class, Pointer.class, Pointer.class, Pointer.class);

    /**
     * @return an Action that displays in the {@link MethodInspector} the code of
     * the {@link VmThread#run()} method in the boot image.
     */
    public final InspectorAction viewThreadRunMethodCodeInBootImage() {
        return viewThreadRunMethodCodeInBootImage;
    }

    private final InspectorAction viewSchemeRunMethodCodeInBootImage =
            new ViewMethodCodeInBootImageAction(maxVM().bootImage().header().runSchemeRunMethodOffset, maxVM().vmConfiguration().runPackage.schemeTypeToImplementation(RunScheme.class), "run");

    /**
     * @return an Action that displays in the {@link MethodInspector} the code of
     * the {@link RunScheme#run()} method in the boot image.
     */
    public final InspectorAction viewSchemeRunMethodCodeInBootImage() {
        return viewSchemeRunMethodCodeInBootImage;
    }


    /**
     * Action:  displays in the {@MethodInspector} a body of native code whose location contains
     * an interactively specified address.
     */
    final class ViewNativeCodeByAddressAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View native code containing  address...";

        public ViewNativeCodeByAddressAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            // Most likely situation is that we are just about to call a native method in which case RAX is the address
            final MaxThread thread = focus().thread();
            assert thread != null;
            final Address indirectCallAddress = thread.integerRegisters().getCallRegisterValue();
            final Address initialAddress = indirectCallAddress == null ? maxVM().bootImageStart() : indirectCallAddress;
            new AddressInputDialog(inspection(), initialAddress, "View native code containing code address...", "View Code") {
                @Override
                public void entered(Address address) {
                    focus().setCodeLocation(maxVM().createCodeLocation(address), true);
                }
            };
        }
    }

    private final InspectorAction viewNativeCodeByAddress = new ViewNativeCodeByAddressAction(null);

    /**
     * @return an interactive action that displays in the {@link MethodInspector} a body of native code whose
     * location contains the specified address in the VM.
     */
    public final InspectorAction viewNativeCodeByAddress() {
        return viewNativeCodeByAddress;
    }


   /**
     * Action:  removes the currently selected breakpoint from the VM.
     */
    final class RemoveSelectedBreakpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Remove selected breakpoint";

        RemoveSelectedBreakpointAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
            focus().addListener(new InspectionFocusAdapter() {
                @Override
                public void breakpointFocusSet(TeleBreakpoint oldTeleBreakpoint, TeleBreakpoint teleBreakpoint) {
                    refresh(false);
                }
            });
        }

        @Override
        protected void procedure() {
            final TeleBreakpoint selectedTeleBreakpoint = focus().breakpoint();
            if (selectedTeleBreakpoint != null) {
                focus().setBreakpoint(null);
                selectedTeleBreakpoint.remove();
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

        final TeleBreakpoint teleBreakpoint;

        RemoveBreakpointAction(TeleBreakpoint teleBreakpoint, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            this.teleBreakpoint = teleBreakpoint;
        }

        @Override
        protected void procedure() {
            if (focus().breakpoint() == teleBreakpoint) {
                focus().setBreakpoint(null);
            }
            teleBreakpoint.remove();
        }
    }

    /**
     * @param surrogate for a breakpoint in the VM.
     * @param title a string name for the Action, uses default name if null.
     * @return an Action that will remove the breakpoint
     */
    public final InspectorAction removeBreakpoint(TeleBreakpoint teleBreakpoint, String title) {
        return new RemoveBreakpointAction(teleBreakpoint, title);
    }


    /**
     * Action: removes all existing breakpoints in the VM.
     */
    final class RemoveAllBreakpointsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Remove all breakpoints";

        RemoveAllBreakpointsAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
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
            for (TeleTargetBreakpoint targetBreakpoint : maxVM().targetBreakpoints()) {
                targetBreakpoint.remove();
            }
            for (TeleBytecodeBreakpoint bytecodeBreakpoint : maxVM().bytecodeBreakpoints()) {
                bytecodeBreakpoint.remove();
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess() && (maxVM().bytecodeBreakpointCount() > 0  || maxVM().targetBreakpointCount() > 0));
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
        final TeleBreakpoint teleBreakpoint;

        EnableBreakpointAction(TeleBreakpoint teleBreakpoint, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            this.teleBreakpoint = teleBreakpoint;
        }

        @Override
        protected void procedure() {
            if (focus().breakpoint() == teleBreakpoint) {
                focus().setBreakpoint(null);
            }
            teleBreakpoint.setEnabled(true);
            inspection().refreshAll(false);
        }


        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess() && maxVM().bytecodeBreakpointCount() > 0);
        }
    }


    /**
     * @param surrogate for a breakpoint in the VM.
     * @param title a string name for the Action, uses default name if null.
     * @return an Action that will enable the breakpoint
     */
    public final InspectorAction enableBreakpoint(TeleBreakpoint teleBreakpoint, String title) {
        return new EnableBreakpointAction(teleBreakpoint, title);
    }


    /**
     * Action: disables a specific  breakpoint in the VM.
     */
    final class DisableBreakpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Disable breakpoint";

        final TeleBreakpoint teleBreakpoint;

        DisableBreakpointAction(TeleBreakpoint teleBreakpoint, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            this.teleBreakpoint = teleBreakpoint;
        }

        @Override
        protected void procedure() {
            if (focus().breakpoint() == teleBreakpoint) {
                focus().setBreakpoint(null);
            }
            teleBreakpoint.setEnabled(false);
            inspection().refreshAll(false);
        }
    }

    /**
     * @param surrogate for a breakpoint in the VM.
     * @param title a string name for the Action, uses default name if null.
     * @return an Action that will disable the breakpoint
     */
    public final InspectorAction disableBreakpoint(TeleBreakpoint teleBreakpoint, String title) {
        return new DisableBreakpointAction(teleBreakpoint, title);
    }


    /**
     * Action:  set a target code breakpoint at a particular address.
     */
    final class SetTargetCodeBreakpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Set target code breakpoint";

        private final Address address;

        public SetTargetCodeBreakpointAction(Address address, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            this.address = address;
            refresh(true);
        }

        @Override
        protected void procedure() {
            try {
                final TeleTargetBreakpoint breakpoint = maxVM().makeMaxTargetBreakpoint(address);
                focus().setBreakpoint(breakpoint);
            } catch (MaxVMException maxVMException) {
                gui().errorMessage(maxVMException.getMessage());
            }

        }

        @Override
        public void refresh(boolean force) {
            setEnabled(maxVM().getTargetBreakpoint(address) == null);
        }
    }

    public final InspectorAction setTargetCodeBreakpoint(Address address, String title) {
        return new SetTargetCodeBreakpointAction(address, title);
    }


    /**
     * Action:  remove a target code breakpoint at a particular address.
     */
    final class RemoveTargetCodeBreakpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Remove target code breakpoint";

        private final Address address;

        public RemoveTargetCodeBreakpointAction(Address address, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            this.address = address;
            refresh(true);
        }

        @Override
        protected void procedure() {
            final TeleTargetBreakpoint breakpoint = maxVM().getTargetBreakpoint(address);
            if (breakpoint != null) {
                breakpoint.remove();
                focus().setBreakpoint(null);
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(maxVM().getTargetBreakpoint(address) != null);
        }
    }

    public final InspectorAction removeTargetCodeBreakpoint(Address address, String title) {
        return new RemoveTargetCodeBreakpointAction(address, title);
    }


     /**
     * Action:  toggle on/off a breakpoint at the target code location of the current focus.
     */
    final class ToggleTargetCodeBreakpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Toggle target code breakpoint";

        ToggleTargetCodeBreakpointAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
            focus().addListener(new InspectionFocusAdapter() {
                @Override
                public void codeLocationFocusSet(TeleCodeLocation codeLocation, boolean interactiveForNative) {
                    refresh(false);
                }
            });
        }

        @Override
        protected void procedure() {
            final Address targetCodeInstructionAddress = focus().codeLocation().targetCodeInstructionAddress();
            if (!targetCodeInstructionAddress.isZero()) {
                TeleTargetBreakpoint breakpoint = maxVM().getTargetBreakpoint(targetCodeInstructionAddress);
                if (breakpoint == null) {
                    try {
                        breakpoint = maxVM().makeMaxTargetBreakpoint(targetCodeInstructionAddress);
                        focus().setBreakpoint(breakpoint);
                    } catch (MaxVMException maxVMException) {
                        gui().errorMessage(maxVMException.getMessage());
                    }
                } else {
                    breakpoint.remove();
                    focus().setBreakpoint(null);
                }
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess()  && focus().codeLocation().hasTargetCodeLocation());
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
     * Action:  sets a  breakpoint at the target code location specified interactively..
     */
    final class SetCustomTargetCodeBreakpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Set custom target code breakpoint...";

        SetCustomTargetCodeBreakpointAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
            refresh(true);
        }

        @Override
        protected void procedure() {
            new NativeLocationInputDialog(inspection(), "Set breakpoint at address...", maxVM().bootImageStart(), "") {
                @Override
                public void entered(Address address, String description) {
                    if (!address.isZero()) {
                        try {
                            final TeleTargetBreakpoint breakpoint = maxVM().makeMaxTargetBreakpoint(address);
                            if (breakpoint == null) {
                                gui().errorMessage("Unable to create breakpoint at: " + "0x" + address.toHexString());
                            } else {
                                breakpoint.setDescription(description);
                            }
                        } catch (MaxVMException maxVMException) {
                            gui().errorMessage(maxVMException.getMessage());
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

    private InspectorAction setCustomTargetCodeBreakpoint = new SetCustomTargetCodeBreakpointAction(null);

    /**
     * @return an Action that will toggle on/off a breakpoint at the target code location of the current focus.
     */
    public final InspectorAction setCustomTargetCodeBreakpoint() {
        return setCustomTargetCodeBreakpoint;
    }


    /**
     * Action:  sets a breakpoint at every label in the target method containing the current code location focus.
     */
    final class SetTargetCodeLabelBreakpointsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Set breakpoint at every target code label";

        SetTargetCodeLabelBreakpointsAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
            focus().addListener(new InspectionFocusAdapter() {
                @Override
                public void codeLocationFocusSet(TeleCodeLocation codeLocation, boolean interactiveForNative) {
                    refresh(false);
                }
            });
        }

        @Override
        protected void procedure() {
            final Address address = focus().codeLocation().targetCodeInstructionAddress();
            final TeleTargetRoutine teleTargetRoutine = maxVM().findTeleTargetRoutine(TeleTargetRoutine.class, address);
            if (teleTargetRoutine != null) {
                teleTargetRoutine.setTargetCodeLabelBreakpoints();
            } else {
                gui().errorMessage("Unable to find target method in which to set breakpoints");
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess() && focus().hasCodeLocation() && focus().codeLocation().hasTargetCodeLocation());
        }
    }

    private InspectorAction setTargetCodeLabelBreakpoints = new SetTargetCodeLabelBreakpointsAction(null);

    /**
     * @return an Action that will set a breakpoint at every label in the target method containing the current code location focus.
     */
    public final InspectorAction setTargetCodeLabelBreakpoints() {
        return setTargetCodeLabelBreakpoints;
    }


    /**
     * Action:  removes any breakpoints at labels in the target method containing the current code location focus.
     */
    final class RemoveTargetCodeLabelBreakpointsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Remove breakpoint at every target code label";

        RemoveTargetCodeLabelBreakpointsAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
            focus().addListener(new InspectionFocusAdapter() {
                @Override
                public void codeLocationFocusSet(TeleCodeLocation codeLocation, boolean interactiveForNative) {
                    refresh(false);
                }
            });
            inspection().addInspectionListener(new InspectionListenerAdapter() {
                @Override
                public void breakpointStateChanged() {
                    refresh(true);
                }
            });
        }

        @Override
        protected void procedure() {
            final Address address = focus().codeLocation().targetCodeInstructionAddress();
            final TeleTargetRoutine teleTargetRoutine = maxVM().findTeleTargetRoutine(TeleTargetRoutine.class, address);
            if (teleTargetRoutine != null) {
                teleTargetRoutine.removeTargetCodeLabelBreakpoints();
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess() && maxVM().targetBreakpointCount() > 0 && focus().hasCodeLocation() && focus().codeLocation().hasTargetCodeLocation());
        }
    }

    private InspectorAction removeTargetCodeLabelBreakpoints = new RemoveTargetCodeLabelBreakpointsAction(null);

    /**
     * @return an Action that will remove any breakpoints labels in the target method containing the current code location focus.
     */
    public final InspectorAction removeTargetCodeLabelBreakpoints() {
        return removeTargetCodeLabelBreakpoints;
    }


    /**
     * Action: removes all existing target code breakpoints in the VM.
     */
    final class RemoveAllTargetCodeBreakpointsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Remove all target code breakpoints";

        RemoveAllTargetCodeBreakpointsAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
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
            for (TeleTargetBreakpoint targetBreakpoint : maxVM().targetBreakpoints()) {
                targetBreakpoint.remove();
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess() && maxVM().targetBreakpointCount() > 0);
        }
    }

    private InspectorAction removeAllTargetCodeBreakpoints = new RemoveAllTargetCodeBreakpointsAction(null);

    /**
     * @return an Action that will remove all target code breakpoints in the VM.
     */
    public final InspectorAction removeAllTargetCodeBreakpoints() {
        return removeAllTargetCodeBreakpoints;
    }


    /**
     * Action:  sets target code breakpoints at  method entries to be selected interactively by name.
     */
    final class SetTargetCodeBreakpointAtMethodEntriesByNameAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Compiled methods...";

        SetTargetCodeBreakpointAtMethodEntriesByNameAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(inspection(), "Class for compiled method entry breakpoints...", "Select");
            if (teleClassActor != null) {
                final Sequence<TeleTargetMethod> teleTargetMethods = TargetMethodSearchDialog.show(inspection(), teleClassActor, "Compiled Method Entry Breakpoints", "Set Breakpoints", true);
                if (teleTargetMethods != null) {
                    // There may be multiple compilations of a method in the result.
                    TeleTargetBreakpoint teleTargetBreakpoint = null;
                    for (TeleTargetMethod teleTargetMethod : teleTargetMethods) {
                        teleTargetBreakpoint = teleTargetMethod.getTeleClassMethodActor().setTargetBreakpointAtEntry();
                    }
                    focus().setBreakpoint(teleTargetBreakpoint);
                }
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess());
        }
    }

    private final SetTargetCodeBreakpointAtMethodEntriesByNameAction setTargetCodeBreakpointAtMethodEntriesByName =
        new SetTargetCodeBreakpointAtMethodEntriesByNameAction(null);

    /**
     * @return an interactive Action that sets a target code breakpoint at  a method entry to be selected by name.
     */
    public final InspectorAction setTargetCodeBreakpointAtMethodEntriesByName() {
        return setTargetCodeBreakpointAtMethodEntriesByName;
    }


    /**
     * Action: sets target code breakpoint at object initializers of a class specified interactively by name.
     */
    final class SetTargetCodeBreakpointAtObjectInitializerAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Break in compiled object initializers of class...";

        SetTargetCodeBreakpointAtObjectInitializerAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(inspection(), "Break in Object Initializers of Class...", "Set Breakpoint");
            if (teleClassActor != null) {
                final ClassActor classActor = teleClassActor.classActor();
                if (classActor.localVirtualMethodActors() != null) {
                    for (VirtualMethodActor virtualMethodActor : classActor.localVirtualMethodActors()) {
                        if (virtualMethodActor.name == SymbolTable.INIT) {
                            final TeleClassMethodActor teleClassMethodActor = maxVM().findTeleMethodActor(TeleClassMethodActor.class, virtualMethodActor);
                            if (teleClassMethodActor != null) {
                                TeleTargetBreakpoint teleTargetBreakpoint = null;
                                for (TeleTargetMethod teleTargetMethod : teleClassMethodActor.targetMethods()) {
                                    teleTargetBreakpoint = teleTargetMethod.setTargetBreakpointAtEntry();
                                }
                                if (teleTargetBreakpoint != null) {
                                    focus().setBreakpoint(teleTargetBreakpoint);
                                }
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess());
        }
    }

    private InspectorAction setTargetCodeBreakpointAtObjectInitializer =
        new SetTargetCodeBreakpointAtObjectInitializerAction(null);

    /**
     * @return an interactive Action that will set a target code breakpoint at the
     * object initializer for a class specified by name.
     */
    public final InspectorAction setTargetCodeBreakpointAtObjectInitializer() {
        return setTargetCodeBreakpointAtObjectInitializer;
    }


    /**
     * Action:  toggle on/off a breakpoint at the  bytecode location of the current focus.
     */
    class ToggleBytecodeBreakpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Toggle bytecode breakpoint";

        ToggleBytecodeBreakpointAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
            focus().addListener(new InspectionFocusAdapter() {
                @Override
                public void codeLocationFocusSet(TeleCodeLocation codeLocation, boolean interactiveForNative) {
                    refresh(false);
                }
            });
        }

        @Override
        protected void procedure() {
            final BytecodeLocation bytecodeLocation = focus().codeLocation().bytecodeLocation();
            if (bytecodeLocation != null) {
                final TeleBytecodeBreakpoint.Key key = new TeleBytecodeBreakpoint.Key(bytecodeLocation);
                final TeleBytecodeBreakpoint breakpoint = maxVM().getBytecodeBreakpoint(key);
                if (breakpoint == null) {
                    maxVM().makeBytecodeBreakpoint(key);
                } else {
                    breakpoint.remove();
                }
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess()  && focus().hasCodeLocation() && focus().codeLocation().hasBytecodeLocation());
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
     * Action: sets a bytecode breakpoint at a method entry specified interactively by name.
     */
    final class SetBytecodeBreakpointAtMethodEntryByNameAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Method on classpath, by name...";

        SetBytecodeBreakpointAtMethodEntryByNameAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
        }

        @Override
        protected void procedure() {
            final TypeDescriptor typeDescriptor = TypeSearchDialog.show(inspection(), "Class for bytecode method entry breakpoint...", "Select");
            if (typeDescriptor != null) {
                final MethodKey methodKey = MethodSearchDialog.show(inspection(), typeDescriptor, "Bytecode method entry breakpoint", "Set Breakpoint");
                if (methodKey != null) {
                    maxVM().makeBytecodeBreakpoint(new TeleBytecodeBreakpoint.Key(methodKey, 0));
                }
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess() && maxVM().activateMessenger());
        }
    }

    private final SetBytecodeBreakpointAtMethodEntryByNameAction setBytecodeBreakpointAtMethodEntryByName =
        new SetBytecodeBreakpointAtMethodEntryByNameAction(null);

    /**
     * @return an interactive Action  that will set a target code breakpoint at  a method entry to be selected by name.
     */
    public final InspectorAction setBytecodeBreakpointAtMethodEntryByName() {
        return setBytecodeBreakpointAtMethodEntryByName;
    }


    /**
     * Action: sets a bytecode breakpoint at a method entry specified interactively by name.
     */
    final class SetBytecodeBreakpointAtMethodEntryByKeyAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Method matched by key...";

        SetBytecodeBreakpointAtMethodEntryByKeyAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
        }

        @Override
        protected void procedure() {
            final MethodKey methodKey = MethodKeyInputDialog.show(inspection(), "Specify method");
            if (methodKey != null) {
                maxVM().makeBytecodeBreakpoint(new TeleBytecodeBreakpoint.Key(methodKey, 0));
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess() && maxVM().activateMessenger());
        }
    }

    private final SetBytecodeBreakpointAtMethodEntryByKeyAction setBytecodeBreakpointAtMethodEntryByKey =
        new SetBytecodeBreakpointAtMethodEntryByKeyAction(null);

    /**
     * @return an interactive Action  that will set a target code breakpoint at  a method entry to be selected by name.
     */
    public final InspectorAction setBytecodeBreakpointAtMethodEntryByKey() {
        return setBytecodeBreakpointAtMethodEntryByKey;
    }


    /**
     * Action: removes all existing bytecode breakpoints in the VM.
     */
    final class RemoveAllBytecodeBreakpointsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Remove all bytecode breakpoints";

        RemoveAllBytecodeBreakpointsAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
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
            for (TeleBytecodeBreakpoint bytecodeBreakpoint : maxVM().bytecodeBreakpoints()) {
                bytecodeBreakpoint.remove();
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess() && maxVM().bytecodeBreakpointCount() > 0);
        }
    }

    private InspectorAction removeAllBytecodeBreakpoints = new RemoveAllBytecodeBreakpointsAction(null);

    /**
     * @return an Action that will remove all target code breakpoints in the VM.
     */
    public final InspectorAction removeAllBytecodeBreakpoints() {
        return removeAllBytecodeBreakpoints;
    }


   /**
     * Action: create a memory word watchpoint, interactive if no location specified.
     */
    final class SetWordWatchpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Watch memory word";
        private final MemoryRegion memoryRegion;

        SetWordWatchpointAction() {
            super(inspection(), "Watch memory word at address...");
            this.memoryRegion = null;
            setEnabled(true);
        }

        SetWordWatchpointAction(Address address, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            this.memoryRegion = new MemoryWordRegion(address, 1, maxVM().wordSize());
            setEnabled(maxVM().findWatchpoint(memoryRegion) == null);
        }

        @Override
        protected void procedure() {
            if (memoryRegion != null) {
                setWatchpoint(memoryRegion, "");
            } else {
                new MemoryRegionInputDialog(inspection(), maxVM().bootImageStart(), "Watch memory starting at address...", "Watch") {
                    @Override
                    public void entered(Address address, Size size) {
                        setWatchpoint(new MemoryWordRegion(address, size.toInt() / Word.size(), Size.fromInt(Word.size())), "User specified region");
                    }
                };
            }
        }

        private void setWatchpoint(MemoryRegion memoryRegion, String description) {
            final WatchpointsViewPreferences prefs = WatchpointsViewPreferences.globalPreferences(inspection());
            try {
                final MaxWatchpoint watchpoint
                    = maxVM().setRegionWatchpoint(description, memoryRegion, true, prefs.read(), prefs.write(), prefs.exec(), prefs.enableDuringGC());
                if (watchpoint == null) {
                    gui().errorMessage("Watchpoint creation failed");
                } else {
                    inspection().focus().setWatchpoint(watchpoint);
                }
            } catch (TooManyWatchpointsException tooManyWatchpointsException) {
                gui().errorMessage(tooManyWatchpointsException.getMessage());
            } catch (DuplicateWatchpointException duplicateWatchpointException) {
                gui().errorMessage(duplicateWatchpointException.getMessage());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess()  && maxVM().watchpointsEnabled());
        }
    }

    private final SetWordWatchpointAction setWordWatchpointAction = new SetWordWatchpointAction();

    /**
     * @return an interactive Action that will create a memory word watchpoint in the VM.
     */
    public final InspectorAction setWordWatchpoint() {
        return setWordWatchpointAction;
    }

    /**
     * Creates an action that will create a memory word watchpoint.
     *
     * @param address a memory location in the VM
     * @param string a title for the action, use default name if null
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
        private final MemoryRegion memoryRegion;

        SetRegionWatchpointAction() {
            super(inspection(), "Watch memory region...");
            this.memoryRegion = null;
            setEnabled(true);
        }

        SetRegionWatchpointAction(MemoryRegion memoryRegion, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            this.memoryRegion = memoryRegion;
            setEnabled(maxVM().findWatchpoint(memoryRegion) == null);
        }

        @Override
        protected void procedure() {
            if (memoryRegion != null) {
                setWatchpoint(memoryRegion, "");
            } else {
                // TODO (mlvdv) Generalize AddressInputDialog for a Region
                new AddressInputDialog(inspection(), maxVM().bootImageStart(), "Watch memory...", "Watch") {
                    @Override
                    public void entered(Address address) {
                        setWatchpoint(new FixedMemoryRegion(address, maxVM().wordSize(), ""), "User specified region");
                    }
                };
            }
        }

        private void setWatchpoint(MemoryRegion memoryRegion, String description) {
            final WatchpointsViewPreferences prefs = WatchpointsViewPreferences.globalPreferences(inspection());
            try {
                final MaxWatchpoint watchpoint
                    = maxVM().setRegionWatchpoint(description, memoryRegion, true, prefs.read(), prefs.write(), prefs.exec(), prefs.enableDuringGC());
                if (watchpoint == null) {
                    gui().errorMessage("Watchpoint creation failed");
                } else {
                    inspection().focus().setWatchpoint(watchpoint);
                }
            } catch (TooManyWatchpointsException tooManyWatchpointsException) {
                gui().errorMessage(tooManyWatchpointsException.getMessage());
            } catch (DuplicateWatchpointException duplicateWatchpointException) {
                gui().errorMessage(duplicateWatchpointException.getMessage());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess()  && maxVM().watchpointsEnabled());
        }
    }

    private final SetRegionWatchpointAction setRegionWatchpointAction = new SetRegionWatchpointAction();

    /**
     * @return an interactive Action that will create a memory  watchpoint in the VM.
     */
    public final InspectorAction setRegionWatchpoint() {
        return setRegionWatchpointAction;
    }

    /**
     * Creates an action that will create a memory watchpoint.
     *
     * @param memoryRegion an area of memory in the VM
     * @param string a title for the action, use default name if null
     * @return an Action that will set a memory watchpoint at the address.
     */
    public final InspectorAction setRegionWatchpoint(MemoryRegion memoryRegion, String string) {
        return new SetRegionWatchpointAction(memoryRegion, string);
    }


     /**
     * Action: create an object memory watchpoint.
     */
    final class SetObjectWatchpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Watch object memory";
        private final TeleObject teleObject;
        private final MemoryRegion memoryRegion;

        SetObjectWatchpointAction(TeleObject teleObject, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            this.teleObject = teleObject;
            this.memoryRegion = teleObject.getCurrentMemoryRegion();
            refresh(true);
        }

        @Override
        protected void procedure() {
            final WatchpointsViewPreferences prefs = WatchpointsViewPreferences.globalPreferences(inspection());
            try {
                final String description = "Object " + inspection().nameDisplay().referenceLabelText(teleObject);
                final MaxWatchpoint watchpoint
                    = maxVM().setObjectWatchpoint(description, teleObject, true, prefs.read(), prefs.write(), prefs.exec(), prefs.enableDuringGC());
                if (watchpoint == null) {
                    gui().errorMessage("Watchpoint creation failed");
                } else {
                    inspection().focus().setWatchpoint(watchpoint);
                }
            } catch (TooManyWatchpointsException tooManyWatchpointsException) {
                gui().errorMessage(tooManyWatchpointsException.getMessage());
            } catch (DuplicateWatchpointException duplicateWatchpointException) {
                gui().errorMessage(duplicateWatchpointException.getMessage());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess()
                && maxVM().watchpointsEnabled()
                && maxVM().findWatchpoint(memoryRegion) == null);
        }
    }

    /**
     * Creates an action that will create an object memory watchpoint.
     *
     * @param teleObject a heap object in the VM
     * @param string a title for the action, use default name if null
     * @return an Action that will set an object field watchpoint.
     */
    public final InspectorAction setObjectWatchpoint(TeleObject teleObject, String string) {
        return new SetObjectWatchpointAction(teleObject, string);
    }


    /**
     * Action: create an object field watchpoint.
     */
    final class SetFieldWatchpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Watch object field";
        private final TeleObject teleObject;
        private final FieldActor fieldActor;
        private final MemoryRegion memoryRegion;

        SetFieldWatchpointAction(TeleObject teleObject, FieldActor fieldActor, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            this.teleObject = teleObject;
            this.fieldActor = fieldActor;
            this.memoryRegion = teleObject.getCurrentMemoryRegion(fieldActor);
        }

        @Override
        protected void procedure() {
            final WatchpointsViewPreferences prefs = WatchpointsViewPreferences.globalPreferences(inspection());
            try {
                final String description = "Field \"" + fieldActor.name.toString() + "\" in " + inspection().nameDisplay().referenceLabelText(teleObject);
                final MaxWatchpoint watchpoint
                    = maxVM().setFieldWatchpoint(description, teleObject, fieldActor, true, prefs.read(), prefs.write(), prefs.exec(), prefs.enableDuringGC());
                if (watchpoint == null) {
                    gui().errorMessage("Watchpoint creation failed");
                } else {
                    inspection().focus().setWatchpoint(watchpoint);
                }
            } catch (TooManyWatchpointsException tooManyWatchpointsException) {
                gui().errorMessage(tooManyWatchpointsException.getMessage());
            } catch (DuplicateWatchpointException duplicateWatchpointException) {
                gui().errorMessage(duplicateWatchpointException.getMessage());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess()
                && maxVM().watchpointsEnabled()
                && maxVM().findWatchpoint(memoryRegion) == null);
        }
    }

    /**
     * Creates an action that will create an object field watchpoint.
     *
     * @param teleObject a heap object in the VM
     * @param fieldActor description of a field in the class type of the heap object
     * @param string a title for the action, use default name if null
     * @return an Action that will set an object field watchpoint.
     */
    public final InspectorAction setFieldWatchpoint(TeleObject teleObject, FieldActor fieldActor, String string) {
        return new SetFieldWatchpointAction(teleObject, fieldActor, string);
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
        private final MemoryRegion memoryRegion;

        SetArrayElementWatchpointAction(TeleObject teleObject, Kind elementKind, int arrayOffsetFromOrigin, int index, String indexPrefix, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            this.teleObject = teleObject;
            this.elementKind = elementKind;
            this.arrayOffsetFromOrigin = arrayOffsetFromOrigin;
            this.index = index;
            this.indexPrefix = indexPrefix;
            final Pointer address = teleObject.getCurrentOrigin().plus(arrayOffsetFromOrigin + (index * elementKind.width.numberOfBytes));
            this.memoryRegion = new FixedMemoryRegion(address, Size.fromInt(elementKind.width.numberOfBytes), "");
            refresh(true);
        }

        @Override
        protected void procedure() {
            final WatchpointsViewPreferences prefs = WatchpointsViewPreferences.globalPreferences(inspection());
            try {
                final String description = "Element " + indexPrefix + "[" + Integer.toString(index) + "] in " + inspection().nameDisplay().referenceLabelText(teleObject);
                final MaxWatchpoint watchpoint
                    = maxVM().setArrayElementWatchpoint(description, teleObject, elementKind, arrayOffsetFromOrigin, index, true, prefs.read(), prefs.write(), prefs.exec(), prefs.enableDuringGC());
                if (watchpoint == null) {
                    gui().errorMessage("Watchpoint creation failed");
                } else {
                    inspection().focus().setWatchpoint(watchpoint);
                }
            } catch (TooManyWatchpointsException tooManyWatchpointsException) {
                gui().errorMessage(tooManyWatchpointsException.getMessage());
            } catch (DuplicateWatchpointException duplicateWatchpointException) {
                gui().errorMessage(duplicateWatchpointException.getMessage());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess()
                && maxVM().watchpointsEnabled()
                && maxVM().findWatchpoint(memoryRegion) == null);
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
     * @param string a title for the action, use default name if null
     * @return an Action that will set an array element watchpoint.
     */
    public final InspectorAction setArrayElementWatchpoint(TeleObject teleObject, Kind elementKind, int arrayOffsetFromOrigin, int index, String indexPrefix, String string) {
        return new SetArrayElementWatchpointAction(teleObject, elementKind, arrayOffsetFromOrigin, index, indexPrefix, string);
    }


     /**
     * Action: create an object header field watchpoint.
     */
    final class SetHeaderWatchpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Watch object header field";
        private final TeleObject teleObject;
        private final HeaderField headerField;
        private final MemoryRegion memoryRegion;

        SetHeaderWatchpointAction(TeleObject teleObject, HeaderField headerField, String title)  {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            this.teleObject = teleObject;
            this.headerField = headerField;
            this.memoryRegion = teleObject.getCurrentMemoryRegion(headerField);
            refresh(true);
        }

        @Override
        protected void procedure() {
            final WatchpointsViewPreferences prefs = WatchpointsViewPreferences.globalPreferences(inspection());
            try {
                final String description = "Field \"" + headerField.name() + "\" in header of " + inspection().nameDisplay().referenceLabelText(teleObject);
                final MaxWatchpoint watchpoint
                    = maxVM().setHeaderWatchpoint(description, teleObject, headerField, true, prefs.read(), prefs.write(), prefs.exec(), prefs.enableDuringGC());
                if (watchpoint == null) {
                    gui().errorMessage("Watchpoint creation failed");
                } else {
                    inspection().focus().setWatchpoint(watchpoint);
                }
            } catch (TooManyWatchpointsException tooManyWatchpointsException) {
                gui().errorMessage(tooManyWatchpointsException.getMessage());
            } catch (DuplicateWatchpointException duplicateWatchpointException) {
                gui().errorMessage(duplicateWatchpointException.getMessage());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess()
                && maxVM().watchpointsEnabled()
                && maxVM().findWatchpoint(memoryRegion) == null);
        }
    }

    /**
     * Creates an action that will create an object header field watchpoint.
     *
     * @param teleObject a heap object in the VM
     * @param headerField identification of an object header field
     * @param title a title for the action, use default name if null
     * @return an Action that will set an object header watchpoint
     */
    public final InspectorAction setHeaderWatchpoint(TeleObject teleObject, HeaderField headerField, String title) {
        return new SetHeaderWatchpointAction(teleObject, headerField, title);
    }


    /**
     * Action: create an object field watchpoint.
     */
    final class SetThreadLocalWatchpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Watch thread local variable";
        private final TeleThreadLocalValues teleThreadLocalValues;
        private final int index;
        private final MemoryRegion memoryRegion;

        SetThreadLocalWatchpointAction(TeleThreadLocalValues teleThreadLocalValues, int index, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            this.teleThreadLocalValues = teleThreadLocalValues;
            this.index = index;
            this.memoryRegion = teleThreadLocalValues.getMemoryRegion(index);
            refresh(true);
        }

        @Override
        protected void procedure() {
            final WatchpointsViewPreferences prefs = WatchpointsViewPreferences.globalPreferences(inspection());
            try {
                final VmThreadLocal vmThreadLocal = teleThreadLocalValues.getVmThreadLocal(index);
                final String description = "Thread local \"" + vmThreadLocal.name
                    + "\" (" + inspection().nameDisplay().shortName(teleThreadLocalValues.getMaxThread()) + ","
                    + teleThreadLocalValues.safepointState().toString() + ")";
                final MaxWatchpoint watchpoint
                    = maxVM().setVmThreadLocalWatchpoint(description, teleThreadLocalValues, index, true, prefs.read(), prefs.write(), prefs.exec(), prefs.enableDuringGC());
                if (watchpoint == null) {
                    gui().errorMessage("Watchpoint creation failed");
                } else {
                    inspection().focus().setWatchpoint(watchpoint);
                }
            } catch (TooManyWatchpointsException tooManyWatchpointsException) {
                gui().errorMessage(tooManyWatchpointsException.getMessage());
            } catch (DuplicateWatchpointException duplicateWatchpointException) {
                gui().errorMessage(duplicateWatchpointException.getMessage());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess()
                && maxVM().watchpointsEnabled()
                && maxVM().findWatchpoint(memoryRegion) == null);
        }
    }

    /**
     * Creates an action that will create a thread local variable watchpoint.
     *
     * @param teleThreadLocalValues the set of thread local variables containing the variable
     * @param index index of the variable to watch
     * @param string a title for the action, use default name if null
     * @return an action that will create a thread local variable watchpoint
     */
    public final InspectorAction setThreadLocalWatchpoint(TeleThreadLocalValues teleThreadLocalValues, int index, String string) {
        return new SetThreadLocalWatchpointAction(teleThreadLocalValues, index, string);
    }


    /**
     * Action: remove a memory watchpoint.
     */
    final class RemoveWatchpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Un-watch memory";
        private final MemoryRegion memoryRegion;

        RemoveWatchpointAction(MemoryRegion memoryRegion, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            this.memoryRegion = memoryRegion;
            refresh(true);
        }

        @Override
        protected void procedure() {
            final MaxWatchpoint watchpoint = maxVM().findWatchpoint(memoryRegion);
            ProgramError.check(watchpoint != null, "Unable to locate field watchpoint for removal");
            if (watchpoint.dispose()) {
                inspection().focus().setWatchpoint(null);
            }  else {
                gui().errorMessage("Watchpoint removal failed");
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(inspection().hasProcess()
                && maxVM().watchpointsEnabled()
                && maxVM().findWatchpoint(memoryRegion) != null);
        }
    }

    /**
     * Creates an action that will remove a watchpoint.
     *
     * @param memoryRegion area of memory
     * @param title a title for the action, use default name if null
     * @return an Action that will remove a watchpoint, if present at memory location.
     */
    public final InspectorAction removeWatchpoint(MemoryRegion memoryRegion, String title) {
        return new RemoveWatchpointAction(memoryRegion, title);
    }


    /**
     * Action:  removes the watchpoint from the VM that is currently selected.
     */
    final class RemoveSelectedWatchpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Remove selected watchpoint";

        RemoveSelectedWatchpointAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
            focus().addListener(new InspectionFocusAdapter() {
                @Override
                public void watchpointFocusSet(MaxWatchpoint oldWatchpoint, MaxWatchpoint watchpoint) {
                    refresh(false);
                }
            });
        }

        @Override
        protected void procedure() {
            final MaxWatchpoint watchpoint = focus().watchpoint();
            if (watchpoint != null) {
                if (watchpoint.dispose()) {
                    focus().setWatchpoint(null);
                } else {
                    gui().errorMessage("Watchpoint removal failed");
                }
            } else {
                gui().errorMessage("No watchpoint selected");
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
     * Action: removes all existing watchpoints in the VM.
     */
    final class RemoveAllWatchpointsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Remove all watchpoints";

        RemoveAllWatchpointsAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
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
            for (MaxWatchpoint watchpoint : maxVM().watchpoints()) {
                if (!watchpoint.dispose()) {
                    gui().errorMessage("Failed to remove watchpoint" + watchpoint);
                }
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(maxVM().watchpoints().length() > 0);
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

        DebugPauseAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
        }

        @Override
        protected void procedure() {
            try {
                maxVM().pause();
            } catch (Exception exception) {
                gui().errorMessage("Pause could not be initiated", exception.toString());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasThread() && inspection().isVMRunning());
        }
    }

    private final DebugPauseAction debugPause = new DebugPauseAction(null);

    public final DebugPauseAction debugPause() {
        return debugPause;
    }


    /**
     * Action: resumes the running VM.
     */
    final class DebugResumeAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Resume";

        DebugResumeAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
        }

        @Override
        protected void procedure() {
            try {
                maxVM().resume(false, false);
            } catch (Exception exception) {
                gui().errorMessage("Run to instruction could not be performed.", exception.toString());
            }
        }


        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasThread() && inspection().isVMReady());
        }
    }

    private final InspectorAction debugResume = new DebugResumeAction(null);

     /**
     * @return an Action that will resume full execution of theVM.
     */
    public final InspectorAction debugResume() {
        return debugResume;
    }


    /**
     * Action:  advance the currently selected thread until it returns from its current frame in the VM,
     * ignoring breakpoints.
     */
    final class DebugReturnFromFrameAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Return from frame (ignoring breakpoints)";

        DebugReturnFromFrameAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
        }

        @Override
        protected void procedure() {
            final Address returnAddress = focus().thread().getReturnAddress();
            if (returnAddress != null) {
                try {
                    maxVM().runToInstruction(returnAddress, false, true);
                } catch (Exception exception) {
                    gui().errorMessage("Return from frame (ignoring breakpoints) could not be performed.", exception.toString());
                }
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasThread() && inspection().isVMReady());
        }
    }

    private final InspectorAction debugReturnFromFrame = new DebugReturnFromFrameAction(null);

    /**
     * @return an Action that will resume execution in the VM, stopping at the first instruction after returning
     *         from the current frame of the currently selected thread
     */
    public final InspectorAction debugReturnFromFrame() {
        return debugReturnFromFrame;
    }


    /**
     * Action:  advance the currently selected thread until it returns from its current frame
     * or hits a breakpoint in the VM.
     */
    final class DebugReturnFromFrameWithBreakpointsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Return from frame";

        DebugReturnFromFrameWithBreakpointsAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
        }

        @Override
        public void procedure() {
            final Address returnAddress = focus().thread().getReturnAddress();
            if (returnAddress != null) {
                try {
                    maxVM().runToInstruction(returnAddress, false, false);
                } catch (Exception exception) {
                    gui().errorMessage("Return from frame could not be performed.", exception.toString());
                }
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasThread() && inspection().isVMReady());
        }
    }

    private final InspectorAction debugReturnFromFrameWithBreakpoints = new DebugReturnFromFrameWithBreakpointsAction(null);

    /**
     * @return an Action that will resume execution in the VM, stopping at the first instruction after returning
     *         from the current frame of the currently selected thread, or at a breakpoint, whichever comes first.
     */
    public final InspectorAction debugReturnFromFrameWithBreakpoints() {
        return debugReturnFromFrameWithBreakpoints;
    }


    /**
     * Action:  advance the currently selected thread in the VM until it reaches the selected instruction,
     * ignoring breakpoints.
     */
    final class DebugRunToSelectedInstructionAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Run to selected instruction (ignoring breakpoints)";

        DebugRunToSelectedInstructionAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
        }

        @Override
        protected void procedure() {
            final Address selectedAddress = focus().codeLocation().targetCodeInstructionAddress();
            if (!selectedAddress.isZero()) {
                try {
                    maxVM().runToInstruction(selectedAddress, false, true);
                } catch (Exception exception) {
                    throw new InspectorError("Run to instruction (ignoring breakpoints) could not be performed.", exception);
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

    private final InspectorAction debugRunToSelectedInstruction = new DebugRunToSelectedInstructionAction(null);

    /**
     * @return an Action that will resume execution in the VM, stopping at the the currently
     * selected instruction, ignoring breakpoints.
     */
    public final InspectorAction debugRunToSelectedInstruction() {
        return debugRunToSelectedInstruction;
    }


    /**
     * Action:  advance the currently selected thread in the VM until it reaches the selected instruction
     * or a breakpoint, whichever comes first.
     */
    final class DebugRunToSelectedInstructionWithBreakpointsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Run to selected instruction";

        DebugRunToSelectedInstructionWithBreakpointsAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
        }

        @Override
        protected void procedure() {
            final Address selectedAddress = focus().codeLocation().targetCodeInstructionAddress();
            if (!selectedAddress.isZero()) {
                try {
                    maxVM().runToInstruction(selectedAddress, false, false);
                } catch (Exception exception) {
                    throw new InspectorError("Run to selection instruction could not be performed.", exception);
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

    private final InspectorAction debugRunToSelectedInstructionWithBreakpoints = new DebugRunToSelectedInstructionWithBreakpointsAction(null);

    /**
     * @return an Action that will resume execution in the VM, stopping at the selected instruction
     * or a breakpoint, whichever comes first..
     */
    public final InspectorAction debugRunToSelectedInstructionWithBreakpoints() {
        return debugRunToSelectedInstructionWithBreakpoints;
    }


    /**
     * Action:  advance the currently selected thread in the VM until it reaches the next call instruction,
     * ignoring breakpoints; fails if there is no known call in the method containing the IP.
     */
    final class DebugRunToNextCallAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Run to next call instruction (ignoring breakpoints)";

        DebugRunToNextCallAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
        }

        @Override
        protected void procedure() {
            final Address address = focus().codeLocation().targetCodeInstructionAddress();
            final TeleTargetMethod teleTargetMethod = maxVM().findTeleTargetRoutine(TeleTargetMethod.class, address);
            if (teleTargetMethod != null) {
                final Address nextCallAddress = teleTargetMethod.getNextCallAddress(address);
                if (!nextCallAddress.isZero()) {
                    try {
                        maxVM().runToInstruction(nextCallAddress, false, true);
                    } catch (Exception exception) {
                        throw new InspectorError("Run to next call instruction (ignoring breakpoints) could not be performed.", exception);
                    }
                }
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasThread() && focus().hasCodeLocation() && inspection().isVMReady());
        }
    }

    private final InspectorAction debugRunToNextCall = new DebugRunToNextCallAction(null);

    /**
     * @return an Action that will resume execution in the VM, stopping at the next call instruction,
     * ignoring breakpoints; fails if there is no known call in the method containing the IP.
     */
    public final InspectorAction debugRunToNextCall() {
        return debugRunToNextCall;
    }


    /**
     * Action:  advance the currently selected thread in the VM until it reaches the selected instruction
     * or a breakpoint.
     */
    final class DebugRunToNextCallWithBreakpointsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Run to next call instruction";

        DebugRunToNextCallWithBreakpointsAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
        }

        @Override
        protected void procedure() {
            final Address address = focus().codeLocation().targetCodeInstructionAddress();
            final TeleTargetMethod teleTargetMethod = maxVM().findTeleTargetRoutine(TeleTargetMethod.class, address);
            if (teleTargetMethod != null) {
                final Address nextCallAddress = teleTargetMethod.getNextCallAddress(address);
                if (!nextCallAddress.isZero()) {
                    try {
                        maxVM().runToInstruction(nextCallAddress, false, false);
                    } catch (Exception exception) {
                        throw new InspectorError("Run to next call instruction could not be performed.", exception);
                    }
                }
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasThread() && focus().hasCodeLocation() && inspection().isVMReady());
        }
    }

    private final InspectorAction debugNextRunToCallWithBreakpoints = new DebugRunToNextCallWithBreakpointsAction(null);

    /**
     * @return an Action that will resume execution in the VM, stopping at the first instruction after returning
     *         from the current frame of the currently selected thread, or at a breakpoint, whichever comes first.
     */
    public final InspectorAction debugRunToNextCallWithBreakpoints() {
        return debugNextRunToCallWithBreakpoints;
    }


    /**
     * Action:  advances the currently selected thread one step in the VM.
     */
    class DebugSingleStepAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Single instruction step";

        DebugSingleStepAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
        }

        @Override
        public  void procedure() {
            final MaxThread thread = focus().thread();
            try {
                maxVM().singleStep(thread, false);
            } catch (Exception exception) {
                gui().errorMessage("Couldn't single step", exception.toString());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasThread() && inspection().isVMReady());
        }
    }

    private final InspectorAction debugSingleStep = new DebugSingleStepAction(null);

    /**
     * @return an action that will single step the currently selected thread in the VM
     */
    public final InspectorAction debugSingleStep() {
        return debugSingleStep;
    }


    /**
     * Action:   resumes execution of the VM, stopping at the one immediately after the current
     *         instruction of the currently selected thread.
     */
    final class DebugStepOverAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Step over (ignoring breakpoints)";

        DebugStepOverAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
        }

        @Override
        protected void procedure() {
            final MaxThread thread = focus().thread();
            try {
                maxVM().stepOver(thread, false, true);
            } catch (Exception exception) {
                gui().errorMessage("Step over (ignoring breakpoints) could not be performed.", exception.toString());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasThread() && inspection().isVMReady());
        }
    }

    private final InspectorAction debugStepOver = new DebugStepOverAction(null);

    /**
     * @return an Action that will resume execution of the VM, stopping at the one immediately after the current
     *         instruction of the currently selected thread
     */
    public final InspectorAction debugStepOver() {
        return debugStepOver;
    }


    /**
     * Action:   resumes execution of the VM, stopping at the one immediately after the current
     *         instruction of the currently selected thread or at a breakpoint.
     */
    final class DebugStepOverWithBreakpointsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Step over";

        DebugStepOverWithBreakpointsAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
        }

        @Override
        protected void procedure() {
            final MaxThread thread = focus().thread();
            try {
                maxVM().stepOver(thread, false, false);
            } catch (Exception exception) {
                gui().errorMessage("Step over could not be performed.", exception.toString());
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(focus().hasThread() && inspection().isVMReady());
        }
    }

    private final InspectorAction debugStepOverWithBreakpoints = new DebugStepOverWithBreakpointsAction(null);

    /**
     * @return an Action that will resume execution of the VM, stopping at the one immediately after the current
     *         instruction of the currently selected thread
     */
    public final InspectorAction debugStepOverWithBreakpoints() {
        return debugStepOverWithBreakpoints;
    }


    /**
     * Action:  displays and highlights an inspection of the current Java frame descriptor.
     */
    final class InspectJavaFrameDescriptorAction extends InspectorAction {
        private static final String DEFAULT_TITLE = "Inspect Java frame descriptor";
        private TargetJavaFrameDescriptor targetJavaFrameDescriptor;
        private TargetABI abi;

        InspectJavaFrameDescriptorAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            refreshableActions.append(this);
            focus().addListener(new InspectionFocusAdapter() {
                @Override
                public void codeLocationFocusSet(TeleCodeLocation codeLocation, boolean interactiveForNative) {
                    refresh(false);
                }
            });
        }

        @Override
        protected void procedure() {
            assert targetJavaFrameDescriptor != null;
            TargetJavaFrameDescriptorInspector.make(inspection(), targetJavaFrameDescriptor, abi).highlight();
        }

        /**
         * @return whether there is a Java frame descriptor at the focus target code location
         */
        private boolean inspectable() {
            final Address instructionAddress = focus().codeLocation().targetCodeInstructionAddress();
            if (instructionAddress.isZero()) {
                return false;
            }
            final TeleTargetMethod teleTargetMethod = maxVM().makeTeleTargetMethod(instructionAddress);
            if (teleTargetMethod != null) {
                final int stopIndex = teleTargetMethod.getJavaStopIndex(instructionAddress);
                if (stopIndex >= 0) {
                    targetJavaFrameDescriptor = teleTargetMethod.getJavaFrameDescriptor(stopIndex);
                    if (targetJavaFrameDescriptor == null) {
                        return false;
                    }
                    abi = teleTargetMethod.getAbi();
                    return true;
                }
            }
            targetJavaFrameDescriptor = null;
            abi = null;
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

        ViewFocusAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
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
     * Action:  lists to the console this history of the VM state.
     */
    final class ListVMStateHistoryAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "List VM state history";

        ListVMStateHistoryAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            maxVM().describeVMStateHistory(System.out);
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
     * Action:  lists to the console all entries in the {@link TeleCodeRegistry}.
     */
    final class ListCodeRegistryAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "List code registry contents";

        ListCodeRegistryAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            maxVM().describeTeleTargetRoutines(System.out);
        }
    }

    private InspectorAction listCodeRegistry = new ListCodeRegistryAction(null);

    /**
     * @return an Action that will list to the console the entries in the {@link TeleCodeRegistry}.
     */
    public final InspectorAction listCodeRegistry() {
        return listCodeRegistry;
    }


    /**
     * Action:  lists to the console all entries in the {@link TeleCodeRegistry} to an interactively specified file.
     */
    final class ListCodeRegistryToFileAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "List code registry contents to a file...";

        ListCodeRegistryToFileAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            //Create a file chooser
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
            fileChooser.setDialogTitle("Save code registry summary to file:");
            final int returnVal = fileChooser.showSaveDialog(inspection().gui().frame());
            if (returnVal != JFileChooser.APPROVE_OPTION) {
                return;
            }
            final File file = fileChooser.getSelectedFile();
            if (file.exists() && !gui().yesNoDialog("File " + file + "exists.  Overwrite?\n")) {
                return;
            }
            try {
                final PrintStream printStream = new PrintStream(new FileOutputStream(file, false));
                maxVM().describeTeleTargetRoutines(printStream);
            } catch (FileNotFoundException fileNotFoundException) {
                gui().errorMessage("Unable to open " + file + " for writing:" + fileNotFoundException);
            }
        }
    }

    private InspectorAction listCodeRegistryToFile = new ListCodeRegistryToFileAction(null);

    /**
     * @return an interactive Action that will list to a specified file the entries in the {@link TeleCodeRegistry}.
     */
    public final InspectorAction listCodeRegistryToFile() {
        return listCodeRegistryToFile;
    }

}
