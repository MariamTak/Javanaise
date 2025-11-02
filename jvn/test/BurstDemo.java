package jvn.test;

import jvn.JvnException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Lance plusieurs instances de BurstNode en parallèle pour simuler un accès concurrent.
 * Chaque nœud manipule le même objet JVN (CounterImpl) à travers le mécanisme JVN.
 */
public class BurstDemo {

    private static long initialValue = 0;
    private static IBurstClient mainBurst;

    public static void main(String[] args)
            throws RemoteException, InterruptedException, JvnException, NotBoundException {

        // Connexion au registre RMI local
        Registry registry = LocateRegistry.getRegistry();

        // Pool de threads (2 Burst en parallèle)
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Récupération du premier Burst pour lecture initiale
        mainBurst = (IBurstClient) registry.lookup("BurstNode0");

        // Lecture de la valeur initiale du compteur partagé
        initialValue = mainBurst.readValue();
        System.out.println("Valeur initiale du compteur : " + initialValue);

        // Lancement des deux processus Burst en parallèle
        for (int i = 0; i < 2; i++) {
            final int id = i;
            executor.execute(() -> {
                try {
                    IBurstClient node = (IBurstClient) registry.lookup("BurstNode" + id);
                    node.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Erreur RMI : " + e.getMessage());
                }
            });
        }

        // Attente de la fin de l'exécution des threads
        executor.shutdown();
        while (!executor.awaitTermination(100, TimeUnit.MILLISECONDS)) ;

        // Lecture finale
        long finalValue = mainBurst.readValue();

        System.out.println("========================");
        System.out.println("Exécution terminée !");
        System.out.println("Valeur de départ : " + initialValue);
        System.out.println("Valeur finale :   " + finalValue);
        System.out.println("Valeur attendue : " + (initialValue + 2 * BurstClient.ITERATION_COUNT));
        System.out.println("========================");
    }
}
