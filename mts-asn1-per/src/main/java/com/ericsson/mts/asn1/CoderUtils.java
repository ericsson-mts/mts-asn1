/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.ericsson.mts.asn1;

public class CoderUtils {

    public static int getIntegerLength(int value) {
        long mask = 0x7f800000L;
        int sizeOfInt = 4;
        if (value < 0) {
            while (((mask & value) == mask) && (sizeOfInt > 1)) {
                mask = mask >> 8;
                sizeOfInt--;
            }
        } else {
            while (((mask & value) == 0) && (sizeOfInt > 1)) {
                mask = mask >> 8;
                sizeOfInt--;
            }
        }
        return sizeOfInt;
    }

    public static int getIntegerLength(long value) {
        long mask = 0x7f80000000000000L;
        int sizeOfInt = 8;
        if (value < 0) {
            while (((mask & value) == mask) && (sizeOfInt > 1)) {
                mask = mask >> 8;
                sizeOfInt--;
            }
        } else {
            while (((mask & value) == 0) && (sizeOfInt > 1)) {
                mask = mask >> 8;
                sizeOfInt--;
            }
        }
        return sizeOfInt;
    }

    public static int getPositiveIntegerLength(long value) {
        if (value < 0) {
            long mask = 0x7f80000000000000L;
            int sizeOfInt = 8;
            while (((mask & ~value) == mask) && (sizeOfInt > 1)) {
                mask = mask >> 8;
                sizeOfInt--;
            }
            return sizeOfInt;
        } else {
            return getIntegerLength(value);
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
