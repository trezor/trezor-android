package com.satoshilabs.trezor.app.style;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.SpannedString;

import com.circlegate.liban.utils.ColorUtils;
import com.satoshilabs.trezor.app.common.GlobalContext;
import com.satoshilabs.trezor.app.style.Html.TagHandler;

import org.xml.sax.Attributes;
import org.xml.sax.XMLReader;

import java.util.Stack;

public class CustomHtml {	
    public static final char HARD_SPACE = (char)0xf00a0;
    public static final String TAG_LINE_BREAK = "<BR />";
    
	protected static final String SPAN_TAG = "span";
    protected static final String SPAN_ATTR_TYPE_NAME = "class";

    protected static final String SPAN_ATTR_TYPE_SIZE = "size";
    protected static final String SPAN_ATTR_SIZE_VALUE = "size";

    protected static final String SPAN_ATTR_TYPE_RELATIVE_SIZE = "rltsz";

	private static Typeface boldTypeface;

	public static Typeface getBoldTypeface(Context context) {
		if (boldTypeface == null) {
			boldTypeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
		}
		return boldTypeface;
	}

	public static String getHardSpaces2(int count) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < count; i++) {
            ret.append(i == count - 1 ? " " : HARD_SPACE);
        }
        return ret.toString();
    }

	public static String getFontColorTag(String text, int color) {
		return "<font color=\"" + ColorUtils.getHtmlColor(color) + "\">" + text + "</font>";
	}

    public static String getTextSizeTag(String s, int textSizePx) {
        return "<" + SPAN_TAG + " " + SPAN_ATTR_TYPE_NAME + "=\"" + SPAN_ATTR_TYPE_SIZE + "\" " + SPAN_ATTR_SIZE_VALUE + "=\"" + textSizePx + "\">" + s + "</" + SPAN_TAG + ">";
    }

    public static String getRelativeTextSizeTag(String s, float proportion) {
        return "<" + SPAN_TAG + " " + SPAN_ATTR_TYPE_NAME + "=\"" + SPAN_ATTR_TYPE_RELATIVE_SIZE + "\" " + SPAN_ATTR_SIZE_VALUE + "=\"" + (int)(proportion * 100) + "\">" + s + "</" + SPAN_TAG + ">";
    }
	
	public static String getBoldTag(String text) {
	    return "<b>" + text + "</b>";
	}
	
	public static String getImgTag(String srcAttr) {
	    return "<img src=\"" + srcAttr + "\" />";
	}

	
	public static Spanned fromHtmlWithCustomSpans(String source) {
		return fromHtmlWithCustomSpans(source, null);
	}
	
	public static Spanned fromHtmlWithCustomSpans(String source, Html.ImageGetter imageGetter) {
		return fromHtmlWithCustomSpans(source, imageGetter, (Html.TagHandler)null);
	}


	public static Spanned fromHtmlWithCustomSpans(String source, Html.ImageGetter imageGetter, Html.TagHandler tagHandler) {
		if (source.indexOf('\n') >= 0) {
		    source = source.replace("\n", TAG_LINE_BREAK);
		}
		
		return SpannedString.valueOf(Html.fromHtml(source, imageGetter, new CustomTagHandler(tagHandler)));
	}



	
	public static class CustomTagHandler implements TagHandler {
		protected final Context context;
		private final TagHandler providedTagHandler;
		protected final Stack<IHtmlTagSpan> spansStack = new Stack<IHtmlTagSpan>();
		
		public CustomTagHandler(TagHandler providedTagHandler) {
			this.context = GlobalContext.get().getAndroidContext();
			this.providedTagHandler = providedTagHandler;
		}
		
		public boolean handleStartTag(String tag, Attributes attributes, Editable output, XMLReader xmlReader) {
			if (tag.equalsIgnoreCase(SPAN_TAG)) {
                String type = attributes.getValue("", SPAN_ATTR_TYPE_NAME);
                if (handleSpanStartTag(type, attributes, output))
                    return true;
                else {
                    spansStack.push(null);
                    return this.providedTagHandler != null && this.providedTagHandler.handleStartTag(tag, attributes, output, xmlReader);
                }
			}
			else {
				return this.providedTagHandler != null && this.providedTagHandler.handleStartTag(tag, attributes, output, xmlReader);
			}
		}

		public boolean handleEndTag(String tag, Editable output, XMLReader xmlReader) {
			if (tag.equalsIgnoreCase(SPAN_TAG)) {
				IHtmlTagSpan spanTag = spansStack.pop();
				if (handleSpanEndTag(spanTag, output))
                    return true;
                else if (spanTag == null) {
                    return this.providedTagHandler != null && this.providedTagHandler.handleEndTag(tag, output, xmlReader);
                }
                else
					throw new RuntimeException();
			}
			else {
				return this.providedTagHandler != null && this.providedTagHandler.handleEndTag(tag, output, xmlReader);
			}
		}


        protected boolean handleSpanStartTag(String type, Attributes attributes, Editable output) {
            if (SPAN_ATTR_TYPE_SIZE.equalsIgnoreCase(type)) {
                spansStack.push(processSizeStart(output, attributes));
                return true;
            }
            else if (SPAN_ATTR_TYPE_RELATIVE_SIZE.equalsIgnoreCase(type)) {
                spansStack.push(processRelativeSizeStart(output, attributes));
                return true;
            }
            else
                return false;
        }

        protected boolean handleSpanEndTag(IHtmlTagSpan spanTag, Editable output) {
            if (spanTag instanceof AbsoluteSizeSpan) {
                processSizeEnd(output);
                return true;
            }
            else if (spanTag instanceof RelativeSizeSpan) {
                processRelativeSizeEnd(output);
                return true;
            }
            else
                return false;
        }


        private AbsoluteSizeSpan processSizeStart(Editable output, Attributes attributes) {
            int len = output.length();
            AbsoluteSizeSpan ret = new AbsoluteSizeSpan(Integer.parseInt(attributes.getValue("", SPAN_ATTR_SIZE_VALUE)));
            output.setSpan(ret, len, len, Spannable.SPAN_MARK_MARK);
            return ret;
        }

        private void processSizeEnd(Editable output) {
            int len = output.length();
            Object obj = getLast(output, AbsoluteSizeSpan.class);
            int where = output.getSpanStart(obj);

            output.removeSpan(obj);

            if (where != len) {
                output.setSpan(obj, where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }


        private RelativeSizeSpan processRelativeSizeStart(Editable output, Attributes attributes) {
            int len = output.length();
            RelativeSizeSpan ret = new RelativeSizeSpan(((float) Integer.parseInt(attributes.getValue("", SPAN_ATTR_SIZE_VALUE)) / 100));
            output.setSpan(ret, len, len, Spannable.SPAN_MARK_MARK);
            return ret;
        }

        private void processRelativeSizeEnd(Editable output) {
            int len = output.length();
            Object obj = getLast(output, RelativeSizeSpan.class);
            int where = output.getSpanStart(obj);

            output.removeSpan(obj);

            if (where != len) {
                output.setSpan(obj, where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }


	    protected Object getLast(Editable text, @SuppressWarnings("rawtypes") Class kind) {
	        @SuppressWarnings("unchecked")
			Object[] objs = text.getSpans(0, text.length(), kind);

	        if (objs.length == 0) {
	            return null;
	        } else {
	            for(int i = objs.length;i>0;i--) {
	                if(text.getSpanFlags(objs[i-1]) == Spannable.SPAN_MARK_MARK) {
	                    return objs[i-1];
	                }
	            }
	            return null;
	        }
	    }
	}
}