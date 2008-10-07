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
/*VCSID=22f05698-cf17-4ef8-b850-06d3cb888b38*/
package com.sun.max.ins.gui;

import java.awt.*;
import java.awt.image.*;

import javax.swing.*;

public abstract class InspectorImageIcon extends ImageIcon {

    protected abstract void draw(Graphics2D g);

    private final int _width;

    public int width() {
        return _width;
    }

    private final int _height;

    public int height() {
        return _height;
    }

    protected InspectorImageIcon(int width, int height) {
        super();
        _width = width;
        _height = height;
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

    private static final int _MARGIN = 2;

    private static final class DownTriangleIcon extends InspectorImageIcon {
        DownTriangleIcon(int width, int height) {
            super(width, height);
        }

        @Override
        public void draw(Graphics2D g) {
            g.setColor(InspectorStyle.Black);
            final Polygon polygon = new Polygon();
            polygon.addPoint(_MARGIN, _MARGIN);
            polygon.addPoint(width() - _MARGIN, _MARGIN);
            polygon.addPoint(width() / 2, height() - _MARGIN);
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
