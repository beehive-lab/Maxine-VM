/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.config.vm;

import com.sun.max.config.*;

/**
 * {@code com.sun.max.vm.*} packages to include in boot image.
 * We could simplify this to {@code com.sun.max.vm.**} but for a few
 * sub-packages that are not to be included.
 */
public class Package extends BootImagePackage {
    public Package() {
        super(
            "com.sun.max.atomic.*",
            "com.sun.max.memory.*",
            "com.sun.max.platform.*",
            "com.sun.max.vm.*",
            "com.sun.max.vm.actor.**",
            "com.sun.max.vm.bytecode.**",
            "com.sun.max.vm.code.*",
            "com.sun.max.vm.compiler.**",
            "com.sun.max.vm.classfile.*",
            "com.sun.max.vm.classfile.constant.*",
            "com.sun.max.vm.classfile.stackmap.*",
            "com.sun.max.vm.collect.*",
            "com.sun.max.vm.debug.*",
            "com.sun.max.vm.heap.**",
            "com.sun.max.vm.instrument.*",
            "com.sun.max.vm.jdk.**",
            "com.sun.max.vm.jni.*",
            "com.sun.max.vm.jvmti.*",
            "com.sun.max.vm.layout.**",
            "com.sun.max.vm.management.*",
            "com.sun.max.vm.monitor.**",
            "com.sun.max.vm.object.*",
            "com.sun.max.vm.profile.*",
            "com.sun.max.vm.profilers.sampling.*",
            "com.sun.max.vm.reference.**",
            "com.sun.max.vm.reflection.*",
            "com.sun.max.vm.run.**",
            "com.sun.max.vm.runtime.**",
            "com.sun.max.vm.stack.**",
            "com.sun.max.vm.tele.*",
            "com.sun.max.vm.test.*",
            "com.sun.max.vm.thread.*",
            "com.sun.max.vm.type.*",
            "com.sun.max.vm.value.*",
            "com.sun.max.vm.verifier.**",
            "com.oracle.max.asm.**",
            "com.sun.max.vm.t1x.**"
                        );
    }
}
