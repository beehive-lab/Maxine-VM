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
package com.sun.max.ins.object;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.layout.*;

/**
 * An object inspector specialized for displaying a low-level heap object in the VM constructed using {@link TupleLayout}.
 *
 * @author Michael Van De Vanter
 */
public class TupleInspector extends ObjectInspector {

    private ObjectScrollPane fieldsPane;

    TupleInspector(Inspection inspection, ObjectInspectorFactory factory, TeleObject teleObject) {
        super(inspection, factory, teleObject);
        final InspectorFrame frame = createFrame(true);

        final MaxCompiledCode compiledCode = vm().codeCache().findCompiledCode(teleObject.origin());
        if (compiledCode != null) {
            frame.makeMenu(MenuKind.DEBUG_MENU).add(actions().setMachineCodeBreakpointAtEntry(compiledCode));
        }

        final TeleClassMethodActor teleClassMethodActor = teleObject.getTeleClassMethodActorForObject();
        final InspectorMenu objectMenu = frame.makeMenu(MenuKind.OBJECT_MENU);
        if (teleClassMethodActor != null) {
            // This object is associated with a class method
            objectMenu.add(actions().inspectObject(teleClassMethodActor, "Method: " + teleClassMethodActor.classActorForObjectType().simpleName()));
            final TeleClassActor teleClassActor = teleClassMethodActor.getTeleHolder();
            objectMenu.add(actions().inspectObject(teleClassActor, "Holder: " + teleClassActor.classActorForObjectType().simpleName()));
            objectMenu.add(actions().inspectSubstitutionSourceClassActorAction(teleClassMethodActor));
            objectMenu.add(actions().inspectTargetMethodCompilationsMenu(teleClassMethodActor, "Method compilations"));

            final InspectorMenu codeMenu = frame.makeMenu(MenuKind.CODE_MENU);
            codeMenu.add(actions().viewJavaSource(teleClassMethodActor));
            codeMenu.add(actions().viewMethodBytecode(teleClassMethodActor));
            codeMenu.add(actions().viewTargetMethodCodeMenu(teleClassMethodActor));
            codeMenu.add(defaultMenuItems(MenuKind.CODE_MENU));

            final InspectorMenu debugMenu = frame.makeMenu(MenuKind.DEBUG_MENU);
            final InspectorMenu breakOnEntryMenu = new InspectorMenu("Break at method entry");
            breakOnEntryMenu.add(actions().setBytecodeBreakpointAtMethodEntry(teleClassMethodActor, "Bytecodes"));
            debugMenu.add(breakOnEntryMenu);
            debugMenu.add(actions().debugInvokeMethod(teleClassMethodActor));
            debugMenu.add(defaultMenuItems(MenuKind.DEBUG_MENU));
        }
        objectMenu.add(defaultMenuItems(MenuKind.OBJECT_MENU));
    }

    @Override
    protected void createView() {
        super.createView();
        final TeleTupleObject teleTupleObject = (TeleTupleObject) teleObject();
        fieldsPane = ObjectScrollPane.createFieldsPane(inspection(), teleTupleObject, instanceViewPreferences);
        getContentPane().add(fieldsPane);
    }

    @Override
    protected void refreshView(boolean force) {
        if (getJComponent().isShowing() || force) {
            fieldsPane.refresh(force);
            super.refreshView(force);
        }
    }

}
