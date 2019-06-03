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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractTests {
    protected static ASN1Translator asn1Translator;
    protected ObjectMapper mapper = new ObjectMapper();
    protected ObjectWriter writer = mapper.writer();
    protected Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());

    protected void test(String type, String binaryPath, String expectedJsonPath, String expectedXmlPath) throws Exception {
        testDecode(type, binaryPath, expectedJsonPath, expectedXmlPath);
        testEncode(type, binaryPath, expectedJsonPath, expectedXmlPath);
    }

    protected void testDecode(String type, String binaryPath, String expectedJsonPath, String expectedXmlPath) throws IOException, TransformerException, SAXException, ParserConfigurationException {
        //JSON decode test
        {
            JSONFormatWriter formatWriter = new JSONFormatWriter();
            asn1Translator.decode(type, this.getClass().getResourceAsStream(binaryPath), formatWriter);
            String actual = writer.writeValueAsString(formatWriter.getJsonNode());
            String expected = IOUtils.toString(this.getClass().getResourceAsStream(expectedJsonPath), StandardCharsets.UTF_8);
            assertJsonEquals(expected, actual);
        }

        //XML decode test
        {
            XMLFormatWriter formatWriter = new XMLFormatWriter();
            asn1Translator.decode(type, this.getClass().getResourceAsStream(binaryPath), formatWriter);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(formatWriter.getResult()), new StreamResult(writer));


            String actual = writer.toString();
            String expected = IOUtils.toString(this.getClass().getResourceAsStream(expectedXmlPath), StandardCharsets.UTF_8);
            XMLUnit.setIgnoreWhitespace(true);
            Diff diff = XMLUnit.compareXML(expected, actual);

            assertTrue(diff.similar(), diff.toString() + "\nActual document is :\n" + actual);
        }
    }

    protected void testEncode(String type, String binaryPath, String expectedJsonPath, String expectedXmlPath) throws Exception {
        //JSON encode test
        {

            JSONFormatReader jsonFormatReader = new JSONFormatReader(this.getClass().getResourceAsStream(expectedJsonPath), type);
            BitArray bitArray = new BitArray();
            asn1Translator.encode(type, bitArray, jsonFormatReader);


            InputStream expectedInputStream = this.getClass().getResourceAsStream(binaryPath);
            BitArray bitArray1 = new BitArray();
            while (expectedInputStream.available() != 0) {
                bitArray1.write(expectedInputStream.read());
            }

            assertEquals(bitArray1.getBinaryMessage(), bitArray.getBinaryMessage());
        }

        //XML encode test
        {
            XMLFormatReader xmlFormatReader = new XMLFormatReader(this.getClass().getResourceAsStream(expectedXmlPath), type);
            BitArray bitArray = new BitArray();
            asn1Translator.encode(type, bitArray, xmlFormatReader);

            InputStream expectedInputStream = this.getClass().getResourceAsStream(binaryPath);
            BitArray bitArray1 = new BitArray();
            while (expectedInputStream.available() != 0) {
                bitArray1.write(expectedInputStream.read());
            }

            assertEquals(bitArray1.getBinaryMessage(), bitArray.getBinaryMessage());
        }
    }
}
