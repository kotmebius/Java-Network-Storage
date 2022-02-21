package CloudMessage;

import lombok.Data;

@Data
public class FileRequest implements CloudMessage {

    private final String fileName;

    @Override
    public CommandType getType() {
        return CommandType.FILE_REQUEST;
    }
}
