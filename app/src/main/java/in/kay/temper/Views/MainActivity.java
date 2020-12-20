package in.kay.temper.Views;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.romainpiel.shimmer.Shimmer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import in.kay.temper.Api.RetrofitClient;
import in.kay.temper.Adapter.MailAdapter;
import in.kay.temper.Models.MailModel;
import in.kay.temper.databinding.ActivityMainBinding;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    Disposable disposable;
    MailAdapter mailAdapter;
    String prefix = "demo", domain = "1secmail.com";
    ArrayList<MailModel> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
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
                list = response.body();
                mailAdapter = new MailAdapter(list, MainActivity.this,prefix,domain);
                if (list.size() > 0) {
                    mailAdapter.notifyDataSetChanged();
                    binding.rv.setAdapter(mailAdapter);
                    binding.rv.setVisibility(View.VISIBLE);
                    binding.ivInbox.setVisibility(View.VISIBLE);
                    binding.tv.setVisibility(View.GONE);
                    binding.iv.setVisibility(View.GONE);
                    binding.pb.setVisibility(View.GONE);
                }
                else if (list.size()==0)
                {
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

}