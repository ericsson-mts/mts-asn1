/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.ericsson.mts.asn1.registry;

import com.ericsson.mts.asn1.ASN1Parser;
import com.ericsson.mts.asn1.classhandler.ClassHandler;
import com.ericsson.mts.asn1.classhandler.ClassObject;
import com.ericsson.mts.asn1.classhandler.ClassObjectSet;
import com.ericsson.mts.asn1.constant.AbstractConstant;
import com.ericsson.mts.asn1.constant.IntegerConstant;
import com.ericsson.mts.asn1.exception.NotHandledCaseException;
import com.ericsson.mts.asn1.factory.AbstractTranslatorFactory;
import com.ericsson.mts.asn1.translator.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Parse(lazy parsing) and store all ASN.1 objects.
 */
public class MainRegistry {
    private AbstractTranslatorFactory abstractTranslatorFactory;
    private IndexingRegistry indexingRegistry = new IndexingRegistry();

    //Constants
    private ParsedRegistry<AbstractConstant> valueTranslatorParsedRegistry = new ParsedRegistry<>();
    //Translator
    private ParsedRegistry<AbstractTranslator> typeTranslatorParsedRegistry = new ParsedRegistry<>();
    //Class
    private ParsedRegistry<ClassHandler> classHandlerParsedRegistry = new ParsedRegistry<>();
    //Object
    private ParsedRegistry<ClassObject> classObjectParsedRegistry = new ParsedRegistry<>();
    //Object set
    private ParsedRegistry<ClassObjectSet> classObjectSetParsedRegistry = new ParsedRegistry<>();

    private Logger logger = LoggerFactory.getLogger(MainRegistry.class.getSimpleName());

    public MainRegistry(AbstractTranslatorFactory abstractTranslatorFactory) {
        this.abstractTranslatorFactory = abstractTranslatorFactory;
    }

    public void addAssignment(ASN1Parser.AssignmentContext assignmentContext) {
        indexingRegistry.addAssignment(assignmentContext);
    }

    public boolean checkIndexingRegistry() {
        return indexingRegistry.checkRegistry();
    }
    //Constants

    public AbstractConstant getConstantFromName(String identifier) {
        AbstractConstant abstractConstant = valueTranslatorParsedRegistry.get(identifier);
        if (null != abstractConstant) {
            return abstractConstant;
        }

        ASN1Parser.ValueAssignmentContext valueAssignmentContext = indexingRegistry.getConstantContext(identifier);
        abstractConstant = createConstant(identifier, valueAssignmentContext);
        valueTranslatorParsedRegistry.add(identifier, abstractConstant);
        return abstractConstant;
    }

    public AbstractConstant getConstant(ASN1Parser.BuiltinValueContext builtinValueContext) throws NotHandledCaseException {
        if (null != builtinValueContext.integerValue()) {
            //Primitive case
            return new IntegerConstant().init(builtinValueContext.integerValue().getText());
        } else if (null != builtinValueContext.enumeratedValue()) {
            //Reference case
            String identifier = builtinValueContext.enumeratedValue().IDENTIFIER().getText();
            return getConstantFromName(identifier);
        } else {
            throw new NotHandledCaseException(builtinValueContext.getChild(0).getClass().getSimpleName());
        }
    }

    private AbstractConstant createConstant(String identifier, ASN1Parser.ValueAssignmentContext valueAssignmentContext) {
        if (valueAssignmentContext.value().builtinValue().integerValue() != null) {
            return new IntegerConstant().init(valueAssignmentContext.value().builtinValue().integerValue().getText());
        } else {
            throw new NotHandledCaseException(identifier);
        }
    }

    public void parseConstants() {
        List<String> identifiers = indexingRegistry.getConstantsIdentifier();
        for (String identifier : identifiers) {
            if (null == getConstantFromName(identifier)) {
                throw new RuntimeException("Identifier : " + identifier);
            }
        }
    }

    //Translators

    public synchronized AbstractTranslator getTranslatorFromName(final String identifier) {
        AbstractTranslator abstractTranslator = typeTranslatorParsedRegistry.get(identifier);
        if (null != abstractTranslator) {
            return abstractTranslator;
        }

        ASN1Parser.TypeAssignmentContext typeAssignmentContext = indexingRegistry.getTranslatorContext(identifier);
        if (typeAssignmentContext != null) {
            abstractTranslator = getTranslator(typeAssignmentContext.asnType());
            abstractTranslator.setName(identifier);
            typeTranslatorParsedRegistry.add(identifier, abstractTranslator);
            return abstractTranslator;
        }

        ASN1Parser.ParameterizedAssignmentContext parameterizedAssignmentContext = indexingRegistry.getParameterizedAssignementContext(identifier);
        if (parameterizedAssignmentContext != null) {
            if (parameterizedAssignmentContext.asnType() != null) {
                abstractTranslator = createTranslator(identifier, parameterizedAssignmentContext.asnType(), parameterizedAssignmentContext.parameterList());
                typeTranslatorParsedRegistry.add(identifier, abstractTranslator);
                return abstractTranslator;
            } else {
                throw new NotHandledCaseException(parameterizedAssignmentContext.getChild(2).getClass().getSimpleName());
            }
        }
        throw new RuntimeException("Can't find translator : " + identifier);
    }

    public AbstractTranslator getTranslator(ASN1Parser.AsnTypeContext asnTypeContext) {
        if (asnTypeContext.builtinType() != null) {
            return createTranslator(null, asnTypeContext.builtinType(), asnTypeContext.constraint());
        } else {
            String identifier = asnTypeContext.referencedType().definedType().IDENTIFIER(0).getText();
            AbstractTranslator abstractTranslator = typeTranslatorParsedRegistry.get(identifier);
            if (null != abstractTranslator) {
                return abstractTranslator;
            }

            ASN1Parser.TypeAssignmentContext typeAssignmentContext = indexingRegistry.getTranslatorContext(identifier);

            if (typeAssignmentContext == null) {
                if (asnTypeContext.referencedType().definedType().DOT() != null) {
                    throw new NotHandledCaseException();
                }

                if (asnTypeContext.referencedType().definedType().actualParameterList() != null) {
                    identifier = asnTypeContext.referencedType().definedType().IDENTIFIER(0).getText();
                }
                ASN1Parser.ParameterizedAssignmentContext parameterizedAssignmentContext = indexingRegistry.getParameterizedAssignementContext(identifier);
                if (parameterizedAssignmentContext != null) {
                    if (parameterizedAssignmentContext.asnType() != null) {
                        abstractTranslator = createTranslator(identifier, parameterizedAssignmentContext.asnType(), parameterizedAssignmentContext.parameterList());
                        typeTranslatorParsedRegistry.add(identifier, abstractTranslator);
                        return abstractTranslator;
                    } else {
                        throw new NotHandledCaseException(parameterizedAssignmentContext.getChild(2).getClass().getSimpleName());
                    }
                }
                throw new NullPointerException(identifier);
            }
            if (typeAssignmentContext.asnType().builtinType() != null) {
                abstractTranslator = createTranslator(identifier, typeAssignmentContext.asnType().builtinType(), typeAssignmentContext.asnType().constraint());
            } else {
                abstractTranslator = getTranslator(typeAssignmentContext.asnType());
            }
            typeTranslatorParsedRegistry.add(identifier, abstractTranslator);
            return abstractTranslator;
        }
    }

    private AbstractTranslator createTranslator(String translatorIdentifier, ASN1Parser.AsnTypeContext asnTypeContext, ASN1Parser.ParameterListContext parameterListContext) {
        if (asnTypeContext.builtinType() != null) {
            if (asnTypeContext.builtinType().sequenceOfType() != null) {
                AbstractSequenceOfTranslator abstractTranslator = abstractTranslatorFactory.sequenceOfTranslator();
                abstractTranslator.setName(translatorIdentifier);
                return abstractTranslator.init(this, asnTypeContext.builtinType().sequenceOfType(), parameterListContext);

            } else if (asnTypeContext.builtinType().sequenceType() != null) {
                AbstractSequenceTranslator abstractSequenceTranslator = abstractTranslatorFactory.sequenceTranslator();
                abstractSequenceTranslator.setName(translatorIdentifier);
                return abstractSequenceTranslator.init(this, asnTypeContext.builtinType().sequenceType(), parameterListContext);
            } else {
                throw new NotHandledCaseException(asnTypeContext.getText());
            }
        } else {
            ReferenceTranslator referenceTranslator = new ReferenceTranslator();
            referenceTranslator.setName(translatorIdentifier);
            return referenceTranslator.init(this, asnTypeContext.referencedType(), parameterListContext);
        }
    }

    private AbstractTranslator createTranslator(String identifier, ASN1Parser.BuiltinTypeContext builtinTypeContext, List<ASN1Parser.ConstraintContext> constraintContexts) throws NotHandledCaseException {
        if (builtinTypeContext.octetStringType() != null) {
            AbstractOctetStringTranslator octetStringTranslator = abstractTranslatorFactory.octetStringTranslator();
            octetStringTranslator.setName(identifier);
            return octetStringTranslator.init(this, constraintContexts);
        } else if (builtinTypeContext.bitStringType() != null) {
            AbstractBitStringTranslator bitStringTranslator = abstractTranslatorFactory.bitStringTranslator();
            bitStringTranslator.setName(identifier);
            return bitStringTranslator.init(builtinTypeContext.bitStringType(), this, constraintContexts);
        } else if (builtinTypeContext.choiceType() != null) {
            AbstractChoiceTranslator choiceTranslator = abstractTranslatorFactory.choiceTranslator();
            choiceTranslator.setName(identifier);
            return choiceTranslator.init(this, builtinTypeContext.choiceType());
        } else if (builtinTypeContext.enumeratedType() != null) {
            AbstractEnumeratedTranslator enumeratedTranslator = abstractTranslatorFactory.enumeratedTranslator();
            enumeratedTranslator.setName(identifier);
            return enumeratedTranslator.init(builtinTypeContext.enumeratedType());
        } else if (builtinTypeContext.integerType() != null) {
            AbstractIntegerTranslator integerTranslator = abstractTranslatorFactory.integerTranslator();
            integerTranslator.setName(identifier);
            return integerTranslator.init(this, builtinTypeContext.integerType(), constraintContexts);
        } else if (builtinTypeContext.sequenceType() != null) {
            AbstractSequenceTranslator sequenceTranslator = abstractTranslatorFactory.sequenceTranslator();
            sequenceTranslator.setName(identifier);
            return sequenceTranslator.init(this, builtinTypeContext.sequenceType());
        } else if (builtinTypeContext.sequenceOfType() != null) {
            AbstractSequenceOfTranslator sequenceOfTranslator = abstractTranslatorFactory.sequenceOfTranslator();
            sequenceOfTranslator.setName(identifier);
            return sequenceOfTranslator.init(this, builtinTypeContext.sequenceOfType());
        } else if (builtinTypeContext.objectClassFieldType() != null) {
            AbstractObjectClassFieldTranslator objectClassFieldTranslator = abstractTranslatorFactory.objectClassFieldTypeTranslator();
            objectClassFieldTranslator.setName(identifier);
            return objectClassFieldTranslator.init(this, builtinTypeContext.objectClassFieldType(), constraintContexts);
        } else if (builtinTypeContext.characterStringType() != null) {
            AbstractRestrictedCharacterStringTranslator restrictedCharacterStringTranslator = abstractTranslatorFactory.characterStringTranslator();
            restrictedCharacterStringTranslator.setName(identifier);
            return restrictedCharacterStringTranslator.init(this, builtinTypeContext.characterStringType(), constraintContexts);
        } else if (builtinTypeContext.realType() != null) {
            AbstractRealTranslator realTranslator = abstractTranslatorFactory.realTranslator();
            realTranslator.setName(identifier);
            return realTranslator;
        } else if (builtinTypeContext.BOOLEAN_LITERAL() != null) {
            AbstractBooleanTranslator abstractBooleanTranslator = abstractTranslatorFactory.booleanTranslator();
            abstractBooleanTranslator.setName(identifier);
            return abstractBooleanTranslator;
        } else if (builtinTypeContext.NULL_LITERAL() != null) {
            NullTranslator nullTranslator = abstractTranslatorFactory.nullTranslator();
            nullTranslator.setName(identifier);
            return nullTranslator;
        } else if (builtinTypeContext.objectidentifiertype() != null) {
            AbstractObjectIdentifierTranslator objectIdentifierTranslator = abstractTranslatorFactory.objectIdentifierTranslator();
            objectIdentifierTranslator.setName(identifier);
            return objectIdentifierTranslator;
        } else {
            throw new NotHandledCaseException("Can't create a translator for " + builtinTypeContext.getText());
        }
    }

    public void parseTranslators() {
        List<String> identifiers = indexingRegistry.getTranslatorsIdentifier();
        for (String identifier : identifiers) {
            if (null == getTranslatorFromName(identifier)) {
                throw new RuntimeException("Identifier : " + identifier);
            }
        }

        identifiers = indexingRegistry.getParameterizedTranslatorsIdentifier();
        for (String identifier : identifiers) {
            if (null == getTranslatorFromName(identifier)) {
                throw new RuntimeException("Identifier : " + identifier);
            }
        }
    }

    // Objects

    public ClassObject getClassObject(String identifier) {
        ClassObject classObject = classObjectParsedRegistry.get(identifier);
        if (null != classObject) {
            return classObject;
        }

        ASN1Parser.ObjectAssignmentContext objectAssignmentContext = indexingRegistry.getObjectContext(identifier);

        if (objectAssignmentContext == null) {
            return null;
        }
        classObject = new ClassObject().init(this, objectAssignmentContext);
        classObjectParsedRegistry.add(identifier, classObject);
        return classObject;
    }

    public void parseClassObject() {
        List<String> identifiers = indexingRegistry.getObjectsContextdentifier();
        for (String identifier : identifiers) {
            if (null == getClassObject(identifier)) {
                throw new RuntimeException("Identifier : " + identifier);
            }
        }
    }

    //Object Sets

    public ClassObjectSet getClassObjectSet(String identifier) {
        ClassObjectSet classObjectSet = classObjectSetParsedRegistry.get(identifier);
        if (null != classObjectSet) {
            return classObjectSet;
        }

        ASN1Parser.ParameterizedAssignmentContext parameterizedAssignmentContext = indexingRegistry.getParameterizedAssignementContext(identifier);
        if (null == parameterizedAssignmentContext) {
            throw new NullPointerException("Can't find context for " + identifier);
        }

        if (null == parameterizedAssignmentContext.objectSet() || null == parameterizedAssignmentContext.definedObjectClass()) {
            throw new RuntimeException(identifier + " is not an object set");
        } else {
            classObjectSet = new ClassObjectSet().init(this, this.getClassHandler(parameterizedAssignmentContext.definedObjectClass().getText()), parameterizedAssignmentContext.objectSet());
            classObjectSetParsedRegistry.add(identifier, classObjectSet);
            return classObjectSet;
        }
    }

    public void parseClassObjectSet() {
        List<String> identifiers = indexingRegistry.getObjectSetAssignment();
        for (String identifier : identifiers) {
            if (null == getClassObjectSet(identifier)) {
                throw new RuntimeException("Identifier : " + identifier);
            }
        }
    }

    //Classes

    public ClassHandler getClassHandler(String identifier) {
        if (null == identifier) {
            throw new RuntimeException("Invalid identifier");
        }
        ClassHandler classHandler = classHandlerParsedRegistry.get(identifier);
        if (null != classHandler) {
            return classHandler;
        }

        classHandler = createClassHandler(indexingRegistry.getClassHandlerContext(identifier));
        classHandlerParsedRegistry.add(identifier, classHandler);
        return classHandler;
    }

    private ClassHandler createClassHandler(ASN1Parser.ObjectClassAssignmentContext objectClassAssignmentContext) {
        return new ClassHandler(this, objectClassAssignmentContext.objectClass().objectClassDefn());
    }

    public void parseClassHandler() {
        List<String> identifiers = indexingRegistry.getClassHandlersdentifier();
        for (String identifier : identifiers) {
            if (null == getClassHandler(identifier)) {
                throw new RuntimeException("Identifier : " + identifier);
            }
        }
    }
}
