package jvn;

import java.io.Serializable;

import jvn.JvnException;
import jvn.JvnObject;

enum STATE{
    R,RC,W,WC,RWC,NL
}
public class JvnObjectImpl implements JvnObject {
    private int id;                // identifiant unique
    private Serializable object;
    private transient JvnLocalServer localServer;
    private STATE state;

    public JvnObjectImpl(int id, Serializable object, JvnLocalServer localServer) {
        this.id = id;
        this.state = STATE.NL;
        this.object = object;
        this.localServer = localServer;
    }
    public JvnObjectImpl(int id, Serializable object) {
        this.id = id;
        this.object = object;
        this.state = STATE.NL;
    }


    @Override
    public synchronized void jvnLockRead() throws JvnException {
        if (this.state == STATE.W) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new JvnException(e.getMessage());
            }
        }
        if (state == STATE.R || state == STATE.RC || state == STATE.RWC) {
            state = STATE.R;
            return;
        }
        if (state == STATE.WC) {
            state = STATE.RWC;
            return;
        }
        if (state == STATE.NL) {
            object = localServer.jvnLockRead(id);
            state = STATE.R;
        }
    }

    @Override
    public synchronized void jvnLockWrite() throws JvnException {
        // attendre si un verrou d'écriture est actif
        if (state == STATE.W) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new JvnException(e.getMessage());
            }}
        if (state != STATE.WC && state != STATE.RWC) {
            object = localServer.jvnLockWrite(id);
        }

        state = STATE.W;
    }

    @Override
    public synchronized void jvnUnLock() throws JvnException {
        if (state == STATE.W) {
            state = STATE.WC;
        } else if (state == STATE.R) {
            state = STATE.RC;
        }
        notifyAll();
    }

    @Override
    public int jvnGetObjectId() throws JvnException {
        return id;
    }

    @Override
    public Serializable jvnGetSharedObject() throws JvnException {
        return object;
    }
    public void setObject(Serializable object) {
        this.object = object;
    }

    @Override
    public synchronized void jvnInvalidateReader() throws JvnException {
        // Attendre si un verrou d'écriture est actif
        if (state == STATE.W) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new JvnException(e.getMessage());
            }
        }
        if (state == STATE.R || state == STATE.RC) {
            state = STATE.NL;
        }
    }

    @Override
    public synchronized Serializable jvnInvalidateWriter() throws JvnException {
        if (state == STATE.W) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new JvnException(e.getMessage());
            }
        }
        if (state == STATE.WC) {
            state = STATE.NL;
        }    return object;}

    @Override
    public synchronized Serializable jvnInvalidateWriterForReader() throws JvnException {
        // Attendre si un verrou d'écriture est actif
        while (state == STATE.W) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new JvnException(e.getMessage());
            }
        }

        if (state == STATE.WC) {
            state = STATE.RC;
        } else if (state == STATE.RWC) {
            state = STATE.R;
        }

        return object;
    }    }

