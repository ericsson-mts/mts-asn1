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

public class SingleValueConstraint extends AbstractConstraint {
    private String value;
    private boolean isNumber;
    private MainRegistry mainRegistry;

    /**
     * Use when only a number/identifier appear in a constraint X.680 51.2.1
     *
     * @param mainRegistry main registry
     */
    public SingleValueConstraint(MainRegistry mainRegistry) {
        this.mainRegistry = mainRegistry;
    }

    public BigInteger getValue() {
        if (null == value) {
            return null;
        }
        if (isNumber) {
            return new BigInteger(value);
        } else {
            return new BigInteger(mainRegistry.getConstantFromName(value).getValue());
        }
    }

    public void setValue(String value, boolean isNumber) {
        if (null != this.value) {
            throw new RuntimeException("Multiple value assigned in the same SingleValueConstraint");
        }
        this.value = value;
        this.isNumber = isNumber;
    }

    public void updateValue(Map<String, String> registry) {
        if (isNumber) {
            return;
        }
        for (String key : registry.keySet()) {
            if (key.equals(value)) {
                value = registry.get(key);
            }
        }
    }
}
