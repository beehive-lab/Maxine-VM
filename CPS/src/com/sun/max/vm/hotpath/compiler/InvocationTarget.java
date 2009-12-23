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
package com.sun.max.vm.hotpath.compiler;

import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.snippet.*;

public class InvocationTarget {
    public static StaticMethodActor findInvokeStaticTarget(MethodActor method) {
        return (StaticMethodActor) method;
    }

    public static VirtualMethodActor findInvokeVirtualTarget(MethodActor method, Object receiver) {
        return MethodSelectionSnippet.SelectVirtualMethod.quasiFold(receiver, (VirtualMethodActor) method);
    }

    public static VirtualMethodActor findInvokeInterfaceTarget(MethodActor method, Object receiver) {
        return MethodSelectionSnippet.SelectInterfaceMethod.quasiFold(receiver, (InterfaceMethodActor) method);
    }

    public static VirtualMethodActor findInvokeSpecialTarget(ClassActor classActor, MethodActor method) {
        VirtualMethodActor virtualMethodActor = (VirtualMethodActor) method;
        if (ResolutionSnippet.isSpecial(virtualMethodActor, classActor)) {
            virtualMethodActor = classActor.superClassActor.findVirtualMethodActor(virtualMethodActor);
            if (virtualMethodActor == null) {
                throw new AbstractMethodError();
            }
        }
        if (virtualMethodActor.isAbstract()) {
            throw new AbstractMethodError();
        }
        return virtualMethodActor;
    }
}
