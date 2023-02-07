package com.example.mydissertation.Fragments;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.mydissertation.MainActivity;
import com.example.mydissertation.Model.User;
import com.example.mydissertation.R;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.CollectionReference;
//import com.google.firebase.firestore.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

//import org.apache.commons.codec.binary.Hex;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;

//1-1-2023
//Το τρέχον fragment προβάλλει το προφίλ του τοπικού χρήστη
//2-1-2023
//Αλλαγή φωτογραφίας χρήστη
public class ProfileFragment extends Fragment {

    CircleImageView image_profile;      //UI στοιχείο εικόνας προφιλ
    TextView username;                  //UI στοιχείο όνομα χρήστη
    CheckBox isSecure;
    Button update_btn;

    DocumentReference reference;
    Task<DocumentSnapshot> pukReference;
    FirebaseUser fuser;                 //τρέχων τοπικός χρήστης

    StorageReference storageReference;
    private static final int IMAGE_REQUEST = 1;
    private Uri imageUri; //αναγνωριστικό εικόνας χρήστη
    private StorageTask uploadTask;
    Blob mypublicKey;
    private static final String NONE = "none";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        image_profile = view.findViewById(R.id.profile_image);
        username = view.findViewById(R.id.username);
        isSecure = (CheckBox) view.findViewById(R.id.isSecure);
        update_btn = view.findViewById(R.id.update_btn);


        storageReference = FirebaseStorage.getInstance().getReference("Uploads");
        //συνδεόμαστε στον αποθηκευτικό χώρο FirebaseStorage του project μας

        //...αυθεντικοποιούμαστε και συνδεόμαστε στη βάση μας
        fuser = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseFirestore.getInstance().collection("Users").document(fuser.getUid());
        pukReference = FirebaseFirestore.getInstance().collection("Users").document(fuser.getUid()).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if(documentSnapshot.exists()){
                    User user = documentSnapshot.toObject(User.class);


                    mypublicKey = user.getPublicKey();
                    if(mypublicKey.equals(Blob.fromBytes(NONE.getBytes()))){
                        isSecure.setChecked(false);
                    }
                    else{
                        isSecure.setChecked(true);
                    }
                }
                else{
                    Toast.makeText(getContext(),"User does not exist", Toast.LENGTH_LONG ).show();
                }
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });

        //η ακόλουθη κλίση μεθόδου .addOnSnapshotListener "Βλέπει" αν ο χρήστης έχει κάποιο URL
        //φωτογραφίας συσχετισμένο με τον Λογαριασμό του στην υπηρεσία και θέτει την φωτογραφία αν
        //υπάρχει αλλιώς θέτει default φωτογραφία

        reference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {

                User user = documentSnapshot.toObject(User.class);
                username.setText(user.getUsername());

                if (user.getImageUrl().toString().equals("default")){      //εδω θα σκάσει
                    image_profile.setImageResource(R.mipmap.ic_launcher);

                }
                else {
                    Glide.with(getContext()).load(user.getImageUrl()).into(image_profile);
                     }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });

        //ορίζουμε την ενέργεια που πραγματοποιείται με το πάτημα του στοιχείου, η οποία στην
        //προκειμένη περίπτωση είναι η κλίση της openImage()
        image_profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImage();
            }
        });

        update_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SharedPreferences sharedPreferences = getActivity().getSharedPreferences(fuser.getUid(), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                HashMap<String, Object> hashMap = new HashMap<>();

                if(isSecure.isChecked()){
                    Log.i("Info", "Checkbox checked!");
                    isSecure.setChecked(true);
                    KeyPairGenerator keyGen = null;
                    try {
                        keyGen = KeyPairGenerator.getInstance("DH", "BC");
                        // Δημιουργεία ζεύγους δημοσίου - ιδιωτικού κλειδιού για την ανταλαγή κλειδιου Diffie-Hellman
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (NoSuchProviderException e) {
                        e.printStackTrace();
                    }
                    keyGen.initialize(256);



                    KeyPair keyPair = keyGen.generateKeyPair();
                    PublicKey publicKey = keyPair.getPublic();
                    Blob publicKeyBlob = Blob.fromBytes(publicKey.getEncoded());
                    String publicKeyString = Base64.encodeToString(publicKey.getEncoded(), Base64.DEFAULT);
                    editor.putString("myPuKey", publicKeyString);
                    PrivateKey privateKey = keyPair.getPrivate();
                    String privateKeyString = Base64.encodeToString(privateKey.getEncoded(), Base64.DEFAULT);
                    editor.putString("myPrKey", privateKeyString);
                    hashMap.put("publicKey", publicKeyBlob);
                    reference.update(hashMap);
                    //εδω θα βάλω το public key μου, ενω παράλληλα θα αποθηκεύσω το private key μου στη συσκευή
                    editor.commit();

                }
                else{
                    //isSecureChecked = false;
                    isSecure.setChecked(false);
                    hashMap.put("publicKey", "none");
                    //reference.updateChildren(hashMap);
                    editor.remove("myPuKey");
                    editor.remove("myPrKey");
                    editor.commit();
                }
            }
        });





        return view;


    }

    //συνάρτηση που ανοίγει τον "Explorer" του λειτουργικού Android για την επιλογή πόρου όπως έχει
    //οριστεί με την μέθδο setType (στην προκειμένη περίπτωση εικόνα)
    private void openImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, IMAGE_REQUEST);
    }


    //...επιστρέφει την επέκταση του ορίσματος Uri
    //η αφηρημένη κλάσση URI (Ομοιόμορφο αναγνωριστικό πόρου) όπως υποδηλώνει το όνομά του
    // χρησιμοποιείται για την αναγνώριση πόρου (είτε πρόκειται για σελίδα κειμένου, βίντεο ή ήχος,
    // ακίνητη ή κινούμενη εικόνα ή πρόγραμμα).
    //στην προκειμένη περίπτωση εικόνα
    private String getFileExtension(Uri uri){
        ContentResolver contentResolver = getContext().getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    //συνάρτηση που ανεβάζει την εικόνα που επιλέξαμε
    private void uploadImage(){
        final ProgressDialog pd = new ProgressDialog(getContext());
        pd.setMessage("Uploading");
        pd.show();


        if(imageUri != null){   //αν έχουμε επιλέξει κάποια εικόνα από τον τοπικό χώρο αποθήκευσης
            final StorageReference fileReference = storageReference.child(System.currentTimeMillis()
                    +"."+getFileExtension(imageUri)) ;      //δημιουργούμε κενή αναφορά στον Firebase χώρο αποθ.
                                                            //προς το "αρχείο"
                                                            //που επιλέξαμε και της δίνουμε ένα αυθαίρετο
                                                            //όνομα: στην προκειμένη περίπτωση System.currentTimeMillis().επέκταση

            uploadTask = fileReference.putFile(imageUri);   //συνδέουμε το URI της εικόνας (imageUri) με την
                                                            //"κενή" αναφορά (fileReference) και
                                                            //αναθέτουμε αυτό το "composite" αντικείμενο
                                                            //στο uploadTask το οποίο είναι Μια ελεγχόμενη
                                                            // εργασία που ανεβάζει και ενεργοποιεί συμβάντα
                                                            //για επιτυχία, πρόοδο και αποτυχία.

            //και με το ακόλουθο block κώδικα η .continueWithTask() μέθοδος Επιστρέφει ένα νέο Task που θα ολοκληρωθεί με το αποτέλεσμα
            //της εφαρμογής της καθορισμένης συνέχισης σε αυτό το Task. Η μέθοδος .continueWithTask() λειτουργεί
            //όπως η μέθοδος .accept() για τα tcp sockets αλλα αντί για νέο socket επιστρέφει νέο Task<Uri>
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()){
                        throw task.getException();
                    }

                    return fileReference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {       //με το πέρας του ανεβάσματος
                    if (task.isSuccessful()){                           //της εικόνας, θέτουμε στο πεδίο ImageURL της βάσης Users το URI που επεστράφει από την .continueWithTask
                        Uri downloadUri = task.getResult();             //ώστε να μπορούν οι απομακρισμένοι χρήστες να δούν την εικόνα προφίλ μας
                        String mUri = downloadUri.toString();
                        //reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("ImageURL", mUri);
                        reference.update(map);

                        pd.dismiss();
                    }
                    else {
                        Toast.makeText(getContext(), "Failed to upload profile picture", Toast.LENGTH_SHORT).show();
                        pd.dismiss();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                }
            });
        }
        else {
            Toast.makeText(getContext(), "No image selected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null){
            imageUri = data.getData();

            if (uploadTask != null && uploadTask.isInProgress()){
                Toast.makeText(getContext(), "Uploading...",Toast.LENGTH_SHORT).show();
            }
            else {
                uploadImage();
            }
        }
    }
}