/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @author Bernd Mathiske
 */
#ifndef __os_h__
#define __os_h__ 1

#include "c.h"

#define os_SOLARIS  0
#define os_LINUX    0
#define os_WINDOWS  0
#define os_DARWIN   0
#define os_MAXVE    0

#if defined(MAXVE)
#   undef os_MAXVE
#   define os_MAXVE   1
#   define os_IDENTIFIER MAXVE
#elif defined(__sun) || defined(SOLARIS)
#   undef os_SOLARIS
#   define os_SOLARIS    1
#   define os_IDENTIFIER SOLARIS
#elif defined(linux) || defined(__linux) || defined(__linux__)
#   undef os_LINUX
#   define os_LINUX      1
#   define os_IDENTIFIER LINUX
#elif defined(__CYGWIN__) || defined(__CYGWIN32__) || defined(WINDOWS)
#   undef os_WINDOWS
#   define os_WINDOWS    1
#   define os_IDENTIFIER WINDOWS
#elif defined(__APPLE__)
#   undef os_DARWIN
#   define os_DARWIN     1
#   define os_IDENTIFIER DARWIN

#define os_STACK_ALIGNMENT    16

#else
#   error
#endif

#define os_STRING   STRINGIZE(os_IDENTIFIER)

#define os_HEADER_FILE_NAME(baseName) \
    STRINGIZE(os/os_IDENTIFIER/CONCATENATE(baseName##_, os_IDENTIFIER).h)

#endif /*__os_h__*/
