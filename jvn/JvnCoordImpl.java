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
    private  Map<Integer, Set<JvnRemoteServer>> readers;
    private Map<Integer, JvnRemoteServer> writers;
    private Map<Integer, Serializable> states;
    private Map<JvnRemoteServer, ArrayList<Integer>> locks;


    /**
  * Default constructor
  * @throws JvnException
  **/
    public JvnCoordImpl() throws Exception {
        super();
        name_id = new HashMap<>();
        readers = new HashMap<>();
        writers = new HashMap<>();
        states = new HashMap<>();
        locks = new HashMap<>();

    }


  /**
  *  Allocate a NEW JVN object id (usually allocated to a
  *  newly created JVN object)
  * @throws java.rmi.RemoteException,JvnException
  **/
  public int jvnGetObjectId()
  throws java.rmi.RemoteException,jvn.JvnException {
    return next_id++;
  }

  /**
  * Associate a symbolic name with a JVN object
  * @param jon : the JVN object name
  * @param jo  : the JVN object
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the JVNServer
  * @throws java.rmi.RemoteException,JvnException
  **/
  public void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js)
  throws java.rmi.RemoteException,jvn.JvnException{
      int id = jo.jvnGetObjectId();
      //enregistrer nom
      name_id.put(jon,id);
      //enregister etat de l'objet
      states.put(id,jo.jvnGetSharedObject());
      readers.putIfAbsent(id,new HashSet<>());
      writers.put(id,null);
      locks.putIfAbsent(js, new ArrayList<>());
  }

  /**
  * Get the reference of a JVN object managed by a given JVN server
  * @param jon : the JVN object name
  * @param js : the remote reference of the JVNServer
  * @throws java.rmi.RemoteException,JvnException
  **/
  public JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
  throws java.rmi.RemoteException,jvn.JvnException{
      Integer id = name_id.get(jon);
      if (id == null) return null;
      //recupere etat
      Serializable obj = states.get(id);
      JvnObject remoteObj = new JvnObjectImpl(id, obj);
      return remoteObj;
  }

  /**
  * Get a Read lock on a JVN object managed by a given JVN server
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the server
  * @return the current JVN object state
  * @throws java.rmi.RemoteException, JvnException
  **/
   public Serializable jvnLockRead(int joi, JvnRemoteServer js)
   throws java.rmi.RemoteException, JvnException {
       locks.putIfAbsent(js, new ArrayList<>());
       if (!locks.get(js).contains(joi)) {
           locks.get(js).add(joi);
       }
       JvnRemoteServer writer = writers.get(joi);
       if (writer != null && writer != js) {
           Serializable state= writer.jvnInvalidateWriterForReader(joi);
           states.put(joi,state);
           writers.put(joi,null);
       }
           // ajouter le serveur appelant à la liste des lecteurs
           readers.putIfAbsent(joi, new HashSet<>());
           readers.get(joi).add(js);

       Serializable state = states.get(joi);
       return state;
   }
  /**
  * Get a Write lock on a JVN object managed by a given JVN server
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the server
  * @return the current JVN object state
  * @throws java.rmi.RemoteException, JvnException
  **/
   public Serializable jvnLockWrite(int joi, JvnRemoteServer js)
   throws java.rmi.RemoteException, JvnException{
    // to be completed
       locks.putIfAbsent(js, new ArrayList<>());
       if (!locks.get(js).contains(joi)) {
           locks.get(js).add(joi);
       }
       // invalider les lecteurs
       Set<JvnRemoteServer> rds = readers.get(joi);
       if (rds != null) {
           for (JvnRemoteServer reader : new HashSet<>(rds)) {
               if (!reader.equals(js)) {
                  Serializable state = reader.jvnInvalidateReader(joi);
                  states.put(joi,state);
               }
           }
           rds.clear();
       }

       // invalider le writer actuel si différent
       JvnRemoteServer currentWriter = writers.get(joi);
       if (currentWriter != null && !currentWriter.equals(js)) {
           Serializable state = currentWriter.jvnInvalidateWriter(joi);
           states.put(joi, state);
       }

       // accorder le verrou d’écriture
       writers.put(joi, js);

       return states.get(joi);
   }
	/**
	* A JVN server terminates
	* @param js  : the remote reference of the server
	* @throws java.rmi.RemoteException, JvnException
	**/
    public void jvnTerminate(JvnRemoteServer js)
	 throws java.rmi.RemoteException, JvnException {
        //enlever server des readers
	 for (Set<JvnRemoteServer> rds : readers.values()){
	     rds.remove(js);
     }
     //enlever server de writers
        for (Map.Entry<Integer, JvnRemoteServer> entry : new HashMap<>(writers).entrySet()) {
            if (entry.getValue() != null && entry.getValue().equals(js)) {
                writers.put(entry.getKey(), null);
            }
        }

        // remove all locks it holds
        locks.remove(js);    }


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


