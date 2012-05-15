/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.ti;

import java.io.*;
import java.security.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.thread.*;

/**
 * General case of multiple VMTI handlers.
 */
class MDVMTIHandler implements VMTIHandler {

    final VMTIHandler[] eventHandlers;

    MDVMTIHandler(ArrayList<VMTIHandler> hostedEventHandlers) {
        eventHandlers = new VMTIHandler[hostedEventHandlers.size()];
        hostedEventHandlers.toArray(eventHandlers);
    }

    @Override
    public void threadStart(VmThread vmThread) {
        for (int i = 0; i < eventHandlers.length; i++) {
            eventHandlers[i].threadStart(vmThread);
        }
    }

    @Override
    public void threadEnd(VmThread vmThread) {
        for (int i = 0; i < eventHandlers.length; i++) {
            eventHandlers[i].threadEnd(vmThread);
        }
    }

    @Override
    public boolean classFileLoadHookHandled() {
        for (int i = 0; i < eventHandlers.length; i++) {
            if (eventHandlers[i].classFileLoadHookHandled()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public byte[] classFileLoadHook(ClassLoader classLoader, String className, ProtectionDomain protectionDomain, byte[] classfileBytes) {
        byte[] newBytes = classfileBytes;
        boolean changed = false;
        for (int i = 0; i < eventHandlers.length; i++) {
            byte[] changedBytes = eventHandlers[i].classFileLoadHook(classLoader, className, protectionDomain, newBytes);
            if (changedBytes != null) {
                newBytes = changedBytes;
                changed = true;
            }
        }
        return changed ? newBytes : null;
    }

    @Override
    public void classLoad(ClassActor classActor) {
        for (int i = 0; i < eventHandlers.length; i++) {
            eventHandlers[i].classLoad(classActor);
        }
    }

    public boolean hasBreakpoints(ClassMethodActor classMethodActor) {
        for (int i = 0; i < eventHandlers.length; i++) {
            if (eventHandlers[i].hasBreakpoints(classMethodActor)) {
                return true;
            }
        }
        return false;
    }

    public String bootclassPathExtension() {
        String result = null;
        for (int i = 0; i < eventHandlers.length; i++) {
            String s = eventHandlers[i].bootclassPathExtension();
            if (s != null) {
                if (result == null) {
                    result = s;
                } else {
                    result = result + File.pathSeparator + s;
                }
            }
        }
        return result;
    }

    @Override
    public void beginUpcallVM() {
        for (int i = 0; i < eventHandlers.length; i++) {
            eventHandlers[i].beginUpcallVM();
        }
    }

    @Override
    public void endUpcallVM() {
        for (int i = 0; i < eventHandlers.length; i++) {
            eventHandlers[i].endUpcallVM();
        }
    }

    @Override
    public void initialize() {
        for (int i = 0; i < eventHandlers.length; i++) {
            eventHandlers[i].initialize();
        }
    }

    @Override
    public void vmInitialized() {
        for (int i = 0; i < eventHandlers.length; i++) {
            eventHandlers[i].vmInitialized();
        }
    }

    @Override
    public void vmDeath() {
        for (int i = 0; i < eventHandlers.length; i++) {
            eventHandlers[i].vmDeath();
        }
    }

    @Override
    public void beginGC() {
        for (int i = 0; i < eventHandlers.length; i++) {
            eventHandlers[i].beginGC();
        }
    }

    @Override
    public void endGC() {
        for (int i = 0; i < eventHandlers.length; i++) {
            eventHandlers[i].endGC();
        }
    }

    @Override
    public boolean nativeCallNeedsPrologueAndEpilogue(MethodActor ma) {
        for (int i = 0; i < eventHandlers.length; i++) {
            if (eventHandlers[i].nativeCallNeedsPrologueAndEpilogue(ma)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void registerAgent(Word agentHandle) {
        for (int i = 0; i < eventHandlers.length; i++) {
            eventHandlers[i].registerAgent(agentHandle);
        }
    }

    @Override
    public void raise(Throwable throwable, Pointer sp, Pointer fp, CodePointer ip) {
        for (int i = 0; i < eventHandlers.length; i++) {
            eventHandlers[i].raise(throwable, sp, fp, ip);
        }
    }

    @Override
    public RuntimeCompiler runtimeCompiler(RuntimeCompiler stdRuntimeCompiler) {
        for (int i = 0; i < eventHandlers.length; i++) {
            RuntimeCompiler runtimeCompiler = eventHandlers[i].runtimeCompiler(stdRuntimeCompiler);
            if (runtimeCompiler != null) {
                return runtimeCompiler;
            }
        }
        return null;
    }

    @Override
    public boolean needsVMTICompilation(ClassMethodActor classMethodActor) {
        for (int i = 0; i < eventHandlers.length; i++) {
            if (eventHandlers[i].needsVMTICompilation(classMethodActor)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean needsSpecialGetCallerClass() {
        for (int i = 0; i < eventHandlers.length; i++) {
            if (eventHandlers[i].needsSpecialGetCallerClass()) {
                return true;
            }
        }
        return false;
    }

    @Override
    @NEVER_INLINE
    public Class getCallerClassForFindClass(int realFramesToSkip) {
        for (int i = 0; i < eventHandlers.length; i++) {
            Class klass = eventHandlers[i].getCallerClassForFindClass(realFramesToSkip++);
            if (klass != null) {
                return klass;
            }
        }
        return null;
    }

    @Override
    public void objectSurviving(Pointer cell) {
        for (int i = 0; i < eventHandlers.length; i++) {
            eventHandlers[i].objectSurviving(cell);
        }
    }

}
