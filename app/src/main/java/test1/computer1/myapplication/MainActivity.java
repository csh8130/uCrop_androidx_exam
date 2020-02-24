package test1.computer1.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Calendar;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_SELECT_PICTURE = 0x01;
    private static final String SAMPLE_CROPPED_IMAGE_NAME = "SampleCropImage.jpeg";

    private static final int RATIO_ORIGIN = 0;
    private static final int RATIO_SQUARE = 1;
    private static final int RATIO_DYNAMIC = 2;
    private static final int RATIO_CUSTOM = 3;

    private static final int FORMAT_PNG = 0;
    private static final int FORMAT_WEBP = 1;
    private static final int FORMAT_JPEG = 2;



    private Uri mDestinationUri;
    private ImageView mImageView;
    private Uri mResultUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // setup crop
        mDestinationUri = Uri.fromFile(new File(getCacheDir(), SAMPLE_CROPPED_IMAGE_NAME));
        mImageView = ((ImageView) findViewById(R.id.image_view_preview));
        _setupPickButton();
        _setupSaveButton();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();



        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_SELECT_PICTURE) {
                final Uri selectedUri = data.getData();
                if (selectedUri != null) {
                    startCropActivity(data.getData());
                } else {
                    Toast.makeText(MainActivity.this,
                            R.string.toast_cannot_retrieve_selected_image,
                            Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == UCrop.REQUEST_CROP) {
                handleCropResult(data);
            }
        }
        if (resultCode == UCrop.RESULT_ERROR) {
            handleCropError(data);
        }
    }

    private void startCropActivity(@NonNull Uri uri) {
        UCrop uCrop = UCrop.of(uri, mDestinationUri);

        uCrop = _setRatio(uCrop, RATIO_ORIGIN, 0, 0);
        uCrop = _setSize(uCrop, 0, 0);

        uCrop = _advancedConfig(uCrop, FORMAT_JPEG, 90);

        uCrop.start(MainActivity.this);
    }


    private void handleCropResult(@NonNull Intent result) {
        mResultUri = UCrop.getOutput(result);
        if (mResultUri != null) {
            // ResultActivity.startWithUri(MainActivity.this, resultUri);
            Toast.makeText(MainActivity.this, mResultUri.toString(), Toast.LENGTH_SHORT).show();
            mImageView.setImageURI(mResultUri);


        } else {
            Toast.makeText(MainActivity.this, R.string.toast_cannot_retrieve_cropped_image, Toast.LENGTH_SHORT).show();
        }
    }


    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private void handleCropError(@NonNull Intent result) {
        final Throwable cropError = UCrop.getError(result);
        if (cropError != null) {
            Log.e(TAG, "handleCropError: ", cropError);
            Toast.makeText(MainActivity.this, cropError.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(MainActivity.this, R.string.toast_unexpected_error, Toast.LENGTH_SHORT).show();
        }
    }


    private void _setupSaveButton() {
        findViewById(R.id.button_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _saveCroppedImage();
            }
        });


    }

    private void _saveCroppedImage() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    getString(R.string.permission_write_storage_rationale),
                    REQUEST_STORAGE_WRITE_ACCESS_PERMISSION);
        } else {
            Uri imageUri = mResultUri;
            if (imageUri != null && imageUri.getScheme().equals("file")) {
                try {
                    copyFileToDownloads(imageUri);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, imageUri.toString(), e);
                }
            } else {
                Toast.makeText(MainActivity.this, getString(R.string.toast_unexpected_error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void copyFileToDownloads(Uri croppedFileUri) throws Exception {
        String downloadsDirectoryPath
                = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .getAbsolutePath();
        String filename = String.format(
                "%d_%s",
                Calendar.getInstance().getTimeInMillis(),
                croppedFileUri.getLastPathSegment());

        File saveFile = new File(downloadsDirectoryPath, filename);

        FileInputStream inStream = new FileInputStream(new File(croppedFileUri.getPath()));
        FileOutputStream outStream = new FileOutputStream(saveFile);
        FileChannel inChannel = inStream.getChannel();
        FileChannel outChannel = outStream.getChannel();
        inChannel.transferTo(0, inChannel.size(), outChannel);
        inStream.close();
        outStream.close();

        // showNotification(saveFile);
        Toast.makeText(MainActivity.this, getString(R.string.toast_image_saved), Toast.LENGTH_SHORT).show();
    }

    private void _setupPickButton(){


        findViewById(R.id.button_crop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _pickFromGallery();
            }
        });

    }


    private void _pickFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN // Permission was added in API Level 16
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE,
                    getString(R.string.permission_read_storage_rationale),
                    REQUEST_STORAGE_READ_ACCESS_PERMISSION);    // @see onRequestPermissionsResult()
        } else {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(
                    Intent.createChooser(intent, getString(R.string.label_select_picture)),
                    REQUEST_SELECT_PICTURE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_STORAGE_READ_ACCESS_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    _pickFromGallery();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }




    private UCrop _setRatio(@NonNull UCrop uCrop, int choice, float xratio, float yratio){
        switch (choice) {
            case RATIO_ORIGIN:
                uCrop = uCrop.useSourceImageAspectRatio();
                break;
            case RATIO_SQUARE:
                uCrop = uCrop.withAspectRatio(1, 1);
                break;
            case RATIO_DYNAMIC:
                // do nothing
                break;
            case RATIO_CUSTOM:
            default:
                try {
                    float ratioX = xratio;
                    float ratioY = yratio;
                    if (ratioX > 0 && ratioY > 0) {
                        uCrop = uCrop.withAspectRatio(ratioX, ratioY);
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Number please", e);
                }
                break;
        }

        return uCrop;

    }

    private UCrop _setSize(@NonNull UCrop uCrop, int maxWidth, int maxHeight){
        if(maxWidth > 0 && maxHeight > 0){
            return uCrop.withMaxResultSize(maxWidth, maxHeight);
        }
        return uCrop;
    }


    /**
     * Sometimes you want to adjust more options, it's done via {@link com.yalantis.ucrop.UCrop.Options} class.
     *
     * @param uCrop - ucrop builder instance
     * @return - ucrop builder instance
     */
    private UCrop _advancedConfig(@NonNull UCrop uCrop, int format, int quality) {
        UCrop.Options options = new UCrop.Options();


        switch (format) {
            case FORMAT_PNG:
                options.setCompressionFormat(Bitmap.CompressFormat.PNG);
                break;
            case FORMAT_WEBP:
                options.setCompressionFormat(Bitmap.CompressFormat.WEBP);
                break;
            case FORMAT_JPEG:
            default:
                options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
                break;
        }
        options.setCompressionQuality(quality); // range [0-100]

        /*
        If you want to configure how gestures work for all UCropActivity tabs

        options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.ALL);
        * */

        /*
        This sets max size for bitmap that will be decoded from source Uri.
        More size - more memory allocation, default implementation uses screen diagonal.

        options.setMaxBitmapSize(640);
        * */


       /*

        Tune everything (ﾉ◕ヮ◕)ﾉ*:･ﾟ✧

        options.setMaxScaleMultiplier(5);
        options.setImageToCropBoundsAnimDuration(666);
        options.setDimmedLayerColor(Color.CYAN);
        options.setOvalDimmedLayer(true);
        options.setShowCropFrame(false);
        options.setCropGridStrokeWidth(20);
        options.setCropGridColor(Color.GREEN);
        options.setCropGridColumnCount(2);
        options.setCropGridRowCount(1);

        // Color palette
        options.setToolbarColor(ContextCompat.getColor(this, R.color.your_color_res));
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.your_color_res));
        options.setActiveWidgetColor(ContextCompat.getColor(this, R.color.your_color_res));
		options.setToolbarTitleTextColor(ContextCompat.getColor(this, R.color.your_color_res));

       */

        return uCrop.withOptions(options);
    }
}
