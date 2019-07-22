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
import com.ericsson.mts.asn1.BitArray;
import com.ericsson.mts.asn1.BitInputStream;
import com.ericsson.mts.asn1.TranslatorContext;
import com.ericsson.mts.asn1.classhandler.ClassHandler;
import com.ericsson.mts.asn1.constraint.Constraints;
import com.ericsson.mts.asn1.exception.InvalideParameterException;
import com.ericsson.mts.asn1.exception.NotHandledCaseException;
import com.ericsson.mts.asn1.factory.FormatReader;
import com.ericsson.mts.asn1.factory.FormatWriter;
import com.ericsson.mts.asn1.registry.MainRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractObjectClassFieldTranslator extends AbstractTranslator {
    protected ClassHandler classHandler;
    protected String fieldName;
    protected Constraints constraints;

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
        constraints = new Constraints(mainRegistry);
        constraints.addConstraint(constraintContext.get(0));
        parameters.add(new Parameter("classObjectSet", constraints.getObjectSetName()));
        return this;
    }


    @Override
    public final void encode(String name, BitArray s, FormatReader reader, TranslatorContext translatorContext, List<String> parameters) throws Exception {
        doEncode(name, s, reader, translatorContext, getRegister(parameters));
    }

    @Override
    public final void decode(String name, BitInputStream s, FormatWriter writer, TranslatorContext translatorContext, List<String> parameters) throws Exception {
        doDecode(name, s, writer, translatorContext, getRegister(parameters));
    }

    public abstract void doEncode(String name, BitArray s, FormatReader reader, TranslatorContext translatorContext, Map<String, String> registry) throws Exception;


    protected abstract void doDecode(String name, BitInputStream s, FormatWriter writer, TranslatorContext translatorContext, Map<String, String> registry) throws Exception;


    @Override
    public String toString() {
        return "AbstractObjectClassFieldTranslator{" +
                "fieldName='" + fieldName + '\'' +
                '}';
    }

    @Override
    public List<String> getParameters() {
        List<String> parameterList = new ArrayList<>();
        if (constraints.getObjectSetName() == null) {
            throw new InvalideParameterException("Need an object set name");
        }
        parameterList.add(constraints.getObjectSetName());
        return parameterList;
    }
}
