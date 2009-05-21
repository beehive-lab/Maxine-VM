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
package com.sun.max.vm.monitor.modal.modehandlers.lightweight.thin;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.monitor.modal.modehandlers.*;
import com.sun.max.vm.monitor.modal.modehandlers.lightweight.biased.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.object.host.*;
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

    private ModalLockWord64 inflate(Object object, ThinLockWord64 lockWord) {
        ModalLockWord64 inflatedLockWord = delegate().prepareModalLockWord(object, lockWord);
        ThinLockWord64 thinLockWord = lockWord;
        while (true) {
            final ModalLockWord64 answer = ModalLockWord64.from(ObjectAccess.compareAndSwapMisc(object, thinLockWord, inflatedLockWord));
            if (answer.equals(thinLockWord)) {
                break;
            } else if (answer.isInflated()) {
                delegate().cancelPreparedModalLockWord(inflatedLockWord);
                inflatedLockWord = answer;
                break;
            }
            // We will spin if the thin lock word has been changed in any way (new owner, rcount change, new hashcode)
            thinLockWord = ThinLockWord64.from(answer);
            inflatedLockWord = delegate().rePrepareModalLockWord(inflatedLockWord, thinLockWord);
        }
        return inflatedLockWord;
    }

    protected void slowPathMonitorEnter(Object object, ModalLockWord64 lockWord, int lockwordThreadID) {
        ModalLockWord64 newLockWord = lockWord;
        int retries = THIN_LOCK_RETRIES;
        while (true) {
            if (ThinLockWord64.isThinLockWord(newLockWord)) {
                final ThinLockWord64 thinLockWord = ThinLockWord64.from(newLockWord);
                // Do we own the lock?
                if (thinLockWord.getLockOwnerID() == lockwordThreadID) {
                    // Attempt to inc the recursion count
                    if (!thinLockWord.countOverflow()) {
                        final ModalLockWord64 answer = ModalLockWord64.from(ObjectAccess.compareAndSwapMisc(object, thinLockWord, thinLockWord.incrementCount()));
                        if (answer.equals(thinLockWord)) {
                            return;
                        }
                        // An inflation or a new hashcode was installed. Try again
                        newLockWord = answer;
                        continue;
                    }
                } else {
                    final ThinLockWord64 asUnlocked = thinLockWord.asUnlocked();
                    final ThinLockWord64 asLocked  = thinLockWord.asLockedOnceBy(lockwordThreadID);
                    final ModalLockWord64 answer = ModalLockWord64.from(ObjectAccess.compareAndSwapMisc(object, asUnlocked, asLocked));
                    if (answer.equals(asUnlocked)) {
                        // The current thread got the lock
                        return;
                    }
                    // This could be a hashcode, inflation or another thread got the lock.
                    // Lets try again.
                    newLockWord = answer;
                    if (--retries > 0) {
                        continue;
                    }
                }
                // Count overflow or too much contention - inflate
                newLockWord = inflate(object, thinLockWord);
            }

            // The monitor is inflated
            if (delegate().delegateMonitorEnter(object, newLockWord, lockwordThreadID)) {
                return;
            }

            // Try again. Monitor was deflated.
            newLockWord = ModalLockWord64.from(ObjectAccess.readMisc(object));
            retries = THIN_LOCK_RETRIES;
        }
    }

    protected void slowPathMonitorExit(Object object, ModalLockWord64 lockWord, int lockwordThreadID) {
        if (ThinLockWord64.isThinLockWord(lockWord)) {
            ThinLockWord64 thinLockWord = ThinLockWord64.from(lockWord);
            if (thinLockWord.countUnderflow() || thinLockWord.getLockOwnerID() != lockwordThreadID) {
                throw new IllegalMonitorStateException();
            }
            final boolean isRelease = thinLockWord.decrementCount().countUnderflow();
            while (true) {
                ThinLockWord64 answer = ThinLockWord64.from(Word.zero());
                if (isRelease) {
                    answer = ThinLockWord64.from(ObjectAccess.compareAndSwapMisc(object, thinLockWord, thinLockWord.asUnlocked()));
                } else {
                    answer = ThinLockWord64.from(ObjectAccess.compareAndSwapMisc(object, thinLockWord, thinLockWord.decrementCount()));
                }
                if (answer.equals(thinLockWord)) {
                    return;
                } else if (answer.isInflated()) {
                    // We don't have to check for deflation, as this cannot happen while we own the lock.
                    delegate().delegateMonitorExit(object, answer);
                    return;
                }
                // A hashcode was installed. Try again.
                thinLockWord = answer;
            }
        }
        // We don't have to check for deflation, as this cannot happen while we own the lock.
        delegate().delegateMonitorExit(object, lockWord);
    }

    protected int makeHashCode(Object object, ModalLockWord64 lockWord) {
        ModalLockWord64 newLockWord = lockWord;
        int newHashcode = 0;
        while (true) {
            if (ThinLockWord64.isThinLockWord(newLockWord)) {
                final ThinLockWord64 thinLockWord = ThinLockWord64.from(newLockWord);
                final int hashcode = thinLockWord.getHashcode();
                if (hashcode != 0) {
                    return hashcode;
                }
                if (newHashcode == 0) {
                    newHashcode = monitorScheme().createHashCode(object);
                }
                newLockWord = ModalLockWord64.from(ObjectAccess.compareAndSwapMisc(object, thinLockWord, thinLockWord.setHashcode(newHashcode)));
                if (newLockWord.equals(thinLockWord)) {
                    return newHashcode;
                }
                // Could be another thread beat us to the hashcode, or an inflation. Try again.
                continue;
            }
            newHashcode = delegate().delegateMakeHashcode(object, newLockWord);
            if (newHashcode != 0) {
                return newHashcode;
            }
            // Possible deflation. Try again.
            newLockWord = ModalLockWord64.from(ObjectAccess.readMisc(object));
        }
    }

    protected void monitorNotify(Object object, boolean all, ModalLockWord64 lockWord) {
        if (ThinLockWord64.isThinLockWord(lockWord)) {
            final ThinLockWord64 thinLockWord = ThinLockWord64.from(lockWord);
            if (thinLockWord.countUnderflow() || thinLockWord.getLockOwnerID() != encodeCurrentThreadIDForLockword()) {
                throw new IllegalMonitorStateException();
            }
            // By lightweight lock semantics we have no threads waiting, so just return.
            return;
        }
        //  We don't have to check for deflation, as either we own the lock or an exception will be thrown
        delegate().delegateMonitorNotify(object, all, lockWord);
    }

    protected void monitorWait(Object object, long timeout, ModalLockWord64 lockWord) throws InterruptedException {
        if (ThinLockWord64.isThinLockWord(lockWord)) {
            final ThinLockWord64 thinLockWord = ThinLockWord64.from(lockWord);
            if (thinLockWord.countUnderflow() || thinLockWord.getLockOwnerID() != encodeCurrentThreadIDForLockword()) {
                throw new IllegalMonitorStateException();
            }
            final ModalLockWord64 newLockWord = inflate(object, thinLockWord);
            // We don't have to check for deflation as we own the lock
            delegate().delegateMonitorWait(object, timeout, newLockWord);
            return;
        }
        // We don't have to check for deflation, as either we own the lock or an exception will be thrown
        delegate().delegateMonitorWait(object, timeout, lockWord);
    }

    private final boolean[] _threadHoldsMonitorResult = new boolean[1];

    protected boolean threadHoldsMonitor(Object object, ModalLockWord64 lockWord, VmThread thread, int threadID) {
        ModalLockWord64 newLockWord = lockWord;
        while (true) {
            if (ThinLockWord64.isThinLockWord(newLockWord)) {
                final ThinLockWord64 thinLockWord = ThinLockWord64.from(newLockWord);
                return !thinLockWord.countUnderflow() && thinLockWord.getLockOwnerID() == threadID;
            }
            if (delegate().delegateThreadHoldsMonitor(object, lockWord, thread, threadID, _threadHoldsMonitorResult)) {
                return _threadHoldsMonitorResult[0];
            }
            // Deflation. Try again.
            newLockWord = ModalLockWord64.from(ObjectAccess.readMisc(object));
        }
    }

    protected void afterGarbageCollection() {
        delegate().delegateAfterGarbageCollection();
    }

    protected void beforeGarbageCollection() {
        delegate().delegateBeforeGarbageCollection();
    }

    // Inspector support
    public static int decodeLockOwnerThreadID(ThinLockWord64 thinLockWord) {
        if (thinLockWord.equals(thinLockWord.asUnlocked())) {
            return -1;
        }
        return decodeLockwordThreadID(thinLockWord.getLockOwnerID());
    }

    /**
     * Implements the required interface to allow a ThinLockModeHandler to acts as a delegate to biased locking.
     */
    private static final class BiasedLockDelegate extends ThinLockModeHandler implements ModeDelegate {

        protected BiasedLockDelegate(ModeDelegate delegate) {
            super(delegate);
        }

        @Override
        public boolean delegateMonitorEnter(Object object, ModalLockWord64 lockWord, int lockwordThreadID) {
            final ThinLockWord64 thinLockWord = ThinLockWord64.from(lockWord);
            final ThinLockWord64 asUnlocked = thinLockWord.asUnlocked();
            final ThinLockWord64 asLocked  = thinLockWord.asLockedOnceBy(lockwordThreadID);
            final ModalLockWord64 answer = ModalLockWord64.from(ObjectAccess.compareAndSwapMisc(object, asUnlocked, asLocked));
            if (!answer.equals(asUnlocked)) {
                slowPathMonitorEnter(object, answer, lockwordThreadID);
            }
            return true;
        }

        @Override
        public void delegateMonitorExit(Object object, ModalLockWord64 lockWord) {
            final int lockwordThreadID = encodeCurrentThreadIDForLockword();
            final ThinLockWord64 thinLockWord = ThinLockWord64.from(lockWord);
            final ThinLockWord64 asUnlocked = thinLockWord.asUnlocked();
            final ThinLockWord64 asLocked  = thinLockWord.asLockedOnceBy(lockwordThreadID);
            final ModalLockWord64 answer = ModalLockWord64.from(ObjectAccess.compareAndSwapMisc(object, asLocked, asUnlocked));
            if (!answer.equals(asLocked)) {
                slowPathMonitorExit(object, answer, lockwordThreadID);
            }
        }

        @Override
        public void delegateMonitorNotify(Object object, boolean all, ModalLockWord64 lockWord) {
            monitorNotify(object, all, ThinLockWord64.from(lockWord));
        }

        @Override
        public void delegateMonitorWait(Object object, long timeout, ModalLockWord64 lockWord) throws InterruptedException {
            monitorWait(object, timeout, ThinLockWord64.from(lockWord));
        }

        @Override
        public boolean delegateThreadHoldsMonitor(Object object, ModalLockWord64 lockWord, VmThread thread, int threadID, boolean[] result) {
            result[0] = threadHoldsMonitor(object, ThinLockWord64.from(lockWord), thread, threadID);
            return true;
        }

        @Override
        public int delegateMakeHashcode(Object object, ModalLockWord64 lockWord) {
            return makeHashCode(object, ThinLockWord64.from(lockWord));
        }

        @Override
        public void delegateAfterGarbageCollection() {
            afterGarbageCollection();
        }

        @Override
        public void delegateBeforeGarbageCollection() {
            beforeGarbageCollection();
        }

        @Override
        public ModalLockWord64 prepareModalLockWord(Object object, ModalLockWord64 currentlockWord) {
            final BiasedLockWord64 biasedLockWord = BiasedLockWord64.from(currentlockWord);
            ThinLockWord64 thinLockWord = ThinLockWord64.from(biasedLockWord.asUnbiasable());
            if (biasedLockWord.countUnderflow()) {
                // The lock was not held, only biased, so remove the threadID too.
                thinLockWord = thinLockWord.asUnlocked();
            }
            return thinLockWord;
        }

        @Override
        public ModalLockWord64 rePrepareModalLockWord(ModalLockWord64 preparedLockWord, ModalLockWord64 currentlockWord) {
            return prepareModalLockWord(null, currentlockWord);
        }

        @Override
        public void cancelPreparedModalLockWord(ModalLockWord64 preparedLockWord) {
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

        @Override
        public void monitorEnter(Object object) {
            nullCheck(object);
            if (MaxineVM.isPrototyping()) {
                HostMonitor.enter(object);
                return;
            }
            // Fast path monitor enter.
            // TODO: Arch dependent membars
            final int lockwordThreadID = encodeCurrentThreadIDForLockword();
            final ThinLockWord64 lockWord = ThinLockWord64.from(ObjectAccess.readMisc(object));
            final ThinLockWord64 asUnlocked = lockWord.asUnlocked();
            final ThinLockWord64 asLocked  = lockWord.asLockedOnceBy(lockwordThreadID);
            final ModalLockWord64 answer = ModalLockWord64.from(ObjectAccess.compareAndSwapMisc(object, asUnlocked, asLocked));
            if (!answer.equals(asUnlocked)) {
                slowPathMonitorEnter(object, answer, lockwordThreadID);
            }
        }

        @Override
        public void monitorExit(Object object) {
            nullCheck(object);
            if (MaxineVM.isPrototyping()) {
                HostMonitor.exit(object);
                return;
            }
            // Fast path monitor exit.
            // TODO: Arch dependent membars
            final int lockwordThreadID = encodeCurrentThreadIDForLockword();
            final ThinLockWord64 lockWord = ThinLockWord64.from(ObjectAccess.readMisc(object));
            final ThinLockWord64 asUnlocked = lockWord.asUnlocked();
            final ThinLockWord64 asLocked  = lockWord.asLockedOnceBy(lockwordThreadID);
            final ModalLockWord64 answer = ModalLockWord64.from(ObjectAccess.compareAndSwapMisc(object, asLocked, asUnlocked));
            if (!answer.equals(asLocked)) {
                slowPathMonitorExit(object, answer, lockwordThreadID);
            }
        }

        @Override
        public Word createMisc(Object object) {
            return ThinLockWord64.unlockedFromHashcode(monitorScheme().createHashCode(object));
        }

        @Override
        public int makeHashCode(Object object) {
            nullCheck(object);
            if (MaxineVM.isPrototyping()) {
                return monitorScheme().createHashCode(object);
            }
            final ThinLockWord64 lockWord = ThinLockWord64.from(ObjectAccess.readMisc(object));
            return super.makeHashCode(object, lockWord);
        }

        @Override
        public boolean threadHoldsMonitor(Object object, VmThread thread) {
            nullCheck(object);
            final ThinLockWord64 lockWord = ThinLockWord64.from(ObjectAccess.readMisc(object));
            return super.threadHoldsMonitor(object, lockWord, thread, encodeCurrentThreadIDForLockword());
        }

        @Override
        public void monitorNotify(Object object, boolean all) {
            nullCheck(object);
            if (MaxineVM.isPrototyping()) {
                HostMonitor.notify(object);
                return;
            }
            final ThinLockWord64 lockWord = ThinLockWord64.from(ObjectAccess.readMisc(object));
            super.monitorNotify(object, all, lockWord);
        }

        @Override
        public void monitorWait(Object object, long timeout) throws InterruptedException {
            nullCheck(object);
            if (MaxineVM.isPrototyping()) {
                HostMonitor.wait(object, timeout);
                return;
            }
            final ThinLockWord64 lockWord = ThinLockWord64.from(ObjectAccess.readMisc(object));
            super.monitorWait(object, timeout, lockWord);
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


