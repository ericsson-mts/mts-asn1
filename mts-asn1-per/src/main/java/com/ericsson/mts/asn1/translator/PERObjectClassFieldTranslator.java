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
import com.ericsson.mts.asn1.factory.FormatReader;
import com.ericsson.mts.asn1.factory.FormatWriter;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class PERObjectClassFieldTranslator extends AbstractObjectClassFieldTranslator {
    private static int OPEN_TYPE_TAG = 0;
    private PERTranscoder perTranscoder;

    public PERObjectClassFieldTranslator(PERTranscoder perTranscoder) {
        this.perTranscoder = perTranscoder;
    }

    @Override
    public void encode(String name, BitArray s, FormatReader reader, TranslatorContext translatorContext, List<String> parameters) throws Exception {
        Map<String, String> registry = getRegister(parameters);
        if (constraints.getTargetComponent() == null) {
            AbstractTranslator typeTranslator = classHandler.getTypeTranslator(fieldName);
            if (typeTranslator == null) {
                throw new RuntimeException("Unknown field " + fieldName + " in class " + classHandler.toString());
            }
            typeTranslator.encode(name, s, reader, translatorContext);
        } else {
            AbstractTranslator typeTranslator = classHandler.getTypeTranslator(fieldName);
            if (typeTranslator != null) {
                typeTranslator.encode(name, s, reader, translatorContext);
            } else {
                String uniqueKey = translatorContext.get(constraints.getTargetComponent());
                if (uniqueKey == null) {
                    throw new RuntimeException("Unique key not found in context for field " + fieldName + " target component " + constraints.getTargetComponent());
                }

                //OpenType
                BitArray bitArray = new BitArray();
                typeTranslator = classHandler.getTypeTranslator(fieldName, registry.get(constraints.getObjectSetName()), uniqueKey);
                if (typeTranslator == null) {
                    throw new RuntimeException("Unknown field " + fieldName + " in object with " + toString());
                }
                int tag = OPEN_TYPE_TAG;
                OPEN_TYPE_TAG++;
                logger.trace("Enter open type : tag=" + tag + " , name=" + name);
                typeTranslator.encode(name, bitArray, reader, translatorContext);
                logger.trace("Leave open type : tag=" + tag + " , name=" + name);
                logger.trace("Open type for field " + name + ": octet length=" + perTranscoder.toByteCount(bitArray.getLength().intValueExact()));
                perTranscoder.encodeLengthDeterminant(s, BigInteger.valueOf(perTranscoder.toByteCount((bitArray.getLength()).intValueExact())));
                perTranscoder.skipAlignedBits(bitArray);
                s.concatBitArray(bitArray);
            }
        }
    }

    @Override
    public void decode(String name, BitInputStream s, FormatWriter writer, TranslatorContext translatorContext, List<String> parameters) throws IOException {
        Map<String, String> registry = getRegister(parameters);

        if (constraints.getTargetComponent() == null) {
            AbstractTranslator typeTranslator = classHandler.getTypeTranslator(fieldName);
            if (typeTranslator == null) {
                throw new RuntimeException("Unknown field " + fieldName + " in class " + classHandler.toString());
            }
            typeTranslator.decode(name, s, writer, translatorContext);
        } else {
            AbstractTranslator typeTranslator = classHandler.getTypeTranslator(fieldName);
            if (typeTranslator != null) {
                typeTranslator.decode(name, s, writer, translatorContext);
            } else {
                String uniqueKey = translatorContext.get(constraints.getTargetComponent());
                if (uniqueKey == null) {
                    throw new RuntimeException("Unique key not found in context for field " + fieldName + " target component " + constraints.getTargetComponent());
                }

                //OpenType
                typeTranslator = classHandler.getTypeTranslator(fieldName, registry.get(constraints.getObjectSetName()), uniqueKey);
                if (typeTranslator == null) {
                    throw new RuntimeException("Unknown field " + fieldName + " in object with " + toString());
                }

                int n = perTranscoder.decodeLengthDeterminant(s);
                typeTranslator.decode(name, s, writer, translatorContext);
            }
        }
    }
}
