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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PERSequenceTranslator extends AbstractSequenceTranslator {
    private PERTranscoder perTranscoder;

    public PERSequenceTranslator(PERTranscoder perTranscoder) {
        this.perTranscoder = perTranscoder;
    }

    @Override
    public void doEncode(BitArray s, FormatReader reader, TranslatorContext translatorContext, List<String> inputFieldList, Map<String, String> registry) throws Exception {
        logger.trace("Enter {} encoder, name {}", this.getClass().getSimpleName(), this.name);
        if (hasEllipsis || optionalExtensionMarker || (extensionAndException != -1)) {
            int isAdditionnalValuePresent = 0;
            for (Field field : additionnalFieldList) {
                if (inputFieldList.contains(field.getName())) {
                    isAdditionnalValuePresent = 1;
                    throw new NotHandledCaseException();
                }
            }
            s.writeBit(isAdditionnalValuePresent);
        }

        //Build preamble (bit-map)
        BigInteger preambleLength = BigInteger.ZERO;
        for (Field field : fieldList) {
            if (field.getOptionnal()) {
                preambleLength = preambleLength.add(BigInteger.ONE);
                if (inputFieldList.contains(field.getName())) {
                    s.writeBit(1);
                } else {
                    s.writeBit(0);
                }
            }
        }

        if (preambleLength.compareTo(BigInteger.valueOf(65536)) > 0) {
            throw new NotHandledCaseException("Preamble fragmentation");
        }

        for (Field field : fieldList) {
            if (inputFieldList.contains(field.getName())) {
                logger.trace("Encode field " + field.getName());
                AbstractTranslator typeTranslator = field.getType();
                List<String> parameters = typeTranslator.getParameters();
                if (parameters.isEmpty()) {
                    field.getType().encode(field.getName(), s, reader, translatorContext);
                } else {
                    //Building parameter list to pass to target translator
                    List<String> inputParameters = new ArrayList<>();
                    for (String parameter : field.getParameters()) {
                        inputParameters.add(registry.get(parameter));
                    }
                    typeTranslator.encode(field.getName(), s, reader, translatorContext, inputParameters);
                }
            } else {
                if (!field.getOptionnal()) {
                    throw new RuntimeException("Sequence " + name + " need field " + field.getName());
                }
            }
        }
    }

    @Override
    public void doDecode(BitInputStream s, FormatWriter writer, TranslatorContext translatorContext, Map<String, String> registry) throws Exception {
        logger.trace("Enter {} translator, name {}", this.getClass().getSimpleName(), this.name);
        boolean rootSequenceHasOptional = false;
        boolean isExtendedSequence = false;

        if (hasEllipsis || optionalExtensionMarker || (extensionAndException != -1)) {
            isExtendedSequence = (1 == s.readBit());
        }

        for (Field field : fieldList) {
            if (field.getOptionnal()) {
                rootSequenceHasOptional = true;
                break;
            }
        }

        if (rootSequenceHasOptional) {
            for (int i = 0; i < optionalBitmap.length; i++) {
                optionalBitmap[i] = (1 == s.readBit());
            }
        }

        int optionalBitmapIndex = 0;

        for (Field field : fieldList) {
            if (!field.getOptionnal() || (field.getOptionnal() && optionalBitmap[optionalBitmapIndex++])) {
                logger.trace("Decode field {} ", field.getName());
                AbstractTranslator typeTranslator = field.getType();
                List<String> parameters = typeTranslator.getParameters();
                if (parameters.isEmpty()) {
                    typeTranslator.decode(field.getName(), s, writer, translatorContext);
                } else {
                    //Building parameter list to pass to target translator
                    List<String> inputParameters = new ArrayList<>();
                    for (String parameter : field.getParameters()) {
                        inputParameters.add(registry.get(parameter));
                    }
                    typeTranslator.decode(field.getName(), s, writer, translatorContext, inputParameters);
                }
            }
        }

        if (isExtendedSequence) {
            logger.trace("isExtendedSequence");
            boolean[] additionalBitmap = new boolean[perTranscoder.decodeNormallySmallNumber(s).intValue()];

            logger.trace("additionalBitmap is {}", additionalBitmap);

            for (int i = 0; i < additionalBitmap.length; i++) {
                additionalBitmap[i] = (1 == s.readBit());
            }

            // ALIGNED ONLY
            perTranscoder.skipAlignedBits(s);

            for (boolean additionalbit : additionalBitmap) {
                if (additionalbit) {
                    int len = perTranscoder.decodeLengthDeterminant(s);
                    byte[] data = new byte[len];
                    if (-1 == s.read(data)) {
                        throw new RuntimeException();
                    }
                    throw new RuntimeException("unsupported");
                }
            }
        }
    }
}
