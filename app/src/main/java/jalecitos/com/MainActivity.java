package jalecitos.com;

import android.content.ComponentName;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.customtabs.TrustedWebUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.placeholder.R;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int SESSION_ID = 9637575;
    private final int DELAY = 1500;

    @Nullable
    private TwaCustomTabsServiceConnection mServiceConnection;

    @Nullable
    private CustomTabsIntent mCustomTabsIntent;

    private String defaultUrl;
    private String path;
    private String scheme;
    private String authority;
    private String param;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        defaultUrl = getString(R.string.default_url);
        path = getString(R.string.path);
        scheme = getString(R.string.scheme);
        authority = getString(R.string.authority);
        param = getString(R.string.param);

        String customTabsProviderPackage = CustomTabsClient.getPackageName(this,
                TrustedWebUtils.SUPPORTED_CHROME_PACKAGES, false);


        mServiceConnection = new TwaCustomTabsServiceConnection();
        CustomTabsClient.bindCustomTabsService(
                this, customTabsProviderPackage, mServiceConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mServiceConnection != null) {
            unbindService(mServiceConnection);
        }
    }

    private void launchTwa(String url) {
        if (mCustomTabsIntent != null) {
            TrustedWebUtils.launchAsTrustedWebActivity(
                    MainActivity.this,
                    mCustomTabsIntent,
                    Uri.parse(url));
        }

    }

    private void init() {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        String token = task.getResult().getToken();
                        Log.d(TAG, "onComplete: " + token);

                        Uri.Builder builder = new Uri.Builder();
                        builder.scheme(scheme)
                                .authority(authority)
                                .appendPath(path)
                                .appendQueryParameter(param, token);

                        String url = builder.build().toString();
                        launchTwa(url);
                        finishThisActivity();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    /**
     * finish this activity after some delay
     */
    private void finishThisActivity() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, DELAY);
    }

    private class TwaCustomTabsServiceConnection extends CustomTabsServiceConnection {

        @Override
        public void onCustomTabsServiceConnected(ComponentName componentName,
                                                 CustomTabsClient client) {

            // Creates a CustomTabsSession with a constant session id.
            CustomTabsSession session = client.newSession(null, SESSION_ID);

            // Creates a CustomTabsIntent to launch the Trusted Web Activity.
            mCustomTabsIntent = new CustomTabsIntent.Builder(session).build();

            init();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "Twa CustomTab Service Disconnected");
        }
    }
}
