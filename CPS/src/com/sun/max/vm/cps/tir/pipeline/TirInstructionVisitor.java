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
package com.sun.max.vm.cps.tir.pipeline;

import com.sun.max.vm.cps.tir.*;

public interface TirInstructionVisitor {

    void visit(TirLocal instruction);

    void visit(TirNestedLocal instruction);

    void visit(TirConstant instruction);

    void visit(TirTreeCall instruction);

    void visit(TirBuiltinCall instruction);

    void visit(TirCall instruction);

    void visit(TirMethodCall instruction);

    void visit(TirDirCall instruction);

    void visit(TirGuard instruction);

    void visit(TirInstruction instruction);

    void visit(TirMessage.TirTreeBegin message);

    void visit(TirMessage.TirTreeEnd message);

    void visit(TirMessage.TirTraceBegin message);

    void visit(TirMessage.TirTraceEnd message);

    void visit(TirMessage message);

}
