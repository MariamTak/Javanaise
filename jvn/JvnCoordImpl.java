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

    public synchronized void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js)
            throws java.rmi.RemoteException, JvnException {
        int id = jo.jvnGetObjectId();
        name_id.put(jon, id);
        states.put(id, jo.jvnGetSharedObject()); //sauvegarder l etat
        readers.putIfAbsent(id, new HashSet<>());
        writers.put(id, null);
        locks.putIfAbsent(js, new ArrayList<>());
    }

    public JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
            throws java.rmi.RemoteException, JvnException {
        Integer id = name_id.get(jon);
        if (id == null) return null;
        Serializable obj = states.get(id);  //recupere l etat
        return new JvnObjectImpl(id, obj, null);
    }

    public synchronized Serializable jvnLockRead(int joi, JvnRemoteServer js)
            throws java.rmi.RemoteException, JvnException {

        locks.putIfAbsent(js, new ArrayList<>());
        if (!locks.get(js).contains(joi)) {
            locks.get(js).add(joi); //serveur utilise cet obj
        }
//gestion ecrivain
        JvnRemoteServer writer = writers.get(joi);
        if (writer != null && !writer.equals(js)) {
            Serializable updatedObject = writer.jvnInvalidateWriterForReader(joi); // si writer existe, invalider verrou pour lecture
            if (updatedObject != null) {
                states.put(joi, updatedObject);
            }
            writers.put(joi, null);
        }

        // ajouter serveur à la liste des lecteurs
        readers.putIfAbsent(joi, new HashSet<>());
        readers.get(joi).add(js);

        return states.get(joi);
    }

    public synchronized Serializable jvnLockWrite(int joi, JvnRemoteServer js)
            throws java.rmi.RemoteException, JvnException {

        locks.putIfAbsent(js, new ArrayList<>());
        if (!locks.get(js).contains(joi)) {
            locks.get(js).add(joi); //enregistre verrou
        }
//gestion ecrivain
        JvnRemoteServer currentWriter = writers.get(joi);
        if (currentWriter != null && !currentWriter.equals(js)) {
            Serializable updatedObject = currentWriter.jvnInvalidateWriter(joi);
            if (updatedObject != null) {
                states.put(joi, updatedObject);
            }
        }
        //gestion lecteur
        Set<JvnRemoteServer> rds = readers.get(joi);
        if (rds != null) {
            for (JvnRemoteServer reader : new HashSet<>(rds)) {
                if (!reader.equals(js)) { //sauf demandeur
                    try {
                        Serializable updatedObject = reader.jvnInvalidateReader(joi);
                        if (updatedObject != null) {
                            states.put(joi, updatedObject);
                        }
                    } catch (Exception e) {
                    }
                }
            }
            rds.clear();
        }

        if (readers.get(joi) != null) {
            readers.get(joi).remove(js);
        }
        writers.put(joi, js);

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

                }
                writers.put(entry.getKey(), null);
            }
        }

        locks.remove(js);
    }

}