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

import java.awt.datatransfer.*;
import java.io.*;

import javax.swing.*;

import com.sun.max.collect.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.java.*;
import com.sun.max.ins.memory.*;
import com.sun.max.ins.method.*;
import com.sun.max.ins.type.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.prototype.BootImage.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.run.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Menus offered by an Inspection.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 * @author Aritra Bandyopadhyay
 */
public final class InspectionMenus implements Prober {

    private final Inspection _inspection;

    public Inspection inspection() {
        return _inspection;
    }

    private TeleVM teleVM() {
        return _inspection.teleVM();
    }

    private TeleProcess teleProcess() {
        return _inspection.teleProcess();
    }

    private TeleProcessController teleProcessController() {
        return inspection().controller();
    }

    private InspectionFocus focus() {
        return _inspection.focus();
    }

    private boolean isSynchronousMode() {
        return inspection().isSynchronousMode();
    }

    private final JMenu _inspectionMenu = new JMenu("Inspector");

    public final class RelocateAction extends InspectorAction {

        RelocateAction() {
            super(_inspection, "Relocate Boot Image");
        }

        private boolean _done;

        @Override
        protected synchronized void procedure() {
            if (!_done) {
                _done = true;
                assert MaxineInspector.suspendingBeforeRelocating();
                try {
                    teleVM().advanceToJavaEntryPoint();
                } catch (IOException ioException) {
                    throw new InspectorError("error during relocation of boot image", ioException);
                }
                _inspectionMenu.remove(0); // remove the menu item
                _inspectionMenu.remove(0); // remove the separator as well
            }
        }
    }

    public final class SetInspectorTraceLevelAction extends InspectorAction {

        SetInspectorTraceLevelAction() {
            super(_inspection, "Set Inspector Trace Level");
        }

        @Override
        protected void procedure() {
            final int oldLevel = Trace.level();
            int newLevel = oldLevel;
            final String input = _inspection.inputDialog("Set Inspector Trace Level", Integer.toString(oldLevel));
            try {
                newLevel = Integer.parseInt(input);
            } catch (NumberFormatException numberFormatException) {
                _inspection.errorMessage(numberFormatException.toString());
            }
            if (newLevel != oldLevel) {
                Trace.on(newLevel);
            }
        }
    }

    public final class RefreshAllAction extends InspectorAction {

        RefreshAllAction() {
            super(_inspection, "Refresh All");
        }

        @Override
        protected void procedure() {
            _inspection.refreshAll(true);
        }
    }

    public final class CloseAllInspectorsAction extends InspectorAction {

        CloseAllInspectorsAction() {
            super(_inspection, "Close All");
        }

        @Override
        protected void procedure() {
            _inspection.desktopPane().removeAll();
            _inspection.repaint();
        }
    }

    public final class QuitAction extends InspectorAction {

        QuitAction() {
            super(_inspection, "Quit");
        }

        @Override
        protected void procedure() {
            _inspection.quit();
        }
    }

    public final class FileCommandsAction extends InspectorAction {

        FileCommandsAction() {
            super(_inspection, FileCommands.actionName());
        }

        @Override
        protected void procedure() {
            final String value = _inspection.inputDialog("File name: ", FileCommands.defaultCommandFile());
            if (value != null && !value.equals("")) {
                FileCommands.executeCommandsFromFile(_inspection, value);
            }
        }
    }

    /**
     * A command that updates the {@linkplain TeleVM#updateLoadableTypeDescriptorsFromClasspath() types available} on
     * the tele VM's class path by rescanning the complete class path for types.
     */
    public final class UpdateClasspathTypes extends InspectorAction {

        UpdateClasspathTypes() {
            super(_inspection, "Rescan Class Path For Types");
        }

        @Override
        protected void procedure() {
            teleVM().updateLoadableTypeDescriptorsFromClasspath();
        }
    }

    private JMenu createInspectionMenu() {
        if (MaxineInspector.suspendingBeforeRelocating()) {
            _inspectionMenu.add(new RelocateAction());
            _inspectionMenu.addSeparator();
        }
        _inspectionMenu.add(new SetInspectorTraceLevelAction());
        _inspectionMenu.add(new ChangeInterpreterUseLevelAction());
        _inspectionMenu.add(new SetTransportDebugLevelAction());
        _inspectionMenu.add(new FileCommandsAction());
        _inspectionMenu.add(new UpdateClasspathTypes());
        _inspectionMenu.addSeparator();
        _inspectionMenu.add(new RefreshAllAction());
        _inspectionMenu.addSeparator();
        _inspectionMenu.add(new CloseAllInspectorsAction());
        _inspectionMenu.addSeparator();
        _inspectionMenu.add(new PreferenceDialogAction());
        _inspectionMenu.addSeparator();
        _inspectionMenu.add(new QuitAction());
        return _inspectionMenu;
    }

    public final class InspectClassAction extends InspectorAction {

        InspectClassAction() {
            super(_inspection, "Inspect Class...");
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(_inspection, "Inspect Class ...", "Inspect");
            if (teleClassActor != null) {
                focus().setHeapObject(teleClassActor);
            }
        }
    }

    public final class InspectClassByHexIdAction extends InspectorAction {

        InspectClassByHexIdAction() {
            super(_inspection, "Inspect Class By ID (Hex) ...");
        }

        @Override
        protected void procedure() {
            final String value = _inspection.questionMessage("ID (hex): ");
            if (value != null && !value.equals("")) {
                try {
                    final int serial = Integer.parseInt(value, 16);
                    final TeleClassActor teleClassActor = teleVM().teleClassRegistry().findTeleClassActorByID(serial);
                    if (teleClassActor == null) {
                        _inspection.errorMessage("failed to find classActor for ID:  0x" + Integer.toHexString(serial));
                    } else {
                        focus().setHeapObject(teleClassActor);
                    }
                } catch (NumberFormatException ex) {
                    _inspection.errorMessage("Hex integer required");
                }
            }
        }
    }

    public final class InspectClassByDecimalIdAction extends InspectorAction {

        InspectClassByDecimalIdAction() {
            super(_inspection, "Inspect Class By ID (decimal) ...");
        }

        @Override
        protected void procedure() {
            final String value = _inspection.questionMessage("ID (decimal): ");
            if (value != null && !value.equals("")) {
                try {
                    final int serial = Integer.parseInt(value, 10);
                    final TeleClassActor teleClassActor = teleVM().teleClassRegistry().findTeleClassActorByID(serial);
                    if (teleClassActor == null) {
                        _inspection.errorMessage("failed to find classActor for ID: " + serial);
                    } else {
                        focus().setHeapObject(teleClassActor);
                    }
                } catch (NumberFormatException ex) {
                    _inspection.errorMessage("Hex integer required");
                }
            }
        }
    }

    private JMenu createClassMenu() {
        final JMenu menu = new JMenu("Class");
        menu.add(new InspectClassAction());
        menu.add(new InspectClassByHexIdAction());
        menu.add(new InspectClassByDecimalIdAction());
        return menu;
    }

    public final class BootClassRegistryAction extends InspectorAction {

        BootClassRegistryAction() {
            super(_inspection, "Inspect Boot Class Registry");
        }

        @Override
        protected void procedure() {
            final TeleObject teleBootClassRegistry = TeleObject.make(teleVM(), teleVM().bootClassRegistryReference());
            focus().setHeapObject(teleBootClassRegistry);
        }
    }

    /**
     * Action to create an object inspector; interactive if a {@link TeleObject} not specified at creation.
     */
    public final class InspectObjectAtAddressAction extends InspectorAction {

        InspectObjectAtAddressAction() {
            super(_inspection, "Inspect Object at Address...");
        }

        @Override
        protected void procedure() {
            new AddressInputDialog(_inspection, teleVM().bootHeapStart(), "Inspect Object at Address...", "Inspect") {

                @Override
                public void entered(Address address) {
                    final Pointer pointer = address.asPointer();
                    if (teleVM().isValidOrigin(pointer)) {
                        final Reference objectReference = teleVM().originToReference(pointer);
                        final TeleObject teleObject = TeleObject.make(teleVM(), objectReference);
                        focus().setHeapObject(teleObject);
                    } else {
                        _inspection.errorMessage("heap object not found at 0x"  + address.toHexString());
                    }
                }
            };
        }
    }

    /**
     * @param surrogate for an object in the tele VM
     * @return an Action that will create an Object Inspector
     */
    public InspectorAction getInspectObjectAtAddressAction() {
        return new InspectObjectAtAddressAction();
    }

    /**
     * Action to create an object inspector; interactive if a {@link TeleObject} not specified at creation.
     */
    public final class InspectObjectByIDAction extends InspectorAction {

        private static final String ACTION_NAME = "Inspect Object By ID...";
        InspectObjectByIDAction() {
            super(_inspection, ACTION_NAME);
        }

        @Override
        protected void procedure() {
            final String input = _inspection.inputDialog("Inspect Object By ID..", "");
            try {
                final long oid = Long.parseLong(input);
                final TeleObject teleObject = TeleObject.lookupObject(oid);
                if (teleObject != null) {
                    focus().setHeapObject(teleObject);
                } else {
                    _inspection.errorMessage("failed to find Object for ID: " + input);
                }
            } catch (NumberFormatException numberFormatException) {
                _inspection.errorMessage("Not a ID: " + input);
            }
        }
    }

    /**
     * @param surrogate for an object in the tele VM
     * @return an Action that will create an Object Inspector interactively, prompting the user for a numeric object ID
     */
    public InspectorAction getInspectObjectByIDAction() {
        return new InspectObjectByIDAction();
    }

    private JMenu createObjectMenu() {
        final JMenu menu = new JMenu("Object");
        menu.add(new BootClassRegistryAction());
        menu.add(new InspectObjectAtAddressAction());
        menu.add(new InspectObjectByIDAction());
        return menu;
    }

    public final class InspectBootHeapMemoryAction extends InspectorAction {

        InspectBootHeapMemoryAction() {
            super(_inspection, "Inspect Memory at Boot Heap Region Start");
        }

        @Override
        protected void procedure() {
            MemoryInspector.create(_inspection, teleVM().bootHeapStart());
        }
    }

    public final class InspectBootCodeMemoryAction extends InspectorAction {

        InspectBootCodeMemoryAction() {
            super(_inspection, "Inspect Memory at Boot Code Region Start");
        }

        @Override
        protected void procedure() {
            MemoryInspector.create(_inspection, teleVM().bootCodeStart());
        }
    }

    /**
     * Action to inspect a memory region, interactive if {@link Address} not specified.
     */
    public final class InspectMemoryAction extends InspectorAction {

        private final Address _address;
        private final TeleObject _teleObject;

        InspectMemoryAction() {
            super(_inspection, "Inspect Memory at Address...");
            _address = null;
            _teleObject = null;
        }

        InspectMemoryAction(Address address, String title) {
            super(_inspection, title);
            _address = address;
            _teleObject = null;
        }

        InspectMemoryAction(Address address) {
            this(address, "Inspect Memory");
        }

        InspectMemoryAction(TeleObject teleObject, String title) {
            super(_inspection, title);
            _address = null;
            _teleObject = teleObject;
        }

        InspectMemoryAction(TeleObject teleObject) {
            this(teleObject, "Inspect Memory");
        }

        @Override
        protected void procedure() {
            if (_teleObject != null) {
                MemoryInspector.create(_inspection, _teleObject);
            } else if (_address != null) {
                MemoryInspector.create(_inspection, _address);
            } else {
                new AddressInputDialog(_inspection, teleVM().bootHeapStart(), "Inspect Memory at Address...", "Inspect") {

                    @Override
                    public void entered(Address address) {
                        MemoryInspector.create(_inspection, address);
                    }
                };
            }
        }
    }

    /**
     * @param teleObject surrogate for an object in the tele VM
     * @return an Action that will create a Memory Inspector at the address
     */
    public InspectorAction getInspectMemoryAction(TeleObject teleObject) {
        return new InspectMemoryAction(teleObject);
    }

    /**
     * @param teleObject surrogate for an object in the tele VM
     * @param title a string name for the Action
     * @return an Action that will create a Memory Inspector at the address
     */
    public InspectorAction getInspectMemoryAction(TeleObject teleObject, String title) {
        return new InspectMemoryAction(teleObject, title);
    }

    /**
     * @param address a valid memory {@link Address} in the tele VM
     * @return an Action that will create a Memory Inspector at the address
     */
    public InspectorAction getInspectMemoryAction(Address address) {
        return new InspectMemoryAction(address);
    }

    /**
     * @param address a valid memory {@link Address} in the tele VM
     * @param title a string name for the Action
     * @return an Action that will create a Memory Inspector at the address
     */
    public InspectorAction getInspectMemoryAction(Address address, String title) {
        return new InspectMemoryAction(address, title);
    }

    /**
     * Action to inspect a memory region, interactive if {@link Address} not specified.
     */
    public final class InspectMemoryWordsAction extends InspectorAction {

        private final Address _address;
        private final TeleObject _teleObject;

        InspectMemoryWordsAction() {
            super(_inspection, "Inspect Memory Words at Address...");
            _address = null;
            _teleObject = null;
        }

        InspectMemoryWordsAction(Address address) {
            this(address, "Inspect Memory Words");
        }

        InspectMemoryWordsAction(Address address, String title) {
            super(_inspection, title);
            _address = address;
            _teleObject = null;
        }

        InspectMemoryWordsAction(TeleObject teleObject) {
            this(teleObject, "Inspect Memory Words");
        }

        InspectMemoryWordsAction(TeleObject teleObject, String title) {
            super(_inspection, title);
            _address = null;
            _teleObject = teleObject;
        }

        @Override
        protected void procedure() {
            if (_teleObject != null) {
                MemoryWordInspector.create(_inspection, _teleObject);
            }
            if (_address != null) {
                MemoryWordInspector.create(_inspection, _address);
            } else {
                new AddressInputDialog(_inspection, teleVM().bootHeapStart(), "Inspect Memory Words at Address...", "Inspect") {

                    @Override
                    public void entered(Address address) {
                        MemoryWordInspector.create(_inspection, address);
                    }
                };
            }
        }
    }

    /**
     * @param teleObject surrogate for a valid object in the teleVM
     * @return an Action that will create a Memory Words Inspector at the address
     */
    public InspectorAction getInspectMemoryWordsAction(TeleObject teleObject) {
        return new InspectMemoryWordsAction(teleObject);
    }

    /**
     * @param teleObject a surrogate for a valid object in the teleVM
     * @param title a name for the action
     * @return an Action that will create a Memory Words Inspector at the address
     */
    public InspectorAction getInspectMemoryWordsAction(TeleObject teleObject, String title) {
        return new InspectMemoryWordsAction(teleObject, title);
    }

    /**
     * @param address a valid memory {@link Address} in the tele VM
     * @return an Action that will create a Memory Words Inspector at the address
     */
    public InspectorAction getInspectMemoryWordsAction(Address address) {
        return new InspectMemoryWordsAction(address);
    }

    /**
     * @param address a valid memory {@link Address} in the tele VM
     * @param title a name for the action
     * @return an Action that will create a Memory Words Inspector at the address
     */
    public InspectorAction getInspectMemoryWordsAction(Address address, String title) {
        return new InspectMemoryWordsAction(address, title);
    }

    private JMenu createMemoryMenu() {
        final JMenu menu = new JMenu("Memory");
        menu.add(new InspectBootHeapMemoryAction());
        menu.add(new InspectBootCodeMemoryAction());
        menu.add(new InspectMemoryAction());
        menu.add(new InspectMemoryWordsAction());
        return menu;
    }

    public final class InspectMethodAction extends InspectorAction {

        public InspectMethodAction() {
            super(_inspection, "Inspect MethodActor...");
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(_inspection, "Inspect MethodActor in Class...", "Select");
            if (teleClassActor != null) {
                final TeleMethodActor teleMethodActor = MethodActorSearchDialog.show(_inspection, teleClassActor, "Inspect MethodActor...", "Inspect");
                if (teleMethodActor != null) {
                    focus().setHeapObject(teleMethodActor);
                }
            }
        }
    }

    private final InspectMethodAction _inspectMethodAction;

    public InspectMethodAction getInspectMethodAction() {
        return _inspectMethodAction;
    }

    public final class ViewSelectedCodeAction extends InspectorAction {

        public ViewSelectedCodeAction() {
            super(_inspection, "View Current Code Selection");
        }

        @Override
        protected void procedure() {
            final TeleCodeLocation teleCodeLocation = focus().codeLocation();
            focus().setCodeLocation(teleCodeLocation, true);
        }
    }

    private final ViewSelectedCodeAction _viewSelectedCodeAction;

    public ViewSelectedCodeAction getViewSelectedCodeAction() {
        return _viewSelectedCodeAction;
    }

    public final class ViewCodeAtIPAction extends InspectorAction {

        public ViewCodeAtIPAction() {
            super(_inspection, "View Code at current IP");
        }

        @Override
        protected void procedure() {
            final Pointer instructionPointer = focus().thread().instructionPointer();
            focus().setCodeLocation(new TeleCodeLocation(teleVM(), instructionPointer), true);
        }
    }

    private final ViewCodeAtIPAction _viewCodeAtIPAction;

    public ViewCodeAtIPAction getViewCodeAtIPAction() {
        return _viewCodeAtIPAction;
    }

    public final class ViewMethodBytecodeAction extends InspectorAction {

        public ViewMethodBytecodeAction() {
            super(_inspection, "View Bytecodes for Method...");
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(_inspection, "View Bytecode for Method in Class...", "Select");
            if (teleClassActor != null) {
                final Predicate<TeleMethodActor> hasBytecodesPredicate = new Predicate<TeleMethodActor>() {

                    @Override
                    public boolean evaluate(TeleMethodActor teleMethodActor) {
                        return teleMethodActor.hasCodeAttribute();
                    }
                };
                final TeleMethodActor teleMethodActor = MethodActorSearchDialog.show(_inspection, teleClassActor, hasBytecodesPredicate, "View Bytecode for Method...", "Inspect");
                if (teleMethodActor != null && teleMethodActor instanceof TeleClassMethodActor) {
                    final TeleClassMethodActor teleClassMethodActor = (TeleClassMethodActor) teleMethodActor;
                    final TeleCodeLocation teleCodeLocation = new TeleCodeLocation(teleVM(), teleClassMethodActor, 0);
                    focus().setCodeLocation(teleCodeLocation, false);
                }
            }
        }
    }

    private final ViewMethodBytecodeAction _viewMethodBytecodeAction;

    public ViewMethodBytecodeAction getViewMethodBytecodeAction() {
        return _viewMethodBytecodeAction;
    }

    public final class ViewMethodTargetCodeAction extends InspectorAction {

        public ViewMethodTargetCodeAction() {
            super(_inspection, "View Code for Method...");
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(_inspection, "View Target Code for Class...", "Select");
            if (teleClassActor != null) {
                final Sequence<TeleTargetMethod> teleTargetMethods = TargetMethodSearchDialog.show(_inspection, teleClassActor, "View Target Code for Method...", "View Code", false);
                if (teleTargetMethods != null) {
                    focus().setCodeLocation(new TeleCodeLocation(teleVM(), teleTargetMethods.first().callEntryPoint()), false);
                }
            }
        }
    }

    private final ViewMethodTargetCodeAction _viewMethodTargetCodeAction;

    public ViewMethodTargetCodeAction getViewMethodTargetCodeAction() {
        return _viewMethodTargetCodeAction;
    }

    public final class ViewBootImageMethodCodeAction extends InspectorAction {

        private final int _offset;

        public ViewBootImageMethodCodeAction(int offset, Class clazz, String name, Class... parameterTypes) {
            super(_inspection, MethodActor.fromJava(Classes.getDeclaredMethod(clazz, name, parameterTypes)).format("%H.%n(%p)"));
            _offset = offset;
        }

        @Override
        protected void procedure() {
            focus().setCodeLocation(new TeleCodeLocation(teleVM(), teleVM().bootHeapStart().plus(_offset)), true);
        }
    }

    private final ViewBootImageMethodCodeAction _viewBootImageRunMethodCodeAction;

    public ViewBootImageMethodCodeAction getViewBootImageRunMethodCodeAction() {
        return _viewBootImageRunMethodCodeAction;
    }

    private final ViewBootImageMethodCodeAction _viewBootImageThreadRunMethodCodeAction;

    public ViewBootImageMethodCodeAction getViewBootImageThreadRunMethodCodeAction() {
        return _viewBootImageThreadRunMethodCodeAction;
    }

    private final ViewBootImageMethodCodeAction _viewBootImageSchemeRunMethodCodeAction;

    public ViewBootImageMethodCodeAction getViewBootImageSchemeRunMethodCodeAction() {
        return _viewBootImageSchemeRunMethodCodeAction;
    }

    public final class ViewMethodCodeContainingAddressAction extends InspectorAction {

        public ViewMethodCodeContainingAddressAction() {
            super(_inspection, "View Method Code Containing Address...");
        }

        @Override
        protected void procedure() {
            new AddressInputDialog(_inspection, teleVM().bootHeapStart(), "View Method Code Containing address...", "View Code") {

                @Override
                public boolean isValidInput(Address address) {
                    return TeleTargetMethod.make(teleVM(), address) != null;
                }

                @Override
                public void entered(Address address) {
                    focus().setCodeLocation(new TeleCodeLocation(teleVM(), address), false);
                }
            };
        }
    }

    private final ViewMethodCodeContainingAddressAction _viewMethodCodeContainingAddressAction;

    public ViewMethodCodeContainingAddressAction getViewMethodCodeContainingAddressAction() {
        return _viewMethodCodeContainingAddressAction;
    }

    public final class ViewNativeCodeContainingAddressAction extends InspectorAction {

        public ViewNativeCodeContainingAddressAction() {
            super(_inspection, "View Native Code Containing Code Address...");
        }

        @Override
        protected void procedure() {
            // Most likely situation is that we are just about to call a native method in which case RAX is the address
            final TeleNativeThread teleNativeThread = focus().thread();
            assert teleNativeThread != null;
            final Address indirectCallAddress = teleNativeThread.integerRegisters().getCallRegisterValue();
            final Address initialAddress = indirectCallAddress == null ? teleVM().bootHeapStart() : indirectCallAddress;
            new AddressInputDialog(_inspection, initialAddress, "View Native Code Containing Code Address...", "View Code") {
                @Override
                public void entered(Address address) {
                    focus().setCodeLocation(new TeleCodeLocation(teleVM(), address), true);
                }
            };
        }
    }

    private final ViewNativeCodeContainingAddressAction _viewNativeCodeContainingAddressAction;

    public ViewNativeCodeContainingAddressAction getViewNativeCodeContainingAddressAction() {
        return _viewNativeCodeContainingAddressAction;
    }

    public JMenu createMethodMenu() {
        final JMenu menu = new JMenu("Method");
        menu.add(_inspectMethodAction);
        menu.add(_viewSelectedCodeAction);
        menu.add(_viewCodeAtIPAction);
        menu.add(_viewMethodBytecodeAction);
        menu.add(_viewMethodTargetCodeAction);
        final JMenu sub = new JMenu("View Boot Image Method Code");
        final Header header = teleVM().bootImage().header();
        sub.add(new ViewBootImageMethodCodeAction(header._vmRunMethodOffset, MaxineVM.class, "run", MaxineVM.runMethodParameterTypes()));
        sub.add(new ViewBootImageMethodCodeAction(header._vmThreadRunMethodOffset, VmThread.class, "run", int.class, Address.class, Pointer.class,
                        Pointer.class, Pointer.class, Pointer.class, Pointer.class, Pointer.class, Pointer.class, Pointer.class));
        sub.add(new ViewBootImageMethodCodeAction(header._runSchemeRunMethodOffset, teleVM().vmConfiguration().runPackage().schemeTypeToImplementation(RunScheme.class), "run"));
        menu.add(sub);
        menu.add(_viewMethodCodeContainingAddressAction);
        menu.add(_viewNativeCodeContainingAddressAction);
        return menu;
    }

    public final class SingleStepAction extends InspectorAction {

        SingleStepAction() {
            super(_inspection, "Single Instruction Step", true);
        }

        @Override
        protected void procedure() {
            final TeleNativeThread selectedThread = focus().thread();
            try {
                teleProcessController().singleStep(selectedThread, isSynchronousMode());
            } catch (Exception exception) {
                throw new InspectorError("Couldn't single step.", exception);
            }
        }
    }

    private final SingleStepAction _singleStepAction;

    /**
     * @return an action that will single step the currently selected thread in the tele VM
     */
    public InspectorAction getSingleStepAction() {
        return _singleStepAction;
    }

    public final class RunToInstructionAction extends InspectorAction {

        RunToInstructionAction() {
            super(_inspection, "Run To Selected Instruction (ignoring breakpoints)", true);
        }

        @Override
        protected void procedure() {
            final Address selectedAddress = focus().codeLocation().targetCodeInstructionAddresss();
            if (!selectedAddress.isZero()) {
                try {
                    teleProcessController().runToInstruction(selectedAddress, isSynchronousMode(), true);
                } catch (Exception exception) {
                    throw new InspectorError("Run to instruction (ignoring breakpoints) could not be performed.", exception);
                }
            } else {
                _inspection.errorMessage("No instruction selected");
            }
        }
    }

    private final RunToInstructionAction _runToInstructionAction;

    /**
     * @return an Action that will resume execution on the tele VM, stopping at the location of the currently selected
     *         instruction
     */
    public InspectorAction getRunToInstructionAction() {
        return _runToInstructionAction;
    }

    public final class RunToInstructionWithBreakpointsAction extends InspectorAction {

        RunToInstructionWithBreakpointsAction() {
            super(_inspection, "Run To Selected Instruction", true);
        }

        @Override
        protected void procedure() {
            final Address selectedAddress = focus().codeLocation().targetCodeInstructionAddresss();
            if (!selectedAddress.isZero()) {
                try {
                    teleProcessController().runToInstruction(selectedAddress, isSynchronousMode(), false);
                } catch (Exception exception) {
                    throw new InspectorError("Run to instruction could not be performed.", exception);
                }
            } else {
                _inspection.errorMessage("No instruction selected");
            }
        }
    }

    private final RunToInstructionWithBreakpointsAction _runToInstructionWithBreakpointsAction;

    public final class ReturnFromFrameAction extends InspectorAction {

        ReturnFromFrameAction() {
            super(_inspection, "Return From Frame (ignoring breakpoints)", true);
        }

        @Override
        protected void procedure() {
            final Address returnAddress = focus().thread().getReturnAddress();
            if (returnAddress != null) {
                try {
                    teleProcessController().runToInstruction(returnAddress, isSynchronousMode(), true);
                } catch (Exception exception) {
                    throw new InspectorError("Return from frame (ignoring breakpoints) could not be performed.", exception);
                }
            }
        }
    }

    private final ReturnFromFrameAction _returnFromFrameAction;

    /**
     * @return an Action that will resume execution in the tele VM, stopping at the first instruction after returning
     *         from the current frame of the currently selected thread
     */
    public InspectorAction getReturnFromFrameAction() {
        return _returnFromFrameAction;
    }

    public final class ReturnFromFrameWithBreakpointsAction extends InspectorAction {

        ReturnFromFrameWithBreakpointsAction() {
            super(_inspection, "Return From Frame", true);
        }

        @Override
        protected void procedure() {
            final Address returnAddress = focus().thread().getReturnAddress();
            if (returnAddress != null) {
                try {
                    teleProcessController().runToInstruction(returnAddress, isSynchronousMode(), false);
                } catch (Exception exception) {
                    throw new InspectorError("Return from frame could not be performed.", exception);
                }
            }
        }
    }

    private final ReturnFromFrameWithBreakpointsAction _returnFromFrameWithBreakpointsAction;

    public final class StepOverAction extends InspectorAction {

        StepOverAction() {
            super(_inspection, "Step Over (ignoring breakpoints)", true);
        }

        @Override
        protected void procedure() {
            final TeleNativeThread thread = focus().thread();
            try {
                teleProcessController().stepOver(thread, isSynchronousMode(), true);
            } catch (Exception exception) {
                throw new InspectorError("Step over (ignoring breakpoints) could not be performed.", exception);
            }
        }
    }

    private final StepOverAction _stepOverAction;

    /**
     * @return an Action that will resume execution of the tele VM, stopping at the one immediately after the current
     *         instruction of the currently selected thread
     */
    public InspectorAction getStepOverAction() {
        return _stepOverAction;
    }

    public final class StepOverWithBreakpointsAction extends InspectorAction {

        StepOverWithBreakpointsAction() {
            super(_inspection, "Step Over", true);
        }

        @Override
        protected void procedure() {
            final TeleNativeThread thread = focus().thread();
            try {
                teleProcessController().stepOver(thread, isSynchronousMode(), false);
            } catch (Exception exception) {
                throw new InspectorError("Step over could not be performed.", exception);
            }
        }
    }

    private final StepOverWithBreakpointsAction _stepOverWithBreakpointsAction;

    public final class ChangeInterpreterUseLevelAction extends InspectorAction {

        ChangeInterpreterUseLevelAction() {
            super(_inspection, "Change Interpreter Use Level...");
        }

        @Override
        protected void procedure() {
            final int oldLevel = teleVM().interpreterUseLevel();
            int newLevel = oldLevel;
            final String input = _inspection.inputDialog("Change interpreter use level (0=none, 1=some, etc)", Integer.toString(oldLevel));
            try {
                newLevel = Integer.parseInt(input);
            } catch (NumberFormatException numberFormatException) {
                _inspection.errorMessage(numberFormatException.toString());
            }
            if (newLevel != oldLevel) {
                teleVM().setInterpreterUseLevel(newLevel);
            }
        }
    }

    public final class SetTransportDebugLevelAction extends InspectorAction {

        SetTransportDebugLevelAction() {
            super(_inspection, "Set Transport Debug Level...");
        }

        @Override
        protected void procedure() {
            final int oldLevel = teleProcess().transportDebugLevel();
            int newLevel = oldLevel;
            final String input = _inspection.inputDialog(" (Set Transport Debug Level, 0=none, 1=some, etc)", Integer.toString(oldLevel));
            try {
                newLevel = Integer.parseInt(input);
            } catch (NumberFormatException numberFormatException) {
                _inspection.errorMessage(numberFormatException.toString());
            }
            if (newLevel != oldLevel) {
                teleProcess().setTransportDebugLevel(newLevel);
            }
        }
    }

    private JMenuItem _showStackFramesMenuItem;

    public final class ShowStackFramesAction extends InspectorAction {

        private static final String MENU_TEXT = "Show All Stack Frames";

        ShowStackFramesAction() {
            super(_inspection, "Show All Stack Frames");
        }

        @Override
        protected void procedure() {
            _showStackFramesMenuItem.setText(MENU_TEXT);
        }
    }

    public final class ResumeAction extends InspectorAction {

        ResumeAction() {
            super(_inspection, "Resume", true);
        }

        @Override
        protected void procedure() {
            try {
                teleProcessController().resume(isSynchronousMode(), false);
            } catch (Exception exception) {
                throw new InspectorError("Run to instruction could not be performed.", exception);
            }
        }
    }

    private final ResumeAction _resumeAction;

    public final class PauseAction extends InspectorAction {
        PauseAction() {
            super(_inspection, "Pause Process");
        }

        @Override
        protected void procedure() {
            try {
                teleProcessController().pause();
            } catch (Exception exception) {
                throw new InspectorError("Pause could not be initiated", exception);
            }

        }
    }

    private final PauseAction _pauseAction;

    public PauseAction getPauseAction() {
        return _pauseAction;
    }

    /**
     * @return an Action that will resume full execution of the tele VM
     */
    public InspectorAction getResumeAction() {
        return _resumeAction;
    }

    public final class ToggleTargetBreakpointAction extends InspectorAction {

        ToggleTargetBreakpointAction() {
            super(_inspection, "Toggle Target Breakpoint");
        }

        @Override
        protected void procedure() {
            // TODO (mlvdv) push toggle method into factory?
            final Address targetCodeInstructionAddress = focus().codeLocation().targetCodeInstructionAddresss();
            if (!targetCodeInstructionAddress.isZero()) {
                TeleTargetBreakpoint breakpoint = teleProcess().targetBreakpointFactory().getNonTransientBreakpointAt(targetCodeInstructionAddress);
                if (breakpoint == null) {
                    breakpoint = teleProcess().targetBreakpointFactory().makeBreakpoint(targetCodeInstructionAddress, false);
                    focus().setBreakpoint(breakpoint);
                } else {
                    teleProcess().targetBreakpointFactory().removeBreakpointAt(targetCodeInstructionAddress);
                    focus().setBreakpoint(null);
                }
            }
        }
    }

    private final ToggleTargetBreakpointAction _toggleTargetBreakpointAction;

    /**
     * @return an Action that will toggle on/off a breakpoint set at the target code location of the currently selected
     *         instruction
     */
    public InspectorAction getToggleTargetBreakpointAction() {
        return _toggleTargetBreakpointAction;
    }

    public final class SetLabelBreakpointsAction extends InspectorAction {

        SetLabelBreakpointsAction() {
            super(_inspection, "Set Breakpoint at Every Target Code Label");
        }

        @Override
        protected void procedure() {
            final Address address = focus().codeLocation().targetCodeInstructionAddresss();
            final TeleTargetRoutine teleTargetRoutine = teleVM().teleCodeRegistry().get(TeleTargetRoutine.class, address);
            if (teleTargetRoutine != null) {
                teleTargetRoutine.setTargetCodeLabelBreakpoints();
            } else {
                _inspection.errorMessage("Unable to find target routine in which to set breakpoints");
            }
        }
    }

    private final SetLabelBreakpointsAction _setLabelBreakpointsAction;

    public final class ClearLabelBreakpointsAction extends InspectorAction {

        ClearLabelBreakpointsAction() {
            super(_inspection, "Clear Breakpoints at All Target Code Labels");
        }

        @Override
        protected void procedure() {
            final Address address = focus().codeLocation().targetCodeInstructionAddresss();
            final TeleTargetRoutine teleTargetRoutine = teleVM().teleCodeRegistry().get(TeleTargetRoutine.class, address);
            if (teleTargetRoutine != null) {
                teleTargetRoutine.clearTargetCodeLabelBreakpoints();
            }
        }
    }

    private final ClearLabelBreakpointsAction _clearLabelBreakpointsAction;

    public final class ClearSelectedBreakpointAction extends InspectorAction {

        ClearSelectedBreakpointAction() {
            super(_inspection, "Clear Selected Breakpoint");
        }

        @Override
        protected void procedure() {
            final TeleBreakpoint selectedTeleBreakpoint = focus().breakpoint();
            if (selectedTeleBreakpoint != null) {
                focus().setBreakpoint(null);
                selectedTeleBreakpoint.remove();
            } else {
                _inspection.errorMessage("No breakpoint selected");
            }
        }
    }

    private final ClearSelectedBreakpointAction _clearSelectedBreakpointAction;

    /**
     * @return an Action that will clear the currently selected breakpoint
     */
    public InspectorAction getClearSelectedBreakpointAction() {
        return _clearSelectedBreakpointAction;
    }

    public final class ClearAllBreakpointsAction extends InspectorAction {

        ClearAllBreakpointsAction() {
            super(_inspection, "Clear All Breakpoints");
        }

        @Override
        protected void procedure() {
            focus().setBreakpoint(null);
            teleProcess().targetBreakpointFactory().removeAllBreakpoints();
        }
    }

    private final ClearAllBreakpointsAction _clearAllBreakpointsAction;

    /**
     * @return an Action that will clear all existing breakpoints
     */
    public InspectorAction getClearAllBreakpointsAction() {
        return _clearAllBreakpointsAction;
    }

    public final class ViewBreakpointsAction extends InspectorAction {

        ViewBreakpointsAction() {
            super(_inspection, "View Breakpoints");
        }

        @Override
        protected void procedure() {
            BreakpointsInspector.make(_inspection);
        }
    }

    private final ViewBreakpointsAction _viewBreakpointsAction;

    public final class BreakAtTargetMethodAction extends InspectorAction {

        BreakAtTargetMethodAction() {
            super(_inspection, "Compiled Method...");
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(_inspection, "Class for Compiled Method Entry Breakpoints...", "Select");
            if (teleClassActor != null) {
                final Sequence<TeleTargetMethod> teleTargetMethods = TargetMethodSearchDialog.show(_inspection, teleClassActor, "Compiled Method Entry Breakpoints", "Set Breakpoints", true);
                if (teleTargetMethods != null) {
                    // There may be multiple compilations of a method in the result.
                    TeleTargetBreakpoint teleTargetBreakpoint = null;
                    for (TeleTargetMethod teleTargetMethod : teleTargetMethods) {
                        teleTargetBreakpoint = teleTargetMethod.getTeleClassMethodActor().setTargetBreakpointAtEntry();
                    }
                    focus().setBreakpoint(teleTargetBreakpoint);
                }
            }
        }
    }

    private final BreakAtTargetMethodAction _breakAtTargetMethodAction;

    /**
     * @return an Action that will set a breakpoint at a target code method to be selected interactively
     */
    public InspectorAction getBreakAtTargetMethodAction() {
        return _breakAtTargetMethodAction;
    }

    public final class BreakAtMethodAction extends InspectorAction {

        BreakAtMethodAction() {
            super(_inspection, "Method on Classpath...");
        }

        @Override
        protected void procedure() {
            final TypeDescriptor typeDescriptor = TypeSearchDialog.show(_inspection, "Class for Bytecode Method Entry Breakpoint...", "Select");
            if (typeDescriptor != null) {
                final MethodKey methodKey = MethodSearchDialog.show(_inspection, typeDescriptor, "Bytecode Method Entry Breakpoint", "Set Breakpoint");
                if (methodKey != null) {
                    teleVM().bytecodeBreakpointFactory().makeBreakpoint(new TeleBytecodeBreakpoint.Key(methodKey, 0), false);
                }
            }
        }
    }

    private final BreakAtMethodAction _breakAtMethodAction;

    /**
     * @return an Action that will set a breakpoint at a method to be selected interactively
     */
    public InspectorAction getBreakAtMethodAction() {
        return _breakAtMethodAction;
    }

    public final class BreakAtMethodKeyAction extends InspectorAction {

        BreakAtMethodKeyAction() {
            super(_inspection, "Method Matched by Key...");
        }

        @Override
        protected void procedure() {
            final MethodKey methodKey = MethodKeyInputDialog.show(_inspection, "Specify Method");
            if (methodKey != null) {
                teleVM().bytecodeBreakpointFactory().makeBreakpoint(new TeleBytecodeBreakpoint.Key(methodKey, 0), false);
            }
        }
    }

    private final BreakAtMethodKeyAction _breakAtMethodKeyAction;

    /**
     * @return an Action that will set a breakpoint at a method to be identified interactively by description
     */
    public InspectorAction getBreakAtMethodKeyAction() {
        return _breakAtMethodKeyAction;
    }

    public final class BreakAtObjectInitializersAction extends InspectorAction {

        BreakAtObjectInitializersAction() {
            super(_inspection, "Break in Compiled Object Initializers of Class...");
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(_inspection, "Break in Object Initializers of Class...", "Set Breakpoint");
            if (teleClassActor != null) {
                final ClassActor classActor = teleClassActor.classActor();
                if (classActor.localVirtualMethodActors() != null) {
                    for (VirtualMethodActor virtualMethodActor : classActor.localVirtualMethodActors()) {
                        if (virtualMethodActor.name() == SymbolTable.INIT) {
                            final TeleClassMethodActor teleClassMethodActor = TeleClassMethodActor.get(teleVM(), virtualMethodActor);
                            if (teleClassMethodActor != null) {
                                TeleTargetBreakpoint teleTargetBreakpoint = null;
                                for (TeleTargetMethod teleTargetMethod : teleClassMethodActor.targetMethods()) {
                                    teleTargetBreakpoint = teleTargetMethod.setTargetBreakpointAtEntry();
                                }
                                focus().setBreakpoint(teleTargetBreakpoint);
                            }
                        }
                    }
                }
            }
        }
    }

    private final BreakAtObjectInitializersAction _breakAtObjectInitializersAction;

    /**
     * @return an Action that will set breakpoints at all initializers of a class to be selected interactively
     */
    public InspectorAction getBreakAtObjectInitializersAction() {
        return _breakAtObjectInitializersAction;
    }

    public final class ToggleBytecodeBreakpointAction extends InspectorAction {

        ToggleBytecodeBreakpointAction() {
            super(_inspection, "Toggle Bytecode Breakpoint");
        }

        @Override
        protected void procedure() {
            final BytecodeLocation bytecodeLocation = focus().codeLocation().bytecodeLocation();
            if (bytecodeLocation != null) {
                final TeleBytecodeBreakpoint.Key key = new TeleBytecodeBreakpoint.Key(bytecodeLocation);
                final TeleBytecodeBreakpoint breakpoint = teleVM().bytecodeBreakpointFactory().getBreakpoint(key);
                if (breakpoint == null) {
                    teleVM().bytecodeBreakpointFactory().makeBreakpoint(key, false);
                } else {
                    teleVM().bytecodeBreakpointFactory().removeBreakpoint(key);
                }
            }
        }
    }

    private final ToggleBytecodeBreakpointAction _toggleBytecodeBreakpointAction;

    /**
     * @return an Action that will toggle on/off a breakpoint set at the bytecode location of the currently selected
     *         instruction
     */
    public ToggleBytecodeBreakpointAction getToggleBytecodeBreakpointAction() {
        return _toggleBytecodeBreakpointAction;
    }

    private JMenu createDebugMenu() {
        final JMenu menu = new JMenu("Debug");
        menu.add(_resumeAction);
        menu.add(_singleStepAction);
        menu.add(_stepOverWithBreakpointsAction);
        menu.add(_stepOverAction);
        menu.add(_returnFromFrameWithBreakpointsAction);
        menu.add(_returnFromFrameAction);
        menu.add(_runToInstructionWithBreakpointsAction);
        menu.add(_runToInstructionAction);
        menu.addSeparator();
        menu.add(_toggleTargetBreakpointAction);
        menu.add(_setLabelBreakpointsAction);
        menu.add(_clearLabelBreakpointsAction);
        final JMenu methodEntryBreakpoints = new JMenu("Break at Method Entry");
        methodEntryBreakpoints.add(_breakAtTargetMethodAction);
        methodEntryBreakpoints.add(_breakAtMethodAction);
        methodEntryBreakpoints.add(_breakAtMethodKeyAction);
        menu.add(methodEntryBreakpoints);
        menu.add(_breakAtObjectInitializersAction);
        menu.add(_clearAllBreakpointsAction);
        menu.addSeparator();
        menu.add(_viewBreakpointsAction);
        menu.addSeparator();
        menu.add(_toggleBytecodeBreakpointAction);
        menu.add(_pauseAction);
        return menu;
    }

    public final class ViewThreadsAction extends InspectorAction {

        ViewThreadsAction() {
            super(_inspection, "Threads");
        }

        @Override
        protected void procedure() {
            ThreadsInspector.make(_inspection);
        }
    }

    private final ViewThreadsAction _viewThreadsAction;

    public final class ViewRegistersAction extends InspectorAction {

        ViewRegistersAction() {
            super(_inspection, "Registers");
        }

        @Override
        protected void procedure() {
            RegistersInspector.make(_inspection, focus().thread());
        }
    }

    private final ViewRegistersAction _viewRegistersAction;

    public final class ViewStackAction extends InspectorAction {

        ViewStackAction() {
            super(_inspection, "Stack");
        }

        @Override
        protected void procedure() {
            StackInspector.make(_inspection, focus().thread());
        }
    }

    private final ViewStackAction _viewStackAction;

    public final class ViewMethodCodeAction extends InspectorAction {

        ViewMethodCodeAction() {
            super(_inspection, "Method Code");
        }

        @Override
        protected void procedure() {
            final TeleCodeLocation teleCodeLocation = new TeleCodeLocation(teleVM(), focus().thread().instructionPointer());
            focus().setCodeLocation(teleCodeLocation, true);
        }
    }

    private final ViewMethodCodeAction _viewMethodCodeAction;

    public final class ViewBootImageAction extends InspectorAction {

        ViewBootImageAction() {
            super(_inspection, "VM Boot Image Info");
        }

        @Override
        protected void procedure() {
            BootImageInspector.make(_inspection);
        }
    }

    private JMenu createViewMenu() {
        final JMenu menu = new JMenu("View");
        menu.add(new ViewBootImageAction());
        if (_inspection.hasProcess()) {
            menu.add(_viewThreadsAction);
            menu.add(_viewRegistersAction);
            menu.add(_viewStackAction);
            menu.add(_viewMethodCodeAction);
            menu.add(_viewBreakpointsAction);
        }
        return menu;
    }

    public final class SetVMTraceLevelAction extends InspectorAction {

        SetVMTraceLevelAction() {
            super(_inspection, "Set VM Trace Level");
        }

        @Override
        protected void procedure() {
            final TeleVMTrace teleVMTrace = _inspection.teleVMTrace();
            final int oldLevel = teleVMTrace.readTraceLevel();
            int newLevel = oldLevel;
            final String input = _inspection.inputDialog("Set VM Trace Level", Integer.toString(oldLevel));
            try {
                newLevel = Integer.parseInt(input);
            } catch (NumberFormatException numberFormatException) {
                _inspection.errorMessage(numberFormatException.toString());
            }
            if (newLevel != oldLevel) {
                teleVMTrace.writeTraceLevel(newLevel);
            }
        }
    }

    private final SetVMTraceLevelAction _setVMTraceLevelAction;

    public final class SetVMTraceThresholdAction extends InspectorAction {

        SetVMTraceThresholdAction() {
            super(_inspection, "Set VM Trace Threshold");
        }

        @Override
        protected void procedure() {
            final TeleVMTrace teleVMTrace = _inspection.teleVMTrace();
            final long oldThreshold = teleVMTrace.readTraceThreshold();
            long newThreshold = oldThreshold;
            final String input = _inspection.inputDialog("Set VM Trace Threshold", Long.toString(oldThreshold));
            try {
                newThreshold = Long.parseLong(input);
            } catch (NumberFormatException numberFormatException) {
                _inspection.errorMessage(numberFormatException.toString());
            }
            if (newThreshold != oldThreshold) {
                teleVMTrace.writeTraceThreshold(newThreshold);
            }
        }
    }

    private final SetVMTraceThresholdAction _setVMTraceThresholdAction;

    public final class InspectJavaFrameDescriptorAction extends InspectorAction {

        InspectJavaFrameDescriptorAction() {
            super(_inspection, "Inspect Java Frame Descriptor");
        }

        private TargetJavaFrameDescriptor _targetJavaFrameDescriptor;
        private TargetABI _abi;

        /**
         * @return // TODO: what does this return?
         */
        boolean update() {
            final Address instructionAddress = focus().codeLocation().targetCodeInstructionAddresss();
            if (instructionAddress.isZero()) {
                return false;
            }
            final TeleTargetMethod teleTargetMethod = TeleTargetMethod.make(teleVM(), instructionAddress);
            if (teleTargetMethod != null) {
                final int stopIndex = teleTargetMethod.getJavaStopIndex(instructionAddress);
                if (stopIndex >= 0) {
                    _targetJavaFrameDescriptor = teleTargetMethod.getJavaFrameDescriptor(stopIndex);
                    if (_targetJavaFrameDescriptor == null) {
                        return false;
                    }
                    _abi = teleTargetMethod.getAbi();
                    return true;
                }
            }
            _targetJavaFrameDescriptor = null;
            _abi = null;
            return false;
        }

        @Override
        protected void procedure() {
            assert _targetJavaFrameDescriptor != null;
            TargetJavaFrameDescriptorInspector.make(_inspection, _targetJavaFrameDescriptor, _abi);
        }
    }

    private final InspectJavaFrameDescriptorAction _inspectJavaFrameDescriptorAction;

    private JMenu createJavaMenu() {
        final JMenu menu = new JMenu("Java");
        menu.add(_setVMTraceLevelAction);
        menu.add(_setVMTraceThresholdAction);
        menu.addSeparator();
        menu.add(_inspectJavaFrameDescriptorAction);
        return menu;
    }

    private JMenuItem _disassembleAllMenuItem;
    private Thread _disassembleAllThread;

    public final class DisassembleAllAction extends InspectorAction {

        private static final String START_TEXT = "Start Disassembling All Methods";
        private static final String STOP_TEXT = "Stop Disassembling All Methods";

        DisassembleAllAction() {
            super(_inspection, START_TEXT);
        }

        private void stopThread() {
            synchronized (_inspection) {
                _disassembleAllThread = null;
                _disassembleAllMenuItem.setText(START_TEXT);
            }
        }

        @Override
        protected void procedure() {
            if (_disassembleAllThread == null) {
                _disassembleAllThread = new Thread() {

                    @Override
                    public void run() {
                        int n = 0;
                        for (TargetCodeRegion targetCodeRegion : teleVM().teleCodeRegistry().targetCodeRegions()) {
                            if (_disassembleAllThread == null) {
                                return;
                            }
                            try {
                                if (!(targetCodeRegion.teleTargetRoutine() instanceof TeleTargetMethod)) {
                                    continue;
                                }
                                final TeleTargetMethod teleTargetMethod = (TeleTargetMethod) targetCodeRegion.teleTargetRoutine();
                                teleTargetMethod.getInstructions();
                            } catch (Throwable throwable) {
                                System.err.println(throwable);
                            }
                            if (n % 100 == 0) {
                                System.out.println("disassembling: " + n);
                            }
                            n++;
                        }
                        stopThread();
                        System.out.println("Done disassembling all methods: " + n);
                    }
                };
                _disassembleAllThread.start();
                _disassembleAllMenuItem.setText(STOP_TEXT);
            } else {
                stopThread();
            }
        }
    }

    public final class ListCodeRegistryToFileAction extends InspectorAction {

        ListCodeRegistryToFileAction() {
            super(_inspection, "List Code Registry contents to File");
        }

        @Override
        protected void procedure() {
            //Create a file chooser
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
            fileChooser.setDialogTitle("Save TeleCodeRegistry summary to file:");
            final int returnVal = fileChooser.showSaveDialog(_inspection);
            if (returnVal != JFileChooser.APPROVE_OPTION) {
                return;
            }
            final File file = fileChooser.getSelectedFile();
            if (file.exists()) {
                final int n = JOptionPane.showConfirmDialog(
                                _inspection,
                                "File " + file + "exists.  Overwrite?\n",
                                "Overwrite?",
                                JOptionPane.YES_NO_OPTION);
                if (n != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            try {
                final PrintStream printStream = new PrintStream(new FileOutputStream(file, false));
                teleVM().teleCodeRegistry().writeSummaryToStream(printStream);
            } catch (FileNotFoundException fileNotFoundException) {
                _inspection.errorMessage("Unable to open " + file + " for writing:" + fileNotFoundException);
            }
        }
    }

    public final class ListCodeRegistryAction extends InspectorAction {

        ListCodeRegistryAction() {
            super(_inspection, "List Code Registry contents");
        }

        @Override
        protected void procedure() {
            teleVM().teleCodeRegistry().writeSummaryToStream(System.out);
        }
    }

    private JMenu createTestMenu() {
        final JMenu menu = new JMenu("Test");
        _disassembleAllMenuItem = menu.add(new DisassembleAllAction());
        _showStackFramesMenuItem = menu.add(new ShowStackFramesAction());
        menu.add(new ListCodeRegistryAction());
        menu.add(new ListCodeRegistryToFileAction());
        return menu;
    }

    public final class AboutDialogAction extends InspectorAction {

        public AboutDialogAction() {
            super(_inspection, "About");
        }

        @Override
        public void procedure() {
            new AboutDialog(_inspection);
        }
    }

    public final class PreferenceDialogAction extends InspectorAction {

        public PreferenceDialogAction() {
            super(_inspection, "Preferences...");
        }

        @Override
        public void procedure() {
            new PreferenceDialog(_inspection);
        }
    }

    private JMenu createHelpMenu() {
        final JMenu menu = new JMenu("Help");
        menu.add(new AboutDialogAction());
        return menu;
    }

    public JMenuBar createJMenuBar() {
        final JMenuBar menuBar = new JMenuBar();
        menuBar.add(createInspectionMenu());
        menuBar.add(createClassMenu());
        menuBar.add(createObjectMenu());
        menuBar.add(createMemoryMenu());
        menuBar.add(createMethodMenu());
        if (_inspection.hasProcess()) {
            menuBar.add(createDebugMenu());
        }
        menuBar.add(createViewMenu());
        menuBar.add(createJavaMenu());
        menuBar.add(createTestMenu());
        menuBar.add(createHelpMenu());
        return menuBar;
    }

    /**
     * Updates the enabled/disabled state of actions.
     */
    private void updateMenuItems() {
        final boolean hasProcess = _inspection.hasProcess();
        final boolean hasThread = hasProcess && focus().hasThread();
        final TeleCodeLocation selection = focus().codeLocation();
        final boolean hasThreadAndReadyToRun = hasThread && !_inspection.isVMRunning();
        final boolean hasThreadAndSelectedCodeAndReadyToRun = focus().hasCodeLocation() && hasThreadAndReadyToRun;
        _singleStepAction.setEnabled(hasThreadAndReadyToRun);
        _stepOverAction.setEnabled(hasThreadAndReadyToRun);
        _stepOverWithBreakpointsAction.setEnabled(hasThreadAndReadyToRun);
        _returnFromFrameAction.setEnabled(hasThreadAndReadyToRun);
        _returnFromFrameWithBreakpointsAction.setEnabled(hasThreadAndReadyToRun);
        _runToInstructionAction.setEnabled(hasThreadAndSelectedCodeAndReadyToRun);
        _runToInstructionWithBreakpointsAction.setEnabled(hasThreadAndSelectedCodeAndReadyToRun);
        _toggleTargetBreakpointAction.setEnabled(hasThread && selection.hasTargetCodeLocation());
        _breakAtMethodAction.setEnabled(hasThread && teleVM().messenger().activate());
        _breakAtMethodKeyAction.setEnabled(hasThread && teleVM().messenger().activate());
        _setLabelBreakpointsAction.setEnabled(hasThread && selection.hasTargetCodeLocation());
        _clearLabelBreakpointsAction.setEnabled(hasThread && selection.hasTargetCodeLocation());
        _clearAllBreakpointsAction.setEnabled(hasThread && teleProcess().targetBreakpointFactory().breakpoints(true).iterator().hasNext());
        _clearSelectedBreakpointAction.setEnabled(hasThread && focus().hasBreakpoint());
        _viewThreadsAction.setEnabled(hasProcess);
        _viewRegistersAction.setEnabled(hasThread);
        _viewStackAction.setEnabled(hasThread);
        _setVMTraceLevelAction.setEnabled(hasProcess);
        _setVMTraceThresholdAction.setEnabled(hasProcess);
        _inspectJavaFrameDescriptorAction.setEnabled(_inspectJavaFrameDescriptorAction.update());
        _resumeAction.setEnabled(hasThreadAndReadyToRun);
        _runToInstructionAction.setEnabled(hasThreadAndReadyToRun);
        _returnFromFrameWithBreakpointsAction.setEnabled(hasThreadAndReadyToRun);
        _runToInstructionWithBreakpointsAction.setEnabled(hasThreadAndReadyToRun);
        _toggleBytecodeBreakpointAction.setEnabled(hasThread && selection.hasBytecodeLocation());
        _pauseAction.setEnabled(hasThread && !isSynchronousMode() && _inspection.isVMRunning());
    }

    InspectionMenus(Inspection inspection) {
        _inspection = inspection;
        final Header header = teleVM().bootImage().header();
        // create new singleton actions; sorted alphabetically here
        _breakAtMethodAction = new BreakAtMethodAction();
        _breakAtMethodKeyAction = new BreakAtMethodKeyAction();
        _breakAtObjectInitializersAction = new BreakAtObjectInitializersAction();
        _breakAtTargetMethodAction = new BreakAtTargetMethodAction();
        _clearAllBreakpointsAction = new ClearAllBreakpointsAction();
        _clearLabelBreakpointsAction = new ClearLabelBreakpointsAction();
        _clearSelectedBreakpointAction = new ClearSelectedBreakpointAction();
        _inspectJavaFrameDescriptorAction = new InspectJavaFrameDescriptorAction();
        _inspectMethodAction = new InspectMethodAction();
        _pauseAction = new PauseAction();
        _resumeAction = new ResumeAction();
        _returnFromFrameAction = new ReturnFromFrameAction();
        _returnFromFrameWithBreakpointsAction = new ReturnFromFrameWithBreakpointsAction();
        _runToInstructionAction = new RunToInstructionAction();
        _runToInstructionWithBreakpointsAction = new RunToInstructionWithBreakpointsAction();
        _setLabelBreakpointsAction = new SetLabelBreakpointsAction();
        _setVMTraceLevelAction = new SetVMTraceLevelAction();
        _setVMTraceThresholdAction = new SetVMTraceThresholdAction();
        _singleStepAction = new SingleStepAction();
        _stepOverAction = new StepOverAction();
        _stepOverWithBreakpointsAction = new StepOverWithBreakpointsAction();
        _toggleBytecodeBreakpointAction = new ToggleBytecodeBreakpointAction();
        _toggleTargetBreakpointAction = new ToggleTargetBreakpointAction();
        _viewBootImageRunMethodCodeAction = new ViewBootImageMethodCodeAction(header._vmRunMethodOffset, MaxineVM.class, "run", MaxineVM.runMethodParameterTypes());
        _viewBootImageSchemeRunMethodCodeAction = new ViewBootImageMethodCodeAction(header._runSchemeRunMethodOffset, teleVM().vmConfiguration().runPackage().schemeTypeToImplementation(RunScheme.class), "run");
        _viewBootImageThreadRunMethodCodeAction = new ViewBootImageMethodCodeAction(header._vmThreadRunMethodOffset, VmThread.class, "run", int.class, Address.class, Pointer.class,
                        Pointer.class, Pointer.class, Pointer.class, Pointer.class, Pointer.class, Pointer.class, Pointer.class);
        _viewBreakpointsAction = new ViewBreakpointsAction();
        _viewCodeAtIPAction = new ViewCodeAtIPAction();
        _viewMethodBytecodeAction = new ViewMethodBytecodeAction();
        _viewMethodCodeAction = new ViewMethodCodeAction();
        _viewMethodCodeContainingAddressAction = new ViewMethodCodeContainingAddressAction();
        _viewMethodTargetCodeAction = new ViewMethodTargetCodeAction();
        _viewNativeCodeContainingAddressAction = new ViewNativeCodeContainingAddressAction();
        _viewRegistersAction = new ViewRegistersAction();
        _viewSelectedCodeAction = new ViewSelectedCodeAction();
        _viewStackAction = new ViewStackAction();
        _viewThreadsAction = new ViewThreadsAction();

        // Listen for changes of user (view state) focus
        focus().addListener(_focusListener);
    }

    public void refresh(long epoch, boolean force) {
        updateMenuItems();
    }

    public void redisplay() {
        updateMenuItems();
    }

    private final ViewFocusListener _focusListener = new ViewFocusListener() {

        public void codeLocationFocusSet(TeleCodeLocation teleCodeLocation, boolean interactiveForNative) {
            updateMenuItems();
        }

        public void threadFocusSet(TeleNativeThread oldTeleNativeThread, TeleNativeThread teleNativeThread) {
            updateMenuItems();
        }

        public void stackFrameFocusChanged(StackFrame oldStackFrame, TeleNativeThread threadForStackFrame, StackFrame stackFrame) {
            updateMenuItems();
        }

        public void breakpointFocusSet(TeleBreakpoint oldTeleBreakpoint, TeleBreakpoint teleBreakpoint) {
            updateMenuItems();
        }

        public void heapObjectFocusChanged(TeleObject oldTeleObject, TeleObject teleObject) {
            updateMenuItems();
        }
    };

    /**
     * Action that copies a hex string version of a word to the system clipboard.
     */
    public final class CopyWordAction extends InspectorAction {

        private final Word _word;

        private CopyWordAction(Word word) {
            this(word, "Copy word to clipboard");
        }

        private CopyWordAction(Word word, String title) {
            super(_inspection, title);
            _word = word;
        }

        @Override
        public void procedure() {
            final Clipboard clipboard = _inspection.getToolkit().getSystemClipboard();
            final StringSelection selection = new StringSelection(_word.toHexString());
            clipboard.setContents(selection, selection);
        }
    }

    /**
     * @param a tele VM {@link Word}
     * @return an Action that copies the word's text value in hex to the system clipboard
     */
    public InspectorAction getCopyWordAction(Word word) {
        return new CopyWordAction(word);
    }

    /**
     * @param a tele VM {@link Word}
     * @param title a string to use as the title of the action
     * @return an Action that copies the word's text value in hex to the system clipboard
     */
    public InspectorAction getCopyWordAction(Word word, String title) {
        return new CopyWordAction(word, title);
    }

    public InspectorAction getCopyValueAction(Value value, String title) {
        Word word = Word.zero();
        try {
            word = value.asWord();
        } catch (Throwable throwable) {
        }
        final InspectorAction action = new CopyWordAction(word, title);
        if (word.isZero()) {
            action.setEnabled(false);
        }
        return action;
    }

    /**
     * Action to create an inspector for a specific heap object in the tele VM.
     */
    public final class InspectObjectAction extends InspectorAction {

        final TeleObject _teleObject;

        InspectObjectAction(TeleObject teleObject, String title) {
            super(_inspection, title);
            _teleObject = teleObject;
        }

        InspectObjectAction(TeleObject teleObject) {
            this(teleObject, "Inspect Object");
        }

        @Override
        protected void procedure() {
            focus().setHeapObject(_teleObject);
        }
    }

    /**
     * @param surrogate for an object in the tele VM
     * @return an Action that will create an Object Inspector
     */
    public InspectorAction getInspectObjectAction(TeleObject teleObject) {
        return new InspectObjectAction(teleObject);
    }

    /**
     * @param surrogate for an object in the tele VM
     * @param title a string name for the Action
     * @return an Action that will create an Object Inspector
     */
    public InspectorAction getInspectObjectAction(TeleObject teleObject, String title) {
        return new InspectObjectAction(teleObject, title);
    }

}
