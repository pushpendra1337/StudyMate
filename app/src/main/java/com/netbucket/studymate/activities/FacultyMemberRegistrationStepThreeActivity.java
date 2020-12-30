package com.netbucket.studymate.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.netbucket.studymate.R;
import com.netbucket.studymate.utils.NetworkInfoUtility;
import com.netbucket.studymate.utils.SessionManager;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;
import de.mateware.snacky.Snacky;

public class FacultyMemberRegistrationStepThreeActivity extends AppCompatActivity {

    final String mSemOrYear = "NA";
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
    String mProfileEditAccess = "revoked";
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
    private Uri mImageUri;
    private Uri mProfileImageUri;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                mImageUri = Objects.requireNonNull(result).getUri();
                mProfileImage.setImageURI(mImageUri);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception exception = Objects.requireNonNull(result).getError();
                Toast.makeText(this, "Error:" + exception, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_member_registration_step_three);

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
        mUserStatus = "disallowed";

        mUsernameField.addTextChangedListener(new ValidationWatcher(mUsernameField));
        mAboutField.addTextChangedListener(new ValidationWatcher(mAboutField));

        mStore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        FirebaseStorage mStorage = FirebaseStorage.getInstance();
        mStorageRef = mStorage.getReference();

        mBackButton.setOnClickListener(view -> onBackPressed());

        mCameraFab.setOnClickListener(view -> CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1, 1)
                .start(FacultyMemberRegistrationStepThreeActivity.this));

        mRegisterButton.setOnClickListener(view -> {
            if ((validUsername() != null) && (validAbout() != null)) {
                registerFaculty();
            }
        });

        mLoginActivityLink.setOnClickListener(view -> {
            Intent intent1 = new Intent(FacultyMemberRegistrationStepThreeActivity.this, LoginActivity.class);
            intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent1);
            finish();
        });
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void registerFaculty() {
        if (new NetworkInfoUtility(getApplicationContext()).isConnectedToInternet()) {
            mProgressDialog = new MaterialDialog.Builder(FacultyMemberRegistrationStepThreeActivity.this)
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
                        Log.i("Account account with:", mEmail);

                        final StorageReference filePath = mStorageRef.child("profileImages").child(Objects.requireNonNull(mAuth.getCurrentUser()).getUid() + ".jpg");
                        filePath.putFile(mImageUri)
                                .addOnSuccessListener(taskSnapshot -> filePath.getDownloadUrl().addOnSuccessListener(uri -> {
                                    Log.d("Profile image URI:", String.valueOf(uri));
                                    mProfileImageUri = uri;

                                    Map<String, String> rootData = new HashMap<>();
                                    mUserPath = "/institutes/" + mInstitute + "/courses/" + mCourse + "/facultyMembers/" + Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                                    rootData.put("userPath", mUserPath);
                                    rootData.put("userStatus", mUserStatus);
                                    rootData.put("username", mUsername);
                                    rootData.put("profileImageUri", String.valueOf(mProfileImageUri));

                                    mStore.collection(("/users/"))
                                            .document(Objects.requireNonNull(mAuth.getCurrentUser()).getUid())
                                            .set(rootData)
                                            .addOnSuccessListener(aVoid -> {
                                                Log.i("Reference created:", mUserPath);

                                                Map<String, String> referenceData = new HashMap<>();
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
                                                referenceData.put("semOrYear", mSemOrYear);
                                                referenceData.put("userStatus", mUserStatus);
                                                referenceData.put("profileEditAccess", mProfileEditAccess);
                                                referenceData.put("profileImageUri", String.valueOf(mProfileImageUri));

                                                mStore.collection("/institutes/" + mInstitute + "/courses/" + mCourse + "/facultyMembers/")
                                                        .document(Objects.requireNonNull(mAuth.getCurrentUser()).getUid())
                                                        .set(referenceData)
                                                        .addOnSuccessListener(aVoid1 -> {
                                                            Log.i("Data stored:", Objects.requireNonNull(mAuth.getCurrentUser()).getUid());

                                                            mAuth.signInWithEmailAndPassword(mEmail, mPassword)
                                                                    .addOnSuccessListener(authResult1 -> {
                                                                        Log.i("Signed in:", Objects.requireNonNull(mAuth.getCurrentUser()).getUid());

                                                                        SessionManager sessionManager = new SessionManager(FacultyMemberRegistrationStepThreeActivity.this, SessionManager.SESSION_USER_SESSION);
                                                                        sessionManager.createUserSession(mAuth.getCurrentUser().getUid(), mUserPath, mUserStatus, mFullName, mEmail, mRole, mGender, mPassword, mInstitute, mCourse, mId, mBirthday, mPhoneNumber, mUsername, mAbout, mSemOrYear, String.valueOf(mProfileImageUri));
                                                                        Intent intent = new Intent(getApplicationContext(), UserApprovalPendingActivity.class);
                                                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                                        mProgressDialog.dismiss();
                                                                        startActivity(intent);
                                                                        finish();
                                                                    })
                                                                    .addOnFailureListener(e -> {
                                                                        Log.e("Failed to logging in:", Objects.requireNonNull(e.getMessage()));

                                                                        mProgressDialog.dismiss();

                                                                        Snacky.builder()
                                                                                .setActivity(FacultyMemberRegistrationStepThreeActivity.this)
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
                                                                            new MaterialDialog.Builder(FacultyMemberRegistrationStepThreeActivity.this)
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
                                                            Log.e("Failed to store data:", Objects.requireNonNull(e.getMessage()));

                                                            mProgressDialog.dismiss();

                                                            Snacky.builder()
                                                                    .setActivity(FacultyMemberRegistrationStepThreeActivity.this)
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
                                                                new MaterialDialog.Builder(FacultyMemberRegistrationStepThreeActivity.this)
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
                                                Log.e("Reference not created:", Objects.requireNonNull(e.getMessage()));

                                                mProgressDialog.dismiss();

                                                Snacky.builder()
                                                        .setActivity(FacultyMemberRegistrationStepThreeActivity.this)
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
                                                    new MaterialDialog.Builder(FacultyMemberRegistrationStepThreeActivity.this)
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
                                }))
                                .addOnFailureListener(e -> Toast.makeText(getApplicationContext(), "NOT UPLOADED", Toast.LENGTH_SHORT).show());
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Account not created:", Objects.requireNonNull(e.getMessage()));

                        if (e instanceof FirebaseAuthUserCollisionException) {
                            mProgressDialog.dismiss();
                            new MaterialDialog.Builder(FacultyMemberRegistrationStepThreeActivity.this)
                                    .typeface(getResources().getFont(R.font.sf_ui_display_medium), getResources().getFont(R.font.sf_ui_display_regular))
                                    .title(R.string.title_dialog_email_already_registered)
                                    .content(R.string.content_dialog_email_already_registered)
                                    .icon(Objects.requireNonNull(getDrawable(R.drawable.ic_baseline_warning_24)))
                                    .positiveText(getResources().getString(R.string.positive_text_dialog_email_already_registered))
                                    .negativeText(getResources().getString(R.string.negative_text_dialog_email_already_registered))
                                    .canceledOnTouchOutside(false)
                                    .cancelable(false)
                                    .onPositive((dialog, which) -> {
                                        Intent intent = new Intent(FacultyMemberRegistrationStepThreeActivity.this, CommonRegistrationStepOneActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .onNegative((dialog, which) -> {
                                        Intent intent = new Intent(FacultyMemberRegistrationStepThreeActivity.this, LoginActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .show();
                        } else {
                            mProgressDialog.dismiss();

                            Snacky.builder()
                                    .setActivity(FacultyMemberRegistrationStepThreeActivity.this)
                                    .setBackgroundColor(getResources().getColor(R.color.snackBar, getTheme()))
                                    .setText(R.string.content_snackBar_task_failed)
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
                            new MaterialDialog.Builder(FacultyMemberRegistrationStepThreeActivity.this)
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
            new MaterialDialog.Builder(FacultyMemberRegistrationStepThreeActivity.this)
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
        Intent intent = new Intent(FacultyMemberRegistrationStepThreeActivity.this, FacultyMemberRegistrationStepTwoActivity.class);
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
            mUsernameLayout.setError(getString(R.string.error_empty_username));
            requestFocus(mUsernameField);
            return null;
        } else {
            if (usernameMatcher.matches()) {
                mUsernameLayout.setErrorEnabled(false);
                mUsername = username;
                return username;
            } else {
                mUsernameLayout.setError(getString(R.string.error_invalid_username));
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

        private ValidationWatcher(View view) {
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