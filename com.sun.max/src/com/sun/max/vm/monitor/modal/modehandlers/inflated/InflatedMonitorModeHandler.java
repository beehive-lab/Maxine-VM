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
package com.sun.max.vm.monitor.modal.modehandlers.inflated;

import com.sun.max.annotate.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.monitor.modal.modehandlers.*;
import com.sun.max.vm.monitor.modal.modehandlers.lightweight.biased.*;
import com.sun.max.vm.monitor.modal.modehandlers.lightweight.thin.*;
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.monitor.modal.sync.JavaMonitorManager.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.thread.*;

/**
 * Inflated mode handler for ModalMonitorSchemes.
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
    protected final InflatedMonitorLockword readMiscAndProtectBinding(Object object) {
        final InflatedMonitorLockword lockword = InflatedMonitorLockword.from(ObjectAccess.readMisc(object));
        if (Platform.target().arch.is64bit()) {
            if (lockword.isBound()) {
                JavaMonitorManager.protectBinding(lockword.getBoundMonitor());
            }
        } else {
            final InflatedMonitorLockword hashword = InflatedMonitorLockword.from(ObjectAccess.readHash(object));
            if (lockword.isBound()) {
                JavaMonitorManager.protectBinding(hashword.getBoundMonitor());
            }
        }
        return lockword;
    }

    protected int makeBoundHashCode(Object object, InflatedMonitorLockword lockword, InflatedMonitorLockword hashword) {
        assert hashword.isZero() || Platform.target().arch.is32bit() : " Hashword != null in 64 bit mode ";
        final JavaMonitor monitor = Platform.target().arch.is64bit() ? lockword.getBoundMonitor() : hashword.getBoundMonitor();
        assert monitor != null  : "null monitor in makeboundHashCode";
        final HashableLockword swappedLockword = Platform.target().arch.is64bit() ? HashableLockword.from(monitor.displacedMisc()) : HashableLockword.from(monitor.displacedHash());
        int hashcode = swappedLockword.getHashcode();
        if (hashcode == 0) {
            hashcode = monitorScheme().createHashCode(object);
            final HashableLockword newSwappedLockword = swappedLockword.setHashcode(hashcode);
            final HashableLockword answer = Platform.target().arch.is64bit() ? HashableLockword.from(monitor.compareAndSwapDisplacedMisc(swappedLockword, newSwappedLockword))
                            : HashableLockword.from(monitor.compareAndSwapDisplacedHash(swappedLockword, newSwappedLockword));
            if (!answer.equals(swappedLockword)) {
                hashcode = answer.getHashcode();
            }
        }
        return hashcode;
    }

    protected void monitorExit(Object object, InflatedMonitorLockword lockword, InflatedMonitorLockword hashword) {
        assert hashword.isZero() || Platform.target().arch.is32bit() : "Non null hashword in 64 bit";
        if (!lockword.isBound()) {
            throw new IllegalMonitorStateException();
        }
        final JavaMonitor monitor = Platform.target().arch.is64bit() ? lockword.getBoundMonitor() : hashword.getBoundMonitor();
        monitor.monitorExit();
    }

    protected void monitorNotify(Object object, boolean all, InflatedMonitorLockword lockword, InflatedMonitorLockword hashword) {
        assert hashword.isZero() || Platform.target().arch.is32bit() : "Non null hashword in 64 bit";
        if (!lockword.isBound()) {
            throw new IllegalMonitorStateException();
        }
        final JavaMonitor monitor = Platform.target().arch.is64bit() ? lockword.getBoundMonitor() : hashword.getBoundMonitor();
        monitor.monitorNotify(all);
    }

    protected void monitorWait(Object object, long timeout, InflatedMonitorLockword lockword, InflatedMonitorLockword hashword) throws InterruptedException {
        assert hashword.isZero() || Platform.target().arch.is32bit() : "Non null hashword in 64 bit";
        if (!lockword.isBound()) {
            throw new IllegalMonitorStateException();
        }
        final JavaMonitor monitor = Platform.target().arch.is64bit() ? lockword.getBoundMonitor() : hashword.getBoundMonitor();
        monitor.monitorWait(timeout);
    }

    protected boolean threadHoldsMonitor(InflatedMonitorLockword lockword, InflatedMonitorLockword hashword, VmThread thread) {
        assert hashword.isZero() || Platform.target().arch.is32bit() : "Non null hashword in 64 bit";
        return lockword.isBound() && (Platform.target().arch.is64bit() ? lockword.getBoundMonitor().isOwnedBy(thread) : hashword.getBoundMonitor().isOwnedBy(thread));
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

                public void writeUnboundMiscWord(Object object, Word preBindingMiscWord) {
                    ObjectAccess.writeMisc(object, preBindingMiscWord);
                }

                public void writeUnboundHashWord(Object object, Word hashWord) {
                    ObjectAccess.writeHash(object, hashWord);
                }
            });
        }

        @Override
        public void initialize(MaxineVM.Phase phase) {
            if (MaxineVM.isHosted() && phase == Phase.BOOTSTRAPPING) {
                JavaMonitorManager.setRequireProxyAcquirableMonitors(false);
            }
        }

        /**
         * This function returns: In 64 Bit mode: A misc word with the hashcode installed in unbound state In 32 Bit
         * mode: A misc word in unbounded stated. The hashcode has to be installed separately.
         */
        public Word createMisc(Object object) {
            return InflatedMonitorLockword.unboundFromHashcode(monitorScheme().createHashCode(object));
        }

        public int createHash(Object object) {
            int hashCode = InflatedMonitorLockword.fromHashcode(monitorScheme().createHashCode(object)).getHashcode();
            assert hashCode == monitorScheme().createHashCode(object) : "Failed installation of hashcode!";
            return hashCode;
        }

        public int makeHashCode(Object object) {
            if (MaxineVM.isHosted()) {
                return monitorScheme().createHashCode(object);
            }
            final InflatedMonitorLockword lockword = readMiscAndProtectBinding(object);
            final InflatedMonitorLockword hashword = Platform.target().arch.is64bit() ? InflatedMonitorLockword.from(Word.zero()) : InflatedMonitorLockword.from(ObjectAccess.readHash(object));

            if (lockword.isBound()) {
                return makeBoundHashCode(object, lockword, hashword);
            }
            int hashcode = Platform.target().arch.is64bit() ? lockword.getHashcode() : hashword.getHashcode();
            if (hashcode == 0) {
                hashcode = monitorScheme().createHashCode(object);
                final HashableLockword newLockword = Platform.target().arch.is64bit() ? lockword.setHashcode(hashcode) : hashword.setHashcode(hashcode);
                final Word answer = Platform.target().arch.is64bit() ? ObjectAccess.compareAndSwapMisc(object, lockword, newLockword) : ObjectAccess.compareAndSwapHash(object, hashword, newLockword);
                if (Platform.target().arch.is64bit()) {
                    if (!answer.equals(lockword)) {
                        return makeHashCode(object);
                    }
                } else {
                    if (!answer.equals(hashword)) {
                        return makeHashCode(object);
                    }
                }
            }
            return hashcode;
        }

        public void monitorEnter(Object object) {
            if (MaxineVM.isHosted()) {
                HostMonitor.enter(object);
                return;
            }
            if (MaxineVM.isPrimordial()) {
                return;
            }
            // 64Bit: hashWord+bits, 32Bit: Just bits
            InflatedMonitorLockword lockword = readMiscAndProtectBinding(object);
            // 64Bit: null, 32Bit: hashWord
            InflatedMonitorLockword hashword = Platform.target().arch.is64bit() ? InflatedMonitorLockword.from(Word.zero()) : InflatedMonitorLockword.from(ObjectAccess.readHash(object));
            JavaMonitor monitor = null;
            while (true) {
                if (lockword.isBound()) {
                    if (monitor != null) {
                        monitor.monitorExit();
                        JavaMonitorManager.unbindMonitor(monitor);
                    }
                    final JavaMonitor boundMonitor = Platform.target().arch.is64bit() ? lockword.getBoundMonitor() : hashword.getBoundMonitor();
                    boundMonitor.monitorEnter();
                    return;
                } else if (monitor == null) {
                    monitor = JavaMonitorManager.bindMonitor(object);
                    monitor.monitorEnter();
                }

                monitor.setDisplacedMisc(lockword);
                if (Platform.target().arch.is32bit()) {
                    monitor.setDisplacedHash(hashword);
                }
                // 64Bit: monitor+bits, 32Bit: monitor address
                final InflatedMonitorLockword newLockword = Platform.target().arch.is64bit() ? InflatedMonitorLockword.boundFromMonitor(monitor) : InflatedMonitorLockword.boundFromZero();
                final InflatedMonitorLockword newHashword = InflatedMonitorLockword.boundFromMonitor(monitor);

                final Word answer = Platform.target().arch.is64bit() ? ObjectAccess.compareAndSwapMisc(object, lockword, newLockword) : ObjectAccess.compareAndSwapHash(object, hashword, newHashword);

                if (Platform.target().arch.is64bit()) {
                    if (answer.equals(lockword)) {
                        return;
                    } else {
                        lockword = InflatedMonitorLockword.from(answer);
                    }
                } else {
                    if (answer.equals(hashword)) {
                        ObjectAccess.writeMisc(object, newLockword);
                        return;
                    } else {
                        hashword = InflatedMonitorLockword.from(answer);
                        while (!lockword.isBound()) {
                            lockword = InflatedMonitorLockword.from(ObjectAccess.readMisc(object));
                        }
                    }
                }
            }
        }

        public void monitorExit(Object object) {
            if (MaxineVM.isHosted()) {
                HostMonitor.exit(object);
                return;
            }
            if (MaxineVM.isPrimordial()) {
                return;
            }
            final InflatedMonitorLockword lockword = InflatedMonitorLockword.from(ObjectAccess.readMisc(object));
            final InflatedMonitorLockword hashword = Platform.target().arch.is64bit() ? InflatedMonitorLockword.from(Word.zero()) : InflatedMonitorLockword.from(ObjectAccess.readHash(object));
            super.monitorExit(object, lockword, hashword);
        }

        public void monitorNotify(Object object, boolean all) {
            nullCheck(object);
            if (MaxineVM.isHosted()) {
                HostMonitor.notify(object);
                return;
            }
            final InflatedMonitorLockword lockword = InflatedMonitorLockword.from(ObjectAccess.readMisc(object));
            final InflatedMonitorLockword hashword = Platform.target().arch.is64bit() ? InflatedMonitorLockword.from(Word.zero()) : InflatedMonitorLockword.from(ObjectAccess.readHash(object));
            super.monitorNotify(object, all, lockword, hashword);
        }

        public void monitorWait(Object object, long timeout) throws InterruptedException {
            nullCheck(object);
            if (MaxineVM.isHosted()) {
                HostMonitor.wait(object, timeout);
                return;
            }
            final InflatedMonitorLockword lockword = readMiscAndProtectBinding(object);
            final InflatedMonitorLockword hashword = Platform.target().arch.is64bit() ? InflatedMonitorLockword.from(Word.zero()) : InflatedMonitorLockword.from(ObjectAccess.readHash(object));
            super.monitorWait(object, timeout, lockword, hashword);
        }

        public boolean threadHoldsMonitor(Object object, VmThread thread) {
            final InflatedMonitorLockword lockword = readMiscAndProtectBinding(object);
            final InflatedMonitorLockword hashword = Platform.target().arch.is64bit() ? InflatedMonitorLockword.from(Word.zero()) : InflatedMonitorLockword.from(ObjectAccess.readHash(object));
            return super.threadHoldsMonitor(lockword, hashword, thread);
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

        public int delegateMakeHashcode(Object object, ModalLockword lockword) {
            final InflatedMonitorLockword inflatedLockword = readMiscAndProtectBinding(object);
            if (inflatedLockword.isBound()) {
                return makeBoundHashCode(object, inflatedLockword,
                                Platform.target().arch.is64bit() ? InflatedMonitorLockword.from(Word.zero()) : InflatedMonitorLockword.from(ObjectAccess.readHash(object)));
            }
            return 0;
        }

        public boolean delegateMonitorEnter(Object object, ModalLockword lockword, int lockwordThreadID) {
            final InflatedMonitorLockword inflatedLockword = readMiscAndProtectBinding(object);
            if (!inflatedLockword.isBound()) {
                return false;
            }
            final JavaMonitor monitor = Platform.target().arch.is64bit() ? inflatedLockword.getBoundMonitor() : InflatedMonitorLockword.from(ObjectAccess.readHash(object)).getBoundMonitor();
            monitor.monitorEnter();
            return true;
        }

        public void delegateMonitorExit(Object object, ModalLockword lockword) {
            monitorExit(object, InflatedMonitorLockword.from(lockword),
                            Platform.target().arch.is64bit() ? InflatedMonitorLockword.from(Word.zero()) : InflatedMonitorLockword.from(ObjectAccess.readHash(object)));
        }

        public void delegateMonitorNotify(Object object, boolean all, ModalLockword lockword) {
            monitorNotify(object, all, InflatedMonitorLockword.from(lockword),
                            Platform.target().arch.is64bit() ? InflatedMonitorLockword.from(Word.zero()) : InflatedMonitorLockword.from(ObjectAccess.readHash(object)));
        }

        public void delegateMonitorWait(Object object, long timeout, ModalLockword lockword) throws InterruptedException {
            final InflatedMonitorLockword inflatedLockword = InflatedMonitorLockword.from(lockword);
            final InflatedMonitorLockword hashword = InflatedMonitorLockword.from(ObjectAccess.readHash(object));
            if (inflatedLockword.isBound()) {
                JavaMonitorManager.protectBinding(Platform.target().arch.is64bit() ? inflatedLockword.getBoundMonitor() : hashword.getBoundMonitor());
            }
            monitorWait(object, timeout, inflatedLockword,
                            Platform.target().arch.is64bit() ? InflatedMonitorLockword.from(Word.zero()) : hashword);
        }

        public DelegatedThreadHoldsMonitorResult delegateThreadHoldsMonitor(Object object, ModalLockword lockword, VmThread thread, int lockwordThreadID) {
            final InflatedMonitorLockword inflatedLockword = readMiscAndProtectBinding(object);
            if (!inflatedLockword.isBound()) {
                return DelegatedThreadHoldsMonitorResult.NOT_THIS_MODE;
            }
            return super.threadHoldsMonitor(inflatedLockword,
                            Platform.target().arch.is64bit() ? InflatedMonitorLockword.from(Word.zero()) : InflatedMonitorLockword.from(ObjectAccess.readHash(object)), thread)
                                            ? DelegatedThreadHoldsMonitorResult.TRUE : DelegatedThreadHoldsMonitorResult.FALSE;
        }

        public void delegateAfterGarbageCollection() {
            afterGarbageCollection();
        }

        public void delegateBeforeGarbageCollection() {
            beforeGarbageCollection();
        }
    }

    /**
     * Implements the required interface to allow an InflatedMonitorModeHandler to acts as a delegate to thin locking.
     */
    static final class ThinLockDelegate extends Delegate implements ModeDelegate {

        ThinLockDelegate() {
            super(new UnboundMiscWordWriter() {

                public void writeUnboundMiscWord(Object object, Word preBindingMiscWord) {
                    ObjectAccess.writeMisc(object, ThinLockword.from(preBindingMiscWord).asUnlocked());
                }

                public void writeUnboundHashWord(Object object, Word hashWord) {
                    ObjectAccess.writeHash(object, hashWord);
                }
            });
        }

        @Override
        public void initialize(MaxineVM.Phase phase) {
            if (MaxineVM.isHosted() && phase == Phase.BOOTSTRAPPING) {
                JavaMonitorManager.setRequireProxyAcquirableMonitors(true);
            }
        }

        public ModalLockword prepareModalLockword(Object object, ModalLockword currentLockword) {
            final ThinLockword thinLockword = ThinLockword.from(currentLockword);
            // FIXME: We have to map the thinLockword thread ID back to the VmThread. This is expensive -
            // and has to lock VMThread.ACTIVE. Maybe we should just keep threadID's in Monitor's too?
            // FIXME: What if the VmThread is null?
            final JavaMonitor monitor = JavaMonitorManager.bindMonitor(object);
            monitor.setDisplacedMisc(thinLockword.asUnlocked());
            if (Platform.target().arch.is32bit()) {
                monitor.setDisplacedHash(ObjectAccess.readHash(object));
            }
            if (!thinLockword.equals(thinLockword.asUnlocked())) {
                final VmThread owner = VmThreadMap.ACTIVE.getVmThreadForID(decodeLockwordThreadID(thinLockword.getLockOwnerID()));
                monitor.monitorPrivateAcquire(owner, thinLockword.getRecursionCount());
            } else {
                monitor.monitorPrivateRelease();
            }
            return InflatedMonitorLockword.boundFromMonitor(monitor);
        }

        public ModalLockword reprepareModalLockword(ModalLockword preparedLockword, ModalLockword currentLockword, ModalLockword hash) {
            final ThinLockword thinLockword = ThinLockword.from(currentLockword);
            final JavaMonitor monitor =  InflatedMonitorLockword.from(preparedLockword).getBoundMonitor();
            monitor.setDisplacedMisc(thinLockword);
            if (Platform.target().arch.is32bit()) {
                monitor.setDisplacedHash(hash);
            }
            if (!thinLockword.equals(thinLockword.asUnlocked())) {
                final VmThread owner = VmThreadMap.ACTIVE.getVmThreadForID(decodeLockwordThreadID(thinLockword.getLockOwnerID()));
                monitor.monitorPrivateAcquire(owner, thinLockword.getRecursionCount());
            } else {
                monitor.monitorPrivateRelease();
            }
            return preparedLockword;
        }

        public void cancelPreparedModalLockword(ModalLockword preparedLockword) {
            JavaMonitorManager.unbindMonitor(InflatedMonitorLockword.from(preparedLockword).getBoundMonitor());
        }
    }

    /**
     * Implements the required interface to allow an InflatedMonitorModeHandler to acts as a delegate to biased locking.
     */
    private static final class BiasedLockDelegate extends Delegate {

        protected BiasedLockDelegate() {
            super(new UnboundMiscWordWriter() {

                public void writeUnboundMiscWord(Object object, Word preBindingMiscWord) {
                    ObjectAccess.writeMisc(object, BiasedLockword.from(preBindingMiscWord).asAnonBiased());
                }

                public void writeUnboundHashWord(Object object, Word hashWord) {
                    ObjectAccess.writeHash(object, hashWord);
                }
            });
            assert Platform.target().arch.is64bit() : "Biased locking not implemented for 32 bit";
        }

        @Override
        public void initialize(MaxineVM.Phase phase) {
            if (MaxineVM.isHosted() && phase == Phase.BOOTSTRAPPING) {
                JavaMonitorManager.setRequireProxyAcquirableMonitors(true);
            }
        }

        public ModalLockword prepareModalLockword(Object object, ModalLockword currentLockword) {
            final BiasedLockword biasedLockword = BiasedLockword.from(currentLockword);
            final JavaMonitor monitor = JavaMonitorManager.bindMonitor(object);
            final InflatedMonitorLockword newLockword = InflatedMonitorLockword.boundFromMonitor(monitor);
            monitor.setDisplacedMisc(biasedLockword);
            if (!biasedLockword.countUnderflow()) {
                // It does not matter if the threadID has been recycled after the bias request.
                final VmThread biasOwner = VmThreadMap.ACTIVE.getVmThreadForID(decodeLockwordThreadID(biasedLockword.getBiasOwnerID()));
                monitor.monitorPrivateAcquire(biasOwner, biasedLockword.getRecursionCount());
            } else {
                monitor.monitorPrivateRelease();
            }
            return newLockword;
        }

        public ModalLockword reprepareModalLockword(ModalLockword preparedLockword, ModalLockword currentLockword, ModalLockword hash) {
            assert Platform.target().arch.is64bit() : "Biased locking is not implemented in 32 bit";
            final BiasedLockword biasedLockword = BiasedLockword.from(currentLockword);
            final JavaMonitor monitor = InflatedMonitorLockword.from(preparedLockword).getBoundMonitor();
            monitor.setDisplacedMisc(biasedLockword);
            if (!biasedLockword.countUnderflow()) {
                final VmThread owner = VmThreadMap.ACTIVE.getVmThreadForID(decodeLockwordThreadID(biasedLockword.getBiasOwnerID()));
                monitor.monitorPrivateAcquire(owner, biasedLockword.getRecursionCount());
            } else {
                monitor.monitorPrivateRelease();
            }
            return preparedLockword;
        }

        public void cancelPreparedModalLockword(ModalLockword preparedLockword) {
            JavaMonitorManager.unbindMonitor(InflatedMonitorLockword.from(preparedLockword).getBoundMonitor());
        }
    }
}
