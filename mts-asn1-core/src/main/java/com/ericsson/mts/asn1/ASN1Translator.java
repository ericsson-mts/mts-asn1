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

import com.ericsson.mts.asn1.exception.NotHandledCaseException;
import com.ericsson.mts.asn1.factory.AbstractFactory;
import com.ericsson.mts.asn1.factory.FormatReader;
import com.ericsson.mts.asn1.factory.FormatWriter;
import com.ericsson.mts.asn1.registry.MainRegistry;
import com.ericsson.mts.asn1.visitor.TopLevelVisitor;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ASN1Translator {
    private Logger logger = LoggerFactory.getLogger(ASN1Translator.class.getSimpleName());
    private MainRegistry registry;

    public ASN1Translator(AbstractFactory factory, List<InputStream> stream) {
        registry = new MainRegistry(factory);
        for (InputStream inputStream : stream) {
            beginVisit(inputStream);
        }
    }

    public void encode(String string, BitArray bitArray, FormatReader formatReader) throws Exception {
        registry.getTranslatorFromName(string).encode(string, bitArray, formatReader, null);
    }

    public void decode(String str, InputStream stream, FormatWriter formatWriter) throws NotHandledCaseException, IOException {
        registry.getTranslatorFromName(str).decode(str, new BitInputStream(stream), formatWriter, null);
    }

    private void beginVisit(InputStream stream) {
        CharStream inputStream = null;
        try {
            inputStream = CharStreams.fromStream(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ASN1Lexer asn1Lexer = new ASN1Lexer(inputStream);
        CommonTokenStream commonTokenStream = new CommonTokenStream(asn1Lexer);
        ASN1Parser asn1Parser = new ASN1Parser(commonTokenStream);
        new TopLevelVisitor(registry).visitModuleDefinition(asn1Parser.moduleDefinition());
    }
}
