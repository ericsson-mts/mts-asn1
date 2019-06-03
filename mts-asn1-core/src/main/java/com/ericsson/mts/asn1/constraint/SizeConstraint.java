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


import java.math.BigInteger;

public class SizeConstraint extends AbstractConstraint {
    private BigInteger lower_bound;
    private BigInteger upper_bound;


    public BigInteger getLower_bound() {
        return lower_bound;
    }

    public void setLower_bound(BigInteger lower_bound) {
        this.lower_bound = lower_bound;
    }

    public BigInteger getUpper_bound() {
        return upper_bound;
    }

    public void setUpper_bound(BigInteger upper_bound) {
        this.upper_bound = upper_bound;
    }

    @Override
    public String toString() {
        return "SizeConstraint{" +
                "lower_bound=" + lower_bound +
                ", upper_bound=" + upper_bound +
                '}';
    }
}
