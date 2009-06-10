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

import static com.sun.max.tele.debug.ProcessState.*;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;

import com.sun.max.collect.*;
import com.sun.max.ins.InspectionPreferences.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.object.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;

/**
 * Holds the user interaction state for the inspection of a Maxine VM, which is accessed via a surrogate implementing {@link MaxVM}.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class Inspection {

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

    private static final String INSPECTOR_NAME = "Maxine Inspector";

    private final MaxVM _maxVM;

    private final String _bootImageFileName;

    private final InspectorNameDisplay _nameDisplay;

    private final InspectionFocus _focus;

    private final InspectionPreferences  _preferences;

    private static final String _SETTINGS_FILE_NAME = "maxine.ins";
    private final InspectionSettings _settings;

    private final InspectionActions _inspectionActions;

    private InspectorMainFrame _inspectorMainFrame;

    public Inspection(MaxVM maxVM) {
        Trace.begin(TRACE_VALUE, tracePrefix() + "Initializing");
        final long startTimeMillis = System.currentTimeMillis();
        _maxVM = maxVM;
        _bootImageFileName = maxVM().bootImageFile().getAbsolutePath().toString();
        _nameDisplay = new InspectorNameDisplay(this);
        _focus = new InspectionFocus(this);
        _settings = new InspectionSettings(this, new File(maxVM().programFile().getParentFile(), _SETTINGS_FILE_NAME));
        _preferences = new InspectionPreferences(this, _settings);
        _inspectionActions = new InspectionActions(this);

        BreakpointPersistenceManager.initialize(this);
        _inspectionActions.refresh(true);
        // Listen for VM state changes
        maxVM().addVMStateObserver(new VMStateObserver());
        // Listen for changes in breakpoints
        maxVM().addBreakpointObserver(new BreakpointObserver());

        _inspectorMainFrame = new InspectorMainFrame(this, INSPECTOR_NAME, _settings, _inspectionActions);

        MethodInspector.Manager.make(this);
        ObjectInspectorFactory.make(this);

        if (maxVMState().processState() == NO_PROCESS) {
            // Inspector is working with a boot image only, no process exists.

            // Initialize the CodeManager and ClassRegistry, which seems to keep some heap reads
            // in the BootImageInspecor from crashing when there's no VM running (mlvdv)
//          if (teleVM.isBootImageRelocated()) {
//          teleVM.teleCodeRegistry();
//          }
        } else {
            try {
                // Choose an arbitrary thread as the "current" thread. If the inspector is
                // creating the process to be debugged (as opposed to attaching to it), then there
                // should only be one thread.
                final IterableWithLength<MaxThread> threads = maxVMState().threads();
                MaxThread nonJavaThread = null;
                for (MaxThread thread : threads) {
                    if (thread.isJava()) {
                        _focus.setThread(thread);
                        nonJavaThread = null;
                        break;
                    }
                    nonJavaThread = thread;
                }
                if (nonJavaThread != null) {
                    _focus.setThread(nonJavaThread);
                }
                // TODO (mlvdv) decide whether to make inspectors visible based on preference and previous session
                ThreadsInspector.make(this);
                RegistersInspector.make(this);
                ThreadLocalsInspector.make(this);
                StackInspector.make(this);
                BreakpointsInspector.make(this);
                _focus.setCodeLocation(maxVM.createCodeLocation(_focus.thread().instructionPointer()), false);
            } catch (Throwable throwable) {
                System.err.println("Error during initialization");
                throwable.printStackTrace();
                System.exit(1);
            }
        }
        refreshAll(false);
        _inspectorMainFrame.refresh(true);
        _inspectorMainFrame.setVisible(true);

        Trace.end(TRACE_VALUE, tracePrefix() + "Initializing", startTimeMillis);
    }

    /**
     * Updates the string appearing the outermost window frame: program name, process state, boot image filename.
     */
    public String currentInspectionTitle() {
        final StringBuilder sb = new StringBuilder(50);
        sb.append(INSPECTOR_NAME);
        sb.append(" (");
        sb.append(maxVMState() == null ? "" : maxVMState().processState());
        sb.append(") ");
        sb.append(_bootImageFileName);
        return sb.toString();
    }

    /**
     * @return a GUI panel suitable for setting global preferences for the inspection session.
     */
    public JPanel globalPreferencesPanel() {
        return _preferences.getPanel();
    }

    public InspectionSettings settings() {
        return _settings;
    }

    public MaxVM maxVM() {
        return _maxVM;
    }

    /**
     * @return The immutable history of Maxine VM state, updated immediately
     * and synchronously; thread safe.
     */
    private MaxVMState maxVMState() {
        return _maxVM.maxVMState();
    }

    public InspectorGUI gui() {
        return _inspectorMainFrame;
    }

    /**
     * @return the global collection of actions, many of which are singletons with state that gets refreshed.
     */
    public InspectionActions actions() {
        return _inspectionActions;
    }

    /**
     * The current configuration for visual style.
     */
    public InspectorStyle style() {
        return _preferences.style();
    }

    /**
     * Default size and layout for windows; overridden by persistent settings from previous sessions.
     */
    public InspectorGeometry geometry() {
        return _preferences.geometry();
    }

    /**
     * @return Inspection utility for generating standard, human-intelligible names for entities in the inspection
     *         environment.
     */
    public InspectorNameDisplay nameDisplay() {
        return _nameDisplay;
    }

    /**
     * @return Does the Inspector attempt to discover proactively what word values might point to in the VM.
     */
    public boolean investigateWordValues() {
        return _preferences.investigateWordValues();
    }

    /**
     * Informs this inspection of a new action that can operate on this inspection.
     */
    public void registerAction(InspectorAction inspectorAction) {
        _preferences.registerAction(inspectorAction);
    }

    /**
     * User oriented focus on particular items in the environment; View state.
     */
    public InspectionFocus focus() {
        return _focus;
    }

    /**
     * @return Is the Inspector in debugging mode with a legitimate process?
     */
    public boolean hasProcess() {
        final ProcessState processState = maxVMState().processState();
        return !(processState == NO_PROCESS || processState == TERMINATED);
    }

    /**
     * Is the VM running, as of the most recent direct (synchronous) notification by the VM?
     *
     * @return VM state == {@link ProcessState#RUNNING}.
     */
    public boolean isVMRunning() {
        return maxVMState().processState() == RUNNING;
    }

    /**
     * Is the VM available to start running, as of the most recent direct (synchronous) notification by the VM?
     *
     * @return VM state == {@link ProcessState#STOPPED}.
     */
    public boolean isVMReady() {
        return maxVMState().processState() == STOPPED;
    }

    private MaxVMState _lastVMStateProcessed = null;

    /**
     * Handles reported changes in the {@linkplain MaxVM#maxVMState()  VM process state}.
     * Must only be run in AWT event thread.
     */
    private void processVMStateChange() {
        // Ensure that we're just looking at one state while making decisions, even
        // though display elements may find the VM in a newer state by the time they
        // attempt to update their state.
        _inspectorMainFrame.refresh(true);
        final MaxVMState maxVMState = maxVMState();
        if (!maxVMState.newerThan(_lastVMStateProcessed)) {
            Trace.line(1, tracePrefix() + "ignoring redundant state change=" + maxVMState);
        }
        _lastVMStateProcessed = maxVMState;
        Tracer tracer = null;
        if (Trace.hasLevel(1)) {
            tracer = new Tracer("process " + maxVMState);
        }
        Trace.begin(1, tracer);
        final long startTimeMillis = System.currentTimeMillis();
        switch (maxVMState.processState()) {
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
                for (InspectionListener listener : _inspectionListeners.clone()) {
                    listener.vmProcessTerminated();
                }
                // Clear any possibly misleading view state.
                focus().clearAll();
                // Be sure all process-sensitive actions are disabled.
                _inspectionActions.refresh(false);
                break;
            case NO_PROCESS:
                break;
        }
        _inspectorMainFrame.refresh(true);
        _inspectionActions.refresh(true);
        Trace.end(1, tracer, startTimeMillis);
    }

    /**
     * Handles reported changes in the {@linkplain MaxVM#maxVMState() VM state}.
     * Updates state synchronously, then posts an event for follow-up on the AST event thread
     */
    private final class VMStateObserver implements TeleVMStateObserver {

        public void upate(final MaxVMState maxVMState) {
            System.out.println("MaxVMState=" + maxVMState);
            for (MaxThread thread : maxVMState.threadsStarted()) {
                Trace.line(TRACE_VALUE, tracePrefix() + "started: " + thread);
            }
            for (MaxThread thread : maxVMState.threadsDied()) {
                Trace.line(TRACE_VALUE, tracePrefix() + "died: " + thread);
            }
            if (java.awt.EventQueue.isDispatchThread()) {
                processVMStateChange();
            } else {
                Tracer tracer = null;
                if (Trace.hasLevel(TRACE_VALUE)) {
                    tracer = new Tracer("scheduled " + maxVMState);
                }
                Trace.begin(TRACE_VALUE, tracer);
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        processVMStateChange();
                    }
                });
                Trace.end(TRACE_VALUE, tracer);
            }
        }
    }

    /**
     * Handles reported breakpoint changes in the VM.
     */
    private void processBreakpointChange() {
        Trace.line(TRACE_VALUE, tracePrefix() + "breakpoint state notification");
        for (InspectionListener listener : _inspectionListeners.clone()) {
            listener.breakpointSetChanged();
        }
    }

    /**
     * Handles reported breakpoint changes in the VM. Ensures that the event is handled only on the
     * AWT event thread.
     */
    private final class BreakpointObserver implements Observer {

        public void update(Observable o, Object arg) {
            if (java.awt.EventQueue.isDispatchThread()) {
                processBreakpointChange();
            } else {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        processBreakpointChange();
                    }
                });
            }
        }
    }

    private InspectorAction _currentAction = null;

    /**
     * Holds the action currently being performed; null when finished.
     */
    public InspectorAction currentAction() {
        return _currentAction;
    }

    void setCurrentAction(InspectorAction action) {
        _currentAction = action;
    }

    /**
     * @return default title for any messages: defaults to name of current {@link InspectorAction} if one is current,
     *         otherwise the generic name of the inspector.
     */
    public String currentActionTitle() {
        return _currentAction != null ? _currentAction.name() : INSPECTOR_NAME;
    }

    private IdentityHashSet<InspectionListener> _inspectionListeners = new IdentityHashSet<InspectionListener>();

    /**
     * Adds a listener for view update when VM state changes.
     */
    public void addInspectionListener(InspectionListener listener) {
        Trace.line(TRACE_VALUE, tracePrefix() + "adding inspection listener: " + listener);
        _inspectionListeners.add(listener);
    }

    /**
     * Removes a listener for view update, for example when an Inspector is disposed or when the default notification
     * mechanism is being overridden.
     */
    public void removeInspectionListener(InspectionListener listener) {
        Trace.line(TRACE_VALUE, tracePrefix() + "removing inspection listener: " + listener);
        _inspectionListeners.remove(listener);
    }

    /**
     * Update all views by reading from VM state as needed.
     *
     * @param force suspend caching behavior; reload state unconditionally.
     */
    public void refreshAll(boolean force) {
        Tracer tracer = null;
        // Additional listeners may come and go during the update cycle, which can be ignored.
        for (InspectionListener listener : _inspectionListeners.clone()) {
            if (Trace.hasLevel(TRACE_VALUE)) {
                tracer = new Tracer("refresh: " + listener);
            }
            Trace.begin(TRACE_VALUE, tracer);
            final long startTimeMillis = System.currentTimeMillis();
            listener.vmStateChanged(force);
            Trace.end(TRACE_VALUE, tracer, startTimeMillis);
        }
        _inspectionActions.refresh(force);
    }

    /**
     * Updates all views, assuming that display and style parameters may have changed; forces state reload from the
     * VM.
     */
    void updateViewConfiguration() {
        for (InspectionListener listener : _inspectionListeners) {
            Trace.line(TRACE_VALUE, tracePrefix() + "updateViewConfiguration: " + listener);
            listener.viewConfigurationChanged();
        }
        _inspectionActions.redisplay();
        _inspectorMainFrame.redisplay();
    }

    private final Tracer _threadTracer = new Tracer("refresh thread state");

    /**
     * Determines what happened in VM execution that just concluded. Then updates all view state as needed.
     */
    public void updateAfterVMStopped() {
        gui().showInspectorBusy(true);
        final IdentityHashSet<InspectionListener> listeners = _inspectionListeners.clone();
        // Notify of any changes of the thread set

        Trace.begin(TRACE_VALUE, _threadTracer);
        final long startTimeMillis = System.currentTimeMillis();
        if (!maxVMState().threadsStarted().isEmpty() || !maxVMState().threadsDied().isEmpty()) {
            for (MaxThread maxThread : maxVMState().threads()) {
                for (InspectionListener listener : listeners) {
                    listener.threadStateChanged(maxThread);
                }
            }
        } else {
            // A kind of optimization that keeps the StackInspector from walking every stack every time; is it needed?
            final MaxThread currentThread = focus().thread();
            for (InspectionListener listener : listeners) {
                listener.threadStateChanged(currentThread);
            }
        }
        Trace.end(TRACE_VALUE, _threadTracer, startTimeMillis);
        try {
            refreshAll(false);
            // Make visible the code at the IP of the thread that triggered the breakpoint.
            boolean atBreakpoint = false;
            for (MaxThread maxThread : maxVMState().threads()) {
                if (maxThread.breakpoint() != null) {
                    focus().setThread(maxThread);
                    atBreakpoint = true;
                    break;
                }
            }
            if (!atBreakpoint) {
                // If there was no selection based on breakpoint, then check the thread that was selected before the
                // change.
                InspectorError.check(focus().thread().isLive(), "Selected thread no longer valid");
            }
            // Reset focus to new IP.
            final MaxThread focusThread = focus().thread();
            focus().setStackFrame(focusThread, focusThread.frames().first(), true);
        } catch (Throwable throwable) {
            new InspectorError("could not update view", throwable).display(this);
        } finally {
            gui().showInspectorBusy(false);
        }
    }

    /**
     * Saves any persistent state, then shuts down VM process and inspection.
     */
    public void quit() {
        settings().quit();
        try {
            maxVM().terminate();
        } catch (Exception exception) {
            ProgramWarning.message("error during VM termination: " + exception);
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
     * @param bytecodeLocation specifies a bytecode position in a class method actor
     * @return true if a file viewer was opened
     */
    public boolean viewSourceExternally(BytecodeLocation bytecodeLocation) {
        if (_preferences.externalViewerType() == ExternalViewerType.NONE) {
            return false;
        }
        final ClassMethodActor classMethodActor = bytecodeLocation.classMethodActor();
        final CodeAttribute codeAttribute = classMethodActor.codeAttribute();
        final int lineNumber = codeAttribute.lineNumberTable().findLineNumber(bytecodeLocation.bytecodePosition());
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
        if (_preferences.externalViewerType() == ExternalViewerType.NONE) {
            return false;
        }
        final File javaSourceFile = maxVM().findJavaSourceFile(classActor);
        if (javaSourceFile == null) {
            return false;
        }

        switch (_preferences.externalViewerType()) {
            case PROCESS: {
                final String config = _preferences.externalViewerConfig().get(ExternalViewerType.PROCESS);
                if (config != null) {
                    final String command = config.replaceAll("\\$file", javaSourceFile.getAbsolutePath()).replaceAll("\\$line", String.valueOf(lineNumber));
                    try {
                        Trace.line(1, tracePrefix() + "Opening file by executing " + command);
                        Runtime.getRuntime().exec(command);
                    } catch (IOException ioException) {
                        ProgramWarning.message("Error opening file by executing " + command + ": " + ioException);
                        return false;
                    }
                }
                break;
            }
            case SOCKET: {
                final String hostname = null;
                final String portString = _preferences.externalViewerConfig().get(ExternalViewerType.SOCKET);
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
                        ProgramWarning.message("Error opening file via localhost:" + portString + ": " + ioException);
                        return false;
                    }
                }
                break;
            }
            default: {
                ProgramError.unknownCase();
            }
        }
        return true;
    }


    /**
     * An object that delays evaluation of a trace message for controller actions.
     */
    private class Tracer {

        private final String _message;

        /**
         * An object that delays evaluation of a trace message.
         *
         * @param message identifies what is being traced
         */
        public Tracer(String message) {
            _message = message;
        }

        @Override
        public String toString() {
            return tracePrefix() + _message;
        }
    }

}
