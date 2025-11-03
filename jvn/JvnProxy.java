package jvn;

import jvn.annotation.JvnRead;
import jvn.annotation.JvnWrite;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.io.Serializable;


public class JvnProxy implements InvocationHandler {

    private JvnObject jvnObject;
    private Object realObject;

    private JvnProxy(JvnObject jvnObject, Object realObject) {
        this.jvnObject = jvnObject;
        this.realObject = realObject;
    }

    @SuppressWarnings("unchecked")
    public static <T> T newInstance(JvnObject jvnObject, Class<T> objectInterface)
            throws JvnException {

        Serializable sharedObject = jvnObject.jvnGetSharedObject(); //recupere obj

        JvnProxy handler = new JvnProxy(jvnObject, sharedObject); //creer intercepteur
//creer proxy dynamique
        return (T) Proxy.newProxyInstance(
                objectInterface.getClassLoader(),
                new Class<?>[] { objectInterface },
                handler
        );
    }



    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // vvérifier si la méthode a l'annotation @JvnRead
        if (method.isAnnotationPresent(JvnRead.class)) {
            return invokeWithReadLock(method, args);
        }

        // vérifier si la méthode a l'annotation @JvnWrite
        if (method.isAnnotationPresent(JvnWrite.class)) {
            return invokeWithWriteLock(method, args);
        }

        //  appel direct (pas de verrou)
        return method.invoke(realObject, args);
    }



    private Object invokeWithReadLock(Method method, Object[] args) throws Throwable {
        try {
            // acquérir le verrou en lecture
            jvnObject.jvnLockRead();

            // récupérer l'objet à jour
            realObject = jvnObject.jvnGetSharedObject();

            // invoquer la méthode
            return method.invoke(realObject, args);

        } finally {
            //libérer verrou
            jvnObject.jvnUnLock();
        }
    }



    private Object invokeWithWriteLock(Method method, Object[] args) throws Throwable {
        try {
            // acquérir le verrou en écriture
            jvnObject.jvnLockWrite();

            // récupérer l'objet à jour
            realObject = jvnObject.jvnGetSharedObject();

            // invoquer la méthode
            Object result = method.invoke(realObject, args);

            // sauvegarder les modifications
            if (jvnObject instanceof JvnObjectImpl) {
                jvnObject.setObject((Serializable) realObject);
            }

            return result;

        } finally {
            // libérer verrou
            jvnObject.jvnUnLock();
        }
    }
}