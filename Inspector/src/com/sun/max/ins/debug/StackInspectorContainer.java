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
package com.sun.max.ins.debug;

import javax.swing.event.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.value.*;

/**
 * An inspector that contains, in a tabbed view, a separate inspector the stack of each current thread.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Mick Jordan
 * @author Michael Van De Vanter
 */
public final class StackInspectorContainer extends TabbedInspector<StackInspector, StackInspectorContainer> {

    /**
     * @return the singleton container instance, if it exists; null otherwise.
     */
    public static StackInspectorContainer get(Inspection inspection) {
        return UniqueInspector.find(inspection, StackInspectorContainer.class);
    }

    /**
     * Return the singleton StackInspectorContainer, creating it if necessary.
     * Policy is to populate container automatically at creation with an inspector for each existing thread.
     */
    private static StackInspectorContainer make(Inspection inspection) {
        StackInspectorContainer stackInspectorContainer = get(inspection);
        if (stackInspectorContainer == null) {
            Trace.begin(1, "[StackInspector] initializing");
            stackInspectorContainer = new StackInspectorContainer(inspection, Residence.INTERNAL);
            for (TeleNativeThread thread : inspection.teleVM().threads()) {
                stackInspectorContainer.add(new StackInspector(inspection, thread, stackInspectorContainer.residence(), stackInspectorContainer));
            }
            Trace.end(1, "[StackInspector] initializing");
        }
        stackInspectorContainer.updateThreadFocus(inspection.focus().thread());
        return stackInspectorContainer;
    }

    /**
     * Find an existing stack inspector for a thread in the {@link TeleVM}.
     * Will create a fully populated ThreadsInspector if one doesn't already exist.
     * @return null if stack inspector for thread doesn't exist
     */
    public static StackInspector getInspector(Inspection inspection, TeleNativeThread teleNativeThread) {
        final StackInspectorContainer stackInspectorContainer = make(inspection);
        for (StackInspector stackInspector : stackInspectorContainer) {
            if (stackInspector.teleNativeThread().equals(teleNativeThread)) {
                return stackInspector;
            }
        }
        return null;
    }

    /**
     * Returns the stack inspector for a thread in the {@link TeleVM}, creating it if necessary.
     * Creates a StackInspectorContainer, if one doesn't already exist, and populates
     * it with stack inspectors for every existing thread.
     */
    public static StackInspector makeInspector(Inspection inspection, TeleNativeThread teleNativeThread) {
        final StackInspectorContainer stackInspectorContainer = make(inspection);
        // if the container is newly created, it will create an inspector for every thread, so wait to check
        StackInspector stackInspector = getInspector(inspection, teleNativeThread);
        if (stackInspector == null) {
            stackInspector = new StackInspector(inspection, teleNativeThread, stackInspectorContainer.residence(), stackInspectorContainer);
            stackInspectorContainer.add(stackInspector);
        }
        return stackInspector;
    }

    private boolean _threadSetNeedsUpdate;

    @Override
    public void threadSetChanged(long epoch) {
        _threadSetNeedsUpdate = true;
        super.threadSetChanged(epoch);
    }

    private final ChangeListener _tabChangeListener = new ChangeListener() {

        public void stateChanged(ChangeEvent event) {
            // A StackInspector tab has become visible that was not visible before.
            final StackInspector selectedInspector = getSelected();
            if (selectedInspector != null) {
                inspection().focus().setThread(selectedInspector.teleNativeThread());
            }
        }
    };
    private StackInspectorContainer(Inspection inspection, Residence residence) {
        super(inspection, residence, inspection.geometry().stacksFrameDefaultLocation(), inspection.geometry().stacksFramePrefSize(), "stacksInspector");
        _threadSetNeedsUpdate = true;
        addChangeListener(_tabChangeListener);
    }

    @Override
    public String getTextForTitle() {
        return "Stacks";
    }

    @Override
    public void refreshView(long epoch, boolean force) {
        if (_threadSetNeedsUpdate || force) {
            // Mark all stack inspectors for possible deletion if their thread is no longer active
            for (StackInspector stackInspector : this) {
                stackInspector.setMarked(true);
            }
            for (TeleNativeThread thread : teleVM().threads()) {
                final UniqueInspector.Key<StackInspector> key = UniqueInspector.Key.create(StackInspector.class, LongValue.from(thread.id()));
                final StackInspector stackInspector = UniqueInspector.find(inspection(), key);
                if (stackInspector == null) {
                    add(new StackInspector(inspection(), thread, residence(), this));
                } else {
                    stackInspector.setMarked(false);
                }
            }
            // Any remaining marked inspectors should be deleted as the threads have gone away
            for (StackInspector stackInspector : this) {
                if (stackInspector.marked()) {
                    close(stackInspector);
                }
            }
            _threadSetNeedsUpdate = false;
        }
        updateThreadFocus(focus().thread());
        super.refreshView(epoch, force);
    }

    @Override
    public void add(StackInspector stackInspector) {
        super.add(stackInspector, stackInspector.getTextForTitle());
        stackInspector.frame().invalidate();
        stackInspector.frame().repaint();
    }

    @Override
    public void threadFocusSet(TeleNativeThread oldTeleNativeThread, TeleNativeThread teleNativeThread) {
        updateThreadFocus(teleNativeThread);
    }

    /**
     * Change the selected tab, if needed, to agree with the global thread selection.
     */
    private void updateThreadFocus(TeleNativeThread selectedThread) {
        for (StackInspector stackInspector : this) {
            if (stackInspector.teleNativeThread().equals(selectedThread)) {
                if (!isSelected(stackInspector)) {
                    // Select and highlight the tabbed inspector; triggers change event  that will cause it to be refreshed.
                    stackInspector.highlight();
                }
                break;
            }
        }
    }

    static class TruncatedStackFrame extends StackFrame {
        private StackFrame _truncatedStackFrame;

        TruncatedStackFrame(StackFrame callee, StackFrame truncatedStackFrame) {
            super(callee, truncatedStackFrame.instructionPointer(), truncatedStackFrame.framePointer(), truncatedStackFrame.stackPointer());
            _truncatedStackFrame = truncatedStackFrame;
        }

        StackFrame getTruncatedStackFrame() {
            return _truncatedStackFrame;
        }

        @Override
        public TargetMethod targetMethod() {
            return _truncatedStackFrame.targetMethod();
        }

        @Override
        public boolean isJavaStackFrame() {
            return false;
        }

        @Override
        public boolean isSameFrame(StackFrame stackFrame) {
            if (stackFrame instanceof TruncatedStackFrame) {
                final TruncatedStackFrame other = (TruncatedStackFrame) stackFrame;
                return _truncatedStackFrame.isSameFrame(other._truncatedStackFrame);
            }
            return false;
        }
    }

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing");
        removeChangeListener(_tabChangeListener);
        super.inspectorClosing();
    }

    @Override
    public void vmProcessTerminated() {
        dispose();
    }

}
