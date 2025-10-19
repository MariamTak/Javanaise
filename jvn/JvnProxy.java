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

    /**
     * Crée un proxy dynamique pour un objet JVN
     * @param jvnObject L'objet JVN qui contient l'état partagé
     * @param objectInterface L'interface que l'objet doit implémenter
     * @return Le proxy qui intercepte les appels
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(JvnObject jvnObject, Class<T> objectInterface)
            throws JvnException {

        Serializable sharedObject = jvnObject.jvnGetSharedObject();

        JvnProxy handler = new JvnProxy(jvnObject, sharedObject);

        return (T) Proxy.newProxyInstance(
                objectInterface.getClassLoader(),
                new Class<?>[] { objectInterface },
                handler
        );
    }

    /**
     * Méthode appelée à chaque invocation de méthode sur le proxy
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Vérifier si la méthode a l'annotation @JvnRead
        if (method.isAnnotationPresent(JvnRead.class)) {
            return invokeWithReadLock(method, args);
        }

        // Vérifier si la méthode a l'annotation @JvnWrite
        if (method.isAnnotationPresent(JvnWrite.class)) {
            return invokeWithWriteLock(method, args);
        }

        // Si pas d'annotation, appel direct (pas de verrou)
        return method.invoke(realObject, args);
    }

    /**
     * Invoque une méthode avec un verrou en lecture
     */
    private Object invokeWithReadLock(Method method, Object[] args) throws Throwable {
        try {
            // 1. Acquérir le verrou en lecture
            jvnObject.jvnLockRead();

            // 2. Récupérer l'objet à jour
            realObject = jvnObject.jvnGetSharedObject();

            // 3. Invoquer la méthode
            return method.invoke(realObject, args);

        } finally {
            // 4. Libérer le verrou
            jvnObject.jvnUnLock();
        }
    }

    /**
     * Invoque une méthode avec un verrou en écriture
     */
    private Object invokeWithWriteLock(Method method, Object[] args) throws Throwable {
        try {
            // 1. Acquérir le verrou en écriture
            jvnObject.jvnLockWrite();

            // 2. Récupérer l'objet à jour
            realObject = jvnObject.jvnGetSharedObject();

            // 3. Invoquer la méthode
            Object result = method.invoke(realObject, args);

            // 4. Sauvegarder les modifications (important!)
            if (jvnObject instanceof JvnObjectImpl) {
                ((JvnObjectImpl) jvnObject).setObject((Serializable) realObject);
            }

            return result;

        } finally {
            // 5. Libérer le verrou
            jvnObject.jvnUnLock();
        }
    }
}