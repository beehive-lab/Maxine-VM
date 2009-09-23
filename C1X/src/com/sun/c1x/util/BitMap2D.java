/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.util;

/**
 * This class implements a two-dimensional bitmap.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 */
public class BitMap2D {

    private BitMap map;
    private int bitsPerSlot;

    private int bitIndex(int slotIndex, int bitWithinSlotIndex)  {
      return slotIndex * bitsPerSlot + bitWithinSlotIndex;
    }

    private void verifyBitWithinSlotIndex(int index)  {
      assert index < bitsPerSlot :  "bitWithinSlot index out of bounds";
    }

    public BitMap2D(int sizeInSlots, int bitsPerSlot) {
        map = new BitMap(sizeInSlots * bitsPerSlot);
        this.bitsPerSlot = bitsPerSlot;
    }

    public int sizeInBits() {
      return map.size();
    }

    // Returns number of full slots that have been allocated
    public int sizeInSlots() {
      return map.size() / bitsPerSlot;
    }

    public boolean isValidIndex(int slotIndex, int bitWithinSlotIndex) {
      verifyBitWithinSlotIndex(bitWithinSlotIndex);
      return (bitIndex(slotIndex, bitWithinSlotIndex) < sizeInBits());
    }

    public boolean at(int slotIndex, int bitWithinSlotIndex)  {
      verifyBitWithinSlotIndex(bitWithinSlotIndex);
      return map.get(bitIndex(slotIndex, bitWithinSlotIndex));
    }

    public void setBit(int slotIndex, int bitWithinSlotIndex) {
      verifyBitWithinSlotIndex(bitWithinSlotIndex);
      map.set(bitIndex(slotIndex, bitWithinSlotIndex));
    }

    public void clearBit(int slotIndex, int bitWithinSlotIndex) {
      verifyBitWithinSlotIndex(bitWithinSlotIndex);
      map.clear(bitIndex(slotIndex, bitWithinSlotIndex));
    }

    public void atPutGrow(int slotIndex, int bitWithinSlotIndex, boolean value) {
       int size = sizeInSlots();
       if (size <= slotIndex) {
           while (size <= slotIndex) {
               size *= 2;
           }
           BitMap newBitMap = new BitMap(size * bitsPerSlot);
           newBitMap.setUnion(map);
           map = newBitMap;
       }

       if (value) {
           setBit(slotIndex, bitWithinSlotIndex);
       } else {
           clearBit(slotIndex, bitWithinSlotIndex);
       }
    }

    public void clear() {
        map.clearAll();
    }
}
