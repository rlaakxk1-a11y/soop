package dev.soopviewer;
import org.json.JSONObject;
/** Do not treat redirects, cookies or unknown result codes as proof of login. */
public final class LoginResult {
 public final int code;public final boolean accepted,extraVerification;public final String message;
 private LoginResult(int code,boolean accepted,boolean extra,String message){this.code=code;this.accepted=accepted;extraVerification=extra;this.message=message;}
 public static LoginResult parse(JSONObject response){int code=response.optInt("RESULT",Integer.MIN_VALUE);if(code==1)return new LoginResult(code,true,false,"계정을 확인하는 중…");if(code==0||code==-1||code==-3)return new LoginResult(code,false,false,"아이디 또는 비밀번호를 확인하세요");return new LoginResult(code,false,true,"계정 확인 또는 추가 인증이 필요합니다. 아래 인증 화면에서 계속하세요.");}
}
