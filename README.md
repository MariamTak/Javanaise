# JVN2 — Proxy Approach avec test Burst

**1) Compilation et lancement du coordinateur**

```bash
cd src
javac jvn/*.java irc/*.java jvn/test/*.java
java jvn.CoordMain
```
**2) Lancer plusieurs instances du client IRC**
```bash
cd src
java irc.Irc
```


<img width="681" height="501" alt="image" src="https://github.com/user-attachments/assets/d0335ca6-897a-4cfa-addb-ef676abd0201" />

**3) Tests Burst**

Lancer Coord
```bash

   cd src
   java jvn.CoordMain
```

Lancer les clients Burst

Terminal 1 :
```bash

   cd src
   java jvn.test.BurstClient 0
```

Terminal 2 :
```bash

   cd src
   java jvn.test.BurstClient 1
```

Lancer le démo Burst
```bash

   cd src
   java jvn.test.BurstDemo
```


