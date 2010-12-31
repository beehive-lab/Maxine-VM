/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.config.asm;

import static com.sun.max.platform.Platform.*;

import com.sun.max.config.*;
import com.sun.max.lang.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.runtime.*;

/**
 * Redirection for the set of Assembler packages to include in the image.
 *
 * @author Doug Simon
 */
public class Package extends BootImagePackage {

    private static String[] packages() {
        if (platform().isa == ISA.AMD64) {
            return new String[] {
                "com.sun.max.asm.*",
                "com.sun.max.asm.x86.*",
                "com.sun.max.asm.amd64.*",
                "com.sun.max.vm.asm.*",
                "com.sun.max.vm.asm.amd64.*"
            };
        }
        throw FatalError.unimplemented();
    }

    public Package() {
        super(packages());
    }

    @Override
    public boolean isPartOfMaxineVM(VMConfiguration vmConfiguration) {
        return CPSCompiler.Static.isCompiler(vmConfiguration);
    }
}
