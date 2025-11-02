package jvn.test;


import jvn.annotation.JvnRead;
import jvn.annotation.JvnWrite;
import java.io.Serializable;

/**
 * Interface annotée pour le compteur géré par le protocole JVN.
 */
public interface ISharedCounter extends Serializable {

    @JvnRead
    int getCounter();

    @JvnWrite
    void increment();
}
