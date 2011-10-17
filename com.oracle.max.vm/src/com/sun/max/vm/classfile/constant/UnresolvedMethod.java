/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.classfile.constant;

import com.oracle.max.criutils.*;
import com.sun.cri.ri.*;

/**
 * An {@linkplain RiField#isResolved() unresolved} method with a back reference
 * to the constant pool entry from which it was derived.
 */
public class UnresolvedMethod extends BaseUnresolvedMethod {

    public final ConstantPool constantPool;
    public final int cpi;

    public UnresolvedMethod(ConstantPool constantPool, int cpi, RiType holder, String name, RiSignature signature) {
        super(holder, name, signature);
        this.constantPool = constantPool;
        this.cpi = cpi;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof UnresolvedMethod) {
            UnresolvedMethod other = (UnresolvedMethod) o;
            return constantPool == other.constantPool && cpi == other.cpi;
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
