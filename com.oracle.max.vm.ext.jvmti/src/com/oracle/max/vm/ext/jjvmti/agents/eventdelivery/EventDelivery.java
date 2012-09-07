/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.jjvmti.agents.eventdelivery;

import static com.sun.max.vm.ext.jvmti.JVMTIEvents.*;
import static com.sun.max.vm.ext.jvmti.JVMTIConstants.*;

import java.lang.reflect.*;
import java.security.*;
import java.util.*;
import java.util.regex.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.ext.jvmti.*;

/**
 * An agent to test event delivery. With no arguments the agent reports all events, otherwise the argument is
 * interpreted as a regular expression for events to report.
 *
 * Can be included in the boot image or dynamically loaded as a VM extension.
 */
public class EventDelivery extends JJVMTIAgentAdapter implements JJVMTI.EventCallbacks {

    private static EventDelivery eventDelivery;
    private static String EventDeliveryArgs;
    private static boolean inEvent;

    private static HashMap<JVMTIEvents.E, EventData> events = new HashMap<JVMTIEvents.E, EventData>();

    static {
        eventDelivery = (EventDelivery) JJVMTIAgentAdapter.register(new EventDelivery());
        if (MaxineVM.isHosted()) {
            VMOptions.addFieldOption("-XX:", "EventDeliveryArgs", "arguments for eventdelivery JJVMTI agent");
        }
    }

    static class EventData {

        final boolean enabled;
        final JVMTIEvents.E event;

        EventData(boolean enabled, JVMTIEvents.E event) {
            this.event = event;
            this.enabled = enabled;
            events.put(event, this);
        }
    }

    /***
     * VM extension entry point.
     *
     * @param args
     */
    public static void onLoad(String agentArgs) {
        EventDeliveryArgs = agentArgs;
        eventDelivery.onBoot();
    }

    /**
     * Boot image entry point.
     */
    @Override
    public void onBoot() {
        eventDelivery.setEventNotificationMode(JVMTI_ENABLE, E.VM_INIT, null);
    }

    private static void usage() {
        fail("usage: events=pattern");
    }

    private static void fail(String message) {
        Log.println(message);
        MaxineVM.exit(-1);
    }

    private static JVMTIEvents.E eventFromName(String methodName) {
        String uMethodName = methodName.toUpperCase();
        for (JVMTIEvents.E event : JVMTIEvents.E.VALUES) {
            String name = event.name().replace("_", "");
            if (name.equals(uMethodName)) {
                return event;
            }
        }
        return null;
    }

    @Override
    public void vmInit() {
        String pattern = ".*";
        if (EventDeliveryArgs != null) {
            String[] args = EventDeliveryArgs.split(",");
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.startsWith("events")) {
                    int ix = arg.indexOf('=');
                    if (ix < 0) {
                        usage();
                    }
                    pattern = arg.substring(ix + 1);
                } else {
                    usage();
                }
            }
        }

        // prevents recursion and deadlock when code eviction first occurs (which holds a lock preventing compilation)
        ClassActor.fromJava(EventDelivery.class).findLocalClassMethodActor(
                        SymbolTable.makeSymbol("compiledMethodUnload"), null).makeTargetMethod();

        Pattern eventsPattern = Pattern.compile(pattern);

        Method[] methods = JJVMTI.EventCallbacks.class.getDeclaredMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            if (methodName.equals("onBoot")) {
                continue;
            }
            E event = eventFromName(methodName);
            boolean enabled = eventsPattern.matcher(methodName).matches() || eventsPattern.matcher(event.name()).matches();
            EventData eventData = new EventData(enabled, event);
            if (eventData.enabled) {
                eventDelivery.setEventNotificationMode(JVMTI_ENABLE, eventData.event, null);
            }
        }

        if (events.get(E.VM_INIT).enabled) {
            System.out.println(E.VM_INIT);
        }
    }

    @Override
    public void garbageCollectionStart() {
        if (events.get(E.GARBAGE_COLLECTION_START).enabled) {
            System.out.println(E.GARBAGE_COLLECTION_START);
        }
    }

    @Override
    public void garbageCollectionFinish() {
        if (events.get(E.GARBAGE_COLLECTION_FINISH).enabled) {
            System.out.println(E.GARBAGE_COLLECTION_FINISH);
        }
    }

    @Override
    public void threadStart(Thread thread) {
        if (events.get(E.THREAD_START).enabled) {
            System.out.printf("%s: %s%n", E.THREAD_START, thread);
        }
    }

    @Override
    public void threadEnd(Thread thread) {
        if (events.get(E.THREAD_END).enabled) {
            System.out.printf("%s: %s%n", E.THREAD_END, thread);
        }
    }

    @Override
    public void vmDeath() {
        if (events.get(E.VM_DEATH).enabled) {
            System.out.println(E.VM_DEATH);
        }
    }

    @Override
    public byte[] classFileLoadHook(ClassLoader loader, String name, ProtectionDomain protectionDomain, byte[] classData) {
        if (events.get(E.CLASS_FILE_LOAD_HOOK).enabled) {
            System.out.printf("%s: %s, loader %s%n", E.CLASS_FILE_LOAD_HOOK, name, loader);
        }
        return null;
    }

    @Override
    public void breakpoint(Thread thread, MethodActor method, long location) {
        if (events.get(E.BREAKPOINT).enabled) {
            System.out.println(E.BREAKPOINT);
        }
    }

    @Override
    public void classLoad(Thread thread, ClassActor klass) {
        if (events.get(E.CLASS_LOAD).enabled) {
            System.out.printf("%s %s%n", E.CLASS_LOAD, klass.name());
        }
    }

    @Override
    public void compiledMethodLoad(MethodActor method, int codeSize, Address codeAddr, AddrLocation[] map, Object compileInfo) {
        if (events.get(E.COMPILED_METHOD_LOAD).enabled) {
            System.out.printf("%s %s, address %x, size %d%n", E.COMPILED_METHOD_LOAD, method.format("%H.%n"), codeAddr.toLong(), codeSize);
        }
    }

    @Override
    public void compiledMethodUnload(MethodActor method, Address codeAddr) {
        if (events.get(E.COMPILED_METHOD_UNLOAD).enabled) {
            System.out.printf("%s %s, address %x%n", E.COMPILED_METHOD_UNLOAD, method.format("%H.%n"), codeAddr.toLong());
        }

    }

    @Override
    public void methodEntry(Thread thread, MethodActor method) {
        if (!inEvent) {
            try {
                inEvent = true;
                if (events.get(E.METHOD_ENTRY).enabled) {
                    System.out.printf("%s %s%n", E.METHOD_ENTRY, method.format("%H.%n"));
                }
            } finally {
                inEvent = false;
            }
        }
    }

    @Override
    public void methodExit(Thread thread, MethodActor method, boolean exception, Object returnValue) {
        if (!inEvent) {
            try {
                inEvent = true;
                if (events.get(E.METHOD_EXIT).enabled) {
                    System.out.printf("%s %s  exception %b%n", E.METHOD_EXIT, method.format("%H.%n"), exception);
                }
            } finally {
                inEvent = false;
            }
        }
    }

    @Override
    public void fieldAccess(Thread thread, MethodActor method, long location, ClassActor klass, Object object, FieldActor field) {
        if (events.get(E.FIELD_ACCESS).enabled) {
            System.out.printf("%s %s%n", E.FIELD_ACCESS, field.format("%H.%n"));
        }
    }

    @Override
    public void fieldModification(Thread thread, MethodActor method, long location, ClassActor klass, Object object, FieldActor field, Object newValue) {
        if (events.get(E.FIELD_MODIFICATION).enabled) {
            System.out.printf("%s %s%n", E.FIELD_MODIFICATION, field.format("%H.%n"));
        }
    }

    @Override
    public void exception(Thread thread, MethodActor method, long location, Object exception, MethodActor catchMethod, long catchLocation) {
        if (events.get(E.EXCEPTION).enabled) {
            System.out.printf("%s %s in %s at %d ", E.EXCEPTION, exception.getClass().getName(), method.format("%H.%n"), location);
            if (catchMethod == null) {
                System.out.println("uncaught");
            } else {
                System.out.printf("caught in %s at %d%n", catchMethod.format("%H.%n"), catchLocation);
            }
        }
    }

    @Override
    public void exceptionCatch(Thread thread, MethodActor method, long location, Object exception) {
        if (events.get(E.EXCEPTION_CATCH).enabled) {
            System.out.printf("%s %s in %s at %d%n", E.EXCEPTION_CATCH, exception.getClass().getName(), method.format("%H.%n"), location);
        }
    }

    @Override
    public void dataDumpRequest() {
        if (events.get(E.DATA_DUMP_REQUEST).enabled) {
            System.out.println(E.DATA_DUMP_REQUEST);
        }
    }

    @Override
    public void dynamicCodeGenerated(String name, Address codeAddr, int length) {
        if (events.get(E.DATA_DUMP_REQUEST).enabled) {
            System.out.println(E.DATA_DUMP_REQUEST);
        }
    }

    @Override
    public void framePop(Thread thread, MethodActor method, boolean wasPoppedByException) {
        if (events.get(E.FRAME_POP).enabled) {
            System.out.printf("%s thread %s method %s exception %b%n", E.FRAME_POP, thread.getName(), method.format("%H.%n"), wasPoppedByException);
        }
    }

    @Override
    public void monitorContendedEnter(Thread thread, Object object) {
        if (events.get(E.MONITOR_CONTENDED_ENTER).enabled) {
            System.out.printf("%s %s%n", E.MONITOR_CONTENDED_ENTER, thread.getName());
        }
    }

    @Override
    public void monitorContendedEntered(Thread thread, Object object) {
        if (events.get(E.MONITOR_CONTENDED_ENTERED).enabled) {
            System.out.printf("%s %s%n", E.MONITOR_CONTENDED_ENTERED, thread.getName());
        }
    }

    @Override
    public void monitorWait(Thread thread, Object object, long timeout) {
        if (events.get(E.MONITOR_WAIT).enabled) {
            System.out.printf("%s %s %d%n", E.MONITOR_WAIT, thread.getName(), timeout);
        }
    }

    @Override
    public void monitorWaited(Thread thread, Object object, long timeout) {
        if (events.get(E.MONITOR_WAITED).enabled) {
            System.out.printf("%s %s %d%n", E.MONITOR_WAITED, thread.getName(), timeout);
        }
    }

    @Override
    public void objectFree(Object tag) {
        if (events.get(E.OBJECT_FREE).enabled) {
            System.out.println(E.OBJECT_FREE);
        }
    }

    @Override
    public void resourceExhausted(int flags, String description) {
        if (events.get(E.RESOURCE_EXHAUSTED).enabled) {
            System.out.println(E.RESOURCE_EXHAUSTED);
        }
    }

    @Override
    public void singleStep(Thread thread, MethodActor method, long location) {
        if (events.get(E.SINGLE_STEP).enabled) {
            System.out.printf("%s %s %d", E.SINGLE_STEP, method.format("%H.%n"), location);
        }
    }

    @Override
    public void vmObjectAlloc(Thread thread, Object object, ClassActor classActor, int size) {
        if (events.get(E.VM_OBJECT_ALLOC).enabled) {
            System.out.printf("%s %s%n", E.VM_OBJECT_ALLOC, classActor.format("%H"));
        }
    }


}
