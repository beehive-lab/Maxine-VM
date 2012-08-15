/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.store.txt.sbps;

import com.oracle.max.vm.ext.vma.store.txt.*;

/**
 * A compact variant of {@link SBPSTextVMAdviceHandlerLog} using {@link CompactTextVMAdviceHandlerLog}.
 */
public class SBPSCSFVMATextStore extends CSFVMATextStore {

    private final SBPSVMATextStore jdel;

    public SBPSCSFVMATextStore() {
        this(new SBPSVMATextStore());
    }

    protected SBPSCSFVMATextStore(SBPSVMATextStore del) {
        super(del);
        this.jdel = del;
    }

    @Override
    public void defineShortForm(CSFVMATextStore.ShortForm type, Object key, String shortForm, String classShortForm) {
        ClassNameId className = null;
        synchronized (jdel) {
            if (type == ShortForm.T) {
                jdel.defineThread(shortForm);
            }
            jdel.sb().append(type.code);
            jdel.sb().append(' ');
            if (type == ShortForm.C) {
                className = (ClassNameId) key;
                jdel.sb().append(className.name);
                jdel.sb().append(' ');
                jdel.sb().append(className.clId);
            } else if (type == ShortForm.T) {
                jdel.sb().append(key);
            } else {
                // F/M
                QualName qualName = (QualName) key;
                // guaranteed to have already created the short form for the class name
                jdel.sb().append(classShortForm);
                jdel.sb().append(' ');
                jdel.sb().append(qualName.name);
            }
            jdel.sb().append(' ');
            jdel.sb().append(shortForm);
            jdel.end();
        }
    }

}
