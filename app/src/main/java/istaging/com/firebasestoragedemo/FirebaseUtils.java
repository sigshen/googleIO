package istaging.com.firebasestoragedemo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by iStaging on 2016/6/23.
 */
public class FirebaseUtils {
    OnUploadListener mCallback;

    Map<String, Double> progressMap = new HashMap<String, Double>();
    Map<String, Integer> statusMap = new HashMap<String, Integer>(); // 0: in progress, 1: success, -1: failed
    Map<String, String> downloadMap = new HashMap<String, String>();

    public final String TAG = "FirebaseUtils";
    public final String FIREBASE_STORAGE_URL = "gs://project-508672422003711190.appspot.com";
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private int numOfTasks = 0;

    SharedPreferences sharedPreferences;
    final String FIREBASE_SHARED_PREF_NAME = "firebasedata";

    private static FirebaseUtils singleton;
    private FirebaseUtils(Activity activity, int numOfTasks) {
        // This makes sure that the container activity has implemented
        // the callback interface. if not, it throws an exception
        try {
            mCallback = (OnUploadListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
             + "must implement OnUploadListener");
        }

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

        mAuth.addAuthStateListener(mAuthListener);
        // sign in as an anonymous user
        mAuth.signInAnonymously()
                .addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInAnonymously:onComplete:" + task.isSuccessful());
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInAnonymously", task.getException());
                        }
                    }
                });


        this.numOfTasks = numOfTasks;
    }

    public static FirebaseUtils getInstance(Activity activity, int numOfTasks) {
        if (singleton == null) {
            return new FirebaseUtils(activity, numOfTasks);
        }
        return singleton;
    }

    public void doUpload(final ArrayList<String> filepaths) {
        for (String filepath: filepaths) {
            upload(filepath);
        }
    }

    public void upload(String filepath) {
        FirebaseStorage storage = FirebaseStorage.getInstance();

        // Create a storage reference from our app
        StorageReference storageRef = storage.getReferenceFromUrl(FIREBASE_STORAGE_URL);

        UploadTask uploadTask;
        File uploadFile = new File(filepath);

        if (uploadFile.exists()) {
            Log.d(TAG, "file exist");
            Uri file = Uri.fromFile(uploadFile);
            final String fileName = file.getLastPathSegment();
            final StorageReference imageRef = storageRef.child("images/" + fileName);

            statusMap.put(fileName, 0);

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
                    // Log.d(TAG, "Upload is " + progress + "% done");
                    progressMap.put(fileName, progress);
                    mCallback.onUploadProgress(progressMap);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, "Handle unsuccessful uploads");
                    Log.d(TAG, e.toString());

                    statusMap.put(fileName, -1);
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // taskSnapShot.getMetadata() contains file metadata such as size, content-type, and download URL.
                    Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    // Log.d(TAG, "downloadUrl: " + downloadUrl);
                    downloadMap.put(fileName, downloadUrl.toString());
                    statusMap.put(fileName, 1);

                    // Delete sharedPreferences
                    sharedPreferences.edit().remove(imageRef.toString()).commit();

                    if (checkAllDone()) {
                        mCallback.onDone(statusMap, downloadMap);
                    }
                }
            });
        } else {
            Log.d(TAG, "File not found.");
        }
    }

    private boolean checkAllDone() {
        int count = 0;
        for (Map.Entry<String, Integer> entry: statusMap.entrySet()) {
            int status = entry.getValue();
            if ((status == 1) || (status == -1)) {
                count++;
            }
        }
        return (count == numOfTasks) ? true : false;
    }

    public interface OnUploadListener {
        public void onUploadProgress(final Map<String, Double> map);
        public void onDone(final Map<String, Integer> statueMap, final Map<String, String> downloadMap);
    }
}
