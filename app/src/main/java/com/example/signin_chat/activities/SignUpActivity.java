package com.example.signin_chat.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.InetAddresses;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.MediaController;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;


import com.example.signin_chat.R;
import com.example.signin_chat.databinding.ActivitySignUpBinding;
import com.example.signin_chat.utilities.Constants;
import com.example.signin_chat.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.ktx.Firebase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;


public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private PreferenceManager preferenceManager;
    private String encodeImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());

        setListener();
    }

    private void setListener() {
        binding.textSignIn.setOnClickListener(v -> onBackPressed());

        binding.signUpButton.setOnClickListener(v -> {
            if (isValidateSignUpDetails()){
                SignUp();
            }
        });

        binding.layoutImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }



    private void showToast(String msg){
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void SignUp(){
        //check loading
        loading(true);

        //Post on Firebase
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        HashMap<String,String> user = new HashMap<>();
        user.put(Constants.KEY_NAME,binding.nameInput.getText().toString());
        user.put(Constants.KEY_EMAIL,binding.emailInput.getText().toString());
        user.put(Constants.KEY_PASSWORD,binding.passwordInput.getText().toString());

        user.put(Constants.KEY_IMAGE,encodeImage);

        db.collection(Constants.KEY_COLLECTION_USERS)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    loading(false);

                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_NAME,binding.nameInput.getText().toString());
                    preferenceManager.putString(Constants.KEY_IMAGE, encodeImage);

                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                }).addOnFailureListener(exception -> {
                    loading(false);
                    showToast(exception.getMessage());
                });
    }

    private String encodeImage(Bitmap bitmap){
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight()*previewWidth / bitmap.getWidth();

        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);

        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes,Base64.DEFAULT);
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK){
                    Uri imageUri = result.getData().getData();
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                        binding.imageProfile.setImageBitmap(bitmap);
                        binding.textAddImage.setVisibility(View.GONE);
                        encodeImage = encodeImage(bitmap);

                    }catch (FileNotFoundException e){
                        e.printStackTrace();
                    }
                }
            }
    );

    private Boolean isValidateSignUpDetails(){
        if(encodeImage == null){
            showToast("Please upload an image");
            return false;
        }else if(binding.nameInput.getText().toString().trim().isEmpty()){
            showToast("Please Enter your Name");
            return false;
        } else if (binding.emailInput.getText().toString().trim().isEmpty()) {
            showToast("Please Enter your Email");
            return false;
        }else if (!Patterns.EMAIL_ADDRESS.matcher(binding.emailInput.getText().toString()).matches()){
            showToast("Please Enter a Valid Email");
            return false;
        } else if (binding.passwordInput.getText().toString().trim().isEmpty()) {
            showToast("Please Enter a Password");
            return false;
        } else if (binding.confirmPasswordInput.getText().toString().trim().isEmpty()) {
            showToast("Please Confirm Your Password");
            return false;
        } else if (!binding.passwordInput.getText().toString().equals(binding.confirmPasswordInput.getText().toString())) {
            showToast("Passwords must match");
            return false;
        }else{
            return true;
        }
    }
    private void loading(Boolean isLoading){
        if (isLoading){
            binding.signUpButton.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        }else{
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.signUpButton.setVisibility(View.VISIBLE);
        }
    }
}