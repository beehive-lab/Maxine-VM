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
#include <stdlib.h>
#include <unistd.h>
#include <assert.h>
#include <alloca.h>

#include "isa.h"
#include "log.h"
#include "jni.h"
#include "threadLocals.h"
#include "teleProcess.h"
#include "teleNativeThread.h"

#include <xg_public.h>

static int tele_xg_readbytes(uint64_t src, char *buf, unsigned short size) {
    unsigned short result = (unsigned short) xg_read_mem(src, buf, size, 0);
//    log_println("tele_xg_readbytes(%llx, %u): %u", src, size, result);
    return size - result;
}

static int tele_xg_writebytes(uint64_t src, char *buf, unsigned short size) {
    unsigned short  result = (unsigned short) xg_write_mem(src, buf, size, 0);
    return size - result;
}

static struct guestvm_memory_handler xg_memory_handler = {
                .readbytes = &tele_xg_readbytes,
                .writebytes = &tele_xg_writebytes
};

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_guestvm_xen_dbchannel_agent_AgentXGProtocol_nativeInit(JNIEnv *env, jclass c) {
    /*tele_*/log_println("Calling xg_init");
    return xg_init();
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_guestvm_xen_dbchannel_xg_XGProtocol_nativeAttach(JNIEnv *env, jclass c, jint domainId) {
    /*tele_*/log_println("Calling xg_attach on domId=%d", domainId);
    return xg_attach(domainId);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_guestvm_xen_dbchannel_xg_XGProtocol_nativeResume(JNIEnv *env, jobject domain) {
    /*tele_*/log_println("Calling xg_resume_n_wait");
    int vcpu = xg_resume_n_wait(64);
}


JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_guestvm_xen_dbchannel_xg_XGProtocol_nativeReadBytes(JNIEnv *env, jclass c, jlong src, jobject dst, jboolean isDirectByteBuffer, jint dstOffset, jint length) {
    return teleProcess_read(&xg_memory_handler, env, c, src, dst, isDirectByteBuffer, dstOffset, length);
}


JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_guestvm_xen_dbchannel_xg_XGProtocol_nativeWriteBytes(JNIEnv *env, jclass c, jlong dst, jobject src, jboolean isDirectByteBuffer, jint srcOffset, jint length) {
    return teleProcess_write(&xg_memory_handler, env, c, dst, src, isDirectByteBuffer, srcOffset, length);
}

