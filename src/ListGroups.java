import javax.net.ssl.*;

/**
 * Probe 1: what does stock JSSE on THIS JDK actually support?
 *
 * Prints supported and default named groups via the public API
 * (SSLParameters.getNamedGroups(), public since JDK 20; null means
 * "provider-specific default"), plus a control experiment showing that an
 * unsupported PROTOCOL name thrown at setEnabledProtocols() surfaces an
 * IllegalArgumentException -- the comparison point for how unsupported
 * NAMED GROUPS ought to behave.
 */
public class ListGroups {

    public static void main(String[] args) throws Exception {
        System.out.println("JDK: " + System.getProperty("java.runtime.version")
                + " (" + System.getProperty("java.vm.vendor") + ")");

        SSLContext ctx = SSLContext.getDefault();

        String[] supported = ctx.getSupportedSSLParameters().getNamedGroups();
        String[] dflt = ctx.getDefaultSSLParameters().getNamedGroups();
        System.out.println("SUPPORTED-NAMED-GROUPS: " + fmt(supported));
        System.out.println("DEFAULT-NAMED-GROUPS:   " + fmt(dflt));
        System.out.println("jdk.tls.namedGroups property: "
                + System.getProperty("jdk.tls.namedGroups", "(not set)"));

        // Control: unsupported protocol version on the same API family.
        SSLSocket s = (SSLSocket) ctx.getSocketFactory().createSocket();
        try {
            s.setEnabledProtocols(new String[] {"TLSv9.9"});
            System.out.println("CONTROL-PROTOCOL: setEnabledProtocols(\"TLSv9.9\") was ACCEPTED (no exception)");
        } catch (IllegalArgumentException e) {
            System.out.println("CONTROL-PROTOCOL: setEnabledProtocols(\"TLSv9.9\") threw "
                    + e.getClass().getName() + ": " + e.getMessage());
        } finally {
            s.close();
        }

        // Control: does setNamedGroups itself validate? (garbage name, no handshake)
        try {
            SSLParameters p = new SSLParameters();
            p.setNamedGroups(new String[] {"NoSuchGroup12345"});
            System.out.println("CONTROL-GROUP: setNamedGroups(\"NoSuchGroup12345\") was ACCEPTED (no exception)");
        } catch (IllegalArgumentException e) {
            System.out.println("CONTROL-GROUP: setNamedGroups(\"NoSuchGroup12345\") threw "
                    + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private static String fmt(String[] a) {
        return a == null ? "null (provider default; not enumerable via public API)"
                         : String.join(", ", a) + "   [" + a.length + " groups]";
    }
}
