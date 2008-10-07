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
/*VCSID=476547a0-4f24-441f-8cc7-9d49ec5be916*/
package com.sun.max.vm.compiler.cir.gui;

import javax.swing.event.*;

import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.ir.observer.*;

/**
 * Adapter for plugging the {@linkplain CirTraceVisualizer CIR visualizer} into the mechanism for
 * {@linkplain IrObserver IR observer}.
 *
 * @author Doug Simon
 * @author Ben L. Titzer
 * @author Thomas Wuerthinger
 */
public class CirObserverAdapter extends IrObserverAdapter {

    private CirGenerator _cirGenerator;
    private static CirTraceVisualizer _visualizer;

    @Override
    public boolean attach(IrGenerator generator) {
        if (_visualizer == null && generator instanceof CirGenerator) {
            _cirGenerator = (CirGenerator) generator;
            _visualizer = CirTraceVisualizer.createAndShowGUI();
            _visualizer.addAncestorListener(new AncestorListener() {

                public void ancestorRemoved(AncestorEvent event) {
                    _cirGenerator.removeIrObserver(CirObserverAdapter.this);
                    _visualizer = null;
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
        if (_visualizer != null) {
            final CirAnnotatedTraceBuilder builder = new CirAnnotatedTraceBuilder(node);
            _visualizer.add(new CirAnnotatedTrace(builder.trace(), builder.elements(), cirMethod.classMethodActor(), transform));
        }
    }
}
