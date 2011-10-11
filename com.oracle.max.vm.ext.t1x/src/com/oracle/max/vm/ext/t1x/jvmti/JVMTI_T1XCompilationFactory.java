package com.oracle.max.vm.ext.t1x.jvmti;

import com.oracle.max.vm.ext.t1x.*;
import com.oracle.max.vm.ext.t1x.jvmti.amd64.*;


public class JVMTI_T1XCompilationFactory extends T1XCompilationFactory {
    @Override
    public T1XCompilation newT1XCompilation(T1X t1x) {
        if (T1X.isAMD64()) {
            return new JVMTI_AMD64T1XCompilation(t1x);
        } else {
            throw T1X.unimplISA();
        }
    }

}
