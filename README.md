**1) compile java source files and start coordinator**

cd src

javac jvn/*.java irc/*.java


java jvn.JvnCoordImpl  

**2) in another terminal run IRC**

cd src

java irc.Irc
