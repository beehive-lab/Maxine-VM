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

import static java.awt.event.InputEvent.*;
import static java.awt.event.KeyEvent.*;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import com.sun.max.collect.*;
import com.sun.max.gui.*;
import com.sun.max.ins.InspectionMenus.*;
import com.sun.max.program.*;
import com.sun.max.tele.debug.TeleProcess.*;

/**
 */
public abstract class InspectorAction extends AbstractAction {

    /**
     * The set of InspectorAction types to which a key stroke can be bound.
     */
    @SuppressWarnings("unchecked")
    public static final Set<Class<? extends InspectorAction>> KEY_BINDABLE_ACTIONS = new HashSet<Class<? extends InspectorAction>>(Arrays.asList(
        ViewBootImageAction.class,
        QuitAction.class,
        InspectClassAction.class,
        InspectMethodAction.class,
        ViewMethodBytecodeAction.class,
        ViewMethodTargetCodeAction.class,
        SingleStepAction.class,
        RunToInstructionAction.class,
        RunToInstructionWithBreakpointsAction.class,
        ReturnFromFrameAction.class,
        ReturnFromFrameWithBreakpointsAction.class,
        StepOverAction.class,
        StepOverWithBreakpointsAction.class,
        ResumeAction.class,
        PauseAction.class,
        ToggleTargetBreakpointAction.class,
        SetLabelBreakpointsAction.class,
        ClearLabelBreakpointsAction.class,
        BreakAtMethodAction.class,
        BreakAtObjectInitializersAction.class,
        BreakAtTargetMethodAction.class
    ));

    /**
     * A map from {@linkplain InspectorAction#KEY_BINDABLE_ACTIONS bindable} inspector actions to the key strokes to
     * which they are bound. Each key binding map has a {@linkplain #name() name} which must be unique across all
     * key binding maps.
     */
    public static class KeyBindingMap extends HashMap<Class<? extends InspectorAction>, KeyStroke> {

        private static final Map<String, KeyBindingMap> ALL_MODIFIABLE = new HashMap<String, KeyBindingMap>();

        /**
         * A map of all the defined key bindings, indexed by their unique names.
         */
        public static final Map<String, KeyBindingMap> ALL = Collections.unmodifiableMap(ALL_MODIFIABLE);

        private final String _name;

        /**
         * Creates a new map of actions to key strokes.
         *
         * @param name the name of the key binding map. This name must not correspond with the name of any other key
         *            binding map present in {@link #ALL}.
         */
        public KeyBindingMap(String name) {
            _name = name;
            final KeyBindingMap oldValue = ALL_MODIFIABLE.put(name, this);
            assert oldValue == null : "There's already a set of key binding map named " + name;
        }

        public String name() {
            return _name;
        }

        @Override
        public String toString() {
            return name();
        }

        KeyBindingMap add(Class<? extends InspectorAction> actionClass, int keyCode) {
            put(actionClass, KeyStroke.getKeyStroke(keyCode, 0));
            return this;
        }

        KeyBindingMap add(Class<? extends InspectorAction> actionClass, int keyCode, int modifiers) {
            put(actionClass, KeyStroke.getKeyStroke(keyCode, modifiers));
            return this;
        }

        private Class<? extends InspectorAction> actionBoundTo(KeyStroke keyStroke) {
            for (Map.Entry<Class<? extends InspectorAction>, KeyStroke> entry : entrySet()) {
                if (entry.getValue().equals(keyStroke)) {
                    return entry.getKey();
                }
            }
            return null;
        }

        @Override
        public KeyStroke put(Class<? extends InspectorAction> actionClass, KeyStroke keyStroke) {
            assert KEY_BINDABLE_ACTIONS.contains(actionClass) : "Cannot bind a key stroke to " + actionClass;
            final Class<? extends InspectorAction> existingBoundAction = actionBoundTo(keyStroke);
            assert existingBoundAction == null : "Cannot bind " + keyStroke + " to " + actionClass.getName() + "; this key stroke is already bound to " + existingBoundAction.getName();
            final KeyStroke existingKeyStrokeBinding = super.put(actionClass, keyStroke);
            assert existingKeyStrokeBinding == null : "Cannot re-bind " + actionClass.getName() + "; it is already bound to " + existingKeyStrokeBinding;
            return existingKeyStrokeBinding;
        }
    }

    /**
     * The keys bindings that were defined in an ad-hoc fashion as the Maxine inspector was developed.
     */
    public static final KeyBindingMap MAXINE_KEY_BINDING_MAP = new KeyBindingMap("Maxine").
        add(ViewBootImageAction.class, 'I', CTRL_DOWN_MASK).
        add(QuitAction.class, 'Q', CTRL_DOWN_MASK).
        add(InspectClassAction.class, 'C', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(InspectMethodAction.class, 'M', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(ViewMethodBytecodeAction.class, 'B', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(ViewMethodTargetCodeAction.class, 'D', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(SingleStepAction.class, 'S', CTRL_DOWN_MASK).
        add(StepOverWithBreakpointsAction.class, 'W', CTRL_DOWN_MASK).
        add(StepOverAction.class, 'W', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(ReturnFromFrameWithBreakpointsAction.class, 'F', CTRL_DOWN_MASK).
        add(ReturnFromFrameAction.class, 'F', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(ResumeAction.class, 'R', CTRL_DOWN_MASK).
        add(PauseAction.class, 'P', CTRL_DOWN_MASK).
        add(ToggleTargetBreakpointAction.class, 'B', CTRL_DOWN_MASK).
        add(BreakAtTargetMethodAction.class, 'E', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(BreakAtMethodAction.class, 'S', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(BreakAtObjectInitializersAction.class, 'I', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(SetLabelBreakpointsAction.class, 'L', CTRL_DOWN_MASK).
        add(ClearLabelBreakpointsAction.class, 'L', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(RunToInstructionWithBreakpointsAction.class, 'T', CTRL_DOWN_MASK).
        add(RunToInstructionAction.class, 'T', CTRL_DOWN_MASK + SHIFT_DOWN_MASK);

    /**
     * A set of key bindings that matches the default bindings in N for similar actions.
     */
    public static final KeyBindingMap NETBEANS_KEY_BINDINGS = new KeyBindingMap("NetBeans").
        add(ViewBootImageAction.class, 'I', CTRL_DOWN_MASK).
        add(QuitAction.class, 'Q', CTRL_DOWN_MASK).
        add(InspectClassAction.class, 'T', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(InspectMethodAction.class, 'M', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(ViewMethodBytecodeAction.class, 'J', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(ViewMethodTargetCodeAction.class, 'D', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(SingleStepAction.class, VK_F7).
        add(StepOverWithBreakpointsAction.class, VK_F8).
        add(StepOverAction.class, VK_F8, CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(ReturnFromFrameWithBreakpointsAction.class, VK_F7, CTRL_DOWN_MASK).
        add(ReturnFromFrameAction.class, VK_F7, CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(ResumeAction.class, VK_F5).
        add(PauseAction.class, 'P', CTRL_DOWN_MASK).
        add(ToggleTargetBreakpointAction.class, VK_F8, CTRL_DOWN_MASK).
        add(BreakAtTargetMethodAction.class, 'E', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(BreakAtMethodAction.class, 'S', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(BreakAtObjectInitializersAction.class, 'I', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(SetLabelBreakpointsAction.class, 'L', CTRL_DOWN_MASK).
        add(ClearLabelBreakpointsAction.class, 'L', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(RunToInstructionWithBreakpointsAction.class, VK_F4).
        add(RunToInstructionAction.class, VK_F4, CTRL_DOWN_MASK + SHIFT_DOWN_MASK);

    /**
     * A set of key bindings that matches the default bindings in Eclipse for similar actions.
     */
    public static final KeyBindingMap ECLIPSE_KEY_BINDINGS = new KeyBindingMap("Eclipse").
        add(ViewBootImageAction.class, 'I', CTRL_DOWN_MASK).
        add(QuitAction.class, 'Q', CTRL_DOWN_MASK).
        add(InspectClassAction.class, 'T', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(InspectMethodAction.class, 'M', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(ViewMethodBytecodeAction.class, 'J', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(ViewMethodTargetCodeAction.class, 'D', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(SingleStepAction.class, VK_F5).
        add(StepOverWithBreakpointsAction.class, VK_F6).
        add(StepOverAction.class, VK_F6, CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(ReturnFromFrameWithBreakpointsAction.class, VK_F7).
        add(ReturnFromFrameAction.class, VK_F7, CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(ResumeAction.class, VK_F8).
        add(PauseAction.class, 'P', CTRL_DOWN_MASK).
        add(ToggleTargetBreakpointAction.class, 'B', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(BreakAtTargetMethodAction.class, 'E', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(BreakAtMethodAction.class, 'S', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(BreakAtObjectInitializersAction.class, 'I', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(SetLabelBreakpointsAction.class, 'L', CTRL_DOWN_MASK).
        add(ClearLabelBreakpointsAction.class, 'L', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(RunToInstructionWithBreakpointsAction.class, 'R', CTRL_DOWN_MASK).
        add(RunToInstructionAction.class, 'R', CTRL_DOWN_MASK + SHIFT_DOWN_MASK);

    private static final int TRACE_VALUE = 2;

    private String tracePrefix() {
        return "[Inspector] ";
    }

    private final Inspection _inspection;

    /*
     * Identifies actions that start the VM running so that the Inspector visuals can be updated immediately.
     */
    private final boolean _runsVM;

    /**
     * Creates an action than involves Inspector machinery in addition to GUI machinery.
     *
     * @param title  name of the action for human consumption
     */
    public InspectorAction(Inspection inspection, String title) {
        this(inspection, title, false);
    }

    /**
     * Creates an action than involves Inspector machinery in addition to GUI machinery.
     *
     * @param title  name of the action for human consumption
     * @param runsVM whether the actions causes the VM to start running.
     */
    public InspectorAction(Inspection inspection, String title, boolean runsVM) {
        super(title);
        _inspection = inspection;
        _runsVM = runsVM;
        inspection.registerAction(this);
    }

    /**
     * @return name of the action suitable for displaying to a user.
     */
    public String name() {
        return (String) getValue(Action.NAME);
    }

    protected abstract void procedure();

    public void perform() {
        synchronized (_inspection) {
            Trace.begin(TRACE_VALUE, tracePrefix() + "action:  " + name());
            _inspection.setBusy(true);
            if (_runsVM) {
                _inspection.processStateChange(State.RUNNING);
            }
            try {
                procedure();
            } catch (InspectorError inspectorError) {
                inspectorError.display(_inspection);
            } catch (Throwable throwable) {
                ThrowableDialog.showLater(throwable, _inspection, "Error while performing \"" + name() + "\"");
            } finally {
                _inspection.setCurrentAction(null);
                Trace.end(TRACE_VALUE, tracePrefix() + "action:  " + name());
                _inspection.setBusy(false);
            }
        }
    }

    public void actionPerformed(ActionEvent event) {
        perform();
    }

    private AppendableSequence<JMenuItem> _menuItems = new LinkSequence<JMenuItem>();

    public Sequence<JMenuItem> menuItems() {
        return _menuItems;
    }

    public void prepend(JMenu menu) {
        _menuItems.append(menu.insert(this, 0));
    }

    public void append(JMenu menu) {
        _menuItems.append(menu.add(this));
    }

    public void append(JPopupMenu menu) {
        _menuItems.append(menu.add(this));
    }

}
