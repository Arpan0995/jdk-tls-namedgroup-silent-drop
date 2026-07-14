import javax.net.ssl.*;
import java.util.Collections;

/**
 * Minimal reproducer: SSLParameters.setNamedGroups() silently ignores named
 * groups the provider does not support; the handshake proceeds on a classical
 * group with no exception, no warning, and no public API to detect it.
 *
 * Run (one command, stock JDK, no dependencies):
 *   java SilentDropRepro.java                       # groups: X25519MLKEM768,x25519
 *   java SilentDropRepro.java MLKEM768,x25519       # persists on JDK 27 EA (out of JEP 527 scope)
 *   java SilentDropRepro.java NoSuchGroup12345,x25519   # generic, not PQC-specific
 *
 * Re-run with -Djavax.net.debug=ssl:handshake to see the only evidence:
 *   "Ignore unspecified named group: <name>"
 */
public class SilentDropRepro {
    public static void main(String[] args) throws Exception {
        String[] groups = (args.length > 0 ? args[0] : "X25519MLKEM768,x25519").split(",");
        String host = args.length > 1 ? args[1] : "cloudflare.com";

        SSLSocketFactory f = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket s = (SSLSocket) f.createSocket(host, 443)) {
            SSLParameters p = s.getSSLParameters();
            p.setProtocols(new String[] {"TLSv1.3"});
            p.setServerNames(Collections.singletonList(new SNIHostName(host)));
            p.setNamedGroups(groups);        // application believes this is now the policy
            s.setSSLParameters(p);

            s.startHandshake();              // no exception, even if groups[0] was dropped

            System.out.println("JDK:              " + System.getProperty("java.runtime.version"));
            System.out.println("Requested groups: " + String.join(",", groups));
            System.out.println("Handshake:        SUCCESS, " + s.getSession().getProtocol()
                    + ", " + s.getSession().getCipherSuite());
            System.out.println("Negotiated named group: NOT OBTAINABLE via any public API.");
            System.out.println("Re-run with -Djavax.net.debug=ssl:handshake and grep for");
            System.out.println("\"Ignore unspecified named group\" to see which groups were dropped.");
        }
    }
}
