/*
 * Copyright (c) 2017-2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
package com.sun.max.vm.classfile.constant;

import static com.sun.max.vm.classfile.constant.ConstantPool.*;
import static com.sun.max.vm.classfile.constant.ConstantPool.Tag.*;
import static com.sun.max.vm.jdk.JDK_java_lang_invoke_MemberName.*;

import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.methodhandle.*;
import com.sun.max.vm.runtime.FatalError;
import com.sun.max.vm.type.Descriptor;
import com.sun.max.vm.type.SignatureDescriptor;
import com.sun.max.vm.type.TypeDescriptor;

/**
 * #4.4.10.
 */
public class InvokeDynamicConstant extends AbstractPoolConstant<InvokeDynamicConstant> implements PoolConstantKey<InvokeDynamicConstant>, MethodRefConstant<InvokeDynamicConstant> {
    final int bootstrapMethodAttrIndex;
    public final int nameAndTypeIndex;
    Object appendix;

    InvokeDynamicConstant(int bootstrapMethodAttrIndex, int nameAndTypeIndex, Tag[] tags) {
        this.bootstrapMethodAttrIndex = bootstrapMethodAttrIndex;
        this.nameAndTypeIndex = nameAndTypeIndex;
        if (tags[nameAndTypeIndex] != NAME_AND_TYPE) {
            throw unexpectedEntry(nameAndTypeIndex, tags[nameAndTypeIndex], "invokedynamic name and type", NAME_AND_TYPE);
        }
    }

    public boolean isResolved() {
        return false;
    }

    public boolean isResolvableWithoutClassLoading(ConstantPool pool) {
        return true;
    }

    @Override
    public Tag tag() {
        return INVOKE_DYNAMIC;
    }

    @Override
    public PoolConstantKey<InvokeDynamicConstant> key(ConstantPool pool) {
        return this;
    }

    NameAndTypeConstant nameAndType(ConstantPool pool) {
        return pool.nameAndTypeAt(nameAndTypeIndex);
    }

    public String valueString(ConstantPool pool) {
        return "bootstrapMethodAttrIndex=" + bootstrapMethodAttrIndex + ",nameAndTypeIndex=" + nameAndTypeIndex;
    }

    @Override
    public SignatureDescriptor signature(ConstantPool pool) {
        return nameAndType(pool).signature();
    }

    /**
     * Implementation ported from resolve_invokedynamic linkResolver.cpp.
     *
     * @param pool
     * @param index
     * @return
     */
    public StaticMethodActor resolve(ConstantPool pool, int index) {
        try {
            final NameAndTypeConstant nameAndTypeConstant = nameAndType(pool);
            final ClassActor          holder              = pool.holder();
            assert holder != null : "invokeDynamic holder is null";
            BootstrapMethod       bootstrapMethod = holder.bootstrapMethods()[bootstrapMethodAttrIndex];
            final MemberNameAlias memberName      = bootstrapMethod.resolve(pool, bootstrapMethodAttrIndex, nameAndTypeConstant);
            VMTarget              vmTarget        = VMTarget.fromMemberName(memberName);
            // Update our local BootstrapMethod which should now be resolved and contain the appendix argument.
            bootstrapMethod = holder.bootstrapMethods()[bootstrapMethodAttrIndex];
            appendix = bootstrapMethod.getAppendix();
            // DO NOT update the InvokeDynamicConstant in the constant pool!
            return (StaticMethodActor) vmTarget.getVmTarget();
        } catch (VirtualMachineError e) {
            // Comment from Hotspot:
            // Just throw the exception and don't prevent these classes from
            // being loaded for virtual machine errors like StackOverflow
            // and OutOfMemoryError, etc.
            // Needs clarification to section 5.4.3 of the JVM spec (see 6308271)
            throw e;
        }
    }

    public Object getAppendix() {
        return appendix;
    }

    @Override
    public final TypeDescriptor holder(ConstantPool pool) {
        throw FatalError.unimplemented();
    }

    @Override
    public final Utf8Constant name(ConstantPool pool) {
        return nameAndType(pool).name();
    }

    @Override
    public final Descriptor descriptor(ConstantPool pool) {
        return nameAndType(pool).descriptor();
    }
}
