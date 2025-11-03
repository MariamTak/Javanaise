package jvn.test;

import java.io.Serializable;

/**
 * Classe simple représentant un compteur partagé.
 * Sert de base à l’objet JVN synchronisé entre plusieurs nœuds.
 */
public class SharedCounter implements ISharedCounter, Serializable {

    private static final long serialVersionUID = 42L;
    private int count;

    public SharedCounter() {
        this.count = 0;
    }

    @Override
    public int getCounter() {
        return count;
    }

    @Override
    public void increment() {
        count++;
    }
}

