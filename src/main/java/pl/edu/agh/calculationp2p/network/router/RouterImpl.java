package pl.edu.agh.calculationp2p.network.router;

import pl.edu.agh.calculationp2p.message.Message;
import pl.edu.agh.calculationp2p.network.connection.ConnectionManager;
import pl.edu.agh.calculationp2p.network.connection.StaticConnection;
import pl.edu.agh.calculationp2p.network.messagequeue.MessageQueueExit;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class RouterImpl implements Router {
    final ConnectionManager connectionManager;
    private final MessageQueueExit messageQueue;
    final RoutingTable routingTable;
    final Map<Integer, StaticConnection> staticInterfaces = new HashMap<>();

    public RouterImpl(ConnectionManager connectionManager, MessageQueueExit messageQueue, RoutingTable routingTable)
    {
        this.routingTable = routingTable;
        this.connectionManager = connectionManager;
        this.messageQueue = messageQueue;
    }

    @Override
    public void createInterface(int nodeId, InetSocketAddress ipAddress)
    {
        StaticConnection newConnection = new StaticConnection(ipAddress);
        staticInterfaces.put(nodeId, newConnection);
        connectionManager.addStaticConnection(newConnection);
        routingTable.addInterface(nodeId);
        routingTable.bind(nodeId, newConnection);
    }

    @Override
    public void deleteInterface(int nodeId) throws InterfaceExistsException
    {
        if(routingTable.interfaceListContains(nodeId))
        {
            if(staticInterfaces.containsKey(nodeId))
            {
                StaticConnection StaticConnection = staticInterfaces.get(nodeId);
                connectionManager.removeStaticConnection(StaticConnection);
                staticInterfaces.remove(nodeId);
            }
            routingTable.removeInterface(nodeId);
        }
    }

    //TODO
    @Override
    public List<Message> getMessage()
    {
        return null;
    }

}
