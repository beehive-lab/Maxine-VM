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
package com.oracle.max.vma.tools.gen;

import java.lang.reflect.*;

import com.oracle.max.vma.tools.gen.t1x.*;
import com.oracle.max.vma.tools.gen.vma.*;
import com.oracle.max.vma.tools.gen.vma.log.*;
import com.oracle.max.vma.tools.gen.vma.log.debug.*;
import com.oracle.max.vma.tools.gen.vma.log.dup.*;
import com.oracle.max.vma.tools.gen.vma.log.txt.*;
import com.oracle.max.vma.tools.gen.vma.runtime.*;

/**
 * Runs all the VMA generators.
 */
public class RunGenerators {

    private static final Class<?>[] generators = new Class<?>[] {
        VMABytecodesGenerator.class,
        BytecodeAdviceGenerator.class,
        VMAStaticBytecodeAdviceGenerator.class,
        NullVMAdviceHandlerGenerator.class,
        ObjectStateHandlerAdaptorGenerator.class,
        SyncLogVMAdviceHandlerGenerator.class,
        CountVMAdviceHandlerGenerator.class,
        VMAdviceHandlerLogGenerator.class,
        LoggingVMAdviceHandlerGenerator.class,
        TransientVMAdviceHandlerTypesGenerator.class,
        TransientVMAdviceHandlerGenerator.class,
        GCTestAdviceHandlerLogGenerator.class,
        DupVMAdviceHandlerLogGenerator.class,
        TextVMAdviceHandlerLogGenerator.class,
        CompactTextVMAdviceHandlerLogGenerator.class,
        LoggingAdviceRecordFlusherGenerator.class,
        VMAdviceTemplateGenerator.class
    };

    public static void main(String[] args) throws Exception {
        for (Class<?> klass : generators) {
            Method main = klass.getDeclaredMethod("main", String[].class);
            System.out.println("Running " + main);
            main.invoke(null, new Object[] {new String[0]});
        }
    }

}
