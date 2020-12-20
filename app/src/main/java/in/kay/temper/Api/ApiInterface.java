package in.kay.temper.Api;

import java.util.ArrayList;
import java.util.List;

import in.kay.temper.Models.MailModel;
import in.kay.temper.Models.SingleMailModel;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiInterface {
    public static String BASE_URL = "https://www.1secmail.com/api/v1/";
    public static String randomMail=BASE_URL+"?action=genRandomMailbox&count=1";

    @GET(randomMail)
    Call<List<String>> getNewMail();

    @GET(BASE_URL)
    Call<ArrayList<MailModel>> getMessages(@Query("action") String action, @Query("login") String login, @Query("domain") String domain);

    @GET(BASE_URL)
    Call<SingleMailModel> readMessage(@Query("action") String action, @Query("login") String login, @Query("domain") String domain, @Query("id") Integer id);

    @GET(BASE_URL)
    Call<ArrayList<MailModel>> downloadAttachment(@Query("action") String action,@Query("login") String login, @Query("domain") String domain,@Query("id") String id,@Query("file") String file);




}
