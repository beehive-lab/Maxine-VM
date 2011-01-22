#!/bin/bash
#
# ----------------------------------------------------------------------------------------------------
#
# Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#
# ----------------------------------------------------------------------------------------------------
#
# This script copies libjava.jnilib from the JDK installation into the directory
# in which the Maxine's version of libjvm.dylib will reside. The copy is modified
# through use of install_name_tool(1) to change the hard-wired dependency to the
# JVM shared library from the libclient.dylib found in the JDK installation to
# the libjvm.dylib built as part of Maxine. Without this work-around, the functions
# in libjava.jnilib that call any of the JVM_* functions will pick up those from
# HotSpot instead of those from Maxine. It should go without saying that this 
# is not the desired behavior.
#
# The underlying issue is the support on Darwin for two-level namespaces in
# shared libraries (http://developer.apple.com/documentation/Porting/Conceptual/PortingUnix/compiling/chapter_4_section_7.html).
# While this most likely improves the startup time of HotSpot, it complicates
# the task of deploying a drop-in replacement for HotSpot. One other workaround
# is to use the DYLD_FORCE_FLAT_NAMESPACE environment variable (see the dyld(1)
# man page). However, environment variables are not propagated as expected through
# calls to dlopen. From the dlopen(3) man page:
#
#   Note: If the main executable is a set[ug]id binary, then all environment variables are ignored..
#
# For the inspector to be able to launch and control another process, the 'java'
# executable used to run the inspector must be setgid. Then, even if all
# the code is written to manually propagate/set DYLD_FORCE_FLAT_NAMESPACE in the
# various contexts in which the Maxine VM can be launched, the flat namespace
# linkage causes intolerable delays in the inspector. Most noticeable is the
# slow down of single-stepping to about 4 seconds per single step.
#
# Author: Doug Simon

# Sanity: exit immediately if a simple command exits with a non-zero status or if
# an unset variable is used when performing expansion
set -e
set -u

test $# -eq 2 || {
    echo "Usage: $0 <directory containing libjvm.dylib> <jdk-home>"
    exit 1
}

dir=$1
JAVA_HOME=$2

pushd $dir >/dev/null

# Make $dir absolute
dir=$(/bin/pwd)

new_jvmlib=$dir/libjvm.dylib
test -f $new_jvmlib || { echo "No libjvm.dylib in $dir"; exit 1; }

# Copy libjava from JDK installation
src=$JAVA_HOME/../Libraries/libjava.jnilib
test -f $src || { echo "Missing $src"; exit 1; }
cp -f $src .

lib=$dir/libjava.jnilib
old_jvmlib=$(otool -l $lib | grep libclient.dylib | awk '{print $2}')
test -n "$old_jvmlib" || {
    echo "Could not find line containing 'libclient.dylib' in $lib"
    exit 1
}

install_name_tool -change $old_jvmlib $new_jvmlib -id $lib $lib
echo "Copied $src to $lib and re-bound it from $old_jvmlib to $new_jvmlib"
