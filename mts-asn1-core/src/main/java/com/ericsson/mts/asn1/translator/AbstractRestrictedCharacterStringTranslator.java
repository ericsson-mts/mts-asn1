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
import java.util.List;

public abstract class AbstractRestrictedCharacterStringTranslator extends AbstractTranslator {
    protected Constraints constraints;
    protected boolean isknownMultiplierCharacterStringType = false;
    protected MainRegistry mainRegistry;
    protected KnownMultiplierCharacterString knownMultiplierCharacterString;

    public AbstractRestrictedCharacterStringTranslator init(MainRegistry mainRegistry, ASN1Parser.CharacterStringTypeContext characterStringTypeContext, List<ASN1Parser.ConstraintContext> constraintContexts) {
        this.mainRegistry = mainRegistry;
        if (characterStringTypeContext.restrictedCharacterStringType() != null && characterStringTypeContext.restrictedCharacterStringType().PRINTABLE_STRING() != null) {
            constraints = new Constraints(mainRegistry);
            constraints.addConstraint(constraintContexts.get(0));
            if (!constraints.hasSizeConstraint()) {
                throw new NotHandledCaseException();
            }
            isknownMultiplierCharacterStringType = true;
            knownMultiplierCharacterString = KnownMultiplierCharacterString.PrintableString;
        } else {
            throw new NotHandledCaseException();
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

    protected enum KnownMultiplierCharacterString {
        PrintableString(32, 122, 7, 8);

        private final int lb;
        private final int ub;
        private final int b;
        private final int b2;

        KnownMultiplierCharacterString(int lb, int ub, int b, int b2) {
            this.lb = lb;
            this.ub = ub;
            this.b = b;
            this.b2 = b2;
        }

        public int getLb() {
            return lb;
        }

        public int getUb() {
            return ub;
        }

        public int getB() {
            return b;
        }

        public int getB2() {
            return b2;
        }
    }
}
