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
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractIntegerTranslator extends AbstractTranslator {
    protected Constraints constraints;
    protected HashMap<BigInteger, String> namedNumbers = new HashMap<>();

    public AbstractTranslator init(MainRegistry mainRegistry, ASN1Parser.IntegerTypeContext integerTypeContext, List<ASN1Parser.ConstraintContext> constraintContext) throws NotHandledCaseException {
        if (integerTypeContext.namedNumberList() != null) {
            for (ASN1Parser.NamedNumberContext namedNumberContext : integerTypeContext.namedNumberList().namedNumber()) {
                if (namedNumberContext.signedNumber() != null) {
                    namedNumbers.put(new BigInteger(namedNumberContext.signedNumber().getText()), namedNumberContext.IDENTIFIER().getText());
                } else {
                    throw new NotHandledCaseException();
                }
            }
        }
        if (constraintContext.size() > 1) {
            throw new NotHandledCaseException("sizeConstraint " + constraintContext.toString());
        } else if (constraintContext.size() == 0) {
            return this;
        }
        if (constraintContext.get(0) != null) {
            constraints = new Constraints(mainRegistry);
            constraints.addConstraint(constraintContext.get(0));
            if (!constraints.hasValueRangeConstraint() || constraints.hasSizeConstraint()) {
                throw new RuntimeException();
            }
        }
        return this;
    }

    @Override
    public final void encode(String name, BitArray s, FormatReader reader, TranslatorContext translatorContext, List<String> parameters) throws Exception {
        BigInteger value = reader.intValue(name);
        if (null != translatorContext) {
            translatorContext.put(name, value.toString());
        }
        doEncode(s, value);
    }

    public abstract void doEncode(BitArray s, BigInteger value) throws IOException;

    @Override
    public final void decode(String name, BitInputStream s, FormatWriter writer, TranslatorContext translatorContext, List<String> parameters) throws NotHandledCaseException, IOException {
        BigInteger bigInteger = doDecode(s);
        if (null != translatorContext) {
            translatorContext.put(name, bigInteger.toString());
        }
        String namedNumber = namedNumbers.get(bigInteger);
        writer.intValue(name, bigInteger, namedNumber);
    }

    public abstract BigInteger doDecode(BitInputStream s) throws NotHandledCaseException, IOException;

    @Override
    public String toString() {
        return "AbstractIntegerTranslator{" +
                "constraints=" + constraints +
                '}';
    }
}