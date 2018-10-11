/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.image.*;

import javax.swing.*;

public abstract class InspectorImageIcon extends ImageIcon {

    protected abstract void draw(Graphics2D g);

    private final int width;

    public int width() {
        return width;
    }

    private final int height;

    public int height() {
        return height;
    }

    protected InspectorImageIcon(int width, int height) {
        this.width = width;
        this.height = height;
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = (Graphics2D) image.getGraphics().create();
        draw(g);
        g.dispose();
        setImage(image);
    }

    private static final class BlankIcon extends InspectorImageIcon {
        private BlankIcon(int width, int height) {
            super(width, height);
        }

        @Override
        public void draw(Graphics2D g) {
            g.setColor(InspectorStyle.CoolGray1);
        }
    }

    public static InspectorImageIcon createBlank(int width, int height) {
        return new BlankIcon(width, height);
    }

    public static InspectorImageIcon createBlank(Dimension size) {
        return createBlank(size.width, size.height);
    }

    private static final int MARGIN = 2;

    private static final class DownTriangleIcon extends InspectorImageIcon {
        DownTriangleIcon(int width, int height) {
            super(width, height);
        }

        @Override
        public void draw(Graphics2D g) {
            g.setColor(InspectorStyle.Black);
            final Polygon polygon = new Polygon();
            polygon.addPoint(MARGIN, MARGIN);
            polygon.addPoint(width() - MARGIN, MARGIN);
            polygon.addPoint(width() / 2, height() - MARGIN);
            g.fillPolygon(polygon);
        }
    }

    public static InspectorImageIcon createDownTriangle(int width, int height) {
        return new DownTriangleIcon(width, height);
    }

    public static InspectorImageIcon createDownTriangle(Dimension size) {
        return createDownTriangle(size.width, size.height);
    }

}
