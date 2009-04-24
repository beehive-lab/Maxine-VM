/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.monitor.modal.modehandlers.lightweight.biased;

import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.monitor.modal.modehandlers.*;
import com.sun.max.vm.monitor.modal.modehandlers.AbstractModeHandler.*;
import com.sun.max.vm.monitor.modal.modehandlers.lightweight.biased.BiasedLockRevocationHeuristics.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.reference.*;
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
 *
 * @author Simon Wilkinson
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
    public static int decodeBiasOwnerThreadID(BiasedLockWord64 biasedLockWord) {
        if (biasedLockWord.equals(biasedLockWord.asAnonBiased())) {
            return -1;
        }
        return decodeLockwordThreadID(biasedLockWord.getBiasOwnerID());
    }

    private static Safepoint.Procedure _safePointProcedure = new Safepoint.Procedure() {
        @Override
        public void run(Pointer trapState) {
            // Suspended bias-owner threads wait here while a revoker thread performs the revocation.
            synchronized (VmThreadMap.ACTIVE) {
            }
            VmThreadLocal.SAFEPOINT_VENUE.setVariableReference(Reference.fromJava(Safepoint.Venue.NATIVE));
        }
    };

    protected ModalLockWord64 revokeBias(Object object) {
        final ModalLockWord64 lockWord = ModalLockWord64.from(ObjectAccess.readMisc(object));
        if (BiasedLockWord64.isBiasedLockWord(lockWord)) {
            final ModalLockWord64 newLockWord = delegate().prepareModalLockWord(object, lockWord);
            ObjectAccess.writeMisc(object, newLockWord);
            return newLockWord;
        }
        return lockWord;
    }

    protected ModalLockWord64 revokeWithoutSafepointing(Object object) {
        synchronized (VmThreadMap.ACTIVE) {
            return revokeBias(object);
        }
    }

    protected ModalLockWord64 revokeWithOwnerSafepointed(Object object, int vmThreadMapThreadID, BiasedLockWord64 biasedLockWord) {
        synchronized (VmThreadMap.ACTIVE) {
            final VmThread biasOwnerThread = VmThreadMap.ACTIVE.getVmThreadForID(vmThreadMapThreadID);
            if (biasOwnerThread == null) {
                // The bias owner is terminated. No need to safepoint.
                // Lets try to reset the bias to anon.
                return ModalLockWord64.from(ObjectAccess.compareAndSwapMisc(object, biasedLockWord, biasedLockWord.asAnonBiased()));
            }
            final Pointer vmThreadLocals = biasOwnerThread.vmThreadLocals();
            if (vmThreadLocals.isZero()) {
                // The bias holding thread is still starting up, so how can it own biases??
                ProgramError.unexpected("Attempted to revoke bias for still initializing thread.");
            }
            // Trigger safepoint for bias owner
            Safepoint.runProcedure(vmThreadLocals, _safePointProcedure);
            // Wait until bias owner is not mutating
            while (biasOwnerThread.isInNative()) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException interruptedException) {
                }
            }
            // Revoke bias
            final ModalLockWord64 newLockWord = revokeBias(object);
            // Reset the bias owner's safepoint latch
            Safepoint.reset(vmThreadLocals);
            return newLockWord;
        }
    }

    @Override
    public Word createMisc(Object object) {
        return BiasedLockWord64.anonBiasedFromHashcode(monitorScheme().createHashCode(object));
    }

    @Override
    public void afterGarbageCollection() {
        delegate().delegateAfterGarbageCollection();
    }

    @Override
    public void beforeGarbageCollection() {
        delegate().delegateBeforeGarbageCollection();
    }

    @Override
    public void monitorExit(Object object) {
        nullCheck(object);
        if (MaxineVM.isPrototyping()) {
            HostMonitor.exit(object);
            return;
        }
        if (ASSUME_PERFECT_ENTRY_AND_EXIT_PAIRS) {
            // We cannot have any safepoints (and hence any revocation) on the code path between here and the lockword store.
            final ModalLockWord64 lockWord = ModalLockWord64.from(ObjectAccess.readMisc(object));
            if (BiasedLockWord64.isBiasedLockWord(lockWord)) {
                // Blind fast path monitor exit.
                ObjectAccess.writeMisc(object, BiasedLockWord64.from(lockWord).decrementCount());
            } else {
                // Not a biased lock; delegate.
                delegate().delegateMonitorExit(object, lockWord);
            }
        } else {
            final int lockwordThreadID = encodeCurrentThreadIDForLockword();
            // We cannot have any safepoints (and hence any revocation) on the code path between here and the lockword store.
            final ModalLockWord64 lockWord = ModalLockWord64.from(ObjectAccess.readMisc(object));
            if (BiasedLockWord64.isBiasedLockWord(lockWord)) {
                final BiasedLockWord64 biasedLockWord = BiasedLockWord64.from(lockWord);
                if (biasedLockWord.getBiasOwnerID() != lockwordThreadID || biasedLockWord.countUnderflow()) {
                    throw new IllegalMonitorStateException();
                }
                // Fast path monitor exit.
                ObjectAccess.writeMisc(object, biasedLockWord.decrementCount());
            } else {
                // Not a biased lock; delegate.
                delegate().delegateMonitorExit(object, lockWord);
            }
        }
    }

    @Override
    public void monitorNotify(Object object, boolean all) {
        nullCheck(object);
        if (MaxineVM.isPrototyping()) {
            HostMonitor.notify(object);
            return;
        }
        final ModalLockWord64 lockWord = ModalLockWord64.from(ObjectAccess.readMisc(object));
        if (BiasedLockWord64.isBiasedLockWord(lockWord)) {
            final BiasedLockWord64 biasedLockWord = BiasedLockWord64.from(lockWord);
            if (biasedLockWord.countUnderflow() || biasedLockWord.getBiasOwnerID() != encodeCurrentThreadIDForLockword()) {
                throw new IllegalMonitorStateException();
            }
            // By biased lock semantics we have no threads waiting, so just return.
        } else {
            // Not a biased lock; delegate.
            delegate().delegateMonitorNotify(object, all, lockWord);
        }
    }

    @Override
    public void monitorWait(Object object, long timeout) throws InterruptedException {
        nullCheck(object);
        if (MaxineVM.isPrototyping()) {
            HostMonitor.wait(object, timeout);
            return;
        }
        ModalLockWord64 lockWord = ModalLockWord64.from(ObjectAccess.readMisc(object));
        if (BiasedLockWord64.isBiasedLockWord(lockWord)) {
            final BiasedLockWord64 biasedLockWord = BiasedLockWord64.from(lockWord);
            final int lockwordThreadID = encodeCurrentThreadIDForLockword();
            if (biasedLockWord.countUnderflow() || biasedLockWord.getBiasOwnerID() != lockwordThreadID) {
                throw new IllegalMonitorStateException();
            }
            // We can't have a wait queue, so move the lock to the next delegate mode and try to wait.
            // (We can revoke without suspending as the current thread owns the bias)
            if (Monitor.traceMonitors()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("Nonsafepointed revoke for monitorWait: ");
                Log.println(object.getClass().getName());
                Log.unlock(lockDisabledSafepoints);
            }
            lockWord = revokeWithoutSafepointing(object);
        }
        // Not a biased lock; delegate.
        delegate().delegateMonitorWait(object, timeout, lockWord);
    }

    private final boolean[] _threadHoldsMonitorResult = new boolean[1];

    @Override
    public boolean threadHoldsMonitor(Object object, VmThread thread) {
        nullCheck(object);
        final int lockwordThreadID = encodeCurrentThreadIDForLockword();
        final ModalLockWord64 lockWord = ModalLockWord64.from(ObjectAccess.readMisc(object));
        if (BiasedLockWord64.isBiasedLockWord(lockWord)) {
            final BiasedLockWord64 biasedLockWord = BiasedLockWord64.from(lockWord);
            return !biasedLockWord.countUnderflow() && biasedLockWord.getBiasOwnerID() == lockwordThreadID;
        }
        delegate().delegateThreadHoldsMonitor(object, lockWord, thread, lockwordThreadID, _threadHoldsMonitorResult);
        return _threadHoldsMonitorResult[0];
    }

    private static final class FastPathNoEpoch extends BiasedLockModeHandler {

        private FastPathNoEpoch(ModeDelegate delegate) {
            super(delegate);
        }

        @Override
        public void monitorEnter(Object object) {
            nullCheck(object);
            if (MaxineVM.isPrototyping()) {
                HostMonitor.enter(object);
                return;
            }
            final int lockwordThreadID = encodeCurrentThreadIDForLockword();
            // We cannot have any safepoints (and hence any revocation) on the code path between here and the lockword store.
            final ModalLockWord64 lockWord = ModalLockWord64.from(ObjectAccess.readMisc(object));
            if (BiasedLockWord64.isBiasedLockWord(lockWord)) {
                final BiasedLockWord64 biasedLockWord = BiasedLockWord64.from(lockWord);
                if (biasedLockWord.getBiasOwnerID() == lockwordThreadID && !biasedLockWord.countOverflow()) {
                    ObjectAccess.writeMisc(object, biasedLockWord.incrementCount());
                    return;
                }
            }
            slowPathMonitorEnter(object, lockWord, lockwordThreadID);
        }

        private void slowPathMonitorEnter(Object object, ModalLockWord64 lockWord, int lockwordThreadID) {
            ModalLockWord64 currentLockWord = lockWord;
            while (BiasedLockWord64.isBiasedLockWord(currentLockWord)) {
                final BiasedLockWord64 biasedLockWord = BiasedLockWord64.from(currentLockWord);
                // Is the lock unbiased and biasable?
                if (biasedLockWord.equals(biasedLockWord.asAnonBiased())) {
                    // Try to get the bias
                    final BiasedLockWord64 newBiasedLockWord = biasedLockWord.asBiasedAndLockedOnceBy(lockwordThreadID);
                    currentLockWord = ModalLockWord64.from(ObjectAccess.compareAndSwapMisc(object, biasedLockWord, newBiasedLockWord));
                    if (currentLockWord.equals(biasedLockWord)) {
                        // Current thread is now the bias owner
                        return;
                    }
                } else if (biasedLockWord.getBiasOwnerID() == lockwordThreadID && biasedLockWord.countOverflow()) {
                    // The current thread owns the lock so we can revoke and delegate
                    currentLockWord = revokeWithoutSafepointing(object);
                } else {
                    // Another thread holds the bias - so revoke.
                    // Note the revoking thread has no special priviledges, we simply revoke and then
                    // drop into the next locking mode's monitor enter code.
                    if (Monitor.traceMonitors()) {
                        final boolean lockDisabledSafepoints = Log.lock();
                        Log.print("Safepointed revoke for monitorEnter: ");
                        Log.println(object.getClass().getName());
                        Log.unlock(lockDisabledSafepoints);
                    }
                    final int vmThreadMapThreadID = decodeLockwordThreadID(biasedLockWord.getBiasOwnerID());
                    currentLockWord = revokeWithOwnerSafepointed(object, vmThreadMapThreadID, biasedLockWord);
                }
            }
            delegate().delegateMonitorEnter(object, currentLockWord, lockwordThreadID);
        }

        @Override
        public int makeHashCode(Object object) {
            nullCheck(object);
            if (MaxineVM.isPrototyping()) {
                return monitorScheme().createHashCode(object);
            }
            int newHashcode = 0;
            ModalLockWord64 lockWord = ModalLockWord64.from(ObjectAccess.readMisc(object));
            while (BiasedLockWord64.isBiasedLockWord(lockWord)) {
                final BiasedLockWord64 biasedLockWord = BiasedLockWord64.from(lockWord);
                final int hashcode = biasedLockWord.getHashcode();
                if (hashcode != 0) {
                    return hashcode;
                }
                if (newHashcode == 0) {
                    newHashcode = monitorScheme().createHashCode(object);
                }
                // Do we own the bias?
                final int lockwordThreadID = encodeCurrentThreadIDForLockword();
                if (biasedLockWord.getBiasOwnerID() == lockwordThreadID) {
                    // No safepoints until after we have the hashcode in place
                    ObjectAccess.writeMisc(object, biasedLockWord.setHashcode(newHashcode));
                    return newHashcode;
                } else if (biasedLockWord.equals(biasedLockWord.asAnonBiased())) {
                    // If the biased lock is anon biased, then try setting a hashcode
                    lockWord = ModalLockWord64.from(ObjectAccess.compareAndSwapMisc(object, biasedLockWord, biasedLockWord.setHashcode(newHashcode)));
                    if (lockWord.equals(biasedLockWord)) {
                        return newHashcode;
                    }
                } else {
                    // We have to revoke to set the hashcode...
                    if (Monitor.traceMonitors()) {
                        final boolean lockDisabledSafepoints = Log.lock();
                        Log.print("Safepointed revoke for hashcode: ");
                        Log.println(object.getClass().getName());
                        Log.unlock(lockDisabledSafepoints);
                    }
                    final int vmThreadMapThreadID = decodeLockwordThreadID(biasedLockWord.getBiasOwnerID());
                    lockWord = revokeWithOwnerSafepointed(object, vmThreadMapThreadID, biasedLockWord);
                }
            }
            // Not a biased lock; delegate.
            return delegate().delegateMakeHashcode(object, lockWord);
        }
    }

    private static final class FastPathWithEpoch extends BiasedLockModeHandler {

        private FastPathWithEpoch(ModeDelegate delegate) {
            super(delegate);
        }

        @Override
        public void monitorEnter(Object object) {
            nullCheck(object);
            if (MaxineVM.isPrototyping()) {
                HostMonitor.enter(object);
                return;
            }
            final int lockwordThreadID = encodeCurrentThreadIDForLockword();
            final BiasedLockEpoch classEpoch = ObjectAccess.readHub(object).biasedLockEpoch();
            // We cannot have any safepoints (and hence any revocation) on the code path between here and the lockword store.
            final ModalLockWord64 lockWord = ModalLockWord64.from(ObjectAccess.readMisc(object));
            if (BiasedLockWord64.isBiasedLockWord(lockWord)) {
                final BiasedLockWord64 biasedLockWord = BiasedLockWord64.from(lockWord);
                if (biasedLockWord.getEpoch().equals(classEpoch) && biasedLockWord.getBiasOwnerID() == lockwordThreadID && !biasedLockWord.countOverflow()) {
                    ObjectAccess.writeMisc(object, biasedLockWord.incrementCount());
                    return;
                }
            }
            slowPathMonitorEnter(object, lockWord, lockwordThreadID);
        }

        private void slowPathMonitorEnter(Object object, ModalLockWord64 lockWord, int lockwordThreadID) {
            ModalLockWord64 currentLockWord = lockWord;
            while (BiasedLockWord64.isBiasedLockWord(currentLockWord)) {
                final BiasedLockWord64 biasedLockWord = BiasedLockWord64.from(currentLockWord);
                final BiasedLockEpoch classEpoch = ObjectAccess.readHub(object).biasedLockEpoch();
                if (classEpoch.isBulkRevocation()) {
                    // Objects of this class are no longer eligable for biased locking
                    if (biasedLockWord.equals(biasedLockWord.asAnonBiased())) {
                        // Object is not biased or locked, change the lockword to the next locking mode
                        final ModalLockWord64 newLockWord = delegate().prepareModalLockWord(object, currentLockWord);
                        currentLockWord = ModalLockWord64.from(ObjectAccess.compareAndSwapMisc(object, biasedLockWord, newLockWord));
                        if (!currentLockWord.equals(biasedLockWord)) {
                            delegate().cancelPreparedModalLockWord(newLockWord);
                        }
                        if (Monitor.traceMonitors()) {
                            final boolean lockDisabledSafepoints = Log.lock();
                            Log.print("New object bulk revocation: ");
                            Log.println(object.getClass().getName());
                            Log.unlock(lockDisabledSafepoints);
                        }
                    } else if (biasedLockWord.getBiasOwnerID() == lockwordThreadID) {
                        // Object is biased and locked by the current thread. Revoke the bias.
                        currentLockWord = revokeWithoutSafepointing(object);
                    } else {
                        // Object is biased to another thread. Revoke the bias.
                        final int vmThreadMapThreadID = decodeLockwordThreadID(biasedLockWord.getBiasOwnerID());
                        currentLockWord = revokeWithOwnerSafepointed(object, vmThreadMapThreadID, biasedLockWord);
                    }
                } else if (biasedLockWord.getBiasOwnerID() == lockwordThreadID && !biasedLockWord.getEpoch().equals(classEpoch) &&
                           !biasedLockWord.countUnderflow() && !biasedLockWord.countOverflow()) {
                    // Object is biased to and locked by the current thread, but the object's class has been rebiased.
                    // We lock as normal. When the lock is released, the object will be rebiased.
                    ObjectAccess.writeMisc(object, biasedLockWord.incrementCount());
                    return;
                } else if (biasedLockWord.equals(biasedLockWord.asAnonBiased()) || !biasedLockWord.getEpoch().equals(classEpoch)) {
                    // Object is not biased or it's bias is not in the current epoch. Try to get the bias.
                    final BiasedLockWord64 newBiasedLockWord = biasedLockWord.asBiasedAndLockedOnceBy(lockwordThreadID, classEpoch);
                    currentLockWord = ModalLockWord64.from(ObjectAccess.compareAndSwapMisc(object, biasedLockWord, newBiasedLockWord));
                    if (currentLockWord.equals(biasedLockWord)) {
                        // Current thread is now the bias owner
                        return;
                    }
                } else if (biasedLockWord.getBiasOwnerID() == lockwordThreadID && biasedLockWord.countOverflow()) {
                    // Overflow of the recursion count. The current thread owns the lock so we can revoke and delegate.
                    if (Monitor.traceMonitors()) {
                        final boolean lockDisabledSafepoints = Log.lock();
                        Log.print("Safepointed revoke for hashcode: ");
                        Log.println(object.getClass().getName());
                        Log.unlock(lockDisabledSafepoints);
                    }
                    currentLockWord = revokeWithoutSafepointing(object);
                } else {
                    // Another thread holds the bias - so revoke.
                    currentLockWord = performRevocation(object, biasedLockWord);
                }
            }
            delegate().delegateMonitorEnter(object, currentLockWord, lockwordThreadID);
        }

        @Override
        public int makeHashCode(Object object) {
            // This is under construction!
            nullCheck(object);
            if (MaxineVM.isPrototyping()) {
                return monitorScheme().createHashCode(object);
            }
            int newHashcode = 0;
            ModalLockWord64 lockWord = ModalLockWord64.from(ObjectAccess.readMisc(object));
            while (BiasedLockWord64.isBiasedLockWord(lockWord)) {
                final BiasedLockWord64 biasedLockWord = BiasedLockWord64.from(lockWord);
                final int hashcode = biasedLockWord.getHashcode();
                if (hashcode != 0) {
                    return hashcode;
                }

                if (newHashcode == 0) {
                    newHashcode = monitorScheme().createHashCode(object);
                }

                // Do we own the bias?
                final BiasedLockEpoch classEpoch = ObjectAccess.readHub(object).biasedLockEpoch();
                final int lockwordThreadID = encodeCurrentThreadIDForLockword();
                if (biasedLockWord.getBiasOwnerID() == lockwordThreadID) {
                    if (biasedLockWord.getEpoch().equals(classEpoch) || !biasedLockWord.countUnderflow()) {
                        ObjectAccess.writeMisc(object, biasedLockWord.setHashcode(newHashcode));
                        return newHashcode;
                    }
                }
                if (biasedLockWord.equals(biasedLockWord.asAnonBiased()) || !biasedLockWord.getEpoch().equals(classEpoch)) {
                    lockWord = ModalLockWord64.from(ObjectAccess.compareAndSwapMisc(object, biasedLockWord, biasedLockWord.setHashcode(newHashcode)));
                    if (lockWord.equals(biasedLockWord)) {
                        return newHashcode;
                    }
                } else {
                    // We have to revoke to set the hashcode...
                    final int vmThreadMapThreadID = decodeLockwordThreadID(biasedLockWord.getBiasOwnerID());
                    lockWord = revokeWithOwnerSafepointed(object, vmThreadMapThreadID, biasedLockWord);
                    if (Monitor.traceMonitors()) {
                        final boolean lockDisabledSafepoints = Log.lock();
                        Log.print("Safepointed revoke for hashcode: ");
                        Log.println(object.getClass().getName());
                        Log.unlock(lockDisabledSafepoints);
                    }
                }
            }
            // Not a biased lock; delegate.
            return delegate().delegateMakeHashcode(object, lockWord);
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

        private ModalLockWord64 performRevocation(Object object, BiasedLockWord64 lockWord) {
            final BiasedLockRevocationHeuristics revocationHeuristics = getHeuristics(object);
            final RevocationType type = revocationHeuristics.notifyContentionRevocationRequest();
            ModalLockWord64 postRevokeLockWord = ModalLockWord64.from(Word.zero());
            switch (type) {
                case SINGLE_OBJECT_REVOCATION:
                    if (Monitor.traceMonitors()) {
                        final boolean lockDisabledSafepoints = Log.lock();
                        Log.print("SINGLE_OBJECT_REVOCATION: ");
                        Log.println(object.getClass().getName());
                        Log.unlock(lockDisabledSafepoints);
                    }
                    postRevokeLockWord = revokeWithOwnerSafepointed(object, decodeLockwordThreadID(lockWord.getBiasOwnerID()), lockWord);
                    break;
                case BULK_REBIAS:
                    if (Monitor.traceMonitors()) {
                        final boolean lockDisabledSafepoints = Log.lock();
                        Log.print("BULK_REBIAS: ");
                        Log.println(object.getClass().getName());
                        Log.unlock(lockDisabledSafepoints);
                    }
                    postRevokeLockWord = revokeWithOwnerSafepointedAndBulkRebias(object);
                    revocationHeuristics.notifyBulkRebiasComplete();
                    break;
                case BULK_REVOCATION:
                    if (Monitor.traceMonitors()) {
                        final boolean lockDisabledSafepoints = Log.lock();
                        Log.print("BULK_REVOCATION: ");
                        Log.println(object.getClass().getName());
                        Log.unlock(lockDisabledSafepoints);
                    }
                    postRevokeLockWord = revokeWithOwnerSafepointedAndBulkRevoke(object);
                    break;
            }
            return postRevokeLockWord;
        }

        private ModalLockWord64 revokeWithOwnerSafepointedAndBulkRevoke(Object object) {
            ModalLockWord64 postRevokeLockWord;
            synchronized (VmThreadMap.ACTIVE) {
                VmThreadMap.ACTIVE.forAllVmThreadLocals(VmThreadMap._isNotCurrent, _triggerSafepoint);
                VmThreadMap.ACTIVE.forAllVmThreadLocals(VmThreadMap._isNotCurrent, _waitUntilNonMutating);
                final Hub hub = ObjectAccess.readHub(object);
                hub.setBiasedLockEpoch(BiasedLockEpoch.bulkRevocation());
                postRevokeLockWord = revokeBias(object);
                VmThreadMap.ACTIVE.forAllVmThreadLocals(VmThreadMap._isNotCurrent, _resetSafepoint);
            }
            return postRevokeLockWord;
        }

        private ModalLockWord64 revokeWithOwnerSafepointedAndBulkRebias(Object object) {
            ModalLockWord64 postRevokeLockWord;
            synchronized (VmThreadMap.ACTIVE) {
                VmThreadMap.ACTIVE.forAllVmThreadLocals(VmThreadMap._isNotCurrent, _triggerSafepoint);
                VmThreadMap.ACTIVE.forAllVmThreadLocals(VmThreadMap._isNotCurrent, _waitUntilNonMutating);
                final Hub hub = ObjectAccess.readHub(object);
                final BiasedLockEpoch epoch = hub.biasedLockEpoch();
                hub.setBiasedLockEpoch(epoch.increment());
                postRevokeLockWord = revokeBias(object);
                VmThreadMap.ACTIVE.forAllVmThreadLocals(VmThreadMap._isNotCurrent, _resetSafepoint);
            }
            return postRevokeLockWord;
        }

        private final Pointer.Procedure _triggerSafepoint = new Pointer.Procedure() {
            public void run(Pointer vmThreadLocals) {
                if (vmThreadLocals.isZero()) {
                    // Thread is still starting up.
                    // Do not need to do anything, because it will try to lock 'VmThreadMap.ACTIVE' and thus block.
                } else {
                    Safepoint.runProcedure(vmThreadLocals, _safePointProcedure);
                }
            }
        };

        private final Pointer.Procedure _resetSafepoint = new Pointer.Procedure() {
            public void run(Pointer vmThreadLocals) {
                Safepoint.reset(vmThreadLocals);
            }
        };

        private final Pointer.Procedure _waitUntilNonMutating = new Pointer.Procedure() {
            public void run(Pointer vmThreadLocals) {
                while (VmThreadLocal.inJava(vmThreadLocals)) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException interruptedException) {
                    }
                }
            }
        };
    }
}
