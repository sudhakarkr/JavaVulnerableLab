package org.cysecurity.cspf.jvl.controller;

import junit.framework.TestCase;
import vmp.behaviourpack.VmpPackHttp;

public class VmpPackInternalForwardNavigationJourneyTest extends TestCase {

    private ForwardMe servlet;

    protected void setUp() throws Exception {
        super.setUp();
        servlet = new ForwardMe();
        servlet.init(VmpPackHttp.config().proxy());
    }

    public void testGetWithoutLocationParameterPrintsMissingMessage() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("GET", "/ForwardMe");
        VmpPackHttp.Response res = VmpPackHttp.response();
        servlet.service(req.proxy(), res.proxy());
        String body = res.body();
        assertNotNull(body);
        assertTrue("Expected missing-location message in body", body.contains("Location Parameter is missing"));
    }

    public void testGetWithLocationParameterForwardsToTarget() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("GET", "/ForwardMe").param("location", "/index.jsp");
        VmpPackHttp.Response res = VmpPackHttp.response();
        servlet.service(req.proxy(), res.proxy());
        assertEquals("/index.jsp", req.forwardedTo());
    }

    public void testPostWithoutLocationParameterPrintsMissingMessage() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/ForwardMe");
        VmpPackHttp.Response res = VmpPackHttp.response();
        servlet.service(req.proxy(), res.proxy());
        String body = res.body();
        assertNotNull(body);
        assertTrue("Expected missing-location message in body", body.contains("Location Parameter is missing"));
    }

    public void testPostWithLocationParameterForwardsToTarget() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/ForwardMe").param("location", "/index.jsp");
        VmpPackHttp.Response res = VmpPackHttp.response();
        servlet.service(req.proxy(), res.proxy());
        assertEquals("/index.jsp", req.forwardedTo());
    }

    public void testGetWithTraversalStyleLocationForwardsExactValue() throws Exception {
        String traversal = "/../vmp-test-1";
        VmpPackHttp.Request req = VmpPackHttp.request("GET", "/ForwardMe").param("location", traversal);
        VmpPackHttp.Response res = VmpPackHttp.response();
        servlet.service(req.proxy(), res.proxy());
        assertEquals(traversal, req.forwardedTo());
    }

    public void testPostWithTraversalStyleLocationForwardsExactValue() throws Exception {
        String traversal = "/../vmp-test-1";
        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/ForwardMe").param("location", traversal);
        VmpPackHttp.Response res = VmpPackHttp.response();
        servlet.service(req.proxy(), res.proxy());
        assertEquals(traversal, req.forwardedTo());
    }
}
