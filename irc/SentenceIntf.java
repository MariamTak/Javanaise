package irc;

import jvn.annotation.JvnRead;
import jvn.annotation.JvnWrite;

public interface SentenceIntf {
    @JvnRead
    String read();

    @JvnWrite
    void write(String text);
}
