/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
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
 */
package com.sun.max.vm.jdk;

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import java.sql.*;
import java.util.concurrent.*;

import com.sun.max.annotate.*;

import sun.reflect.*;

/**
 * Make dacapo h2 work in JDK8.  This is essentially a copy of getDriver without any changes in its logic, however
 * performing this substitution allows dacapo's h2 to run without issues. If not substituted, maxine fails to get the
 * {@code org.h2.driver} driver.
 */
@METHOD_SUBSTITUTIONS(java.sql.DriverManager.class)
final class JDK_java_sql_DriverManager {

    @ALIAS(declaringClass = java.sql.DriverManager.class,
            descriptor = "Ljava/util/concurrent/CopyOnWriteArrayList;")
    private static CopyOnWriteArrayList<DriverInfoAlias> registeredDrivers;

    @INTRINSIC(UNSAFE_CAST)
    private static native DriverInfoAlias asDriverInfoAlias(Object cl);

    @ALIAS(declaringClass = java.sql.DriverManager.class)
    private static native boolean isDriverAllowed(Driver driver, Class<?> caller);

    @SUBSTITUTE
    @CallerSensitive
    public static Driver getDriver(String url) throws SQLException {

        Class<?> callerClass = Reflection.getCallerClass();

        // Walk through the loaded registeredDrivers attempting to locate someone
        // who understands the given URL.
        for (Object aDriver : registeredDrivers) {
            // If the caller does not have permission to load the driver then
            // skip it.
            if (isDriverAllowed(asDriverInfoAlias(aDriver).driver, callerClass)) {
                try {
                    if (asDriverInfoAlias(aDriver).driver.acceptsURL(url)) {
                        // Success!
                        return asDriverInfoAlias(aDriver).driver;
                    }

                } catch (SQLException sqe) {
                    // Drop through and try the next driver.
                }
            }
        }

        throw new SQLException("No suitable driver", "08001");
    }

    class DriverInfoAlias {
        @ALIAS(declaringClassName = "java.sql.DriverInfo")
        Driver driver;
    }
}
