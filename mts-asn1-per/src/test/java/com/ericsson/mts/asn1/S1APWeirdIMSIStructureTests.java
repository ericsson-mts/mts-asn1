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

public class S1APWeirdIMSIStructureTests extends AbstractTests {

    @BeforeAll
    static void init() {
        try {
            asn1Translator = new ASN1Translator(new PERTranslatorFactory(true), Collections.singletonList(S1APWeirdIMSIStructureTests.class.getResourceAsStream("/grammar/S1AP/S1AP.asn")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testInitialUEMessage() throws Exception {
        test("S1AP-PDU", "/data/S1AP/weird_imsi_structure/InitialUEMessage/InitialUEMessage.bin", "/data/S1AP/weird_imsi_structure/InitialUEMessage/InitialUEMessage.json", "/data/S1AP/weird_imsi_structure/InitialUEMessage/InitialUEMessage.xml");
    }

    @Test
    void testDownlinkNASTransport() throws Exception {
        test("S1AP-PDU", "/data/S1AP/weird_imsi_structure/DownlinkNASTransport/DownlinkNASTransport.bin", "/data/S1AP/weird_imsi_structure/DownlinkNASTransport/DownlinkNASTransport.json", "/data/S1AP/weird_imsi_structure/DownlinkNASTransport/DownlinkNASTransport.xml");
    }

//    @Test
//    void testUEContextReleaseCommand() throws Exception {
//        updateDataformatFileTest("S1AP-PDU", "/data/S1AP/weird_imsi_structure/UEContextReleaseCommand/UEContextReleaseCommand.bin", "/data/S1AP/weird_imsi_structure/UEContextReleaseCommand/UEContextReleaseCommand.json", "/data/S1AP/weird_imsi_structure/UEContextReleaseCommand/UEContextReleaseCommand.xml");
//    }

    @Test
    void testUEContextReleaseComplete() throws Exception {
        test("S1AP-PDU", "/data/S1AP/weird_imsi_structure/UEContextReleaseComplete/UEContextReleaseComplete.bin", "/data/S1AP/weird_imsi_structure/UEContextReleaseComplete/UEContextReleaseComplete.json", "/data/S1AP/weird_imsi_structure/UEContextReleaseComplete/UEContextReleaseComplete.xml");
    }

    @Test
    void testInitialUEMessage2() throws Exception {
        test("S1AP-PDU", "/data/S1AP/weird_imsi_structure/InitialUEMessage2/InitialUEMessage2.bin", "/data/S1AP/weird_imsi_structure/InitialUEMessage2/InitialUEMessage2.json", "/data/S1AP/weird_imsi_structure/InitialUEMessage2/InitialUEMessage2.xml");
    }

    @Test
    void testDownlinkNASTransport2() throws Exception {
        test("S1AP-PDU", "/data/S1AP/weird_imsi_structure/DownlinkNASTransport2/DownlinkNASTransport2.bin", "/data/S1AP/weird_imsi_structure/DownlinkNASTransport2/DownlinkNASTransport2.json", "/data/S1AP/weird_imsi_structure/DownlinkNASTransport2/DownlinkNASTransport2.xml");
    }

//    @Test
//    void testUEContextReleaseCommand2() throws Exception {
//        updateDataformatFileTest("S1AP-PDU", "/data/S1AP/weird_imsi_structure/UEContextReleaseCommand2/UEContextReleaseCommand2.bin", "/data/S1AP/weird_imsi_structure/UEContextReleaseCommand2/UEContextReleaseCommand2.json", "/data/S1AP/weird_imsi_structure/UEContextReleaseCommand2/UEContextReleaseCommand2.xml");
//    }

    @Test
    void testUEContextReleaseComplete2() throws Exception {
        test("S1AP-PDU", "/data/S1AP/weird_imsi_structure/UEContextReleaseComplete2/UEContextReleaseComplete2.bin", "/data/S1AP/weird_imsi_structure/UEContextReleaseComplete2/UEContextReleaseComplete2.json", "/data/S1AP/weird_imsi_structure/UEContextReleaseComplete2/UEContextReleaseComplete2.xml");
    }

    @Test
    void testInitialUEMessage3() throws Exception {
        test("S1AP-PDU", "/data/S1AP/weird_imsi_structure/InitialUEMessage3/InitialUEMessage3.bin", "/data/S1AP/weird_imsi_structure/InitialUEMessage3/InitialUEMessage3.json", "/data/S1AP/weird_imsi_structure/InitialUEMessage3/InitialUEMessage3.xml");
    }

    @Test
    void testDownlinkNASTransport3() throws Exception {
        test("S1AP-PDU", "/data/S1AP/weird_imsi_structure/DownlinkNASTransport3/DownlinkNASTransport3.bin", "/data/S1AP/weird_imsi_structure/DownlinkNASTransport3/DownlinkNASTransport3.json", "/data/S1AP/weird_imsi_structure/DownlinkNASTransport3/DownlinkNASTransport3.xml");
    }

//    @Test
//    void testUEContextReleaseCommand3() throws Exception {
//        updateDataformatFileTest("S1AP-PDU", "/data/S1AP/weird_imsi_structure/UEContextReleaseCommand3/UEContextReleaseCommand3.bin", "/data/S1AP/weird_imsi_structure/UEContextReleaseCommand3/UEContextReleaseCommand3.json", "/data/S1AP/weird_imsi_structure/UEContextReleaseCommand3/UEContextReleaseCommand3.xml");
//    }

    @Test
    void testUEContextReleaseComplete3() throws Exception {
        test("S1AP-PDU", "/data/S1AP/weird_imsi_structure/UEContextReleaseComplete3/UEContextReleaseComplete3.bin", "/data/S1AP/weird_imsi_structure/UEContextReleaseComplete3/UEContextReleaseComplete3.json", "/data/S1AP/weird_imsi_structure/UEContextReleaseComplete3/UEContextReleaseComplete3.xml");
    }
}
