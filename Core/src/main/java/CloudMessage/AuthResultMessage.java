package CloudMessage;

import lombok.Data;

@Data
public class AuthResultMessage implements CloudMessage{
    private final int result;

    public AuthResultMessage(int result) {
        this.result = result;
    }

    @Override
    public CommandType getType() {
        return CommandType.AUTH_FAIL;
    }

    public int getResult() {
        return result;
    }
}
