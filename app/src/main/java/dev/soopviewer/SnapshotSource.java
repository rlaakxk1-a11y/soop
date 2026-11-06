package dev.soopviewer;
import android.graphics.Bitmap;
public interface SnapshotSource extends AutoCloseable {
 interface Callback {void complete(Bitmap bitmap);}
 void request(VodPreview.Frame frame,Callback callback);
 void cancel();
 void close();
}
