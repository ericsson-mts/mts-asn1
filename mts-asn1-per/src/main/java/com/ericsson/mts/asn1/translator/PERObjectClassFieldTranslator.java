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
import com.ericsson.mts.asn1.TranslatorContext;
import com.ericsson.mts.asn1.exception.NotHandledCaseException;
import com.ericsson.mts.asn1.factory.FormatReader;
import com.ericsson.mts.asn1.factory.FormatWriter;

import java.math.BigInteger;
import java.util.Map;

public class PERObjectClassFieldTranslator extends AbstractObjectClassFieldTranslator {
    //Use for debug
    private static int openTypeTag = 0;
    private PERTranscoder perTranscoder;

    public PERObjectClassFieldTranslator(PERTranscoder perTranscoder) {
        this.perTranscoder = perTranscoder;
    }

    @Override
    public void doEncode(String name, BitArray s, FormatReader reader, TranslatorContext translatorContext, Map<String, String> registry) throws Exception {

        if (constraints.hasSingleValueConstraints()) {
            throw new NotHandledCaseException();
        }

        if (constraints.getTargetComponent() == null) {
            AbstractTranslator typeTranslator = classHandler.getTypeTranslator(fieldName);
            typeTranslator.encode(name, s, reader, translatorContext);
        } else {
            AbstractTranslator typeTranslator = classHandler.getTypeTranslator(fieldName);
            if (typeTranslator != null) {
                typeTranslator.encode(name, s, reader, translatorContext);
            } else {
                String uniqueKey = translatorContext.get(constraints.getTargetComponent());
                if (uniqueKey == null) {
                    throw new NullPointerException("Unique key not found in context for field " + fieldName + " target component " + constraints.getTargetComponent());
                }

                //OpenType
                BitArray bitArray = new BitArray();
                typeTranslator = classHandler.getTypeTranslator(fieldName, registry.get(constraints.getObjectSetName()), uniqueKey);
                if (typeTranslator == null) {
                    throw new RuntimeException("Unknown field " + fieldName + " in object with " + toString());
                }
                int tag = openTypeTag;
                openTypeTag++;
                logger.trace("Enter open type : tag={} , name={}", tag, this.name);

                reader.enterObject(name);
                typeTranslator.encode(typeTranslator.getName(), bitArray, reader, translatorContext);
                logger.trace("Leave open type : tag={} , name={}", tag, name);
                logger.trace("Open type for field {} : octet length={}", name, perTranscoder.toByteCount(bitArray.getLength().intValueExact()));
                perTranscoder.encodeLengthDeterminant(s, BigInteger.valueOf(perTranscoder.toByteCount((bitArray.getLength()).intValueExact())));
                perTranscoder.skipAlignedBits(bitArray);
                s.concatBitArray(bitArray);
                reader.leaveObject(name);
            }
        }
    }

    @Override
    public void doDecode(String name, BitInputStream s, FormatWriter writer, TranslatorContext translatorContext, Map<String, String> registry) throws Exception {

        if (constraints.hasSingleValueConstraints()) {
            throw new NotHandledCaseException();
        }

        if (constraints.getTargetComponent() == null) {
            AbstractTranslator typeTranslator = classHandler.getTypeTranslator(fieldName);
            typeTranslator.decode(name, s, writer, translatorContext);
        } else {
            AbstractTranslator typeTranslator = classHandler.getTypeTranslator(fieldName);
            if (typeTranslator != null) {
                typeTranslator.decode(name, s, writer, translatorContext);
            } else {
                String uniqueKey = translatorContext.get(constraints.getTargetComponent());
                if (uniqueKey == null) {
                    throw new NullPointerException("Unique key not found in context for field " + fieldName + " target component " + constraints.getTargetComponent());
                }

                //OpenType
                typeTranslator = classHandler.getTypeTranslator(fieldName, registry.get(constraints.getObjectSetName()), uniqueKey);
                if (perTranscoder.decodeLengthDeterminant(s) >= 16384) {
                    throw new NotHandledCaseException("Open type fragmentation");
                }
                writer.enterObject(name);
                typeTranslator.decode(typeTranslator.getName(), s, writer, translatorContext);
                writer.leaveObject(name);
            }
        }
    }
}
