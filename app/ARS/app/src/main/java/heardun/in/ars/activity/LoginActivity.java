package heardun.in.ars.activity;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import heardun.in.ars.ArsRequest;
import heardun.in.ars.BuildConfig;
import heardun.in.ars.R;
import heardun.in.ars.Usersession;


import heardun.in.ars.config.Config;
import heardun.in.ars.config.Constants;
import heardun.in.ars.config.Serverconfig;
import heardun.in.ars.utils.Utils;


/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends Activity implements OnClickListener {

    public String TAG = LoginActivity.this.getClass().getName();
    StringRequest stringRequest;
    String nameCheck, passowrdCheck;
    Usersession usersession;
    Utils utils;

    @BindView(R.id.username)
    EditText username;
    @BindView(R.id.password)
    EditText password;
    @BindView(R.id.login)
    Button login;
    @BindView(R.id.progress_bar)
    ProgressBar progressbar;
    @BindView(R.id.txt_version)
    TextView txt_version;


    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_login);

        ButterKnife.bind(this);

        LocalBroadcastManager.getInstance(this).registerReceiver(tokenReceiver,
                new IntentFilter("tokenReceiver"));


        usersession = new Usersession(getApplication());
        utils = new Utils(getApplication());

        String toekn = FirebaseInstanceId.getInstance().getToken();
        if (toekn == null) {
            login.setEnabled(false);
        }

        utils.showLog(TAG, "firebase id is " + toekn, Config.MENUACTIVITY);


        txt_version.setText("Version " + BuildConfig.VERSION_NAME);
        Log.i(TAG, "login activity");

        login.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.login:

                nameCheck = username.getText().toString();
                passowrdCheck = password.getText().toString();

                if (nameCheck.length() > 0 && passowrdCheck.length() > 0) {
                    Log.i(TAG, "login button");
                    loginRequest();
                    break;

                }
        }


    }

    public void loginRequest() {
        utils.showProgressBar(progressbar);
        View view = this.getCurrentFocus();
        utils.hideKeyboard(view);
        stringRequest = new StringRequest(Request.Method.POST, Serverconfig.SERVER_ENDPOINT + Serverconfig.login,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.i(TAG, "login button response" + response);
                        try {
                            Log.i(TAG, "resonse is" + response);
                            JSONObject jobj = new JSONObject(response);

                            Log.i(TAG, "jobj.optString(\"sessionkey\")" + jobj.optString("sessionkey") +
                                    "jobj.optString(\"username\")" + jobj.optString("username"));

                            usersession.setusersession(jobj.optString("sessionkey"));
                            usersession.setLoginname(jobj.optString("username"));

                            Log.i(TAG,
                                    "session name is" + usersession.getloginname() +
                                            "session key2 is" + usersession.getusersession());
                            utils.showProgressBar(progressbar);

                        } catch (JSONException e) {
                            e.printStackTrace();

                        }
                        if (!usersession.getusersession().isEmpty()) {
                            start_Homescreen();

                        } else {
                            errorAlert("Login failed");
                            //artActivity(new Intent(getApplication(), LoginActivity.class));
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(TAG, "error response" + error);
                //String error_msg = "Login Faield";
                progressbar.setVisibility(View.GONE);
                if (error.networkResponse != null && error.networkResponse.data != null) {
                    String errorresonse = new String(error.networkResponse.data);
                    //startActivity(new Intent(LoginActivity.this, ResponseError.class).putExtra("error", errorresonse));

                    try {
                        JSONObject jobj = new JSONObject(errorresonse);
                        String error_msg = jobj.getString("message");
                        utils.showProgressBar(progressbar);
                        errorAlert(error_msg);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
                if (error instanceof NetworkError) {
                    errorAlert("Check the network connection");
                }
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("login_id", nameCheck);
                params.put("password", passowrdCheck);
                params.put("app_login", "true");
                Log.i(TAG, "login button nameCheck" + nameCheck + "\npassowrdCheck\t" + passowrdCheck);

                return params;
            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                Constants.REQUEST_TIMEOUT,
                Constants.REQUEST_RETRY_TIMES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        stringRequest.setTag(TAG);
        ArsRequest.getInstance(getApplication()).addToRequestQueue(stringRequest);

    }

    public void start_Homescreen() {
        startActivity(new Intent(getApplication(), MenuAtivity.class));
        progressbar.setVisibility(View.GONE);
        finish();
    }

    private void errorAlert(String msg) {
        Log.i(TAG, "alert dialog");
        progressbar.setVisibility(View.GONE);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage(msg)
                .setTitle(R.string.ars_error);
        alertDialog.setPositiveButton(R.string.try_again, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

            }
        });

        AlertDialog dialog = alertDialog.create();
        dialog.show();
    }

    public boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {

                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST,
                        new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {

                                dialogInterface.dismiss();
                            }
                        }).show();

            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
    }


    BroadcastReceiver tokenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String token = intent.getStringExtra("token");
            utils.showLog(TAG, "token broad cast receiver" + token, Config.UTILS);
            if (token != null) {
                login.setEnabled(true);

                if (utils.usersession.get_preFirebaseId().isEmpty()) {
                    utils.usersession.set_preFirebaseId(token);
                } else {
                    utils.usersession.set_preFirebaseId(utils.usersession.get_newFirebaseId());
                }
                utils.usersession.set_newFirebaseId(token);


                //send token to your server or what you want to do
                utils.update_FCMToken(token);
            } else {
                login.setEnabled(false);
            }

        }
    };
}


