/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.cps.cir.gui;

import javax.swing.event.*;

import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.cps.ir.observer.*;

/**
 * Adapter for plugging the {@linkplain CirTraceVisualizer CIR visualizer} into the mechanism for
 * {@linkplain IrObserver IR observer}.
 *
 * @author Doug Simon
 * @author Ben L. Titzer
 * @author Thomas Wuerthinger
 */
public class CirObserverAdapter extends IrObserverAdapter {

    private CirGenerator cirGenerator;
    private static CirTraceVisualizer visualizer;

    @Override
    public boolean attach(IrGenerator generator) {
        if (visualizer == null && generator instanceof CirGenerator) {
            cirGenerator = (CirGenerator) generator;
            visualizer = CirTraceVisualizer.createAndShowGUI();
            visualizer.addAncestorListener(new AncestorListener() {

                public void ancestorRemoved(AncestorEvent event) {
                    cirGenerator.removeIrObserver(CirObserverAdapter.this);
                    visualizer = null;
                }

                public void ancestorAdded(AncestorEvent event) {
                }

                public void ancestorMoved(AncestorEvent event) {
                }
            });
            return true;
        }
        return false;
    }

    @Override
    public void observeAfterGeneration(IrMethod irMethod, IrGenerator irGenerator) {
        if (irMethod instanceof CirMethod) {
            final CirMethod cirMethod = (CirMethod) irMethod;
            visualize(cirMethod, cirMethod.closure(), "Generated CIR");
        }
    }

    @Override
    public void observeBeforeTransformation(IrMethod irMethod, Object context, Object transform) {
        if (irMethod instanceof CirMethod && context instanceof CirNode) {
            final CirMethod cirMethod = (CirMethod) irMethod;
            visualize(cirMethod, (CirNode) context, "BEFORE: " + transform);
        }
    }

    @Override
    public void observeAfterTransformation(IrMethod irMethod, Object context, Object transform) {
        if (irMethod instanceof CirMethod && context instanceof CirNode) {
            final CirMethod cirMethod = (CirMethod) irMethod;
            visualize(cirMethod, (CirNode) context, "AFTER: " + transform);
        }
    }

    public static synchronized void visualize(CirMethod cirMethod, CirNode node, String transform) {
        if (visualizer != null && visualizer.shouldBeTraced(cirMethod.classMethodActor())) {
            final CirAnnotatedTraceBuilder builder = new CirAnnotatedTraceBuilder(node);
            visualizer.add(new CirAnnotatedTrace(builder.trace(), builder.elements(), cirMethod.classMethodActor(), transform));
        }
    }
}
