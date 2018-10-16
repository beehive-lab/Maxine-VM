/*
 * Copyright (c) 2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
package com.oracle.max.vm.tests.crossisa.aarch64.adapters;

import java.lang.reflect.*;

import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.AdapterGenerator.*;
import com.sun.max.vm.compiler.target.aarch64.*;


public abstract class Opt2BaselineAarch64AdapterTest extends Aarch64AdapterTest {

    public Opt2BaselineAarch64AdapterTest(String signature) throws Exception {
        super(signature);
    }

    @Override
    public Adapter createAdapter(Sig sig) throws Exception {
        Aarch64AdapterGenerator adapterGenerator = new Aarch64AdapterGenerator.Opt2Baseline();
        Method m = adapterGenerator.getClass().getDeclaredMethod("create", Sig.class);
        m.setAccessible(true);
        Adapter adapter = (Adapter) m.invoke(adapterGenerator, sig);
        return adapter;
    }
}
