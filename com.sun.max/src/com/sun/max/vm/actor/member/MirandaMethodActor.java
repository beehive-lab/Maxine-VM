/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.actor.member;


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
 */
public class MirandaMethodActor extends VirtualMethodActor {

    public MirandaMethodActor(InterfaceMethodActor interfaceMethodActor) {
        super(interfaceMethodActor.name,
              interfaceMethodActor.descriptor(),
              ACC_PUBLIC | MethodActor.ACC_ABSTRACT | ACC_SYNTHETIC,
              null, null);
    }

    @Override
    public final boolean isMiranda() {
        return true;
    }
}
