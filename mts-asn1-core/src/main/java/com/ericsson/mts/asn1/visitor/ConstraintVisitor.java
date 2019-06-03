/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.ericsson.mts.asn1.visitor;

import com.ericsson.mts.asn1.ASN1Parser;
import com.ericsson.mts.asn1.ASN1ParserBaseVisitor;
import com.ericsson.mts.asn1.constraint.AbstractConstraint;
import com.ericsson.mts.asn1.constraint.ClassFieldConstraint;
import com.ericsson.mts.asn1.constraint.SizeConstraint;
import com.ericsson.mts.asn1.exception.NotHandledCaseException;
import com.ericsson.mts.asn1.registry.MainRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

public class ConstraintVisitor extends ASN1ParserBaseVisitor<AbstractConstraint> {
    public final static String SIZE_CONSTRAINT = "SIZE_CONSTRAINT";
    public final static String CLASS_FIELD_CONSTRAINT = "CLASS_FIELD_CONSTRAINT";
    protected Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());
    private String constraintType;
    private AbstractConstraint abstractConstraint;
    private MainRegistry mainRegistry;

    public ConstraintVisitor(String constraintType, MainRegistry mainRegistry) {
        this.constraintType = constraintType;
        this.mainRegistry = mainRegistry;
    }

    @Override
    public AbstractConstraint visitConstraint(ASN1Parser.ConstraintContext ctx) {
        super.visitConstraint(ctx);
        return abstractConstraint;
    }

    @Override
    public AbstractConstraint visitGeneralConstraint(ASN1Parser.GeneralConstraintContext ctx) {
        if (ctx.tableConstraint() == null)
            throw new NotHandledCaseException();
        return super.visitGeneralConstraint(ctx);
    }

    @Override
    public AbstractConstraint visitComponentRelationConstraint(ASN1Parser.ComponentRelationConstraintContext ctx) {
        if (ctx.IDENTIFIER().size() != 1 || ctx.atNotation().size() > 1) {
            throw new NotHandledCaseException();
        } else {
            if (constraintType.compareTo(CLASS_FIELD_CONSTRAINT) == 0) {
                if (ctx.atNotation().size() == 0) {
                    abstractConstraint = new ClassFieldConstraint(ctx.IDENTIFIER(0).getText(), null);
                } else {
                    abstractConstraint = new ClassFieldConstraint(ctx.IDENTIFIER(0).getText(), ctx.atNotation(0).componentIdList().getText());
                }
                return super.visitComponentRelationConstraint(ctx);
            } else {
                throw new NotHandledCaseException();
            }
        }
    }

    @Override
    public AbstractConstraint visitElementSetSpecs(ASN1Parser.ElementSetSpecsContext ctx) {
        AbstractConstraint abstractConstraint1 = super.visitElementSetSpecs(ctx);
        if (ctx.COMMA(0) != null) {
            if (constraintType.compareTo(ConstraintVisitor.SIZE_CONSTRAINT) == 0) {
                abstractConstraint.setExtensible(true);
            }
            if (null != ctx.COMMA(1)) {
                throw new NotHandledCaseException(ctx.getText());
            }
        }
        return abstractConstraint1;
    }

    @Override
    public AbstractConstraint visitUnions(ASN1Parser.UnionsContext ctx) {
        if (ctx.unionMark(0) != null) {
            throw new NotHandledCaseException();
        }
        return super.visitUnions(ctx);
    }

    @Override
    public AbstractConstraint visitIntersections(ASN1Parser.IntersectionsContext ctx) {
        if (ctx.intersectionMark(0) != null) {
            throw new NotHandledCaseException();
        }
        return super.visitIntersections(ctx);
    }

    @Override
    public AbstractConstraint visitSubtypeElements(ASN1Parser.SubtypeElementsContext ctx) {
        if (!(ctx.value().isEmpty())) {
            if (constraintType.compareTo(ConstraintVisitor.SIZE_CONSTRAINT) == 0) {
                SizeConstraint sizeConstraint = new SizeConstraint();
                if (ctx.value(0).builtinValue().integerValue() != null && ctx.MIN_LITERAL() == null) {
                    sizeConstraint.setLower_bound(new BigInteger(ctx.value(0).getText()));
                } else if (ctx.value(0).builtinValue().enumeratedValue() != null) {
                    sizeConstraint.setLower_bound(new BigInteger(ctx.value(0).getText()));
                } else {
                    throw new NotHandledCaseException(ctx.getText());
                }
                if (ctx.value(1) != null) {
                    if (ctx.value(1).builtinValue().integerValue() != null) {
                        sizeConstraint.setUpper_bound(new BigInteger(ctx.value(1).getText()));
                    } else if (ctx.value(1).builtinValue().enumeratedValue() != null) {
                        sizeConstraint.setUpper_bound(new BigInteger(mainRegistry.getConstant(ctx.value(1).builtinValue()).getValue()));
                    } else {
                        throw new NotHandledCaseException();
                    }
                }
                abstractConstraint = sizeConstraint;
            }
        }
        return super.visitSubtypeElements(ctx);
    }
}
