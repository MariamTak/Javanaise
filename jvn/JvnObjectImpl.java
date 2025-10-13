package jvn;

import java.io.Serializable;

import jvn.JvnException;
import jvn.JvnObject;

enum STATE{
    R,RC,W,WC,RWC,NL
}

public class JvnObjectImpl implements JvnObject {
    private static final long serialVersionUID = 1L;

    private int id;
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
        this.state = STATE.NL;
        this.object = object;
    }

    @Override
    public synchronized  void jvnLockRead() throws JvnException {
        // Déjà en lecture
        if (state == STATE.W) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new JvnException(e.getMessage());
            }
        }

        // Peut réutiliser le cache en lecture
        if (state == STATE.RC ) {
            state = STATE.R;
            return;
        }

        // Avait un cache en écriture, peut lire localement
        if (state == STATE.WC) {
            state = STATE.RWC;
            return;
        }

        // Sinon, demander au coordinateur
        if (state == STATE.NL) {
            object = localServer.jvnLockRead(id);
            state = STATE.R;
        }
    }

    @Override
    public synchronized void jvnLockWrite() throws JvnException {
        // Déjà en écriture
        if (state == STATE.W) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new JvnException(e.getMessage());
            }
        }

        // Peut réutiliser le cache en écriture
        if (state == STATE.WC ) {
            state = STATE.W;
            return;
        }

        // Sinon, demander au coordinateur (états NL, R, RC)
        object = localServer.jvnLockWrite(id);
        state = STATE.W;
    }

    @Override
    public synchronized  void jvnUnLock() throws JvnException {
        if (state == STATE.W) {
            state = STATE.WC;
            notifyAll();
        } else if (state == STATE.R) {
            state = STATE.RC;
            notifyAll();
        }

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
        // Invalider le cache de lecture
        if(state == STATE.R) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (state == STATE.RC) {
            state = STATE.NL;
            object = null;
        }
    }

    @Override
    public synchronized Serializable jvnInvalidateWriter() throws JvnException {
        Serializable result = object;
        if (state == STATE.W) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (state == STATE.WC || state == STATE.RWC || state == STATE.W) {
            state = STATE.NL;
            object = null;
        }

        return result;
    }

    @Override
    public synchronized Serializable jvnInvalidateWriterForReader() throws JvnException {
        Serializable result = object;
        if (state == STATE.W) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (state == STATE.WC || state == STATE.W) {
            state = STATE.RC;
        } else if (state == STATE.RWC) {
            state = STATE.R;
        }

        return result;
    }

    public void setLocalServer(JvnLocalServer localServer) {
        this.localServer = localServer;
    }
}