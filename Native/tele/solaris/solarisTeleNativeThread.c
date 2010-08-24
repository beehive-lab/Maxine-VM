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

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_channel_natives_TeleChannelNatives_readRegisters(JNIEnv *env, jobject  this, jlong processHandle, jlong lwpId,
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

	isa_canonicalizeTeleIntegerRegisters(&osRegisters[0], &canonicalIntegerRegisters);
	isa_canonicalizeTeleStateRegisters(&osRegisters[0], &canonicalStateRegisters);
	isa_canonicalizeTeleFloatingPointRegisters(&osFloatingPointRegisters, &canonicalFloatingPointRegisters);

    (*env)->SetByteArrayRegion(env, integerRegisters, 0, integerRegistersLength, (void *) &canonicalIntegerRegisters);
    (*env)->SetByteArrayRegion(env, stateRegisters, 0, stateRegistersLength, (void *) &canonicalStateRegisters);
    (*env)->SetByteArrayRegion(env, floatingPointRegisters, 0, floatingPointRegistersLength, (void *) &canonicalFloatingPointRegisters);

    return true;
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
