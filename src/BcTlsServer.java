import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;

/**
 * TLS 1.3 echo server on BCJSSE (Bouncy Castle JSSE provider).
 *
 * The set of named groups the server offers is controlled EXCLUSIVELY via the
 * -Djdk.tls.namedGroups=<group> system property (honored by BCJSSE), so each
 * run offers exactly one group and a successful handshake proves which group
 * was actually used -- no fallback path can contaminate the result.
 *
 * Server design (single-group runs, RSA-2048 self-signed keystore, echo data
 * path) follows the pqc-hybrid-vs-classical benchmark server (read-only
 * reference); the JSSE provider here is Bouncy Castle bctls 1.84.
 *
 * Usage: java -Djdk.tls.namedGroups=x25519 -cp lib/... BcTlsServer <port>
 */
public class BcTlsServer {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        String groups = System.getProperty("jdk.tls.namedGroups", "(not set)");

        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        Security.insertProviderAt(new BouncyCastleJsseProvider(), 2);

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = new FileInputStream("server.keystore")) {
            ks.load(in, "changeit".toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX", "BCJSSE");
        kmf.init(ks, "changeit".toCharArray());

        SSLContext ctx = SSLContext.getInstance("TLSv1.3", "BCJSSE");
        ctx.init(kmf.getKeyManagers(), null, new SecureRandom());

        SSLServerSocket ss = (SSLServerSocket) ctx.getServerSocketFactory().createServerSocket(port);
        SSLParameters params = ss.getSSLParameters();
        params.setProtocols(new String[] {"TLSv1.3"});
        ss.setSSLParameters(params);

        System.out.println("[server] BCJSSE provider: " + Security.getProvider("BCJSSE"));
        System.out.println("[server] jdk.tls.namedGroups=" + groups);
        System.out.println("[server] listening on port " + port);

        while (true) {
            try (SSLSocket s = (SSLSocket) ss.accept()) {
                s.startHandshake();
                SSLSession sess = s.getSession();
                System.out.println("[server] handshake ok: " + sess.getProtocol()
                        + " " + sess.getCipherSuite());
                BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()));
                BufferedWriter w = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                String line = r.readLine();
                w.write("OK: " + line + "\n");
                w.flush();
            } catch (Exception e) {
                System.out.println("[server] handshake failed: " + e);
            }
        }
    }
}
