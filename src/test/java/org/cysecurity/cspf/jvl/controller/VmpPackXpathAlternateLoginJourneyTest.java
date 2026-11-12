package org.cysecurity.cspf.jvl.controller;

import junit.framework.TestCase;
import vmp.behaviourpack.VmpPackHttp;

public class VmpPackXpathAlternateLoginJourneyTest extends TestCase {

    private XPathQuery servlet;

    protected void setUp() throws Exception {
        super.setUp();
        servlet = new XPathQuery();
        servlet.init(VmpPackHttp.config().proxy());
    }

    public void testGetXpathLoginFormReturnsExceptionOutput() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("GET", "/XPathQuery.do");
        VmpPackHttp.Response res = VmpPackHttp.response();
        servlet.service(req.proxy(), res.proxy());
        assertEquals(200, res.status());
        assertNull(res.redirect());
        String body = res.body();
        assertNotNull(body);
    }

    public void testPostWithValidCredentialsHitsExceptionPath() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/XPathQuery.do")
                .param("username", "morgan1")
                .param("password", "vmp-Test-eb0302e3!");
        VmpPackHttp.Response res = VmpPackHttp.response();
        servlet.service(req.proxy(), res.proxy());
        assertEquals(200, res.status());
        assertNull(res.redirect());
        String body = res.body();
        assertNotNull(body);
    }

    public void testPostWithInvalidCredentialsHitsExceptionPath() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/XPathQuery.do")
                .param("username", "morgan1")
                .param("password", "vmp-test-1");
        VmpPackHttp.Response res = VmpPackHttp.response();
        servlet.service(req.proxy(), res.proxy());
        assertEquals(200, res.status());
        assertNull(res.redirect());
        String body = res.body();
        assertNotNull(body);
    }
}
