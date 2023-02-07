package com.example.mydissertation.Fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.mydissertation.Adapter.MessageAdapter;
import com.example.mydissertation.Adapter.UserAdapter;
import com.example.mydissertation.Model.Chat;
import com.example.mydissertation.Model.User;
import com.example.mydissertation.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

//1-1-2023
//fragment που περιέχει τις συνομιλίες που έχω κάνει ανα χρήστη
public class ChatsFragment extends Fragment {

    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private CopyOnWriteArrayList<User> mUsers;

    FirebaseUser fuser;     //τοπικός χρήστης
    FirebaseFirestore reference;    //Firebase βάση δεδομένων

    private  CopyOnWriteArrayList<String> ContactsList;    //λίστα απομακρισμένων χρηστών με τους οποίου έχω επικοινωνία
    @SuppressLint("SuspiciousIndentation")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        //Authentication
        fuser = FirebaseAuth.getInstance().getCurrentUser();

        ContactsList = new CopyOnWriteArrayList<>();

        //Παίρνουμε  ένα snapshot της βάσης "Chats", από το οποίο τραβάμε τα μηνύματα που μας αφορούν
        //και συμπληρώνουμε την String λίστα  usersList των επαφών μας
                reference = FirebaseFirestore.getInstance();
                reference.collection("Chats")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                ContactsList.clear();
                ListIterator<String> contactListIterator = ContactsList.listIterator();

                    for (QueryDocumentSnapshot doc : value) {
                        Chat chat = doc.toObject(Chat.class);
                        contactListIterator.hasNext();

                        if (chat.getSender().equals(fuser.getUid())) {       //μηνύματα που έχω στείλει
                            ContactsList.add(chat.getReceiver());              //που τα έχω στείλει
                        }
                        if (chat.getReceiver().equals(fuser.getUid())) {     //μηνύματα που έχω λάβει
                            ContactsList.add(chat.getSender());                //απο πού τα έχω λάβει
                        }

                    }

                readChats(); //αναγνωση μηνυμάτων
            }
        });
        reference = FirebaseFirestore.getInstance();

        return view;
    }

    private void readChats(){
        mUsers = new CopyOnWriteArrayList<>();     //προσωρινή LOCAL  User λιστα επαφών που προορίζεται για την
        //δυναμική προβολή recyclerView

        //Παίρνουμε  ένα snapshot της βάσης "Users"

        reference =FirebaseFirestore.getInstance();

        reference.collection("Users").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                mUsers.clear();

                for(QueryDocumentSnapshot dataSnapshot : value){
                    User user = dataSnapshot.toObject(User.class);

                    //προσθέτουμε ένα User αντικείμενο στην λιστα mUsers
                    for(ListIterator<String> id = ContactsList.listIterator(); id.hasNext();){      //οποιοδήποτε στοιχείο της String λίστας επαφών "ταιριάζει"
                        if (user.getId().equals(id.next() )){   //με το id οποιουδήποτε χρήστη της User λίστας όλων των χρηστών
                            if (mUsers.size() != 0){    //και η User λίστα δεν είναι κενή
                                for (ListIterator<User> userl = mUsers.listIterator(); userl.hasNext();){
                                    if(!user.getId().equals(userl.next().getId())){ //και δεν βρίσκεται ήδη στην mUsers λίστα επαφών
                                        mUsers.add(user);
                                    }
                                }
                            }
                            else {
                                mUsers.add(user);
                            }
                        }
                    }
                }

                userAdapter =new UserAdapter(getContext(), mUsers, true);
                recyclerView.setAdapter(userAdapter);
            }
        });


    }
}