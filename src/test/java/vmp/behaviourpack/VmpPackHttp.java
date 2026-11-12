package vmp.behaviourpack;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * The Behaviour Pack's servlet fakes (ADR 0080). Written by the platform, not by a
 * model. A request, response, session and servlet config built on
 * java.lang.reflect.Proxy, so they hold on any version of the servlet API and
 * add no dependency. A method they do not fake answers its type's default and
 * is listed by unfaked(), so a test can tell.
 *
 * <pre>
 * VmpPackHttp.Request req = VmpPackHttp.request("POST", "/login").param("user", "a");
 * VmpPackHttp.Response res = VmpPackHttp.response();
 * servlet.service(req.proxy(), res.proxy());
 * assertEquals("/home.jsp", req.forwardedTo());
 * </pre>
 */
public final class VmpPackHttp {

    private VmpPackHttp() {
    }

    public static Request request(String method, String path) {
        return new Request(method, path);
    }

    public static Response response() {
        return new Response();
    }

    public static Session session() {
        return new Session();
    }

    public static Config config() {
        return new Config();
    }

    static final List<String> UNFAKED = Collections.synchronizedList(new ArrayList<String>());

    /** Every method a test's code called that these fakes do not answer. */
    public static List<String> unfaked() {
        return new ArrayList<String>(UNFAKED);
    }

    static Object defaultFor(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) {
            return null;
        }
        if (type == boolean.class) {
            return Boolean.FALSE;
        }
        if (type == char.class) {
            return Character.valueOf('\0');
        }
        if (type == long.class) {
            return Long.valueOf(0L);
        }
        if (type == float.class) {
            return Float.valueOf(0f);
        }
        if (type == double.class) {
            return Double.valueOf(0d);
        }
        if (type == byte.class) {
            return Byte.valueOf((byte) 0);
        }
        if (type == short.class) {
            return Short.valueOf((short) 0);
        }
        return Integer.valueOf(0);
    }

    static Object unfakedCall(String owner, Method m) {
        UNFAKED.add(owner + "." + m.getName());
        return defaultFor(m.getReturnType());
    }

    static Object objectMethod(Object self, Object proxy, Method m, Object[] a) {
        String n = m.getName();
        if (n.equals("toString")) {
            return "vmp fake " + self.getClass().getSimpleName();
        }
        if (n.equals("hashCode")) {
            return Integer.valueOf(System.identityHashCode(proxy));
        }
        if (n.equals("equals")) {
            return Boolean.valueOf(a != null && a.length == 1 && a[0] == proxy);
        }
        return null;
    }

    static boolean isObjectMethod(Method m) {
        return m.getDeclaringClass() == Object.class;
    }

    static Object proxyOf(Class<?> type, InvocationHandler handler) {
        return Proxy.newProxyInstance(VmpPackHttp.class.getClassLoader(), new Class<?>[] {type}, handler);
    }

    static String arg(Object[] a, int i) {
        return a == null || a.length <= i || a[i] == null ? null : String.valueOf(a[i]);
    }

    /**
     * A RequestDispatcher that records where it sent the request, on the fake
     * request it was handed: from the request or from the servlet context alike.
     */
    static Object dispatcher(final String target) {
        return proxyOf(javax.servlet.RequestDispatcher.class, new InvocationHandler() {
            public Object invoke(Object p, Method m, Object[] a) {
                if (isObjectMethod(m)) {
                    return objectMethod(this, p, m, a);
                }
                Request request = null;
                if (a != null && a.length > 0 && a[0] != null && Proxy.isProxyClass(a[0].getClass())) {
                    InvocationHandler h = Proxy.getInvocationHandler(a[0]);
                    if (h instanceof Request) {
                        request = (Request) h;
                    }
                }
                if (m.getName().equals("forward")) {
                    if (request != null) {
                        request.forwardedTo = target;
                    }
                    return null;
                }
                if (m.getName().equals("include")) {
                    if (request != null) {
                        request.included.add(target);
                    }
                    return null;
                }
                return unfakedCall("RequestDispatcher", m);
            }
        });
    }

    // --- the session -------------------------------------------------------------

    public static final class Session implements InvocationHandler {
        final Map<String, Object> attributes = new LinkedHashMap<String, Object>();
        boolean invalidated;
        int maxInactive;
        private Object proxy;

        public Session attribute(String name, Object value) {
            attributes.put(name, value);
            return this;
        }

        public Object attribute(String name) {
            return attributes.get(name);
        }

        public boolean invalidated() {
            return invalidated;
        }

        public javax.servlet.http.HttpSession proxy() {
            if (proxy == null) {
                proxy = proxyOf(javax.servlet.http.HttpSession.class, this);
            }
            return (javax.servlet.http.HttpSession) proxy;
        }

        public Object invoke(Object p, Method m, Object[] a) {
            if (isObjectMethod(m)) {
                return objectMethod(this, p, m, a);
            }
            String n = m.getName();
            if (n.equals("getAttribute") || n.equals("getValue")) {
                return attributes.get(arg(a, 0));
            }
            if (n.equals("setAttribute") || n.equals("putValue")) {
                if (a[1] == null) {
                    attributes.remove(arg(a, 0));
                } else {
                    attributes.put(arg(a, 0), a[1]);
                }
                return null;
            }
            if (n.equals("removeAttribute") || n.equals("removeValue")) {
                attributes.remove(arg(a, 0));
                return null;
            }
            if (n.equals("getAttributeNames")) {
                return Collections.enumeration(new ArrayList<String>(attributes.keySet()));
            }
            if (n.equals("getValueNames")) {
                return attributes.keySet().toArray(new String[0]);
            }
            if (n.equals("invalidate")) {
                invalidated = true;
                attributes.clear();
                return null;
            }
            if (n.equals("getId")) {
                return "vmp-session";
            }
            if (n.equals("isNew")) {
                return Boolean.FALSE;
            }
            if (n.equals("getMaxInactiveInterval")) {
                return Integer.valueOf(maxInactive);
            }
            if (n.equals("setMaxInactiveInterval")) {
                maxInactive = ((Integer) a[0]).intValue();
                return null;
            }
            if (n.equals("getServletContext")) {
                return new Config().context();
            }
            return unfakedCall("HttpSession", m);
        }
    }

    // --- the servlet config and context -------------------------------------------

    public static final class Config implements InvocationHandler {
        final Map<String, String> initParameters = new LinkedHashMap<String, String>();
        final Map<String, Object> contextAttributes = new LinkedHashMap<String, Object>();
        private Object proxy;
        private Object contextProxy;

        public Config init(String name, String value) {
            initParameters.put(name, value);
            return this;
        }

        public javax.servlet.ServletConfig proxy() {
            if (proxy == null) {
                proxy = proxyOf(javax.servlet.ServletConfig.class, this);
            }
            return (javax.servlet.ServletConfig) proxy;
        }

        public javax.servlet.ServletContext context() {
            if (contextProxy == null) {
                contextProxy = proxyOf(javax.servlet.ServletContext.class, new Context(this));
            }
            return (javax.servlet.ServletContext) contextProxy;
        }

        public Object invoke(Object p, Method m, Object[] a) {
            if (isObjectMethod(m)) {
                return objectMethod(this, p, m, a);
            }
            String n = m.getName();
            if (n.equals("getInitParameter")) {
                return initParameters.get(arg(a, 0));
            }
            if (n.equals("getInitParameterNames")) {
                return Collections.enumeration(new ArrayList<String>(initParameters.keySet()));
            }
            if (n.equals("getServletName")) {
                return "vmp";
            }
            if (n.equals("getServletContext")) {
                return context();
            }
            return unfakedCall("ServletConfig", m);
        }
    }

    static final class Context implements InvocationHandler {
        final Config config;

        Context(Config config) {
            this.config = config;
        }

        public Object invoke(Object p, Method m, Object[] a) {
            if (isObjectMethod(m)) {
                return objectMethod(this, p, m, a);
            }
            String n = m.getName();
            if (n.equals("getAttribute")) {
                return config.contextAttributes.get(arg(a, 0));
            }
            if (n.equals("setAttribute")) {
                config.contextAttributes.put(arg(a, 0), a[1]);
                return null;
            }
            if (n.equals("removeAttribute")) {
                config.contextAttributes.remove(arg(a, 0));
                return null;
            }
            if (n.equals("getAttributeNames")) {
                return Collections.enumeration(new ArrayList<String>(config.contextAttributes.keySet()));
            }
            if (n.equals("getInitParameter")) {
                return config.initParameters.get(arg(a, 0));
            }
            if (n.equals("getInitParameterNames")) {
                return Collections.enumeration(new ArrayList<String>(config.initParameters.keySet()));
            }
            if (n.equals("getContextPath")) {
                return "";
            }
            if (n.equals("getServerInfo") || n.equals("getServletContextName")) {
                return "vmp";
            }
            if (n.equals("log")) {
                return null;
            }
            if (n.equals("getRequestDispatcher") || n.equals("getNamedDispatcher")) {
                return dispatcher(arg(a, 0));
            }
            if (n.equals("getRealPath") || n.equals("getResource") || n.equals("getResourceAsStream")
                    || n.equals("getMimeType")) {
                return null;
            }
            return unfakedCall("ServletContext", m);
        }
    }

    // --- the request ------------------------------------------------------------------

    public static final class Request implements InvocationHandler {
        final String method;
        final String path;
        final Map<String, List<String>> parameters = new LinkedHashMap<String, List<String>>();
        final Map<String, List<String>> headers = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
        final Map<String, Object> attributes = new LinkedHashMap<String, Object>();
        final Map<String, String> cookies = new LinkedHashMap<String, String>();
        String body = "";
        String contentType;
        String characterEncoding = "UTF-8";
        String queryString;
        Session session;
        String forwardedTo;
        final List<String> included = new ArrayList<String>();
        private Object proxy;

        Request(String method, String path) {
            this.method = method == null ? "GET" : method.toUpperCase(Locale.ENGLISH);
            int q = path == null ? -1 : path.indexOf('?');
            this.path = path == null ? "/" : (q >= 0 ? path.substring(0, q) : path);
            this.queryString = q >= 0 ? path.substring(q + 1) : null;
        }

        public Request param(String name, String... values) {
            List<String> list = parameters.get(name);
            if (list == null) {
                list = new ArrayList<String>();
                parameters.put(name, list);
            }
            for (String v : values) {
                list.add(v);
            }
            return this;
        }

        public Request header(String name, String value) {
            List<String> list = headers.get(name);
            if (list == null) {
                list = new ArrayList<String>();
                headers.put(name, list);
            }
            list.add(value);
            return this;
        }

        public Request cookie(String name, String value) {
            cookies.put(name, value);
            return this;
        }

        public Request attribute(String name, Object value) {
            attributes.put(name, value);
            return this;
        }

        public Request body(String text, String type) {
            this.body = text == null ? "" : text;
            this.contentType = type;
            return this;
        }

        public Request session(Session s) {
            this.session = s;
            return this;
        }

        /** The session the code under test used or made, or null. */
        public Session session() {
            return session;
        }

        public Object attribute(String name) {
            return attributes.get(name);
        }

        /** Where a RequestDispatcher forwarded, or null. */
        public String forwardedTo() {
            return forwardedTo;
        }

        public List<String> included() {
            return new ArrayList<String>(included);
        }

        public javax.servlet.http.HttpServletRequest proxy() {
            if (proxy == null) {
                proxy = proxyOf(javax.servlet.http.HttpServletRequest.class, this);
            }
            return (javax.servlet.http.HttpServletRequest) proxy;
        }

        public Object invoke(Object p, Method m, Object[] a) {
            if (isObjectMethod(m)) {
                return objectMethod(this, p, m, a);
            }
            String n = m.getName();
            if (n.equals("getMethod")) {
                return method;
            }
            if (n.equals("getRequestURI") || n.equals("getServletPath")) {
                return path;
            }
            if (n.equals("getRequestURL")) {
                return new StringBuffer("http" + ":" + "//localhost" + path);
            }
            if (n.equals("getContextPath")) {
                return "";
            }
            if (n.equals("getPathInfo") || n.equals("getPathTranslated")) {
                return null;
            }
            if (n.equals("getQueryString")) {
                return queryString;
            }
            if (n.equals("getParameter")) {
                List<String> v = parameters.get(arg(a, 0));
                return v == null || v.isEmpty() ? null : v.get(0);
            }
            if (n.equals("getParameterValues")) {
                List<String> v = parameters.get(arg(a, 0));
                return v == null ? null : v.toArray(new String[0]);
            }
            if (n.equals("getParameterNames")) {
                return Collections.enumeration(new ArrayList<String>(parameters.keySet()));
            }
            if (n.equals("getParameterMap")) {
                Map<String, String[]> out = new LinkedHashMap<String, String[]>();
                for (Map.Entry<String, List<String>> e : parameters.entrySet()) {
                    out.put(e.getKey(), e.getValue().toArray(new String[0]));
                }
                return Collections.unmodifiableMap(out);
            }
            if (n.equals("getHeader")) {
                List<String> v = headers.get(arg(a, 0));
                return v == null || v.isEmpty() ? null : v.get(0);
            }
            if (n.equals("getHeaders")) {
                List<String> v = headers.get(arg(a, 0));
                return Collections.enumeration(v == null ? new ArrayList<String>() : v);
            }
            if (n.equals("getHeaderNames")) {
                return Collections.enumeration(new ArrayList<String>(headers.keySet()));
            }
            if (n.equals("getIntHeader")) {
                List<String> v = headers.get(arg(a, 0));
                return Integer.valueOf(v == null || v.isEmpty() ? -1 : Integer.parseInt(v.get(0)));
            }
            if (n.equals("getDateHeader")) {
                return Long.valueOf(-1L);
            }
            if (n.equals("getAttribute")) {
                return attributes.get(arg(a, 0));
            }
            if (n.equals("setAttribute")) {
                attributes.put(arg(a, 0), a[1]);
                return null;
            }
            if (n.equals("removeAttribute")) {
                attributes.remove(arg(a, 0));
                return null;
            }
            if (n.equals("getAttributeNames")) {
                return Collections.enumeration(new ArrayList<String>(attributes.keySet()));
            }
            if (n.equals("getSession")) {
                boolean create = a == null || a.length == 0 || Boolean.TRUE.equals(a[0]);
                if (session == null && create) {
                    session = new Session();
                }
                return session == null ? null : session.proxy();
            }
            if (n.equals("getRequestedSessionId")) {
                return session == null ? null : "vmp-session";
            }
            if (n.equals("isRequestedSessionIdValid")) {
                return Boolean.valueOf(session != null && !session.invalidated);
            }
            if (n.equals("getCookies")) {
                if (cookies.isEmpty()) {
                    return null;
                }
                javax.servlet.http.Cookie[] out = new javax.servlet.http.Cookie[cookies.size()];
                int i = 0;
                for (Map.Entry<String, String> e : cookies.entrySet()) {
                    out[i++] = new javax.servlet.http.Cookie(e.getKey(), e.getValue());
                }
                return out;
            }
            if (n.equals("getReader")) {
                return new BufferedReader(new StringReader(body));
            }
            if (n.equals("getContentType")) {
                return contentType;
            }
            if (n.equals("getContentLength")) {
                return Integer.valueOf(body.length());
            }
            if (n.equals("getContentLengthLong")) {
                return Long.valueOf(body.length());
            }
            if (n.equals("getCharacterEncoding")) {
                return characterEncoding;
            }
            if (n.equals("setCharacterEncoding")) {
                characterEncoding = arg(a, 0);
                return null;
            }
            if (n.equals("getRemoteAddr") || n.equals("getLocalAddr")) {
                return "127.0.0.1";
            }
            if (n.equals("getRemoteHost") || n.equals("getServerName") || n.equals("getLocalName")) {
                return "localhost";
            }
            if (n.equals("getServerPort") || n.equals("getLocalPort") || n.equals("getRemotePort")) {
                return Integer.valueOf(80);
            }
            if (n.equals("getScheme")) {
                return "http";
            }
            if (n.equals("getProtocol")) {
                return "HTTP/1.1";
            }
            if (n.equals("isSecure") || n.equals("isUserInRole")) {
                return Boolean.FALSE;
            }
            if (n.equals("getRemoteUser") || n.equals("getUserPrincipal") || n.equals("getAuthType")) {
                return null;
            }
            if (n.equals("getLocale")) {
                return Locale.US;
            }
            if (n.equals("getLocales")) {
                return Collections.enumeration(Collections.singletonList(Locale.US));
            }
            if (n.equals("getRequestDispatcher")) {
                return dispatcher(arg(a, 0));
            }
            if (n.equals("getServletContext")) {
                return new Config().context();
            }
            return unfakedCall("HttpServletRequest", m);
        }
    }

    // --- the response ------------------------------------------------------------------

    public static final class Response implements InvocationHandler {
        int status = 200;
        String error;
        String redirect;
        String contentType;
        String characterEncoding = "UTF-8";
        final Map<String, List<String>> headers = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
        final Map<String, String> cookies = new LinkedHashMap<String, String>();
        final StringWriter body = new StringWriter();
        final PrintWriter writer = new PrintWriter(body);
        boolean committed;
        private Object proxy;

        public int status() {
            return status;
        }

        public String body() {
            writer.flush();
            return body.toString();
        }

        public String redirect() {
            return redirect;
        }

        public String error() {
            return error;
        }

        public String contentType() {
            return contentType;
        }

        public String header(String name) {
            List<String> v = headers.get(name);
            return v == null || v.isEmpty() ? null : v.get(0);
        }

        public String cookie(String name) {
            return cookies.get(name);
        }

        public boolean committed() {
            return committed;
        }

        public javax.servlet.http.HttpServletResponse proxy() {
            if (proxy == null) {
                proxy = proxyOf(javax.servlet.http.HttpServletResponse.class, this);
            }
            return (javax.servlet.http.HttpServletResponse) proxy;
        }

        void put(String name, String value, boolean add) {
            List<String> list = headers.get(name);
            if (list == null || !add) {
                list = new ArrayList<String>();
                headers.put(name, list);
            }
            list.add(value);
        }

        public Object invoke(Object p, Method m, Object[] a) {
            if (isObjectMethod(m)) {
                return objectMethod(this, p, m, a);
            }
            String n = m.getName();
            if (n.equals("setStatus")) {
                status = ((Integer) a[0]).intValue();
                return null;
            }
            if (n.equals("getStatus")) {
                return Integer.valueOf(status);
            }
            if (n.equals("sendError")) {
                status = ((Integer) a[0]).intValue();
                error = a.length > 1 ? arg(a, 1) : "";
                committed = true;
                return null;
            }
            if (n.equals("sendRedirect")) {
                status = 302;
                redirect = arg(a, 0);
                committed = true;
                return null;
            }
            if (n.equals("setHeader") || n.equals("setIntHeader") || n.equals("setDateHeader")) {
                put(arg(a, 0), arg(a, 1), false);
                return null;
            }
            if (n.equals("addHeader") || n.equals("addIntHeader") || n.equals("addDateHeader")) {
                put(arg(a, 0), arg(a, 1), true);
                return null;
            }
            if (n.equals("containsHeader")) {
                return Boolean.valueOf(headers.containsKey(arg(a, 0)));
            }
            if (n.equals("getHeader")) {
                return header(arg(a, 0));
            }
            if (n.equals("getHeaders")) {
                List<String> v = headers.get(arg(a, 0));
                return v == null ? new ArrayList<String>() : new ArrayList<String>(v);
            }
            if (n.equals("getHeaderNames")) {
                return new ArrayList<String>(headers.keySet());
            }
            if (n.equals("setContentType")) {
                contentType = arg(a, 0);
                return null;
            }
            if (n.equals("getContentType")) {
                return contentType;
            }
            if (n.equals("setCharacterEncoding")) {
                characterEncoding = arg(a, 0);
                return null;
            }
            if (n.equals("getCharacterEncoding")) {
                return characterEncoding;
            }
            if (n.equals("getWriter")) {
                return writer;
            }
            if (n.equals("getOutputStream")) {
                throw new UnsupportedOperationException(
                        "vmp fake response: getOutputStream is not faked; the code under test must use getWriter");
            }
            if (n.equals("addCookie")) {
                Object c = a[0];
                try {
                    String name = String.valueOf(c.getClass().getMethod("getName").invoke(c));
                    String value = String.valueOf(c.getClass().getMethod("getValue").invoke(c));
                    cookies.put(name, value);
                } catch (Exception e) {
                    throw new IllegalStateException("vmp fake response: unreadable cookie", e);
                }
                return null;
            }
            if (n.equals("isCommitted")) {
                return Boolean.valueOf(committed);
            }
            if (n.equals("flushBuffer")) {
                committed = true;
                writer.flush();
                return null;
            }
            if (n.equals("reset") || n.equals("resetBuffer")) {
                body.getBuffer().setLength(0);
                return null;
            }
            if (n.startsWith("encode")) {
                return arg(a, 0);
            }
            if (n.equals("getLocale")) {
                return Locale.US;
            }
            if (n.equals("setLocale") || n.equals("setBufferSize") || n.equals("setContentLength")
                    || n.equals("setContentLengthLong")) {
                return null;
            }
            if (n.equals("getBufferSize")) {
                return Integer.valueOf(8192);
            }
            return unfakedCall("HttpServletResponse", m);
        }
    }
}
