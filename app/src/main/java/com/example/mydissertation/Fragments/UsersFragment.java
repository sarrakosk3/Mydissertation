package com.example.mydissertation.Fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.example.mydissertation.Adapter.UserAdapter;
import com.example.mydissertation.Model.User;
import com.example.mydissertation.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.Query;
//import com.google.firebase.database.ValueEventListener;
//import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

//fragment χρηστών
//19-5-2021
//12-7-2021 αναζήτηση απομακρυσμένων χρηστών της υπηρεσίας (case insensitive??  )
public class UsersFragment extends Fragment {

    private RecyclerView recyclerView;  //δυναμική λίστα
    private UserAdapter userAdapter;    //προσαρμογέας
    private List<User> mUsers;

    EditText searchUserByName;

    //Η μέθοδος onCreateView καλείτε αυτόματα με το που εμφανίζουμε - μεταβαίνουμε στο παρων Fragment
    //και επιστρέφει

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_users, container, false);  //κάνουμε inflate το layout xml αρχείο fragment_users.xml

        recyclerView = view.findViewById(R.id.reycler_view); //'παίρνουμε το στοιχείο recycler_view'
        recyclerView.setHasFixedSize(true);                  //και το προσαρμόζουμε
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mUsers = new ArrayList<>();     //δημιουργούμε μια νέα κενή λίστα

        readUsers();                    //την οποία γεμίζουμε με τους εγγεγραμμένους FirebaseUsers

        searchUserByName = view.findViewById(R.id.search_user_by_name);  //αναζητούμε χρήστη
        searchUserByName.addTextChangedListener(new TextWatcher() {     //όταν γράφουμε στο πλαίσιο αναζήτητσης
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
               search_User(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        return view;
    }

    private void search_User(String s) {        //12-7-2021 αναζήτηση απομακρισμένων χρηστών

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();    //αυθεντικοποιούμε τον τοπικό χρήστη
        Query user_query = FirebaseFirestore.getInstance()
                .collection("Users")
                .whereNotEqualTo("username", firebaseUser.getUid())
                .orderBy("username")
                .startAt(s).endAt(s+"\uf8ff");          //

        user_query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (!searchUserByName.getText().toString().equals("")) {
                    mUsers.clear();
                    for(QueryDocumentSnapshot doc : value){
                        User user  = doc.toObject(User.class);
                        assert user != null;
                        assert firebaseUser != null;
                        mUsers.add(user);

                    }

                    userAdapter = new UserAdapter(getContext(), mUsers, false);
                    recyclerView.setAdapter(userAdapter);
                }
            }
        });

    }


    private void readUsers() {

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();    //βρισκουμε των τρέχοντα εγγεγραμμένο χρήστη
        //DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users"); //συνδεόμαστε στην βάση χρηστών
        Query allUsersQuery = FirebaseFirestore
                .getInstance()
                .collection("users")
                .whereNotEqualTo("username", firebaseUser.getUid());

        allUsersQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {

                mUsers.clear(); //καθαρίζουμε την 'κενή' λιστα από σκουπίδια
                for(QueryDocumentSnapshot doc : value){
                    User user = doc.toObject(User.class);
                    assert user != null;
                    assert firebaseUser != null;
                    mUsers.add(user);
                }
            }
        });
        
    }
}

/* ***ΠΑΡΑΤΗΡΗΣΗ***
* Τόσο η μέθοδος onDataChange(@NonNull DataSnapshot snapshot) όσο και η μέθοδος
* onCancelled(@NonNull DatabaseError error) δημιουργούντε αυτόματα, σαν ορίσματα, εγκλεισμένες σε
* άγκριστρα, με το που δίνουμε όρισμα new ValueEventListener() στην μέθοδο addValueEventListener()
* το reference αντικειμένου*/