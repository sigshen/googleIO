package istaging.com.firebasestoragedemo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ListViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.database.Cursor;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import istaging.com.firebasestoragedemo.adapter.UsersAdapter;
import istaging.com.firebasestoragedemo.adapter.items.User;
import istaging.com.firebasestoragedemo.service.MyFirebaseMessagingService;

public class MainActivity extends AppCompatActivity implements FirebaseUtils.OnUploadListener {
    final String TAG = "MainActivity";
    int RESULT_LOAD_IMAGE = 0;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        /*
        Button button = (Button) findViewById(R.id.upload_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                // startActivityForResult(intent, RESULT_LOAD_IMAGE);

                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), RESULT_LOAD_IMAGE);
            }
        });
        */

        // receive notification from firebase?
        Intent intent = getIntent();
        if (intent != null && intent.getStringExtra(MyFirebaseMessagingService.EXTRA_MESSAGE) != null) {
            String message = intent.getStringExtra(MyFirebaseMessagingService.EXTRA_MESSAGE);
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
        }

        // Construct the data source
        User u1 = new User("Nathan", "San Diego");
        User u2 = new User("Sig", "New taipei");
        User[] users = {u1, u2};
        ArrayList<User> arrayOfUsers = new ArrayList<User>(Arrays.asList(users));

        // Create the adapter to convert the array of views
        UsersAdapter adapter = new UsersAdapter(this, arrayOfUsers);
        // Attach the adapter to ListView
        ListView listView = (ListView) findViewById(R.id.lvItems);
        listView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Log.d(TAG, "If there's an upload in progress, save the reference so you can query it laster");
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        Log.d(TAG, "If there was an upload in progress, get its reference and create a new StorageReference");
    }

    @Override
    protected  void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            ArrayList<String> filepaths = new ArrayList<>(Arrays.asList(
                    "/storage/emulated/0/Download/panorama-20.jpg",
                    "/storage/emulated/0/Download/test3.jpg"
            ));
            doUpload(filepaths);
        }
    }

    @Override
    public void onUploadProgress(final Map<String, Double> map) {
        for (Map.Entry<String, Double> entry: map.entrySet()) {
            String msg = entry.getKey() + " Upload is " + entry.getValue() + "% done";
            Log.d(TAG, msg);
        }
    }

    @Override
    public void onDone(final Map<String, Integer> statueMap, final Map<String, String> downloadMap) {
        for (Map.Entry<String, String> entry: downloadMap.entrySet()) {
            String downloadUrl = entry.getValue();
            Log.d(TAG, downloadUrl);
        }
    }

    private void doUpload(final ArrayList<String> filepaths) {
        int numOfTasks = filepaths.size();

        FirebaseUtils firebaseUtils = FirebaseUtils.getInstance(this, numOfTasks);
        firebaseUtils.doUpload(filepaths);
    }
}
