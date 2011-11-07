/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

// WARNING: Generated file, do not modify!

// To use the original JDWP specification as a basis, some styleguides have to be turned off
// Checkstyle: stop field name check

package com.sun.max.jdwp.constants;

public final class EventKind {
    public static final int SINGLE_STEP = 1;
    public static final int BREAKPOINT = 2;
    public static final int FRAME_POP = 3;
    public static final int EXCEPTION = 4;
    public static final int USER_DEFINED = 5;
    public static final int THREAD_START = 6;
    public static final int THREAD_DEATH = 7;
    public static final int THREAD_END = 7;
    public static final int CLASS_PREPARE = 8;
    public static final int CLASS_UNLOAD = 9;
    public static final int CLASS_LOAD = 10;
    public static final int FIELD_ACCESS = 20;
    public static final int FIELD_MODIFICATION = 21;
    public static final int EXCEPTION_CATCH = 30;
    public static final int METHOD_ENTRY = 40;
    public static final int METHOD_EXIT = 41;
    public static final int METHOD_EXIT_WITH_RETURN_VALUE = 42;
    public static final int MONITOR_CONTENDED_ENTER = 43;
    public static final int MONITOR_CONTENDED_ENTERED = 44;
    public static final int MONITOR_WAIT = 45;
    public static final int MONITOR_WAITED = 46;
    public static final int VM_START = 90;
    public static final int VM_INIT = 90;
    public static final int VM_DEATH = 99;
    public static final int VM_DISCONNECTED = 100;
}
