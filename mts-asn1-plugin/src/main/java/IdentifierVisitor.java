/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
import java.util.Stack;

public class IdentifierVisitor {
    private IdentifierVisitorInner identifierVisitorInner;
    private Stack<String> parentNameStack = new Stack<>();
    private HashSet insertedName = new HashSet<String>() {
        @Override
        public boolean add(String o) {
            if (this.contains(o)) {
                throw new RuntimeException();
            }
            return super.add(o);
        }
    };

    public IdentifierVisitor(TypeSpec.Builder typeSpec) {
        identifierVisitorInner = new IdentifierVisitorInner(typeSpec);
    }

    public TypeSpec.Builder beginVisit(InputStream stream) throws IOException {
        return identifierVisitorInner.beginVisit(stream);
    }


    private class IdentifierVisitorInner extends ASN1ParserBaseVisitor {

        private TypeSpec.Builder builder;

        IdentifierVisitorInner(TypeSpec.Builder builder) {
            this.builder = builder;
        }

        TypeSpec.Builder beginVisit(InputStream stream) throws IOException {
            CharStream inputStream = CharStreams.fromStream(stream);
            ASN1Lexer asn1Lexer = new ASN1Lexer(inputStream);
            CommonTokenStream commonTokenStream = new CommonTokenStream(asn1Lexer);
            ASN1Parser asn1Parser = new ASN1Parser(commonTokenStream);
            visitModuleDefinition(asn1Parser.moduleDefinition());
            if (!parentNameStack.empty()) {
                throw new RuntimeException();
            }
            return builder;
        }

        @Override
        public Object visitAssignment(ASN1Parser.AssignmentContext ctx) {
            addConstant(ctx.IDENTIFIER().getText(), ctx.IDENTIFIER().getText());
            parentNameStack.push(ctx.IDENTIFIER().getText());
            Object o = super.visitAssignment(ctx);
            if (!ctx.IDENTIFIER().getText().equals(parentNameStack.pop())) {
                throw new RuntimeException();
            }
            return o;
        }

        /***** Fields *****/

        @Override
        public Object visitNamedNumber(ASN1Parser.NamedNumberContext ctx) {
            addField(ctx.IDENTIFIER().getText(), ctx.IDENTIFIER().getText());
            parentNameStack.push(addParentName(ctx.IDENTIFIER().getText()));
            Object o = super.visitNamedNumber(ctx);
            parentNameStack.pop();
            return o;
        }

        @Override
        public Object visitNamedType(ASN1Parser.NamedTypeContext ctx) {
            addField(ctx.IDENTIFIER().getText(), ctx.IDENTIFIER().getText());
            parentNameStack.push(addParentName(ctx.IDENTIFIER().getText()));
            Object o = super.visitNamedType(ctx);
            parentNameStack.pop();
            return o;
        }

        /***** Format *****/

        private void addConstant(String constantName, String constantValue) {
            String inputName = constantName.replace("-", "_").toUpperCase();
            insertedName.add(inputName);
            builder.addField(FieldSpec.builder(String.class, inputName)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$S", constantValue)
                    .build());
        }

        private void addField(String fieldName, String fieldValue) {
            String inputName = addParentName(fieldName).replace("-", "_").toUpperCase();
            insertedName.add(inputName);
            builder.addField(FieldSpec.builder(String.class, inputName)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$S", fieldValue)
                    .build());
        }

        private String addParentName(String fieldName) {
            return parentNameStack.peek() + "." + fieldName;
        }
    }
}
