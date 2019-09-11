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

import com.ericsson.mts.asn1.BitArray;
import com.ericsson.mts.asn1.BitInputStream;
import com.ericsson.mts.asn1.PERTranscoder;
import com.ericsson.mts.asn1.exception.NotHandledCaseException;
import com.ericsson.mts.asn1.exception.UnknownIdentifierException;
import com.ericsson.mts.asn1.factory.FormatReader;
import com.ericsson.mts.asn1.factory.FormatWriter;
import org.javatuples.Pair;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;

public class PERChoiceTranslator extends AbstractChoiceTranslator {
    private PERTranscoder perTranscoder;

    public PERChoiceTranslator(PERTranscoder perTranscoder) {
        this.perTranscoder = perTranscoder;
    }

    @Override
    public void doEncode(BitArray s, FormatReader reader, String choiceValue) throws Exception {
        logger.trace("Enter {} encoder, name {}", this.getClass().getSimpleName(), this.name);
        int index;
        AbstractTranslator abstractTranslator;

        if (fieldList.size() == 1) {
            if (fieldList.get(0).getValue0().equals(choiceValue)) {
                fieldList.get(0).getValue1().encode(choiceValue, s, reader, null);
                return;
            } else {
                throw new UnknownIdentifierException(choiceValue + " isn't part of translator " + this.name);
            }
        }

        for (Pair<String, AbstractTranslator> pair : fieldList) {
            if (choiceValue.equals(pair.getValue0())) {
                abstractTranslator = pair.getValue1();
                index = fieldList.indexOf(pair);
                if (optionalExtensionMarker) {
                    s.writeBit(0);
                }
                perTranscoder.encodeConstrainedWholeNumber(s, BigInteger.valueOf(index), BigInteger.ZERO, BigInteger.valueOf(fieldList.size() - 1L));
                abstractTranslator.encode(choiceValue, s, reader, null);
                return;
            }
        }
        //Encode optional extension bit 1 also !
        throw new NotHandledCaseException("In " + this.name + ", need length to encode additional extension choice " + choiceValue);
    }

    @Override
    public void doDecode(BitInputStream s, FormatWriter writer) throws Exception {
        logger.trace("Enter {} translator, name {}", this.getClass().getSimpleName(), this.name);
        boolean choiceWithinAdditionalValues = false;
        if (optionalExtensionMarker) {
            choiceWithinAdditionalValues = (1 == s.readBit());
        }

        BigInteger index;
        if (!choiceWithinAdditionalValues) {
            if (fieldList.size() < 64) {
                index = perTranscoder.decodeConstrainedNumber(BigInteger.ZERO, BigInteger.valueOf(fieldList.size() - 1L), s);
            } else {
                index = perTranscoder.decodeNormallySmallNumber(s);
            }
            fieldList.get(index.intValue()).getValue1().decode(fieldList.get(index.intValue()).getValue0(), s, writer, null);
        } else {
            index = perTranscoder.decodeNormallySmallNumber(s);
            byte[] choiceData = new byte[perTranscoder.decodeLengthDeterminant(s)];
            if (-1 == s.read(choiceData)) {
                throw new RuntimeException();
            }

            extensionFieldList.get(index.intValue() - fieldList.size() - 1).getValue1().decode(extensionFieldList.get(index.intValue() - fieldList.size() - 1).getValue0(), new BitInputStream(new ByteArrayInputStream(choiceData)), writer, null);
        }
    }
}
