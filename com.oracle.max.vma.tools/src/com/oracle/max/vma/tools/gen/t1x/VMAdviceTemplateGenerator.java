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
package com.oracle.max.vma.tools.gen.t1x;

import static com.oracle.max.vm.ext.t1x.T1XTemplateTag.*;

import java.io.*;
import java.util.*;

import com.oracle.max.vm.ext.t1x.*;
import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vm.ext.vma.run.java.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.t1x.vma.*;
import com.oracle.max.vma.tools.gen.vma.AdviceGeneratorHelper;

/**
 * Template generation that supports the {@link VMAdviceHandler} interface.
 * Note that not all templates support both before/after advice but every one supports
 * at least one of them. Accordingly, this program can generate several output variants
 * so as to provide runtime flexibility and avoid unnecessary duplication:
 * <ul>
 * <li>All templates generated with whatever before/advice is available. This essentially parallels what is
 * produced by {@link T1XTemplateGenerator} in normal mode.
 * <li>Only the templates that provide before advice, with only before advice generated.
 * <li>Only the templates that provide after advice, with only after advice generated.
 * <li>Only the templates that provide before and after advice, with before and advice generated.
 * </ul>
 *
 * Unfortunately, we cannot reuse the standard templates for INVOKE as we need consistent access to the
 * {@link MethodActor} to pass to the advice handler. So we override the standard definitions in this class.
 * {@link T1X} doesn't use templates for many of the simple bytecodes, or some of the INVOKE bytecodes,
 * any more, which complicates the implementation as we have to generate templates for those directly.
 *
 * Two different mechanisms can be generated for testing whether the advice methods should be invoked.
 * All ultimately test bit zero of the {@klink VMAJavaRunScheme#VM_ADVISING} thread local.
 * The first mechanism is to include the body of {@link VMAJavaRunScheme#isAdvising} which is an
 * {@code INLINE} method that tests the bit using standard Java. The reason we don't just rely on the
 * inlining mechanism is that the generated code is less efficient than the manually inlined version
 * owing to a C1X limitation.
 *
 * The second, and default, mechanism uses an {@code INTRINSIC} method that tests the bit explicitly
 * and branches directly on its value. This is preferable because it avoids the use of a register to
 * load the value for a standard comparison .
 */
@HOSTED_ONLY
public class VMAdviceTemplateGenerator extends T1XTemplateGenerator {

    /**
     * This records the advice capability for each {@link T1XTemplateTag}.
     */
    private static boolean[][] tagAdviceCapabilities = new boolean[T1XTemplateTag.values().length][AdviceType.values().length];

    private static class DiscoverCapabilitiesHook implements AdviceHook {

        public void startMethodGeneration() {
        }

        public void generate(T1XTemplateTag tag, AdviceType at, Object... args) {
            tagAdviceCapabilities[tag.ordinal()][at.ordinal()] = true;
        }
    }

    private static class MethodStatus {
        int offset; // in the ByteArrayOutputStream array
        boolean output; // are we outputting this method
        T1XTemplateTag tag; // that this method is associated with
        MethodStatus(int offset) {
            this.offset = offset;
            this.output = true;
        }
    }

    private static class ThisByteArrayOutputStream extends ByteArrayOutputStream {
        ArrayList<MethodStatus> methodStatusList;

        void writeOut() {
            System.out.print(toString());
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            int listLength = methodStatusList.size();
            for (int i = 0; i < listLength; i++) {
                MethodStatus methodStatus = methodStatusList.get(i);
                if (methodStatus.output) {
                    sb.append(new String(buf, methodStatus.offset, (i == listLength - 1 ? size() : methodStatusList.get(i + 1).offset) - methodStatus.offset));
                }
            }
            return sb.toString();
        }

        void addMethodStatus() {
            methodStatusList.add(new MethodStatus(size()));
        }

        /**
         *  Discard this and any intervening untagged (helper) methods.
         * @param tag
         */
        void discard(T1XTemplateTag tag) {
            final int listLength  = methodStatusList.size();
            //Checkstyle: stop Indentation check
            methodStatusList.get(listLength - 1).output = false;
            //Checkstyle: resume Indentation check
            int index = listLength - 2;
            while (index >= 0) {
                MethodStatus ms = methodStatusList.get(index);
                if (ms.tag == null) {
                    ms.output = false;
                } else if (ms.tag != tag) {
                    return;
                }
                index--;
            }
        }

        void setTag(T1XTemplateTag tag) {
            //Checkstyle: stop Indentation check
            methodStatusList.get(methodStatusList.size() - 1).tag = tag;
            //Checkstyle: resume Indentation check

        }

        @Override
        public void reset() {
            super.reset();
            methodStatusList = new ArrayList<MethodStatus>();
        }
    }

    private static final String INDENT8_PREFIX = "        ";
    private static final String INDENT12_PREFIX = "            ";
    private static final String ADVISE_PREFIX = "VMAStaticBytecodeAdvice.advise%s%s";
    private static final String INDENT12_ADVISE_PREFIX = INDENT12_PREFIX + ADVISE_PREFIX;
    private static AdviceType adviceType;
    private static String methodName;
    private static ThisByteArrayOutputStream byteArrayOut;

    private static final String ISADVISING = "VMAJavaRunScheme.isAdvising()";
    /**
     * We explicitly inline the above as the automatic inlining currently generates sub-optimal code.
     */
    private static final String INLINE_ISADVISING = "VmThread.currentTLA().getWord(VMAJavaRunScheme.VM_ADVISING.index) != Word.zero()";
    /**
     * This is the version that gets optimal code via the BT instruction.
     */
    private static final String ALT_ISADVISING = "Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)";
    /**
     * If {@code true}, generate bt instruction variant of the advice guard.
     */
    private static boolean bitGuard = true;

    /**
     * Records whether we want to generate a particular advice type.
     */
    private static boolean[] generating = new boolean[AdviceType.values().length];

    private static boolean hasBeforeAndAfter(T1XTemplateTag tag) {
        for (AdviceType at : AdviceType.values()) {
            if (!tagAdviceCapabilities[tag.ordinal()][at.ordinal()]) {
                return false;
            }
        }
        return true;
    }

    private class VMAT1XAdvice implements AdviceHook {

        public void startMethodGeneration() {
            byteArrayOut.addMethodStatus();
        }

        private boolean generateTag(T1XTemplateTag tag, AdviceType at) {
            // otherwise we only generate if the tag (a) has the capability for the requested advice type
            // and (b) the program is being run to generate that kind of advice.
            return tagAdviceCapabilities[tag.ordinal()][at.ordinal()] && generating[at.ordinal()];
        }

        public void generate(T1XTemplateTag tag, AdviceType at, Object... args) {
            byteArrayOut.setTag(tag);
            if (!generateTag(tag, at)) {
                byteArrayOut.discard(tag);
                return;
            }
            adviceType = at;
            methodName = tag.opcode >= 0 ? AdviceGeneratorHelper.codeMap.get(tag.opcode).methodName : "???";

            String k = null;
            if (args.length > 0) {
                if (args[0] instanceof CiKind) {
                    k = ((CiKind) args[0]).javaName;
                } else {
                    assert args[0] instanceof String;
                    k = (String) args[0];
                }
            }
            switch (tag) {
                case GETFIELD$byte:
                case GETFIELD$boolean:
                case GETFIELD$char:
                case GETFIELD$short:
                case GETFIELD$int:
                case GETFIELD$float:
                case GETFIELD$long:
                case GETFIELD$double:
                case GETFIELD$reference:
                case GETFIELD$word:
                    assert adviceType == AdviceType.BEFORE;
                    generateGetField(k, false);
                    break;

                case GETFIELD$byte$resolved:
                case GETFIELD$boolean$resolved:
                case GETFIELD$char$resolved:
                case GETFIELD$short$resolved:
                case GETFIELD$int$resolved:
                case GETFIELD$float$resolved:
                case GETFIELD$long$resolved:
                case GETFIELD$double$resolved:
                case GETFIELD$reference$resolved:
                case GETFIELD$word$resolved:
                    assert adviceType == AdviceType.BEFORE;
                    generateGetField(k, true);
                    break;

                case PUTFIELD$byte:
                case PUTFIELD$boolean:
                case PUTFIELD$char:
                case PUTFIELD$short:
                case PUTFIELD$int:
                case PUTFIELD$float:
                case PUTFIELD$long:
                case PUTFIELD$double:
                case PUTFIELD$reference:
                case PUTFIELD$word:
                    assert adviceType == AdviceType.BEFORE;
                    generatePutField(k, false);
                    break;

                case PUTFIELD$byte$resolved:
                case PUTFIELD$boolean$resolved:
                case PUTFIELD$char$resolved:
                case PUTFIELD$short$resolved:
                case PUTFIELD$int$resolved:
                case PUTFIELD$float$resolved:
                case PUTFIELD$long$resolved:
                case PUTFIELD$double$resolved:
                case PUTFIELD$reference$resolved:
                case PUTFIELD$word$resolved:
                    assert adviceType == AdviceType.BEFORE;
                    generatePutField(k, true);
                    break;

                case GETSTATIC$byte:
                case GETSTATIC$boolean:
                case GETSTATIC$char:
                case GETSTATIC$short:
                case GETSTATIC$int:
                case GETSTATIC$float:
                case GETSTATIC$long:
                case GETSTATIC$double:
                case GETSTATIC$reference:
                case GETSTATIC$word:
                    assert adviceType == AdviceType.BEFORE;
                    generateGetStatic(k, false);
                    break;

                case GETSTATIC$byte$init:
                case GETSTATIC$boolean$init:
                case GETSTATIC$char$init:
                case GETSTATIC$short$init:
                case GETSTATIC$int$init:
                case GETSTATIC$float$init:
                case GETSTATIC$long$init:
                case GETSTATIC$double$init:
                case GETSTATIC$reference$init:
                case GETSTATIC$word$init:
                    assert adviceType == AdviceType.BEFORE;
                    generateGetStatic(k, true);
                    break;

                case PUTSTATIC$byte:
                case PUTSTATIC$boolean:
                case PUTSTATIC$char:
                case PUTSTATIC$short:
                case PUTSTATIC$int:
                case PUTSTATIC$float:
                case PUTSTATIC$long:
                case PUTSTATIC$double:
                case PUTSTATIC$reference:
                case PUTSTATIC$word:
                    assert adviceType == AdviceType.BEFORE;
                    generatePutStatic(k, false);
                    break;

                case PUTSTATIC$byte$init:
                case PUTSTATIC$boolean$init:
                case PUTSTATIC$char$init:
                case PUTSTATIC$short$init:
                case PUTSTATIC$int$init:
                case PUTSTATIC$float$init:
                case PUTSTATIC$long$init:
                case PUTSTATIC$double$init:
                case PUTSTATIC$reference$init:
                case PUTSTATIC$word$init:
                    assert adviceType == AdviceType.BEFORE;
                    generatePutStatic(k, true);
                    break;

                case IALOAD:
                case LALOAD:
                case FALOAD:
                case DALOAD:
                case AALOAD:
                case BALOAD:
                case CALOAD:
                case SALOAD:
                    assert adviceType == AdviceType.BEFORE;
                    generateArrayLoad(k);
                    break;

                case IASTORE:
                case LASTORE:
                case FASTORE:
                case DASTORE:
                case AASTORE:
                case BASTORE:
                case CASTORE:
                case SASTORE:
                    assert adviceType == AdviceType.BEFORE;
                    generateArrayStore(k);
                    break;

                case WCONST_0:
                case ACONST_NULL:
                    assert adviceType == AdviceType.BEFORE;
                    generateLoadConst(tag, k, null);
                    break;

                case ICONST:
                case FCONST:
                case DCONST:
                case LCONST:
                    assert adviceType == AdviceType.BEFORE;
                    generateLoadConst(tag, k, null);
                    break;

                case ILOAD:
                case LLOAD:
                case FLOAD:
                case DLOAD:
                case ALOAD:
                case WLOAD:
                    assert adviceType == AdviceType.BEFORE;
                    generateLoad(k);
                    break;

                case ISTORE:
                case LSTORE:
                case FSTORE:
                case DSTORE:
                case ASTORE:
                case WSTORE:
                    assert adviceType == AdviceType.BEFORE;
                    generateStore(k);
                    break;

                case BIPUSH:
                case SIPUSH:
                    assert adviceType == AdviceType.BEFORE;
                    generateIPush(k);
                    break;

                case IADD:
                case LADD:
                case FADD:
                case DADD:
                case ISUB:
                case LSUB:
                case FSUB:
                case DSUB:
                case IMUL:
                case LMUL:
                case FMUL:
                case DMUL:
                case IDIV:
                case LDIV:
                case FDIV:
                case DDIV:
                case IREM:
                case LREM:
                case FREM:
                case DREM:
                case INEG:
                case LNEG:
                case FNEG:
                case DNEG:
                case ISHL:
                case LSHL:
                case ISHR:
                case LSHR:
                case IUSHR:
                case LUSHR:
                case IAND:
                case LAND:
                case IOR:
                case LOR:
                case IXOR:
                case LXOR:
                case LCMP:
                case FCMPL:
                case FCMPG:
                case DCMPL:
                case DCMPG:
                    assert adviceType == AdviceType.BEFORE;
                    generateOperation(tag, k);
                    break;

                case WDIV:
                case WDIVI:
                case WREM:
                case WREMI:
                    assert adviceType == AdviceType.BEFORE;
                    generateWOperation(tag);
                    break;

                case IINC:
                    assert adviceType == AdviceType.BEFORE;
                    generateIInc();
                    break;

                case I2L:
                case I2F:
                case I2D:
                case L2I:
                case L2F:
                case L2D:
                case F2I:
                case F2L:
                case F2D:
                case D2I:
                case D2L:
                case D2F:
                case I2B:
                case I2C:
                case I2S:
                case MOV_I2F:
                case MOV_F2I:
                case MOV_L2D:
                case MOV_D2L:
                    assert adviceType == AdviceType.BEFORE;
                    generateConversion(tag, k);
                    break;

                case IFEQ:
                case IFNE:
                case IFLT:
                case IFGE:
                case IFGT:
                case IFLE:
                case IFNULL:
                case IFNONNULL:
                case IF_ICMPEQ:
                case IF_ICMPNE:
                case IF_ICMPLT:
                case IF_ICMPGE:
                case IF_ICMPGT:
                case IF_ICMPLE:
                case IF_ACMPEQ:
                case IF_ACMPNE:
                    assert adviceType == AdviceType.BEFORE;
                    generateIf(tag);
                    break;

                case CHECKCAST:
                case CHECKCAST$resolved:
                case INSTANCEOF:
                case INSTANCEOF$resolved:
                    assert adviceType == AdviceType.BEFORE;
                    generateTypeCheck(tag);
                    break;


                case NEW:
                case NEW$init:
                    assert adviceType == AdviceType.AFTER;
                    generateNew();
                    break;

                case NEWARRAY:
                case ANEWARRAY:
                case ANEWARRAY$resolved:
                    assert adviceType == AdviceType.AFTER;
                    generateNewUniArray();
                    break;

                case MULTIANEWARRAY:
                case MULTIANEWARRAY$resolved:
                    assert adviceType == AdviceType.AFTER;
                    generateNewMultiArray();
                    break;

                case INVOKEVIRTUAL$void:
                case INVOKEVIRTUAL$float:
                case INVOKEVIRTUAL$long:
                case INVOKEVIRTUAL$double:
                case INVOKEVIRTUAL$word:
                case INVOKEVIRTUAL$reference:
                case INVOKEVIRTUAL$void$resolved:
                case INVOKEVIRTUAL$float$resolved:
                case INVOKEVIRTUAL$long$resolved:
                case INVOKEVIRTUAL$double$resolved:
                case INVOKEVIRTUAL$word$resolved:
                case INVOKEVIRTUAL$reference$resolved:
                case INVOKEVIRTUAL$void$instrumented:
                case INVOKEVIRTUAL$float$instrumented:
                case INVOKEVIRTUAL$long$instrumented:
                case INVOKEVIRTUAL$double$instrumented:
                case INVOKEVIRTUAL$word$instrumented:
                case INVOKEVIRTUAL$reference$instrumented:
                    if (adviceType == AdviceType.BEFORE) {
                        generateInvokeVirtual();
                    }
                    break;

                case INVOKESPECIAL$void:
                case INVOKESPECIAL$float:
                case INVOKESPECIAL$long:
                case INVOKESPECIAL$double:
                case INVOKESPECIAL$word:
                case INVOKESPECIAL$reference:
                case INVOKESPECIAL$void$resolved:
                case INVOKESPECIAL$float$resolved:
                case INVOKESPECIAL$long$resolved:
                case INVOKESPECIAL$double$resolved:
                case INVOKESPECIAL$word$resolved:
                case INVOKESPECIAL$reference$resolved:
                    if (adviceType == AdviceType.BEFORE) {
                        generateInvokeSpecial();
                    }
                    break;

                case INVOKESTATIC$void:
                case INVOKESTATIC$float:
                case INVOKESTATIC$long:
                case INVOKESTATIC$double:
                case INVOKESTATIC$word:
                case INVOKESTATIC$reference:
                case INVOKESTATIC$void$init:
                case INVOKESTATIC$float$init:
                case INVOKESTATIC$long$init:
                case INVOKESTATIC$double$init:
                case INVOKESTATIC$word$init:
                case INVOKESTATIC$reference$init:
                    if (adviceType == AdviceType.BEFORE) {
                        generateInvokeStatic();
                    }
                    break;

                case INVOKEINTERFACE$void:
                case INVOKEINTERFACE$float:
                case INVOKEINTERFACE$long:
                case INVOKEINTERFACE$double:
                case INVOKEINTERFACE$word:
                case INVOKEINTERFACE$reference:
                case INVOKEINTERFACE$void$resolved:
                case INVOKEINTERFACE$float$resolved:
                case INVOKEINTERFACE$long$resolved:
                case INVOKEINTERFACE$double$resolved:
                case INVOKEINTERFACE$word$resolved:
                case INVOKEINTERFACE$reference$resolved:
                case INVOKEINTERFACE$void$instrumented:
                case INVOKEINTERFACE$float$instrumented:
                case INVOKEINTERFACE$long$instrumented:
                case INVOKEINTERFACE$double$instrumented:
                case INVOKEINTERFACE$word$instrumented:
                case INVOKEINTERFACE$reference$instrumented:
                    if (adviceType == AdviceType.BEFORE) {
                        generateInvokeInterface();
                    }
                    break;

                case ATHROW:
                    generateThrow();
                    break;

                case ARRAYLENGTH:
                    generateArrayLength();
                    break;

                case MONITORENTER:
                case MONITOREXIT:
                    generateMonitor(tag);
                    break;

                case POP:
                case POP2:
                case DUP:
                case DUP_X1:
                case DUP_X2:
                case DUP2:
                case DUP2_X1:
                case DUP2_X2:
                case SWAP:
                    generateStackAdjust(tag);
                    break;

                case LDC$int:
                case LDC$long:
                case LDC$float:
                case LDC$double:
                case LDC$reference:
                case LDC$reference$resolved:
                    generateLDC(tag, k);
                    break;

                case IRETURN:
                case LRETURN:
                case FRETURN:
                case DRETURN:
                case ARETURN:
                case WRETURN:
                case RETURN:

                case IRETURN$unlock:
                case LRETURN$unlock:
                case FRETURN$unlock:
                case DRETURN$unlock:
                case ARETURN$unlock:
                case WRETURN$unlock:
                case RETURN$unlock:
                    generateReturn(tag, k);
                    break;

                case LOCK:
                    methodName = "MonitorEnter";
                    generateMonitor(tag);
                    break;

                case UNLOCK:
                    methodName = "MonitorExit";
                    generateMonitor(tag);
                    break;

                // No special treatment for the following codes
                case RETURN$registerFinalizer:
                    break;

                case PREAD_BYTE:
                case PREAD_CHAR:
                case PREAD_SHORT:
                case PREAD_INT:
                case PREAD_FLOAT:
                case PREAD_LONG:
                case PREAD_DOUBLE:
                case PREAD_WORD:
                case PREAD_REFERENCE:

                case PREAD_BYTE_I:
                case PREAD_CHAR_I:
                case PREAD_SHORT_I:
                case PREAD_INT_I:
                case PREAD_FLOAT_I:
                case PREAD_LONG_I:
                case PREAD_DOUBLE_I:
                case PREAD_WORD_I:
                case PREAD_REFERENCE_I:

                case PWRITE_BYTE:
                case PWRITE_SHORT:
                case PWRITE_INT:
                case PWRITE_FLOAT:
                case PWRITE_LONG:
                case PWRITE_DOUBLE:
                case PWRITE_WORD:
                case PWRITE_REFERENCE:

                case PWRITE_BYTE_I:
                case PWRITE_SHORT_I:
                case PWRITE_INT_I:
                case PWRITE_FLOAT_I:
                case PWRITE_LONG_I:
                case PWRITE_DOUBLE_I:
                case PWRITE_WORD_I:
                case PWRITE_REFERENCE_I:

                case PGET_BYTE:
                case PGET_CHAR:
                case PGET_SHORT:
                case PGET_INT:
                case PGET_FLOAT:
                case PGET_LONG:
                case PGET_DOUBLE:
                case PGET_WORD:
                case PGET_REFERENCE:

                case PSET_BYTE:
                case PSET_SHORT:
                case PSET_INT:
                case PSET_FLOAT:
                case PSET_LONG:
                case PSET_DOUBLE:
                case PSET_WORD:
                case PSET_REFERENCE:

                case PCMPSWP_INT:
                case PCMPSWP_WORD:
                case PCMPSWP_REFERENCE:

                case PCMPSWP_INT_I:
                case PCMPSWP_WORD_I:
                case PCMPSWP_REFERENCE_I:
                    generateDefault(tag);
                    break;

                case TRACE_METHOD_ENTRY:
                    generateTraceMethodEntry();
                    break;

                default:
                    ProgramError.unexpected("tag " + tag + " not implemented");
            }
        }
    }

    private void startGuardAdvice() {
        out.printf("        if (%s) {%n", bitGuard ? ALT_ISADVISING : INLINE_ISADVISING);
    }

    private void endGuardAdvice() {
        out.printf("        }%n");
    }

    private void generatePutField(String k, boolean resolved) {
        String offset = resolved ? "offset" : "f.offset()";
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(object, %s, %s);%n", adviceType.methodNameComponent, methodName, offset, putValue(k, ""));
        endGuardAdvice();
    }

    private void generateGetField(String k, boolean resolved) {
        String offset = resolved ? "offset" : "f.offset()";
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(object, %s);%n", adviceType.methodNameComponent, methodName, offset);
        endGuardAdvice();
    }

    private void generatePutStatic(String k, boolean init) {
        String args = init ? "staticTuple, offset" : "f.holder().staticTuple(), f.offset()";
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(%s, %s);%n", adviceType.methodNameComponent, methodName, args, putValue(k, ""));
        endGuardAdvice();
    }

    private void generateGetStatic(String k, boolean init) {
        String args = init ? "staticTuple, offset" : "f.holder().staticTuple(), f.offset()";
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(%s);%n", adviceType.methodNameComponent, methodName, args);
        endGuardAdvice();
    }

    private void generateArrayLoad(String k) {
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(array, index);%n", adviceType.methodNameComponent, methodName);
        endGuardAdvice();
    }

    private void generateArrayStore(String k) {
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(array, index, %s);%n", adviceType.methodNameComponent, methodName, putValue(k, ""));
        endGuardAdvice();
    }

    private void generateLoadConst(T1XTemplateTag tag, String k, String v) {
        String value = k.equals("Reference") ? v : (k.equals("Word") ? "0" : "constant");
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(%s);%n", adviceType.methodNameComponent, methodName, value);
        endGuardAdvice();
    }

    private void generateLDC(T1XTemplateTag tag, String k) {
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(constant);%n", adviceType.methodNameComponent, methodName);
        endGuardAdvice();
    }

    private void generateLoad(String k) {
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(index);%n", adviceType.methodNameComponent, methodName);
        endGuardAdvice();
    }

    private void generateStore(String k) {
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(index, %s);%n", adviceType.methodNameComponent, methodName, putValue(k));
        endGuardAdvice();
    }

    private void generateIPush(String k) {
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(value);%n", adviceType.methodNameComponent, methodName);
        endGuardAdvice();
    }

    private void generateOperation(T1XTemplateTag tag, String k) {
        String value1 = isNeg(tag) ? "value" : "value1";
        String value2 = isNeg(tag) ? "zero" : "value2";
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(%d, %s, %s);%n", adviceType.methodNameComponent, methodName, tag.opcode, value1, value2);
        endGuardAdvice();
    }

    private boolean isNeg(T1XTemplateTag tag) {
        return tag == INEG || tag == FNEG || tag == LNEG || tag == DNEG;
    }

    private void generateWOperation(T1XTemplateTag tag) {
        String value1 = "value1.toLong()";
        String value2 = tag == T1XTemplateTag.WDIVI || tag == T1XTemplateTag.WREMI ? "value2" : "value2.toLong()";
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(%d, %s, %s);%n", adviceType.methodNameComponent, methodName, tag.opcode, value1, value2);
        endGuardAdvice();
    }

    private void generateConversion(T1XTemplateTag tag, String k) {
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(%d, value);%n", adviceType.methodNameComponent, methodName, tag.opcode);
        endGuardAdvice();
    }

    private void generateNew() {
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(object);%n", adviceType.methodNameComponent, methodName);
        endGuardAdvice();
    }

    private void generateNewUniArray() {
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(array, length);%n", adviceType.methodNameComponent, methodName);
        endGuardAdvice();
    }

    private void generateNewMultiArray() {
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(array, lengths);%n", adviceType.methodNameComponent, methodName);
        endGuardAdvice();
    }

    private void generateInvokeVirtual() {
        startGuardAdvice();
        out.printf(INDENT12_PREFIX + "VMAJavaRunScheme.saveMethodActorAndReceiver(receiver, methodActor);%n");
        out.printf(INDENT12_ADVISE_PREFIX + "(receiver, methodActor);%n", adviceType.methodNameComponent, methodName);
        endGuardAdvice();
    }

    private void generateInvokeInterface() {
        startGuardAdvice();
        out.printf(INDENT12_PREFIX + "VMAJavaRunScheme.saveMethodActorAndReceiver(receiver, methodActor);%n");
        out.printf(INDENT12_ADVISE_PREFIX + "(receiver, methodActor);%n", adviceType.methodNameComponent, methodName);
        endGuardAdvice();
    }

    private void generateInvokeSpecial() {
        startGuardAdvice();
        out.printf(INDENT12_PREFIX + "VMAJavaRunScheme.saveMethodActorAndReceiver(receiver, methodActor);%n");
        out.printf(INDENT12_ADVISE_PREFIX + "(receiver, methodActor);%n", adviceType.methodNameComponent, methodName);
        endGuardAdvice();
    }

    private void generateInvokeStatic() {
        startGuardAdvice();
        out.printf(INDENT12_PREFIX + "VMAJavaRunScheme.saveMethodActor(methodActor);%n");
        out.printf(INDENT12_ADVISE_PREFIX + "(null, methodActor);%n", adviceType.methodNameComponent, methodName);
        endGuardAdvice();
    }

    private void generateTraceMethodEntry() {
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(receiver, methodActor);%n", adviceType.methodNameComponent, "MethodEntry");
        endGuardAdvice();
    }

    private void generateIf(T1XTemplateTag tag) {
        boolean isNull = tag == T1XTemplateTag.IFNULL || tag == T1XTemplateTag.IFNONNULL;
        String value2 = isNull ? "null" : (IF_TEMPLATE_TAGS.contains(tag) ? "0" : "value2");
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(%d, value1, %s);%n", adviceType.methodNameComponent, methodName, tag.opcode, value2);
        endGuardAdvice();
    }

    private void generateTypeCheck(T1XTemplateTag tag) {
        String name = CHECKCAST_TEMPLATE_TAGS.contains(tag) ? "CheckCast" : "InstanceOf";
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(object, classActor);%n", adviceType.methodNameComponent, methodName, name);
        endGuardAdvice();
    }

    private void generateThrow() {
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(object);%n", adviceType.methodNameComponent, methodName);
        endGuardAdvice();
    }

    private void generateArrayLength() {
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(array, length);%n", adviceType.methodNameComponent, methodName);
        endGuardAdvice();
    }

    private void generateMonitor(T1XTemplateTag tag) {
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(object);%n", adviceType.methodNameComponent, methodName);
        endGuardAdvice();
    }

    private void generateReturn(T1XTemplateTag tag, String k) {
        String arg = AdviceGeneratorHelper.codeMap.get(tag.opcode) == VMABytecodes.RETURN ? "" : putArg("value", k, "");
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(%s);%n", adviceType.methodNameComponent, methodName, arg);
        endGuardAdvice();
    }

    private void generateIInc() {
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(%d, index, increment);%n", adviceType.methodNameComponent, methodName, IINC.opcode);
        endGuardAdvice();
    }

    private void generateStackAdjust(T1XTemplateTag tag) {
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(%d);%n", adviceType.methodNameComponent, methodName, tag.opcode);
        endGuardAdvice();
    }

    private void generateDefault(T1XTemplateTag tag) {
        startGuardAdvice();
        out.printf(INDENT12_ADVISE_PREFIX + "(%d);%n", adviceType.methodNameComponent, methodName, tag.opcode);
        endGuardAdvice();
    }


    private String putValue(String k) {
        return putValue(k, "");
    }

    private static String putValue(String k, String vSuffix) {
        return putArg("value", k, vSuffix);
    }

    private static String putArg(String name, String k, String vSuffix) {
        String value = name + vSuffix;
        if (k.equals("Word")) {
            return value + ".asAddress().toLong()";
        } else {
            return value;
        }
    }

    private static String getDefault(String k) {
        if (k.equals("float")) {
            return "0.0f";
        } else if (k.equals("double")) {
            return "0.0";
        } else if (k.equals("Object")) {
            return "null";
        } else {
            return "0";
        }
    }

    public VMAdviceTemplateGenerator(PrintStream ps) {
        super(ps);
    }

    @Override
    public void generateUnresolvedInvokeVITemplate(CiKind k, String variant) {
        generateInvokeVITemplate(k, variant, "");
    }

    @Override
    public void generateInvokeVITemplate(CiKind k, String variant, boolean instrumented) {
        generateInvokeVITemplate(k, variant, instrumented ? "instrumented" : "resolved");
    }

    /**
     * Generate a specific {@code INVOKE} template.
     * @param k type
     * @param variant one of "virtual" or "interface"
     * @param tag one of "", "resolved" or "instrumented"
     */
    private void generateInvokeVITemplate(CiKind k, String variant, String tag) {
        String params = tag.equals("") ? "ResolutionGuard.InPool guard" :
            (variant.equals("interface") ? "InterfaceMethodActor methodActor" : "VirtualMethodActor methodActor");
        if (tag.equals("instrumented")) {
            params += ", MethodProfile mpo, int mpoIndex";
        }
        params += ", Reference receiver";
        startMethodGeneration();
        generateTemplateTag("INVOKE%s$%s%s", variant.toUpperCase(), lr(k), prefixDollar(tag));
        out.printf("    @Slot(-1)%n");
        out.printf("    public static Address invoke%s%s(%s) {%n", variant, u(k), params);
        if (tag.equals("")) {
            if (variant.equals("interface")) {
                out.printf("        final InterfaceMethodActor methodActor = Snippets.resolveInterfaceMethod(guard);%n");
            } else {
                out.printf("        VirtualMethodActor methodActor = Snippets.resolveVirtualMethod(guard);%n");
            }
        }
        generateBeforeAdvice(k, variant, tag);
        if (variant.equals("interface")) {
            if (tag.equals("")) {
                out.printf("        return VMAT1XRuntime.selectInterfaceMethod(receiver, methodActor).%n");
            } else if (tag.equals("resolved")) {
                out.printf("        return VMAT1XRuntime.selectInterfaceMethod(receiver, methodActor).%n");
            } else if (tag.equals("instrumented")) {
                out.printf("        return Snippets.selectInterfaceMethod(receiver, methodActor, mpo, mpoIndex).%n");
            }
        } else {
            // virtual
            if (tag.equals("")) {
                out.printf("        return VMAT1XRuntime.selectNonPrivateVirtualMethod(receiver, methodActor).%n");
            } else if (tag.equals("resolved")) {
                out.printf("        return ObjectAccess.readHub(receiver).getWord(methodActor.vTableIndex()).asAddress().%n");
            } else if (tag.equals("instrumented")) {
                out.printf("        return selectVirtualMethod(receiver, methodActor.vTableIndex(), mpo, mpoIndex).%n");
            }
        }
        out.printf("            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());%n");
        out.printf("    }%n");
        newLine();
        // for record keeping only, no output
        generateAfterAdvice(k, variant, tag);
    }


    @Override
    public void generateInvokeSSTemplate(CiKind k, String variant) {
        generateInvokeSSTemplate(k, variant, "");
        generateInvokeSSTemplate(k, variant, "resolved");
    }

    /**
     * Generate a specific {@code INVOKE} template.
     * @param k type
     * @param variant one of "special" or "static"
     * @param xtag one of "" or "resolved"
     */
    public void generateInvokeSSTemplate(CiKind k, String variant, String xtag) {
        boolean resolved = xtag.equals("resolved");
        boolean isStatic = variant.equals("static");
        String tag = isStatic && resolved ? "init" : xtag;
        String params = xtag.equals("") ? "ResolutionGuard.InPool guard" :
            (isStatic ? "StaticMethodActor methodActor" : "VirtualMethodActor methodActor");
        if (variant.equals("special")) {
            params += ", Reference receiver";
        }
        startMethodGeneration();
        generateTemplateTag("INVOKE%s$%s%s", variant.toUpperCase(), lr(k), prefixDollar(tag));
        if (!resolved) {
            out.printf("    @Slot(-1)%n");
        }
        out.printf("    public static %s invoke%s%s(%s) {%n", resolved ? "void" : "Address", variant, u(k), params);
        if (!isStatic) {
            out.printf("        nullCheck(receiver.toOrigin());%n");
        }
        if (!resolved) {
            out.printf("        %sMethodActor methodActor = VMAT1XRuntime.resolve%sMethod(guard);%n", isStatic ? "Static" : "Virtual", toFirstUpper(variant));
        }
        generateBeforeAdvice(k, variant, tag);
        if (!resolved) {
            out.printf("        return VMAT1XRuntime.initialize%sMethod(methodActor);%n", toFirstUpper(variant));

        }
        out.printf("    }%n");
        newLine();
        // for record keeping only, no output
        generateAfterAdvice(k, variant, tag);
    }


    @Override
    public void generateTraceMethodEntryTemplate() {
        startMethodGeneration();
        generateTemplateTag("%s", TRACE_METHOD_ENTRY);
        out.printf("    public static void traceMethodEntry(MethodActor methodActor, Object receiver) {%n");
        generateAfterAdvice();
        out.printf("    }%n");
        newLine();

    }

    private static final EnumSet<T1XTemplateTag>  INVOKE_AFTER_TEMPLATES = EnumSet.of(
                    INVOKEVIRTUAL$adviseafter,
                    INVOKEINTERFACE$adviseafter,
                    INVOKESPECIAL$adviseafter,
                    INVOKESTATIC$adviseafter
    );

    private void generateInvokeAfterTemplates() {
        if (generating[AdviceType.AFTER.ordinal()]) {
            for (T1XTemplateTag tag : INVOKE_AFTER_TEMPLATES) {
                generateInvokeAfterTemplate(tag);
            }
        }
    }

    private void generateInvokeAfterTemplate(T1XTemplateTag tag) {
        String methodName = null;
        // Checkstyle: stop
        switch (tag) {
            case INVOKEVIRTUAL$adviseafter: methodName = "InvokeVirtual"; break;
            case INVOKEINTERFACE$adviseafter: methodName = "InvokeInterface"; break;
            case INVOKESPECIAL$adviseafter: methodName = "InvokeSpecial"; break;
            case INVOKESTATIC$adviseafter: methodName = "InvokeStatic"; break;
        }
        // Checkstyle: resume
        startMethodGeneration();
        generateTemplateTag("%s", tag);
        out.printf("    public static void adviseAfter%s() {%n", methodName);
        out.printf(INDENT8_PREFIX + ADVISE_PREFIX +
                        "(VMAJavaRunScheme.loadReceiver(), VMAJavaRunScheme.loadMethodActor());%n",
                        AdviceType.AFTER.methodNameComponent, methodName);
        out.printf("    }%n");
        newLine();
    }


    private static final EnumSet<T1XTemplateTag> STACK_ADJUST_TEMPLATES = EnumSet.of(POP, POP2, DUP, DUP_X1, DUP_X2, DUP2, DUP2_X1, DUP2_X2, SWAP);

    private void generateStackAdjustTemplates() {
        for (T1XTemplateTag tag : STACK_ADJUST_TEMPLATES) {
            tagAdviceCapabilities[tag.ordinal()][AdviceType.BEFORE.ordinal()] = true;
            generateStackAdjustTemplate(tag);
        }
    }

    private void generateStackAdjustTemplate(T1XTemplateTag tag) {
        startMethodGeneration();
        generateTemplateTag("%s", tag);
        out.printf("    public static void %s() {%n", tag.name().toLowerCase());
        generateBeforeAdvice();
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> SHORT_CONST_TEMPLATES = EnumSet.of(WCONST_0, ACONST_NULL);

    /**
     * Generate all the {@link SHORT_CONST_TEMPLATES}.
     */
    public void generateShortConstTemplates() {
        for (T1XTemplateTag tag : SHORT_CONST_TEMPLATES) {
            tagAdviceCapabilities[tag.ordinal()][AdviceType.BEFORE.ordinal()] = true;
            generateShortConstTemplate(tag);
        }
    }

    /**
     * Generates one of the {@link #SHORT_CONST_TEMPLATES}.
     */
    public void generateShortConstTemplate(T1XTemplateTag tag) {
        String k = tag == WCONST_0 ? "Word" : "Reference";
        String v = tag == WCONST_0 ? "0" : "null";
        startMethodGeneration();
        generateTemplateTag("%s", tag);
        out.printf("    public static void %s() {%n", tag.name().toLowerCase());
        generateBeforeAdvice(k,  v);
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> CONST_TEMPLATES = EnumSet.of(ICONST, LCONST, FCONST, DCONST);

    /**
     * Generate all the {@link #CONST_TEMPLATES}.
     */
    public void generateConstTemplates() {
        for (T1XTemplateTag tag : CONST_TEMPLATES) {
            tagAdviceCapabilities[tag.ordinal()][AdviceType.BEFORE.ordinal()] = true;
            generateConstTemplate(tag);
        }
    }

    /**
     * Generates one of the {@link #CONST_TEMPLATES}.
     * @param k type, one of {@code float}, {@code double} or {@code long}
     */
    public void generateConstTemplate(T1XTemplateTag tag) {
        String k = "???";
        // Checkstyle: stop
        switch (tag) {
            case ICONST: k = "int"; break;
            case FCONST: k = "float"; break;
            case DCONST: k = "double"; break;
            case LCONST: k = "long"; break;
        }
        // Checkstyle: resume
        startMethodGeneration();
        generateTemplateTag("%s", tag);
        out.printf("    public static void %s(%s constant) {%n", tag.name().toLowerCase(), k);
        generateBeforeAdvice(k);
        out.printf("    }%n");
        newLine();
    }

    private static boolean hasLDCTemplates(CiKind k) {
        return k == CiKind.Int || k == CiKind.Long || k == CiKind.Float || k == CiKind.Double || k == CiKind.Object;
    }

    private static final EnumSet<T1XTemplateTag> LDC_TEMPLATE_TAGS = EnumSet.of(LDC$int, LDC$long, LDC$float, LDC$double, LDC$reference, LDC$reference$resolved);

    /**
     * Generate all the {@link #LDC_TEMPLATE_TAGS}.
     */
    private void generateLDCTemplates() {
        for (T1XTemplateTag tag : LDC_TEMPLATE_TAGS) {
            generateLDCTemplate(tag);
        }
    }

    /**
     * Generate the {@code LDC} template(s) for given type.
     * @param k type
     */
    private void generateLDCTemplate(T1XTemplateTag tag) {
        int ix = tag.name().indexOf('$');
        String k = tag == LDC$reference || tag == LDC$reference$resolved ? "Object" : tag.name().substring(ix + 1);
        startMethodGeneration();
        generateTemplateTag("%s", tag);
        if (tag == LDC$reference) {
            out.printf("    public static void uoldc(ResolutionGuard guard) {%n");
            out.printf("        ClassActor classActor = Snippets.resolveClass(guard);%n");
            out.printf("        Object constant = T1XRuntime.getClassMirror(classActor);%n");
            generateBeforeAdvice(k);
        } else {
            String ks = k.substring(0, 1).toLowerCase();
            out.printf("    public static void %sldc(%s constant) {%n", ks, k);
            generateBeforeAdvice(k);
        }
        out.printf("    }%n");
        newLine();

    }

    private static final EnumSet<T1XTemplateTag> LOAD_TEMPLATE_TAGS = EnumSet.of(ILOAD, LLOAD, FLOAD, DLOAD, ALOAD, WLOAD);

    private static String lsType(T1XTemplateTag tag) {
        // Checkstyle: stop
        switch (tag) {
            case ILOAD: case ISTORE: return "int";
            case LLOAD: case LSTORE: return "long";
            case FLOAD: case FSTORE: return "float";
            case DLOAD: case DSTORE: return "double";
            case ALOAD: case ASTORE: return "Object";
            case WLOAD: case WSTORE: return "Word";
        }
        // Checkstype: resume
        return "???";
    }
    /**
     * Generate all the {@link #LOAD_TEMPLATE_TAGS}.
     */
    private void generateLoadTemplates() {
        for (T1XTemplateTag tag : LOAD_TEMPLATE_TAGS) {
            generateLoadTemplate(tag);
        }
    }

    /**
     * Generate the {@code LOAD} template for given type.
     */
    private void generateLoadTemplate(T1XTemplateTag tag) {
        String k = lsType(tag);
        startMethodGeneration();
        generateTemplateTag("%s", tag);
        out.printf("    public static void %sload(int index) {%n", k.substring(0, 1).toLowerCase());
        generateBeforeAdvice(k);
        out.printf("    }%n");
        newLine();
    }


    private static final EnumSet<T1XTemplateTag> STORE_TEMPLATE_TAGS = EnumSet.of(ISTORE, LSTORE, FSTORE, DSTORE, ASTORE, WSTORE);

    /**
     * Generate all the {@link #STORE_TEMPLATE_TAGS}.
     */
    private void generateStoreTemplates() {
        for (T1XTemplateTag tag : STORE_TEMPLATE_TAGS) {
            generateStoreTemplate(tag);
        }
    }

    /**
     * Generate the {@code STORE} template for given type.
     */
    private void generateStoreTemplate(T1XTemplateTag tag) {
        String k = lsType(tag);
        startMethodGeneration();
        generateTemplateTag("%s", tag);
        out.printf("    public static void %sstore(int index, %s value) {%n", k.substring(0, 1).toLowerCase(), k);
        generateBeforeAdvice(k);
        out.printf("    }%n");
        newLine();
    }

    private void generateIINC() {
        startMethodGeneration();
        generateTemplateTag("%s", IINC);
        out.printf("    public static void iinc(int index, int increment) {%n");
        generateBeforeAdvice();
        out.printf("    }%n");
        newLine();

    }

    private static final EnumSet<T1XTemplateTag> IFCMP_TEMPLATE_TAGS = EnumSet.of(IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE);

    /**
     * Generate all the {@link #IFCMP_TEMPLATE_TAGS}.
     */
    private void generateIfCmpTemplates() {
        for (CiKind k : new CiKind[] { CiKind.Int, CiKind.Object}) {
            for (String s : T1XTemplateGenerator.conditions) {
                if (k == CiKind.Object && !(s.equals("eq") || s.equals("ne"))) {
                    continue;
                }
                generateIfCmpTemplate(k, s);
            }
        }
    }

    /**
     * Generate a specific {@link #IFCMP_TEMPLATE_TAGS} template.
     * @param k type
     * @param op one of "eq", "ne", "lt", "ge", "gt", "le"
     */
    private void generateIfCmpTemplate(CiKind ciKind, String op) {
        startMethodGeneration();
        generateTemplateTag("IF_%sCMP%s", tagPrefix(ciKind), op.toUpperCase());
        out.printf("    public static void if_%scmp%s(@Slot(1) %s value1, @Slot(0) %s value2) {%n", opPrefix(ciKind), op, ciKind.javaName, ciKind.javaName);
        generateBeforeAdvice();
        out.printf("    }%n");
        newLine();
    }

    private static final EnumSet<T1XTemplateTag> IF_TEMPLATE_TAGS = EnumSet.of(IFEQ, IFNE, IFLT, IFLE, IFGE, IFGT, IFNULL, IFNONNULL);

    /**
     * Generate all the {@link #IF_TEMPLATE_TAGS}.
     *
     */
    private void generateIfTemplates() {
        for (String s : T1XTemplateGenerator.conditions) {
            generateIfTemplate(CiKind.Int, s);
        }
        for (String s : new String[] {"null", "nonnull"}) {
            generateIfTemplate(CiKind.Object, s);
        }
    }

    /**
     * Generate a specific {@link #IF_TEMPLATE_TAGS} template.
     * @param k type one of "int" or "Reference"
     * @param op one of "eq", "ne", "lt", "ge", "gt", "le" ("null" or "nonnull" for k == "Reference")
     */
    private void generateIfTemplate(CiKind ciKind, String op) {
        startMethodGeneration();
        generateTemplateTag("IF%s", op.toUpperCase());
        out.printf("    public static void if%s(@Slot(0) %s value1) {%n", op, ciKind.javaName);
        generateBeforeAdvice();
        out.printf("    }%n");
        newLine();
    }

    private static VMAdviceTemplateGenerator vmaT1XTemplateGen;

    @Override
    public void generateAll(AdviceHook hook) {
        // do the standard template generator first
        super.generateAll(hook);
        // now the VMA advice templates
        generateStackAdjustTemplates();
        generateShortConstTemplates();
        generateConstTemplates();
        generateLDCTemplates();
        generateLoadTemplates();
        generateStoreTemplates();
        generateIINC();
        generateIfTemplates();
        generateIfCmpTemplates();
        generateInvokeAfterTemplates();
    }

    /**
     * Generate the given advice source class.
     * @param target
     */
    private static int generateAdviceSource(Class<?> target, boolean checkOnly) throws IOException {
        byteArrayOut.reset();
        VMAT1XAdvice adviceHook = vmaT1XTemplateGen.new VMAT1XAdvice();
        vmaT1XTemplateGen.generateAll(adviceHook);
        if (checkOnly) {
            byteArrayOut.writeOut();
        }
        return AdviceGeneratorHelper.updateSource(target, byteArrayOut.toString(), checkOnly);

    }

    public static void main(String[] args) throws IOException {
        boolean checkOnly = false;
        for (String arg : args) {
            if (arg.equals("javaguard")) {
                bitGuard = false;
            } else if (arg.equals("checkonly")) {
                checkOnly = true;
            }
        }
        byteArrayOut = new ThisByteArrayOutputStream();
        vmaT1XTemplateGen = new VMAdviceTemplateGenerator(new PrintStream(byteArrayOut));
        // discover what the advice capabilities are for each tag
        vmaT1XTemplateGen.generateAll(new DiscoverCapabilitiesHook());

        generating[AdviceType.BEFORE.ordinal()] = true;
        generating[AdviceType.AFTER.ordinal()] = false;
        generateAdviceSource(VMAdviceBeforeTemplateSource.class, checkOnly);

        generating[AdviceType.BEFORE.ordinal()] = false;
        generating[AdviceType.AFTER.ordinal()] = true;
        generateAdviceSource(VMAdviceAfterTemplateSource.class, checkOnly);

        generating[AdviceType.BEFORE.ordinal()] = true;
        generateAdviceSource(VMAdviceBeforeAfterTemplateSource.class, checkOnly);

    }

}
