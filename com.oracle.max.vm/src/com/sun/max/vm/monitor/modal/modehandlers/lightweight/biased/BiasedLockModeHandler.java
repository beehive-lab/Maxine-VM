/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.monitor.modal.modehandlers.lightweight.biased;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.monitor.modal.modehandlers.*;
import com.sun.max.vm.monitor.modal.modehandlers.AbstractModeHandler.ModeDelegate.DelegatedThreadHoldsMonitorResult;
import com.sun.max.vm.monitor.modal.modehandlers.AbstractModeHandler.MonitorSchemeEntry;
import com.sun.max.vm.monitor.modal.modehandlers.lightweight.biased.BiasedLockRevocationHeuristics.RevocationType;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Biased locking mode handler.
 *
 * Common case MonitorEnter hierarchy:
 *      1) Bias held by current thread >> blind inc to recursion count.
 *      2) Biasable lock with no bias owner >> Attempt to bias to current thread.
 *      3) Not a biased lock >> delegate to next locking mode.
 *      4) Bias held by current thread + recursion count overflow >> Remove bias and delegate to next locking mode.
 *      5) Biased to other thread >> Remove bias and delegate to next locking mode.
 *
 * Common case MonitorExit hierarchy:
 *      1) Bias held by current thread >> blind dec to recursion count.
 *      3) Not a biased lock >> delegate to next locking mode.
 *
 * Note that bias revocation is is not implemented directly by this class, rather it is requested from an
 * associated BiasRevocation object.
 *
 * TODO: For AMD64 and Sparc TSO we don't need any membars. PPC, IA32 and ARM will need some adding to the acquire and release logic.
 */
public abstract class BiasedLockModeHandler extends AbstractModeHandler implements MonitorSchemeEntry {

    // Should the fast-path MonitorExit check that the current thread is the
    // owner, or do we assume that it is by implication of block-structured locking?
    private static final boolean ASSUME_PERFECT_ENTRY_AND_EXIT_PAIRS = false;

    public static MonitorSchemeEntry asFastPath(boolean useBulkRevocation, ModeDelegate delegate) {
        if (useBulkRevocation) {
            return new BiasedLockModeHandler.FastPathWithEpoch(delegate);
        }
        return new BiasedLockModeHandler.FastPathNoEpoch(delegate);
    }

    protected BiasedLockModeHandler(ModeDelegate delegate) {
        super(delegate);
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
    }

    // Inspector support
    public static int decodeBiasOwnerThreadID(BiasedLockword64 biasedLockword) {
        if (biasedLockword.equals(biasedLockword.asAnonBiased())) {
            return -1;
        }
        return decodeLockwordThreadID(biasedLockword.getBiasOwnerID());
    }

    protected ModalLockword64 revokeBias(Object object) {
        final ModalLockword64 lockword = ModalLockword64.from(ObjectAccess.readMisc(object));
        if (BiasedLockword64.isBiasedLockword(lockword)) {
            final ModalLockword64 newLockword = delegate().prepareModalLockword(object, lockword);
            ObjectAccess.writeMisc(object, newLockword);
            return newLockword;
        }
        return lockword;
    }

    protected ModalLockword64 revokeWithoutSafepointing(Object object) {
        synchronized (VmThreadMap.THREAD_LOCK) {
            return revokeBias(object);
        }
    }

    class RevokeBiasOperation extends VmOperation {
        final Object object;
        ModalLockword64 newLockword;
        RevokeBiasOperation(VmThread thread, Object object) {
            super("RevokeBias", thread, Mode.Safepoint);
            this.object = object;
        }
        @Override
        protected void doIt() {
            newLockword = revokeBias(object);
        }
    }

    protected ModalLockword64 revokeWithOwnerSafepointed(final Object object, int vmThreadMapThreadID, BiasedLockword64 biasedLockword) {
        synchronized (VmThreadMap.THREAD_LOCK) {
            final VmThread biasOwnerThread = VmThreadMap.ACTIVE.getVmThreadForID(vmThreadMapThreadID);
            if (biasOwnerThread == null) {
                // The bias owner is terminated. No need to safepoint.
                // Lets try to reset the bias to anon.
                return ModalLockword64.from(ObjectAccess.compareAndSwapMisc(object, biasedLockword, biasedLockword.asAnonBiased()));
            }
            final Pointer tla = biasOwnerThread.tla();
            if (tla.isZero()) {
                // The bias holding thread is still starting up, so how can it own biases??
                FatalError.unexpected("Attempted to revoke bias for still initializing thread.");
            }

            RevokeBiasOperation operation = new RevokeBiasOperation(VmThread.fromTLA(tla), object);
            operation.submit();
            return operation.newLockword;
        }
    }

    public Word createMisc(Object object) {
        return BiasedLockword64.anonBiasedFromHashcode(monitorScheme().createHashCode(object));
    }

    public void afterGarbageCollection() {
        delegate().delegateAfterGarbageCollection();
    }

    public void beforeGarbageCollection() {
        delegate().delegateBeforeGarbageCollection();
    }

    public void monitorExit(Object object) {
        nullCheck(object);
        if (MaxineVM.isHosted()) {
            HostMonitor.exit(object);
            return;
        }
        if (ASSUME_PERFECT_ENTRY_AND_EXIT_PAIRS) {
            // We cannot have any safepoints (and hence any revocation) on the code path between here and the lockword store.
            final ModalLockword64 lockword = ModalLockword64.from(ObjectAccess.readMisc(object));
            if (BiasedLockword64.isBiasedLockword(lockword)) {
                // Blind fast path monitor exit.
                ObjectAccess.writeMisc(object, BiasedLockword64.from(lockword).decrementCount());
            } else {
                // Not a biased lock; delegate.
                delegate().delegateMonitorExit(object, lockword);
            }
        } else {
            final int lockwordThreadID = encodeCurrentThreadIDForLockword();
            // We cannot have any safepoints (and hence any revocation) on the code path between here and the lockword store.
            final ModalLockword64 lockword = ModalLockword64.from(ObjectAccess.readMisc(object));
            if (BiasedLockword64.isBiasedLockword(lockword)) {
                final BiasedLockword64 biasedLockword = BiasedLockword64.from(lockword);
                if (biasedLockword.getBiasOwnerID() != lockwordThreadID || biasedLockword.countUnderflow()) {
                    throw new IllegalMonitorStateException();
                }
                // Fast path monitor exit.
                ObjectAccess.writeMisc(object, biasedLockword.decrementCount());
            } else {
                // Not a biased lock; delegate.
                delegate().delegateMonitorExit(object, lockword);
            }
        }
    }

    public void monitorNotify(Object object, boolean all) {
        nullCheck(object);
        if (MaxineVM.isHosted()) {
            HostMonitor.notify(object);
            return;
        }
        final ModalLockword64 lockword = ModalLockword64.from(ObjectAccess.readMisc(object));
        if (BiasedLockword64.isBiasedLockword(lockword)) {
            final BiasedLockword64 biasedLockword = BiasedLockword64.from(lockword);
            if (biasedLockword.countUnderflow() || biasedLockword.getBiasOwnerID() != encodeCurrentThreadIDForLockword()) {
                throw new IllegalMonitorStateException();
            }
            // By biased lock semantics we have no threads waiting, so just return.
        } else {
            // Not a biased lock; delegate.
            delegate().delegateMonitorNotify(object, all, lockword);
        }
    }

    public void monitorWait(Object object, long timeout) throws InterruptedException {
        nullCheck(object);
        if (MaxineVM.isHosted()) {
            HostMonitor.wait(object, timeout);
            return;
        }
        ModalLockword64 lockword = ModalLockword64.from(ObjectAccess.readMisc(object));
        if (BiasedLockword64.isBiasedLockword(lockword)) {
            final BiasedLockword64 biasedLockword = BiasedLockword64.from(lockword);
            final int lockwordThreadID = encodeCurrentThreadIDForLockword();
            if (biasedLockword.countUnderflow() || biasedLockword.getBiasOwnerID() != lockwordThreadID) {
                throw new IllegalMonitorStateException();
            }
            // We can't have a wait queue, so move the lock to the next delegate mode and try to wait.
            // (We can revoke without suspending as the current thread owns the bias)
            if (Monitor.TraceMonitors) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("Nonsafepointed revoke for monitorWait: ");
                Log.println(object.getClass().getName());
                Log.unlock(lockDisabledSafepoints);
            }
            lockword = revokeWithoutSafepointing(object);
        }
        // Not a biased lock; delegate.
        delegate().delegateMonitorWait(object, timeout, lockword);
    }

    public boolean threadHoldsMonitor(Object object, VmThread thread) {
        nullCheck(object);
        final int lockwordThreadID = encodeCurrentThreadIDForLockword();
        final ModalLockword64 lockword = ModalLockword64.from(ObjectAccess.readMisc(object));
        if (BiasedLockword64.isBiasedLockword(lockword)) {
            final BiasedLockword64 biasedLockword = BiasedLockword64.from(lockword);
            return !biasedLockword.countUnderflow() && biasedLockword.getBiasOwnerID() == lockwordThreadID;
        }
        final DelegatedThreadHoldsMonitorResult result = delegate().delegateThreadHoldsMonitor(object, lockword, thread, lockwordThreadID);
        return result == DelegatedThreadHoldsMonitorResult.TRUE;
    }

    static final class FastPathNoEpoch extends BiasedLockModeHandler {

        FastPathNoEpoch(ModeDelegate delegate) {
            super(delegate);
        }

        public void monitorEnter(Object object) {
            nullCheck(object);
            if (MaxineVM.isHosted()) {
                HostMonitor.enter(object);
                return;
            }
            final int lockwordThreadID = encodeCurrentThreadIDForLockword();
            // We cannot have any safepoints (and hence any revocation) on the code path between here and the lockword store.
            final ModalLockword64 lockword = ModalLockword64.from(ObjectAccess.readMisc(object));
            if (BiasedLockword64.isBiasedLockword(lockword)) {
                final BiasedLockword64 biasedLockword = BiasedLockword64.from(lockword);
                if (biasedLockword.getBiasOwnerID() == lockwordThreadID && !biasedLockword.countOverflow()) {
                    ObjectAccess.writeMisc(object, biasedLockword.incrementCount());
                    return;
                }
            }
            slowPathMonitorEnter(object, lockword, lockwordThreadID);
        }

        private void slowPathMonitorEnter(Object object, ModalLockword64 lockword, int lockwordThreadID) {
            ModalLockword64 currentLockword = lockword;
            while (BiasedLockword64.isBiasedLockword(currentLockword)) {
                final BiasedLockword64 biasedLockword = BiasedLockword64.from(currentLockword);
                // Is the lock unbiased and biasable?
                if (biasedLockword.equals(biasedLockword.asAnonBiased())) {
                    // Try to get the bias
                    final BiasedLockword64 newBiasedLockword = biasedLockword.asBiasedAndLockedOnceBy(lockwordThreadID);
                    currentLockword = ModalLockword64.from(ObjectAccess.compareAndSwapMisc(object, biasedLockword, newBiasedLockword));
                    if (currentLockword.equals(biasedLockword)) {
                        // Current thread is now the bias owner
                        return;
                    }
                } else if (biasedLockword.getBiasOwnerID() == lockwordThreadID && biasedLockword.countOverflow()) {
                    // The current thread owns the lock so we can revoke and delegate
                    currentLockword = revokeWithoutSafepointing(object);
                } else {
                    // Another thread holds the bias - so revoke.
                    // Note the revoking thread has no special priviledges, we simply revoke and then
                    // drop into the next locking mode's monitor enter code.
                    if (Monitor.TraceMonitors) {
                        final boolean lockDisabledSafepoints = Log.lock();
                        Log.print("Safepointed revoke for monitorEnter: ");
                        Log.println(object.getClass().getName());
                        Log.unlock(lockDisabledSafepoints);
                    }
                    final int vmThreadMapThreadID = decodeLockwordThreadID(biasedLockword.getBiasOwnerID());
                    currentLockword = revokeWithOwnerSafepointed(object, vmThreadMapThreadID, biasedLockword);
                }
            }
            delegate().delegateMonitorEnter(object, currentLockword, lockwordThreadID);
        }

        public int makeHashCode(Object object) {
            nullCheck(object);
            if (MaxineVM.isHosted()) {
                return monitorScheme().createHashCode(object);
            }
            int newHashcode = 0;
            ModalLockword64 lockword = ModalLockword64.from(ObjectAccess.readMisc(object));
            while (BiasedLockword64.isBiasedLockword(lockword)) {
                final BiasedLockword64 biasedLockword = BiasedLockword64.from(lockword);
                final int hashcode = biasedLockword.getHashcode();
                if (hashcode != 0) {
                    return hashcode;
                }
                if (newHashcode == 0) {
                    newHashcode = monitorScheme().createHashCode(object);
                }
                // Do we own the bias?
                final int lockwordThreadID = encodeCurrentThreadIDForLockword();
                if (biasedLockword.getBiasOwnerID() == lockwordThreadID) {
                    // No safepoints until after we have the hashcode in place
                    ObjectAccess.writeMisc(object, biasedLockword.setHashcode(newHashcode));
                    return newHashcode;
                } else if (biasedLockword.equals(biasedLockword.asAnonBiased())) {
                    // If the biased lock is anon biased, then try setting a hashcode
                    lockword = ModalLockword64.from(ObjectAccess.compareAndSwapMisc(object, biasedLockword, biasedLockword.setHashcode(newHashcode)));
                    if (lockword.equals(biasedLockword)) {
                        return newHashcode;
                    }
                } else {
                    // We have to revoke to set the hashcode...
                    if (Monitor.TraceMonitors) {
                        final boolean lockDisabledSafepoints = Log.lock();
                        Log.print("Safepointed revoke for hashcode: ");
                        Log.println(object.getClass().getName());
                        Log.unlock(lockDisabledSafepoints);
                    }
                    final int vmThreadMapThreadID = decodeLockwordThreadID(biasedLockword.getBiasOwnerID());
                    lockword = revokeWithOwnerSafepointed(object, vmThreadMapThreadID, biasedLockword);
                }
            }
            // Not a biased lock; delegate.
            return delegate().delegateMakeHashcode(object, lockword);
        }
    }

    static final class FastPathWithEpoch extends BiasedLockModeHandler {

        FastPathWithEpoch(ModeDelegate delegate) {
            super(delegate);
        }

        public void monitorEnter(Object object) {
            nullCheck(object);
            if (MaxineVM.isHosted()) {
                HostMonitor.enter(object);
                return;
            }
            final int lockwordThreadID = encodeCurrentThreadIDForLockword();
            final BiasedLockEpoch64 classEpoch = ObjectAccess.readHub(object).biasedLockEpoch;
            // We cannot have any safepoints (and hence any revocation) on the code path between here and the lockword store.
            final ModalLockword64 lockword = ModalLockword64.from(ObjectAccess.readMisc(object));
            if (BiasedLockword64.isBiasedLockword(lockword)) {
                final BiasedLockword64 biasedLockword = BiasedLockword64.from(lockword);
                if (biasedLockword.getEpoch().equals(classEpoch) && biasedLockword.getBiasOwnerID() == lockwordThreadID && !biasedLockword.countOverflow()) {
                    ObjectAccess.writeMisc(object, biasedLockword.incrementCount());
                    return;
                }
            }
            slowPathMonitorEnter(object, lockword, lockwordThreadID);
        }

        private void slowPathMonitorEnter(Object object, ModalLockword64 lockword, int lockwordThreadID) {
            ModalLockword64 currentLockword = lockword;
            while (BiasedLockword64.isBiasedLockword(currentLockword)) {
                final BiasedLockword64 biasedLockword = BiasedLockword64.from(currentLockword);
                final BiasedLockEpoch64 classEpoch = ObjectAccess.readHub(object).biasedLockEpoch;
                if (classEpoch.isBulkRevocation()) {
                    // Objects of this class are no longer eligible for biased locking
                    if (biasedLockword.equals(biasedLockword.asAnonBiased())) {
                        // Object is not biased or locked, change the lockword to the next locking mode
                        final ModalLockword64 newLockword = delegate().prepareModalLockword(object, currentLockword);
                        currentLockword = ModalLockword64.from(ObjectAccess.compareAndSwapMisc(object, biasedLockword, newLockword));
                        if (!currentLockword.equals(biasedLockword)) {
                            delegate().cancelPreparedModalLockword(newLockword);
                        }
                        if (Monitor.TraceMonitors) {
                            final boolean lockDisabledSafepoints = Log.lock();
                            Log.print("New object bulk revocation: ");
                            Log.println(object.getClass().getName());
                            Log.unlock(lockDisabledSafepoints);
                        }
                    } else if (biasedLockword.getBiasOwnerID() == lockwordThreadID) {
                        // Object is biased and locked by the current thread. Revoke the bias.
                        currentLockword = revokeWithoutSafepointing(object);
                    } else {
                        // Object is biased to another thread. Revoke the bias.
                        final int vmThreadMapThreadID = decodeLockwordThreadID(biasedLockword.getBiasOwnerID());
                        currentLockword = revokeWithOwnerSafepointed(object, vmThreadMapThreadID, biasedLockword);
                    }
                } else if (biasedLockword.getBiasOwnerID() == lockwordThreadID && !biasedLockword.getEpoch().equals(classEpoch) &&
                           !biasedLockword.countUnderflow() && !biasedLockword.countOverflow()) {
                    // Object is biased to and locked by the current thread, but the object's class has been rebiased.
                    // We lock as normal. When the lock is released, the object will be rebiased.
                    ObjectAccess.writeMisc(object, biasedLockword.incrementCount());
                    return;
                } else if (biasedLockword.equals(biasedLockword.asAnonBiased()) || !biasedLockword.getEpoch().equals(classEpoch)) {
                    // Object is not biased or it's bias is not in the current epoch. Try to get the bias.
                    final BiasedLockword64 newBiasedLockword = biasedLockword.asBiasedAndLockedOnceBy(lockwordThreadID, classEpoch);
                    currentLockword = ModalLockword64.from(ObjectAccess.compareAndSwapMisc(object, biasedLockword, newBiasedLockword));
                    if (currentLockword.equals(biasedLockword)) {
                        // Current thread is now the bias owner
                        return;
                    }
                } else if (biasedLockword.getBiasOwnerID() == lockwordThreadID && biasedLockword.countOverflow()) {
                    // Overflow of the recursion count. The current thread owns the lock so we can revoke and delegate.
                    if (Monitor.TraceMonitors) {
                        final boolean lockDisabledSafepoints = Log.lock();
                        Log.print("Safepointed revoke for hashcode: ");
                        Log.println(object.getClass().getName());
                        Log.unlock(lockDisabledSafepoints);
                    }
                    currentLockword = revokeWithoutSafepointing(object);
                } else {
                    // Another thread holds the bias - so revoke.
                    currentLockword = performRevocation(object, biasedLockword);
                }
            }
            delegate().delegateMonitorEnter(object, currentLockword, lockwordThreadID);
        }

        public int makeHashCode(Object object) {
            // This is under construction!
            nullCheck(object);
            if (MaxineVM.isHosted()) {
                return monitorScheme().createHashCode(object);
            }
            int newHashcode = 0;
            ModalLockword64 lockword = ModalLockword64.from(ObjectAccess.readMisc(object));
            while (BiasedLockword64.isBiasedLockword(lockword)) {
                final BiasedLockword64 biasedLockword = BiasedLockword64.from(lockword);
                final int hashcode = biasedLockword.getHashcode();
                if (hashcode != 0) {
                    return hashcode;
                }

                if (newHashcode == 0) {
                    newHashcode = monitorScheme().createHashCode(object);
                }

                // Do we own the bias?
                final BiasedLockEpoch64 classEpoch = ObjectAccess.readHub(object).biasedLockEpoch;
                final int lockwordThreadID = encodeCurrentThreadIDForLockword();
                if (biasedLockword.getBiasOwnerID() == lockwordThreadID) {
                    if (biasedLockword.getEpoch().equals(classEpoch) || !biasedLockword.countUnderflow()) {
                        ObjectAccess.writeMisc(object, biasedLockword.setHashcode(newHashcode));
                        return newHashcode;
                    }
                }
                if (biasedLockword.equals(biasedLockword.asAnonBiased()) || !biasedLockword.getEpoch().equals(classEpoch)) {
                    lockword = ModalLockword64.from(ObjectAccess.compareAndSwapMisc(object, biasedLockword, biasedLockword.setHashcode(newHashcode)));
                    if (lockword.equals(biasedLockword)) {
                        return newHashcode;
                    }
                } else {
                    // We have to revoke to set the hashcode...
                    final int vmThreadMapThreadID = decodeLockwordThreadID(biasedLockword.getBiasOwnerID());
                    lockword = revokeWithOwnerSafepointed(object, vmThreadMapThreadID, biasedLockword);
                    if (Monitor.TraceMonitors) {
                        final boolean lockDisabledSafepoints = Log.lock();
                        Log.print("Safepointed revoke for hashcode: ");
                        Log.println(object.getClass().getName());
                        Log.unlock(lockDisabledSafepoints);
                    }
                }
            }
            // Not a biased lock; delegate.
            return delegate().delegateMakeHashcode(object, lockword);
        }

        private BiasedLockRevocationHeuristics getHeuristics(Object object) {
            final Hub hub = ObjectAccess.readHub(object);
            BiasedLockRevocationHeuristics revocationHeuristics = hub.biasedLockRevocationHeuristics();
            if (revocationHeuristics == null) {
                // This is purposely not synchronised.
                // We only may miss an update to the heuristics.
                revocationHeuristics = new BiasedLockRevocationHeuristics();
                hub.setBiasedLockRevocationHeuristics(revocationHeuristics);
            }
            return revocationHeuristics;
        }

        private ModalLockword64 performRevocation(Object object, BiasedLockword64 lockword) {
            final BiasedLockRevocationHeuristics revocationHeuristics = getHeuristics(object);
            final RevocationType type = revocationHeuristics.notifyContentionRevocationRequest();
            ModalLockword64 postRevokeLockword = ModalLockword64.from(Word.zero());
            switch (type) {
                case SINGLE_OBJECT_REVOCATION: {
                    if (Monitor.TraceMonitors) {
                        final boolean lockDisabledSafepoints = Log.lock();
                        Log.print("SINGLE_OBJECT_REVOCATION: ");
                        Log.println(object.getClass().getName());
                        Log.unlock(lockDisabledSafepoints);
                    }
                    postRevokeLockword = revokeWithOwnerSafepointed(object, decodeLockwordThreadID(lockword.getBiasOwnerID()), lockword);
                    break;
                }
                case BULK_REBIAS: {
                    if (Monitor.TraceMonitors) {
                        final boolean lockDisabledSafepoints = Log.lock();
                        Log.print("BULK_REBIAS: ");
                        Log.println(object.getClass().getName());
                        Log.unlock(lockDisabledSafepoints);
                    }

                    BulkRebiasOperation operation = new BulkRebiasOperation(object);
                    operation.submit();
                    postRevokeLockword = operation.postRebiasLockword;
                    revocationHeuristics.notifyBulkRebiasComplete();
                    break;
                }
                case BULK_REVOCATION: {
                    if (Monitor.TraceMonitors) {
                        final boolean lockDisabledSafepoints = Log.lock();
                        Log.print("BULK_REVOCATION: ");
                        Log.println(object.getClass().getName());
                        Log.unlock(lockDisabledSafepoints);
                    }

                    BulkRevokeOperation operation = new BulkRevokeOperation(object);
                    operation.submit();
                    postRevokeLockword = operation.postRevokeLockword;
                    break;
                }
            }
            return postRevokeLockword;
        }

        class BulkRevokeOperation extends VmOperation {
            private final Object object;
            ModalLockword64 postRevokeLockword;
            BulkRevokeOperation(Object object) {
                super("BulkRevoke", null, Mode.Safepoint);
                this.object = object;
            }
            @Override
            protected void doIt() {
                final Hub hub = ObjectAccess.readHub(object);
                hub.biasedLockEpoch = BiasedLockEpoch64.bulkRevocation();
                postRevokeLockword = revokeBias(object);
            }
        }

        class BulkRebiasOperation extends VmOperation {
            private final Object object;
            ModalLockword64 postRebiasLockword;
            BulkRebiasOperation(Object object) {
                super("BulkRebias", null, Mode.Safepoint);
                this.object = object;
            }
            @Override
            protected void doIt() {
                final Hub hub = ObjectAccess.readHub(object);
                final BiasedLockEpoch64 epoch = hub.biasedLockEpoch;
                hub.biasedLockEpoch = epoch.increment();
                postRebiasLockword = revokeBias(object);
            }
        }
    }
}
