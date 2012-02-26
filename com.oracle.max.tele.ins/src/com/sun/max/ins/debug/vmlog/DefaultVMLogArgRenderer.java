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
package com.sun.max.ins.debug.vmlog;

import java.awt.*;
import java.lang.reflect.*;

import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.value.WordValueLabel.ValueMode;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.log.VMLog.Record;
import com.sun.max.vm.log.*;
import com.sun.max.vm.thread.*;

/**
 * Attempts to divine the types of the arguments based on the convention that
 * a logger will define a method {@code logXXX(...)} for the operation
 * defined as the enum constant {@code Operation.XXX}.
 */
public class DefaultVMLogArgRenderer extends VMLogArgRenderer {

    private final PlainLabel BEGIN_LABEL = new PlainLabel(vmLogView.inspection(), VMLogger.Interval.BEGIN.name());
    private final PlainLabel END_LABEL = new PlainLabel(vmLogView.inspection(), VMLogger.Interval.END.name());


    public DefaultVMLogArgRenderer(VMLogView vmLogView) {
        super(vmLogView);
    }

    private static class OperationLogger {
        Class<?> operationClass;
        Class<?> loggerClass;
        OperationLogger(Class<?> operationClass, Class<?> loggerClass) {
            this.operationClass = operationClass;
            this.loggerClass = loggerClass;
        }
    }

    @Override
    protected Component getRenderer(int header, int argNum, long argValue) {
        int op = Record.getOperation(header);
        VMLogger vmLogger = vmLogView.getLogger(Record.getLoggerId(header));
        String inspectedArg = vmLogger.inspectedArgValue(op, argNum, Address.fromLong(argValue));
        if (inspectedArg != null) {
            return new PlainLabel(vmLogView.inspection(), inspectedArg);
        }
        OperationLogger operationDefiningClass = getOperationDefiningClass(vmLogger);
        if (operationDefiningClass != null) {
            Enum[] enums = (Enum[]) operationDefiningClass.operationClass.getEnumConstants();
            if (enums != null) {
                if (op < enums.length) {
                    Enum e = enums[op];
                    Class<?>[] types = getParameterTypes(operationDefiningClass.loggerClass, e.name());
                    if (types == null) {
                        TeleError.unexpected("failed to get parameter types for log" + e.name());
                    }
                    if (argNum <= types.length) {
                        return getRenderer(types[argNum - 1], argValue);
                    }
                }
            }
        }
        return defaultRenderer(argValue);

    }

    private Component getRenderer(Class klass, long argValue) {
        if (Hub.class.isAssignableFrom(klass)) {
            // currently ClassID of Hub.classActor
            return safeGetReferenceValueLabel(getTeleClassActor(argValue));
        } else if (ClassActor.class.isAssignableFrom(klass)) {
            return safeGetReferenceValueLabel(getTeleClassActor(argValue));
        } else if (ClassMethodActor.class.isAssignableFrom(klass)) {
            return safeGetReferenceValueLabel(getTeleClassMethodActor(argValue));
        } else if (klass == VmThread.class) {
            return VMLogView.ThreadCellRenderer.getThreadRenderer((int) argValue);
        } else if (klass == VMLogger.Interval.class) {
            return argValue == 0 ? BEGIN_LABEL : END_LABEL;
        } else if (klass == int.class || klass == byte.class || klass == short.class || klass == long.class) {
            return new PlainLabel(vmLogView.inspection(), String.valueOf(argValue));
        } else if (klass == boolean.class) {
            return new PlainLabel(vmLogView.inspection(), argValue == 0 ? "false" : "true");
        } else {
            Method inspectedValueMethod = getInspectedValueMethod(klass);
            if (inspectedValueMethod != null) {
                try {
                    return new PlainLabel(vmLogView.inspection(), (String) inspectedValueMethod.invoke(null, Address.fromLong(argValue)));
                } catch (Exception ex) {
                }
            }
            if (klass == Pointer.class || klass == Address.class) {
                return new WordValueLabel(vmLogView.inspection(), ValueMode.WORD, Address.fromLong(argValue), vmLogView.getTable());
            } else if (Object.class.isAssignableFrom(klass)) {
                return new WordValueLabel(vmLogView.inspection(), ValueMode.REFERENCE, Address.fromLong(argValue), vmLogView.getTable());
            }
        }
        return defaultRenderer(argValue);
    }

    PlainLabel defaultRenderer(long argValue) {
        return new PlainLabel(inspection(), Long.toHexString(argValue));
    }

    private OperationLogger getOperationDefiningClass(VMLogger vmLogger) {
        Class< ? > klass = vmLogger.getClass();
        while (klass != null) {
            Class< ? >[] declaredClasses = klass.getDeclaredClasses();
            for (Class declaredClass : declaredClasses) {
                if (declaredClass.isEnum() && declaredClass.getSimpleName().equals("Operation")) {
                    return new OperationLogger(declaredClass, klass);
                }
            }
            klass = klass.getSuperclass();
        }
        return null;
    }

    private Class<?>[] getParameterTypes(Class<?> vmLoggerClass, String name) {
        Method[] methods = vmLoggerClass.getDeclaredMethods();
        String logName = "log" + name;
        for (Method method : methods) {
            if (method.getName().equals(logName)) {
                return method.getParameterTypes();
            }
        }
        return null;
    }

    private Method getInspectedValueMethod(Class<?> klass) {
        Method[] methods = klass.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals("inspectedValue")) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 1 && params[0].getSimpleName().equals("Word")) {
                    return method;
                }
            }
        }
        return null;

    }

}
