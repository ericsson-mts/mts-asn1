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


import com.ericsson.mts.asn1.ASN1Parser;
import com.ericsson.mts.asn1.exception.NotHandledCaseException;
import com.ericsson.mts.asn1.registry.MainRegistry;
import com.ericsson.mts.asn1.translator.AbstractTranslator;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Constraints {
    private ConstraintVisitor constraintVisitor;
    private ClassFieldConstraint classFieldConstraint;
    private SizeConstraint sizeConstraint;
    private ContentsConstraint contentsConstraint;
    private ValueRangeConstraint valueRangeConstraint;
    private List<SingleValueConstraint> singleValueConstraintList = new ArrayList<>();


    public Constraints(MainRegistry mainRegistry) {
        constraintVisitor = new ConstraintVisitor(mainRegistry);
    }

    //Core methods

    public void addConstraint(ASN1Parser.ConstraintContext constraintContext) {
        constraintVisitor.addConstraint(constraintContext, this);
    }

    public void addSizeConstraint(ASN1Parser.SizeConstraintContext sizeConstraintContext) {
        constraintVisitor.addSizeConstraint(sizeConstraintContext, this);
        if (!hasSizeConstraint()) {
            throw new RuntimeException();
        }
    }


    public boolean hasSizeConstraint() {
        return sizeConstraint != null;
    }

    private boolean hasClassFieldConstraint() {
        return classFieldConstraint != null;
    }

    public boolean hasContentsConstraint() {
        return contentsConstraint != null;
    }

    public boolean hasValueRangeConstraint() {
        return valueRangeConstraint != null;
    }

    public boolean hasSingleValueConstraints() {
        return !singleValueConstraintList.isEmpty();
    }

    public boolean isExtensible() {
        if (sizeConstraint != null && sizeConstraint.isExtensible() ||
                classFieldConstraint != null && classFieldConstraint.isExtensible() ||
                contentsConstraint != null && contentsConstraint.isExtensible() ||
                valueRangeConstraint != null && valueRangeConstraint.isExtensible()
        ) {
            return true;
        } else if (hasSingleValueConstraints()) {
            for (SingleValueConstraint singleValueConstraint : singleValueConstraintList) {
                if (singleValueConstraint.isExtensible()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**********Methods for ConstraintVisitor **********/

    void addSizeConstraint(AbstractConstraint sizeConstraint) {
        if (!hasSizeConstraint()) {
            this.sizeConstraint = (SizeConstraint) sizeConstraint;
        } else if (!(this.sizeConstraint == sizeConstraint)) {
            throw new NotHandledCaseException("Multiple size constraint");
        }
    }

    void addClassFieldConstraint(AbstractConstraint classFieldConstraint) {
        if (!hasClassFieldConstraint()) {
            this.classFieldConstraint = (ClassFieldConstraint) classFieldConstraint;
        } else {
            throw new NotHandledCaseException("Multiple class field constraint");
        }
    }

    void addContentConstraint(AbstractConstraint contentConstraint) {
        if (!hasContentsConstraint()) {
            this.contentsConstraint = (ContentsConstraint) contentConstraint;
        } else {
            throw new NotHandledCaseException("Multiple contents constraint");
        }
    }

    void addValueRangeConstraint(AbstractConstraint valueRangeConstraint) {
        if (!hasValueRangeConstraint()) {
            this.valueRangeConstraint = (ValueRangeConstraint) valueRangeConstraint;
        } else {
            throw new NotHandledCaseException("Multiple value range constraint");
        }
    }

    void addSingleValueConstraint(AbstractConstraint singleValueConstraint) {
        SingleValueConstraint singleValueConstraint1 = (SingleValueConstraint) singleValueConstraint;
        if (!singleValueConstraintList.contains(singleValueConstraint1)) {
            singleValueConstraintList.add(singleValueConstraint1);
        }
    }

    //Methods of SizeConstraint

    public BigInteger getLowerBound() {
        return sizeConstraint.getLowerBound();
    }

    public BigInteger getUpperBound() {
        return sizeConstraint.getUpperBound();
    }

    public void updateSizeConstraint(Map<String, String> registry) {
        sizeConstraint.updateValue(registry);
    }

    //Methods of ClassFieldConstraint

    public String getObjectSetName() {
        return classFieldConstraint.getObjectSetName();
    }

    public String getTargetComponent() {
        return classFieldConstraint.getTargetComponent();
    }

    //Methods of ContentsConstraint

    public AbstractTranslator getContentTranslator() {
        return contentsConstraint.getContentTranslator();
    }

    //Methods of ValueRangeConstraint

    public BigInteger getLowerRange() {
        return valueRangeConstraint.getLowerRange();
    }

    public BigInteger getUpperRange() {
        return valueRangeConstraint.getUpperRange();
    }

    public void updateValueRangeConstraint(Map<String, String> registry) {
        valueRangeConstraint.updateValue(registry);
    }

    //Methods of SingleValueConstraint

    public BigInteger getSingleValueConstraint() {
        if (singleValueConstraintList.size() != 1) {
            throw new RuntimeException();
        }
        return singleValueConstraintList.get(0).getValue();
    }

    public void updateSingleValueConstraints(Map<String, String> registry) {
        singleValueConstraintList.forEach(singleValueConstraint -> {
            singleValueConstraint.updateValue(registry);
        });
    }
}
