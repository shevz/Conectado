import java.io.Serializable;
import java.util.Arrays;

public class Message implements Serializable{
    
    private static final long serialVersionUID = 1L;
    public String type, sender, content, recipient;
    public String[] group;
    
    public Message(String type, String sender, String content, String recipient){
        this.type = type; 
        this.sender = sender; 
        this.content = content; 
        this.recipient = recipient;
    }
    
    public Message(String type, String sender, String content, String[] recipients){
        this.type = type; 
        this.sender = sender; 
        this.content = content; 
        this.group = recipients;
    }
    
    @Override
	public String toString() {
		return "Message [type=" + type + ", sender=" + sender + ", content=" + content + ", recipient=" + recipient
				+ ", group=" + Arrays.toString(group) + "]";
	}
}
