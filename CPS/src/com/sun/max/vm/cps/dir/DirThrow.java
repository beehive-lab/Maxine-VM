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
package com.sun.max.vm.cps.dir;

import com.sun.max.vm.cps.dir.transform.*;

/**
 * Explicit exception throwing.
 *
 * @author Hiroshi Yamauchi
 * @author Bernd Mathiske
 */
public class DirThrow extends DirInstruction {

    final DirValue throwable;

    public DirThrow(DirValue throwable) {
        this.throwable = throwable;
    }

    public DirValue throwable() {
        return throwable;
    }

    @Override
    public boolean isEquivalentTo(DirInstruction other, DirBlockEquivalence dirBlockEquivalence) {
        if (other instanceof DirThrow) {
            final DirThrow dirThrow = (DirThrow) other;
            return throwable.equals(dirThrow.throwable);
        }
        return false;
    }

    @Override
    public int hashCodeForBlock() {
        return super.hashCodeForBlock() ^ throwable.hashCodeForBlock();
    }

    @Override
    public String toString() {
        return "throw " + throwable;
    }

    @Override
    public void acceptVisitor(DirVisitor visitor) {
        visitor.visitThrow(this);
    }
}
