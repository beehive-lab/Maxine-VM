/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.object;

import java.lang.ref.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import com.sun.cri.ci.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.gcx.*;
import com.sun.max.vm.heap.gcx.rset.ctbl.*;
import com.sun.max.vm.layout.hom.*;
import com.sun.max.vm.layout.ohm.*;
import com.sun.max.vm.log.*;
import com.sun.max.vm.log.java.*;
import com.sun.max.vm.log.nat.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.reference.direct.*;
import com.sun.max.vm.run.java.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * A singleton factory that manages the creation and maintenance of instances of {@link TeleObject}, each of which is a
 * canonical surrogate for an object in the VM.
 * <p>
 * Instances are created reflectively, based on a table in which constructors for various subtypes of {@link TeleObject}
 * are registered.  This allows the factory to create a {@link TeleObject} instance of the most specific subtype of the
 * VM object for which a constructor is registered.
 * <p>
 * A {@link TeleObject} is intended to be cannonical, so the unique instance for each object can be retrieved either by
 * location or by OID. Exceptions to this can occur because of GC:
 * <ul>
 * <li>An object in the VM can be released by the running application and "collected" by the GC. As soon as this is
 * discovered, the {@TeleObject} that refers to it is marked "dead".
 * <li>During some phases of copying GC, there may be two instances that refer to what is semantically the same object:
 * one referring to the old copy and one referring to the new.</li>
 * <li>As soon as a duplication due to copying is discovered, the {@link TeleObject} that refers to the old copy is
 * marked "obsolete". It is possible to discover the {@link TeleObject} that refers to the newer copy of the object.</li>
 * <li>A {@link TeleObject} that is either "dead" or "obsolete" is removed from the maps and cannot be discovered,
 * either by location or OID.</li>
 * </ul>
 * @see TeleObject
 */
public final class TeleObjectFactory extends AbstractVmHolder implements TeleVMCache {

    private static final int TRACE_VALUE = 1;

    /**
     * Map:  Class -> counters.
     * <br>
     * Class-indexed statistics.
     * 0: number of objects in class updated
     * 1: total system time of updates for objects in class
     *
     */
    private static class TimerPerType extends HashMap<Class, long[]> {
        @Override
        public long[] get(Object key) {
            long[] time = super.get(key);
            if (time == null) {
                time = new long[2];
                super.put((Class) key, time);
            }
            return time;
        }
    }

    /**
     * A count associated with an instance of {@link Class}.
     */
    public static final class ClassCount {

        public final Class type;
        public int value = 0;

        private ClassCount(Class type) {
            this.type = type;
        }
    }

    public final class ObjectFactoryMapStats {

        public int mapSize;
        public int liveCount = 0;
        public int quasiCount = 0;
        public int deadCount = 0;
        public int collectedCount = 0;

        public ObjectFactoryMapStats() {
            int live = 0;
            int quasi = 0;
            int dead = 0;
            int collected = 0;

            for (WeakReference<TeleObject> weakRef : referenceToTeleObject.values()) {
                final TeleObject teleObject = weakRef.get();
                if (teleObject == null) {
                    collected++;
                } else {
                    switch(teleObject.reference().status()) {
                        case LIVE:
                            live++;
                            break;
                        case DEAD:
                            dead++;
                            break;
                        default:
                            quasi++;
                            break;
                    }
                }
            }
            this.mapSize = referenceToTeleObject.size();
            this.liveCount = live;
            this.quasiCount = quasi;
            this.deadCount = dead;
            this.collectedCount = collected;
        }

        public ObjectFactoryMapStats(int mapSize, int liveCount, int quasiCount, int deadCount, int collectedCount) {
            this.mapSize = mapSize;
            this.liveCount = liveCount;
            this.quasiCount = quasiCount;
            this.deadCount = deadCount;
            this.collectedCount = collectedCount;
        }
    }

    private static TeleObjectFactory teleObjectFactory;

    /**
     * @return the singleton manager for instances of {@link TeleObject}.
     */
    public static TeleObjectFactory make(TeleVM vm, long processEpoch) {
        if (teleObjectFactory == null) {
            teleObjectFactory = new TeleObjectFactory(vm, processEpoch);
        }
        return teleObjectFactory;
    }

    /**
     * Map: Reference to {@link Object}s in the VM --> canonical local {@link TeleObject} that represents the
     * object in the VM. Relies on References being canonical and GC-safe.
     */
    private  final Map<RemoteReference, WeakReference<TeleObject>> referenceToTeleObject = new ConcurrentHashMap<RemoteReference, WeakReference<TeleObject>>();

    private  final Map<RemoteReference, WeakReference<TeleObject>> referenceToTeleHeapFreeChunk = new HashMap<RemoteReference, WeakReference<TeleObject>>();
    private int heapFreeChunksCount = 0;

    /**
     * Map: OID --> {@link TeleObject}.
     * <br>
     * Synchronized so that it can be read outside the VM lock if needed.
     */
    private final Map<Long, WeakReference<TeleObject>> oidToTeleObject =
        Collections.synchronizedMap(new HashMap<Long, WeakReference<TeleObject>>());

    /**
     * Constructors for specific classes of tuple objects in the heap in the VM.
     * The most specific class that matches a particular {@link TeleObject} will
     * be used, in an emulation of virtual method dispatch.
     */
    private final Map<Class, Constructor> classToTeleTupleObjectConstructor = new HashMap<Class, Constructor>();

    private final TimedTrace updateTracer;

    /**
     * A printer for statistics concerning a cache update.
     */
    private final Object statsPrinter = new Object() {

        @Override
        public String toString() {
            final ObjectFactoryMapStats stats = lastUpdateMapStats;
            final StringBuilder msg = new StringBuilder();
            msg.append("#mapped=(live=").append(stats.liveCount).append(", quasi=").append(stats.quasiCount).append("), ");
            msg.append("#removed=(dead=").append(stats.deadCount).append(", collected=").append(stats.collectedCount).append(")");
            return msg.toString();
        }
    };

    /**
     * Total number of instances of {@link TeleObject} created during session.
     */
    private int objectsCreatedCount = 0;

    /**
     * Map: {@link Class} of a {@link TeleObject} --> counter for number of instances created during session.
     */
    private final HashMap<Class, ClassCount> objectsCreatedPerType = new HashMap<Class, ClassCount>() {

        @Override
        public ClassCount get(Object key) {
            ClassCount count = super.get(key);
            if (count == null) {
                count = new ClassCount((Class) key);
                put((Class) key, count);
            }
            return count;
        }
    };

    /**
     * The census of the map's content, as determined by the most recent refresh.
     */
    private ObjectFactoryMapStats lastUpdateMapStats = new ObjectFactoryMapStats(0, 0, 0, 0, 0);

    private long lastUpdateEpoch = -1L;

    private TeleObjectFactory(TeleVM vm, long processEpoch) {
        super(vm);
        assert vm().lockHeldByCurrentThread();
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " creating");
        tracer.begin();

        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " updating");

        // Representation for all tuple objects not otherwise mentioned
        classToTeleTupleObjectConstructor.put(Object.class, getConstructor(TeleTupleObject.class));
        // Some common Java classes
        classToTeleTupleObjectConstructor.put(String.class, getConstructor(TeleString.class));
        classToTeleTupleObjectConstructor.put(Enum.class, getConstructor(TeleEnum.class));
        classToTeleTupleObjectConstructor.put(ClassLoader.class, getConstructor(TeleClassLoader.class));
        // Default representation of scheme implementations; should be specialized for implementation-specific support
        classToTeleTupleObjectConstructor.put(AbstractMonitorScheme.class, getConstructor(TeleMonitorScheme.class));
        classToTeleTupleObjectConstructor.put(DirectReferenceScheme.class, getConstructor(TeleReferenceScheme.class));
        classToTeleTupleObjectConstructor.put(HeapSchemeAdaptor.class, getConstructor(TeleHeapScheme.class));
        classToTeleTupleObjectConstructor.put(HomLayoutScheme.class, getConstructor(TeleLayoutScheme.class));
        classToTeleTupleObjectConstructor.put(JavaRunScheme.class, getConstructor(TeleRunScheme.class));
        classToTeleTupleObjectConstructor.put(MonitorScheme.class, getConstructor(TeleMonitorScheme.class));
        classToTeleTupleObjectConstructor.put(OhmLayoutScheme.class, getConstructor(TeleLayoutScheme.class));
        // Maxine Actors
        classToTeleTupleObjectConstructor.put(FieldActor.class, getConstructor(TeleFieldActor.class));
        classToTeleTupleObjectConstructor.put(VirtualMethodActor.class, getConstructor(TeleVirtualMethodActor.class));
        classToTeleTupleObjectConstructor.put(StaticMethodActor.class, getConstructor(TeleStaticMethodActor.class));
        classToTeleTupleObjectConstructor.put(InterfaceMethodActor.class, getConstructor(TeleInterfaceMethodActor.class));
        classToTeleTupleObjectConstructor.put(InterfaceActor.class, getConstructor(TeleInterfaceActor.class));
        classToTeleTupleObjectConstructor.put(VmThread.class, getConstructor(TeleVmThread.class));
        classToTeleTupleObjectConstructor.put(PrimitiveClassActor.class, getConstructor(TelePrimitiveClassActor.class));
        classToTeleTupleObjectConstructor.put(ArrayClassActor.class, getConstructor(TeleArrayClassActor.class));
        classToTeleTupleObjectConstructor.put(ReferenceClassActor.class, getConstructor(TeleReferenceClassActor.class));
        // Memory management
        classToTeleTupleObjectConstructor.put(MemoryRegion.class, getConstructor(TeleMemoryRegion.class));
        classToTeleTupleObjectConstructor.put(LinearAllocationMemoryRegion.class, getConstructor(TeleLinearAllocationMemoryRegion.class));
        // Maxine code management
        classToTeleTupleObjectConstructor.put(TargetMethod.class, getConstructor(TeleTargetMethod.class));
        classToTeleTupleObjectConstructor.put(CodeRegion.class, getConstructor(TeleCodeRegion.class));
        classToTeleTupleObjectConstructor.put(SemiSpaceCodeRegion.class, getConstructor(TeleSemiSpaceCodeRegion.class));
        classToTeleTupleObjectConstructor.put(CodeManager.class, getConstructor(TeleCodeManager.class));
        // Maxine heap management
        classToTeleTupleObjectConstructor.put(BaseAtomicBumpPointerAllocator.class, getConstructor(TeleBaseAtomicBumpPointerAllocator.class));
        classToTeleTupleObjectConstructor.put(CardTableRSet.class, getConstructor(TeleCardTableRSet.class));
        classToTeleTupleObjectConstructor.put(ContiguousHeapSpace.class, getConstructor(TeleContiguousHeapSpace.class));
        classToTeleTupleObjectConstructor.put(FirstFitMarkSweepSpace.class, getConstructor(TeleFirstFitMarkSweepSpace.class));
        classToTeleTupleObjectConstructor.put(FreeHeapSpaceManager.class, getConstructor(TeleFreeHeapSpaceManager.class));
        classToTeleTupleObjectConstructor.put(HeapFreeChunk.class, getConstructor(TeleHeapFreeChunk.class));
        classToTeleTupleObjectConstructor.put(NoAgingRegionalizedNursery.class, getConstructor(TeleNoAgingRegionalizedNursery.class));
        classToTeleTupleObjectConstructor.put(TricolorHeapMarker.class, getConstructor(TeleTricolorHeapMarker.class));
        // Other Maxine support
        classToTeleTupleObjectConstructor.put(MaxineVM.class, getConstructor(TeleMaxineVM.class));
        classToTeleTupleObjectConstructor.put(VMConfiguration.class, getConstructor(TeleVMConfiguration.class));
        classToTeleTupleObjectConstructor.put(ClassRegistry.class, getConstructor(TeleClassRegistry.class));
        classToTeleTupleObjectConstructor.put(Kind.class, getConstructor(TeleKind.class));
        classToTeleTupleObjectConstructor.put(ObjectReferenceValue.class, getConstructor(TeleObjectReferenceValue.class));
        classToTeleTupleObjectConstructor.put(CiConstant.class, getConstructor(TeleCiConstant.class));
        classToTeleTupleObjectConstructor.put(HeapRegionInfo.class, getConstructor(TeleHeapRegionInfo.class));
        classToTeleTupleObjectConstructor.put(VMLog.class, getConstructor(TeleVMLog.class));
        classToTeleTupleObjectConstructor.put(VMLogArray.class, getConstructor(TeleVMLogArray.class));
        classToTeleTupleObjectConstructor.put(VMLogNative.class, getConstructor(TeleVMLogNative.class));
        // ConstantPool and PoolConstants
        classToTeleTupleObjectConstructor.put(ConstantPool.class, getConstructor(TeleConstantPool.class));
        classToTeleTupleObjectConstructor.put(CodeAttribute.class, getConstructor(TeleCodeAttribute.class));
        classToTeleTupleObjectConstructor.put(PoolConstant.class, getConstructor(TelePoolConstant.class));
        classToTeleTupleObjectConstructor.put(Utf8Constant.class, getConstructor(TeleUtf8Constant.class));
        classToTeleTupleObjectConstructor.put(StringConstant.class, getConstructor(TeleStringConstant.class));
        classToTeleTupleObjectConstructor.put(ClassConstant.Resolved.class, getConstructor(TeleClassConstant.Resolved.class));
        classToTeleTupleObjectConstructor.put(ClassConstant.Unresolved.class, getConstructor(TeleClassConstant.Unresolved.class));
        classToTeleTupleObjectConstructor.put(FieldRefConstant.Resolved.class, getConstructor(TeleFieldRefConstant.Resolved.class));
        classToTeleTupleObjectConstructor.put(FieldRefConstant.Unresolved.class, getConstructor(TeleFieldRefConstant.Unresolved.class));
        classToTeleTupleObjectConstructor.put(FieldRefConstant.UnresolvedIndices.class, getConstructor(TeleFieldRefConstant.UnresolvedIndices.class));
        classToTeleTupleObjectConstructor.put(ClassMethodRefConstant.Resolved.class, getConstructor(TeleClassMethodRefConstant.Resolved.class));
        classToTeleTupleObjectConstructor.put(ClassMethodRefConstant.Unresolved.class, getConstructor(TeleClassMethodRefConstant.Unresolved.class));
        classToTeleTupleObjectConstructor.put(ClassMethodRefConstant.UnresolvedIndices.class, getConstructor(TeleClassMethodRefConstant.UnresolvedIndices.class));
        classToTeleTupleObjectConstructor.put(InterfaceMethodRefConstant.Resolved.class, getConstructor(TeleInterfaceMethodRefConstant.Resolved.class));
        classToTeleTupleObjectConstructor.put(InterfaceMethodRefConstant.Unresolved.class, getConstructor(TeleInterfaceMethodRefConstant.Unresolved.class));
        classToTeleTupleObjectConstructor.put(InterfaceMethodRefConstant.UnresolvedIndices.class, getConstructor(TeleInterfaceMethodRefConstant.UnresolvedIndices.class));

        // Java language objects
        classToTeleTupleObjectConstructor.put(Class.class, getConstructor(TeleClass.class));
        classToTeleTupleObjectConstructor.put(Constructor.class, getConstructor(TeleConstructor.class));
        classToTeleTupleObjectConstructor.put(Field.class, getConstructor(TeleField.class));
        classToTeleTupleObjectConstructor.put(Method.class, getConstructor(TeleMethod.class));
        classToTeleTupleObjectConstructor.put(TypeDescriptor.class, getConstructor(TeleTypeDescriptor.class));
        classToTeleTupleObjectConstructor.put(SignatureDescriptor.class, getConstructor(TeleSignatureDescriptor.class));
        classToTeleTupleObjectConstructor.put(StackTraceElement.class, getConstructor(TeleStackTraceElement.class));

        tracer.end(statsPrinter);
    }

    public void updateCache(long epoch) {
        updateTracer.begin();
        assert vm().lockHeldByCurrentThread();
        TimerPerType timePerType = new TimerPerType();
        int liveCount = 0;
        int quasiCount = 0;
        int deadCount = 0;
        int collectedCount = 0;
        final Iterator<RemoteReference> iterator = referenceToTeleObject.keySet().iterator();
        while (iterator.hasNext()) {
            final RemoteReference remoteRef = iterator.next();
            final WeakReference<TeleObject> weakRef = referenceToTeleObject.get(remoteRef);
            TeleObject teleObject = weakRef.get();
            if (teleObject == null) {
                collectedCount++;
                iterator.remove();
            } else {
                switch(teleObject.reference().status()) {
                    case LIVE:
                        liveCount++;
                        break;
                    case DEAD:
                        deadCount++;
                        iterator.remove();
                        Trace.line(TRACE_VALUE + 1, tracePrefix() + ": DEAD reference removed from map " + teleObject.reference().toString() +
                                        " gc=\"" + remoteRef.gcDescription() + "\"");
                        break;
                    default:
                        quasiCount++;
                        break;
                }
                Class type = teleObject.getClass();
                long[] stats = timePerType.get(type);
                long s = System.currentTimeMillis();
                teleObject.updateCache(epoch);
                stats[1] += System.currentTimeMillis() - s;
                stats[0]++;
            }
        }
        lastUpdateMapStats = new ObjectFactoryMapStats(referenceToTeleObject.size(), liveCount, quasiCount, deadCount, collectedCount);
        lastUpdateEpoch = epoch;
        updateTracer.end(statsPrinter);

        // Check that we haven't stumbled into a very bad update situation with an object.
        for (Map.Entry<Class, long[]> entry : timePerType.entrySet()) {
            long[] stats = entry.getValue();
            long time = stats[1];
            if (time > 100) {
                long count = stats[0];
                Class key = entry.getKey();
                Trace.line(TRACE_VALUE, tracePrefix() + "Excessive refresh time for type " + key + ": " + count + " updated, total time=" + time + "ms");
            }
        }
    }

    /**
     * Registers a type of surrogate object to be created for a specific VM object type.
     * The local object must be a concrete subtype of {@link TeleTupleObject} and must have
     * a constructor that takes two arguments:  {@link TeleVM}, {@link RemoteReference}.
     *
     * @param vmClass the VM class for which a specialized representation is desired
     * @param localClass the class of local surrogates for the VM type objects.
     */
    public void register(Class vmClass, Class localClass) {
        classToTeleTupleObjectConstructor.put(vmClass, getConstructor(localClass));
    }

    /**
     * Factory method for canonical {@link TeleObject} surrogate for heap objects in the VM. Specific subclasses are
     * created for Maxine implementation objects of special interest, and for other objects for which special treatment
     * is desired.
     * <p>
     * Returns {@code null} for the distinguished zero {@link RemoteReference}.
     * <p>
     * Must be called with current thread holding the VM lock.
     * <p>
     * Care is taken to avoid I/O with the VM during synchronized access to the canonicalization map. There is a small
     * exception to this for {@link TeleTargetMethod}, which can lead to infinite regress if the constructors for
     * mutually referential objects (notably {@link TeleClassMethodActor}) also create {@link TeleObject}s.
     *
     * @param reference non-null location of a Java object in the VM
     * @return canonical local surrogate for the object
     * @throws TeleError if the reference is not live or otherwise illegitimate
     */
    public TeleObject make(RemoteReference reference) throws TeleError {
        assert reference != null;
        if (reference.isZero()) {
            return null;
        }

        if (reference.isTemporary()) {
            // TODO (mlvdv) this should become an error
            TeleWarning.message("Creating a TeleObject with a temporary Reference" + reference.toString() + " @" + reference.toOrigin().to0xHexString());
        }
        if (reference.status().isDead()) {
            // TODO (mlvdv) This should probably be an error when it all shakes out
            TeleWarning.message("Attempt to create TeleObject with a DEAD Reference" + reference.toString() + " @" + reference.toOrigin().to0xHexString());
        }

        //assert vm().lockHeldByCurrentThread();
        TeleObject teleObject = getTeleObject(reference);
        if (teleObject != null) {
            return teleObject;
        }

        if (reference.status().isForwarder()) {
            final TeleObject newCopyObject = objects().makeTeleObject(reference.followIfForwarded());
            switch (newCopyObject.kind()) {
                case TUPLE:
                    teleObject = new TeleTupleForwarderQuasi(vm(), reference);
                    break;
                case ARRAY:
                    final ClassActor classActor = newCopyObject.classActorForObjectType();
                    if (classActor == null) {
                        return null;
                    }
                    teleObject = new TeleArrayForwarderQuasi(vm(), reference, classActor.componentClassActor().kind, classActor.dynamicHub().specificLayout);
                    break;
                case HYBRID:
                    if (newCopyObject instanceof TeleDynamicHub) {
                        teleObject = new TeleDynamicHubForwarderQuasi(vm(), reference);
                    } else if (newCopyObject instanceof TeleStaticHub) {
                        teleObject = new TeleStaticHubForwarderQuasi(vm(), reference);
                    } else {
                        TeleError.unexpected(tracePrefix() + "unknown hub type");
                    }
                    break;
            }
        } else { // not a Forwarder
            // Most important of the roles played by a {@link TeleObject} is to capture
            // the type of the object at the specified location.  This gets done empirically,
            // by examining the meta-information stored with the presumed object.
            // Because of the meta-circular design, this relies on analysis of meta-information
            // in the VM that is also stored as objects (notably hubs and class actors).  This
            // must be done carefully in order to avoid circularities, which is why the initial
            // investigation must be done using the lowest level memory reading primitives.

            // Location of the {@link Hub} in the VM that describes the layout of the presumed object.
            RemoteReference hubReference = vm().referenceManager().zeroReference();

            // Location of the {@link ClassActor} in the VM that describes the type of the presumed object.
            RemoteReference classActorReference = vm().referenceManager().zeroReference();

            // Local copy of the {@link ClassActor} in the VM that describes the type of the presumed object.
            // We presume to have loaded exactly the same classes as in the VM, so we can use this local
            // copy for a kind of reflective access to the structure of the presumed object.
            ClassActor classActor = null;

            try {
                hubReference = reference.readHubAsRemoteReference();
                classActorReference = fields().Hub_classActor.readRemoteReference(hubReference);
                classActor = classes().makeClassActor(classActorReference);
            } catch (InvalidReferenceException invalidReferenceException) {
                Log.println("InvalidReferenceException reference: " + reference + "/" + reference.toOrigin() +
                                " hubReference: " + hubReference + "/" + hubReference.toOrigin() + " classActorReference: " +
                                classActorReference + "/" + classActorReference.toOrigin() + " classActor: " + classActor);
                return null;
            }

            // Must check for the static tuple case first; it doesn't follow the usual rules
            final RemoteReference hubhubReference = hubReference.readHubAsRemoteReference();
            final RemoteReference hubClassActorReference = fields().Hub_classActor.readRemoteReference(hubhubReference);
            final ClassActor hubClassActor = classes().makeClassActor(hubClassActorReference);
            final Class hubJavaClass = hubClassActor.toJava();  // the class of this object's hub
            if (StaticHub.class.isAssignableFrom(hubJavaClass)) {
                teleObject = getTeleObject(reference);
                if (teleObject == null) {
                    teleObject = new TeleStaticTuple(vm(), reference);
                }
            } else if (classActor.isArrayClass()) {
                // Check map again, just in case there's a race
                teleObject = getTeleObject(reference);
                if (teleObject == null) {
                    teleObject = new TeleArrayObject(vm(), reference, classActor.componentClassActor().kind, classActor.dynamicHub().specificLayout);
                }
            } else if (classActor.isHybridClass()) {
                final Class javaClass = classActor.toJava();
                // Check map again, just in case there's a race
                teleObject = getTeleObject(reference);
                if (teleObject == null) {
                    if (DynamicHub.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleDynamicHub(vm(), reference);
                    } else if (StaticHub.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleStaticHub(vm(), reference);
                    } else {
                        throw TeleError.unexpected(tracePrefix() + "invalid hybrid implementation type");
                    }
                }
            } else if (classActor.isTupleClass()) {
                // Check map again, just in case there's a race
                teleObject = getTeleObject(reference);
                if (teleObject == null) {
                    // Walk up the type hierarchy for the class, locating the most specific type
                    // for which a constructor is defined.
                    Constructor constructor = null;
                    for (Class javaClass = classActor.toJava(); javaClass != null; javaClass = javaClass.getSuperclass()) {
                        constructor = classToTeleTupleObjectConstructor.get(javaClass);
                        if (constructor != null) {
                            break;
                        }
                    }
                    if (constructor == null) {
                        TeleError.unexpected(tracePrefix() + "failed to find constructor for class" + classActor.toJava());
                    }
                    try {
                        teleObject = (TeleObject) constructor.newInstance(vm(), reference);
                    } catch (InstantiationException e) {
                        TeleError.unexpected();
                    } catch (IllegalAccessException e) {
                        TeleError.unexpected();
                    } catch (InvocationTargetException e) {
                        TeleError.unexpected(e);
                    }
                }
            } else {
                //throw TeleError.unexpected("invalid object implementation type");
                Trace.line(TRACE_VALUE, tracePrefix() + "failed to create object at apparently valid origin=0x" + reference.toOrigin().toHexString());
                return null;
            }
        }
        final WeakReference<TeleObject> teleObjectWeakReference = new WeakReference<TeleObject>(teleObject);
        if (reference.status().isDead()) {
            if (teleObject.classActorForObjectType().toJava() == HeapFreeChunk.class) {
                referenceToTeleHeapFreeChunk.put(reference, teleObjectWeakReference);
                heapFreeChunksCount++;
                teleObject.updateCache(vm().teleProcess().epoch());
            }
        } else {
            oidToTeleObject.put(teleObject.getOID(), teleObjectWeakReference);
            //Log.println("OID: " + teleObject.getOID() + " ref: " + teleObject.getCurrentOrigin());
            //assert oidToTeleObject.containsKey(teleObject.getOID());

            referenceToTeleObject.put(reference, teleObjectWeakReference);
            teleObject.updateCache(vm().teleProcess().epoch());

            objectsCreatedCount++;
            objectsCreatedPerType.get(teleObject.getClass()).value++;
        }

        return teleObject;
    }

    /**
     * Gets a VM object with a specified OID, if it exists.
     * <br>
     * Thread-safe; synchronized lookup does not require the VM lock.
     *
     * @return the {@link TeleObject} with specified OID, null if none exists.
     */
    public TeleObject lookupObject(long id) {
        WeakReference<TeleObject> teleObject = oidToTeleObject.get(id);
        return teleObject == null ? null : teleObject.get();
    }

    /**
     * @return the total number of {@link TeleObject} instances created during the session.
     */
    public int objectsCreatedCount() {
        return objectsCreatedCount;
    }

    /**
     * @return counters for each type of {@link TeleObject}, counting the number created during the session.
     */
    public Collection<ClassCount> objectsCreatedPerType() {
        return objectsCreatedPerType.values();
    }

    /**
     * @return census of the map's current contents, not necessarily the same as the census during the refresh.
     */
    public ObjectFactoryMapStats mapStats() {
        return new ObjectFactoryMapStats();
    }

    private Constructor getConstructor(Class clazz) {
        return Classes.getDeclaredConstructor(clazz, TeleVM.class, RemoteReference.class);
    }

    /**
     * @param reference location of an object in the VM
     * @return the (preferably) canonical instance of {@link TeleObject} corresponding to the VM object
     */
    private TeleObject getTeleObject(RemoteReference reference) {
        TeleObject teleObject = null;
        synchronized (referenceToTeleObject) {
            final WeakReference<TeleObject> teleObjectRef = referenceToTeleObject.get(reference);
            if (teleObjectRef != null) {
                teleObject = teleObjectRef.get();
            }
        }
        return teleObject;
    }

}
