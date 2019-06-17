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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PERSequenceOfTranslator extends AbstractSequenceOfTranslator {
    private PERTranscoder perTranscoder;

    public PERSequenceOfTranslator(PERTranscoder perTranscoder) {
        this.perTranscoder = perTranscoder;
    }

    @Override
    public void doEncode(BitArray s, FormatReader reader, int numberOfComponents, Map<String, String> registry) throws Exception {
        logger.trace("Enter {} encoder, name {}", this.getClass().getSimpleName(), this.name);
        BigInteger ub, lb;
        if (!constraints.hasSizeConstraint()) {
            ub = null;
            lb = BigInteger.ZERO;
        } else {

            constraints.updateSizeConstraint(registry);
            ub = constraints.getUpper_bound();
            lb = constraints.getLower_bound();
            if (lb == null) {
                lb = BigInteger.ZERO;
            }
        }

        if (constraints.hasSizeConstraint() && constraints.isSizeConstraintExtensible()) {
            // X.691 : clause 20.4
            throw new NotHandledCaseException();
        } else if (lb.equals(ub) && ub.compareTo(BigInteger.valueOf(65536)) < 0) {
            // X.691 : clause 20.5
            launchEncode(s, reader, registry, ub);
        } else {
            // X.691 : clause 20.6
            if (ub != null && ub.compareTo(BigInteger.valueOf(65536)) < 0) {
                perTranscoder.encodeConstrainedWholeNumber(s, BigInteger.valueOf(numberOfComponents), lb, ub);
            } else {
                perTranscoder.encodeSemiConstrainedWholeNumber(s, lb, BigInteger.valueOf(numberOfComponents));
            }
            launchEncode(s, reader, registry, BigInteger.valueOf(numberOfComponents));
        }
    }

    private void launchEncode(BitArray s, FormatReader reader, Map<String, String> registry, BigInteger numberOfComponents) throws Exception {
        List<String> parameters = typeTranslator.getParameters();
        List<String> inputParameters = new ArrayList<>();
        if (parameters.isEmpty()) {
            for (int i = 0; i < numberOfComponents.intValueExact(); i++) {

                typeTranslator.encode(null, s, reader, null);
            }
        } else {
            for (String parameter : actualParameters) {
                inputParameters.add(registry.get(parameter));
            }
            for (int i = 0; i < numberOfComponents.intValueExact(); i++) {
                typeTranslator.encode(null, s, reader, null, inputParameters);
            }
        }
    }

    @Override
    public void doDecode(BitInputStream s, FormatWriter writer, Map<String, String> registry) throws Exception {
        logger.trace("Enter {} translator, name {}", this.getClass().getSimpleName(), this.name);
        BigInteger ub, lb;

        if (!constraints.hasSizeConstraint()) {
            ub = null;
            lb = BigInteger.ZERO;
        } else {
            constraints.updateSizeConstraint(registry);
            ub = constraints.getUpper_bound();
            lb = constraints.getLower_bound();
            if (lb == null) {
                lb = BigInteger.ZERO;
            }
        }

        if (constraints.hasSizeConstraint() && constraints.isSizeConstraintExtensible()) {
            /* X.691 : clause 20.4
            If there is a PER-visible constraint and an extension marker is present in it, a single bit shall be added
            to the field-list in a bit-field of length one. The bit shall be set to 1 if the number of components in
            this encoding is not within the range of the extension root, and zero otherwise. In the former case 11.9
            shall be invoked to add the length determinant as a semi-constrained whole number to the field-list,
            followed by the component values. In the latter case the length and value shall be encoded as if the
            extension marker is not present.
             */
            throw new NotHandledCaseException();
        } else if (lb.equals(ub) && ub.compareTo(BigInteger.valueOf(65536)) < 0) {
            /* X.691 : clause 20.5
            If the number of components is fixed ("ub" equals "lb") and "ub" is less than 64K, then there shall be no
            length determinant for the sequence-of, and the fields of each component shall be appended in turn to the
            field-list of the sequence-of.
             */
            launchDecode(s, writer, registry, ub);
        } else {
             /* X.691 : clause 20.6
            Otherwise, the procedures of 11.9 shall be invoked to add the list of fields generated by the "n" components
             to the field-list, preceded by a length determinant equal to "n" components as a constrained whole number
             if "ub" is set, and as a semi-constrained whole number if "ub" is unset. "lb" is as determined above.
             */

            BigInteger n;
            if (ub != null && BigInteger.valueOf(65536).compareTo(ub) >= 0) {
                n = perTranscoder.decodeConstrainedNumber(lb, ub, s);
            } else {
                n = BigInteger.valueOf(perTranscoder.decodeLengthDeterminant(s));
            }
            launchDecode(s, writer, registry, n);
        }
    }

    private void launchDecode(BitInputStream s, FormatWriter writer, Map<String, String> registry, BigInteger ub) throws Exception {
        List<String> parameters = typeTranslator.getParameters();
        List<String> inputParameters = new ArrayList<>();
        if (parameters.isEmpty()) {
            for (int i = 0; i < ub.intValue(); i++) {
                typeTranslator.decode(null, s, writer, null);
            }
        } else {
            for (String parameter : actualParameters) {
                inputParameters.add(registry.get(parameter));
            }
            for (int i = 0; i < ub.intValue(); i++) {
                typeTranslator.decode(null, s, writer, null, inputParameters);
            }
        }
    }
}