package pl.edu.agh.calculationp2p.message.body;

import pl.edu.agh.calculationp2p.message.process.MessageProcessContext;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GiveInit implements Body{

    private final List<Integer> privateNodes;
    private final Map<Integer, InetSocketAddress> publicNodes;
    private final int newId;

    public GiveInit(int newId, List<Integer> privateNodes, Map<Integer, InetSocketAddress> publicNodes) {
        this.newId = newId;
        this.privateNodes = privateNodes;
        this.publicNodes = publicNodes;
    }

    @Override
    public String serializeType() {
        return "\"give_init\"";
    }

    @Override
    public String serializeContent() {

        String result = "";
        result = result.concat("{\"your_new_id\":");
        result = result.concat(String.valueOf(this.newId));
        result = result.concat(",\"public_nodes\":[");

        List<Integer> keys = new ArrayList<>();
        this.publicNodes.forEach((id, ip)-> keys.add(id));

        for(int i=0;i<keys.size();i++){
            result = result.concat("{\"id\":");
            result = result.concat(String.valueOf(keys.get(i)));
            result = result.concat(",\"ip_address\":\"");
            result = result.concat(String.valueOf(this.publicNodes.get(keys.get(i)).getAddress()));
            result = result.concat("\"}");
            if(i<keys.size()-1){
                result = result.concat(",");
            }
        }
        result = result.concat("],\"private_nodes\":[");
        for(int i=0;i<this.privateNodes.size();i++){
            result = result.concat("{\"id\":"+this.privateNodes.get(i)+"}");
            if(i<this.privateNodes.size()-1){
                result = result.concat(",");
            }
        }
        result = result.concat("]}");
        return result;
    }

    @Override
    public void process(int sender, MessageProcessContext context) {
        this.privateNodes.forEach(node -> context.getRouter().createInterface(node));
        this.publicNodes.forEach((nodeId, ip) -> context.getRouter().createInterface(nodeId, ip));
        context.getRouter().setId(this.newId);
    }
}
