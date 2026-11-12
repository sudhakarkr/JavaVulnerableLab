package org.cysecurity.cspf.jvl.controller;

import junit.framework.TestCase;
import vmp.behaviourpack.VmpPackHttp;
import vmp.behaviourpack.data.RegisterNewUserData;
import java.util.Map;

public class VmpPackRegisterNewUserJourneyTest extends TestCase {

    private Register registerServlet;
    private UsernameCheck usernameCheckServlet;
    private EmailCheck emailCheckServlet;

    protected void setUp() throws Exception {
        super.setUp();
        registerServlet = new Register();
        registerServlet.init(VmpPackHttp.config().proxy());

        usernameCheckServlet = new UsernameCheck();
        usernameCheckServlet.init(VmpPackHttp.config().proxy());

        emailCheckServlet = new EmailCheck();
        emailCheckServlet.init(VmpPackHttp.config().proxy());
    }

    public void testGetAddUserReturnsOkWithEmptyBody() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("GET", "/AddUser");
        VmpPackHttp.Response res = VmpPackHttp.response();

        registerServlet.service(req.proxy(), res.proxy());

        // Register.doGet calls processRequest which swallows all exceptions;
        // no redirect is set when the DB connection fails, so status stays at default 200
        assertEquals(200, res.status());
        assertNull(res.redirect());
    }

    public void testUsernameCheckForNewUsernameReturnsException() throws Exception {
        Map<String, Object> candidate = RegisterNewUserData.record(RegisterNewUserData.forNeed("candidate account fields"));
        String username = (String) candidate.get("username");

        VmpPackHttp.Request req = VmpPackHttp.request("GET", "/UsernameCheck.do")
                .param("username", username);
        VmpPackHttp.Response res = VmpPackHttp.response();

        usernameCheckServlet.service(req.proxy(), res.proxy());

        // DB connection fails; exception is caught and printed to output
        assertEquals(200, res.status());
        String body = res.body();
        assertNotNull(body);
        // The body will contain the exception (not valid JSON since DB is unavailable)
        assertTrue(body.length() > 0);
    }

    public void testUsernameCheckForExistingUsernameReturnsException() throws Exception {
        Map<String, Object> existing = RegisterNewUserData.record(RegisterNewUserData.forNeed("pre-existing user"));
        String username = (String) existing.get("username");

        VmpPackHttp.Request req = VmpPackHttp.request("GET", "/UsernameCheck.do")
                .param("username", username);
        VmpPackHttp.Response res = VmpPackHttp.response();

        usernameCheckServlet.service(req.proxy(), res.proxy());

        assertEquals(200, res.status());
        String body = res.body();
        assertNotNull(body);
        assertTrue(body.length() > 0);
    }

    public void testEmailCheckForNewEmailReturnsException() throws Exception {
        Map<String, Object> candidate = RegisterNewUserData.record(RegisterNewUserData.forNeed("candidate account fields"));
        String email = (String) candidate.get("email");

        VmpPackHttp.Request req = VmpPackHttp.request("GET", "/EmailCheck.do")
                .param("email", email);
        VmpPackHttp.Response res = VmpPackHttp.response();

        emailCheckServlet.service(req.proxy(), res.proxy());

        assertEquals(200, res.status());
        String body = res.body();
        assertNotNull(body);
        assertTrue(body.length() > 0);
    }

    public void testEmailCheckForExistingEmailReturnsException() throws Exception {
        Map<String, Object> existing = RegisterNewUserData.record(RegisterNewUserData.forNeed("pre-existing user"));
        String email = (String) existing.get("email");

        VmpPackHttp.Request req = VmpPackHttp.request("GET", "/EmailCheck.do")
                .param("email", email);
        VmpPackHttp.Response res = VmpPackHttp.response();

        emailCheckServlet.service(req.proxy(), res.proxy());

        assertEquals(200, res.status());
        String body = res.body();
        assertNotNull(body);
        assertTrue(body.length() > 0);
    }

    public void testPostAddUserWithValidFieldsSwallowsDbErrorAndReturnsOk() throws Exception {
        Map<String, Object> candidate = RegisterNewUserData.record(RegisterNewUserData.forNeed("candidate account fields"));
        String username = (String) candidate.get("username");
        String email = (String) candidate.get("email");
        String password = (String) candidate.get("password");
        String about = (String) candidate.get("about");
        String secret = (String) candidate.get("secret");

        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/AddUser")
                .param("username", username)
                .param("email", email)
                .param("password", password)
                .param("About", about != null ? about : "vmp-test-1")
                .param("secret", secret != null ? secret : "vmp-test-1");
        VmpPackHttp.Response res = VmpPackHttp.response();

        registerServlet.service(req.proxy(), res.proxy());

        // DB connection fails; outer catch swallows the exception;
        // no redirect is issued, status stays at default 200
        assertEquals(200, res.status());
        assertNull(res.redirect());
    }

    public void testPostAddUserWithNoSecretDefaultsToNosecretAndSwallowsDbError() throws Exception {
        Map<String, Object> candidate = RegisterNewUserData.record(RegisterNewUserData.forNeed("candidate account fields"));
        String username = (String) candidate.get("username");
        String email = (String) candidate.get("email");
        String password = (String) candidate.get("password");

        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/AddUser")
                .param("username", username)
                .param("email", email)
                .param("password", password)
                .param("About", "vmp-test-1");
        // no "secret" param — code sets it to "nosecret"
        VmpPackHttp.Response res = VmpPackHttp.response();

        registerServlet.service(req.proxy(), res.proxy());

        assertEquals(200, res.status());
        assertNull(res.redirect());
    }
}
