package com.netbucket.studymate.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.netbucket.studymate.R;
import com.netbucket.studymate.utils.NetworkInfoUtility;
import com.netbucket.studymate.utils.SessionManager;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;
import de.mateware.snacky.Snacky;

public class AdminRegistrationStepThreeActivity extends AppCompatActivity {

    final String mTermOrYear = "NA";
    CircleImageView mProfileImage;
    TextInputLayout mUsernameLayout;
    TextInputLayout mAboutLayout;
    TextInputEditText mUsernameField;
    TextInputEditText mAboutField;
    ImageView mBackButton;
    Button mRegisterButton;
    TextView mLoginActivityLink;
    MaterialDialog mProgressDialog;
    String mUserPath;
    String mUserStatus;
    String mProfileEditAccess = "granted";
    FloatingActionButton mCameraFab;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mStore;
    private StorageReference mStorageRef;
    private String mFullName;
    private String mEmail;
    private String mRole;
    private String mGender;
    private String mPassword;
    private String mInstitute;
    private String mCourse;
    private String mId;
    private String mBirthday;
    private String mPhoneNumber;
    private String mUsername;
    private String mAbout;
    private Uri mProfileImageUri = null;
    private Uri mImageUri = null;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                mImageUri = result != null ? result.getUri() : null;

                if (!(mImageUri == null)) {
                    Glide
                            .with(AdminRegistrationStepThreeActivity.this)
                            .load(mImageUri)
                            .centerCrop()
                            .placeholder(R.drawable.avatar)
                            .into(mProfileImage);
                } else {
                    Glide
                            .with(AdminRegistrationStepThreeActivity.this)
                            .load(R.drawable.avatar)
                            .centerCrop()
                            .placeholder(R.drawable.avatar)
                            .into(mProfileImage);
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_registration_step_three);

        Intent intent = getIntent();
        mFullName = intent.getStringExtra("fullName");
        mEmail = intent.getStringExtra("email");
        mRole = intent.getStringExtra("role");
        mGender = intent.getStringExtra("gender");
        mPassword = intent.getStringExtra("password");
        mInstitute = intent.getStringExtra("institute");
        mCourse = intent.getStringExtra("course");
        mId = intent.getStringExtra("id");
        mBirthday = intent.getStringExtra("birthday");
        mPhoneNumber = intent.getStringExtra("phoneNumber");

        mProfileImage = findViewById(R.id.imageView_profile_image);
        mUsernameLayout = findViewById(R.id.textInputLayout_username);
        mAboutLayout = findViewById(R.id.textInputLayout_about);
        mUsernameField = findViewById(R.id.textInputEditText_username);
        mAboutField = findViewById(R.id.textInputEditText_about);
        mBackButton = findViewById(R.id.imageView_back);
        mRegisterButton = findViewById(R.id.button_register);
        mLoginActivityLink = findViewById(R.id.textView_login);
        mCameraFab = findViewById(R.id.floatingActionButton_camera);

        mAboutField.setText("Hey There! I am using StudyMate.");
        mUserStatus = "allowed";

        mUsernameField.addTextChangedListener(new ValidationWatcher(mUsernameField));
        mAboutField.addTextChangedListener(new ValidationWatcher(mAboutField));

        mStore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        FirebaseStorage mStorage = FirebaseStorage.getInstance();
        mStorageRef = mStorage.getReference();


        mBackButton.setOnClickListener(v -> onBackPressed());


        mCameraFab.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {

                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(AdminRegistrationStepThreeActivity.this);
                View bottomSheetView = LayoutInflater.from(AdminRegistrationStepThreeActivity.this).inflate(R.layout.bottom_sheet_image_picker, findViewById(R.id.linearLayout_bottom_sheet_container));

                if (mImageUri == null) {
                    bottomSheetView.findViewById(R.id.linearLayout_remove_photo).setVisibility(View.GONE);
                } else {
                    bottomSheetView.findViewById(R.id.linearLayout_remove_photo).setVisibility(View.VISIBLE);
                }

                bottomSheetView.findViewById(R.id.linearLayout_capture_or_select_from_gallery).setOnClickListener(v1 -> Dexter.withContext(AdminRegistrationStepThreeActivity.this)
                        .withPermissions(
                                Manifest.permission.CAMERA,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                        .withListener(new MultiplePermissionsListener() {
                            @Override
                            public void onPermissionsChecked(MultiplePermissionsReport report) {

                                CropImage.activity()
                                        .setGuidelines(CropImageView.Guidelines.ON)
                                        .setAspectRatio(1, 1)
                                        .setRequestedSize(512, 512)
                                        .setCropShape(CropImageView.CropShape.RECTANGLE)
                                        .setBackgroundColor(R.color.background_color)
                                        .setCropMenuCropButtonIcon(R.drawable.ic_baseline_crop_30)
                                        .setFixAspectRatio(true)
                                        .setActivityTitle("Select and Crop")
                                        .setCropMenuCropButtonTitle("Crop")
                                        .setOutputCompressQuality(80)
                                        .setOutputCompressFormat(Bitmap.CompressFormat.JPEG)
                                        .setMinCropResultSize(192, 192)
                                        .start(AdminRegistrationStepThreeActivity.this);
                                bottomSheetDialog.dismiss();

                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                                token.continuePermissionRequest();
                            }
                        }).check());


                bottomSheetView.findViewById(R.id.linearLayout_remove_photo).setOnClickListener(new View.OnClickListener() {
                    @SuppressLint("UseCompatLoadingForDrawables")
                    @Override
                    public void onClick(View v) {
                        if (mImageUri != null) {
                            mImageUri = null;
                            mProfileImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_person_24, getTheme()));
                        }
                        bottomSheetDialog.dismiss();
                    }
                });

                bottomSheetDialog.setContentView(bottomSheetView);
                bottomSheetDialog.show();

            }
        });

        mRegisterButton.setOnClickListener(v -> {
            if ((validUsername() != null) && (validAbout() != null)) {
                registerAdmin();
            }
        });

        mLoginActivityLink.setOnClickListener(v -> {
            Intent intent1 = new Intent(AdminRegistrationStepThreeActivity.this, LoginActivity.class);
            intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent1);
            finish();
        });
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void registerAdmin() {
        if (new NetworkInfoUtility(getApplicationContext()).isConnectedToInternet()) {
            mProgressDialog = new MaterialDialog.Builder(AdminRegistrationStepThreeActivity.this)
                    .typeface(getResources().getFont(R.font.sf_ui_display_medium), getResources().getFont(R.font.sf_ui_display_regular))
                    .progress(true, 0)
                    .canceledOnTouchOutside(false)
                    .content(getResources().getString(R.string.content_dialog_registering))
                    .cancelable(false)
                    .canceledOnTouchOutside(false)
                    .build();
            mProgressDialog.show();

            mAuth.createUserWithEmailAndPassword(mEmail, mPassword)
                    .addOnSuccessListener(authResult -> {
                        Log.i("Account created with", mEmail);

                        final StorageReference filePath = mStorageRef.child("profileImages").child(Objects.requireNonNull(mAuth.getCurrentUser()).getUid() + ".jpg");

                        if (mImageUri != null) {
                            filePath.putFile(mImageUri)
                                    .addOnSuccessListener(taskSnapshot -> {

                                        filePath.getDownloadUrl()
                                                .addOnSuccessListener(uri -> {
                                                    Log.d("Profile image URI", String.valueOf(uri));
                                                    mProfileImageUri = uri;

                                                    Log.i("Profile image uploaded", String.valueOf(mProfileImageUri));
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Log.e("URL get failed", Objects.requireNonNull(e.getMessage()));
                                                    }
                                                });
                                    })
                                    .addOnFailureListener(e -> Log.e("Failed to upload image", Objects.requireNonNull(e.getMessage())));
                        } else {
                            mProfileImageUri = null;
                        }

                        Map<String, Object> rootData = new HashMap<>();
                        mUserPath = "/institutes/" + mInstitute + "/courses/" + mCourse + "/admin/" + Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                        rootData.put("userPath", mUserPath);
                        rootData.put("userStatus", mUserStatus);
                        rootData.put("username", mUsername);
                        rootData.put("profileImageUri", String.valueOf(mProfileImageUri));

                        mStore.collection("users")
                                .document(Objects.requireNonNull(mAuth.getCurrentUser()).getUid())
                                .set(rootData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.i("USER PATH CREATED", mUserPath);

                                    Map<String, Object> referenceData = new HashMap<>();
                                    referenceData.put("fullName", mFullName);
                                    referenceData.put("email", mEmail);
                                    referenceData.put("role", mRole);
                                    referenceData.put("gender", mGender);
                                    referenceData.put("password", mPassword);
                                    referenceData.put("institute", mInstitute);
                                    referenceData.put("course", mCourse);
                                    referenceData.put("id", mId);
                                    referenceData.put("birthday", mBirthday);
                                    referenceData.put("phoneNumber", mPhoneNumber);
                                    referenceData.put("username", mUsername);
                                    referenceData.put("about", mAbout);
                                    referenceData.put("termOrYear", mTermOrYear);
                                    referenceData.put("userStatus", mUserStatus);
                                    referenceData.put("profileEditAccess", mProfileEditAccess);
                                    referenceData.put("profileImageUri", String.valueOf(mProfileImageUri));

                                    mStore.collection("/institutes/" + mInstitute + "/courses/" + mCourse + "/admin/")
                                            .document(Objects.requireNonNull(mAuth.getCurrentUser()).getUid())
                                            .set(referenceData)
                                            .addOnSuccessListener(aVoid1 -> {
                                                Log.i("Data stored", Objects.requireNonNull(mAuth.getCurrentUser()).getUid());

                                                mAuth.signInWithEmailAndPassword(mEmail, mPassword)
                                                        .addOnSuccessListener(authResult1 -> {
                                                            Log.i("Signed in", Objects.requireNonNull(mAuth.getCurrentUser()).getUid());

                                                            SessionManager sessionManager = new SessionManager(AdminRegistrationStepThreeActivity.this, SessionManager.SESSION_USER_SESSION);
                                                            sessionManager.createUserSession(mAuth.getCurrentUser().getUid(), mUserPath, mUserStatus, mFullName, mEmail, mRole, mGender, mPassword, mInstitute, mCourse, mId, mBirthday, mPhoneNumber, mUsername, mAbout, mTermOrYear, String.valueOf(mProfileImageUri));
                                                            Intent intent = new Intent(getApplicationContext(), AdminDashboardActivity.class);
                                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                                                            mProgressDialog.dismiss();

                                                            startActivity(intent);
                                                            finish();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Log.e("Failed to logging in", Objects.requireNonNull(e.getMessage()));

                                                            mProgressDialog.dismiss();

                                                            Snacky.builder()
                                                                    .setActivity(AdminRegistrationStepThreeActivity.this)
                                                                    .setBackgroundColor(getResources().getColor(R.color.snackBar, getTheme()))
                                                                    .setText(R.string.content_snackBar_login_failed)
                                                                    .setTextColor(getResources().getColor(R.color.snackBarText, getTheme()))
                                                                    .setTextTypeface(getResources().getFont(R.font.sf_ui_display_regular))
                                                                    .setIcon(R.drawable.ic_outline_warning_24)
                                                                    .setTextSize(16)
                                                                    .setDuration(Snacky.LENGTH_LONG)
                                                                    .build()
                                                                    .show();

                                                            if (!new NetworkInfoUtility(getApplicationContext()).isConnectedToInternet()) {
                                                                mProgressDialog.dismiss();

                                                                new MaterialDialog.Builder(AdminRegistrationStepThreeActivity.this)
                                                                        .typeface(getResources().getFont(R.font.sf_ui_display_medium), getResources().getFont(R.font.sf_ui_display_regular))
                                                                        .title(R.string.title_dialog_no_internet)
                                                                        .content(R.string.content_dialog_no_internet)
                                                                        .icon(Objects.requireNonNull(getDrawable(R.drawable.ic_baseline_signal_wifi_off_24)))
                                                                        .positiveText(R.string.positive_text_dialog_no_internet)
                                                                        .negativeText(R.string.negative_text_dialog_no_internet)
                                                                        .canceledOnTouchOutside(false)
                                                                        .cancelable(false)
                                                                        .onPositive((dialog, which) -> {
                                                                            Intent intent = new Intent(getApplicationContext(), CommonRegistrationStepOneActivity.class);
                                                                            startActivity(intent);
                                                                        })
                                                                        .onNegative((dialog, which) -> {
                                                                        })
                                                                        .show();
                                                            }
                                                        });
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("Failed to store data", Objects.requireNonNull(e.getMessage()));

                                                mProgressDialog.dismiss();

                                                Snacky.builder()
                                                        .setActivity(AdminRegistrationStepThreeActivity.this)
                                                        .setBackgroundColor(getResources().getColor(R.color.snackBar, getTheme()))
                                                        .setText(R.string.content_snackBar_task_failed)
                                                        .setTextColor(getResources().getColor(R.color.snackBarText, getTheme()))
                                                        .setTextTypeface(getResources().getFont(R.font.sf_ui_display_regular))
                                                        .setIcon(R.drawable.ic_outline_warning_24)
                                                        .setTextSize(16)
                                                        .setDuration(Snacky.LENGTH_LONG)
                                                        .build()
                                                        .show();

                                                if (!new NetworkInfoUtility(getApplicationContext()).isConnectedToInternet()) {
                                                    mProgressDialog.dismiss();

                                                    new MaterialDialog.Builder(AdminRegistrationStepThreeActivity.this)
                                                            .typeface(getResources().getFont(R.font.sf_ui_display_medium), getResources().getFont(R.font.sf_ui_display_regular))
                                                            .title(R.string.title_dialog_no_internet)
                                                            .content(R.string.content_dialog_no_internet)
                                                            .icon(Objects.requireNonNull(getDrawable(R.drawable.ic_baseline_signal_wifi_off_24)))
                                                            .positiveText(R.string.positive_text_dialog_no_internet)
                                                            .negativeText(R.string.negative_text_dialog_no_internet)
                                                            .canceledOnTouchOutside(false)
                                                            .cancelable(false)
                                                            .onPositive((dialog, which) -> {
                                                                Intent intent = new Intent(getApplicationContext(), CommonRegistrationStepOneActivity.class);
                                                                startActivity(intent);
                                                            })
                                                            .onNegative((dialog, which) -> {
                                                            })
                                                            .show();
                                                }
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("USER PATH NOT CREATED", Objects.requireNonNull(e.getMessage()));

                                    mProgressDialog.dismiss();

                                    Snacky.builder()
                                            .setActivity(AdminRegistrationStepThreeActivity.this)
                                            .setBackgroundColor(getResources().getColor(R.color.snackBar, getTheme()))
                                            .setText(R.string.content_snackBar_task_failed)
                                            .setTextColor(getResources().getColor(R.color.snackBarText, getTheme()))
                                            .setTextTypeface(getResources().getFont(R.font.sf_ui_display_regular))
                                            .setIcon(R.drawable.ic_outline_warning_24)
                                            .setTextSize(16)
                                            .setDuration(Snacky.LENGTH_LONG)
                                            .build()
                                            .show();

                                    if (!new NetworkInfoUtility(getApplicationContext()).isConnectedToInternet()) {
                                        mProgressDialog.dismiss();

                                        new MaterialDialog.Builder(AdminRegistrationStepThreeActivity.this)
                                                .typeface(getResources().getFont(R.font.sf_ui_display_medium), getResources().getFont(R.font.sf_ui_display_regular))
                                                .title(R.string.title_dialog_no_internet)
                                                .content(R.string.content_dialog_no_internet)
                                                .icon(Objects.requireNonNull(getDrawable(R.drawable.ic_baseline_signal_wifi_off_24)))
                                                .positiveText(R.string.positive_text_dialog_no_internet)
                                                .negativeText(R.string.negative_text_dialog_no_internet)
                                                .canceledOnTouchOutside(false)
                                                .cancelable(false)
                                                .onPositive((dialog, which) -> {
                                                    Intent intent = new Intent(getApplicationContext(), CommonRegistrationStepOneActivity.class);
                                                    startActivity(intent);
                                                })
                                                .onNegative((dialog, which) -> {
                                                })
                                                .show();
                                    }
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Account not created", Objects.requireNonNull(e.getMessage()));

                        if (e instanceof FirebaseAuthUserCollisionException) {
                            mProgressDialog.dismiss();

                            new MaterialDialog.Builder(AdminRegistrationStepThreeActivity.this)
                                    .typeface(getResources().getFont(R.font.sf_ui_display_medium), getResources().getFont(R.font.sf_ui_display_regular))
                                    .title(R.string.title_dialog_email_already_registered)
                                    .content(R.string.content_dialog_email_already_registered)
                                    .icon(Objects.requireNonNull(getDrawable(R.drawable.ic_baseline_warning_24)))
                                    .positiveText(getResources().getString(R.string.positive_text_dialog_email_already_registered))
                                    .negativeText(getResources().getString(R.string.negative_text_dialog_email_already_registered))
                                    .canceledOnTouchOutside(false)
                                    .cancelable(false)
                                    .onPositive((dialog, which) -> {
                                        Intent intent = new Intent(AdminRegistrationStepThreeActivity.this, CommonRegistrationStepOneActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .onNegative((dialog, which) -> {
                                        Intent intent = new Intent(AdminRegistrationStepThreeActivity.this, LoginActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .show();
                        } else {
                            mProgressDialog.dismiss();

                            Snacky.builder()
                                    .setActivity(AdminRegistrationStepThreeActivity.this)
                                    .setBackgroundColor(getResources().getColor(R.color.snackBar, getTheme()))
                                    .setText(R.string.content_snackBar_login_failed)
                                    .setTextColor(getResources().getColor(R.color.snackBarText, getTheme()))
                                    .setTextTypeface(getResources().getFont(R.font.sf_ui_display_regular))
                                    .setIcon(R.drawable.ic_outline_warning_24)
                                    .setTextSize(16)
                                    .setDuration(Snacky.LENGTH_LONG)
                                    .build()
                                    .show();
                        }
                        if (!new NetworkInfoUtility(getApplicationContext()).isConnectedToInternet()) {
                            mProgressDialog.dismiss();
                            new MaterialDialog.Builder(AdminRegistrationStepThreeActivity.this)
                                    .typeface(getResources().getFont(R.font.sf_ui_display_medium), getResources().getFont(R.font.sf_ui_display_regular))
                                    .title(R.string.title_dialog_no_internet)
                                    .content(R.string.content_dialog_no_internet)
                                    .icon(Objects.requireNonNull(getDrawable(R.drawable.ic_baseline_signal_wifi_off_24)))
                                    .positiveText(R.string.positive_text_dialog_no_internet)
                                    .negativeText(R.string.negative_text_dialog_no_internet)
                                    .canceledOnTouchOutside(false)
                                    .cancelable(false)
                                    .onPositive((dialog, which) -> {
                                        Intent intent = new Intent(getApplicationContext(), CommonRegistrationStepOneActivity.class);
                                        startActivity(intent);
                                    })
                                    .onNegative((dialog, which) -> {
                                    })
                                    .show();
                        }
                    });
        } else {
            new MaterialDialog.Builder(AdminRegistrationStepThreeActivity.this)
                    .typeface(getResources().getFont(R.font.sf_ui_display_medium), getResources().getFont(R.font.sf_ui_display_regular))
                    .title(R.string.title_dialog_no_internet)
                    .content(R.string.content_dialog_no_internet)
                    .icon(Objects.requireNonNull(getDrawable(R.drawable.ic_baseline_signal_wifi_off_24)))
                    .positiveText(R.string.positive_text_dialog_no_internet)
                    .negativeText(R.string.negative_text_dialog_no_internet)
                    .canceledOnTouchOutside(false)
                    .cancelable(false)
                    .onPositive((dialog, which) -> {
                        Intent intent = new Intent(getApplicationContext(), CommonRegistrationStepOneActivity.class);
                        startActivity(intent);
                    })
                    .onNegative((dialog, which) -> {
                    })
                    .show();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Intent intent = new Intent(AdminRegistrationStepThreeActivity.this, AdminRegistrationStepTwoActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void requestFocus(View view) {
        if (view.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }

    private String validUsername() {
        String username = Objects.requireNonNull(mUsernameLayout.getEditText()).getText().toString().trim();
        Pattern usernamePattern = Pattern.compile("[a-zA-Z0-9._:\\p{Pd}]{1,20}");
        Matcher usernameMatcher = usernamePattern.matcher(username);

        if (username.isEmpty()) {
            mUsernameLayout.setError(getString(R.string.error_empty_roll_no_or_id));
            requestFocus(mUsernameField);
            return null;
        } else {
            if (usernameMatcher.matches()) {
                mUsernameLayout.setErrorEnabled(false);
                mUsername = username;
                return username;
            } else {
                mUsernameLayout.setError(getString(R.string.error_invalid_roll_no_or_id));
                requestFocus(mUsernameField);
                return null;
            }
        }
    }

    private String validAbout() {
        String about = Objects.requireNonNull(mAboutLayout.getEditText()).getText().toString().trim();
        Pattern aboutPattern = Pattern.compile("^.{1,256}$");
        Matcher aboutMatcher = aboutPattern.matcher(about);

        if (about.isEmpty()) {
            mAboutLayout.setError(getString(R.string.error_empty_about));
            requestFocus(mAboutField);
            return null;
        } else {
            if (aboutMatcher.matches()) {
                mAboutLayout.setErrorEnabled(false);
                mAbout = about;
                return about;
            } else {
                mAboutLayout.setError(getString(R.string.error_invalid_about));
                requestFocus(mAboutField);
                return null;
            }
        }
    }


    private class ValidationWatcher implements TextWatcher {

        private final View view;

        public ValidationWatcher(View view) {
            this.view = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @SuppressLint("NonConstantResourceId")
        @RequiresApi(api = Build.VERSION_CODES.O)
        public void afterTextChanged(Editable editable) {
            switch (view.getId()) {
                case R.id.textInputEditText_username:
                    validUsername();
                    break;
                case R.id.textInputEditText_about:
                    validAbout();
                    break;
            }
        }
    }
}