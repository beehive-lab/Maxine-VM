/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.b.c.d.e.amd64;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.cps.b.c.d.e.*;
import com.sun.max.vm.cps.eir.amd64.*;

/**
 * @author Bernd Mathiske
 */
public class BcdeAMD64Compiler extends BcdeCompiler<AMD64EirGenerator> implements AMD64EirGeneratorScheme {

    private DirToAMD64EirTranslator dirToEirTranslator;

    public AMD64EirGenerator eirGenerator() {
        return dirToEirTranslator;
    }

    @HOSTED_ONLY
    public BcdeAMD64Compiler() {
        Platform platform = Platform.platform();
        ProgramError.check(platform.endianness() == Endianness.LITTLE);
        ProgramError.check(platform.isa == ISA.AMD64);
        ProgramError.check(platform.cpu == CPU.AMD64);
        ProgramError.check(platform.wordWidth() == WordWidth.BITS_64);
        dirToEirTranslator = new DirToAMD64EirTranslator(this);
    }

    @Override
    protected Class<? extends BuiltinVisitor> builtinTranslationClass() {
        return DirToAMD64EirBuiltinTranslation.class;
    }

}
