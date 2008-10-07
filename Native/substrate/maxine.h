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
/*VCSID=fc59e36e-80ec-465b-90c2-6154bdce8ad8*/
/**
 * @author Ben L. Titzer
 */

#ifndef __maxine_h__
#define __maxine_h__ 1

#include "os.h"
#include "jni.h"

extern jlong native_nanoTime();
extern jlong native_currentTimeMillis();
extern void *native_executablePath();
extern void  native_exit(int code);
extern void *native_environment();

extern int maxine(int argc, char *argv[], char *executablePath);

#endif /* __maxine_h__ */
