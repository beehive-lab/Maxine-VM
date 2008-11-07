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

import java.awt.event.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.method.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 *
 * A textual label for a word of machine data from the {@link TeleVM},
 * with multiple display modes and user interaction affordances.
 */
public class WordValueLabel extends ValueLabel {

    /**
     * The expected kind of word value. The visual
     * representations available (of which there may only
     * be one) are derived from this and the word's value.
     */
    public enum ValueMode {
        WORD, REFERENCE, LITERAL_REFERENCE, INTEGER_REGISTER, FLAGS_REGISTER, FLOATING_POINT, CALL_ENTRY_POINT, ITABLE_ENTRY, CALL_RETURN_POINT;
    }

    private final ValueMode _valueMode;

    /**
     * The actual kind of word value, determined empirically by reading from the {@link TeleVM}; this may change after update.
     * Possible visual presentations of a word, constrained by the {@linkplain ValueMode valueMode} of the
     * label and its value.
     */
    private enum ValueKind {
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
        UNCHECKED_REFERENCE, // understood to be a reference, but not checked by reading from the {@link TeleVM}.
        UNCHECKED_CALL_POINT, // understood to be a code pointer, but not checked by reading from the {@link TeleVM}.
        UNCHECKED_WORD, // unknown word value, not checked by reading from the {@link TeleVM}..
        INVALID // this value is completely invalid
    }

    private ValueKind _valueKind;

    private String _prefix;

    /**
     * Sets a string to be prepended to all label displays.
     */
    public final void setPrefix(String prefix) {
        _prefix = prefix;
    }

    private String _suffix;

    /**
     * Sets a string to be appended to all label displays.
     */
    public final void setSuffix(String suffix) {
        _suffix = suffix;
    }

    private String _toolTipSuffix;

    /**
     * Sets a string to be appended to all tooltip displays over the label.
     */
    public final void setToolTipSuffix(String toolTipSuffix) {
        _toolTipSuffix = toolTipSuffix;
    }

    /**
     * Creates a display label for a word of machine data, initially set to null.
     * Automatically updated if {@link ValueLabel#fetchValue()} is overridden.
     *
     * @param valueMode presumed type of value for the word, influences display modes
     */
    public WordValueLabel(Inspection inspection, ValueMode valueMode) {
        this(inspection, valueMode, Word.zero());
    }

    /**
     * Creates a display label for a word of machine data.
     * Automatically updated if {@link ValueLabel#fetchValue()} is overridden.
     *
     * @param valueMode presumed type of value for the word, influences display modes
     * @param word initial value for word
     */
    public WordValueLabel(Inspection inspection, ValueMode valueMode, Word word) {
        super(inspection, null);
        _valueMode = valueMode;
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
                switch (MaxineInspector.mouseButtonWithModifiers(mouseEvent)) {
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
                        final InspectorMenu menu = new InspectorMenu();
                        menu.add(new WordValueMenuItems(inspection(), value()));
                        switch (_valueKind) {
                            case OBJECT_REFERENCE:
                            case OBJECT_REFERENCE_TEXT: {
                                final TeleObject teleObject = TeleObject.make(teleVM(), teleVM().wordToReference(value().toWord()));
                                final TeleClassMethodActor teleClassMethodActor = teleObject.getTeleClassMethodActorForObject();
                                if (teleClassMethodActor != null) {
                                    // Add method-related menu items
                                    menu.add(new ClassMethodMenuItems(inspection(), teleClassMethodActor));
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
                        menu.popupMenu().show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                    }
                }
            }
        });
    }

    /** Object in the {@link TeleVM} heap pointed to by the word, if it is a valid reference. */
    private TeleObject _teleObject;

    /** Non-null if a Class ID. */
    private TeleClassActor _teleClassActor;

    /** Non-null if a code pointer. */
    private TeleTargetMethod _teleTargetMethod;

    /** Non-null if a stack reference. */
    private TeleNativeThread _teleNativeThread;

    @Override
    public void setValue(Value newValue) {
        _teleObject = null;
        _teleClassActor = null;
        _teleTargetMethod = null;
        _teleNativeThread = null;

        if (newValue == VoidValue.VOID) {
            _valueKind = ValueKind.INVALID;
        } else if (_valueMode == ValueMode.FLAGS_REGISTER) {
            if (newValue == null) {
                _valueKind = ValueKind.INVALID;
            } else if (_valueKind == null) {
                _valueKind = ValueKind.FLAGS;
            }
        } else if (_valueMode == ValueMode.FLOATING_POINT) {
            if (newValue == null) {
                _valueKind = ValueKind.INVALID;
            } else if (_valueKind == null) {
                _valueKind = ValueKind.DOUBLE;
            }
        } else if (!inspection().investigateWordValues()) {
            if (_valueMode == ValueMode.REFERENCE || _valueMode == ValueMode.LITERAL_REFERENCE) {
                _valueKind = ValueKind.UNCHECKED_REFERENCE;
            } else if (_valueMode == ValueMode.CALL_ENTRY_POINT || _valueMode == ValueMode.CALL_RETURN_POINT) {
                _valueKind = ValueKind.UNCHECKED_CALL_POINT;
            } else {
                _valueKind = ValueKind.UNCHECKED_WORD;
            }
        } else {
            _valueKind = ValueKind.WORD;
            if (teleVM().isBootImageRelocated()) {
                if (newValue == null || newValue.isZero()) {
                    if (_valueMode == ValueMode.REFERENCE) {
                        _valueKind = ValueKind.NULL;
                    }
                } else if (teleVM().isValidReference(teleVM().wordToReference(newValue.toWord()))) {
                    _valueKind = (_valueMode == ValueMode.REFERENCE || _valueMode == ValueMode.LITERAL_REFERENCE) ? ValueKind.OBJECT_REFERENCE_TEXT : ValueKind.OBJECT_REFERENCE;
                    final TeleReference reference = (TeleReference) teleVM().wordToReference(newValue.toWord());

                    try {
                        _teleObject = TeleObject.make(teleVM(), reference);
                    } catch (Throwable throwable) {
                        // If we don't catch this the views will not be updated at all.
                        _teleObject = null;
                        _valueKind = ValueKind.INVALID_OBJECT_REFERENCE;
                    }
                } else {
                    final Address address = newValue.asWord().asAddress();
                    _teleNativeThread = teleVM().teleProcess().threadContaining(address);
                    if (_teleNativeThread != null) {
                        _valueKind = _valueMode == ValueMode.REFERENCE ? ValueKind.STACK_LOCATION_TEXT : ValueKind.STACK_LOCATION;
                    } else {
                        if (_valueMode == ValueMode.REFERENCE || _valueMode == ValueMode.LITERAL_REFERENCE) {
                            _valueKind = ValueKind.INVALID_OBJECT_REFERENCE;
                        } else {
                            _teleTargetMethod = TeleTargetMethod.make(teleVM(), newValue.toWord().asAddress());
                            if (_teleTargetMethod != null) {
                                final Address codeStart = _teleTargetMethod.codeStart();
                                final Word jitEntryPoint = codeStart.plus(CallEntryPoint.JIT_ENTRY_POINT.offsetFromCodeStart());
                                final Word optimizedEntryPoint = codeStart.plus(CallEntryPoint.OPTIMIZED_ENTRY_POINT.offsetFromCodeStart());
                                if (newValue.toWord().equals(optimizedEntryPoint) || newValue.toWord().equals(jitEntryPoint)) {
                                    _valueKind = (_valueMode == ValueMode.CALL_ENTRY_POINT) ? ValueKind.CALL_ENTRY_POINT_TEXT : ValueKind.CALL_ENTRY_POINT;
                                } else {
                                    _valueKind = (_valueMode == ValueMode.CALL_RETURN_POINT) ? ValueKind.CALL_RETURN_POINT : ValueKind.CALL_RETURN_POINT;
                                }
                            } else if (_valueMode == ValueMode.ITABLE_ENTRY) {
                                final TeleClassActor teleClassActor = teleVM().teleClassRegistry().findTeleClassActorByID(newValue.asWord().asAddress().toInt());
                                if (teleClassActor != null) {
                                    _teleClassActor = teleClassActor;
                                    _valueKind = ValueKind.CLASS_ACTOR;
                                } else {
                                    _valueKind = ValueKind.CLASS_ACTOR_ID;
                                }
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
        setBackground(style().wordDataBackgroundColor());
        if (value == VoidValue.VOID) {
            setFont(style().wordAlternateTextFont());
            setForeground(style().wordInvalidDataColor());
            setText("void");
            setToolTipText("Location not in allocated regions");
            return;
        }
        final String hexString = (_valueMode == ValueMode.INTEGER_REGISTER || _valueMode == ValueMode.FLAGS_REGISTER || _valueMode == ValueMode.FLOATING_POINT) ? value.toWord().toPaddedHexString('0') : value.toWord().toHexString();
        switch (_valueKind) {
            case WORD: {
                setFont(style().wordDataFont());
                setForeground(value.isZero() ? style().wordNullDataColor() : style().wordDataColor());
                setText(hexString);
                setToolTipText("Int: " + (value.isZero() ? 0 : Long.toString(value.toLong())));
                break;
            }
            case UNCHECKED_WORD: {
                setFont(style().wordDataFont());
                setForeground(value.isZero() ? style().wordNullDataColor() : style().wordDataColor());
                setText(hexString);
                setToolTipText("Unchecked word");
                break;
            }
            case NULL: {
                setFont(style().wordAlternateTextFont());
                setForeground(style().wordNullDataColor());
                setText("null");
                if (_valueMode == ValueMode.LITERAL_REFERENCE) {
                    setToolTipText("null" + _toolTipSuffix);
                }
                break;
            }
            case INVALID: {
                setFont(style().wordAlternateTextFont());
                setForeground(style().wordInvalidDataColor());
                setText("invalid");
                if (_valueMode == ValueMode.LITERAL_REFERENCE) {
                    setToolTipText("invalid" + _toolTipSuffix);
                }
                break;
            }
            case OBJECT_REFERENCE: {
                setFont(style().wordDataFont());
                setForeground(style().wordValidObjectReferenceDataColor());
                setText(hexString);
                if (_valueMode == ValueMode.LITERAL_REFERENCE) {
                    setToolTipText(inspection().nameDisplay().referenceToolTipText(_teleObject) + _toolTipSuffix);
                } else {
                    setToolTipText(inspection().nameDisplay().referenceToolTipText(_teleObject));
                }
                break;
            }
            case OBJECT_REFERENCE_TEXT: {
                try {
                    final String labelText = inspection().nameDisplay().referenceLabelText(_teleObject);
                    if (labelText != null) {
                        setText(labelText);
                        setToolTipText(inspection().nameDisplay().referenceToolTipText(_teleObject));
                        setFont(style().wordAlternateTextFont());
                        setForeground(style().wordValidObjectReferenceDataColor());
                        if (_valueMode == ValueMode.LITERAL_REFERENCE) {
                            setToolTipText(getToolTipText() + _toolTipSuffix);
                        }
                        break;
                    }
                } catch (NoClassDefFoundError noClassDefFoundError) {
                    // some required class is only known in the tele VM
                    System.out.println("WVL: setAlternateReferenceText error" + noClassDefFoundError);
                }
                System.out.println("WVL:  set AlternateReferenceText failed");
                _valueKind = ValueKind.OBJECT_REFERENCE;
                updateText();
                break;
            }
            case STACK_LOCATION: {
                setFont(style().wordDataFont());
                setForeground(style().wordStackLocationDataColor());
                setText(hexString);
                final String threadName = inspection().nameDisplay().longName(_teleNativeThread);
                final long offset = value().asWord().asAddress().minus(_teleNativeThread.stack().start()).toLong();
                final String hexOffsetString = offset >= 0 ? ("+0x" + Long.toHexString(offset)) : "0x" + Long.toHexString(offset);
                setToolTipText("Stack:  thread=" + threadName + ", offset=" + hexOffsetString);
                break;
            }
            case STACK_LOCATION_TEXT: {
                setFont(style().wordAlternateTextFont());
                setForeground(style().wordStackLocationDataColor());
                final String threadName = inspection().nameDisplay().longName(_teleNativeThread);
                final long offset = value().asWord().asAddress().minus(_teleNativeThread.stack().start()).toLong();
                final String decimalOffsetString = offset >= 0 ? ("+" + offset) : Long.toString(offset);
                setText(threadName + " " + decimalOffsetString);
                setToolTipText("Stack:  thread=" + threadName + ", addr=0x" +  Long.toHexString(value().asWord().asAddress().toLong()));
                break;
            }
            case UNCHECKED_REFERENCE: {
                setFont(style().wordDataFont());
                setForeground(style().wordUncheckedReferenceDataColor());
                setText(hexString);
                if (_valueMode == ValueMode.LITERAL_REFERENCE) {
                    setToolTipText("<unchecked>" + _toolTipSuffix);
                } else {
                    setToolTipText("Unchecked Reference");
                }
                break;
            }
            case INVALID_OBJECT_REFERENCE: {
                setFont(style().wordDataFont());
                setForeground(style().wordInvalidObjectReferenceDataColor());
                setText(hexString);
                if (_valueMode == ValueMode.LITERAL_REFERENCE) {
                    setToolTipText("<invalid>" + _toolTipSuffix);
                }
                break;
            }
            case CALL_ENTRY_POINT: {
                setFont(style().wordDataFont());
                setForeground(style().wordCallEntryPointColor());
                setText(hexString);
                setToolTipText("Code: " + inspection().nameDisplay().longName(_teleTargetMethod));
                break;
            }
            case CALL_ENTRY_POINT_TEXT: {
                setFont(style().wordAlternateTextFont());
                setForeground(style().wordCallEntryPointColor());
                setText(inspection().nameDisplay().veryShortName(_teleTargetMethod));
                setToolTipText("Code: " + inspection().nameDisplay().longName(_teleTargetMethod));
                break;
            }
            case CLASS_ACTOR_ID: {
                setFont(style().wordDataFont());
                setForeground(style().wordDataColor());
                setText(Long.toString(value.asWord().asAddress().toLong()));
                if (_teleClassActor != null) {
                    setToolTipText(inspection().nameDisplay().referenceToolTipText(_teleClassActor));
                } else {
                    setToolTipText("Class{???}");
                }
                break;
            }
            case CLASS_ACTOR: {
                setFont(style().javaClassNameFont());
                setForeground(style().javaNameColor());
                setText(_teleClassActor.classActor().simpleName());
                setToolTipText(inspection().nameDisplay().referenceToolTipText(_teleClassActor));
                break;
            }
            case CALL_RETURN_POINT: {
                setFont(style().wordDataFont());
                setForeground(style().wordCallReturnPointColor());
                setText(hexString);
                if (_teleTargetMethod != null) {
                    setToolTipText("Code: " + inspection().nameDisplay().longName(_teleTargetMethod, value.toWord().asAddress()));
                }
                break;
            }
            case CALL_RETURN_POINT_TEXT: {
                setFont(style().wordAlternateTextFont());
                setForeground(style().wordCallReturnPointColor());
                if (_teleTargetMethod != null) {
                    setText(inspection().nameDisplay().veryShortName(_teleTargetMethod, value.toWord().asAddress()));
                    setToolTipText("Code: " + inspection().nameDisplay().longName(_teleTargetMethod, value.toWord().asAddress()));
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
                setForeground(style().wordDataColor());
                setText(TeleStateRegisters.flagsToString(teleVM(), value.toLong()));
                setToolTipText("Flags 0x" + hexString);
                break;
            }
            case DECIMAL: {
                setFont(style().decimalDataFont());
                setForeground(style().wordDataColor());
                setText(Integer.toString(value.toInt()));
                setToolTipText("0x" + hexString);
                break;
            }
            case FLOAT: {
                setFont(style().wordAlternateTextFont());
                setForeground(style().wordDataColor());
                setText(Float.toString(Float.intBitsToFloat((int) (value.toLong() & 0xffffffffL))));
                setToolTipText("0x" + hexString);
                break;
            }
            case DOUBLE: {
                setFont(style().wordAlternateTextFont());
                setForeground(style().wordDataColor());
                setText(Double.toString(Double.longBitsToDouble(value.toLong())));
                setToolTipText("0x" + hexString);
                break;
            }
        }
        if (_prefix != null) {
            setText(_prefix + getText());
        }
        if (_suffix != null) {
            setText(getText() + _suffix);
        }
    }

    private InspectorAction getToggleDisplayTextAction() {
        ValueKind alternateValueKind = _valueKind;
        if (_valueMode == ValueMode.FLAGS_REGISTER) {
            switch (_valueKind) {
                case WORD: {
                    alternateValueKind = ValueKind.FLAGS;
                    break;
                }
                case FLAGS: {
                    alternateValueKind = ValueKind.WORD;
                    break;
                }
                default: {
                    break;
                }
            }
        }
        if (_valueMode == ValueMode.FLOATING_POINT) {
            switch (alternateValueKind) {
                case WORD: {
                    alternateValueKind = ValueKind.DOUBLE;
                    break;
                }
                case DOUBLE: {
                    alternateValueKind = ValueKind.FLOAT;
                    break;
                }
                case FLOAT: {
                    alternateValueKind = ValueKind.WORD;
                    break;
                }
                default: {
                    break;
                }
            }
        }
        if (_valueMode == ValueMode.INTEGER_REGISTER) {
            switch (alternateValueKind) {
                case WORD: {
                    alternateValueKind = ValueKind.DECIMAL;
                    break;
                }
                case DECIMAL: {
                    alternateValueKind = ValueKind.WORD;
                    break;
                }
                default: {
                    break;
                }
            }
        }
        switch (alternateValueKind) {
            case OBJECT_REFERENCE: {
                alternateValueKind = ValueKind.OBJECT_REFERENCE_TEXT;
                break;
            }
            case OBJECT_REFERENCE_TEXT: {
                alternateValueKind = ValueKind.OBJECT_REFERENCE;
                break;
            }
            case STACK_LOCATION: {
                alternateValueKind = ValueKind.STACK_LOCATION_TEXT;
                break;
            }
            case STACK_LOCATION_TEXT: {
                alternateValueKind = ValueKind.STACK_LOCATION;
                break;
            }
            case CALL_ENTRY_POINT: {
                alternateValueKind = ValueKind.CALL_ENTRY_POINT_TEXT;
                break;
            }
            case CALL_ENTRY_POINT_TEXT: {
                alternateValueKind = ValueKind.CALL_ENTRY_POINT;
                break;
            }
            case CLASS_ACTOR_ID: {
                if (_teleClassActor != null) {
                    alternateValueKind = ValueKind.CLASS_ACTOR;
                }
                break;
            }
            case CLASS_ACTOR: {
                alternateValueKind = ValueKind.CLASS_ACTOR_ID;
                break;
            }
            case CALL_RETURN_POINT: {
                alternateValueKind = ValueKind.CALL_RETURN_POINT_TEXT;
                break;
            }
            case CALL_RETURN_POINT_TEXT: {
                alternateValueKind = ValueKind.CALL_RETURN_POINT;
                break;
            }
            default: {
                break;
            }
        }
        if (alternateValueKind != _valueKind) {
            final ValueKind newValueKind = alternateValueKind;
            return new InspectorAction(inspection(), "Toggle alternate display text") {

                @Override
                public void procedure() {
                    _valueKind = newValueKind;
                    WordValueLabel.this.updateText();
                }
            };
        }
        return null;
    }

    private InspectorAction getInspectValueAction(Value value) {
        InspectorAction action = null;
        switch (_valueKind) {
            case OBJECT_REFERENCE:
            case UNCHECKED_REFERENCE:
            case OBJECT_REFERENCE_TEXT: {
                final TeleObject teleObject = TeleObject.make(teleVM(), teleVM().wordToReference(value.toWord()));
                action = inspection().inspectionMenus().getInspectObjectAction(teleObject);
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
                        inspection().focus().setCodeLocation(new TeleCodeLocation(teleVM(), address), true);
                    }
                };
                break;
            }
            case CLASS_ACTOR_ID:
            case CLASS_ACTOR: {
                final TeleClassActor teleClassActor = teleVM().teleClassRegistry().findTeleClassActorByID(value.asWord().asAddress().toInt());
                if (teleClassActor != null) {
                    action = inspection().inspectionMenus().getInspectObjectAction(teleClassActor, "Inspect ClassActor");
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

    private InspectorAction getInspectMemoryAction(Value value) {
        InspectorAction action = null;
        if (value != VoidValue.VOID) {
            final Address address = value.toWord().asAddress();
            switch (_valueKind) {
                case INVALID_OBJECT_REFERENCE:
                case UNCHECKED_REFERENCE:
                case OBJECT_REFERENCE:
                case OBJECT_REFERENCE_TEXT:
                case STACK_LOCATION:
                case  STACK_LOCATION_TEXT:
                case CALL_ENTRY_POINT:
                case CALL_ENTRY_POINT_TEXT:
                case CALL_RETURN_POINT:
                case CALL_RETURN_POINT_TEXT:
                case UNCHECKED_CALL_POINT: {
                    action = inspection().inspectionMenus().getInspectMemoryAction(address);
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
                    if (teleVM().heapOrCodeOrStackContains(address)) {
                        action = inspection().inspectionMenus().getInspectMemoryAction(address);
                    }
                    break;
                }
            }
        }
        return action;
    }

    private InspectorAction getInspectMemoryWordsAction(Value value) {
        InspectorAction action = null;
        if (value != VoidValue.VOID) {
            final Address address = value.toWord().asAddress();
            switch (_valueKind) {
                case INVALID_OBJECT_REFERENCE:
                case UNCHECKED_REFERENCE:
                case OBJECT_REFERENCE:
                case OBJECT_REFERENCE_TEXT:
                case STACK_LOCATION:
                case  STACK_LOCATION_TEXT:
                case CALL_ENTRY_POINT:
                case CALL_ENTRY_POINT_TEXT:
                case CALL_RETURN_POINT:
                case CALL_RETURN_POINT_TEXT:
                case UNCHECKED_CALL_POINT: {
                    action = inspection().inspectionMenus().getInspectMemoryWordsAction(address);
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
                    if (teleVM().heapOrCodeOrStackContains(address)) {
                        action = inspection().inspectionMenus().getInspectMemoryWordsAction(address);
                    }
                    break;
                }
            }
        }
        return action;
    }

    private final class WordValueMenuItems implements InspectorMenuItems {

        private final InspectorAction _copyWordAction;

        private final class MenuInspectObjectAction extends InspectorAction {

            private final InspectorAction _inspectAction;

            private MenuInspectObjectAction(Value value) {
                super(inspection(), "Inspect Object (Left-Button)");
                _inspectAction = getInspectValueAction(value);
                setEnabled(_inspectAction != null);
            }

            @Override
            public void procedure() {
                _inspectAction.perform();
            }
        }

        private final MenuInspectObjectAction _menuInspectObjectAction;


        private final class MenuToggleDisplayAction extends InspectorAction {

            private final InspectorAction _toggleAction;

            private MenuToggleDisplayAction() {
                super(inspection(), "Toggle display (Middle-Button)");
                _toggleAction = getToggleDisplayTextAction();
                setEnabled(_toggleAction != null);
            }

            @Override
            public void procedure() {
                _toggleAction.perform();
            }
        }

        private final MenuToggleDisplayAction _menuToggleDisplayAction;

        private final class MenuInspectMemoryAction extends InspectorAction {

            private final InspectorAction _inspectMemoryAction;

            private MenuInspectMemoryAction(Value value) {
                super(inspection(), "Inspect memory");
                _inspectMemoryAction = getInspectMemoryAction(value);
                setEnabled(_inspectMemoryAction != null);
            }

            @Override
            public void procedure() {
                _inspectMemoryAction.perform();
            }
        }

        private final MenuInspectMemoryAction _menuInspectMemoryAction;

        private final class MenuInspectMemoryWordsAction extends InspectorAction {

            private final InspectorAction _inspectMemoryWordsAction;

            private MenuInspectMemoryWordsAction(Value value) {
                super(inspection(), "Inspect memory words");
                _inspectMemoryWordsAction = getInspectMemoryWordsAction(value);
                setEnabled(_inspectMemoryWordsAction != null);
            }

            @Override
            public void procedure() {
                _inspectMemoryWordsAction.perform();
            }
        }

        private final MenuInspectMemoryWordsAction _menuInspectMemoryWordsAction;

        private WordValueMenuItems(Inspection inspection, Value value) {
            _copyWordAction = inspection.inspectionMenus().getCopyValueAction(value, "Copy value to clipboard");
            _menuInspectObjectAction = new MenuInspectObjectAction(value);
            _menuToggleDisplayAction = new MenuToggleDisplayAction();
            _menuInspectMemoryAction = new MenuInspectMemoryAction(value);
            _menuInspectMemoryWordsAction = new MenuInspectMemoryWordsAction(value);
        }

        public void addTo(InspectorMenu menu) {
            menu.add(_copyWordAction);
            menu.add(_menuInspectObjectAction);
            menu.add(_menuToggleDisplayAction);
            menu.add(_menuInspectMemoryAction);
            menu.add(_menuInspectMemoryWordsAction);
            menu.addSeparator();

        }
        public Inspection inspection() {
            return WordValueLabel.this.inspection();
        }

        public void refresh(long epoch, boolean force) {
        }

        public void redisplay() {
        }
    }


}
