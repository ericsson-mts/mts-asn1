/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.ericsson.mts.asn1.classhandler;

import com.ericsson.mts.asn1.ASN1Parser;
import com.ericsson.mts.asn1.exception.NotHandledCaseException;
import com.ericsson.mts.asn1.registry.MainRegistry;
import com.ericsson.mts.asn1.translator.AbstractTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class ClassHandler {
    private static Logger logger = LoggerFactory.getLogger(ClassHandler.class.getSimpleName());
    private ArrayList<Field> fields = new ArrayList<>();
    private MainRegistry mainRegistry;

    public ClassHandler(MainRegistry mainRegistry, ASN1Parser.ObjectClassDefnContext ctx) {
        this.mainRegistry = mainRegistry;
        ctx.fieldSpec().forEach(fieldSpecContext -> {
            Field field = new Field();
            field.name = fieldSpecContext.IDENTIFIER().getText();
            if (fieldSpecContext.typeOptionalitySpec() != null) {
                if (fieldSpecContext.typeOptionalitySpec().OPTIONAL_LITERAL() != null) {
                    field.qualifier = FIELDTYPE.OPTIONAL;
                } else if (fieldSpecContext.typeOptionalitySpec().DEFAULT_LITERAL() != null) {
                    field.qualifier = FIELDTYPE.DEFAULT;
                    field.defaultValue = fieldSpecContext.typeOptionalitySpec().asnType().getText();
                }
            } else if (fieldSpecContext.asnType() != null) {
                field.type = mainRegistry.getTranslator(fieldSpecContext.asnType());
                if (fieldSpecContext.UNIQUE_LITERAL() != null) {
                    field.qualifier = FIELDTYPE.UNIQUE;
                }
                if (fieldSpecContext.valueOptionalitySpec() != null) {
                    if (fieldSpecContext.valueOptionalitySpec().OPTIONAL_LITERAL() != null) {
                        field.qualifier = FIELDTYPE.OPTIONAL;
                    } else {
                        field.qualifier = FIELDTYPE.DEFAULT;
                        field.defaultValue = fieldSpecContext.valueOptionalitySpec().value().getText();
                    }
                }
                if (fieldSpecContext.valueSetOptionalitySpec() != null) {
                    throw new NotHandledCaseException();
                }
            } else if (fieldSpecContext.fieldName() != null || fieldSpecContext.definedObjectClass() != null) {
                throw new NotHandledCaseException();
            }
            fields.add(field);
        });
        if (ctx.withSyntaxSpec() != null) {
            String syntax = "";
            for (ASN1Parser.TokenOrGroupSpecContext tokenOrGroupSpecContext : ctx.withSyntaxSpec().syntaxList().tokenOrGroupSpec()) {
                if (tokenOrGroupSpecContext.requiredToken() != null) {
                    if (tokenOrGroupSpecContext.requiredToken().literal() != null) {
                        if (tokenOrGroupSpecContext.requiredToken().literal().IDENTIFIER() != null) {
                            syntax += tokenOrGroupSpecContext.requiredToken().literal().getText() + " ";
                        }
                    } else {
                        for (Field field : fields) {
                            if (field.name.compareTo(tokenOrGroupSpecContext.requiredToken().primitiveFieldName().IDENTIFIER().getText()) == 0) {
                                field.syntax = syntax.substring(0, syntax.length() - 1);
                                syntax = "";
                            }
                        }
                        if ("".compareTo(syntax) != 0) {
                            throw new NotHandledCaseException("Can't find field " + tokenOrGroupSpecContext.requiredToken().primitiveFieldName().IDENTIFIER().getText());
                        }
                    }
                } else {
                    for (ASN1Parser.TokenOrGroupSpecContext tokenOrGroupSpecContext1 : tokenOrGroupSpecContext.optionalGroup().tokenOrGroupSpec()) {
                        if (tokenOrGroupSpecContext1.requiredToken() != null) {
                            if (tokenOrGroupSpecContext1.requiredToken().literal() != null) {
                                if (tokenOrGroupSpecContext1.requiredToken().literal().IDENTIFIER() != null) {
                                    syntax += tokenOrGroupSpecContext1.requiredToken().literal().getText() + " ";
                                }
                            } else {
                                for (Field field : fields) {
                                    if (field.name.compareTo(tokenOrGroupSpecContext1.requiredToken().primitiveFieldName().IDENTIFIER().getText()) == 0) {
                                        field.syntax = syntax.substring(0, syntax.length() - 1);
                                        syntax = "";
                                    }
                                }
                                if ("".compareTo(syntax) != 0) {
                                    throw new NotHandledCaseException("Can't find optionnal field " + tokenOrGroupSpecContext1.requiredToken().primitiveFieldName().IDENTIFIER().getText());
                                }
                            }
                        } else {
                            throw new NotHandledCaseException();
                        }
                    }
                }
            }
        }
    }

    public String getUniqueKeyName() {
        for (Field field : fields) {
            if (field.qualifier == FIELDTYPE.UNIQUE) {
                if (null != field.syntax) {
                    return field.syntax;
                } else {
                    return field.name;
                }
            }
        }
        throw new RuntimeException("Can't find unique key for class " + toString());
    }

    public AbstractTranslator getTypeTranslator(String fieldName, String objectSetIdentifier, String uniqueKey) {
        AbstractTranslator abstractTranslator = mainRegistry.getClassObjectSet(objectSetIdentifier).getTranslatorForField(uniqueKey, fieldName);
        if (abstractTranslator == null) {
            throw new RuntimeException();
        }
        return abstractTranslator;
    }

    public AbstractTranslator getTypeTranslator(String fieldName) {
        for (Field field : fields) {
            if (fieldName.compareTo(field.name) == 0) {
                return field.type;
            } else if (fieldName.compareTo(field.syntax) == 0) {
                return field.type;
            }
        }

        throw new RuntimeException("Can't find field " + fieldName);
    }

    public String getSyntaxName(String fieldName) {
        for (Field field : fields) {
            if (fieldName.equals(field.name)) {
                return field.syntax;
            }
        }
        return fieldName;
    }

    public ArrayList<String> getSyntaxFields() {
        ArrayList<String> syntaxFields = new ArrayList<>();
        for (Field field : fields) {
            syntaxFields.add(field.syntax);
        }
        return syntaxFields;
    }

    @Override
    public String toString() {
        return "ClassHandler{" +
                "fields=" + fields +
                ", mainRegistry=" + mainRegistry +
                '}';
    }

    public enum FIELDTYPE {
        OPTIONAL, UNIQUE, DEFAULT
    }

    private class Field {
        public String name;
        public AbstractTranslator type;
        public FIELDTYPE qualifier = null;
        public String syntax = null;
        private String defaultValue = null;

        public String getDefaultValue() {
            if (qualifier == FIELDTYPE.DEFAULT)
                return defaultValue;
            return null;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        public String toString() {
            return "Field{" +
                    "name='" + name + '\'' +
                    ", type=" + type +
                    ", qualifier=" + qualifier +
                    ", syntax='" + syntax + '\'' +
                    ", defaultValue='" + defaultValue + '\'' +
                    '}';
        }
    }
}
