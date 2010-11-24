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

#include "c.h"
#include "jni.h"
#include "log.h"
#include "threadLocals.h"
#include "teleProcess.h"

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved)
{
    c_initialize();
#if !os_GUESTVMXEN
    log_initialize(getenv("TELE_LOG_FILE"));
#endif
    return JNI_VERSION_1_2;
}

JNIEXPORT void JNICALL
Java_com_sun_max_tele_channel_natives_TeleChannelNatives_teleInitialize(JNIEnv *env, jclass c, jint tlaSize) {
    tla_initialize(tlaSize);
}

