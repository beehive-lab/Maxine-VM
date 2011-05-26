/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.runtime;

import static com.oracle.max.vm.ext.vma.runtime.JavaVMAdviceHandlerEvents.EventType.*;
import static com.oracle.max.vm.ext.vma.runtime.JavaEventFlusher.*;

import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vm.ext.vma.runtime.JavaVMAdviceHandlerEvents.*;

import com.sun.max.vm.heap.*;
import com.sun.max.vm.thread.*;

/**
 * An implementation of {@link VMAdviceHandler} that stores the event data as (Java) objects in a per-thread
 * buffer, and delegates to a separate handler to process those events when the buffer is full.
 *
 * This implementation does no allocation in the user heap. New threads are allocated an event buffer from the immortal
 * heap. When an event is logged, an event record of the required type is taken from a preallocated list (created at
 * image build time) and added to that thread's event buffer. If the list is empty, the buffer is flushed
 * and the events reclaimed.
 *
 * Buffer flushing is currently single-threaded through the {@link #LogBuffer} class. Any thread that needs to flush its
 * buffer will block until the logger thread is done.
 */

public class JavaEventVMAdviceHandler extends ObjectStateHandlerAdaptor {
    private static final String EVENT_BUFSIZE_PROPERTY = "max.vma.eventbuf.size";
    private static final int DEFAULT_EVENTBUF_SIZE = 16 * 1024;
    private static final int DEFAULT_EVENTQUOTA_SIZE = 1024;
    private static final EventType[] EVENT_TYPE_VALUES = EventType.values();

    /**
     * Pre-allocated in boot image memory.
     */
    private static Event[][] eventLists;

    static {
        eventLists = new Event[EVENT_TYPE_VALUES.length][];
        for (EventType et : EVENT_TYPE_VALUES) {
            Event[] eventList = new Event[DEFAULT_EVENTQUOTA_SIZE];
            eventLists[et.ordinal()] = eventList;
            for (int i = 0; i < DEFAULT_EVENTQUOTA_SIZE; i++) {
                eventList[i] = et.newEvent();
            }
        }
    }

    private ThreadEventBuffer tb;
    private JavaEventFlusher eventFlusher;
    private volatile boolean finalising;

    static class ASyncRemovalTracker extends ObjectStateHandler.RemovalTracker {
        JavaEventVMAdviceHandler lta;

        ASyncRemovalTracker(JavaEventVMAdviceHandler lta) {
            this.lta = lta;
        }

        @Override
        public void removed(long id) {
            lta.storeGCEvent(Removal, id);
        }
    }

    /**
     * A {@link ThreadLocal} that holds the event buffer for a thread.
     */
    private static class ThreadEventBuffer extends ThreadLocal<EventBuffer> {
        @Override
        public EventBuffer initialValue() {
            EventBuffer result = null;
            try {
                Heap.enableImmortalMemoryAllocation();
                result = new EventBuffer(new Event[DEFAULT_EVENTBUF_SIZE]);
                assert result != null;
            } finally {
                Heap.disableImmortalMemoryAllocation();
            }
            return result;
        }
    }

    private Event getEvent(EventType et) {
        Event[] list = eventLists[et.ordinal()];
        int count = 0;
        while (count < 10) {
            synchronized (list) {
                for (Event event : list) {
                    if (event.owner == null) {
                        event.owner = VmThread.current();
                        return event;
                    }
                }
            }
            // Out of events of requested type, flush and try again
            eventFlusher.flushBuffer(tb.get());
            count++;
        }
        assert false;
        return null;
    }

    @Override
    public void initialise(ObjectStateHandler state) {
        super.initialise(state);
        try {
            Heap.enableImmortalMemoryAllocation();
            tb = new ThreadEventBuffer();
            super.setRemovalTracker(new ASyncRemovalTracker(this));
            eventFlusher = JavaEventFlusherFactory.create();
            eventFlusher.initialise(state);
        } finally {
            Heap.disableImmortalMemoryAllocation();
        }
    }

    @Override
    public void finalise() {
        finalising = true; // this will prevent daemon threads from logging any more events.
        // This is called in the main thread which, clearly, has not yet called {@link #threadTerminating}.
        adviseThreadTerminating(AdviceMode.BEFORE, VmThread.current());
        eventFlusher.finalise();
    }

    @Override
    public void adviseThreadStarting(AdviceMode adviceMode, VmThread vmThread) {
    }

    @Override
    public void adviseThreadTerminating(AdviceMode adviceMode, VmThread vmThread) {
        final EventBuffer buffer = tb.get();
        if (buffer != null) {
            eventFlusher.flushBuffer(buffer);
        }
    }

    private Event getCheckFlush(EventType evt) {
        if (finalising) {
            return null;
        }
        final EventBuffer buffer = tb.get();
        if (buffer.index >= buffer.events.length) {
            eventFlusher.flushBuffer(buffer);
        }
        final Event event = getEvent(evt);
        event.time = System.nanoTime();
        event.evtCodeAndValue = evt.ordinal();
        buffer.events[buffer.index++] = event;
        return event;
    }

    private Event storeGCEvent(EventType evt, long id) {
        Event event = getCheckFlush(evt);
        if (event != null) {
            event.evtCodeAndValue |= id << 8;
        }
        return event;
    }

    private ObjectEvent storeEvent(EventType evt, Object obj) {
        ObjectEvent event = (ObjectEvent) getCheckFlush(evt);
        if (event != null) {
            event.obj = obj;
        }
        return event;
    }

    private ObjectEvent storeEvent(EventType evt, Object obj, int index) {
        ObjectEvent event = (ObjectEvent) getCheckFlush(evt);
        if (event != null) {
            event.evtCodeAndValue |= index << 8;
            event.obj = obj;
        }
        return event;
    }

    public static EventType getEventType(Event event) {
        int evtInt = (int) event.evtCodeAndValue & 0xFF;
        return EVENT_TYPE_VALUES[evtInt];
    }

    public static int getArrayIndex(Event event) {
        return getPackedValue(event);
    }

    public static int getPackedValue(Event event) {
        return (int) (event.evtCodeAndValue >> 8);
    }

    @Override
    public void adviseGC(AdviceMode adviceMode) {
        storeGCEvent(GC, 0);
        super.adviseGC(adviceMode);
    }

    @Override
    protected void handleUnseen(Object obj) {
        storeEvent(UnseenObject, obj);
    }

    // BEGIN GENERATED CODE

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseAfterMultiNewArray(Object arg1, int[] arg2) {
        adviseAfterNewArray(arg1, arg2[0]);
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseAfterNew(Object arg1) {
        super.adviseAfterNew(arg1);
        storeEvent(NewObject, arg1);
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseAfterNewArray(Object arg1, int arg2) {
        super.adviseAfterNewArray(arg1, arg2);
        storeEvent(NewArray, arg1, arg2);
        MultiNewArrayHelper.handleMultiArray(this, arg1);
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayLoad(Object arg1, int arg2, Object arg3) {
        super.adviseBeforeArrayLoad(arg1, arg2, arg3);
        storeEvent(ArrayLoad, arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayLoad(Object arg1, int arg2, float arg3) {
        super.adviseBeforeArrayLoad(arg1, arg2, arg3);
        storeEvent(ArrayLoad, arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayLoad(Object arg1, int arg2, double arg3) {
        super.adviseBeforeArrayLoad(arg1, arg2, arg3);
        storeEvent(ArrayLoad, arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayLoad(Object arg1, int arg2, long arg3) {
        super.adviseBeforeArrayLoad(arg1, arg2, arg3);
        storeEvent(ArrayLoad, arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, Object arg3) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3);
        ArrayObjectValueEvent event = (ArrayObjectValueEvent) storeEvent(ArrayStoreObject, arg1, arg2);
        if (event != null) {
            event.value = arg3;
        }
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, float arg3) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3);
        ArrayFloatValueEvent event = (ArrayFloatValueEvent) storeEvent(ArrayStoreFloat, arg1, arg2);
        if (event != null) {
            event.value = arg3;
        }
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, long arg3) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3);
        ArrayLongValueEvent event = (ArrayLongValueEvent) storeEvent(ArrayStoreLong, arg1, arg2);
        if (event != null) {
            event.value = arg3;
        }
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, double arg3) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3);
        ArrayDoubleValueEvent event = (ArrayDoubleValueEvent) storeEvent(ArrayStoreDouble, arg1, arg2);
        if (event != null) {
            event.value = arg3;
        }
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetStatic(Object arg1, int arg2, Object arg3) {
        super.adviseBeforeGetStatic(arg1, arg2, arg3);
        storeEvent(GetStatic, arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetStatic(Object arg1, int arg2, double arg3) {
        super.adviseBeforeGetStatic(arg1, arg2, arg3);
        storeEvent(GetStatic, arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetStatic(Object arg1, int arg2, long arg3) {
        super.adviseBeforeGetStatic(arg1, arg2, arg3);
        storeEvent(GetStatic, arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetStatic(Object arg1, int arg2, float arg3) {
        super.adviseBeforeGetStatic(arg1, arg2, arg3);
        storeEvent(GetStatic, arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, Object arg3) {
        super.adviseBeforePutStatic(arg1, arg2, arg3);
        ObjectObjectValueEvent event = (ObjectObjectValueEvent) storeEvent(PutStaticObject, arg1, arg2);
        if (event != null) {
            event.value = arg3;
        }
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, double arg3) {
        super.adviseBeforePutStatic(arg1, arg2, arg3);
        ObjectDoubleValueEvent event = (ObjectDoubleValueEvent) storeEvent(PutStaticDouble, arg1, arg2);
        if (event != null) {
            event.value = arg3;
        }
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, long arg3) {
        super.adviseBeforePutStatic(arg1, arg2, arg3);
        ObjectLongValueEvent event = (ObjectLongValueEvent) storeEvent(PutStaticLong, arg1, arg2);
        if (event != null) {
            event.value = arg3;
        }
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, float arg3) {
        super.adviseBeforePutStatic(arg1, arg2, arg3);
        ObjectFloatValueEvent event = (ObjectFloatValueEvent) storeEvent(PutStaticFloat, arg1, arg2);
        if (event != null) {
            event.value = arg3;
        }
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetField(Object arg1, int arg2, Object arg3) {
        super.adviseBeforeGetField(arg1, arg2, arg3);
        storeEvent(GetField, arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetField(Object arg1, int arg2, double arg3) {
        super.adviseBeforeGetField(arg1, arg2, arg3);
        storeEvent(GetField, arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetField(Object arg1, int arg2, long arg3) {
        super.adviseBeforeGetField(arg1, arg2, arg3);
        storeEvent(GetField, arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetField(Object arg1, int arg2, float arg3) {
        super.adviseBeforeGetField(arg1, arg2, arg3);
        storeEvent(GetField, arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutField(Object arg1, int arg2, Object arg3) {
        super.adviseBeforePutField(arg1, arg2, arg3);
        ObjectObjectValueEvent event = (ObjectObjectValueEvent) storeEvent(PutFieldObject, arg1, arg2);
        if (event != null) {
            event.value = arg3;
        }
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutField(Object arg1, int arg2, double arg3) {
        super.adviseBeforePutField(arg1, arg2, arg3);
        ObjectDoubleValueEvent event = (ObjectDoubleValueEvent) storeEvent(PutFieldDouble, arg1, arg2);
        if (event != null) {
            event.value = arg3;
        }
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutField(Object arg1, int arg2, long arg3) {
        super.adviseBeforePutField(arg1, arg2, arg3);
        ObjectLongValueEvent event = (ObjectLongValueEvent) storeEvent(PutFieldLong, arg1, arg2);
        if (event != null) {
            event.value = arg3;
        }
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutField(Object arg1, int arg2, float arg3) {
        super.adviseBeforePutField(arg1, arg2, arg3);
        ObjectFloatValueEvent event = (ObjectFloatValueEvent) storeEvent(PutFieldFloat, arg1, arg2);
        if (event != null) {
            event.value = arg3;
        }
    }

    // GENERATED -- EDIT AND RUN JavaEventVMAdviceHandlerGenerator.main() TO MODIFY
    @Override
    public void adviseAfterInvokeSpecial(Object arg1) {
        super.adviseAfterInvokeSpecial(arg1);
        storeEvent(InvokeSpecial, arg1);
    }



}
