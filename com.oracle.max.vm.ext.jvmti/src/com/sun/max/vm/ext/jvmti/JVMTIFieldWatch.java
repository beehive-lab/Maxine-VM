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
package com.sun.max.vm.ext.jvmti;

import java.util.concurrent.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.ext.jvmti.JVMTIUtil.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.object.*;

import static com.sun.max.vm.ext.jvmti.JVMTIConstants.*;

/**
 * Support for field watch events.
 */
public class JVMTIFieldWatch {

    private static int ACCESS = 1;
    private static int MODIFICATION = 2;

    // Watch state handling for fields

    private static class WatchState {
        final int state;

        WatchState(int state) {
            this.state = state;
        }
    }

    private static final WatchState ACCESS_STATE = new WatchState(ACCESS);
    private static final WatchState MODIFICATION_STATE = new WatchState(MODIFICATION);
    private static final WatchState ACCESS_MODIFICATION_STATE = new WatchState(ACCESS | MODIFICATION);

    private static ConcurrentHashMap<FieldActor, WatchState> fieldMap = new ConcurrentHashMap<FieldActor, WatchState>();

    static int setAccessWatch(Class klass, FieldActor fieldActor) {
        return setWatch(fieldActor, ACCESS_STATE);
    }

    static int setModificationWatch(Class klass, FieldActor fieldActor) {
        return setWatch(fieldActor, MODIFICATION_STATE);
    }

    static int setWatch(FieldActor fieldActor, WatchState stateToSet) {
        WatchState watchState = fieldMap.get(fieldActor);
        if (watchState != null) {
            if ((watchState.state & stateToSet.state) != 0) {
                return JVMTI_ERROR_DUPLICATE;
            }
            // other state must be set already
            fieldMap.put(fieldActor, ACCESS_MODIFICATION_STATE);
        } else {
            fieldMap.put(fieldActor, stateToSet);
        }
        return JVMTI_ERROR_NONE;
    }

    static int clearAccessWatch(Class klass, FieldActor fieldActor) {
        return clearWatch(fieldActor,  ACCESS_STATE);
    }

    static int clearModificationWatch(Class klass, FieldActor fieldActor) {
        return clearWatch(fieldActor,  MODIFICATION_STATE);
    }

    static int clearWatch(FieldActor fieldActor, WatchState stateToClear) {
        WatchState watchState = fieldMap.get(fieldActor);
        if (watchState == null) {
            return JVMTI_ERROR_NOT_FOUND;
        } else {
            if ((watchState.state & stateToClear.state) != 0) {
                WatchState other = other(stateToClear);
                if ((watchState.state & other.state) != 0) {
                    fieldMap.put(fieldActor, other);
                } else {
                    fieldMap.remove(fieldActor);
                }
            }
        }
        return JVMTI_ERROR_NONE;
    }

    private static WatchState other(WatchState watchState) {
        if (watchState == ACCESS_STATE) {
            return MODIFICATION_STATE;
        } else if (watchState == MODIFICATION_STATE) {
            return ACCESS_STATE;
        } else {
            assert false;
            return null;
        }
    }

    /**
     * Data handling for field events.
     * There is a lot of data and we use a union type to handle the modification values.
     * The following might be a good optimization in due course.
     *
     * public static final VmThreadLocal JVMTI_FIELD_EVENT_DATA = new VmThreadLocal(
     *               "JVMTI_FIELDWATCH_DATA", true, "Storage for field watch data for JVMTI");
     *
     */

    static class FieldEventData extends TypedData {
        Object object;
        int offset;
        boolean isStatic;

        // value "union" for modification events        //
    }

    public static void invokeFieldAccessCallback(Pointer callback, Pointer jvmtiEnv, JniHandle thread, FieldEventData data) {
        ClassActor classActor = ObjectAccess.readClassActor(data.object);
        FieldActor fieldActor;
        if (data.isStatic) {
            fieldActor = classActor.findStaticFieldActor(data.offset);
        } else {
            fieldActor = classActor.findInstanceFieldActor(data.offset);
        }
        WatchState watchState = fieldMap.get(fieldActor);
        int watchStateToCheck = data.tag == FieldEventData.DATA_NONE ? ACCESS : MODIFICATION;
        if (watchState != null && (watchState.state & watchStateToCheck) != 0) {
            JVMTICallbacks.invokeFieldWatchCallback(callback, jvmtiEnv, thread,
                Word.zero(), 0, // TODO set these values
                JniHandles.createLocalHandle(classActor.toJava()), JniHandles.createLocalHandle(data.object),
                FieldID.fromFieldActor(fieldActor),
                data.tag == FieldEventData.DATA_NONE ? 0 : signatureType(data.tag),
                Word.zero());
        }
    }

    private static byte signatureType(int tag) {
        return 'I';
    }

}
