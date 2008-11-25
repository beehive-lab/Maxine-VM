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
package com.sun.max.vm.monitor.modal.modehandlers.inflated;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.monitor.modal.modehandlers.*;
import com.sun.max.vm.monitor.modal.modehandlers.lightweight.biased.*;
import com.sun.max.vm.monitor.modal.modehandlers.lightweight.thin.*;
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.monitor.modal.sync.JavaMonitorManager.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.thread.*;

/**
 * Inflated mode handler for ModalMonitorSchemes.
 *
 * @author Simon Wilkinson
 */
public abstract class InflatedMonitorModeHandler extends AbstractModeHandler {

    protected InflatedMonitorModeHandler(UnboundMiscWordWriter unboundMiscWordWriter) {
        super(null);
        JavaMonitorManager.registerMonitorUnbinder(unboundMiscWordWriter);
    }

    /**
     * Returns an InflatedMonitorModeHandler with the required interface for fast-path entry from a MonitorScheme.
     */
    public static MonitorSchemeEntry asFastPath() {
        return new InflatedMonitorModeHandler.FastPath();
    }

    /**
     * Returns an InflatedMonitorModeHandler with the required interface to act as delegate for biased locking.
     */
    public static ModeDelegate asBiasedLockDelegate() {
        return new InflatedMonitorModeHandler.BiasedLockDelegate();
    }

    /**
     * Returns an InflatedMonitorModeHandler with the required interface to act as delegate for thin locking.
     */
    public static ModeDelegate asThinLockDelegate() {
        return new InflatedMonitorModeHandler.ThinLockDelegate();
    }

    @INLINE
    protected final InflatedMonitorLockWord64 readMiscAndProtectBinding(Object object) {
        final InflatedMonitorLockWord64 lockWord = InflatedMonitorLockWord64.as(ObjectAccess.readMisc(object));
        if (lockWord.isBound()) {
            JavaMonitorManager.protectBinding(lockWord.getBoundMonitor());
        }
        return lockWord;
    }

    protected int makeBoundHashCode(Object object, InflatedMonitorLockWord64 lockWord) {
        final JavaMonitor monitor = lockWord.getBoundMonitor();
        final HashableLockWord64 swappedLockword = HashableLockWord64.as(monitor.displacedMisc());
        int hashcode = swappedLockword.getHashcode();
        if (hashcode == 0) {
            hashcode = monitorScheme().createHashCode(object);
            final HashableLockWord64 newSwappedLockword = swappedLockword.setHashcode(hashcode);
            final HashableLockWord64 answer = HashableLockWord64.as(monitor.compareAndSwapDisplacedMisc(swappedLockword, newSwappedLockword));
            if (!answer.equals(swappedLockword)) {
                hashcode = answer.getHashcode();
            }
        }
        return hashcode;
    }

    protected void monitorExit(Object object, InflatedMonitorLockWord64 lockWord) {
        if (!lockWord.isBound()) {
            throw new IllegalMonitorStateException();
        }
        final JavaMonitor monitor = lockWord.getBoundMonitor();
        monitor.monitorExit();
    }

    protected void monitorNotify(Object object, boolean all, InflatedMonitorLockWord64 lockWord) {
        if (!lockWord.isBound()) {
            throw new IllegalMonitorStateException();
        }
        final JavaMonitor monitor = lockWord.getBoundMonitor();
        monitor.monitorNotify(all);
    }

    protected void monitorWait(Object object, long timeout, InflatedMonitorLockWord64 lockWord) throws InterruptedException {
        if (!lockWord.isBound()) {
            throw new IllegalMonitorStateException();
        }
        final JavaMonitor monitor = lockWord.getBoundMonitor();
        monitor.monitorWait(timeout);
    }

    protected boolean threadHoldsMonitor(InflatedMonitorLockWord64 lockWord, VmThread thread) {
        return lockWord.isBound() && lockWord.getBoundMonitor().isOwnedBy(thread);
    }

    protected void afterGarbageCollection() {
        JavaMonitorManager.afterGarbageCollection();
    }

    protected void beforeGarbageCollection() {
        JavaMonitorManager.beforeGarbageCollection();
    }

    /**
     * Implements fast-path monitor entry and exit for an InflatedMonitorModeHandler.
     */
    private static final class FastPath extends InflatedMonitorModeHandler implements MonitorSchemeEntry {

        protected FastPath() {
            super(new UnboundMiscWordWriter() {
                @Override
                public void writeUnboundMiscWord(Object object, Word preBindingMiscWord) {
                    ObjectAccess.writeMisc(object, preBindingMiscWord);
                }
            });
        }

        @Override
        public void initialize(MaxineVM.Phase phase) {
            if (phase == MaxineVM.Phase.PROTOTYPING) {
                JavaMonitorManager.setRequireProxyAcquirableMonitors(false);
            }
        }

        @Override
        public Word createMisc(Object object) {
            return InflatedMonitorLockWord64.unboundFromHashcode(monitorScheme().createHashCode(object));
        }

        @Override
        public int makeHashCode(Object object) {
            nullCheck(object);
            if (MaxineVM.isPrototyping()) {
                return monitorScheme().createHashCode(object);
            }
            final InflatedMonitorLockWord64 lockWord = readMiscAndProtectBinding(object);
            if (lockWord.isBound()) {
                return makeBoundHashCode(object, lockWord);
            }
            int hashcode = lockWord.getHashcode();
            if (hashcode == 0) {
                hashcode = monitorScheme().createHashCode(object);
                final HashableLockWord64 newLockword = lockWord.setHashcode(hashcode);
                final Word answer = ObjectAccess.compareAndSwapMisc(object, lockWord, newLockword);
                if (!answer.equals(lockWord)) {
                    return makeHashCode(object);
                }
            }
            return hashcode;
        }

        @Override
        public void monitorEnter(Object object) {
            nullCheck(object);
            if (MaxineVM.isPrototyping()) {
                HostMonitor.enter(object);
                return;
            }
            InflatedMonitorLockWord64 lockWord = readMiscAndProtectBinding(object);
            JavaMonitor monitor = null;
            while (true) {
                if (lockWord.isBound()) {
                    if (monitor != null) {
                        JavaMonitorManager.unbindMonitor(monitor);
                    }
                    final JavaMonitor boundMonitor = lockWord.getBoundMonitor();
                    boundMonitor.monitorEnter();
                    return;
                } else if (monitor == null) {
                    monitor = JavaMonitorManager.bindMonitor(object);
                    monitor.monitorEnter();
                }
                monitor.setDisplacedMisc(lockWord);
                final InflatedMonitorLockWord64 newLockWord = InflatedMonitorLockWord64.boundFromMonitor(monitor);
                final Word answer = ObjectAccess.compareAndSwapMisc(object, lockWord, newLockWord);
                if (answer.equals(lockWord)) {
                    return;
                }
                // Another thread installed a hashcode or got the monitor.
                // Try again.
                lockWord = InflatedMonitorLockWord64.as(answer);
            }
        }

        @Override
        public void monitorExit(Object object) {
            nullCheck(object);
            if (MaxineVM.isPrototyping()) {
                HostMonitor.exit(object);
                return;
            }
            final InflatedMonitorLockWord64 lockWord = InflatedMonitorLockWord64.as(ObjectAccess.readMisc(object));
            super.monitorExit(object, lockWord);
        }

        @Override
        public void monitorNotify(Object object, boolean all) {
            nullCheck(object);
            if (MaxineVM.isPrototyping()) {
                HostMonitor.notify(object);
                return;
            }
            final InflatedMonitorLockWord64 lockWord = InflatedMonitorLockWord64.as(ObjectAccess.readMisc(object));
            super.monitorNotify(object, all, lockWord);
        }

        @Override
        public void monitorWait(Object object, long timeout) throws InterruptedException {
            nullCheck(object);
            if (MaxineVM.isPrototyping()) {
                HostMonitor.wait(object, timeout);
                return;
            }
            final InflatedMonitorLockWord64 lockWord = readMiscAndProtectBinding(object);
            super.monitorWait(object, timeout, lockWord);
        }

        @Override
        public boolean threadHoldsMonitor(Object object, VmThread thread) {
            nullCheck(object);
            final InflatedMonitorLockWord64 lockWord = readMiscAndProtectBinding(object);
            return super.threadHoldsMonitor(lockWord, thread);
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

    /**
     * Implements default delegate entry points for InflatedMonitorModeHandler.
     */
    private abstract static class Delegate extends InflatedMonitorModeHandler implements ModeDelegate {

        private Delegate(UnboundMiscWordWriter unboundMiscWordWriter) {
            super(unboundMiscWordWriter);
        }

        @Override
        public int delegateMakeHashcode(Object object, ModalLockWord64 lockWord) {
            final InflatedMonitorLockWord64 inflatedlockWord = readMiscAndProtectBinding(object);
            if (inflatedlockWord.isBound()) {
                return makeBoundHashCode(object, inflatedlockWord);
            }
            return 0;
        }

        @Override
        public boolean delegateMonitorEnter(Object object, ModalLockWord64 lockWord, int lockwordThreadID) {
            final InflatedMonitorLockWord64 inflatedlockWord = readMiscAndProtectBinding(object);
            if (!inflatedlockWord.isBound()) {
                return false;
            }
            final JavaMonitor monitor = inflatedlockWord.getBoundMonitor();
            monitor.monitorEnter();
            return true;
        }

        @Override
        public void delegateMonitorExit(Object object, ModalLockWord64 lockWord) {
            monitorExit(object, InflatedMonitorLockWord64.as(lockWord));
        }

        @Override
        public void delegateMonitorNotify(Object object, boolean all, ModalLockWord64 lockWord) {
            monitorNotify(object, all, InflatedMonitorLockWord64.as(lockWord));
        }

        @Override
        public void delegateMonitorWait(Object object, long timeout, ModalLockWord64 lockWord) throws InterruptedException {
            final InflatedMonitorLockWord64 inflatedLockWord = InflatedMonitorLockWord64.as(lockWord);
            if (inflatedLockWord.isBound()) {
                JavaMonitorManager.protectBinding(inflatedLockWord.getBoundMonitor());
            }
            monitorWait(object, timeout, inflatedLockWord);
        }

        @Override
        public boolean delegateThreadHoldsMonitor(Object object, ModalLockWord64 lockWord, VmThread thread, int lockwordThreadID, boolean[] result) {
            final InflatedMonitorLockWord64 inflatedlockWord = readMiscAndProtectBinding(object);
            if (!inflatedlockWord.isBound()) {
                return false;
            }
            result[0] = super.threadHoldsMonitor(inflatedlockWord, thread);
            return true;
        }

        @Override
        public void delegateAfterGarbageCollection() {
            afterGarbageCollection();
        }

        @Override
        public void delegateBeforeGarbageCollection() {
            beforeGarbageCollection();
        }
    }

    /**
     * Implements the required interface to allow an InflatedMonitorModeHandler to acts as a delegate to thin locking.
     */
    private static final class ThinLockDelegate extends Delegate implements ModeDelegate {

        private ThinLockDelegate() {
            super(new UnboundMiscWordWriter() {
                @Override
                public void writeUnboundMiscWord(Object object, Word preBindingMiscWord) {
                    ObjectAccess.writeMisc(object, ThinLockWord64.as(preBindingMiscWord).asUnlocked());
                }
            });
        }

        @Override
        public void initialize(MaxineVM.Phase phase) {
            if (phase == MaxineVM.Phase.PROTOTYPING) {
                JavaMonitorManager.setRequireProxyAcquirableMonitors(true);
            }
        }

        @Override
        public ModalLockWord64 prepareModalLockWord(Object object, ModalLockWord64 currentlockWord) {
            final ThinLockWord64 thinLockWord =  ThinLockWord64.as(currentlockWord);
            // FIXME: We have to map the thinLockWord threadID back to the VmThread. This is expensive -
            // and has to lock VMThread.ACTIVE. Maybe we should just keep threadID's in Monitor's too?
            // FIXME: What if the VmThread is null?
            final JavaMonitor monitor = JavaMonitorManager.bindMonitor(object);
            monitor.setDisplacedMisc(thinLockWord);
            if (!thinLockWord.equals(thinLockWord.asUnlocked())) {
                final VmThread owner = VmThreadMap.ACTIVE.getVmThreadForID(decodeLockwordThreadID(thinLockWord.getLockOwnerID()));
                monitor.monitorPrivateAcquire(owner, thinLockWord.getRecursionCount());
            } else {
                monitor.monitorPrivateRelease();
            }
            return InflatedMonitorLockWord64.boundFromMonitor(monitor);
        }

        @Override
        public ModalLockWord64 rePrepareModalLockWord(ModalLockWord64 preparedLockWord, ModalLockWord64 currentlockWord) {
            final ThinLockWord64 thinLockWord =  ThinLockWord64.as(currentlockWord);
            final JavaMonitor monitor = InflatedMonitorLockWord64.as(preparedLockWord).getBoundMonitor();
            monitor.setDisplacedMisc(thinLockWord);
            if (!thinLockWord.equals(thinLockWord.asUnlocked())) {
                final VmThread owner = VmThreadMap.ACTIVE.getVmThreadForID(decodeLockwordThreadID(thinLockWord.getLockOwnerID()));
                monitor.monitorPrivateAcquire(owner, thinLockWord.getRecursionCount());
            } else {
                monitor.monitorPrivateRelease();
            }
            return preparedLockWord;
        }

        @Override
        public void cancelPreparedModalLockWord(ModalLockWord64 preparedLockWord) {
            JavaMonitorManager.unbindMonitor(InflatedMonitorLockWord64.as(preparedLockWord).getBoundMonitor());
        }
    }

    /**
     * Implements the required interface to allow an InflatedMonitorModeHandler to acts as a delegate to biased locking.
     */
    private static final class BiasedLockDelegate extends Delegate {

        protected BiasedLockDelegate() {
            super(new UnboundMiscWordWriter() {
                @Override
                public void writeUnboundMiscWord(Object object, Word preBindingMiscWord) {
                    ObjectAccess.writeMisc(object, BiasedLockWord64.as(preBindingMiscWord).asAnonBiased());
                }
            });
        }

        @Override
        public void initialize(MaxineVM.Phase phase) {
            if (phase == MaxineVM.Phase.PROTOTYPING) {
                JavaMonitorManager.setRequireProxyAcquirableMonitors(true);
            }
        }

        @Override
        public ModalLockWord64 prepareModalLockWord(Object object, ModalLockWord64 currentlockWord) {
            final BiasedLockWord64 biasedLockWord = BiasedLockWord64.as(currentlockWord);
            final JavaMonitor monitor = JavaMonitorManager.bindMonitor(object);
            final InflatedMonitorLockWord64 newLockWord = InflatedMonitorLockWord64.boundFromMonitor(monitor);
            monitor.setDisplacedMisc(biasedLockWord);
            if (!biasedLockWord.countUnderflow()) {
                // It does not matter if the threadID has been recycled after the bias request.
                final VmThread biasOwner = VmThreadMap.ACTIVE.getVmThreadForID(decodeLockwordThreadID(biasedLockWord.getBiasOwnerID()));
                monitor.monitorPrivateAcquire(biasOwner, biasedLockWord.getRecursionCount());
            } else {
                monitor.monitorPrivateRelease();
            }
            return newLockWord;
        }

        @Override
        public ModalLockWord64 rePrepareModalLockWord(ModalLockWord64 preparedLockWord, ModalLockWord64 currentlockWord) {
            final BiasedLockWord64 biasedLockWord =  BiasedLockWord64.as(currentlockWord);
            final JavaMonitor monitor = InflatedMonitorLockWord64.as(preparedLockWord).getBoundMonitor();
            monitor.setDisplacedMisc(biasedLockWord);
            if (!biasedLockWord.countUnderflow()) {
                final VmThread owner = VmThreadMap.ACTIVE.getVmThreadForID(decodeLockwordThreadID(biasedLockWord.getBiasOwnerID()));
                monitor.monitorPrivateAcquire(owner, biasedLockWord.getRecursionCount());
            } else {
                monitor.monitorPrivateRelease();
            }
            return preparedLockWord;
        }

        @Override
        public void cancelPreparedModalLockWord(ModalLockWord64 preparedLockWord) {
            JavaMonitorManager.unbindMonitor(InflatedMonitorLockWord64.as(preparedLockWord).getBoundMonitor());
        }
    }
}
