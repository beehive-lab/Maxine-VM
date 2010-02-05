/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
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
        return region.description();
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
