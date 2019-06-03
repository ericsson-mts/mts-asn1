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
import com.ericsson.mts.asn1.translator.AbstractEnumeratedTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;

public class PEREnumeratedTranslator extends AbstractEnumeratedTranslator {
    private PERTranscoder perTranscoder;
    private Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());

    public PEREnumeratedTranslator(PERTranscoder perTranscoder) {
        this.perTranscoder = perTranscoder;
    }

    @Override
    public void doEncode(BitArray s, FormatReader reader, String value) throws IOException {
        logger.trace("Enter {} decoder, name {}", this.getClass().getSimpleName(), this.name);
        if (hasExtensionMarker && addtionnalfieldList.indexOf(value) != -1) {
//            perTranscoder.encodeNormallySmallWholeNumber(s, BigInteger.valueOf(addtionnalfieldList.indexOf(value)));
            throw new NotHandledCaseException();
        } else {
            if (hasExtensionMarker) {
                s.writeBit(0);
            }
            perTranscoder.encodeConstrainedWholeNumber(s, BigInteger.valueOf(fieldList.indexOf(value)), BigInteger.ZERO, BigInteger.valueOf(fieldList.size() - 1));
        }
    }

    @Override
    public String doDecode(BitInputStream s, FormatWriter writer) throws IOException {
        logger.trace("Enter {} translator, name {}", this.getClass().getSimpleName(), this.name);

        // read the extension bit and returns the extension status, if grammar indicates it can be extended
        boolean isExtendedValue = false;
        isExtendedValue = hasExtensionMarker && (1 == s.readBit());

        String enumValue = null;

        if (!isExtendedValue) {
            enumValue = fieldList.get(perTranscoder.decodeConstrainedNumber(BigInteger.ZERO, BigInteger.valueOf(fieldList.size() - 1), s).intValueExact());
        } else {
            throw new RuntimeException(); //"probably not working"
            //enumValue = extensionMapping.get(decoder.decodeConstrainedNumber(BigInteger.ZERO, BigInteger.valueOf(extensionMapping.size() - 1), bs).intValueExact());
        }
        return enumValue;
    }
}
