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
/*
 * @author Bernd Mathiske
 */

#include <strings.h>

#include "log.h"
#include "jni.h"
#include "proc.h"

#define MAX_SYSCALLS_BEFORE_GIVING_UP 10000

typedef struct Argument {
    jlong mappingSize;
    jlong heap;
} *Argument;

static int mappingFunction(void *data, const prmap_t *map, const char *name) {
    Argument a = (Argument) data;    
    if (map->pr_size == a->mappingSize) {
        a->heap = (jlong) map->pr_vaddr;
    }
    return 0;
}

/**
 * Step the tele VM process forward until the boot heap is loaded by it and report its address.
 *
 * Make the tele VM process execute stretches of code, stopping each time it exits from a syscall.
 * At each stop, iterate over the tele's mappings and look for one that has the exact same size 
 * as the boot heap and boot code regions of the tele VM combined.
 * When found, leave the tele process stopped and report the tele boot heap's address.
 */
JNIEXPORT jlong JNICALL 
Java_com_sun_max_tele_debug_solaris_SolarisTeleVM_nativeLoadBootHeap(JNIEnv *env, jclass c, jlong handle, jlong mappingSize) {
    struct ps_prochandle *ph = (struct ps_prochandle *) handle;
    int i;
    struct Argument a;
    a.mappingSize = mappingSize;
    a.heap = 0L;

    sysset_t syscalls;
    prfillset(&syscalls);    
    proc_Psetsysexit(ph, &syscalls);
    proc_Psync(ph);
    
    for (i = 0; i < MAX_SYSCALLS_BEFORE_GIVING_UP; i++) {
    	proc_Pupdate_maps(ph);
	
        if (proc_Psetrun(ph, 0, 0) != 0) {
            log_println("nativeLoadBootHeap: Psetrun failed");
            return 0L;
	    }
        if (proc_Pwait(ph, 0) != 0) {
            log_println("nativeLoadBootHeap: Pwait failed");
            return 0L;
	    }
        if (proc_Pmapping_iter(ph, mappingFunction, &a) != 0) {
            log_println("nativeLoadBootHeap: Pmapping_iter failed");
            return 0L;
        }
        if (a.heap != 0L) {
        	break;
	    }
    }

    premptyset(&syscalls);    
    proc_Psetsysexit(ph, &syscalls);
    proc_Psync(ph);
        
    return a.heap;
}
