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
import java.util.HashMap;

public class ClassObjectSet {
    private MainRegistry mainRegistry;
    private ClassHandler classType;
    private Logger logger = LoggerFactory.getLogger(ClassObjectSet.class.getSimpleName());
    private ArrayList<String> objects = new ArrayList<>(); //object nor object set
    private HashMap<String, ClassObject> localObjects = new HashMap<>();
    private boolean hasEllipsis = false;

    /**
     * Initialize objectSet by parsing associate context
     *
     * @param mainRegistry     main registry
     * @param classHandler     object set class
     * @param objectSetContext context
     * @return object set initialized
     */
    public ClassObjectSet init(MainRegistry mainRegistry, ClassHandler classHandler, ASN1Parser.ObjectSetContext objectSetContext) {
        this.mainRegistry = mainRegistry;
        this.classType = classHandler;
        if (objectSetContext.objectSetSpec().rootElementSetSpec() != null) {
            parseElementSetSpecContext(objectSetContext.objectSetSpec().rootElementSetSpec().elementSetSpec());
        }

        if (objectSetContext.objectSetSpec().ELLIPSIS() != null) {
            hasEllipsis = true;
        }

        if (objectSetContext.objectSetSpec().additionalElementSetSpec() != null) {
            parseElementSetSpecContext(objectSetContext.objectSetSpec().additionalElementSetSpec().elementSetSpec());
        }
        return this;
    }

    /**
     * Parse context
     * @param elementSetSpecContext context
     */
    private void parseElementSetSpecContext(ASN1Parser.ElementSetSpecContext elementSetSpecContext) {
        if (elementSetSpecContext.unions() != null) {
            for (ASN1Parser.IntersectionsContext intersectionsContext : elementSetSpecContext.unions().intersections()) {
                if (intersectionsContext.intersectionMark().size() == 0) {
                    if (intersectionsContext.intersectionElements(0).exclusions() == null) {
                        if (intersectionsContext.intersectionElements(0).elements().objectSetElements() != null) {
                            if (intersectionsContext.intersectionElements(0).elements().objectSetElements().object().definedObject() != null) {
                                objects.add(intersectionsContext.intersectionElements(0).elements().objectSetElements().object().definedObject().IDENTIFIER().getText());
                            } else {
                                ClassObject classObject = new ClassObject();
                                classObject.buildLocalObject(mainRegistry, classType, intersectionsContext.intersectionElements(0).elements().objectSetElements().object().objectDefn());
                                localObjects.put(String.valueOf(localObjects.size()), classObject);
                                objects.add(String.valueOf(localObjects.size() - 1));
                            }
                        } else {
                            throw new NotHandledCaseException("subtypeElements");
                        }
                    } else {
                        throw new NotHandledCaseException("intersectionElements : exclusions");
                    }
                } else {
                    throw new NotHandledCaseException("intersections : More than one intersectionElements");
                }
            }
        } else {
            throw new NotHandledCaseException("elementSetSpec : ... | exclusions");
        }
    }

    /**
     * Open type : Return translator for a given key and name
     * @param uniqueKey unique key value
     * @param componentName translator name
     * @return target translator
     */
    public AbstractTranslator getTranslatorForField(String uniqueKey, String componentName) {
        ClassObject classObject;
        ClassObjectSet classObjectSet;
        AbstractTranslator abstractTranslator;
        //Loop
        for (String object : objects) {
            //Check if the object is local
            classObject = localObjects.get(object);
            if (classObject == null) {
                classObject = mainRegistry.getClassObject(object);
            }
            if (classObject != null) {
                abstractTranslator = classObject.getTranslatorFromUniqueKey(classType.getUniqueKeyName(), uniqueKey, componentName);
                if (abstractTranslator != null) {
                    return abstractTranslator;
                }
            } else {
                //if the object is an object set
                classObjectSet = mainRegistry.getClassObjectSet(object);
                if (classObjectSet != null) {
                    abstractTranslator = classObjectSet.getTranslatorForField(uniqueKey, componentName);
                    if (abstractTranslator != null) {
                        return abstractTranslator;
                    }
                } else {
                    throw new RuntimeException("Can't find object " + object + " in class object set " + toString());
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "ClassObjectSet{" +
                " classType=" + classType +
                ", objects=" + objects +
                ", hasEllipsis=" + hasEllipsis +
                '}';
    }
}
