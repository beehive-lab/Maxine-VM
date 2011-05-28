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
package com.oracle.max.vma.tools.gen.vma.runtime;

import static com.sun.max.vm.t1x.T1XTemplateGenerator.*;
import static com.oracle.max.vm.ext.vma.runtime.JavaVMAdviceHandlerEvents.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.t1x.*;

/**
 * Generates the case statements for logging the events.
 */

@HOSTED_ONLY
public class LoggingJavaEventFlusherGenerator {
    public static void main(String[] args) {
        T1XTemplateGenerator.setGeneratingClass(LoggingJavaEventFlusherGenerator.class);
        generateAutoComment();
        for (EventType m : EventType.values()) {
            generate(m);
        }
    }

    private static void generate(EventType e) {
        String[] splitType = null;
        out.printf("            case %s: {%n", e.name());
        switch (e) {
            case PutStaticObject:
            case PutStaticLong:
            case PutStaticFloat:
            case PutStaticDouble:
                splitType = splitType(e, "Static");
                // Checkstyle: stop FallThrough check
            case PutFieldObject:
            case PutFieldLong:
            case PutFieldFloat:
            case PutFieldDouble:
                // Checkstyle: resume FallThrough check
                if (splitType == null) {
                    splitType = splitType(e, "Field");
                }
                out.printf("                Object%sValueEvent event = (Object%sValueEvent) thisEvent;%n", splitType[1], splitType[1]);
                out.printf("                logHandler.adviseBefore%s(event.obj, getPackedValue(event), event.value);%n", splitType[0]);
                break;

            case GetStatic:
            case GetField:
                out.printf("                ObjectEvent event = (ObjectEvent) thisEvent;%n");
                out.printf("                logHandler.adviseBefore%s(event.obj, getPackedValue(event), 0);%n", e.name());
                break;

            case ArrayStoreObject:
            case ArrayStoreLong:
            case ArrayStoreFloat:
            case ArrayStoreDouble:
                splitType = splitType(e, "ArrayStore");
                out.printf("                Array%sValueEvent event = (Array%sValueEvent) thisEvent;%n", splitType[1], splitType[1]);
                out.printf("                logHandler.adviseBefore%s(event.obj, getArrayIndex(event), event.value);%n", splitType[0]);
                break;

            case ArrayLoad:
                out.printf("                ObjectEvent event = (ObjectEvent) thisEvent;%n");
                out.printf("                logHandler.adviseBefore%s(event.obj, getArrayIndex(event), 0);%n", e.name());
                break;

            case NewObject:
                out.printf("                ObjectEvent event = (ObjectEvent) thisEvent;%n");
                out.printf("                logHandler.adviseAfterNew(event.obj);%n");
                break;

            case NewArray:
                out.printf("                ObjectEvent event = (ObjectEvent) thisEvent;%n");
                out.printf("                logHandler.adviseAfterNewArray(event.obj, getArrayIndex(event));%n");
                break;

            case InvokeSpecial:
                out.printf("                ObjectEvent event = (ObjectEvent) thisEvent;%n");
                out.printf("                logHandler.adviseAfter%s(event.obj);%n", e.name());
                break;

            case UnseenObject:
                out.printf("                ObjectEvent event = (ObjectEvent) thisEvent;%n");
                out.printf("                logHandler.unseenObject(event.obj);%n");
                break;

            case Removal:
                out.printf("                logHandler.removal(getPackedValue(thisEvent));%n");
                break;

            case GC:
                out.printf("                logHandler.adviseGC(AdviceMode.AFTER);%n");

        }
        out.printf("                break;%n");
        out.printf("            }%n");
    }

    private static String[] splitType(EventType e, String k) {
        final String[] result = new String[2];
        final String name = e.name();
        final int sx = name.indexOf(k);
        final int ex = sx + k.length();
        result[0] = name.substring(0, ex);
        result[1] = name.substring(ex);
        return result;
    }
}
