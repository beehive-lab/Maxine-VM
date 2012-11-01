/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.t1x.vma;

import static com.oracle.max.vm.ext.t1x.T1XTemplateTag.*;
import static com.sun.max.vm.MaxineVM.*;

import java.util.*;

import com.oracle.max.vm.ext.t1x.*;
import com.oracle.max.vm.ext.vma.run.java.*;
import com.sun.cri.ci.CiStatistics;
import com.sun.max.annotate.HOSTED_ONLY;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.member.ClassMethodActor;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.TargetMethod;

/**
 * Variant of T1X that uses modified templates to support VM advising.
 * We actually create two compilers, one that instruments for advice and a standard
 * one that doesn't (since the templates are built at image build time).
 * Furthermore, in order to selectively apply before/after advice on a
 * per-bytecode level, we create two sets of additional templates one with just before
 * advice and one with just after advice.
 *
 * The {@link VMAT1X} instance has access to the standard templates array via the {@link #stdT1X} field.
 * The {@link #templates} array for the instance has both before and after advice generated.
 * The {@link #beforeTemplates} array has the templates with only before advice.
 * The {@link #afterTemplates} array has the templates with only after advice.
 * Both of the latter two arrays utilize the standard templates array for any templates
 * that do not support the associated advice. E.g., very few templates support after advice.
 *
 */
public class VMAT1X extends T1X {

    private static final Class<?> BeforeAfterTemplateSourceClass = VMAdviceBeforeAfterTemplateSource.class;
    private static final Class<?> BeforeTemplateSourceClass = VMAdviceBeforeTemplateSource.class;
    private static final Class<?> AfterTemplateSourceClass = VMAdviceAfterTemplateSource.class;
    private boolean instrumenting;
    T1XTemplate[]  beforeTemplates;
    T1XTemplate[]  afterTemplates;
    Class<?> templateSource = BeforeAfterTemplateSourceClass;

    @HOSTED_ONLY
    public VMAT1X() {
        super(VMAdviceBeforeAfterTemplateSource.class, getDefaultT1X());
    }

    private static T1XCompilationFactory getDefaultT1X() {
        new T1X();
        return new VMAT1XCompilationFactory();
    }

    @Override
    public void initialize(Phase phase) {
        instrumenting = VMAOptions.initialize(phase);
        stdT1X.initialize(phase);
        if (isHosted() && phase == Phase.HOSTED_COMPILING) {
            super.initialize(phase);
            RuntimeCompiler compiler = createBootCompiler();
            templateSource = BeforeTemplateSourceClass;
            beforeTemplates = createTemplates(compiler, templateSource, true, null);
            templateSource = AfterTemplateSourceClass;
            afterTemplates = createTemplates(compiler, templateSource, true, null);
        } else {
            super.initialize(phase);
        }
    }

    @Override
    public TargetMethod compile(ClassMethodActor method, boolean isDeopt, boolean install, CiStatistics stats) {
        if (instrumenting && !method.holder().isReflectionStub() && VMAOptions.instrumentForAdvising(method)) {
            return super.compile(method, isDeopt, install, stats);
        } else {
            return stdT1X.compile(method, false, install, stats);
        }
    }

    T1X getAltT1X() {
        return stdT1X;
    }

    // These will eventually be implemented
    private static final EnumSet<T1XTemplateTag>  TEMP_UNIMPLEMENTED_TEMPLATES = EnumSet.of(
                    NOP, BIPUSH, SIPUSH, LDC$int, LDC$long, LDC$float, LDC$double,
                    LDC$reference$resolved, ILOAD, LLOAD, FLOAD, DLOAD, ALOAD, ISTORE, LSTORE, FSTORE, DSTORE, ASTORE, POP, POP2, DUP, DUP_X1, DUP_X2, DUP2, DUP2_X1, DUP2_X2, SWAP,
                    IINC, IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE, IFNULL, IFNONNULL, GOTO, GOTO_W
    );

    private static final EnumSet<T1XTemplateTag>  AFTER_UNIMPLEMENTED_TEMPLATES = EnumSet.of(
                    INVOKESPECIAL$void$resolved,
                    INVOKESPECIAL$float$resolved,
                    INVOKESPECIAL$long$resolved,
                    INVOKESPECIAL$double$resolved,
                    INVOKESPECIAL$reference$resolved,
                    INVOKESPECIAL$word$resolved,
                    INVOKESTATIC$void$init,
                    INVOKESTATIC$float$init,
                    INVOKESTATIC$long$init,
                    INVOKESTATIC$double$init,
                    INVOKESTATIC$reference$init,
                    INVOKESTATIC$word$init
    );

    private static final EnumSet<T1XTemplateTag>  BEFORE_UNIMPLEMENTED_TEMPLATES = EnumSet.of(
                    INVOKEVIRTUAL$adviseafter,
                    INVOKEINTERFACE$adviseafter,
                    INVOKESPECIAL$adviseafter,
                    INVOKESTATIC$adviseafter
    );

    @Override
    protected boolean isUnimplemented(T1XTemplateTag tag) {
        if (TEMP_UNIMPLEMENTED_TEMPLATES.contains(tag)) {
            return true;
        }
        if (templateSource == BeforeTemplateSourceClass) {
            return BEFORE_UNIMPLEMENTED_TEMPLATES.contains(tag);
        } else if (templateSource == AfterTemplateSourceClass) {
            return AFTER_UNIMPLEMENTED_TEMPLATES.contains(tag);
        } else {
            return false;
        }
    }

}
