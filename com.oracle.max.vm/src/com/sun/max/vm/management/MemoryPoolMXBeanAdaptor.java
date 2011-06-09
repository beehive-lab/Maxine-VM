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
package com.sun.max.vm.management;

 import java.lang.management.*;

 import com.sun.max.memory.*;

/**
 * Adaptor class for the Maxine implementations of MemoryPoolMXBean.
 * Each instance has an associated MemoryRegion and an associated manager (MemoryManagerMXBean).
 * The management API supports multiple managers for a given memory pool but we don't exploit that currently.
 *
 */

public class MemoryPoolMXBeanAdaptor implements MemoryPoolMXBean {
    protected MemoryManagerMXBean manager;
    protected MemoryRegion region;
    private MemoryType type;

    private MemoryPoolMXBeanAdaptor() {
    }

    public MemoryPoolMXBeanAdaptor(MemoryType type, MemoryRegion region, MemoryManagerMXBean manager) {
        this.type = type;
        this.region = region;
    }

    public MemoryUsage getCollectionUsage() {
        return null;
    }

    public long getCollectionUsageThreshold() {
        return 0;
    }

    public long getCollectionUsageThresholdCount() {
        return 0;
    }

    public String[] getMemoryManagerNames() {
        return new String[] {manager.getName()};
    }

    public String getName() {
        return region.regionName();
    }

    public MemoryUsage getPeakUsage() {
        return null;
    }

    public MemoryType getType() {
        return type;
    }

    public MemoryUsage getUsage() {
        return region.getUsage();
    }

    public long getUsageThreshold() {
        return 0;
    }

    public long getUsageThresholdCount() {
        return 0;
    }

    public boolean isCollectionUsageThresholdExceeded() {
        return false;
    }

    public boolean isCollectionUsageThresholdSupported() {
        return false;
    }

    public boolean isUsageThresholdExceeded() {
        return false;
    }

    public boolean isUsageThresholdSupported() {
        return false;
    }

    public boolean isValid() {
        return true;
    }

    public void resetPeakUsage() {
    }

    public void setCollectionUsageThreshold(long threhsold) {
    }

    public void setUsageThreshold(long threshold) {
    }
}
