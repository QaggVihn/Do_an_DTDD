/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package DoQuang;

/**
 *
 * @author quang
 */
public class Server {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        HostGUI host = new HostGUI();
        ClientGUI client1 = new ClientGUI();
        ClientGUI client2 = new ClientGUI();
        client2.setLocationRelativeTo(null);
    }
    
}
