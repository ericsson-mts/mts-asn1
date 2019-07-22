/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.ericsson.mts.asn1.plugin;

import com.ericsson.mts.asn1.ASN1Lexer;
import com.ericsson.mts.asn1.ASN1Parser;
import com.ericsson.mts.asn1.ASN1ParserBaseVisitor;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;

class IdentifierVisitor {
    private IdentifierVisitorInner identifierVisitorInner;
    private HashSet<String> insertedName = new HashSet<>();

    IdentifierVisitor(TypeSpec.Builder typeSpec) {
        identifierVisitorInner = new IdentifierVisitorInner(typeSpec);
    }

    TypeSpec.Builder beginVisit(InputStream stream) throws IOException {
        return identifierVisitorInner.beginVisit(stream);
    }


    private class IdentifierVisitorInner extends ASN1ParserBaseVisitor {
        private TypeSpec.Builder builder;

        /**
         * Constructor
         *
         * @param builder javapoet builder
         */
        IdentifierVisitorInner(TypeSpec.Builder builder) {
            this.builder = builder;
        }

        /**
         * Visit asn file and return completed builder
         * @param stream asn file
         * @return javapoet builder
         * @throws IOException output file error
         */
        TypeSpec.Builder beginVisit(InputStream stream) throws IOException {
            CharStream inputStream = CharStreams.fromStream(stream);
            ASN1Lexer asn1Lexer = new ASN1Lexer(inputStream);
            CommonTokenStream commonTokenStream = new CommonTokenStream(asn1Lexer);
            ASN1Parser asn1Parser = new ASN1Parser(commonTokenStream);
            visitModuleDefinition(asn1Parser.moduleDefinition());
            return builder;
        }


        @Override
        public Object visitNamedNumber(ASN1Parser.NamedNumberContext ctx) {
            addField(ctx.IDENTIFIER().getText(), ctx.IDENTIFIER().getText());
            return super.visitNamedNumber(ctx);
        }


        @Override
        public Object visitNamedType(ASN1Parser.NamedTypeContext ctx) {
            addField(ctx.IDENTIFIER().getText(), ctx.IDENTIFIER().getText());
            return super.visitNamedType(ctx);
        }

        /**
         * add a field to javapoet Builder
         * @param fieldName name of the field
         * @param fieldValue value of the field
         */
        private void addField(String fieldName, String fieldValue) {
            String inputName = fieldName.replace("-", "_").toUpperCase();
            if (!insertedName.contains(fieldName)) {
                insertedName.add(fieldName);
                builder.addField(FieldSpec.builder(String.class, inputName)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$S", fieldValue)
                        .build());
            }
        }
    }
}
