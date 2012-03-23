/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.debug;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.data.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.Layout.HeaderField;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * <strong>Watchpoints</strong>.
 * <p>
 * A watchpoint triggers <strong>after</strong> a specified event has occurred: read, write, or exec.  So-called "before"
 * watchpoints are not supported.
 * <p>
 * Watchpoint creation may fail for platform-specific reasons, for example if watchpoints are not supported at all, or are
 * only supported in limited numbers, or only permitted in certain sizes or locations.
 * <p>
 * A new watchpoint is "alive" and remains so until removed (deleted), at which time it become permanently inert.  Any attempt
 * to enable or otherwise manipulate a removed watchpoint will cause a TeleError to be thrown.
 * <p>
 * A watchpoint is by definition "enabled" (client concept) if it is alive and one or more of the three trigger settings
 * is true:  <strong>trapOnRead</strong>, <strong>trapOnWrite</strong>, or <strong>trapOnExec</strong>.
 * If none is true, then the watchpoint is by definition "disabled" and can have no effect on VM execution.
 * <p>
 * A watchpoint is "active" (implementation concept) if it has been installed in the process running the VM, something that may
 * happen when when it is enabled.  If a watchpoint becomes disabled, it will be deactivated (removed from the process).
 * A watchpoint may also be deactivated/reactivated transparently to the client for implementation purposes.
 * <p>
 * A watchpoint with <strong>enabledDuringGC</strong> set to false will be effectively disabled during any period of time when
 * the VM is performing GC.  In practice, the watchpoint may trigger if an event takes place during GC, but execution will then
 * resume silently when it is determined that GC is underway.  This is true whether the watchpoint is relocatable or not.
 * <p>
 * A <strong>relocatable</strong> watchpoint is set on a location that is part of an object's representation.  Such a watchpoint
 * follows the object should its representation be moved during GC to a different location. The life cycle of a relocatable watchpoint
 * depends on the state of the object when first created, on the treatment of the object by the GC in the VM, and by the timing
 * in which the Inspector is able to update its state in response to GC actions.
 * <p>
 * A watchpoint may only be created on an object known to the inspector as live (neither dead nor forwarded).
 * Attempting to set a watchpoint on an object known to the inspector to be not live will cause a TeleError to be thrown.
 * <p>
 * A relocatable watchpoint associated with an object that is eventually determined to have been collected will be removed and
 * replaced with a non-relocatable watchpoint covering the same memory region.
 * <p>
 * <strong>Concurrency:</strong> operations that modify a watchpoint must necessarily be implemented by directly affecting
 * the VM processes.  Such operations fail if they are unable to acquire the VM lock held during the execution of VM commands.
 */
public abstract class VmWatchpoint extends AbstractVmHolder implements VMTriggerEventHandler, MaxWatchpoint {

    // TODO (mlvdv) Consider a response when user tries to set a watchpoint on first header word.  May mean that
    // there can be multiple watchpoints at a location.  Work through the use cases.
    // Note that system watchpoint code does not check for too many or for overlap.

    /**
     * Distinguishes among uses for watchpoints, independently of how the location is specified.
     */
    private enum WatchpointKind {

        /**
         * A watchpoint created on behalf of a client external to the VM.  Such
         * a watchpoint is presumed to be managed completely by the client:  creation/deletion,
         * enable/disable etc.  Only client watchpoints are visible to the client in ordinary use.
         */
        CLIENT,

        /**
         * A watchpoint created by one of the services in the VM, generally in order
         * to catch certain events in the VM so that state can be synchronized for
         * some purpose.  Presumed to be managed completely by the service using it.  These
         * are generally not visible to clients.
         * <p>
         * Not relocatable.
         */
        SYSTEM;
    }

    private static final int TRACE_VALUE = 1;

    private final WatchpointKind kind;

    private final String description;

    private volatile TeleFixedMemoryRegion memoryRegion;

    /**
     * Watchpoints manager.
     */
    protected final VmWatchpointManager watchpointManager;

    /**
     * Is this watchpoint still alive (not yet removed) and available for activation/deactivation?
     * This is true from the creation of the watchpoint until it is removed, at which event
     * it becomes permanently false and it cannot be used.
     */
    private volatile boolean alive = true;

    /**
     * Is this watchpoint currently active in the process?
     * <p>
     * This is an implementation issue, which should not be visible to clients.
     * <p>
     * Only "live" watchpoints may be activated.
     */
    private boolean active = false;

    /**
     * Watchpoint configuration.
     */
    private volatile WatchpointSettings settings;

    /**
     * Stores data read from the memory covered by watchpoint.
     */
    private byte[] memoryCache;

    private VMTriggerEventHandler triggerEventHandler = VMTriggerEventHandler.Static.ALWAYS_TRUE;

    private VmWatchpoint(WatchpointKind kind, VmWatchpointManager watchpointManager, String description, Address start, long nBytes, WatchpointSettings settings) {
        super(watchpointManager.vm());
        this.kind = kind;
        this.watchpointManager = watchpointManager;
        this.settings = settings;
        this.memoryRegion = new TeleFixedMemoryRegion(vm(), "watchpoint region", start, nBytes);
        this.description = description;
    }

    private VmWatchpoint(WatchpointKind kind, VmWatchpointManager watchpointManager, String description, MaxMemoryRegion memoryRegion, WatchpointSettings settings) {
        this(kind, watchpointManager, description, memoryRegion.start(), memoryRegion.nBytes(), settings);
    }

    public final TeleFixedMemoryRegion memoryRegion() {
        return memoryRegion;
    }

    public final String description() {
        return description;
    }

    @Override
    public final boolean equals(Object o) {
        // For the purposes of the collection, define ordering and equality in terms of start location only.
        if (o instanceof VmWatchpoint) {
            final VmWatchpoint watchpoint = (VmWatchpoint) o;
            return memoryRegion().start().equals(watchpoint.memoryRegion().start());
        }
        return false;
    }

    public final WatchpointSettings getSettings() {
        return settings;
    }

    public final boolean setTrapOnRead(boolean trapOnRead) throws MaxVMBusyException, TeleError {
        TeleError.check(alive, "Attempt to modify settings on a removed watchpoint");
        if (!vm().tryLock()) {
            throw new MaxVMBusyException();
        }
        boolean success = false;
        final WatchpointSettings oldSettings = settings;
        try {
            this.settings = new WatchpointSettings(trapOnRead, oldSettings.trapOnWrite, oldSettings.trapOnExec, oldSettings.enabledDuringGC);
            success = reset();
        }  finally {
            if (!success) {
                this.settings = oldSettings;
            }
            vm().unlock();
        }
        return success;
    }

    public final boolean setTrapOnWrite(boolean trapOnWrite) throws MaxVMBusyException, TeleError {
        TeleError.check(alive, "Attempt to modify settings on a removed watchpoint");
        if (!vm().tryLock()) {
            throw new MaxVMBusyException();
        }
        boolean success = false;
        final WatchpointSettings oldSettings = settings;
        try {
            this.settings = new WatchpointSettings(settings.trapOnRead, trapOnWrite, settings.trapOnExec, settings.enabledDuringGC);
            success = reset();
        }  finally {
            if (!success) {
                this.settings = oldSettings;
            }
            vm().unlock();
        }
        return success;
    }

    public final boolean setTrapOnExec(boolean trapOnExec) throws MaxVMBusyException, TeleError {
        TeleError.check(alive, "Attempt to modify settings on a removed watchpoint");
        if (!vm().tryLock()) {
            throw new MaxVMBusyException();
        }
        boolean success = false;
        final WatchpointSettings oldSettings = settings;
        try {
            this.settings = new WatchpointSettings(settings.trapOnRead, settings.trapOnWrite, trapOnExec, settings.enabledDuringGC);
            success = reset();
        }  finally {
            if (!success) {
                this.settings = oldSettings;
            }
            vm().unlock();
        }
        return success;
    }

    public final boolean setEnabledDuringGC(boolean enabledDuringGC) throws MaxVMBusyException, TeleError {
        TeleError.check(alive, "Attempt to modify settings on a removed watchpoint");
        if (!vm().tryLock()) {
            throw new MaxVMBusyException();
        }
        boolean success = false;
        final WatchpointSettings oldSettings = settings;
        try {
            this.settings = new WatchpointSettings(settings.trapOnRead, settings.trapOnWrite, settings.trapOnExec, enabledDuringGC);
            if (enabledDuringGC && watchpointManager.heap().phase().isCollecting() && !active) {
                setActive(true);
            }
            success = reset();
        }  finally {
            if (!success) {
                this.settings = oldSettings;
            }
            vm().unlock();
        }
        return success;
    }

    public final boolean isEnabled() {
        final WatchpointSettings settings = this.settings;
        return alive && (settings.trapOnRead || settings.trapOnWrite || settings.trapOnExec);
    }

    public boolean remove() throws MaxVMBusyException, TeleError {
        TeleError.check(alive, "Attempt to remove a removed watchpoint");
        if (!vm().tryLock()) {
            throw new MaxVMBusyException();
        }
        boolean success = false;
        try {
            if (active) {
                setActive(false);
            }
            success =  watchpointManager.removeWatchpoint(this);
            if (success) {
                alive = false;
            }
        } finally {
            vm().unlock();
        }
        return success;
    }

    public final boolean handleTriggerEvent(TeleNativeThread teleNativeThread) {
        assert alive;
        assert teleNativeThread.state() == MaxThreadState.WATCHPOINT;
        Trace.begin(TRACE_VALUE, tracePrefix() + "handling trigger event for " + this);
        if (watchpointManager.heap().phase().isCollecting() && !settings.enabledDuringGC) {
            // Ignore the event if the VM is in GC and the watchpoint is not to be enabled during GC.
            // This is a lazy policy that avoids the need to interrupt the VM every time GC starts.
            // Just in case such a watchpoint would trigger repeatedly during GC, however, deactivate
            // it now (at first trigger) for the duration of the GC.  All such watchpoints will be
            // reactivated at the conclusion of GC, when it is necessary to interrupt the VM anyway.
            setActive(false);
            Trace.end(TRACE_VALUE, tracePrefix() + "handling trigger event (IGNORED) for " + this);
            return false;
        }
        final boolean handleTriggerEvent = triggerEventHandler.handleTriggerEvent(teleNativeThread);
        Trace.end(TRACE_VALUE, tracePrefix() + "handling trigger event for " + this);
        return handleTriggerEvent;
    }

    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{").append(kind.toString());
        if (!alive) {
            sb.append("(DELETED)");
        }
        sb.append(", ").append(isEnabled() ? "enabled" : "disabled");
        sb.append(", ").append(isActive() ? "active" : "inactive");
        sb.append(", 0x").append(memoryRegion.start().toHexString());
        sb.append(", size=").append(memoryRegion.nBytes());
        sb.append(", \"").append(description).append("\"");
        sb.append("}");
        return sb.toString();
    }

    protected final boolean isAlive() {
        return alive;
    }

    protected final boolean isActive() {
        return active;
    }

    /**
     * Assigns to this watchpoint a  handler for events triggered by this watchpoint.  A null handler
     * is equivalent to there being no handling action and a return of true (VM execution should halt).
     *
     * @param triggerEventHandler handler for VM execution events triggered by this watchpoint.
     */
    protected final void setTriggerEventHandler(VMTriggerEventHandler triggerEventHandler) {
        this.triggerEventHandler =
            (triggerEventHandler == null) ? VMTriggerEventHandler.Static.ALWAYS_TRUE : triggerEventHandler;
    }

    /**
     * Reads and stores the contents of VM memory in the region of the watchpoint.
     *
     * Future usage: e.g. for conditional Watchpoints
     */
    private void updateMemoryCache() {
        long nBytes = memoryRegion().nBytes();
        assert nBytes < Integer.MAX_VALUE;
        if (memoryCache == null || memoryCache.length != nBytes) {
            memoryCache = new byte[(int) nBytes];
        }
        try {
            memoryCache = watchpointManager.vm().memoryIO().readBytes(memoryRegion().start(), (int) nBytes);
        } catch (DataIOError e) {
            // Must be a watchpoint in an address space that doesn't (yet?) exist in the VM process.
            memoryCache = null;
        }
    }

    private void setStart(Address start) {
        memoryRegion = new TeleFixedMemoryRegion(vm(), "", start, memoryRegion.nBytes());
    }

    /**
     * Change the activation state of the watchpoint in the VM.
     *
     * @param active the desired activation state
     * @return whether the change succeeded
     * @throws TeleError if requested state same as current state
     */
    private boolean setActive(boolean active) {
        assert alive;
        if (active) {  // Try to activate
            TeleError.check(!this.active, "Attempt to activate an active watchpoint:", this);
            if (watchpointManager.teleProcess.activateWatchpoint(this)) {
                this.active = true;
                Trace.line(TRACE_VALUE, tracePrefix() + "Watchpoint activated: " + this);
                return true;
            } else {
                TeleWarning.message("Failed to activate watchpoint: " + this);
                return false;
            }
        } else { // Try to deactivate
            TeleError.check(this.active, "Attempt to deactivate an inactive watchpoint:", this);
            if (watchpointManager.teleProcess.deactivateWatchpoint(this)) {
                this.active = false;
                Trace.line(TRACE_VALUE, tracePrefix() + "Watchpoint deactivated " + this);
                return true;
            }
            TeleWarning.message("Failed to deactivate watchpoint: " + this);
            return false;
        }
    }

    /**
     * Resets a watchpoint by deactivating it and then reactivating at the same
     * location.  This should be done when any settings change.
     *
     * @return true if reset was successful
     */
    private boolean reset() {
        assert alive;
        if (active) {
            if (!setActive(false)) {
                TeleWarning.message("Failed to reset watchpoint: " + this);
                return false;
            }
            if (!setActive(true)) {
                TeleWarning.message("Failed to reset and install watchpoint: " + this);
                return false;
            }
        }
        Trace.line(TRACE_VALUE, tracePrefix() + "Watchpoint reset " + this);
        watchpointManager.updateAfterWatchpointChanges();
        return true;
    }

    /**
     * Relocates a watchpoint by deactivating it and then reactivating
     * at a new start location.
     * <p>
     * Note that the location of a watchpoint should <strong>only</strong> be changed
     * via this method, since it must first be deactivated at its old location before the
     * new location is set.
     *
     * @param newAddress a new starting location for the watchpoint
     * @return true if reset was successful
     */
    protected final boolean relocate(Address newAddress) {
        assert newAddress != null;
        assert alive;
        if (active) {
            // Must deactivate before we change the location
            if (!setActive(false)) {
                TeleWarning.message("Failed to reset watchpoint: " + this);
                return false;
            }
            setStart(newAddress);
            if (!setActive(true)) {
                TeleWarning.message("Failed to reset and install watchpoint: " + this);
                return false;
            }
        } else {  // not active
            setStart(newAddress);
        }
        Trace.line(TRACE_VALUE, tracePrefix() + "Watchpoint reset " + memoryRegion().start().toHexString());
        watchpointManager.updateAfterWatchpointChanges();
        return true;
    }

    /**
     * Perform any updates on watchpoint state at the conclusion of a GC.
     */
    protected void updateAfterGC() {
        if (isEnabled() && !active) {
            // This watchpoint was apparently deactivated during GC because
            // it is not to be enabled during GC.
            setActive(true);
        }
    }

    /**
     * A watchpoint for a specified, fixed memory region.
     */
    private static final class TeleRegionWatchpoint extends VmWatchpoint {

        private TeleRegionWatchpoint(WatchpointKind kind, VmWatchpointManager watchpointManager, String description, MaxMemoryRegion memoryRegion, WatchpointSettings settings) {
            super(kind, watchpointManager, description, memoryRegion.start(), memoryRegion.nBytes(), settings);
        }

        public boolean isRelocatable() {
            return false;
        }

        public TeleObject getTeleObject() {
            return null;
        }
    }


    /**
     * A watchpoint for the memory holding a {@linkplain VmThreadLocal thread local variable}.
     *
     * @see VmThreadLocal
     */
    private static final class TeleVmThreadLocalWatchpoint extends VmWatchpoint {

        private TeleVmThreadLocalWatchpoint(WatchpointKind kind, VmWatchpointManager watchpointManager, String description, MaxThreadLocalVariable threadLocalVariable, WatchpointSettings settings) {
            super(kind, watchpointManager,  description, threadLocalVariable.memoryRegion(), settings);
        }

        public boolean isRelocatable() {
            return false;
        }

        public TeleObject getTeleObject() {
            return null;
        }
    }

    /**
     * Abstraction for watchpoints covering some or all of an object, and which will
     * be relocated to follow the absolute location of the object whenever it is relocated by GC.
     *
     */
    private abstract static class TeleObjectWatchpoint extends VmWatchpoint {

        /**
         * Watchpoint settings to use when a system watchpoint is placed on the field
         * to which a forwarding pointer gets written, designed to catch the relocation
         * of this specific object.
         */
        private static final WatchpointSettings relocationWatchpointSettings = new WatchpointSettings(false, true, false, true);

        /**
         * The VM heap object on which this watchpoint is set.
         */
        private TeleObject teleObject;

        /**
         * Starting location of the watchpoint, relative to the origin of the object.
         */
        private final int offset;

        /**
         * A hidden (system) watchpoint set on the object's field that the GC uses to store forwarding
         * pointers.
         */
        private VmWatchpoint relocationWatchpoint = null;

        private TeleObjectWatchpoint(WatchpointKind kind, VmWatchpointManager watchpointManager, String description, TeleObject teleObject, int offset, long nBytes, WatchpointSettings settings)
            throws MaxWatchpointManager.MaxTooManyWatchpointsException, MaxWatchpointManager.MaxDuplicateWatchpointException  {
            super(kind, watchpointManager, description, teleObject.origin().plus(offset), nBytes, settings);
            TeleError.check(teleObject.status().isNotDead(), "Attempt to set an object-based watchpoint on an object that is not live: ", teleObject);
            this.teleObject = teleObject;
            this.offset = offset;
            setRelocationWatchpoint(teleObject.origin());
        }

        /**
         * Sets a watchpoint on the area of the object where GC writes a forwarding pointer; when
         * triggered, the watchpoint relocates this watchpoint as well as itself to the new location
         * identified by the forwarding pointer.
         *
         * @param origin the object origin for this watchpoint
         * @throws MaxWatchpointManager.MaxTooManyWatchpointsException
         */
        private void setRelocationWatchpoint(final Pointer origin) throws MaxWatchpointManager.MaxTooManyWatchpointsException {
            final MaxVM vm = watchpointManager.vm();
            final Pointer forwardPointerLocation = origin.plus(objects().gcForwardingPointerOffset());
            final TeleFixedMemoryRegion forwardPointerRegion = new TeleFixedMemoryRegion(vm(), "Forwarding pointer for object relocation watchpoint", forwardPointerLocation, vm.platform().nBytesInWord());
            relocationWatchpoint = watchpointManager.createSystemWatchpoint("Object relocation watchpoint", forwardPointerRegion, relocationWatchpointSettings);
            relocationWatchpoint.setTriggerEventHandler(new VMTriggerEventHandler() {

                public boolean handleTriggerEvent(TeleNativeThread teleNativeThread) {
                    final TeleObjectWatchpoint thisWatchpoint = TeleObjectWatchpoint.this;
                    if (objects().isObjectForwarded(origin)) {
                        final TeleObject newTeleObject = objects().getForwardedObject(origin);
                        if (newTeleObject == null) {
                            TeleWarning.message("Unlable to find relocated teleObject" + this);
                        } else {
                            TeleObjectWatchpoint.this.teleObject = newTeleObject;
                            final Pointer newWatchpointStart = newTeleObject.origin().plus(thisWatchpoint.offset);
                            Trace.line(TRACE_VALUE, thisWatchpoint.tracePrefix() + " relocating watchpoint " + thisWatchpoint.memoryRegion().start().toHexString() + "-->" + newWatchpointStart.toHexString());
                            thisWatchpoint.relocate(newWatchpointStart);
                            // Now replace this relocation watchpoint for the next time the objects gets moved.
                            thisWatchpoint.clearRelocationWatchpoint();
                            try {
                                thisWatchpoint.setRelocationWatchpoint(newTeleObject.origin());
                            } catch (MaxWatchpointManager.MaxTooManyWatchpointsException maxTooManyWatchpointsException) {
                                TeleError.unexpected(thisWatchpoint.tracePrefix() + " failed to relocate the relocation watchpoint for " + thisWatchpoint);
                            }
                        }
                    } else {
                        Trace.line(TRACE_VALUE, thisWatchpoint.tracePrefix() + " relocating watchpoint (IGNORED) 0x" + thisWatchpoint.memoryRegion().start().toHexString());
                    }

                    return false;
                }
            });
            relocationWatchpoint.setActive(true);
        }

        /**
         * Clears the watchpoint, if any, set on the area of the object where GC writes a forwarding pointer.
         */
        private void clearRelocationWatchpoint() {
            if (relocationWatchpoint != null) {
                try {
                    relocationWatchpoint.remove();
                } catch (MaxVMBusyException maxVMBusyException) {
                    TeleError.unexpected("Should only be called with lock held");
                } catch (TeleError teleError) {
                    TeleError.unexpected("Attempt to remove an already removed object relocation watchpoint");
                }
                relocationWatchpoint = null;
            }
        }

        @Override
        public boolean remove() throws MaxVMBusyException, TeleError {
            if (!vm().tryLock()) {
                throw new MaxVMBusyException();
            }
            boolean success = false;
            try {
                clearRelocationWatchpoint();
                success = super.remove();
            } finally {
                vm().unlock();
            }
            return success;
        }

        public final boolean isRelocatable() {
            return true;
        }

        public final TeleObject getTeleObject() {
            assert isAlive();
            return teleObject;
        }

        @Override
        protected void updateAfterGC() {
            assert isAlive();
            super.updateAfterGC();
            // TODO (mlvdv) watchpoint on forwarded object?
            switch(teleObject.status()) {
                case LIVE:
                case UNKNOWN:
                    // A relocatable watchpoint on a live object should have been relocated
                    // (eagerly) just as the relocation took place.   Check that the locations match.
                    if (!teleObject.objectMemoryRegion().start().plus(offset).equals(memoryRegion().start())) {
                        TeleWarning.message("Watchpoint relocation failure - watchpoint on live object at wrong location " + this);
                    }
                    break;
                case DEAD:
                    // The watchpoint's object has been collected; convert it to a fixed memory region watchpoint
                    try {
                        remove();
                        final TeleFixedMemoryRegion watchpointRegion = new TeleFixedMemoryRegion(vm(), "Old memory location of watched object", memoryRegion().start(), memoryRegion().nBytes());
                        final VmWatchpoint newRegionWatchpoint =
                            watchpointManager.createRegionWatchpoint("Replacement for watchpoint on GC'd object", watchpointRegion, getSettings());
                        Trace.line(TRACE_VALUE, tracePrefix() + "Watchpoint on collected object replaced: " + newRegionWatchpoint);
                    } catch (MaxWatchpointManager.MaxTooManyWatchpointsException maxTooManyWatchpointsException) {
                        TeleWarning.message("Failed to replace object watchpoint with region watchpoint", maxTooManyWatchpointsException);
                    } catch (MaxWatchpointManager.MaxDuplicateWatchpointException maxDuplicateWatchpointException) {
                        TeleWarning.message("Failed to replace object watchpoint with region watchpoint", maxDuplicateWatchpointException);
                    } catch (MaxVMBusyException maxVMBusyException) {
                        TeleError.unexpected("Should only be called on request handling thread, where lock acquision should not fail");
                    }
            }
        }
    }

    /**
     * A watchpoint for a whole object.
     */
    private static final class TeleWholeObjectWatchpoint extends TeleObjectWatchpoint {

        private TeleWholeObjectWatchpoint(WatchpointKind kind, VmWatchpointManager watchpointManager, String description, TeleObject teleObject, WatchpointSettings settings)
            throws MaxWatchpointManager.MaxTooManyWatchpointsException, MaxWatchpointManager.MaxDuplicateWatchpointException {
            super(kind, watchpointManager, description, teleObject, 0, teleObject.objectMemoryRegion().nBytes(), settings);
        }
    }

    /**
     * A watchpoint for the memory holding an object's field.
     */
    private static final class TeleFieldWatchpoint extends TeleObjectWatchpoint {

        private TeleFieldWatchpoint(WatchpointKind kind, VmWatchpointManager watchpointManager, String description, TeleObject teleObject, FieldActor fieldActor, WatchpointSettings settings)
            throws MaxWatchpointManager.MaxTooManyWatchpointsException, MaxWatchpointManager.MaxDuplicateWatchpointException {
            super(kind, watchpointManager, description, teleObject, fieldActor.offset(), teleObject.fieldSize(fieldActor), settings);
        }
    }

    /**
     *A watchpoint for the memory holding an array element.
     */
    private static final class TeleArrayElementWatchpoint extends TeleObjectWatchpoint {

        private TeleArrayElementWatchpoint(WatchpointKind kind, VmWatchpointManager watchpointManager, String description, TeleObject teleObject, Kind elementKind, int arrayOffsetFromOrigin, int index, WatchpointSettings settings)
            throws MaxWatchpointManager.MaxTooManyWatchpointsException, MaxWatchpointManager.MaxDuplicateWatchpointException {
            super(kind, watchpointManager, description, teleObject, arrayOffsetFromOrigin + (index * elementKind.width.numberOfBytes), elementKind.width.numberOfBytes, settings);
        }
    }

    /**
     * A watchpoint for the memory holding an object's header field.
     */
    private static final class TeleHeaderWatchpoint extends TeleObjectWatchpoint {

        private TeleHeaderWatchpoint(WatchpointKind kind, VmWatchpointManager watchpointManager, String description, TeleObject teleObject, HeaderField headerField, WatchpointSettings settings)
            throws MaxWatchpointManager.MaxTooManyWatchpointsException, MaxWatchpointManager.MaxDuplicateWatchpointException {
            super(kind, watchpointManager, description, teleObject, teleObject.headerOffset(headerField), teleObject.headerSize(headerField), settings);
        }
    }

    /**
     * Singleton manager for creating and managing process watchpoints.
     * <p>
     * Overlapping watchpoints are not permitted.
     *
     */
    public static final class VmWatchpointManager extends AbstractVmHolder implements MaxWatchpointManager {

        private static VmWatchpointManager vmWatchpointManager;

        public static VmWatchpointManager make(TeleVM vm, TeleProcess teleProcess) {
            if (vmWatchpointManager == null) {
                vmWatchpointManager = new VmWatchpointManager(vm, teleProcess);
            }
            return vmWatchpointManager;
        }

        private final TeleProcess teleProcess;

        private final Comparator<VmWatchpoint> watchpointComparator = new Comparator<VmWatchpoint>() {

            public int compare(VmWatchpoint o1, VmWatchpoint o2) {
                // For the purposes of the collection, define equality and comparison to be based
                // exclusively on starting address.
                return o1.memoryRegion().start().compareTo(o2.memoryRegion().start());
            }
        };

        // This implementation is not thread-safe; this manager must take care of that.
        // Keep the set ordered by start address only, implemented by the comparator and equals().
        // An additional constraint imposed by this manager is that no regions overlap,
        // either in part or whole, with others in the set.
        private final TreeSet<VmWatchpoint> clientWatchpoints = new TreeSet<VmWatchpoint>(watchpointComparator);

        /**
         * A thread-safe, immutable collection of the current watchpoint list.
         * This list will be read many, many more times than it will change.
         */
        private volatile List<MaxWatchpoint> clientWatchpointsCache = Collections.emptyList();

        // Watchpoints used for internal purposes, for example for GC and relocation services
        private final TreeSet<VmWatchpoint> systemWatchpoints = new TreeSet<VmWatchpoint>(watchpointComparator);
        private volatile List<MaxWatchpoint> systemWatchpointsCache = Collections.emptyList();

        /**
         * A listener for GC completions, whenever there are any watchpoints; null when no watchpoints.
         */
        private MaxGCPhaseListener gcCompletedListener = null;

        private volatile List<MaxWatchpointListener> watchpointListeners = new CopyOnWriteArrayList<MaxWatchpointListener>();

        /**
         * Creates a manager for creating and managing watchpoints in the VM.
         *
         */
        private VmWatchpointManager(TeleVM vm, TeleProcess teleProcess) {
            super(vm);
            this.teleProcess = teleProcess;
            vm().addVMStateListener(new MaxVMStateListener() {

                public void stateChanged(MaxVMState maxVMState) {
                    if (maxVMState.processState() == MaxProcessState.TERMINATED) {
                        clientWatchpoints.clear();
                        systemWatchpoints.clear();
                        updateAfterWatchpointChanges();
                    }
                }
            });
        }

        /**
         * Adds a listener for watchpoint changes.
         * <p>
         * Thread-safe
         *
         * @param listener a watchpoint listener
         */
        public void addListener(MaxWatchpointListener listener) {
            assert listener != null;
            watchpointListeners.add(listener);
        }

        /**
         * Removes a listener for watchpoint changes.
         * <p>
         * Thread-safe
         *
         * @param listener a watchpoint listener
         */
        public void removeListener(MaxWatchpointListener listener) {
            assert listener != null;
            watchpointListeners.remove(listener);
        }

        /**
         * Creates a new, active watchpoint that covers a given memory region in the VM.
         * <p>
         * The trigger occurs <strong>after</strong> the specified event.
         *
         * @param description text useful to a person, for example capturing the intent of the watchpoint
         * @param memoryRegion the region of memory in the VM to be watched.
         * @param settings initial settings for the watchpoint
         *
         * @return a new watchpoint, if successful
         * @throws MaxWatchpointManager.MaxTooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
         * @throws MaxWatchpointManager.MaxDuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
         * @throws MaxVMBusyException if watchpoints cannot be set at present, presumably because the VM is running.
         */
        public VmWatchpoint createRegionWatchpoint(String description, MaxMemoryRegion memoryRegion, WatchpointSettings settings)
            throws MaxWatchpointManager.MaxTooManyWatchpointsException, MaxWatchpointManager.MaxDuplicateWatchpointException, MaxVMBusyException {
            if (!vm().tryLock()) {
                throw new MaxVMBusyException();
            }
            VmWatchpoint watchpoint;
            try {
                watchpoint = new TeleRegionWatchpoint(WatchpointKind.CLIENT, this, description, memoryRegion, settings);
                watchpoint = addClientWatchpoint(watchpoint);
            } finally {
                vm().unlock();
            }
            return watchpoint;
        }

        /**
         * Creates a new, active watchpoint that covers an entire heap object's memory in the VM.
         * @param description text useful to a person, for example capturing the intent of the watchpoint
         * @param teleObject a heap object in the VM
         * @param settings initial settings for the watchpoint
         * @return a new watchpoint, if successful
         * @throws MaxWatchpointManager.MaxTooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
         * @throws MaxWatchpointManager.MaxDuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
         * @throws MaxVMBusyException if watchpoints cannot be set at present, presumably because the VM is running.
         */
        public VmWatchpoint createObjectWatchpoint(String description, TeleObject teleObject, WatchpointSettings settings)
            throws MaxWatchpointManager.MaxTooManyWatchpointsException, MaxWatchpointManager.MaxDuplicateWatchpointException, MaxVMBusyException {
            if (!vm().tryLock()) {
                throw new MaxVMBusyException();
            }
            VmWatchpoint watchpoint;
            try {
                if (teleObject.status().isNotDead()) {
                    watchpoint  = new TeleWholeObjectWatchpoint(WatchpointKind.CLIENT, this, description, teleObject, settings);
                } else {
                    String amendedDescription = (description == null) ? "" : description;
                    amendedDescription = amendedDescription + " (non-live object))";
                    final TeleFixedMemoryRegion region = teleObject.objectMemoryRegion();
                    watchpoint = new TeleRegionWatchpoint(WatchpointKind.CLIENT, this, amendedDescription, region, settings);
                }
                watchpoint = addClientWatchpoint(watchpoint);
            } finally {
                vm().unlock();
            }
            return watchpoint;
        }

        /**
         * Creates a new, active watchpoint that covers a heap object's field in the VM. If the object is live,
         * than this watchpoint will track the object's location during GC.
         * <p>
         * If the object is not live, a plain memory region watchpoint is returned, one that does not relocate.
         *
         * @param description text useful to a person, for example capturing the intent of the watchpoint
         * @param teleObject a heap object in the VM
         * @param fieldActor description of a field in object of that type
         * @param settings initial settings for the watchpoint
         * @return a new watchpoint, if successful
         * @throws MaxWatchpointManager.MaxTooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
         * @throws MaxWatchpointManager.MaxDuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
         * @throws MaxVMBusyException if watchpoints cannot be set at present, presumably because the VM is running.
         */
        public VmWatchpoint createFieldWatchpoint(String description, TeleObject teleObject, FieldActor fieldActor, WatchpointSettings settings)
            throws MaxWatchpointManager.MaxTooManyWatchpointsException, MaxWatchpointManager.MaxDuplicateWatchpointException, MaxVMBusyException {
            if (!vm().tryLock()) {
                throw new MaxVMBusyException();
            }
            VmWatchpoint watchpoint;
            try {
                if (teleObject.status().isNotDead()) {
                    watchpoint  = new TeleFieldWatchpoint(WatchpointKind.CLIENT, this, description, teleObject, fieldActor, settings);
                } else {
                    String amendedDescription = (description == null) ? "" : description;
                    amendedDescription = amendedDescription + " (non-live object))";
                    final TeleFixedMemoryRegion region = teleObject.fieldMemoryRegion(fieldActor);
                    watchpoint = new TeleRegionWatchpoint(WatchpointKind.CLIENT, this, amendedDescription, region, settings);
                }
                watchpoint = addClientWatchpoint(watchpoint);
            } finally {
                vm().unlock();
            }
            return watchpoint;
        }

        /**
         * Creates a new, active watchpoint that covers an element in an array in the VM.
         *
         * @param description text useful to a person, for example capturing the intent of the watchpoint
         * @param teleObject a heap object in the VM that contains the array
         * @param elementKind the type category of the array elements
         * @param arrayOffsetFromOrigin location relative to the object's origin of element 0 in the array
         * @param index index of the element to watch
         * @param settings initial settings for the watchpoint
         * @return a new watchpoint, if successful
         * @throws MaxWatchpointManager.MaxTooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
         * @throws MaxWatchpointManager.MaxDuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
         * @throws MaxVMBusyException if watchpoints cannot be set at present, presumably because the VM is running.
         */
        public VmWatchpoint createArrayElementWatchpoint(String description, TeleObject teleObject, Kind elementKind, int arrayOffsetFromOrigin, int index, WatchpointSettings settings)
            throws MaxWatchpointManager.MaxTooManyWatchpointsException, MaxWatchpointManager.MaxDuplicateWatchpointException, MaxVMBusyException {
            if (!vm().tryLock()) {
                throw new MaxVMBusyException();
            }
            VmWatchpoint watchpoint;
            try {
                if (teleObject.status().isNotDead()) {
                    watchpoint = new TeleArrayElementWatchpoint(WatchpointKind.CLIENT, this, description, teleObject, elementKind, arrayOffsetFromOrigin, index, settings);
                } else {
                    String amendedDescription = (description == null) ? "" : description;
                    amendedDescription = amendedDescription + " (non-live object))";
                    final Pointer address = teleObject.origin().plus(arrayOffsetFromOrigin + (index * elementKind.width.numberOfBytes));
                    final TeleFixedMemoryRegion region = new TeleFixedMemoryRegion(vm(), "", address, elementKind.width.numberOfBytes);
                    watchpoint = new TeleRegionWatchpoint(WatchpointKind.CLIENT, this, amendedDescription, region, settings);
                }
                watchpoint =  addClientWatchpoint(watchpoint);
            } finally {
                vm().unlock();
            }
            return watchpoint;
        }

        /**
         * Creates a new, active watchpoint that covers a field in an object's header in the VM.  If the object is live,
         * than this watchpoint will track the object's location during GC.
         * <p>
         * If the object is not live, a plain memory region watchpoint is returned, one that does not relocate.
         *
         * @param description text useful to a person, for example capturing the intent of the watchpoint
         * @param teleObject a heap object in the VM
         * @param headerField a field in the object's header
         * @param settings initial settings for the watchpoint
         * @return a new watchpoint, if successful
         * @throws MaxWatchpointManager.MaxTooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
         * @throws MaxWatchpointManager.MaxDuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
         * @throws MaxVMBusyException if watchpoints cannot be set at present, presumably because the VM is running.
         */
        public VmWatchpoint createHeaderWatchpoint(String description, TeleObject teleObject, HeaderField headerField, WatchpointSettings settings)
            throws MaxWatchpointManager.MaxTooManyWatchpointsException, MaxWatchpointManager.MaxDuplicateWatchpointException, MaxVMBusyException {
            if (!vm().tryLock()) {
                throw new MaxVMBusyException();
            }
            VmWatchpoint watchpoint;
            try {
                if (teleObject.status().isNotDead()) {
                    watchpoint = new TeleHeaderWatchpoint(WatchpointKind.CLIENT, this, description, teleObject, headerField, settings);
                } else {
                    String amendedDescription = (description == null) ? "" : description;
                    amendedDescription = amendedDescription + " (non-live object)";
                    final TeleFixedMemoryRegion region = teleObject.headerMemoryRegion(headerField);
                    watchpoint = new TeleRegionWatchpoint(WatchpointKind.CLIENT, this, amendedDescription, region, settings);
                }
                watchpoint =  addClientWatchpoint(watchpoint);
            } finally {
                vm().unlock();
            }
            return watchpoint;
        }

        /**
         * Creates a new, active watchpoint that covers a thread local variable in the VM.
         *
         * @param description text useful to a person, for example capturing the intent of the watchpoint
         * @param threadLocalVariable a thread local variable in the VM
         * @param settings initial settings for the watchpoint
         * @return a new watchpoint, if successful
         * @throws MaxWatchpointManager.MaxTooManyWatchpointsException if setting a watchpoint would exceed a platform-specific limit
         * @throws MaxWatchpointManager.MaxDuplicateWatchpointException if the region overlaps, in part or whole, with an existing watchpoint.
         * @throws MaxVMBusyException if watchpoints cannot be set at present, presumably because the VM is running.
         */
        public VmWatchpoint createVmThreadLocalWatchpoint(String description, MaxThreadLocalVariable threadLocalVariable, WatchpointSettings settings)
            throws MaxWatchpointManager.MaxTooManyWatchpointsException, MaxWatchpointManager.MaxDuplicateWatchpointException, MaxVMBusyException {
            if (!vm().tryLock(100)) {
                throw new MaxVMBusyException();
            }
            VmWatchpoint watchpoint;
            try {
                watchpoint = new TeleVmThreadLocalWatchpoint(WatchpointKind.CLIENT, this, description, threadLocalVariable, settings);
                watchpoint = addClientWatchpoint(watchpoint);
            } finally {
                vm().unlock();
            }
            return watchpoint;
        }

        /**
         * Find existing <strong>client</strong> watchpoints in the VM by location.
         * <p>
         * Returns an immutable collection; membership is thread-safe
         *
         * @param memoryRegion a memory region in the VM
         * @return all watchpoints whose memory regions overlap the specified region, empty sequence if none.
         */
        public List<MaxWatchpoint> findWatchpoints(MaxMemoryRegion memoryRegion) {
            List<MaxWatchpoint> watchpoints = Collections.emptyList();
            for (MaxWatchpoint maxWatchpoint : clientWatchpointsCache) {
                if (maxWatchpoint.memoryRegion().overlaps(memoryRegion)) {
                    if (watchpoints.isEmpty()) {
                        watchpoints = new ArrayList<MaxWatchpoint>(1);
                    }
                    watchpoints.add(maxWatchpoint);
                }
            }
            return Collections.unmodifiableList(watchpoints);
        }

        /**
         * Find an existing client watchpoint set in the VM.
         * <p>
         * Thread-safe
         *
         * @param address a memory address in the VM
         * @return the watchpoint whose memory region includes the address, null if none.
         */
        VmWatchpoint findClientWatchpointContaining(Address address) {
            for (MaxWatchpoint maxWatchpoint : clientWatchpointsCache) {
                if (maxWatchpoint.memoryRegion().contains(address)) {
                    return (VmWatchpoint) maxWatchpoint;
                }
            }
            return null;
        }

        public List<MaxWatchpoint> watchpoints() {
            // Hand out the cached, thread-safe summary
            return clientWatchpointsCache;
        }

        /**
         * Creates a new, inactive system watchpoint. This watchpoint is not shown in the list of current watchpoints.
         * This watchpoint has to be explicitly activated.
         *
         * @param description a human-readable description of the watchpoint's purpose, for debugging.
         * @param memoryRegion the memory region to watch.
         * @param settings initial settings for the watchpoint
         * @return a new, inactive system watchpoint
         * .
         * @throws MaxWatchpointManager.MaxTooManyWatchpointsException
         */
        private VmWatchpoint createSystemWatchpoint(String description, TeleFixedMemoryRegion memoryRegion, WatchpointSettings settings) throws MaxWatchpointManager.MaxTooManyWatchpointsException {
            final VmWatchpoint watchpoint = new TeleRegionWatchpoint(WatchpointKind.SYSTEM, this, description, memoryRegion, settings);
            return addSystemWatchpoint(watchpoint);
        }

        /**
         * Find an system watchpoint set at a particular location.
         * <p>
         * Thread-safe
         *
         * @param address a location in VM memory
         * @return a system watchpoint, null if none exists at the address.
         */
        public VmWatchpoint findSystemWatchpoint(Address address) {
            for (MaxWatchpoint maxWatchpoint : systemWatchpointsCache) {
                if (maxWatchpoint.memoryRegion().contains(address)) {
                    return (VmWatchpoint) maxWatchpoint;
                }
            }
            return null;
        }

        /**
         * Updates the watchpoint caches of memory contents.
         */
        void updateWatchpointMemoryCaches() {
            for (VmWatchpoint watchpoint : clientWatchpoints) {
                watchpoint.updateMemoryCache();
            }
        }

        /**
         * @return total number of existing watchpoints of all kinds.[
         */
        private int watchpointCount() {
            return clientWatchpoints.size() + systemWatchpoints.size();
        }

        private void updateAfterWatchpointChanges() {
            clientWatchpointsCache = Collections.unmodifiableList(new ArrayList<MaxWatchpoint>(clientWatchpoints));
            systemWatchpointsCache = new ArrayList<MaxWatchpoint>(systemWatchpoints);
            // Ensure that the manager listens for GC completion events iff
            // there are watchpoints.
            if (watchpointCount() > 0) {
                if (gcCompletedListener == null) {
                    gcCompletedListener = new MaxGCPhaseListener() {

                        public void gcPhaseChange(HeapPhase phase) {

                            for (MaxWatchpoint maxWatchpoint : clientWatchpointsCache) {
                                final VmWatchpoint watchpoint = (VmWatchpoint) maxWatchpoint;
                                Trace.line(TRACE_VALUE, watchpoint.tracePrefix() + "updating after GC: " + watchpoint);
                                watchpoint.updateAfterGC();
                            }
                        }
                    };
                    try {
                        vm().addGCPhaseListener(gcCompletedListener, HeapPhase.ALLOCATING);
                    } catch (MaxVMBusyException maxVMBusyException) {
                        TeleWarning.message("update after watchpoint changes failed to set GC completed listener", maxVMBusyException);
                        gcCompletedListener = null;
                    }
                }
            } else { // no watchpoints
                if (gcCompletedListener != null) {
                    try {
                        vm().removeGCPhaseListener(gcCompletedListener, HeapPhase.ALLOCATING);
                    } catch (MaxVMBusyException maxVMBusyException) {
                        TeleWarning.message("update after watchpoint changes failed to remove GC completed listener", maxVMBusyException);
                        gcCompletedListener = null;
                    }
                    gcCompletedListener = null;
                }
            }
            for (final MaxWatchpointListener listener : watchpointListeners) {
                listener.watchpointsChanged();
            }
        }

        /**
         * Adds a watchpoint to the list of current client watchpoints, and activates this watchpoint.
         * <p>
         * If the addition fails, the watchpoint is not activated.
         *
         * @param watchpoint the new client watchpoint, presumed to be inactive and not to have been added before.
         * @return the watchpoint, null if failed to create for some reason
         * @throws MaxWatchpointManager.MaxTooManyWatchpointsException
         * @throws MaxWatchpointManager.MaxDuplicateWatchpointException
         */
        private VmWatchpoint addClientWatchpoint(VmWatchpoint watchpoint)  throws MaxWatchpointManager.MaxTooManyWatchpointsException, MaxWatchpointManager.MaxDuplicateWatchpointException {
            assert watchpoint.kind == WatchpointKind.CLIENT;
            assert watchpoint.isAlive();
            assert !watchpoint.isActive();

            if (watchpointCount() >= teleProcess.platformWatchpointCount()) {
                throw new MaxWatchpointManager.MaxTooManyWatchpointsException("Number of watchpoints supported by platform (" +
                    teleProcess.platformWatchpointCount() + ") exceeded");
            }
            if (!clientWatchpoints.add(watchpoint)) {
                // TODO (mlvdv) call out special case where there's a hidden system watchpoint at the same location as this.
                // An existing watchpoint starts at the same location
                throw new MaxWatchpointManager.MaxDuplicateWatchpointException("Watchpoint already exists at location: " + watchpoint);
            }
            // Check for possible overlaps with predecessor or successor (according to start location)
            final VmWatchpoint lowerWatchpoint = clientWatchpoints.lower(watchpoint);
            final VmWatchpoint higherWatchpoint = clientWatchpoints.higher(watchpoint);
            if ((lowerWatchpoint != null && lowerWatchpoint.memoryRegion().overlaps(watchpoint.memoryRegion())) ||
                            (higherWatchpoint != null && higherWatchpoint.memoryRegion().overlaps(watchpoint.memoryRegion()))) {
                clientWatchpoints.remove(watchpoint);
                final StringBuilder msgBuilder = new StringBuilder();
                msgBuilder.append("Watchpoint already exists that overlaps with start=");
                msgBuilder.append(watchpoint.memoryRegion().start().toHexString());
                msgBuilder.append(", size=");
                msgBuilder.append(watchpoint.memoryRegion().nBytes());
                throw new MaxWatchpointManager.MaxDuplicateWatchpointException(msgBuilder.toString());
            }
            if (!heap().phase().isCollecting() || watchpoint.settings.enabledDuringGC) {
                // Try to activate the new watchpoint
                if (!watchpoint.setActive(true)) {
                    clientWatchpoints.remove(watchpoint);
                    return null;
                }
            }
            Trace.line(TRACE_VALUE, watchpoint.tracePrefix() + "added watchpoint: " + watchpoint);
            updateAfterWatchpointChanges();
            return watchpoint;
        }

        /**
         * Add a system watchpoint, assumed to be newly created.
         * <p>Does <strong>not</strong> activate the watchpoint.
         * <p>Does <strong>not</strong> check for overlap with existing watchpoints.
         *
         * @param watchpoint
         * @return the watchpoint
         * @throws MaxWatchpointManager.MaxTooManyWatchpointsException
         */
        private VmWatchpoint addSystemWatchpoint(VmWatchpoint watchpoint) throws MaxWatchpointManager.MaxTooManyWatchpointsException {
            if (watchpointCount() >= teleProcess.platformWatchpointCount()) {
                throw new MaxWatchpointManager.MaxTooManyWatchpointsException("Number of watchpoints supported by platform (" +
                    teleProcess.platformWatchpointCount() + ") exceeded");
            }
            systemWatchpoints.add(watchpoint);
            updateAfterWatchpointChanges();
            return watchpoint;
        }

        /**
         * Removes a memory watchpoint from the VM.
         * <p>
         * Notifies observers if a client watchpoint.
         *
         * @param watchpoint an existing, inactive watchpoint in the VM
         * @return true if successful
         */
        private boolean removeWatchpoint(VmWatchpoint watchpoint) {
            assert watchpoint.isAlive();
            assert !watchpoint.isActive();

            switch(watchpoint.kind) {
                case CLIENT: {
                    if (!clientWatchpoints.remove(watchpoint)) {
                        TeleError.unexpected(tracePrefix() + " Failed to remove watchpoint: " + watchpoint);
                    }
                    Trace.line(TRACE_VALUE, tracePrefix() + "Removed watchpoint: " + watchpoint);
                    updateAfterWatchpointChanges();
                    return true;
                }
                case SYSTEM: {
                    if (!systemWatchpoints.remove(watchpoint)) {
                        TeleError.unexpected(watchpoint.tracePrefix() + " Failed to remove watchpoint: " + watchpoint);
                    }
                    Trace.line(TRACE_VALUE, watchpoint.tracePrefix() + "Removed watchpoint: " + watchpoint);
                    return true;
                }
                default:
                    TeleError.unknownCase();
                    return false;
            }
        }

        public void writeSummary(PrintStream printStream) {
            printStream.println("Watchpoints :");
            for (MaxWatchpoint maxWatchpoint : clientWatchpointsCache) {
                printStream.println("  " + maxWatchpoint.toString());

            }
            for (MaxWatchpoint maxWatchpoint : systemWatchpoints) {
                printStream.println("  " + maxWatchpoint.toString());
            }
        }
    }

}
