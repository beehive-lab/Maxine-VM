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
package com.oracle.max.vm.ext.jjvmti.agents.fieldwatch;

import static com.sun.max.vm.ext.jvmti.JVMTIConstants.*;
import static com.sun.max.vm.ext.jvmti.JVMTIEvents.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.oracle.max.vm.ext.jjvmti.agents.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.ext.jvmti.*;

/**
 * A {@link JJVMTI Java JVMTI agent} that tests the field watch part of the interface.
 * Can be included in the boot image or dynamically loaded as a VM extension.
 */
public class FieldWatch  extends NullJJVMTICallbacks {
    private static FieldWatch fieldWatch;
    private static String FieldWatchArgs;

    static {
        fieldWatch = (FieldWatch) JJVMTIAgentAdapter.register(new FieldWatch());
        if (MaxineVM.isHosted()) {
            VMOptions.addFieldOption("-XX:", "FieldWatchArgs", FieldWatch.class, "arguments for fieldwatch JJVMTI agent");
        }
    }

    private static Pattern classPattern;
    private static Pattern fieldPattern;
    private static boolean read;
    private static boolean write;

    private static class Counter {
        long readCount;
        long writeCount;
    }

    private static ConcurrentHashMap<FieldActor, Counter> counters = new ConcurrentHashMap<FieldActor, Counter>();

    /***
     * VM extension entry point.
     * @param args
     */
    public static void onLoad(String agentArgs) {
        FieldWatchArgs = agentArgs;
        fieldWatch.onBoot();
    }

    /**
     * Boot image entry point.
     */
    @Override
    public void onBoot() {
        fieldWatch.setEventNotificationMode(JVMTI_ENABLE, E.VM_INIT, null);
    }

    @Override
    public void vmInit() {
        String patternString = null;
        if (FieldWatchArgs != null) {
            String[] args = FieldWatchArgs.split(",");
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.startsWith("pattern")) {
                    int ix = arg.indexOf('=');
                    if (ix < 0) {
                        fail("= expected after pattern");
                    }
                    patternString = arg.substring(ix + 1);
                } else if (arg.equals("read")) {
                    read = true;
                } else if (arg.equals("write")) {
                    write = true;
                } else {
                    if (patternString == null) {
                        patternString = arg;
                    } else {
                        fail("illegal option");
                    }
                }
            }
        }

        if (patternString == null) {
            patternString = ".*";
        }
        int ix = patternString.indexOf(':');
        if (ix < 0) {
            classPattern = Pattern.compile(patternString);
            fieldPattern = Pattern.compile(".*");
        } else {
            classPattern = Pattern.compile(patternString.substring(0, ix));
            fieldPattern = Pattern.compile(patternString.substring(ix + 1));
        }
        System.out.printf("cp %s, fp %s%n", classPattern.pattern(), fieldPattern.pattern());

        try {
            fieldWatch.addCapabilities(EnumSet.of(JVMTICapabilities.E.CAN_GENERATE_FIELD_ACCESS_EVENTS,
                                                  JVMTICapabilities.E.CAN_GENERATE_FIELD_MODIFICATION_EVENTS));
            fieldWatch.setEventNotificationMode(JVMTI_ENABLE, E.CLASS_LOAD, null);
            fieldWatch.setEventNotificationMode(JVMTI_ENABLE, E.FIELD_ACCESS, null);
            fieldWatch.setEventNotificationMode(JVMTI_ENABLE, E.FIELD_MODIFICATION, null);
            fieldWatch.setEventNotificationMode(JVMTI_ENABLE, E.VM_DEATH, null);
        } catch (JJVMTIException ex) {
            fail("initialization error: " + JVMTIError.getName(ex.error));
        }
    }

    @Override
    public void classLoad(Thread thread, ClassActor klass) {
        if (classPattern.matcher(klass.qualifiedName()).matches()) {
            List<FieldActor> fields = klass.getLocalFieldActors();
            for (FieldActor field : fields) {
                if (fieldPattern.matcher(field.name()).matches()) {
                    if (read) {
                        setFieldAccessWatch(field);
                    }
                    if (write) {
                        setFieldModificationWatch(field);
                    }
                }
            }
        }
    }

    @Override
    public void vmDeath() {
        for (Map.Entry<FieldActor, Counter> entry : counters.entrySet()) {
            System.out.printf("field %s, read %d, write %d%n", entry.getKey(), entry.getValue().readCount, entry.getValue().writeCount);
        }
    }

    @Override
    public void fieldAccess(Thread thread, MethodActor method, long location, ClassActor klass, Object object, FieldActor field) {
        getCounter(field).readCount++;
    }

    @Override
    public void fieldModification(Thread thread, MethodActor method, long location, ClassActor klass, Object object, FieldActor field, Object newValue) {
        getCounter(field).writeCount++;
    }

    private static Counter getCounter(FieldActor field) {
        Counter counter = counters.get(field);
        if (counter == null) {
            counter = new Counter();
            counters.put(field, counter);
        }
        return counter;
    }

    private static void fail(String message) {
        Log.println(message);
        MaxineVM.exit(-1);
    }

}
