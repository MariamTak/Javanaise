package jvn.test;


import jvn.JvnException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface RMI pour les nœuds Burst (processus parallèles).
 */
public interface IBurstClient extends Remote {

    void execute() throws JvnException, InterruptedException, RemoteException;

    long readValue() throws JvnException, RemoteException;
}
