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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class S1APENodeBSetup extends AbstractTests {

    @BeforeAll
    static void init() {
        try {
            asn1Translator = new ASN1Translator(new PERTranslatorFactory(true), Collections.singletonList(S1APENodeBSetup.class.getResourceAsStream("/grammar/S1AP/S1AP.asn")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testS1SetupRequest() throws Exception {
        test("S1AP-PDU", "/data/S1AP/eNodeB_Setup/S1SetupRequest/S1SetupRequest.bin", "/data/S1AP/eNodeB_Setup/S1SetupRequest/S1SetupRequest.json", "/data/S1AP/eNodeB_Setup/S1SetupRequest/S1SetupRequest.xml");
    }

    @Test
    void testS1SetupResponse() throws Exception {
        test("S1AP-PDU", "/data/S1AP/eNodeB_Setup/S1SetupResponse/S1SetupResponse.bin", "/data/S1AP/eNodeB_Setup/S1SetupResponse/S1SetupResponse.json", "/data/S1AP/eNodeB_Setup/S1SetupResponse/S1SetupResponse.xml");
    }

}
