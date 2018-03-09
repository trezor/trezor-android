package com.satoshilabs.trezor.app.activity;

import android.Manifest.permission;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.GridLayoutManager.SpanSizeLookup;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.TextPaint;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.circlegate.liban.base.ApiBase.ApiCreator;
import com.circlegate.liban.base.ApiBase.ApiParcelable;
import com.circlegate.liban.base.ApiDataIO.ApiDataInput;
import com.circlegate.liban.base.ApiDataIO.ApiDataOutput;
import com.circlegate.liban.task.TaskCommon.TaskParam;
import com.circlegate.liban.task.TaskCommon.TaskResult;
import com.circlegate.liban.task.TaskErrors.BaseError;
import com.circlegate.liban.task.TaskErrors.ITaskError;
import com.circlegate.liban.task.TaskErrors.TaskException;
import com.circlegate.liban.task.TaskInterfaces.ITask;
import com.circlegate.liban.task.TaskInterfaces.ITaskContext;
import com.circlegate.liban.task.TaskInterfaces.ITaskResult;
import com.circlegate.liban.task.TaskInterfaces.ITaskResultListener;
import com.circlegate.liban.utils.ActivityUtils;
import com.circlegate.liban.utils.LogUtils;
import com.circlegate.liban.utils.ViewUtils;
import tinyguava.ImmutableList;

import com.satoshilabs.trezor.app.R;
import com.satoshilabs.trezor.app.activity.base.BaseActivity;
import com.satoshilabs.trezor.app.common.GlobalContext;
import com.satoshilabs.trezor.app.common.TrezorTasks.TrezorTaskParam;
import com.satoshilabs.trezor.app.view.CustomActionBar;
import com.satoshilabs.trezor.lib.TrezorImageUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ChangeHomescreenActivity extends BaseActivity {
    private static final String TAG = ChangeHomescreenActivity.class.getSimpleName();

    private static final String TASK_PREPARE_IMAGE = "TACK_PREPARE_IMAGE";

    //private static final int TREZOR_SCREEN_WIDTH = 128;
    //private static final int TREZOR_SCREEN_HEIGHT = 64;

    // Views
    private RecyclerView recyclerView;

    // Immutable members
    private boolean isV2;
    private int imageWidth;
    private int imageHeight;
    private ImmutableList<BitmapDrawable> images;
    private Adapter adapter;
    private Handler handler = new Handler(Looper.getMainLooper());


    public static Intent createIntent(Context context, String trezorLabel, boolean isV2) {
        return new Intent(context, ChangeHomescreenActivity.class)
                .putExtra("trezorLabel", trezorLabel)
                .putExtra("isV2", isV2);
    }


    //
    // LIFECYCLE CALLBACKS
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.isV2 = getIntent().getBooleanExtra("isV2", false);

        CustomActionBar.setContentView(this, R.layout.change_homescreen_activity, true);

        this.recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        ViewUtils.addOnGlobalLayoutCalledOnce(recyclerView, new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int gridWidth = recyclerView.getWidth() - recyclerView.getPaddingLeft() - recyclerView.getPaddingRight();
                if (gridWidth > 0) {
                    Context context = recyclerView.getContext();

                    imageWidth = ViewUtils.getPixelsFromDp(context, isV2 ? 96 : getTrezorScreenWidth());
                    int columns = gridWidth / imageWidth;

                    if (columns <= 0) {
                        columns = 1;
                        imageWidth = gridWidth;
                    }
                    else {
                        imageWidth += (gridWidth % imageWidth) / columns;
                    }
                    final Resources res = getResources();

                    if (isV2) {
                        imageHeight = imageWidth;
                    }
                    else {
                        imageHeight = (imageWidth - (res.getDimensionPixelOffset(R.dimen.homescreen_img_padding_outer_hor) * 2) - (res.getDimensionPixelOffset(R.dimen.homescreen_img_padding_inner) * 2)) / 2;
                        imageHeight += (res.getDimensionPixelOffset(R.dimen.homescreen_img_padding_inner) * 2) + (res.getDimensionPixelOffset(R.dimen.homescreen_img_padding_outer_vert) * 2);
                    }

                    try {
                        final AssetManager assets = getAssets();
                        final String dir = isV2 ? "homescreens-v2" : "homescreens";
                        final String[] files = assets.list(dir);

                        ImmutableList.Builder<BitmapDrawable> bImages = ImmutableList.builder();
                        for (int i = 0; i < files.length; i++) {
                            String file = files[i];
                            InputStream stream = null;
                            try {
                                String assetName = dir + File.separator + file;
                                stream = assets.open(assetName);

                                if (!isV2) {
                                    if (i == 0) {
                                        Bitmap bmpSrc = BitmapFactory.decodeStream(stream);
                                        Bitmap bmp = Bitmap.createBitmap(bmpSrc.getWidth(), bmpSrc.getHeight(), Bitmap.Config.ARGB_8888);
                                        Canvas canvas = new Canvas(bmp);
                                        canvas.drawBitmap(bmpSrc, new Rect(0, 0, bmpSrc.getWidth(), bmpSrc.getHeight()), new Rect(0, 0, bmp.getWidth(), bmp.getHeight()), null);
                                        TextPaint paint = new TextPaint();
                                        paint.setTextSize(12);
                                        paint.setColor(Color.WHITE);
                                        paint.setFakeBoldText(true);
                                        paint.setAntiAlias(true);
                                        String text = getIntent().getStringExtra("trezorLabel");
                                        float textWidth = paint.measureText(text);
                                        canvas.drawText(text, (bmp.getWidth() - textWidth) / 2, bmp.getHeight() - 2, paint);
                                        bImages.add(new BitmapDrawable(getResources(), bmp));
                                    } else {
                                        bImages.add(new BitmapDrawable(getResources(), stream));
                                    }
                                }
                                else {
                                    Bitmap bitmap = BitmapFactory.decodeStream(stream);
                                    Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
                                    Canvas canvas = new Canvas(output);
                                    final Paint paint = new Paint();
                                    final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

                                    paint.setAntiAlias(true);
                                    canvas.drawARGB(0, 0, 0, 0);
                                    canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2, bitmap.getWidth() / 2, paint);
                                    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
                                    canvas.drawBitmap(bitmap, rect, rect, paint);
                                    bImages.add(new BitmapDrawable(getResources(), output));
                                }
                            }
                            finally {
                                try {
                                    if (stream != null)
                                        stream.close();
                                }
                                catch (Exception ex) {
                                    LogUtils.e(TAG, "Exception closing stream", ex);
                                }
                            }
                        }
                        images = bImages.build();
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    final int columnsFinal = columns;
                    GridLayoutManager layoutManager = new GridLayoutManager(context, columns);
                    layoutManager.setSpanSizeLookup(new SpanSizeLookup() {
                        @Override
                        public int getSpanSize(int position) {
                            return position == 0 ? columnsFinal : 1;
                        }
                    });
                    recyclerView.setLayoutManager(layoutManager);


                    adapter = new Adapter();
                    recyclerView.setAdapter(adapter);
                }
            }
        });

        setResult(RESULT_CANCELED);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GlobalContext.RQC_PICK_CUSTOM_IMAGE) {
            if (resultCode == RESULT_OK && data != null) {
                try {
                    Bitmap bmp = getCorrectlyOrientedImage(this, data.getData(), 1024);
                    final Bitmap bmpOut = Bitmap.createBitmap(getTrezorScreenWidth(), getTrezorScreenHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bmpOut);

                    final Rect srcRect;
                    if (getTrezorScreenWidth() / getTrezorScreenHeight() > bmp.getWidth() / bmp.getHeight()) {
                        final int srcW = bmp.getWidth();
                        final int srcH = (srcW * getTrezorScreenHeight()) / getTrezorScreenWidth();
                        final int srcT = (bmp.getHeight() - srcH) / 2;
                        srcRect = new Rect(0, srcT, srcW, srcT + srcH);
                    }
                    else {
                        final int srcH = bmp.getHeight();
                        final int srcW = (srcH * getTrezorScreenWidth()) / getTrezorScreenHeight();
                        final int srcL = (bmp.getWidth() - srcW) / 2;
                        srcRect = new Rect(srcL, 0, srcL + srcW, srcH);
                    }

                    canvas.drawBitmap(bmp, srcRect, new Rect(0, 0, getTrezorScreenWidth(), getTrezorScreenHeight()), null);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            finishWithImage(bmpOut);
                        }
                    });
                }
                catch (Exception ex) {
                    LogUtils.e(TAG, "Exception loading image", ex);
                    BaseError.ERR_UNKNOWN_ERROR.showToast(GlobalContext.get());
                }
            }
        }
        else
            super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStart() {
        super.onStart();
        onRequestPermissionsResultReceiver.register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        onRequestPermissionsResultReceiver.unregister(this);
    }


    //
    // CUSTOM CALLBACKS
    //

    @Override
    public void onOtherTaskCompleted(String id, ITaskResult result, Bundle bundle) {
        if (id.equals(TASK_PREPARE_IMAGE)) {
            getDialogsFragment().hideProgressDialog();

            if (result.isValidResult()) {
                PrepareImageResult res = (PrepareImageResult)result;
                ActivityUtils.setResultParcelable(ChangeHomescreenActivity.this, RESULT_OK, new ChangeHomescreenActivityResult(res.imageData));
                finish();
            }
            else
                Toast.makeText(this, result.getError().getMsg(GlobalContext.get()), Toast.LENGTH_LONG).show();
        }
        else
            super.onOtherTaskCompleted(id, result, bundle);
    }


    //
    // PRIVATE
    //

    private int getTrezorScreenWidth() {
        return isV2 ? 144 : 128;
    }

    private int getTrezorScreenHeight() {
        return isV2 ? 144 : 64;
    }

    private void pickCustomImage() {
        if (ActivityCompat.checkSelfPermission(this, permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ChangeHomescreenActivity.this, new String[] { permission.READ_EXTERNAL_STORAGE }, OnRequestPermissionsResultReceiver.RQC_COMMON);
        }
        else {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            photoPickerIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try {
                startActivityForResult(photoPickerIntent, GlobalContext.RQC_PICK_CUSTOM_IMAGE);
            }
            catch (Exception ex) {
                BaseError.ERR_UNKNOWN_ERROR.showToast(GlobalContext.get());
            }
        }
    }

    private void finishWithImage(Bitmap bmp) {
        if (bmp.getWidth() != getTrezorScreenWidth() || bmp.getHeight() != getTrezorScreenHeight())
            throw new IllegalArgumentException("Bitmap must be exactly: " + getTrezorScreenWidth() + "x" + getTrezorScreenHeight() + " pixels");

        getTaskFragment().cancelTask(TASK_PREPARE_IMAGE, null);
        PrepareImageParam param = new PrepareImageParam(bmp, isV2);
        getDialogsFragment().showProgressDialog(getString(R.string.loading), false);
        getTaskFragment().executeTask(TASK_PREPARE_IMAGE, param, null, false, null);
    }

    private static Bitmap getCorrectlyOrientedImage(Context context, Uri photoUri, int maxLongerSizeCca) throws IOException {
        InputStream is = null;

        try {
            is = context.getContentResolver().openInputStream(photoUri);
            BitmapFactory.Options dbo = new BitmapFactory.Options();
            dbo.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, dbo);
            is.close();


            int rotatedWidth, rotatedHeight;
            int orientation = getImageOrientation(context, photoUri);

            if (orientation == 90 || orientation == 270) {
                LogUtils.d("ImageUtil", "Will be rotated");
                rotatedWidth = dbo.outHeight;
                rotatedHeight = dbo.outWidth;
            } else {
                rotatedWidth = dbo.outWidth;
                rotatedHeight = dbo.outHeight;
            }

            Bitmap srcBitmap;
            is = context.getContentResolver().openInputStream(photoUri);
            LogUtils.d("ImageUtil", String.format("rotatedWidth=%s, rotatedHeight=%s, maxLongerSizeCca=%s",
                    rotatedWidth, rotatedHeight, maxLongerSizeCca));
            if (rotatedWidth > maxLongerSizeCca || rotatedHeight > maxLongerSizeCca) {
                float widthRatio = ((float) rotatedWidth) / ((float) maxLongerSizeCca);
                float heightRatio = ((float) rotatedHeight) / ((float) maxLongerSizeCca);
                float maxRatio = Math.max(widthRatio, heightRatio);
                LogUtils.d("ImageUtil", String.format("Shrinking. maxRatio=%s",
                        maxRatio));

                // Create the bitmap from file
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = (int) maxRatio;
                srcBitmap = BitmapFactory.decodeStream(is, null, options);
            } else {
                LogUtils.d("ImageUtil", String.format("No need for Shrinking. maxRatio=%s",
                        1));

                srcBitmap = BitmapFactory.decodeStream(is);
                LogUtils.d("ImageUtil", String.format("Decoded bitmap successful"));
            }
            is.close();

            /*
             * if the orientation is not 0 (or -1, which means we don't know), we
             * have to do a rotation.
             */
            if (orientation > 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(orientation);

                srcBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(),
                        srcBitmap.getHeight(), matrix, true);
            }

            return srcBitmap;
        }
        finally {
            try {
                if (is != null)
                    is.close();
            }
            catch (Exception ex) {
                LogUtils.e(TAG, "getCorrectlyOrientedImage: Exception closing stream", ex);
            }
        }
    }

    private static int getImageOrientation(Context context, Uri photoUri) {
        Cursor cursor = null;

        try {
            cursor = context.getContentResolver().query(photoUri,
                    new String[]{MediaStore.Images.ImageColumns.ORIENTATION}, null, null, null);

            if (cursor == null || cursor.getCount() != 1) {
                return 0;
            }

            cursor.moveToFirst();
            return cursor.getInt(0);
        }
        finally {
            if (cursor != null) {
                try {
                    cursor.close();
                }
                catch (Exception ex) {
                    LogUtils.e(TAG, "getImageOrientation: Exception closing cursor", ex);
                }
            }
        }
    }





    private final OnRequestPermissionsResultReceiver onRequestPermissionsResultReceiver = new OnRequestPermissionsResultReceiver() {
        @Override
        public void onRequestPermissionsResultReceived(Context context, int requestCode, String[] permissions, int[] grantResults) {
            if (requestCode == RQC_COMMON) {
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickCustomImage();
                }
            }
        }
    };


    //
    // INNER CLASSES
    //
    private static class PrepareImageParam extends TaskParam {
        public final Bitmap bmp;
        public final boolean isV2;

        public PrepareImageParam(Bitmap bmp, boolean isV2) {
            this.bmp = bmp;
            this.isV2 = isV2;
        }

        @Override
        public String getSerialExecutionKey(ITaskContext context) {
            return TrezorTaskParam.SERIAL_EXECUTION_KEY_TREZOR;
        }

        @Override
        public PrepareImageResult createErrorResult(ITaskContext context, ITask task, ITaskError error) {
            return new PrepareImageResult(this, error, null);
        }

        @Override
        public PrepareImageResult createResultUncached(ITaskContext context, ITask task) throws TaskException {
            final byte[] byteArray;

            if (!isV2) {
                int[] pixels = new int[bmp.getWidth() * bmp.getHeight()];
                bmp.getPixels(pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());

                byteArray = new byte[pixels.length / 8];
                float[] hsv = new float[3];

                for (int i = 0; i < pixels.length; i += 8) {
                    int curr = 0;

                    for (int j = 0; j < 8; j++) {
                        int color = pixels[i + 7 - j];

                        Color.colorToHSV(color, hsv );
                        if (hsv[2] > 0.5f) {
                            curr |= 1 << j;
                        }
                    }
                    byteArray[i / 8] = (byte)curr;
                }
            }
            else {
                try {
                    byteArray = TrezorImageUtils.processBitmap(bmp);
                } catch (IOException e) {
                    return createErrorResult(context, task, BaseError.ERR_UNKNOWN_ERROR);
                }
            }
            return new PrepareImageResult(this, BaseError.ERR_OK, byteArray);
        }
    }

    private static class PrepareImageResult extends TaskResult<PrepareImageParam> {
        public final byte[] imageData;

        public PrepareImageResult(PrepareImageParam param, ITaskError error, byte[] imageData) {
            super(param, error);
            this.imageData = imageData;
        }
    }


    private class Adapter extends RecyclerView.Adapter<ViewHolderBase> {
        @Override
        public int getItemViewType(int position) {
            return position == 0 ? R.layout.change_homescreen_item_custom_img : R.layout.change_homescreen_item;
        }

        @Override
        public int getItemCount() {
            return images.size() + 1;
        }

        @Override
        public ViewHolderBase onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == R.layout.change_homescreen_item_custom_img) {
                return new ViewHolderPickCustomImg(getLayoutInflater().inflate(R.layout.change_homescreen_item_custom_img, parent, false));
            }
            else {
                ViewHolderImage ret = new ViewHolderImage((ImageButton) getLayoutInflater().inflate(R.layout.change_homescreen_item, parent, false));
                ret.imageButton.getLayoutParams().width = imageWidth;
                ret.imageButton.getLayoutParams().height = imageHeight;

                if (isV2)
                    ViewUtils.setBackgroundResourceKeepPadding(ret.imageButton, R.drawable.bg_homescreen_item_v2);
                return ret;
            }
        }

        @Override
        public void onBindViewHolder(ViewHolderBase holder, int position) {
            if (getItemViewType(position) == R.layout.change_homescreen_item) {
                ((ViewHolderImage)holder).imageButton.setImageDrawable(images.get(position - 1));
            }
        }
    }

    private class ViewHolderBase extends ViewHolder {
        public ViewHolderBase(View itemView) {
            super(itemView);
        }
    }

    private class ViewHolderPickCustomImg extends ViewHolderBase {
        public final View btnPickCustomImg;
        public final TextView txtDesc;

        public ViewHolderPickCustomImg(View itemView) {
            super(itemView);
            this.btnPickCustomImg = itemView.findViewById(R.id.btn_pick_custom_img);
            this.txtDesc = itemView.findViewById(R.id.txt_desc);

            if (isV2)
                txtDesc.setText(R.string.change_homescreen_custom_img_advice_v2);

            btnPickCustomImg.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    pickCustomImage();
                }
            });
        }
    }

    private class ViewHolderImage extends ViewHolderBase {
        public final ImageButton imageButton;

        public ViewHolderImage(ImageButton imageButton) {
            super(imageButton);
            this.imageButton = imageButton;

            imageButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition() - 1;
                    if (position >= 0) {
                        if (position == 0) {
                            ActivityUtils.setResultParcelable(ChangeHomescreenActivity.this, RESULT_OK, new ChangeHomescreenActivityResult(new byte[0]));
                            finish();
                        }
                        else {
                            Bitmap bmp = images.get(position).getBitmap();
                            finishWithImage(bmp);
                        }
                    }
                }
            });
        }
    }

    public static class ChangeHomescreenActivityResult extends ApiParcelable {
        public final byte[] homescreen;

        public ChangeHomescreenActivityResult(byte[] homescreen) {
            this.homescreen = homescreen;
        }

        public ChangeHomescreenActivityResult(ApiDataInput d) {
            this.homescreen = d.readBytes();
        }

        @Override
        public void save(ApiDataOutput d, int flags) {
            d.write(this.homescreen);
        }

        public static final ApiCreator<ChangeHomescreenActivityResult> CREATOR = new ApiCreator<ChangeHomescreenActivityResult>() {
            public ChangeHomescreenActivityResult create(ApiDataInput d) { return new ChangeHomescreenActivityResult(d); }
            public ChangeHomescreenActivityResult[] newArray(int size) { return new ChangeHomescreenActivityResult[size]; }
        };
    }
}
