import com.temboo.Library.Twitter.FriendsAndFollowers.GetFollowersByID;
import com.temboo.Library.Twitter.FriendsAndFollowers.GetFollowersByID.GetFollowersByIDInputSet;
import com.temboo.Library.Twitter.FriendsAndFollowers.GetFollowersByID.GetFollowersByIDResultSet;
import com.temboo.core.TembooException;
import com.temboo.core.TembooSession;


public class tembooTest {
	public static final String CONSUMNER_KEY_STRING =  "uQSDnl6Ke8LhRCAOHc7VwTzRG";
	public static final String CONSUMNER_SECRET_STRING = 
			"5eTq7BGpVqdr4koAtvu40dj6E0KaLgCT9tnj59ZePcD1PXTpju";
	public static final String ACCESS_TOKEN_STRING = 
			"2937089573-ltw692SyFRpGFDI3ZaxSl8tkLbci6Paefa1UywB";
	public static final String ACCESS_TOKEN_SECRET_STRING = 
			"g1KXvekf0F2pQctzlCk5E7Pv4o7QShvjNnBsFjwfdeVri";
	
	public static void main(String[] args) throws TembooException {
		String userNameString = "Anglelababy";
		// Instantiate the Choreo, using a previously instantiated TembooSession object, eg:
		//TembooSession session = new TembooSession("derekzhang", "twitterTest", "zdgZDG1234");
		TembooSession session = new TembooSession("derekzhang", "myFirstApp", "dc21fa6431c64c77bc09f3782b5493f2");
		GetFollowersByID getFollowersByIDChoreo = new GetFollowersByID(session);

		// Get an InputSet object for the choreo
		GetFollowersByIDInputSet getFollowersByIDInputs = getFollowersByIDChoreo.newInputSet();

		// Set inputs
		getFollowersByIDInputs.set_ConsumerKey(CONSUMNER_KEY_STRING);
		getFollowersByIDInputs.set_ConsumerSecret(CONSUMNER_SECRET_STRING);
		getFollowersByIDInputs.set_AccessToken(ACCESS_TOKEN_STRING);
		getFollowersByIDInputs.set_AccessTokenSecret(ACCESS_TOKEN_SECRET_STRING);
		getFollowersByIDInputs.set_ScreenName(userNameString);
		getFollowersByIDInputs.set_StringifyIDs(true);
		// Execute Choreo
		GetFollowersByIDResultSet getFollowersByIDResults = getFollowersByIDChoreo.execute(getFollowersByIDInputs);
		
		String limits_String = getFollowersByIDResults.get_Limit();
		String remaining_String = getFollowersByIDResults.get_Remaining();
		String response_String = getFollowersByIDResults.get_Response();
		
		System.out.println("limits is " + limits_String);
		System.out.println("remaining is " + remaining_String);
		System.out.println("response is " + response_String);
		
	}
}
