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

import static com.oracle.max.vm.ext.t1x.T1XTemplateTag.*;

import java.util.*;

import com.oracle.max.vm.ext.t1x.*;
import com.oracle.max.vm.ext.t1x.T1XTemplateGenerator.*;
import com.oracle.max.vm.ext.t1x.amd64.*;
import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vm.ext.vma.options.*;
import com.sun.cri.ci.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;

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
public class VMAT1XCompilation extends AMD64T1XCompilation {

    private static final int BEFORE_INDEX = AdviceType.BEFORE.ordinal();
    private static final int AFTER_INDEX = AdviceType.AFTER.ordinal();

    private VMAT1X vmaT1X;
    /**
     * The specific templates to be used for processing the current bytecode,
     * based on whether the bytecode is being advised and what kind of advice is
     * being applied.
      */
    private T1XTemplate[] templates;
    /**
     * The templates in use by the default T1X compiler.
     */
    private final T1XTemplate[] defaultTemplates;
    /**
     * If non-null, this denotes the tag for AFTER advice for an INVOKE.
     * The after advice is actually prefixed to the code for the bytecode
     * after the invoke.
     */
    private T1XTemplateTag invokeAfterTag;

    public VMAT1XCompilation(T1X t1x) {
        super(t1x);
        this.vmaT1X = (VMAT1X) t1x;
        templates = vmaT1X.getAltT1X().templates;
        defaultTemplates = templates;
        makeInvokeAfterTagMap();
        makeIfTagMap();
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
    protected void beginBytecode(int opcode) {
        super.beginBytecode(opcode);
        if (invokeAfterTag != null) {
            start(invokeAfterTag);
            finish();
            invokeAfterTag = null;
        }
        // Based on the option settings for this bytecode, we choose the correct templates
        selectTemplates(opcode);
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
    protected void assignInvokeVirtualTemplateParameters(VirtualMethodActor virtualMethodActor, int receiverStackIndex) {
        if (templates == defaultTemplates) {
            super.assignInvokeVirtualTemplateParameters(virtualMethodActor, receiverStackIndex);
        } else {
            assignObject(0, "methodActor", virtualMethodActor);
            peekObject(1, "receiver", receiverStackIndex);
        }
    }

    @Override
    protected void do_invokespecial_resolved(T1XTemplateTag tag, VirtualMethodActor virtualMethodActor, int receiverStackIndex) {
        if (templates == defaultTemplates) {
            super.do_invokespecial_resolved(tag, virtualMethodActor, receiverStackIndex);
        } else {
            start(tag.resolved);
            assignObject(0, "methodActor", virtualMethodActor);
            peekObject(1, "receiver", receiverStackIndex);
            finish();
        }
    }

    @Override
    protected void do_invokestatic_resolved(T1XTemplateTag tag, StaticMethodActor staticMethodActor) {
        if (templates == defaultTemplates) {
            super.do_invokestatic_resolved(tag, staticMethodActor);
        } else {
            start(tag.initialized);
            assignObject(0, "methodActor", staticMethodActor);
            finish();
        }
    }

    @Override
    protected void finishCall(T1XTemplateTag tag, CiKind returnKind, int safepoint, ClassMethodActor directCallee) {
        super.finishCall(tag, returnKind, safepoint, directCallee);
        // check if after advice required and if so, set invokeAfterTag
        if (templates == vmaT1X.templates || templates == vmaT1X.afterTemplates) {
            invokeAfterTag = invokeAfterTagMap.get(tag);
        }
    }
    @Override
    protected void do_methodTraceEntry() {
        // We turn this into advice if we are advising
        if (templates == defaultTemplates) {
            super.do_methodTraceEntry();
        } else {
            start(TRACE_METHOD_ENTRY);
            assignObject(0, "methodActor", method);
            if (method.isStatic()) {
                assignObject(1, "receiver", null);
            } else {
                loadObject(1, "receiver", 0);
            }
            finish();
        }
    }

    /*
     * The following overrides handle before advice for bytecodes that, by default in T1X,
     * do not have an associated template.
     *
     * Typically we emit the associated template and then just call the super method to do the normal work.
     */

    private void checkDoBeforeTemplate(T1XTemplateTag tag) {
        if (templates != defaultTemplates) {
            start(tag);
            finish();
        }
    }

    @Override
    protected void do_pop() {
        checkDoBeforeTemplate(POP);
        super.do_pop();
    }

    @Override
    protected void do_pop2() {
        checkDoBeforeTemplate(POP2);
        super.do_pop2();
    }

    @Override
    protected void do_dup() {
        checkDoBeforeTemplate(DUP);
        super.do_dup();
    }

    @Override
    protected void do_dup_x1() {
        checkDoBeforeTemplate(DUP_X1);
        super.do_dup_x1();
    }

    @Override
    protected void do_dup_x2() {
        checkDoBeforeTemplate(DUP_X2);
        super.do_dup_x2();
    }

    @Override
    protected void do_dup2() {
        checkDoBeforeTemplate(DUP2);
        super.do_dup2();
    }

    @Override
    protected void do_dup2_x1() {
        checkDoBeforeTemplate(DUP2_X1);
        super.do_dup2_x1();
    }

    @Override
    protected void do_dup2_x2() {
        checkDoBeforeTemplate(DUP2_X2);
        super.do_dup_x2();
    }

    @Override
    protected void do_swap() {
        checkDoBeforeTemplate(SWAP);
        super.do_swap();
    }

    @Override
    protected void do_oconst(Object value) {
        super.do_oconst(value);
    }

    @Override
    protected void do_iconst(int value) {
        if (templates != defaultTemplates) {
            start(ICONST);
            assignInt(0, "constant", value);
            finish();
        }
        super.do_iconst(value);
    }

    @Override
    protected void do_dconst(double value) {
        if (templates != defaultTemplates) {
            start(DCONST);
            assignDouble(0, "constant", value);
            finish();
        }
        super.do_dconst(value);
    }

    @Override
    protected void do_fconst(float value) {
        if (templates != defaultTemplates) {
            start(FCONST);
            assignFloat(0, "constant", value);
            finish();
        }
        super.do_fconst(value);
    }

    @Override
    protected void do_lconst(long value) {
        if (templates != defaultTemplates) {
            start(LCONST);
            assignLong(0, "constant", value);
            finish();
        }
        super.do_lconst(value);
    }

    @Override
    protected void do_iinc(int index, int increment) {
        if (templates != defaultTemplates) {
            start(IINC);
            assignInt(0, "index", index);
            assignInt(1, "increment", increment);
            finish();
        }
        super.do_iinc(index, increment);
    }

    @Override
    protected void do_load(int index, CiKind kind) {
        if (templates != defaultTemplates) {
            T1XTemplateTag tag = null;
            // Checkstyle: stop
            switch (kind) {
                case Object: tag = ALOAD; break;
                case Int: tag = ILOAD; break;
                case Long: tag = LLOAD; break;
                case Float: tag = FLOAD; break;
                case Double: tag = DLOAD; break;
                case Word: tag = WLOAD; break;
            }
            // Checkstyle: resume
            start(tag);
            assignInt(0, "index", index);
            finish();
        }
        super.do_load(index, kind);
    }

    @Override
    protected void do_store(int index, CiKind kind) {
        if (templates != defaultTemplates) {
            T1XTemplateTag tag = null;
            // Checkstyle: stop
            switch (kind) {
                case Object: tag = ASTORE; break;
                case Int: tag = ISTORE; break;
                case Long: tag = LSTORE; break;
                case Float: tag = FSTORE; break;
                case Double: tag = DSTORE; break;
                case Word: tag = WSTORE; break;
            }
            // Checkstyle: resume
            start(tag);
            assignInt(0, "index", index);
            CiRegister reg = reg(1, "value", kind);
            switch(kind) {
                case Int:
                case Float:
                    peekInt(reg, 0);
                    break;
                case Word:
                case Object:
                    peekWord(reg, 0);
                    break;
                case Long:
                case Double:
                    peekLong(reg, 0);
                    break;
                default:
                    throw new InternalError("Unexpected kind: " + kind);
            }
            finish();
        }
        super.do_store(index, kind);
    }

    @Override
    protected void do_branch(int opcode, int targetBCI) {
        if (templates != defaultTemplates) {
            T1XTemplateTag tag = branchTagMap.get(opcode);
            switch (tag) {
                case GOTO:
                case GOTO_W:
                    emit(tag);
                    break;
                case IFNULL: case IFNONNULL: {
                    start(tag);
                    CiRegister reg = reg(0, "value1", CiKind.Object);
                    peekObject(reg, 0);
                    finish();
                    break;
                }

                case IFEQ: case IFNE: case IFLT: case IFLE: case IFGE: case IFGT: {
                    start(tag);
                    CiRegister reg = reg(0, "value1", CiKind.Int);
                    peekInt(reg, 0);
                    finish();
                    break;
                }

                case IF_ICMPEQ: case IF_ICMPNE: case IF_ICMPLT: case IF_ICMPGE:
                case IF_ICMPGT: case IF_ICMPLE: case IF_ACMPEQ: case IF_ACMPNE: {
                    start(tag);
                    CiRegister reg1 = reg(0, "value1", CiKind.Int);
                    CiRegister reg2 = reg(1, "value2", CiKind.Int);
                    peekInt(reg1, 1);
                    peekInt(reg2, 0);
                    finish();
                    break;
                }
            }
        }
        super.do_branch(opcode, targetBCI);
    }

    /*
     * Support for finding the INVOKE adviseafter tag quickly.
     */
    private static EnumMap<T1XTemplateTag, T1XTemplateTag> invokeAfterTagMap;

    private static void makeInvokeAfterTagMap() {
        invokeAfterTagMap = new EnumMap<T1XTemplateTag, T1XTemplateTag>(T1XTemplateTag.class);
        addToInvokeAfterTagMap(T1XTemplateTag.INVOKEINTERFACES, INVOKEINTERFACE$adviseafter);
        addToInvokeAfterTagMap(T1XTemplateTag.INVOKEVIRTUALS, INVOKEVIRTUAL$adviseafter);
        addToInvokeAfterTagMap(T1XTemplateTag.INVOKESPECIALS, INVOKESPECIAL$adviseafter);
        addToInvokeAfterTagMap(T1XTemplateTag.INVOKESTATICS, INVOKESTATIC$adviseafter);
    }

    private static void addToInvokeAfterTagMap(EnumMap<CiKind, T1XTemplateTag> map, T1XTemplateTag value) {
        for (T1XTemplateTag tag : map.values()) {
            invokeAfterTagMap.put(tag, value);
        }
    }

    // reverse map from opcode to IF template tag

    private static Map<Integer, T1XTemplateTag> branchTagMap;

    private static final EnumSet<T1XTemplateTag> IF_TEMPLATE_TAGS = EnumSet.of(
                    IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE,
                    IFEQ, IFNE, IFLT, IFLE, IFGE, IFGT, IFNULL, IFNONNULL);

    private static final EnumSet<T1XTemplateTag> GOTO_TEMPLATE_TAGS = EnumSet.of(GOTO, GOTO_W);

    private static void makeIfTagMap() {
        branchTagMap = new HashMap<Integer, T1XTemplateTag>();
        for (T1XTemplateTag tag : IF_TEMPLATE_TAGS) {
            branchTagMap.put(tag.opcode, tag);
        }
        for (T1XTemplateTag tag : GOTO_TEMPLATE_TAGS) {
            branchTagMap.put(tag.opcode, tag);
        }
    }

}
