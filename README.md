JVN Project — Instructions de compilation et d’exécution

1) Compilation et lancement du coordinateur

cd src
javac jvn/*.java irc/*.java jvn/test/*.java
java jvn.CoordMain

2) Lancer plusieurs instances du client IRC

cd src
java irc.Irc

3) Tests de performance : Burst
Lancer Coord
   cd src
   java jvn.CoordMain
Lancer les clients Burst
terminal 1 :
   cd src
   java jvn.test.BurstClient 0
terminal2 :
   cd src
   java jvn.test.BurstClient 1
Lancer le démo Burst
   cd src
   java jvn.test.BurstDemo

