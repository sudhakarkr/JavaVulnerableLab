package org.cysecurity.cspf.jvl.controller;

import junit.framework.TestCase;
import vmp.behaviourpack.VmpPackHttp;
import vmp.behaviourpack.data.LogoutSessionData;

import java.util.Map;

public class VmpPackLogoutSessionJourneyTest extends TestCase {

    private Logout servlet;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        servlet = new Logout();
        servlet.init(VmpPackHttp.config().proxy());
    }

    public void testGetLogoutInvalidatesSessionAndRedirects() throws Exception {
        Map<String, Object> userData = LogoutSessionData.record(LogoutSessionData.forNeed("active user session"));

        VmpPackHttp.Session session = VmpPackHttp.session()
                .attribute("username", userData.get("username"))
                .attribute("email", userData.get("email"));

        VmpPackHttp.Request req = VmpPackHttp.request("GET", "/Logout")
                .session(session);
        VmpPackHttp.Response res = VmpPackHttp.response();

        servlet.service(req.proxy(), res.proxy());

        assertEquals("Response should redirect to index.jsp",
                "index.jsp", res.redirect());

        assertTrue("Session should be invalidated after GET logout",
                req.session().invalidated());
    }

    public void testPostLogoutInvalidatesSessionAndRedirects() throws Exception {
        Map<String, Object> userData = LogoutSessionData.record(LogoutSessionData.forNeed("active user session"));

        VmpPackHttp.Session session = VmpPackHttp.session()
                .attribute("username", userData.get("username"))
                .attribute("email", userData.get("email"));

        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/Logout")
                .session(session);
        VmpPackHttp.Response res = VmpPackHttp.response();

        servlet.service(req.proxy(), res.proxy());

        assertEquals("Response should redirect to index.jsp",
                "index.jsp", res.redirect());

        assertTrue("Session should be invalidated after POST logout",
                req.session().invalidated());
    }

    public void testGetLogoutWithNoActiveSessionStillRedirects() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("GET", "/Logout");
        VmpPackHttp.Response res = VmpPackHttp.response();

        servlet.service(req.proxy(), res.proxy());

        assertEquals("Response should redirect to index.jsp even with no prior session",
                "index.jsp", res.redirect());
    }

    public void testPostLogoutWithNoActiveSessionStillRedirects() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/Logout");
        VmpPackHttp.Response res = VmpPackHttp.response();

        servlet.service(req.proxy(), res.proxy());

        assertEquals("Response should redirect to index.jsp even with no prior session",
                "index.jsp", res.redirect());
    }
}
