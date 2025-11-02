package jvn;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class CordMain {
    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(1099);
            JvnCoordImpl coord = new JvnCoordImpl();
            Naming.rebind("JvnCoord", coord);

            System.out.println("JVN Coordinator is running");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
