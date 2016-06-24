package istaging.com.firebasestoragedemo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;

/**
 * Created by iStaging on 2016/6/23.
 */
public class FirebaseUtils {

    public final String TAG = "FirebaseUtils";
    public final String FIREBASE_STORAGE_URL = "gs://project-508672422003711190.appspot.com";
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    SharedPreferences sharedPreferences;
    final String FIREBASE_SHARED_PREF_NAME = "firebasedata";

    private static FirebaseUtils singleton;
    private FirebaseUtils(Activity activity) {
        sharedPreferences = activity.getSharedPreferences(FIREBASE_SHARED_PREF_NAME, Context.MODE_PRIVATE);
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };
    }

    public static FirebaseUtils getInstance(Activity activity) {
        if (singleton == null) {
            return new FirebaseUtils(activity);
        }
        return singleton;
    }

    public void upload(String picturePath) {
        FirebaseStorage storage = FirebaseStorage.getInstance();

        // Create a storage reference from our app
        StorageReference storageRef = storage.getReferenceFromUrl(FIREBASE_STORAGE_URL);

        UploadTask uploadTask;
        File uploadFile = new File(picturePath);

        if (uploadFile.exists()) {
            Log.d(TAG, "file exist");
            Uri file = Uri.fromFile(uploadFile);
            final StorageReference imageRef = storageRef.child("images/" + file.getLastPathSegment());

            String sessionUriFromStorage = sharedPreferences.getString(imageRef.toString(), null);
            if (sessionUriFromStorage != null) {
                Log.d(TAG, "awesome resume!");
                // resume the upload task from where it left off when the process died.
                // to do this, pass the sessionUri as the last parameter
                uploadTask = imageRef.putFile(file, new StorageMetadata.Builder().build(), Uri.parse(sessionUriFromStorage));
            } else {
                Log.d(TAG, "normal resume!");
                uploadTask = imageRef.putFile(file);
            }

            uploadTask.addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                    Log.d(TAG, "Upload is paused");
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                    Uri sessionUri = taskSnapshot.getUploadSessionUri();
                    if (sessionUri != null) {
                        // save the sessionUri to persistent storage in case the process dies.
                        sharedPreferences.edit().putString(imageRef.toString(), sessionUri.toString()).apply();
                    }
                    Log.d(TAG, "Upload is " + progress + "% done");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, "Handle unsuccessful uploads");
                    Log.d(TAG, e.toString());
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // taskSnapShot.getMetadata() contains file metadata such as size, content-type, and download URL.
                    Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    Log.d(TAG, "downloadUrl: " + downloadUrl);
                    // Delete sharedPreferences
                    sharedPreferences.edit().remove(imageRef.toString()).commit();
                }
            });
        } else {
            Log.d(TAG, "File not found.");
        }
    }
}
