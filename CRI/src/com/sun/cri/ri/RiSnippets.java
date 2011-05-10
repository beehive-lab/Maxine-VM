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
package com.sun.cri.ri;


/**
 * Set of runtime calls to be used when translating bytecodes. These calls
 * must be woven into the IR during translation in a phase where they are
 * subject to inlining and folding.
 *
 * @author Doug Simon
 */
public interface RiSnippets {

    /**
     * Gets the snippet call that links a native method.
     */
    RiSnippetCall link(RiMethod nativeMethod);

    /**
     * Gets the snippet call that implements the transition from the VM into native code.
     */
    RiSnippetCall enterNative(RiMethod nativeMethod);

    /**
     * Gets the snippet call that implements the transition from native code into the VM.
     */
    RiSnippetCall enterVM(RiMethod nativeMethod);
}
