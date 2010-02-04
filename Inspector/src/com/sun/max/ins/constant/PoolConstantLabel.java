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
package com.sun.max.ins.constant;

import java.awt.event.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.method.*;
import com.sun.max.tele.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.classfile.constant.FieldRefConstant.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * A label for displaying a {@link PoolConstant} in the VM that is a reference.
 * For literal constants, the labels rely on the local counterpart to the remote one.
 * For resolvable constants, an attempt is made to read the remote version in order
 * to determine if resolved or not.
 *
 * @author Michael Van De Vanter
 */
public abstract class PoolConstantLabel extends InspectorLabel {

  // TODO (mlvdv)  PoolConstantLabel, only one mode implemented so far; refactor when more modes added
    public enum Mode {
        JAVAP,
        TERSE
    }

    private Mode mode;

    protected final Mode mode() {
        return mode;
    }

    private final int index;

    /**
     * Surrogate for the {@link ConstantPool} in the VM.
     * Might be null, and might be unreachable.
     */
    private final TeleConstantPool teleConstantPool;

    private TelePoolConstant telePoolConstant;

    protected final TelePoolConstant telePoolConstant() {
        return telePoolConstant;
    }

    private final ConstantPool localConstantPool;

    protected final ConstantPool localConstantPool() {
        return localConstantPool;
    }

    private final PoolConstant localPoolConstant;

    protected final PoolConstant localPoolConstant() {
        return localPoolConstant;
    }

    protected final boolean isResolved() {
        return telePoolConstant != null && telePoolConstant.isResolved();
    }

    public static PoolConstantLabel make(Inspection inspection, int index, ConstantPool localConstantPool, TeleConstantPool teleConstantPool, Mode mode) {
        PoolConstantLabel poolConstantLabel;
        final PoolConstant poolConstant = localConstantPool.at(index);
        if (poolConstant == null) {
            poolConstantLabel = new PoolConstantLabel.Null(inspection, index, localConstantPool, teleConstantPool, mode);
        } else {
            switch (poolConstant.tag()) {
                case CLASS:
                    poolConstantLabel = new PoolConstantLabel.Class(inspection, index, localConstantPool, teleConstantPool, mode);
                    break;
                case FIELD_REF:
                    poolConstantLabel = new PoolConstantLabel.Field(inspection, index, localConstantPool, teleConstantPool, mode);
                    break;
                case METHOD_REF:
                    poolConstantLabel = new PoolConstantLabel.ClassMethod(inspection, index, localConstantPool, teleConstantPool, mode);
                    break;
                case INTERFACE_METHOD_REF:
                    poolConstantLabel = new PoolConstantLabel.InterfaceMethod(inspection, index, localConstantPool, teleConstantPool, mode);
                    break;
                case STRING:
                    poolConstantLabel = new PoolConstantLabel.StringConstant(inspection, index, localConstantPool, teleConstantPool, mode);
                    break;
                case UTF8:
                    poolConstantLabel = new PoolConstantLabel.Utf8Constant(inspection, index, localConstantPool, teleConstantPool, mode);
                    break;
                default:
                    poolConstantLabel = new PoolConstantLabel.Other(inspection, index, localConstantPool, teleConstantPool, mode);
            }
        }
        return poolConstantLabel;
    }

    protected PoolConstantLabel(Inspection inspection, int index, ConstantPool localConstantPool, TeleConstantPool teleConstantPool, Mode mode) {
        super(inspection, null);
        this.index = index;
        this.localConstantPool = localConstantPool;
        this.localPoolConstant = localConstantPool.at(index);
        this.teleConstantPool = teleConstantPool;
        this.mode = mode;
        addMouseListener(new InspectorMouseClickAdapter(inspection) {
            @Override
            public void procedure(final MouseEvent mouseEvent) {
                switch (Inspection.mouseButtonWithModifiers(mouseEvent)) {
                    case MouseEvent.BUTTON1:
                        handleLeftButtonEvent();
                        break;
                    case MouseEvent.BUTTON3: {
                        createPopupMenu().show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }
        });
        refresh(true);
    }

    protected abstract void updateText();

    private MaxVMState lastRefreshedState = null;

    public void refresh(boolean force) {
        if (maxVMState().newerThan(lastRefreshedState) || force) {
            lastRefreshedState = maxVMState();
            if (teleConstantPool != null) {
                try {
                    telePoolConstant = teleConstantPool.readTelePoolConstant(index);
                } catch (DataIOError dataIOError) {
                    telePoolConstant = null;
                } catch (InvalidReferenceException invalidReferenceException) {
                    telePoolConstant = null;
                }
                updateText();
            }
        }
    }

    public void redisplay() {
        updateText();
    }

    protected String prefix = "";

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        updateText();
    }

    private String toolTipPrefix = "";

    public void setToolTipPrefix(String toolTipPrefix) {
        this.toolTipPrefix = toolTipPrefix;
        updateText();
    }

    protected final void setJavapText(String kind, String name) {
        setText(prefix + "#" + Integer.toString(index) + "; //" + kind + " " + name);
    }

    protected final void setJavapToolTipText(String kind, String name) {
        setToolTipText(toolTipPrefix + "#" + Integer.toString(index) + "; //" + kind + " " + name);
    }

    protected final void setJavapResolvableToolTipText(String kind, String name) {
        final String resolution = telePoolConstant == null ? inspection().nameDisplay().unavailableDataLongText() :
            (isResolved() ? "Resolved" : "Unresolved");
        setToolTipText(toolTipPrefix + "#" + Integer.toString(index) + "; //" + kind + " " + name + " (" + resolution + ")");
    }

    private InspectorPopupMenu createPopupMenu() {
        final InspectorPopupMenu menu = new InspectorPopupMenu();
        if (telePoolConstant != null) {
            menu.add(inspection().actions().copyWord(telePoolConstant.origin(), "Copy PoolConstant address toclipboard"));
            menu.add(inspection().actions().inspectObjectMemoryWords(telePoolConstant, "Inspect PoolConstant memory words"));
            menu.add(inspection().actions().inspectObject(telePoolConstant, "Inspect PoolConstant #" + Integer.toString(index)));
            menu.add(inspection().actions().inspectObject(teleConstantPool, "Inspect ConstantPool"));
            specializeMenu(menu);
        }
        return menu;
    }

    /**
     * Opportunity for subclasses to add additional menu items, depending on the type and state of the constant.
     */
    protected void specializeMenu(InspectorPopupMenu menu) {
    }

    /**
     * Opportunity for subclasses to take an action on left-click.
     */
    protected void handleLeftButtonEvent() {
    }

    private static final class Class extends PoolConstantLabel {

        private Class(Inspection inspection, int index, ConstantPool localConstantPool, TeleConstantPool teleConstantPool, Mode mode) {
            super(inspection, index, localConstantPool, teleConstantPool, mode);
            redisplay();
        }

        @Override
        protected void updateText() {
            final ClassConstant classConstant = (ClassConstant) localPoolConstant();
            final TypeDescriptor typeDescriptor = classConstant.typeDescriptor();
            switch (mode()) {
                case JAVAP:
                    setJavapText("Class", typeDescriptor.toJavaString(false));
                    break;
                case TERSE:
                    setText(prefix + typeDescriptor.toJavaString(false));
                    break;
                default:
                    FatalError.unimplemented();
            }
            setJavapResolvableToolTipText("Class", typeDescriptor.toString());
            if (isResolved()) {
                setForeground(null);
            } else {
                setForeground(style().javaUnresolvedNameColor());
            }
        }

        @Override
        protected void specializeMenu(InspectorPopupMenu menu) {
            if (isResolved()) {
                menu.addSeparator();
                final TeleClassConstant.Resolved teleResolvedClassConstant = (TeleClassConstant.Resolved) telePoolConstant();
                final TeleClassActor teleClassActor = teleResolvedClassConstant.getTeleClassActor();
                menu.add(inspection().actions().inspectObject(teleClassActor, "Inspect ClassActor"));
            }
        }
    }

    private static final class Field extends PoolConstantLabel {

        private Field(Inspection inspection, int index, ConstantPool localConstantPool, TeleConstantPool teleConstantPool, Mode mode) {
            super(inspection, index, localConstantPool, teleConstantPool, mode);
            redisplay();
        }

        @Override
        public void updateText() {
            final FieldRefConstant fieldRefConstant = (FieldRefConstant) localPoolConstant();
            final FieldRefKey fieldRefKey = fieldRefConstant.key(localConstantPool());
            final TypeDescriptor holder = fieldRefKey.holder();
            final String holderName = holder.toJavaString(false);
            final String fieldName = fieldRefKey.name().toString();
            switch (mode()) {
                case JAVAP:
                    setJavapText("Field",  fieldName);
                    break;
                case TERSE:
                    setText(prefix + fieldName);
                    break;
                default:
                    FatalError.unimplemented();
            }
            setJavapResolvableToolTipText("Field", holderName + "." + fieldName + ":" + fieldRefConstant.type(localConstantPool()).toString());
            if (isResolved()) {
                setForeground(null);
            } else {
                setForeground(style().javaUnresolvedNameColor());
            }
        }

        @Override
        protected void specializeMenu(InspectorPopupMenu menu) {
            if (isResolved()) {
                menu.addSeparator();
                final TeleFieldRefConstant.Resolved teleResolvedFieldRefConstant = (TeleFieldRefConstant.Resolved) telePoolConstant();
                final TeleFieldActor teleFieldActor = teleResolvedFieldRefConstant.getTeleFieldActor();
                menu.add(inspection().actions().inspectObject(teleFieldActor, "Inspect FieldActor"));
            }
        }

    }

    private static final class ClassMethod extends PoolConstantLabel {

        /**
         * Assigned when resolved.
         */
        private TeleClassMethodActor teleClassMethodActor;

        private ClassMethod(Inspection inspection, int index, ConstantPool localConstantPool, TeleConstantPool teleConstantPool, Mode mode) {
            super(inspection, index, localConstantPool, teleConstantPool, mode);
            redisplay();
        }

        private void checkResolved() {
            teleClassMethodActor = null;
            if (isResolved()) {
                final TeleClassMethodRefConstant.Resolved teleResolvedClassMethodRefConstant = (TeleClassMethodRefConstant.Resolved) telePoolConstant();
                try {
                    teleClassMethodActor = teleResolvedClassMethodRefConstant.getTeleClassMethodActor();
                } catch (DataIOError dataIOError) {
                }
            }
        }

        @Override
        public void updateText() {
            checkResolved();
            final MethodRefConstant methodRefConstant = (MethodRefConstant) localPoolConstant();
            final String methodName = methodRefConstant.name(localConstantPool()).toString();
            final String holderName = methodRefConstant.holder(localConstantPool()).toJavaString(false);
            switch (mode()) {
                case JAVAP:
                    setJavapText("Method",  methodName + "()");
                    break;
                case TERSE:
                    setText(prefix + methodName + "()");
                    break;
                default:
                    FatalError.unimplemented();
            }
            setJavapResolvableToolTipText("ClassMethod", holderName + "." + methodName + ":" + methodRefConstant.descriptor(localConstantPool()).toString());
            if (teleClassMethodActor == null) {
                setForeground(style().javaUnresolvedNameColor());
            } else {
                if (teleClassMethodActor.hasCodeAttribute()) {
                    setForeground(style().bytecodeMethodEntryColor());
                } else {
                    setForeground(null);
                }
            }
        }

        @Override
        protected void specializeMenu(InspectorPopupMenu menu) {
            checkResolved();
            if (teleClassMethodActor != null) {
                menu.addSeparator();
                final ClassMethodMenuItems classMethodMenuItems = new ClassMethodMenuItems(inspection(), teleClassMethodActor);
                classMethodMenuItems.addTo(menu);
            }
        }

        @Override
        protected void handleLeftButtonEvent() {
            checkResolved();
            if (teleClassMethodActor != null && teleClassMethodActor.hasCodeAttribute()) {
                final TeleCodeLocation teleCodeLocation = maxVM().createCodeLocation(teleClassMethodActor, 0);
                inspection().focus().setCodeLocation(teleCodeLocation, false);
            }
        }
    }

    private static final class InterfaceMethod extends PoolConstantLabel {

        private InterfaceMethod(Inspection inspection, int index, ConstantPool localConstantPool, TeleConstantPool teleConstantPool, Mode mode) {
            super(inspection, index, localConstantPool, teleConstantPool, mode);
            redisplay();
        }

        @Override
        public void updateText() {
            final MethodRefConstant methodRefConstant = (MethodRefConstant) localPoolConstant();
            final String methodName = methodRefConstant.name(localConstantPool()).toString();
            final String holderName = methodRefConstant.holder(localConstantPool()).toJavaString(false);
            switch (mode()) {
                case JAVAP:
                    setJavapText("Method",  methodName + "()");
                    break;
                case TERSE:
                    setText(prefix + methodName + "()");
                    break;
                default:
                    FatalError.unimplemented();
            }
            setJavapResolvableToolTipText("InterfaceMethod", holderName + "." + methodName + ":" + methodRefConstant.descriptor(localConstantPool()).toString());
            if (isResolved()) {
                setForeground(null);
            } else {
                setForeground(style().javaUnresolvedNameColor());
            }
        }

        @Override
        protected void specializeMenu(InspectorPopupMenu menu) {
            if (isResolved()) {
                menu.addSeparator();
                final TeleInterfaceMethodRefConstant.Resolved teleResolvedInterfaceMethodRefConstant = (TeleInterfaceMethodRefConstant.Resolved) telePoolConstant();
                final TeleInterfaceMethodActor teleInterfaceMethodActor = teleResolvedInterfaceMethodRefConstant.getTeleInterfaceMethodActor();
                menu.add(inspection().actions().inspectObject(teleInterfaceMethodActor, "Inspect InterfaceMethodActor"));
            }
        }

    }

    private static final class StringConstant extends PoolConstantLabel {

        private StringConstant(Inspection inspection, int index, ConstantPool localConstantPool, TeleConstantPool teleConstantPool, Mode mode) {
            super(inspection, index, localConstantPool, teleConstantPool, mode);
            redisplay();
        }

        @Override
        public void updateText() {
            final String fullText = localPoolConstant().valueString(localConstantPool());
            final String shortText =  (fullText.length() > style().maxBytecodeOperandDisplayLength() + 2) ?
                            fullText.substring(0, style().maxBytecodeOperandDisplayLength()) + "...\"" : fullText;
            switch (mode()) {
                case JAVAP:
                    setJavapText("", shortText);
                    break;
                case TERSE:
                    setText(prefix + shortText);
                    break;
                default:
                    FatalError.unimplemented();
            }
            setJavapToolTipText("String", fullText);
        }
    }

    private static final class Utf8Constant extends PoolConstantLabel {

        private Utf8Constant(Inspection inspection, int index, ConstantPool localConstantPool, TeleConstantPool teleConstantPool, Mode mode) {
            super(inspection, index, localConstantPool, teleConstantPool, mode);
            redisplay();
        }

        @Override
        public void updateText() {
            final String fullText = localPoolConstant().valueString(localConstantPool());
            final String shortText =  (fullText.length() > style().maxBytecodeOperandDisplayLength() + 2) ?
                            fullText.substring(0, style().maxBytecodeOperandDisplayLength()) + "...\"" : fullText;
            switch (mode()) {
                case JAVAP:
                    setJavapText("", shortText);
                    break;
                case TERSE:
                    setText(prefix + shortText);
                    break;
                default:
                    FatalError.unimplemented();
            }
            setJavapToolTipText("Utf8", fullText);
        }

    }

    private static final class Other extends PoolConstantLabel {

        private Other(Inspection inspection, int index, ConstantPool localConstantPool, TeleConstantPool teleConstantPool, Mode mode) {
            super(inspection, index, localConstantPool, teleConstantPool, mode);
            redisplay();
        }

        @Override
        public void updateText() {
            final String text = localPoolConstant().valueString(localConstantPool());
            switch (mode()) {
                case JAVAP:
                    setText(prefix + text);
                    break;
                case TERSE:
                    setText(prefix + text);
                    break;
                default:
                    FatalError.unimplemented();
            }
            setJavapToolTipText("", text);
        }

    }

    private static final class Null extends PoolConstantLabel {

        private Null(Inspection inspection, int index, ConstantPool localConstantPool, TeleConstantPool teleConstantPool, Mode mode) {
            super(inspection, index, localConstantPool, teleConstantPool, mode);
            redisplay();
        }

        @Override
        public void updateText() {
            final String text = "Error: null";
            setJavapToolTipText("", text);
            setForeground(style().defaultErrorTextColor());
            setBackground(style().defaultErrorTextBackgroundColor());
        }

    }

}
