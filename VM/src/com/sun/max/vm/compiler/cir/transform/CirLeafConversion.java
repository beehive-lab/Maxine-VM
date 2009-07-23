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
package com.sun.max.vm.compiler.cir.transform;

import com.sun.max.vm.compiler.cir.*;

/**
 * A CIR transformation that only affects leaf nodes.
 * 
 * @author Bernd Mathiske
 */
public abstract class CirLeafConversion extends CirTransformation {

    protected CirLeafConversion() {
        super();
    }

    @Override
    public CirNode transformCall(CirCall call) {
        call.setProcedure((CirValue) call.procedure().acceptTransformation(this));
        call.clearJavaFrameDescriptorIfNotNeeded();

        transformValues(call.arguments());

        CirJavaFrameDescriptor javaFrameDescriptor = call.javaFrameDescriptor();
        while (javaFrameDescriptor != null) {
            transformValues(javaFrameDescriptor.locals);
            transformValues(javaFrameDescriptor.stackSlots);
            javaFrameDescriptor = javaFrameDescriptor.parent();
        }

        return call;
    }

    @Override
    public CirNode transformClosure(CirClosure closure) {
        transformVariables(closure.parameters());
        closure.body().acceptTransformation(this);
        return closure;
    }

}
