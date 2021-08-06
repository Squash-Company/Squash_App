package dev.kaua.squash.Adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;
import dev.kaua.squash.Activitys.MainActivity;
import dev.kaua.squash.Activitys.PostDetailsActivity;
import dev.kaua.squash.Activitys.ViewMediaActivity;
import dev.kaua.squash.Data.Account.AccountServices;
import dev.kaua.squash.Data.Account.DtoAccount;
import dev.kaua.squash.Data.Post.AsyncLikes_Posts;
import dev.kaua.squash.Data.Post.DtoPost;
import dev.kaua.squash.Data.Post.PostServices;
import dev.kaua.squash.Fragments.MainFragment;
import dev.kaua.squash.Fragments.ProfileFragment;
import dev.kaua.squash.LocalDataBase.DaoPosts;
import dev.kaua.squash.R;
import dev.kaua.squash.Security.EncryptHelper;
import dev.kaua.squash.Tools.LoadingDialog;
import dev.kaua.squash.Tools.Methods;
import dev.kaua.squash.Tools.MyPrefs;
import dev.kaua.squash.Tools.PatternEditableBuilder;
import dev.kaua.squash.Tools.ToastHelper;
import dev.kaua.squash.Tools.Warnings;
import me.saket.bettermovementmethod.BetterLinkMovementMethod;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

@SuppressLint({"UseCompatLoadingForDrawables", "StaticFieldLeak"})
public class Posts_Adapters extends RecyclerView.Adapter<Posts_Adapters.MyHolderPosts> {
    ArrayList<DtoPost> list;
    static Context mContext;
    static DaoPosts daoPosts;
    private static BottomSheetDialog bottomSheetDialog;
    FirebaseStorage firebaseStorage;

    final Retrofit retrofit = Methods.GetRetrofitBuilder();

    public Posts_Adapters(ArrayList<DtoPost> ArrayList, Context mContext) {
        this.list = ArrayList;
        Posts_Adapters.mContext = mContext;
        daoPosts = new DaoPosts(mContext);
    }

    @NonNull
    @Override
    public MyHolderPosts onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View listItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_posts, parent, false);
        return new MyHolderPosts(listItem);
    }

    @SuppressWarnings("RegExpRedundantEscape")
    @SuppressLint({"SetTextI18n", "UseCompatLoadingForDrawables"})
    @Override
    public void onBindViewHolder(@NonNull MyHolderPosts holder, int position) {
        if(Integer.parseInt(Objects.requireNonNull(list.get(position).getVerification_level())) != 0){
            holder.ic_account_badge.setVisibility(View.VISIBLE);
            if (Integer.parseInt(Objects.requireNonNull(list.get(position).getVerification_level())) == 1)
                holder.ic_account_badge.setImageDrawable(mContext.getDrawable(R.drawable.ic_verified_account));
            else
                holder.ic_account_badge.setImageDrawable(mContext.getDrawable(R.drawable.ic_verified_employee_account));

        }else holder.ic_account_badge.setVisibility(View.GONE);
        holder.img_secondImage_post.setVisibility(View.GONE);
        holder.container_third_img.setVisibility(View.GONE);
        Glide.with(mContext).load(list.get(position).getProfile_image()).diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .into(holder.icon_user_profile_post);
        holder.txt_name_user_post.setText(list.get(position).getName_user());
        holder.txt_username_post.setText( "| @" + list.get(position).getUsername());
        holder.txt_post_content.setText(list.get(position).getPost_content());
        new PatternEditableBuilder().
                addPattern(Pattern.compile("\\@(\\w+)"), mContext.getColor(R.color.base_color),
                        text -> {
                            DtoAccount account = new DtoAccount();
                            account.setUsername(text.replace("@", ""));
                            AccountServices services = retrofit.create(AccountServices.class);
                            Call<DtoPost> call = services.search_with_username(account);
                            LoadingDialog loadingDialog = new LoadingDialog(((Activity)mContext));
                            loadingDialog.startLoading();
                            call.enqueue(new Callback<DtoPost>() {
                                @Override
                                public void onResponse(@NotNull Call<DtoPost> call, @NotNull Response<DtoPost> response) {
                                    loadingDialog.dismissDialog();
                                    if(response.code() == 200){
                                        if(response.body() != null){
                                            Bundle bundle = new Bundle();
                                            bundle.putString("account_id", response.body().getAccount_id());
                                            bundle.putInt("control", 0);
                                            MainActivity.getInstance().GetBundleProfile(bundle);
                                            MainActivity.getInstance().CallProfile();
                                            ProfileFragment.getInstance().LoadAnotherUser();
                                        }
                                    }else ToastHelper.toast(((Activity)mContext), mContext.getString(R.string.user_not_found), 0);
                                }
                                @Override
                                public void onFailure(@NotNull Call<DtoPost> call, @NotNull Throwable t) {
                                    loadingDialog.dismissDialog();
                                    Warnings.showWeHaveAProblem(mContext);
                                }
                            });
                        }).into(holder.txt_post_content);

        //  Apply all url on Texts Views
        Linkify.addLinks(holder.txt_post_content, Linkify.ALL);

        //  URL CLICK'S listener
        holder.txt_post_content.setMovementMethod(BetterLinkMovementMethod.newInstance().setOnLinkClickListener((textView, url) -> {
            if (Patterns.WEB_URL.matcher(url).matches()) {
                //An web url is detected
                Methods.browseTo(mContext, url);
                return true;
            }

            return false;
        }));

        Check_Like(holder, position);

        holder.txt_likes_post.setText(Methods.NumberTrick(Long.parseLong(list.get(position).getPost_likes())));
        holder.txt_date_time_post.setText(" • " + list.get(position).getPost_time());
        holder.txt_comments_post.setText(Methods.NumberTrick(Long.parseLong(list.get(position).getPost_comments_amount())));

        DtoPost img_list = daoPosts.get_post_img(Long.parseLong(list.get(position).getPost_id()));
        if(img_list.getPost_images() != null && img_list.getPost_images().size() > 0 && !img_list.getPost_images().get(0).equals("NaN")){
            int ImagesAmount = img_list.getPost_images().size();
            if(ImagesAmount < 2){
                for (int i = 0; i < 1; i++){
                    RequestOptions myOptions = new RequestOptions()
                            .fitCenter() // or centerCrop
                            .override(500, 500);
                    Glide.with(mContext)
                            .asBitmap()
                            .apply(myOptions)
                            .load(EncryptHelper.decrypt(img_list.getPost_images().get(i)))
                            .into(holder.img_firstImage_post);

                    int finalI = i;
                    holder.img_firstImage_post.setOnClickListener(v -> {
                        Intent intent = new Intent(mContext, ViewMediaActivity.class);
                        intent.putExtra("image_url", EncryptHelper.decrypt(img_list.getPost_images().get(finalI)));
                        intent.putExtra("receive_time", "post");
                        String id = list.get(position).getUsername() + "_" + list.get(position).getPost_id();
                        if(id.length() < 11) id += "posts_media";
                        intent.putExtra("chat_id", id);
                        ActivityOptionsCompat activityOptionsCompat = ActivityOptionsCompat.makeCustomAnimation(mContext, R.anim.move_to_left_go, R.anim.move_to_right_go);
                        ActivityCompat.startActivity(mContext, intent, activityOptionsCompat.toBundle());
                    });
                }
            }else{
                RequestOptions myOptions = new RequestOptions()
                        .fitCenter() // or centerCrop
                        .override(100, 100);
                Glide.with(mContext)
                        .asBitmap()
                        .apply(myOptions)
                        .load(EncryptHelper.decrypt(EncryptHelper.decrypt(EncryptHelper.decrypt(img_list.getPost_images().get(0)))))
                        .into(holder.img_firstImage_post);
                Glide.with(mContext)
                        .asBitmap()
                        .apply(myOptions)
                        .load(EncryptHelper.decrypt(EncryptHelper.decrypt(img_list.getPost_images().get(1))))
                        .into(holder.img_secondImage_post);
                holder.img_secondImage_post.setVisibility(View.VISIBLE);
                int width = holder.img_firstImage_post.getWidth();
                holder.img_firstImage_post.getLayoutParams().width = width - 50;
                holder.img_firstImage_post.requestLayout();
                if (ImagesAmount > 2) {
                    holder.container_third_img.setVisibility(View.VISIBLE);
                    Picasso.get().load(EncryptHelper.decrypt(EncryptHelper.decrypt(img_list.getPost_images().get(2)))).into(holder.img_thirdImage_post);
                    if (ImagesAmount == 3) holder.container_blur_post.setVisibility(View.GONE);
                    else {
                        holder.txt_images_amount_post.setText("+" + (ImagesAmount - 2));
                    }
                }
            }
        }else holder.img_firstImage_post.setVisibility(View.GONE);

        holder.icon_user_profile_post.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("account_id", list.get(position).getAccount_id());
            bundle.putInt("control", 0);
            MainActivity.getInstance().GetBundleProfile(bundle);
            MainActivity.getInstance().CallProfile();
            ProfileFragment.getInstance().LoadAnotherUser();
        });

        holder.container_post_adapter.setOnClickListener(v -> {
            Intent i = new Intent(mContext, PostDetailsActivity.class);
            i.putExtra("post_id", Long.parseLong(list.get(position).getPost_id()));
            i.putExtra("comment", 0);
            ActivityOptionsCompat activityOptionsCompat = ActivityOptionsCompat.makeCustomAnimation(mContext, R.anim.move_to_left_go, R.anim.move_to_right_go);
            ActivityCompat.startActivity(mContext, i, activityOptionsCompat.toBundle());
        });

        holder.btn_comment_post.setOnClickListener(v -> {
            Intent i = new Intent(mContext, PostDetailsActivity.class);
            i.putExtra("post_id", Long.parseLong(list.get(position).getPost_id()));
            i.putExtra("comment", 1);
            mContext.startActivity(i);
        });

        holder.btn_share_post.setOnClickListener(v -> {
            Intent myIntent = new Intent(Intent.ACTION_SEND);
            myIntent.setType("text/plain");
            String body = Methods.BASE_URL_HTTPS + "share/" + list.get(position).getUsername() + "/post/" +  list.get(position).getPost_id()
                    + "?s=" + Methods.RandomCharactersWithoutSpecials(3);
            myIntent.putExtra(Intent.EXTRA_TEXT,body);
            mContext.startActivity(Intent.createChooser(myIntent, "Share Using"));
        });

        EnableActions(holder, position);

        holder.btn_like_post.setOnClickListener(v -> Like_Un_Like_A_Post(holder, position, list.get(position).getPost_id()));

        if(list.get(position).isSuggestion()) {
            holder.suggestion_container.setVisibility(View.VISIBLE);
            if(list.get(position).getAccount_id().equals("5")) holder.txt_suggestion.setText(mContext.getString(R.string.developer_post));
        }
        else holder.suggestion_container.setVisibility(View.GONE);
    }

    private void EnableActions(MyHolderPosts holder, int position) {
        DtoAccount user = MyPrefs.getUserInformation(mContext);
        if(Long.parseLong(list.get(position).getAccount_id()) == Long.parseLong(user.getAccount_id() + "")){
            holder.btn_actions.setVisibility(View.VISIBLE);
            holder.btn_actions.setOnClickListener(v -> {
                DtoPost dtoPost = new DtoPost();
                dtoPost.setAccount_id(EncryptHelper.encrypt(user.getAccount_id() + ""));
                dtoPost.setPost_id(EncryptHelper.encrypt(list.get(position).getPost_id()));
                bottomSheetDialog = new BottomSheetDialog(mContext, R.style.BottomSheetTheme);
                bottomSheetDialog.setCancelable(true);
                //  Creating View for SheetMenu
                View sheetView = LayoutInflater.from(mContext).inflate(R.layout.adapter_sheet_menu_post_action,
                        ((Activity)mContext).findViewById(R.id.sheet_menu_post_action));

                sheetView.findViewById(R.id.btn_delete_post).setOnClickListener(v1 -> {
                    AlertDialog.Builder alert = new AlertDialog.Builder(mContext, R.style.MyAlertDialogStyle)
                            .setTitle(mContext.getString(R.string.delete_post))
                            .setMessage(mContext.getString(R.string.delete_post_message))
                            .setPositiveButton(mContext.getString(R.string.yes), (dialog, which) -> {
                                dialog.dismiss();
                                PostServices services = retrofit.create(PostServices.class);
                                Call<DtoPost> call = services.delete_post(dtoPost);

                                LoadingDialog loadingDialog = new LoadingDialog((Activity) mContext);
                                loadingDialog.startLoading();
                                call.enqueue(new Callback<DtoPost>() {
                                    @Override
                                    public void onResponse(@NotNull Call<DtoPost> call, @NotNull Response<DtoPost> response) {
                                        loadingDialog.dismissDialog();
                                        if(response.code() == 200){
                                            DtoPost img_list = daoPosts.get_post_img(Long.parseLong(list.get(position).getPost_id()));
                                            if(img_list.getPost_images() != null && img_list.getPost_images().size() > 0){
                                                for (int i = 0; i < img_list.getPost_images().size(); i++){
                                                    if(img_list.getPost_images().get(i) != null){
                                                        firebaseStorage = FirebaseStorage.getInstance();
                                                        StorageReference photoRef = firebaseStorage.getReferenceFromUrl(Objects.requireNonNull(EncryptHelper.decrypt(img_list.getPost_images().get(i))));
                                                        photoRef.delete().addOnSuccessListener(aVoid -> {
                                                            // File deleted successfully
                                                            Log.d("POSTS_ADAPTER", "onSuccess: deleted file");
                                                        }).addOnFailureListener(exception -> {
                                                            // Uh-oh, an error occurred!
                                                            Log.d("POSTS_ADAPTER", "onFailure: did not delete file");
                                                        });
                                                    }
                                                }
                                            }
                                            list.remove(position);
                                            notifyDataSetChanged();
                                            MainFragment.RefreshRecycler();
                                        }else
                                            Warnings.showWeHaveAProblem(mContext);
                                    }

                                    @Override
                                    public void onFailure(@NotNull Call<DtoPost> call, @NotNull Throwable t) {
                                        loadingDialog.dismissDialog();
                                        Warnings.showWeHaveAProblem(mContext);
                                    }
                                });
                            })
                            .setNeutralButton(mContext.getString(R.string.no), (dialogInterface, i) -> dialogInterface.dismiss());
                    Dialog mDialog = alert.create();
                    mDialog.getWindow().getAttributes().windowAnimations = R.style.MyAlertDialogStyle;
                    mDialog.show();
                    bottomSheetDialog.dismiss();
                });

                sheetView.findViewById(R.id.btn_cancel_actions).setOnClickListener(v1 -> bottomSheetDialog.dismiss());

                bottomSheetDialog.setContentView(sheetView);
                bottomSheetDialog.show();
            });
        }else holder.btn_actions.setVisibility(View.GONE);
    }

    private void Check_Like(@NotNull MyHolderPosts holder, int position) {
        DtoAccount user = MyPrefs.getUserInformation(mContext);
        boolean result_like = daoPosts.get_A_Like(Long.parseLong(list.get(position).getPost_id()), Long.parseLong(user.getAccount_id() + ""));
        if(result_like) holder.img_heart_like.setImageDrawable(mContext.getDrawable(R.drawable.red_heart));
        else holder.img_heart_like.setImageDrawable(mContext.getDrawable(R.drawable.ic_heart));
    }

    private void Like_Un_Like_A_Post(@NotNull MyHolderPosts holder, long position, String post_id) {
        //  Get User info
        DtoAccount user = MyPrefs.getUserInformation(mContext);

        boolean result_like = daoPosts.get_A_Like(Long.parseLong(post_id), Long.parseLong(user.getAccount_id() + ""));
        if(result_like) {
            holder.img_heart_like.setImageDrawable(mContext.getDrawable(R.drawable.ic_heart));
            long like_now = Long.parseLong(list.get((int) position).getPost_likes());
            like_now = like_now - 1;
            holder.txt_likes_post.setText(Methods.NumberTrick(like_now));
        }else{
            holder.img_heart_like.setImageDrawable(mContext.getDrawable(R.drawable.red_heart));
            long like_now = Long.parseLong(list.get((int) position).getPost_likes());
            like_now = like_now + 1;
            holder.txt_likes_post.setText(Methods.NumberTrick(like_now));
        }

        //  Do Like or Un Like
        DtoPost dtoPost = new DtoPost();
        dtoPost.setPost_id(EncryptHelper.encrypt(post_id));
        dtoPost.setAccount_id_cry(EncryptHelper.encrypt(user.getAccount_id() + ""));
        PostServices services = retrofit.create(PostServices.class);
        Call<DtoPost> call = services.like_Un_Like_A_Post(dtoPost);
        call.enqueue(new Callback<DtoPost>() {
            @Override
            public void onResponse(@NotNull Call<DtoPost> call, @NotNull Response<DtoPost> response) {
                AsyncLikes_Posts async = new AsyncLikes_Posts((Activity) mContext , Long.parseLong(user.getAccount_id() + ""));
                //noinspection unchecked
                async.execute();
            }
            @Override
            public void onFailure(@NotNull Call<DtoPost> call, @NotNull Throwable t) { Warnings.showWeHaveAProblem(mContext); }
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class MyHolderPosts extends RecyclerView.ViewHolder{
        CircleImageView icon_user_profile_post;
        TextView txt_name_user_post, txt_username_post, txt_post_content, txt_images_amount_post, txt_date_time_post;
        TextView txt_likes_post, txt_comments_post, txt_suggestion;
        ImageView img_firstImage_post, img_secondImage_post, img_thirdImage_post, ic_account_badge, img_heart_like, btn_actions;
        RelativeLayout container_third_img, container_post_adapter;
        ConstraintLayout container_blur_post;
        LinearLayout btn_like_post, btn_share_post, btn_comment_post, suggestion_container;

        public MyHolderPosts(@NonNull View itemView) {
            super(itemView);
            txt_date_time_post = itemView.findViewById(R.id.txt_date_time_post);
            suggestion_container = itemView.findViewById(R.id.suggestion_container);
            txt_suggestion = itemView.findViewById(R.id.txt_suggestion);
            container_post_adapter = itemView.findViewById(R.id.container_post_adapter);
            btn_comment_post = itemView.findViewById(R.id.btn_comment_post);
            icon_user_profile_post = itemView.findViewById(R.id.icon_user_profile_post);
            txt_name_user_post = itemView.findViewById(R.id.txt_name_user_post);
            btn_actions = itemView.findViewById(R.id.btn_actions);
            img_heart_like = itemView.findViewById(R.id.img_heart_like_post);
            txt_username_post = itemView.findViewById(R.id.txt_username_post);
            txt_post_content = itemView.findViewById(R.id.txt_post_content);
            img_firstImage_post = itemView.findViewById(R.id.img_firstImage_post);
            img_secondImage_post = itemView.findViewById(R.id.img_secondImage_post);
            container_third_img = itemView.findViewById(R.id.container_third_img_posts);
            btn_share_post = itemView.findViewById(R.id.btn_share_post);
            btn_like_post = itemView.findViewById(R.id.btn_like_post);
            img_thirdImage_post = itemView.findViewById(R.id.img_thirdImage_post);
            ic_account_badge = itemView.findViewById(R.id.ic_account_badge);
            container_blur_post = itemView.findViewById(R.id.container_blur_post);
            txt_images_amount_post = itemView.findViewById(R.id.txt_images_amount_post);
            txt_likes_post = itemView.findViewById(R.id.txt_likes_post);
            txt_comments_post = itemView.findViewById(R.id.txt_comments_post);
        }
    }
}
