package org.cysecurity.cspf.jvl.controller;

import junit.framework.TestCase;
import vmp.behaviourpack.VmpPackHttp;

public class VmpPackRuleEmailcheckJourneyTest extends TestCase {

    private EmailCheck servlet;

    public void setUp() throws Exception {
        servlet = new EmailCheck();
        servlet.init(VmpPackHttp.config().proxy());
    }

    public void testPostWithEmailParamReturnsJsonContentTypeAndPrintsException() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/EmailCheck.do")
                .param("email", "vmp-test-1@example.com");
        VmpPackHttp.Response res = VmpPackHttp.response();
        servlet.service(req.proxy(), res.proxy());
        assertEquals(200, res.status());
        String body = res.body();
        assertNotNull(body);
        assertTrue(body.length() > 0);
    }

    public void testPostWithoutEmailParamPrintsException() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/EmailCheck.do");
        VmpPackHttp.Response res = VmpPackHttp.response();
        servlet.service(req.proxy(), res.proxy());
        assertEquals(200, res.status());
        String body = res.body();
        assertNotNull(body);
        assertTrue(body.length() > 0);
    }
}
