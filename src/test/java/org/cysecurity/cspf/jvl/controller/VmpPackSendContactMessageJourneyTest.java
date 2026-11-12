package org.cysecurity.cspf.jvl.controller;

import junit.framework.TestCase;
import vmp.behaviourpack.VmpPackHttp;
import vmp.behaviourpack.data.SendContactMessageData;
import java.util.Map;

public class VmpPackSendContactMessageJourneyTest extends TestCase {

    private SendMessage servlet;

    protected void setUp() throws Exception {
        super.setUp();
        servlet = new SendMessage();
        servlet.init(VmpPackHttp.config().proxy());
    }

    public void testGetSendMessageFormRedirectsToStatusPage() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("GET", "/SendMessage.do");
        VmpPackHttp.Response res = VmpPackHttp.response();

        servlet.service(req.proxy(), res.proxy());

        String redirect = res.redirect();
        assertNotNull("Expected a redirect from GET /SendMessage.do", redirect);
        assertTrue(
            "Redirect should point to SendMessage.jsp",
            redirect.contains("SendMessage.jsp")
        );
        assertTrue(
            "Redirect should contain a status parameter",
            redirect.contains("status=")
        );
    }

    public void testPostWithSendParamAndNoDbRedirectsToErrorStatus() throws Exception {
        Map<String, Object> message = SendContactMessageData.record(SendContactMessageData.forNeed("message payload"));

        String sender = message.get("email") != null ? (String) message.get("email") : "rowan.mockford1@example.test";
        String msg = message.get("msg") != null ? (String) message.get("msg") : "Sample msg 1";
        String subject = message.get("name") != null ? (String) message.get("name") : "Sample Message 1";

        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/SendMessage.do")
            .param("recipient", "vmp-test-1")
            .param("sender", sender)
            .param("subject", subject)
            .param("msg", msg)
            .param("send", "Send");
        VmpPackHttp.Response res = VmpPackHttp.response();

        servlet.service(req.proxy(), res.proxy());

        String redirect = res.redirect();
        assertNotNull("Expected a redirect from POST /SendMessage.do with send param", redirect);
        assertTrue(
            "Redirect should point to SendMessage.jsp",
            redirect.contains("SendMessage.jsp")
        );
        assertTrue(
            "Redirect should contain a status parameter",
            redirect.contains("status=")
        );
        assertTrue(
            "Redirect should indicate Something Went Wrong when DB is unavailable",
            redirect.contains("Something+Went+Wrong") || redirect.contains("Something Went Wrong")
        );
    }

    public void testPostWithoutSendParamRedirectsToErrorStatus() throws Exception {
        Map<String, Object> message = SendContactMessageData.record(SendContactMessageData.forNeed("message payload"));

        String sender = message.get("email") != null ? (String) message.get("email") : "rowan.mockford1@example.test";
        String msg = message.get("msg") != null ? (String) message.get("msg") : "Sample msg 1";
        String subject = message.get("name") != null ? (String) message.get("name") : "Sample Message 1";

        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/SendMessage.do")
            .param("recipient", "vmp-test-1")
            .param("sender", sender)
            .param("subject", subject)
            .param("msg", msg);
        VmpPackHttp.Response res = VmpPackHttp.response();

        servlet.service(req.proxy(), res.proxy());

        String redirect = res.redirect();
        assertNotNull("Expected a redirect from POST /SendMessage.do without send param", redirect);
        assertTrue(
            "Redirect should point to SendMessage.jsp",
            redirect.contains("SendMessage.jsp")
        );
        assertTrue(
            "Redirect should contain a status parameter",
            redirect.contains("status=")
        );
        assertTrue(
            "Redirect should indicate Something Went Wrong when send param absent",
            redirect.contains("Something+Went+Wrong") || redirect.contains("Something Went Wrong")
        );
    }
}
