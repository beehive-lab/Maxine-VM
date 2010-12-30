/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.eir.amd64.unix;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.vm.cps.eir.amd64.*;

/**
 * @author Bernd Mathiske
 */
public class UnixAMD64EirJavaABI extends UnixAMD64EirABI {

    /**
     * Allocated registers from the allocatable set are caller saved.
     */
    @Override
    public PoolSet<AMD64EirRegister> callerSavedRegisters() {
        return allocatableRegisters();
    }

    /**
     * No callee saved registers.
     */
    protected List<AMD64EirRegister> calleeSavedRegisters = Collections.emptyList();

    @Override
    public List<AMD64EirRegister> calleeSavedRegisters() {
        return calleeSavedRegisters;
    }
}
