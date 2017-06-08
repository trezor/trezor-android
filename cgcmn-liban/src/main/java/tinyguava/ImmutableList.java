package tinyguava;

import android.support.annotation.NonNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ImmutableList<E> extends ArrayList<E> {

    public ImmutableList(int size) {
        super(size);
    }

    private void _addAll(Collection<? extends E> c) {
        super.addAll(c);
    }

    @Override
    public boolean add(final E e) {
        throw (new UnsupportedOperationException());
    }

    @Override
    public boolean remove(final Object o) {
        throw (new UnsupportedOperationException());
    }

    @Override
    public boolean addAll(@NonNull final Collection<? extends E> c) {
        throw (new UnsupportedOperationException());
    }

    @Override
    public boolean addAll(final int index, @NonNull final Collection<? extends E> c) {
        throw (new UnsupportedOperationException());
    }

    @Override
    public boolean removeAll(@NonNull final Collection<?> c) {
        throw (new UnsupportedOperationException());
    }

    @Override
    public boolean retainAll(@NonNull final Collection<?> c) {
        throw (new UnsupportedOperationException());
    }

    @Override
    public void clear() {
        throw (new UnsupportedOperationException());
    }

    @Override
    public E set(final int index, final E element) {
        throw (new UnsupportedOperationException());
    }

    @Override
    public E remove(final int index) {
        throw (new UnsupportedOperationException());
    }


    public static <E> ImmutableList<E> copyOf(List<E> from) {
        final Builder<E> builder = builder();
        builder.addAll(from);
        return builder.build();
    }

    public static <E> Builder<E> builder() {
        return new Builder<>();
    }

    public static <E> ImmutableList<E> of() {
        return new Builder<E>().build();
    }

    public static class Builder<E> {

        private final ArrayList<E> arr;

        public Builder() {
            arr = new ArrayList<>();
        }

        public void add(E e) {
            arr.add(e);
        }

        void addAll(Collection<? extends E> c) {
            arr.addAll(c);
        }

        public ImmutableList<E> build() {
            final ImmutableList<E> list = new ImmutableList<>(arr.size());
            list._addAll(arr);
            return list;
        }
    }
}
