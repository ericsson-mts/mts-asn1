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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("Duplicates")
public abstract class AbstractEnumeratedTranslator extends AbstractTranslator {
    protected ArrayList<String> fieldList = new ArrayList<>();
    protected boolean exceptionSpec = false;
    protected boolean hasExtensionMarker = false;
    protected ArrayList<String> addtionnalfieldList = new ArrayList<>();
    private Logger logger = LoggerFactory.getLogger(AbstractEnumeratedTranslator.class.getSimpleName());

    public AbstractTranslator init(ASN1Parser.EnumeratedTypeContext enumeratedTypeContext) {
        if (enumeratedTypeContext.enumerations().ELLIPSIS() != null)
            hasExtensionMarker = true;
        enumeratedTypeContext.enumerations().rootEnumeration().enumeration().enumerationItem().forEach(enumerationItemContext -> {
            if (enumerationItemContext.IDENTIFIER() != null) {
                fieldList.add(enumerationItemContext.IDENTIFIER().getText());
            } else if (enumerationItemContext.namedNumber() != null) {
                if (enumerationItemContext.namedNumber().definedValue() != null) {
                    throw new NotHandledCaseException();
                } else {
                    fieldList.add(enumerationItemContext.namedNumber().IDENTIFIER().getText());
                }
            } else {
                fieldList.add(enumerationItemContext.value().getText());
            }
        });

        if (enumeratedTypeContext.enumerations().exceptionSpec() != null) {
            exceptionSpec = true;
        }
        if (enumeratedTypeContext.enumerations().additionalEnumeration() != null) {
            enumeratedTypeContext.enumerations().additionalEnumeration().enumeration().enumerationItem().forEach(enumerationItemContext -> {
                if (enumerationItemContext.IDENTIFIER() != null) {
                    fieldList.add(enumerationItemContext.IDENTIFIER().getText());
                } else if (enumerationItemContext.namedNumber() != null) {
                    if (enumerationItemContext.namedNumber().definedValue() != null) {
                        throw new NotHandledCaseException();
                    } else {
                        addtionnalfieldList.add(enumerationItemContext.namedNumber().IDENTIFIER().getText());
                    }
                } else {
                    throw new NotHandledCaseException();
                }
            });
        }
        return this;
    }

    @Override
    public final void encode(String name, BitArray s, FormatReader reader, TranslatorContext translatorContext, List<String> parameters) throws Exception {
        doEncode(s, reader, reader.stringValue(name));
    }

    public abstract void doEncode(BitArray s, FormatReader reader, String value) throws IOException;

    @Override
    public final void decode(String name, BitInputStream s, FormatWriter writer, TranslatorContext translatorContext, List<String> parameters) throws NotHandledCaseException, IOException {
        writer.stringValue(name, doDecode(s, writer));
    }

    public abstract String doDecode(BitInputStream s, FormatWriter writer) throws IOException;

    @Override
    public String toString() {
        return "AbstractEnumeratedTranslator{" +
                "fieldList=" + fieldList +
                ", exceptionSpec=" + exceptionSpec +
                ", addtionnalfieldList=" + addtionnalfieldList +
                '}';
    }
}
