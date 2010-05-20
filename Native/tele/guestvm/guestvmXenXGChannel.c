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

