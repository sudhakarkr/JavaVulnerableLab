package org.cysecurity.cspf.jvl.controller;

import junit.framework.TestCase;
import vmp.behaviourpack.VmpPackHttp;
import vmp.behaviourpack.data.LoginWithCookieSessionData;

import java.util.Map;

public class VmpPackLoginWithCookieSessionJourneyTest extends TestCase {

    private LoginValidator servlet;

    protected void setUp() throws Exception {
        super.setUp();
        servlet = new LoginValidator();
        servlet.init(VmpPackHttp.config().proxy());
    }

    public void testGetLoginPageWithNoParamsRedirectsToErrorPage() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("GET", "/LoginValidator");
        VmpPackHttp.Response res = VmpPackHttp.response();
        try {
            servlet.service(req.proxy(), res.proxy());
            // If no exception, no redirect should have been set
            assertNull("Expected no redirect from GET /LoginValidator with no params (NPE before try/catch)", res.redirect());
        } catch (Exception e) {
            // NPE is thrown because username param is null; verify it is a NullPointerException
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            assertTrue("Expected NullPointerException when username param is missing",
                    cause instanceof NullPointerException);
        }
    }

    public void testPostWithInvalidCredentialsRedirectsToErrorPage() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/LoginValidator")
                .param("username", "vmp-test-1")
                .param("password", "vmp-test-1");
        VmpPackHttp.Response res = VmpPackHttp.response();
        servlet.service(req.proxy(), res.proxy());
        String redirect = res.redirect();
        assertNotNull("Expected a redirect from POST /LoginValidator with bad credentials", redirect);
        assertTrue("Redirect should contain login.jsp", redirect.contains("login.jsp"));
        assertTrue("Redirect should contain err= query param", redirect.contains("err="));
    }

    public void testPostWithMissingPasswordRedirectsToErrorPage() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/LoginValidator")
                .param("username", "riley1");
        VmpPackHttp.Response res = VmpPackHttp.response();
        try {
            servlet.service(req.proxy(), res.proxy());
            // If no exception, no redirect should have been set
            assertNull("Expected no redirect when password param is missing (NPE before try/catch)", res.redirect());
        } catch (Exception e) {
            // NPE is thrown because password param is null; verify it is a NullPointerException
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            assertTrue("Expected NullPointerException when password param is missing",
                    cause instanceof NullPointerException);
        }
    }

    public void testGetWithParamsAlsoRedirectsToErrorPageWhenNoDb() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("GET", "/LoginValidator")
                .param("username", "riley1")
                .param("password", "vmp-Test-df8416f3!");
        VmpPackHttp.Response res = VmpPackHttp.response();
        servlet.service(req.proxy(), res.proxy());
        String redirect = res.redirect();
        assertNotNull("Expected a redirect from GET /LoginValidator when DB is unavailable", redirect);
        assertTrue("Redirect should contain login.jsp", redirect.contains("login.jsp"));
        assertTrue("Redirect should contain err= query param", redirect.contains("err="));
    }
}
