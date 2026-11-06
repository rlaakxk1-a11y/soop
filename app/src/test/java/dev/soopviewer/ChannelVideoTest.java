package dev.soopviewer;
import org.junit.Test;
import org.json.JSONObject;
import static org.junit.Assert.*;
public class ChannelVideoTest {
 @Test public void channelVideosUseNestedVideoThumbnailInsteadOfProfile()throws Exception{
  ContentItem item=ContentItem.parse(new JSONObject("{data:[{title_no:123,user_id:'demo',profile_image:'//profile.img.sooplive.com/a.jpg',ucc:{thumb:'//videoimg.sooplive.com/video.jpg',total_file_duration:14027700}}]}"),true).get(0);
  assertEquals("https://videoimg.sooplive.com/video.jpg",item.thumb);assertEquals("3:53:47",item.duration);
 }
}
