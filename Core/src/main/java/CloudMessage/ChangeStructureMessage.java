package CloudMessage;
import lombok.Data;

//private final int DELETE = 0;
//private final int RENAME = 1;
//private final int MAKE_DIR = 2;

@Data
public class ChangeStructureMessage implements CloudMessage {
    String mainName;
    String oldName="";
    int operation;

    public ChangeStructureMessage(String mainName, String oldName, int operation) {
        this.mainName = mainName;
        this.oldName = oldName;
        this.operation = operation;
    }

    public ChangeStructureMessage(String mainName, int operation) {
        this.mainName = mainName;
        this.operation = operation;
    }

    @Override
    public CommandType getType() {
        return CommandType.CHANGE_STRUCTURE;
    }
}
