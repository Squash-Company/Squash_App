package dev.kaua.squash.Activitys;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.os.Bundle;
import android.os.Handler;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import dev.kaua.squash.R;
import dev.kaua.squash.Tools.LoadingDialog;
import dev.kaua.squash.Tools.MyPrefs;

public class PrivacyPolicyUpdateActivity extends AppCompatActivity {
    TextView txt_include_information, txt_please_accept;
    CardView btn_accept;
    boolean accept = false;
    long version;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy_update);
        Ids();

        Bundle bundle = getIntent().getExtras();
        version = bundle.getLong("privacy_policy");

        btn_accept.setOnClickListener(v -> {
            accept = true;
            MyPrefs.setPrivacy_Policy(this, version);
            LoadingDialog loadingDialog = new LoadingDialog(this);
            loadingDialog.startLoading();
            new Handler().postDelayed(() -> {
                loadingDialog.dismissDialog();
                finish();
            }, 1500);
        });

    }

    private void Ids() {
        btn_accept = findViewById(R.id.btn_accept);
        txt_include_information = findViewById(R.id.txt_include_information);
        txt_please_accept = findViewById(R.id.txt_please_accept);
        txt_include_information.setMovementMethod(LinkMovementMethod.getInstance());
        txt_please_accept.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(!accept) finishAffinity();
        else finish();
    }
}