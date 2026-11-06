package dev.soopviewer;
import androidx.media3.common.Player;

/** Temporary gesture speed; restores the exact previous rate on release, cancellation or teardown. */
public final class HoldPlaybackSpeed implements AutoCloseable {
 private final Player player;private final Runnable changed;private boolean held;private float previous;
 public HoldPlaybackSpeed(Player player,Runnable changed){this.player=player;this.changed=changed;}
 public boolean held(){return held;}
 public void start(){if(held||!player.isPlaying())return;previous=player.getPlaybackParameters().speed;held=true;player.setPlaybackSpeed(2f);changed.run();}
 public void stop(){if(!held)return;held=false;player.setPlaybackSpeed(previous);changed.run();}
 @Override public void close(){stop();}
}
