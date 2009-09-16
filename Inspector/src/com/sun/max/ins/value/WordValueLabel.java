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
package com.sun.max.ins.value;

import java.awt.*;
import java.awt.event.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.method.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.value.*;

/**
 * A textual label for a word of machine data from the VM,
 * with multiple display modes and user interaction affordances.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public class WordValueLabel extends ValueLabel {

    private static final int TRACE_VALUE = 1;

    // Optionally supplied component that needs to be
    // repainted when this label changes its appearance.
    private final Component parent;

    /**
     * The expected kind of word value. The visual
     * representations available (of which there may only
     * be one) are derived from this and the word's value.
     */
    public enum ValueMode {
        WORD,
        REFERENCE,
        LITERAL_REFERENCE,
        INTEGER_REGISTER,
        FLAGS_REGISTER,
        FLOATING_POINT,
        CALL_ENTRY_POINT,
        ITABLE_ENTRY,
        CALL_RETURN_POINT;
    }

    private final ValueMode valueMode;

    /**
     * The actual kind of word value, determined empirically by reading from the VM; this may change after update.
     * Possible visual presentations of a word, constrained by the {@linkplain ValueMode valueMode} of the
     * label and its value.
     */
    private enum DisplayMode {
        WORD,
        NULL,
        INVALID_OBJECT_REFERENCE, // something about this reference is decidedly broken
        OBJECT_REFERENCE,
        OBJECT_REFERENCE_TEXT,
        STACK_LOCATION,
        STACK_LOCATION_TEXT,
        CALL_ENTRY_POINT,
        CALL_ENTRY_POINT_TEXT,
        CLASS_ACTOR_ID,
        CLASS_ACTOR,
        CALL_RETURN_POINT,
        CALL_RETURN_POINT_TEXT,
        FLAGS,
        DECIMAL,
        FLOAT,
        DOUBLE,
        UNCHECKED_REFERENCE, // understood to be a reference, but not checked by reading from the VM.
        UNCHECKED_CALL_POINT, // understood to be a code pointer, but not checked by reading from the VM.
        UNCHECKED_WORD, // unknown word value, not checked by reading from the VM..
        INVALID // this value is completely invalid
    }

    private DisplayMode displayMode;

    private String prefix;

    /**
     * Sets a string to be prepended to all label displays.
     */
    public final void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    private String suffix;

    /**
     * Sets a string to be appended to all label displays.
     */
    public final void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    private String toolTipSuffix;

    /**
     * Sets a string to be appended to all tooltip displays over the label.
     */
    public final void setToolTipSuffix(String toolTipSuffix) {
        this.toolTipSuffix = toolTipSuffix;
    }

    /**
     * Creates a display label for a word of machine data, initially set to null.
     * <br>
     * Content of label is supplied by override {@link ValueLabel#fetchValue()}, which
     * gets called initially and when the label is refreshed.
     * <br>
     * Display state can be toggled between alternate presentations in some situations.
     * <br>
     * Can be used as a cell renderer in a table, but the enclosing table must be explicitly repainted
     * when the display state is toggled; this will be done automatically if the table is passed in
     * as the parent component.
     *
     * @param inspection
     * @param valueMode presumed type of value for the word, influences display modes
     * @param parent a component that should be repainted when the display state is toggled;
     */
    public WordValueLabel(Inspection inspection, ValueMode valueMode, Component parent) {
        this(inspection, valueMode, Word.zero(), parent);
    }

    /**
     * Creates a display label for a word of machine data, initially set to null.
     * <br>
     * Content of label is set initially by parameter.  It can be updated by overriding{@link ValueLabel#fetchValue()}, which
     * gets called initially and when the label is refreshed.
     * <br>
     * Display state can be toggled between alternate presentations in some situations.
     * <br>
     * Can be used as a cell renderer in a table, but the enclosing table must be explicitly repainted
     * when the display state is toggled; this will be done automatically if the table is passed in
     * as the parent component.
     *
     * @param inspection
     * @param valueMode presumed type of value for the word, influences display modes
     * @param word initial value for content.
     * @param parent a component that should be repainted when the display state is toggled;
     */
    public WordValueLabel(Inspection inspection, ValueMode valueMode, Word word, Component parent) {
        super(inspection, null);
        this.parent = parent;
        this.valueMode = valueMode;
        initializeValue();
        if (value() == null) {
            setValue(new WordValue(word));
        } else {
            setValue(value());
        }
        redisplay();
        addMouseListener(new InspectorMouseClickAdapter(inspection()) {
            @Override
            public void procedure(final MouseEvent mouseEvent) {
                //System.out.println("WVL (" + _valueMode.toString() + ", " + _valueKind.toString() + ")");
                switch (Inspection.mouseButtonWithModifiers(mouseEvent)) {
                    case MouseEvent.BUTTON1: {
                        final InspectorAction inspectAction = getInspectValueAction(value());
                        if (inspectAction != null) {
                            inspectAction.perform();
                        }
                        break;
                    }
                    case MouseEvent.BUTTON2: {
                        final InspectorAction toggleAction = getToggleDisplayTextAction();
                        if (toggleAction != null) {
                            toggleAction.perform();
                        }
                        break;
                    }
                    case MouseEvent.BUTTON3: {
                        final InspectorPopupMenu menu = new InspectorPopupMenu();
                        menu.add(new WordValueMenuItems(inspection(), value()));
                        switch (displayMode) {
                            case OBJECT_REFERENCE:
                            case OBJECT_REFERENCE_TEXT: {
                                final TeleObject teleObject = maxVM().makeTeleObject(maxVM().wordToReference(value().toWord()));
                                final TeleClassMethodActor teleClassMethodActor = teleObject.getTeleClassMethodActorForObject();
                                if (teleClassMethodActor != null) {
                                    // Add method-related menu items
                                    final ClassMethodMenuItems items = new ClassMethodMenuItems(inspection(), teleClassMethodActor);
                                    items.addTo(menu);
                                }
                                break;
                            }
                            case STACK_LOCATION:
                            case STACK_LOCATION_TEXT: {
                                // TODO (mlvdv)  special right-button menu items appropriate to a pointer into stack memory
                                break;
                            }
                            default: {
                                break;
                            }
                        }
                        menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                    }
                }
            }
        });
    }

    /** Object in the VM heap pointed to by the word, if it is a valid reference. */
    private TeleObject teleObject;

    /** Non-null if a Class ID. */
    private TeleClassActor teleClassActor;

    /** Non-null if a code pointer. */
    private TeleTargetMethod teleTargetMethod;

    /** Non-null if a stack reference. */
    private MaxThread thread;

    @Override
    public final void setValue(Value newValue) {
        teleObject = null;
        teleClassActor = null;
        teleTargetMethod = null;
        thread = null;

        if (newValue == VoidValue.VOID) {
            displayMode = DisplayMode.INVALID;
        } else if (valueMode == ValueMode.FLAGS_REGISTER) {
            if (newValue == null) {
                displayMode = DisplayMode.INVALID;
            } else if (displayMode == null) {
                displayMode = DisplayMode.FLAGS;
            }
        } else if (valueMode == ValueMode.FLOATING_POINT) {
            if (newValue == null) {
                displayMode = DisplayMode.INVALID;
            } else if (displayMode == null) {
                displayMode = DisplayMode.DOUBLE;
            }
        } else if (!inspection().investigateWordValues()) {
            if (valueMode == ValueMode.REFERENCE || valueMode == ValueMode.LITERAL_REFERENCE) {
                displayMode = DisplayMode.UNCHECKED_REFERENCE;
            } else if (valueMode == ValueMode.CALL_ENTRY_POINT || valueMode == ValueMode.CALL_RETURN_POINT) {
                displayMode = DisplayMode.UNCHECKED_CALL_POINT;
            } else {
                displayMode = DisplayMode.UNCHECKED_WORD;
            }
        } else {
            displayMode = DisplayMode.WORD;
            if (maxVM().isBootImageRelocated()) {
                if (newValue == null || newValue.isZero()) {
                    if (valueMode == ValueMode.REFERENCE) {
                        displayMode = DisplayMode.NULL;
                    }
                } else if (maxVM().isValidReference(maxVM().wordToReference(newValue.toWord()))) {
                    displayMode = (valueMode == ValueMode.REFERENCE || valueMode == ValueMode.LITERAL_REFERENCE) ? DisplayMode.OBJECT_REFERENCE_TEXT : DisplayMode.OBJECT_REFERENCE;
                    final TeleReference reference = (TeleReference) maxVM().wordToReference(newValue.toWord());

                    try {
                        teleObject = maxVM().makeTeleObject(reference);
                    } catch (Throwable throwable) {
                        // If we don't catch this the views will not be updated at all.
                        teleObject = null;
                        displayMode = DisplayMode.INVALID_OBJECT_REFERENCE;
                        setToolTipText("<html><b>" + throwable + "</b><br>See log for complete stack trace.");
                        throwable.printStackTrace(Trace.stream());
                    }
                } else {
                    final Address address = newValue.toWord().asAddress();
                    thread = maxVM().threadContaining(address);
                    if (thread != null) {
                        displayMode = valueMode == ValueMode.REFERENCE ? DisplayMode.STACK_LOCATION_TEXT : DisplayMode.STACK_LOCATION;
                    } else {
                        if (valueMode == ValueMode.REFERENCE || valueMode == ValueMode.LITERAL_REFERENCE) {
                            displayMode = DisplayMode.INVALID_OBJECT_REFERENCE;
                        } else {
                            try {
                                teleTargetMethod = maxVM().makeTeleTargetMethod(newValue.toWord().asAddress());
                                if (teleTargetMethod != null) {
                                    final Address codeStart = teleTargetMethod.getCodeStart();
                                    final Word jitEntryPoint = codeStart.plus(CallEntryPoint.JIT_ENTRY_POINT.offsetFromCodeStart());
                                    final Word optimizedEntryPoint = codeStart.plus(CallEntryPoint.OPTIMIZED_ENTRY_POINT.offsetFromCodeStart());
                                    if (newValue.toWord().equals(optimizedEntryPoint) || newValue.toWord().equals(jitEntryPoint)) {
                                        displayMode = (valueMode == ValueMode.CALL_ENTRY_POINT) ? DisplayMode.CALL_ENTRY_POINT_TEXT : DisplayMode.CALL_ENTRY_POINT;
                                    } else {
                                        displayMode = (valueMode == ValueMode.CALL_RETURN_POINT) ? DisplayMode.CALL_RETURN_POINT : DisplayMode.CALL_RETURN_POINT;
                                    }
                                } else if (valueMode == ValueMode.ITABLE_ENTRY) {
                                    final TeleClassActor teleClassActor = maxVM().findTeleClassActor(newValue.asWord().asAddress().toInt());
                                    if (teleClassActor != null) {
                                        this.teleClassActor = teleClassActor;
                                        displayMode = DisplayMode.CLASS_ACTOR;
                                    } else {
                                        displayMode = DisplayMode.CLASS_ACTOR_ID;
                                    }
                                }
                            } catch (Throwable throwable) {
                                // If we don't catch this the views will not be updated at all.
                                displayMode = DisplayMode.INVALID;
                                setToolTipText("<html><b>" + throwable + "</b><br>See log for complete stack trace.");
                                throwable.printStackTrace(Trace.stream());
                            }
                        }
                    }
                }
            }
        }
        super.setValue(newValue);
    }


    public void redisplay() {
        setValue(value());
    }

    @Override
    public void updateText() {
        final Value value = value();
        if (value == null) {
            return;
        }
        if (value == VoidValue.VOID) {
            setFont(style().wordAlternateTextFont());
            setForeground(style().wordInvalidDataColor());
            setText("void");
            setToolTipText("Unable to read value");
            if (parent != null) {
                parent.repaint();
            }
            return;
        }
        final String hexString = (valueMode == ValueMode.INTEGER_REGISTER || valueMode == ValueMode.FLAGS_REGISTER || valueMode == ValueMode.FLOATING_POINT) ? value.toWord().toPaddedHexString('0') : value.toWord().toHexString();
        switch (displayMode) {
            case WORD: {
                setFont(style().wordDataFont());
                setForeground(value.isZero() ? style().wordNullDataColor() : null);
                setText(hexString);
                setToolTipText("Int: " + (value.isZero() ? 0 : Long.toString(value.toLong())));
                break;
            }
            case UNCHECKED_WORD: {
                setFont(style().wordDataFont());
                setForeground(value.isZero() ? style().wordNullDataColor() : null);
                setText(hexString);
                setToolTipText("Unchecked word");
                break;
            }
            case NULL: {
                setFont(style().wordAlternateTextFont());
                setForeground(style().wordNullDataColor());
                setText("null");
                if (valueMode == ValueMode.LITERAL_REFERENCE) {
                    setToolTipText("null" + toolTipSuffix);
                }
                break;
            }
            case INVALID: {
                setFont(style().wordAlternateTextFont());
                setForeground(style().wordInvalidDataColor());
                setText("invalid");
                if (valueMode == ValueMode.LITERAL_REFERENCE) {
                    setToolTipText("invalid" + toolTipSuffix);
                }
                break;
            }
            case OBJECT_REFERENCE: {
                setFont(style().wordDataFont());
                setForeground(style().wordValidObjectReferenceDataColor());
                setText(hexString);
                try {
                    if (valueMode == ValueMode.LITERAL_REFERENCE) {
                        setToolTipText(inspection().nameDisplay().referenceToolTipText(teleObject) + toolTipSuffix);
                    } else {
                        setToolTipText(inspection().nameDisplay().referenceToolTipText(teleObject));
                    }
                } catch (Throwable throwable) {
                    // If we don't catch this the views will not be updated at all.
                    teleObject = null;
                    displayMode = DisplayMode.INVALID_OBJECT_REFERENCE;
                    setToolTipText("<html><b>" + throwable + "</b><br>See log for complete stack trace.");
                    throwable.printStackTrace(Trace.stream());
                }
                break;
            }
            case OBJECT_REFERENCE_TEXT: {
                try {
                    final String labelText = inspection().nameDisplay().referenceLabelText(teleObject);
                    if (labelText != null) {
                        setText(labelText);
                        setToolTipText(inspection().nameDisplay().referenceToolTipText(teleObject));
                        setFont(style().wordAlternateTextFont());
                        setForeground(style().wordValidObjectReferenceDataColor());
                        if (valueMode == ValueMode.LITERAL_REFERENCE) {
                            setToolTipText(getToolTipText() + toolTipSuffix);
                        }
                        break;
                    }
                } catch (Throwable throwable) {
                    // If we don't catch this the views will not be updated at all.
                    teleObject = null;
                    displayMode = DisplayMode.INVALID_OBJECT_REFERENCE;
                    setToolTipText("<html><b>" + throwable + "</b><br>See log for complete stack trace.");
                    throwable.printStackTrace(Trace.stream());
                    break;
                }
                displayMode = DisplayMode.OBJECT_REFERENCE;
                updateText();
                break;
            }
            case STACK_LOCATION: {
                setFont(style().wordDataFont());
                setForeground(style().wordStackLocationDataColor());
                setText(hexString);
                final String threadName = inspection().nameDisplay().longName(thread);
                final long offset = value().asWord().asAddress().minus(thread.stack().start()).toLong();
                final String hexOffsetString = offset >= 0 ? ("+0x" + Long.toHexString(offset)) : "0x" + Long.toHexString(offset);
                setToolTipText("Stack:  thread=" + threadName + ", offset=" + hexOffsetString);
                break;
            }
            case STACK_LOCATION_TEXT: {
                setFont(style().wordAlternateTextFont());
                setForeground(style().wordStackLocationDataColor());
                final String threadName = inspection().nameDisplay().longName(thread);
                final long offset = value().asWord().asAddress().minus(thread.stack().start()).toLong();
                final String decimalOffsetString = offset >= 0 ? ("+" + offset) : Long.toString(offset);
                setText(threadName + " " + decimalOffsetString);
                setToolTipText("Stack:  thread=" + threadName + ", addr=0x" +  Long.toHexString(value().asWord().asAddress().toLong()));
                break;
            }
            case UNCHECKED_REFERENCE: {
                setFont(style().wordDataFont());
                setForeground(style().wordUncheckedReferenceDataColor());
                setText(hexString);
                if (valueMode == ValueMode.LITERAL_REFERENCE) {
                    setToolTipText("<unchecked>" + toolTipSuffix);
                } else {
                    setToolTipText("Unchecked Reference");
                }
                break;
            }
            case INVALID_OBJECT_REFERENCE: {
                setFont(style().wordDataFont());
                setForeground(style().wordInvalidObjectReferenceDataColor());
                setText(hexString);
                if (valueMode == ValueMode.LITERAL_REFERENCE) {
                    setToolTipText("<invalid>" + toolTipSuffix);
                }
                break;
            }
            case CALL_ENTRY_POINT: {
                setFont(style().wordDataFont());
                setForeground(style().wordCallEntryPointColor());
                setText(hexString);
                setToolTipText("Code: " + inspection().nameDisplay().longName(teleTargetMethod));
                break;
            }
            case CALL_ENTRY_POINT_TEXT: {
                setFont(style().wordAlternateTextFont());
                setForeground(style().wordCallEntryPointColor());
                setText(inspection().nameDisplay().veryShortName(teleTargetMethod));
                setToolTipText("Code: " + inspection().nameDisplay().longName(teleTargetMethod));
                break;
            }
            case CLASS_ACTOR_ID: {
                setFont(style().wordDataFont());
                setForeground(null);
                setText(Long.toString(value.asWord().asAddress().toLong()));
                if (teleClassActor != null) {
                    setToolTipText(inspection().nameDisplay().referenceToolTipText(teleClassActor));
                } else {
                    setToolTipText("Class{???}");
                }
                break;
            }
            case CLASS_ACTOR: {
                setText(teleClassActor.classActor().simpleName());
                setToolTipText(inspection().nameDisplay().referenceToolTipText(teleClassActor));
                break;
            }
            case CALL_RETURN_POINT: {
                setFont(style().wordDataFont());
                setForeground(style().wordCallReturnPointColor());
                setText(hexString);
                if (teleTargetMethod != null) {
                    setToolTipText("Code: " + inspection().nameDisplay().longName(teleTargetMethod, value.toWord().asAddress()));
                }
                break;
            }
            case CALL_RETURN_POINT_TEXT: {
                setFont(style().wordAlternateTextFont());
                setForeground(style().wordCallReturnPointColor());
                if (teleTargetMethod != null) {
                    setText(inspection().nameDisplay().veryShortName(teleTargetMethod, value.toWord().asAddress()));
                    setToolTipText("Code: " + inspection().nameDisplay().longName(teleTargetMethod, value.toWord().asAddress()));
                }
                break;
            }
            case UNCHECKED_CALL_POINT: {
                setFont(style().wordDataFont());
                setForeground(style().wordUncheckedCallPointColor());
                setText(hexString);
                setToolTipText("Unchecked call entry/return point");
                break;
            }
            case FLAGS: {
                setFont(style().wordFlagsFont());
                setForeground(null);
                setText(maxVM().visualizeStateRegister(value.toLong()));
                setToolTipText("Flags 0x" + hexString);
                break;
            }
            case DECIMAL: {
                setFont(style().decimalDataFont());
                setForeground(null);
                setText(Integer.toString(value.toInt()));
                setToolTipText("0x" + hexString);
                break;
            }
            case FLOAT: {
                setFont(style().wordAlternateTextFont());
                setForeground(null);
                setText(Float.toString(Float.intBitsToFloat((int) (value.toLong() & 0xffffffffL))));
                setToolTipText("0x" + hexString);
                break;
            }
            case DOUBLE: {
                setFont(style().wordAlternateTextFont());
                setForeground(null);
                setText(Double.toString(Double.longBitsToDouble(value.toLong())));
                setToolTipText("0x" + hexString);
                break;
            }
        }
        if (prefix != null) {
            setText(prefix + getText());
        }
        if (suffix != null) {
            setText(getText() + suffix);
        }
        if (parent != null) {
            parent.repaint();
        }
    }

    private InspectorAction getToggleDisplayTextAction() {
        DisplayMode alternateValueKind = displayMode;
        if (valueMode == ValueMode.FLAGS_REGISTER) {
            switch (displayMode) {
                case WORD: {
                    alternateValueKind = DisplayMode.FLAGS;
                    break;
                }
                case FLAGS: {
                    alternateValueKind = DisplayMode.WORD;
                    break;
                }
                default: {
                    break;
                }
            }
        }
        if (valueMode == ValueMode.FLOATING_POINT) {
            switch (alternateValueKind) {
                case WORD: {
                    alternateValueKind = DisplayMode.DOUBLE;
                    break;
                }
                case DOUBLE: {
                    alternateValueKind = DisplayMode.FLOAT;
                    break;
                }
                case FLOAT: {
                    alternateValueKind = DisplayMode.WORD;
                    break;
                }
                default: {
                    break;
                }
            }
        }
        if (valueMode == ValueMode.INTEGER_REGISTER) {
            switch (alternateValueKind) {
                case WORD: {
                    alternateValueKind = DisplayMode.DECIMAL;
                    break;
                }
                case DECIMAL: {
                    alternateValueKind = DisplayMode.WORD;
                    break;
                }
                default: {
                    break;
                }
            }
        }
        switch (alternateValueKind) {
            case OBJECT_REFERENCE: {
                alternateValueKind = DisplayMode.OBJECT_REFERENCE_TEXT;
                break;
            }
            case OBJECT_REFERENCE_TEXT: {
                alternateValueKind = DisplayMode.OBJECT_REFERENCE;
                break;
            }
            case STACK_LOCATION: {
                alternateValueKind = DisplayMode.STACK_LOCATION_TEXT;
                break;
            }
            case STACK_LOCATION_TEXT: {
                alternateValueKind = DisplayMode.STACK_LOCATION;
                break;
            }
            case CALL_ENTRY_POINT: {
                alternateValueKind = DisplayMode.CALL_ENTRY_POINT_TEXT;
                break;
            }
            case CALL_ENTRY_POINT_TEXT: {
                alternateValueKind = DisplayMode.CALL_ENTRY_POINT;
                break;
            }
            case CLASS_ACTOR_ID: {
                if (teleClassActor != null) {
                    alternateValueKind = DisplayMode.CLASS_ACTOR;
                }
                break;
            }
            case CLASS_ACTOR: {
                alternateValueKind = DisplayMode.CLASS_ACTOR_ID;
                break;
            }
            case CALL_RETURN_POINT: {
                alternateValueKind = DisplayMode.CALL_RETURN_POINT_TEXT;
                break;
            }
            case CALL_RETURN_POINT_TEXT: {
                alternateValueKind = DisplayMode.CALL_RETURN_POINT;
                break;
            }
            default: {
                break;
            }
        }
        if (alternateValueKind != displayMode) {
            final DisplayMode newValueKind = alternateValueKind;
            return new InspectorAction(inspection(), "Toggle alternate display text") {

                @Override
                public void procedure() {
                    Trace.line(TRACE_VALUE, "WVL: " + displayMode.toString() + "->" + newValueKind);
                    displayMode = newValueKind;
                    WordValueLabel.this.updateText();
                }
            };
        }
        return null;
    }

    private InspectorAction getInspectValueAction(Value value) {
        InspectorAction action = null;
        switch (displayMode) {
            case OBJECT_REFERENCE:
            case UNCHECKED_REFERENCE:
            case OBJECT_REFERENCE_TEXT: {
                final TeleObject teleObject = maxVM().makeTeleObject(maxVM().wordToReference(value.toWord()));
                action = inspection().actions().inspectObject(teleObject, null);
                break;
            }
            case CALL_ENTRY_POINT:
            case CALL_ENTRY_POINT_TEXT:
            case CALL_RETURN_POINT:
            case CALL_RETURN_POINT_TEXT:
            case UNCHECKED_CALL_POINT: {
                final Address address = value.toWord().asAddress();
                action = new InspectorAction(inspection(), "View Code at address") {
                    @Override
                    public void procedure() {
                        inspection().focus().setCodeLocation(maxVM().createCodeLocation(address), true);
                    }
                };
                break;
            }
            case CLASS_ACTOR_ID:
            case CLASS_ACTOR: {
                final TeleClassActor teleClassActor = maxVM().findTeleClassActor(value.asWord().asAddress().toInt());
                if (teleClassActor != null) {
                    action = inspection().actions().inspectObject(teleClassActor, "Inspect ClassActor");
                }
                break;
            }
            case STACK_LOCATION:
            case STACK_LOCATION_TEXT:
            case WORD:
            case NULL:
            case INVALID_OBJECT_REFERENCE:
            case FLAGS:
            case DECIMAL:
            case FLOAT:
            case  DOUBLE:
            case UNCHECKED_WORD:
            case INVALID: {
                // no action
                break;
            }
        }
        return action;
    }


    private InspectorAction getInspectMemoryWordsAction(Value value) {
        InspectorAction action = null;
        if (value != VoidValue.VOID) {
            final Address address = value.toWord().asAddress();
            switch (displayMode) {
                case INVALID_OBJECT_REFERENCE:
                case UNCHECKED_REFERENCE:
                case STACK_LOCATION:
                case  STACK_LOCATION_TEXT:
                case CALL_ENTRY_POINT:
                case CALL_ENTRY_POINT_TEXT:
                case CALL_RETURN_POINT:
                case CALL_RETURN_POINT_TEXT:
                case UNCHECKED_CALL_POINT: {
                    action = inspection().actions().inspectMemoryWords(address, null);
                    break;
                }
                case OBJECT_REFERENCE:
                case OBJECT_REFERENCE_TEXT: {
                    if (teleObject != null) {
                        action = inspection().actions().inspectObjectMemoryWords(teleObject, "Inspect memory for " + inspection().nameDisplay().referenceLabelText(teleObject));
                    } else {
                        action = inspection().actions().inspectMemoryWords(address, null);
                    }
                    break;
                }
                case WORD:
                case NULL:
                case CLASS_ACTOR_ID:
                case CLASS_ACTOR:
                case FLAGS:
                case DECIMAL:
                case FLOAT:
                case DOUBLE:
                case UNCHECKED_WORD:
                case INVALID: {
                    if (maxVM().contains(address)) {
                        action = inspection().actions().inspectMemoryWords(address, null);
                    }
                    break;
                }
            }
        }
        return action;
    }

    private InspectorAction getShowMemoryRegionAction(Value value) {
        InspectorAction action = null;
        if (value != VoidValue.VOID) {
            final Address address = value.toWord().asAddress();
            final MemoryRegion memoryRegion = maxVM().memoryRegionContaining(address);
            if (memoryRegion != null) {
                action = inspection().actions().selectMemoryRegion(memoryRegion);
            }
        }
        return action;
    }

    private final class WordValueMenuItems extends InspectorPopupMenuItems {

        private final class MenuInspectObjectAction extends InspectorAction {

            private final InspectorAction inspectAction;

            private MenuInspectObjectAction(Value value) {
                super(inspection(), "Inspect Object (Left-Button)");
                inspectAction = getInspectValueAction(value);
                setEnabled(inspectAction != null);
            }

            @Override
            public void procedure() {
                inspectAction.perform();
            }
        }

        private final class MenuToggleDisplayAction extends InspectorAction {

            private final InspectorAction toggleAction;

            private MenuToggleDisplayAction() {
                super(inspection(), "Toggle display (Middle-Button)");
                toggleAction = getToggleDisplayTextAction();
                setEnabled(toggleAction != null);
            }

            @Override
            public void procedure() {
                toggleAction.perform();
            }
        }

        private final class MenuInspectMemoryWordsAction extends InspectorAction {

            private final InspectorAction inspectMemoryWordsAction;

            private MenuInspectMemoryWordsAction(Value value) {
                super(inspection(), "Inspect memory");
                inspectMemoryWordsAction = getInspectMemoryWordsAction(value);
                if (inspectMemoryWordsAction == null) {
                    setEnabled(false);
                } else {
                    setName(inspectMemoryWordsAction.name());
                    setEnabled(true);
                }
            }

            @Override
            public void procedure() {
                inspectMemoryWordsAction.perform();
            }
        }

        private final class MenuShowMemoryRegionAction extends InspectorAction {

            private final InspectorAction showMemoryRegionAction;

            private MenuShowMemoryRegionAction(Value value) {
                super(inspection(), "Show memory region");
                showMemoryRegionAction = getShowMemoryRegionAction(value);
                if (showMemoryRegionAction == null) {
                    setEnabled(false);
                } else {
                    setEnabled(true);
                    setName(showMemoryRegionAction.name());
                }
            }

            @Override
            public void procedure() {
                showMemoryRegionAction.perform();
            }
        }

        public WordValueMenuItems(Inspection inspection, Value value) {
            add(inspection.actions().copyValue(value, "Copy value to clipboard"));
            add(new MenuInspectObjectAction(value));
            add(new MenuToggleDisplayAction());
            add(new MenuInspectMemoryWordsAction(value));
            add(new MenuShowMemoryRegionAction(value));
        }
    }

}
