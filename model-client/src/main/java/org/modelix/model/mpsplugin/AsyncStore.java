package org.modelix.model.mpsplugin;



import org.modelix.model.IKeyValueStore;
import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;
import jetbrains.mps.internal.collections.runtime.MapSequence;
import java.util.LinkedHashMap;
import org.modelix.model.IKeyListener;
import java.util.List;
import jetbrains.mps.internal.collections.runtime.ListSequence;
import java.util.ArrayList;
import java.util.Iterator;
import jetbrains.mps.internal.collections.runtime.IMapping;
import java.util.Objects;
import org.apache.log4j.Level;

public class AsyncStore implements IKeyValueStore {
  private static final Logger LOG = LogManager.getLogger(AsyncStore.class);
  private IKeyValueStore store;

  private final AtomicBoolean consumerActive = new AtomicBoolean();
  private Map<String, String> pendingWrites = MapSequence.fromMap(new LinkedHashMap<String, String>(16, (float) 0.75, false));

  public AsyncStore(IKeyValueStore store) {
    this.store = store;
  }

  @Override
  public String get(String key) {
    synchronized (pendingWrites) {
      if (MapSequence.fromMap(pendingWrites).containsKey(key)) {
        return MapSequence.fromMap(pendingWrites).get(key);
      }
    }
    return store.get(key);
  }

  @Override
  public void listen(final String key, final IKeyListener listener) {
    store.listen(key, listener);
  }

  @Override
  public void removeListener(final String key, final IKeyListener listener) {
    store.removeListener(key, listener);
  }

  @Override
  public void put(final String key, final String value) {
    synchronized (pendingWrites) {
      MapSequence.fromMap(pendingWrites).put(key, value);
    }
    processQueue();
  }

  @Override
  public Map<String, String> getAll(Iterable<String> keys_) {
    List<String> keys = ListSequence.fromListWithValues(new ArrayList<String>(), keys_);
    Map<String, String> result = MapSequence.fromMap(new LinkedHashMap<String, String>(16, (float) 0.75, false));
    synchronized (pendingWrites) {
      Iterator<String> itr = ListSequence.fromList(keys).iterator();
      while (itr.hasNext()) {
        String key = itr.next();
        // always put even if null to have the same order in the linked hash map as in the input 
        MapSequence.fromMap(result).put(key, MapSequence.fromMap(pendingWrites).get(key));
        if (MapSequence.fromMap(pendingWrites).containsKey(key)) {
          itr.remove();
        }
      }
    }
    if (ListSequence.fromList(keys).isNotEmpty()) {
      MapSequence.fromMap(result).putAll(store.getAll(keys));
    }
    return result;
  }

  @Override
  public void putAll(Map<String, String> entries) {
    synchronized (pendingWrites) {
      MapSequence.fromMap(pendingWrites).putAll(entries);
    }
    processQueue();
  }

  @Override
  public void prefetch(String key) {
    store.prefetch(key);
  }

  protected void processQueue() {
    if (consumerActive.compareAndSet(false, true)) {
      SharedExecutors.FIXED.execute(new Runnable() {
        public void run() {
          try {
            while (MapSequence.fromMap(pendingWrites).isNotEmpty()) {
              try {
                Map<String, String> entries = MapSequence.fromMap(new LinkedHashMap<String, String>(16, (float) 0.75, false));
                synchronized (pendingWrites) {
                  MapSequence.fromMap(entries).putAll(pendingWrites);
                }
                store.putAll(entries);
                synchronized (pendingWrites) {
                  for (IMapping<String, String> entry : MapSequence.fromMap(entries)) {
                    if (Objects.equals(MapSequence.fromMap(pendingWrites).get(entry.key()), entry.value())) {
                      MapSequence.fromMap(pendingWrites).removeKey(entry.key());
                    }
                  }
                }
              } catch (Exception ex) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                  LOG.error("", ex);
                }
                try {
                  Thread.sleep(1000);
                } catch (InterruptedException ex2) {
                  return;
                }
              }
            }
          } finally {
            consumerActive.set(false);
          }
        }
      });
    }
  }

  public void dispose() {
  }
}
