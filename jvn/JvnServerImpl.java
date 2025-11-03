/***
 * JAVANAISE Implementation
 * JvnServerImpl class
 * Implementation of a Jvn server
 * Contact: 
 *
 * Authors: 
 */

package jvn;

import java.rmi.Naming;
import java.rmi.server.UnicastRemoteObject;
import java.io.*;
import java.util.HashMap;
import java.util.Map;


public class JvnServerImpl
              extends UnicastRemoteObject 
							implements JvnLocalServer, JvnRemoteServer{ 
	
  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// A JVN server is managed as a singleton  
	private static JvnServerImpl js = null;
	private JvnRemoteCoord jvnRemoteCoord;
	private Map<Integer,JvnObject> obj;


  /**
  * Default constructor
  * @throws JvnException
  **/
	private JvnServerImpl() throws Exception {
		super();
		obj = new HashMap<>();
try {
jvnRemoteCoord = (JvnRemoteCoord)  Naming.lookup("rmi://localhost/JvnCoord"); // cherche jvncoord dans rmi registry
} catch (Exception e) {
		throw new RuntimeException(e);
	}
	}
	
  /**
    * Static method allowing an application to get a reference to 
    * a JVN server instance
    * @throws JvnException
    **/
	public static JvnServerImpl jvnGetServer() {
		if (js == null){
			try {
				js = new JvnServerImpl(); // instance unique
			} catch (Exception e) {
				return null;
			}
		}
		return js;
	}
	
	/**
	* The JVN service is not used anymore
	* @throws JvnException
	**/
	public  void jvnTerminate()
	throws jvn.JvnException {
try{
jvnRemoteCoord.jvnTerminate(this);
}catch (Exception e) {
	throw new RuntimeException(e);
}
	}

	/**
	* creation of a JVN object
	* @param o : the JVN object state
	* @throws JvnException
	**/
	public  JvnObject jvnCreateObject(Serializable o)
	throws jvn.JvnException {
		try {
			int id = jvnRemoteCoord.jvnGetObjectId();
			JvnObjectImpl jo = new JvnObjectImpl(id, o, this);
			obj.put(id, jo);
			return jo;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	*  Associate a symbolic name with a JVN object
	* @param jon : the JVN object name
	* @param jo : the JVN object 
	* @throws JvnException
	**/
	public  void jvnRegisterObject(String jon, JvnObject jo)
	throws jvn.JvnException {
	try{
		jvnRemoteCoord.jvnRegisterObject(jon, jo, this);
	} catch (Exception e) {
		throw new RuntimeException(e);
	}
	}
	
	/**
	* Provide the reference of a JVN object beeing given its symbolic name
	* @param jon : the JVN object name
	* @return the JVN object 
	* @throws JvnException
	**/

	public JvnObject jvnLookupObject(String jon) throws JvnException {
		try {
			JvnObject objRef = jvnRemoteCoord.jvnLookupObject(jon, this);
			if (objRef != null) {
				if (objRef instanceof JvnObjectImpl) {
					((JvnObjectImpl) objRef).setLocalServer(this);
				}
				obj.put(objRef.jvnGetObjectId(), objRef);
			}
			return objRef;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	
	/**
	* Get a Read lock on a JVN object 
	* @param joi : the JVN object identification
	* @return the current JVN object state
	* @throws  JvnException
	**/
	public Serializable jvnLockRead(int joi) throws JvnException {
		try {
			Serializable state = jvnRemoteCoord.jvnLockRead(joi, this);
			if (obj.get(joi) instanceof JvnObjectImpl) {
				obj.get(joi).setObject(state);
			}
			return state;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	/**
	* Get a Write lock on a JVN object 
	* @param joi : the JVN object identification
	* @return the current JVN object state
	* @throws  JvnException
	**/

	public Serializable jvnLockWrite(int joi) throws JvnException {
		try {
			Serializable state = jvnRemoteCoord.jvnLockWrite(joi, this);
			// Update local object with fresh state
			if (obj.get(joi) instanceof JvnObjectImpl) {
				obj.get(joi).setObject(state);
			}
			return state;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
  /**
   * Invalidate the Read lock of the JVN object identified by id
   * called by the JvnCoord
   *
   * @param joi : the JVN object id
   * @return void
   * @throws java.rmi.RemoteException,JvnException
   **/

	public Serializable jvnInvalidateReader(int joi) throws JvnException {
		JvnObject localObj = obj.get(joi);
		if (localObj != null) {
			localObj.jvnInvalidateReader();
			return localObj.jvnGetSharedObject();
		}
		return null;
	}
	/**
	* Invalidate the Write lock of the JVN object identified by id 
	* @param joi : the JVN object id
	* @return the current JVN object state
	* @throws java.rmi.RemoteException,JvnException
	**/
  public Serializable jvnInvalidateWriter(int joi)
	throws java.rmi.RemoteException,jvn.JvnException {
	  return obj.get(joi).jvnInvalidateWriter();
	};
	
	/**
	* Reduce the Write lock of the JVN object identified by id 
	* @param joi : the JVN object id
	* @return the current JVN object state
	* @throws java.rmi.RemoteException,JvnException
	**/
   public Serializable jvnInvalidateWriterForReader(int joi)
	 throws java.rmi.RemoteException,jvn.JvnException {
	   return obj.get(joi).jvnInvalidateWriterForReader();
	 };


}

 
