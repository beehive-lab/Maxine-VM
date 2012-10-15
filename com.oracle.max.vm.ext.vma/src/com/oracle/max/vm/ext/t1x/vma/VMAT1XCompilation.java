/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import com.oracle.max.vm.ext.t1x.*;
import com.oracle.max.vm.ext.t1x.T1XTemplateGenerator.*;
import com.oracle.max.vm.ext.t1x.amd64.*;
import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vm.ext.vma.options.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.hosted.*;
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

    public VMAT1XCompilation(T1X t1x) {
        super(t1x);
        this.vmaT1X = (VMAT1X) t1x;
        templates = vmaT1X.getAltT1X().templates;
        defaultTemplates = templates;
    }

    @Override
    protected T1XTargetMethod newT1XTargetMethod(T1XCompilation comp, boolean install) {
        return new VMAT1XTargetMethod(comp, install);
    }

    @Override
    public T1XTargetMethod compile(ClassMethodActor method, boolean isDeopt, boolean install) {
        // just to catch and report exceptions, VM will bailout to C1X but that is not helpful for VMA
        try {
            return super.compile(method, isDeopt, install);
        } catch (Error ex) {
            Log.print("VMA compilation of ");
            Log.printMethod(method, false);
            Log.print(" failed: ");
            Log.println(ex.getMessage());
            throw ex;
        }
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
                templates = defaultTemplates;
            }
        }
    }

    @Override
    protected T1XTemplate getTemplate(T1XTemplateTag tag) {
        return templates[tag.ordinal()];
    }

    @Override
    protected void finish() {
        if (templates != defaultTemplates && template.tag != null && template.tag != CREATE_MULTIANEWARRAY_DIMENSIONS) {
            // assign the bci value as the last argument
            assignInt(template.sig.in.length - 1, "bci", stream.currentBCI());
        }
        super.finish();
    }

    /*
     * If and, only if, we are advising the relevant invoke bytecode, we override the default parameter assignment
     * to always pass the MethodActor.
     */

    @Override
    protected void assignFieldAccessParameter(T1XTemplateTag tag, FieldActor fieldActor) {
        if (templates == defaultTemplates) {
            super.assignFieldAccessParameter(tag, fieldActor);
        } else {
            assignObject(1, "f", fieldActor);
        }
    }

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

    @Override
    protected void do_synchronizedMethodAcquire() {
        // Called without a call to processBytecode so have to select templates explicitly.
        if (method.isSynchronized()) {
            selectTemplates(VMABytecodes.MONITORENTER.ordinal());
        }
        super.do_synchronizedMethodAcquire();
    }

    @Override
    protected void do_synchronizedMethodHandler(ClassMethodActor method, int endBCI) {
        // Called without a call to processBytecode so have to select templates explicitly.
        if (method.isSynchronized()) {
            selectTemplates(VMABytecodes.MONITOREXIT.ordinal());
        }
        super.do_synchronizedMethodHandler(method, endBCI);
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
    protected void do_load(int index, Kind kind) {
        if (templates != defaultTemplates && kind.asEnum == KindEnum.REFERENCE) {
            start(ALOAD);
            assignInt(0, "index", index);
            assignInt(1, "localOffset", localSlotOffset(index, Kind.REFERENCE));
            finish();
            return;
        }
        if (templates != defaultTemplates) {
            T1XTemplateTag tag = null;
            // Checkstyle: stop
            switch (kind.asEnum) {
                case REFERENCE: tag = ALOAD; assert false; break;
                case INT: tag = ILOAD; break;
                case LONG: tag = LLOAD; break;
                case FLOAT: tag = FLOAD; break;
                case DOUBLE: tag = DLOAD; break;
            }
            // Checkstyle: resume
            start(tag);
            assignInt(0, "index", index);
            finish();
        }
        super.do_load(index, kind);
    }

    @Override
    protected void do_store(int index, Kind kind) {
        if (templates != defaultTemplates) {
            T1XTemplateTag tag = null;
            // Checkstyle: stop
            switch (kind.asEnum) {
                case REFERENCE: tag = ASTORE; break;
                case INT: tag = ISTORE; break;
                case LONG: tag = LSTORE; break;
                case FLOAT: tag = FSTORE; break;
                case DOUBLE: tag = DSTORE; break;
            }
            // Checkstyle: resume
            start(tag);
            assignInt(0, "index", index);
            CiRegister reg = reg(1, "value", kind);
            switch(kind.asEnum) {
                case INT:
                case FLOAT:
                    peekInt(reg, 0);
                    break;
                case REFERENCE:
                    peekWord(reg, 0);
                    break;
                case LONG:
                case DOUBLE:
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
    protected void do_branch(int opcode, int targetBci) {
        if (templates != defaultTemplates) {
            T1XTemplateTag tag = branchTagMap.get(opcode);
            switch (tag) {
                case GOTO:
                case GOTO_W:
                    start(tag);
                    assignTargetBci(targetBci);
                    finish();
                    break;
                case IFNULL: case IFNONNULL: {
                    start(tag);
                    CiRegister reg = reg(0, "value1", Kind.REFERENCE);
                    peekObject(reg, 0);
                    assignTargetBci(targetBci);
                    finish();
                    break;
                }

                case IFEQ: case IFNE: case IFLT: case IFLE: case IFGE: case IFGT: {
                    start(tag);
                    CiRegister reg = reg(0, "value1", Kind.INT);
                    peekInt(reg, 0);
                    assignTargetBci(targetBci);
                    finish();
                    break;
                }

                case IF_ICMPEQ: case IF_ICMPNE: case IF_ICMPLT: case IF_ICMPGE:
                case IF_ICMPGT: case IF_ICMPLE: {
                    start(tag);
                    CiRegister reg1 = reg(0, "value1", Kind.INT);
                    CiRegister reg2 = reg(1, "value2", Kind.INT);
                    peekInt(reg1, 1);
                    peekInt(reg2, 0);
                    assignTargetBci(targetBci);
                    finish();
                    break;
                }

                case IF_ACMPEQ: case IF_ACMPNE: {
                    start(tag);
                    CiRegister reg1 = reg(0, "value1", Kind.REFERENCE);
                    CiRegister reg2 = reg(1, "value2", Kind.REFERENCE);
                    peekObject(reg1, 1);
                    peekObject(reg2, 0);
                    assignTargetBci(targetBci);
                    finish();
                    break;
                }

            }
        }
        super.do_branch(opcode, targetBci);
    }

    private void assignTargetBci(int targetBci) {
        assignInt(template.sig.in.length - 2, "targetBci", targetBci);
    }

    @HOSTED_ONLY
    private static class InitializationCompleteCallback implements JavaPrototype.InitializationCompleteCallback {

        @Override
        public void initializationComplete() {
            makeInvokeAfterTagMap();
            makeIfTagMap();
        }

    }

    static {
        JavaPrototype.registerInitializationCompleteCallback(new InitializationCompleteCallback());
    }

    /*
     * Support for finding the INVOKE adviseafter tag quickly.
     */
    private static final EnumMap<T1XTemplateTag, T1XTemplateTag> invokeAfterTagMap = new EnumMap<T1XTemplateTag, T1XTemplateTag>(T1XTemplateTag.class);

    @HOSTED_ONLY
    private static void makeInvokeAfterTagMap() {
        addToInvokeAfterTagMap(T1XTemplateTag.INVOKEINTERFACES, INVOKEINTERFACE$adviseafter);
        addToInvokeAfterTagMap(T1XTemplateTag.INVOKEVIRTUALS, INVOKEVIRTUAL$adviseafter);
        addToInvokeAfterTagMap(T1XTemplateTag.INVOKESPECIALS, INVOKESPECIAL$adviseafter);
        addToInvokeAfterTagMap(T1XTemplateTag.INVOKESTATICS, INVOKESTATIC$adviseafter);
    }

    @HOSTED_ONLY
    private static void addToInvokeAfterTagMap(EnumMap<KindEnum, T1XTemplateTag> map, T1XTemplateTag value) {
        for (T1XTemplateTag tag : map.values()) {
            invokeAfterTagMap.put(tag, value);
        }
    }

    // reverse map from opcode to IF template tag

    private static final Map<Integer, T1XTemplateTag> branchTagMap = new HashMap<Integer, T1XTemplateTag>();

    private static final EnumSet<T1XTemplateTag> IF_TEMPLATE_TAGS = EnumSet.of(
                    IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE,
                    IFEQ, IFNE, IFLT, IFLE, IFGE, IFGT, IFNULL, IFNONNULL);

    private static final EnumSet<T1XTemplateTag> GOTO_TEMPLATE_TAGS = EnumSet.of(GOTO, GOTO_W);

    @HOSTED_ONLY
    private static void makeIfTagMap() {
        for (T1XTemplateTag tag : IF_TEMPLATE_TAGS) {
            branchTagMap.put(tag.opcode, tag);
        }
        for (T1XTemplateTag tag : GOTO_TEMPLATE_TAGS) {
            branchTagMap.put(tag.opcode, tag);
        }
    }

}
