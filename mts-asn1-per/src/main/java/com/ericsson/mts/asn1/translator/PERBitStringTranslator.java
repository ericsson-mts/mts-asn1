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

public class PERBitStringTranslator extends AbstractBitStringTranslator {
    private PERTranscoder perTranscoder;

    public PERBitStringTranslator(PERTranscoder perTranscoder) {
        this.perTranscoder = perTranscoder;
    }

    @Override
    public void doEncode(BitArray s, FormatReader reader, String value) throws IOException {
        logger.trace("Enter {} encoder, name {}", this.getClass().getSimpleName(), this.name);
        BigInteger ub;
        BigInteger lb;
        boolean ubUnset = false;

        if (constraints.hasSingleValueConstraints()) {
            ub = lb = constraints.getSingleValueConstraint();
        } else if (constraints.hasSizeConstraint()) {
            ub = constraints.getUpperBound();
            lb = constraints.getLowerBound();
        } else {
            ub = null;
            lb = BigInteger.ZERO;
        }

        if (constraints.hasContentsConstraint()) {
            throw new NotHandledCaseException();
        }

        if (lb == null) {
            lb = BigInteger.ZERO;
        }
        if (ub == null) {
            ub = lb;
            ubUnset = true;
        }

        if (!namedBitList.isEmpty()) {
            //16.2, 16.3
            throw new NotHandledCaseException();
        }

        if (constraints.isExtensible()) {
            //16.6
            if (lb.compareTo(BigInteger.valueOf(value.length())) > 0 || ub.compareTo(BigInteger.valueOf(value.length())) < 0) {
                throw new NotHandledCaseException();
            } else {
                s.writeBit(0);
            }
        }

        value = value.replaceAll("[\\t\\n\\r ]", "");

        if (BigInteger.ZERO.equals(ub)) {
            throw new RuntimeException();
        } else if (lb.equals(ub) && BigInteger.valueOf(16).compareTo(ub) >= 0) {
            //16.9
            perTranscoder.encodeBitField(s, new BigInteger(value, 2), value.length());
        } else if (lb.equals(ub) && BigInteger.valueOf(65536).compareTo(ub) >= 0) {
            //16.10
            perTranscoder.skipAlignedBits(s);
            perTranscoder.encodeBitField(s, new BigInteger(value, 2), value.length());
        } else {
            //16.11
            if (!ubUnset && ub.compareTo(BigInteger.valueOf(65536)) <= 0) {
                perTranscoder.encodeConstrainedWholeNumber(s, BigInteger.valueOf(value.length()), lb, ub);
            } else if (ubUnset) {
                perTranscoder.encodeSemiConstrainedWholeNumber(s, lb, BigInteger.valueOf(value.length()));
            } else {
                throw new RuntimeException();
            }

            perTranscoder.skipAlignedBits(s);
            perTranscoder.encodeBitField(s, new BigInteger(value, 2), value.length());
            logger.trace("Encode value={} , length={}", value, value.length());
        }
    }

    @Override
    public String doDecode(BitInputStream s, FormatWriter writer) throws NotHandledCaseException, IOException {
        logger.trace("Enter {} translator, name {}", this.getClass().getSimpleName(), this.name);
        BigInteger ub;
        BigInteger lb;
        boolean isExtendedBitString = false;
        boolean ubUnset = false;


        if (constraints.hasSingleValueConstraints()) {
            ub = lb = constraints.getSingleValueConstraint();
        } else if (constraints.hasSizeConstraint()) {
            ub = constraints.getUpperBound();
            lb = constraints.getLowerBound();
        } else {
            ub = null;
            lb = BigInteger.ZERO;
        }

        if (constraints.hasContentsConstraint()) {
            throw new NotHandledCaseException();
        }

        if (lb == null) {
            lb = BigInteger.ZERO;
        }
        if (ub == null) {
            ub = lb;
            ubUnset = true;
        }

        if (!namedBitList.isEmpty()) {
            //16.2, 16.3
            throw new NotHandledCaseException();
        }

        if (constraints.isExtensible()) {
            //16.6
            isExtendedBitString = (1 == s.readBit());
        }

        if (isExtendedBitString) {
            //16.6
            BigInteger length = perTranscoder.decodeSemiConstraintNumber(lb.intValue(), s);
            throw new NotHandledCaseException(length.toString());
        } else {
            if (BigInteger.ZERO.equals(ub)) {
                throw new RuntimeException();
            } else if (lb.equals(ub) && BigInteger.valueOf(16).compareTo(ub) >= 0) {
                //16.9
                return perTranscoder.readsBitsAsString(s, ub.intValue());
            } else if (lb.equals(ub) && BigInteger.valueOf(65536).compareTo(ub) >= 0) {
                //16.10
                perTranscoder.skipAlignedBits(s);
                return perTranscoder.readsBitsAsString(s, ub.intValue());
            } else {
                //16.11
                BigInteger length;
                if (!ubUnset && ub.compareTo(BigInteger.valueOf(65536)) <= 0) {
                    length = perTranscoder.decodeConstrainedNumber(lb, ub, s);
                } else if (ubUnset) {
                    length = perTranscoder.decodeSemiConstraintNumber(lb.intValue(), s);
                } else {
                    throw new RuntimeException();
                }

                perTranscoder.skipAlignedBits(s);
                String value = perTranscoder.readsBitsAsString(s, length.intValue());
                logger.trace("Decode value={} , length={}", value, length);
                return value;
            }
        }
    }
}
