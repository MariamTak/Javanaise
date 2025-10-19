package jvn;

import java.rmi.server.UnicastRemoteObject;
import java.io.Serializable;
import java.util.*;

public class JvnCoordImpl extends UnicastRemoteObject implements JvnRemoteCoord {

    private static final long serialVersionUID = 1L;
    private int next_id = 0;
    private Map<String, Integer> name_id;
    private Map<Integer, Set<JvnRemoteServer>> readers;
    private Map<Integer, JvnRemoteServer> writers;
    private Map<Integer, Serializable> states;
    private Map<JvnRemoteServer, ArrayList<Integer>> locks;

    public JvnCoordImpl() throws Exception {
        super();
        name_id = new HashMap<>();
        readers = new HashMap<>();
        writers = new HashMap<>();
        states = new HashMap<>();
        locks = new HashMap<>();
    }

    public int jvnGetObjectId() throws java.rmi.RemoteException, JvnException {
        return next_id++;
    }

    public void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js)
            throws java.rmi.RemoteException, JvnException {
        int id = jo.jvnGetObjectId();
        name_id.put(jon, id);
        states.put(id, jo.jvnGetSharedObject());
        readers.putIfAbsent(id, new HashSet<>());
        writers.put(id, null);
        locks.putIfAbsent(js, new ArrayList<>());
    }

    public JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
            throws java.rmi.RemoteException, JvnException {
        Integer id = name_id.get(jon);
        if (id == null) return null;
        Serializable obj = states.get(id);
        return new JvnObjectImpl(id, obj, null);
    }

    public synchronized Serializable jvnLockRead(int joi, JvnRemoteServer js)
            throws java.rmi.RemoteException, JvnException {

        // Initialiser la liste des locks pour ce client
        locks.putIfAbsent(js, new ArrayList<>());
        if (!locks.get(js).contains(joi)) {
            locks.get(js).add(joi);
        }

        // Invalider le writer actuel si nécessaire
        JvnRemoteServer writer = writers.get(joi);
        if (writer != null && !writer.equals(js)) {
            Serializable updatedObject = writer.jvnInvalidateWriterForReader(joi);
            if (updatedObject != null) {
                states.put(joi, updatedObject);
            }
            writers.put(joi, null);
        }

        // Ajouter le client à la liste des lecteurs
        readers.putIfAbsent(joi, new HashSet<>());
        readers.get(joi).add(js);

        return states.get(joi);
    }

    public synchronized Serializable jvnLockWrite(int joi, JvnRemoteServer js)
            throws java.rmi.RemoteException, JvnException {

        // Initialiser la liste des locks pour ce client
        locks.putIfAbsent(js, new ArrayList<>());
        if (!locks.get(js).contains(joi)) {
            locks.get(js).add(joi);
        }

        // 1️⃣ Invalider tous les lecteurs
        Set<JvnRemoteServer> rds = readers.get(joi);
        if (rds != null) {
            for (JvnRemoteServer reader : new HashSet<>(rds)) {
                if (!reader.equals(js)) {
                    Serializable updatedObject = reader.jvnInvalidateReader(joi);
                    if (updatedObject != null) {
                        states.put(joi, updatedObject);
                    }
                }
            }
            rds.clear();
        }

        // 2️⃣ Invalider le writer actuel si différent
        JvnRemoteServer currentWriter = writers.get(joi);
        if (currentWriter != null && !currentWriter.equals(js)) {
            Serializable updatedObject = currentWriter.jvnInvalidateWriter(joi);
            if (updatedObject != null) {
                states.put(joi, updatedObject);
            }
            writers.put(joi, null);
        }

        // 3️⃣ Supprimer le client actuel de la liste des lecteurs (s’il était là)
        if (readers.get(joi) != null) {
            readers.get(joi).remove(js);
        }

        // 4️⃣ Accorder le verrou d’écriture au client actuel
        writers.put(joi, js);

        // 5️⃣ Retourner la version la plus récente de l’objet
        return states.get(joi);
    }

    public synchronized void jvnTerminate(JvnRemoteServer js)
            throws java.rmi.RemoteException, JvnException {
        // Remove server from readers
        for (Set<JvnRemoteServer> rds : readers.values()) {
            rds.remove(js);
        }

        // Remove server from writers and get final state
        for (Map.Entry<Integer, JvnRemoteServer> entry : new HashMap<>(writers).entrySet()) {
            if (entry.getValue() != null && entry.getValue().equals(js)) {
                try {
                    Serializable finalState = js.jvnInvalidateWriter(entry.getKey());
                    if (finalState != null) {
                        states.put(entry.getKey(), finalState);
                    }
                } catch (Exception e) {
                    // Server might already be down
                }
                writers.put(entry.getKey(), null);
            }
        }

        locks.remove(js);
    }

    public static void main(String[] args) {
        try {
            java.rmi.registry.LocateRegistry.createRegistry(1099);
            JvnCoordImpl coord = new JvnCoordImpl();
            java.rmi.Naming.rebind("JvnCoord", coord);
            System.out.println("JVN Coordinator is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}