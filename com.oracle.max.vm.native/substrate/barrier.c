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

/*
 * The functions in this module perform the membarrier Linux system call.
 * This system call causes an inter-processor-interrupt (IPI) to be delivered
 * to concurrently executing cores on the current system. On Aarch64 platforms
 * we use that mechanism to synchronise instruction streams on multi-cores.
 */

#include <stdlib.h>
#include <stdio.h>
#include "log.h"
#include "os.h"
#if os_LINUX
#include "isa.h"
#include <unistd.h>
#include <sys/syscall.h>
#include <linux/membarrier.h>
#include <linux/version.h>


/*
 * Pre-processor override for whether to compile in the membarrier system call.
 * Currently only affects Aarch64. See syscall_membarrier() in this compilation
 * unit.
 */
#ifndef USE_SYS_MEMBARRIER
# define USE_SYS_MEMBARRIER 1
#endif

/*
 * Local membarrier macro in the fashion of the one described in
 * the membarrier manual page.
 */
#define membarrier(cmd, flags) syscall(__NR_membarrier, cmd, flags)

/*
 * Initialise the system to use the best available barrier.
 */
static int membarrier_init(void) __attribute__ ((unused));

/*
 * Execute the membarrier system call
 */
void
syscall_membarrier()
{
#if isa_AARCH64
# if USE_SYS_MEMBARRIER
    static volatile int barrier_kind = 0;
    if (!barrier_kind) {
        barrier_kind = membarrier_init();
    }
    membarrier(barrier_kind, 0);
# endif /* USE_SYS_MEMBARRIER */
#else
    log_exit(1, "membarrier not configured on this platform");
#endif /* isa_AARCH64 */
}

static int
membarrier_init(void)
{
    long lv;

    lv = membarrier(MEMBARRIER_CMD_QUERY, 0);

    if (lv <= 0) {
        log_exit(1, "No barriers available on this platform.");
    }

#if LINUX_VERSION_CODE >= KERNEL_VERSION(4,14,0)
    /*
     * Check first for the availability of the expedited barrier that
     * limits the mask to CPUs running the current process only. This
     * barrier, if available, has a much lower overhead than the shared
     * barrier that will interrupt all cores on the system, regardless
     * of whether they are executing the current process.
     */
    if (lv & MEMBARRIER_CMD_PRIVATE_EXPEDITED) {
        /* Register our intention to use the expedited barrier, always
         * returns 0 so no need to test the return value.
         */
        membarrier(MEMBARRIER_CMD_REGISTER_PRIVATE_EXPEDITED, 0);
        if (log_MEMBARRIER) {
            log_println("Using private expedited barrier");
        }
        return MEMBARRIER_CMD_PRIVATE_EXPEDITED;
    }
#endif /* LINUX_VERSION_CODE >= KERNEL_VERSION(4,14,0) */

    /*
     * Fallback to the system-wide barrier if available.
     */
    if (lv & MEMBARRIER_CMD_SHARED) {
        if (log_MEMBARRIER) {
            log_println("Using shared barrier");
        }
        return MEMBARRIER_CMD_SHARED;
    }
    /* No useable barrier available. */
    log_exit(1, "No useable barrier on this platform.");
}

#else
/* Print an informative message if called on a non-Linux OS. */
void
syscall_membarrier()
{
    log_exit(1, "membarrier not available on this platform");
}
#endif /* os_LINUX */

