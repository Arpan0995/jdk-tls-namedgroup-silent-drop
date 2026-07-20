import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Settles case (A) vs case (B) for the STATUS.md Probe 3 claim.
 *
 * Connects a stock-JSSE TLS 1.3 client with setNamedGroups({"MLKEM768","x25519"})
 * (C3 groups) to a real PQ-capable endpoint, then AFTER the handshake prints:
 *   1. the RETAINED SSLParameters object's getNamedGroups()   (case B read)
 *   2. a FRESH socket.getSSLParameters().getNamedGroups()     (case A read)
 *   3. identity hash codes proving the fresh object is a distinct instance
 *
 * Usage: java FreshVsRetained [host] [port]   (default pq.cloudflareresearch.com 443)
 */
public class FreshVsRetained {
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "pq.cloudflareresearch.com";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 443;
        String[] groups = {"MLKEM768", "x25519"};

        System.out.println("JDK: " + System.getProperty("java.runtime.version"));

        TrustManager[] trustAll = { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            public void checkClientTrusted(X509Certificate[] c, String a) {}
            public void checkServerTrusted(X509Certificate[] c, String a) {}
        }};
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, trustAll, new SecureRandom());

        try (SSLSocket socket = (SSLSocket) ctx.getSocketFactory().createSocket(host, port)) {
            SSLParameters retained = socket.getSSLParameters();
            retained.setProtocols(new String[] {"TLSv1.3"});
            retained.setServerNames(Collections.singletonList(new SNIHostName(host)));
            retained.setNamedGroups(groups);
            socket.setSSLParameters(retained);

            socket.startHandshake();
            System.out.println("HANDSHAKE: SUCCESS, protocol=" + socket.getSession().getProtocol()
                    + ", cipher=" + socket.getSession().getCipherSuite());

            SSLParameters fresh = socket.getSSLParameters();   // fresh, post-handshake
            System.out.println("RETAINED params.getNamedGroups()            = "
                    + String.join(",", retained.getNamedGroups()));
            System.out.println("FRESH socket.getSSLParameters().getNamedGroups() = "
                    + String.join(",", fresh.getNamedGroups()));
            System.out.println("identityHashCode retained=" + System.identityHashCode(retained)
                    + " fresh=" + System.identityHashCode(fresh)
                    + " sameObject=" + (retained == fresh));
            SSLParameters fresh2 = socket.getSSLParameters();
            System.out.println("second fresh call sameObject-as-first-fresh=" + (fresh == fresh2)
                    + " (each call returns a new instance: " + (fresh != fresh2) + ")");
        }
    }
}
