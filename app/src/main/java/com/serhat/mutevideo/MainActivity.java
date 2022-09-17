package com.serhat.mutevideo;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.serhat.mutevideo.databinding.ActivityMainBinding;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private Context context;
    private ActivityMainBinding binding;

    private String video_url;
    private final String root = Environment.getExternalStorageDirectory().toString();
    private final String app_folder = root + "/Movies/MutedVideos/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = MainActivity.this;

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setMainActivity(this);
    }

    @SuppressLint("IntentReset")
    public void selectVideo() {
        if (checkPermission()) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            intent.setType(getString(R.string.content_type));
            startActivityForResult(intent, 100);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
        }
    }

    public boolean createDir(File storageDir) {
        if (!storageDir.exists()) {
            return storageDir.mkdirs();
        } else {
            return true;
        }
    }

    public void muteVideo() {
        if (video_url != null) {
            if (!createDir(new File(app_folder))) {
                //Log.e(getString(R.string.tag_error), getString(R.string.msg_directory_couldnt_created));
                Toast.makeText(context, getString(R.string.msg_video_couldnt_be_muted), Toast.LENGTH_SHORT).show();
                return;
            }

            String filePrefix = getString(R.string.file_prefix);
            String fileExtension = getString(R.string.file_extension);

            File dest = new File(new File(app_folder), filePrefix + System.currentTimeMillis() + fileExtension);
            String filePath = dest.getAbsolutePath();

            //delete "/raw" part with substring
            String exe = "-i " + video_url.substring(4) + " -c copy -an " + filePath;
            FFmpeg.executeAsync(exe, (executionId, returnCode) -> {
                if (returnCode == RETURN_CODE_SUCCESS) {
                    binding.videoView.setVideoURI(Uri.parse(filePath));
                    binding.videoView.start();

                    Toast.makeText(context, getString(R.string.msg_video_muted), Toast.LENGTH_SHORT).show();
                    MediaScannerConnection.scanFile(this, new String[]{filePath}, null, (path, uri) -> { });
                } else if (returnCode == RETURN_CODE_CANCEL) {
                    //Log.e(getString(R.string.tag_error), getString(R.string.msg_command_cancelled));
                    Toast.makeText(context, getString(R.string.msg_video_couldnt_be_muted), Toast.LENGTH_SHORT).show();
                } else {
                    //Log.e(getString(R.string.tag_error), String.format(getString(R.string.msg_command_failed), returnCode));
                    Toast.makeText(context, getString(R.string.msg_video_couldnt_be_muted), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(context, getString(R.string.msg_select_a_video), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                File video_file = new File(uri.getPath());

                binding.videoView.setVideoURI(uri);
                binding.videoView.setVisibility(View.VISIBLE);
                binding.videoView.start();

                video_url = video_file.getAbsolutePath();
            } catch (Exception e) {
                Log.e(getString(R.string.tag_error), e.getMessage());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, getString(R.string.msg_permission_granted), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, getString(R.string.msg_permission_denied), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean checkPermission() {
        int permWrite = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permRead = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);

        return permWrite == PackageManager.PERMISSION_GRANTED && permRead == PackageManager.PERMISSION_GRANTED;
    }
}