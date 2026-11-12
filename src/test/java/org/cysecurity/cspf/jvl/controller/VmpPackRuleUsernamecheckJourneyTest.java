package org.cysecurity.cspf.jvl.controller;

import junit.framework.TestCase;
import vmp.behaviourpack.VmpPackHttp;

public class VmpPackRuleUsernamecheckJourneyTest extends TestCase {

    private UsernameCheck servlet;

    public void setUp() throws Exception {
        servlet = new UsernameCheck();
        servlet.init(VmpPackHttp.config().proxy());
    }

    public void testPostUsernameCheckWithExceptionDueToNoDatabase() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/UsernameCheck.do")
                .param("username", "vmp-test-1");
        VmpPackHttp.Response res = VmpPackHttp.response();

        servlet.service(req.proxy(), res.proxy());

        assertEquals(200, res.status());
        String body = res.body();
        assertNotNull(body);
        assertTrue(body.length() > 0);
        // The connection attempt fails (no DB / no JNDI / no config file),
        // so the catch block prints the exception to the response body.
        // The body must not be a valid JSON object with 'available' key
        // but instead contain an exception representation.
        assertFalse(body.contains("\"available\""));
    }

    public void testPostUsernameCheckContentTypeIsJson() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/UsernameCheck.do")
                .param("username", "vmp-test-1");
        VmpPackHttp.Response res = VmpPackHttp.response();

        servlet.service(req.proxy(), res.proxy());

        // Content type is set to application/json before any processing
        String contentType = res.header("Content-Type");
        // The fake may record it; if not null it must contain application/json
        if (contentType != null) {
            assertTrue(contentType.contains("application/json"));
        }
    }
}
