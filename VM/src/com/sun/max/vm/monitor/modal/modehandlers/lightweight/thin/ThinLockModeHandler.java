/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.monitor.modal.modehandlers.lightweight.thin;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.monitor.modal.modehandlers.*;
import com.sun.max.vm.monitor.modal.modehandlers.AbstractModeHandler.ModeDelegate.DelegatedThreadHoldsMonitorResult;
import com.sun.max.vm.monitor.modal.modehandlers.lightweight.biased.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.thread.*;

/**
 *
 * TODO: For AMD64 and Sparc TSO we don't need any membars. PPC, IA32 and ARM will need some adding to the acquire and release logic.
 *
 * @author Simon Wilkinson
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

    private ModalLockword64 inflate(Object object, ThinLockword64 lockword) {
        ModalLockword64 inflatedLockword = delegate().prepareModalLockword(object, lockword);
        ThinLockword64 thinLockword = lockword;
        while (true) {
            final ModalLockword64 answer = ModalLockword64.from(ObjectAccess.compareAndSwapMisc(object, thinLockword, inflatedLockword));
            if (answer.equals(thinLockword)) {
                break;
            } else if (answer.isInflated()) {
                delegate().cancelPreparedModalLockword(inflatedLockword);
                inflatedLockword = answer;
                break;
            }
            // We will spin if the thin lock word has been changed in any way (new owner, rcount change, new hashcode)
            thinLockword = ThinLockword64.from(answer);
            inflatedLockword = delegate().reprepareModalLockword(inflatedLockword, thinLockword);
        }
        return inflatedLockword;
    }

    protected void slowPathMonitorEnter(Object object, ModalLockword64 lockword, int lockwordThreadID) {
        ModalLockword64 newLockword = lockword;
        int retries = THIN_LOCK_RETRIES;
        while (true) {
            if (ThinLockword64.isThinLockword(newLockword)) {
                final ThinLockword64 thinLockword = ThinLockword64.from(newLockword);
                // Do we own the lock?
                if (thinLockword.getLockOwnerID() == lockwordThreadID) {
                    // Attempt to inc the recursion count
                    if (!thinLockword.countOverflow()) {
                        final ModalLockword64 answer = ModalLockword64.from(ObjectAccess.compareAndSwapMisc(object, thinLockword, thinLockword.incrementCount()));
                        if (answer.equals(thinLockword)) {
                            return;
                        }
                        // An inflation or a new hashcode was installed. Try again
                        newLockword = answer;
                        continue;
                    }
                } else {
                    final ThinLockword64 asUnlocked = thinLockword.asUnlocked();
                    final ThinLockword64 asLocked  = thinLockword.asLockedOnceBy(lockwordThreadID);
                    final ModalLockword64 answer = ModalLockword64.from(ObjectAccess.compareAndSwapMisc(object, asUnlocked, asLocked));
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
            newLockword = ModalLockword64.from(ObjectAccess.readMisc(object));
            retries = THIN_LOCK_RETRIES;
        }
    }

    protected void slowPathMonitorExit(Object object, ModalLockword64 lockword, int lockwordThreadID) {
        if (ThinLockword64.isThinLockword(lockword)) {
            ThinLockword64 thinLockword = ThinLockword64.from(lockword);
            if (thinLockword.countUnderflow() || thinLockword.getLockOwnerID() != lockwordThreadID) {
                throw new IllegalMonitorStateException();
            }
            final boolean isRelease = thinLockword.decrementCount().countUnderflow();
            while (true) {
                ThinLockword64 answer = ThinLockword64.from(Word.zero());
                if (isRelease) {
                    answer = ThinLockword64.from(ObjectAccess.compareAndSwapMisc(object, thinLockword, thinLockword.asUnlocked()));
                } else {
                    answer = ThinLockword64.from(ObjectAccess.compareAndSwapMisc(object, thinLockword, thinLockword.decrementCount()));
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

    protected int makeHashCode(Object object, ModalLockword64 lockword) {
        ModalLockword64 newLockword = lockword;
        int newHashcode = 0;
        while (true) {
            if (ThinLockword64.isThinLockword(newLockword)) {
                final ThinLockword64 thinLockword = ThinLockword64.from(newLockword);
                final int hashcode = thinLockword.getHashcode();
                if (hashcode != 0) {
                    return hashcode;
                }
                if (newHashcode == 0) {
                    newHashcode = monitorScheme().createHashCode(object);
                }
                newLockword = ModalLockword64.from(ObjectAccess.compareAndSwapMisc(object, thinLockword, thinLockword.setHashcode(newHashcode)));
                if (newLockword.equals(thinLockword)) {
                    return newHashcode;
                }
                // Could be another thread beat us to the hashcode, or an inflation. Try again.
                continue;
            }
            newHashcode = delegate().delegateMakeHashcode(object, newLockword);
            if (newHashcode != 0) {
                return newHashcode;
            }
            // Possible deflation. Try again.
            newLockword = ModalLockword64.from(ObjectAccess.readMisc(object));
        }
    }

    protected void monitorNotify(Object object, boolean all, ModalLockword64 lockword) {
        if (ThinLockword64.isThinLockword(lockword)) {
            final ThinLockword64 thinLockword = ThinLockword64.from(lockword);
            if (thinLockword.countUnderflow() || thinLockword.getLockOwnerID() != encodeCurrentThreadIDForLockword()) {
                throw new IllegalMonitorStateException();
            }
            // By lightweight lock semantics we have no threads waiting, so just return.
            return;
        }
        //  We don't have to check for deflation, as either we own the lock or an exception will be thrown
        delegate().delegateMonitorNotify(object, all, lockword);
    }

    protected void monitorWait(Object object, long timeout, ModalLockword64 lockword) throws InterruptedException {
        if (ThinLockword64.isThinLockword(lockword)) {
            final ThinLockword64 thinLockword = ThinLockword64.from(lockword);
            if (thinLockword.countUnderflow() || thinLockword.getLockOwnerID() != encodeCurrentThreadIDForLockword()) {
                throw new IllegalMonitorStateException();
            }
            final ModalLockword64 newLockword = inflate(object, thinLockword);
            // We don't have to check for deflation as we own the lock
            delegate().delegateMonitorWait(object, timeout, newLockword);
            return;
        }
        // We don't have to check for deflation, as either we own the lock or an exception will be thrown
        delegate().delegateMonitorWait(object, timeout, lockword);
    }

    protected boolean threadHoldsMonitor(Object object, ModalLockword64 lockword, VmThread thread, int threadID) {
        ModalLockword64 newLockword = lockword;
        while (true) {
            if (ThinLockword64.isThinLockword(newLockword)) {
                final ThinLockword64 thinLockword = ThinLockword64.from(newLockword);
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
            newLockword = ModalLockword64.from(ObjectAccess.readMisc(object));
        }
    }

    protected void afterGarbageCollection() {
        delegate().delegateAfterGarbageCollection();
    }

    protected void beforeGarbageCollection() {
        delegate().delegateBeforeGarbageCollection();
    }

    // Inspector support
    public static int decodeLockOwnerThreadID(ThinLockword64 thinLockword) {
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
        }

        public boolean delegateMonitorEnter(Object object, ModalLockword64 lockword, int lockwordThreadID) {
            final ThinLockword64 thinLockword = ThinLockword64.from(lockword);
            final ThinLockword64 asUnlocked = thinLockword.asUnlocked();
            final ThinLockword64 asLocked  = thinLockword.asLockedOnceBy(lockwordThreadID);
            final ModalLockword64 answer = ModalLockword64.from(ObjectAccess.compareAndSwapMisc(object, asUnlocked, asLocked));
            if (!answer.equals(asUnlocked)) {
                slowPathMonitorEnter(object, answer, lockwordThreadID);
            }
            return true;
        }

        public void delegateMonitorExit(Object object, ModalLockword64 lockword) {
            final int lockwordThreadID = encodeCurrentThreadIDForLockword();
            final ThinLockword64 thinLockword = ThinLockword64.from(lockword);
            final ThinLockword64 asUnlocked = thinLockword.asUnlocked();
            final ThinLockword64 asLocked  = thinLockword.asLockedOnceBy(lockwordThreadID);
            final ModalLockword64 answer = ModalLockword64.from(ObjectAccess.compareAndSwapMisc(object, asLocked, asUnlocked));
            if (!answer.equals(asLocked)) {
                slowPathMonitorExit(object, answer, lockwordThreadID);
            }
        }

        public void delegateMonitorNotify(Object object, boolean all, ModalLockword64 lockword) {
            monitorNotify(object, all, ThinLockword64.from(lockword));
        }

        public void delegateMonitorWait(Object object, long timeout, ModalLockword64 lockword) throws InterruptedException {
            monitorWait(object, timeout, ThinLockword64.from(lockword));
        }

        public DelegatedThreadHoldsMonitorResult delegateThreadHoldsMonitor(Object object, ModalLockword64 lockword, VmThread thread, int threadID) {
            return threadHoldsMonitor(object, ThinLockword64.from(lockword), thread, threadID) ? DelegatedThreadHoldsMonitorResult.TRUE : DelegatedThreadHoldsMonitorResult.FALSE;
        }

        public int delegateMakeHashcode(Object object, ModalLockword64 lockword) {
            return makeHashCode(object, ThinLockword64.from(lockword));
        }

        public void delegateAfterGarbageCollection() {
            afterGarbageCollection();
        }

        public void delegateBeforeGarbageCollection() {
            beforeGarbageCollection();
        }

        public ModalLockword64 prepareModalLockword(Object object, ModalLockword64 currentLockword) {
            final BiasedLockword64 biasedLockword = BiasedLockword64.from(currentLockword);
            ThinLockword64 thinLockword = ThinLockword64.from(biasedLockword.asUnbiasable());
            if (biasedLockword.countUnderflow()) {
                // The lock was not held, only biased, so remove the threadID too.
                thinLockword = thinLockword.asUnlocked();
            }
            return thinLockword;
        }

        public ModalLockword64 reprepareModalLockword(ModalLockword64 preparedLockword, ModalLockword64 currentLockword) {
            return prepareModalLockword(null, currentLockword);
        }

        public void cancelPreparedModalLockword(ModalLockword64 preparedLockword) {
            // Nothing to do
        }
    }

    /**
     * Implements fast-path monitor entry and exit for a ThinLockModeHandler.
     */
    private static final class FastPath extends ThinLockModeHandler implements MonitorSchemeEntry {

        protected FastPath(ModeDelegate delegate) {
            super(delegate);
        }

        public void monitorEnter(Object object) {
            nullCheck(object);
            if (MaxineVM.isHosted()) {
                HostMonitor.enter(object);
                return;
            }
            // Fast path monitor enter.
            // TODO: Arch dependent membars
            final int lockwordThreadID = encodeCurrentThreadIDForLockword();
            final ThinLockword64 lockword = ThinLockword64.from(ObjectAccess.readMisc(object));
            final ThinLockword64 asUnlocked = lockword.asUnlocked();
            final ThinLockword64 asLocked = lockword.asLockedOnceBy(lockwordThreadID);
            final ModalLockword64 answer = ModalLockword64.from(ObjectAccess.compareAndSwapMisc(object, asUnlocked, asLocked));
            if (!answer.equals(asUnlocked)) {
                slowPathMonitorEnter(object, answer, lockwordThreadID);
            }
        }

        public void monitorExit(Object object) {
            nullCheck(object);
            if (MaxineVM.isHosted()) {
                HostMonitor.exit(object);
                return;
            }
            // Fast path monitor exit.
            // TODO: Arch dependent membars
            final int lockwordThreadID = encodeCurrentThreadIDForLockword();
            final ThinLockword64 lockword = ThinLockword64.from(ObjectAccess.readMisc(object));
            final ThinLockword64 asUnlocked = lockword.asUnlocked();
            final ThinLockword64 asLocked  = lockword.asLockedOnceBy(lockwordThreadID);
            final ModalLockword64 answer = ModalLockword64.from(ObjectAccess.compareAndSwapMisc(object, asLocked, asUnlocked));
            if (!answer.equals(asLocked)) {
                slowPathMonitorExit(object, answer, lockwordThreadID);
            }
        }

        public Word createMisc(Object object) {
            return ThinLockword64.unlockedFromHashcode(monitorScheme().createHashCode(object));
        }

        public int makeHashCode(Object object) {
            nullCheck(object);
            if (MaxineVM.isHosted()) {
                return monitorScheme().createHashCode(object);
            }
            final ThinLockword64 lockword = ThinLockword64.from(ObjectAccess.readMisc(object));
            return super.makeHashCode(object, lockword);
        }

        public boolean threadHoldsMonitor(Object object, VmThread thread) {
            nullCheck(object);
            final ThinLockword64 lockword = ThinLockword64.from(ObjectAccess.readMisc(object));
            return super.threadHoldsMonitor(object, lockword, thread, encodeCurrentThreadIDForLockword());
        }

        public void monitorNotify(Object object, boolean all) {
            nullCheck(object);
            if (MaxineVM.isHosted()) {
                HostMonitor.notify(object);
                return;
            }
            final ThinLockword64 lockword = ThinLockword64.from(ObjectAccess.readMisc(object));
            super.monitorNotify(object, all, lockword);
        }

        public void monitorWait(Object object, long timeout) throws InterruptedException {
            nullCheck(object);
            if (MaxineVM.isHosted()) {
                HostMonitor.wait(object, timeout);
                return;
            }
            final ThinLockword64 lockword = ThinLockword64.from(ObjectAccess.readMisc(object));
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
    }
}

