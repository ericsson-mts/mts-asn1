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
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractChoiceTranslator extends AbstractTranslator {
    protected List<Pair<String, AbstractTranslator>> fieldList = new ArrayList<>();
    protected List<Pair<String, AbstractTranslator>> extensionFieldList = new ArrayList<>();
    protected boolean optionalExtensionMarker = false;

    public AbstractTranslator init(MainRegistry mainRegistry, ASN1Parser.ChoiceTypeContext ctx) throws NotHandledCaseException {

        List<ASN1Parser.NamedTypeContext> namedTypeContexts = ctx.alternativeTypeLists().rootAlternativeTypeList().alternativeTypeList().namedType();
        namedTypeContexts.forEach(namedTypeContext -> {
            AbstractTranslator translator = mainRegistry.getTranslator(namedTypeContext.asnType());
            fieldList.add(new Pair<>(namedTypeContext.IDENTIFIER().getText(), translator));
        });

        if (ctx.alternativeTypeLists().extensionAndException() != null) {
            optionalExtensionMarker = true;
            if (ctx.alternativeTypeLists().extensionAndException().exceptionSpec() != null) {
                throw new NotHandledCaseException();
            }
            if (ctx.alternativeTypeLists().extensionAdditionAlternatives().extensionAdditionAlternativesList() != null) {
                ctx.alternativeTypeLists().extensionAdditionAlternatives().extensionAdditionAlternativesList().extensionAdditionAlternative().forEach(extensionAdditionAlternativeContext -> {
                    if (extensionAdditionAlternativeContext.extensionAdditionAlternativesGroup() != null) {
                        throw new NotHandledCaseException();
                    } else {
                        AbstractTranslator translator = mainRegistry.getTranslator(extensionAdditionAlternativeContext.namedType().asnType());
                        extensionFieldList.add(new Pair<>(extensionAdditionAlternativeContext.namedType().IDENTIFIER().getText(), translator));
                    }
                });
                if (ctx.alternativeTypeLists().optionalExtensionMarker() != null) {
                    if (ctx.alternativeTypeLists().optionalExtensionMarker().ELLIPSIS() != null)
                        optionalExtensionMarker = true;
                }
            }
        }
        return this;
    }

    @Override
    public final void encode(String name, BitArray s, FormatReader reader, TranslatorContext translatorContext, List<String> parameters) throws Exception {
        reader.enterObject(name);
        doEncode(s, reader, reader.fieldsValue().get(0));
        reader.leaveObject(name);
    }

    public abstract void doEncode(BitArray s, FormatReader reader, String choiceValue) throws Exception;

    @Override
    public void decode(String name, BitInputStream s, FormatWriter writer, TranslatorContext translatorContext, List<String> parameters) throws Exception {
        writer.enterObject(name);
        doDecode(s, writer);
        writer.leaveObject(name);
    }

    public abstract void doDecode(BitInputStream s, FormatWriter writer) throws Exception;


    @Override
    public String toString() {
        return "AbstractChoiceTranslator{" +
                "fieldList=" + fieldList +
                ", optionalExtensionMarker=" + optionalExtensionMarker +
                '}';
    }
}
