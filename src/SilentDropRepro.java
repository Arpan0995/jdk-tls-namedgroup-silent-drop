import javax.net.ssl.*;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Minimal reproducer, fully offline (no server, no network, no dependencies):
 * SSLParameters.setNamedGroups() silently accepts named groups the provider
 * does not support and drops them from the ClientHello -- no exception, no
 * warning, no public API to detect it. By contrast, an unsupported PROTOCOL
 * name on the same parameter object throws IllegalArgumentException at once.
 *
 * Run (one command, stock JDK 21+):
 *   java src/SilentDropRepro.java                        # groups: X25519MLKEM768,x25519
 *   java src/SilentDropRepro.java MLKEM768,x25519        # persists on JDK 27 EA (out of JEP 527 scope)
 *   java src/SilentDropRepro.java NoSuchGroup12345,x25519    # generic, not PQC-specific
 *
 * Add -Djavax.net.debug=ssl:handshake and grep "named group" for the only
 * evidence the JDK ever emits: "Ignore unspecified named group: <name>".
 */
public class SilentDropRepro {
    public static void main(String[] args) throws Exception {
        String[] requested = (args.length > 0 ? args[0] : "X25519MLKEM768,x25519").split(",");

        SSLContext ctx = SSLContext.getDefault();
        String[] supported = ctx.getSupportedSSLParameters().getNamedGroups();
        System.out.println("JDK:              " + System.getProperty("java.runtime.version"));
        System.out.println("Supported groups: "
                + (supported == null ? "(null: provider default, not enumerable)" : String.join(",", supported)));
        System.out.println("Requested groups: " + String.join(",", requested));

        List<String> dropped = new ArrayList<>(List.of(requested));
        if (supported != null) dropped.removeAll(Arrays.asList(supported));

        // The API under test: request the groups, then produce a ClientHello
        // entirely in memory via SSLEngine -- no socket is ever opened.
        SSLEngine engine = ctx.createSSLEngine();
        engine.setUseClientMode(true);
        SSLParameters params = engine.getSSLParameters();
        params.setProtocols(new String[] {"TLSv1.3"});
        params.setNamedGroups(requested);                // silently accepts unsupported names
        engine.setSSLParameters(params);
        System.out.println("setNamedGroups(" + String.join(",", requested) + "): accepted, NO exception");
        System.out.println("getNamedGroups() reads back:  "
                + String.join(",", engine.getSSLParameters().getNamedGroups())
                + "   <-- what was SET, even if dropped");

        engine.beginHandshake();
        ByteBuffer net = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
        SSLEngineResult res = engine.wrap(ByteBuffer.allocate(0), net);
        System.out.println("ClientHello produced: " + res.getStatus() + ", " + net.position()
                + " bytes, NO exception");

        // Control: unsupported PROTOCOL on the same API family fails fast.
        String control;
        try {
            ctx.createSSLEngine().setEnabledProtocols(new String[] {"TLSv9.9"});
            control = "accepted, NO exception (unexpected)";
        } catch (IllegalArgumentException e) {
            control = "threw IllegalArgumentException: " + e.getMessage();
        }
        System.out.println("Control setEnabledProtocols(TLSv9.9): " + control);

        System.out.println();
        if (!dropped.isEmpty()) {
            System.out.println("=> Unsupported named group(s) " + dropped + " were silently accepted and");
            System.out.println("   dropped from the ClientHello; an unsupported protocol throws immediately.");
            System.out.println("   Only evidence: -Djavax.net.debug=ssl:handshake 2>&1 | grep \"named group\"");
        } else if (supported == null) {
            System.out.println("=> This JDK does not enumerate supported groups (getNamedGroups()==null);");
            System.out.println("   run with -Djavax.net.debug=ssl:handshake and grep \"named group\" to see drops.");
        } else {
            System.out.println("=> All requested groups are supported on this JDK; nothing was dropped here.");
            System.out.println("   Try: java src/SilentDropRepro.java MLKEM768,x25519   (unsupported even on JDK 27 EA)");
        }
    }
}
