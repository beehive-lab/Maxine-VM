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

import java.awt.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;

/**
 * A tabbed container for {@link MethodInspector}s.
 *
 * @author Bernd Mathiske
 * @author Mick Jordan
 * @author Michael Van De Vanter
 */
public final class MethodInspectorContainer extends TabbedInspector<MethodInspector> {

    private static MethodInspectorContainer methodInspectorContainer = null;

    /**
     * Return the singleton MethodInspectorContainer, creating it if necessary.
     */
    public static MethodInspectorContainer make(Inspection inspection) {
        if (methodInspectorContainer == null) {
            methodInspectorContainer = new MethodInspectorContainer(inspection);
        }
        return methodInspectorContainer;
    }

    private MethodInspectorContainer(Inspection inspection) {
        super(inspection, "methodsInspector");
    }

    @Override
    protected Rectangle defaultFrameBounds() {
        return  inspection().geometry().methodsFrameDefaultBounds();
    }

    @Override
    public String getTextForTitle() {
        final MethodInspector methodInspector = getSelected();
        return methodInspector == null ? "Methods" : "Method: " + methodInspector.getToolTip();
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            public void procedure() {
                MethodInspectorPreferences.globalPreferences(inspection()).showDialog();
            }
        };
    }

    @Override
    public InspectorAction getPrintAction() {
        return new InspectorAction(inspection(), "Print") {
            @Override
            public void procedure() {
                final MethodInspector methodInspector = MethodInspectorContainer.this.getSelected();
                if (methodInspector != null) {
                    methodInspector.print();
                }
            }
        };
    }

    @Override
    public void add(MethodInspector methodInspector) {
        final String longTitle = methodInspector.getToolTip();
        add(methodInspector, methodInspector.getTextForTitle(), longTitle);
        addCloseIconToTab(methodInspector, longTitle);
        methodInspector.highlight();
    }

    @Override
    public void inspectorClosing() {
        methodInspectorContainer = null;
        super.inspectorClosing();
    }

}
