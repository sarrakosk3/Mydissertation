package com.example.mydissertation;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;


//
public class LoginActivity extends AppCompatActivity {

    EditText email, password;
    Button btn_login;

    FirebaseAuth auth;
    TextView password_reset;
    private static final String NONE = "none";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //στις επόμενες 4 γραμμές θετουμε το στοιχείο toolbar, σαν κεφαλίδα του κάθε Activity, το
        // οποίο περιέχει τον τίτλο του κάθε Activity και το popup menu με το κουμπι για αποσύνδεσητου
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Login");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        auth = FirebaseAuth.getInstance(); //συνδεόμαστε με τον "Αuthentication Server"

        email = findViewById(R.id.email);           //συνδέουμε το interface με τον κώδικά μας
        password = findViewById(R.id.password);
        btn_login = findViewById(R.id.btn_login);
        password_reset = findViewById(R.id.password_reset);


        /*πατώντας στο "reset your password" μεταφερόμαστε στο Activity PasswordResetActivity που
        * αλλαζουμε κωδικό*/
        password_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(com.example.mydissertation.LoginActivity.this, com.example.mydissertation.PasswordResetActivity.class));
            }
        });


        /*Ακολουθεί η μέθοδος setOnClickListener() με την οποία κάνουμε το login στην υπηρεσία */
        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //  Παίρνουμε τα string των login credentials
                String txt_email = email.getText().toString();
                String txt_password = password.getText().toString();

                if (TextUtils.isEmpty(txt_email) || TextUtils.isEmpty(txt_password)){
                    //αν έχουμε κενά πεδία τυπώνουμε αντίστοιχο μηνυμα
                    Toast.makeText(com.example.mydissertation.LoginActivity.this, "All fields are required", Toast.LENGTH_LONG).show();
                }   else {
                    auth.signInWithEmailAndPassword(txt_email, txt_password)    //κάνουμε το signin στην υπηρεσία
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {   //με onComleteListener
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()){        //αν η διαδικασία ολοκληρωθεί με επιτυχία  μεταβαίνουμε στην MainActivity
                                Intent intent = new Intent(com.example.mydissertation.LoginActivity.this, com.example.mydissertation.MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            }   else {
                                Toast.makeText(com.example.mydissertation.LoginActivity.this, "Authentication failed", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }
        });
    }
}