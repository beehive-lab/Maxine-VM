package com.oracle.max.vm.ext.graal;

import com.oracle.max.vm.ext.maxri.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;


public class GraalMaxTargetMethod extends MaxTargetMethod {

    private GraalMaxTargetMethod(ClassMethodActor classMethodActor, CiTargetMethod ciTargetMethod, boolean install) {
        super(classMethodActor, ciTargetMethod, install);
    }

    @NEVER_INLINE
    public static MaxTargetMethod create(ClassMethodActor classMethodActor, CiTargetMethod ciTargetMethod, boolean install) {
        return new GraalMaxTargetMethod(classMethodActor, ciTargetMethod, install);
    }

}
