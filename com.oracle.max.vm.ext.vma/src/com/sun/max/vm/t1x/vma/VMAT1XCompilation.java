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
package com.sun.max.vm.t1x.vma;

import java.util.*;

import com.oracle.max.vm.ext.vma.options.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.ClassMethodRefConstant;
import com.sun.max.vm.t1x.*;
import com.sun.max.vm.type.*;

/**
 * Overrides some {@link T1XCompilation} methods to provide finer compile time control over advising.
 * N.B. Entry to this class can only occur if some advising is occurring in the method being compiled.
 */
public class VMAT1XCompilation extends T1XCompilation {

    private VMAT1X oat1x;
    /**
     * Used to control whether the advised template is used for the bytecode compilation
     * or the default (altT1X) template. This is always reset after it is checked in {@link #getTemplate(T1XTemplateTag).
     */
    private boolean useVMATemplate = true;

    public VMAT1XCompilation(T1X t1x) {
        super(t1x);
        this.oat1x = (VMAT1X) t1x;
    }

    @Override
    protected void initCompile(ClassMethodActor method, CodeAttribute codeAttribute) {
        super.initCompile(method, codeAttribute);
        // we do not want code to be recompiled as the optimizing compiler does not
        // support advising currently.
        methodProfileBuilder = null;
    }


    @Override
    protected void emitInvokespecial(int index) {
        // We are only interested in tracking calls to constructors (currently)
        ClassMethodRefConstant classMethodRef = cp.classMethodAt(index);
        useVMATemplate = classMethodRef.name(cp).equals("<init>");
        super.emitInvokespecial(index);
    }

    @Override
    protected void emitFieldAccess(EnumMap<KindEnum, T1XTemplateTag> tags, int index) {
        // Depending on the runtime settings, reads and/or write tracking may be suppressed
        final boolean isGet = tags == T1XTemplateTag.GETFIELDS || tags == T1XTemplateTag.GETSTATICS;
        useVMATemplate = (VMAOptions.trackWrites && !isGet) || (VMAOptions.trackReads && isGet);
        super.emitFieldAccess(tags, index);

    }

    @Override
    protected T1XTemplate getTemplate(T1XTemplateTag tag) {
        T1X t1x = useVMATemplate ? oat1x : oat1x.getAltT1X();
        useVMATemplate = true;
        return t1x.templates.t1XTemplates[tag.ordinal()];
    }

}
