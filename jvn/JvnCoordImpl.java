/***
 * JAVANAISE Implementation
 * JvnCoordImpl class
 * This class implements the Javanaise central coordinator
 * Contact:  
 *
 * Authors: 
 */
package jvn;

import java.rmi.server.UnicastRemoteObject;
import java.io.Serializable;
import java.util.*;

public class JvnCoordImpl extends UnicastRemoteObject implements JvnRemoteCoord{

    private static final long serialVersionUID = 1L;
    private int next_id = 0;
    private Map<String,Integer> name_id;
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

    public int jvnGetObjectId()
            throws java.rmi.RemoteException,jvn.JvnException {
        return next_id++;
    }

    public void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js)
            throws java.rmi.RemoteException,jvn.JvnException{
        int id = jo.jvnGetObjectId();
        name_id.put(jon,id);
        states.put(id,jo.jvnGetSharedObject());
        readers.putIfAbsent(id,new HashSet<>());
        writers.put(id,null);
        locks.putIfAbsent(js, new ArrayList<>());
    }

    public JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
            throws java.rmi.RemoteException,jvn.JvnException{
        Integer id = name_id.get(jon);
        if (id == null) return null;
        Serializable obj = states.get(id);
        JvnObject remoteObj = new JvnObjectImpl(id, obj);
        return remoteObj;
    }

    public Serializable jvnLockRead(int joi, JvnRemoteServer js)
            throws java.rmi.RemoteException, JvnException {
        locks.putIfAbsent(js, new ArrayList<>());
        if (!locks.get(js).contains(joi)) {
            locks.get(js).add(joi);
        }

        JvnRemoteServer writer = writers.get(joi);
        if (writer != null && !writer.equals(js)) {
            // Get the latest state from the writer
            Serializable state = writer.jvnInvalidateWriterForReader(joi);
            if (state != null) {
                states.put(joi, state);
            }
            writers.put(joi, null);
        }

        // Add the calling server to readers list
        readers.putIfAbsent(joi, new HashSet<>());
        readers.get(joi).add(js);

        return states.get(joi);
    }

    public Serializable jvnLockWrite(int joi, JvnRemoteServer js)
            throws java.rmi.RemoteException, JvnException{
        locks.putIfAbsent(js, new ArrayList<>());
        if (!locks.get(js).contains(joi)) {
            locks.get(js).add(joi);
        }

        // invalider les lecteurs
        Set<JvnRemoteServer> rds = readers.get(joi);
        if (rds != null) {
            for (JvnRemoteServer reader : new HashSet<>(rds)) {
                if (!reader.equals(js)) {
                    reader.jvnInvalidateReader(joi);
                }
            }
            rds.clear();
        }

        // Invalidate the current writer if different and get its state
        JvnRemoteServer currentWriter = writers.get(joi);
        if (currentWriter != null && !currentWriter.equals(js)) {
            Serializable state = currentWriter.jvnInvalidateWriter(joi);
            if (state != null) {
                states.put(joi, state);
            }
        }

        if (readers.get(joi) != null) {
            readers.get(joi).remove(js);
        }

        // Grant write lock
        writers.put(joi, js);

        return states.get(joi);
    }

    public void jvnTerminate(JvnRemoteServer js)
            throws java.rmi.RemoteException, JvnException {
        // Remove server from readers
        for (Set<JvnRemoteServer> rds : readers.values()){
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