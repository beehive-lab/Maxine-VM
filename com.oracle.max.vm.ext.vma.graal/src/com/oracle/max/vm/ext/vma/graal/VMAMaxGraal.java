/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.graal;

import com.oracle.graal.phases.*;
import com.oracle.graal.phases.PhasePlan.*;
import com.oracle.max.vm.ext.graal.*;
import com.oracle.max.vm.ext.graal.snippets.*;
import com.oracle.max.vm.ext.vma.graal.phases.*;
import com.oracle.max.vm.ext.vma.graal.snippets.*;
import com.oracle.max.vm.ext.vma.run.java.*;
import com.sun.max.vm.actor.member.*;

/**
 * Subclass of {@link MaxGraal} that (optionally) adds advice.
 * Unlike {@link VMAT1X} there is only one instance of a Graal compiler
 * in the workspace since the {@link AdvicePhase} can be added on a per-compilation
 * basis so is not wired into the compiler.
 *
 */
public class VMAMaxGraal extends MaxGraal {

    @Override
    protected void addCustomPhase(ClassMethodActor methodActor, PhasePlan phasePlan) {
        if (VMAOptions.instrumentForAdvising(methodActor)) {
            phasePlan.addPhase(PhasePosition.AFTER_PARSING, new AdvicePhase());
        }
    }

    @Override
    protected void addCustomSnippets(MaxReplacementsImpl replacements) {
        replacements.installAndRegisterSnippets(AdviceSnippets.class);
    }

}
