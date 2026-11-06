package dev.soopviewer;
import android.os.Looper;
import android.widget.TextView;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.*;
import androidx.media3.ui.PlayerView;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public class VodPlaybackTest {
 private final List<Long> lengths=Arrays.asList(17999167L,18004000L,4735333L,100L);
 private ExoPlayer player;private MediaSource source;private PlaybackInfo info;
 @Before public void setup(){
  List<MediaSource> parts=new ArrayList<>();info=new PlaybackInfo();info.joinedVod=true;info.durations.addAll(lengths);
  for(int i=0;i<lengths.size();i++){String url="https://fixture.invalid/part-"+i+".m3u8";info.media.add(url);parts.add(new DefaultMediaSourceFactory(RuntimeEnvironment.getApplication()).createMediaSource(new MediaItem.Builder().setUri(url).setMimeType(MimeTypes.APPLICATION_M3U8).build()));}
  source=VodMediaSource.join(parts,lengths);player=new ExoPlayer.Builder(RuntimeEnvironment.getApplication()).build();player.setMediaSource(source);
 }
 @After public void cleanup(){player.release();}
 private VodTimeline.Position position(){return VodMediaSource.position(info,player.getCurrentTimeline(),player.getCurrentPeriodIndex(),player.getCurrentMediaItemIndex(),player.getCurrentPosition());}
 @Test public void entireElevenHourVodIsOneSeekableWindowBeforeLoadingTheFiles(){Timeline t=source.getInitialTimeline();assertNotNull(t);assertEquals(1,t.getWindowCount());assertEquals(4,t.getPeriodCount());assertEquals(40738600L,t.getWindow(0,new Timeline.Window()).getDurationMs());assertEquals(40738600L,player.getDuration());assertEquals(1,player.getMediaItemCount());}
 @Test public void playerControlsDisplayElevenHoursRatherThanTheFirstFiveHourFile(){PlayerView view=new PlayerView(RuntimeEnvironment.getApplication());view.setPlayer(player);view.showController();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals("11:18:59",((TextView)view.findViewById(androidx.media3.ui.R.id.exo_duration)).getText().toString());view.setPlayer(null);}
 @Test public void seekingPastTenHoursReachesTheThirdFileAndKeepsChatTimeRelative(){player.seekTo(36063167L);Shadows.shadowOf(Looper.getMainLooper()).idle();VodTimeline.Position p=position();assertEquals(2,p.part);assertEquals(60000L,p.positionMs);assertEquals(36063167L,player.getCurrentPosition());}
 @Test public void oldPartAndPositionResumeOnTheCombinedClockWithoutLosingFiveHours(){ContentItem item=new ContentItem("211688299","b13246","VOD","타요","","",true);WatchHistory.Entry saved=new WatchHistory.Entry(item,1,42000,40738600,18041167,1,false);long at=VodTimeline.elapsed(lengths,saved.resumePart(4),saved.resumePosition(lengths));player.setMediaSource(source,at);VodTimeline.Position p=position();assertEquals(1,p.part);assertEquals(42000,p.positionMs);assertEquals("5:00:41부터 이어보기",saved.label());}
 @Test public void exactFileBoundaryAdvancesToTheNextChatFile(){VodTimeline.Position p=VodTimeline.locate(lengths,17999167L);assertEquals(1,p.part);assertEquals(0,p.positionMs);}
 @Test public void terminalStubRemainsPartOfTheOriginalDurationWithoutTruncatingTheVod(){VodTimeline.Position p=VodTimeline.locate(lengths,40738550L);assertEquals(3,p.part);assertEquals(50,p.positionMs);assertEquals(40738600L,VodTimeline.elapsed(lengths,4,0));}
 @Test public void scrubPreviewMapsAnArbitraryTenHourTargetToTheThirdFile(){VodTimeline.Position p=VodMediaSource.at(info,player.getCurrentTimeline(),0,36063167L);assertEquals(2,p.part);assertEquals(60000,p.positionMs);VodPreview.Frame frame=VodPreview.frame(new VodPreview.Track("https://videoimg.sooplive.com/snapshot?rowKey=part3_t&column=",lengths.get(2)),p.positionMs);assertTrue(frame.url.endsWith("column=1"));assertEquals(20,frame.cell);}
}
