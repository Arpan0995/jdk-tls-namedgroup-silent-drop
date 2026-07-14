#!/bin/bash
# Probe 2 matrix: cases C1..C5 x {JDK 26 GA, JDK 27 EA} stock-JSSE clients
# against a BCJSSE (Bouncy Castle 1.84) TLS 1.3 server pinned to ONE named
# group per case. Evidence = client-side javax.net.debug traces in traces/.
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

JDK26="$ROOT/jdks/jdk-26.0.1.jdk/Contents/Home"
JDK27="$ROOT/jdks/jdk-27.jdk/Contents/Home"
BCCP="$ROOT/lib/bcprov-jdk18on-1.84.jar:$ROOT/lib/bctls-jdk18on-1.84.jar:$ROOT/lib/bcutil-jdk18on-1.84.jar"
SERVER_JAVA="$JDK26/bin/java"   # server JVM is pinned: JDK 26 + BCJSSE 1.84

mkdir -p traces

run_case() {
    local case="$1" server_group="$2" client_groups="$3" port="$4"
    echo "=== $case: server offers [$server_group], client requests [$client_groups] (port $port) ==="

    "$SERVER_JAVA" -Djdk.tls.namedGroups="$server_group" -cp "$BCCP" \
        src/BcTlsServer.java "$port" > "traces/${case}-server.log" 2>&1 &
    local spid=$!
    for i in $(seq 1 60); do nc -z localhost "$port" 2>/dev/null && break; sleep 0.5; done

    for jdk in 26 27; do
        local jhome; [ "$jdk" = 26 ] && jhome="$JDK26" || jhome="$JDK27"
        "$jhome/bin/java" -Djavax.net.debug=ssl:handshake \
            src/ClientProbe.java localhost "$port" "$client_groups" \
            > "traces/${case}-jdk${jdk}.log" 2>&1
        echo "--- $case / JDK $jdk (exit $?) ---"
        grep -E "^PROBE" "traces/${case}-jdk${jdk}.log" | head -8
        grep -m1 "Ignore unspecified named group" "traces/${case}-jdk${jdk}.log" || true
        grep -m1 '"named group"' "traces/${case}-jdk${jdk}.log" || true
    done

    kill "$spid" 2>/dev/null; wait "$spid" 2>/dev/null
}

#        case  server-group      client-groups                port
run_case C1    x25519            X25519MLKEM768,x25519        8451
run_case C2    X25519MLKEM768    X25519MLKEM768               8452
run_case C3    x25519            MLKEM768,x25519              8453
run_case C4    MLKEM768          MLKEM768                     8454
run_case C5    x25519            NoSuchGroup12345,x25519      8455

echo "Done. Full traces in traces/."
