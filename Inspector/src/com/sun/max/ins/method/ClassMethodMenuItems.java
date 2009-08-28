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
package com.sun.max.ins.method;

import java.io.*;
import java.math.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.interpreter.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.actor.member.MethodKey.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.value.*;

/**
 * Provides menu items related to a specific {@link MethodActor} in the VM.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public final class ClassMethodMenuItems implements InspectorMenuItems {

    private final Inspection inspection;

    public Inspection inspection() {
        return inspection;
    }

    private final TeleClassMethodActor teleClassMethodActor;

    private final class InspectClassMethodActorAction extends InspectorAction {
        private InspectClassMethodActorAction() {
            super(inspection(), "Inspect ClassMethodActor");
        }

        @Override
        public void procedure() {
            if (teleClassMethodActor != null) {
                inspection().focus().setHeapObject(teleClassMethodActor);
            }
        }
    }

    private final InspectClassMethodActorAction inspectClassMethodActorAction;

    private final class InspectSubstitutionSourceClassActorAction extends InspectorAction {
        private InspectSubstitutionSourceClassActorAction() {
            super(inspection(), "Inspect Method Substitution Source");
        }

        @Override
        public void procedure() {
            if (teleClassMethodActor != null) {
                inspection().focus().setHeapObject(teleClassMethodActor.teleClassActorSubstitutedFrom());
            }
        }
    }

    private final InspectSubstitutionSourceClassActorAction inspectSubstitutionSourceClassActorAction;

    private final class ViewJavaSourceAction extends InspectorAction {
        private ViewJavaSourceAction() {
            super(inspection(), "View Java Source (external)");
        }

        @Override
        public void procedure() {
            if (teleClassMethodActor != null) {
                inspection().viewSourceExternally(new BytecodeLocation(teleClassMethodActor.classMethodActor(), 0));
            }
        }
    }

    private final ViewJavaSourceAction viewJavaSourceAction;


    private final class ViewBytecodeAction extends InspectorAction {
        private ViewBytecodeAction() {
            super(inspection(), "View Bytecode");
        }

        @Override
        public void procedure() {
            if (teleClassMethodActor != null) {
                final TeleCodeLocation teleCodeLocation = maxVM().createCodeLocation(teleClassMethodActor, 0);
                inspection().focus().setCodeLocation(teleCodeLocation, false);
            }
        }
    }

    private final ViewBytecodeAction viewBytecodeAction;


    private final class BytecodeBreakOnEntryAction extends InspectorAction {
        private BytecodeBreakOnEntryAction() {
            super(inspection(), "Set Bytecode Breakpoint at Method Entry");
        }

        @Override
        public void procedure() {
            if (teleClassMethodActor != null) {
                final MethodKey methodKey = new MethodActorKey(teleClassMethodActor.classMethodActor());
                maxVM().makeBytecodeBreakpoint(new TeleBytecodeBreakpoint.Key(methodKey, 0));
            }
        }
    }

    private final BytecodeBreakOnEntryAction bytecodeBreakOnEntryAction;


    private final class InvokeMethodAction extends InspectorAction {
        private InvokeMethodAction() {
            super(inspection(), "Invoke method...");
        }

        @Override
        public void procedure() {

            if (teleClassMethodActor == null) {
                return;
            }

            ClassMethodActor classMethodActor = teleClassMethodActor.classMethodActor();
            ReferenceValue receiver = null;

            if (classMethodActor instanceof VirtualMethodActor) {
                final String input = inspection().gui().inputDialog("Argument 0 (receiver, must be a reference to a " + classMethodActor.holder() + " or subclass, origin address in hex):", "");

                if (input == null) {
                    // User clicked cancel.
                    return;
                }

                receiver = maxVM().createReferenceValue(maxVM().originToReference(Pointer.fromLong(new BigInteger(input, 16).longValue())));
                final ClassActor dynamicClass = receiver.getClassActor();
                classMethodActor = dynamicClass.findClassMethodActor(classMethodActor);
            }

            final Value[] arguments = MethodArgsDialog.getArgs(inspection(), classMethodActor, receiver);
            if (arguments == null) {
                // User clicked cancel.
                return;
            }

            try {
                final Value returnValue = maxVM().interpretMethod(classMethodActor, arguments);
                inspection().gui().informationMessage("Method " + classMethodActor.name + " returned " + returnValue.toString());
            } catch (TeleInterpreterException teleInterpreterException) {
                throw new InspectorError(teleInterpreterException);
            }
        }
    }

    private final InvokeMethodAction invokeMethodAction;

    private InspectorMenu inspectCompilationsMenu = new InspectorMenu(null, "Method Compilations");


    public ClassMethodMenuItems(Inspection inspection, TeleClassMethodActor teleClassMethodActor) {
        this.inspection = inspection;
        this.teleClassMethodActor = teleClassMethodActor;
        this.viewJavaSourceAction = new ViewJavaSourceAction();
        this.viewBytecodeAction = new ViewBytecodeAction();
        bytecodeBreakOnEntryAction = new BytecodeBreakOnEntryAction();
        invokeMethodAction = new InvokeMethodAction();
        inspectClassMethodActorAction = new InspectClassMethodActorAction();
        inspectSubstitutionSourceClassActorAction = new InspectSubstitutionSourceClassActorAction();
        refresh(true);
    }

    public void addTo(InspectorMenu menu) {

        menu.add(viewJavaSourceAction);
        menu.add(viewBytecodeAction);

        menu.addSeparator();
        menu.add(bytecodeBreakOnEntryAction);
        menu.add(invokeMethodAction);

        menu.addSeparator();
        menu.add(inspectClassMethodActorAction);
        menu.add(inspectSubstitutionSourceClassActorAction);
        menu.add(inspectCompilationsMenu);
    }

    private MaxVM maxVM() {
        return inspection.maxVM();
    }

    public void refresh(boolean force) {

        if (teleClassMethodActor == null) {
            return;
        }

        teleClassMethodActor.refreshView();
        final boolean hasCodeAttribute =  teleClassMethodActor.hasCodeAttribute();
        final File javaSourceFile = maxVM().findJavaSourceFile(teleClassMethodActor.getTeleHolder().classActor());
        viewJavaSourceAction.setEnabled(javaSourceFile != null);
        viewBytecodeAction.setEnabled(hasCodeAttribute);
        bytecodeBreakOnEntryAction.setEnabled(hasCodeAttribute);
        inspectSubstitutionSourceClassActorAction.setEnabled(teleClassMethodActor.isSubstituted());
        if (inspectCompilationsMenu.length() < teleClassMethodActor.numberOfCompilations()) {
            for (int index = inspectCompilationsMenu.length(); index < teleClassMethodActor.numberOfCompilations(); index++) {
                final TeleTargetMethod teleTargetMethod = teleClassMethodActor.getJavaTargetMethod(index);
                final StringBuilder name = new StringBuilder();
                name.append(inspection().nameDisplay().methodCompilationID(teleTargetMethod));
                name.append("  ");
                name.append(teleTargetMethod.classActorForType().simpleName());
                inspectCompilationsMenu.add(inspection().actions().inspectObject(teleTargetMethod, name.toString()));
            }
        }
    }

    public void redisplay() {
    }

}
