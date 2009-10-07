/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.ins.gui;

import java.awt.*;

import javax.swing.*;

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
