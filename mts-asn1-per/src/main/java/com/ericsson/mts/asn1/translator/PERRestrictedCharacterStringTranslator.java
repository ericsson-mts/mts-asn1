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
import java.nio.charset.StandardCharsets;

public class PERRestrictedCharacterStringTranslator extends AbstractRestrictedCharacterStringTranslator {
    private PERTranscoder perTranscoder;

    public PERRestrictedCharacterStringTranslator(PERTranscoder perTranscoder) {
        this.perTranscoder = perTranscoder;
    }

    @Override
    public void doEncode(BitArray s, FormatReader reader, String value) throws IOException {
        logger.trace("Enter {} encoder, name {}", this.getClass().getSimpleName(), this.name);
        BigInteger lb, ub,
                bitLength = BigInteger.valueOf((long) (value.length() * knownMultiplierCharacterString.getB2()));
        boolean ubUnset = false;

        if (constraints.hasSizeConstraint()) {
            lb = constraints.getLower_bound();
            ub = constraints.getUpper_bound();
        } else {
            throw new NotHandledCaseException();
        }

        if (constraints.isSizeConstraintExtensible()) {
            //16.6
            if (bitLength.compareTo(lb) < 0 || bitLength.compareTo(ub) > 0) {
                throw new NotHandledCaseException();
            } else {
                perTranscoder.writeBit(s, 0);
            }
        }

        if (!isknownMultiplierCharacterStringType) {
            //30.1
            lb = BigInteger.ZERO;
            ubUnset = true;
        }

        if (lb == null) {
            //30.3
            lb = BigInteger.ZERO;
        }

        if (ub == null) {
            //30.2
            ub = lb;
            ubUnset = true;
        }
        if (isknownMultiplierCharacterStringType) {
            if (KnownMultiplierCharacterString.PrintableString.equals(knownMultiplierCharacterString)) {
                if (!ubUnset && lb.equals(ub) && lb.compareTo(BigInteger.valueOf(65536)) < 0) {

                } else if (!ubUnset && ub.compareTo(BigInteger.valueOf(65536)) <= 0) {
                    perTranscoder.encodeConstrainedWholeNumber(s, BigInteger.valueOf(perTranscoder.toByteCount(bitLength.intValueExact())), lb, ub);
                } else if (ubUnset) {
                    perTranscoder.encodeSemiConstrainedWholeNumber(s, lb, BigInteger.valueOf(perTranscoder.toByteCount(bitLength.intValueExact())));
                } else {
                    throw new RuntimeException();
                }
                if (!((!ubUnset && lb.equals(ub)) || (!lb.equals(ub) && ub.subtract(lb).compareTo(BigInteger.valueOf(2)) <= 0))) {
                    //Note 1
                    perTranscoder.skipAlignedBits(s);
                }
                perTranscoder.encodeBitField(s, new BigInteger(value.getBytes(StandardCharsets.US_ASCII)), bitLength.intValueExact());
            } else {
                throw new NotHandledCaseException();
            }
        } else {
            throw new NotHandledCaseException();
        }
    }

    @Override
    public String doDecode(BitInputStream s, FormatWriter writer) throws IOException {
        logger.trace("Enter {} translator, name {}", this.getClass().getSimpleName(), this.name);
        BigInteger lb, ub;
        boolean ubUnset = false, isExtendedRestrictedString = false;

        if (constraints.hasSizeConstraint()) {
            lb = constraints.getLower_bound();
            ub = constraints.getUpper_bound();
        } else {
            throw new NotHandledCaseException();
        }

        if (!isknownMultiplierCharacterStringType) {
            //30.1
            lb = BigInteger.ZERO;
            ubUnset = true;
        }

        if (lb == null) {
            //30.3
            lb = BigInteger.ZERO;
        }

        if (ub == null) {
            //30.2
            ub = lb;
            ubUnset = true;
        }

        if (constraints.isSizeConstraintExtensible()) {
            //16.6
            isExtendedRestrictedString = (1 == s.readBit());
        }

        if (isExtendedRestrictedString) {
            throw new NotHandledCaseException();
        } else {
            if (isknownMultiplierCharacterStringType) {
                if (KnownMultiplierCharacterString.PrintableString.equals(knownMultiplierCharacterString)) {
                    BigInteger length;
                    if (!ubUnset && ub.compareTo(BigInteger.valueOf(65536)) <= 0) {
                        length = perTranscoder.decodeConstrainedNumber(lb, ub, s);
                    } else if (ubUnset) {
                        length = perTranscoder.decodeSemiConstraintNumber(lb.intValue(), s);
                    } else {
                        throw new RuntimeException();
                    }
                    s.skipUnreadedBits();
                    byte[] result = perTranscoder.readBits(s, length.intValue() * 8);
                    logger.trace("Result " + new String(result, StandardCharsets.UTF_8));
                    return new String(result, StandardCharsets.UTF_8);
                } else {
                    throw new NotHandledCaseException();
                }
            }
        }
        throw new RuntimeException();
    }
}
