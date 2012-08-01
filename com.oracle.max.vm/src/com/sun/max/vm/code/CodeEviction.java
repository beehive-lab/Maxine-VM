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
import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

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
import com.sun.max.vm.log.VMLog.Record;
import com.sun.max.vm.log.hosted.*;
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
 * The workflow, controlled from the {@link #doIt()} method, is as follows.
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
 * This step also takes care of resetting references to native code in {@linkplain ClassMethodActor method actors}
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

    /**
     * Protect baseline methods until the given callee depth.
     */
    private static int CodeEvictionProtectCalleeDepth = 1;

    static {
        VMOptions.addFieldOption("-XX:", "CodeEvictionProtectCalleeDepth", CodeEviction.class,
            "During code eviction, protect callees of on-stack methods up until the given depth (default: 1).",
            MaxineVM.Phase.STARTING);
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
     * <li>whose invocation count is within the threshold denoted by {@link MethodInstrumentation#PROTECTION_PERCENTAGE}.</li>
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

        if (codeEvictionLogger.enabled()) {
            codeEvictionLogger.logRun("starting", evictionCount, callingThread());
        }

        // phase 0 (optional): dump before
        if (logging()) {
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
        if (logging()) {
            codeEvictionLogger.logMove_Progress("FINISHED walking threads");
        }

        CodeManager.Inspect.notifyEvictionCompleted(CodeManager.runtimeBaselineCodeRegion);

        // phase 3 (optional): dump after
        if (logging()) {
            phase = Phase.DUMPING;
            dumpCodeAddresses("after");
        }

        if (codeEvictionLogger.enabled()) {
            codeEvictionLogger.logRun("completed", evictionCount, callingThread());
        }
        logTimingResults();
    }

    /**
     * Perform a specific action for a given thread.
     * This method is invoked multiple times during the execution of {@linkplain #doIt()}.
     * What action is performed is controlled by the {@linkplain #phase} member,
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
                if (logging()) {
                    stackDump(ip, sp, fp);
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
                    if (logging()) {
                        codeEvictionLogger.logDetails_PatchType("  NONVIRTUAL");
                    }
                }
            }
        } else if (cma instanceof StaticMethodActor) {
            if (count) {
                ++nStatic;
            }
            if (logging()) {
                codeEvictionLogger.logDetails_PatchType("  STATIC");
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
        if (logging()) {
            codeEvictionLogger.logMove_Progress("compacting code cache: copying starts ...");
        }
        cr.flip();
        logCodeCacheBoundaries(cr);
        cr.doOldTargetMethods(copySurvivors);
        if (logging()) {
            codeEvictionLogger.logMove_Progress("copying done!");
        }
    }

    private final CopySurvivors copySurvivors = new CopySurvivors();

    /**
     * Iterate over all methods code caches one last time, fixing direct calls to moved code.
     */
    private void fixCallSitesForMovedCode() {
        if (logging()) {
            codeEvictionLogger.logMove_Progress("fixing call sites ...\nmoved code ...");
        }

        timerStart();
        baselineFixCalls.fixed = 0;
        CodeManager.runtimeBaselineCodeRegion.doNewTargetMethods(baselineFixCalls);
        nCallBaseline = baselineFixCalls.fixed;
        tFixCallsBaseline = timerEnd();

        if (logging()) {
            codeEvictionLogger.logMove_Progress("optimised code ...");
        }

        timerStart();
        optFixCalls.fixed = 0;
        CodeManager.runtimeOptCodeRegion.doAllTargetMethods(optFixCalls);
        nCallOpt = optFixCalls.fixed;
        tFixCallsOpt = timerEnd();

        if (logging()) {
            codeEvictionLogger.logMove_Progress("boot code ...");
        }

        timerStart();
        optFixCalls.fixed = 0;
        if (CodeManager.bootToBaselineSize() > 0) {
            CodeManager.bootToBaselineDo(optFixCalls);
        }
        nCallBoot = optFixCalls.fixed;
        tFixCallsBoot = timerEnd();

        if (logging()) {
            codeEvictionLogger.logMove_Progress("fixing done!");
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

    /*
     *  Everything below here is related to logging the behavior of the algorithm, or dumping the state of
     *  VM that is pertinent to the algorithm.
     */

    private static void stackDump(Pointer ip, Pointer sp, Pointer fp) {
        // We have no direct control over this (tracing) output, so we control via the operation enabling.
        if (CodeEvictionLogger.stackDumpEnabled()) {
            Throw.stackDump("dump after patching", ip, sp, fp);
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
     * Dump all code addresses from stacks (return addresses), vtables/itables/targetStates, and direct calls. This
     * generates a great deal of output and we currently do not store it in the {@link VMLog}. However, it is enabled
     * through the logging interface.
     */
    private void dumpCodeAddresses(String when) {
        if (CodeEvictionLogger.dumpEnabled()) {
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

    /**
     * Start logging upon the n-th eviction cycle.
     */
    private static int LogStartEviction;

    static {
        VMOptions.addFieldOption("-XX:", "LogStartEviction", CodeEviction.class,
            "Start logging upon the n-th code eviction cycle; all cycles before that are silent.",
            MaxineVM.Phase.STARTING);
    }

    /**
     * Checks whether the logger is enabled and if we have reached the evictionCount to start logging.
     * @return
     */
    protected static boolean logging() {
        return codeEvictionLogger.enabled() && evictionCount >= LogStartEviction;
    }

    // The following methods are essentially convenience methods that avoid cluttering the algorithm
    // with checks that logging is enabled, although some do a small amount of additional setup.

    @NEVER_INLINE
    private void logTimingResults() {
        if (logging()) {
            tTotal =
                tMarking + tMarkProtected +
                tInvalidateCallsBaseline + tInvalidateCallsOpt + tInvalidateCallsBoot + tInvalidateTables +
                tCompact + tPatchStacks + tFixCallsBaseline + tFixCallsOpt + tFixCallsBoot;
            codeEvictionLogger.logStats_TimingResults(this);
        }
    }

    private void logStaleMethod(TargetMethod tm) {
        if (logging()) {
            codeEvictionLogger.logDetails_StaleMethod(nStale, tm);
        }
    }

    private void logMethodPatch(final TargetMethod tm, final boolean patch) {
        if (logging()) {
            codeEvictionLogger.logMove_MethodPatch(tm, patch);
        }
    }

    @NEVER_INLINE
    private void logCalleeReturnAddress(StackFrameCursor callee) {
        if (logging() && callee != null && callee.targetMethod() != null) {
            final Pointer rap = callee.targetMethod().returnAddressPointer(callee);
            final Pointer ret = rap.readWord(0).asPointer();
            codeEvictionLogger.logMove_CalleeReturnAddress(ret);
        }
    }

    private void logReturnAddressPatch(final TargetMethod tm, final CodePointer calleeRet, final CodePointer newCalleeRet) {
        if (logging()) {
            codeEvictionLogger.logMove_ReturnAddressPatch(tm, calleeRet, newCalleeRet);
        }
    }

    private void logCodeMotion(TargetMethod tm, final Pointer from, final Pointer to, final Size size) {
        if (logging()) {
            codeEvictionLogger.logMove_CodeMotion(tm, from, to, size);
        }
    }

    private void logNotCopying(TargetMethod tm) {
        if (logging()) {
            codeEvictionLogger.logMove_NotCopying(tm);
        }
    }

    private void logFixCall(TargetMethod tm, int i, CodePointer callTarget) {
        if (logging()) {
            codeEvictionLogger.logMove_FixCall(tm, i, callTarget);
        }
    }

    private void logStatistics() {
        if (logging()) {
            codeEvictionLogger.logStats_Statistics(this);
        }
    }

    private void logPatchDetails() {
        if (logging()) {
            codeEvictionLogger.logDetails_PatchDetails(this);
        }
    }

    private void logCodeCacheBoundaries(final SemiSpaceCodeRegion cr) {
        if (logging()) {
            codeEvictionLogger.logMove_CodeCacheBoundaries(cr);
        }
    }

    private void logDirectCallReset(TargetMethod tm, int i, final TargetMethod callee) {
        if (logging()) {
            codeEvictionLogger.logDetails_DirectCallReset(tm, i, callee);
        }
    }

    private void logDirectCallNumbers() {
        if (logging()) {
            codeEvictionLogger.logStats_DirectCallNumbers(this);
        }
    }

    private void logDispatchTableReset(char tableKind, Hub hub, int index) {
        if (logging()) {
            codeEvictionLogger.logDetails_DispatchTableReset(tableKind, hub, index);
        }
    }

    private void logThread(VmThread vmThread) {
        if (logging()) {
            Log.print("THREAD ");
            Log.println(vmThread.getName());
        }
    }

    private void logFixCallForMovedCode(TargetMethod tm, Offset d) {
        if (logging()) {
            codeEvictionLogger.logMove_FixCallForMovedCode(tm, d);
        }
    }

    private void logDirectCallInfo(int i, int callPos, CodePointer target, CodePointer itarget) {
        if (logging()) {
            codeEvictionLogger.logMove_DirectCallInfo(i, callPos, target, itarget);
        }
    }

    private void logToMoved(TargetMethod callee, Address oldCalleeStart, Address newCalleeStart, Address epoffset, CodePointer newTarget) {
        if (logging()) {
            codeEvictionLogger.logMove_ToMoved(callee, oldCalleeStart, newCalleeStart, epoffset, newTarget);
        }
    }

    private void logToUnmoved(CodePointer iTarget) {
        if (logging()) {
            codeEvictionLogger.logMove_ToUnmoved(iTarget);
        }
    }

    private void logOptMethod(TargetMethod tm) {
        if (logging()) {
            codeEvictionLogger.logMove_OptMethod(tm);
        }
    }

    private void logFixed() {
        if (logging()) {
            codeEvictionLogger.logStats_Fixed(this);
        }
    }

    private void logMark(String s, TargetMethod tm) {
        if (logging()) {
            codeEvictionLogger.logDetails_Mark(s, tm);
        }
    }

    private void logMarkLevel(String s, TargetMethod tm, int level) {
        if (logging()) {
            codeEvictionLogger.logDetails_MarkLevel(s, tm, level);
        }
    }

    private void logVisitRefMapBits(StackFrameCursor cursor, Pointer slotPointer, int refMap, int numBits) {
        if (logging()) {
            codeEvictionLogger.logDetails_VisitRefMapBits(cursor, slotPointer, refMap, numBits);
        }
    }

    private void logRelocateCodePointer(int bitIndex, CodePointer from, CodePointer to, TargetMethod tm) {
        if (logging()) {
            codeEvictionLogger.logDetails_RelocateCodePointer(bitIndex, from, to, tm);
        }
    }

    /**
     * The interface to the {@link CodeEviction logger}. This evolved from a hand-crafted system using {@link Log}. It
     * has several distinct facets that used to be controlled by an integer level, but are now controlled by pattern
     * matching on a prefix to the the operation name. To make this clear, the prefix is separated by a '_' character
     * from the operation name proper.
     *
     * An alternate design would be separate loggers for each of the facets.
     *
     * Dumping of the VM state pre/post a code eviction and generating a thread stack trace after patching
     * do not place data in the {@link VMLog}. However, they are defined here as operations so that they
     * can be enabled/disabled selectively using {@code -XX:LogCodeEvictionInclude/Exclude}. N.B., when
     * enabled they will generate {@link Log} output regardless of whether {@code TraceCodeEviction} is set.
     *
     */
    @HOSTED_ONLY
    @VMLoggerInterface
    private interface CodeEvictionLoggerInterface {
        // Control
        void run(
            @VMLogParam(name = "mode") String mode,
            @VMLogParam(name = "evictionCount") int evictionCount,
            @VMLogParam(name = "callingThread") VmThread callingThread);

        // Statistics. The data is all stored in the CodeEviction instance, so for now we just log that.
        void stats_DirectCallNumbers(@VMLogParam(name = "codeEviction") CodeEviction codeEviction);
        void stats_Fixed(@VMLogParam(name = "codeEviction") CodeEviction codeEviction);
        void stats_Statistics(@VMLogParam(name = "codeEviction") CodeEviction codeEviction);
        void stats_TimingResults(@VMLogParam(name = "codeEviction") CodeEviction codeEviction);
        void stats_Surviving(
            @VMLogParam(name = "lastSurvivorSize") int lastSurvivorSize,
            @VMLogParam(name = "largestSurvivorSize") int largestSurvivorSize);

        // Details
        void details_DirectCallReset(
            @VMLogParam(name = "tm") TargetMethod tm,
            @VMLogParam(name = "i") int i,
            @VMLogParam(name = "callee") TargetMethod callee);

        void details_DispatchTableReset(
            @VMLogParam(name = "tableKind") char tableKind,
            @VMLogParam(name = "hub") Hub hub,
            @VMLogParam(name = "index") int index);

        void details_Mark(@VMLogParam(name = "s") String s, @VMLogParam(name = "tm") TargetMethod tm);

        void details_MarkLevel(
            @VMLogParam(name = "s") String s,
            @VMLogParam(name = "tm") TargetMethod tm,
            @VMLogParam(name = "level") int level);

        void details_PatchDetails(@VMLogParam(name = "codeEviction") CodeEviction codeEviction);

        void details_RelocateCodePointer(
            @VMLogParam(name = "bitIndex") int bitIndex,
            @VMLogParam(name = "from") CodePointer from,
            @VMLogParam(name = "to") CodePointer to,
            @VMLogParam(name = "tm") TargetMethod tm);

        void details_StaleMethod(@VMLogParam(name = "nStale") int nStale, @VMLogParam(name = "tm") TargetMethod tm);

        void details_VisitRefMapBits(
            @VMLogParam(name = "cursor") StackFrameCursor cursor,
            @VMLogParam(name = "slotPointer") Pointer slotPointer,
            @VMLogParam(name = "refMap") int refMap,
            @VMLogParam(name = "numBits") int numBits);

        void details_PatchType(@VMLogParam(name = "type") String type);

        // Threads/Code Motion

        void move_Progress(@VMLogParam(name = "s") String s);

        void move_CalleeReturnAddress(@VMLogParam(name = "ret") Pointer ret);

        void move_ReturnAddressPatch(
            @VMLogParam(name = "tm") TargetMethod tm,
            @VMLogParam(name = "calleeRet") CodePointer calleeRet,
            @VMLogParam(name = "newCalleeRet") CodePointer newCalleeRet);

        void move_CodeMotion(
            @VMLogParam(name = "tm") TargetMethod tm,
            @VMLogParam(name = "from") Pointer from,
            @VMLogParam(name = "to") Pointer to,
            @VMLogParam(name = "size") Size size);

        void move_NotCopying(@VMLogParam(name = "tm") TargetMethod tm);

        void move_FixCall(
            @VMLogParam(name = "tm") TargetMethod tm,
            @VMLogParam(name = "i") int i,
            @VMLogParam(name = "callTarget") CodePointer callTarget);

        void move_FixCallForMovedCode(
            @VMLogParam(name = "tm") TargetMethod tm,
            @VMLogParam(name = "d") Offset d);

        void move_CodeCacheBoundaries(@VMLogParam(name = "cr") SemiSpaceCodeRegion cr);

        void move_DirectCallInfo(
            @VMLogParam(name = "i") int i,
            @VMLogParam(name = "callPos") int callPos,
            @VMLogParam(name = "target") CodePointer target,
            @VMLogParam(name = "itarget") CodePointer itarget);

        void move_ToMoved(
            @VMLogParam(name = "callee") TargetMethod callee,
            @VMLogParam(name = "oldCalleeStart") Address oldCalleeStart,
            @VMLogParam(name = "newCalleeStart") Address newCalleeStart,
            @VMLogParam(name = "epoffset") Address epoffset,
            @VMLogParam(name = "newTarget") CodePointer newTarget);

        void move_ToUnmoved(@VMLogParam(name = "iTarget") CodePointer itarget);

        void move_OptMethod(@VMLogParam(name = "tm") TargetMethod tm);

        void move_MethodPatch(
            @VMLogParam(name = "tm") TargetMethod tm,
            @VMLogParam(name = "patch") boolean patch);

        void bootToBaseline(@VMLogParam(name = "tm") TargetMethod tm);

        /**
         * This is a placeholder to control this operation which does not go via {@link VMLog}.
         */
        void stackDump();

        // Dumping.

        /**
         * This is a placeholder that serves simply to enable dumping, as the dump info is not stored in the {@link VMLog}.
         */
        void dump();
    }

    static final CodeEvictionLogger codeEvictionLogger = new CodeEvictionLogger();

    static class CodeEvictionLogger extends CodeEvictionLoggerAuto {

        protected CodeEvictionLogger() {
            super("CodeEviction", "Log code eviction after baseline code cache contention. Operation prefixes control logging:, " +
                            "Run = log each code eviction run (enabled by default)" +
                            "Stat_.* = statistics (count evicted/surviving bytes and methods), " +
                            "Details_.* = give detailed information about what methods and dispatch entries are treated, " +
                            "Move_.* = print details about threads and code motion, " +
                            "Dump = give full dumps of all code addresses before and after eviction");
        }

        static boolean dumpEnabled() {
            return codeEvictionLogger.opEnabled(Operation.Dump.ordinal());
        }

        static boolean stackDumpEnabled() {
            return codeEvictionLogger.opEnabled(Operation.StackDump.ordinal());
        }

        @Override
        public void checkOptions() {
            super.checkOptions();
            // Always enable the Run operation if we are doing any logging.
            if (enabled()) {
                setOperationState(Operation.Run.ordinal(), true);
            }
        }

        // The implementations of the trace* methods (for non-Inspector) usage.

        @Override
        protected void traceRun(String mode, int evictionCount, VmThread callingThread) {
            Log.print(mode);
            Log.print(" code eviction run #");
            Log.print(evictionCount);
            Log.print(" triggered by ");
            Log.printThread(callingThread, true);
        }

        @Override
        protected void traceStats_DirectCallNumbers(CodeEviction codeEviction) {
            Log.print("patched direct calls: ");
            Log.print(codeEviction.nCallBaseline);
            Log.print(" baseline, ");
            Log.print(codeEviction.nCallOpt);
            Log.print(" opt, ");
            Log.print(codeEviction.nCallBoot);
            Log.print(" boot; looked at ");
            Log.print(codeEviction.nBaseDirect);
            Log.print(" sites in ");
            Log.print(codeEviction.nBaseMeth);
            Log.print(" baseline methods, ");
            Log.print(codeEviction.nOptDirect);
            Log.print(" sites in ");
            Log.print(codeEviction.nOptMeth);
            Log.print(" opt methods, and ");
            Log.print(codeEviction.nBootDirect);
            Log.print(" sites in ");
            Log.print(codeEviction.nBootMeth);
            Log.println(" boot image methods");
        }

        @Override
        protected void traceStats_Fixed(CodeEviction codeEviction) {
            Log.print("fixed direct calls: ");
            Log.print(codeEviction.nCallBaseline);
            Log.print(" baseline, ");
            Log.print(codeEviction.nCallOpt);
            Log.println(" opt");
        }

        @Override
        protected void traceStats_Statistics(CodeEviction codeEviction) {
            Log.print("code eviction: ");
            Log.print(codeEviction.nStale);
            Log.print(" stale methods (");
            Log.print(codeEviction.nStaleBytes / 1024);
            Log.print(" kB); ");
            Log.print(codeEviction.nSurvivors);
            Log.print(" survivors (");
            Log.print(codeEviction.nSurvivingBytes / 1024);
            Log.print(" kB) - ");
            final int totalMethods = codeEviction.nSurvivors + codeEviction.nStale;
            Log.print(codeEviction.nSurvivors * 100 / totalMethods);
            Log.println(" % of methods survived");
        }

        @Override
        protected void traceStats_TimingResults(CodeEviction codeEviction) {
            Log.println("timing summary");
            Log.print("total ");
            Log.print(codeEviction.tTotal / 1000000.0);
            Log.println(" ms");
            long tTotal = codeEviction.tTotal;
            printTime("  Phase 1 - mark                       ", codeEviction.tMarking, tTotal);
            printTime("            mark protected methods     ", codeEviction.tMarkProtected, tTotal);
            printTime("            invalidate baseline calls  ", codeEviction.tInvalidateCallsBaseline, tTotal);
            printTime("            invalicate opt calls       ", codeEviction.tInvalidateCallsOpt, tTotal);
            printTime("            invalidate boot calls      ", codeEviction.tInvalidateCallsBoot, tTotal);
            printTime("            invalidate dispatch tables ", codeEviction.tInvalidateTables, tTotal);
            printTime("  Phase 2 - compact                    ", codeEviction.tCompact, tTotal);
            printTime("            patch stacks               ", codeEviction.tPatchStacks, tTotal);
            printTime("            fix baseline calls         ", codeEviction.tFixCallsBaseline, tTotal);
            printTime("            fix opt calls              ", codeEviction.tFixCallsOpt, tTotal);
            printTime("            fix boot calls             ", codeEviction.tFixCallsBoot, tTotal);
        }

        private static void printTime(String s, long t, long tTotal) {
            Log.print(s);
            Log.print(t);
            Log.print(" ns (");
            Log.print(t / 1000000.0);
            Log.print(" ms, ");
            Log.print(t * 100.0 / tTotal);
            Log.println(" %)");
        }

        @Override
        protected void traceDetails_DirectCallReset(TargetMethod tm, int i, TargetMethod callee) {
            Log.print("DIRECT CALL ");
            Log.print(tm);
            Log.print('@');
            Log.print(i);
            Log.print(" -> ");
            Log.println(callee);
        }

        @Override
        protected void traceDetails_DispatchTableReset(char tableKind, Hub hub, int index) {
            Log.print("  ");
            Log.print(tableKind);
            Log.print("TABLE ");
            Log.print(hub);
            Log.print('@');
            Log.println(index);
        }

        @Override
        protected void traceDetails_Mark(String s, TargetMethod tm) {
            Log.print("MARKING ");
            Log.print(s);
            Log.print(' ');
            Log.println(tm);
        }

        @Override
        protected void traceDetails_PatchDetails(CodeEviction codeEviction) {
            Log.print("dispatch table / target state summary: patched ");
            Log.print(codeEviction.nVT);
            Log.print(" vtable, ");
            Log.print(codeEviction.nIT);
            Log.print(" itable entries; ");
            Log.print(codeEviction.nNonvirtual);
            Log.print(" nonvirtual (leaves), ");
            Log.print(codeEviction.nStatic);
            Log.println(" static methods");
        }

        @Override
        protected void traceDetails_PatchType(String type) {
            Log.println(type);
        }

        @Override
        protected void traceDetails_RelocateCodePointer(int bitIndex, CodePointer from, CodePointer to, TargetMethod tm) {
            Log.print("   bit ");
            Log.print(bitIndex);
            Log.print(" from ");
            Log.print(from);
            Log.print(" to ");
            Log.print(to);
            Log.print(": ");
            Log.println(tm);
        }

        @Override
        protected void traceDetails_StaleMethod(int nStale, TargetMethod tm) {
            Log.print(nStale);
            Log.print(". ");
            Log.print(tm);
            Log.print(" - invocations: ");
            Log.println(MethodInstrumentation.initialEntryCount - tm.profile().entryCount);
        }

        @Override
        protected void traceDetails_VisitRefMapBits(StackFrameCursor cursor, Pointer slotPointer, int refMap, int numBits) {
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

        @Override
        protected void traceDetails_MarkLevel(String s, TargetMethod tm, int level) {
            Log.print("MARKING (level ");
            Log.print(CodeEvictionProtectCalleeDepth - level + 1);
            Log.print(')');
            Log.print(s);
            Log.print(' ');
            Log.println(tm);
        }

        @Override
        protected void traceMove_CalleeReturnAddress(Pointer ret) {
            Log.print("    \\---> callee return address ");
            Log.println(ret);
        }

        @Override
        protected void traceMove_CodeCacheBoundaries(SemiSpaceCodeRegion cr) {
            Log.print("flipped spaces. from: ");
            Log.print(cr.fromSpace);
            Log.print("..");
            Log.print(cr.fromSpace.plus(cr.spaceSize).minus(1));
            Log.print(", to: ");
            Log.print(cr.toSpace);
            Log.print("..");
            Log.println(cr.toSpace.plus(cr.spaceSize).minus(1));
        }

        @Override
        protected void traceMove_CodeMotion(TargetMethod tm, Pointer from, Pointer to, Size size) {
            Log.print("copying ");
            Log.print(tm);
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

        @Override
        protected void traceMove_DirectCallInfo(int i, int callPos, CodePointer target, CodePointer itarget) {
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

        @Override
        protected void traceMove_FixCall(TargetMethod tm, int i, CodePointer callTarget) {
            final TargetMethod callee = callTarget.toTargetMethod();
            Log.print("direct call ");
            Log.print(tm);
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

        @Override
        protected void traceMove_FixCallForMovedCode(TargetMethod tm, Offset d) {
            Log.print("fixing calls for moved method ");
            Log.println(tm);
            Log.print("  moved by ");
            Log.println(d.toInt());
        }

        @Override
        protected void traceMove_NotCopying(TargetMethod tm) {
            Log.print("NOT copying ");
            Log.print(tm);
            Log.print(" [");
            Log.print(tm.start());
            Log.print("..");
            Log.print(tm.start().plus(tm.size()).minus(1));
            Log.println(']');
        }

        @Override
        protected void traceMove_OptMethod(TargetMethod tm) {
            Log.print("fixing calls for opt method ");
            Log.println(tm);
        }

        @Override
        protected void traceMove_ReturnAddressPatch(TargetMethod tm, CodePointer calleeRet, CodePointer newCalleeRet) {
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

        @Override
        protected void traceMove_ToMoved(TargetMethod callee, Address oldCalleeStart, Address newCalleeStart, Address epoffset, CodePointer newTarget) {
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

        @Override
        protected void traceMove_ToUnmoved(CodePointer iTarget) {
            Log.print("    TO UNMOVED ");
            Log.println(iTarget.toTargetMethod());
        }

        @Override
        protected void traceMove_Progress(String s) {
            Log.println(s);
        }

        @Override
        protected void traceMove_MethodPatch(TargetMethod tm, boolean patch) {
            if (patch) {
                Log.print("  P ");
            } else {
                Log.print("    ");
            }
            Log.println(tm);
        }

        @Override
        protected void traceDump() {
            // The tracing associated with a dump is hand-coded and does not go via the VMLog
        }

        @Override
        protected void traceBootToBaseline(TargetMethod tm) {
            Log.print("boot->baseline ");
            Log.println(tm);
        }

        @Override
        protected void traceStats_Surviving(int lastSurvivorSize, int largestSurvivorSize) {
            Log.print("amount surviving code eviction: ");
            Log.print(lastSurvivorSize);
            Log.print(" bytes, largest so far: ");
            Log.print(largestSurvivorSize);
            Log.println(" bytes");
        }

        @Override
        protected void traceStackDump() {
            // The tracing associated with a dump is hand-coded and does not go via the VMLog
        }

    }

// START GENERATED CODE
    private static abstract class CodeEvictionLoggerAuto extends com.sun.max.vm.log.VMLogger {
        public enum Operation {
            BootToBaseline, Details_DirectCallReset, Details_DispatchTableReset,
            Details_Mark, Details_MarkLevel, Details_PatchDetails, Details_PatchType,
            Details_RelocateCodePointer, Details_StaleMethod, Details_VisitRefMapBits, Dump,
            Move_CalleeReturnAddress, Move_CodeCacheBoundaries, Move_CodeMotion, Move_DirectCallInfo,
            Move_FixCall, Move_FixCallForMovedCode, Move_MethodPatch, Move_NotCopying,
            Move_OptMethod, Move_Progress, Move_ReturnAddressPatch, Move_ToMoved,
            Move_ToUnmoved, Run, StackDump, Stats_DirectCallNumbers,
            Stats_Fixed, Stats_Statistics, Stats_Surviving, Stats_TimingResults;

            @SuppressWarnings("hiding")
            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = new int[] {0x1, 0x5, 0x2, 0x3, 0x3, 0x1, 0x1, 0xe, 0x2, 0x1, 0x0, 0x0, 0x1, 0x1,
            0xc, 0x5, 0x1, 0x1, 0x1, 0x1, 0x1, 0x7, 0x11, 0x1, 0x1, 0x0, 0x1, 0x1,
            0x1, 0x0, 0x1};

        protected CodeEvictionLoggerAuto(String name, String optionDescription) {
            super(name, Operation.VALUES.length, optionDescription, REFMAPS);
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @INLINE
        public final void logBootToBaseline(TargetMethod tm) {
            log(Operation.BootToBaseline.ordinal(), objectArg(tm));
        }
        protected abstract void traceBootToBaseline(TargetMethod tm);

        @INLINE
        public final void logDetails_DirectCallReset(TargetMethod tm, int i, TargetMethod callee) {
            log(Operation.Details_DirectCallReset.ordinal(), objectArg(tm), intArg(i), objectArg(callee));
        }
        protected abstract void traceDetails_DirectCallReset(TargetMethod tm, int i, TargetMethod callee);

        @INLINE
        public final void logDetails_DispatchTableReset(char tableKind, Hub hub, int index) {
            log(Operation.Details_DispatchTableReset.ordinal(), charArg(tableKind), classActorArg(hub.classActor), intArg(index));
        }
        protected abstract void traceDetails_DispatchTableReset(char tableKind, Hub hub, int index);

        @INLINE
        public final void logDetails_Mark(String s, TargetMethod tm) {
            log(Operation.Details_Mark.ordinal(), objectArg(s), objectArg(tm));
        }
        protected abstract void traceDetails_Mark(String s, TargetMethod tm);

        @INLINE
        public final void logDetails_MarkLevel(String s, TargetMethod tm, int level) {
            log(Operation.Details_MarkLevel.ordinal(), objectArg(s), objectArg(tm), intArg(level));
        }
        protected abstract void traceDetails_MarkLevel(String s, TargetMethod tm, int level);

        @INLINE
        public final void logDetails_PatchDetails(CodeEviction codeEviction) {
            log(Operation.Details_PatchDetails.ordinal(), objectArg(codeEviction));
        }
        protected abstract void traceDetails_PatchDetails(CodeEviction codeEviction);

        @INLINE
        public final void logDetails_PatchType(String type) {
            log(Operation.Details_PatchType.ordinal(), objectArg(type));
        }
        protected abstract void traceDetails_PatchType(String type);

        @INLINE
        public final void logDetails_RelocateCodePointer(int bitIndex, CodePointer from, CodePointer to, TargetMethod tm) {
            log(Operation.Details_RelocateCodePointer.ordinal(), intArg(bitIndex), codePointerArg(from), codePointerArg(to), objectArg(tm));
        }
        protected abstract void traceDetails_RelocateCodePointer(int bitIndex, CodePointer from, CodePointer to, TargetMethod tm);

        @INLINE
        public final void logDetails_StaleMethod(int nStale, TargetMethod tm) {
            log(Operation.Details_StaleMethod.ordinal(), intArg(nStale), objectArg(tm));
        }
        protected abstract void traceDetails_StaleMethod(int nStale, TargetMethod tm);

        @INLINE
        public final void logDetails_VisitRefMapBits(StackFrameCursor cursor, Pointer slotPointer, int refMap, int numBits) {
            log(Operation.Details_VisitRefMapBits.ordinal(), objectArg(cursor), slotPointer, intArg(refMap), intArg(numBits));
        }
        protected abstract void traceDetails_VisitRefMapBits(StackFrameCursor cursor, Pointer slotPointer, int refMap, int numBits);

        @INLINE
        public final void logDump() {
            log(Operation.Dump.ordinal());
        }
        protected abstract void traceDump();

        @INLINE
        public final void logMove_CalleeReturnAddress(Pointer ret) {
            log(Operation.Move_CalleeReturnAddress.ordinal(), ret);
        }
        protected abstract void traceMove_CalleeReturnAddress(Pointer ret);

        @INLINE
        public final void logMove_CodeCacheBoundaries(SemiSpaceCodeRegion cr) {
            log(Operation.Move_CodeCacheBoundaries.ordinal(), objectArg(cr));
        }
        protected abstract void traceMove_CodeCacheBoundaries(SemiSpaceCodeRegion cr);

        @INLINE
        public final void logMove_CodeMotion(TargetMethod tm, Pointer from, Pointer to, Size size) {
            log(Operation.Move_CodeMotion.ordinal(), objectArg(tm), from, to, size);
        }
        protected abstract void traceMove_CodeMotion(TargetMethod tm, Pointer from, Pointer to, Size size);

        @INLINE
        public final void logMove_DirectCallInfo(int i, int callPos, CodePointer target, CodePointer itarget) {
            log(Operation.Move_DirectCallInfo.ordinal(), intArg(i), intArg(callPos), codePointerArg(target), codePointerArg(itarget));
        }
        protected abstract void traceMove_DirectCallInfo(int i, int callPos, CodePointer target, CodePointer itarget);

        @INLINE
        public final void logMove_FixCall(TargetMethod tm, int i, CodePointer callTarget) {
            log(Operation.Move_FixCall.ordinal(), objectArg(tm), intArg(i), codePointerArg(callTarget));
        }
        protected abstract void traceMove_FixCall(TargetMethod tm, int i, CodePointer callTarget);

        @INLINE
        public final void logMove_FixCallForMovedCode(TargetMethod tm, Offset d) {
            log(Operation.Move_FixCallForMovedCode.ordinal(), objectArg(tm), d);
        }
        protected abstract void traceMove_FixCallForMovedCode(TargetMethod tm, Offset d);

        @INLINE
        public final void logMove_MethodPatch(TargetMethod tm, boolean patch) {
            log(Operation.Move_MethodPatch.ordinal(), objectArg(tm), booleanArg(patch));
        }
        protected abstract void traceMove_MethodPatch(TargetMethod tm, boolean patch);

        @INLINE
        public final void logMove_NotCopying(TargetMethod tm) {
            log(Operation.Move_NotCopying.ordinal(), objectArg(tm));
        }
        protected abstract void traceMove_NotCopying(TargetMethod tm);

        @INLINE
        public final void logMove_OptMethod(TargetMethod tm) {
            log(Operation.Move_OptMethod.ordinal(), objectArg(tm));
        }
        protected abstract void traceMove_OptMethod(TargetMethod tm);

        @INLINE
        public final void logMove_Progress(String s) {
            log(Operation.Move_Progress.ordinal(), objectArg(s));
        }
        protected abstract void traceMove_Progress(String s);

        @INLINE
        public final void logMove_ReturnAddressPatch(TargetMethod tm, CodePointer calleeRet, CodePointer newCalleeRet) {
            log(Operation.Move_ReturnAddressPatch.ordinal(), objectArg(tm), codePointerArg(calleeRet), codePointerArg(newCalleeRet));
        }
        protected abstract void traceMove_ReturnAddressPatch(TargetMethod tm, CodePointer calleeRet, CodePointer newCalleeRet);

        @INLINE
        public final void logMove_ToMoved(TargetMethod callee, Address oldCalleeStart, Address newCalleeStart, Address epoffset, CodePointer newTarget) {
            log(Operation.Move_ToMoved.ordinal(), objectArg(callee), oldCalleeStart, newCalleeStart, epoffset, codePointerArg(newTarget));
        }
        protected abstract void traceMove_ToMoved(TargetMethod callee, Address oldCalleeStart, Address newCalleeStart, Address epoffset, CodePointer newTarget);

        @INLINE
        public final void logMove_ToUnmoved(CodePointer iTarget) {
            log(Operation.Move_ToUnmoved.ordinal(), codePointerArg(iTarget));
        }
        protected abstract void traceMove_ToUnmoved(CodePointer iTarget);

        @INLINE
        public final void logRun(String mode, int evictionCount, VmThread callingThread) {
            log(Operation.Run.ordinal(), objectArg(mode), intArg(evictionCount), vmThreadArg(callingThread));
        }
        protected abstract void traceRun(String mode, int evictionCount, VmThread callingThread);

        @INLINE
        public final void logStackDump() {
            log(Operation.StackDump.ordinal());
        }
        protected abstract void traceStackDump();

        @INLINE
        public final void logStats_DirectCallNumbers(CodeEviction codeEviction) {
            log(Operation.Stats_DirectCallNumbers.ordinal(), objectArg(codeEviction));
        }
        protected abstract void traceStats_DirectCallNumbers(CodeEviction codeEviction);

        @INLINE
        public final void logStats_Fixed(CodeEviction codeEviction) {
            log(Operation.Stats_Fixed.ordinal(), objectArg(codeEviction));
        }
        protected abstract void traceStats_Fixed(CodeEviction codeEviction);

        @INLINE
        public final void logStats_Statistics(CodeEviction codeEviction) {
            log(Operation.Stats_Statistics.ordinal(), objectArg(codeEviction));
        }
        protected abstract void traceStats_Statistics(CodeEviction codeEviction);

        @INLINE
        public final void logStats_Surviving(int lastSurvivorSize, int largestSurvivorSize) {
            log(Operation.Stats_Surviving.ordinal(), intArg(lastSurvivorSize), intArg(largestSurvivorSize));
        }
        protected abstract void traceStats_Surviving(int lastSurvivorSize, int largestSurvivorSize);

        @INLINE
        public final void logStats_TimingResults(CodeEviction codeEviction) {
            log(Operation.Stats_TimingResults.ordinal(), objectArg(codeEviction));
        }
        protected abstract void traceStats_TimingResults(CodeEviction codeEviction);

        @Override
        protected void trace(Record r) {
            switch (r.getOperation()) {
                case 0: { //BootToBaseline
                    traceBootToBaseline(toTargetMethod(r, 1));
                    break;
                }
                case 1: { //Details_DirectCallReset
                    traceDetails_DirectCallReset(toTargetMethod(r, 1), toInt(r, 2), toTargetMethod(r, 3));
                    break;
                }
                case 2: { //Details_DispatchTableReset
                    traceDetails_DispatchTableReset(toChar(r, 1), toHub(r, 2), toInt(r, 3));
                    break;
                }
                case 3: { //Details_Mark
                    traceDetails_Mark(toString(r, 1), toTargetMethod(r, 2));
                    break;
                }
                case 4: { //Details_MarkLevel
                    traceDetails_MarkLevel(toString(r, 1), toTargetMethod(r, 2), toInt(r, 3));
                    break;
                }
                case 5: { //Details_PatchDetails
                    traceDetails_PatchDetails(toCodeEviction(r, 1));
                    break;
                }
                case 6: { //Details_PatchType
                    traceDetails_PatchType(toString(r, 1));
                    break;
                }
                case 7: { //Details_RelocateCodePointer
                    traceDetails_RelocateCodePointer(toInt(r, 1), toCodePointer(r, 2), toCodePointer(r, 3), toTargetMethod(r, 4));
                    break;
                }
                case 8: { //Details_StaleMethod
                    traceDetails_StaleMethod(toInt(r, 1), toTargetMethod(r, 2));
                    break;
                }
                case 9: { //Details_VisitRefMapBits
                    traceDetails_VisitRefMapBits(toStackFrameCursor(r, 1), toPointer(r, 2), toInt(r, 3), toInt(r, 4));
                    break;
                }
                case 10: { //Dump
                    traceDump();
                    break;
                }
                case 11: { //Move_CalleeReturnAddress
                    traceMove_CalleeReturnAddress(toPointer(r, 1));
                    break;
                }
                case 12: { //Move_CodeCacheBoundaries
                    traceMove_CodeCacheBoundaries(toSemiSpaceCodeRegion(r, 1));
                    break;
                }
                case 13: { //Move_CodeMotion
                    traceMove_CodeMotion(toTargetMethod(r, 1), toPointer(r, 2), toPointer(r, 3), toSize(r, 4));
                    break;
                }
                case 14: { //Move_DirectCallInfo
                    traceMove_DirectCallInfo(toInt(r, 1), toInt(r, 2), toCodePointer(r, 3), toCodePointer(r, 4));
                    break;
                }
                case 15: { //Move_FixCall
                    traceMove_FixCall(toTargetMethod(r, 1), toInt(r, 2), toCodePointer(r, 3));
                    break;
                }
                case 16: { //Move_FixCallForMovedCode
                    traceMove_FixCallForMovedCode(toTargetMethod(r, 1), toOffset(r, 2));
                    break;
                }
                case 17: { //Move_MethodPatch
                    traceMove_MethodPatch(toTargetMethod(r, 1), toBoolean(r, 2));
                    break;
                }
                case 18: { //Move_NotCopying
                    traceMove_NotCopying(toTargetMethod(r, 1));
                    break;
                }
                case 19: { //Move_OptMethod
                    traceMove_OptMethod(toTargetMethod(r, 1));
                    break;
                }
                case 20: { //Move_Progress
                    traceMove_Progress(toString(r, 1));
                    break;
                }
                case 21: { //Move_ReturnAddressPatch
                    traceMove_ReturnAddressPatch(toTargetMethod(r, 1), toCodePointer(r, 2), toCodePointer(r, 3));
                    break;
                }
                case 22: { //Move_ToMoved
                    traceMove_ToMoved(toTargetMethod(r, 1), toAddress(r, 2), toAddress(r, 3), toAddress(r, 4), toCodePointer(r, 5));
                    break;
                }
                case 23: { //Move_ToUnmoved
                    traceMove_ToUnmoved(toCodePointer(r, 1));
                    break;
                }
                case 24: { //Run
                    traceRun(toString(r, 1), toInt(r, 2), toVmThread(r, 3));
                    break;
                }
                case 25: { //StackDump
                    traceStackDump();
                    break;
                }
                case 26: { //Stats_DirectCallNumbers
                    traceStats_DirectCallNumbers(toCodeEviction(r, 1));
                    break;
                }
                case 27: { //Stats_Fixed
                    traceStats_Fixed(toCodeEviction(r, 1));
                    break;
                }
                case 28: { //Stats_Statistics
                    traceStats_Statistics(toCodeEviction(r, 1));
                    break;
                }
                case 29: { //Stats_Surviving
                    traceStats_Surviving(toInt(r, 1), toInt(r, 2));
                    break;
                }
                case 30: { //Stats_TimingResults
                    traceStats_TimingResults(toCodeEviction(r, 1));
                    break;
                }
            }
        }
        static CodeEviction toCodeEviction(Record r, int argNum) {
            if (MaxineVM.isHosted()) {
                return (CodeEviction) ObjectArg.getArg(r, argNum);
            } else {
                return asCodeEviction(toObject(r, argNum));
            }
        }
        @INTRINSIC(UNSAFE_CAST)
        private static native CodeEviction asCodeEviction(Object arg);

        static Hub toHub(Record r, int argNum) {
            if (MaxineVM.isHosted()) {
                return (Hub) ObjectArg.getArg(r, argNum);
            } else {
                return asHub(toObject(r, argNum));
            }
        }
        @INTRINSIC(UNSAFE_CAST)
        private static native Hub asHub(Object arg);

        static SemiSpaceCodeRegion toSemiSpaceCodeRegion(Record r, int argNum) {
            if (MaxineVM.isHosted()) {
                return (SemiSpaceCodeRegion) ObjectArg.getArg(r, argNum);
            } else {
                return asSemiSpaceCodeRegion(toObject(r, argNum));
            }
        }
        @INTRINSIC(UNSAFE_CAST)
        private static native SemiSpaceCodeRegion asSemiSpaceCodeRegion(Object arg);

        static StackFrameCursor toStackFrameCursor(Record r, int argNum) {
            if (MaxineVM.isHosted()) {
                return (StackFrameCursor) ObjectArg.getArg(r, argNum);
            } else {
                return asStackFrameCursor(toObject(r, argNum));
            }
        }
        @INTRINSIC(UNSAFE_CAST)
        private static native StackFrameCursor asStackFrameCursor(Object arg);

    }

// END GENERATED CODE

}
