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
import com.ericsson.mts.asn1.factory.FormatReader;
import com.ericsson.mts.asn1.factory.FormatWriter;

import java.io.IOException;
import java.math.BigInteger;

public class PEREnumeratedTranslator extends AbstractEnumeratedTranslator {

    private PERTranscoder perTranscoder;

    public PEREnumeratedTranslator(PERTranscoder perTranscoder) {
        this.perTranscoder = perTranscoder;
    }

    @Override
    public void doEncode(BitArray s, FormatReader reader, String value) throws IOException {
        logger.trace("Encode {}", this);
        if (hasExtensionMarker && additionalFieldsList.indexOf(value) != -1) {
            throw new NotHandledCaseException(value);
        } else {
            if (hasExtensionMarker) {
                s.writeBit(0);
            }
            perTranscoder.encodeConstrainedWholeNumber(s, BigInteger.valueOf(fieldList.indexOf(value)), BigInteger.ZERO, BigInteger.valueOf(fieldList.size() - 1L));
        }
    }

    @Override
    public String doDecode(BitInputStream s, FormatWriter writer) throws IOException {
        logger.trace("{} : {}", this.name, this);
        // read the extension bit and returns the extension status, if grammar indicates it can be extended
        boolean isExtendedValue = hasExtensionMarker && (1 == s.readBit());
        logger.trace("{} is extended : {}", this.name, isExtendedValue);
        if (!isExtendedValue) {
            return fieldList.get(perTranscoder.decodeConstrainedNumber(BigInteger.ZERO, BigInteger.valueOf(fieldList.size() - 1L), s).intValueExact());
        } else {
            int choice = perTranscoder.decodeNormallySmallNumber(s).intValueExact();
            if (additionalFieldsList.size() > choice) {
                return additionalFieldsList.get(choice);
            } else {
                return "UNKNOWN_EXTENDED(" + choice + ")";
            }
        }
    }
}
