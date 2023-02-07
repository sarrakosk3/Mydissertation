package com.example.mydissertation;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.util.Log;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.mydissertation.Adapter.MessageAdapter;
import com.example.mydissertation.Model.Chat;
import com.example.mydissertation.Model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.base.Charsets;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;


import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;




//14-1-2023

public class MessageActivity extends AppCompatActivity {

    CircleImageView profile_image;
    TextView username;
    boolean isSecureFlag = false;

    //Shared secret

    FirebaseUser fuser;
    CollectionReference colReference;
    DocumentReference pukReference, docReference;

    ImageButton send_btn;
    EditText send_text;

    MessageAdapter messageAdapter;
    List<Chat> mychat;

    RecyclerView recyclerView;

    Intent intent;

    SecretKey sharedAESKey;

    PublicKey remotePublicKey;

    PrivateKey myPrivateKey;

    SharedPreferences myKeyPairSharedPreferences, myAESKeys;
    private static final String NONE = "none";


    //η μέθοδος onCreate() δημιουργείται αυτόματα και θεωρείται η 'main()' του κάθε activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        Toolbar toolbar = findViewById(R.id.toolbar);       //θετουμε το συνήθες toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivity(new Intent(MessageActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            }
        });

        /*Στις 10 επόμενες γραμμές κώδικα 'στήνουμε' το user interface το παρόντος activity, αρχικά
        βρίσκοντας το recyclerView το αντίστοιχου layout αρχείου (στην προκειμένη περίπτωση
        activity_message.xml) και το ορίζουμε να είναι γραμμικό-κάθετο layout. Στο στοιχείο
        recycler_view θα προβάλετε η συνομιλία μεταξύ του τοπικού και απομακρισμένου χρήστη.Επίσης
        συνδλεουμε όλα τα UI elements μετον κώδικα του Activity*/
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        profile_image = findViewById(R.id.profile_image);
        username = findViewById(R.id.username);
        send_btn = findViewById(R.id.sendBtn);
        send_text = findViewById(R.id.sendText);


        /*παίρνουμε' το userid του απομακρισμένου χρήστη από το extra της κλήσης του παρόντος activity*/
        intent = getIntent();
        final String userid = intent.getStringExtra("userid");
        fuser = FirebaseAuth.getInstance().getCurrentUser();    //Αυθεντικοποιούμαστε

        //ανταλλαγή δημοσίων κλειδιών και δημιουργεία Shared secret με το οποίο κρυπτογραφώ και αποκρυπτογραφώ τα μηνύματα
        pukReference = FirebaseFirestore.getInstance().collection("Users").document(userid);

        pukReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                User user = documentSnapshot.toObject(User.class);

                if (user.getPublicKey().equals(Blob.fromBytes(NONE.getBytes()))){

                    isSecureFlag =false;
                }
                else {
                    isSecureFlag=true;
                    KeyFactory factory;

                    if (getSharedPreferences("myAESKeys",MODE_PRIVATE).getString(fuser.getUid() + "to" + userid,null)==null){
                        Log.i("Info", "Exchange Diagnostic: Executing Key agreement...");
                        Blob blobPublicKey = user.getPublicKey();
                        Log.i("Info", "Exchange Diagnostic: his public key is "+blobPublicKey);
                        myKeyPairSharedPreferences = getSharedPreferences(fuser.getUid(), MODE_PRIVATE);
                        myAESKeys = getSharedPreferences("myAESKeys", MODE_PRIVATE);
                        String strMyPrivateKey = myKeyPairSharedPreferences.getString("myPrKey", null);
                        byte[] byteMyPrivateKey = Base64.decode(strMyPrivateKey, Base64.DEFAULT);
                        Log.i("Info", "Exchange Diagnostic: my private key is "+strMyPrivateKey);

                        try {
                            factory = KeyFactory.getInstance("DH", "BC");
                            remotePublicKey = factory.generatePublic(new X509EncodedKeySpec(blobPublicKey.toBytes()));//Μετατροπή του String public Key απο string σε PublicKey
                            Log.i("Info", "Exchange Diagnostic:   his public key generated by the String is"+remotePublicKey);
                            myPrivateKey = factory.generatePrivate(new PKCS8EncodedKeySpec(byteMyPrivateKey));
                            Log.i("Info", "Exchange Diagnostic:  my private key generated by the String is"+myPrivateKey);
                            KeyAgreement ka = KeyAgreement.getInstance("DH", "BC");
                            Log.i("Info","Exchange Diagnostic: Key Agreement instantiated");
                            ka.init(myPrivateKey);
                            Log.i("Info","Exchange Diagnostic: Key Agreement initialized with local private key:"+myPrivateKey+
                                    " and remote public key:"+remotePublicKey);
                            ka.doPhase(remotePublicKey, true);
                            Log.i("Info","Exchange Diagnostic: Key Agreement finalized with "+remotePublicKey);
                            sharedAESKey = ka.generateSecret("AES");    //δημιουργία συμμετρικού κλειδιου
                            Log.i("Info","Exchange Diagnostic: The Shared AES Key is"+sharedAESKey);
                            //Base64.Encoder encoder = Base64.getEncoder();
                            String stringSharedAESKey = new String(sharedAESKey.getEncoded(), Charsets.ISO_8859_1);
                            SharedPreferences.Editor editor = myAESKeys.edit();
                            editor.putString(fuser.getUid() + "to" + userid, stringSharedAESKey);
                            editor.commit();
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        } catch (NoSuchProviderException e) {
                            e.printStackTrace();
                        } catch (InvalidKeySpecException e) {
                            e.printStackTrace();
                        } catch (InvalidKeyException e) {
                            e.printStackTrace();
                        } catch (IllegalStateException e){
                            e.printStackTrace();
                        }

                    }
                    else{
                        myAESKeys = getSharedPreferences("myAESKeys", MODE_PRIVATE);
                        String strSharedAESKey = myAESKeys.getString(fuser.getUid() + "to" + userid,null);
                        Log.i("Info","Agreement already in place");
                        Log.i("Info","String Shared AES key is  "+strSharedAESKey);
                        byte[] decodedKey = strSharedAESKey.getBytes(Charsets.ISO_8859_1);
                        sharedAESKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
                    }

                    Log.i("Info", "established Shared secret with user: "+userid+" is "+sharedAESKey);
                }

                readMessages(fuser.getUid(), userid, user.getImageUrl());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });


        //και αποστέλλουμε μηνύματα
        send_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = send_text.getText().toString();
                if(!msg.equals("")){
                    try {
                        sendMessage(fuser.getUid(), userid, msg);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else {
                    Toast.makeText(com.example.mydissertation.MessageActivity.this, "message must not be empty", Toast.LENGTH_SHORT).show();
                }
                send_text.setText("");
            }
        });

        //συνδεόμαστε στη βάση δεδομένων πραγματικού χρόνου
        docReference = FirebaseFirestore.getInstance().collection("Users").document(userid);
        docReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                User user = documentSnapshot.toObject(User.class);
                username.setText(user.getUsername());
                if (user.getImageUrl().equals("default")){
                    profile_image.setImageResource(R.mipmap.ic_launcher);//διαβάζουμε το thumbnail το χρήστη
                }
                else{
                    Glide.with(com.example.mydissertation.MessageActivity.this).load(user.getImageUrl()).into(profile_image);
                }

                readMessages(fuser.getUid(), userid, user.getImageUrl()); //διαβάζουμε τα μηνυματα απο και προς τον χρήστη
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });


    }

    public static byte[] encrypt(byte[] pText, SecretKey secret, byte[] iv) throws Exception {

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        Log.i("Info", "AES ολα καλα...");
        cipher.init(Cipher.ENCRYPT_MODE, secret, new GCMParameterSpec(128, iv));
        Log.i("Info", "AES ολα καλα...2");
        byte[] encryptedText = cipher.doFinal(pText);
        return encryptedText;

    }

    public static byte[] decrypt(byte[] cText, SecretKey secret, byte[] iv) throws Exception {

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        Log.i("Info","Decryption Diagnostic: secret Key is "+secret);
        cipher.init(Cipher.DECRYPT_MODE, secret, new GCMParameterSpec(128, iv));
        byte[] plainText = cipher.doFinal(cText);
        return  plainText;
    }

    public static byte[] getRandomNonce(int numBytes) {
        byte[] nonce = new byte[numBytes];
        new SecureRandom().nextBytes(nonce);
        return nonce;
    }

    public static String getRandomString(int i)
    {

        // bind the length
        byte[] bytearray = new byte[256];
        String mystring;
        StringBuffer thebuffer;
        String theAlphaNumericS;

        new Random().nextBytes(bytearray);

        mystring = new String(bytearray, Charset.forName("UTF-8"));

        thebuffer = new StringBuffer();

        //remove all special char
        theAlphaNumericS = mystring.replaceAll("[^A-Z0-9]", "");

        //random selection
        for (int m = 0; m < theAlphaNumericS.length(); m++) {

            if (Character.isLetter(theAlphaNumericS.charAt(m)) && (i > 0) || Character.isDigit(theAlphaNumericS.charAt(m)) && (i > 0)) {
                thebuffer.append(theAlphaNumericS.charAt(m));
                i--;
            }
        }

        // the resulting string
        return thebuffer.toString();
    }

    //αποστέλλουμε μηνυμα ως hashMap με ta 'πεδία' sender, receiver και message
    private void sendMessage (String sender, String receiver, String message) throws Exception {

        CollectionReference reference = FirebaseFirestore.
                                        getInstance().
                                        collection("Chats");
        String iv_string, cipher_str;

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", sender);
        hashMap.put("receiver", receiver);
        if (isSecureFlag) {
            byte[] iv=getRandomNonce(12);
            Blob ivBlob = Blob.fromBytes(iv);
            byte[] cipherBytes = encrypt(message.getBytes(),sharedAESKey, iv);//byte[] cipherBytes = encrypt(Base64.getMimeDecoder().decode(message),sharedAESKey, iv);
            Blob cipherBlob = Blob.fromBytes(cipherBytes);
            Log.i("Info", "Diagnostic: CipherText to be sent is "+cipherBlob+" in its Hexadecimal Representation");
            hashMap.put("message", cipherBlob);

            //iv_str = iv.toString();//iv_str = new String(Base64.getEncoder().encode(iv));
            Log.i("Info", "Diagnostic: iv byte array length is "+ivBlob.toBytes().length+" and its string representation is "+ivBlob.toString());

            hashMap.put("iv", ivBlob);
        }
        else {
            hashMap.put("message", Blob.fromBytes(message.getBytes()));
            hashMap.put("iv", Blob.fromBytes(NONE.getBytes()));
        }

        reference.add(hashMap)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                Log.i("Info", "Diagnostic: Message sent to "+receiver);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i("Info", "Diagnostic: Message failed ");
            }
        });

    }

    /*διαβάζουμε τα υπάρχοντα μηνύμα που αφορούν τον τρέχοντα και απομακρισμένο χρήστη, τα
    τοποθετούμε στο τοπικό ΑrrayList<> myChat το οποίο τροφοδοτούμε στο 'global' messageAdapter του
    παρόντος Activity για προβολή */

    @SuppressLint("SuspiciousIndentation")
    private void readMessages(String myid, String userid, String imageurl){
        mychat = new ArrayList<>();

        colReference = FirebaseFirestore.getInstance()
                .collection("Chats");

                colReference.addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        mychat.clear();
                        for(QueryDocumentSnapshot doc : value){
                            Chat chat = doc.toObject(Chat.class);
                            if (((chat.getSender().equals(myid))
                                    &&(chat.getReceiver().equals(userid)))
                                    ||((chat.getReceiver().equals(myid))
                                    &&(chat.getSender().equals(userid)))) {
                                if(chat.getIv().equals("none")){
                                    mychat.add(chat);
                                }
                                else{
                                    try {

                                        byte[] iv = chat.getIv().toBytes();//byte[] iv = Base64.getDecoder().decode(chat.getIv());
                                        //Log.i("Info","Decryption Diagnostic: String IV is"+ chat.getIv());
                                        Log.i("Info","Decryption Diagnostic: String IV length is"+ iv.length+" and its byte representation is "+iv);
                                        //String hexCipherText = chat.getMessage();
                                        byte[] cipherBytes = chat.getMessage().toBytes();
                                        Log.i("Info", "Decryption Diagnostic: CipherText to be decrypted is"+chat.getMessage()+" and its byte representation is "+cipherBytes);
                                        byte[] plaintextbyte = decrypt(cipherBytes, sharedAESKey, iv);
                                        Blob plaintextBlob = Blob.fromBytes(plaintextbyte);
                                        chat.setMessage(plaintextBlob);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    mychat.add(chat);

                                }
                            }
                        }

                        messageAdapter = new MessageAdapter(com.example.mydissertation.MessageActivity.this, mychat, imageurl);
                        recyclerView.setAdapter(messageAdapter);

                    }
                });


    }

    private void status(String status){
        docReference = FirebaseFirestore.getInstance().collection("Users").document(fuser.getUid());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);

        docReference.update(hashMap);
    }

    @Override
    protected void onResume() {
        super.onResume();
        status("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        status("offline");
    }
}