package CloudMessage;

import lombok.Data;

@Data
public class AuthMessage implements CloudMessage{
    private String login;
    private int hash;

    public AuthMessage(String login, int hash) {
        this.login = login;
        this.hash = hash;
    }

    @Override
    public CommandType getType() {
        return CommandType.AUTH;
    }
}
