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

import com.ericsson.mts.asn1.factory.FormatReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class JSONFormatReader implements FormatReader {
    private Logger logger = LoggerFactory.getLogger(JSONFormatReader.class.getSimpleName());

    private String ignoredNode;
    private JsonNode currentNode;
    private Stack<JsonNode> stack = new Stack<>();
    private Stack<Integer> arrayStack = new Stack<>();

    public JSONFormatReader(InputStream inputStream, String ignoredNode) throws IOException {
        this.ignoredNode = ignoredNode;
        ObjectMapper mapper = new ObjectMapper();
        currentNode = mapper.readTree(inputStream);
    }

    private JsonNode getFromStack(JsonNode node) {
        if (currentNode.isArray()) {
            Integer currentIndex = arrayStack.pop();
            arrayStack.push(currentIndex + 1);
            return currentNode.get(currentIndex);
        }
        return node;
    }

    @Override
    public void enterObject(String name) {
        logger.trace("Enter object {}", name);
        if (!ignoredNode.equals(name)) {
            stack.push(currentNode);
            currentNode = getFromStack(currentNode);
            if (name != null) {
                currentNode = currentNode.get(name);
            }
            if (currentNode == null) {
                throw new NullPointerException("Name : " + name);
            }
        }
    }

    @Override
    public void leaveObject(String name) {
        logger.trace("Leave object {}", name);
        if (!ignoredNode.equals(name)) {
            currentNode = stack.pop();
            if (currentNode == null) {
                throw new NullPointerException("Name : " + name);
            }
        }
    }

    @Override
    public int enterArray(String name) {
        logger.trace("Enter array {}", name);
        stack.push(currentNode);
        currentNode = currentNode.get(name);
        arrayStack.push(0);
        if (currentNode == null) {
            throw new NullPointerException("Name : " + name);
        }
        return currentNode.size();
    }

    @Override
    public void leaveArray(String name) {
        logger.trace("Leave array {}", name);
        currentNode = stack.pop();
        arrayStack.pop();
        if (currentNode == null) {
            throw new NullPointerException("Name : " + name);
        }
    }

    @Override
    public boolean booleanValue(String name) {
        return getFromStack(currentNode).get(name).booleanValue();
    }

    @Override
    public String bitsValue(String name) {
        return getFromStack(currentNode).get(name).textValue();
    }

    @Override
    public String bytesValue(String name) {
        JsonNode node = getFromStack(currentNode);
        if (node.isTextual()) {
            return node.textValue();
        }
        return node.get(name).textValue();

    }

    @Override
    public BigInteger intValue(String name) {
        return getFromStack(currentNode).get(name).bigIntegerValue();
    }

    @Override
    public List<String> fieldsValue() {
        Iterator<String> fieldNames = getFromStack(currentNode).fieldNames();
        List<String> stringList = new ArrayList<>();
        while (fieldNames.hasNext()) {
            stringList.add(fieldNames.next());
        }
        return stringList;
    }

    @Override
    public String stringValue(String name) {
        return getFromStack(currentNode).get(name).asText();
    }

    @Override
    public String printCurrentnode() {
        return currentNode.toString();
    }
}
