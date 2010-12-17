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
package com.sun.max.vm.cps.cir.transform;

import com.sun.max.vm.cps.cir.*;

/**
 * Remove a value that is no longer wanted in all Java frame descriptors in a given graph, outside blocks.
 * This is done by setting the corresponding value to {@link CirValue.UNDEFINED}.
 *
 * @author Bernd Mathiske
 */
public final class CirJavaFrameDescriptorClipping extends CirTraversal.OutsideBlocks {

    private final CirValue clipValue;

    private CirJavaFrameDescriptorClipping(CirNode node, CirValue clipValue) {
        super(node);
        this.clipValue = clipValue;
    }

    private void clipValues(CirValue[] values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == clipValue) {
                values[i] = CirValue.UNDEFINED;
            }
        }
    }

    @Override
    protected void visitJavaFrameDescriptor(CirJavaFrameDescriptor javaFrameDescriptor) {
        CirJavaFrameDescriptor j = javaFrameDescriptor;
        while (j != null) {
            clipValues(j.locals);
            clipValues(j.stackSlots);
            j = j.parent();
        }
    }

    public static void applySingle(CirNode node, CirValue clipValue) {
        final CirJavaFrameDescriptorClipping clipping = new CirJavaFrameDescriptorClipping(node, clipValue);
        clipping.run();
    }

}
