/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.gui;

import com.sun.max.ins.*;

/**
 * A very simple provider of display styles, at this time only varying by font size.
 *
 * @author Michael Van De Vanter
 */
public class InspectorStyleFactory extends AbstractInspectionHolder {

    private final InspectorStyle standard10;
    private final InspectorStyle standard11;
    private final InspectorStyle standard12;
    private final InspectorStyle standard14;
    private final InspectorStyle standard16;
    private final InspectorStyle standard20;
    private final InspectorStyle[] allStyles;

    public InspectorStyleFactory(Inspection inspection) {
        super(inspection);
        standard10 = new StandardInspectorStyle(inspection, 10, 16);
        standard11 = new StandardInspectorStyle(inspection, 11, 18);
        standard12 = new StandardInspectorStyle(inspection, 12, 20);
        standard14 = new StandardInspectorStyle(inspection, 14, 23);
        standard16 = new StandardInspectorStyle(inspection, 16, 26);
        standard20 = new StandardInspectorStyle(inspection, 20, 32);
        allStyles = new InspectorStyle[] {standard10, standard11, standard12, standard14, standard16, standard20};
    }

    /**
     * @return the default {@link InspectorStyle} to use when no preference specified.
     */
    public InspectorStyle defaultStyle() {
        return standard12;
    }

    /**
     * @param name a style name
     * @return the style by that name, null if none exists.
     */
    public InspectorStyle findStyle(String name) {
        for (InspectorStyle style : allStyles) {
            if (name.equals(style.name())) {
                return style;
            }
        }
        return null;
    }

    /**
     * @return all available styles.
     */
    public InspectorStyle[] allStyles() {
        return allStyles;
    }

}
