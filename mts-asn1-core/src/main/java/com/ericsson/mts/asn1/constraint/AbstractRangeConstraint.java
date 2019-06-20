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

import java.util.Map;

public abstract class AbstractRangeConstraint extends AbstractConstraint {
    protected String lowerBound;
    protected String upperBound;
    protected boolean arelbNumber;
    protected boolean areubNumber;
    protected MainRegistry mainRegistry;

    AbstractRangeConstraint(MainRegistry mainRegistry) {
        this.mainRegistry = mainRegistry;
    }


    void setLowerBound(String lower_bound, boolean isNumber) {
        if (this.lowerBound != null) {
            throw new RuntimeException();
        }
        this.lowerBound = lower_bound;
        this.arelbNumber = isNumber;
    }

    void setUpperBound(String upper_bound, boolean isNumber) {
        if (this.upperBound != null) {
            throw new RuntimeException();
        }
        this.upperBound = upper_bound;
        this.areubNumber = isNumber;
    }

    void updateValue(Map<String, String> registry) {
        if (arelbNumber && areubNumber) {
            return;
        }
        for (String key : registry.keySet()) {
            if (key.equals(lowerBound)) {
                lowerBound = registry.get(key);
            }
            if (key.equals(upperBound)) {
                upperBound = registry.get(key);
            }
        }
    }

    @Override
    public String toString() {
        return "SizeConstraint{" +
                "lowerBound=" + lowerBound +
                ", upperBound=" + upperBound +
                '}';
    }
}
