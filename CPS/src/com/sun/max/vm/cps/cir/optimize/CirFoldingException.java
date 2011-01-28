/*
 * Copyright (c) 2009, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.cir.optimize;

import java.lang.reflect.*;

/**
 * A checked exception that wraps an exception thrown when
 * {@linkplain CirFoldable#fold(CirOptimizer, com.sun.max.vm.compiler.cir.CirValue...) folding} a
 * {@linkplain CirFoldable foldable} CIR node. In general, a foldable CIR node should be able to determine whether is
 * will fold without an exception for a given set of arguments. However, guaranteeing this may make the test too
 * expensive for the common case. For example, a CIR node may indicate that it is foldable if a given set of arguments
 * are all constants even the folding may also fail if one of those arguments is null. Testing for null adds more time
 * to the common case (e.g. the CIR node for an {@code arraylength} operation) where the arguments do not contain a null
 * value.
 *
 * @author Doug Simon
 */
public class CirFoldingException extends InvocationTargetException {

    public CirFoldingException(Throwable cause) {
        super(cause);
    }
}
