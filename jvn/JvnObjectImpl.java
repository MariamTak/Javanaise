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
    public synchronized void jvnLockRead() throws JvnException {
        // Attendre si quelqu'un écrit
        while (state == STATE.W) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new JvnException(e.getMessage());
            }
        }

        // Si cache de lecture valide, le réutiliser
        if (state == STATE.RC) {
            state = STATE.R;
            return;
        }

        // Si cache d'écriture valide, peut lire localement
        if (state == STATE.WC || state == STATE.RWC) {
            state = STATE.RWC;
            return;
        }

        // Sinon (NL ou R), demander au coordinateur
        if (state == STATE.NL || state == STATE.R) {
            object = localServer.jvnLockRead(id);
            state = STATE.R;
        }
    }

    @Override
    public synchronized void jvnLockWrite() throws JvnException {
        // Attendre si quelqu'un écrit
        while (state == STATE.W) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new JvnException(e.getMessage());
            }
        }

        // Si cache d'écriture valide, le réutiliser
        if (state == STATE.WC || state == STATE.RWC) {
            state = STATE.W;
            return;
        }

        // Sinon, demander au coordinateur
        object = localServer.jvnLockWrite(id);
        state = STATE.W;
    }

    @Override
    public synchronized void jvnUnLock() throws JvnException {
        if (state == STATE.W) {
            state = STATE.WC;
        } else if (state == STATE.R) {
            state = STATE.RC;
        } else if (state == STATE.RWC) {
            // Reste en RWC après un unlock de lecture
            // (on garde le cache d'écriture)
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
        // Attendre si lecture en cours
        while (state == STATE.R) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // Invalider le cache de lecture
        if (state == STATE.RC) {
            state = STATE.NL;
        } else if (state == STATE.RWC) {
            state = STATE.WC;
        }

        notifyAll();
    }

    @Override
    public synchronized Serializable jvnInvalidateWriter() throws JvnException {
        Serializable result = object;

        // Attendre si écriture en cours
        while (state == STATE.W) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // Invalider complètement
        if (state == STATE.WC || state == STATE.RWC) {
            state = STATE.NL;
        }

        notifyAll();
        return result;
    }

    @Override
    public synchronized Serializable jvnInvalidateWriterForReader() throws JvnException {
        Serializable result = object;

        // Attendre si écriture en cours
        while (state == STATE.W) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if (state == STATE.WC) {
            state = STATE.RC;
        } else if (state == STATE.RWC) {
            state = STATE.R;
        }

        notifyAll();
        return result;
    }

    public void setLocalServer(JvnLocalServer localServer) {
        this.localServer = localServer;
    }
}