# Probe 3 addendum — fresh vs retained SSLParameters (run 2026-07-19)

Settles which object the Probe 3 `getNamedGroups()` observation was made on.
`src/FreshVsRetained.java` connects a stock-JSSE TLS 1.3 client with
`setNamedGroups({"MLKEM768","x25519"})` (the C3 group list) to
`pq.cloudflareresearch.com:443`, then AFTER the handshake prints both the
RETAINED SSLParameters object's `getNamedGroups()` and a FRESH
`socket.getSSLParameters().getNamedGroups()`, plus identity hash codes.

Result: the fresh call returns a NEW provider-constructed SSLParameters
instance (distinct from the object passed to `setSSLParameters()`), and it
still reports the requested set — including the dropped MLKEM768 — not what
was negotiated. Raw `-Djavax.net.debug=ssl:handshake` stderr traces are the
adjacent `probe3-freshvsretained-jdk2{6,7}-debug.log`; both show the drop line
and ServerHello `key_share` → `"named group": x25519`.

## JDK 26.0.1 GA (26.0.1+8-34) — stdout, verbatim

```
JDK: 26.0.1+8-34
HANDSHAKE: SUCCESS, protocol=TLSv1.3, cipher=TLS_AES_256_GCM_SHA384
RETAINED params.getNamedGroups()            = MLKEM768,x25519
FRESH socket.getSSLParameters().getNamedGroups() = MLKEM768,x25519
identityHashCode retained=307829448 fresh=1518331471 sameObject=false
second fresh call sameObject-as-first-fresh=false (each call returns a new instance: true)
```

Trace excerpt (`probe3-freshvsretained-jdk26-debug.log`):

```
javax.net.ssl|DEBUG|30|main|2026-07-19 12:43:03.912 CDT|SupportedGroupsExtension.java:181|Ignore unspecified named group: MLKEM768
    "key_share (51)": {
      "server_share": {
        "named group": x25519
```

## JDK 27 EA (27-ea+30-2302) — stdout, verbatim

```
JDK: 27-ea+30-2302
HANDSHAKE: SUCCESS, protocol=TLSv1.3, cipher=TLS_AES_256_GCM_SHA384
RETAINED params.getNamedGroups()            = MLKEM768,x25519
FRESH socket.getSSLParameters().getNamedGroups() = MLKEM768,x25519
identityHashCode retained=214187874 fresh=1528923159 sameObject=false
second fresh call sameObject-as-first-fresh=false (each call returns a new instance: true)
```

Trace excerpt (`probe3-freshvsretained-jdk27-debug.log`):

```
javax.net.ssl|DEBUG|30|main|2026-07-19 12:44:12.713 CDT|SupportedGroupsExtension.java:200|Ignore inactive or disabled named group: MLKEM768
    "key_share (51)": {
      "server_share": {
        "named group": x25519
```

## Caveat

This run used the public Probe 4 endpoint (pq.cloudflareresearch.com), not the
local BCJSSE server, because `lib/` and `server.keystore` are gitignored and
were absent at run time. The client-side code path under test is identical to
C3 (`src/ClientProbe.java` probe3(), the `PROBE3-PARAMS-AFTER-HANDSHAKE` read
at `socket.getSSLParameters()`), and Probe 4 already established the finding
is server-independent.
