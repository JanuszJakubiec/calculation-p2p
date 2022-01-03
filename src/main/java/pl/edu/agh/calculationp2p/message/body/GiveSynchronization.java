package pl.edu.agh.calculationp2p.message.body;

import pl.edu.agh.calculationp2p.message.process.MessageProcessContext;
import pl.edu.agh.calculationp2p.message.utils.TaskStateMess;

import java.util.List;
import java.util.Objects;

public class GiveSynchronization implements Body{

    private final List<TaskStateMess> currStateList;

    public List<TaskStateMess> getCurrStateList() {
        return currStateList;
    }

    public GiveSynchronization(List<TaskStateMess> listOfTasks) {

        this.currStateList = listOfTasks;

    }

    @Override
    public String serializeType() {
        return "\"give_synchronization\"";
    }

    @Override
    public String serializeContent() {

        String result = "";
        result = result.concat("{\"tasks\":[");

        for(int i=0;i<this.currStateList.size();i++){
            result = result.concat(this.currStateList.get(i).serialize());
            if(i<this.currStateList.size()-1){
                result = result.concat(",");
            }
        }

        result = result.concat("]}");
        return result;

    }

    @Override
    public void process(int sender, MessageProcessContext context) {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (getClass() != o.getClass()) {
            return false;
        }
        GiveSynchronization message = (GiveSynchronization) o;
        return message.getCurrStateList() == this.currStateList;
    }
}
