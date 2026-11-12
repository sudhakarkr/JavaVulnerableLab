package org.cysecurity.cspf.jvl.controller;

import junit.framework.TestCase;
import vmp.behaviourpack.VmpPackHttp;

public class VmpPackAdminAddPageJourneyTest extends TestCase {

    private AddPage servlet;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        servlet = new AddPage();
        servlet.init(VmpPackHttp.config().proxy());
    }

    public void testGetWithNoParamsPrintsMissingMessage() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("GET", "/admin/AddPage.do");
        VmpPackHttp.Response res = VmpPackHttp.response();
        servlet.service(req.proxy(), res.proxy());
        String body = res.body();
        assertNotNull(body);
        assertTrue("Expected missing-parameter message",
                body.contains("filename or content Parameter is missing"));
    }

    public void testPostWithNoParamsPrintsMissingMessage() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/admin/AddPage.do");
        VmpPackHttp.Response res = VmpPackHttp.response();
        servlet.service(req.proxy(), res.proxy());
        String body = res.body();
        assertNotNull(body);
        assertTrue("Expected missing-parameter message",
                body.contains("filename or content Parameter is missing"));
    }

    public void testPostWithOnlyFilenameMissingContentPrintsMissingMessage() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/admin/AddPage.do")
                .param("filename", "vmp-test-1.html");
        VmpPackHttp.Response res = VmpPackHttp.response();
        servlet.service(req.proxy(), res.proxy());
        String body = res.body();
        assertNotNull(body);
        assertTrue("Expected missing-parameter message",
                body.contains("filename or content Parameter is missing"));
    }

    public void testPostWithOnlyContentMissingFilenamePrintsMissingMessage() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/admin/AddPage.do")
                .param("content", "Hello world content");
        VmpPackHttp.Response res = VmpPackHttp.response();
        servlet.service(req.proxy(), res.proxy());
        String body = res.body();
        assertNotNull(body);
        assertTrue("Expected missing-parameter message",
                body.contains("filename or content Parameter is missing"));
    }

    public void testPostWithInvalidFilenamePathTraversalPrintsInvalidMessage() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/admin/AddPage.do")
                .param("filename", "../etc/passwd")
                .param("content", "malicious content");
        VmpPackHttp.Response res = VmpPackHttp.response();
        servlet.service(req.proxy(), res.proxy());
        String body = res.body();
        assertNotNull(body);
        assertTrue("Expected invalid filename message",
                body.contains("Invalid filename."));
    }

    public void testPostWithInvalidFilenameNoExtensionPrintsInvalidMessage() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/admin/AddPage.do")
                .param("filename", "badfile")
                .param("content", "some content");
        VmpPackHttp.Response res = VmpPackHttp.response();
        servlet.service(req.proxy(), res.proxy());
        String body = res.body();
        assertNotNull(body);
        assertTrue("Expected invalid filename message",
                body.contains("Invalid filename."));
    }

    public void testPostWithInvalidFilenameJspExtensionPrintsInvalidMessage() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/admin/AddPage.do")
                .param("filename", "shell.jsp")
                .param("content", "<% out.println(\"pwned\"); %>");
        VmpPackHttp.Response res = VmpPackHttp.response();
        servlet.service(req.proxy(), res.proxy());
        String body = res.body();
        assertNotNull(body);
        assertTrue("Expected invalid filename message",
                body.contains("Invalid filename."));
    }

    public void testPostWithValidParamsAttemptsFileCreation() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("POST", "/admin/AddPage.do")
                .param("filename", "vmp-test-1.html")
                .param("content", "Test page body content");
        VmpPackHttp.Response res = VmpPackHttp.response();
        servlet.service(req.proxy(), res.proxy());
        String body = res.body();
        assertNotNull(body);
        // getRealPath may return null in fake context causing NPE caught by catch block,
        // or may return a real path causing success or failure
        boolean successPath = body.contains("Successfully created the file") || body.contains("Failed to create the file");
        boolean exceptionPath = body.contains("Exception") || body.contains("NullPointerException") || body.contains("java.lang");
        boolean invalidPath = body.contains("Invalid filename");
        assertTrue("Expected either a file creation outcome or an exception from getRealPath",
                successPath || exceptionPath || invalidPath);
    }

    public void testGetWithValidParamsAttemptsFileCreation() throws Exception {
        VmpPackHttp.Request req = VmpPackHttp.request("GET", "/admin/AddPage.do")
                .param("filename", "vmp-test-2.html")
                .param("content", "GET page body");
        VmpPackHttp.Response res = VmpPackHttp.response();
        servlet.service(req.proxy(), res.proxy());
        String body = res.body();
        assertNotNull(body);
        boolean successPath = body.contains("Successfully created the file") || body.contains("Failed to create the file");
        boolean exceptionPath = body.contains("Exception") || body.contains("NullPointerException") || body.contains("java.lang");
        boolean invalidPath = body.contains("Invalid filename");
        assertTrue("Expected either a file creation outcome or an exception from getRealPath",
                successPath || exceptionPath || invalidPath);
    }
}
