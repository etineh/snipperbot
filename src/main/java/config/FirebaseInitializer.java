package config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;

@Component
public class FirebaseInitializer {

//    @Value("${firebase.key.path}")
//    private String path;

//    @Value("${firebase.key.databaseUrl}")
//    private String databaseUrl;

    @PostConstruct
    public void initialize() {
        try {
            // Read the Firebase key JSON from the environment variable
            String firebaseKeyJson = System.getenv("FIREBASE_KEY_JSON");
            String databaseUrl = System.getenv("FIREBASE_DATABASE_URL");

            if (firebaseKeyJson == null || firebaseKeyJson.isEmpty()) {
                throw new IllegalStateException("FIREBASE_KEY_JSON environment variable is not set or empty.");
            }

            // Load the credentials from the JSON content
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(firebaseKeyJson.getBytes())
            );

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(credentials)
                    .setDatabaseUrl(databaseUrl)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                checkDatabaseConnection();
                System.out.println("Firebase initialized successfully");
            } else {
                System.out.println("Firebase already initialized");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    @PostConstruct
//    public void initialize() {
//        try {
//            FileInputStream serviceAccountStream = new FileInputStream(path);
//
//            FirebaseOptions options = new FirebaseOptions.Builder()
//                    .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
//                    .setDatabaseUrl(databaseUrl)
//                    .build();
//
//            if (FirebaseApp.getApps().isEmpty())
//            {
//                FirebaseApp.initializeApp(options);
//                checkDatabaseConnection();
//
//            } else {
////                checkDatabaseConnection();
//                System.out.println("Firebase already initialized");
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    private static final String TEST_URL = "http://www.google.com";

    public boolean isInternetAvailable() {
        RestTemplate restTemplate = new RestTemplate();
        try {
            restTemplate.getForObject(TEST_URL, String.class);
            return true;
        } catch (Exception e) {
            System.err.println("Internet connectivity check failed: " + e.getMessage());
            return false;
        }
    }

    private void checkDatabaseConnection()
    {
        boolean isConnected = isInternetAvailable();
        if (isConnected)
        {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("testing");

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        System.out.println("Firebase connection successful.");
                    } else {
                        System.out.println("Data does not exist.");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Error: " + databaseError.getMessage());
                }
            });

            System.out.println("Internet connection is available.");

        } else {
            System.err.println("No internet connection.");
        }

    }

}
