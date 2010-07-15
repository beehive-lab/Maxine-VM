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

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.actor.member.*;

/**
 * Provides menu items related to a specific {@link MethodActor} in the VM.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public final class ClassMethodActorMenuItems extends AbstractInspectorMenuItems {

    private final TeleClassMethodActor teleClassMethodActor;

    public ClassMethodActorMenuItems(Inspection inspection, TeleClassMethodActor teleClassMethodActor) {
        super(inspection);
        this.teleClassMethodActor = teleClassMethodActor;
        refresh(true);
    }

    public void addTo(InspectorMenu menu) {
        final InspectorMenu objectMenu = new InspectorMenu("Object");
        objectMenu.add(actions().inspectObject(teleClassMethodActor, "Method: " + teleClassMethodActor.classActorForObjectType().simpleName()));
        final TeleClassActor teleClassActor = teleClassMethodActor.getTeleHolder();
        objectMenu.add(actions().inspectObject(teleClassActor, "Holder: " + teleClassActor.classActorForObjectType().simpleName()));
        objectMenu.add(actions().inspectSubstitutionSourceClassActorAction(teleClassMethodActor));
        objectMenu.add(actions().inspectTargetMethodCompilationsMenu(teleClassMethodActor));
        menu.add(objectMenu);

        final InspectorMenu codeMenu = new InspectorMenu("Code");
        codeMenu.add(actions().viewJavaSource(teleClassMethodActor));
        codeMenu.add(actions().viewMethodBytecode(teleClassMethodActor));
        codeMenu.add(actions().viewTargetMethodCodeMenu(teleClassMethodActor));
        menu.add(codeMenu);

        final InspectorMenu debugMenu = new InspectorMenu("Debug");
        debugMenu.add(actions().setBytecodeBreakpointAtMethodEntry(teleClassMethodActor));
        debugMenu.add(actions().debugInvokeMethod(teleClassMethodActor));
        menu.add(debugMenu);
    }

    public void addTo(InspectorPopupMenu menu) {
        final InspectorMenu objectMenu = new InspectorMenu("Object");
        objectMenu.add(actions().inspectObject(teleClassMethodActor, "Method: " + teleClassMethodActor.classActorForObjectType().simpleName()));
        final TeleClassActor teleClassActor = teleClassMethodActor.getTeleHolder();
        objectMenu.add(actions().inspectObject(teleClassActor, "Holder: " + teleClassActor.classActorForObjectType().simpleName()));
        objectMenu.add(actions().inspectSubstitutionSourceClassActorAction(teleClassMethodActor));
        objectMenu.add(actions().inspectTargetMethodCompilationsMenu(teleClassMethodActor));
        menu.add(objectMenu);

        final InspectorMenu codeMenu = new InspectorMenu("Code");
        codeMenu.add(actions().viewJavaSource(teleClassMethodActor));
        codeMenu.add(actions().viewMethodBytecode(teleClassMethodActor));
        codeMenu.add(actions().viewTargetMethodCodeMenu(teleClassMethodActor));
        menu.add(codeMenu);

        final InspectorMenu debugMenu = new InspectorMenu("Debug");
        final InspectorMenu breakOnEntryMenu = new InspectorMenu("Break at method entry");
        breakOnEntryMenu.add(actions().setBytecodeBreakpointAtMethodEntry(teleClassMethodActor, "Bytecodes"));
        debugMenu.add(breakOnEntryMenu);
        debugMenu.add(actions().debugInvokeMethod(teleClassMethodActor));
        menu.add(debugMenu);
    }

    @Override
    public void refresh(boolean force) {
        if (teleClassMethodActor != null) {
            teleClassMethodActor.refreshView();
        }
        super.refresh(force);
    }

}
