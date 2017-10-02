package com.sun.max.vm.classfile;

import com.sun.max.vm.classfile.constant.*;

public class BootstrapMethod {
    MethodHandleConstant methodHandleConstant;
    PoolConstant[]       bootstrapArguments;

    BootstrapMethod(MethodHandleConstant methodHandleConstant, PoolConstant[] bootstrapArguments) {
        this.methodHandleConstant = methodHandleConstant;
        this.bootstrapArguments = bootstrapArguments;
    }
}
