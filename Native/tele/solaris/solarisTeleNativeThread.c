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
#include "solarisTeleProcess.h"

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
 * Convenience macro for initializing a variable to hold the handle to a LWP denoted by a process handle and a LWP identifier.
 */
#define INIT_LWP_HANDLE(lh, ph, lwpId, errorReturnValue) \
	int error; \
	struct ps_lwphandle *lh = proc_Lgrab((struct ps_prochandle *) ph, (lwpid_t) lwpId, &error); \
	if (error != 0) { \
    	log_println("Lgrab failed: %s", Lgrab_error(error)); \
		return errorReturnValue; \
	}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_solaris_SolarisTeleNativeThread_nativeReadRegisters(JNIEnv *env, jclass c, jlong processHandle, jlong lwpId,
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
	if (proc_Plwp_getregs(ph, lwpId, osRegisters) != 0) {
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

static long getRegister(jlong processHandle, jlong lwpId, int registerIndex) {
	struct ps_prochandle *ph = (struct ps_prochandle *) processHandle;
	INIT_LWP_HANDLE(lh, processHandle, lwpId, false);

	if (proc_Lwait(lh, 0) != 0) {
    	log_println("Lwait failed");
    	proc_Lfree(lh);
		return -1L;
	}
	proc_Lsync(lh);

	jlong result = -1L;
	if (proc_Lgetareg(lh, registerIndex, &result) != 0) {
    	log_println("Lgetareg failed");
    	proc_Lfree(lh);
    	return -1L;
	}
	proc_Lfree(lh);
	return result;
}

static jboolean setRegister(jlong processHandle, jlong lwpId, int registerIndex, jlong value) {
	struct ps_prochandle *ph = (struct ps_prochandle *) processHandle;
	INIT_LWP_HANDLE(lh, processHandle, lwpId, false);

	if (proc_Lwait(lh, 0) != 0) {
    	log_println("Lwait failed");
    	proc_Lfree(lh);
		return false;
	}

	if (proc_Lputareg(lh, registerIndex, value) != 0) {
    	log_println("Lputareg failed");
    	proc_Lfree(lh);
    	return false;
	}
	proc_Lsync(lh);

	proc_Lfree(lh);
	return true;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_solaris_SolarisTeleNativeThread_nativeSetInstructionPointer(JNIEnv *env, jclass c, jlong processHandle, jlong lwpId, jlong address) {
	return setRegister(processHandle, lwpId, R_PC, address);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_solaris_SolarisTeleNativeThread_nativeSingleStep(JNIEnv *env, jclass c, jlong processHandle, jlong lwpId) {
	INIT_LWP_HANDLE(lh, processHandle, lwpId, false);
    if (proc_Lclearfault(lh) != 0) {
        log_println("Lclearfault failed");
        proc_Lfree(lh);
        return false;
    }

    if (proc_Lsetrun(lh, 0, PRSTEP) != 0) {
        log_println("Lsetrun failed");
        proc_Lfree(lh);
        return false;
    }

    proc_Lfree(lh);
    return true;
}
