/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
package com.sun.max.vm.monitor.modal.modehandlers.lightweight.thin;

import com.sun.max.annotate.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.monitor.modal.modehandlers.*;
import com.sun.max.vm.monitor.modal.modehandlers.AbstractModeHandler.ModeDelegate.*;
import com.sun.max.vm.monitor.modal.modehandlers.inflated.*;
import com.sun.max.vm.monitor.modal.modehandlers.lightweight.biased.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.thread.*;

/**
 *
 * TODO: For AMD64 and Sparc TSO we don't need any membars. PPC, IA32 and ARM will need some adding to the acquire and
 * release logic.
 */
public abstract class ThinLockModeHandler extends AbstractModeHandler {

    private static final int THIN_LOCK_RETRIES = 20;

    /**
     * Returns a ThinLockModeHandler with the required interface for fast-path entry from a MonitorScheme.
     */
    public static MonitorSchemeEntry asFastPath(ModeDelegate delegate) {
        return new ThinLockModeHandler.FastPath(delegate);
    }

    /**
     * Returns a ThinLockModeHandler with the required interface to act as delegate for biased locking.
     */
    public static ModeDelegate asBiasedLockDelegate(ModeDelegate delegate) {
        return new ThinLockModeHandler.BiasedLockDelegate(delegate);
    }

    /**
     * Returns a ThinLockModeHandler with the required interface to act as delegate for the monitor observer mode.
     */
    public static ModeDelegate asObserverModeDelegate(ModeDelegate delegate) {
        // We cheat here by re-using the biased-lock delegate, as it has the correct interface.
        return new ThinLockModeHandler.BiasedLockDelegate(delegate);
    }

    protected ThinLockModeHandler(ModeDelegate delegate) {
        super(delegate);
    }

    //64bit: The whole inflated word
    // 32bit: Just the inflated bits
    private ModalLockword inflate(Object object, ThinLockword lockword) {
        // 64bit: inflated lock word, 32bit: monitor
        ModalLockword inflatedLockword = delegate().prepareModalLockword(object, lockword);
        // 64bit: zero, 32bit: the inflatedBits
        ModalLockword inflatedBits = Platform.target().arch.is32bit() ? InflatedMonitorLockword.boundFromZero() : ModalLockword.from(Word.zero());

        // 32bit: We save the original hash in order to ensure that the two stage CAS is consistent;
        ModalLockword originalHash = ModalLockword.from(ObjectAccess.readHash(object));
        boolean isOriginalInflated = ThinLockword.from(ObjectAccess.readMisc(object)).isInflated();

        ThinLockword thinLockword = lockword;

        while (true) {
            final ModalLockword answer = ModalLockword.from(ObjectAccess.compareAndSwapMisc(object, thinLockword, Platform.target().arch.is64bit() ? inflatedLockword : inflatedBits));
            if (answer.equals(thinLockword)) {
                if (Platform.target().arch.is32bit()) {
                    ObjectAccess.writeHash(object, inflatedLockword);
                }
                break;
            } else if (answer.isInflated()) {
                ModalLockword currentHash = ModalLockword.from(Word.zero());
                if (Platform.target().arch.is32bit() && !isOriginalInflated) {
                    currentHash = ModalLockword.from(ObjectAccess.readHash(object));
                    assert !currentHash.equals(originalHash);
                    while (currentHash.equals(originalHash)) {
                        currentHash = ModalLockword.from(ObjectAccess.readHash(object));
                    }
                }
                // Unbind the speculative monitor
                delegate().cancelPreparedModalLockword(inflatedLockword);
                inflatedLockword = answer;
                break;
            }
            // We will spin if the thin lock word has been changed in any way (new owner, rcount change, new hashcode)
            thinLockword = ThinLockword.from(answer);
            inflatedLockword = delegate().reprepareModalLockword(inflatedLockword, thinLockword,
                            Platform.target().arch.is64bit() ? ModalLockword.from(Word.zero()) : ModalLockword.from(ObjectAccess.readHash(object)));
        }
        return Platform.target().arch.is64bit() ? inflatedLockword : inflatedBits;
    }

    @SNIPPET_SLOWPATH
    protected void slowPathMonitorEnter(Object object, ModalLockword lockword, int lockwordThreadID) {
        ModalLockword newLockword = lockword;
        int retries = THIN_LOCK_RETRIES;
        while (true) {
            if (ThinLockword.isThinLockword(newLockword)) {
                final ThinLockword thinLockword = ThinLockword.from(newLockword);
                // Do we own the lock?
                if (thinLockword.getLockOwnerID() == lockwordThreadID) {
                    // Attempt to inc the recursion count
                    if (!thinLockword.countOverflow()) {
                        final ModalLockword answer = ModalLockword.from(ObjectAccess.compareAndSwapMisc(object, thinLockword, thinLockword.incrementCount()));
                        if (answer.equals(thinLockword)) {
                            return;
                        }
                        // An inflation or a new hashcode was installed. Try again
                        newLockword = answer;
                        continue;
                    }
                } else {
                    final ThinLockword asUnlocked = thinLockword.asUnlocked();
                    final ThinLockword asLocked = thinLockword.asLockedOnceBy(lockwordThreadID);
                    final ModalLockword answer = ModalLockword.from(ObjectAccess.compareAndSwapMisc(object, asUnlocked, asLocked));
                    if (answer.equals(asUnlocked)) {
                        // The current thread got the lock
                        return;
                    }
                    // This could be a hashcode, inflation or another thread got the lock.
                    // Lets try again.
                    newLockword = answer;
                    if (--retries > 0) {
                        continue;
                    }
                }
                // Count overflow or too much contention - inflate
                newLockword = inflate(object, thinLockword);
            }

            // The monitor is inflated
            if (delegate().delegateMonitorEnter(object, newLockword, lockwordThreadID)) {
                return;
            }
            // Try again. Monitor was deflated.
            newLockword = ModalLockword.from(ObjectAccess.readMisc(object));
            retries = THIN_LOCK_RETRIES;
        }
    }

    @SNIPPET_SLOWPATH
    protected void slowPathMonitorExit(Object object, ModalLockword lockword, int lockwordThreadID) {
        if (ThinLockword.isThinLockword(lockword)) {
            ThinLockword thinLockword = ThinLockword.from(lockword);
            if (thinLockword.countUnderflow() || thinLockword.getLockOwnerID() != lockwordThreadID) {
                throw new IllegalMonitorStateException();
            }
            final boolean isRelease = thinLockword.decrementCount().countUnderflow();
            while (true) {
                ThinLockword answer = ThinLockword.from(Word.zero());
                if (isRelease) {
                    answer = ThinLockword.from(ObjectAccess.compareAndSwapMisc(object, thinLockword, thinLockword.asUnlocked()));
                } else {
                    answer = ThinLockword.from(ObjectAccess.compareAndSwapMisc(object, thinLockword, thinLockword.decrementCount()));
                }
                if (answer.equals(thinLockword)) {
                    return;
                } else if (answer.isInflated()) {
                    // We don't have to check for deflation, as this cannot happen while we own the lock.
                    delegate().delegateMonitorExit(object, answer);
                    return;
                }
                // A hashcode was installed. Try again.
                thinLockword = answer;
            }
        }
        // We don't have to check for deflation, as this cannot happen while we own the lock.
        delegate().delegateMonitorExit(object, lockword);
    }

    protected int makeHashCode(Object object, ModalLockword lockword, ModalLockword hashword) {
        assert hashword.isZero() || Platform.target().arch.is32bit() : "Non null hashword in 64bit!";
        ModalLockword newLockword = lockword;
        int newHashcode = 0;
        while (true) {
            if (ThinLockword.isThinLockword(newLockword)) {
                final ThinLockword hashLockword = ThinLockword.from(hashword);
                final ThinLockword thinLockword = ThinLockword.from(newLockword);
                final int hashcode = Platform.target().arch.is64bit() ? thinLockword.getHashcode() : hashLockword.getHashcode();
                if (hashcode != 0) {
                    return hashcode;
                }
                if (newHashcode == 0) {
                    newHashcode = monitorScheme().createHashCode(object);
                }
                newLockword = Platform.target().arch.is64bit() ? ModalLockword.from(ObjectAccess.compareAndSwapMisc(object, thinLockword, thinLockword.setHashcode(newHashcode)))
                                : ModalLockword.from(ObjectAccess.compareAndSwapHash(object, hashLockword, hashLockword.setHashcode(newHashcode)));
                if (Platform.target().arch.is64bit()) {
                    if (newLockword.equals(thinLockword)) {
                        return newHashcode;
                    }
                } else {
                    if (newLockword.equals(hashLockword)) {
                        return newHashcode;
                    } else {
                        newLockword = ModalLockword.from(ObjectAccess.readMisc(object));
                        hashword = ModalLockword.from(ObjectAccess.readHash(object));
                    }
                }
                // Could be another thread beat us to the hashcode, or an inflation. Try again.
                continue;
            }
            newHashcode = delegate().delegateMakeHashcode(object, newLockword);
            if (newHashcode != 0) {
                return newHashcode;
            }
            // Possible deflation. Try again.
            newLockword = ModalLockword.from(ObjectAccess.readMisc(object));
        }
    }

    protected void monitorNotify(Object object, boolean all, ModalLockword lockword) {
        if (ThinLockword.isThinLockword(lockword)) {
            final ThinLockword thinLockword = ThinLockword.from(lockword);
            if (thinLockword.countUnderflow() || thinLockword.getLockOwnerID() != encodeCurrentThreadIDForLockword()) {
                throw new IllegalMonitorStateException();
            }
            // By lightweight lock semantics we have no threads waiting, so just return.
            return;
        }
        // We don't have to check for deflation, as either we own the lock or an exception will be thrown
        delegate().delegateMonitorNotify(object, all, lockword);
    }

    protected void monitorWait(Object object, long timeout, ModalLockword lockword) throws InterruptedException {
        if (ThinLockword.isThinLockword(lockword)) {
            final ThinLockword thinLockword = ThinLockword.from(lockword);
            if (thinLockword.countUnderflow() || thinLockword.getLockOwnerID() != encodeCurrentThreadIDForLockword()) {
                throw new IllegalMonitorStateException();
            }
            final ModalLockword newLockword = inflate(object, thinLockword);
            // We don't have to check for deflation as we own the lock
            delegate().delegateMonitorWait(object, timeout, newLockword);
            return;
        }
        // We don't have to check for deflation, as either we own the lock or an exception will be thrown
        delegate().delegateMonitorWait(object, timeout, lockword);
    }

    protected boolean threadHoldsMonitor(Object object, ModalLockword lockword, VmThread thread, int threadID) {
        ModalLockword newLockword = lockword;
        while (true) {
            if (ThinLockword.isThinLockword(newLockword)) {
                final ThinLockword thinLockword = ThinLockword.from(newLockword);
                return !thinLockword.countUnderflow() && thinLockword.getLockOwnerID() == threadID;
            }
            final DelegatedThreadHoldsMonitorResult result = delegate().delegateThreadHoldsMonitor(object, lockword, thread, threadID);
            switch (result) {
                case TRUE:
                    return true;
                case FALSE:
                    return false;
                case NOT_THIS_MODE:
                    break;
            }

            // Deflation. Try again.
            newLockword = ModalLockword.from(ObjectAccess.readMisc(object));
        }
    }

    protected void afterGarbageCollection() {
        delegate().delegateAfterGarbageCollection();
    }

    protected void beforeGarbageCollection() {
        delegate().delegateBeforeGarbageCollection();
    }

    // Inspector support
    public static int decodeLockOwnerThreadID(ThinLockword thinLockword) {
        if (thinLockword.equals(thinLockword.asUnlocked())) {
            return -1;
        }
        return decodeLockwordThreadID(thinLockword.getLockOwnerID());
    }

    /**
     * Implements the required interface to allow a ThinLockModeHandler to acts as a delegate to biased locking.
     */
    private static final class BiasedLockDelegate extends ThinLockModeHandler implements ModeDelegate {

        protected BiasedLockDelegate(ModeDelegate delegate) {
            super(delegate);
            assert Platform.target().arch.is64bit() : "Biased locking is not implemented for 32 bit";
        }

        public boolean delegateMonitorEnter(Object object, ModalLockword lockword, int lockwordThreadID) {
            final ThinLockword thinLockword = ThinLockword.from(lockword);
            final ThinLockword asUnlocked = thinLockword.asUnlocked();
            final ThinLockword asLocked = thinLockword.asLockedOnceBy(lockwordThreadID);
            final ModalLockword answer = ModalLockword.from(ObjectAccess.compareAndSwapMisc(object, asUnlocked, asLocked));
            if (!answer.equals(asUnlocked)) {
                slowPathMonitorEnter(object, answer, lockwordThreadID);
            }
            return true;
        }

        public void delegateMonitorExit(Object object, ModalLockword lockword) {
            final int lockwordThreadID = encodeCurrentThreadIDForLockword();
            final ThinLockword thinLockword = ThinLockword.from(lockword);
            final ThinLockword asUnlocked = thinLockword.asUnlocked();
            final ThinLockword asLocked = thinLockword.asLockedOnceBy(lockwordThreadID);
            final ModalLockword answer = ModalLockword.from(ObjectAccess.compareAndSwapMisc(object, asLocked, asUnlocked));
            if (!answer.equals(asLocked)) {
                slowPathMonitorExit(object, answer, lockwordThreadID);
            }
        }

        public void delegateMonitorNotify(Object object, boolean all, ModalLockword lockword) {
            monitorNotify(object, all, ThinLockword.from(lockword));
        }

        public void delegateMonitorWait(Object object, long timeout, ModalLockword lockword) throws InterruptedException {
            monitorWait(object, timeout, ThinLockword.from(lockword));
        }

        public DelegatedThreadHoldsMonitorResult delegateThreadHoldsMonitor(Object object, ModalLockword lockword, VmThread thread, int threadID) {
            return threadHoldsMonitor(object, ThinLockword.from(lockword), thread, threadID) ? DelegatedThreadHoldsMonitorResult.TRUE : DelegatedThreadHoldsMonitorResult.FALSE;
        }

        public int delegateMakeHashcode(Object object, ModalLockword lockword) {
            return makeHashCode(object, ThinLockword.from(lockword), Platform.target().arch.is64bit() ? ModalLockword.from(Word.zero()) : ModalLockword.from(ObjectAccess.readHash(object)));
        }

        public void delegateAfterGarbageCollection() {
            afterGarbageCollection();
        }

        public void delegateBeforeGarbageCollection() {
            beforeGarbageCollection();
        }

        public ModalLockword prepareModalLockword(Object object, ModalLockword currentLockword) {
            final BiasedLockword biasedLockword = BiasedLockword.from(currentLockword);
            ThinLockword thinLockword = ThinLockword.from(biasedLockword.asUnbiasable());
            if (biasedLockword.countUnderflow()) {
                // The lock was not held, only biased, so remove the threadID too.
                thinLockword = thinLockword.asUnlocked();
            }
            return thinLockword;
        }

        public void cancelPreparedModalLockword(ModalLockword preparedLockword) {
            // Nothing to do
        }

        public ModalLockword reprepareModalLockword(ModalLockword preparedLockword, ModalLockword currentLockword, ModalLockword hash) {
            // TODO Auto-generated method stub
            return ModalLockword.from(Word.zero());
        }
    }

    /**
     * Implements fast-path monitor entry and exit for a ThinLockModeHandler.
     */
    private static final class FastPath extends ThinLockModeHandler implements MonitorSchemeEntry {

        protected FastPath(ModeDelegate delegate) {
            super(delegate);
        }

        @INLINE
        public void monitorEnter(Object object) {
            if (MaxineVM.isHosted()) {
                HostMonitor.enter(object);
                return;
            }
            // Fast path monitor enter.
            // TODO: Arch dependent membars
            final int lockwordThreadID = encodeCurrentThreadIDForLockword();
            final ThinLockword lockword = ThinLockword.from(ObjectAccess.readMisc(object));
            final ThinLockword asUnlocked = lockword.asUnlocked();
            final ThinLockword asLocked = lockword.asLockedOnceBy(lockwordThreadID);
            final ModalLockword answer = ModalLockword.from(ObjectAccess.compareAndSwapMisc(object, asUnlocked, asLocked));
            if (!answer.equals(asUnlocked)) {
                slowPathMonitorEnter(object, answer, lockwordThreadID);
            }
        }

        @INLINE
        public void monitorExit(Object object) {
            if (MaxineVM.isHosted()) {
                HostMonitor.exit(object);
                return;
            }
            // Fast path monitor exit.
            // TODO: Arch dependent membars
            final int lockwordThreadID = encodeCurrentThreadIDForLockword();
            final ThinLockword lockword = ThinLockword.from(ObjectAccess.readMisc(object));
            final ThinLockword asUnlocked = lockword.asUnlocked();
            final ThinLockword asLocked = lockword.asLockedOnceBy(lockwordThreadID);
            final ModalLockword answer = ModalLockword.from(ObjectAccess.compareAndSwapMisc(object, asLocked, asUnlocked));
            if (!answer.equals(asLocked)) {
                slowPathMonitorExit(object, answer, lockwordThreadID);
            }
        }

        @INLINE
        public Word createMisc(Object object) {
            return ThinLockword.unlockedFromHashcode(monitorScheme().createHashCode(object));
        }

        @INLINE
        public int makeHashCode(Object object) {
            if (MaxineVM.isHosted()) {
                return monitorScheme().createHashCode(object);
            }
            final ThinLockword lockword = ThinLockword.from(ObjectAccess.readMisc(object));
            return super.makeHashCode(object, lockword, Platform.target().arch.is64bit() ? ModalLockword.from(Word.zero()) : ModalLockword.from(ObjectAccess.readHash(object)));
        }

        public boolean threadHoldsMonitor(Object object, VmThread thread) {
            final ThinLockword lockword = ThinLockword.from(ObjectAccess.readMisc(object));
            return super.threadHoldsMonitor(object, lockword, thread, encodeCurrentThreadIDForLockword());
        }

        public void monitorNotify(Object object, boolean all) {
            nullCheck(object);
            if (MaxineVM.isHosted()) {
                HostMonitor.notify(object);
                return;
            }
            final ThinLockword lockword = ThinLockword.from(ObjectAccess.readMisc(object));
            super.monitorNotify(object, all, lockword);
        }

        public void monitorWait(Object object, long timeout) throws InterruptedException {
            nullCheck(object);
            if (MaxineVM.isHosted()) {
                HostMonitor.wait(object, timeout);
                return;
            }
            final ThinLockword lockword = ThinLockword.from(ObjectAccess.readMisc(object));
            super.monitorWait(object, timeout, lockword);
        }

        @Override
        public void afterGarbageCollection() {
            super.afterGarbageCollection();
        }

        @Override
        public void beforeGarbageCollection() {
            super.beforeGarbageCollection();
        }

        public int createHash(Object object) {
            int hashCode = ThinLockword.fromHashcode(monitorScheme().createHashCode(object)).getHashcode();
            assert hashCode == monitorScheme().createHashCode(object) : "Failed installation of hashcode!";
            return hashCode;
        }
    }
}
