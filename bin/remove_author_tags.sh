#!/bin/bash
#
# ----------------------------------------------------------------------------------------------------
#
# Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
# Script to remove @author tags from the *.java files supplied as args to this script.
#

files="$@"
for f in $files; do
    if [[ ! "$f" =~ ".java" ]]; then
        #echo "ignoring $f"
        continue;
    fi
    awk <$f >$f.tmp '
BEGIN  { i = 0; }    
/\* @author [A-Za-z\. ]*$/ { while (i > 0 && lines[i - 1] ~ /^ \* *$/ ) { i-- ; } next; }
      { lines[i++] = $0; } 
END   { j = 0; while (j < i) print lines[j++]; } '
     echo "------ $f ------"
    diff $f $f.tmp >/dev/null
    if [ $? -ne 0 ]; then
        mv $f $f.orig
        mv $f.tmp $f
        echo "Modified $f - original in $f.orig"
    else
        rm $f.tmp
    fi
done
