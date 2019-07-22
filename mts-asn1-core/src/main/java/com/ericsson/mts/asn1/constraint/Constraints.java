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

/**
 * Handle all constraints in one class
 */
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

    /**
     * Call constraintVisitor which will parse and append constraint
     *
     * @param constraintContext context of the constraint
     */
    public void addConstraint(ASN1Parser.ConstraintContext constraintContext) {
        constraintVisitor.addConstraint(constraintContext, this);
    }

    /**
     * Use by SET OF and SEQUENCE OF keywords according to X.680 49.5
     *
     * @param sizeConstraintContext context of the constraint
     */
    public void addSizeConstraint(ASN1Parser.SizeConstraintContext sizeConstraintContext) {
        constraintVisitor.addSizeConstraint(sizeConstraintContext, this);
        if (!hasSizeConstraint()) {
            throw new RuntimeException();
        }
    }

    /**
     * Use by translators to check if there is a size constraint
     *
     * @return true if the is a size constraint, false otherwise
     */
    public boolean hasSizeConstraint() {
        return sizeConstraint != null;
    }

    /**
     * Use by translators to check if there is a class field constraint
     *
     * @return true if the is a class field constraint, false otherwise
     */
    private boolean hasClassFieldConstraint() {
        return classFieldConstraint != null;
    }

    /**
     * Use by translators to check if there is a content constraint
     *
     * @return true if the is a content constraint, false otherwise
     */
    public boolean hasContentsConstraint() {
        return contentsConstraint != null;
    }

    /**
     * Use by translators to check if there is a value range constraint
     *
     * @return true if the is a value range constraint, false otherwise
     */
    public boolean hasValueRangeConstraint() {
        return valueRangeConstraint != null;
    }

    /**
     * Use by translators to check if there is a single value constraint
     *
     * @return true if the is a single value constraint, false otherwise
     */
    public boolean hasSingleValueConstraints() {
        return !singleValueConstraintList.isEmpty();
    }

    /**
     * Check if constrains is extensible
     *
     * @return true if extensible, false otherwise
     */
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

    //WARNING ! Following methods are used for ConstraintVisitor and should not be used outside the package

    /**
     * Add a size constraint
     * @param sizeConstraint size constraint
     */
    void addSizeConstraint(AbstractConstraint sizeConstraint) {
        if (!hasSizeConstraint()) {
            this.sizeConstraint = (SizeConstraint) sizeConstraint;
        } else if (!(this.sizeConstraint == sizeConstraint)) {
            throw new NotHandledCaseException("Multiple size constraint");
        }
    }

    /**
     * Add a class field constraint
     * @param classFieldConstraint class field constraint
     */
    void addClassFieldConstraint(AbstractConstraint classFieldConstraint) {
        if (!hasClassFieldConstraint()) {
            this.classFieldConstraint = (ClassFieldConstraint) classFieldConstraint;
        } else {
            throw new NotHandledCaseException("Multiple class field constraint");
        }
    }

    /**
     * Add a content constraint
     * @param contentConstraint content constraint
     */
    void addContentConstraint(AbstractConstraint contentConstraint) {
        if (!hasContentsConstraint()) {
            this.contentsConstraint = (ContentsConstraint) contentConstraint;
        } else {
            throw new NotHandledCaseException("Multiple contents constraint");
        }
    }

    /**
     * Add a value range constraint
     * @param valueRangeConstraint value range constraint
     */
    void addValueRangeConstraint(AbstractConstraint valueRangeConstraint) {
        if (!hasValueRangeConstraint()) {
            this.valueRangeConstraint = (ValueRangeConstraint) valueRangeConstraint;
        } else {
            throw new NotHandledCaseException("Multiple value range constraint");
        }
    }

    /**
     * Add a single value constraint
     * @param singleValueConstraint single value constraint
     */
    void addSingleValueConstraint(AbstractConstraint singleValueConstraint) {
        SingleValueConstraint singleValueConstraint1 = (SingleValueConstraint) singleValueConstraint;
        if (!singleValueConstraintList.contains(singleValueConstraint1)) {
            singleValueConstraintList.add(singleValueConstraint1);
        }
    }

    //Methods of SizeConstraint

    /**
     * Get lower bound
     * @return lower bound
     */
    public BigInteger getLowerBound() {
        return sizeConstraint.getLowerBound();
    }

    /**
     * Get upper bound
     * @return upper bound
     */
    public BigInteger getUpperBound() {
        return sizeConstraint.getUpperBound();
    }

    /**
     * Update value when a size constraint depend on a parameter
     * @param registry parameter registry
     */
    public void updateSizeConstraint(Map<String, String> registry) {
        sizeConstraint.updateValue(registry);
    }

    //Methods of ClassFieldConstraint

    /**
     * Get the target object set identifier
     * @return object set identifier
     */
    public String getObjectSetName() {
        return classFieldConstraint.getObjectSetName();
    }

    /**
     * Get the target component identifier
     * @return target component identifier
     */
    public String getTargetComponent() {
        return classFieldConstraint.getTargetComponent();
    }

    //Methods of ContentsConstraint

    /**
     * Get content translator (keyword : CONTAINING)
     * @return an abstract translator
     */
    public AbstractTranslator getContentTranslator() {
        return contentsConstraint.getContentTranslator();
    }

    //Methods of ValueRangeConstraint

    /**
     * Get the lower range
     * @return lower range
     */
    public BigInteger getLowerRange() {
        return valueRangeConstraint.getLowerRange();
    }

    /**
     * Get the upper range
     * @return upper range
     */
    public BigInteger getUpperRange() {
        return valueRangeConstraint.getUpperRange();
    }

    /**
     * Update value when a value range constraint depend on a parameter
     * @param registry parameter registry
     */
    public void updateValueRangeConstraint(Map<String, String> registry) {
        valueRangeConstraint.updateValue(registry);
    }

    //Methods of SingleValueConstraint; should be a replace by a value check in the future

    /**
     * Get the single value constraint
     * @return single value constraint
     */
    public BigInteger getSingleValueConstraint() {
        if (singleValueConstraintList.size() != 1) {
            throw new RuntimeException();
        }
        return singleValueConstraintList.get(0).getValue();
    }

    /**
     * Update value when a single value constraint depend on a parameter
     * @param registry parameter registry
     */
    public void updateSingleValueConstraints(Map<String, String> registry) {
        singleValueConstraintList.forEach(singleValueConstraint -> singleValueConstraint.updateValue(registry));
    }
}
