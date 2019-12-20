/*
 * Copyright (c) 2019, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */

/**
 * libNUMA wrappers for Maxine VM
 */
#include <numa.h>

#include "vm.h"

int numalib_available() {
    return numa_available();
}

int numaNodeOfAddress(jlong address) {
    int status = -1;
    void *ptr = (void *)address;
    numa_move_pages(0, 1, &ptr, NULL, &status, 0);
    return status;
}

int numaConfiguredCPUs() {
    return numa_num_configured_cpus();
}

int numaNodeOfCPU(int cpuID) {
    return numa_node_of_cpu(cpuID);
}

int numaPageSize() {
    return numa_pagesize();
}