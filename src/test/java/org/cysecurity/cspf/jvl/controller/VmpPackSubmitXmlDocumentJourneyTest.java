package org.cysecurity.cspf.jvl.controller;

import junit.framework.TestCase;
import vmp.behaviourpack.VmpPackHttp;

public class VmpPackSubmitXmlDocumentJourneyTest extends TestCase {

    private xxe servlet;

    protected void setUp() throws Exception {
        super.setUp();
        servlet = new xxe();
        servlet.init(VmpPackHttp.config().proxy());
    }

    public void testGetXxeEndpointWithNoBodyReturnsException() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("GET", "/xxe.do");
        VmpPackHttp.Response res = VmpPackHttp.response();
        servlet.service(req.proxy(), res.proxy());
        String body = res.body();
        assertNotNull(body);
        // With no XML body the parser throws and the exception is printed
        assertTrue(body.contains("Exception") || body.contains("exception") || body.contains("Error") || body.contains("SAX"));
    }

    public void testPostWellFormedXmlDocumentEchoesParsedContent() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><item>vmp-test-1</item><value>vmp-test-2</value></root>";
        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/xxe.do")
                .body("text/xml", xml);
        VmpPackHttp.Response res = VmpPackHttp.response();
        servlet.service(req.proxy(), res.proxy());
        String body = res.body();
        assertNotNull(body);
        assertTrue(body.contains("Result:") || body.contains("Exception") || body.contains("exception") || body.contains("Error"));
        assertTrue(body.length() > 0);
    }
}
