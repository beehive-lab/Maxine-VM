/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
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
/**
 * JDK implementation support.
 * 
 * We substitute certain methods in the JDK with our own implementation in Java,
 * without touching any JDK source code.
 * For each class, in which this occurs, we create a parallel class,
 * in a parallel package structure underneath here.
 * We name the substitute class like the original class,
 * for easy recognition, but with with "_" appended for better distinction.
 * We annotate the substitute class with '@METHOD_ANNOTATIONS',
 * passing the original class as annotation parameter.
 * Then we annotate the methods in our substitute class,
 * which substitute original methods with the '@SUBSTITUTE' annotation.
 * 
 * @author Bernd Mathiske
 */
package com.sun.max.vm.jdk;
