package dev.soopviewer;
import android.content.SharedPreferences;
import java.util.List;
public final class HistoryStore {
 private static final String KEY="vodHistory.v1";
 private final SharedPreferences prefs;
 private final WatchHistory history;
 public HistoryStore(SharedPreferences prefs){this.prefs=prefs;history=WatchHistory.restore(prefs.getString(KEY,"{}"));}
 public synchronized WatchHistory.Entry find(String id){return history.find(id);}
 public synchronized List<WatchHistory.Entry> recent(){return history.recent();}
 public synchronized void save(WatchHistory.Entry entry){history.put(entry);persist();}
 public synchronized void remove(String id){history.remove(id);persist();}
 public synchronized void clear(){history.clear();persist();}
 private void persist(){prefs.edit().putString(KEY,history.serialize()).apply();}
}
