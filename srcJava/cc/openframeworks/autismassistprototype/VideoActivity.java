package cc.openframeworks.autismassistprototype;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.IntentService;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.os.Bundle;
import android.os.AsyncTask;
import android.widget.Toast;


import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.List;


public class VideoActivity extends cc.openframeworks.OFActivity {
    private String inputFile = "/storage/emulated/0/DCIM/Camera/Autistic.mp4";
    private static VideoActivity mainActivity;
    private String outputFile = "/storage/emulated/0/DCIM/Camera/theVide.wav";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd__hhmmSSS");
    private int[] theArr = new int[4];
    private static final int RECORD_LENGTH = 5000;
    private static final int PICK_VIDEO = 100;

    private static final boolean AUDIO_ENABLED = false;
    private static final int REQUEST_CAMERA_RESULT = 1;
    private static final int SEC_RESULT = 1;
    private static final int THIRD_RESULT = 1;
    private static final int FOURTH_RESULT = 1;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        mainActivity = this;

        int version = Build.VERSION.SDK_INT;
        if (version >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CAMERA_RESULT);
        }

    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_RESULT:
                try {
                    Log.e("Loaded everything ", "Proceed to click: ");
                }
                catch (Exception e) {
                    Log.e("The exception: ", e.getMessage());
                }
                break;
            default:
                break;
        }
    }

    public void chooseVideo(View view) {
        // abort current video detection in progress, if any
        //if (videoThread != null) {
        //    videoThread.abort();
        //}
        Intent mediaChooser = new Intent(Intent.ACTION_GET_CONTENT);
        mediaChooser.setType("video/*");
        startActivityForResult(mediaChooser, PICK_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("Entering: ", "onActivityForResult");
        if (resultCode == RESULT_OK && requestCode == PICK_VIDEO) {
            Uri videoUri = data.getData();
            inputFile = getPath(this,videoUri);
            outputFile = inputFile.split("\\.")[0] + ".wav";
            Intent intent = new Intent(getApplicationContext(), VokaturiService.class);
            intent.putExtra("input", inputFile);
            intent.putExtra("output", outputFile);
            startService(intent);
            //VideoDetectorThread videoThread = new VideoDetectorThread(inputFile,(Activity)mainActivity,(TextView) findViewById(R.id.videoTextView), (DrawingView) findViewById(R.id.drawing_view));
            //videoThread.start();

        } else {
            Toast.makeText(this,"No image selected.",Toast.LENGTH_LONG).show();
        }
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    @SuppressLint("NewApi")
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static class VokaturiService extends IntentService {

        private FFmpeg ffmpeg;
        private String outputFile;
        private String inputFile;
        private String textHolder;
        public VokaturiService() {
            super("WatsoService");
        }


        private void frameRecord() {
            File toDelete = new File(outputFile);
            toDelete.delete();
            try {
                ffmpeg.loadBinary(new FFmpegLoadBinaryResponseHandler() {

                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onFailure() {
                        Log.e("fail", "error loading ffmpeg binary");
                    }

                    @Override
                    public void onSuccess() {
                        Log.e("success", "success loading ffmpeg binary");
                        loadTestVideo();
                    }

                    @Override
                    public void onFinish() {

                    }
                });
            }
            catch (Exception e) {
                Log.e("Fail: ", "ffmpeg didn't load");
            }
        }


        private void loadTestVideo() {
            try {
                String [] cmdArray = {"-i", this.inputFile, this.outputFile};
                ffmpeg.execute(cmdArray, new ExecuteBinaryResponseHandler() {

                    @Override
                    public void onStart() {
                        Log.e("Start: ", "Starting ffmpeg");
                    }

                    @Override
                    public void onProgress(String message) {
                        Log.e("Progress: ", "Started command : ffmpeg " + message);
                    }

                    @Override
                    public void onFailure(String message) {
                        Log.e("Failure: ", message);
                    }

                    @Override
                    public void onFinish() {}

                    @Override
                    public void onSuccess(String message) {
                        Log.e("Success: ", "Processing microphone");
                        try {
                            InputStream myInputStream = new FileInputStream(outputFile);
                        }
                        catch (Exception e) {
                            Log.e("excepted",e.getMessage());
                        }
                    }
                });
            } catch (FFmpegCommandAlreadyRunningException e) {
                Log.e("Here is the error: ", e.getMessage());
            }

        }

        @Override
        protected void onHandleIntent(Intent intent) {

        }
    }

}
