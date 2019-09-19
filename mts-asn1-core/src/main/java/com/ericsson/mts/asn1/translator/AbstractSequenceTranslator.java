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

import com.ericsson.mts.asn1.ASN1Parser;
import com.ericsson.mts.asn1.BitArray;
import com.ericsson.mts.asn1.BitInputStream;
import com.ericsson.mts.asn1.TranslatorContext;
import com.ericsson.mts.asn1.exception.NotHandledCaseException;
import com.ericsson.mts.asn1.factory.FormatReader;
import com.ericsson.mts.asn1.factory.FormatWriter;
import com.ericsson.mts.asn1.registry.MainRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractSequenceTranslator extends AbstractTranslator {

    protected List<Field> fieldList = new ArrayList<>();
    protected List<Field> additionnalFieldList = new ArrayList<>();
    protected boolean hasEllipsis = false;
    protected int extensionAndException = -1;
    protected boolean optionalExtensionMarker = false;
    protected int rootSequenceOptionalCount = 0;
    protected boolean[] optionalBitmap;

    public AbstractTranslator init(MainRegistry mainRegistry, ASN1Parser.SequenceTypeContext ctx) throws NotHandledCaseException {
        AtomicReference<Boolean> isOptionnal = new AtomicReference<>(false);
        if (ctx.extensionAndException() != null) {
            hasEllipsis = true;
        } else {
            if (ctx.componentTypeLists().getChild(0).getClass().getSimpleName().compareTo(ASN1Parser.RootComponentTypeListContext.class.getSimpleName()) == 0) {
                ctx.componentTypeLists().rootComponentTypeList(0).componentTypeList().componentType().forEach(componentTypeContext -> {
                    if (componentTypeContext.namedType() != null) {
                        if (componentTypeContext.DEFAULT_LITERAL() != null) {
                            throw new NotHandledCaseException();
                        }
                        if (componentTypeContext.OPTIONAL_LITERAL() != null) {
                            isOptionnal.set(true);
                            rootSequenceOptionalCount++;
                        }
                        AbstractTranslator abstractTranslator = mainRegistry.getTranslator(componentTypeContext.namedType().asnType());
                        Field field = new Field(componentTypeContext.namedType().IDENTIFIER().getText(),
                                abstractTranslator,
                                isOptionnal.get());
                        if (componentTypeContext.namedType().asnType().referencedType() != null) {
                            if (componentTypeContext.namedType().asnType().referencedType().definedType().actualParameterList() != null) {
                                this.handleParameters(componentTypeContext.namedType().asnType().referencedType().definedType().actualParameterList(), field);
                            }
                        }
                        if (componentTypeContext.namedType().asnType().builtinType() != null) {
                            if (componentTypeContext.namedType().asnType().builtinType().objectClassFieldType() != null) {
                                //Add catalog to field parameter
                                field.addParameter(abstractTranslator.getParameters().get(0));
                            }
                        }

                        fieldList.add(field);
                        isOptionnal.set(false);
                    } else {
                        throw new NotHandledCaseException();
                    }
                });
                if (ctx.componentTypeLists().extensionAndException() != null) {
                    extensionAndException = fieldList.size() + 1;
                    if (ctx.componentTypeLists().extensionAndException().exceptionSpec() != null) {
                        throw new NotHandledCaseException();
                    }
                    if (ctx.componentTypeLists().extensionAdditions() != null) {
                        if (ctx.componentTypeLists().extensionAdditions().extensionAdditionList() != null) {
                            ctx.componentTypeLists().extensionAdditions().extensionAdditionList().extensionAddition().forEach(extensionAdditionContext -> {
                                if (extensionAdditionContext.componentType() != null) {
                                    if (extensionAdditionContext.componentType().namedType() != null) {
                                        if (extensionAdditionContext.componentType().DEFAULT_LITERAL() != null) {
                                            throw new NotHandledCaseException();
                                        }
                                        if (extensionAdditionContext.componentType().OPTIONAL_LITERAL() != null) {
                                            isOptionnal.set(true);
                                        }
                                        AbstractTranslator abstractTranslator = mainRegistry.getTranslator(extensionAdditionContext.componentType().namedType().asnType());
                                        Field field = new Field(extensionAdditionContext.componentType().namedType().IDENTIFIER().getText(),
                                                abstractTranslator,
                                                isOptionnal.get());
                                        if (extensionAdditionContext.componentType().namedType().asnType().referencedType() != null) {
                                            if (extensionAdditionContext.componentType().namedType().asnType().referencedType().definedType().actualParameterList() != null) {
                                                this.handleParameters(extensionAdditionContext.componentType().namedType().asnType().referencedType().definedType().actualParameterList(), field);
                                            }
                                        }
                                        if (extensionAdditionContext.componentType().namedType().asnType().builtinType() != null) {
                                            if (extensionAdditionContext.componentType().namedType().asnType().builtinType().objectClassFieldType() != null) {
                                                //Add catalog to field parameter
                                                field.addParameter(abstractTranslator.getParameters().get(0));
                                            }
                                        }

                                        additionnalFieldList.add(field);
                                        isOptionnal.set(false);
                                    } else {
                                        throw new NotHandledCaseException();
                                    }
                                } else {
                                    throw new NotHandledCaseException();
                                }
                            });
                        }
                    }
                    if (ctx.componentTypeLists().optionalExtensionMarker() != null) {
                        optionalExtensionMarker = true;
                    }
                }
            } else if (ctx.componentTypeLists().getChild(0).getClass().getSimpleName().compareTo(ASN1Parser.ExtensionAndExceptionContext.class.getSimpleName()) == 0) {
                throw new NotHandledCaseException();
            }
        }
        optionalBitmap = new boolean[rootSequenceOptionalCount];
        return this;
    }

    private void handleParameters(ASN1Parser.ActualParameterListContext actualParameterListContext, Field field) {
        for (ASN1Parser.ActualParameterContext actualParameterContext : actualParameterListContext.actualParameter()) {
            if (actualParameterContext.value() != null) {
                if (actualParameterContext.value().builtinValue().objectIdentifierValue() != null) {
                    if (actualParameterContext.value().builtinValue().objectIdentifierValue().objIdComponentsList() != null) {
                        if (actualParameterContext.value().builtinValue().objectIdentifierValue().objIdComponentsList().objIdComponents().size() == 1) {
                            if (actualParameterContext.value().builtinValue().objectIdentifierValue().objIdComponentsList().objIdComponents(0).IDENTIFIER() != null
                                    && actualParameterContext.value().builtinValue().objectIdentifierValue().objIdComponentsList().objIdComponents(0).L_PARAN() == null) {
                                field.addParameter(actualParameterContext.value().builtinValue().objectIdentifierValue().objIdComponentsList().objIdComponents(0).IDENTIFIER().getText());
                            }
                        } else {
                            throw new NotHandledCaseException();
                        }
                    } else {
                        throw new NotHandledCaseException();
                    }
                } else {
                    throw new NotHandledCaseException();
                }
            } else {
                throw new NotHandledCaseException("asnType");
            }
        }
    }

    public AbstractTranslator init(MainRegistry mainRegistry, ASN1Parser.SequenceTypeContext ctx, ASN1Parser.ParameterListContext parameterListContext) {
        addParameters(parameterListContext);
        return init(mainRegistry, ctx);
    }

    @Override
    public final void encode(String name, BitArray s, FormatReader reader, TranslatorContext translatorContext, List<String> parameters) throws Exception {
        reader.enterObject(name);
        doEncode(s, reader, new TranslatorContext(), reader.fieldsValue(), getRegister(parameters));
        reader.leaveObject(name);
    }

    public abstract void doEncode(BitArray s, FormatReader reader, TranslatorContext translatorContext, List<String> inputFieldList, Map<String, String> registry) throws Exception;

    @Override
    public final void decode(String name, BitInputStream s, FormatWriter writer, TranslatorContext translatorContext, List<String> parameters) throws Exception {
        writer.enterObject(name);
        try {
            doDecode(s, writer, new TranslatorContext(), getRegister(parameters));
        } finally {
            writer.leaveObject(name);
        }
    }

    protected abstract void doDecode(BitInputStream s, FormatWriter writer, TranslatorContext translatorContext, Map<String, String> registry) throws Exception;

    @Override
    public String toString() {
        return "AbstractSequenceTranslator{"
                + "fieldList=" + fieldList
                + ", additionnalFieldList=" + additionnalFieldList
                + ", hasEllipsis=" + hasEllipsis
                + ", extensionAndException=" + extensionAndException
                + ", optionalExtensionMarker=" + optionalExtensionMarker
                + ", rootSequenceOptionalCount=" + rootSequenceOptionalCount
                + ", optionalBitmap=" + Arrays.toString(optionalBitmap)
                + '}';
    }

    protected class Field {

        String name;
        AbstractTranslator type;
        boolean isOptionnal;
        List<String> parameters = new ArrayList<>();

        public Field(String name, AbstractTranslator type, Boolean isOptionnal) {
            this.name = name;
            this.type = type;
            this.isOptionnal = isOptionnal;
        }

        public void addParameter(String parameter) {
            parameters.add(parameter);
        }

        public List<String> getParameters() {
            return parameters;
        }

        public String getName() {
            return name;
        }

        public AbstractTranslator getType() {
            return type;
        }

        public Boolean getOptionnal() {
            return isOptionnal;
        }

    }
}
