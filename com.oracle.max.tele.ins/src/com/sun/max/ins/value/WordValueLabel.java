/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.value;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.method.*;
import com.sun.max.ins.object.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.MaxMarkBitmap.MarkColor;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.value.*;

// TODO (mlvdv) the design of this class has long gotten out of control; there are much better alternatives,
// for example reifying a subset of the display modes using the State patter.
/**
 * A textual label for a word of machine data from the VM,
 * with multiple display modes and user interaction affordances.
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
        HUB_REFERENCE,
        LITERAL_REFERENCE,
        INTEGER_REGISTER,
        FLAGS_REGISTER,
        FLOATING_POINT,
        SIZE,
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

        /**
         * Display generically as a word of data, about which nothing is known other than non-null.
         */
        WORD,

        /**
         * Display generically as a word of data in the special case where the word is the null value.
         */
        NULL_WORD,

        /**
         * Expected to be an object reference, but something about it is broken.
         */
        INVALID_OBJECT_REFERENCE,

        /**
         * Numeric display of a valid object reference.
         */
        OBJECT_REFERENCE,

        /**
         * Textual display of a valid object reference.
         */
        OBJECT_REFERENCE_TEXT,

        /**
         * Numeric display of a valid object reference occurring in a Hub field.
         */
        HUB_REFERENCE,

        /**
         * Textual display of a valid object reference occurring in a Hub field.
         */
        HUB_REFERENCE_TEXT,

        /**
         * Numeric display of a valid object reference.
         */
        QUASI_OBJECT_REFERENCE,

        /**
         * Textual display of a valid object reference.
         */
        QUASI_OBJECT_REFERENCE_TEXT,

        /**
         * Numeric display of a valid pointer into a stack.
         */
        STACK_LOCATION,

        /**
         * Textual display of a valid pointer into a stack.
         */
        STACK_LOCATION_TEXT,

        /**
         * Numeric display of a valid pointer to a thread local value.
         */
        THREAD_LOCALS_BLOCK_LOCATION,

        /**
         * Textual display of a valid pointer to a thread local value.
         */
        THREAD_LOCALS_BLOCK_LOCATION_TEXT,

        /**
         * Numeric display of a valid pointer to a method entry in compiled code.
         */
        CALL_ENTRY_POINT,

        /**
         * Textual display of a valid pointer to a method entry in compiled code.
         */
        CALL_ENTRY_POINT_TEXT,

        /**
         * The internal ID of a {@link ClassActor} in the VM.
         */
        CLASS_ACTOR_ID,

        CLASS_ACTOR,

        /**
         * Numeric display of a valid pointer into a method in compiled code, not at the entry.
         */
        CALL_RETURN_POINT,

        /**
         * Textual display of a valid pointer into a method in compiled code, not at the entry.
         */
        CALL_RETURN_POINT_TEXT,

        /**
         * Numeric display of a valid pointer into a native function.
         */
        NATIVE_FUNCTION,

        /**
         * Textual display of a valid pointer into a native function.
         */
        NATIVE_FUNCTION_TEXT,

        /**
         * Display of bits interpreted as flags.
         */
        FLAGS,

        /**
         * Display of numeric value in decimal.
         */
        DECIMAL,

        /**
         * Display of a floating point value.
         */
        FLOAT,

        /**
         * Display of an extended floating point value.
         */
        DOUBLE,
        /**
         * Display of an unsigned decimal numeric value representing a Size.
         */
        SIZE,
        /**
         * Numeric display of what is expected to be a reference, but which is not checked by reading from the VM.
         */
        UNCHECKED_REFERENCE,

        /**
         * Numeric display of what is expected to point to a code call site, but which is not checked by reading from the VM.
         */
        UNCHECKED_CALL_POINT,

        /**
         * Numeric display of a word for which there are no expectations, and which is not checked by reading from the VM.
         */
        UNCHECKED_WORD,

        /**
         * Numeric display of a word whose value is invalid relative to expectations for it.
         */
        INVALID,

        /**
         * Display in situations where the value cannot be read from the VM.
         */
        UNAVAILABLE;
    }

    private DisplayMode displayMode = null;

    private final boolean preferTextInitially;

    private Font wordDataFont;

    /**
     * Creates a display label for a word of machine data, initially set to null.
     * <p>
     * Content of label is supplied by override {@link ValueLabel#fetchValue()}, which
     * gets called initially and when the label is refreshed.
     * <br>
     * Display state can be cycled among alternate presentations in some situations.
     * <p>
     * Can be used as a cell renderer in a table, but the enclosing table must be explicitly repainted
     * when the display state is cycled; this will be done automatically if the table is passed in
     * as the parent component.
     *
     * @param inspection
     * @param valueMode presumed type of value for the word, influences display modes
     * @param parent a component that should be repainted when the display state is cycled;
     */
    public WordValueLabel(Inspection inspection, ValueMode valueMode, Component parent) {
        this(inspection, valueMode, Word.zero(), parent);
    }

    /**
     * Creates a display label for a word of machine data, initially set to null.
     * <p>
     * Content of label is supplied by override {@link ValueLabel#fetchValue()}, which
     * gets called initially and when the label is refreshed.
     * <br>
     * Display state can be cycled among alternate presentations in some situations.
     * <p>
     * Can be used as a cell renderer in a table, but the enclosing table must be explicitly repainted
     * when the display state is cycled; this will be done automatically if the table is passed in
     * as the parent component.
     *
     * @param inspection
     * @param valueMode presumed type of value for the word, influences display modes
     * @param parent a component that should be repainted when the display state is cycled;
     * @param preferTextInitially if {@code true} causes any possible alternate (textual) display to be used initially.
     */
    public WordValueLabel(Inspection inspection, ValueMode valueMode, Component parent, boolean preferTextInitially) {
        this(inspection, valueMode, Word.zero(), parent, preferTextInitially);
    }

    /**
     * Creates a display label for a word of machine data, initially set to null.
     * <p>
     * Content of label is set initially by parameter.  It can be updated by overriding{@link ValueLabel#fetchValue()}, which
     * gets called initially and when the label is refreshed.
     * <p>
     * Display state can be cycled among alternate presentations in some situations.
     * <p>
     * Can be used as a cell renderer in a table, but the enclosing table must be explicitly repainted
     * when the display state is cycled; this will be done automatically if the table is passed in
     * as the parent component.
     *
     * @param inspection
     * @param valueMode presumed type of value for the word, influences display modes
     * @param word initial value for content.
     * @param parent a component that should be repainted when the display state is cycled;
     */
    public WordValueLabel(Inspection inspection, ValueMode valueMode, Word word, Component parent) {
        this(inspection, valueMode, word, parent, inspection.preference().forceTextualWordValueDisplay());
    }

    /**
     * Creates a display label for a word of machine data, initially set to null.
     * <p>
     * Content of label is set initially by parameter.  It can be updated by overriding{@link ValueLabel#fetchValue()}, which
     * gets called initially and when the label is refreshed.
     * <p>
     * Display state can be cycled among alternate presentations in some situations, and the {@code forceTxt} can specify
     * which to use initially.
     * <p>
     * Can be used as a cell renderer in a table, but the enclosing table must be explicitly repainted
     * when the display state is cycled; this will be done automatically if the table is passed in
     * as the parent component.
     *
     * @param inspection
     * @param valueMode presumed type of value for the word, influences display modes
     * @param word initial value for content.
     * @param parent a component that should be repainted when the display state is cycled;
     * @param preferTextInitially if {@code true} causes any possible alternate (textual) display to be used initially.
     */
    public WordValueLabel(Inspection inspection, ValueMode valueMode, Word word, Component parent, boolean preferTextInitially) {
        super(inspection, null);
        this.parent = parent;
        this.valueMode = valueMode;
        this.wordDataFont = inspection.preference().style().defaultWordDataFont();
        this.preferTextInitially = preferTextInitially;
        initializeValue();
        if (value() == null) {
            setValue(new WordValue(word));
        }
        redisplay();
        addMouseListener(new InspectorMouseClickAdapter(inspection()) {
            @Override
            public void procedure(final MouseEvent mouseEvent) {
                //System.out.println("WVL (" + _valueMode.toString() + ", " + _valueKind.toString() + ")");
                switch (inspection().gui().getButton(mouseEvent)) {
                    case MouseEvent.BUTTON1: {
                        if (mouseEvent.isShiftDown()) {
                            final InspectorAction viewMemoryAction = getViewMemoryAction(value());
                            if (viewMemoryAction != null) {
                                viewMemoryAction.perform();
                            }
                        } else if (mouseEvent.isMetaDown()) {
                            InspectorAction inspectAction = getCloseAndViewObjectAction(value());
                            if (inspectAction == null) {
                                inspectAction = getInspectValueAction(value());
                            }
                            if (inspectAction != null) {
                                inspectAction.perform();
                            }
                        } else {
                            final InspectorAction inspectAction = getInspectValueAction(value());
                            if (inspectAction != null) {
                                inspectAction.perform();
                            }
                        }
                        break;
                    }
                    case MouseEvent.BUTTON2: {
                        final InspectorAction cycleAction = getCycleDisplayTextAction();
                        if (cycleAction != null) {
                            cycleAction.perform();
                        }
                        break;
                    }
                    case MouseEvent.BUTTON3: {
                        final InspectorPopupMenu menu = new InspectorPopupMenu();
                        menu.add(new WordValueMenuItems(inspection(), value()));
                        switch (displayMode) {
                            case OBJECT_REFERENCE:
                            case OBJECT_REFERENCE_TEXT: {
                                MaxObject object = null;
                                try {
                                    object = vm().objects().findObjectAt(value().toWord().asAddress());
                                } catch (MaxVMBusyException e) {
                                }
                                if (object != null) {
                                    final TeleClassMethodActor teleClassMethodActor = object.getTeleClassMethodActorForObject();
                                    if (teleClassMethodActor != null) {
                                        // Add method-related menu items
                                        final ClassMethodActorMenuItems items = new ClassMethodActorMenuItems(inspection(), teleClassMethodActor);
                                        items.addTo(menu);
                                    }
                                }
                                break;
                            }
                            case QUASI_OBJECT_REFERENCE:
                            case QUASI_OBJECT_REFERENCE_TEXT: {
                                // TODO (mlvdv) special right-button menu items to a pointer at a quasi object, perhaps specialized for each kind
                                break;
                            }
                            case HUB_REFERENCE:
                            case HUB_REFERENCE_TEXT: {
                                // TODO (mlvdv) special right-button menu items appropriate to a forwarding pointer
                                break;
                            }
                            case STACK_LOCATION:
                            case STACK_LOCATION_TEXT: {
                                // TODO (mlvdv)  special right-button menu items appropriate to a pointer into stack memory
                                break;
                            }
                            case THREAD_LOCALS_BLOCK_LOCATION:
                            case THREAD_LOCALS_BLOCK_LOCATION_TEXT: {
                                // TODO (mlvdv)  special right-button menu items appropriate to a pointer into a thread locals block
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
    private MaxObject object;

    /** Non-null if a Class ID. */
    private TeleClassActor teleClassActor;

    /** Non-null if a pointer into a method compilation. */
    private MaxCompilation compilation;

    /** Non-null if a tagged pointer into a method compilation, only meaningful if compilation is non-null. */
    private RemoteCodePointer taggedCodePointer;

    /** Non-null if a pointer into a native function. */
    TeleNativeFunction nativeFunction;

    /** Non-null if a pointer into thread memory. */
    private MaxThread thread;

    /** Non-null if a pointer into a stack. */
    private MaxStack stack;

    /** Non-null if pointer at a thread local variable. */
    private MaxThreadLocalsBlock threadLocalsBlock;

    @Override
    public final void setValue(Value newValue) {
        object = null;
        teleClassActor = null;
        compilation = null;
        taggedCodePointer = null;
        thread = null;
        stack = null;
        threadLocalsBlock = null;

        /**
         * The previous display mode, null initially, which will supersede the default and be reused if appropriate
         */
        final DisplayMode oldDisplayMode = displayMode;

        /**
         * {@code true} only the first time the value gets set <em>and</em> there is a preference for initial text.
         * After the first time, the mode previously in effect, as selected by the user, prevails if appropriate to the value.
         */
        final boolean forceText = oldDisplayMode == null && preferTextInitially;

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
        } else if (valueMode == ValueMode.SIZE) {
            displayMode = DisplayMode.SIZE;
        } else if (!preference().investigateWordValues()) {
            if (valueMode == ValueMode.REFERENCE || valueMode == ValueMode.LITERAL_REFERENCE) {
                displayMode = DisplayMode.UNCHECKED_REFERENCE;
            } else if (valueMode == ValueMode.CALL_ENTRY_POINT || valueMode == ValueMode.CALL_RETURN_POINT) {
                displayMode = DisplayMode.UNCHECKED_CALL_POINT;
            } else {
                displayMode = DisplayMode.UNCHECKED_WORD;
            }
        } else {
            displayMode = DisplayMode.WORD;
            if (vm().isBootImageRelocated()) {
                try {
                    final Address address = newValue.toWord().asAddress();
                    thread = vm().threadManager().findThread(address);
                    // From here on, we need to try reading from the VM, if it is available
                    if (newValue == null || newValue.isZero()) {
                        if (valueMode == ValueMode.REFERENCE || preferTextInitially) {
                            displayMode = DisplayMode.NULL_WORD;
                        }
                    } else if ((object = vm().objects().findObjectAt(address)) != null) {
                        // Value is the address of an object origin; select display mode, keeping previous one if relevant.
                        switch(valueMode) {
                            case HUB_REFERENCE:
                                displayMode = (oldDisplayMode == DisplayMode.HUB_REFERENCE_TEXT || forceText) ? DisplayMode.HUB_REFERENCE_TEXT : DisplayMode.HUB_REFERENCE;
                                break;
                            default:
                                displayMode = (oldDisplayMode == DisplayMode.OBJECT_REFERENCE_TEXT || forceText) ? DisplayMode.OBJECT_REFERENCE_TEXT : DisplayMode.OBJECT_REFERENCE;
                        }
                    } else if ((object = vm().objects().findQuasiObjectAt(address)) != null) {
                        displayMode = (oldDisplayMode == DisplayMode.QUASI_OBJECT_REFERENCE_TEXT || forceText) ? DisplayMode.QUASI_OBJECT_REFERENCE_TEXT : DisplayMode.QUASI_OBJECT_REFERENCE;
                    } else if (thread != null && thread.stack().memoryRegion().contains(address)) {
                        stack = thread.stack();
                        displayMode = (oldDisplayMode == DisplayMode.STACK_LOCATION_TEXT || forceText) ? DisplayMode.STACK_LOCATION_TEXT : DisplayMode.STACK_LOCATION;
                    } else if (thread != null && thread.localsBlock().memoryRegion() != null && thread.localsBlock().memoryRegion().contains(address)) {
                        threadLocalsBlock = thread.localsBlock();
                        displayMode = (oldDisplayMode == DisplayMode.THREAD_LOCALS_BLOCK_LOCATION_TEXT || forceText) ? DisplayMode.THREAD_LOCALS_BLOCK_LOCATION_TEXT : DisplayMode.THREAD_LOCALS_BLOCK_LOCATION;
                    } else if (valueMode == ValueMode.REFERENCE || valueMode == ValueMode.LITERAL_REFERENCE) {
                        displayMode = DisplayMode.INVALID_OBJECT_REFERENCE;
                    } else if (valueMode == ValueMode.HUB_REFERENCE) {
                        object = vm().objects().findForwardedObjectAt(address);
                        if (object != null) {
                            displayMode = DisplayMode.HUB_REFERENCE_TEXT;
                        } else {
                            displayMode = DisplayMode.INVALID_OBJECT_REFERENCE;
                        }
                    } else {
                        compilation = vm().machineCode().findCompilation(address);
                        if (compilation == null) {
                            // No compilation at that address; maybe it's really a tagged pointer.
                            taggedCodePointer = vm().machineCode().makeCodePointerFromTaggedLong(address.toLong());
                            if (taggedCodePointer != null) {
                                // Could be a code pointer
                                compilation = vm().machineCode().findCompilation(taggedCodePointer);
                            }
                        }
                        if (compilation != null) {
                            // The word points at a method compilation
                            final Address codeStart = compilation.getCodeStart();
                            final Word jitEntryPoint = codeStart.plus(CallEntryPoint.BASELINE_ENTRY_POINT.offset());
                            final Word optimizedEntryPoint = codeStart.plus(CallEntryPoint.OPTIMIZED_ENTRY_POINT.offset());
                            if (newValue.toWord().equals(optimizedEntryPoint) || newValue.toWord().equals(jitEntryPoint)) {
                                displayMode = (oldDisplayMode == DisplayMode.CALL_ENTRY_POINT_TEXT || forceText) ? DisplayMode.CALL_ENTRY_POINT_TEXT : DisplayMode.CALL_ENTRY_POINT;
                            } else {
                                displayMode = (oldDisplayMode == DisplayMode.CALL_RETURN_POINT_TEXT || forceText) ? DisplayMode.CALL_RETURN_POINT_TEXT : DisplayMode.CALL_RETURN_POINT;
                            }
                        } else {
                            nativeFunction = vm().machineCode().findNativeFunction(address);
                            if (nativeFunction != null) {
                                // The word points into a native function
                                displayMode = (valueMode == ValueMode.CALL_ENTRY_POINT || oldDisplayMode == DisplayMode.NATIVE_FUNCTION_TEXT || forceText) ? DisplayMode.NATIVE_FUNCTION_TEXT : DisplayMode.NATIVE_FUNCTION;
                            } else if (valueMode == ValueMode.ITABLE_ENTRY) {
                                final TeleClassActor teleClassActor = vm().classes().findTeleClassActor(address.toInt());
                                if (teleClassActor != null) {
                                    this.teleClassActor = teleClassActor;
                                    displayMode = DisplayMode.CLASS_ACTOR;
                                } else {
                                    displayMode = DisplayMode.CLASS_ACTOR_ID;
                                }
                            }
                        }
                    }
                    final MaxMarkBitmap markBitMap = vm().heap().markBitMap();
                    if (markBitMap != null && markBitMap.isCovered(address)) {
                        final int bitIndex = markBitMap.getBitIndexOf(address);
                        final StringBuilder sb = new StringBuilder();
                        sb.append("<br>Heap mark bit(");
                        sb.append(bitIndex);
                        sb.append(")=");
                        sb.append(markBitMap.isBitSet(bitIndex) ? "1" : "0");
                        sb.append(", color=");
                        sb.append(markBitMap.getMarkColor(bitIndex));
                        this.setToolTipSuffix(sb.toString());
                    }
                } catch (TerminatedProcessIOException terminatedProcessIOException) {
                    object = null;
                    teleClassActor = null;
                    displayMode = DisplayMode.WORD;
                } catch (Throwable throwable) {
                    object = null;
                    teleClassActor = null;
                    displayMode = DisplayMode.INVALID;
                    setWrappedToolTipHtmlText("<b>" + throwable + "</b><br>See log for complete stack trace.");
                    throwable.printStackTrace(Trace.stream());
                }
            }
        }
        super.setValue(newValue);
    }

    public void redisplay() {
        this.wordDataFont = inspection().preference().style().defaultWordDataFont();
        setValue(value());
    }

    @Override
    public void updateText() {
        final Value value = value();
        if (value == null) {
            return;
        }
        final InspectorStyle style = preference().style();
        final InspectorNameDisplay nameDisplay = inspection().nameDisplay();
        if (value == VoidValue.VOID) {
            setFont(style.wordAlternateTextFont());
            setForeground(style.wordInvalidDataColor());
            setWrappedText("void");
            setWrappedToolTipHtmlText(htmlify("<memory unreadable>"));
            if (parent != null) {
                parent.repaint();
            }
            return;
        }
        final String hexString = (valueMode == ValueMode.WORD
                        || valueMode == ValueMode.INTEGER_REGISTER
                        || valueMode == ValueMode.FLAGS_REGISTER
                        || valueMode == ValueMode.FLOATING_POINT) ? value.toWord().toPaddedHexString('0') : value.toWord().toHexString();
        switch (displayMode) {
            case WORD: {
                setFont(wordDataFont);
                setWrappedText(hexString);
                if (value.isZero()) {
                    setForeground(style.wordNullDataColor());
                    setWrappedToolTipHtmlText("zero");
                } else {
                    setForeground(null);
                    final StringBuilder ttText = new StringBuilder();
                    ttText.append(hexString);
                    ttText.append("<br>Decimal= ").append(Long.toString(value.toLong()));
                    final Address address = value.toWord().asAddress();
                    final MaxMemoryRegion memoryRegion = vm().state().findMemoryRegion(address);
                    if (memoryRegion != null) {
                        ttText.append("<br>Points into region ").append(memoryRegion.regionName());
                    }
                    setWrappedToolTipHtmlText(ttText.toString());
                }
                break;
            }
            case UNCHECKED_WORD: {
                setFont(wordDataFont);
                setWrappedText(hexString);
                if (value.isZero()) {
                    setForeground(style.wordNullDataColor());
                    setWrappedToolTipHtmlText("zero");
                } else {
                    setForeground(null);
                    setWrappedToolTipHtmlText(valueToDecimalAndHex(value) + " - UNCHECKED");
                }
                break;
            }
            case NULL_WORD: {
                setFont(style.wordAlternateTextFont());
                setForeground(style.wordNullDataColor());
                setWrappedText("null");
                setWrappedToolTipHtmlText("null");
                break;
            }
            case INVALID: {
                setFont(style.wordAlternateTextFont());
                setForeground(style.wordInvalidDataColor());
                setWrappedText(hexString);
                if (valueMode == ValueMode.LITERAL_REFERENCE) {
                    setWrappedToolTipHtmlText("invalid reference");
                } else {
                    setWrappedToolTipHtmlText(valueToDecimalAndHex(value));
                }
                break;
            }
            case OBJECT_REFERENCE: {
                setFont(wordDataFont);
                setForeground(style.wordValidObjectReferenceDataColor());
                setWrappedText(hexString);
                try {
                    // The syntax of object reference names contains "<" and ">"; make them safe for HTML tool tips.
                    final StringBuilder toolTipSB = new StringBuilder();
                    toolTipSB.append(value.toWord().toPadded0xHexString('0'));
                    toolTipSB.append("<br>Reference(").append(object.status().label()).append(") to ").append(htmlify(nameDisplay.referenceToolTipText(object)));
                    toolTipSB.append("<br>In ");
                    final MaxMemoryRegion memoryRegion = vm().state().findMemoryRegion(value().toWord().asAddress());
                    if (memoryRegion == null) {
                        toolTipSB.append(htmlify("<unknown memory region>"));
                    } else {
                        toolTipSB.append("\"").append(nameDisplay.longName(memoryRegion)).append("\"");
                    }
                    final String gcDescription = object.reference().gcDescription();
                    if (gcDescription != null) {
                        toolTipSB.append("<br>GC: " + gcDescription);
                    }
                    setWrappedToolTipHtmlText(toolTipSB.toString());
                } catch (Throwable throwable) {
                    // If we don't catch this the views will not be updated at all.
                    object = null;
                    displayMode = DisplayMode.INVALID_OBJECT_REFERENCE;
                    setWrappedToolTipHtmlText("<b>" + throwable + "</b><br>See log for complete stack trace.");
                    throwable.printStackTrace(Trace.stream());
                }
                break;
            }
            case QUASI_OBJECT_REFERENCE: {
                // TODO (mlvdv) specialize the display for each kind of quasi object
                setFont(wordDataFont);
                setForeground(style.wordNullDataColor());
                setWrappedText(hexString);
                try {
                    // The syntax of object reference names contains "<" and ">"; make them safe for HTML tool tips.
                    final StringBuilder toolTipSB = new StringBuilder();
                    toolTipSB.append(value.toWord().toPadded0xHexString('0'));
                    toolTipSB.append("<br>Quasi-reference(").append(object.status().label()).append(") to ").append(htmlify(nameDisplay.referenceToolTipText(object)));
                    toolTipSB.append("<br>In ");
                    final MaxMemoryRegion memoryRegion = vm().state().findMemoryRegion(value().toWord().asAddress());
                    if (memoryRegion == null) {
                        toolTipSB.append(htmlify("<unknown memory region>"));
                    } else {
                        toolTipSB.append("\"").append(nameDisplay.longName(memoryRegion)).append("\"");
                    }
                    final String gcDescription = object.reference().gcDescription();
                    if (gcDescription != null) {
                        toolTipSB.append("<br>GC: " + gcDescription);
                    }
                    setWrappedToolTipHtmlText(toolTipSB.toString());
                } catch (Throwable throwable) {
                    // If we don't catch this the views will not be updated at all.
                    object = null;
                    displayMode = DisplayMode.INVALID_OBJECT_REFERENCE;
                    setWrappedToolTipHtmlText("<b>" + throwable + "</b><br>See log for complete stack trace.");
                    throwable.printStackTrace(Trace.stream());
                }
                break;
            }
            case HUB_REFERENCE: {
                final Address address = value.toWord().asAddress();
                String forward = "";
                try {
                    if (vm().objects().findForwardedObjectAt(address) != null) {
                        forward = "=> ";
                    }
                } catch (MaxVMBusyException e) {
                }
                setFont(wordDataFont);
                setForeground(style.wordValidObjectReferenceDataColor());
                setWrappedText(forward + hexString);
                try {
                    // The syntax of object reference names contains "<" and ">"; make them safe for HTML tool tips.
                    final StringBuilder toolTipSB = new StringBuilder();
                    toolTipSB.append(value.toWord().toPadded0xHexString('0'));
                    toolTipSB.append("<br>Pointer to ").append(htmlify(nameDisplay.referenceToolTipText(object)));
                    toolTipSB.append("<br>In ");
                    final MaxMemoryRegion memoryRegion = vm().state().findMemoryRegion(value().toWord().asAddress());
                    if (memoryRegion == null) {
                        toolTipSB.append(htmlify("<unknown memory region>"));
                    } else {
                        toolTipSB.append("\"").append(nameDisplay.longName(memoryRegion)).append("\"");
                    }
                    final String gcDescription = object.reference().gcDescription();
                    if (gcDescription != null) {
                        toolTipSB.append("<br>GC: " + gcDescription);
                    }
                    setWrappedToolTipHtmlText(toolTipSB.toString());
                } catch (Throwable throwable) {
                    // If we don't catch this the views will not be updated at all.
                    object = null;
                    displayMode = DisplayMode.INVALID_OBJECT_REFERENCE;
                    setWrappedToolTipHtmlText("<b>" + throwable + "</b><br>See log for complete stack trace.");
                    throwable.printStackTrace(Trace.stream());
                }
                break;
            }
            case OBJECT_REFERENCE_TEXT: {
                final boolean objectIsForwarded = object != null && object.reference().status().isForwarder();
                setFont(style.wordAlternateTextFont());
                if (objectIsForwarded) {
                    setForeground(style.wordForwardingReferenceDataColor());
                } else {
                    setForeground(style.wordValidObjectReferenceDataColor());
                }
                try {
                    final String labelText = nameDisplay.referenceLabelText(object);
                    if (labelText != null) {
                        setWrappedText(labelText);
                        // The syntax of object reference names contains "<" and ">"; make them safe for HTML tool tips.
                        final StringBuilder toolTipSB = new StringBuilder();
                        toolTipSB.append(value.toWord().toPadded0xHexString('0'));
                        toolTipSB.append("<br>Reference(").append(object.status().label()).append(") to ").append(htmlify(nameDisplay.referenceToolTipText(object)));
                        toolTipSB.append("<br>In ");
                        final MaxMemoryRegion memoryRegion = vm().state().findMemoryRegion(value().toWord().asAddress());
                        if (memoryRegion == null) {
                            toolTipSB.append(htmlify("<unknown memory region>"));
                        } else {
                            toolTipSB.append("\"").append(nameDisplay.longName(memoryRegion)).append("\"");
                        }
                        final String gcDescription = object.reference().gcDescription();
                        if (gcDescription != null) {
                            toolTipSB.append("<br>GC: " + gcDescription);
                        }
                        setWrappedToolTipHtmlText(toolTipSB.toString());
                        break;
                    }
                } catch (Throwable throwable) {
                    // If we don't catch this the views will not be updated at all.
                    object = null;
                    displayMode = DisplayMode.INVALID_OBJECT_REFERENCE;
                    setWrappedToolTipHtmlText("<b>" + throwable + "</b><br>See log for complete stack trace.");
                    throwable.printStackTrace(Trace.stream());
                    break;
                }
                displayMode = DisplayMode.OBJECT_REFERENCE;
                updateText();
                break;
            }
            case QUASI_OBJECT_REFERENCE_TEXT: {
                // TODO (mlvdv) specialize the display for each kind of quasi object
                setFont(style.wordAlternateTextFont());
                setForeground(style.wordNullDataColor());
                try {
                    final String labelText = nameDisplay.referenceLabelText(object);
                    if (labelText != null) {
                        setWrappedText(labelText);
                        // The syntax of object reference names contains "<" and ">"; make them safe for HTML tool tips.
                        final StringBuilder toolTipSB = new StringBuilder();
                        toolTipSB.append(value.toWord().toPadded0xHexString('0'));
                        toolTipSB.append("<br>Quasi-reference(").append(object.status().label()).append(") to ").append(htmlify(nameDisplay.referenceToolTipText(object)));
                        toolTipSB.append("<br>In ");
                        final MaxMemoryRegion memoryRegion = vm().state().findMemoryRegion(value().toWord().asAddress());
                        if (memoryRegion == null) {
                            toolTipSB.append(htmlify("<unknown memory region>"));
                        } else {
                            toolTipSB.append("\"").append(nameDisplay.longName(memoryRegion)).append("\"");
                        }
                        final String gcDescription = object.reference().gcDescription();
                        if (gcDescription != null) {
                            toolTipSB.append("<br>GC: " + gcDescription);
                        }
                        setWrappedToolTipHtmlText(toolTipSB.toString());
                        break;
                    }
                } catch (Throwable throwable) {
                    // If we don't catch this the views will not be updated at all.
                    object = null;
                    displayMode = DisplayMode.INVALID_OBJECT_REFERENCE;
                    setWrappedToolTipHtmlText("<b>" + throwable + "</b><br>See log for complete stack trace.");
                    throwable.printStackTrace(Trace.stream());
                    break;
                }
                displayMode = DisplayMode.OBJECT_REFERENCE;
                updateText();
                break;
            }
            case HUB_REFERENCE_TEXT: {
                final boolean hubIsForwarded = object != null && object.reference().status().isForwarder();
                setFont(style.wordAlternateTextFont());
                if (hubIsForwarded) {
                    setForeground(style.wordForwardingReferenceDataColor());
                } else {
                    setForeground(style.wordValidObjectReferenceDataColor());
                }
                try {
                    final Address address = value.toWord().asAddress();
                    final String forward = vm().objects().findForwardedObjectAt(address) == null ? "" : "=> ";
                    final String labelText = nameDisplay.referenceLabelText(object);
                    if (labelText != null) {
                        setWrappedText(forward + labelText);
                        // The syntax of object reference names contains "<" and ">"; make them safe for HTML tool tips.
                        final StringBuilder toolTipSB = new StringBuilder();
                        toolTipSB.append(value.toWord().toPadded0xHexString('0'));
                        toolTipSB.append(hubIsForwarded ? "<br> Forwarder to" : "<br>Pointer to ").append(htmlify(nameDisplay.referenceToolTipText(object)));
                        toolTipSB.append("<br>In ");
                        final MaxMemoryRegion memoryRegion = vm().state().findMemoryRegion(value().toWord().asAddress());
                        if (memoryRegion == null) {
                            toolTipSB.append(htmlify("<unknown memory region>"));
                        } else {
                            toolTipSB.append("\"").append(nameDisplay.longName(memoryRegion)).append("\"");
                        }
                        final String gcDescription = object.reference().gcDescription();
                        if (gcDescription != null) {
                            toolTipSB.append("<br>GC: " + gcDescription);
                        }
                        setWrappedToolTipHtmlText(toolTipSB.toString());
                        break;
                    }
                } catch (Throwable throwable) {
                    // If we don't catch this the views will not be updated at all.
                    object = null;
                    displayMode = DisplayMode.INVALID_OBJECT_REFERENCE;
                    setWrappedToolTipHtmlText("<b>" + throwable + "</b><br>See log for complete stack trace.");
                    throwable.printStackTrace(Trace.stream());
                    break;
                }
                displayMode = DisplayMode.OBJECT_REFERENCE;
                updateText();
                break;
            }
            case STACK_LOCATION: {
                setFont(wordDataFont);
                setForeground(style.wordStackLocationDataColor());
                setWrappedText(hexString);
                final String threadName = nameDisplay.longName(thread);
                final Address address = value().asWord().asAddress();
                final long offset = address.minus(stack.memoryRegion().start()).toLong();
                final StringBuilder ttBuilder = new StringBuilder();
                ttBuilder.append(value.toWord().to0xHexString());
                try {
                    final MaxStackFrame stackFrame = stack.findStackFrame(address);
                    String methodName = null;
                    if (stackFrame instanceof MaxStackFrame.Compiled) {
                        methodName = inspection().nameDisplay().veryShortName(stackFrame.compilation());
                    }
                    if (stackFrame == null) {
                        ttBuilder.append("<br>Points into stack for thread ").append(threadName);
                        ttBuilder.append("<br>").append(longToDecimalAndHex(offset)).append(" bytes from beginning");
                    } else {
                        ttBuilder.append("<br>Points at stack frame for thread ").append(threadName);
                        final String positionString = Integer.toString(stackFrame.position());
                        if (methodName == null) {
                            ttBuilder.append("<br>  position=").append(positionString);
                        } else {
                            ttBuilder.append("<br>  ").append(positionString).append(": ").append(methodName);
                        }
                    }
                } catch (NullPointerException enull) {
                    ttBuilder.append("<br>Points into stack for thread ").append(threadName);
                    ttBuilder.append("<br>").append(longToDecimalAndHex(offset)).append(" bytes from beginning");
                }
                setWrappedToolTipHtmlText(ttBuilder.toString());
                break;
            }
            case STACK_LOCATION_TEXT: {
                setFont(style.wordAlternateTextFont());
                setForeground(style.wordStackLocationDataColor());

                final String threadName = nameDisplay.longName(thread);
                final Address address = value().asWord().asAddress();
                final long offset = address.minus(stack.memoryRegion().start()).toLong();
                final String offsetString = offset >= 0 ? ("+" + offset) : Long.toString(offset);
                final MaxStackFrame stackFrame = stack.findStackFrame(address);
                String methodName = null;
                if (stackFrame instanceof MaxStackFrame.Compiled) {
                    methodName = inspection().nameDisplay().veryShortName(stackFrame.compilation());
                }

                final StringBuilder textBuilder = new StringBuilder();
                textBuilder.append(threadName).append(" ");

                final StringBuilder ttBuilder = new StringBuilder();
                ttBuilder.append(value.toWord().to0xHexString());

                if (stackFrame == null) {
                    textBuilder.append(offsetString);
                    ttBuilder.append("<br>Points into stack for thread ").append(threadName);
                    ttBuilder.append("<br>").append(longToDecimalAndHex(offset)).append(" bytes from beginning");
                } else {
                    ttBuilder.append("<br>Points into stack frame for thread ").append(threadName);
                    final String positionString = Integer.toString(stackFrame.position());
                    if (methodName == null) {
                        textBuilder.append(offsetString);
                        ttBuilder.append("<br>  position=").append(positionString);
                    } else {
                        textBuilder.append(positionString).append(": ").append(methodName);
                        ttBuilder.append("<br>  ").append(positionString).append(": ").append(methodName);
                    }
                }
                setWrappedText(textBuilder.toString());
                setWrappedToolTipHtmlText(ttBuilder.toString());
                break;
            }
            case THREAD_LOCALS_BLOCK_LOCATION: {
                setFont(wordDataFont);
                setForeground(style.wordThreadLocalsBlockLocationDataColor());
                setWrappedText(hexString);
                final String threadName = nameDisplay.longName(thread);
                final Address address = value().asWord().asAddress();
                final long offset = address.minus(thread.localsBlock().memoryRegion().start()).toLong();
                MaxThreadLocalVariable tlVariable = null;
                MaxThreadLocalsArea tlArea = threadLocalsBlock.findTLA(address);
                if (tlArea != null) {
                    tlVariable = tlArea.findThreadLocalVariable(address);
                }
                final StringBuilder ttBuilder = new StringBuilder();
                ttBuilder.append(value.toWord().to0xHexString());
                if (tlVariable == null) {
                    ttBuilder.append("<br>Points into thread locals area for thread ").append(threadName);
                    ttBuilder.append("<br>").append(longToDecimalAndHex(offset)).append(" bytes from beginning");
                } else {
                    ttBuilder.append("<br>Points at thread local variable ").append(tlVariable.variableName()).append(" for:");
                    ttBuilder.append("<br>  thread=").append(threadName);
                    ttBuilder.append("<br>  state=").append(tlVariable.safepointState().name());
                    ttBuilder.append("<br>  desc.=").append(tlVariable.entityDescription());
                    ttBuilder.append("<br>  value=").append(tlVariable.value().toString());
                }
                setWrappedToolTipHtmlText(ttBuilder.toString());
                break;
            }
            case THREAD_LOCALS_BLOCK_LOCATION_TEXT: {
                setFont(style.wordAlternateTextFont());
                setForeground(style.wordThreadLocalsBlockLocationDataColor());
                final String threadName = nameDisplay.longName(thread);
                final Address address = value().asWord().asAddress();
                final long offset = address.minus(thread.localsBlock().memoryRegion().start()).toLong();
                MaxThreadLocalVariable tlVariable = null;
                MaxThreadLocalsArea tlArea = threadLocalsBlock.findTLA(address);
                if (tlArea != null) {
                    tlVariable = tlArea.findThreadLocalVariable(address);
                }

                final StringBuilder textBuilder = new StringBuilder();
                textBuilder.append(threadName).append(" ");

                final StringBuilder ttBuilder = new StringBuilder();
                ttBuilder.append(value.toWord().to0xHexString());


                if (tlVariable == null) {
                    textBuilder.append(longToPlusMinusDecimal(offset));
                    ttBuilder.append("<br>Points into thread locals area for thread ").append(threadName);
                    ttBuilder.append("<br>").append(longToDecimalAndHex(offset)).append(" bytes from beginning");
                } else {
                    textBuilder.append(tlVariable.variableName());
                    ttBuilder.append("<br>Points at thread local variable ").append(tlVariable.variableName()).append(" for:");
                    ttBuilder.append("<br>  thread=").append(threadName);
                    ttBuilder.append("<br>  state=").append(tlVariable.safepointState().name());
                    ttBuilder.append("<br>  desc.=").append(tlVariable.entityDescription());
                    ttBuilder.append("<br>  value=").append(tlVariable.value().toString());
                }
                setWrappedText(textBuilder.toString());
                setWrappedToolTipHtmlText(ttBuilder.toString());
                break;
            }
            case UNCHECKED_REFERENCE: {
                setFont(wordDataFont);
                setForeground(style.wordUncheckedReferenceDataColor());
                setWrappedText(hexString);
                final StringBuilder toolTipSB = new StringBuilder();
                toolTipSB.append(value.toWord().toPadded0xHexString('0'));
                if (valueMode == ValueMode.LITERAL_REFERENCE) {
                    toolTipSB.append("<br>").append(htmlify("<unchecked>"));
                } else {
                    toolTipSB.append("<br>").append(htmlify("Unchecked Reference"));
                }
                setWrappedToolTipHtmlText(toolTipSB.toString());
                break;
            }
            case INVALID_OBJECT_REFERENCE: {
                setFont(wordDataFont);
                setForeground(style.wordInvalidObjectReferenceDataColor());
                setWrappedText(hexString);
                final StringBuilder toolTipSB = new StringBuilder();
                toolTipSB.append(value.toWord().toPadded0xHexString('0'));
                if (valueMode == ValueMode.LITERAL_REFERENCE) {
                    toolTipSB.append("<br>").append(htmlify("<invalid>"));
                } else {
                    toolTipSB.append("<br> invalid object reference");
                    final MaxEntityMemoryRegion<?> memoryRegion = vm().state().findMemoryRegion(value().asWord().asAddress());
                    if (memoryRegion == null) {
                        toolTipSB.append("<br> points into no known memory region");
                    } else {
                        toolTipSB.append("<br> points into region " + memoryRegion.regionName());
                    }
                }
                setWrappedToolTipHtmlText(toolTipSB.toString());
                break;
            }
            case CALL_ENTRY_POINT: {
                setFont(wordDataFont);
                setForeground(style.wordCallEntryPointColor());
                setWrappedText(hexString);
                if (compilation != null) {
                    final StringBuilder toolTip = new StringBuilder();
                    toolTip.append(value.toWord().to0xHexString());
                    if (taggedCodePointer != null) {
                        toolTip.append("<br>Decimal= " + Long.toString(value.toLong()));
                        toolTip.append("<br>POSSIBLY TAGGED encoding of " + taggedCodePointer.getAddress().to0xHexString());
                    }
                    toolTip.append("<br>Points to entry in compilation " + nameDisplay.longMethodCompilationID(compilation) + " for method");
                    toolTip.append("<br>" + htmlify(nameDisplay.longName(compilation)));
                    setWrappedToolTipHtmlText(toolTip.toString());
                }
                break;
            }
            case CALL_ENTRY_POINT_TEXT: {
                setFont(style.wordAlternateTextFont());
                setForeground(style.wordCallEntryPointColor());
                setWrappedText(nameDisplay.veryShortName(compilation));
                if (compilation != null) {
                    final StringBuilder toolTip = new StringBuilder();
                    toolTip.append(value.toWord().to0xHexString());
                    if (taggedCodePointer != null) {
                        toolTip.append("<br>Decimal= " + Long.toString(value.toLong()));
                        toolTip.append("<br>POSSIBLY TAGGED encoding of " + taggedCodePointer.getAddress().to0xHexString());
                    }
                    toolTip.append("<br>Points to entry in compilation " + nameDisplay.longMethodCompilationID(compilation) + " for method");
                    toolTip.append("<br>" + htmlify(nameDisplay.longName(compilation)));
                    setWrappedToolTipHtmlText(toolTip.toString());
                }
                break;
            }
            case NATIVE_FUNCTION: {
                setFont(wordDataFont);
                setForeground(style.wordCallEntryPointColor());
                setWrappedText(hexString);
                setWrappedToolTipHtmlText(value.toWord().to0xHexString() +
                                "<br>Points into native function:  " + nameDisplay.longName(nativeFunction));
                break;
            }
            case NATIVE_FUNCTION_TEXT: {
                setFont(style.wordAlternateTextFont());
                setForeground(style.wordCallEntryPointColor());
                setWrappedText(nameDisplay.shortName(nativeFunction));
                setWrappedToolTipHtmlText(value.toWord().to0xHexString() +
                                "<br>Points into native function:  " + nameDisplay.longName(nativeFunction));

                break;
            }
            case CLASS_ACTOR_ID: {
                setFont(wordDataFont);
                setForeground(null);
                setWrappedText(Long.toString(value.asWord().asAddress().toLong()));
                if (teleClassActor != null) {
                    setWrappedToolTipHtmlText(nameDisplay.referenceToolTipText(teleClassActor));
                } else {
                    setToolTipText("Class{???}");
                }
                break;
            }
            case CLASS_ACTOR: {
                setWrappedText(teleClassActor.classActor().simpleName());
                setWrappedToolTipHtmlText(nameDisplay.referenceToolTipText(teleClassActor));
                break;
            }
            case CALL_RETURN_POINT: {
                setFont(wordDataFont);
                setForeground(style.wordCallReturnPointColor());
                setWrappedText(hexString);
                if (compilation != null) {
                    final StringBuilder toolTip = new StringBuilder();
                    toolTip.append(value.toWord().to0xHexString());
                    final Address address = taggedCodePointer == null ? value.toWord().asAddress() : taggedCodePointer.getAddress();
                    if (taggedCodePointer != null) {
                        toolTip.append("<br>Decimal= " + Long.toString(value.toLong()));
                        toolTip.append("<br>POSSIBLY TAGGED encoding of " + address.to0xHexString());
                    }
                    final long position = address.minus(compilation.getCodeStart()).toLong();
                    toolTip.append("<br>Points into compilation " + nameDisplay.longMethodCompilationID(compilation) + " for method");
                    toolTip.append("<br>" + htmlify(nameDisplay.longName(compilation)));
                    toolTip.append("<br>" + longToDecimalAndHex(position) + "bytes from beginning");
                    setWrappedToolTipHtmlText(toolTip.toString());
                }
                break;
            }
            case CALL_RETURN_POINT_TEXT: {
                setFont(style.wordAlternateTextFont());
                setForeground(style.wordCallReturnPointColor());
                if (compilation != null) {
                    final Address address = taggedCodePointer == null ? value.toWord().asAddress() : taggedCodePointer.getAddress();
                    setWrappedText(nameDisplay.veryShortName(compilation, address));
                    final StringBuilder toolTip = new StringBuilder();
                    toolTip.append(value.toWord().to0xHexString());
                    if (taggedCodePointer != null) {
                        toolTip.append("<br>Decimal= " + Long.toString(value.toLong()));
                        toolTip.append("<br>POSSIBLY TAGGED encoding of " + address.to0xHexString());
                    }
                    final long position = address.minus(compilation.getCodeStart()).toLong();
                    toolTip.append("<br>Points into compilation " + nameDisplay.longMethodCompilationID(compilation) + " for method");
                    toolTip.append("<br>" + htmlify(nameDisplay.longName(compilation)));
                    toolTip.append("<br>" + longToDecimalAndHex(position) + "bytes from beginning");
                    setWrappedToolTipHtmlText(toolTip.toString());
                }
                break;
            }
            case UNCHECKED_CALL_POINT: {
                setFont(wordDataFont);
                setForeground(style.wordUncheckedCallPointColor());
                setWrappedText(hexString);
                setWrappedToolTipHtmlText("Unchecked call entry/return point");
                break;
            }
            case FLAGS: {
                setFont(style.wordFlagsFont());
                setForeground(null);
                setWrappedText(focus().thread().registers().stateRegisterValueToString(value.toLong()));
                setWrappedToolTipHtmlText("Flags 0x" + hexString);
                break;
            }
            case DECIMAL: {
                setFont(style.decimalDataFont());
                setForeground(null);
                setWrappedText(Integer.toString(value.toInt()));
                setWrappedToolTipHtmlText("0x" + hexString);
                break;
            }
            case SIZE: {
                setFont(style.sizeDataFont());
                setForeground(null);
                if (value.isZero()) {
                    setWrappedText("0");
                    setWrappedToolTipHtmlText("zero");
                } else {
                    final long longValue = value.toLong();
                    setWrappedText(Long.toString(longValue));
                    if (longValue == Long.MAX_VALUE) {
                        setWrappedToolTipHtmlText("MAX Size" + " [" + valueToDecimalAndHex(value) + "]");
                    } else {
                        setWrappedToolTipHtmlText(Longs.toUnitsString(longValue, false) + " [" + valueToDecimalAndHex(value) + "]");
                    }
                }
                break;
            }
            case FLOAT: {
                setFont(style.wordAlternateTextFont());
                setForeground(null);
                final String floatText = valueToFloatText(value);
                final String doubleText = valueToDoubleText(value);
                setWrappedText(floatText);
                setWrappedToolTipHtmlText("0x" + hexString + "<br>As float = " + floatText + "<br>As double = " + doubleText);
                break;
            }
            case DOUBLE: {
                setFont(style.wordAlternateTextFont());
                setForeground(null);
                final String floatText = valueToFloatText(value);
                final String doubleText = valueToDoubleText(value);
                setWrappedText(doubleText);
                setWrappedToolTipHtmlText("0x" + hexString + "<br>As float = " + floatText + "<br>As double = " + doubleText);
                break;
            }
            case UNAVAILABLE: {
                setFont(wordDataFont);
                setForeground(null);
                setWrappedText(nameDisplay.unavailableDataShortText());
                setWrappedToolTipHtmlText(nameDisplay.unavailableDataLongText());
                break;
            }
        }
        if (parent != null) {
            parent.repaint();
        }
    }

    /**
     * Sets the default font for displaying word data in this label, overriding
     * the default specified by the style mechanism.
     *
     * @param font a font do use in this label as the default font for word data
     * @see InspectorStyle#defaultWordDataFont()
     */
    public void setWordDataFont(Font font) {
        this.wordDataFont = font;
    }

    private InspectorAction getCycleDisplayTextAction() {
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

            case QUASI_OBJECT_REFERENCE: {
                alternateValueKind = DisplayMode.QUASI_OBJECT_REFERENCE_TEXT;
                break;
            }
            case QUASI_OBJECT_REFERENCE_TEXT: {
                alternateValueKind = DisplayMode.QUASI_OBJECT_REFERENCE;
                break;
            }

            case HUB_REFERENCE: {
                alternateValueKind = DisplayMode.HUB_REFERENCE_TEXT;
                break;
            }
            case HUB_REFERENCE_TEXT: {
                alternateValueKind = DisplayMode.HUB_REFERENCE;
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

            case THREAD_LOCALS_BLOCK_LOCATION: {
                alternateValueKind = DisplayMode.THREAD_LOCALS_BLOCK_LOCATION_TEXT;
                break;
            }
            case THREAD_LOCALS_BLOCK_LOCATION_TEXT: {
                alternateValueKind = DisplayMode.THREAD_LOCALS_BLOCK_LOCATION;
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

            case NATIVE_FUNCTION: {
                alternateValueKind = DisplayMode.NATIVE_FUNCTION_TEXT;
                break;
            }
            case NATIVE_FUNCTION_TEXT: {
                alternateValueKind = DisplayMode.NATIVE_FUNCTION;
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
            return new InspectorAction(inspection(), "Cycle alternate display text") {

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

    private InspectorAction getCloseAndViewObjectAction(Value value) {
        InspectorAction action = null;
        final Address valueAsAddress = value.toWord().asAddress();
        switch (displayMode) {
            case OBJECT_REFERENCE:
            case OBJECT_REFERENCE_TEXT:
            case HUB_REFERENCE:
            case HUB_REFERENCE_TEXT:
            case QUASI_OBJECT_REFERENCE:
            case QUASI_OBJECT_REFERENCE_TEXT:
            case UNCHECKED_REFERENCE:
                if (parent instanceof InspectorViewElement) {
                    final InspectorViewElement viewElement = (InspectorViewElement) parent;
                    final InspectorView oldView = viewElement.getView();
                    if (oldView instanceof ObjectView) {
                        MaxObject newObject = null;
                        try {
                            newObject = vm().objects().findAnyObjectAt(valueAsAddress);
                            if (newObject == null) {
                                newObject = vm().objects().findForwardedObjectAt(valueAsAddress);
                            }
                        } catch (MaxVMBusyException e) {
                        }
                        if (newObject != null) {
                            final MaxObject finalObject = newObject;
                            action = new InspectorAction(inspection(), null) {

                                @Override
                                protected void procedure() {
                                    final ObjectView newView = views().objects().makeView(finalObject);
                                    if (newView != null) {
                                        if (newView == oldView) {
                                            newView.highlight();
                                        } else {
                                            oldView.dispose();
                                        }
                                    }
                                }
                            };
                        }
                    }
                }
                break;
            default:
                action = null;
        }
        return action;
    }

    private InspectorAction getInspectValueAction(Value value) {
        InspectorAction action = null;
        final Address valueAsAddress = value.toWord().asAddress();
        switch (displayMode) {
            case OBJECT_REFERENCE:
            case OBJECT_REFERENCE_TEXT:
            case QUASI_OBJECT_REFERENCE:
            case QUASI_OBJECT_REFERENCE_TEXT:
            case UNCHECKED_REFERENCE: {
                MaxObject object = null;
                try {
                    object = vm().objects().findAnyObjectAt(valueAsAddress);
                } catch (MaxVMBusyException e) {
                }
                if (object != null) {
                    action = views().objects().makeViewAction(object, null);
                }
                break;
            }
            case HUB_REFERENCE:
            case HUB_REFERENCE_TEXT:
                MaxObject object = null;
                try {
                    object = vm().objects().findAnyObjectAt(valueAsAddress);
                    if (object == null) {
                        object = vm().objects().findForwardedObjectAt(valueAsAddress);
                    }
                } catch (MaxVMBusyException e) {
                }
                if (object != null) {
                    action = views().objects().makeViewAction(object, null);
                }
                break;
            case CALL_ENTRY_POINT:
            case CALL_ENTRY_POINT_TEXT:
            case CALL_RETURN_POINT:
            case CALL_RETURN_POINT_TEXT:
            case NATIVE_FUNCTION:
            case NATIVE_FUNCTION_TEXT:
            case UNCHECKED_CALL_POINT: {
                try {
                    final Address address = taggedCodePointer == null ? valueAsAddress : taggedCodePointer.getAddress();
                    final MaxCodeLocation codeLocation = vm().codeLocations().createMachineCodeLocation(address, "code address from WordValueLabel");
                    action = new InspectorAction(inspection(), "View Code at address") {
                        @Override
                        public void procedure() {
                            focus().setCodeLocation(codeLocation, true);
                        }
                    };
                } catch (InvalidCodeAddressException e) {
                }
                break;
            }
            case CLASS_ACTOR_ID:
            case CLASS_ACTOR: {
                final TeleClassActor teleClassActor = vm().classes().findTeleClassActor(value.asWord().asAddress().toInt());
                if (teleClassActor != null) {
                    action = views().objects().makeViewAction(teleClassActor, "View ClassActor");
                }
                break;
            }
            case STACK_LOCATION:
            case STACK_LOCATION_TEXT:
            case THREAD_LOCALS_BLOCK_LOCATION:
            case THREAD_LOCALS_BLOCK_LOCATION_TEXT:
            case WORD:
            case NULL_WORD:
            case INVALID_OBJECT_REFERENCE:
            case FLAGS:
            case DECIMAL:
            case FLOAT:
            case  DOUBLE:
            case UNCHECKED_WORD:
            case INVALID:
            case UNAVAILABLE: {
                // no action
                break;
            }
        }
        return action;
    }

    private InspectorAction getViewMemoryAction(Value value) {
        InspectorAction action = null;
        if (value != VoidValue.VOID) {
            final Address address = value.toWord().asAddress();
            switch (displayMode) {
                case INVALID_OBJECT_REFERENCE:
                case UNCHECKED_REFERENCE:
                case STACK_LOCATION:
                case STACK_LOCATION_TEXT:
                case THREAD_LOCALS_BLOCK_LOCATION:
                case THREAD_LOCALS_BLOCK_LOCATION_TEXT:
                case CALL_ENTRY_POINT:
                case CALL_ENTRY_POINT_TEXT:
                case CALL_RETURN_POINT:
                case CALL_RETURN_POINT_TEXT:
                case NATIVE_FUNCTION:
                case NATIVE_FUNCTION_TEXT:
                case UNCHECKED_CALL_POINT: {
                    action = views().memory().makeViewAction(address, null);
                    break;
                }
                case OBJECT_REFERENCE:
                case OBJECT_REFERENCE_TEXT:
                case QUASI_OBJECT_REFERENCE:
                case QUASI_OBJECT_REFERENCE_TEXT:
                case HUB_REFERENCE:
                case HUB_REFERENCE_TEXT: {
                    if (object != null) {
                        action = views().memory().makeViewAction(object, "View memory for Object (Shift-Left-Button)");
                    } else {
                        action = views().memory().makeViewAction(address, null);
                    }
                    break;
                }
                case WORD:
                case NULL_WORD:
                case CLASS_ACTOR_ID:
                case CLASS_ACTOR:
                case FLAGS:
                case DECIMAL:
                case FLOAT:
                case DOUBLE:
                case UNCHECKED_WORD:
                case INVALID: {
                    if (address.isNotZero()) {
                        action = views().memory().makeViewAction(address, null);
                    }
                    break;
                }
                case UNAVAILABLE:
                    break;
            }
        }
        return action;
    }

    private InspectorAction getShowMemoryRegionAction(Value value) {
        InspectorAction action = null;
        if (value != VoidValue.VOID) {
            final Address address = value.toWord().asAddress();
            final MaxMemoryRegion memoryRegion = vm().state().findMemoryRegion(address);
            if (memoryRegion != null) {
                action = actions().selectMemoryRegion(memoryRegion);
            }
        }
        return action;
    }

    private InspectorAction getShowHeapMarkAction(Value value) {
        InspectorAction action = null;
        final MaxMarkBitmap markBitMap = vm().heap().markBitMap();
        if (value != VoidValue.VOID && markBitMap != null) {
            final Address address = value.toWord().asAddress();
            if (markBitMap.isCovered(address)) {
                final int bitIndex = markBitMap.getBitIndexOf(address);
                final MarkColor markColor = markBitMap.getMarkColor(bitIndex);

                final StringBuilder sb = new StringBuilder();
                sb.append("Show heap mark bit(");
                sb.append(bitIndex);
                sb.append(")=");
                sb.append(markBitMap.isBitSet(bitIndex) ? "1" : "0");
                sb.append(", color=");
                sb.append(markColor);
                action = new InspectorAction(inspection(), sb.toString()) {

                    @Override
                    protected void procedure() {
                        focus().setMarkBitIndex(bitIndex);
                        focus().setAddress(address);
                    }

                };
            }
        }
        return action;
    }


    @Override
    public Transferable getTransferable() {
        Transferable transferable = null;
        if (value() != VoidValue.VOID) {
            final Address address = value().toWord().asAddress();
            switch (displayMode) {
                case INVALID_OBJECT_REFERENCE:
                case UNCHECKED_REFERENCE:
                case STACK_LOCATION:
                case STACK_LOCATION_TEXT:
                case THREAD_LOCALS_BLOCK_LOCATION:
                case THREAD_LOCALS_BLOCK_LOCATION_TEXT:
                case CALL_ENTRY_POINT:
                case CALL_RETURN_POINT:
                case UNCHECKED_CALL_POINT:
                case WORD:
                case NULL_WORD:
                case CLASS_ACTOR_ID:
                case CLASS_ACTOR:
                case FLAGS:
                case DECIMAL:
                case FLOAT:
                case DOUBLE:
                case OBJECT_REFERENCE:
                case QUASI_OBJECT_REFERENCE:
                case UNCHECKED_WORD:
                case INVALID: {
                    if (vm().state().findMemoryRegion(address) != null) {
                        transferable = new InspectorTransferable.AddressTransferable(inspection(), address);
                    }
                    break;
                }
                case HUB_REFERENCE:
                case HUB_REFERENCE_TEXT:
                    // TODO (mlvdv) what to transfer in this case?
                    break;
                case OBJECT_REFERENCE_TEXT:
                case QUASI_OBJECT_REFERENCE_TEXT: {
                    if (object != null) {
                        transferable = new InspectorTransferable.TeleObjectTransferable(inspection(), object);
                    } else {
                        transferable = new InspectorTransferable.AddressTransferable(inspection(), address);
                    }
                    break;
                }
                case CALL_ENTRY_POINT_TEXT:
                case CALL_RETURN_POINT_TEXT: {
                    if (compilation != null) {
                        transferable = new InspectorTransferable.TeleObjectTransferable(inspection(), compilation.representation());
                    } else {
                        transferable = new InspectorTransferable.AddressTransferable(inspection(), address);
                    }
                    break;
                }
                case UNAVAILABLE:
                    break;
            }
        }
        return transferable;
    }

    private final class WordValueMenuItems extends InspectorPopupMenuItems {

        private final class MenuViewObjectAction extends InspectorAction {

            private final InspectorAction inspectAction;

            private MenuViewObjectAction(Value value) {
                super(inspection(), "View Object (Left-Button)");
                inspectAction = getInspectValueAction(value);
                setEnabled(inspectAction != null);
            }

            @Override
            public void procedure() {
                inspectAction.perform();
            }
        }

        private final class MenuCloseAndViewObjectAction extends InspectorAction {

            private InspectorAction inspectAction;

            private MenuCloseAndViewObjectAction(Value value) {
                super(inspection(), "Close and View Object (Meta-Left-Button)");
                inspectAction = getCloseAndViewObjectAction(value);
                setEnabled(inspectAction != null);
            }

            @Override
            public void procedure() {
                inspectAction.perform();
            }
        }

        private final class MenuCycleDisplayAction extends InspectorAction {

            private final InspectorAction cycleAction;

            private MenuCycleDisplayAction() {
                super(inspection(), "Cycle display (Middle-Button)");
                cycleAction = getCycleDisplayTextAction();
                setEnabled(cycleAction != null);
            }

            @Override
            public void procedure() {
                cycleAction.perform();
            }
        }

        private final class MenuViewMemoryAction extends InspectorAction {

            private final InspectorAction viewMemoryAction;

            private MenuViewMemoryAction(Value value) {
                super(inspection(), "View memory");
                viewMemoryAction = getViewMemoryAction(value);
                if (viewMemoryAction == null) {
                    setEnabled(false);
                } else {
                    setName(viewMemoryAction.name());
                    setEnabled(true);
                }
            }

            @Override
            public void procedure() {
                viewMemoryAction.perform();
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

        private final class MenuShowHeapMarkAction extends InspectorAction {

            private final InspectorAction showHeapMarkAction;

            private MenuShowHeapMarkAction(Value value) {
                super(inspection(), "Show heap bitmap mark for location");
                showHeapMarkAction = getShowHeapMarkAction(value);
                if (showHeapMarkAction == null) {
                    setEnabled(false);
                } else {
                    setEnabled(true);
                    setName(showHeapMarkAction.name());
                }
            }

            @Override
            public void procedure() {
                showHeapMarkAction.perform();
            }
        }

        public WordValueMenuItems(Inspection inspection, Value value) {
            add(actions().copyValue(value, "Copy value to clipboard"));
            add(new MenuViewObjectAction(value));
            add(new MenuViewMemoryAction(value));
            add(new MenuCloseAndViewObjectAction(value));
            add(new MenuCycleDisplayAction());
            add(new MenuShowHeapMarkAction(value));
            add(new MenuShowMemoryRegionAction(value));
        }
    }

}
