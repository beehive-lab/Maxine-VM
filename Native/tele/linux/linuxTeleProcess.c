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
#include <malloc.h>
#include <signal.h>
#include <string.h>
#include <thread_db.h>
#include <sys/types.h>

#include "log.h"
#include "jni.h"
#include "libInfo.h"
#include "threads.h"

#include "teleProcess.h"

void teleProcess_initialize(void) {
    td_init();
}

JNIEXPORT jlong JNICALL
Java_com_sun_max_tele_debug_linux_LinuxTeleProcess_nativeSuspend(JNIEnv *env, jclass c, jint processID) {
    kill(processID, SIGINT);
}

JNIEXPORT jlong JNICALL
Java_com_sun_max_tele_debug_linux_LinuxTeleProcess_nativeCreateAgent(JNIEnv *env, jclass c, jint processID) {
    td_err_e error;
    td_thragent_t *ta = NULL;

    struct ps_prochandle *ph = (struct ps_prochandle *) calloc(1, sizeof(*ph));
    if (ph == NULL) {
        log_println("could not calloc ps_prochandle");
        return 0L;
    }
    ph->pid = processID;

    if (!read_LibInfo(ph)) {
        log_println("could not read lib info");
        return 0L;
    }

    error = td_ta_new(ph, &ta);
    if (error != TD_OK || ta == NULL) {
        log_println("td_ta_new failed");
        free(ph);
        return 0L;
    }

    return (long) ta;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_LinuxTeleProcess_nativeFreeAgent(JNIEnv *env, jclass c, jlong agent) {
    struct ps_prochandle *ph = NULL;
    td_thragent_t *ta = (td_thragent_t *) agent;
    td_err_e error = td_ta_get_ph(ta, &ph);
    if (error != TD_OK || ph == NULL) {
        return false;
    }

    error = td_ta_delete(ta);
    if (error != TD_OK) {
        return false;
    }

    free(ph);
    memset(ph, 0, sizeof(*ph));
    return true;
}

typedef struct Argument {
    struct ps_prochandle *ph;
    JNIEnv *env;
    jobject process;
    jobject result;
} *Argument;

static void *_javaThreadStartFunction = NULL;
static jmethodID _methodID = NULL;

static int gatherThread(const td_thrhandle_t *th, void *data) {
    Argument a = (Argument) data;
    td_thrinfo_t info;
    jboolean isSuspended;

    td_err_e error = td_thr_get_info(th, &info);
    if (error != TD_OK) {
        log_println("td_thr_get_info failed: %d", error);
        return -1;
    }

    if (_javaThreadStartFunction == NULL) {
        _javaThreadStartFunction = (void *) lookup_symbol(a->ph, STRINGIZE(thread_runJava));
    }
    isSuspended = info.ti_db_suspended != 0;

    if (_methodID == NULL) {
        jclass c = (*a->env)->GetObjectClass(a->env, a->process);
        _methodID = (*a->env)->GetMethodID(a->env, c, "jniGatherThread",
                                           "(Ljava/util/Map;ZJIJJ)V");
        c_ASSERT(_methodID != NULL);
    }

    (*a->env)->CallVoidMethod(a->env, a->process, _methodID, a->result, isSuspended,
                              (jlong) info.ti_tid, (jint) info.ti_lid, (jlong) info.ti_stkbase, (jlong) info.ti_stksize);
    return 0;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_LinuxTeleProcess_nativeGatherThreads(JNIEnv *env, jobject process, jlong agent, jobject result) {
    td_thragent_t *ta = (td_thragent_t *) agent;
    struct ps_prochandle *ph = NULL;
    td_err_e error = td_ta_get_ph(ta, &ph);
    if (error != TD_OK || ph == NULL) {
        log_println("td_ta_get_ph failed");
        return false;
    }

    struct Argument argument;
    argument.ph = ph;
    argument.env = env;
    argument.process = process;
    argument.result = result;

    error = td_ta_thr_iter(ta, gatherThread, &argument, TD_THR_ANY_STATE, TD_THR_LOWEST_PRIORITY, TD_SIGNO_MASK, TD_THR_ANY_USER_FLAGS);
    return error == TD_OK;
}
