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
 * Provides menu items related to a specific {@link MethodActor} in the {@link TeleVM}.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public final class ClassMethodMenuItems implements InspectorMenuItems {

    private final Inspection _inspection;

    public Inspection inspection() {
        return _inspection;
    }

    private final TeleClassMethodActor _teleClassMethodActor;

    private final class InspectClassMethodActorAction extends InspectorAction {
        private InspectClassMethodActorAction() {
            super(inspection(), "Inspect ClassMethodActor");
        }

        @Override
        public void procedure() {
            inspection().focus().setHeapObject(_teleClassMethodActor);
        }
    }

    private final InspectClassMethodActorAction _inspectClassMethodActorAction;

    private final class InspectSubstitutionSourceClassActorAction extends InspectorAction {
        private InspectSubstitutionSourceClassActorAction() {
            super(inspection(), "Inspect Method Substitution Source");
        }

        @Override
        public void procedure() {
            inspection().focus().setHeapObject(_teleClassMethodActor.teleClassActorSubstitutedFrom());
        }
    }

    private final InspectSubstitutionSourceClassActorAction _inspectSubstitutionSourceClassActorAction;

    private final class ViewJavaSourceAction extends InspectorAction {
        private ViewJavaSourceAction() {
            super(inspection(), "View Java Source (external)");
        }

        @Override
        public void procedure() {
            inspection().viewSourceExternally(new BytecodeLocation(_teleClassMethodActor.classMethodActor(), 0));
        }
    }

    private final ViewJavaSourceAction _viewJavaSourceAction;


    private final class ViewBytecodeAction extends InspectorAction {
        private ViewBytecodeAction() {
            super(inspection(), "View Bytecode");
        }

        @Override
        public void procedure() {
            final TeleCodeLocation teleCodeLocation = new TeleCodeLocation(teleVM(), _teleClassMethodActor, 0);
            inspection().focus().setCodeLocation(teleCodeLocation, false);
        }
    }

    private final ViewBytecodeAction _viewBytecodeAction;


    private final class BytecodeBreakOnEntryAction extends InspectorAction {
        private BytecodeBreakOnEntryAction() {
            super(inspection(), "Set Bytecode Breakpoint at Method Entry");
        }

        @Override
        public void procedure() {
            final MethodKey methodKey = new MethodActorKey(_teleClassMethodActor.classMethodActor());
            teleVM().bytecodeBreakpointFactory().makeBreakpoint(new TeleBytecodeBreakpoint.Key(methodKey, 0), false);
        }
    }

    private final BytecodeBreakOnEntryAction _bytecodeBreakOnEntryAction;


    private final class InvokeMethodAction extends InspectorAction {
        private InvokeMethodAction() {
            super(inspection(), "Invoke method...");
        }

        @Override
        public void procedure() {
            ClassMethodActor classMethodActor = _teleClassMethodActor.classMethodActor();
            ReferenceValue receiver = null;

            if (classMethodActor instanceof VirtualMethodActor) {
                final String input = inspection().inputDialog("Argument 0 (receiver, must be a reference to a " + classMethodActor.holder() + " or subclass, origin address in hex):", "");

                if (input == null) {
                    // User clicked cancel.
                    return;
                }

                receiver = teleVM().createReferenceValue(teleVM().originToReference(Pointer.fromLong(new BigInteger(input, 16).longValue())));
                final ClassActor dynamicClass = receiver.getClassActor();
                classMethodActor = dynamicClass.findClassMethodActor(classMethodActor);
            }

            final Value[] arguments = MethodArgsDialog.getArgs(inspection(), classMethodActor, receiver);
            if (arguments == null) {
                // User clicked cancel.
                return;
            }

            try {
                final Value returnValue = TeleInterpreter.execute(teleVM(), classMethodActor, arguments);
                inspection().informationMessage("Method " + classMethodActor.name() + " returned " + returnValue.toString());
            } catch (TeleInterpreterException teleInterpreterException) {
                throw new InspectorError(teleInterpreterException);
            }
        }
    }

    private final InvokeMethodAction _invokeMethodAction;

    private InspectorMenu _inspectCompilationsMenu = new InspectorMenu(null, "Method Compilations");


    public ClassMethodMenuItems(Inspection inspection, TeleClassMethodActor teleClassMethodActor) {
        _inspection = inspection;
        _teleClassMethodActor = teleClassMethodActor;
        _viewJavaSourceAction = new ViewJavaSourceAction();
        _viewBytecodeAction = new ViewBytecodeAction();
        _bytecodeBreakOnEntryAction = new BytecodeBreakOnEntryAction();
        _invokeMethodAction = new InvokeMethodAction();
        _inspectClassMethodActorAction = new InspectClassMethodActorAction();
        _inspectSubstitutionSourceClassActorAction = new InspectSubstitutionSourceClassActorAction();
        refresh(teleVM().teleProcess().epoch(), true);
    }

    public void addTo(InspectorMenu menu) {

        menu.add(_viewJavaSourceAction);
        menu.add(_viewBytecodeAction);

        menu.addSeparator();
        menu.add(_bytecodeBreakOnEntryAction);
        menu.add(_invokeMethodAction);

        menu.addSeparator();
        menu.add(_inspectClassMethodActorAction);
        menu.add(_inspectSubstitutionSourceClassActorAction);
        menu.add(_inspectCompilationsMenu);
    }

    private TeleVM teleVM() {
        return _inspection.teleVM();
    }

    public void refresh(long epoch, boolean force) {
        _teleClassMethodActor.refreshView();
        final boolean hasCodeAttribute =  _teleClassMethodActor.hasCodeAttribute();
        final File javaSourceFile = teleVM().findJavaSourceFile(_teleClassMethodActor.getTeleHolder().classActor());
        _viewJavaSourceAction.setEnabled(javaSourceFile != null);
        _viewBytecodeAction.setEnabled(hasCodeAttribute);
        _bytecodeBreakOnEntryAction.setEnabled(hasCodeAttribute);
        _inspectSubstitutionSourceClassActorAction.setEnabled(_teleClassMethodActor.isSubstituted());
        if (_inspectCompilationsMenu.length() < _teleClassMethodActor.numberOfCompilations()) {
            for (int index = _inspectCompilationsMenu.length(); index < _teleClassMethodActor.numberOfCompilations(); index++) {
                final TeleTargetMethod teleTargetMethod = _teleClassMethodActor.getJavaTargetMethod(index);
                final StringBuilder name = new StringBuilder();
                name.append(inspection().nameDisplay().methodCompilationID(teleTargetMethod));
                name.append("  ");
                name.append(teleTargetMethod.classActorForType().simpleName());
                _inspectCompilationsMenu.add(inspection().actions().inspectObject(teleTargetMethod, name.toString()));
            }
        }
    }

    public void redisplay() {
    }

}
