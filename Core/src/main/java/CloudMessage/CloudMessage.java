package CloudMessage;

import java.io.Serializable;

public interface CloudMessage extends Serializable {
    CommandType getType();
}