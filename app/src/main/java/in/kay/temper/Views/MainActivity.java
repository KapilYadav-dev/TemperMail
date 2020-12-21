package in.kay.temper.Views;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.romainpiel.shimmer.Shimmer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import in.kay.temper.Adapter.MailAdapter;
import in.kay.temper.Api.RetrofitClient;
import in.kay.temper.Models.MailModel;
import in.kay.temper.R;
import in.kay.temper.databinding.ActivityMainBinding;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static in.kay.temper.Helper.App.CHANNEL_1_ID;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    Disposable disposable;
    private NotificationManagerCompat notificationManager;
    MailAdapter mailAdapter;
    String prefix, domain;
    ArrayList<MailModel> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        prefix = generateSessionKey(6);
        domain = "1secmail.com";
        binding.tvMailAddress.setText(prefix+"@"+domain);
        notificationManager = NotificationManagerCompat.from(this);
        binding.rv.setLayoutManager(new LinearLayoutManager(this));
        RefreshMailBox();
        binding.btnCopyMail.setOnClickListener(view1 -> {
            CopyMail();
        });
        binding.btnNewMail.setOnClickListener(view1 -> {
            NewMail();
        });
    }

    private void RefreshMailBox() {
        binding.pb.setVisibility(View.VISIBLE);
        disposable = Observable.interval(2000, 5000,
                TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::SuccessCall, this::onError);
    }

    private void SuccessCall(Long aLong) {
        FetchEmail();
    }

    private void FetchEmail() {
        Call<ArrayList<MailModel>> call = RetrofitClient.getInstance().getApi().getMessages("getMessages", prefix, domain);
        call.enqueue(new Callback<ArrayList<MailModel>>() {
            @Override
            public void onResponse(Call<ArrayList<MailModel>> call, Response<ArrayList<MailModel>> response) {
                if (response.body().size() > list.size() && list.size() > 0) {
                    String subject = response.body().get(0).getSubject();
                    String from = response.body().get(0).getFrom();
                    ShowNotification(subject, from, response.body().get(0).id);
                }
                list = response.body();
                mailAdapter = new MailAdapter(list, MainActivity.this, prefix, domain);
                if (list.size() > 0) {
                    mailAdapter.notifyDataSetChanged();
                    binding.rv.setAdapter(mailAdapter);
                    binding.rv.setVisibility(View.VISIBLE);
                    binding.ivInbox.setVisibility(View.VISIBLE);
                    binding.tv.setVisibility(View.GONE);
                    binding.iv.setVisibility(View.GONE);
                    binding.pb.setVisibility(View.GONE);
                } else if (list.size() == 0) {
                    binding.pb.setVisibility(View.GONE);
                    binding.tv.setText("Mail box is empty");
                }
            }

            @Override
            public void onFailure(Call<ArrayList<MailModel>> call, Throwable t) {

            }
        });
    }

    private void onError(Throwable throwable) {
    }

    public void CopyMail() {
        Toast.makeText(this, "Copied email successfully...", Toast.LENGTH_SHORT).show();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(this.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Email copied...", binding.tvMailAddress.getText().toString());
        clipboard.setPrimaryClip(clip);
    }

    public void NewMail() {
        list.clear();
        binding.tv.setVisibility(View.VISIBLE);
        binding.tv.setText("Fetching Mail");
        binding.ivInbox.setVisibility(View.GONE);
        binding.iv.setVisibility(View.VISIBLE);
        binding.pb.setVisibility(View.VISIBLE);
        binding.rv.setVisibility(View.GONE);
        binding.btnNewMail.setClickable(false);
        Shimmer shimmer = new Shimmer();
        shimmer.setDuration(2000);
        shimmer.start(binding.tvMailAddress);
        Call<List<String>> call = RetrofitClient.getInstance().getApi().getNewMail();
        call.enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                binding.btnNewMail.setClickable(true);
                if (response.code() == 200) {
                    binding.tvMailAddress.setText(response.body().get(0));
                    shimmer.cancel();
                    prefix = SplitEmail(response.body().get(0)).get(0);
                    domain = SplitEmail(response.body().get(0)).get(1);
                    FetchEmail();
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                binding.btnNewMail.setClickable(true);
            }
        });
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

    private void ShowDialog() {
        View view = getLayoutInflater().inflate(R.layout.alert_diag, null);
        final AlertDialog alertbox = new AlertDialog.Builder(this)
                .setView(view)
                .show();
        Button yes, no;
        yes = alertbox.findViewById(R.id.btn_yes);
        no = alertbox.findViewById(R.id.btn_no);
        yes.setOnClickListener(view1 -> CloseApp());
        no.setOnClickListener(view1 -> alertbox.dismiss());
    }

    @Override
    public void onBackPressed() {
        ShowDialog();
    }

    private void CloseApp() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
        int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);
    }

    public void ShowNotification(String msg, String from, Integer id) {
        Intent resultIntent = new Intent(this, ViewMailActivity.class);
        resultIntent.putExtra("prefix", prefix);
        resultIntent.putExtra("domain", domain);
        resultIntent.putExtra("id", id);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        String title = "New mail received from " + from;
        String message = msg;
        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, activityIntent, 0);
        Bitmap picture = BitmapFactory.decodeResource(getResources(), R.drawable.noti_img);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_1_ID)
                .setSmallIcon(R.drawable.ic_icon)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(resultPendingIntent)
                .setLargeIcon(picture)
                .setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(picture)
                        .bigLargeIcon(null))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build();
        notificationManager.notify(1, notification);
    }

    public static String generateSessionKey(int length) {
        String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        int n = alphabet.length();
        String result = new String();
        Random r = new Random();
        for (int i = 0; i < length; i++)
            result = result + alphabet.charAt(r.nextInt(n));
        return result;
    }

}