#!/usr/bin/python
#
# mymx.py - example of how to extend mx.py with extra commands
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

""" Execution of the example extended command:

 >>> mymx.py help mycmd
 mx mycmd [options]

 an example extended command on linux

    Prints a simple message and exits.
 >>> mymx.py mycmd
 hello from mycmd
 args:  []

"""

import mx
import commands

def _msgHelper(env):
    return env.os

def mycmd(env, args):
    """an example extended command on {0}

    Prints a simple {1} and exits."""

    print 'hello from mycmd'
    print 'args: ', args

if __name__ == '__main__':
    commands.table['mycmd'] = [mycmd, '[options]', _msgHelper, 'message']
    mx.main()
