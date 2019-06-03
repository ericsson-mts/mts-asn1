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

import com.ericsson.mts.asn1.exception.NotHandledCaseException;
import com.ericsson.mts.asn1.factory.FormatReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class XMLFormatReader implements FormatReader {
    private static String IS_ARRAY = "isArray";
    private Logger logger = LoggerFactory.getLogger(XMLFormatReader.class.getSimpleName());
    private String ignoredObject;
    private Element currentNode;
    private Stack<Integer> arrayStack = new Stack<>();

    public XMLFormatReader(File file, String type) throws Exception {
        this(new FileInputStream(file), type);
    }

    public XMLFormatReader(InputStream inputStream, String type) throws Exception {
        this(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream), type);

    }

    public XMLFormatReader(Document document, String type) throws TransformerException {
        this.currentNode = document.getDocumentElement();
        ignoredObject = type;
//        NodeList nodeList = currentNode.getElementsByTagName("initiatingMessage");
//        for(int i = 0; i < nodeList.getLength(); i++){
//            Element element = (Element) nodeList.item(i);
//            Element element1InitiatingMessage = (Element) element.getElementsByTagName("initiatingMessage").item(0);
//            Element element1ProcedureCode = (Element) element1InitiatingMessage.getElementsByTagName("procedureCode").item(0);
//            System.out.println(element1ProcedureCode.getTextContent());
////            System.out.println(element.getElementsByTagName("initiatingMessage").item(0).getFirstChild().getTextContent());
//        }

    }

    @Override
    public void enterObject(String name) {
        if (!ignoredObject.equals(name)) {
            if (name != null) {
                logger.trace("Enter object {}", name);
                currentNode = getChildNode(name);
            } else {
                if (!currentNode.getAttribute(IS_ARRAY).equals("")) {
                    int arrayIndice = Integer.parseInt(currentNode.getAttribute(IS_ARRAY));
                    logger.trace("Enter array field {}", arrayIndice);
                    NodeList nodeList = currentNode.getElementsByTagName(currentNode.getTagName());
                    currentNode = (Element) nodeList.item(arrayIndice);
                    arrayIndice++;
                    ((Element) currentNode.getParentNode()).setAttribute(IS_ARRAY, String.valueOf(arrayIndice));
                    if (currentNode == null) {
                        throw new RuntimeException();
                    }
                } else {
                    throw new RuntimeException();
                }
            }
        }
    }

    @Override
    public void leaveObject(String name) {
        if (!ignoredObject.equals(name)) {
            if (name != null) {
                logger.trace("Leave object {}", name);
                currentNode = (Element) currentNode.getParentNode();
            } else {
                currentNode = (Element) currentNode.getParentNode();
                if (currentNode == null || currentNode.getAttribute(IS_ARRAY).equals("")) {
                    throw new RuntimeException();
                }
                logger.trace("Leave array field {}", Integer.valueOf(currentNode.getAttribute(IS_ARRAY)) - 1);
            }
        }
    }

    @Override
    public int enterArray(String name) {
        if (name != null) {
            currentNode = getChildNode(name);
        } else {
            throw new RuntimeException();
        }
        currentNode.setAttribute(IS_ARRAY, "0");


        int n = 0;
        NodeList nodeList = currentNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i).getNodeType() != Node.TEXT_NODE) {
                n++;
            }
        }
        logger.trace("Enter array {}, size={}", name, n);
//        System.out.println(currentNode.getNodeName() + " : " + currentNode.getTextContent());
        return n;
    }

    @Override
    public void leaveArray(String name) {
        logger.trace("Leave array {}", name);
        if (currentNode.getAttribute(IS_ARRAY).equals("")) {
            throw new RuntimeException();
        }
        currentNode.removeAttribute(IS_ARRAY);
        currentNode = (Element) currentNode.getParentNode();
        if (currentNode == null) {
            throw new RuntimeException();
        }
    }

    @Override
    public boolean booleanValue(String name) {
        throw new NotHandledCaseException(name);
    }

    @Override
    public String bitsValue(String name) {
        if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
            return getChildNode(name).getTextContent();
        }
        throw new RuntimeException(String.valueOf(currentNode.getNodeType()));
    }

    @Override
    public String bytesValue(String name) {
        if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
            return getChildNode(name).getTextContent().replaceAll("[\\t\\n\\r ]", "");
        }
        throw new RuntimeException(String.valueOf(currentNode.getNodeType()));
    }

    @Override
    public BigInteger intValue(String name) {
        if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
            return new BigInteger(getChildNode(name).getTextContent());
        }
        throw new RuntimeException(String.valueOf(currentNode.getNodeType()));
    }

    @Override
    public List<String> fieldsValue() {
        List<String> stringList = new ArrayList<>();
        NodeList nodeList = currentNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
//            System.out.println(nodeList.item(i).getNodeName() + " : " + nodeList.item(i).getTextContent());
            if (!(nodeList.item(i).getNodeType() == Node.TEXT_NODE) || !"#text".equals(nodeList.item(i).getNodeName())) {
                stringList.add(nodeList.item(i).getNodeName());
            }
        }
        return stringList;
    }

    @Override
    public String stringValue(String name) {
        if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
            return getChildNode(name).getTextContent();
        }
        throw new RuntimeException(String.valueOf(currentNode.getNodeType()));
    }

    @Override
    public String printCurrentnode() {
        throw new NotHandledCaseException();
    }

    private Element getChildNode(String name) {
        NodeList nodeList = currentNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i).getNodeName().equals(name)) {
                return (Element) nodeList.item(i);
            }
        }
        throw new RuntimeException("Can't find child node " + name);
    }
}
