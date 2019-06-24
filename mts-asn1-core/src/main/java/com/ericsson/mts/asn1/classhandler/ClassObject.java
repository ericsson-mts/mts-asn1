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
import com.ericsson.mts.asn1.constant.AbstractConstant;
import com.ericsson.mts.asn1.exception.NotHandledCaseException;
import com.ericsson.mts.asn1.registry.MainRegistry;
import com.ericsson.mts.asn1.translator.AbstractTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassObject {
    private MainRegistry mainRegistry;
    private ClassHandler classtype;
    private Logger logger = LoggerFactory.getLogger(ClassObject.class.getSimpleName());
    private List<Map<String, String>> fieldMap = new ArrayList<>();
    private Map<String, ASN1Parser.AsnTypeContext> settingsMap = new HashMap<>();

    public ClassObject init(MainRegistry mainRegistry, ASN1Parser.ObjectAssignmentContext objectAssignmentContext) throws NotHandledCaseException {
        if (objectAssignmentContext.definedObjectClass().IDENTIFIER().size() != 1) {
            throw new NotHandledCaseException();
        }
        this.mainRegistry = mainRegistry;
        this.classtype = mainRegistry.getClassHandler(objectAssignmentContext.definedObjectClass().IDENTIFIER(0).getText());

        if (objectAssignmentContext.object().objectDefn() == null) {
            throw new NotHandledCaseException();
        }

        buildFields(objectAssignmentContext.object().objectDefn().definedSyntax());
        return this;
    }

    public void buildLocalObject(MainRegistry mainRegistry, ClassHandler classHandler, ASN1Parser.ObjectDefnContext objectDefnContext) {
        this.mainRegistry = mainRegistry;
        this.classtype = classHandler;
        if (objectDefnContext.defaultSyntax() != null) {
            throw new NotHandledCaseException();
        } else {
            List<String> unknowsFields = new ArrayList<>();
            for (ASN1Parser.DefinedSyntaxTokenContext definedSyntaxTokenContext : objectDefnContext.definedSyntax().definedSyntaxToken()) {
                if (definedSyntaxTokenContext.literal() != null && definedSyntaxTokenContext.literal().IDENTIFIER() != null) {
                    unknowsFields.add(definedSyntaxTokenContext.literal().IDENTIFIER().getText());
                } else if (definedSyntaxTokenContext.setting() != null) {
                    unknowsFields.add("In settingsMap at " + settingsMap.size());
                    settingsMap.put(unknowsFields.get(unknowsFields.size() - 1), definedSyntaxTokenContext.setting().asnType());
                } else {
                    throw new NotHandledCaseException("comma");
                }
            }
            buildFields(unknowsFields);
        }
    }

    private void buildFields(ASN1Parser.DefinedSyntaxContext definedSyntaxContexts) {
        List<String> unknowFields = new ArrayList<>();
        for (ASN1Parser.DefinedSyntaxTokenContext definedSyntaxTokenContext : definedSyntaxContexts.definedSyntaxToken()) {
            unknowFields.add(definedSyntaxTokenContext.literal().IDENTIFIER().getText());
        }
        buildFields(unknowFields);
    }

    private void buildFields(List<String> unknowFields) {
        int current_component = 0;
        int consumed_component = 0;
        String current_component_string = "";
        ArrayList<String> classfields = classtype.getSyntaxFields();
        HashMap<String, String> row = new HashMap<>();
        while (consumed_component < unknowFields.size()) {
            current_component_string = unknowFields.get(current_component);
            for (String syntax : classfields) {
                if (syntax.equals(current_component_string)) {
                    String value = unknowFields.get(current_component + 1);
                    row.put(syntax, value);
                    current_component += 2;
                    consumed_component = current_component;
                    break;
                } else if (syntax.startsWith(current_component_string)) {
                    current_component_string += " " + unknowFields.get(current_component + 1);
                    current_component++;
                    if (0 == syntax.compareTo(current_component_string)) {
                        String value = unknowFields.get(current_component + 1);
                        row.put(syntax, value);
                        current_component += 2;
                        consumed_component = current_component;
                        break;
                    } else {
                        throw new RuntimeException("Can't find syntax field for " + current_component_string);
                    }
                }
            }
        }
        fieldMap.add(row);
    }

    public AbstractTranslator getTranslatorFromUniqueKey(String uniqueKeySyntax, String uniqueKeyValue, String componentName) {
        boolean correctKey = false;
        componentName = classtype.getSyntaxName(componentName);
        for (Map<String, String> map : fieldMap) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (entry.getKey().compareTo(uniqueKeySyntax) == 0) {
                    AbstractConstant uniqueKeyValue1 = mainRegistry.getConstantFromName(entry.getValue());
                    if (uniqueKeyValue1.getValue().equals(uniqueKeyValue)) {
                        correctKey = true;
                        break;
                    }
                }
            }

            if (correctKey) {
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    if (entry.getKey().equals(componentName)) {
                        AbstractTranslator abstractTranslator;
                        if (settingsMap.containsKey(componentName)) {
                            abstractTranslator = mainRegistry.getTranslator(settingsMap.get(componentName));
                        } else {
                            abstractTranslator = mainRegistry.getTranslatorFromName(entry.getValue());
                        }
                        if (null != abstractTranslator) {
                            return abstractTranslator;
                        } else {
                            abstractTranslator = classtype.getTypeTranslator(entry.getKey());
                            if (abstractTranslator == null) {
                                throw new RuntimeException(entry.getKey());
                            }
                            return abstractTranslator;
                        }
                    }
                }
            }
            correctKey = false;
        }
        return null;
    }
}