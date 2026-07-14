### C1 — JDK 26
```
PROBE-JDK: 26.0.1+8-34 (Oracle Corporation)
PROBE-REQUESTED-GROUPS: X25519MLKEM768,x25519
javax.net.ssl|DEBUG|30|main|2026-07-14 11:54:46.442 CDT|SupportedGroupsExtension.java:181|Ignore unspecified named group: X25519MLKEM768
PROBE-RESULT: HANDSHAKE=SUCCESS EXCEPTION=NONE
ServerHello key_share          "named group": x25519
```

### C1 — JDK 27
```
PROBE-JDK: 27-ea+30-2302 (Oracle Corporation)
PROBE-REQUESTED-GROUPS: X25519MLKEM768,x25519
PROBE-RESULT: HANDSHAKE=SUCCESS EXCEPTION=NONE
ServerHello key_share          "named group": x25519
```

### C2 — JDK 26
```
PROBE-JDK: 26.0.1+8-34 (Oracle Corporation)
PROBE-REQUESTED-GROUPS: X25519MLKEM768
javax.net.ssl|DEBUG|30|main|2026-07-14 11:54:48.957 CDT|SupportedGroupsExtension.java:181|Ignore unspecified named group: X25519MLKEM768
javax.net.ssl|WARNING|30|main|2026-07-14 11:54:48.959 CDT|KeyShareExtension.java:241|Ignore key_share extension, no supported groups
PROBE-RESULT: HANDSHAKE=FAILED EXCEPTION=javax.net.ssl.SSLHandshakeException: (handshake_failure) Received fatal alert: handshake_failure
```

### C2 — JDK 27
```
PROBE-JDK: 27-ea+30-2302 (Oracle Corporation)
PROBE-REQUESTED-GROUPS: X25519MLKEM768
PROBE-RESULT: HANDSHAKE=SUCCESS EXCEPTION=NONE
ServerHello key_share          "named group": X25519MLKEM768
```

### C3 — JDK 26
```
PROBE-JDK: 26.0.1+8-34 (Oracle Corporation)
PROBE-REQUESTED-GROUPS: MLKEM768,x25519
javax.net.ssl|DEBUG|30|main|2026-07-14 11:54:51.244 CDT|SupportedGroupsExtension.java:181|Ignore unspecified named group: MLKEM768
PROBE-RESULT: HANDSHAKE=SUCCESS EXCEPTION=NONE
ServerHello key_share          "named group": x25519
```

### C3 — JDK 27
```
PROBE-JDK: 27-ea+30-2302 (Oracle Corporation)
PROBE-REQUESTED-GROUPS: MLKEM768,x25519
javax.net.ssl|DEBUG|30|main|2026-07-14 11:54:51.771 CDT|SupportedGroupsExtension.java:200|Ignore inactive or disabled named group: MLKEM768
PROBE-RESULT: HANDSHAKE=SUCCESS EXCEPTION=NONE
ServerHello key_share          "named group": x25519
```

### C4 — JDK 26
```
PROBE-JDK: 26.0.1+8-34 (Oracle Corporation)
PROBE-REQUESTED-GROUPS: MLKEM768
javax.net.ssl|DEBUG|30|main|2026-07-14 11:54:53.623 CDT|SupportedGroupsExtension.java:181|Ignore unspecified named group: MLKEM768
javax.net.ssl|WARNING|30|main|2026-07-14 11:54:53.625 CDT|KeyShareExtension.java:241|Ignore key_share extension, no supported groups
PROBE-RESULT: HANDSHAKE=FAILED EXCEPTION=javax.net.ssl.SSLHandshakeException: (handshake_failure) Received fatal alert: handshake_failure
```

### C4 — JDK 27
```
PROBE-JDK: 27-ea+30-2302 (Oracle Corporation)
PROBE-REQUESTED-GROUPS: MLKEM768
javax.net.ssl|DEBUG|30|main|2026-07-14 11:54:54.033 CDT|SupportedGroupsExtension.java:200|Ignore inactive or disabled named group: MLKEM768
javax.net.ssl|WARNING|30|main|2026-07-14 11:54:54.034 CDT|KeyShareExtension.java:247|Ignore key_share extension, no supported groups
PROBE-RESULT: HANDSHAKE=FAILED EXCEPTION=javax.net.ssl.SSLHandshakeException: (handshake_failure) Received fatal alert: handshake_failure
```

### C5 — JDK 26
```
PROBE-JDK: 26.0.1+8-34 (Oracle Corporation)
PROBE-REQUESTED-GROUPS: NoSuchGroup12345,x25519
javax.net.ssl|DEBUG|30|main|2026-07-14 11:54:55.836 CDT|SupportedGroupsExtension.java:181|Ignore unspecified named group: NoSuchGroup12345
PROBE-RESULT: HANDSHAKE=SUCCESS EXCEPTION=NONE
ServerHello key_share          "named group": x25519
```

### C5 — JDK 27
```
PROBE-JDK: 27-ea+30-2302 (Oracle Corporation)
PROBE-REQUESTED-GROUPS: NoSuchGroup12345,x25519
javax.net.ssl|DEBUG|30|main|2026-07-14 11:54:56.290 CDT|SupportedGroupsExtension.java:183|Ignore unspecified named group: NoSuchGroup12345
PROBE-RESULT: HANDSHAKE=SUCCESS EXCEPTION=NONE
ServerHello key_share          "named group": x25519
```

## Probe 4 — pq.cloudflareresearch.com:443 (client requests {X25519MLKEM768, x25519})

### JDK 26
```
PROBE-JDK: 26.0.1+8-34 (Oracle Corporation)
javax.net.ssl|DEBUG|30|main|2026-07-14 11:59:37.544 CDT|SupportedGroupsExtension.java:181|Ignore unspecified named group: X25519MLKEM768
PROBE-RESULT: HANDSHAKE=SUCCESS EXCEPTION=NONE
ServerHello key_share          "named group": x25519
```

### JDK 27
```
PROBE-JDK: 27-ea+30-2302 (Oracle Corporation)
PROBE-RESULT: HANDSHAKE=SUCCESS EXCEPTION=NONE
ServerHello key_share          "named group": X25519MLKEM768
```
