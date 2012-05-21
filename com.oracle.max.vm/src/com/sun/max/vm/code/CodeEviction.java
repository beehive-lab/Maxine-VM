/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.code;

import static com.sun.max.vm.compiler.CallEntryPoint.*;
import static com.sun.max.vm.compiler.target.Safepoints.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.amd64.*;
import com.sun.max.vm.profile.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * CodeEviction manages baseline code eviction and code cache compaction.
 * It is implemented as a {@link VmOperation} as it needs to run in stop-the-world fashion.
 *
 * All methods are considered live that are present on any of the executing threads' call stacks
 * at the time baseline code cache contention triggers eviction. All methods not currently being
 * executed in any thread, and methods not holding a type profile, are considered stale and removed.
 * The baseline code cache is organised in a semi-space fashion; i.e., all live methods are moved
 * from from-space to to-space.
 *
 * Note that only baseline methods are subject to eviction. Optimised code as well as code in
 * the boot image code region is not affected.
 *
 * The workflow, controlled from the {@link #doit()} method, is as follows.
 * <ul>
 * <li><b>Phase 1: Patching.</b>
 * <ol>
 * <li><i>Identify live and stale methods.</i><br>
 * This is done by walking all threads' call stacks and marking all {@linkplain TargetMethod target methods} met on the
 * stacks, and all target methods that are directly called from those. Furthermore, the baseline code cache is traversed,
 * and all protected methods are marked as live as well. Protected methods are to be excluded from eviction due to some
 * reason, e.g., because they are currently being assembled but not yet installed properly, or because they have a type
 * profile and will soon be recompiled by the optimising compiler, or because their invocation counter is within a
 * certain threshold of the value indicating recompilation.</li>
 * <li><i>Invalidate direct calls.</i><br>
 * For methods in baseline, opt, and boot code region, invalidate those direct calls that go to one of the stale baseline
 * methods. Invalidation is done by routing those direct calls through trampolines again.</li>
 * <li><i>Invalidate dispatch tables and target states.</i><br>
 * Invalidation here means that the respective vtable and itable entries are made to point to the corresponding virtual
 * trampoline. This takes place for all subclasses of the class containing a stale method as well.<br>
 * This step also takes care of resetting references to native code in {@linkplain ClassMethodActor#targetState method actors}
 * to {@code null}, and of unmarking marked (live) methods. Furthermore, all stale methods are wiped, i.e., their machine
 * code and literal arrays are made to reference empty sentinel arrays.</li>
 * </ol></li>
 * <li><b>Phase 2: Compacting.</b>
 * <li><i>Copy all live methods from from-space to to-space.</i><br>
 * This affects methods that have not been wiped in the previous step. In particular, this involves the following steps for
 * each live method:<ol>
 * <li>Invalidate vtable and itable entries.</li>
 * <li>Copy the method's entire bytes (code and literals arrays) over to to-space.</li>
 * <li>Wipe the machine code and literal arrays as described above.</li>
 * <li>Memoise the old start of the method in from-space, and set new values for its start and end in to-space.</li>
 * <li>Compute and set new values for the code and literals arrays and for the {@linkplain TargetMethod#codeStart codeStart}
 * pointer.</li>
 * <li>Advance the to-space allocation mark by the method's size.</li>
 * </ol></li>
 * <li><i>Fix direct calls in and to moved code.</i><br>
 * Direct call sites are relative calls. Hence, <b>all</b> direct calls <b>in</b> moved code have to be adjusted. This is
 * achieved by iterating over all baseline methods (at this point, only methods surviving eviction are affected) and fixing all
 * direct call sites contained therein.<br>
 * Also, direct calls <b>to</b> moved code have to be adjusted. This is achieved by iterating over the optimised and boot code
 * regions and fixing all direct calls to moved code.</li>
 * <li><i>Compact the baseline code region's {@linkplain SemiSpaceCodeRegion#targetMethods target methods array}</i><br>
 * by removing entries for wiped (stale) methods.</li>
 * <li><i>Fix return addresses on call stacks, and code pointers in local variables.</i><br>
 * Walk all threads' call stacks once more and fix return addresses that point to moved code. Likewise, fix pointers to machine
 * code held in {@link CodePointer}s in the methods' frames. This logic makes use of the saved old code start of moved
 * methods.</li>
 * </ol></li>
 * <li><b>Phase 0/3: Dumping. (Optional.)</b><br>
 * In this phase, all code addresses found in dispatch tables, target states, and direct calls are dumped.
 * This takes place before and after eviction.</li>
 * </ul>
 */
public final class CodeEviction extends VmOperation {

    public static int TraceCodeEviction;

    public static final int TRACE_NONE = 0;
    public static final int TRACE_STAT = 1;
    public static final int TRACE_DETAILS = 2;
    public static final int TRACE_THREADS_CODE_MOTION = 3;
    public static final int TRACE_FULL_DUMP = 4;

    static {
        VMOptions.addFieldOption("-XX:", "TraceCodeEviction", CodeEviction.class,
            "Trace code eviction after baseline code cache contention with increasing verbosity. Values: 0 = no tracing, " +
            "1 = statistics (count evicted/surviving bytes and methods), " +
            "2 = give detailed information about what methods and dispatch entries are treated, " +
            "3 = print details about threads and code motion, " +
            "4 = give full dumps of all code addresses before and after eviction",
            MaxineVM.Phase.STARTING);
    }

    /**
     * Start logging upon the n-th eviction cycle.
     */
    public static int LogStartEviction;

    static {
        VMOptions.addFieldOption("-XX:", "LogStartEviction", CodeEviction.class,
            "Start logging upon the n-th code eviction cycle; all cycles before that are silent.",
            MaxineVM.Phase.STARTING);
    }

    protected static boolean logLevel(int level) {
        return TraceCodeEviction >= level && evictionCount >= LogStartEviction;
    }

    /**
     * Protect baseline methods until the given callee depth.
     */
    private static int CodeEvictionProtectCalleeDepth = 1;

    static {
        VMOptions.addFieldOption("-XX:", "CodeEvictionProtectCalleeDepth", CodeEviction.class,
            "During code eviction, protect callees of on-stack methods up until the given depth (default: 1).",
            MaxineVM.Phase.STARTING);
    }

    private final class DumpDispatchTables implements ClassActor.Closure {
        @Override
        public boolean doClass(ClassActor classActor) {
            final DynamicHub dhub = classActor.dynamicHub();
            if (dhub != null) {
                dumpHub(dhub, DynamicHub.vTableStartIndex(), true, "DHUB");
            }
            final StaticHub shub = classActor.staticHub();
            if (shub != null) {
                dumpHub(shub, Hub.vTableStartIndex(), !(classActor.isInterface() || classActor.isPrimitiveClassActor()), "SHUB");
            }
            return true;
        }
    }

    /**
     * Marks all target methods on the stack as live that are short-lived (baseline),
     * and all baseline methods directly invoked from those.
     */
    final class LiveMethodsMarker extends RawStackFrameVisitor {
        @Override
        public boolean visitFrame(StackFrameCursor current, StackFrameCursor callee) {
            TargetMethod tm = current.targetMethod();
            if (tm != null && CodeManager.isShortlived(tm)) {
                logMark("ON STACK", tm);
                tm.mark();
                markDirectCalleesOf(tm, CodeEvictionProtectCalleeDepth);
            }
            return true;
        }

        /**
         * Iterate over direct callees in the {@code tm} parameter and mark them.
         * Recurse (depth-first) if necessary.
         */
        private void markDirectCalleesOf(TargetMethod tm, int depthRemaining) {
            if (depthRemaining == 0) {
                return;
            }
            final Safepoints sps = tm.safepoints();
            for (int i = sps.nextDirectCall(0); i >= 0; i = sps.nextDirectCall(i + 1)) {
                TargetMethod directCallee = AMD64TargetMethodUtil.readCall32Target(tm, sps.causePosAt(i)).toTargetMethod();
                if (directCallee != null && CodeManager.isShortlived(directCallee) && !directCallee.isMarked()) {
                    logMarkLevel("DIRECT CALLEE", directCallee, depthRemaining);
                    directCallee.mark();
                    markDirectCalleesOf(directCallee, depthRemaining - 1);
                }
            }
        }
    }

    /**
     * Marks all protected methods.
     *
     * A method is protected if it must not be evicted in spite of not being present on any call stack.
     * Currently, this is true for methods ...<ul>
     * <li>having type profiles (as they will soon be recompiled by the optimising compiler),</li>
     * <li>that have just been compiled but are not yet fully installed in the system (e.g., by being referenced
     * from a stack),</li>
     * <li>whose invocation count is within the threshold denoted by {@link MethodInstrumentation#PROTECTION_THRESHOLD}.</li>
     * </ul>
     */
    final class ProtectedMethodsMarker implements TargetMethod.Closure {
        @Override
        public boolean doTargetMethod(TargetMethod targetMethod) {
            // avoid further tests if already marked
            if (!targetMethod.isMarked()) {
                if (targetMethod.isProtected()) {
                    logMark("PROTECTED (protected)", targetMethod);
                    targetMethod.mark();
                } else if (targetMethod.withinInvocationThreshold()) {
                    logMark("PROTECTED (invocation count)", targetMethod);
                    targetMethod.mark();
                } else if (targetMethod.hasTypeProfile()) {
                    logMark("PROTECTED (type profile)", targetMethod);
                    targetMethod.mark();
                }
            }
            return true;
        }
    }

    final class InvalidateDispatchTables implements TargetMethod.Closure {
        @Override
        public boolean doTargetMethod(TargetMethod targetMethod) {
            if (!targetMethod.isMarked() && !targetMethod.isWiped()) {
                ++nStale;
                nStaleBytes += targetMethod.codeLength();
                logStaleMethod(targetMethod);
                patchDispatchTables(targetMethod, true);
                assert invalidateCode(targetMethod.code());
                targetMethod.wipe();
                targetMethod.classMethodActor.compiledState = Compilations.EMPTY;
            } else {
                ++nSurvivors;
                nSurvivingBytes += targetMethod.codeLength();
                targetMethod.unmark();
            }
            return true;
        }
    }

    final class InvalidateBaselineDirectCalls implements TargetMethod.Closure {
        @Override
        public boolean doTargetMethod(TargetMethod targetMethod) {
            if (targetMethod.isMarked() && !targetMethod.isWiped()) {
                ++nBaseMeth;
                nBaseDirect += targetMethod.safepoints().numberOfDirectCalls();
                nCallBaseline += patchDirectCallsIn(targetMethod);
            }
            return true;
        }
    }

    final class InvalidateOptDirectCalls implements TargetMethod.Closure {
        @Override
        public boolean doTargetMethod(TargetMethod targetMethod) {
            ++nOptMeth;
            nOptDirect += targetMethod.safepoints().numberOfDirectCalls();
            nCallOpt += patchDirectCallsIn(targetMethod);
            return true;
        }
    }

    final class StackPatcher extends RawStackFrameVisitor {
        @Override
        public boolean visitFrame(StackFrameCursor current, StackFrameCursor callee) {
            final TargetMethod tm = current.targetMethod();
            if (tm == null) {
                return true;
            }

            // Patch locals
            // (ds) There used to exist a mechanism for querying whether a TargetMethod
            // may include CodePointer values in an activation frame. This meant that
            // scanning for CodePointers only had to be done for these methods. However,
            // the extra complexity introduced into the CRI was not worth the
            // performance gain of this optimization, especially given how rare code
            // evictions are.
            tm.prepareReferenceMap(current, callee, codePointerRelocator);

            // If the method executing in the current stack frame (tm) is a baseline method,
            // the callee's return address needs to be patched.
            final boolean patch = CodeManager.runtimeBaselineCodeRegion.contains(tm.codeStart().toAddress());

            logMethodPatch(tm, patch);

            if (!patch) {
                logCalleeReturnAddress(callee);
                return true;
            }

            final Pointer patchHere = callee.targetMethod().returnAddressPointer(callee);
            CodePointer calleeRet = CodePointer.from(patchHere.readWord(0));
            final Address offset = calleeRet.minus(tm.oldStart()).toAddress();
            CodePointer newCalleeRet = CodePointer.from(tm.start().plus(offset));

            logReturnAddressPatch(tm, calleeRet, newCalleeRet);

            patchHere.writeWord(0, newCalleeRet.toAddress());

            return true;
        }
    }

    /**
     * Most of the methods in this subclass of {@link FrameReferenceMapVisitor} are not needed.
     * Code eviction logic is piggybacking on the stack root scanning logic to avoid duplicating
     * the respective code.
     */
    private final class CodePointerRelocator extends FrameReferenceMapVisitor {

        @Override
        public void visitReferenceMapBits(StackFrameCursor cursor, Pointer slotPointer, int refMap, int numBits) {
            logVisitRefMapBits(cursor, slotPointer, refMap, numBits);

            if (refMap == 0) {
                return; // nothing to do
            }

            // Iterate over the bits in the ref map. Check references. If they are tagged, they represent
            // code pointers and need to be relocated if they point to from-space.
            for (int i = 0; i < numBits; i++) {
                if (((refMap >> i) & 1) == 1) {
                    final Address raw = slotPointer.getWord(i).asAddress();
                    if ((raw.toLong() & 1L) == 1L) { // tagged?
                        final CodePointer cp = CodePointer.fromTaggedLong(raw.toLong());
                        if (CodeManager.runtimeBaselineCodeRegion.isInFromSpace(cp.toAddress())) {
                            final TargetMethod tm = CodeManager.runtimeBaselineCodeRegion.findInFromSpace(cp.toAddress());
                            final Offset offset = tm.start().minus(tm.oldStart()).asOffset();
                            final CodePointer newCp = cp.relocate(offset);
                            assert CodeManager.runtimeBaselineCodeRegion.isInToSpace(newCp.toPointer());
                            final Address newCpAddress = Address.fromLong(newCp.toTaggedLong());
                            logRelocateCodePointer(i, cp, newCp, tm);
                            slotPointer.setWord(i, newCpAddress);
                        }
                    }
                }
            }
        }

        @Override
        public void logPrepareReferenceMap(TargetMethod targetMethod, int safepointIndex, Pointer refmapFramePointer, String label) {
            // unimplemented
        }

        @Override
        public int referenceMapBitIndex(Address slotAddress) {
            // unimplemented
            return -1;
        }

        @Override
        public void setBits(int baseSlotIndex, byte referenceMapByte) {
            // unimplemented
        }

        @Override
        public void logReferenceMapByteBefore(int byteIndex, byte referenceMapByte, String referenceMapLabel) {
            // unimplemented
        }

        @Override
        public void logReferenceMapByteAfter(Pointer framePointer, int baseSlotIndex, byte referenceMapByte) {
            // unimplemented
        }

    }

    private final class StackDumper extends RawStackFrameVisitor {
        @Override
        public boolean visitFrame(StackFrameCursor current, StackFrameCursor callee) {
            final TargetMethod tmCurrent = current.targetMethod();
            final TargetMethod tmCallee = callee.targetMethod();
            if (tmCurrent == null || tmCallee == null) {
                return true;
            }
            final Pointer rap = tmCallee.returnAddressPointer(callee);
            final CodePointer ret = CodePointer.from(rap.readWord(0));
            dump("STACK", printThreadName, tmCurrent, ret);
            return true;
        }
    }

    final class DumpDirectCalls implements TargetMethod.Closure {
        @Override
        public boolean doTargetMethod(TargetMethod targetMethod) {
            s1 = targetMethod.toString();
            final Safepoints safepoints = targetMethod.safepoints();
            for (int spi = safepoints.nextDirectCall(0); spi >= 0; spi = safepoints.nextDirectCall(spi + 1)) {
                final int callPos = safepoints.causePosAt(spi);
                final CodePointer target = AMD64TargetMethodUtil.readCall32Target(targetMethod, callPos);
                final TargetMethod callee = target.toTargetMethod();
                assert callee != null : "callee should not be null in " + targetMethod + "@" + callPos + "->" + target.to0xHexString();
                idx = callPos;
                dump("DIRECT", printTriple, callee, target);
            }
            return true;
        }
    }

    /**
     * Overwrites a machine code array with an illegal instruction pattern.
     */
    static boolean invalidateCode(byte[] code) {
        if (Platform.platform().isa == ISA.AMD64) {
            byte int3 = (byte) 0xcc;
            Arrays.fill(code, int3);
        } else {
            throw FatalError.unimplemented();
        }
        return true;
    }

    final class CopySurvivors implements TargetMethod.Closure {

        final SemiSpaceCodeRegion cr = CodeManager.runtimeBaselineCodeRegion;

        @Override
        public boolean doTargetMethod(TargetMethod targetMethod) {
            assert cr.isInFromSpace(targetMethod.start()) : "all target methods to be copied should be in from-space";
            if (!targetMethod.isWiped()) {
                // preparation
                final Pointer from = targetMethod.start().asPointer();
                final Pointer to = cr.mark();
                final Size size = targetMethod.size();
                // first, address dispatch table entries
                logCodeMotion(targetMethod, from, to, size);
                patchDispatchTables(targetMethod, false);
                // next, physically move the code
                Memory.copyBytes(from, to, size);
                assert invalidateCode(targetMethod.code()); // this invalidates the old code as targetMethod's pointers have not been changed yet!
                targetMethod.setOldStart(targetMethod.start());
                targetMethod.setStart(to);
                final byte[] code = (byte[]) relocate(from, to, targetMethod.code());
                final Pointer codeStart = to.plus(targetMethod.codeStart().toPointer().minus(from));
                final byte[] scalarLiterals = targetMethod.scalarLiterals() == null ?
                    null : (byte[]) relocate(from, to, targetMethod.scalarLiterals());
                final Object[] referenceLiterals = targetMethod.referenceLiterals() == null ?
                    null : (Object[]) relocate(from, to, targetMethod.referenceLiterals());
                targetMethod.setCodeArrays(code, codeStart, scalarLiterals, referenceLiterals);
                cr.setMark(cr.mark().plus(size));
                CodeManager.runtimeBaselineCodeRegion.add(targetMethod);
                targetMethod.survivedEviction();
            } else {
                // set the oldStart address to mark this method as "old"
                targetMethod.setOldStart(targetMethod.start());
                logNotCopying(targetMethod);
            }
            return true;
        }

        private Object relocate(Pointer fromBase, Pointer toBase, Object o) {
            if (o == null) {
                return null;
            }
            final Address offset = Reference.fromJava(o).toOrigin().minus(fromBase);
            return Reference.fromOrigin(toBase.plus(offset)).toJava();
        }
    }

    /**
     * Fixes all direct calls in the machine code of a moved (baseline) target method.
     *
     * After a method has been relocated, its direct calls all point to invalid addresses.
     *
     * The following assumes that the method has been moved by an offset called {@code delta}:
     * {@code delta = newStart - oldStart},
     * and that the call target extractable from a direct call site is named {@code target}.
     * Furthermore, the <i>intended</i> call target {@code itarget} is determined by appropiately applying {@code delta}:
     * {@code itarget = target - delta}.
     *
     * There are two cases for a direct call in moved (baseline) code:<ul>
     * <li><i>Direct call to moved code.</i>
     * In this case, first the old {@code oldCalleeStart} and new {@code newCalleeStart} start locations
     * of the callee method are obtained.
     * After that, the entry point offset into the old callee location is obtained:
     * {@code epoffset = itarget - oldCalleeStart}.
     * Finally, the actual target is computed:
     * {@code newTarget = newCalleeStart + epoffset}.</li>
     * <li><i>Direct call to non-moved code.</i>
     * In this case, the correct target is simply the intended target:
     * {@code newTarget = itarget}.</li>
     * </ul>
     */
    final class BaselineFixCalls implements TargetMethod.Closure {
        int fixed;

        @Override
        public boolean doTargetMethod(TargetMethod targetMethod) {
            final Offset delta = targetMethod.start().minus(targetMethod.oldStart()).asOffset();
            logFixCallForMovedCode(targetMethod, delta);
            final Safepoints safepoints = targetMethod.safepoints();
            for (int spi = safepoints.nextDirectCall(0); spi >= 0; spi = safepoints.nextDirectCall(spi + 1)) {
                final int callPos = safepoints.causePosAt(spi);
                final CodePointer target = AMD64TargetMethodUtil.readCall32Target(targetMethod, callPos);
                final CodePointer itarget = target.minus(delta);
                logDirectCallInfo(spi, callPos, target, itarget);
                if (CodeManager.runtimeBaselineCodeRegion.isInFromSpace(itarget.toAddress())) {
                    // direct call to moved code
                    final TargetMethod callee = CodeManager.runtimeBaselineCodeRegion.findInFromSpace(itarget.toAddress());
                    assert callee != null : "callee should not be null, from-space address " + itarget.to0xHexString();
                    final Address oldCalleeStart = callee.oldStart();
                    final Address newCalleeStart = callee.start();
                    final Address epoffset = itarget.minus(oldCalleeStart).toAddress();
                    final CodePointer newTarget = CodePointer.from(newCalleeStart.plus(epoffset));
                    logToMoved(callee, oldCalleeStart, newCalleeStart, epoffset, newTarget);
                    targetMethod.fixupCallSite(callPos, newTarget);
                } else {
                    // direct call to unmoved code
                    logToUnmoved(itarget);
                    targetMethod.fixupCallSite(callPos, itarget);
                }
                ++fixed;
            }
            return true;
        }
    }

    /**
     * Fixes all direct calls in the machine code of a moved (baseline) target method.
     *
     * After a method has been relocated, calls to it from unmoved code must be updated.
     *
     * The following assumes the call target extractable from a direct call site is named {@code target}.
     *
     * First, the old {@code oldCalleeStart} and new {@code newCalleeStart} start locations
     * of the callee method are obtained.
     * Next, the entry point offset into the old callee location is obtained:
     * {@code epoffset = target - oldCalleeStart}.
     * Finally, the actual target is computed:
     * {@code newTarget = newCalleeStart + epoffset}.
     */
    final class OptFixCalls implements TargetMethod.Closure {
        int fixed;

        @Override
        public boolean doTargetMethod(TargetMethod targetMethod) {
            boolean haveLoggedMethod = false;
            final Safepoints safepoints = targetMethod.safepoints();
            for (int spi = safepoints.nextDirectCall(0); spi >= 0; spi = safepoints.nextDirectCall(spi + 1)) {
                final int callPos = safepoints.causePosAt(spi);
                final CodePointer target = AMD64TargetMethodUtil.readCall32Target(targetMethod, callPos);
                if (CodeManager.runtimeBaselineCodeRegion.isInFromSpace(target.toAddress())) {
                    if (!haveLoggedMethod) {
                        logOptMethod(targetMethod);
                        haveLoggedMethod = true;
                    }
                    logDirectCallInfo(spi, callPos, target, CodePointer.zero());
                    final TargetMethod callee = CodeManager.runtimeBaselineCodeRegion.findInFromSpace(target.toAddress());
                    assert callee != null : "callee should not be null, from-space address " + target.to0xHexString();
                    assert !callee.isWiped() : "callee should not have been wiped, from-space address " + target.to0xHexString();
                    final Address oldCalleeStart = callee.oldStart();
                    final Address newCalleeStart = callee.start();
                    final Address epoffset = target.toAddress().minus(oldCalleeStart);
                    final CodePointer newTarget = CodePointer.from(newCalleeStart.plus(epoffset));
                    logToMoved(callee, oldCalleeStart, newCalleeStart, epoffset, newTarget);
                    targetMethod.fixupCallSite(callPos, newTarget);
                    ++fixed;
                }
            }
            return true;
        }
    }

    private static enum Phase {
        DUMPING,
        PATCHING,
        COMPACTING
    }

    private Phase phase;

    public CodeEviction() {
        super("code cache cleaner", null, Mode.Safepoint);
    }

    private static int evictionCount = 0;

    public static int evictionCount() {
        return evictionCount;
    }

    @Override
    protected void doIt() {

        ++evictionCount;

        if (TraceCodeEviction > TRACE_NONE) { // do not use logLevel() here
            Log.print("starting code eviction run #");
            Log.print(evictionCount);
            Log.print(" triggered by ");
            Log.printThread(callingThread(), true);
        }

        // phase 0 (optional): dump before
        if (logLevel(TRACE_FULL_DUMP)) {
            phase = Phase.DUMPING;
            dumpCodeAddresses("before");
        }

        // phase 1: identify stale methods and invalidate them, patching all relevant dispatch table entries and direct calls

        phase = Phase.PATCHING;

        CodeManager.Inspect.notifyEvictionStarted(CodeManager.runtimeBaselineCodeRegion);

        timerStart();
        doAllThreads();
        tMarking = timerEnd();

        timerStart();
        markProtectedMethods();
        tMarkProtected = timerEnd();

        invalidateDirectCalls();

        timerStart();
        invalidateDispatchTableEntries();
        tInvalidateTables = timerEnd();

        if (CodeManager.CodeCacheContentionFrequency > 0) {
            Code.getCodeManager().recordSurvivorSize(nSurvivingBytes);
        }

        logStatistics();

        resetCounters();

        // phase 2: compact the baseline code cache and patch all PC values and return addresses

        phase = Phase.COMPACTING;

        timerStart();
        compact();
        tCompact = timerEnd();

        fixCallSitesForMovedCode();
        logFixed();

        timerStart();
        doAllThreads();
        tPatchStacks = timerEnd();

        CodeManager.runtimeBaselineCodeRegion.resetFromSpace();
        if (logLevel(TRACE_THREADS_CODE_MOTION)) {
            Log.println("FINISHED walking threads");
        }

        CodeManager.Inspect.notifyEvictionCompleted(CodeManager.runtimeBaselineCodeRegion);

        // phase 3 (optional): dump after
        if (logLevel(TRACE_FULL_DUMP)) {
            phase = Phase.DUMPING;
            dumpCodeAddresses("after");
        }

        if (TraceCodeEviction > TRACE_NONE) { // do not use logLevel() here
            Log.print("completed code eviction run #");
            Log.print(evictionCount);
            Log.print(" triggered by ");
            Log.printThread(callingThread(), true);
        }
        logTimingResults();
    }

    /**
     * Perform a specific action for a given thread.
     * This method is invoked multiple times during the execution of {@linkplain #doIt()}.
     * What action is performed is controlled by the {@linkplain phase} member,
     * which is set accordingly in {@linkplain #doIt()}.
     */
    @Override
    protected void doThread(VmThread vmThread, Pointer ip, Pointer sp, Pointer fp) {
        // bail out if the thread was stopped in native code before invoking any Java method
        if (ip.isZero() && sp.isZero() && fp.isZero()) {
            return;
        }

        walker.setTLA(vmThread.tla());

        switch(phase) {
            case DUMPING:
                threadName = vmThread.getName();
                walker.inspect(ip, sp, fp, stackDumper);
                break;
            case PATCHING :
                // collect all reachable baseline methods
                walker.inspect(ip, sp, fp, liveMethodsMarker);
                break;
            case COMPACTING:
                // walk all stacks, patching PC values, return addresses, and local variables
                logThread(vmThread);
                CodeManager.runtimeBaselineCodeRegion.allowFromSpaceLookup = true;
                walker.inspect(ip, sp, fp, stackPatcher);
                CodeManager.runtimeBaselineCodeRegion.allowFromSpaceLookup = false;
                if (logLevel(TRACE_THREADS_CODE_MOTION)) {
                    Throw.stackDump("dump after patching", ip, sp, fp);
                }
                break;
            default:
                throw FatalError.unexpected("invalid code cache cleaner phase");
        }
    }

    private final VmStackFrameWalker walker = new VmStackFrameWalker(Pointer.zero());

    private final LiveMethodsMarker liveMethodsMarker = new LiveMethodsMarker();

    private final ProtectedMethodsMarker protectedMethodsMarker = new ProtectedMethodsMarker();

    private final StackPatcher stackPatcher = new StackPatcher();

    private final CodePointerRelocator codePointerRelocator = new CodePointerRelocator();

    private void markProtectedMethods() {
        CodeManager.runtimeBaselineCodeRegion.doNewTargetMethods(protectedMethodsMarker);
    }

    /**
     * Iterate over the baseline code region and invalidate references to stale methods.
     * This includes vtable and itable entries as well as {@linkplain MethodActor} target states.
     * These entries are invalidated by letting them reference the respective trampolines again.
     * This also wipes out all invalidated methods by overwriting them with illegal instructions,
     * and unmarks all marked methods.
     */
    private void invalidateDispatchTableEntries() {
        // to/from have not yet been flipped
        CodeManager.runtimeBaselineCodeRegion.doNewTargetMethods(invalidateDispatchTables);
        logPatchDetails();
    }

    private final InvalidateDispatchTables invalidateDispatchTables = new InvalidateDispatchTables();

    /**
     * Patch the dispatch table entries for a given {@linkplain TargetMethod}.
     */
    private void patchDispatchTables(final TargetMethod tm, final boolean count) {
        final ClassMethodActor cma = tm.classMethodActor;
        if (cma instanceof VirtualMethodActor) {
            final ClassActor ca = cma.holder();
            final DynamicHub holderHub = ca.dynamicHub();
            final StaticHub staticHub = ca.staticHub();
            final int vtableIndex = ((VirtualMethodActor) cma).vTableIndex();
            final boolean patchStaticHubAndArrayHubs = vtableIndex < StaticHub.vTableStartIndex() + staticHub.vTableLength();
            if (vtableIndex >= 0) { // constructors and other nonvirtual methods are dealt with below
                if (count) {
                    ++nVT;
                }
                holderHub.resetVTableEntry(vtableIndex);
                logDispatchTableReset('V', holderHub, vtableIndex);
                if (patchStaticHubAndArrayHubs) {
                    staticHub.resetVTableEntry(vtableIndex);
                    logDispatchTableReset('V', staticHub, vtableIndex);
                    patchArrayHubs(vtableIndex);
                }
                // act accordingly if this method is also an interface method
                patchItables(cma, ca, holderHub, tm);
                // include subclasses (the vtable index stays the same for this method)
                subclassPatcher.cma = cma;
                subclassPatcher.tm = tm;
                subclassPatcher.vtableIndex = vtableIndex;
                subclassPatcher.patchStaticHub = patchStaticHubAndArrayHubs;
                ca.allSubclassesDo(subclassPatcher);
            } else {
                assert vtableIndex != VirtualMethodActor.INVALID_VTABLE_INDEX : "must not be an invalid vtable index";
                assert cma.isLeafMethod() || cma.isConstructor() : "expected leaf (statically resolvable) method or constructor";
                if (vtableIndex != VirtualMethodActor.INVALID_VTABLE_INDEX) {
                    if (count) {
                        ++nNonvirtual;
                    }
                    if (logLevel(TRACE_DETAILS)) {
                        Log.println("  NONVIRTUAL");
                    }
                }
            }
        } else if (cma instanceof StaticMethodActor) {
            if (count) {
                ++nStatic;
            }
            if (logLevel(TRACE_DETAILS)) {
                Log.println("  STATIC");
            }
        } else {
            throw FatalError.unexpected("unexpected target method type: " + cma.getClass().getName() + " for " + cma);
        }
    }

    private final SubclassPatcher subclassPatcher = new SubclassPatcher();

    private class SubclassPatcher implements ClassActor.Closure {
        int vtableIndex;
        ClassMethodActor cma;
        TargetMethod tm;
        boolean patchStaticHub;

        @Override
        public boolean doClass(ClassActor ca) {
            ++nVT;
            DynamicHub hub = ca.dynamicHub();
            hub.resetVTableEntry(vtableIndex);
            logDispatchTableReset('V', hub, vtableIndex);
            if (patchStaticHub) {
                StaticHub shub = ca.staticHub();
                shub.resetVTableEntry(vtableIndex);
                logDispatchTableReset('V', shub, vtableIndex);
            }
            patchItables(cma, ca, hub, tm);
            return true;
        }
    }

    /**
     * Helper function to collect patch locations for interface methods.
     */
    private void patchItables(ClassMethodActor cma, ClassActor ca, DynamicHub holderHub, TargetMethod tm) {
        final int lastITableIndex = holderHub.iTableStartIndex + holderHub.iTableLength;
        for (int i = holderHub.iTableStartIndex; i < lastITableIndex; i++) {
            if (holderHub.getWord(i).equals(tm.getEntryPoint(VTABLE_ENTRY_POINT))) {
                ++nIT;
                holderHub.resetITableEntry(i);
                logDispatchTableReset('I', holderHub, i);
            }
        }
    }

    /**
     * Patches all dynamic and static hubs of array classes.
     * This separate treatment is required as array classes are not part of the hierarchy represented in {@link ClassActor}.
     */
    private void patchArrayHubs(final int vtableIndex) {
        arrayHubPatcher.vtableIndex = vtableIndex;
        ClassActor.allNonInstanceClassesDo(arrayHubPatcher);
    }

    private final ArrayHubPatcher arrayHubPatcher = new ArrayHubPatcher();

    private class ArrayHubPatcher implements ClassActor.Closure {
        int vtableIndex;

        @Override
        public boolean doClass(ClassActor ca) {
            if (ca instanceof ArrayClassActor) {
                final DynamicHub dhub = ca.dynamicHub();
                final StaticHub shub = ca.staticHub();
                dhub.resetVTableEntry(vtableIndex);
                logDispatchTableReset('V', dhub, vtableIndex);
                shub.resetVTableEntry(vtableIndex);
                logDispatchTableReset('V', shub, vtableIndex);
            }
            return true;
        }
    }

    /**
     * Iterate over native code and identify places to patch. Those places are all direct call sites that call
     * one of the stale methods.
     */
    private void invalidateDirectCalls() {
        // to/from have not yet been flipped
        timerStart();
        CodeManager.runtimeBaselineCodeRegion.doNewTargetMethods(invalidateBaselineDirectCalls);
        tInvalidateCallsBaseline = timerEnd();

        timerStart();
        CodeManager.runtimeOptCodeRegion.doAllTargetMethods(invalidateOptDirectCalls);
        tInvalidateCallsOpt = timerEnd();

        timerStart();
        invalidateBootDirectCalls();
        tInvalidateCallsBoot = timerEnd();

        logDirectCallNumbers();
    }

    private final InvalidateBaselineDirectCalls invalidateBaselineDirectCalls = new InvalidateBaselineDirectCalls();

    private final InvalidateOptDirectCalls invalidateOptDirectCalls = new InvalidateOptDirectCalls();

    private void invalidateBootDirectCalls() {
        final int btbSize = CodeManager.bootToBaselineSize();
        if (btbSize != 0) {
            final TargetMethod[] bootDirectCallers = CodeManager.bootToBaselineCallers();
            for (int i = 0; i < btbSize; ++i) {
                final TargetMethod tm = bootDirectCallers[i];
                ++nBootMeth;
                nBootDirect += tm.safepoints().numberOfDirectCalls();
                nCallBoot += patchDirectCallsIn(tm);
            }
        }
    }

    private int directCalleePosition(TargetMethod tm, int callPos) {
        final Safepoints safepoints = tm.safepoints();
        int dcIndex = 0;
        for (int i = 0; i < safepoints.size(); i++) {
            if (safepoints.isSetAt(DIRECT_CALL, i)) {
                if (safepoints.causePosAt(i) == callPos) {
                    return dcIndex;
                }
                dcIndex++;
            }
        }
        return -1;
    }

    private int patchDirectCallsIn(TargetMethod tm) {
        int calls = 0;
        final Safepoints safepoints = tm.safepoints();
        for (int spi = safepoints.nextDirectCall(0); spi >= 0; spi = safepoints.nextDirectCall(spi + 1)) {
            final int callPos = safepoints.causePosAt(spi);
            final CodePointer target = AMD64TargetMethodUtil.readCall32Target(tm, callPos);
            final TargetMethod callee = target.toTargetMethod();
            assert callee != null : "callee should not be null in " + tm + "@" + callPos + " " + target.to0xHexString();
            final int dcIndex = directCalleePosition(tm, callPos);
            assert dcIndex >= 0 : "direct callee index should not be -1 for " + tm + "@" + callPos + " calling " + callee;
            if (isStaleCallee(callee)) {
                ++calls;
                logDirectCallReset(tm, spi, callee);
                tm.resetDirectCall(spi, dcIndex);
            }
        }
        return calls;
    }

    private boolean isStaleCallee(TargetMethod tm) {
        return tm != null && CodeManager.runtimeBaselineCodeRegion.contains(tm.codeStart().toAddress()) && !tm.isMarked() && !tm.isWiped();
    }

    /**
     * Compact the baseline code cache, i.e., flip spaces and copy all survivors (non-wiped methods) from from-space
     * to to-space.
     */
    private void compact() {
        final SemiSpaceCodeRegion cr = CodeManager.runtimeBaselineCodeRegion;
        if (logLevel(TRACE_THREADS_CODE_MOTION)) {
            Log.println("compacting code cache: copying starts ...");
        }
        cr.flip();
        logCodeCacheBoundaries(cr);
        cr.doOldTargetMethods(copySurvivors);
        if (logLevel(TRACE_THREADS_CODE_MOTION)) {
            Log.println("copying done!");
        }
    }

    private final CopySurvivors copySurvivors = new CopySurvivors();

    /**
     * Iterate over all methods code caches one last time, fixing direct calls to moved code.
     */
    private void fixCallSitesForMovedCode() {
        if (logLevel(TRACE_THREADS_CODE_MOTION)) {
            Log.println("fixing call sites ...");
            Log.println("moved code ...");
        }

        timerStart();
        baselineFixCalls.fixed = 0;
        CodeManager.runtimeBaselineCodeRegion.doNewTargetMethods(baselineFixCalls);
        nCallBaseline = baselineFixCalls.fixed;
        tFixCallsBaseline = timerEnd();

        if (logLevel(TRACE_THREADS_CODE_MOTION)) {
            Log.println("optimised code ...");
        }

        timerStart();
        optFixCalls.fixed = 0;
        CodeManager.runtimeOptCodeRegion.doAllTargetMethods(optFixCalls);
        nCallOpt = optFixCalls.fixed;
        tFixCallsOpt = timerEnd();

        if (logLevel(TRACE_THREADS_CODE_MOTION)) {
            Log.println("boot code ...");
        }

        timerStart();
        optFixCalls.fixed = 0;
        if (CodeManager.bootToBaselineSize() > 0) {
            CodeManager.bootToBaselineDo(optFixCalls);
        }
        nCallBoot = optFixCalls.fixed;
        tFixCallsBoot = timerEnd();

        if (logLevel(TRACE_THREADS_CODE_MOTION)) {
            Log.println("fixing done!");
        }
    }

    /**
     * It is crucial that *all* direct calls in moved code are fixed, because they are relative and hence no longer
     * valid after code motion.
     */
    private final BaselineFixCalls baselineFixCalls = new BaselineFixCalls();

    /**
     * Optimised and boot image code might contain direct calls to moved baseline code.
     * These are fixed here.
     */
    private final OptFixCalls optFixCalls = new OptFixCalls();

    private boolean wasMoved(TargetMethod tm) {
        return CodeManager.runtimeBaselineCodeRegion.isInToSpace(tm.codeStart().toAddress());
    }

    private void resetCounters() {
        nStale = 0;
        nStaleBytes = 0;
        nBaseDirect = 0;
        nBaseMeth = 0;
        nOptDirect = 0;
        nOptMeth = 0;
        nBootDirect = 0;
        nBootMeth = 0;
        nSurvivors = 0;
        nSurvivingBytes = 0;
        nVT = 0;
        nIT = 0;
        nNonvirtual = 0;
        nStatic = 0;
        nCallBaseline = 0;
        nCallOpt = 0;
        nCallBoot = 0;
    }

    int nStale = 0;
    int nStaleBytes = 0;
    int nBaseDirect = 0;
    int nBaseMeth = 0;
    int nOptDirect = 0;
    int nOptMeth = 0;
    int nBootDirect = 0;
    int nBootMeth = 0;
    int nSurvivors = 0;
    int nSurvivingBytes = 0;
    int nVT = 0;
    int nIT = 0;
    int nNonvirtual = 0;
    int nStatic = 0;
    int nCallBaseline = 0;
    int nCallOpt = 0;
    int nCallBoot = 0;

    private long timer;
    private long tMarking;
    private long tMarkProtected;
    private long tInvalidateCallsBaseline;
    private long tInvalidateCallsOpt;
    private long tInvalidateCallsBoot;
    private long tInvalidateTables;
    private long tCompact;
    private long tPatchStacks;
    private long tFixCallsBaseline;
    private long tFixCallsOpt;
    private long tFixCallsBoot;
    private long tTotal;

    private void timerStart() {
        timer = System.nanoTime();
    }

    private long timerEnd() {
        return System.nanoTime() - timer;
    }

    private void printTime(String s, long t) {
        Log.print(s);
        Log.print(t);
        Log.print(" ns (");
        Log.print(t / 1000000.0);
        Log.print(" ms, ");
        Log.print(t * 100.0 / tTotal);
        Log.println(" %)");
    }

    private void logTimingResults() {
        if (logLevel(TRACE_STAT)) {
            Log.println("timing summary");
            tTotal =
                tMarking + tMarkProtected +
                tInvalidateCallsBaseline + tInvalidateCallsOpt + tInvalidateCallsBoot + tInvalidateTables +
                tCompact + tPatchStacks + tFixCallsBaseline + tFixCallsOpt + tFixCallsBoot;
            Log.print("total ");
            Log.print(tTotal / 1000000.0);
            Log.println(" ms");
            printTime("  Phase 1 - mark                       ", tMarking);
            printTime("            mark protected methods     ", tMarkProtected);
            printTime("            invalidate baseline calls  ", tInvalidateCallsBaseline);
            printTime("            invalicate opt calls       ", tInvalidateCallsOpt);
            printTime("            invalidate boot calls      ", tInvalidateCallsBoot);
            printTime("            invalidate dispatch tables ", tInvalidateTables);
            printTime("  Phase 2 - compact                    ", tCompact);
            printTime("            patch stacks               ", tPatchStacks);
            printTime("            fix baseline calls         ", tFixCallsBaseline);
            printTime("            fix opt calls              ", tFixCallsOpt);
            printTime("            fix boot calls             ", tFixCallsBoot);
        }
    }

    /**
     * Dump all code addresses from stacks (return addresses), vtables/itables/targetStates, and direct calls.
     */
    private void dumpCodeAddresses(String when) {
        Log.print("++++++++++ start dump ");
        Log.print(when);
        Log.println(" code eviction ++++++++++");
        doAllThreads();
        dumpTables();
        dumpDirectCalls();
        Log.print("++++++++++ end dump ");
        Log.print(when);
        Log.println(" code eviction ++++++++++");
    }

    private String threadName;

    /**
     * Dump one piece of information in the form [context] [target method] [target method code start] +[offset],
     * with tabs in between the elements.
     */
    private void dump(final String context, final DumpElement element, final TargetMethod tm, final CodePointer a) {
        Log.print(context);
        Log.print('\t');
        element.print();
        Log.print('\t');
        if (tm != null) {
            Log.printMethod(tm, false);
            Log.print('\t');
            CodePointer cs = tm.codeStart();
            Log.print(cs);
            Log.print('\t');
            final Offset offset = a.minus(cs).toOffset();
            Log.println(offset.toInt());
        } else {
            Log.print("<null>\t");
            Log.print(a);
            Log.println("\t<null>");
        }
    }

    private interface DumpElement {
        void print();
    }

    private final DumpElement printThreadName = new DumpElement() {
        @Override
        public void print() {
            Log.print(threadName);
        }
    };

    private final StackDumper stackDumper = new StackDumper();

    private void dumpTables() {
        ClassActor.allClassesDo(dumpDispatchTables);
    }

    private final DumpDispatchTables dumpDispatchTables = new DumpDispatchTables();

    private String s1;
    private String s2;
    private int idx;

    private final DumpElement printTriple = new DumpElement() {
        @Override
        public void print() {
            Log.print(s1);
            Log.print(s2);
            Log.print(idx);
        }
    };

    private void dumpHub(final Hub hub, final int vstart, final boolean dumpVtable, final String context) {
        s1 = hub.toString();

        if (dumpVtable) {
            s2 = "@V";
            final int vend = vstart + hub.vTableLength();
            for (int i = vstart; i < vend; ++i) {
                final CodePointer address = CodePointer.from(hub.getWord(i));
                final TargetMethod tm = address.toTargetMethod();
                idx = i;
                dump(context, printTriple, tm, address);
            }
        }

        s2 = "@I";
        final int istart = hub.iTableStartIndex;
        final int iend = istart + hub.iTableLength;
        for (int i = istart + 1; i < iend; ++i) {
            final CodePointer p = CodePointer.from(hub.getWord(i));
            if (!(p.isZero() || Hub.validItableEntry(p))) {
                final TargetMethod tm = p.toTargetMethod();
                idx = i;
                dump(context, printTriple, tm, p);
            }
        }
    }

    private void dumpDirectCalls() {
        s2 = "@";
        CodeManager.runtimeBaselineCodeRegion.doNewTargetMethods(dumpDirectCalls);
        CodeManager.runtimeOptCodeRegion.doAllTargetMethods(dumpDirectCalls);
        Code.bootCodeRegion().doAllTargetMethods(dumpDirectCalls);
    }

    private final DumpDirectCalls dumpDirectCalls = new DumpDirectCalls();

    @NEVER_INLINE
    private void logStaleMethod(TargetMethod targetMethod) {
        if (logLevel(TRACE_DETAILS)) {
            Log.print(nStale);
            Log.print(". ");
            Log.print(targetMethod);
            Log.print(" - invocations: ");
            Log.println(MethodInstrumentation.initialEntryCount - targetMethod.profile().entryCount);
        }
    }

    @NEVER_INLINE
    private void logMethodPatch(final TargetMethod tm, final boolean patch) {
        if (logLevel(TRACE_THREADS_CODE_MOTION)) {
            if (patch) {
                Log.print("  P ");
            } else {
                Log.print("    ");
            }
            Log.println(tm);
        }
    }

    @NEVER_INLINE
    private void logCalleeReturnAddress(StackFrameCursor callee) {
        if (logLevel(TRACE_THREADS_CODE_MOTION) && callee != null && callee.targetMethod() != null) {
            final Pointer rap = callee.targetMethod().returnAddressPointer(callee);
            final Pointer ret = rap.readWord(0).asPointer();
            Log.print("    \\---> callee return address ");
            Log.println(ret);
        }
    }

    @NEVER_INLINE
    private void logReturnAddressPatch(final TargetMethod tm, final CodePointer calleeRet, final CodePointer newCalleeRet) {
        if (logLevel(TRACE_THREADS_CODE_MOTION)) {
            Log.print("   \\---> callee return address (value to patch) ");
            Log.print(calleeRet);
            Log.print(" -> ");
            Log.println(newCalleeRet);
            if (!(calleeRet.toAddress().greaterEqual(tm.oldStart()) && tm.oldStart().plus(tm.size()).greaterEqual(calleeRet.toAddress()))) {
                Log.print("    >>> old return address not in old method memory, but in ");
                Log.println(calleeRet.toTargetMethod());
            }
            if (!(newCalleeRet.toAddress().greaterEqual(tm.start()) && tm.start().plus(tm.size()).greaterEqual(newCalleeRet.toAddress()))) {
                Log.print("    >>> new return address not in new method memory, but in ");
                Log.println(newCalleeRet.toTargetMethod());
            }
        }
    }

    @NEVER_INLINE
    private void logCodeMotion(TargetMethod targetMethod, final Pointer from, final Pointer to, final Size size) {
        if (logLevel(TRACE_THREADS_CODE_MOTION)) {
            Log.print("copying ");
            Log.print(targetMethod);
            Log.print(" [");
            Log.print(from);
            Log.print("..");
            Log.print(from.plus(size));
            Log.print("]->[");
            Log.print(to);
            Log.print("..");
            Log.print(to.plus(size).minus(1));
            Log.println(']');
        }
    }

    @NEVER_INLINE
    private void logNotCopying(TargetMethod tm) {
        if (logLevel(TRACE_THREADS_CODE_MOTION)) {
            Log.print("NOT copying ");
            Log.print(tm);
            Log.print(" [");
            Log.print(tm.start());
            Log.print("..");
            Log.print(tm.start().plus(tm.size()).minus(1));
            Log.println(']');
        }
    }

    @NEVER_INLINE
    private void logFixCall(TargetMethod targetMethod, int i, CodePointer callTarget) {
        if (logLevel(TRACE_THREADS_CODE_MOTION)) {
            final TargetMethod callee = callTarget.toTargetMethod();
            Log.print("direct call ");
            Log.print(targetMethod);
            Log.print('@');
            Log.print(i);
            Log.print(" -> ");
            Log.print(callTarget);
            Log.print("=");
            Log.print(callee);
            if (callee == null) {
                Log.print(" (trampoline)");
            }
            Log.println();
        }
    }

    @NEVER_INLINE
    private void logStatistics() {
        if (logLevel(TRACE_STAT)) {
            Log.print("code eviction: ");
            Log.print(nStale);
            Log.print(" stale methods (");
            Log.print(nStaleBytes / 1024);
            Log.print(" kB); ");
            Log.print(nSurvivors);
            Log.print(" survivors (");
            Log.print(nSurvivingBytes / 1024);
            Log.print(" kB) - ");
            final int totalMethods = nSurvivors + nStale;
            Log.print(nSurvivors * 100 / totalMethods);
            Log.println(" % of methods survived");
        }
    }

    @NEVER_INLINE
    private void logPatchDetails() {
        if (logLevel(TRACE_DETAILS)) {
            Log.print("dispatch table / target state summary: patched ");
            Log.print(nVT);
            Log.print(" vtable, ");
            Log.print(nIT);
            Log.print(" itable entries; ");
            Log.print(nNonvirtual);
            Log.print(" nonvirtual (leaves), ");
            Log.print(nStatic);
            Log.println(" static methods");
        }
    }

    @NEVER_INLINE
    private void logCodeCacheBoundaries(final SemiSpaceCodeRegion cr) {
        if (logLevel(TRACE_THREADS_CODE_MOTION)) {
            Log.print("flipped spaces. from: ");
            Log.print(cr.fromSpace);
            Log.print("..");
            Log.print(cr.fromSpace.plus(cr.spaceSize).minus(1));
            Log.print(", to: ");
            Log.print(cr.toSpace);
            Log.print("..");
            Log.println(cr.toSpace.plus(cr.spaceSize).minus(1));
        }
    }

    @NEVER_INLINE
    private void logDirectCallReset(TargetMethod tm, int i, final TargetMethod callee) {
        if (logLevel(TRACE_DETAILS)) {
            Log.print("DIRECT CALL ");
            Log.print(tm);
            Log.print('@');
            Log.print(i);
            Log.print(" -> ");
            Log.println(callee);
        }
    }

    @NEVER_INLINE
    private void logDirectCallNumbers() {
        if (logLevel(TRACE_STAT)) {
            Log.print("patched direct calls: ");
            Log.print(nCallBaseline);
            Log.print(" baseline, ");
            Log.print(nCallOpt);
            Log.print(" opt, ");
            Log.print(nCallBoot);
            Log.print(" boot; looked at ");
            Log.print(nBaseDirect);
            Log.print(" sites in ");
            Log.print(nBaseMeth);
            Log.print(" baseline methods, ");
            Log.print(nOptDirect);
            Log.print(" sites in ");
            Log.print(nOptMeth);
            Log.print(" opt methods, and ");
            Log.print(nBootDirect);
            Log.print(" sites in ");
            Log.print(nBootMeth);
            Log.println(" boot image methods");
        }
    }

    @NEVER_INLINE
    private void logDispatchTableReset(char tableKind, Hub hub, int index) {
        if (logLevel(TRACE_DETAILS)) {
            Log.print("  ");
            Log.print(tableKind);
            Log.print("TABLE ");
            Log.print(hub);
            Log.print('@');
            Log.println(index);
        }
    }

    @NEVER_INLINE
    private void logThread(VmThread vmThread) {
        if (logLevel(TRACE_THREADS_CODE_MOTION)) {
            Log.print("THREAD ");
            Log.println(vmThread.getName());
        }
    }

    @NEVER_INLINE
    private void logFixCallForMovedCode(TargetMethod tm, Offset d) {
        if (logLevel(TRACE_THREADS_CODE_MOTION)) {
            Log.print("fixing calls for moved method ");
            Log.println(tm);
            Log.print("  moved by ");
            Log.println(d.toInt());
        }
    }

    @NEVER_INLINE
    private void logDirectCallInfo(int i, int callPos, CodePointer target, CodePointer itarget) {
        if (logLevel(TRACE_THREADS_CODE_MOTION)) {
            Log.print("  ");
            Log.print(i);
            Log.print(". ");
            Log.print("call pos ");
            Log.print(callPos);
            Log.print(" target ");
            Log.print(target);
            if (!itarget.isZero()) {
                Log.print(" itarget ");
                Log.print(itarget);
            }
            Log.println();
        }
    }

    @NEVER_INLINE
    private void logToMoved(TargetMethod callee, Address oldCalleeStart, Address newCalleeStart, Address epoffset, CodePointer newTarget) {
        if (logLevel(TRACE_THREADS_CODE_MOTION)) {
            Log.print("    TO MOVED ");
            Log.println(callee);
            Log.print("    old start ");
            Log.print(oldCalleeStart);
            Log.print(" new start ");
            Log.print(newCalleeStart);
            Log.print(" epoffset ");
            Log.println(epoffset);
            Log.print("    NEW TARGET ");
            Log.print(newTarget);
            Log.print(' ');
            if (!CodeManager.runtimeBaselineCodeRegion.isInToSpace(newTarget.toAddress())) {
                Log.print("NOT IN TO-SPACE ");
            }
            CodeManager.runtimeBaselineCodeRegion.allowFromSpaceLookup = true;
            Log.println(newTarget.toTargetMethod());
            CodeManager.runtimeBaselineCodeRegion.allowFromSpaceLookup = false;
        }
    }

    @NEVER_INLINE
    private void logToUnmoved(CodePointer itarget) {
        if (logLevel(TRACE_THREADS_CODE_MOTION)) {
            Log.print("    TO UNMOVED ");
            Log.println(itarget.toTargetMethod());
        }
    }

    @NEVER_INLINE
    private void logOptMethod(TargetMethod tm) {
        if (logLevel(TRACE_THREADS_CODE_MOTION)) {
            Log.print("fixing calls for opt method ");
            Log.println(tm);
        }
    }

    @NEVER_INLINE
    private void logFixed() {
        if (logLevel(TRACE_STAT)) {
            Log.print("fixed direct calls: ");
            Log.print(nCallBaseline);
            Log.print(" baseline, ");
            Log.print(nCallOpt);
            Log.println(" opt");
        }
    }

    @NEVER_INLINE
    private void logMark(String s, TargetMethod tm) {
        if (logLevel(TRACE_DETAILS)) {
            Log.print("MARKING ");
            Log.print(s);
            Log.print(' ');
            Log.println(tm);
        }
    }

    @NEVER_INLINE
    private void logMarkLevel(String s, TargetMethod tm, int level) {
        if (logLevel(TRACE_DETAILS)) {
            Log.print("MARKING (level ");
            Log.print(CodeEvictionProtectCalleeDepth - level + 1);
            Log.print(')');
            Log.print(s);
            Log.print(' ');
            Log.println(tm);
        }
    }

    @NEVER_INLINE
    private void logVisitRefMapBits(StackFrameCursor cursor, Pointer slotPointer, int refMap, int numBits) {
        if (logLevel(TRACE_DETAILS)) {
            Log.print("visit ref map bits for code relocation: ");
            Log.print(cursor.targetMethod());
            Log.print(" slot pointer ");
            Log.print(slotPointer);
            Log.print(" ref map ");
            Log.print(refMap);
            Log.print(" num bits ");
            Log.println(numBits);
            if (refMap == 0) {
                Log.println("   empty ref map");
            }
        }
    }

    @NEVER_INLINE
    private void logRelocateCodePointer(int bitIndex, CodePointer from, CodePointer to, TargetMethod tm) {
        if (logLevel(TRACE_DETAILS)) {
            Log.print("   bit ");
            Log.print(bitIndex);
            Log.print(" from ");
            Log.print(from);
            Log.print(" to ");
            Log.print(to);
            Log.print(": ");
            Log.println(tm);
        }
    }

}
