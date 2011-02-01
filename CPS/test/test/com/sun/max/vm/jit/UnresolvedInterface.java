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
package test.com.sun.max.vm.jit;

/**
 * The purpose of this interface is solely for testing cases JIT-compilation of bytecode with operands to interfaces that aren't resolved
 * at compile-time. We need to use a interface different than UnresolvedAtCompileTimeInterface  to make sure the patching of literal references to
 * the class by the template-based JIT compiler are effective. The interface also declare several methods so that testing can verify that
 * an interface index different from the one in the template is being set in place in the copy of the template emitted by the JIT.
 *
 * @author Laurent Daynes
 */
public interface UnresolvedInterface {
    void parameterlessUnresolvedInterfaceMethod0();
    void parameterlessUnresolvedInterfaceMethod1();
    void parameterlessUnresolvedInterfaceMethod();
    void unresolvedInterfaceMethod(int i1, int i2);
}
