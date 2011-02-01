/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.jdwp.vm.proxy;

import com.sun.max.jdwp.vm.core.*;

/**
 * Class representing a group of thread. This is a construct that is in the JDWP protocol and many Java IDEs (e.g. NetBeans, Eclipse) support grouped views of
 * thread based on this concept.
 *
 * @author Thomas Wuerthinger
 *
 */
public interface ThreadGroupProvider extends ObjectProvider {

    /**
     * @return the display name of the thread group
     */
    @ConstantReturnValue
    String getName();

    /**
     * @return the parent thread group or null if this is a top level thread group
     */
    ThreadGroupProvider getParent();

    /**
     * @return an array of child thread groups
     */
    ThreadGroupProvider[] getThreadGroupChildren();

    /**
     * @return an array of threads that are children of this thread group
     */
    ThreadProvider[] getThreadChildren();
}
