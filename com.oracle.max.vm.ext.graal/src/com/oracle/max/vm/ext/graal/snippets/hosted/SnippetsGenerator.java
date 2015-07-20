/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.graal.snippets.hosted;

import com.oracle.graal.api.meta.*;
import com.oracle.max.vm.ext.graal.hosted.*;


public abstract class SnippetsGenerator extends SourceGenerator {

    protected static final String UNSAFE_CAST_BEFORE = "UnsafeCastNode.unsafeCast(";
    protected static final String UNSAFE_CAST_AFTER = ", StampFactory.forNodeIntrinsic())";

    protected String replace(String template, String param, String arg) {
        return template.replaceAll(param, arg);
    }

    protected String replaceKinds(String template, Kind kind) {
        String uJavaName = toFirstUpper(kind.getJavaName());
        String result = replace(template, "#UKIND#", uJavaName);
        return replace(result, "#KIND#", kind.getJavaName());
    }

    protected String replaceUCast(String template, Kind kind) {
        String ucb = kind != Kind.Object ? "" : UNSAFE_CAST_BEFORE;
        String uca = kind != Kind.Object ? "" : UNSAFE_CAST_AFTER;
        return replace(replace(template, "#UCA#", uca), "#UCB#", ucb);
    }

    protected boolean notVoidOrIllegal(Kind kind) {
        return !(kind == Kind.Void || kind == Kind.Illegal);
    }


}
