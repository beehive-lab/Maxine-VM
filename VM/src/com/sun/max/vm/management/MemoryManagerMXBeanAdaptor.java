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
    @Override
    public void add(MemoryPoolMXBean bean) {
        pool.add(bean);
    }

    @Override
    public void remove(MemoryPoolMXBean bean) {
        pool.remove(bean);
    }

    @Override
    public List<MemoryPoolMXBean> getAll() {
        return pool;
    }

    @Override
    public String[] getMemoryPoolNames() {
        final String[] result = new String[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            final MemoryPoolMXBean bean = pool.get(i);
            result[i] = bean.getName();
        }
        return result;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isValid() {
        return true;
    }


}
