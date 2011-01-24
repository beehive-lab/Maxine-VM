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
package com.sun.max.vm.cps.eir;

import static com.sun.max.platform.Platform.*;

import com.sun.max.config.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.cps.dir.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public abstract class EirGenerator<EirGeneratorScheme_Type extends EirGeneratorScheme>
    extends IrGenerator<EirGeneratorScheme_Type, EirMethod> {

    private final EirABIsScheme eirABIsScheme;

    public EirABIsScheme eirABIsScheme() {
        return eirABIsScheme;
    }

    private final WordWidth wordWidth;

    public WordWidth wordWidth() {
        return wordWidth;
    }

    protected EirGenerator(EirGeneratorScheme_Type eirGeneratorScheme) {
        super(eirGeneratorScheme, "EIR");
        final Platform platform = platform();
        wordWidth = platform.wordWidth();
        String p = Classes.getPackageName(EirGenerator.class) + "." + platform.isa.name().toLowerCase() + "." + platform.os.name().toLowerCase();
        eirABIsScheme = BootImagePackage.fromName(p).loadAndInstantiateScheme(EirABIsScheme.class);
    }

    @Override
    public final EirMethod createIrMethod(ClassMethodActor classMethodActor) {
        final EirMethod eirMethod = new EirMethod(classMethodActor, eirABIsScheme);
        notifyAllocation(eirMethod);
        return eirMethod;
    }

    public abstract EirLocation catchParameterLocation();

    public Kind eirKind(Kind kind) {
        final Kind k = kind.stackKind;
        if (k.isWord) {
            return (wordWidth == WordWidth.BITS_64) ? Kind.LONG : Kind.INT;
        }
        return k;
    }

    public EirMethod makeIrMethod(DirMethod dirMethod) {
        ProgramError.unexpected();
        return null;
    }
}
