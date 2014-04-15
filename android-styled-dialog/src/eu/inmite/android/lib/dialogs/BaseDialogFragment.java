/*
 * Copyright 2013 Inmite s.r.o. (www.inmite.eu).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.inmite.android.lib.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

/**
 * Base dialog fragment for all your dialogs, stylable and same design on Android 2.2+.
 *
 * @author David Vávra (david@inmite.eu)
 */
public abstract class BaseDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new Dialog(getActivity(), R.style.SDL_Dialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Builder builder = new Builder(this, getActivity(), inflater, container);
        return build(builder, savedInstanceState).create();
    }

    protected abstract Builder build(Builder initialBuilder, Bundle savedInstanceState);

    @Override
    public void onDestroyView() {
        // bug in the compatibility library
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    /**
     * Custom dialog builder
     */
    protected static class Builder {

        private DialogFragment mDialogFragment;
        private Context mContext;
        private ViewGroup mContainer;
        private LayoutInflater mInflater;
        private CharSequence mTitle = null;
        private Drawable mTitleIcon;
        private boolean mButtonsTopDivider = false;
        private boolean mButtonsMatchWidth = false;
        private boolean mButtonsSmallHeight = false;
        private boolean mButtonsVertical = false;
        private CharSequence mPositiveButtonText;
        private View.OnClickListener mPositiveButtonListener;
        private CharSequence mNegativeButtonText;
        private View.OnClickListener mNegativeButtonListener;
        private CharSequence mNeutralButtonText;
        private View.OnClickListener mNeutralButtonListener;
        private CharSequence mMessage;
        private View mView;
        private boolean mViewSpacingSpecified;
        private int mViewSpacingLeft;
        private int mViewSpacingTop;
        private int mViewSpacingRight;
        private int mViewSpacingBottom;
        private Button vPositiveButton;
        private ListAdapter mListAdapter;
        private int mListCheckedItemIdx;
        private AdapterView.OnItemClickListener mOnItemClickListener;

        public Builder(DialogFragment dialogFragment, Context context, LayoutInflater inflater, ViewGroup container) {
            this.mDialogFragment = dialogFragment;
            this.mContext = context;
            this.mContainer = container;
            this.mInflater = inflater;
        }

        public Builder setTitle(int titleId) {
            this.mTitle = mContext.getText(titleId);
            return this;
        }

        public Builder setTitle(CharSequence title) {
            this.mTitle = title;
            return this;
        }

        public Builder setTitleIcon(Drawable titleIcon) {
            this.mTitleIcon = titleIcon;
            return this;
        }

        public Builder setButtonsTopDivider(boolean buttonsTopDivider) {
            mButtonsTopDivider = buttonsTopDivider;
            return this;
        }

        public Builder setButtonsMatchWidth(boolean matchWidth) {
            mButtonsMatchWidth = matchWidth;
            return this;
        }

        public Builder setButtonsSmallHeight(boolean smallHeight) {
            mButtonsSmallHeight = smallHeight;
            return this;
        }

        public Builder setButtonsVertical(boolean buttonsVertical) {
            mButtonsVertical = buttonsVertical;
            return this;
        }


        public Builder setPositiveButton(int textId, final View.OnClickListener listener) {
            mPositiveButtonText = mContext.getText(textId);
            mPositiveButtonListener = listener;
            return this;
        }

        public Builder setPositiveButton(CharSequence text, final View.OnClickListener listener) {
            mPositiveButtonText = text;
            mPositiveButtonListener = listener;
            return this;
        }

        public Builder setNegativeButton(int textId, final View.OnClickListener listener) {
            mNegativeButtonText = mContext.getText(textId);
            mNegativeButtonListener = listener;
            return this;
        }

        public Builder setNegativeButton(CharSequence text, final View.OnClickListener listener) {
            mNegativeButtonText = text;
            mNegativeButtonListener = listener;
            return this;
        }

        public Builder setNeutralButton(int textId, final View.OnClickListener listener) {
            mNeutralButtonText = mContext.getText(textId);
            mNeutralButtonListener = listener;
            return this;
        }

        public Builder setNeutralButton(CharSequence text, final View.OnClickListener listener) {
            mNeutralButtonText = text;
            mNeutralButtonListener = listener;
            return this;
        }

        public Builder setMessage(int messageId) {
            mMessage = mContext.getText(messageId);
            return this;
        }

        public Builder setMessage(CharSequence message) {
            mMessage = message;
            return this;
        }

        /** Set list
         *
         * @param listAdapter
         * @param checkedItemIdx Item check by default, -1 if no item should be checked
         * @param listener
         * @return
         */
        public Builder setItems(ListAdapter listAdapter, int checkedItemIdx, final AdapterView.OnItemClickListener listener) {
            mListAdapter = listAdapter;
            mOnItemClickListener = listener;
            mListCheckedItemIdx = checkedItemIdx;
            return this;
        }

        public Builder setView(View view) {
            mView = view;
            mViewSpacingSpecified = false;
            return this;
        }

        public Builder setView(View view, int viewSpacingLeft, int viewSpacingTop,
                               int viewSpacingRight, int viewSpacingBottom) {
            mView = view;
            mViewSpacingSpecified = true;
            mViewSpacingLeft = viewSpacingLeft;
            mViewSpacingTop = viewSpacingTop;
            mViewSpacingRight = viewSpacingRight;
            mViewSpacingBottom = viewSpacingBottom;
            return this;
        }

        public View create() {
            View v = getDialogLayoutAndInitTitle();

            LinearLayout content = (LinearLayout) v.findViewById(R.id.sdl__content);

            if (mMessage != null) {
                View viewMessage = mInflater.inflate(R.layout.dialog_part_message, content, false);
                TextView tvMessage = (TextView) viewMessage.findViewById(R.id.sdl__message);
                tvMessage.setText(mMessage);
                if (mTitle == null) {
                    tvMessage.setPadding(tvMessage.getPaddingLeft(), tvMessage.getResources().getDimensionPixelOffset(R.dimen.sdl__text_top_padding_big),
                            tvMessage.getPaddingRight(), tvMessage.getPaddingBottom());
                }
                if (mNegativeButtonText == null && mNegativeButtonText == null && mPositiveButtonText == null) {
                    if (mTitle == null) {
                        tvMessage.setPadding(tvMessage.getPaddingLeft(), tvMessage.getPaddingTop(),
                                tvMessage.getPaddingRight(), tvMessage.getResources().getDimensionPixelOffset(R.dimen.sdl__text_bottom_padding_big));
                    }
                }
                content.addView(viewMessage);
            }

            if (mView != null) {
                FrameLayout customPanel = (FrameLayout) mInflater.inflate(R.layout.dialog_part_custom, content, false);
                FrameLayout custom = (FrameLayout) customPanel.findViewById(R.id.sdl__custom);
                custom.addView(mView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                if (mViewSpacingSpecified) {
                    custom.setPadding(mViewSpacingLeft, mViewSpacingTop, mViewSpacingRight, mViewSpacingBottom);
                }
                content.addView(customPanel);
            }

            if (mListAdapter != null) {
                ListView list = (ListView) mInflater.inflate(R.layout.dialog_part_list, content, false);
                list.setAdapter(mListAdapter);
                list.setOnItemClickListener(mOnItemClickListener);
                if (mListCheckedItemIdx != -1) {
                    list.setSelection(mListCheckedItemIdx);
                }
                content.addView(list);
            }

            addButtons(content);

            return v;
        }

        private View getDialogLayoutAndInitTitle() {
            View v = mInflater.inflate(R.layout.dialog_part_title, mContainer, false);
            TextView tvTitle = (TextView) v.findViewById(R.id.sdl__title);
            if (mTitle != null) {
                tvTitle.setText(mTitle);
                if (mTitleIcon != null) {
                    tvTitle.setCompoundDrawables(mTitleIcon, null, null, null);
                }
            } else {
                tvTitle.setVisibility(View.GONE);
            }
            return v;
        }

        private void addButtons(LinearLayout llListDialog) {
            if (mNegativeButtonText != null || mNeutralButtonText != null || mPositiveButtonText != null) {

                View viewButtonPanel = mInflater.inflate(R.layout.dialog_part_button_panel, llListDialog, false);
                LinearLayout llButtonPanel = (LinearLayout)viewButtonPanel.findViewById(R.id.dialog_button_panel);

                if (mButtonsTopDivider) {
                    View divider = new View(llListDialog.getContext());
                    divider.setBackgroundColor(llListDialog.getResources().getColor(R.color.dialog_button_separator));
                    llListDialog.addView(divider, LinearLayout.LayoutParams.MATCH_PARENT, 1);
                }
                if (mButtonsSmallHeight) {
                    llButtonPanel.setPadding(llButtonPanel.getPaddingLeft(), 0, llButtonPanel.getPaddingRight(), 0);
                }
                if (mButtonsVertical) {
                    llButtonPanel.setOrientation(LinearLayout.VERTICAL);
                }

                if (mNegativeButtonText != null) {
                    Button btn = (Button) mInflater.inflate(R.layout.dialog_part_button, llButtonPanel, false);
                    btn.setText(mNegativeButtonText);
                    btn.setOnClickListener(mNegativeButtonListener);
                    btn.setId(android.R.id.button2);
                    if (mButtonsMatchWidth)
                        ((LinearLayout.LayoutParams)btn.getLayoutParams()).weight = 1;
                    llButtonPanel.addView(btn);
                }
                if (mNeutralButtonText != null) {
//                    if (mNegativeButtonText != null) {
//                        addDivider(llButtonPanel);
//                    }
                    Button btn = (Button) mInflater.inflate(R.layout.dialog_part_button, llButtonPanel, false);
                    btn.setText(mNeutralButtonText);
                    btn.setOnClickListener(mNeutralButtonListener);
                    btn.setId(android.R.id.button3);
                    if (mButtonsMatchWidth)
                        ((LinearLayout.LayoutParams)btn.getLayoutParams()).weight = 1;
                    llButtonPanel.addView(btn);
                }
                if (mPositiveButtonText != null) {
//                    if (mNegativeButtonText != null || mNeutralButtonText != null) {
//                        addDivider(llButtonPanel);
//                    }
                    vPositiveButton = (Button) mInflater.inflate(R.layout.dialog_part_button, llButtonPanel, false);
                    vPositiveButton.setText(mPositiveButtonText);
                    vPositiveButton.setOnClickListener(mPositiveButtonListener);
                    vPositiveButton.setId(android.R.id.button1);
                    if (mButtonsMatchWidth)
                        ((LinearLayout.LayoutParams)vPositiveButton.getLayoutParams()).weight = 1;
                    llButtonPanel.addView(vPositiveButton);
                }

                ((Button)llButtonPanel.getChildAt(llButtonPanel.getChildCount() - 1)).setTextColor(llButtonPanel.getResources().getColorStateList(R.color.dialog_button_text_primary));

                llListDialog.addView(viewButtonPanel);
            }
        }

//        private void addDivider(ViewGroup parent) {
//            mInflater.inflate(R.layout.dialog_part_button_separator, parent, true);
//        }
    }
}
