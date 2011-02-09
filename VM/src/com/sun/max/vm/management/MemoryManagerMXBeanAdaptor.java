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
package com.sun.max.vm.management;

import java.lang.management.*;
import java.util.*;

/**
 * Adaptor class for the Maxine implementations of MemoryManagerMXBean.
 *
 * @author Mick Jordan
 */

public class MemoryManagerMXBeanAdaptor implements MemoryManagerMXBean, MemoryManagerMXBeanPools {
    private List<MemoryPoolMXBean> pool = new ArrayList<MemoryPoolMXBean>();
    private String name;

    private MemoryManagerMXBeanAdaptor() {
    }

    public MemoryManagerMXBeanAdaptor(String name) {
        this.name = name;
    }

    public void add(MemoryPoolMXBean bean) {
        pool.add(bean);
    }

    public void remove(MemoryPoolMXBean bean) {
        pool.remove(bean);
    }

    public List<MemoryPoolMXBean> getAll() {
        return pool;
    }

    public String[] getMemoryPoolNames() {
        final String[] result = new String[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            final MemoryPoolMXBean bean = pool.get(i);
            result[i] = bean.getName();
        }
        return result;
    }

    public String getName() {
        return name;
    }

    public boolean isValid() {
        return true;
    }


}
