/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.ericsson.mts.asn1.constraint;

import com.ericsson.mts.asn1.registry.MainRegistry;

import java.math.BigInteger;

/**
 * Use when SIZE keyword appears X.680 51.5
 */
class SizeConstraint extends AbstractRangeConstraint {

    SizeConstraint(MainRegistry mainRegistry) {
        super(mainRegistry);
    }

    BigInteger getLowerBound() {
        if (arelbNumber) {
            return new BigInteger(lowerBound);
        } else {
            return new BigInteger(mainRegistry.getConstantFromName(lowerBound).getValue());
        }
    }

    BigInteger getUpperBound() {
        if (null == upperBound) {
            return null;
        }
        if (areubNumber) {
            return new BigInteger(upperBound);
        } else {
            return new BigInteger(mainRegistry.getConstantFromName(upperBound).getValue());
        }
    }
}
