/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.vm.t1x.T1XTemplateTag.*;

import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vm.ext.vma.options.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.t1x.*;
import com.sun.max.vm.t1x.T1XTemplateGenerator.AdviceType;
import com.sun.max.vm.type.*;

/**
 * Overrides some {@link T1XCompilation} methods to provide finer compile time control over advising.
 * N.B. Entry to this class can only occur if some advising is occurring in the method being compiled.
 * For classes that are being advised, A decision is made as each bytecode is compiled whether to use the
 * advice template or whether to use the standard template. This decision is based on the options
 * set in {@link VMAOptions}. Note that this cannot handle selecting {@link AdviceMode before/after}
 * advice at runtime, as the template has both compiled in at boot image generation time.
 * To handle this we (potentially) generate three instances of a template with the
 * appropriate advice generated, and select between those at runtime.
 *
 */
public class VMAT1XCompilation extends T1XCompilation {

    private static final int BEFORE_INDEX = AdviceType.BEFORE.ordinal();
    private static final int AFTER_INDEX = AdviceType.AFTER.ordinal();

    private VMAT1X vmaT1X;
    /**
     * The specific templates to be used for processing the current bytecode,
     * based on whether the bytecode is being advised and what kind of advice is
     * being applied.
      */
    private T1XTemplate[] templates;
    private final T1XTemplate[] defaultTemplates;

    public VMAT1XCompilation(T1X t1x) {
        super(t1x);
        this.vmaT1X = (VMAT1X) t1x;
        templates = vmaT1X.getAltT1X().templates;
        defaultTemplates = templates;
    }

    @Override
    protected void initCompile(ClassMethodActor method, CodeAttribute codeAttribute) {
        super.initCompile(method, codeAttribute);
        // we do not want code to be recompiled as the optimizing compiler does not
        // currently support advising.
        methodProfileBuilder = null;
        // Simulate the method entry, so that emitMethodTraceEntry gets the right template
        selectTemplates(VMABytecodes.MENTRY.ordinal());
    }


    @Override
    protected void processBytecode(int opcode) throws InternalError {
        // Based on the option settings for this bytecode, we choose the correct templates
        selectTemplates(opcode);
        super.processBytecode(opcode);
    }

    private void selectTemplates(int opcode) {
        boolean[] adviceTypeOptions = VMAOptions.getVMATemplateOptions(opcode);
        if (adviceTypeOptions[BEFORE_INDEX]) {
            if (adviceTypeOptions[AFTER_INDEX]) {
                templates = vmaT1X.templates;
            } else {
                templates = vmaT1X.beforeTemplates;
            }
        } else {
            if (adviceTypeOptions[AFTER_INDEX]) {
                templates = vmaT1X.afterTemplates;
            } else {
                templates = vmaT1X.getAltT1X().templates;
            }
        }
    }

    @Override
    protected T1XTemplate getTemplate(T1XTemplateTag tag) {
        return templates[tag.ordinal()];
    }

    /*
     * If and, only if, we are advising the relevant invoke bytecode, we override the default parameter assignment
     * to always pass the MethodActor.
     */

    @Override
    protected void assignInvokeVirtualTemplateParameters(T1XTemplate template, VirtualMethodActor virtualMethodActor, int receiverStackIndex) {
        if (templates == defaultTemplates) {
            super.assignInvokeVirtualTemplateParameters(template, virtualMethodActor, receiverStackIndex);
        } else {
            assignTemplateParameters(template, virtualMethodActor, receiverStackIndex);
        }
    }

    @Override
    protected void assignInvokeInterfaceTemplateParameters(T1XTemplate template, InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        if (templates == defaultTemplates) {
            super.assignInvokeInterfaceTemplateParameters(template, interfaceMethodActor, receiverStackIndex);
        } else {
            assignTemplateParameters(template, interfaceMethodActor, receiverStackIndex);
        }
    }

    @Override
    protected void assignInvokeSpecialTemplateParameters(T1XTemplate template, VirtualMethodActor virtualMethodActor, int receiverStackIndex) {
        if (templates == defaultTemplates) {
            super.assignInvokeSpecialTemplateParameters(template, virtualMethodActor, receiverStackIndex);
        } else {
            assignTemplateParameters(template, virtualMethodActor, receiverStackIndex);
        }
    }

    @Override
    protected void assignInvokeStaticTemplateParameters(T1XTemplate template, StaticMethodActor staticMethodActor) {
        if (templates == defaultTemplates) {
            super.assignInvokeStaticTemplateParameters(template, staticMethodActor);
        } else {
            assignTemplateParameters(template, staticMethodActor);
        }
    }

    @Override
    protected void emitMethodTraceEntry() {
        // We turn this into advice if we are advising
        if (templates == defaultTemplates) {
            super.emitMethodTraceEntry();
        } else {
            T1XTemplate template = getTemplate(TRACE_METHOD_ENTRY);
            assignReferenceLiteralTemplateArgument(0, method);
            if (method.isStatic()) {
                assignIntTemplateArgument(1, 0);
            } else {
                assignLocalDisplacementTemplateArgument(1, 0, Kind.REFERENCE);
            }
            emitAndRecordSafepoints(template);
        }
    }
}
