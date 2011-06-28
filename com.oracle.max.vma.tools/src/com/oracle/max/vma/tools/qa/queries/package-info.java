/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * This package contains a collection of queries that analyse the object analysis trace information.
 * Many of the queries explicitly or implicitly report data on the immutability of instances
 * as this analysis was the origin of the tool.
 *
 * <ul>
 * <li>{@link com.oracle.max.vma.tools.qa.queries.BasicCountsQuery basic information on object instances}.
 * <li>{@link com.oracle.max.vma.tools.qa.queries.ClassLoadersQuery list class loader instances}.
 * <li>{@link com.oracle.max.vma.tools.qa.queries.DataByClassQuery show data on instances, organised by class}.
 * <li>{@link com.oracle.max.vma.tools.qa.queries.ImmutableClassBucketsQuery analyse immutable instances into 1% buckets}.
 * <li>{@link com.oracle.max.vma.tools.qa.queries.LiveObjectsQuery show info on objects still live at end of run}.
 * </ul>
 */

package com.oracle.max.vma.tools.qa.queries;
