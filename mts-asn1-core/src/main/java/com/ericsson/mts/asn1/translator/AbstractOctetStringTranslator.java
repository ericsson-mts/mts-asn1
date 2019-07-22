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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

public abstract class AbstractOctetStringTranslator extends AbstractTranslator {
    protected Constraints constraints;

    public AbstractTranslator init(MainRegistry mainRegistry, List<ASN1Parser.ConstraintContext> constraintContexts) throws NotHandledCaseException {
        constraints = new Constraints(mainRegistry);
        if (constraintContexts.size() == 0) {
            return this;
        } else if (constraintContexts.size() != 1) {
            throw new NotHandledCaseException();
        }
        if (constraintContexts.get(0) != null) {
            constraints.addConstraint(constraintContexts.get(0));
            if (!constraints.hasSizeConstraint() && !constraints.hasContentsConstraint() && !constraints.hasSingleValueConstraints()) {
                throw new NotHandledCaseException(constraintContexts.get(0).getText());
            }
        }
        return this;
    }

    @Override
    public final void encode(String name, BitArray s, FormatReader reader, TranslatorContext translatorContext, List<String> parameters) throws Exception {
        if (!constraints.hasContentsConstraint()) {
            doEncode(s, reader, reader.bytesValue(name));
        } else {
            BitArray bitArray = new BitArray();
            constraints.getContentTranslator().encode(name, bitArray, reader, translatorContext);
            if (!bitArray.getLength().mod(BigInteger.valueOf(8)).equals(BigInteger.ZERO)) {
                throw new RuntimeException("specification error ! X.682 : 11.4.a)");
            }
            doEncode(s, reader, bitArray.getBinaryMessage());
        }
    }

    public abstract void doEncode(BitArray s, FormatReader reader, String value) throws IOException;

    @Override
    public void decode(String name, BitInputStream s, FormatWriter writer, TranslatorContext translatorContext, List<String> parameters) throws Exception {
        if (!constraints.hasContentsConstraint()) {
            writer.bytesValue(name, doDecode(s, writer));
        } else {
            BitInputStream bitInputStream = new BitInputStream(new ByteArrayInputStream(doDecode(s, writer)));
            constraints.getContentTranslator().decode(name, bitInputStream, writer, translatorContext);
        }
    }

    public abstract byte[] doDecode(BitInputStream s, FormatWriter writer) throws IOException;

    @Override
    public String toString() {
        return "AbstractOctetStringTranslator{" +
                "constraints=" + constraints +
                '}';
    }
}
