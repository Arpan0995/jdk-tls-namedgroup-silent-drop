import javax.net.ssl.*;
import java.io.*;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Stock-JSSE TLS 1.3 client probe. NO Bouncy Castle on the client side.
 *
 * Usage: java ClientProbe <host> <port> <group1,group2,...>
 *        java ClientProbe <host> <port> -            (do not call setNamedGroups)
 *
 * Records, in machine-greppable PROBE lines:
 *   (a) whether an exception surfaced to the application,
 *   (b) whether the handshake succeeded,
 *   (d) every public-API avenue by which the app could learn the negotiated
 *       named group (Probe 3). (c), the actually negotiated group, must come
 *       from the javax.net.debug trace -- by design it is NOT available here.
 */
public class ClientProbe {

    public static void main(String[] args) {
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String[] groups = args[2].equals("-") ? null : args[2].split(",");

        System.out.println("PROBE-JDK: " + System.getProperty("java.runtime.version")
                + " (" + System.getProperty("java.vm.vendor") + ")");
        System.out.println("PROBE-REQUESTED-GROUPS: "
                + (groups == null ? "(default, setNamedGroups not called)" : String.join(",", groups)));

        try {
            TrustManager[] trustAll = { new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] c, String a) {}
                public void checkServerTrusted(X509Certificate[] c, String a) {}
            }};
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustAll, new SecureRandom());

            try (SSLSocket socket = (SSLSocket) ctx.getSocketFactory().createSocket(host, port)) {
                SSLParameters params = socket.getSSLParameters();
                params.setProtocols(new String[] {"TLSv1.3"});
                params.setServerNames(Collections.singletonList(new SNIHostName(host)));
                if (groups != null) {
                    params.setNamedGroups(groups);   // <-- the API under test
                }
                socket.setSSLParameters(params);

                socket.startHandshake();

                SSLSession session = socket.getSession();
                System.out.println("PROBE-RESULT: HANDSHAKE=SUCCESS EXCEPTION=NONE");
                System.out.println("PROBE-SESSION: protocol=" + session.getProtocol()
                        + " cipher=" + session.getCipherSuite());

                probe3(socket, session);

                BufferedWriter w = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                BufferedReader r = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                w.write("hello\n"); w.flush();
                System.out.println("PROBE-ECHO: " + r.readLine());
            }
        } catch (Exception e) {
            System.out.println("PROBE-RESULT: HANDSHAKE=FAILED EXCEPTION="
                    + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }

    /**
     * Probe 3: can the application learn the negotiated named group through
     * ANY public API? Enumerate every zero-arg public method on the session's
     * public types and on SSLParameters/SSLSocket whose name mentions
     * group/kex/key-exchange, and dump session value names.
     */
    private static void probe3(SSLSocket socket, SSLSession session) {
        System.out.println("PROBE3-SESSION-CLASS: " + session.getClass().getName());
        System.out.println("PROBE3-IS-EXTENDED: " + (session instanceof ExtendedSSLSession));

        List<Class<?>> apiTypes = Arrays.asList(
                SSLSession.class, ExtendedSSLSession.class, SSLParameters.class,
                SSLSocket.class, SSLEngine.class);
        boolean any = false;
        for (Class<?> t : apiTypes) {
            for (Method m : t.getMethods()) {
                String n = m.getName().toLowerCase();
                if (n.contains("group") || n.contains("keyexchange") || n.contains("kem")) {
                    System.out.println("PROBE3-API-CANDIDATE: " + t.getSimpleName() + "." + m.getName());
                    any = true;
                }
            }
        }
        if (!any) System.out.println("PROBE3-API-CANDIDATE: (none found on any javax.net.ssl type)");

        String[] after = socket.getSSLParameters().getNamedGroups();
        System.out.println("PROBE3-PARAMS-AFTER-HANDSHAKE: getNamedGroups()="
                + (after == null ? "null" : String.join(",", after))
                + "   <-- returns what was SET, not what was negotiated");
        System.out.println("PROBE3-SESSION-VALUE-NAMES: " + Arrays.toString(session.getValueNames()));
    }
}
