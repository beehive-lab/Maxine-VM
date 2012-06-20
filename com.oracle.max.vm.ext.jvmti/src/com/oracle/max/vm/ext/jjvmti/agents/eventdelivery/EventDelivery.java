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

import static com.sun.max.vm.ext.jvmti.JVMTIConstants.*;

import java.lang.reflect.*;
import java.security.*;
import java.util.*;
import java.util.regex.*;

import com.oracle.max.vm.ext.jjvmti.agents.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.ext.jvmti.*;

/**
 * An agent to test event delivery. With no arguments the agent reports all events, otherwise the argument is
 * interpreted as a regular expression for events to report. Supports both {@link JJVMTIStd} and {@link JJVMTIMax},
 * registers both agents, selects one at runtime, default {@link JJVMTIStd}.
 *
 * Can be included in the boot image or dynamically loaded as a VM extension.
 */
public class EventDelivery extends NullJJVMTICallbacks implements JJVMTI.EventCallbacks {

    private static EventDelivery eventDelivery;
    private static String EventDeliveryArgs;
    private static boolean inEvent;

    private static HashMap<String, EventData> events = new HashMap<String, EventData>();

    static {
        eventDelivery = (EventDelivery) JJVMTIAgentAdapter.register(new EventDelivery());
        if (MaxineVM.isHosted()) {
            VMOptions.addFieldOption("-XX:", "EventDeliveryArgs", "arguments for eventdelivery JJVMTI agent");
        }
    }

    static class EventData {

        final boolean enabled;
        final int event;

        EventData(boolean enabled, int event) {
            this.event = event;
            this.enabled = enabled;
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
        eventDelivery.setEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, null);
    }

    private static void usage() {
        fail("usage: events=pattern");
    }

    private static void fail(String message) {
        Log.println(message);
        MaxineVM.exit(-1);
    }

    private static int eventFromName(String name) {
        // Checkstyle: stop
        if (name.equals("vmInit"))return JVMTI_EVENT_VM_INIT;
        else if (name.equals("vmDeath")) return JVMTI_EVENT_VM_DEATH;
        else if (name.equals("breakpoint")) return JVMTI_EVENT_BREAKPOINT;
        else if (name.equals("classLoad")) return JVMTI_EVENT_CLASS_LOAD;
        else if (name.equals("classFileLoadHook")) return JVMTI_EVENT_CLASS_FILE_LOAD_HOOK;
        else if (name.equals("compiledMethodLoad")) return JVMTI_EVENT_COMPILED_METHOD_LOAD;
        else if (name.equals("compiledMethodUnload")) return JVMTI_EVENT_COMPILED_METHOD_UNLOAD;
        else if (name.equals("garbageCollectionStart")) return JVMTI_EVENT_GARBAGE_COLLECTION_START;
        else if (name.equals("garbageCollectionFinish")) return JVMTI_EVENT_GARBAGE_COLLECTION_FINISH;
        else if (name.equals("methodEntry")) return JVMTI_EVENT_METHOD_ENTRY;
        else if (name.equals("methodExit")) return JVMTI_EVENT_METHOD_EXIT;
        else if (name.equals("threadStart")) return JVMTI_EVENT_THREAD_START;
        else if (name.equals("threadEnd")) return JVMTI_EVENT_THREAD_END;
        else if (name.equals("fieldAccess")) return JVMTI_EVENT_FIELD_ACCESS;
        else if (name.equals("fieldModification")) return JVMTI_EVENT_FIELD_MODIFICATION;
        else assert false;
        return -1;
        // Checkstyle: resume
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

        Pattern eventsPattern = Pattern.compile(pattern);

        Method[] methods = JJVMTI.EventCallbacks.class.getDeclaredMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            EventData eventData = new EventData(eventsPattern.matcher(methodName).matches(), eventFromName(methodName));
            events.put(methodName, eventData);
            if (eventData.enabled) {
                eventDelivery.setEventNotificationMode(JVMTI_ENABLE, eventData.event, null);
            }
        }

        if (events.get("vmInit").enabled) {
            System.out.println("VMInit");
        }
    }

    @Override
    public void garbageCollectionStart() {
        if (events.get("garbageCollectionStart").enabled) {
            System.out.println("GC Start");
        }
    }

    @Override
    public void garbageCollectionFinish() {
        if (events.get("garbageCollectionFinish").enabled) {
            System.out.println("GC Finish");
        }
    }

    @Override
    public void threadStart(Thread thread) {
        if (events.get("threadStart").enabled) {
            System.out.printf("Thread start: %s%n", thread);
        }
    }

    @Override
    public void threadEnd(Thread thread) {
        if (events.get("threadEnd").enabled) {
            System.out.printf("Thread end: %s%n", thread);
        }
    }

    @Override
    public void vmDeath() {
        if (events.get("vmDeath").enabled) {
            System.out.println("VMDeath");
        }
    }

    @Override
    public byte[] classFileLoadHook(ClassLoader loader, String name, ProtectionDomain protectionDomain, byte[] classData) {
        if (events.get("classFileLoadHook").enabled) {
            System.out.printf("ClassFile LoadHook: %s, loader %s%n", name, loader);
        }
        return null;
    }

    @Override
    public void breakpoint(Thread thread, MethodActor method, long location) {
        if (events.get("breakpoint").enabled) {
            System.out.printf("breakpoint%n");
        }
    }

    @Override
    public void classLoad(Thread thread, ClassActor klass) {
        if (events.get("classLoad").enabled) {
            System.out.printf("classLoad %s%n", klass.name());
        }
    }
    @Override
    public void compiledMethodLoad(MethodActor method, int codeSize, Address codeAddr, AddrLocation[] map, Object compileInfo) {
        if (events.get("compiledMethodLoad").enabled) {
            System.out.printf("compiledMethodLoad %s, address %x, size %d%n", method.format("%H.%n"), codeAddr.toLong(), codeSize);
        }
    }

    @Override
    public void compiledMethodUnload(MethodActor method, Address codeAddr) {

    }

    @Override
    public void methodEntry(Thread thread, MethodActor method) {
        if (!inEvent) {
            try {
                inEvent = true;
                if (events.get("methodEntry").enabled) {
                    System.out.printf("methodEntry %s%n", method.format("%H.%n"));
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
                if (events.get("methodExit").enabled) {
                    System.out.printf("methodExit %s  exception %b%n", method.format("%H.%n"), exception);
                }
            } finally {
                inEvent = false;
            }
        }
    }

    @Override
    public void fieldAccess(Thread thread, MethodActor method, long location, ClassActor klass, Object object, FieldActor field) {
        if (events.get("fieldAccess").enabled) {
            System.out.printf("fieldAccess %s%n", field.format("%H.%n"));
        }
    }

    @Override
    public void fieldModification(Thread thread, MethodActor method, long location, ClassActor klass, Object object, FieldActor field, Object newValue) {
        if (events.get("fieldModification").enabled) {
            System.out.printf("fieldModification %s%n", field.format("%H.%n"));
        }
    }

}
