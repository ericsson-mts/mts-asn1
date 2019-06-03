/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.ericsson.mts.asn1.decoder;

import com.ericsson.mts.asn1.BitArray;
import com.ericsson.mts.asn1.BitInputStream;
import com.ericsson.mts.asn1.PERTranscoder;
import com.ericsson.mts.asn1.exception.NotHandledCaseException;
import com.ericsson.mts.asn1.factory.FormatReader;
import com.ericsson.mts.asn1.factory.FormatWriter;
import com.ericsson.mts.asn1.translator.AbstractChoiceTranslator;
import com.ericsson.mts.asn1.translator.AbstractTranslator;
import org.javatuples.Pair;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;

public class PERChoiceTranslator extends AbstractChoiceTranslator {
    private PERTranscoder perTranscoder;

    public PERChoiceTranslator(PERTranscoder perTranscoder) {
        this.perTranscoder = perTranscoder;
    }

    @Override
    public void doEncode(BitArray s, FormatReader reader, String choiceValue) throws Exception {
        logger.trace("Enter {} encoder, name {}", this.getClass().getSimpleName(), this.name);
        int index = -1;
        AbstractTranslator abstractTranslator = null;

        if (fieldList.size() == 1) {
            if (fieldList.get(0).getValue0().equals(choiceValue)) {
                fieldList.get(0).getValue1().encode(choiceValue, s, reader, null);
                return;
            } else {
                throw new RuntimeException("Wrong choice value : " + choiceValue);
            }
        }

        for (Pair<String, AbstractTranslator> pair : fieldList) {
            if (choiceValue.equals(pair.getValue0())) {
                abstractTranslator = pair.getValue1();
                index = fieldList.indexOf(pair);
                if (optionalExtensionMarker) {
                    s.writeBit(0);
                }
                perTranscoder.encodeConstrainedWholeNumber(s, BigInteger.valueOf(index), BigInteger.ZERO, BigInteger.valueOf(fieldList.size() - 1));
                abstractTranslator.encode(choiceValue, s, reader, null);
                return;
            }
        }
        //Encode optional extension bit 1 also !
        throw new RuntimeException("Need length to encode additional extension");
    }

    @Override
    public void doDecode(BitInputStream s, FormatWriter writer) throws NotHandledCaseException, IOException {
        logger.trace("Enter {} translator, name {}", this.getClass().getSimpleName(), this.name);
        boolean choiceWithinAdditionalValues = false;
        if (optionalExtensionMarker) {
            choiceWithinAdditionalValues = (1 == s.readBit());
        }

        BigInteger index = null;
        if (!choiceWithinAdditionalValues) {
            if (fieldList.size() < 64) {
                index = perTranscoder.decodeConstrainedNumber(BigInteger.ZERO, BigInteger.valueOf(fieldList.size() - 1), s);
            } else {
                try {
                    index = perTranscoder.decodeNormallySmallNumber(s);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            fieldList.get(index.intValue()).getValue1().decode(fieldList.get(index.intValue()).getValue0(), s, writer, null);
        } else {
            try {
                index = perTranscoder.decodeNormallySmallNumber(s);
            } catch (Exception e) {
                e.printStackTrace();
            }
            byte[] choiceData = new byte[perTranscoder.decodeLengthDeterminant(s)];
            s.read(choiceData);

            extensionFieldList.get(index.intValue() - fieldList.size() - 1).getValue1().decode(extensionFieldList.get(index.intValue() - fieldList.size() - 1).getValue0(), new BitInputStream(new ByteArrayInputStream(choiceData)), writer, null);
        }
    }
}
