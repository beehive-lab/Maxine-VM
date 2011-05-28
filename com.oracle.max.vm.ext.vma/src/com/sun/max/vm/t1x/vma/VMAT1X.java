/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.t1x.vma;

import static com.sun.max.vm.MaxineVM.*;

import com.oracle.max.vm.ext.vma.options.*;
import com.sun.cri.ci.CiStatistics;
import com.sun.max.annotate.HOSTED_ONLY;
import com.sun.max.program.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.member.ClassMethodActor;
import com.sun.max.vm.compiler.target.TargetMethod;
import com.sun.max.vm.t1x.T1X;

/**
 * Variant of T1X that uses modified templates to support object analysis.
 * We actually create two compilers, one that instruments and a vanilla
 * one that doesn't (since the templates are built at image build time).
 *
 * The actual templates that are used are specified in {@link VMAOptions#VMATemplatesClass}.
 *
 * @author Mick Jordan
 *
 */
public class VMAT1X extends T1X {

    private boolean instrumenting;

    @HOSTED_ONLY
    public VMAT1X() {
        super(getDefaultT1X(), new VMAT1XCompilationFactory());
    }

    private static T1X getDefaultT1X() {
        // TODO if opt is T1X use it
        final T1X t1x = new T1X();
        return t1x;
    }

    @Override
    public void initialize(Phase phase) {
        instrumenting = VMAOptions.initialize(phase);
        altT1X.initialize(phase);
        if (isHosted() && phase == Phase.COMPILING) {
            Class< ? > templateSource = null;
            if (VMAOptions.VMATemplatesClass == null) {
                templateSource = NullT1XTemplateSource.class;
            } else {
                try {
                    templateSource = Class.forName(VMAOptions.VMATemplatesClass);
                } catch (Exception ex) {
                    ProgramError.unexpected("failed to load class " + VMAOptions.VMATemplatesClass);
                }
            }
            setTemplateSource(templateSource);
        }
        super.initialize(phase);
    }

    @Override
    public TargetMethod compile(ClassMethodActor method, boolean install, CiStatistics stats) {
        if (instrumenting && VMAOptions.instrumentForAdvising(method)) {
            return super.compile(method, install, stats);
        } else {
            return altT1X.compile(method, install, stats);
        }
    }

    T1X getAltT1X() {
        return altT1X;
    }

}
