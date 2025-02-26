package dev.kaua.squash.Activities;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;

import de.hdodenhof.circleimageview.CircleImageView;
import dev.kaua.squash.Activities.Chat.ShareContentActivity;
import dev.kaua.squash.Activities.Posts.ComposeActivity;
import dev.kaua.squash.Data.Account.AsyncUser_Follow;
import dev.kaua.squash.Data.Account.DtoAccount;
import dev.kaua.squash.Firebase.myFirebaseHelper;
import dev.kaua.squash.Fragments.FragmentPageAdapter;
import dev.kaua.squash.Fragments.ProfileFragment;
import dev.kaua.squash.R;
import dev.kaua.squash.Security.EncryptHelper;
import dev.kaua.squash.Security.Login;
import dev.kaua.squash.Tools.LoadingDialog;
import dev.kaua.squash.Tools.Methods;
import dev.kaua.squash.Tools.MyPrefs;
import dev.kaua.squash.Tools.SheetHelper;
import dev.kaua.squash.Tools.ShortCutsHelper;
import dev.kaua.squash.Tools.ToastHelper;

/**
 *  Copyright (c) 2021 Kauã Vitório
 *  Official repository https://github.com/Kauavitorio/River_App
 *  Responsible developer: https://github.com/Kauavitorio
 *  @author Kaua Vitorio
 **/

@SuppressWarnings("FieldCanBeLocal")
@SuppressLint({"StaticFieldLeak", "UseCompatLoadingForDrawables"})
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity_LOG";
    private static final int CHAT_POSITION = 0;
    private static final int MAIN_POSITION = 1;
    private static final int SEARCH_POSITION = 2;
    private static final int PROFILE_POSITION = 3;
    public static final int SHARED_ID = 1;
    public static final int SHARED_PLAIN_TEXT = 1;
    public static final int SHARED_IMAGE = 2;
    public static final String SHARED_TAG = "shared";
    public static final String SHARED_TYPE_TAG = "shared_type";
    public static final String SHARED_CONTENT_TAG = "shared_content";
    private static ImageView btn_search_main, btn_home_main, btn_chat_main;
    private RelativeLayout click_home, click_chat;
    private CircleImageView btn_profile_main;
    private RelativeLayout container_btn_profile_main;
    private static ViewPager viewPager;
    private FragmentPageAdapter adapter;
    private static FirebaseAnalytics mFirebaseAnalytics;

    private Bundle bundle;
    private static MainActivity instance;

    private static DtoAccount account = new DtoAccount();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Ids();

        // Create an adapter that
        // knows which fragment should
        // be shown on each page
        adapter = new FragmentPageAdapter(getSupportFragmentManager());

        // Set the adapter onto
        // the view pager
        viewPager.setOffscreenPageLimit(4);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(MAIN_POSITION, true);

        //  Get all SharedPreferences
        bundle = getIntent().getExtras();
        if (account.getAccount_id() > DtoAccount.NORMAL_ACCOUNT) StartNavigation();
        else Login.LogOut(this, Login.LOGOUT_STATUS_WITHOUT_FLAG, Login.NOT_DISABLE_ACCOUNT);

        if(bundle != null){
            if(bundle.getInt(SHARED_TAG) == SHARED_ID){
                Intent i = new Intent(this, ShareContentActivity.class);
                i.putExtra(SHARED_TYPE_TAG, bundle.getInt(SHARED_TYPE_TAG));
                if(bundle.getInt(SHARED_TYPE_TAG) == MainActivity.SHARED_PLAIN_TEXT)
                    i.putExtra(SHARED_CONTENT_TAG, bundle.getString(SHARED_CONTENT_TAG));
                else if(bundle.getInt(SHARED_TYPE_TAG) == MainActivity.SHARED_IMAGE)
                    i.putExtra(SHARED_CONTENT_TAG, (Uri) bundle.get(SHARED_CONTENT_TAG));
                startActivity(i);
            }else if(bundle.getInt(ShortCutsHelper.SHORTCUT_TAG) != ShortCutsHelper.NONE_SHORT){
                final int shortCut = bundle.getInt(ShortCutsHelper.SHORTCUT_TAG);
                LoadingDialog loadingDialog = new LoadingDialog(this);
                loadingDialog.startLoading();
                switch (shortCut){
                    case ShortCutsHelper.NEW_POST: CallComposePost();
                        loadingDialog.dismissDialog();
                    case ShortCutsHelper.SEARCH: {
                        new Handler().postDelayed(() -> {
                            LoadSearchFragment();
                            loadingDialog.dismissDialog();
                        }, 2000);
                    }
                    case ShortCutsHelper.PROFILE:
                        new Handler().postDelayed(() -> {
                            CallProfile();
                            loadingDialog.dismissDialog();
                        }, 500);
                }
            }
            Log.d(TAG, "ShortCut -> " + bundle.getInt(ShortCutsHelper.SHORTCUT_TAG));
        }

        btn_search_main.setOnClickListener(v -> LoadSearchFragment());
        click_home.setOnClickListener(v -> LoadMainFragment());
        click_chat.setOnClickListener(v -> CallChat());

        container_btn_profile_main.setOnClickListener(v -> CallProfile());

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
            @Override
            public void onPageSelected(int position) {
                adapter.notifyDataSetChanged();
                viewPager.setCurrentItem(position, true);
                Check_Fragments(position);
            }
            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        ReviewManager manager = ReviewManagerFactory.create(this);
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // We can get the ReviewInfo object
                ReviewInfo reviewInfo = task.getResult();
                Task<Void> flow = manager.launchReviewFlow(this, reviewInfo);
                flow.addOnCompleteListener(tasks -> {
                    // The flow has finished. The API does not indicate whether the user
                    // reviewed or not, or even whether the review dialog was shown. Thus, no
                    // matter the result, we continue our app flow.
                });
            } else {
                // There was some problem, log or handle the error code.
                ToastHelper.toast(this, task.toString(), ToastHelper.SHORT_DURATION);
                Log.d(TAG, task.toString());
            }
        });

    }

    private void StartNavigation() {
        getUserInformationAndLoadProfile();
        LoadMainFragment();
    }

    public static MainActivity getInstance(){ return instance; }

    Bundle bundle_profile;
    public static final String ID_REQUEST_ACCOUNT_ID = "account_id";
    public static final String ID_REQUEST_CONTROL = "control";
    public static final int ID_REQUEST_CONTROL_BASE = 0;
    public void GetBundleProfile(Bundle bundle){
        bundle_profile = bundle;
    }
    public Bundle SetBundleProfile(){ return bundle_profile; }
    public void ResetBundleProfile() { bundle_profile = null; }

    public void CallProfile(){
        viewPager.setCurrentItem(PROFILE_POSITION, true);
        adapter.notifyDataSetChanged();
        try {
            ProfileFragment.getInstance().GetUserInfo(this);
        }catch (Exception ex){
            Log.d(TAG, "Profile Resume -> " +  ex.getMessage());
        }
    }

    public void CallChat(){
        viewPager.setCurrentItem(CHAT_POSITION, true);
        adapter.notifyDataSetChanged();
    }

    public void CallComposePost(){
        Intent compose = new Intent(this, ComposeActivity.class);
        startActivity(compose);
    }

    public void getUserInformationAndLoadProfile(){
        account = MyPrefs.getUserInformation(this);
        Glide.with(this).load(account.getProfile_image()).diskCacheStrategy(DiskCacheStrategy.ALL)
                .signature(new ObjectKey(account.getProfile_image()))
                .into(btn_profile_main);
    }

    public void LoadMainFragment() {
        viewPager.setCurrentItem(MAIN_POSITION, true);
        adapter.notifyDataSetChanged();
    }

    private void LoadSearchFragment() {
        viewPager.setCurrentItem(SEARCH_POSITION, true);
        adapter.notifyDataSetChanged();
    }

    public void Check_Fragments(int position){
        btn_chat_main.setImageDrawable(getDrawable(R.drawable.ic_chat_no_focus));
        btn_home_main.setImageDrawable(getDrawable(R.drawable.ic_home));
        btn_search_main.setImageDrawable(getDrawable(R.drawable.ic_search));
        btn_profile_main.setBorderWidth(0);
        if(position == CHAT_POSITION) btn_chat_main.setImageDrawable(getDrawable(R.drawable.ic_chat_focus));
        else if(position == MAIN_POSITION) btn_home_main.setImageDrawable(getDrawable(R.drawable.ic_home_select));
        else if(position == SEARCH_POSITION) btn_search_main.setImageDrawable(getDrawable(R.drawable.ic_search_select));
        else if(position == PROFILE_POSITION) btn_profile_main.setBorderWidth(4);
    }

    void Ids() {
        instance = this;
        getWindow().setStatusBarColor(getColor(R.color.BarColor));
        getWindow().setNavigationBarColor(getColor(R.color.BarColor));
        viewPager = findViewById(R.id.viewpager_main);
        btn_profile_main = findViewById(R.id.btn_profile_main);
        click_home = findViewById(R.id.click_home);
        click_chat = findViewById(R.id.click_chat);
        btn_search_main = findViewById(R.id.btn_search_main);
        btn_home_main = findViewById(R.id.btn_home_main);
        btn_chat_main = findViewById(R.id.ic_btn_chat_main);
        container_btn_profile_main = findViewById(R.id.container_btn_profile_main);
        mFirebaseAnalytics = myFirebaseHelper.getFirebaseAnalytics(this);
        btn_home_main.setImageDrawable(getDrawable(R.drawable.ic_home_select));

        getUserInformationAndLoadProfile();
        Login.ReloadUserinfo(this, account.getEmail(), account.getPassword());
        new Handler().postDelayed(() -> NotificationManagerCompat.from(MainActivity.this).cancelAll(), 2000);

        //  Creating analytic for open app event
        Bundle bundle_Analytics = new Bundle();
        bundle_Analytics.putString(FirebaseAnalytics.Param.ITEM_ID, myFirebaseHelper.getFirebaseUser().getUid());
        bundle_Analytics.putString(FirebaseAnalytics.Param.ITEM_NAME, EncryptHelper.decrypt(account.getUsername()));
        bundle_Analytics.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle_Analytics);
    }

    @Nullable
    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        Dialog dialog = super.onCreateDialog(id, args);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
            SheetHelper.setWhiteNavigationBar(this, dialog, R.color.BarColor);

        return dialog;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onResume() {
        super.onResume();
        getUserInformationAndLoadProfile();
        Methods.status_chat(Methods.ONLINE, this);
        Methods.LoadFollowersAndFollowing(this, 1);
        AsyncUser_Follow asyncUser_follow = new AsyncUser_Follow(this);
        asyncUser_follow.execute();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Methods.status_chat(Methods.OFFLINE, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        int currentItem = viewPager.getCurrentItem();
        if (currentItem != MAIN_POSITION){
            if(currentItem == CHAT_POSITION)
                viewPager.setCurrentItem(MAIN_POSITION, true);
            else
                viewPager.setCurrentItem(currentItem - 1, true);
        }
        else
            super.onBackPressed();
    }
}