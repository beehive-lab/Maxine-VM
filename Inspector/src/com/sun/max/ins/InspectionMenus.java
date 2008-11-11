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

import java.io.*;

import javax.swing.*;

import com.sun.max.collect.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.java.*;
import com.sun.max.ins.memory.*;
import com.sun.max.ins.method.*;
import com.sun.max.ins.type.*;
import com.sun.max.memory.*;
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
import com.sun.max.vm.run.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * General menus offered by an Inspection.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 * @author Aritra Bandyopadhyay
 */
public final class InspectionMenus extends InspectionHolder implements Prober {

    private TeleProcessController teleProcessController() {
        return inspection().controller();
    }

    private boolean isSynchronousMode() {
        return inspection().isSynchronousMode();
    }

    private final JMenu _inspectionMenu = new JMenu("Inspector");

    private JMenu createInspectionMenu() {
        if (MaxineInspector.suspendingBeforeRelocating()) {
            _inspectionMenu.add(actions().relocateBootImage());
            _inspectionMenu.addSeparator();
        }
        _inspectionMenu.add(actions().setInspectorTraceLevel());
        _inspectionMenu.add(actions().changeInterpreterUseLevel());
        _inspectionMenu.add(actions().setTransportDebugLevel());
        _inspectionMenu.add(actions().runFileCommands());
        _inspectionMenu.add(actions().updateClasspathTypes());
        _inspectionMenu.addSeparator();
        _inspectionMenu.add(actions().refreshAll());
        _inspectionMenu.addSeparator();
        _inspectionMenu.add(actions().closeAll());
        _inspectionMenu.addSeparator();
        _inspectionMenu.add(new PreferenceDialogAction());
        _inspectionMenu.addSeparator();
        _inspectionMenu.add(actions().quit());
        return _inspectionMenu;
    }




    private JMenu createClassMenu() {
        final JMenu menu = new JMenu("Class");
        menu.add(actions().inspectClassActorByName());
        menu.add(actions().inspectClassActorByHexId());
        menu.add(actions().inspectClassActorByDecimalId());
        return menu;
    }

    private JMenu createObjectMenu() {
        final JMenu menu = new JMenu("Object");
        menu.add(actions().inspectBootClassRegistry());
        menu.add(actions().inspectObject());
        menu.add(actions().inspectObjectByID());
        return menu;
    }

    private JMenu createMemoryMenu() {
        final JMenu menu = new JMenu("Memory");

        final JMenu wordsMenu = new JMenu("As words");
        wordsMenu.add(actions().inspectBootHeapMemoryWords());
        wordsMenu.add(actions().inspectBootCodeMemoryWords());
        wordsMenu.add(actions().inspectMemoryWords());
        menu.add(wordsMenu);

        final JMenu bytesMenu = new JMenu("As bytes");
        bytesMenu.add(actions().inspectBootHeapMemory());
        bytesMenu.add(actions().inspectBootCodeMemory());
        bytesMenu.add(actions().inspectMemory());
        menu.add(bytesMenu);
        return menu;
    }

    public final class InspectMethodAction extends InspectorAction {

        public InspectMethodAction() {
            super(inspection(), "Inspect MethodActor...");
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(inspection(), "Inspect MethodActor in Class...", "Select");
            if (teleClassActor != null) {
                final TeleMethodActor teleMethodActor = MethodActorSearchDialog.show(inspection(), teleClassActor, "Inspect MethodActor...", "Inspect");
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
            super(inspection(), "View Current Code Selection");
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
            super(inspection(), "View Code at current IP");
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
            super(inspection(), "View Bytecodes for Method...");
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(inspection(), "View Bytecode for Method in Class...", "Select");
            if (teleClassActor != null) {
                final Predicate<TeleMethodActor> hasBytecodesPredicate = new Predicate<TeleMethodActor>() {

                    @Override
                    public boolean evaluate(TeleMethodActor teleMethodActor) {
                        return teleMethodActor.hasCodeAttribute();
                    }
                };
                final TeleMethodActor teleMethodActor = MethodActorSearchDialog.show(inspection(), teleClassActor, hasBytecodesPredicate, "View Bytecode for Method...", "Inspect");
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
            super(inspection(), "View Code for Method...");
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(inspection(), "View Target Code for Class...", "Select");
            if (teleClassActor != null) {
                final Sequence<TeleTargetMethod> teleTargetMethods = TargetMethodSearchDialog.show(inspection(), teleClassActor, "View Target Code for Method...", "View Code", false);
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
            super(inspection(), clazz.getName() + "." + name + SignatureDescriptor.fromJava(Void.TYPE, parameterTypes).toJavaString(false, false));
            _offset = offset;
        }

        @Override
        protected void procedure() {
            focus().setCodeLocation(new TeleCodeLocation(teleVM(), teleVM().bootImageStart().plus(_offset)), true);
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
            super(inspection(), "View Method Code Containing Address...");
        }

        @Override
        protected void procedure() {
            new AddressInputDialog(inspection(), teleVM().bootImageStart(), "View Method Code Containing address...", "View Code") {

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
            super(inspection(), "View Native Code Containing Code Address...");
        }

        @Override
        protected void procedure() {
            // Most likely situation is that we are just about to call a native method in which case RAX is the address
            final TeleNativeThread teleNativeThread = focus().thread();
            assert teleNativeThread != null;
            final Address indirectCallAddress = teleNativeThread.integerRegisters().getCallRegisterValue();
            final Address initialAddress = indirectCallAddress == null ? teleVM().bootImageStart() : indirectCallAddress;
            new AddressInputDialog(inspection(), initialAddress, "View Native Code Containing Code Address...", "View Code") {
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
            super(inspection(), "Single Instruction Step", true);
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
            super(inspection(), "Run To Selected Instruction (ignoring breakpoints)", true);
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
                inspection().errorMessage("No instruction selected");
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
            super(inspection(), "Run To Selected Instruction", true);
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
                inspection().errorMessage("No instruction selected");
            }
        }
    }

    private final RunToInstructionWithBreakpointsAction _runToInstructionWithBreakpointsAction;

    public final class ReturnFromFrameAction extends InspectorAction {

        ReturnFromFrameAction() {
            super(inspection(), "Return From Frame (ignoring breakpoints)", true);
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
            super(inspection(), "Return From Frame", true);
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
            super(inspection(), "Step Over (ignoring breakpoints)", true);
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
            super(inspection(), "Step Over", true);
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



    private JMenuItem _showStackFramesMenuItem;

    public final class ShowStackFramesAction extends InspectorAction {

        private static final String MENU_TEXT = "Show All Stack Frames";

        ShowStackFramesAction() {
            super(inspection(), "Show All Stack Frames");
        }

        @Override
        protected void procedure() {
            _showStackFramesMenuItem.setText(MENU_TEXT);
        }
    }

    public final class ResumeAction extends InspectorAction {

        ResumeAction() {
            super(inspection(), "Resume", true);
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
            super(inspection(), "Pause Process");
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

    public final class SetLabelBreakpointsAction extends InspectorAction {

        SetLabelBreakpointsAction() {
            super(inspection(), "Set Breakpoint at Every Target Code Label");
        }

        @Override
        protected void procedure() {
            final Address address = focus().codeLocation().targetCodeInstructionAddresss();
            final TeleTargetRoutine teleTargetRoutine = teleVM().teleCodeRegistry().get(TeleTargetRoutine.class, address);
            if (teleTargetRoutine != null) {
                teleTargetRoutine.setTargetCodeLabelBreakpoints();
            } else {
                inspection().errorMessage("Unable to find target routine in which to set breakpoints");
            }
        }
    }

    private final SetLabelBreakpointsAction _setLabelBreakpointsAction;

    public final class RemoveLabelBreakpointsAction extends InspectorAction {

        RemoveLabelBreakpointsAction() {
            super(inspection(), "Remove Breakpoints at All Target Code Labels");
        }

        @Override
        protected void procedure() {
            final Address address = focus().codeLocation().targetCodeInstructionAddresss();
            final TeleTargetRoutine teleTargetRoutine = teleVM().teleCodeRegistry().get(TeleTargetRoutine.class, address);
            if (teleTargetRoutine != null) {
                teleTargetRoutine.removeTargetCodeLabelBreakpoints();
            }
        }
    }

    private final RemoveLabelBreakpointsAction _removeLabelBreakpointsAction;

    public final class RemoveSelectedBreakpointAction extends InspectorAction {

        RemoveSelectedBreakpointAction() {
            super(inspection(), "Remove Selected Breakpoint");
        }

        @Override
        protected void procedure() {
            final TeleBreakpoint selectedTeleBreakpoint = focus().breakpoint();
            if (selectedTeleBreakpoint != null) {
                focus().setBreakpoint(null);
                selectedTeleBreakpoint.remove();
            } else {
                inspection().errorMessage("No breakpoint selected");
            }
        }
    }

    private final RemoveSelectedBreakpointAction _removeSelectedBreakpointAction;

    /**
     * @return an Action that will remove the currently selected breakpoint
     */
    public InspectorAction getRemoveSelectedBreakpointAction() {
        return _removeSelectedBreakpointAction;
    }

    public final class RemoveAllBreakpointsAction extends InspectorAction {

        RemoveAllBreakpointsAction() {
            super(inspection(), "Remove All Breakpoints");
        }

        @Override
        protected void procedure() {
            focus().setBreakpoint(null);
            teleProcess().targetBreakpointFactory().removeAllBreakpoints();
        }
    }

    private final RemoveAllBreakpointsAction _removeAllBreakpointsAction;

    /**
     * @return an Action that will remove all existing breakpoints
     */
    public InspectorAction getRemoveAllBreakpointsAction() {
        return _removeAllBreakpointsAction;
    }

    public final class ViewBreakpointsAction extends InspectorAction {

        ViewBreakpointsAction() {
            super(inspection(), "View Breakpoints");
        }

        @Override
        protected void procedure() {
            BreakpointsInspector.make(inspection());
        }
    }

    private final ViewBreakpointsAction _viewBreakpointsAction;

    public final class BreakAtTargetMethodAction extends InspectorAction {

        BreakAtTargetMethodAction() {
            super(inspection(), "Compiled Method...");
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(inspection(), "Class for Compiled Method Entry Breakpoints...", "Select");
            if (teleClassActor != null) {
                final Sequence<TeleTargetMethod> teleTargetMethods = TargetMethodSearchDialog.show(inspection(), teleClassActor, "Compiled Method Entry Breakpoints", "Set Breakpoints", true);
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
            super(inspection(), "Method on Classpath...");
        }

        @Override
        protected void procedure() {
            final TypeDescriptor typeDescriptor = TypeSearchDialog.show(inspection(), "Class for Bytecode Method Entry Breakpoint...", "Select");
            if (typeDescriptor != null) {
                final MethodKey methodKey = MethodSearchDialog.show(inspection(), typeDescriptor, "Bytecode Method Entry Breakpoint", "Set Breakpoint");
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
            super(inspection(), "Method Matched by Key...");
        }

        @Override
        protected void procedure() {
            final MethodKey methodKey = MethodKeyInputDialog.show(inspection(), "Specify Method");
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
            super(inspection(), "Break in Compiled Object Initializers of Class...");
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(inspection(), "Break in Object Initializers of Class...", "Set Breakpoint");
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
            super(inspection(), "Toggle Bytecode Breakpoint");
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
        menu.add(actions().toggleTargetCodeBreakpoint());
        menu.add(_setLabelBreakpointsAction);
        menu.add(_removeLabelBreakpointsAction);
        final JMenu methodEntryBreakpoints = new JMenu("Break at Method Entry");
        methodEntryBreakpoints.add(_breakAtTargetMethodAction);
        methodEntryBreakpoints.add(_breakAtMethodAction);
        methodEntryBreakpoints.add(_breakAtMethodKeyAction);
        menu.add(methodEntryBreakpoints);
        menu.add(_breakAtObjectInitializersAction);
        menu.add(_removeAllBreakpointsAction);
        menu.addSeparator();
        menu.add(_viewBreakpointsAction);
        menu.addSeparator();
        menu.add(_toggleBytecodeBreakpointAction);
        menu.add(_pauseAction);
        return menu;
    }

    public final class ViewThreadsAction extends InspectorAction {

        ViewThreadsAction() {
            super(inspection(), "Threads");
        }

        @Override
        protected void procedure() {
            ThreadsInspector.make(inspection());
        }
    }

    private final ViewThreadsAction _viewThreadsAction;

    public final class ViewRegistersAction extends InspectorAction {

        ViewRegistersAction() {
            super(inspection(), "Registers");
        }

        @Override
        protected void procedure() {
            RegistersInspector.make(inspection(), focus().thread());
        }
    }

    private final ViewRegistersAction _viewRegistersAction;

    public final class ViewStackAction extends InspectorAction {

        ViewStackAction() {
            super(inspection(), "Stack");
        }

        @Override
        protected void procedure() {
            StackInspector.make(inspection(), focus().thread());
        }
    }

    private final ViewStackAction _viewStackAction;

    public final class ViewMethodCodeAction extends InspectorAction {

        ViewMethodCodeAction() {
            super(inspection(), "Method Code");
        }

        @Override
        protected void procedure() {
            final TeleCodeLocation teleCodeLocation = new TeleCodeLocation(teleVM(), focus().thread().instructionPointer());
            focus().setCodeLocation(teleCodeLocation, true);
        }
    }

    private final ViewMethodCodeAction _viewMethodCodeAction;

    public final class ViewMemoryRegionsAction extends InspectorAction {

        ViewMemoryRegionsAction() {
            super(inspection(), "MemoryRegions");
        }

        @Override
        protected void procedure() {
            MemoryRegionsInspector.make(inspection());
        }
    }

    private final ViewMemoryRegionsAction _viewMemoryRegionsAction;

    public final class ViewBootImageAction extends InspectorAction {

        ViewBootImageAction() {
            super(inspection(), "VM Boot Image Info");
        }

        @Override
        protected void procedure() {
            BootImageInspector.make(inspection());
        }
    }

    private JMenu createViewMenu() {
        final JMenu menu = new JMenu("View");
        menu.add(new ViewBootImageAction());
        if (inspection().hasProcess()) {
            menu.add(_viewThreadsAction);
            menu.add(_viewRegistersAction);
            menu.add(_viewStackAction);
            menu.add(_viewMethodCodeAction);
            menu.add(_viewBreakpointsAction);
            menu.add(_viewMemoryRegionsAction);
        }
        return menu;
    }

    public final class SetVMTraceLevelAction extends InspectorAction {

        SetVMTraceLevelAction() {
            super(inspection(), "Set VM Trace Level");
        }

        @Override
        protected void procedure() {
            final TeleVMTrace teleVMTrace = inspection().teleVMTrace();
            final int oldLevel = teleVMTrace.readTraceLevel();
            int newLevel = oldLevel;
            final String input = inspection().inputDialog("Set VM Trace Level", Integer.toString(oldLevel));
            try {
                newLevel = Integer.parseInt(input);
            } catch (NumberFormatException numberFormatException) {
                inspection().errorMessage(numberFormatException.toString());
            }
            if (newLevel != oldLevel) {
                teleVMTrace.writeTraceLevel(newLevel);
            }
        }
    }

    private final SetVMTraceLevelAction _setVMTraceLevelAction;

    public final class SetVMTraceThresholdAction extends InspectorAction {

        SetVMTraceThresholdAction() {
            super(inspection(), "Set VM Trace Threshold");
        }

        @Override
        protected void procedure() {
            final TeleVMTrace teleVMTrace = inspection().teleVMTrace();
            final long oldThreshold = teleVMTrace.readTraceThreshold();
            long newThreshold = oldThreshold;
            final String input = inspection().inputDialog("Set VM Trace Threshold", Long.toString(oldThreshold));
            try {
                newThreshold = Long.parseLong(input);
            } catch (NumberFormatException numberFormatException) {
                inspection().errorMessage(numberFormatException.toString());
            }
            if (newThreshold != oldThreshold) {
                teleVMTrace.writeTraceThreshold(newThreshold);
            }
        }
    }

    private final SetVMTraceThresholdAction _setVMTraceThresholdAction;

    public final class InspectJavaFrameDescriptorAction extends InspectorAction {

        InspectJavaFrameDescriptorAction() {
            super(inspection(), "Inspect Java Frame Descriptor");
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
            TargetJavaFrameDescriptorInspector.make(inspection(), _targetJavaFrameDescriptor, _abi);
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
            super(inspection(), START_TEXT);
        }

        private void stopThread() {
            synchronized (inspection()) {
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
            super(inspection(), "List Code Registry contents to File");
        }

        @Override
        protected void procedure() {
            //Create a file chooser
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
            fileChooser.setDialogTitle("Save TeleCodeRegistry summary to file:");
            final int returnVal = fileChooser.showSaveDialog(inspection());
            if (returnVal != JFileChooser.APPROVE_OPTION) {
                return;
            }
            final File file = fileChooser.getSelectedFile();
            if (file.exists()) {
                final int n = JOptionPane.showConfirmDialog(
                                inspection(),
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
                inspection().errorMessage("Unable to open " + file + " for writing:" + fileNotFoundException);
            }
        }
    }

    public final class ListCodeRegistryAction extends InspectorAction {

        ListCodeRegistryAction() {
            super(inspection(), "List Code Registry contents");
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
            super(inspection(), "About");
        }

        @Override
        public void procedure() {
            new AboutDialog(inspection());
        }
    }

    public final class PreferenceDialogAction extends InspectorAction {

        public PreferenceDialogAction() {
            super(inspection(), "Preferences...");
        }

        @Override
        public void procedure() {
            new PreferenceDialog(inspection());
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
        if (inspection().hasProcess()) {
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
        final boolean hasProcess = inspection().hasProcess();
        final boolean hasThread = hasProcess && focus().hasThread();
        final TeleCodeLocation selection = focus().codeLocation();
        final boolean hasThreadAndReadyToRun = hasThread && !inspection().isVMRunning();
        final boolean hasThreadAndSelectedCodeAndReadyToRun = focus().hasCodeLocation() && hasThreadAndReadyToRun;
        _singleStepAction.setEnabled(hasThreadAndReadyToRun);
        _stepOverAction.setEnabled(hasThreadAndReadyToRun);
        _stepOverWithBreakpointsAction.setEnabled(hasThreadAndReadyToRun);
        _returnFromFrameAction.setEnabled(hasThreadAndReadyToRun);
        _returnFromFrameWithBreakpointsAction.setEnabled(hasThreadAndReadyToRun);
        _runToInstructionAction.setEnabled(hasThreadAndSelectedCodeAndReadyToRun);
        _runToInstructionWithBreakpointsAction.setEnabled(hasThreadAndSelectedCodeAndReadyToRun);
        _breakAtMethodAction.setEnabled(hasThread && teleVM().messenger().activate());
        _breakAtMethodKeyAction.setEnabled(hasThread && teleVM().messenger().activate());
        _setLabelBreakpointsAction.setEnabled(hasThread && selection.hasTargetCodeLocation());
        _removeLabelBreakpointsAction.setEnabled(hasThread && selection.hasTargetCodeLocation());
        _removeAllBreakpointsAction.setEnabled(hasThread && teleProcess().targetBreakpointFactory().breakpoints(true).iterator().hasNext());
        _removeSelectedBreakpointAction.setEnabled(hasThread && focus().hasBreakpoint());
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
        _pauseAction.setEnabled(hasThread && !isSynchronousMode() && inspection().isVMRunning());
    }

    InspectionMenus(Inspection inspection) {
        super(inspection);
        final Header header = teleVM().bootImage().header();
        // create new singleton actions; sorted alphabetically here
        _breakAtMethodAction = new BreakAtMethodAction();
        _breakAtMethodKeyAction = new BreakAtMethodKeyAction();
        _breakAtObjectInitializersAction = new BreakAtObjectInitializersAction();
        _breakAtTargetMethodAction = new BreakAtTargetMethodAction();
        _removeAllBreakpointsAction = new RemoveAllBreakpointsAction();
        _removeLabelBreakpointsAction = new RemoveLabelBreakpointsAction();
        _removeSelectedBreakpointAction = new RemoveSelectedBreakpointAction();
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
        _viewBootImageRunMethodCodeAction = new ViewBootImageMethodCodeAction(header._vmRunMethodOffset, MaxineVM.class, "run", MaxineVM.runMethodParameterTypes());
        _viewBootImageSchemeRunMethodCodeAction = new ViewBootImageMethodCodeAction(header._runSchemeRunMethodOffset, teleVM().vmConfiguration().runPackage().schemeTypeToImplementation(RunScheme.class), "run");
        _viewBootImageThreadRunMethodCodeAction = new ViewBootImageMethodCodeAction(header._vmThreadRunMethodOffset, VmThread.class, "run", int.class, Address.class, Pointer.class,
                        Pointer.class, Pointer.class, Pointer.class, Pointer.class, Pointer.class, Pointer.class, Pointer.class);
        _viewBreakpointsAction = new ViewBreakpointsAction();
        _viewCodeAtIPAction = new ViewCodeAtIPAction();
        _viewMethodBytecodeAction = new ViewMethodBytecodeAction();
        _viewMethodCodeAction = new ViewMethodCodeAction();
        _viewMemoryRegionsAction = new ViewMemoryRegionsAction();
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

        public void memoryRegionFocusChanged(MemoryRegion oldMemoryRegion, MemoryRegion memoryRegion) {
            updateMenuItems();
        }

        public void breakpointFocusSet(TeleBreakpoint oldTeleBreakpoint, TeleBreakpoint teleBreakpoint) {
            updateMenuItems();
        }

        public void heapObjectFocusChanged(TeleObject oldTeleObject, TeleObject teleObject) {
            updateMenuItems();
        }
    };


}
