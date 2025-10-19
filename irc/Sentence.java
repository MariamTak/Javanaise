/***
 * Sentence class : used for keeping the text exchanged between users
 * during a chat application
 * Contact: 
 *
 * Authors: 
 */

package irc;

import jvn.annotation.JvnRead;
import jvn.annotation.JvnWrite;

public class Sentence implements java.io.Serializable, SentenceIntf {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String 	data;
  
	public Sentence() {
		data = new String("");
	}
	@JvnWrite
	public void write(String text) {
		data = text;
	}
	@JvnRead
	public String read() {
		return data;	
	}
	
}