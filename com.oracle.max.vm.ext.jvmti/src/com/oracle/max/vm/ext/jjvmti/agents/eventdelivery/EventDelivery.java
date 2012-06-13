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

import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.ext.jvmti.*;

/**
 * An agent to test event delivery.
 * With no arguments the agent reports all events, otherwise the argument
 * is interpreted as a regular expression for events to report.
 * Supports both {@link JJVMTIStd} and {@link JJVMTIMax}, registers
 * both agents, selects one at runtime, default {@link JJVMTIStd}.
 *
 * Can be included in the boot image or dynamically loaded as a VM extension.
 */
public class EventDelivery {
    private static JJVMTICommonAgentAdapter eventDelivery;
    private static StdAgentAdapter stdEventDelivery;
    private static MaxAgentAdapter maxEventDelivery;
    private static String EventDeliveryArgs;

    private static HashMap<String, EventData> events = new HashMap<String, EventData>();

    static {
        stdEventDelivery = new StdAgentAdapter(new StdEventCallbackHandler());
        maxEventDelivery = new MaxAgentAdapter(new MaxEventCallbackHandler());
        if (MaxineVM.isHosted()) {
            VMOptions.addFieldOption("-XX:", "EventDeliveryArgs", "arguments for classfunctions JJVMTI agent");
            JJVMTIStdAgentAdapter.register(stdEventDelivery);
            JJVMTIMaxAgentAdapter.register(maxEventDelivery);
        }
    }

    static class EventData {
        boolean enabled;
        int event;

        EventData(int event) {
            this.event = event;
        }
    }

    /***
     * VM extension entry point.
     * @param args
     */
    public static void onLoad(String agentArgs) {
        EventDeliveryArgs = agentArgs;
        boolean max = false;
        String[] args = agentArgs.split(",");
        for (String arg : args) {
            if (arg.equals("max")) {
                max = true;
                break;
            }
        }
        if (max) {
            JJVMTIMaxAgentAdapter.register(maxEventDelivery);
            eventDelivery = maxEventDelivery;
        } else {
            JJVMTIStdAgentAdapter.register(stdEventDelivery);
            eventDelivery = stdEventDelivery;
        }
        ((JJVMTICommon.EventCallbacks) eventDelivery).agentStartup();
    }

    private static void usage() {
        fail("usage: events=pattern,max");
    }

    private static void fail(String message) {
        Log.println(message);
        MaxineVM.exit(-1);
    }

    private static int eventFromName(String name) {
        // Checkstyle: stop
        if (name.equals("vmInit")) return JVMTI_EVENT_VM_INIT;
        else if (name.equals("vmDeath")) return JVMTI_EVENT_VM_DEATH;
        else if (name.equals("breakpoint")) return JVMTI_EVENT_BREAKPOINT;
        else if (name.equals("classLoad")) return JVMTI_EVENT_CLASS_LOAD;
        else if (name.equals("classFileLoadHook")) return JVMTI_EVENT_CLASS_FILE_LOAD_HOOK;
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

    private static class CommonEventCallbackHandler implements JJVMTICommon.EventCallbacks {

        CommonEventCallbackHandler(Class<? extends JJVMTICommon.EventCallbacks> callbacks) {
            Method[] methods = callbacks.getMethods();
            for (Method method : methods) {
                if (JJVMTICommon.EventCallbacks.class.isAssignableFrom(method.getDeclaringClass())) {
                    String methodName = method.getName();
                    events.put(methodName, new EventData(eventFromName(methodName)));
                }
            }
        }

        /**
         * Boot image entry point.
         */
        @Override
        public void agentStartup() {
            eventDelivery.setEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, null);
        }


        @Override
        public void vmInit() {
            boolean max = false;
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
                    } else if (arg.equals("max")) {
                        max = true;
                    } else {
                        usage();
                    }
                }
            }

            Pattern eventsPattern = Pattern.compile(pattern);

            if (MaxineVM.isHosted())  {
                if (max) {
                    eventDelivery = maxEventDelivery;
                    stdEventDelivery.disposeEnvironment();
                } else {
                    maxEventDelivery.disposeEnvironment();
                    eventDelivery = stdEventDelivery;
                }
            }

            for (Map.Entry<String, EventData> entry : events.entrySet()) {
                if (eventsPattern.matcher(entry.getKey()).matches()) {
                    EventData eventData = entry.getValue();
                    eventData.enabled = true;
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

    }

    private static class StdEventCallbackHandler extends CommonEventCallbackHandler {

        StdEventCallbackHandler() {
            super(JJVMTIStd.EventCallbacksStd.class);
        }

    }

    private static class StdAgentAdapter extends JJVMTIStdAgentAdapter implements JJVMTIStd.EventCallbacksStd {
        StdEventCallbackHandler cbh;
        private boolean inEvent;

        StdAgentAdapter(StdEventCallbackHandler cbh) {
            this.cbh = cbh;
        }

        @Override
        public void agentStartup() {
            cbh.agentStartup();
        }

        @Override
        public void vmInit() {
            cbh.vmInit();
        }

        @Override
        public byte[] classFileLoadHook(ClassLoader loader, String name, ProtectionDomain protectionDomain, byte[] classData) {
            return null;
        }

        @Override
        public void garbageCollectionStart() {
            cbh.garbageCollectionStart();
        }

        @Override
        public void garbageCollectionFinish() {
            cbh.garbageCollectionFinish();
        }

        @Override
        public void threadStart(Thread thread) {
            cbh.threadStart(thread);
        }

        @Override
        public void threadEnd(Thread thread) {
            cbh.threadEnd(thread);
        }

        @Override
        public void vmDeath() {
            cbh.vmDeath();
        }

        @Override
        public void breakpoint(Thread thread, Member method, long location) {
            if (events.get("breakpoint").enabled) {
                System.out.printf("breakpoint%n");
            }
        }

        @Override
        public void classLoad(Thread thread, Class< ? > klass) {
            if (events.get("classLoad").enabled) {
                System.out.printf("classLoad %s%n", klass.getName());
            }
        }

        @Override
        public void methodEntry(Thread thread, Member method) {
            if (!inEvent) {
                try {
                    inEvent = true;
                    if (events.get("methodEntry").enabled) {
                        System.out.printf("methodEntry %s%n", method.getName());
                    }
                } finally {
                    inEvent = false;
                }
            }
        }

        @Override
        public void methodExit(Thread thread, Member method, boolean exception, Object returnValue) {
            if (!inEvent) {
                try {
                    inEvent = true;
                    if (events.get("methodExit").enabled) {
                        System.out.printf("methodExit %s  exception %b%n", method.getName(), exception);
                    }
                } finally {
                    inEvent = false;
                }
            }
        }

        @Override
        public void fieldAccess(Thread thread, Method method, long location, Class< ? > klass, Object object, Field field) {
            if (events.get("fieldAccess").enabled) {
                System.out.printf("fieldAccess %s%n", field.getName());
            }
        }

        @Override
        public void fieldModification(Thread thread, Method method, long location, Class< ? > klass, Object object, Field field, Object newValue) {
            if (events.get("fieldModification").enabled) {
                System.out.printf("fieldModification %s%n", field.getName());
            }
        }

    }

    private static class MaxEventCallbackHandler extends CommonEventCallbackHandler {

        MaxEventCallbackHandler() {
            super(JJVMTIMax.EventCallbacksMax.class);
        }

    }

    private static class MaxAgentAdapter extends JJVMTIMaxAgentAdapter implements JJVMTIMax.EventCallbacksMax {
        MaxEventCallbackHandler cbh;
        private boolean inEvent;

        MaxAgentAdapter(MaxEventCallbackHandler cbh) {
            this.cbh = cbh;
        }

        @Override
        public void agentStartup() {
            cbh.agentStartup();
        }
        @Override
        public byte[] classFileLoadHook(ClassLoader loader, String name,
                        ProtectionDomain protectionDomain, byte[] classData) {
            return cbh.classFileLoadHook(loader, name, protectionDomain, classData);
        }
        @Override
        public void garbageCollectionStart() {
            cbh.garbageCollectionStart();
        }
        @Override
        public void garbageCollectionFinish() {
            cbh.garbageCollectionFinish();
        }
        @Override
        public void threadStart(Thread thread) {
            cbh.threadStart(thread);
        }
        @Override
        public void threadEnd(Thread thread) {
            cbh.threadEnd(thread);
        }
        @Override
        public void vmDeath() {
            cbh.vmDeath();
        }

        @Override
        public void vmInit() {
            cbh.vmInit();
        }

        @Override
        public void breakpoint(Thread thread, MethodActor method, long location) {
            if (events.get("breakpoint").enabled) {
                System.out.printf("breakpoint%n");
            }
        }

        @Override
        public void classLoad(Thread thread, ClassActor classActor) {
            if (events.get("classLoad").enabled) {
                System.out.printf("classLoad %s%n", classActor.name());
            }
        }

        @Override
        public void methodEntry(Thread thread, MethodActor methodActor) {
            if (!inEvent) {
                try {
                    inEvent = true;
                    if (events.get("methodEntry").enabled) {
                        System.out.printf("methodEntry %s%n", methodActor.format("%H.%n"));
                    }
                } finally {
                    inEvent = false;
                }
            }
        }

        @Override
        public void methodExit(Thread thread, MethodActor methodActor, boolean exception, Object returnValue) {
            if (!inEvent) {
                try {
                    inEvent = true;
                    if (events.get("methodExit").enabled) {
                        System.out.printf("methodExit %s  exception %b%n", methodActor.format("%H.%n"), exception);
                    }
                } finally {
                    inEvent = false;
                }
            }
        }

        @Override
        public void fieldAccess(Thread thread, MethodActor methodActor, long location, ClassActor classActor, Object object, FieldActor fieldActor) {
            if (events.get("fieldAccess").enabled) {
                System.out.printf("fieldAccess %s%n", fieldActor.format("%H.%n"));
            }
        }

        @Override
        public void fieldModification(Thread thread, MethodActor methodActor, long location, ClassActor classActor, Object object, FieldActor fieldActor, Object newValue) {
            if (events.get("fieldModification").enabled) {
                System.out.printf("fieldModification %s%n", fieldActor.format("%H.%n"));
            }
        }

    }

}
