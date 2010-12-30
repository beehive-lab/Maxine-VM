/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.*;

/**
 * A generator for simple geometric icons.
 */
public final class IconFactory {

    private IconFactory() {
    }

    private static class BlankIcon implements Icon {
        private final int width;
        private final int height;

        public BlankIcon(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int getIconHeight() {
            return height;
        }

        public int getIconWidth() {
            return width;
        }

        public void paintIcon(Component component, Graphics g, int x, int y) {
            if (component.isOpaque()) {
                g.setColor(component.getBackground());
                g.fillRect(x, y, width, height);
            }
        }
    }

    public static Icon createBlank(int width, int height) {
        return new BlankIcon(width, height);
    }

    public static Icon createBlank(Dimension size) {
        return createBlank(size.width, size.height);
    }

    private static class CrossIcon extends BlankIcon {
        public CrossIcon(int width, int height) {
            super(width, height);
        }

        @Override
        public void paintIcon(Component component, Graphics g, int x, int y) {
            super.paintIcon(component, g, x, y);
            final Graphics2D g2 = (Graphics2D) g.create();

            g2.setColor(component.getForeground());
            g2.translate(x, y);
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(MARGIN, MARGIN, getIconWidth() - MARGIN, getIconHeight() - MARGIN);
            g2.drawLine(MARGIN, getIconHeight() - MARGIN, getIconWidth() - MARGIN, MARGIN);
            g2.translate(-x, -y);
            g2.dispose();
        }
    }

    public static Icon createCrossIcon(int width, int height) {
        return new CrossIcon(width, height);
    }

    private static class PixelatedIcon extends BlankIcon {

        private final Color foreground;

        public PixelatedIcon(Color foreground) {
            super(2, 2);
            this.foreground = foreground;
        }

        @Override
        public void paintIcon(Component component, Graphics g, int x, int y) {
            super.paintIcon(component, g, x, y);
            final Graphics2D g2 = (Graphics2D) g.create();

            g2.setColor(foreground);
            g2.translate(x, y);
            g2.drawLine(0, 0, 1, 1);
            g2.translate(-x, -y);
            g2.dispose();
        }
    }

    public static Icon createPixelatedIcon(Color foreground) {
        return new PixelatedIcon(foreground);
    }

    private static class PolygonIcon extends BlankIcon {
        protected final Polygon polygon = new Polygon();

        public PolygonIcon(int width, int height) {
            super(width, height);
        }

        @Override
        public void paintIcon(Component component, Graphics g, int x, int y) {
            super.paintIcon(component, g, x, y);
            g.setColor(component.getForeground());
            g.translate(x, y);
            g.fillPolygon(polygon);
            g.translate(-x, -y);
        }
    }

    private static final int MARGIN = 2;

    private static class UpArrowIcon extends PolygonIcon {
        UpArrowIcon(int width, int height) {
            super(width, height);
            polygon.addPoint(width / 2, MARGIN);
            polygon.addPoint(width * 7 / 8, height / 3);
            polygon.addPoint(width * 5 / 8, height / 3);
            polygon.addPoint(width * 5 / 8, height - MARGIN);
            polygon.addPoint(width * 3 / 8, height - MARGIN);
            polygon.addPoint(width * 3 / 8, height / 3);
            polygon.addPoint(width * 1 / 8, height / 3);
        }
    }

    public static Icon createUpArrow(int width, int height) {
        return new UpArrowIcon(width, height);
    }

    public static Icon createUpArrow(Dimension size) {
        return createUpArrow(size.width, size.height);
    }

    private static class DownArrowIcon extends PolygonIcon {
        DownArrowIcon(int width, int height) {
            super(width, height);
            polygon.addPoint(width / 2, height - MARGIN);
            polygon.addPoint(width * 7 / 8, height * 2 / 3);
            polygon.addPoint(width * 5 / 8, height * 2 / 3);
            polygon.addPoint(width * 5 / 8, MARGIN);
            polygon.addPoint(width * 3 / 8, MARGIN);
            polygon.addPoint(width * 3 / 8, height * 2 / 3);
            polygon.addPoint(width * 1 / 8, height * 2 / 3);
        }
    }

    public static Icon createDownArrow(int width, int height) {
        return new DownArrowIcon(width, height);
    }

    public static Icon createDownArrow(Dimension size) {
        return createDownArrow(size.width, size.height);
    }

    private static class LeftArrowIcon extends PolygonIcon {
        LeftArrowIcon(int width, int height) {
            super(width, height);
            polygon.addPoint(width - MARGIN, MARGIN);
            polygon.addPoint(MARGIN, height / 2);
            polygon.addPoint(width - MARGIN, height - MARGIN);
        }
    }

    public static Icon createLeftArrow(int width, int height) {
        return new LeftArrowIcon(width, height);
    }

    public static Icon createLeftArrow(Dimension size) {
        return createLeftArrow(size.width, size.height);
    }

    private static class RightArrowIcon extends PolygonIcon {
        RightArrowIcon(int width, int height) {
            super(width, height);
            polygon.addPoint(MARGIN, MARGIN);
            polygon.addPoint(width - MARGIN, height / 2);
            polygon.addPoint(MARGIN, height - MARGIN);
        }
    }

    public static Icon createRightArrow(int width, int height) {
        return new RightArrowIcon(width, height);
    }

    public static Icon createRightArrow(Dimension size) {
        return createRightArrow(size.width, size.height);
    }

    private static class LeftEndIcon extends PolygonIcon {
        LeftEndIcon(int width, int height) {
            super(width, height);
            polygon.addPoint(width - MARGIN, MARGIN);
            polygon.addPoint(2 * MARGIN, height / 2);
            polygon.addPoint(2 * MARGIN, height - MARGIN);
            polygon.addPoint(MARGIN, height - MARGIN);
            polygon.addPoint(MARGIN, MARGIN);
            polygon.addPoint(2 * MARGIN, MARGIN);
            polygon.addPoint(2 * MARGIN, height / 2);
            polygon.addPoint(width - MARGIN, height - MARGIN);
        }
    }

    public static Icon createLeftEnd(int width, int height) {
        return new LeftEndIcon(width, height);
    }

    public static Icon createLeftEnd(Dimension size) {
        return createLeftEnd(size.width, size.height);
    }

    private static class RightEndIcon extends PolygonIcon {
        RightEndIcon(int width, int height) {
            super(width, height);
            polygon.addPoint(MARGIN, MARGIN);
            polygon.addPoint(width - (2 * MARGIN), height / 2);
            polygon.addPoint(width - (2 * MARGIN), height - MARGIN);
            polygon.addPoint(width - MARGIN, height - MARGIN);
            polygon.addPoint(width - MARGIN, MARGIN);
            polygon.addPoint(width - (2 * MARGIN), MARGIN);
            polygon.addPoint(width - (2 * MARGIN), height / 2);
            polygon.addPoint(MARGIN, height - MARGIN);
        }
    }

    public static Icon createRightEnd(int width, int height) {
        return new RightEndIcon(width, height);
    }

    public static Icon createRightEnd(Dimension size) {
        return createRightEnd(size.width, size.height);
    }

    private static class DownTriangleIcon extends PolygonIcon {
        DownTriangleIcon(int width, int height) {
            super(width, height);
            polygon.addPoint(MARGIN, MARGIN);
            polygon.addPoint(width - MARGIN, MARGIN);
            polygon.addPoint(width / 2, height - MARGIN);
        }
    }

    public static Icon createDownTriangle(int width, int height) {
        return new DownTriangleIcon(width, height);
    }

    public static Icon createDownTriangle(Dimension size) {
        return createDownTriangle(size.width, size.height);
    }

}
