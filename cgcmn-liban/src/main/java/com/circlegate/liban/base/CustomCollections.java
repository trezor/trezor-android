package com.circlegate.liban.base;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import com.circlegate.liban.base.CommonClasses.EntryImpl;

public class CustomCollections {
    public interface ICache<TKey, TValue> {
        int size();
        boolean containsKey(TKey key);
        void put(TKey key, TValue value);
        TValue get(TKey key);
        TValue remove(TKey key);
        void clear();
        List<Entry<TKey, TValue>> generateAll();
        ISynchronizedCache<TKey, TValue> getSynchronizedCache();
    }

    public interface ISynchronizedCache<TKey, TValue> extends ICache<TKey, TValue> {
        Object getLock();
    }


    public static class SynchronizedCache<TKey, TValue> implements ISynchronizedCache<TKey, TValue> {
        private final ICache<TKey, TValue> wrappedCache;

        public SynchronizedCache(ICache<TKey, TValue> wrappedCache) {
            this.wrappedCache = wrappedCache;
        }

        @Override
        public synchronized int size() {
            return wrappedCache.size();
        }

        @Override
        public synchronized boolean containsKey(TKey key) {
            return wrappedCache.containsKey(key);
        }

        @Override
        public synchronized void put(TKey key, TValue value) {
            wrappedCache.put(key, value);
        }

        @Override
        public synchronized TValue get(TKey key) {
            return wrappedCache.get(key);
        }

        @Override
        public synchronized TValue remove(TKey key) {
            return wrappedCache.remove(key);
        }

        @Override
        public synchronized void clear() {
            wrappedCache.clear();
        }

        @Override
        public synchronized List<Entry<TKey, TValue>> generateAll() {
            return wrappedCache.generateAll();
        }

        @Override
        public ISynchronizedCache<TKey, TValue> getSynchronizedCache() {
            return this;
        }

        @Override
        public Object getLock() {
            return this;
        }
    }

    /**
     * An LRU cache, based on <code>LinkedHashMap</code>.
     *
     * <p>
     * This cache has a fixed maximum number of elements (<code>cacheSize</code>
     * ). If the cache is full and another entry is added, the LRU (least
     * recently used) entry is dropped.
     * <p>
     */
    public static class LRUCache<TKey, TValue> implements ICache<TKey, TValue> {
        private static final float hashTableLoadFactor = 0.75f;

        private LinkedHashMap<TKey, TValue> map;
        private int cacheSize;

        /**
         * Creates a new LRU cache.
         *
         * @param cacheSize
         *            the maximum number of entries that will be kept in this
         *            cache.
         */
        public LRUCache(int cacheSize) {
            this.cacheSize = cacheSize;
            int hashTableCapacity = (int) Math.ceil(cacheSize
                    / hashTableLoadFactor) + 1;
            map = new LinkedHashMap<TKey, TValue>(hashTableCapacity,
                    hashTableLoadFactor, true) {
                // (an anonymous inner class)
                private static final long serialVersionUID = 1;

                @Override
                protected boolean removeEldestEntry(Entry<TKey, TValue> eldest) {
                    return size() > LRUCache.this.cacheSize;
                }
            };
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public boolean containsKey(TKey key) {
            return map.containsKey(key);
        }

        /**
         * Retrieves an entry from the cache.<br>
         * The retrieved entry becomes the MRU (most recently used) entry.
         *
         * @param key
         *            the key whose associated value is to be returned.
         * @return the value associated to this key, or null if no value with
         *         this key exists in the cache.
         */
        @Override
        public TValue get(TKey key) {
            return map.get(key);
        }

        /**
         * Adds an entry to this cache. The new entry becomes the MRU (most
         * recently used) entry. If an entry with the specified key already
         * exists in the cache, it is replaced by the new entry. If the cache is
         * full, the LRU (least recently used) entry is removed from the cache.
         *
         * @param key
         *            the key with which the specified value is to be
         *            associated.
         * @param value
         *            a value to be associated with the specified key.
         */
        @Override
        public void put(TKey key, TValue value) {
            map.put(key, value);
        }

        /**
         * Clears the cache.
         */
        @Override
        public void clear() {
            map.clear();
        }

        @Override
        public TValue remove(TKey key) {
            return map.remove(key);
        }

        @Override
        public List<Entry<TKey, TValue>> generateAll() {
            return new ArrayList<Entry<TKey, TValue>>(map.entrySet());
        }

        @Override
        public ISynchronizedCache<TKey, TValue> getSynchronizedCache() {
            return new SynchronizedCache<TKey, TValue>(this);
        }
    }

    public static class CacheWeakRef<TKey, TValue> implements ICache<TKey, TValue> {
        private final HashMap<TKey, WeakReference<TValue>> cache = new HashMap<TKey, WeakReference<TValue>>();
        private int counter = 0;

        @Override
        public int size() {
            return cache.size();
        }

        @Override
        public boolean containsKey(TKey key) {
            WeakReference<TValue> w = cache.get(key);
            return w != null && w.get() != null;
        }

        @Override
        public void put(TKey key, TValue value) {
            //  v kazdem pripade chci, aby v cachi byla posledni hodnota value
            if (cache.put(key, new WeakReference<TValue>(value)) == null) {
                if (counter++ >= 100) {
                    removeDeadReferences();
                    counter = 0;
                }
            }
        }

        @Override
        public TValue get(TKey key) {
            WeakReference<TValue> w = cache.get(key);
            return w != null ? w.get() : null;
        }

        @Override
        public TValue remove(TKey key) {
            WeakReference<TValue> w = cache.remove(key);
            return w != null ? w.get() : null;
        }

        @Override
        public void clear() {
            cache.clear();
        }

        @Override
        public List<Entry<TKey, TValue>> generateAll() {
            ArrayList<Entry<TKey, TValue>> ret = new ArrayList<Entry<TKey,TValue>>();
            for (Entry<TKey, WeakReference<TValue>> entry : cache.entrySet()) {
                TValue v = entry.getValue().get();
                if (v != null)
                    ret.add(new EntryImpl<TKey, TValue>(entry.getKey(), v));
            }
            return ret;
        }

        @Override
        public ISynchronizedCache<TKey, TValue> getSynchronizedCache() {
            return new SynchronizedCache<TKey, TValue>(this);
        }


        void removeDeadReferences() {
            for (Iterator<Entry<TKey, WeakReference<TValue>>> it = cache
                    .entrySet().iterator(); it.hasNext();) {
                Entry<TKey, WeakReference<TValue>> entry = it.next();
                if (entry.getValue().get() == null) {
                    it.remove();
                }
            }
        }
    }
}
