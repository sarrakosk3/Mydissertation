package com.example.mydissertation;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.util.Hex;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.base.Charsets;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

//import org.apache.commons.codec.binary.Hex;

import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
//import java.util.Base64;
import java.util.HashMap;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class RegisterActivity extends AppCompatActivity {

    EditText username, email, password;
    Button btn_register;
    CheckBox isSecure;

    FirebaseAuth auth;
    FirebaseFirestore reference;
    private static final String NONE = "none";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);


        Toolbar toolbar = findViewById(R.id.toolbar);           //με αυτές τις τεσσερις γραμμές κωδικα στηνουμε την πάνω οριζόντια μπάρα τις εφαρμογής
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Register");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        username = findViewById(R.id.username);                 //συνδέδουμε τα interface στοιχεία με τον κώδικά μας
        password = findViewById(R.id.password);
        email = findViewById(R.id.email);
        btn_register = findViewById(R.id.btn_register);
        isSecure = findViewById(R.id.isSecure);

        auth = FirebaseAuth.getInstance();      //Authentication Server της εφαρμογής μας

        btn_register.setOnClickListener(new View.OnClickListener() {        //πατώντας το κουμπί register γινεται το validation
            @Override                                                       //των πεδίων εγγραφής πυ έχει δώσει ο χρήστης
            public void onClick(View v) {
                String txt_username = username.getText().toString();        //παίρνουμε τα strings τον εισόδων
                String txt_email = email.getText().toString();
                String txt_password = password.getText().toString();
                boolean isSecureChecked;

                if(isSecure.isChecked()){
                    isSecure.setChecked(true);
                    isSecureChecked = true;
                }
                else{
                    isSecureChecked = false;
                }

                if (TextUtils.isEmpty(txt_username) || TextUtils.isEmpty(txt_email) || TextUtils.isEmpty(txt_password)) {   //αν εχουμε αφήσει πεδία κενά
                    Toast.makeText(com.example.mydissertation.RegisterActivity.this, "All fields Are required", Toast.LENGTH_LONG).show();//τυπωνουμε αντιστοιχο μηνυμα
                }   else if (txt_password.length()<6) {     //αν το password είναι πολύ μικρό
                    Toast.makeText(com.example.mydissertation.RegisterActivity.this, "password is too short", Toast.LENGTH_LONG).show();  //τυπωνουμε αντιστοιχο μηνυμα
                }   else{                                               //αν τα στοιχεία είναι επαρκή
                    register(txt_username, txt_email, txt_password, isSecureChecked);    //εγγράφουμε τον χρήστη στην υπηρεσία
                }

            }
        });
    }

    private void register(String username, String email, String password, boolean isSecureChecked){

        auth.createUserWithEmailAndPassword(email, password)    //δημιουργούμε χρήστη με email και password
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {       //παρακολουθούμε την ολοκηρωση της διαδικασιας
                    @Override                                                       //αυτής στο κύριο νήμα
                    public void onComplete(@NonNull Task<AuthResult> task) {        //και με την ολοκλήρωση αυτής
                        if (task.isSuccessful()){                                   //αν είναι επιτυχής
                            Log.d("Info", "Authentication succes");
                            FirebaseUser firebaseUser = auth.getCurrentUser();      //αναθέτουμε στο αντικείμεμενο firebaseUser
                                                                                    //τον χρήστη που επεστράφη με το πέρας της εγγραφής
                            assert firebaseUser !=null; // αν είναι null το γραφουμε στο stacktrace
                            String userId = firebaseUser.getUid();                  //παίρνουμε το id του χρήστη

                            reference = FirebaseFirestore.getInstance();     //με το οποίο κάνουμε αναφορά
                                                                                                                      //στη βάση δεδομένων
                            HashMap<String, Object> hashMap = new HashMap<>();  //  φτιαχνουμε ένα Key:Value ζευγάρι για τον χρήστη
                            hashMap.put("id", userId);
                            hashMap.put("username", username);
                            hashMap.put("ImageURL", "default");
                            hashMap.put("status", "offline");

                            if(isSecureChecked) { // Ελέγχουμε αν ο χρήστης ενεργοποίησε την επιλογή ασφαλούς Messaging

                                KeyPairGenerator keyGen = null;

                                try {
                                    keyGen = KeyPairGenerator.getInstance("DH", "BC");
                                    // Δημιουργεία ζεύγους δημοσίου - ιδιωτικού κλειδιού για την ανταλαγή κλειδιου Diffie-Hellman

                                } catch (NoSuchAlgorithmException e) {
                                    e.printStackTrace();
                                } catch (NoSuchProviderException e) {
                                    e.printStackTrace();
                                }

                                SecureRandom random = null;
                                try {

                                    byte bytes[] = new byte[20];
                                    random = SecureRandom.getInstanceStrong();
                                    random.nextBytes(bytes);
                                } catch (NoSuchAlgorithmException e) {
                                    e.printStackTrace();
                                }
                                keyGen.initialize(256,random);
                                SharedPreferences sharedPreferences = getSharedPreferences(userId, MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                KeyPair keyPair = keyGen.generateKeyPair();
                                PublicKey publicKey = keyPair.getPublic();
                                editor.putString("myPuKey", Base64.encodeToString(publicKey.getEncoded(), Base64.DEFAULT));
                                PrivateKey privateKey = keyPair.getPrivate();
                                editor.putString("myPrKey", Base64.encodeToString(privateKey.getEncoded(), Base64.DEFAULT));
                                hashMap.put("publicKey", Blob.fromBytes(publicKey.getEncoded()));

                                //εδω θα βάλω το public key μου, ενω παράλληλα θα αποθηκεύσω το private key μου στη συσκευή
                                editor.commit();
                            }
                            else {
                                try {
                                    hashMap.put("publicKey", Blob.fromBytes(NONE.getBytes("UTF8")));
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }


                            reference.collection("Users").document(userId).set(hashMap)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d("Info", "Writing successful");
                                            Intent intent = new Intent(com.example.mydissertation.RegisterActivity.this, com.example.mydissertation.MainActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                            startActivity(intent);
                                            finish();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(com.example.mydissertation.RegisterActivity.this, "Cannot write to database", Toast.LENGTH_LONG).show();
                                            Log.d("Info", "The error is "+e.toString());
                                        }
                                    });


                        }else {
                            Toast.makeText(com.example.mydissertation.RegisterActivity.this, "You can't register with this email or password", Toast.LENGTH_LONG).show();
                        }

                    }
                });

    }
}