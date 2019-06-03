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
import com.ericsson.mts.asn1.constraint.SizeConstraint;
import com.ericsson.mts.asn1.exception.NotHandledCaseException;
import com.ericsson.mts.asn1.factory.FormatReader;
import com.ericsson.mts.asn1.factory.FormatWriter;
import com.ericsson.mts.asn1.registry.MainRegistry;
import com.ericsson.mts.asn1.visitor.ConstraintVisitor;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractBitStringTranslator extends AbstractTranslator {
    protected SizeConstraint sizeConstraint;
    protected HashMap<String, String> namedBitList = new HashMap<>();

    public AbstractTranslator init(ASN1Parser.BitStringTypeContext bitStringTypeContext, MainRegistry mainRegistry, List<ASN1Parser.ConstraintContext> constraintContexts) throws NotHandledCaseException {
        if (bitStringTypeContext.namedBitList() != null) {
            throw new NotHandledCaseException();
        }

        if (constraintContexts.size() != 1) {
            throw new NotHandledCaseException();
        }
        if (constraintContexts.get(0) != null) {
            sizeConstraint = (SizeConstraint) new ConstraintVisitor(ConstraintVisitor.SIZE_CONSTRAINT, mainRegistry).visitConstraint(constraintContexts.get(0));
            if (sizeConstraint == null) {
                throw new NotHandledCaseException();
            }
        }
        return this;
    }

    @Override
    public final void encode(String name, BitArray s, FormatReader reader, TranslatorContext translatorContext, List<String> parameters) throws Exception {
        doEncode(s, reader, reader.bitsValue(name));
    }

    public abstract void doEncode(BitArray s, FormatReader reader, String value) throws IOException;

    @Override
    public final void decode(String name, BitInputStream s, FormatWriter writer, TranslatorContext translatorContext, List<String> parameters) throws NotHandledCaseException, IOException {
        writer.bitsValue(name, doDecode(s, writer));
    }

    public abstract String doDecode(BitInputStream s, FormatWriter writer) throws NotHandledCaseException, IOException;
}
