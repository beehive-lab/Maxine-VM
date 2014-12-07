/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.graal;

import static com.sun.max.vm.VMOptions.*;

import java.lang.reflect.*;
import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.compiler.*;
import com.oracle.graal.compiler.target.*;
import com.oracle.graal.debug.*;
import com.oracle.graal.debug.internal.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.java.*;
import com.oracle.graal.lir.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.extended.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.OptimisticOptimizations.Optimization;
import com.oracle.graal.phases.PhasePlan.PhasePosition;
import com.oracle.graal.phases.common.*;
import com.oracle.graal.phases.tiers.*;
import com.oracle.graal.printer.*;
import com.oracle.graal.replacements.*;
import com.oracle.graal.virtual.phases.ea.*;
import com.oracle.max.vm.ext.graal.amd64.*;
import com.oracle.max.vm.ext.graal.phases.*;
import com.oracle.max.vm.ext.graal.snippets.*;
import com.oracle.max.vm.ext.graal.stubs.*;
import com.oracle.max.vm.ext.maxri.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.config.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.deps.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.profile.MethodProfile;
import com.sun.max.vm.type.*;

public class MaxProfilingInfo implements ProfilingInfo {

    private final MethodProfile methodProfile;
    private final MaxResolvedJavaMethod method;

    MaxProfilingInfo(MethodProfile methodProfile, MaxResolvedJavaMethod method) {
        this.methodProfile = methodProfile;
        this.method = method;
    }

    @Override
    public int getCodeSize() {
        return method.getCodeSize();
    }

    @Override
    public JavaTypeProfile getTypeProfile(int bci) {
        Integer[] typeProfile = methodProfile.getTypeProfile(bci);
        if (typeProfile == null) {
            return null;
        }
        TriState nullSeen = TriState.get(methodProfile.getNullSeen(bci) > 0);
        int typesNum = typeProfile.length / 2;
        boolean isAnonymousTypeRecorded = false;
        double anonymousRecordedTypeProbability = 0;
        Long totalCount = 0L;
        for (int i = 0; i < typesNum; i++) {
            totalCount += typeProfile[i * 2 + 1];
            if (typeProfile[i * 2] == MethodProfile.UNDEFINED_TYPE_ID) {
                isAnonymousTypeRecorded = true;
            }
        }
        assert totalCount > 0;
        JavaTypeProfile.ProfiledType[] profiledTypes =
            new JavaTypeProfile.ProfiledType[typesNum - (isAnonymousTypeRecorded ? 1 : 0)];
        int j = 0;
        for (int i = 0; i < typesNum; i++) {
            Integer typeId = typeProfile[i * 2];
            Integer typeCount = typeProfile[i * 2  + 1];
            if (typeId == MethodProfile.UNDEFINED_TYPE_ID) {
                anonymousRecordedTypeProbability = typeCount / totalCount.doubleValue();
            } else {
                ClassActor classActor = ClassIDManager.toClassActor(typeId);
                ResolvedJavaType javaType = MaxResolvedJavaType.get(classActor);
                profiledTypes[j] = new JavaTypeProfile.ProfiledType(javaType, typeCount / totalCount.doubleValue());
                j++;
            }
        }
        Arrays.sort(profiledTypes);
        return new JavaTypeProfile(nullSeen, anonymousRecordedTypeProbability, profiledTypes);
    }

    @Override
    public JavaMethodProfile getMethodProfile(int bci) {
        // TODO: implement when method profile information is utilzied in Graal
        return null;
    }

    @Override
    public double getBranchTakenProbability(int bci) {
        return methodProfile.getBranchTakenProbability(bci);
    }

    @Override
    public double[] getSwitchProbabilities(int bci) {
        return methodProfile.getSwitchProbabilities(bci);
    }

    @Override
    public TriState getExceptionSeen(int bci) {
        return TriState.UNKNOWN;
    }

    @Override
    public TriState getNullSeen(int bci) {
        int profiledTypesNum = methodProfile.getProfiledTypesNum(bci);
        if (profiledTypesNum == 0) {
            return TriState.UNKNOWN;
        }
        int nullSeen = methodProfile.getNullSeen(bci);
        return TriState.get(nullSeen > 0);
    }

    @Override
    public int getExecutionCount(int bci) {
        return methodProfile.getExecutionCount(bci);
    }

    @Override
    public int getDeoptimizationCount(DeoptimizationReason reason) {
        return 0;
    }

    @Override
    public boolean isMature() {
        return true;
    }

    @Override
    public String toString() {
        return "MaxProfilingInfo<" + MetaUtil.profileToString(this, null, "; ") + ">";
    }
}
