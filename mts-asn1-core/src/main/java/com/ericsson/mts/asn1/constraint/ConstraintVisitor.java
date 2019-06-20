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

    void addConstraint(ASN1Parser.ConstraintContext constraintContext, Constraints constraints) {
        innerConstraintVisitor.addConstraint(constraintContext, constraints);
    }

    void addSizeConstraint(ASN1Parser.SizeConstraintContext sizeConstraintContext, Constraints constraints) {
        innerConstraintVisitor.addSizeConstraint(sizeConstraintContext, constraints);
    }

    private class InnerConstraintVisitor extends ASN1ParserBaseVisitor<Void> {
        private final Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());
        private final MainRegistry mainRegistry;
        private TypeConstraint typeConstraint;
        private AbstractConstraint abstractConstraint;
        private Constraints constraints;

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
            super.visitElementSetSpecs(ctx);
            if (ctx.COMMA(0) != null) {
                abstractConstraint.setExtensible(true);
                if (null != ctx.COMMA(1)) {
                    throw new ANTLRVisitorException("additionalElementSetSpec");
                }
            }
            return null;
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
//                abstractConstraint = classFieldConstraint;
//                typeConstraint = TypeConstraint.CLASS_FIELD_CONSTRAINT;
                return null;
            }
        }

        @Override
        public Void visitUnions(ASN1Parser.UnionsContext ctx) {
            if (ctx.unionMark(0) != null) {
                throw new NotHandledCaseException(ctx.getText());
            }
            return super.visitUnions(ctx);
        }

        @Override
        public Void visitIntersections(ASN1Parser.IntersectionsContext ctx) {
            if (ctx.intersectionMark(0) != null) {
                //Reset typeConstraint if change here
                throw new NotHandledCaseException();
            }
            super.visitIntersections(ctx);
            if (abstractConstraint == null) {
                throw new RuntimeException();
            }

            if (typeConstraint == TypeConstraint.SIZE_CONSTRAINT) {
                constraints.addSizeConstraint(abstractConstraint);
            } else if (typeConstraint == TypeConstraint.VALUE_RANGE_CONSTRAINT) {
                constraints.addValueRangeConstraint(abstractConstraint);
            } else {
                throw new NotHandledCaseException();
            }

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
                //SingleValue
                SizeConstraint sizeConstraint = new SizeConstraint(mainRegistry);
                if (ctx.value(0).builtinValue().integerValue() != null) {
                    sizeConstraint.setLowerBound((ctx.value(0).getText()), true);
                } else if (ctx.value(0).builtinValue().enumeratedValue() != null) {
                    sizeConstraint.setLowerBound(mainRegistry.getConstant(ctx.value(0).builtinValue()).getValue(), false);
                } else {
                    throw new ANTLRVisitorException(ctx.value(0).builtinValue().getText());
                }
                typeConstraint = TypeConstraint.SIZE_CONSTRAINT;
                abstractConstraint = sizeConstraint;
                return null;
            } else {
                throw new ANTLRVisitorException();
            }
        }

        @Override
        public Void visitSizeConstraint(ASN1Parser.SizeConstraintContext ctx) {
            //Check sequenceOfType and AbstractSequenceOfTranslator before any change here
            abstractConstraint = new SizeConstraint(mainRegistry);
            typeConstraint = TypeConstraint.SIZE_CONSTRAINT;
            return super.visitSizeConstraint(ctx);
        }
    }
}

