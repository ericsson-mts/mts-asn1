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
import com.ericsson.mts.asn1.exception.NotHandledCaseException;
import com.ericsson.mts.asn1.factory.FormatReader;
import com.ericsson.mts.asn1.factory.FormatWriter;
import com.ericsson.mts.asn1.registry.MainRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReferenceTranslator extends AbstractTranslator {
    private AbstractTranslator referencedTranslator;

    public AbstractTranslator init(MainRegistry mainRegistry, ASN1Parser.ReferencedTypeContext referencedTypeContext) {
        if (referencedTypeContext.definedType().DOT() != null) {
            throw new NotHandledCaseException();
        }
        String identifier = referencedTypeContext.definedType().IDENTIFIER(0).getText();
        referencedTranslator = mainRegistry.getTranslatorFromName(identifier);
        if (referencedTranslator == null) {
            throw new RuntimeException("Can't find translator " + identifier);
        }
        return this;
    }

    public AbstractTranslator init(MainRegistry mainRegistry, ASN1Parser.ReferencedTypeContext referencedTypeContext, ASN1Parser.ParameterListContext parameterListContext) {
        addParameters(parameterListContext);
        return init(mainRegistry, referencedTypeContext);
    }

    @Override
    public final void encode(String name, BitArray s, FormatReader reader, TranslatorContext translatorContext, List<String> parameters) throws Exception {
        logger.trace("Enter {} encoder, name {}", this.getClass().getSimpleName(), this.name);
        Map<String, String> registry = getRegister(parameters);
        List<String> parametersNeeded = referencedTranslator.getParameters();
        if (parametersNeeded.isEmpty()) {
            referencedTranslator.encode(name, s, reader, translatorContext);
        } else {
            //Building parameter list to pass to target translator
            List<String> inputParameters = new ArrayList<>();
            for (String parameter : parametersNeeded) {
                inputParameters.add(registry.get(parameter));
            }
            referencedTranslator.encode(name, s, reader, translatorContext, inputParameters);
        }
    }

    @Override
    public final void decode(String name, BitInputStream s, FormatWriter writer, TranslatorContext translatorContext, List<String> parameters) throws Exception {
        logger.trace("Enter {} translator, name {}", this.getClass().getSimpleName(), this.name);
        Map<String, String> registry = getRegister(parameters);
        List<String> parametersNeeded = referencedTranslator.getParameters();
        if (parametersNeeded.isEmpty()) {
            referencedTranslator.decode(name, s, writer, translatorContext);
        } else {
            //Building parameter list to pass to target translator
            List<String> inputParameters = new ArrayList<>();
            for (String parameter : parametersNeeded) {
                inputParameters.add(registry.get(parameter));
            }
            referencedTranslator.decode(name, s, writer, translatorContext, inputParameters);
        }
    }
}
