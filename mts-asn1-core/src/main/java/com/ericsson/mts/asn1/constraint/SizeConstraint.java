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
import java.util.Map;

class SizeConstraint extends AbstractConstraint {
    private String lower_bound;
    private String upper_bound;
    private boolean arelbNumber;
    private boolean areubNumber;
    private MainRegistry mainRegistry;

    SizeConstraint(MainRegistry mainRegistry) {
        this.mainRegistry = mainRegistry;
    }

    BigInteger getLower_bound() {
        if (arelbNumber) {
            return new BigInteger(lower_bound);
        } else {
            return new BigInteger(mainRegistry.getConstantFromName(lower_bound).getValue());
        }
    }

    void setLower_bound(String lower_bound, boolean isNumber) {
        if (this.lower_bound != null) {
            throw new RuntimeException();
        }
        this.lower_bound = lower_bound;
        this.arelbNumber = isNumber;
    }

    BigInteger getUpper_bound() {
        if (null == upper_bound) {
            return null;
        }
        if (areubNumber) {
            return new BigInteger(upper_bound);
        } else {
            return new BigInteger(mainRegistry.getConstantFromName(upper_bound).getValue());
        }
    }

    void setUpper_bound(String upper_bound, boolean isNumber) {
        if (this.upper_bound != null) {
            throw new RuntimeException();
        }
        this.upper_bound = upper_bound;
        this.areubNumber = isNumber;
    }

    void updateValue(Map<String, String> registry) {
        if (arelbNumber && areubNumber) {
            return;
        }
        for (String key : registry.keySet()) {
            if (key.equals(lower_bound)) {
                lower_bound = registry.get(key);
            }
            if (key.equals(upper_bound)) {
                upper_bound = registry.get(key);
            }
        }
    }

    @Override
    public String toString() {
        return "SizeConstraint{" +
                "lower_bound=" + lower_bound +
                ", upper_bound=" + upper_bound +
                '}';
    }
}
