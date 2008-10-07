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
/*VCSID=d133acb7-5f2a-4bd0-87a1-f6d0205bff99*/
package com.sun.max.ins.method;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;


/**
 * Provides the menu items related to a {@link TeleTargetMethod}.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public final class TargetMethodMenuItems implements InspectorMenuItems {

    private final Inspection _inspection;

    public Inspection inspection() {
        return _inspection;
    }

    private final TeleTargetMethod _teleTargetMethod;

    private final class InspectTargetMethodAction extends InspectorAction {
        private InspectTargetMethodAction() {
            super(inspection(), "Inspect Target Method");
        }

        @Override
        public void procedure() {
            inspection().focus().setHeapObject(_teleTargetMethod);
        }
    }

    private final InspectTargetMethodAction _inspectTargetMethodAction;


    private final class ViewTargetMethodCodeAction extends InspectorAction {
        private ViewTargetMethodCodeAction() {
            super(inspection(), "View Target Code");
        }

        @Override
        public void procedure() {
            inspection().focus().setCodeLocation(new TeleCodeLocation(_inspection.teleVM(), _teleTargetMethod.callEntryPoint()), false);
        }
    }

    private final ViewTargetMethodCodeAction _viewTargetMethodCodeAction;


    private final class TargetCodeBreakOnEntryAction extends InspectorAction {
        private TargetCodeBreakOnEntryAction() {
            super(inspection(), "Set Target Code Breakpoint at Entry");
        }

        @Override
        public void procedure() {
            _teleTargetMethod.setTargetBreakpointAtEntry();
        }
    }

    private final TargetCodeBreakOnEntryAction _targetCodeBreakOnEntryAction;


    public TargetMethodMenuItems(Inspection inspection, TeleTargetMethod teleTargetMethod) {
        _inspection = inspection;
        _teleTargetMethod = teleTargetMethod;
        _inspectTargetMethodAction = new InspectTargetMethodAction();
        _viewTargetMethodCodeAction = new ViewTargetMethodCodeAction();
        _targetCodeBreakOnEntryAction = new TargetCodeBreakOnEntryAction();
        refresh(_inspection.teleVM().teleProcess().epoch());
    }

    public void addTo(InspectorMenu menu) {

        menu.add(_viewTargetMethodCodeAction);
        menu.add(_targetCodeBreakOnEntryAction);

        menu.addSeparator();
        menu.add(_inspectTargetMethodAction);
    }

    public void refresh(long epoch) {
    }

    public void redisplay() {
    }

}

