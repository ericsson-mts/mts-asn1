/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.ericsson.mts.asn1;

import com.ericsson.mts.asn1.exception.EncodingException;
import com.ericsson.mts.asn1.exception.InvalidParameterException;
import com.ericsson.mts.asn1.exception.NotHandledCaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

public class PERTranscoder {

    private final boolean aligned;
    private Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());

    public PERTranscoder(boolean aligned) {
        this.aligned = aligned;
    }

    public boolean isAligned() {
        return aligned;
    }

    /**
     * Decode of the constrained whole number ITU-T X.691. 10.5. NOTE (Tutorial)
     * This subclause is referenced by other clauses, and itself references
     * earlier clauses for the production of a nonnegative-data-integer or a
     * 2's-complement-data-integer encoding.
     *
     * @param lb lower bound
     * @param ub upper bound
     * @param stream binary stream
     * @return number decoded
     * @throws IOException Input exception
     */
    public BigInteger decodeConstrainedNumber(BigInteger lb, BigInteger ub, BitInputStream stream) throws IOException {
        logger.trace("decodeConstrainedNumber min=" + lb + ", max=" + ub);
        BigInteger n;
        BigInteger max = ub.subtract(lb);
        BigInteger range = max.add(ONE);

        if (lb.compareTo(ub) > 0 || range.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("illegal ranges lb=" + lb + ", ub=" + ub);
        }

        if (range.compareTo(BigInteger.valueOf(256)) < 0) {
            /*
             * 1. Where the range is less than or equal to 255, the value encodes
             * into a bit-field of the minimum size for the range.
             */
            n = stream.bigReadBits(max.bitLength()).add(lb);
        } else if (range.compareTo(BigInteger.valueOf(256)) == 0) {
            /*
             * 2. Where the range is exactly 256, the value encodes
             * into a single octet octet-aligned bit-field.
             */
            if (aligned) {
                skipAlignedBits(stream);
            }

            n = stream.bigReadBits(8).add(lb);
        } else if (range.compareTo(BigInteger.valueOf(65536)) <= 0) {
            logger.trace("257 < range < 64K, decode on two bytes");

            /*
             * 3. Where the range is 257 to 64K, the value encodes into
             * a two octet octet-aligned bit-field.
             */
            if (aligned) {
                skipAlignedBits(stream);
            }
            n = stream.bigReadBits(16).add(lb);
        } else {
            /*
             * 4. Where the range is greater than 64K, the range is ignored
             * and the value encodes into an  octet-aligned bit-field
             * which is the minimum number of octets for the value.
             * In this latter case, later procedures (see 10.9)
             * also encode a length field (usually a single octet) to indicate
             * the length of the encoding. For the other cases, the length
             * of the encoding is independent of the value being encoded,
             * and is not explicitly encoded.
             */
            logger.trace("decodeConstrainedNumber (range " + max + ") encoded over " + max.bitLength() + " bits (" + toByteCount(max.bitLength()) + " bytes)");
            BigInteger intLen = decodeConstrainedLengthDeterminant(
                    BigInteger.ONE,
                    BigInteger.valueOf(toByteCount(max.bitLength())),
                    stream
            );
            if (aligned) {
                skipAlignedBits(stream);
            }
            n = decodeUnsignedIntegerValueAsBytes(intLen.intValueExact(), stream).add(lb);
        }
        logger.trace("decodeConstrainedNumber result=" + n);

        return n;
    }

    public int toByteCount(int bitCount) {
        int byteCount = 0;
        while (bitCount > 0) {
            byteCount++;
            bitCount -= 8;
        }
        return (byteCount == 0) ? 1 : byteCount;
    }

    /**
     * Decode the constraint length determinant. ITU-T X.691. 10.9. General
     * rules for encoding a length determinant
     *
     * @param min lower bound
     * @param max upper bound
     * @param stream binary stream
     * @return length decoded
     * @throws IOException Input exception
     */
    private BigInteger decodeConstrainedLengthDeterminant(BigInteger min, BigInteger max, BitInputStream stream) throws IOException {
        logger.trace("decodeConstrainedLengthDeterminant : " + min + " - " + max);

        BigInteger len;
        if (max.compareTo(BigInteger.valueOf(0xFFFF)) <= 0) {
            // 10.9. NOTE 2  (Tutorial) In the case of the ALIGNED variant
            // if the length count is bounded above by an upper bound that is
            // less than 64K, then the constrained whole number encoding
            // is used for the length.
            int nbBits = max.subtract(min).bitLength();
            logger.trace("decodeConstrainedLengthDeterminant : nbBits=" + nbBits);

            len = decodeConstrainedNumber(min, max, stream);//stream.bigReadBits(max.subtract(min).bitLength()).add(min);
        } else {
            len = BigInteger.valueOf(decodeLengthDeterminant(stream));
        }
        logger.trace("decodeConstrainedLengthDeterminant : " + len);
        return len;
    }

    /**
     * Decode the normally small number ITU-T X.691. 10.6 NOTE (Tutorial) This
     * procedure is used when encoding a non-negative whole number that is
     * expected to be small, but whose size is potentially unlimited due to the
     * presence of an extension marker. An example is a choice index.
     *
     * @param stream binary stream
     * @return normally small number
     * @throws IOException input exception
     */
    public BigInteger decodeNormallySmallNumber(BitInputStream stream) throws IOException {
        BigInteger result;
        int bitIndicator = stream.readBit();
        if (bitIndicator == 0) {
            /* 10.6.1 If the non-negative whole number, "n", is less than
             * or equal to 63, then a single-bit bit-field shall be appended
             * to the field-list with the bit set to 0, and "n" shall be
             * encoded as a non-negative-data-integer into a 6-bit bit-field.
             */
            result = BigInteger.valueOf(stream.readBits(6));
        } else {
            /* If "n" is greater than or equal to 64, a single-bit
             * bit-field with the bit set to 1 shall be appended to the field-list.
             * The value "n" shall then be encoded as a semi-constrained
             * whole number with "lb" equal to 0 and the procedures of
             * 10.9 shall be invoked to add it to the field-list preceded
             * by a length determinant.
             */
            result = decodeSemiConstraintNumber(0, stream);
        }
        return result;
    }

    /**
     * Decode the semi-constrained whole number ITU-T X.691. 10.7. NOTE
     * (Tutorial) This procedure is used when a lower bound can be identified
     * but not an upper bound. The encoding procedure places the offset from the
     * lower bound into the minimum number of octets as a
     * non-negative-data-integer, and requires an explicit length encoding
     * (typically a single octet) as specified in later procedures.
     *
     * @param lb lower bound
     * @param stream binary input
     * @return unsigned integer
     * @throws IOException input exception
     */
    public BigInteger decodeSemiConstraintNumber(int lb, BitInputStream stream) throws IOException {
        int intLen = decodeLengthDeterminant(stream);
        if (aligned) {
            skipAlignedBits(stream);
        }
        return decodeUnsignedIntegerValueAsBytes(intLen, stream).add(BigInteger.valueOf(lb));
    }

    /**
     * Decode n bytes as a unsigned integer
     *
     * @param n the maximum number of bytes to read
     * @param stream binary stream
     * @return unsigned integer decoded
     * @throws IOException input exception
     */
    private BigInteger decodeUnsignedIntegerValueAsBytes(int n, InputStream stream) throws IOException {
        byte[] bytes = new byte[n + 1];
        if (-1 == stream.read(bytes, 1, n)) {
            throw new RuntimeException();
        }
        return new BigInteger(bytes);
    }

    /**
     * Decode n bytes as a signed integer
     *
     * @param n the maximum number of bytes to read
     * @param stream input stream
     * @return decoded integer
     * @throws IOException input exception
     */
    protected BigInteger decodeSignedIntegerValueAsBytes(int n, InputStream stream) throws IOException {
        byte[] bytes = new byte[n];
        if (-1 == stream.read(bytes)) {
            throw new RuntimeException();
        }
        return new BigInteger(bytes);
    }

    /**
     * Decode the length determinant ITU-T X.691. 10.9. General rules for
     * encoding a length determinant
     *
     * @param stream binary streaù
     * @return length determinant
     * @throws IOException input exception
     */
    public int decodeLengthDeterminant(BitInputStream stream) throws IOException {
        skipAlignedBits(stream);
        int result = stream.read();
        if ((result & 0b10000000) != 0b00000000) {
            if ((result & 0b11000000) == 0b10000000) {
                // NOTE 2. b) ("n" less than 16K) two octets
                // containing "n" with bit 8 of the first octet
                // set to 1 and bit 7 set to zero;
                result = (result & 0x3f) << 8;
                result |= stream.read();
            } else if ((result & 0b11000000) == 0b11000000) {
                // WARNING! Large N doesn't supported NOW!
                // NOTE 2. b) (large "n") a single octet containing a count "m"
                // with bit 8 set to 1 and bit 7 set to 1.
                // The count "m" is one to four, and the length indicates that
                // a fragment of the material follows (a multiple "m" of 16K items).
                // For all values of "m", the fragment is then followed
                // by another length encoding for the remainder of the material.
                throw new UnsupportedOperationException("number of 16k chunks not supported");
            }
        }
        // NOTE 2. a) ("n" less than 128)
        // a single octet containing "n" with bit 8 set to zero;

        logger.trace("decoded len determinent {}", result);
        return result;
    }

    /**
     * Decode an octet string ITU-T X.691. 17. Encoding the octetstring type
     *
     * @param stream binary stream
     * @param len octet string length
     * @return octet string as a byte array
     * @throws IOException input exception
     */
    public byte[] decodeOctetString(BitInputStream stream, BigInteger len) throws IOException {
        if (len.compareTo(new BigInteger("2")) <= 0) {
            return stream.readUnalignedByteArray(len.intValueExact());
        } else if (len.compareTo(BigInteger.valueOf(64L * 1024L)) < 0) {
            return stream.readAlignedByteArray(len.intValueExact());
        } else {
            throw new RuntimeException("unsupported case");
        }
    }

    /**
     * Read n bits
     *
     * @param s binary stream
     * @param n number of bits read
     * @return byte array containing n bits read (aligned left)
     * @throws IOException input exception
     */
    public byte[] readBits(BitInputStream s, int n) throws IOException {
        if (aligned) {
            return s.readAlignedBitArray(n);
        } else {
            throw new NotHandledCaseException();
        }
    }

    /**
     * Read n bits and return it as a string
     *
     * @param s input stream
     * @param n nomber of bits read
     * @return string containing n bits read (aligned left)
     * @throws IOException input stream
     */
    public String readsBitsAsString(BitInputStream s, int n) throws IOException {
        StringBuilder sb = new StringBuilder(n);
        if (aligned) {
            for (int i = 0; i < n; i++) {
                sb.append(s.readBit() == 0 ? '0' : '1');
            }

        } else {
            throw new RuntimeException();
        }
        return sb.toString();
    }

    /**
     * Aligned bits of the binary input stream
     *
     * @param stream binary stream
     */
    public void skipAlignedBits(InputStream stream) {
        ((BitInputStream) stream).skipUnreadedBits();
    }

    //Output stream part
    /**
     * Aligned bits of the binary output stream
     *
     * @param stream binary stream
     * @throws IOException output stream exception
     */
    public void skipAlignedBits(BitArray stream) throws IOException {
        if (aligned) {
            stream.skipAlignedBits();
        }
    }

    /**
     * Write preamble bit for extension marker of ASN.1 string type
     *
     * @param bitArray binary stream
     * @param bit first bit to be written (0x01)
     * @throws IOException output stream exception
     */
    public void writePreambleBit(BitArray bitArray, int bit) throws IOException {
        bitArray.writeBit(bit);
    }

    /**
     * Encode a constrained whole number : X.691 11.5
     *
     * @param s binary ouput stream
     * @param number value to encode
     * @param lb lower bound
     * @param ub upper bound
     * @throws IOException ouput stream exception
     */
    public void encodeConstrainedWholeNumber(BitArray s, BigInteger number, BigInteger lb, BigInteger ub) throws IOException {
        logger.trace("encodeConstrainedWholeNumber : number(DEC)=" + number.toString(10) + ", lb=" + lb + ", ub=" + ub);
        BigInteger range = ub.subtract(lb).add(ONE);
        BigInteger value = number.subtract(lb);
        if (range.compareTo(ZERO) <= 0) {
            throw new InvalidParameterException("Bad range " + range);
        } else {
            if (!range.equals(ONE)) {
                if (range.compareTo(BigInteger.valueOf(255)) <= 0) {
                    this.encodeBitField(s, value, range.subtract(BigInteger.valueOf(1)).bitLength());
                } else if (range.compareTo(BigInteger.valueOf(256)) == 0) {
                    if (aligned) {
                        s.skipAlignedBits();
                    }
                    this.encodeBitField(s, value, 8);
                } else if (range.compareTo(BigInteger.valueOf(65536)) <= 0) {
                    if (aligned) {
                        s.skipAlignedBits();
                    }
                    this.encodeBitField(s, value, 16);
                } else {
                    //11.5.7.4 (The indefinite length case.) Otherwise, the value ("n" – "lb") shall be encoded as a
                    // non-negative-binary-integer in a bit-field (octet-aligned in the ALIGNED variant) with the minimum number
                    // of octets as specified in 11.3, and the number of octets "len" used in the encoding is used by other
                    // clauses that reference this subclause to specify an encoding of the length.
                    this.encodeBitField(s, value, toByteCount(value.bitLength()) * 8);
                }
            }
        }
    }

    /**
     * Encode a normally whole number : X.691 11.6
     *
     * @param s binary ouput streaù
     * @param number value to encode
     * @throws IOException ouput stream exception
     */
    public void encodeNormallySmallWholeNumber(BitArray s, BigInteger number) throws IOException {
        if ((number.compareTo(BigInteger.valueOf(63)) <= 0) && (number.compareTo(BigInteger.valueOf(0)) >= 0)) {
            logger.trace("encodeNormallySmallWholeNumber : number=" + number.intValueExact() + " , length=6");
            s.writeBit(0);
            encodeBitField(s, number, 6);
        } else if (number.compareTo(BigInteger.valueOf(64)) >= 0) {
            logger.trace("encodeNormallySmallWholeNumber : number=" + number.toString(10) + " , length=6");
            s.writeBit(1);
            encodeSemiConstrainedWholeNumber(s, ZERO, number);
        } else {
            throw new RuntimeException();
        }
    }

    /**
     * Add a bits array to a given binary output stream
     *
     * @param s binary output stream
     * @param string string to encode
     * @param length length of the string
     * @throws IOException ouput stream exception
     */
    public void encodeBitField(BitArray s, String string, int length) throws IOException {
        int[] value = stringToBytes(string);
        int i = 0, j = 0;
        int currentByte;
        while (i < length) {
            currentByte = value[j] & 0xf;
            for (int l = 0; l < 4 && i + l < length; l++) {
                if ((currentByte >> (3 - l) & 0x01) == 0x01) {
                    s.writeBit(1);
                } else {
                    s.writeBit(0);
                }
            }
            i += 4;
            j++;
        }
    }

    public void encodeBitString(BitArray s, String string) throws IOException {
        byte[] bytes = string.getBytes();
        for (byte digit : bytes) {
            if (digit == 49) {
                s.writeBit(1);
            } else if (digit == 48) {
                s.writeBit(0);
            } else {
                throw new EncodingException("Not equals to 1 or 0 : " + digit);
            }
        }
    }

    private int[] stringToBytes(String value) {
        String[] strings = value.split("");
        int[] result = new int[value.length()];
        for (int i = 0; i < value.length(); i++) {
            result[i] = Integer.parseInt(strings[i], 16);
        }
        return result;
    }

    /**
     * Encoding a constrained length determinant X.691 13.2.6
     *
     * @param s binary output stream
     * @param length length to encode
     * @param lb lower bound
     * @param ub upper bound
     * @throws IOException output stream exception
     */
    public void encodeConstrainedLengthDeterminant(BitArray s, BigInteger length, BigInteger lb, BigInteger ub) throws IOException {
        logger.trace("encodeConstrainedLengthDeterminant : length=" + length + " , lb=" + lb + " , ub=" + ub);
        if (ub.compareTo(BigInteger.valueOf(65536)) < 0) {
            this.encodeConstrainedWholeNumber(s, length, lb, ub);
        } else {
            this.encodeLengthDeterminant(s, length);
        }
    }

    /**
     * Encode a length determinant X.691 11.9
     *
     * @param s binary ouput streaù
     * @param length length to encode
     * @throws IOException output stream exception
     */
    public void encodeLengthDeterminant(BitArray s, BigInteger length) throws IOException {
        logger.trace("encodeLengthDeterminant : length=" + length);
        if (aligned) {
            s.skipAlignedBits();
        }
        if (length.compareTo(BigInteger.valueOf(128)) < 0) {
            s.write(length.intValueExact() & 0b01111111);
        } else if (length.compareTo(BigInteger.valueOf(16384)) < 0) {
            int value = length.intValueExact();
            s.write((((value >> 8)) & 0x3f | 0b10000000) & 0b10111111);
            s.write(value & 0xff);
        } else {
            throw new UnsupportedOperationException("number of 16k chunks not supported");
        }
    }

    /**
     * Write a bit field (should use encodeBitField instead), can cause bug with
     * a bit field who begin with zero
     *
     * @param s binary output stream
     * @param number bit field to encode
     * @param length bit field length
     * @throws IOException output stream exception
     */
    public void encodeRestrictedCharacterString(BitArray s, BigInteger number, int length) throws IOException {
        logger.trace("encodeRestrictedCharacterString : number=" + String.format("%x", number).toUpperCase() + " , length=" + length);
        encodeBitField(s, number, length);
    }

    /**
     * Encode a bit field
     *
     * @param s bit array
     * @param bitField bit field to encode
     * @param length length of the bit field
     * @throws IOException output stream exception
     */
    private void encodeBitField(BitArray s, BigInteger bitField, int length) throws IOException {
        for (int i = length - 1; i >= 0; i--) {
            if (((bitField.shiftRight(i)).and(ONE)).equals(ONE)) {
                s.writeBit(1);
            } else {
                s.writeBit(0);
            }
        }
    }

    /**
     * Encode a semi constrained whole number X.691 11.7
     *
     * @param s binary ouput streaù
     * @param lb lower bound
     * @param number number to encode
     * @throws IOException output stream exception
     */
    public void encodeSemiConstrainedWholeNumber(BitArray s, BigInteger lb, BigInteger number) throws IOException {
        int octetLength = number.bitLength() / 8 + ((number.bitLength() % 8 != 0) ? 1 : 0) - lb.intValueExact();
        logger.trace("encodeSemiConstrainedWholeNumber : value=" + String.format("%x", number).toUpperCase() + ", length=" + octetLength);
        encodeLengthDeterminant(s, BigInteger.valueOf(octetLength));
        if (aligned) {
            s.skipAlignedBits();
        }
        encodeBitField(s, number, octetLength * 8);
    }
}
