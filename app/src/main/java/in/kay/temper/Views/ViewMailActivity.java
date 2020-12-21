package in.kay.temper.Views;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import in.kay.temper.Api.RetrofitClient;
import in.kay.temper.Models.SingleMailModel;
import in.kay.temper.R;
import in.kay.temper.databinding.ActivityViewMailBinding;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ViewMailActivity extends AppCompatActivity {
    ActivityViewMailBinding binding;
    private TextToSpeech textToSpeechSystem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_mail);
        binding = ActivityViewMailBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        Integer id = getIntent().getIntExtra("id", 1);
        String prefix = getIntent().getStringExtra("prefix");
        String domain = getIntent().getStringExtra("domain");
        setContentView(view);
        Call<SingleMailModel> call = RetrofitClient.getInstance().getApi().readMessage("readMessage", prefix, domain, id);
        call.enqueue(new Callback<SingleMailModel>() {
            @Override
            public void onResponse(Call<SingleMailModel> call, Response<SingleMailModel> response) {
                String myString = SplitEmail(response.body().getFrom()).get(0);
                String upperString = myString.substring(0, 1).toUpperCase() + myString.substring(1).toLowerCase();
                binding.pb.setVisibility(View.GONE);
                binding.cl.setVisibility(View.VISIBLE);
                binding.tvDate.setText(response.body().getDate());
                binding.textView.setText(response.body().getSubject());
                binding.textView2.loadDataWithBaseURL(null, response.body().getHtmlBody(), "text/html", "UTF-8", null);
                binding.tvName.setText(upperString);
                Picasso.get().load("https://ui-avatars.com/api/?background=234&color=fff&size=256&rounded=true&name=" + binding.tvName.getText().toString()).placeholder(R.drawable.demo_image).into(binding.circleImageView);
                if (!TextUtils.isEmpty(response.body().getTextBody())) {
                    binding.ivTts.setVisibility(View.VISIBLE);
                    binding.ivTts.setOnClickListener(view1 -> {
                        TTS(response.body().getTextBody());
                    });
                }
                if (response.body().getAttachments().size() > 0) {
                    String url = "https://www.1secmail.com/api/v1/?action=download&login=" + prefix + "&domain=" + domain + "&id=" + id + "&file=" + response.body().getAttachments().get(0).getFilename();
                    binding.layoutDownload.setVisibility(View.VISIBLE);
                    binding.tvAttachmentName.setText(response.body().getAttachments().get(0).getFilename());
                    binding.tvSize.setText(response.body().getAttachments().get(0).getSize() / 1000 + " Kb");
                    binding.layoutDownload.setOnClickListener(view1 -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
                        } else {
                            downloadFile(url, response.body().getAttachments().get(0).getFilename());
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<SingleMailModel> call, Throwable t) {

            }
        });
    }

    public void Back(View view) {
        onBackPressed();
    }

    public List<String> SplitEmail(String string) {
        int index = string.indexOf('@');
        int length = string.length();
        String prefix = string.substring(0, index);
        String domain = string.substring(index + 1, length);
        List<String> list = new ArrayList<>();
        list.add(0, prefix);
        list.add(1, domain);
        return list;
    }

    public void TTS(String textBody) {
        textToSpeechSystem = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                if (textToSpeechSystem.isSpeaking() && textToSpeechSystem != null)
                    Toast.makeText(ViewMailActivity.this, "TTS already in progress..", Toast.LENGTH_SHORT).show();
                else {
                    Toast.makeText(this, "Text to speech is activating.. ", Toast.LENGTH_SHORT).show();
                    textToSpeechSystem.setLanguage(new Locale("en", "IN"));
                    String textToSay = textBody;
                    textToSpeechSystem.speak(textToSay, TextToSpeech.QUEUE_ADD, null);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
        if (textToSpeechSystem != null) {
            textToSpeechSystem.shutdown();
            textToSpeechSystem.stop();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (textToSpeechSystem != null) {
            textToSpeechSystem.shutdown();
            textToSpeechSystem.stop();
        }
    }

    public void downloadFile(String string, String name) {
        Toast.makeText(this, "Downloading ...", Toast.LENGTH_SHORT).show();
        File myDirectory = new File("/TemperMail/Attachments");
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(string));
        request.setDescription(name);
        request.setTitle(name);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        }
        request.setDestinationInExternalPublicDir(myDirectory.getAbsolutePath(), name);
        DownloadManager manager = (DownloadManager) this.getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
    }
}