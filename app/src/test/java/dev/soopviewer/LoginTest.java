package dev.soopviewer;
import org.junit.Test;
import org.json.JSONObject;
import java.util.*;
import static org.junit.Assert.*;
public class LoginTest {
 @Test public void nativeLoginUsesOnlyOfficialEndpointAndDoesNotRequestPasswordStorage()throws Exception{new SoopClient(){@Override public JSONObject json(String url,String form,String ref)throws Exception{assertEquals("https://login.sooplive.com/app/LoginAction.php",url);Map<String,String> data=new HashMap<>();for(String field:form.split("&")){String[] kv=field.split("=",2);data.put(kv[0],java.net.URLDecoder.decode(kv[1],"UTF-8"));}assertEquals("user",data.get("szUid"));assertEquals("a+b&= !",data.get("szPassword"));assertEquals("false",data.get("isSavePw"));return new JSONObject("{RESULT:1}");}}.login(" user ","a+b&= !");}
 @Test public void invalidCredentialsAreNotReportedAsSuccess()throws Exception{LoginResult r=LoginResult.parse(new JSONObject("{RESULT:-1}"));assertFalse(r.accepted);assertFalse(r.extraVerification);}
 @Test public void unknownResultRequiresOfficialVerification()throws Exception{LoginResult r=LoginResult.parse(new JSONObject("{RESULT:-11}"));assertFalse(r.accepted);assertTrue(r.extraVerification);assertFalse(LoginResult.parse(new JSONObject()).accepted);}
 @Test public void sessionIdentityComesFromServerResponse()throws Exception{SoopClient client=new SoopClient(){@Override public JSONObject json(String url,String form,String ref)throws Exception{assertEquals("https://afevent2.sooplive.com/api/get_private_info.php",url);return new JSONObject("{CHANNEL:{LOGIN_ID:'fixture'}}");}};assertEquals("fixture",client.loggedInUser());}
}
