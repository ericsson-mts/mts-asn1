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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class AbstractTranslator {
    protected Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());
    protected String name;
    protected List<Parameter> parameters = new ArrayList<Parameter>() {
        @Override
        public String toString() {
            StringBuilder str = new StringBuilder();
            for (Parameter parameter : parameters) {
                str.append(parameter).append("\n");
            }
            return str.toString();
        }
    };


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    protected void addParameters(ASN1Parser.ParameterListContext parameterListContext) {
        for (ASN1Parser.ParameterContext parameterContext : parameterListContext.parameter()) {
            if (parameterContext.paramGovernor().governor() != null) {
                if (parameterContext.paramGovernor().governor().asnType() != null) {
                    if (parameterContext.paramGovernor().governor().asnType().referencedType() != null) {
                        if (parameterContext.paramGovernor().governor().asnType().referencedType().definedType().IDENTIFIER().size() == 1) {
                            parameters.add(new Parameter(parameterContext.paramGovernor().governor().asnType().referencedType().definedType().IDENTIFIER(0).getText(), parameterContext.IDENTIFIER().getText()));
                        } else {
                            throw new NotHandledCaseException();
                        }
                    }
                } else {
                    throw new NotHandledCaseException();
                }
            } else {
                parameters.add(new Parameter(parameterContext.IDENTIFIER().getText(), parameterContext.IDENTIFIER().getText()));
            }
        }
    }

    public List<String> getParameters() {
        List<String> parameterList = new ArrayList<>();
        for (Parameter parameter : parameters) {
            parameterList.add(parameter.getName());
        }
        return parameterList;
    }

    abstract public void encode(String name, BitArray s, FormatReader reader, TranslatorContext translatorContext, List<String> parameters) throws Exception;

    public final void encode(String name, BitArray s, FormatReader reader, TranslatorContext translatorContext) throws Exception {
        if (!parameters.isEmpty()) {
            throw new RuntimeException("Translator name : " + this.name + " , type : " + this.getClass().getSimpleName() + " , name : " + name + " , " + "parameters : " + parameters.toString());
        }
        this.encode(name, s, reader, translatorContext, Collections.emptyList());
    }

    abstract public void decode(String name, BitInputStream s, FormatWriter writer, TranslatorContext translatorContext, List<String> parameters) throws Exception;

    public final void decode(String name, BitInputStream s, FormatWriter writer, TranslatorContext translatorContext) throws Exception {
        if (!parameters.isEmpty()) {
            throw new RuntimeException("name : " + name + " , " + "parameters : " + parameters.toString());
        }
        this.decode(name, s, writer, translatorContext, Collections.emptyList());
    }

    protected Map<String, String> getRegister(List<String> values) {
        Map<String, String> register = new HashMap<String, String>() {
            @Override
            public String get(Object key) {
                Object value = super.get(key);
                return null == value ? key.toString() : value.toString();
            }
        };

        if (values.size() != parameters.size()) {
            throw new RuntimeException();
        }
        // fill register
        Iterator<Parameter> parametersDefIterator = parameters.iterator();
        Iterator<String> parametersValuesIterator = values.iterator();

        while (parametersDefIterator.hasNext() && parametersValuesIterator.hasNext()) {
            register.put(parametersDefIterator.next().getName(), parametersValuesIterator.next());
        }

        return register;
    }

    protected class Parameter {
        private String type;
        private String name;
        private String value;

        public Parameter(String type, String name) {
            this.setType(type);
            this.setName(name);
        }

        @Override
        public String toString() {
            return "Parameter{" +
                    "type='" + getType() + '\'' +
                    ", name='" + getName() + '\'' +
                    ", value='" + getValue() + '\'' +
                    '}';
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
