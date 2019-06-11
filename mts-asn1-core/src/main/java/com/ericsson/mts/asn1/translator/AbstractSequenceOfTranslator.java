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
import com.ericsson.mts.asn1.constraint.Constraints;
import com.ericsson.mts.asn1.exception.NotHandledCaseException;
import com.ericsson.mts.asn1.factory.FormatReader;
import com.ericsson.mts.asn1.factory.FormatWriter;
import com.ericsson.mts.asn1.registry.MainRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractSequenceOfTranslator extends AbstractTranslator {
    protected AbstractTranslator typeTranslator;
    protected Constraints constraints;
    protected List<String> actualParameters = new ArrayList<>();

    public AbstractTranslator init(MainRegistry mainRegistry, ASN1Parser.SequenceOfTypeContext sequenceOfTypeContext) throws NotHandledCaseException {
        constraints = new Constraints(mainRegistry);
        if (sequenceOfTypeContext.sizeConstraint() != null) {

            constraints.addSizeConstraint(sequenceOfTypeContext.sizeConstraint());
            if (!constraints.hasSizeConstraint()) {
                throw new RuntimeException();
            }
        } else if (sequenceOfTypeContext.constraint() != null) {
            throw new NotHandledCaseException();
        }
        typeTranslator = mainRegistry.getTranslator(sequenceOfTypeContext.asnType());
        if ((sequenceOfTypeContext.asnType().referencedType() != null) && (sequenceOfTypeContext.asnType().referencedType().definedType().actualParameterList() != null)) {
            handleParameters(sequenceOfTypeContext.asnType().referencedType().definedType().actualParameterList());
        }
        return this;
    }

    public AbstractTranslator init(MainRegistry mainRegistry, ASN1Parser.SequenceOfTypeContext sequenceOfTypeContext, ASN1Parser.ParameterListContext parameterListContext) {
        addParameters(parameterListContext);
        return init(mainRegistry, sequenceOfTypeContext);
    }

    private void handleParameters(ASN1Parser.ActualParameterListContext actualParameterListContext) {
        for (ASN1Parser.ActualParameterContext actualParameterContext : actualParameterListContext.actualParameter()) {
            if (actualParameterContext.value() != null) {
                if (actualParameterContext.value().builtinValue().objectIdentifierValue() != null) {
                    if (actualParameterContext.value().builtinValue().objectIdentifierValue().objIdComponentsList() != null) {
                        if (actualParameterContext.value().builtinValue().objectIdentifierValue().objIdComponentsList().objIdComponents().size() == 1) {
                            if (actualParameterContext.value().builtinValue().objectIdentifierValue().objIdComponentsList().objIdComponents(0).IDENTIFIER() != null
                                    && actualParameterContext.value().builtinValue().objectIdentifierValue().objIdComponentsList().objIdComponents(0).L_PARAN() == null) {
                                actualParameters.add(actualParameterContext.value().builtinValue().objectIdentifierValue().objIdComponentsList().objIdComponents(0).IDENTIFIER().getText());
                            }
                        } else {
                            throw new NotHandledCaseException();
                        }
                    } else {
                        throw new NotHandledCaseException();
                    }
                } else {
                    throw new NotHandledCaseException();
                }
            } else {
                throw new NotHandledCaseException("asnType");
            }
        }
    }

    @Override
    public final void encode(String inputName, BitArray s, FormatReader reader, TranslatorContext translatorContext, List<String> parameters) throws Exception {
        int numberOfComponents = reader.enterArray(inputName);
        doEncode(s, reader, numberOfComponents, getRegister(parameters));
        reader.leaveArray(inputName);
    }

    public abstract void doEncode(BitArray s, FormatReader reader, int numberOfComponents, Map<String, String> registry) throws Exception;

    @Override
    public final void decode(String name, BitInputStream s, FormatWriter writer, TranslatorContext translatorContext, List<String> parameters) throws NotHandledCaseException, IOException {
        writer.enterArray(name);
        doDecode(s, writer, getRegister(parameters));
        writer.leaveArray(name);
    }

    protected abstract void doDecode(BitInputStream s, FormatWriter writer, Map<String, String> registry) throws NotHandledCaseException, IOException;

    @Override
    public String toString() {
        return "AbstractSequenceOfTranslator{" +
                "typeTranslator=" + typeTranslator +
                ", constraints=" + constraints +
                ", actualParameters=" + actualParameters +
                ", name='" + name + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}
