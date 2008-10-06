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
/*VCSID=d6f8a9b7-8625-449d-8bc3-6bcad0055c92*/
package com.sun.max.ins.method;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;

/**
 * A tabbed container for {@link MethodInspector}s.
 *
 * @author Bernd Mathiske
 * @author Mick Jordan
 * @author Michael Van De Vanter
 */
public final class MethodInspectorContainer extends TabbedInspector<MethodInspector, MethodInspectorContainer> {

    /**
     * Return the singleton MethodInspectorContainer, creating it if necessary.
     */
    public static MethodInspectorContainer make(Inspection inspection) {
        MethodInspectorContainer methodInspectorContainer = UniqueInspector.find(inspection, MethodInspectorContainer.class);
        if (methodInspectorContainer == null) {
            methodInspectorContainer = new MethodInspectorContainer(inspection, Residence.INTERNAL);
        }
        return methodInspectorContainer;
    }

    private MethodInspectorContainer(Inspection inspection, Residence residence) {
        super(inspection, residence, inspection.geometry().methodsFrameDefaultLocation(), inspection.geometry().methodsFramePrefSize(), "methodsInspector");
        frame().add(new MethodsMenuItems());
    }

    @Override
    public String getTitle() {
        return "Methods";
    }

    @Override
    public void add(MethodInspector methodInspector) {
        final String longTitle = methodInspector.getToolTip();
        add(methodInspector, methodInspector.getTitle(), longTitle, longTitle);
        addCloseIconToTab(methodInspector);
        methodInspector.frame().invalidate();
        methodInspector.frame().repaint();
    }

    private final class MethodsMenuItems implements InspectorMenuItems {

        public void addTo(InspectorMenu menu) {
            menu.add(inspection().inspectionMenus().getInspectMethodAction());
            menu.add(inspection().inspectionMenus().getViewSelectedCodeAction());
            menu.add(inspection().inspectionMenus().getViewCodeAtIPAction());
            menu.add(inspection().inspectionMenus().getViewMethodBytecodeAction());
            menu.add(inspection().inspectionMenus().getViewMethodTargetCodeAction());
            final JMenu sub = new JMenu("View Boot Image Method Code");
            sub.add(inspection().inspectionMenus().getViewBootImageRunMethodCodeAction());
            sub.add(inspection().inspectionMenus().getViewBootImageThreadRunMethodCodeAction());
            sub.add(inspection().inspectionMenus().getViewBootImageSchemeRunMethodCodeAction());
            menu.add(sub);
            menu.add(inspection().inspectionMenus().getViewMethodCodeContainingAddressAction());
            menu.add(inspection().inspectionMenus().getViewNativeCodeContainingAddressAction());
        }

        public Inspection inspection() {
            return MethodInspectorContainer.this.inspection();
        }

        public void refresh(long epoch) {
        }

        public void redisplay() {
        }

    }

}
