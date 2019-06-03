/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.ericsson.mts.asn1.translator;

import com.ericsson.mts.asn1.ASN1Parser;
import com.ericsson.mts.asn1.classhandler.ClassHandler;
import com.ericsson.mts.asn1.constraint.ClassFieldConstraint;
import com.ericsson.mts.asn1.exception.NotHandledCaseException;
import com.ericsson.mts.asn1.registry.MainRegistry;
import com.ericsson.mts.asn1.visitor.ConstraintVisitor;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractObjectClassFieldTranslator extends AbstractTranslator {
    protected ClassHandler classHandler;
    protected String fieldName;
    protected ClassFieldConstraint classFieldConstraint;

    public AbstractTranslator init(MainRegistry mainRegistry, ASN1Parser.ObjectClassFieldTypeContext objectClassFieldTypeContext, List<ASN1Parser.ConstraintContext> constraintContext) {
        if (objectClassFieldTypeContext.definedObjectClass().IDENTIFIER(0) != null) {
            if (objectClassFieldTypeContext.definedObjectClass().DOT() == null) {
                classHandler = mainRegistry.getClassHandler(objectClassFieldTypeContext.definedObjectClass().IDENTIFIER(0).getText());
            } else {
                throw new NotHandledCaseException();
            }
        } else {
            throw new NotHandledCaseException();
        }

        if (objectClassFieldTypeContext.fieldName().DOT(0) == null) {
            fieldName = objectClassFieldTypeContext.fieldName().IDENTIFIER(0).getText();
        } else {
            throw new NotHandledCaseException();
        }

        if (constraintContext.size() != 1) {
            throw new NotHandledCaseException(" sizeConstraint " + constraintContext.toString());
        }
        classFieldConstraint = (ClassFieldConstraint) new ConstraintVisitor(ConstraintVisitor.CLASS_FIELD_CONSTRAINT, mainRegistry).visitConstraint(constraintContext.get(0));
        parameters.add(new Parameter("classObjectSet", classFieldConstraint.getObjectSetName()));
        return this;
    }

    @Override
    public String toString() {
        return "AbstractObjectClassFieldTranslator{" +
                "fieldName='" + fieldName + '\'' +
                ", classFieldConstraint=" + classFieldConstraint +
                '}';
    }

    @Override
    public List<String> getParameters() {
        List<String> parameterList = new ArrayList<>();
        if (classFieldConstraint.getObjectSetName() == null) {
            throw new RuntimeException();
        }
        parameterList.add(classFieldConstraint.getObjectSetName());
        return parameterList;
    }
}
