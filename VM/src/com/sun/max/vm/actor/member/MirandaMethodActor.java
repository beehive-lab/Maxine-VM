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
/*VCSID=0dda5397-fec3-47d4-a37f-ba36450035bc*/
package com.sun.max.vm.actor.member;

import com.sun.max.vm.compiler.*;

/**
 * Hiroshi pointed out that the following leads to an
 * invokevirtual that references an interface method:
 * 
 * interface I {
 *     void foo();
 * }
 * 
 * abstract class C implements I { }
 * 
 * class D extends C {
 *     public void foo() { }
 * }
 * 
 * void m() {
 *     C c = new D();
 *     c.foo(); // invokevirtual C.foo
 * }
 * 
 * To deal with "missing dynamic methods" like this,
 * we supplement classes like C with synthesized dynamic methods
 * that do nothing but appropriately throw AbtractMethodError.
 * 
 * Such methods are known as Miranda methods.
 * (This now common naming of this "feature"
 *  has been losely derived from the US's "Miranda" law as paraphrased here:
 *  "If you don't already have one,
 *   then you have the right that one will provided be for you".)
 *
 * The miranda methods we create are public and abstract, they have no code.
 * Executing the invokevirtual byte code against one of these is expected to throw AbstractMethodError.
 * 
 * @author Bernd Mathiske
 */
public class MirandaMethodActor extends VirtualMethodActor {

    public MirandaMethodActor(InterfaceMethodActor interfaceMethodActor) {
        super(interfaceMethodActor.name(),
              interfaceMethodActor.descriptor(),
              ACC_PUBLIC | MethodActor.ACC_ABSTRACT | ACC_SYNTHETIC,
              null);
    }

    @Override
    public final boolean isMiranda() {
        return true;
    }

    @Override
    public boolean isDeclaredInline(CompilerScheme compilerScheme) {
        return false;
    }

}
