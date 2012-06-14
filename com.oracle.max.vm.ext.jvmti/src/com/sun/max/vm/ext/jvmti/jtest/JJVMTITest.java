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
package com.sun.max.vm.ext.jvmti.jtest;

import static com.sun.max.vm.ext.jvmti.JVMTIConstants.*;

import java.lang.reflect.*;
import java.security.*;
import java.util.*;
import java.util.regex.*;

import com.oracle.max.vm.ext.jjvmti.agents.util.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.ext.jvmti.*;
import com.sun.max.vm.hosted.*;

/**
 * A test class for the {@link JJVMTIStd} interface. Currently this must be built into the boot image as the VM cannot
 * (yet) be extended at runtime.
 */
class JJVMTITest {

    static final String JJVMTI_TEST = "max.jvmti.jtest";

    private static String JJVMTIInitArgs;
    private static String JJVMTIDeathArgs;
    private static String JJVMTIEvents;
    private static String JJVMTIMEArgs;
    private static boolean JJVMTIVerbose = true;


    static class CommandData {
        final Method method;
        boolean set;
        String args;

        CommandData(Method m) {
            this.method = m;
        }
    }

    static class EventData {
        boolean enabled;
        int event;

        EventData(boolean enabled, int event) {
            this.enabled = enabled;
            this.event = event;
        }
    }

    static {
        VMOptions.addFieldOption("-XX:", "JJVMTIInitArgs", "arguments for VmInit");
        VMOptions.addFieldOption("-XX:", "JJVMTIDeathArgs", "arguments for VmDeath");
        VMOptions.addFieldOption("-XX:", "JJVMTIEvents", "print JVMTI events");
        VMOptions.addFieldOption("-XX:", "JJVMTIMEArgs", "methods to print arguments");
        VMOptions.addFieldOption("-XX:", "JJVMTIVerbose", "verbose output");
    }

    static {
        JavaPrototype.registerInitializationCompleteCallback(new InitializationCompleteCallback());
    }

    @HOSTED_ONLY
    private static class InitializationCompleteCallback implements JavaPrototype.InitializationCompleteCallback {
        @Override
        public void initializationComplete() {
            JJVMTIAgentAdapter.register(new Agent(new CommandsHandler(JJVMTI.class, JJVMTI.EventCallbacks.class)));
        }
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

    private static void processArgs(String args, HashMap<String, CommandData> commands) {
        if (args != null) {
            for (String arg : args.split(",")) {
                String[] pair = arg.split("=");
                CommandData val = commands.get(pair[0]);
                if (val == null) {
                    System.out.println("unknown method, ignoring");
                    continue;
                }
                val.set = true;
                if (pair.length > 1) {
                    val.args = pair[1];
                }
            }
        }
    }

    private static class CommandsHandler {
        private HashMap<String, CommandData> initCommands = new HashMap<String, CommandData>();
        private HashMap<String, CommandData> deathCommands = new HashMap<String, CommandData>();
        private HashMap<String, EventData> events = new HashMap<String, EventData>();
        private Pattern methodArgsPattern;

        CommandsHandler(Class api, Class callbacks) {
            for (Method m : api.getMethods()) {
                initCommands.put(m.getName(), new CommandData(m));
                deathCommands.put(m.getName(), new CommandData(m));
            }
            for (Method m : callbacks.getMethods()) {
                if (!m.getName().equals("onBoot")) {
                    events.put(m.getName(), new EventData(false, eventFromName(m.getName())));
                }
            }

        }
        private void executeCommands(HashMap<String, CommandData> commands) {
            for (Map.Entry<String, CommandData> entry : commands.entrySet()) {
                CommandData commandData = entry.getValue();
                if (commandData.set) {
                    Method method = commandData.method;
                    Class<?>[] params = method.getParameterTypes();
                    Object[] args = new Object[params.length];
                    try {
                        Object result = method.invoke(this, args);
                        Class<?> resultType = method.getReturnType();
                        if (resultType == Class[].class) {
                            Class[] resultArray = (Class[]) result;
                            for (Class klass : resultArray) {
                                System.out.println(klass.getName());
                            }
                        } else if (resultType == Thread[].class) {
                            Thread[] resultArray = (Thread[]) result;
                            for (Thread thread : resultArray) {
                                System.out.println(thread);
                            }
                        } else if (resultType == Thread.class) {
                            Thread thread  = (Thread) result;
                            System.out.println(thread);
                        } else {
                            throw new IllegalArgumentException("unhandled result type");
                        }

                    } catch (InvocationTargetException ex) {
                        commandError(method.getName(), ex.getCause());
                    } catch (Throwable ex) {
                        commandError(method.getName(), ex);
                    }
                }
            }

        }
    }

    private static class Agent extends JJVMTIAgentAdapter implements JJVMTI.EventCallbacks {

        CommandsHandler commandsHandler;

        Agent(CommandsHandler commandsHandler) {
            this.commandsHandler = commandsHandler;
        }

        @Override
        public void onBoot() {
            // always take these two events
            setEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, null);
            setEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, null);
        }

        @Override
        public void vmInit() {
            if (JJVMTIEvents != null) {
                Pattern eventsPattern = Pattern.compile(JJVMTIEvents);
                for (Map.Entry<String, EventData> entry : commandsHandler.events.entrySet()) {
                    if (eventsPattern.matcher(entry.getKey()).matches()) {
                        // Checkstyle: stop
                        entry.getValue().enabled = true;
                        // Checkstyle: resume
                        setEventNotificationMode(JVMTI_ENABLE, eventFromName(entry.getKey()), null);
                    }
                }
            }
            if (JJVMTIMEArgs != null) {
                commandsHandler.methodArgsPattern = Pattern.compile(JJVMTIMEArgs);
            }
            if (commandsHandler.events.get("vmInit").enabled) {
                output("VMInit");
            }
            processArgs(JJVMTIInitArgs, commandsHandler.initCommands);
            processArgs(JJVMTIDeathArgs, commandsHandler.deathCommands);
            commandsHandler.executeCommands(commandsHandler.initCommands);
        }

        @Override
        public void garbageCollectionStart() {
            if (commandsHandler.events.get("garbageCollectionStart").enabled) {
                output("GC Start");
            }
        }

        @Override
        public void garbageCollectionFinish() {
            if (commandsHandler.events.get("garbageCollectionFinish").enabled) {
                output("GC Finish");
            }
        }

        @Override
        public void threadStart(Thread thread) {
            if (commandsHandler.events.get("threadStart").enabled) {
                if (JJVMTIVerbose) {
                    System.out.printf("Thread start: %s", thread);
                }
            }
        }

        @Override
        public void threadEnd(Thread thread) {
            if (commandsHandler.events.get("threadEnd").enabled) {
                if (JJVMTIVerbose) {
                    System.out.printf("Thread end: %s", thread);
                }
            }
        }

        @Override
        public void vmDeath() {
            if (commandsHandler.events.get("vmDeath").enabled) {
                output("VMDeath");
            }
            commandsHandler.executeCommands(commandsHandler.deathCommands);
        }

        @Override
        public byte[] classFileLoadHook(ClassLoader loader, String name, ProtectionDomain protectionDomain, byte[] classData) {
            if (commandsHandler.events.get("classFileLoadHook").enabled) {
                System.out.printf("ClassFile LoadHook: %s, loader %s%n", name, loader);
            }
            return null;
        }


        @Override
        public void breakpoint(Thread thread, MethodActor method, long location) {
            if (commandsHandler.events.get("breakpoint").enabled) {
                output("Breakpoint");
            }
        }

        @Override
        public void classLoad(Thread thread, ClassActor classActor) {
            if (commandsHandler.events.get("classLoad").enabled) {
                System.out.printf("Class Load: %s in thread %s%n", classActor.name(), thread);
            }
        }

        @Override
        public void methodEntry(Thread thread, MethodActor methodActor) {
            if (commandsHandler.events.get("methodEntry").enabled) {
                if (commandsHandler.methodArgsPattern != null) {
                    String string = methodActor.format("%H.%n");
                    if (commandsHandler.methodArgsPattern.matcher(string).matches()) {
                        System.out.printf("Method entry: %s%n", string);
                        Type[] params = methodActor.getGenericParameterTypes();
                        JJVMTI.LocalVariableEntry[] lve = getLocalVariableTable(methodActor);
                        for (int i = 0; i < params.length; i++) {
                            Class< ? > param = (Class) params[i];
                            int index = methodActor.isStatic() ? i : i + 1;
                            System.out.printf("param %d, name %s, type %s, value ", index, lve[index].name, lve[index].signature);
                            int slot = lve[index].slot;
                            if (Object.class.isAssignableFrom(param)) {
                                Object paramValue = getLocalObject(null, 0, slot);
                                System.out.println(paramValue);
                            } else {
                                if (param == int.class) {
                                    System.out.println(getLocalInt(null, 0, slot));
                                } else if (param == long.class) {
                                    System.out.println(getLocalLong(null, 0, slot));
                                } else if (param == float.class) {
                                    System.out.println(getLocalFloat(null, 0, slot));
                                } else if (param == double.class) {
                                    System.out.println(getLocalDouble(null, 0, slot));
                                } else {
                                    assert false;
                                }
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void methodExit(Thread thread, MethodActor methodActor, boolean exeception, Object returnValue) {
            if (commandsHandler.events.get("methodExit").enabled) {
                if (commandsHandler.methodArgsPattern != null) {
                    String string = methodActor.format("%H.%n");
                    if (commandsHandler.methodArgsPattern.matcher(string).matches()) {
                        // don't print return value as it invokes toString
                        System.out.printf("Method exit: %s, exception %b%n", string, exeception);
                    }
                }
            }
        }

        @Override
        public void fieldAccess(Thread thread, MethodActor method, long location, ClassActor classActor, Object object, FieldActor field) {
            // TODO Auto-generated method stub

        }

        @Override
        public void fieldModification(Thread thread, MethodActor method, long location, ClassActor classActor, Object object, FieldActor field, Object newValue) {
            // TODO Auto-generated method stub

        }
    }

    private static class MaxTest  {


    }
    private static void commandError(String name, Throwable ex) {
        System.err.printf("JJVMTI invocation %s failed: %s%n", name, ex);
    }

    private static void output(String msg) {
        if (JJVMTIVerbose) {
            System.out.println(msg);
        }
    }


}
