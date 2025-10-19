package irc;

import jvn.annotation.JvnRead;
import jvn.annotation.JvnWrite;

public interface SentenceIntf {
    @JvnRead
    public String read();

    @JvnWrite
    public void write(String text);
}
