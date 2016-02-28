package socket;
import java.io.Serializable;

public class Message implements Serializable{
    
    private static final long serialVersionUID = 1L;
    public String type, sender, content, recipient;
    public String[] group;
    
    public Message(String type, String sender, String content, String recipient){
        this.type = type; 
        this.sender = sender; 
        this.content = content; 
        this.recipient = recipient;
        this.group=null;
    }
    
    public Message(String type, String sender, String content, String[] recipients){
        this.type = type; 
        this.sender = sender; 
        this.content = content; 
        this.recipient = null;
        this.group=recipients;
    }
    
    @Override
    public String toString(){
        return "{type='"+type+"', sender='"+sender+"', content='"+content+"', recipient='"+recipient+"'}";
    }
}
