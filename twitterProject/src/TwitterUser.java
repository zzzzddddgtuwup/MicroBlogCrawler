
public class TwitterUser {
	public String name;
	public String userId;
	
	public TwitterUser(String name,String userId){
		this.name = name;
		this.userId = userId;
	}
	
	public String toString(){
		return name + " " + userId; 
	}
}
