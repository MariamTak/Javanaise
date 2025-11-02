package jvn.test;

import jvn.JvnException;
import jvn.JvnObject;
import jvn.JvnProxy;
import jvn.JvnServerImpl;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Représente un processus "Burst" individuel qui modifie le compteur partagé.
 * Chaque instance communique avec le serveur JVN.
 */
public class BurstClient extends UnicastRemoteObject implements IBurstClient {

    public static final long ITERATION_COUNT = 100_000_000L;

    public BurstClient(int id) throws Exception {
        Registry registry = LocateRegistry.getRegistry();
        registry.rebind("BurstNode" + id, this);
        System.out.println("BurstNode " + id + " enregistre sur le registre RMI");
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Utilisation : java burst.BurstNode <id>");
            System.exit(1);
        }
        new BurstClient(Integer.parseInt(args[0]));
    }

    @Override
    public void execute() throws JvnException, java.rmi.RemoteException {
        JvnServerImpl server = JvnServerImpl.jvnGetServer();

        // Recherche ou création du compteur partagé
        assert server != null;
        JvnObject jvnObject = server.jvnLookupObject("sharedCounter");
        if (jvnObject == null) {
            jvnObject = server.jvnCreateObject(new SharedCounter());
            server.jvnRegisterObject("sharedCounter", jvnObject);
        }

        // création du proxy JVN vers l’objet distant
        ISharedCounter counter = JvnProxy.newInstance(jvnObject, ISharedCounter.class);

        System.out.println("Valeur avant exécution : " + counter.getCounter());

        //incrémentation
        for (int i = 0; i < ITERATION_COUNT; i++) {
            counter.increment();
        }

        System.out.println("Valeur après exécution : " + counter.getCounter());

        server.jvnTerminate();
    }

    @Override
    public long readValue() throws JvnException, java.rmi.RemoteException {
        JvnServerImpl server = JvnServerImpl.jvnGetServer();
        assert server != null;
        JvnObject jvnObject = server.jvnLookupObject("sharedCounter");

        if (jvnObject == null) return 0;

        ISharedCounter counter = JvnProxy.newInstance(jvnObject, ISharedCounter.class);
        long value = counter.getCounter();

        server.jvnTerminate();
        return value;
    }
}
