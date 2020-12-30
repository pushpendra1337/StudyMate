package com.netbucket.studymate.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.netbucket.studymate.R;
import com.netbucket.studymate.activities.ViewUserProfileActivity;
import com.netbucket.studymate.model.Student;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import es.dmoral.toasty.Toasty;

public class StudentPendingRequestsAdapter extends FirestoreRecyclerAdapter<Student, StudentPendingRequestsAdapter.PendingRequestsHolder> {
    private final FirebaseFirestore mStore = FirebaseFirestore.getInstance();
    public StudentPendingRequestsAdapter(@NonNull FirestoreRecyclerOptions<Student> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull PendingRequestsHolder holder, int position, @NonNull Student model) {
        holder.textViewName.setText(model.getFullName());
        holder.textViewCourse.setText(model.getCourse());
        holder.textViewId.setText(model.getId());
        holder.textViewSemOrYear.setText(model.getSemOrYear());
        Glide.with(holder.imageViewProfileImage.getContext())
                .load(model.getProfileImageUri())
                .into(holder.imageViewProfileImage);
        holder.constraintLayoutItem.setOnClickListener(v -> {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(holder.constraintLayoutItem.getContext());
            View bottomSheetView = LayoutInflater.from(holder.constraintLayoutItem.getContext()).inflate(R.layout.bottom_sheet_pending_requests_options, holder.itemView.findViewById(R.id.linearLayout_bottom_sheet_container));

            bottomSheetView.findViewById(R.id.linearLayout_view_profile).setOnClickListener(v1 -> {
                Intent intent1 = new Intent(v1.getContext(), ViewUserProfileActivity.class);
                intent1.putExtra("fullName", model.getFullName());
                intent1.putExtra("email", model.getEmail());
                intent1.putExtra("phoneNumber", model.getPhoneNumber());
                intent1.putExtra("username", model.getUsername());
                intent1.putExtra("about", model.getAbout());
                intent1.putExtra("birthday", model.getBirthday());
                intent1.putExtra("gender", model.getGender());
                intent1.putExtra("institute", model.getInstitute());
                intent1.putExtra("role", model.getRole());
                intent1.putExtra("course", model.getCourse());
                intent1.putExtra("id", model.getId());
                intent1.putExtra("semOrYear", model.getSemOrYear());
                intent1.putExtra("profileImageUri", model.getProfileImageUri());
                intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                bottomSheetDialog.dismiss();

                v1.getContext().startActivity(intent1);
            });

            bottomSheetView.findViewById(R.id.linearLayout_accept_request).setOnClickListener(v12 -> {
                Map<String, Object> userStatus = new HashMap<>();
                userStatus.put("userStatus", "allowed");

                bottomSheetDialog.dismiss();

                mStore.collection("users")
                        .document(getSnapshots().getSnapshot(holder.getAdapterPosition()).getId())
                        .update(userStatus)
                        .addOnSuccessListener(aVoid -> {

                            getSnapshots()
                                    .getSnapshot(holder.getAdapterPosition())
                                    .getReference()
                                    .update(userStatus);

                            Toasty.success(v.getContext(), model.getFullName() + " added!", Toast.LENGTH_SHORT, true).show();
                        })
                        .addOnFailureListener(e -> Toasty.error(v.getContext(), "Failed to add " + model.getFullName() + ", Try again!", Toast.LENGTH_SHORT, true).show());
            });

            bottomSheetView.findViewById(R.id.linearLayout_reject_request).setOnClickListener(v13 -> {
                Map<String, Object> userStatus = new HashMap<>();
                userStatus.put("userStatus", "disallowed");

                bottomSheetDialog.dismiss();

                mStore.collection("users")
                        .document(getSnapshots().getSnapshot(holder.getAdapterPosition()).getId())
                        .update(userStatus)
                        .addOnSuccessListener(aVoid -> {

                            getSnapshots()
                                    .getSnapshot(holder.getAdapterPosition())
                                    .getReference()
                                    .update(userStatus);

                            Toasty.success(v.getContext(), model.getFullName() + " blocked!", Toast.LENGTH_SHORT, true).show();
                        })
                        .addOnFailureListener(e -> Toasty.error(v.getContext(), "Failed to block " + model.getFullName() + " , Try again!", Toast.LENGTH_SHORT, true).show());
            });

            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.show();
        });
    }

    @NonNull
    @Override
    public PendingRequestsHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
        return new PendingRequestsHolder(view);
    }

    static class PendingRequestsHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        TextView textViewId;
        TextView textViewCourse;
        TextView textViewSemOrYear;
        CircleImageView imageViewProfileImage;
        ConstraintLayout constraintLayoutItem;

        public PendingRequestsHolder(View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textView_full_name);
            textViewId = itemView.findViewById(R.id.textView_id_or_roll_no);
            textViewCourse = itemView.findViewById(R.id.textView_course);
            textViewSemOrYear = itemView.findViewById(R.id.textView_sem_or_year);
            imageViewProfileImage = itemView.findViewById(R.id.imageView_profile_image);
            constraintLayoutItem = itemView.findViewById(R.id.constraintLayout_item);
        }
    }
}
