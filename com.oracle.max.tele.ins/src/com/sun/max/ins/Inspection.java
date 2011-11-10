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
package com.sun.max.ins;

import static com.sun.max.ins.MaxineInspector.*;
import static com.sun.max.tele.MaxInspectionMode.*;
import static com.sun.max.tele.MaxProcessState.*;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;

import com.sun.cri.ci.*;
import com.sun.max.ins.InspectionPreferences.ExternalViewerType;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.util.*;
import com.sun.max.ins.view.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.tele.*;
import com.sun.max.tele.util.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;

/**
 * Holds the user interaction state for the inspection of a VM, which is accessed via a surrogate implementing {@link MaxVM}.
 */
public final class Inspection implements InspectionHolder {

    private static final int TRACE_VALUE = 1;

    /**
     * @return a string suitable for tagging all trace lines; mention the thread if it isn't the AWT event handler.
     */
    private String tracePrefix() {
        if (java.awt.EventQueue.isDispatchThread()) {
            return "[Inspection] ";
        }
        return "[Inspection: " + Thread.currentThread().getName() + "] ";
    }

    private final MaxVM vm;

    private final String bootImageFileName;

    private final OptionSet options;

    private final InspectorNameDisplay nameDisplay;

    private final InspectionFocus focus;

    private final InspectionPreferences  preferences;

    private static final String SETTINGS_FILE_NAME = "maxine.ins";
    private final InspectionSettings settings;

    private final InspectionActions inspectionActions;

    private final InspectionViews inspectionViews;

    private InspectorMainFrame inspectorMainFrame;

    public Inspection(MaxVM vm, OptionSet options) {
        Trace.begin(TRACE_VALUE, tracePrefix() + "Initializing");
        final long startTimeMillis = System.currentTimeMillis();
        this.vm = vm;
        this.options = options;
        this.bootImageFileName = vm.bootImageFile().getAbsolutePath().toString();
        this.nameDisplay = new InspectorNameDisplay(this);
        this.focus = new InspectionFocus(this);
        this.settings = new InspectionSettings(this, new File(vm.programFile().getParentFile(), SETTINGS_FILE_NAME));
        this.preferences = new InspectionPreferences(this, settings);
        this.inspectionActions = new InspectionActions(this);
        this.inspectionViews = new InspectionViews(this);

        BreakpointPersistenceManager.initialize(this);
        inspectionActions.refresh(true);

        vm.addVMStateListener(new VMStateListener());
        vm.breakpointManager().addListener(new BreakpointListener());
        if (vm.watchpointManager() != null) {
            vm.watchpointManager().addListener(new WatchpointListener());
        }

        inspectorMainFrame = new InspectorMainFrame(this, MaxineInspector.NAME, nameDisplay, settings, inspectionActions);


        if (vm.state().processState() == UNKNOWN) {
            // Inspector is working with a boot image only, no process exists.
            inspectionViews.activateInitialViews();
        } else {
            try {
                // Choose an arbitrary thread as the "current" thread. If the Inspector is
                // creating the process to be debugged (as opposed to attaching to it), then there
                // should only be one thread.
                final List<MaxThread> threads = vm().state().threads();
                MaxThread nonJavaThread = null;
                for (MaxThread thread : threads) {
                    if (thread.isJava()) {
                        focus.setThread(thread);
                        nonJavaThread = null;
                        break;
                    }
                    nonJavaThread = thread;
                }
                if (nonJavaThread != null) {
                    focus.setThread(nonJavaThread);
                }
                inspectionViews.activateInitialViews();
                focus.setCodeLocation(focus.thread().ipLocation());
            } catch (Throwable throwable) {
                InspectorWarning.message(null, "Error during initialization", throwable);
                throwable.printStackTrace();
                System.exit(1);
            }
        }
        refreshAll(false);
        inspectorMainFrame.refresh(true);
        inspectorMainFrame.setVisible(true);

        Trace.end(TRACE_VALUE, tracePrefix() + "Initializing", startTimeMillis);
    }

    public Inspection inspection() {
        return this;
    }

    public MaxVM vm() {
        return vm;
    }

    public InspectorGUI gui() {
        return inspectorMainFrame;
    }

    public InspectionFocus focus() {
        return focus;
    }

    public InspectionViews views() {
        return inspectionViews;
    }

    public InspectionActions actions() {
        return inspectionActions;
    }

    public InspectionPreferences preference() {
        return preferences;
    }

    public OptionSet options() {
        return options;
    }

    /**
     * Updates the string appearing the outermost window frame: program name, process state, boot image filename.
     */
    public String currentInspectionTitle() {
        final StringBuilder sb = new StringBuilder(50);
        sb.append(NAME);
        sb.append(" (mode=").append(vm().inspectionMode().toString()).append(")");
        if (vm().inspectionMode() != IMAGE) {
            sb.append(" VM Process ");
            final MaxVMState vmState = vm().state();
            if (vmState == null) {
                sb.append(UNKNOWN.label());
            } else {
                sb.append(vmState.processState().label());
                if (vmState.isInGC()) {
                    sb.append(", in GC");
                }
                if (vmState.isInEviction()) {
                    sb.append(", in Eviction");
                }
            }
        }
        return sb.toString();
    }

    public InspectionSettings settings() {
        return settings;
    }

    /**
     * @return Inspection utility for generating standard, human-intelligible names for entities in the inspection
     *         environment.
     */
    public InspectorNameDisplay nameDisplay() {
        return nameDisplay;
    }

    /**
     * @return Is the Inspector in debugging mode with a legitimate process?
     */
    public boolean hasProcess() {
        final MaxProcessState processState = vm().state().processState();
        return !(processState == UNKNOWN || processState == TERMINATED);
    }

    /**
     * Is the VM running, as of the most recent direct (synchronous) notification by the VM?
     *
     * @return VM state == {@link MaxProcessState#RUNNING}.
     */
    public boolean isVMRunning() {
        return vm().state().processState() == RUNNING;
    }

    /**
     * Is the VM available to start running, as of the most recent direct (synchronous) notification by the VM?
     *
     * @return VM state == {@link MaxProcessState#STOPPED}.
     */
    public boolean isVMReady() {
        return vm().state().processState() == STOPPED;
    }

    private MaxVMState lastVMStateProcessed = null;

    /**
     * Gets a copy of the current set of inspection listeners. This is useful for iterating
     * over the listeners where the body of the loop may change {@link #inspectionListeners}.
     */
    private InspectionListener[] copyInspectionListeners() {
        return inspectionListeners.toArray(new InspectionListener[inspectionListeners.size()]);
    }

    /**
     * Handles reported changes in the {@linkplain MaxVM#state()  VM process state}.
     * Must only be run in AWT event thread.
     */
    private void processVMStateChange() {
        // Ensure that we're just looking at one state while making decisions, even
        // though display elements may find the VM in a newer state by the time they
        // attempt to update their state.
        final MaxVMState vmState = vm().state();
        final String stateDescription = vm.state().toString();
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + "process new VM state=" + stateDescription);
        tracer.begin();
        inspectorMainFrame.refresh(true);
        if (!vmState.newerThan(lastVMStateProcessed)) {
            Trace.line(1, tracePrefix() + "redundant state change=" + stateDescription);
        }
        lastVMStateProcessed = vmState;
        switch (vmState.processState()) {
            case STOPPED:
                updateAfterVMStopped();
                break;
            case RUNNING:
                break;
            case TERMINATED:
                Trace.line(1, tracePrefix() + " - VM process terminated");
                // Clear any possibly misleading view state.
                focus().clearAll();
                // Give all process-sensitive views a chance to shut down
                for (InspectionListener listener : copyInspectionListeners()) {
                    listener.vmProcessTerminated();
                }
                // Clear any possibly misleading view state.
                focus().clearAll();
                // Be sure all process-sensitive actions are disabled.
                inspectionActions.refresh(false);
                break;
            case NONE:
            case UNKNOWN:
                break;
        }
        inspectorMainFrame.refresh(true);
        inspectionActions.refresh(true);
        tracer.end();
    }

    /**
     * Handles reported changes in the {@linkplain MaxVM#state() VM state}.
     * Updates state synchronously, then posts an event for follow-up on the AST event thread
     */
    private final class VMStateListener implements MaxVMStateListener {

        public void stateChanged(final MaxVMState vmState) {
            Trace.line(TRACE_VALUE, tracePrefix() + "notified vmState=" + vmState);
            for (MaxThread thread : vmState.threadsStarted()) {
                Trace.line(TRACE_VALUE, tracePrefix() + "started: " + thread);
            }
            for (MaxThread thread : vmState.threadsDied()) {
                Trace.line(TRACE_VALUE, tracePrefix() + "died: " + thread);
            }
            if (java.awt.EventQueue.isDispatchThread()) {
                processVMStateChange();
            } else {
                Tracer tracer = null;
                if (Trace.hasLevel(TRACE_VALUE)) {
                    tracer = new Tracer("scheduled " + vmState);
                }
                Trace.begin(TRACE_VALUE, tracer);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        processVMStateChange();
                    }
                    @Override
                    public String toString() {
                        return "processVMStateChange";
                    }
                });
                Trace.end(TRACE_VALUE, tracer);
            }
        }
    }

    /**
     * Propagates reported breakpoint changes in the VM.
     * Ensures that notification is handled only on the
     * AWT event thread.
     */
    private final class BreakpointListener implements MaxBreakpointListener {

        public void breakpointsChanged() {
            Runnable runnable = new Runnable() {
                public void run() {
                    Trace.begin(TRACE_VALUE, tracePrefix() + "breakpoint state change notification");
                    for (InspectionListener listener : copyInspectionListeners()) {
                        listener.breakpointStateChanged();
                    }
                    Trace.end(TRACE_VALUE, tracePrefix() + "breakpoint state change notification");
                }
            };
            if (java.awt.EventQueue.isDispatchThread()) {
                runnable.run();
            } else {
                SwingUtilities.invokeLater(runnable);
            }
        }
    }

    /**
     * Propagates reported watchpoint changes in the VM.
     * Ensures that notification is handled only on the
     * AWT event thread.
     */
    private final class WatchpointListener implements MaxWatchpointListener {

        public void watchpointsChanged() {
            Runnable runnable = new Runnable() {
                public void run() {
                    Trace.begin(TRACE_VALUE, tracePrefix() + "watchpoint state change notification");
                    for (InspectionListener listener : copyInspectionListeners()) {
                        listener.watchpointSetChanged();
                    }
                    Trace.end(TRACE_VALUE, tracePrefix() + "watchpoint state change notification");
                }
            };
            if (java.awt.EventQueue.isDispatchThread()) {
                runnable.run();
            } else {
                SwingUtilities.invokeLater(runnable);
            }
        }
    }

    private InspectorAction currentAction = null;

    /**
     * Holds the action currently being performed; null when finished.
     */
    public InspectorAction currentAction() {
        return currentAction;
    }

    void setCurrentAction(InspectorAction action) {
        currentAction = action;
    }

    /**
     * @return default title for any messages: defaults to name of current {@link InspectorAction} if one is current,
     *         otherwise the generic name of the Inspector.
     */
    public String currentActionTitle() {
        return currentAction != null ? currentAction.name() : MaxineInspector.NAME;
    }

    private Set<InspectionListener> inspectionListeners = CiUtil.newIdentityHashSet();

    /**
     * Adds a listener for view update when VM state changes.
     */
    public void addInspectionListener(InspectionListener listener) {
        Trace.line(TRACE_VALUE, tracePrefix() + "adding inspection listener: " + listener);
        inspectionListeners.add(listener);
    }

    /**
     * Removes a listener for view update, for example when an Inspector is disposed or when the default notification
     * mechanism is being overridden.
     */
    public void removeInspectionListener(InspectionListener listener) {
        Trace.line(TRACE_VALUE, tracePrefix() + "removing inspection listener: " + listener);
        inspectionListeners.remove(listener);
    }

    /**
     * Update all views by reading from VM state as needed.
     *
     * @param force suspend caching behavior; reload state unconditionally.
     */
    public void refreshAll(boolean force) {
        // Additional listeners may come and go during the update cycle, which can be ignored.
        for (InspectionListener listener : copyInspectionListeners()) {
            listener.vmStateChanged(force);
        }
        inspectionActions.refresh(force);
    }

    /**
     * Updates all views, assuming that display and style parameters may have changed; forces state reload from the
     * VM.
     */
    void updateViewConfiguration() {
        for (InspectionListener listener : inspectionListeners) {
            Trace.line(TRACE_VALUE, tracePrefix() + "updateViewConfiguration: " + listener);
            listener.viewConfigurationChanged();
        }
        inspectionActions.redisplay();
        inspectorMainFrame.redisplay();
    }

    /**
     * This is the main update loop for all inspection state after a period of VM execution.
     * Determines what happened in VM execution that just concluded. Then updates all view state as needed.
     */
    public void updateAfterVMStopped() {
        gui().showInspectorBusy(true);
        // Clear any breakpoint selection; if we're at a breakpoint, it will be highlighted.
        // This also avoids a regrettable event bug, where the breakpoint view decides
        // on update to send the method viewer to the currently selected breakpoint, even
        // if it has nothing to do with where we are.
        focus().setBreakpoint(null);
        if (!focus().thread().isLive()) {
            // Our most recent thread focus died; pick a new one to maintain the
            // invariant, even if another one gets set eventually.
            focus().setThread(vm().state().threads().get(0));
        }
        try {
            // Notify all listeners (Inspectors, menu items, etc.() that
            // there has been a significant VM state change.
            refreshAll(false);
            // Make visible the code at the IP of the thread that triggered the breakpoint
            // or the memory location that triggered a watchpoint
            final MaxWatchpointEvent watchpointEvent = vm().state().watchpointEvent();
            if (watchpointEvent != null) {
                focus().setThread(watchpointEvent.thread());
                focus().setWatchpoint(watchpointEvent.watchpoint());
                focus().setAddress(watchpointEvent.address());
            } else if (!vm().state().breakpointEvents().isEmpty()) {
                final MaxThread thread = vm().state().breakpointEvents().get(0).thread();
                if (thread != null) {
                    focus().setThread(thread);
                } else {
                    // If there was no selection based on breakpoint, then check the thread that was selected before the
                    // change.
                    InspectorError.check(focus().thread().isLive(), "Selected thread no longer valid");
                }
            }
            // Reset focus to new IP.
            final MaxThread focusThread = focus().thread();
            focus().setStackFrame(focusThread.stack().top(), false);
        } catch (Throwable throwable) {
            InspectorError.unexpected("could not update view", throwable).display(this);
        } finally {
            gui().showInspectorBusy(false);
        }
    }

    /**
     * Make a standard announcement that an action has failed because the Inspector
     * was unable to acquire the lock on the VM.
     *
     * @param attemptedAction description of what was being attempted
     */
    public void announceVMBusyFailure(String attemptedAction) {
        gui().errorMessage(attemptedAction + " failed: VM Busy");
    }

    /**
     * Saves any persistent state, then shuts down VM process if needed and inspection.
     */
    public void quit() {
        for (InspectionListener listener : inspectionListeners) {
            Trace.line(TRACE_VALUE, tracePrefix() + "inspection quitting: " + listener);
            listener.inspectionEnding();
        }
        settings().quit();
        try {
            if (vm().state().processState() != TERMINATED) {
                vm().terminateVM();
            }
        } catch (Exception exception) {
            InspectorWarning.message(null, "error during VM termination: " + exception);
        } finally {
            Trace.line(1, tracePrefix() + " exiting, Goodbye");
            System.exit(0);
        }
    }

    /**
     * If an external viewer has been {@linkplain #setExternalViewer(ExternalViewerType) configured}, attempt to view a
     * source file location corresponding to a given bytecode location. The view attempt is only made if an existing
     * source file and source line number can be derived from the given bytecode location.
     *
     * @param codePos specifies a bytecode position in a class method actor
     * @return true if a file viewer was opened
     */
    public boolean viewSourceExternally(CiCodePos codePos) {
        if (preferences.externalViewerType() == ExternalViewerType.NONE) {
            return false;
        }
        final ClassMethodActor classMethodActor = (ClassMethodActor) codePos.method;
        final CodeAttribute codeAttribute = classMethodActor.codeAttribute();
        final int lineNumber = codeAttribute.lineNumberTable().findLineNumber(codePos.bci);
        if (lineNumber == -1) {
            return false;
        }
        return viewSourceExternally(classMethodActor.holder(), lineNumber);
    }

    /**
     * If an external viewer has been {@linkplain #setExternalViewer(ExternalViewerType) configured}, attempt to view a
     * source file location corresponding to a given class actor and line number. The view attempt is only made if an
     * existing source file and source line number can be derived from the given bytecode location.
     *
     * @param classActor the class whose source file is to be viewed
     * @param lineNumber the line number at which the viewer should position the current focus point
     * @return true if a file viewer was opened
     */
    public boolean viewSourceExternally(ClassActor classActor, int lineNumber) {
        if (preferences.externalViewerType() == ExternalViewerType.NONE) {
            return false;
        }
        final File javaSourceFile = vm().findJavaSourceFile(classActor);
        if (javaSourceFile == null) {
            return false;
        }

        switch (preferences.externalViewerType()) {
            case PROCESS: {
                final String config = preferences.externalViewerConfig().get(ExternalViewerType.PROCESS);
                if (config != null) {
                    final String command = config.replaceAll("\\$file", javaSourceFile.getAbsolutePath()).replaceAll("\\$line", String.valueOf(lineNumber));
                    try {
                        Trace.line(1, tracePrefix() + "Opening file by executing " + command);
                        Runtime.getRuntime().exec(command);
                    } catch (IOException ioException) {
                        InspectorWarning.message(this, "Error opening file by executing " + command, ioException);
                        return false;
                    }
                }
                break;
            }
            case SOCKET: {
                final String hostname = null;
                final String portString = preferences.externalViewerConfig().get(ExternalViewerType.SOCKET);
                if (portString != null) {
                    try {
                        final int port = Integer.parseInt(portString);
                        final Socket fileViewer = new Socket(hostname, port);
                        final String command = javaSourceFile.getAbsolutePath() + "|" + lineNumber;
                        Trace.line(1, tracePrefix() + "Opening file '" + command + "' via localhost:" + portString);
                        final OutputStream fileViewerStream = fileViewer.getOutputStream();
                        fileViewerStream.write(command.getBytes());
                        fileViewerStream.flush();
                        fileViewer.close();
                    } catch (IOException ioException) {
                        InspectorWarning.message(this, "Error opening file via localhost:" + portString + ": " + ioException);
                        return false;
                    }
                }
                break;
            }
            default: {
                InspectorError.unknownCase();
            }
        }
        return true;
    }

    /**
     * An object that delays evaluation of a trace message for controller actions.
     */
    private class Tracer {

        private final String message;

        /**
         * An object that delays evaluation of a trace message.
         *
         * @param message identifies what is being traced
         */
        public Tracer(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return tracePrefix() + message;
        }
    }

}
