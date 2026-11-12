package org.cysecurity.cspf.jvl.controller;

import junit.framework.TestCase;
import vmp.behaviourpack.VmpPackHttp;

public class VmpPackOutboundLinkRedirectJourneyTest extends TestCase {

    private Open servlet;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        servlet = new Open();
        servlet.init(VmpPackHttp.config().proxy());
    }

    // GET /Open with a url parameter — servlet issues a redirect to the supplied URL
    public void testGetWithUrlParameterRedirects() throws Exception {
        String targetUrl = "http://vmp-test-1.example.com/external";
        VmpPackHttp.Request req = VmpPackHttp.request("GET", "/Open").param("url", targetUrl);
        VmpPackHttp.Response res = VmpPackHttp.response();

        servlet.service(req.proxy(), res.proxy());

        assertEquals(302, res.status());
        assertEquals(targetUrl, res.redirect());
    }

    // GET /Open without a url parameter — servlet writes "Missing url parameter" to the body
    public void testGetWithoutUrlParameterShowsMessage() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("GET", "/Open");
        VmpPackHttp.Response res = VmpPackHttp.response();

        servlet.service(req.proxy(), res.proxy());

        // No redirect should have been issued
        assertNull(res.redirect());
        assertTrue(res.body().contains("Missing url parameter"));
    }

    // POST /Open with an internal (application-relative) url parameter
    public void testPostWithInternalUrlRedirects() throws Exception {
        String internalUrl = "/home";
        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/Open").param("url", internalUrl);
        VmpPackHttp.Response res = VmpPackHttp.response();

        servlet.service(req.proxy(), res.proxy());

        assertEquals(302, res.status());
        assertEquals(internalUrl, res.redirect());
    }

    // POST /Open with a fully-qualified external url parameter
    public void testPostWithExternalUrlRedirects() throws Exception {
        String externalUrl = "http://vmp-test-external.example.org/landing";
        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/Open").param("url", externalUrl);
        VmpPackHttp.Response res = VmpPackHttp.response();

        servlet.service(req.proxy(), res.proxy());

        assertEquals(302, res.status());
        assertEquals(externalUrl, res.redirect());
    }

    // POST /Open without a url parameter — servlet writes "Missing url parameter"
    public void testPostWithoutUrlParameterShowsMessage() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/Open");
        VmpPackHttp.Response res = VmpPackHttp.response();

        servlet.service(req.proxy(), res.proxy());

        assertNull(res.redirect());
        assertTrue(res.body().contains("Missing url parameter"));
    }
}
