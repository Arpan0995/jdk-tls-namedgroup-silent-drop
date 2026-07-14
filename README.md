# jdk-tls-namedgroup-silent-drop

Reproduction and characterization of a TLS API-contract hazard in stock JSSE:
`SSLParameters.setNamedGroups()` **silently ignores** named groups the provider
does not support. A client that requests a post-quantum (hybrid or pure ML-KEM)
group and gets it dropped proceeds to a **classical** handshake with no
exception, no warning, and — as Probe 3 shows — **no public API by which the
application can ever learn which group was actually negotiated**.

Verified on **JDK 26.0.1 GA (26.0.1+8-34)** and **JDK 27 EA (27-ea+30-2302)**,
i.e. the behavior **persists after JEP 527** (Post-Quantum Hybrid Key Exchange)
for any group outside JEP 527's scope — including pure `MLKEM768` and arbitrary
unknown names, proving it is a generic contract problem, not a PQC gap.

Key artifacts:

- [STATUS.md](STATUS.md) — pinned environment, full results matrix (C1–C5 × 2 JDKs), findings.
- [results/EVIDENCE.md](results/EVIDENCE.md) — verbatim `javax.net.debug` excerpts per case (machine-extracted from real traces).
- [src/SilentDropRepro.java](src/SilentDropRepro.java) — minimal standalone reproducer (41 lines, stock JDK, one command).
- [scripts/run_matrix.sh](scripts/run_matrix.sh) — re-runs the whole matrix from scratch.

## Quick demonstration

```bash
# stock JDK 26: request hybrid X25519MLKEM768 -> silently connects classical
java src/SilentDropRepro.java

# JDK 27 EA: pure ML-KEM is still silently dropped post-JEP-527
java src/SilentDropRepro.java MLKEM768,x25519

# the only evidence, buried in the debug trace:
java -Djavax.net.debug=ssl:handshake src/SilentDropRepro.java 2>&1 | grep "Ignore"
#   Ignore unspecified named group: X25519MLKEM768
```

## Reproducing the full matrix

1. Download JDK 26 GA and JDK 27 EA (macOS/AArch64 URLs pinned in STATUS.md)
   into `jdks/`, and Bouncy Castle 1.84 (`bcprov`/`bctls`/`bcutil-jdk18on`)
   into `lib/` (sha256 sums in STATUS.md).
2. Generate the server keystore:
   ```bash
   jdks/jdk-26.0.1.jdk/Contents/Home/bin/keytool -genkeypair -alias server \
     -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore server.keystore \
     -storepass changeit -keypass changeit -dname "CN=localhost" -validity 365
   ```
3. `./scripts/run_matrix.sh` — starts a BCJSSE TLS 1.3 server pinned to a
   single named group per case and runs the stock-JSSE client probe on both
   JDKs with `-Djavax.net.debug=ssl:handshake`. Traces land in `traces/`.

The single-group server design, RSA-2048 self-signed keystore, and
debug-trace negotiation-evidence method follow the
`pqc-hybrid-vs-classical` benchmark repository (used read-only; that repo is
frozen at the commit pinned in the companion paper).

## Relationship to prior work

Study B of *From Primitives to Protocol* observed the silent drop on stock
OpenJDK 26. This repository isolates the reportable defect: not "hybrid is
missing" (JEP 527 fixes that specific group on 27), but that JSSE silently
drops *any* unsupported named group while giving the application a false
belief that its key-exchange policy was applied — and provides no public API
to verify it. See STATUS.md for the C3/C5 evidence on JDK 27 EA.
