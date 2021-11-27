package pl.edu.agh.calculationp2p.network.connection;
import pl.edu.agh.calculationp2p.network.message.Message;
import pl.edu.agh.calculationp2p.network.messagequeue.MessageQueue;
import pl.edu.agh.calculationp2p.network.messagequeue.MessageQueueEntry;
import pl.edu.agh.calculationp2p.network.messagequeue.MessageQueueExit;


import org.junit.jupiter.api.Test;

import javax.swing.plaf.TableHeaderUI;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionManagerTest {

    @Test
    void addStaticConnection() {


    }

    @Test
    void removeStaticConnection() {
    }

    @Test
    void run2() {

        /*** test wymaga odpalonej drugiego programu na lokalnej maszynie */
        MessageQueue messageQueue = new MessageQueue();
        MessageQueueEntry messageQueueEntry = messageQueue;
        MessageQueueExit messageQueueExit = messageQueue;

        ConnectionManager connectionManager = new ConnectionManager(messageQueueEntry, false);

        InetSocketAddress inetSocketAddress = new InetSocketAddress("localhost",49153);
        StaticConnection staticConnection = new StaticConnection(inetSocketAddress);

        connectionManager.addStaticConnection(staticConnection);

        staticConnection.send(new Message("chalo world"));
        connectionManager.start();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //messageQueueExit.get();

    }

    @Test
    void run1(){
        MessageQueue messageQueue = new MessageQueue();
        MessageQueueEntry messageQueueEntry = messageQueue;
        MessageQueueExit messageQueueExit = messageQueue;

        ConnectionManager connectionManager = new ConnectionManager(messageQueueEntry, false);

        //connectionManager.start();



        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}