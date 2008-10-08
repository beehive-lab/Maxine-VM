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
import com.sun.max.program.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.classfile.constant.ConstantPool.*;
import com.sun.max.vm.classfile.constant.FieldRefConstant.*;
import com.sun.max.vm.type.*;


/**
 * A label for displaying a {@link PoolConstant} in the {@link TeleVM} that is a reference.
 *
 * @author Michael Van De Vanter
 */
public abstract class PoolConstantLabel extends InspectorLabel {

  // TODO (mlvdv)  PoolConstantLabel, only one mode implemented so far; refactor when more modes added
    public enum Mode {
        JAVAP,
        TERSE
    }

    private Mode _mode;

    protected final Mode mode() {
        return _mode;
    }

    private final int _index;

    private final TeleConstantPool _teleConstantPool;

    private TelePoolConstant _telePoolConstant;

    protected final TelePoolConstant telePoolConstant() {
        return _telePoolConstant;
    }

    private final ConstantPool _localConstantPool;

    protected final ConstantPool localConstantPool() {
        return _localConstantPool;
    }

    private final PoolConstant _localPoolConstant;

    protected final PoolConstant localPoolConstant() {
        return _localPoolConstant;
    }

    private long _epoch = -1;

    public static PoolConstantLabel make(Inspection inspection, int index, TeleConstantPool teleConstantPool, Mode mode) {
        final Tag tag = teleConstantPool.getTeleHolder().classActor().constantPool().at(index).tag();
        PoolConstantLabel poolConstantLabel;
        switch (tag) {
            case CLASS:
                poolConstantLabel = new PoolConstantLabel.Class(inspection, index, teleConstantPool, mode);
                break;
            case FIELD_REF:
                poolConstantLabel = new PoolConstantLabel.Field(inspection, index, teleConstantPool, mode);
                break;
            case METHOD_REF:
                poolConstantLabel = new PoolConstantLabel.ClassMethod(inspection, index, teleConstantPool, mode);
                break;
            case INTERFACE_METHOD_REF:
                poolConstantLabel = new PoolConstantLabel.InterfaceMethod(inspection, index, teleConstantPool, mode);
                break;
            case STRING:
                poolConstantLabel = new PoolConstantLabel.StringConstant(inspection, index, teleConstantPool, mode);
                break;
            case UTF8:
                poolConstantLabel = new PoolConstantLabel.Utf8Constant(inspection, index, teleConstantPool, mode);
                break;
            default:
                poolConstantLabel = new PoolConstantLabel.Other(inspection, index, teleConstantPool, mode);
                break;
        }
        return poolConstantLabel;
    }

    protected PoolConstantLabel(Inspection inspection, int index, TeleConstantPool teleConstantPool, Mode mode) {
        super(inspection, null);
        _index = index;
        _localConstantPool = teleConstantPool.getTeleHolder().classActor().constantPool();
        _localPoolConstant = _localConstantPool.at(index);
        _teleConstantPool = teleConstantPool;
        _mode = mode;
        addMouseListener(new InspectorMouseClickAdapter(inspection) {
            @Override
            public void procedure(final MouseEvent mouseEvent) {
                switch (MaxineInspector.mouseButtonWithModifiers(mouseEvent)) {
                    case MouseEvent.BUTTON1:
                        handleLeftButtonEvent();
                        break;
                    case MouseEvent.BUTTON3: {
                        showMenu(mouseEvent);
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }
        });
        refresh(teleVM().teleProcess().epoch());
    }

    protected abstract void updateText();

    public void refresh(long epoch) {
        if (epoch > _epoch) {
            if (_telePoolConstant == null || !_telePoolConstant.isResolved()) {
                _telePoolConstant = _teleConstantPool.readTelePoolConstant(_index);
                updateText();
            }
            _epoch = epoch;
        }
    }

    public void redisplay() {
        updateText();
    }

    protected final void setJavapText(String kind, String name) {
        setText("#" + Integer.toString(_index) + "; //" + kind + " " + name);
    }

    protected final void setJavapToolTipText(String kind, String name) {
        setToolTipText("#" + Integer.toString(_index) + "; //" + kind + " " + name);
    }

    protected final void setJavapResolvableToolTipText(String kind, String name) {
        setToolTipText("#" + Integer.toString(_index) + "; //" + kind + " " + name
                        + " (" + (_telePoolConstant.isResolved() ? "Resolved" : "Unresolved") + ")");
    }

    protected void showMenu(MouseEvent mouseEvent) {
        final InspectorMenu menu = new InspectorMenu();
        menu.add(inspection().inspectionMenus().getCopyWordAction(_telePoolConstant.getCurrentOrigin(), "Copy PoolConstant address toclipboard"));
        menu.add(inspection().inspectionMenus().getInspectMemoryAction(_telePoolConstant, "Inspect PoolConstant memory"));
        menu.add(inspection().inspectionMenus().getInspectMemoryWordsAction(_telePoolConstant, "Inspect PoolConstant memory words"));
        menu.add(inspection().inspectionMenus().getInspectObjectAction(_telePoolConstant, "Inspect PoolConstant #" + Integer.toString(_index)));
        menu.add(inspection().inspectionMenus().getInspectObjectAction(_teleConstantPool, "Inspect ConstantPool"));
        specializeMenu(menu);
        menu.popupMenu().show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
    }

    /**
     * Opportunity for subclasses to add additional menu items, depending on the type and state of the constant.
     */
    protected void specializeMenu(InspectorMenu menu) {
    }

    /**
     * Opportunity for subclasses to take an action on left-click.
     */
    protected void handleLeftButtonEvent() {
    }

    private static final class Class extends PoolConstantLabel {

        private Class(Inspection inspection, int index, TeleConstantPool teleConstantPool, Mode mode) {
            super(inspection, index, teleConstantPool, mode);
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
                    setText(typeDescriptor.toJavaString(false));
                    break;
                default:
                    Problem.unimplemented();
            }
            setJavapResolvableToolTipText("Class", typeDescriptor.toString());
            if (telePoolConstant().isResolved()) {
                setForeground(style().bytecodeColor());
            } else {
                setForeground(style().javaUnresolvedNameColor());
            }
        }

        @Override
        protected void specializeMenu(InspectorMenu menu) {
            if (telePoolConstant().isResolved()) {
                menu.addSeparator();
                final TeleClassConstant.Resolved teleResolvedClassConstant = (TeleClassConstant.Resolved) telePoolConstant();
                final TeleClassActor teleClassActor = teleResolvedClassConstant.getTeleClassActor();
                menu.add(inspection().inspectionMenus().getInspectObjectAction(teleClassActor, "Inspect ClassActor"));
            }
        }
    }

    private static final class Field extends PoolConstantLabel {

        private Field(Inspection inspection, int index, TeleConstantPool teleConstantPool, Mode mode) {
            super(inspection, index, teleConstantPool, mode);
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
                    setText(fieldName);
                    break;
                default:
                    Problem.unimplemented();
            }
            setJavapResolvableToolTipText("Field", holderName + "." + fieldName + ":" + fieldRefConstant.type(localConstantPool()).toString());
            if (telePoolConstant().isResolved()) {
                setForeground(style().bytecodeColor());
            } else {
                setForeground(style().javaUnresolvedNameColor());
            }
        }

        @Override
        protected void specializeMenu(InspectorMenu menu) {
            if (telePoolConstant().isResolved()) {
                menu.addSeparator();
                final TeleFieldRefConstant.Resolved teleResolvedFieldRefConstant = (TeleFieldRefConstant.Resolved) telePoolConstant();
                final TeleFieldActor teleFieldActor = teleResolvedFieldRefConstant.getTeleFieldActor();
                menu.add(inspection().inspectionMenus().getInspectObjectAction(teleFieldActor, "Inspect FieldActor"));
            }
        }

    }

    private static final class ClassMethod extends PoolConstantLabel {

        /**
         * Assigned when resolved.
         */
        private TeleClassMethodActor _teleClassMethodActor;

        private ClassMethod(Inspection inspection, int index, TeleConstantPool teleConstantPool, Mode mode) {
            super(inspection, index, teleConstantPool, mode);
            redisplay();
        }

        private void checkResolved() {
            if (_teleClassMethodActor == null && telePoolConstant().isResolved()) {
                final TeleClassMethodRefConstant.Resolved teleResolvedClassMethodRefConstant = (TeleClassMethodRefConstant.Resolved) telePoolConstant();
                _teleClassMethodActor = teleResolvedClassMethodRefConstant.getTeleClassMethodActor();
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
                    setText(methodName + "()");
                    break;
                default:
                    Problem.unimplemented();
            }
            setJavapResolvableToolTipText("ClassMethod", holderName + "." + methodName + ":" + methodRefConstant.descriptor(localConstantPool()).toString());
            if (_teleClassMethodActor == null) {
                setForeground(style().javaUnresolvedNameColor());
            } else {
                if (_teleClassMethodActor.hasBytecodes()) {
                    setForeground(style().bytecodeMethodEntryColor());
                } else {
                    setForeground(style().bytecodeColor());
                }
            }
        }

        @Override
        protected void specializeMenu(InspectorMenu menu) {
            checkResolved();
            if (_teleClassMethodActor != null) {
                menu.addSeparator();
                final ClassMethodMenuItems classMethodMenuItems = new ClassMethodMenuItems(inspection(), _teleClassMethodActor);
                classMethodMenuItems.addTo(menu);
            }
        }

        @Override
        protected void handleLeftButtonEvent() {
            checkResolved();
            if (_teleClassMethodActor != null && _teleClassMethodActor.hasBytecodes()) {
                final TeleCodeLocation teleCodeLocation = new TeleCodeLocation(teleVM(), _teleClassMethodActor, 0);
                inspection().focus().setCodeLocation(teleCodeLocation, false);
            }
        }
    }

    private static final class InterfaceMethod extends PoolConstantLabel {

        private InterfaceMethod(Inspection inspection, int index, TeleConstantPool teleConstantPool, Mode mode) {
            super(inspection, index, teleConstantPool, mode);
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
                    setText(methodName + "()");
                    break;
                default:
                    Problem.unimplemented();
            }
            setJavapResolvableToolTipText("InterfaceMethod", holderName + "." + methodName + ":" + methodRefConstant.descriptor(localConstantPool()).toString());
            if (telePoolConstant().isResolved()) {
                setForeground(style().bytecodeColor());
            } else {
                setForeground(style().javaUnresolvedNameColor());
            }
        }

        @Override
        protected void specializeMenu(InspectorMenu menu) {
            if (telePoolConstant().isResolved()) {
                menu.addSeparator();
                final TeleInterfaceMethodRefConstant.Resolved teleResolvedInterfaceMethodRefConstant = (TeleInterfaceMethodRefConstant.Resolved) telePoolConstant();
                final TeleInterfaceMethodActor teleInterfaceMethodActor = teleResolvedInterfaceMethodRefConstant.getTeleInterfaceMethodActor();
                menu.add(inspection().inspectionMenus().getInspectObjectAction(teleInterfaceMethodActor, "Inspect InterfaceMethodActor"));
            }
        }

    }

    private static final class StringConstant extends PoolConstantLabel {

        private StringConstant(Inspection inspection, int index, TeleConstantPool teleConstantPool, Mode mode) {
            super(inspection, index, teleConstantPool, mode);
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
                    setText(shortText);
                    break;
                default:
                    Problem.unimplemented();
            }
            setJavapToolTipText("String", fullText);
        }
    }

    private static final class Utf8Constant extends PoolConstantLabel {

        private Utf8Constant(Inspection inspection, int index, TeleConstantPool teleConstantPool, Mode mode) {
            super(inspection, index, teleConstantPool, mode);
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
                    setText(shortText);
                    break;
                default:
                    Problem.unimplemented();
            }
            setJavapToolTipText("Utf8", fullText);
        }

    }

    private static final class Other extends PoolConstantLabel {

        private Other(Inspection inspection, int index, TeleConstantPool teleConstantPool, Mode mode) {
            super(inspection, index, teleConstantPool, mode);
            redisplay();
        }

        @Override
        public void updateText() {
            final String text = localPoolConstant().valueString(localConstantPool());
            switch (mode()) {
                case JAVAP:
                    setText(text);
                    break;
                case TERSE:
                    setText(text);
                    break;
                default:
                    Problem.unimplemented();
            }
            setJavapToolTipText("", text);
        }

    }

}
