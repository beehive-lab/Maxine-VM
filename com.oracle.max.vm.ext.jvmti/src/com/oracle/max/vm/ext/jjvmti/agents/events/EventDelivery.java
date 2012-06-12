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
package com.oracle.max.vm.ext.jjvmti.agents.events;

import static com.sun.max.vm.ext.jvmti.JVMTIConstants.*;

import java.lang.reflect.*;
import java.security.*;
import java.util.*;
import java.util.regex.*;

import com.sun.max.vm.*;
import com.sun.max.vm.ext.jvmti.*;

/**
 * An agent to test event delivery.
 * With no arguments the agent reports all events, otherwise the argument
 * is interpreted as a regular expression for events to report.
 */
public class EventDelivery {
    private static HashMap<String, EventData> events = new HashMap<String, EventData>();

    static class EventData {
        boolean enabled;
        int event;

        EventData(boolean enabled, int event) {
            this.enabled = enabled;
            this.event = event;
        }
    }

    public static void onLoad(String agentArgs) {
        boolean max = false;
        String pattern = ".*";
        String[] args = agentArgs.split(",");
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

        Pattern eventsPattern = Pattern.compile(pattern);

        JJVMTICommonAgentAdapter agent = null;
        if (max) {
        } else {
            agent = JJVMTIStdAgentAdapter.register(new StdAgentAdapter(new StdEventCallbackHandler(eventsPattern)));

        }
        for (EventData eventData : events.values()) {
            if (eventData.enabled) {
                agent.setEventNotificationMode(JVMTI_ENABLE, eventData.event, null);
            }
        }
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
        else assert false;
        return -1;
        // Checkstyle: resume
    }

    private static class CommonEventCallbackHandler implements JJVMTICommon.EventCallbacks {

        CommonEventCallbackHandler(Pattern pattern, Class<? extends JJVMTICommon.EventCallbacks> callbacks) {
            Method[] methods = callbacks.getMethods();
            for (Method method : methods) {
                if (JJVMTICommon.EventCallbacks.class.isAssignableFrom(method.getDeclaringClass())) {
                    String methodName = method.getName();
                    events.put(methodName, new EventData(pattern.matcher(methodName).matches(), eventFromName(methodName)));
                }
            }
        }

        @Override
        public void agentStartup() {
            // TODO Auto-generated method stub

        }

        @Override
        public void vmInit() {
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

        StdEventCallbackHandler(Pattern pattern) {
            super(pattern, JJVMTIStd.EventCallbacksStd.class);
        }

    }

    private static class StdAgentAdapter extends JJVMTIStdAgentAdapter {
        StdEventCallbackHandler cbh;

        StdAgentAdapter(StdEventCallbackHandler cbh) {
            this.cbh = cbh;
        }

        @Override
        public void agentStartup() {
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
        public void vmInit() {
            cbh.vmInit();
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
            if (events.get("methodEntry").enabled) {
                System.out.printf("methodEntry %s%n", method.getName());
            }
        }

        @Override
        public void methodExit(Thread thread, Member method, boolean exeception, Object returnValue) {
            if (events.get("methodExit").enabled) {
                System.out.printf("methodExit %s%n", method.getName());
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

}
