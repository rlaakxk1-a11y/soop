package dev.soopviewer;
import java.util.*;
public final class BrowseOptions {
 public static final String[] LIVE_KEYS={"view_cnt","view_cnt_asc","broad_start"},LIVE_LABELS={"시청자 많은순","시청자 적은순","최신 방송순"};
 public static final String[] VIDEO_KEYS={"reg_date","reg_date_asc","read_cnt"},VIDEO_LABELS={"최신순","오래된순","조회 많은순"};
 public static final String[] SEARCH_KEYS={"score","reg_date","view_cnt"},SEARCH_LABELS={"관련도순","최신순","조회·시청자순"};
 public static int index(String[] keys,String value){int i=Arrays.asList(keys).indexOf(value);return Math.max(0,i);}
 public static String checked(String[] keys,String value){if(!Arrays.asList(keys).contains(value))throw new IllegalArgumentException("정렬 기준이 올바르지 않습니다");return value;}
 public static List<ContentItem> additions(List<ContentItem> existing,List<ContentItem> incoming){Set<String> seen=new HashSet<>();for(ContentItem item:existing)seen.add(item.vod+":"+item.user+":"+item.id);List<ContentItem> out=new ArrayList<>();for(ContentItem item:incoming)if(seen.add(item.vod+":"+item.user+":"+item.id))out.add(item);return out;}
}
