/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
#include "proc.h"
#include "log.h"
#include "isa.h"
#include "jni.h"
#include "os.h"
#include "threadLocals.h"
#include "solarisTeleProcess.h"
#include <sys/siginfo.h>

struct ps_lwphandle {
    struct ps_prochandle *lwp_proc;	/* process to which this lwp belongs */
    struct ps_lwphandle *lwp_hash;	/* hash table linked list */
    lwpstatus_t	lwp_status;	/* status when stopped */
    lwpsinfo_t	lwp_psinfo;	/* lwpsinfo_t from last Lpsinfo() */
    lwpid_t		lwp_id;		/* lwp identifier */
    int		lwp_state;	/* state of the lwp, see "libproc.h" */
    uint_t		lwp_flags;	/* SETHOLD and/or SETREGS */
    int		lwp_ctlfd;	/* /proc/<pid>/lwp/<lwpid>/lwpctl */
    int		lwp_statfd;	/* /proc/<pid>/lwp/<lwpid>/lwpstatus */
};

/*
 * Function copies from native register data structures to Java byte arrays. Does 3 things:
 * 1. Checks size of provided array lengths
 * 2. Canonicalizes the native register data structures
 * 3. Copies the canonicalized structures into the byte arrays
 */
static jboolean copyRegisters(JNIEnv *env, jobject  this, prgregset_t osRegisters, prfpregset_t *osFloatingPointRegisters,
        jbyteArray integerRegisters, jint integerRegistersLength,
        jbyteArray floatingPointRegisters, jint floatingPointRegistersLength,
        jbyteArray stateRegisters, jint stateRegistersLength) {
    isa_CanonicalIntegerRegistersStruct canonicalIntegerRegisters;
    isa_CanonicalStateRegistersStruct canonicalStateRegisters;
    isa_CanonicalFloatingPointRegistersStruct canonicalFloatingPointRegisters;

    if (integerRegistersLength > sizeof(canonicalIntegerRegisters)) {
        log_println("buffer for integer register data is too large");
        return false;
    }

    if (stateRegistersLength > sizeof(canonicalStateRegisters)) {
        log_println("buffer for state register data is too large");
        return false;
    }

    if (floatingPointRegistersLength > sizeof(canonicalFloatingPointRegisters)) {
        log_println("buffer for floating point register data is too large");
        return false;
    }

    isa_canonicalizeTeleIntegerRegisters(&osRegisters[0], &canonicalIntegerRegisters);
    isa_canonicalizeTeleStateRegisters(&osRegisters[0], &canonicalStateRegisters);
    isa_canonicalizeTeleFloatingPointRegisters(osFloatingPointRegisters, &canonicalFloatingPointRegisters);

    (*env)->SetByteArrayRegion(env, integerRegisters, 0, integerRegistersLength, (void *) &canonicalIntegerRegisters);
    (*env)->SetByteArrayRegion(env, stateRegisters, 0, stateRegistersLength, (void *) &canonicalStateRegisters);
    (*env)->SetByteArrayRegion(env, floatingPointRegisters, 0, floatingPointRegistersLength, (void *) &canonicalFloatingPointRegisters);
    return true;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_channel_natives_TeleChannelNatives_readRegisters(JNIEnv *env, jobject  this, jlong processHandle, jlong lwpId,
		jbyteArray integerRegisters, jint integerRegistersLength,
		jbyteArray floatingPointRegisters, jint floatingPointRegistersLength,
		jbyteArray stateRegisters, jint stateRegistersLength) {
    prgregset_t osRegisters;
    prfpregset_t osFloatingPointRegisters;

    struct ps_prochandle *ph = (struct ps_prochandle *) processHandle;
	if (Plwp_getregs(ph, lwpId, osRegisters) != 0) {
		log_println("Plwp_getregs failed");
		return false;
	}
	if (Plwp_getfpregs(ph, lwpId, &osFloatingPointRegisters) != 0) {
		log_println("Plwp_getfpregs failed");
		return false;
	}

	return copyRegisters(env, this, &osRegisters[0], &osFloatingPointRegisters,
	                integerRegisters, integerRegistersLength,
	                floatingPointRegisters, floatingPointRegistersLength,
	                stateRegisters, stateRegistersLength);
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_solaris_SolarisDumpThreadAccess_lwpRegisters(JNIEnv *env, jclass  class,  jobject bytebuffer,
                jbyteArray integerRegisters, jint integerRegistersLength,
                jbyteArray floatingPointRegisters, jint floatingPointRegistersLength,
                jbyteArray stateRegisters, jint stateRegistersLength) {
    lwpstatus_t * lwpstatus = (lwpstatus_t *) ((*env)->GetDirectBufferAddress(env, bytebuffer));
    return copyRegisters(env, class, &lwpstatus->pr_reg[0], &lwpstatus->pr_fpreg,
                    integerRegisters, integerRegistersLength,
                    floatingPointRegisters, floatingPointRegistersLength,
                    stateRegisters, stateRegistersLength);
}

static jboolean setRegister(jlong processHandle, jlong lwpId, int registerIndex, jlong value) {
	struct ps_prochandle *ph = (struct ps_prochandle *) processHandle;
	INIT_LWP_HANDLE(lh, processHandle, lwpId, false);

    /* This is only called after a Pwait so all threads should be stopped. */
    c_ASSERT((lh->lwp_status.pr_flags & PR_STOPPED) != 0);

#if 0
    Lputareg(lh, registerIndex, value);
#else
	/* We use Plwp_getregs & Plwp_setregs instead of Lputareg as the latter is buggy. */
    prgregset_t osRegisters;
    if (Plwp_getregs(ph, lwpId, osRegisters) != 0) {
        log_println("Plwp_getregs failed");
        Lfree(lh);
        return false;
    }
    osRegisters[registerIndex] = value;
    Plwp_setregs(ph, lwpId, osRegisters);
#endif

    Lsync(lh);
    Lfree(lh);
    return true;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_channel_natives_TeleChannelNatives_setInstructionPointer(JNIEnv *env, jobject  this, jlong processHandle, jlong lwpId, jlong address) {
	return setRegister(processHandle, lwpId, R_PC, address);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_channel_natives_TeleChannelNatives_singleStep(JNIEnv *env, jobject  this, jlong processHandle, jlong lwpId) {
    struct ps_prochandle *ph = (struct ps_prochandle *) processHandle;
    INIT_LWP_HANDLE(lh, processHandle, lwpId, false);

    if (Lsetrun(lh, 0, PRSTEP | PRCFAULT) != 0) {
        log_println("Lsetrun failed");
        Lfree(lh);
        return false;
    }

    Lfree(lh);
    return true;
}
