/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
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
#ifndef __cc_h__
#define __cc_h__ 1

#include "c.h"

#define cc_GNU  0
#define cc_SUN  0

#ifdef __GNUC__
#   undef cc_GNU
#   define cc_GNU        1
#   define cc_IDENTIFIER gnu
#elif defined(sun)
#   undef cc_SUN
#   define cc_SUN        1
#   define cc_IDENTIFIER sun
#else
#   error
#endif

#if cc_SUN
#   define cc_HEADER_FILE_NAME(baseName) \
        STRINGIZE(CONCATENATE(baseName##_sun).h)
#else
#   define cc_HEADER_FILE_NAME(baseName) \
        STRINGIZE(CONCATENATE(baseName##_, cc_IDENTIFIER).h)
#endif

#endif /*__cc_h__*/
