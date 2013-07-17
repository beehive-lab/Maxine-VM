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

package com.sun.max.config.graal;

import com.sun.max.config.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.jdk.*;


public class Package extends BootImagePackage {

    public Package() {
        super("com.oracle.max.vm.ext.graal.**",
              "com.oracle.graal.alloc.*",
              "com.oracle.graal.amd64.*",
              "com.oracle.graal.api.*",
              "com.oracle.graal.api.code.*",
              "com.oracle.graal.api.meta.*",
              "com.oracle.graal.bytecode.*",
              "com.oracle.graal.compiler.*",
              "com.oracle.graal.compiler.alloc.*",
              "com.oracle.graal.compiler.amd64.*",
              "com.oracle.graal.compiler.gen.*",
              "com.oracle.graal.compiler.target.*",
              "com.oracle.graal.debug.*",
              "com.oracle.graal.graph.**",
              "com.oracle.graal.java.*",
              "com.oracle.graal.lir.*",
              "com.oracle.graal.lir.amd64.*",
              "com.oracle.graal.loop.*",
              "com.oracle.graal.nodes.**",
              "com.oracle.graal.phases.**",
              "com.oracle.graal.printer.*",
              "com.oracle.graal.replacements.*",
              "com.oracle.graal.replacements.nodes.*",
              "com.oracle.graal.replacements.amd64.*",
              "com.oracle.graal.virtual.*"
              );

    }

    private static final String MEMBARNODE_CLASSNAME = "com.oracle.graal.nodes.extended.MembarNode";

    @Override
    public void loading() {
        if (this.name().equals("com.oracle.graal.replacements")) {
            UnsafeUsageChecker.addWhiteList("com.oracle.graal.replacements.SnippetCounter");
        } else if (this.name().equals("com.oracle.graal.nodes.extended")) {
            JDKInterceptor.addInterceptedField(MEMBARNODE_CLASSNAME,
                            new JDKInterceptor.FieldOffsetRecomputation("dummyOffset", new JDK.ClassRef(MEMBARNODE_CLASSNAME), "dummy"));
        } else if (this.name().equals("com.oracle.graal.api.code")) {
            // java.lang.invoke.CallSite causes unresolved problems in the boot image
            HostedBootClassLoader.omitClass("java.lang.invoke.CallSite");
        } else if (this.name().equals("com.oracle.graal.debug")) {
            JDKInterceptor.resetField("com.oracle.graal.debug.Debug", "ENABLED");
        }
    }

    @Override
    public boolean isPartOfMaxineVM(VMConfiguration vmConfig) {
        return CompilationBroker.optName().contains("Graal");
    }


}
