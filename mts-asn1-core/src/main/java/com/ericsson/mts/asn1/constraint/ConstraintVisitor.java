/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.ericsson.mts.asn1.constraint;

import com.ericsson.mts.asn1.ASN1Parser;
import com.ericsson.mts.asn1.ASN1ParserBaseVisitor;
import com.ericsson.mts.asn1.exception.ANTLRVisitorException;
import com.ericsson.mts.asn1.exception.NotHandledCaseException;
import com.ericsson.mts.asn1.registry.MainRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ConstraintVisitor {
    //Wrapper
    private InnerConstraintVisitor innerConstraintVisitor;

    ConstraintVisitor(MainRegistry mainRegistry) {
        innerConstraintVisitor = new InnerConstraintVisitor(mainRegistry);
    }

    /**
     * Call constraintVisitor which will parse and append constraint
     *
     * @param constraintContext context of the constraint
     */
    void addConstraint(ASN1Parser.ConstraintContext constraintContext, Constraints constraints) {
        innerConstraintVisitor.addConstraint(constraintContext, constraints);
    }

    /**
     * Use by SET OF and SEQUENCE OF keywords according to X.680 49.5
     *
     * @param sizeConstraintContext context of the constraint
     * @param constraints           Constraints to store SizeConstraint
     */
    void addSizeConstraint(ASN1Parser.SizeConstraintContext sizeConstraintContext, Constraints constraints) {
        innerConstraintVisitor.addSizeConstraint(sizeConstraintContext, constraints);
    }

    /**
     * Parse a given context constraint and had to Constraints object
     */
    private class InnerConstraintVisitor extends ASN1ParserBaseVisitor<Void> {
        private final Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());
        private final MainRegistry mainRegistry;
        private TypeConstraint typeConstraint;
        private AbstractConstraint abstractConstraint;
        private Constraints constraints;
        private boolean nextConstraintIsExtensible = false;

        InnerConstraintVisitor(MainRegistry mainRegistry) {
            this.mainRegistry = mainRegistry;
        }

        void addConstraint(ASN1Parser.ConstraintContext constraintContext, Constraints constraints) {
            this.constraints = constraints;
            visitConstraint(constraintContext);
            resetAttributes();
        }

        void addSizeConstraint(ASN1Parser.SizeConstraintContext sizeConstraintContext, Constraints constraints) {
            this.constraints = constraints;
            this.visitSizeConstraint(sizeConstraintContext);
            if (!constraints.hasSizeConstraint()) {
                throw new RuntimeException();
            }
            resetAttributes();
        }

        private void resetAttributes() {
            typeConstraint = null;
            abstractConstraint = null;
            constraints = null;
            nextConstraintIsExtensible = false;
        }

        @Override
        public Void visitConstraint(ASN1Parser.ConstraintContext ctx) {
            if (ctx.exceptionSpec() != null) {
                throw new ANTLRVisitorException("exceptionSpec");
            }
            return super.visitConstraint(ctx);
        }

        @Override
        public Void visitGeneralConstraint(ASN1Parser.GeneralConstraintContext ctx) {
            if (ctx.contentsConstraint() == null && ctx.tableConstraint() == null)
                throw new ANTLRVisitorException("userDefinedConstraint");
            return super.visitGeneralConstraint(ctx);
        }

        @Override
        public Void visitContentsConstraint(ASN1Parser.ContentsConstraintContext ctx) {
            if (ctx.CONTAINING_LITERAL() != null && ctx.ENCODED_LITERAL() == null) {
                abstractConstraint = new ContentsConstraint(mainRegistry.getTranslator(ctx.asnType()));
                constraints.addContentConstraint(abstractConstraint);
                return null;
            } else {
                throw new ANTLRVisitorException("in contentsConstraint");
            }
        }


        @Override
        public Void visitElementSetSpecs(ASN1Parser.ElementSetSpecsContext ctx) {
            if (ctx.COMMA(0) != null) {
                nextConstraintIsExtensible = true;
                if (null != ctx.COMMA(1)) {
                    throw new ANTLRVisitorException("additionalElementSetSpec");
                }
            }
            return super.visitElementSetSpecs(ctx);

        }


        @Override
        public Void visitComponentRelationConstraint(ASN1Parser.ComponentRelationConstraintContext ctx) {
            if (ctx.IDENTIFIER().size() != 1 || ctx.atNotation().size() > 1) {
                throw new NotHandledCaseException();
            } else {
                ClassFieldConstraint classFieldConstraint;
                if (ctx.atNotation().size() == 0) {
                    classFieldConstraint = new ClassFieldConstraint(ctx.IDENTIFIER(0).getText(), null);
                } else {
                    classFieldConstraint = new ClassFieldConstraint(ctx.IDENTIFIER(0).getText(), ctx.atNotation(0).componentIdList().getText());
                }
                constraints.addClassFieldConstraint(classFieldConstraint);
                return null;
            }
        }

        @Override
        public Void visitUnions(ASN1Parser.UnionsContext ctx) {
            return super.visitUnions(ctx);
        }

        @Override
        public Void visitIntersections(ASN1Parser.IntersectionsContext ctx) {
            if (ctx.intersectionMark(0) != null) {
                throw new NotHandledCaseException();
            }
            boolean isExtensible = (abstractConstraint != null && abstractConstraint.isExtensible());
            super.visitIntersections(ctx);
            if (abstractConstraint == null) {
                throw new RuntimeException();
            }

            if (isExtensible) {
                abstractConstraint.setExtensible(true);
            } else if (nextConstraintIsExtensible) {
                abstractConstraint.setExtensible(true);
                nextConstraintIsExtensible = false;
            }

            if (typeConstraint == TypeConstraint.SIZE_CONSTRAINT) {
                return null;
            } else if (typeConstraint == TypeConstraint.VALUE_RANGE_CONSTRAINT) {
                constraints.addValueRangeConstraint(abstractConstraint);
            } else if (typeConstraint == TypeConstraint.SINGLE_VALUE_CONSTRAINT) {
                constraints.addSingleValueConstraint(abstractConstraint);
            } else {
                throw new NotHandledCaseException();
            }
            abstractConstraint = null;
            typeConstraint = null;
            return null;
        }

        @Override
        public Void visitSubtypeElements(ASN1Parser.SubtypeElementsContext ctx) {
            if (ctx.DOUBLE_DOT() != null) {
                //ValueRange
                AbstractRangeConstraint rangeConstraint;
                if (typeConstraint == TypeConstraint.SIZE_CONSTRAINT) {
                    rangeConstraint = (SizeConstraint) abstractConstraint;
                } else {
                    rangeConstraint = new ValueRangeConstraint(mainRegistry);
                    typeConstraint = TypeConstraint.VALUE_RANGE_CONSTRAINT;
                }

                if (ctx.MIN_LITERAL() != null) {
                    rangeConstraint.setLowerBound(null, true);
                } else {
                    if (ctx.value(0).builtinValue().integerValue() != null) {
                        rangeConstraint.setLowerBound((ctx.value(0).getText()), true);
                    } else if (ctx.value(0).builtinValue().enumeratedValue() != null) {
                        rangeConstraint.setLowerBound((ctx.value(0).getText()), false);
                    } else {
                        throw new ANTLRVisitorException(ctx.value(0).builtinValue().getText());
                    }
                }

                if (ctx.LESS_THAN().size() != 0) {
                    throw new ANTLRVisitorException();
                }

                if (ctx.MAX_LITERAL() != null) {
                    rangeConstraint.setUpperBound(null, true);
                } else {
                    if (ctx.MIN_LITERAL() != null) {
                        if (ctx.value(0).builtinValue().integerValue() != null) {
                            rangeConstraint.setUpperBound((ctx.value(0).getText()), true);
                        } else if (ctx.value(0).builtinValue().enumeratedValue() != null) {
                            rangeConstraint.setUpperBound(ctx.value(0).builtinValue().getText(), false);
                        } else {
                            throw new ANTLRVisitorException(ctx.value(0).builtinValue().getText());
                        }
                    } else {
                        if (ctx.value(1).builtinValue().integerValue() != null) {
                            rangeConstraint.setUpperBound((ctx.value(1).getText()), true);
                        } else if (ctx.value(1).builtinValue().enumeratedValue() != null) {
                            rangeConstraint.setUpperBound(ctx.value(1).builtinValue().getText(), false);
                        } else {
                            throw new ANTLRVisitorException(ctx.value(1).builtinValue().getText());
                        }
                    }
                }
                abstractConstraint = rangeConstraint;
                return null;
            } else if (ctx.sizeConstraint() != null) {
                //SizeConstraint
                return visitSizeConstraint(ctx.sizeConstraint());
            } else if (ctx.PATTERN_LITERAL() != null) {
                //PatternConstraint
                throw new ANTLRVisitorException();
            } else if (ctx.value(0) != null) {
                if (typeConstraint == TypeConstraint.SIZE_CONSTRAINT) {
                    SizeConstraint sizeConstraint = (SizeConstraint) abstractConstraint;
                    if (ctx.value(0).builtinValue().integerValue() != null) {
                        sizeConstraint.setLowerBound((ctx.value(0).getText()), true);
                    } else if (ctx.value(0).builtinValue().enumeratedValue() != null) {
                        sizeConstraint.setLowerBound(mainRegistry.getConstant(ctx.value(0).builtinValue()).getValue(), false);
                    } else {
                        throw new ANTLRVisitorException(ctx.value(0).builtinValue().getText());
                    }
                    sizeConstraint.setUpperBound(null, true);
                    return null;
                } else {
                    //SingleValue
                    SingleValueConstraint singleValueConstraint = new SingleValueConstraint(mainRegistry);
                    if (ctx.value(0).builtinValue().integerValue() != null) {
                        singleValueConstraint.setValue((ctx.value(0).getText()), true);
                    } else if (ctx.value(0).builtinValue().enumeratedValue() != null) {
                        singleValueConstraint.setValue(mainRegistry.getConstant(ctx.value(0).builtinValue()).getValue(), false);
                    } else {
                        throw new ANTLRVisitorException(ctx.value(0).builtinValue().getText());
                    }
                    typeConstraint = TypeConstraint.SINGLE_VALUE_CONSTRAINT;
                    abstractConstraint = singleValueConstraint;
                    return null;
                }
            } else {
                throw new ANTLRVisitorException();
            }
        }

        @Override
        public Void visitSizeConstraint(ASN1Parser.SizeConstraintContext ctx) {
            //WARNING ! Check sequenceOfType and AbstractSequenceOfTranslator before any change here
            abstractConstraint = new SizeConstraint(mainRegistry);
            typeConstraint = TypeConstraint.SIZE_CONSTRAINT;
            super.visitSizeConstraint(ctx);
            if (typeConstraint != TypeConstraint.SIZE_CONSTRAINT) {
                throw new RuntimeException();
            }
            constraints.addSizeConstraint(abstractConstraint);
            return null;
        }
    }
}

