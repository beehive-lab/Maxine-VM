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

import java.util.*;

import javax.swing.*;

import com.sun.max.ins.InspectionActions.*;


/**
 * Support for binding {@link KeyStroke}s to {@link InspectorActions}s.
 *
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public class InspectorKeyBindings {

    /**
     * The set of InspectorAction types to which a key stroke can be bound.
     */
    @SuppressWarnings("unchecked")
    public static final Set<Class<? extends InspectorAction>> KEY_BINDABLE_ACTIONS = new HashSet<Class<? extends InspectorAction>>(Arrays.asList(
        ViewBootImageAction.class,
        QuitAction.class,
        InspectClassActorByNameAction.class,
        InspectMethodActorByNameAction.class,
        ViewMethodBytecodeByNameAction.class,
        ViewMethodTargetCodeByNameAction.class,
        DebugSingleStepAction.class,
        DebugRunToInstructionAction.class,
        DebugRunToInstructionWithBreakpointsAction.class,
        DebugRunToNextCallAction.class,
        DebugRunToNextCallWithBreakpointsAction.class,
        DebugReturnFromFrameAction.class,
        DebugReturnFromFrameWithBreakpointsAction.class,
        DebugStepOverAction.class,
        DebugStepOverWithBreakpointsAction.class,
        DebugResumeAction.class,
        DebugPauseAction.class,
        ToggleTargetCodeBreakpointAction.class,
        SetTargetCodeLabelBreakpointsAction.class,
        RemoveTargetCodeLabelBreakpointsAction.class,
        SetBytecodeBreakpointAtMethodEntryByNameAction.class,
        SetTargetCodeBreakpointAtObjectInitializerAction.class,
        SetTargetCodeBreakpointAtMethodEntriesByNameAction.class
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

        private final String name;

        /**
         * Creates a new map of actions to key strokes.
         *
         * @param name the name of the key binding map. This name must not correspond with the name of any other key
         *            binding map present in {@link #ALL}.
         */
        KeyBindingMap(String name) {
            this.name = name;
            final KeyBindingMap oldValue = ALL_MODIFIABLE.put(name, this);
            assert oldValue == null : "There's already a set of key binding map named " + name;
        }

        public String name() {
            return name;
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
     * A set of key bindings that matches the default bindings in NetBeans for similar actions.
     */
    public static final KeyBindingMap NETBEANS_KEY_BINDINGS = new KeyBindingMap("NetBeans").
        add(ViewBootImageAction.class, 'I', CTRL_DOWN_MASK).
        add(QuitAction.class, 'Q', CTRL_DOWN_MASK).
        add(InspectClassActorByNameAction.class, 'T', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(InspectMethodActorByNameAction.class, 'M', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(ViewMethodBytecodeByNameAction.class, 'J', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(ViewMethodTargetCodeByNameAction.class, 'D', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(DebugSingleStepAction.class, VK_F7).
        add(DebugStepOverWithBreakpointsAction.class, VK_F8).
        add(DebugStepOverAction.class, VK_F8, CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(DebugReturnFromFrameWithBreakpointsAction.class, VK_F7, CTRL_DOWN_MASK).
        add(DebugReturnFromFrameAction.class, VK_F7, CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(DebugResumeAction.class, VK_F5).
        add(DebugPauseAction.class, 'P', CTRL_DOWN_MASK).
        add(ToggleTargetCodeBreakpointAction.class, VK_F8, CTRL_DOWN_MASK).
        add(SetTargetCodeBreakpointAtMethodEntriesByNameAction.class, 'E', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(SetBytecodeBreakpointAtMethodEntryByNameAction.class, 'S', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(SetTargetCodeBreakpointAtObjectInitializerAction.class, 'I', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(SetTargetCodeLabelBreakpointsAction.class, 'L', CTRL_DOWN_MASK).
        add(RemoveTargetCodeLabelBreakpointsAction.class, 'L', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(DebugRunToInstructionWithBreakpointsAction.class, VK_F4).
        add(DebugRunToInstructionAction.class, VK_F4, CTRL_DOWN_MASK + SHIFT_DOWN_MASK);

    /**
     * A default set of key bindings.
     */
    public static final KeyBindingMap DEFAULT_KEY_BINDINGS = new KeyBindingMap("Default").
        add(ViewBootImageAction.class, 'I', CTRL_DOWN_MASK).
        add(QuitAction.class, 'Q', CTRL_DOWN_MASK).
        add(InspectClassActorByNameAction.class, 'T', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(InspectMethodActorByNameAction.class, 'M', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(ViewMethodBytecodeByNameAction.class, 'J', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(ViewMethodTargetCodeByNameAction.class, 'D', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(DebugSingleStepAction.class, VK_F5).
        add(DebugStepOverWithBreakpointsAction.class, VK_F6).
        add(DebugStepOverAction.class, VK_F6, CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(DebugReturnFromFrameWithBreakpointsAction.class, VK_F7).
        add(DebugReturnFromFrameAction.class, VK_F7, CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(DebugResumeAction.class, VK_F8).
        add(DebugPauseAction.class, 'P', CTRL_DOWN_MASK).
        add(ToggleTargetCodeBreakpointAction.class, 'B', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(SetTargetCodeBreakpointAtMethodEntriesByNameAction.class, 'E', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(SetBytecodeBreakpointAtMethodEntryByNameAction.class, 'S', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(SetTargetCodeBreakpointAtObjectInitializerAction.class, 'I', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(SetTargetCodeLabelBreakpointsAction.class, 'L', CTRL_DOWN_MASK).
        add(RemoveTargetCodeLabelBreakpointsAction.class, 'L', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(DebugRunToInstructionWithBreakpointsAction.class, 'R', CTRL_DOWN_MASK).
        add(DebugRunToInstructionAction.class, 'R', CTRL_DOWN_MASK + SHIFT_DOWN_MASK).
        add(DebugRunToNextCallWithBreakpointsAction.class, 'C', CTRL_DOWN_MASK).
        add(DebugRunToNextCallAction.class, 'C', CTRL_DOWN_MASK + SHIFT_DOWN_MASK);


}
