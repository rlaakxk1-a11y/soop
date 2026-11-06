package dev.soopviewer;
import androidx.media3.common.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class HoldPlaybackSpeedTest {
 private float speed=1.25f;private boolean playing=true;private int changes;
 private HoldPlaybackSpeed control(){Player p=(Player)java.lang.reflect.Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(proxy,method,args)->{switch(method.getName()){case "isPlaying":return playing;case "getPlaybackParameters":return new PlaybackParameters(speed);case "setPlaybackSpeed":speed=(Float)args[0];return null;default:return null;}});return new HoldPlaybackSpeed(p,()->changes++);}
 @Test public void releaseRestoresTheOriginalCustomPlaybackRate(){HoldPlaybackSpeed c=control();c.start();assertTrue(c.held());assertEquals(2,speed,0);c.stop();assertFalse(c.held());assertEquals(1.25f,speed,0);assertEquals(2,changes);}
 @Test public void cancellationAndPlayerTeardownCannotLeaveTwoTimesSpeedEnabled(){HoldPlaybackSpeed c=control();speed=.75f;c.start();c.close();assertEquals(.75f,speed,0);assertFalse(c.held());}
 @Test public void repeatedLongPressDoesNotOverwriteTheOriginalRate(){HoldPlaybackSpeed c=control();c.start();c.start();c.stop();assertEquals(1.25f,speed,0);assertEquals(2,changes);}
 @Test public void pausedVideoDoesNotStartOrChangeSpeedOnLongPress(){playing=false;HoldPlaybackSpeed c=control();c.start();assertFalse(c.held());assertEquals(1.25f,speed,0);assertEquals(0,changes);}
}
