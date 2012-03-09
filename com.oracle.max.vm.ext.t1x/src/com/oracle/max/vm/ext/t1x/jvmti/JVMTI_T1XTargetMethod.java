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
package com.oracle.max.vm.ext.t1x.jvmti;

import static com.oracle.max.vm.ext.t1x.T1X.*;
import static com.sun.max.vm.compiler.target.Safepoints.*;

import com.oracle.max.vm.ext.t1x.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ri.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.deopt.Deoptimization.Info;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jvmti.*;
import com.sun.max.vm.runtime.*;

/**
 * Carries additional information to handle "deopt" of baseline methods that contains or will contain
 * JVMTI support code.
 *
 * There are several possible scenarios:
 *
 * <ol>
 * <li>Transforming from optimized code to non-instrumented baseline code.
 *     This should never get here as non-instrumented baseline code
 *     is compiled by the default T1X and so creates a vanilla {@link T1XTargetMethod}.</li>
 *
 * <li>Transforming from any code to baseline code containing instrumentation.
 *     In this case the deoptimization is being invoked by JVMTI in response to some new requirement,
 *     e.g., breakpoint, single step.
 *     In consequence the new code will contain embedded DIRECT_CALLs to JVMTI events, e.g. {@link JVMTIBreakpoints#event}.
 *     In which case the normal logic in {@link T1XTargetMethod#findTemplatCallReturnAddress}}
 *     will unroll to the beginning of the bytecode containing the inserted event call, causing the event
 *     to be taken immediately, or recursion if the code was already instrumented..
 *     So we have to detect that case and unroll to the "logcial" start of the bytecode, after (all) the event call(s).</li>
 * <li>
 *
 *     The "this" reference refers to the new method containing the (possibly modified) instrumentation.
 *     The {@link Deoptimization.Info#tm} field denotes the old method, allowing determination of the above states.
 * </ol>
 */
public class JVMTI_T1XTargetMethod extends T1XTargetMethod {

    private final boolean[] eventBci;
    private final long eventSettings;
    private final long[] breakpoints;

    public JVMTI_T1XTargetMethod(T1XCompilation comp, boolean install, boolean[] eventBci, long eventSettings, long[] breakpoints) {
        super(comp, install);
        this.eventBci = eventBci;
        this.eventSettings = eventSettings;
        this.breakpoints = breakpoints;
    }

    @Override
    /**
     * Checks if this method supports the event settings and breakpoints passed as arguments.
     * This does not need to be an exact match since events are not delivered if they are not enabled.
     * However, this method must support at least the requested settings.
     * @param eventSettings
     * @param breakpoints
     * @return
     */
    public boolean jvmtiCheck(long eventSettings, long[] breakpoints) {
        if ((eventSettings & this.eventSettings) == eventSettings) {
            if (breakpoints == null) {
                return true;
            }
            if (this.breakpoints == null) {
                return false;
            }
            for (long rb : breakpoints) {
                boolean match = false;
                for (long thisRb : this.breakpoints) {
                    if (rb == thisRb) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected CodePointer findTemplateCallReturnAddress(Info info, int bci, RiMethod callee) throws FatalError {
        if (!eventBci[bci]) {
            // no instrumentation code was generated for this bytecode so no special treatment.
            return super.findTemplateCallReturnAddress(info, bci, callee);
        }
        // find the event call in the old code
        ClassMethodActor eventCallee = callSiteToEventCallee(info.tm, info.ip.pos());
        assert eventCallee != null;

        int curPos = bciToPos[bci];
        byte[] bytecode = classMethodActor.code();
        int opcode = bytecode[bci] & 0xFF;
        final int invokeSize = Bytecodes.lengthOf(opcode);
        int succBCI = bci + invokeSize;
        int succPos = bci < bciToPos.length - 2 ? bciToPos[succBCI] : Integer.MAX_VALUE;
        assert succPos > curPos;

        // look for the corresponding call in the new code
        int dcIndex = 0;
        for (int i = 0; i < safepoints.size(); i++) {
            if (safepoints.isSetAt(DIRECT_CALL, i)) {
                // have to check this first to step through the directCallees array correctly
                if (directCallees[dcIndex] == eventCallee) {
                    int safepointPos = safepoints.posAt(i);
                    if (curPos <= safepointPos && safepointPos < succPos) {
                        if (isAMD64()) {
                            // On x86 the safepoint position of a call *is* the return position
                            return codeAt(safepointPos);
                        } else {
                            throw unimplISA();
                        }
                    }
                }
                dcIndex++;
            }
        }

        // if we get here, then we didn't find the old event call in the new code
        // i.e., it has been removed, in which case we just do the default thing.
        return super.findTemplateCallReturnAddress(info, bci, eventCallee);
    }

    /**
     * Similar to  {@link TargetMethod#callSiteToCallee(CodePointer), but works with an offset.
     * @param tm the {@link TargetMethod} to search in.
     * @param pos the code offset used to locate the call.
     * @return the {@link ClassMethodActor} at the given code offset in {@code tm} or null if not found.
     */
    private ClassMethodActor callSiteToEventCallee(TargetMethod tm, int pos) {
        int dcIndex = 0;
        Safepoints oldSafepoints = tm.safepoints();
        Object[] oldDirectCallees = tm.directCallees();
        for (int i = 0; i < oldSafepoints.size(); i++) {
            if (oldSafepoints.isSetAt(DIRECT_CALL, i)) {
                if (oldSafepoints.posAt(i) == pos && oldDirectCallees[dcIndex] instanceof ClassMethodActor) {
                    return (ClassMethodActor) oldDirectCallees[dcIndex];
                }
                dcIndex++;
            }
        }
        return null;
    }

}
