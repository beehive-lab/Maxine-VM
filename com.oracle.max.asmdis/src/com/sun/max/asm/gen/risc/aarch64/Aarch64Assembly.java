/*
 * Copyright (c) 2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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

package com.sun.max.asm.gen.risc.aarch64;

import com.sun.max.asm.gen.risc.RiscAssembly;
import com.sun.max.asm.gen.risc.RiscTemplate;
import com.sun.max.asm.gen.risc.RiscTemplateCreator;
import com.sun.max.asm.gen.risc.bitRange.BitRangeOrder;
import com.sun.max.lang.ISA;

import java.util.List;

/**
 */

public final class Aarch64Assembly extends RiscAssembly {

    public static final Aarch64Assembly ASSEMBLY = new Aarch64Assembly();

    private Aarch64Assembly() {
        super(ISA.Aarch64, RiscTemplate.class);
    }

    @Override
    public BitRangeOrder bitRangeEndianness() {
        return BitRangeOrder.DESCENDING;
    }

    @Override
    protected List<RiscTemplate> createTemplates() {
        assert false : "Unimplemented T1X createTemplates";
        final RiscTemplateCreator creator = new RiscTemplateCreator();
        return creator.templates();
    }
}
