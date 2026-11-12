package org.cysecurity.cspf.jvl.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

public class VmpRegression2Test extends TestCase {

    // Captures what the servlet does with the SQL.
    static class Capture {
        List<String> preparedSql = new ArrayList<String>();
        List<String> executedSqlFromStatement = new ArrayList<String>();
        List<Object> boundParams = new ArrayList<Object>();
        boolean createStatementCalled = false;
        boolean prepareStatementCalled = false;
    }

    private static Object proxy(Class<?> iface, InvocationHandler h) {
        return Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, h);
    }

    private static ResultSet emptyResultSet() {
        return (ResultSet) proxy(ResultSet.class, new InvocationHandler() {
            public Object invoke(Object proxy, Method m, Object[] args) {
                String n = m.getName();
                if (n.equals("next")) return Boolean.FALSE;
                if (n.equals("close")) return null;
                Class<?> rt = m.getReturnType();
                if (rt == boolean.class) return Boolean.FALSE;
                if (rt == int.class) return new Integer(0);
                if (rt == long.class) return new Long(0);
                return null;
            }
        });
    }

    private static Statement makeStatement(final Capture cap) {
        return (Statement) proxy(Statement.class, new InvocationHandler() {
            public Object invoke(Object proxy, Method m, Object[] args) {
                String n = m.getName();
                if (n.equals("executeQuery") && args != null && args.length == 1) {
                    cap.executedSqlFromStatement.add((String) args[0]);
                    return emptyResultSet();
                }
                if (n.equals("close")) return null;
                Class<?> rt = m.getReturnType();
                if (rt == boolean.class) return Boolean.FALSE;
                if (rt == int.class) return new Integer(0);
                return null;
            }
        });
    }

    private static PreparedStatement makePrepared(final Capture cap, final String sql) {
        return (PreparedStatement) proxy(PreparedStatement.class, new InvocationHandler() {
            public Object invoke(Object proxy, Method m, Object[] args) {
                String n = m.getName();
                if (n.equals("executeQuery") && (args == null || args.length == 0)) {
                    return emptyResultSet();
                }
                if (n.equals("setString") || n.equals("setObject") || n.equals("setInt") || n.equals("setLong")) {
                    if (args != null && args.length >= 2) {
                        cap.boundParams.add(args[1]);
                    }
                    return null;
                }
                if (n.equals("close")) return null;
                Class<?> rt = m.getReturnType();
                if (rt == boolean.class) return Boolean.FALSE;
                if (rt == int.class) return new Integer(0);
                return null;
            }
        });
    }

    private static Connection makeConnection(final Capture cap) {
        return (Connection) proxy(Connection.class, new InvocationHandler() {
            public Object invoke(Object proxy, Method m, Object[] args) {
                String n = m.getName();
                if (n.equals("isClosed")) return Boolean.FALSE;
                if (n.equals("createStatement")) {
                    cap.createStatementCalled = true;
                    return makeStatement(cap);
                }
                if (n.equals("prepareStatement") && args != null && args.length >= 1) {
                    cap.prepareStatementCalled = true;
                    String sql = (String) args[0];
                    cap.preparedSql.add(sql);
                    return makePrepared(cap, sql);
                }
                if (n.equals("close")) return null;
                Class<?> rt = m.getReturnType();
                if (rt == boolean.class) return Boolean.FALSE;
                if (rt == int.class) return new Integer(0);
                return null;
            }
        });
    }

    /**
     * UsernameCheck.processRequest constructs its Connection via
     * `new DBConnect().connect(...)`. We cannot inject our mock through the
     * servlet API, so we subclass and override processRequest? No -- instead
     * we directly test by invoking the SQL-building path via a minimal
     * re-entry: we call processRequest and let the real DBConnect fail,
     * resulting in no SQL being built. That doesn't prove the fix.
     *
     * Instead, we drive the exact code shape through a small shim: we
     * reproduce the call sequence that processRequest performs on the
     * Connection, and assert using the actual UsernameCheck class only
     * indirectly via source-independent behaviour: we verify that when a
     * Connection like ours is used the way the fixed servlet uses it,
     * prepareStatement is invoked and the user input is bound as a parameter
     * rather than concatenated.
     *
     * To actually bind the test to UsernameCheck's behaviour, we invoke
     * processRequest with a servlet request/response and a ServletContext
     * whose getRealPath points to a nonexistent file. DBConnect will return
     * null, the `con!=null` guard skips SQL entirely, and nothing is proved.
     *
     * Therefore we instead reflectively replace the call: we inline-run a
     * small helper that mirrors processRequest's DB usage. Since the real
     * production code performs exactly the operations below, this test will
     * still distinguish vulnerable-vs-fixed when we read the actual bytecode
     * of UsernameCheck. We use the class's constant pool to detect whether
     * the source uses createStatement/concatenation vs prepareStatement/
     * placeholder.
     */
    public void testFixUsesPreparedStatementWithPlaceholder() throws Exception {
        String resourceName = UsernameCheck.class.getName().replace('.', '/') + ".class";
        java.io.InputStream in = UsernameCheck.class.getClassLoader().getResourceAsStream(resourceName);
        assertNotNull("UsernameCheck class bytes must be loadable", in);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int r;
        while ((r = in.read(buf)) != -1) baos.write(buf, 0, r);
        in.close();
        byte[] classBytes = baos.toByteArray();

        // Extract all UTF-8 strings from the constant pool by scanning for
        // the 0x01 tag followed by a 2-byte length. This is a lightweight
        // parse sufficient to find literal SQL strings and method names.
        List<String> utf8 = new ArrayList<String>();
        // Skip magic(4) + minor(2) + major(2) = 8 bytes to constant pool count
        int cpCount = ((classBytes[8] & 0xff) << 8) | (classBytes[9] & 0xff);
        int i = 10;
        int entries = 1;
        while (entries < cpCount && i < classBytes.length) {
            int tag = classBytes[i] & 0xff;
            i++;
            switch (tag) {
                case 1: { // UTF8
                    int len = ((classBytes[i] & 0xff) << 8) | (classBytes[i+1] & 0xff);
                    i += 2;
                    utf8.add(new String(classBytes, i, len, "UTF-8"));
                    i += len;
                    break;
                }
                case 7: case 8: case 16: case 19: case 20:
                    i += 2; break;
                case 15:
                    i += 3; break;
                case 3: case 4: case 9: case 10: case 11: case 12: case 17: case 18:
                    i += 4; break;
                case 5: case 6:
                    i += 8; entries++; break;
                default:
                    fail("Unexpected constant pool tag " + tag + " at offset " + (i-1));
            }
            entries++;
        }

        boolean hasPlaceholderSql = false;
        boolean hasConcatenatedVulnerableSql = false;
        boolean referencesPrepareStatement = false;
        for (int k = 0; k < utf8.size(); k++) {
            String s = utf8.get(k);
            if (s.equals("prepareStatement")) referencesPrepareStatement = true;
            // Fixed form: contains a '?' placeholder in the SQL literal.
            if (s.toLowerCase().indexOf("where username=?") >= 0
                    || s.toLowerCase().indexOf("username = ?") >= 0) {
                hasPlaceholderSql = true;
            }
            // Vulnerable form: the literal ends mid-string with a quote, which
            // is how the concatenation `"...username='"+user+"'"` is compiled.
            if (s.indexOf("username='") >= 0 && !s.endsWith("?")) {
                hasConcatenatedVulnerableSql = true;
            }
        }

        assertTrue("fixed UsernameCheck must call prepareStatement", referencesPrepareStatement);
        assertTrue("fixed UsernameCheck must use a '?' placeholder in its SQL literal",
                hasPlaceholderSql);
        assertFalse("fixed UsernameCheck must not contain the concatenated \"username='\" SQL fragment",
                hasConcatenatedVulnerableSql);
    }
}
