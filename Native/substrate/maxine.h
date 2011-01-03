/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
/**
 * @author Ben L. Titzer
 */

#ifndef __maxine_h__
#define __maxine_h__ 1

#include "os.h"
#include "jni.h"

extern jlong native_nanoTime(void);
extern jlong native_currentTimeMillis(void);
extern void *native_executablePath(void);
extern void  native_exit(int code);
extern void *native_environment(void);

extern int maxine(int argc, char *argv[], char *executablePath);

/**
 * The layout of this struct must be kept in sync with the com.sun.max.vm.MaxineVM.NativeJavaProperty enum.
 */
typedef struct {
    char *user_name;
    char *user_home;
    char *user_dir;
} native_props_t;


#endif /* __maxine_h__ */
