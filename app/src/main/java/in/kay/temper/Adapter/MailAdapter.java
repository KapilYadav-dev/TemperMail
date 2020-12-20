package in.kay.temper.Adapter;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import in.kay.temper.Models.MailModel;
import in.kay.temper.R;
import in.kay.temper.Views.ViewMailActivity;

public class MailAdapter extends RecyclerView.Adapter<MailAdapter.ViewHolder> {
    List<MailModel> list;
    Context context;
    String prefix,domain;

    public MailAdapter(List<MailModel> list, Context context,String prefix,String domain) {
        this.list = list;
        this.context = context;
        this.prefix=prefix;
        this.domain=domain;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.mail_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        MailModel model = list.get(position);
        holder.tvEmail.setText(model.getFrom());
        holder.tvSubject.setText(model.getSubject());
        holder.tvName.setText(SplitEmail(model.getFrom()).get(0));
        Picasso.get().load("https://ui-avatars.com/api/?background=234&color=fff&size=256&rounded=true&name=" + SplitEmail(model.getFrom()).get(0)).placeholder(R.drawable.demo_image).into(holder.imageView);
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(model.getDate());
            String currentDate = (String) DateFormat.format("dd", date);
            String currentMonth = (String) DateFormat.format("MMM", date);
            holder.tvDate.setText(currentDate + " " + currentMonth);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        holder.itemView.setOnClickListener(view -> {
            Intent intent=new Intent(context, ViewMailActivity.class);
            intent.putExtra("prefix",prefix);
            intent.putExtra("domain",domain);
            intent.putExtra("id",model.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvSubject, tvDate;
        ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvName = itemView.findViewById(R.id.tvName);
            imageView = itemView.findViewById(R.id.circleImageView);
        }
    }

    public List<String> SplitEmail(String string) {
        int index = string.indexOf('@');
        String prefix = string.substring(0, index);
        String domain = string.substring(index + 1);
        List<String> list = new ArrayList<>();
        list.add(0, prefix);
        list.add(1, domain);
        return list;
    }
}
