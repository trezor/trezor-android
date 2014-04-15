package com.circlegate.liban.adapter;

import java.util.HashMap;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FixedFragmentUtils;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.circlegate.liban.utils.FragmentUtils;

/**
 * Upravena verte FragmenStatePagerAdapter z compatibility library v4
 * Navic se v teto verzi resi, aby vsechny fragmenty mely prirazen platny Tag.
 *
 * Implementation of {@link android.support.v4.view.PagerAdapter} that
 * uses a {@link Fragment} to manage each page. This class also handles
 * saving and restoring of fragment's state.
 *
 * <p>This version of the pager is more useful when there are a large number
 * of pages, working more like a list view.  When pages are not visible to
 * the user, their entire fragment may be destroyed, only keeping the saved
 * state of that fragment.  This allows the pager to hold on to much less
 * memory associated with each visited page as compared to FragmentPagerAdapter
 * at the cost of potentially more overhead when
 * switching between pages.
 *
 * <p>When using FragmentPagerAdapter the host ViewPager must have a
 * valid ID set.</p>
 *
 * <p>Here is an example implementation of a pager containing fragments of
 * lists:
 *
 * {@sample development/samples/Support13Demos/src/com/example/android/supportv13/app/FragmentStatePagerSupport.java
 *      complete}
 *
 * <p>The <code>R.layout.fragment_pager</code> resource of the top-level fragment is:
 *
 * {@sample development/samples/Support13Demos/res/layout/fragment_pager.xml
 *      complete}
 *
 * <p>The <code>R.layout.fragment_pager_list</code> resource containing each
 * individual fragment's layout is:
 *
 * {@sample development/samples/Support13Demos/res/layout/fragment_pager_list.xml
 *      complete}
 */
public abstract class FragmentStatePagerAdapterWtTags extends PagerAdapter {
    private static final String TAG = FragmentStatePagerAdapterWtTags.class.getSimpleName();
    private static final boolean DEBUG = false;

    private final FragmentManager mFragmentManager;
    private FragmentTransaction mCurTransaction = null;

    private HashMap<String, Fragment.SavedState> mSavedState = new HashMap<>();
    private HashMap<String, Fragment> mFragments = new HashMap<>();
    private Fragment mCurrentPrimaryItem = null;

    public FragmentStatePagerAdapterWtTags(FragmentManager fm) {
        mFragmentManager = fm;
    }

    public abstract Fragment createFragmentAt(int position);
    public abstract String getFragmentTagAt(int position);

    public int getFragmentPositionIfNeeded(String fragmentTag) {
        return POSITION_UNCHANGED;
    }

    public Fragment getFragmentByFragmentTagIfAny(String fragmentTag) {
        return mFragments.get(fragmentTag);
    }

    public Fragment getCurrentPrimaryFragmentIfAny() {
        return mCurrentPrimaryItem;
    }

    public String getCurrentPrimaryFragmentTagIfAny() {
        return mCurrentPrimaryItem != null ? FragmentUtils.getTagNotNull(mCurrentPrimaryItem) : null;
    }

    public void onPrimaryFragmentChanged(String fragmentTag, Fragment fragment) {
    }


    @Override
    public void startUpdate(ViewGroup container) {
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        String tag = getFragmentTagAt(position);
        Fragment fragment;

        // If we already have this item instantiated, there is nothing
        // to do.  This can happen when we are restoring the entire pager
        // from its saved state, where the fragment manager has already
        // taken care of restoring the fragments we previously had instantiated.
        if ((fragment = mFragments.get(tag)) != null) {
            return fragment;
        }

        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }

        fragment = createFragmentAt(position);
        if (DEBUG) Log.v(TAG, "Adding item #" + position + ": f=" + fragment);
        Fragment.SavedState fss = mSavedState.get(tag);
        if (fss != null) {
            fragment.setInitialSavedState(fss);
        }
        fragment.setMenuVisibility(false);
        mFragments.put(tag, fragment);
        mCurTransaction.add(container.getId(), fragment, tag);

        // oprava podle https://code.google.com/p/android/issues/detail?id=37484
        Bundle savedFragmentState = FixedFragmentUtils.getFragmentSavedStateField(fragment);
        if (savedFragmentState != null) {
            savedFragmentState.setClassLoader(fragment.getClass().getClassLoader());
        }
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        Fragment fragment = (Fragment)object;
        String tag = FragmentUtils.getTagNotNull(fragment);

        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }
        if (DEBUG) Log.v(TAG, "Removing item #" + position + ": f=" + object
                + " v=" + ((Fragment)object).getView());

        Fragment.SavedState fss = mFragmentManager.saveFragmentInstanceState(fragment);
        if (fss != null)
            mSavedState.put(tag, fss);
        mFragments.remove(tag);

        mCurTransaction.remove(fragment);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        Fragment fragment = (Fragment)object;
        if (fragment != mCurrentPrimaryItem) {
            if (mCurrentPrimaryItem != null) {
                mCurrentPrimaryItem.setMenuVisibility(false);
            }
            if (fragment != null) {
                fragment.setMenuVisibility(true);
            }
            mCurrentPrimaryItem = fragment;
            onPrimaryFragmentChanged(FragmentUtils.getTagNotNull(fragment), fragment);
        }
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        if (mCurTransaction != null) {
            mCurTransaction.commitAllowingStateLoss();
            mCurTransaction = null;
            mFragmentManager.executePendingTransactions();
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return ((Fragment)object).getView() == view;
    }

    @Override
    public int getItemPosition(Object object) {
        String tag = FragmentUtils.getTagNotNull((Fragment)object);
        return getFragmentPositionIfNeeded(tag);
    }

    @Override
    public Parcelable saveState() {
        Bundle state = null;

        if (mSavedState.size() > 0) {
            state = new Bundle();

            Fragment.SavedState[] fss = new Fragment.SavedState[mSavedState.size()];
            String[] ssTags = new String[mSavedState.size()];
            int counter = 0;

            for (HashMap.Entry<String, Fragment.SavedState> entry : mSavedState.entrySet()) {
                fss[counter] = entry.getValue();
                ssTags[counter] = entry.getKey();
                counter++;
            }

            state.putParcelableArray("states", fss);
            state.putStringArray("ssTags", ssTags);
        }

        if (mFragments.size() > 0) {
            if (state == null) {
                state = new Bundle();
            }

            state.putStringArray("fragmentTags", mFragments.keySet().toArray(new String[mFragments.size()]));

            for (HashMap.Entry<String, Fragment> entry : mFragments.entrySet()) {
                mFragmentManager.putFragment(state, entry.getKey(), entry.getValue());
            }
        }

        return state;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
        if (state != null) {
            Bundle bundle = (Bundle)state;
            bundle.setClassLoader(loader);
            Parcelable[] fss = bundle.getParcelableArray("states");
            String[] ssTags = bundle.getStringArray("ssTags");
            mSavedState.clear();
            mFragments.clear();
            if (fss != null) {
                for (int i=0; i<fss.length; i++) {
                    mSavedState.put(ssTags[i], (Fragment.SavedState)fss[i]);
                }
            }

            String[] fragmentTags = bundle.getStringArray("fragmentTags");
            for (String tag : fragmentTags) {
                Fragment f = mFragmentManager.getFragment(bundle, tag);
                if (f != null) {
                    f.setMenuVisibility(false);
                    mFragments.put(tag, f);
                } else {
                    Log.w(TAG, "Bad fragment at key " + tag);
                }
            }
        }
    }
}


