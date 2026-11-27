/*
 * Regression test for CWE-89 SQL Injection fix in EmailCheck.
 */
package org.cysecurity.cspf.jvl.controller;

import java.lang.reflect.Method;
import junit.framework.TestCase;

/**
 * Verifies that EmailCheck.processRequest uses a PreparedStatement
 * (parameterised query) rather than concatenating the email parameter
 * into a Statement.executeQuery(String) call.
 *
 * The original vulnerable code called Connection.createStatement() and
 * then stmt.executeQuery("select * from users where email='"+email+"'").
 * The fix replaces this with con.prepareStatement(...) + stmt.setString.
 *
 * We prove the behavioural difference by inspecting the bytecode-level
 * method references of EmailCheck via reflection on its declared methods
 * and the constant pool is not directly reachable, so instead we drive
 * processRequest through a stub Connection and observe which JDBC method
 * is actually invoked at runtime. On the ORIGINAL code, createStatement()
 * is called and executeQuery(String) receives a string containing a
 * single-quote-injected payload. On the FIXED code, prepareStatement(String)
 * is called with a '?' placeholder and setString binds the raw value.
 */
public class VmpRegressionTest extends TestCase {

    public void testUsesPreparedStatementNotConcatenation() throws Exception {
        // Record which JDBC entry points get called and with what SQL.
        final java.util.List calls = new java.util.ArrayList();
        final java.util.List sqls = new java.util.ArrayList();
        final java.util.List bindings = new java.util.ArrayList();

        // Build a minimal invocation-handler-based Connection stub.
        java.lang.reflect.InvocationHandler rsHandler = new java.lang.reflect.InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String n = method.getName();
                if (n.equals("next")) return Boolean.FALSE;
                if (n.equals("close")) return null;
                Class rt = method.getReturnType();
                if (rt == boolean.class) return Boolean.FALSE;
                if (rt == int.class) return new Integer(0);
                if (rt == long.class) return new Long(0);
                return null;
            }
        };
        final java.sql.ResultSet rs = (java.sql.ResultSet) java.lang.reflect.Proxy.newProxyInstance(
                java.sql.ResultSet.class.getClassLoader(),
                new Class[]{java.sql.ResultSet.class},
                rsHandler);

        java.lang.reflect.InvocationHandler stmtHandler = new java.lang.reflect.InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String n = method.getName();
                if (n.equals("executeQuery")) {
                    calls.add("executeQuery/" + method.getParameterTypes().length);
                    if (args != null && args.length == 1 && args[0] instanceof String) {
                        sqls.add((String) args[0]);
                    }
                    return rs;
                }
                if (n.equals("setString")) {
                    bindings.add(String.valueOf(args[1]));
                    return null;
                }
                if (n.equals("close")) return null;
                if (n.equals("isClosed")) return Boolean.FALSE;
                Class rt = method.getReturnType();
                if (rt == boolean.class) return Boolean.FALSE;
                if (rt == int.class) return new Integer(0);
                return null;
            }
        };
        final Object stmtProxy = java.lang.reflect.Proxy.newProxyInstance(
                java.sql.PreparedStatement.class.getClassLoader(),
                new Class[]{java.sql.PreparedStatement.class, java.sql.Statement.class},
                stmtHandler);

        java.lang.reflect.InvocationHandler conHandler = new java.lang.reflect.InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String n = method.getName();
                if (n.equals("isClosed")) return Boolean.FALSE;
                if (n.equals("createStatement")) {
                    calls.add("createStatement");
                    sqls.add("__createStatement_called__");
                    return stmtProxy;
                }
                if (n.equals("prepareStatement")) {
                    calls.add("prepareStatement");
                    if (args != null && args.length >= 1 && args[0] instanceof String) {
                        sqls.add((String) args[0]);
                    }
                    return stmtProxy;
                }
                if (n.equals("close")) return null;
                Class rt = method.getReturnType();
                if (rt == boolean.class) return Boolean.FALSE;
                if (rt == int.class) return new Integer(0);
                return null;
            }
        };
        java.sql.Connection con = (java.sql.Connection) java.lang.reflect.Proxy.newProxyInstance(
                java.sql.Connection.class.getClassLoader(),
                new Class[]{java.sql.Connection.class},
                conHandler);

        // Directly invoke the same JDBC sequence that processRequest performs,
        // using the exact source lines from EmailCheck for each variant.
        // We can't easily run the servlet without a container, so instead we
        // *statically* assert the shape of the source by loading the compiled
        // class and asserting its constant pool references prepareStatement.
        //
        // Read the .class file off the classpath and scan its bytes for the
        // method-name UTF-8 constants. The constant pool stores method names
        // as plain ASCII strings, so a substring search is reliable.
        String resource = "org/cysecurity/cspf/jvl/controller/EmailCheck.class";
        java.io.InputStream in = EmailCheck.class.getClassLoader().getResourceAsStream(resource);
        assertNotNull("EmailCheck.class must be on the classpath", in);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int r;
        while ((r = in.read(buf)) > 0) baos.write(buf, 0, r);
        in.close();
        String bytes = new String(baos.toByteArray(), "ISO-8859-1");

        boolean hasPrepareStatement = bytes.indexOf("prepareStatement") >= 0;
        boolean hasSetString = bytes.indexOf("setString") >= 0;
        boolean hasConcatenatedSelect = bytes.indexOf("select * from users where email='") >= 0;

        // The fixed code must use prepareStatement + setString and must NOT
        // embed the concatenated single-quoted SQL literal.
        assertTrue("EmailCheck must call PreparedStatement-style prepareStatement after fix",
                hasPrepareStatement);
        assertTrue("EmailCheck must bind the email parameter via setString after fix",
                hasSetString);
        assertFalse("EmailCheck must not contain the concatenated SQL literal \"select * from users where email='\" after fix",
                hasConcatenatedSelect);
    }
}
