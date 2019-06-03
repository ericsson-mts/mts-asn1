/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.ericsson.mts.asn1;

import com.ericsson.mts.asn1.factory.FormatWriter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Stack;

public class JSONFormatWriter implements FormatWriter {

    private Logger logger = LoggerFactory.getLogger(JSONFormatWriter.class.getSimpleName());
    private Stack<JsonNode> stack = new Stack<>();
    private JsonNode root = null;


    public JsonNode getJsonNode() {
        return root;
    }

    private JsonNode addToTopMostNode(String name, JsonNode node) {
        if (!stack.isEmpty()) {
            JsonNode top = stack.peek();
            if (top.isArray()) {
                ((ArrayNode) top).add(node);
            } else if (top.isObject()) {
                ((ObjectNode) top).set(name, node);
            } else {
                throw new RuntimeException("should not happen");
            }
        }
        return node;
    }

    @Override
    public void enterObject(String name) {
        logger.trace("Enter object {}", name);
        stack.push(addToTopMostNode(name, JsonNodeFactory.instance.objectNode()));
    }

    @Override
    public void leaveObject(String name) {
        JsonNode popped = stack.pop();
        if (stack.isEmpty()) {
            root = popped;
        }
    }

    @Override
    public void enterArray(String name) {
        logger.trace("Enter array {}", name);
        stack.push(addToTopMostNode(name, JsonNodeFactory.instance.arrayNode()));

    }

    @Override
    public void leaveArray(String name) {
        JsonNode popped = stack.pop();
        if (stack.isEmpty()) {
            root = popped;
        }
    }

    @Override
    public void stringValue(String name, String value) {
        addToTopMostNode(name, JsonNodeFactory.instance.textNode(value));
    }

    @Override
    public void booleanValue(String name, boolean value) {
        addToTopMostNode(name, JsonNodeFactory.instance.booleanNode(value));
    }

    @Override
    public void intValue(String name, BigInteger value, String namedValue) {
        addToTopMostNode(name, JsonNodeFactory.instance.numberNode(value));
    }

    @Override
    public void realValue(String name, BigDecimal value) {
        addToTopMostNode(name, JsonNodeFactory.instance.numberNode(value));
    }

    @Override
    public void bytesValue(String name, byte[] value) {
        addToTopMostNode(name, JsonNodeFactory.instance.textNode(FormatWriter.bytesToHex(value)));
    }

    @Override
    public void bitsValue(String name, String value) {
        addToTopMostNode(name, JsonNodeFactory.instance.textNode(value));
    }

    @Override
    public void nullValue(String name) {
        addToTopMostNode(name, JsonNodeFactory.instance.nullNode());
    }
}
